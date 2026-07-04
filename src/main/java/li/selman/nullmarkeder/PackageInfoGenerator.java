package li.selman.nullmarkeder;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates or updates {@code package-info.java} files so that every package in a tree is annotated
 * with JSpecify's {@code @NullMarked} annotation.
 */
public final class PackageInfoGenerator {

    private static final System.Logger LOGGER = System.getLogger(PackageInfoGenerator.class.getName());

    private static final String DEFAULT_ROOT_DIR = "src/main/java";

    private static final String REQUIRED_ANNOTATION = "NullMarked";

    private static final String ANNOTATION_IMPORT = "org.jspecify.annotations.NullMarked";

    private PackageInfoGenerator() {}

    public static void updatePackageInfoFiles(String rootDir, String basePackage) throws IOException {
        List<String> packages = findPackages(Paths.get(rootDir), basePackage);

        for (String packageName : packages) {
            Path packageInfoPath = Paths.get(rootDir, packageName.replace('.', '/'), "package-info.java");

            if (Files.exists(packageInfoPath)) {
                updateExistingPackageInfo(packageInfoPath, packageName);
            } else {
                generateNewPackageInfo(packageInfoPath, packageName);
            }
        }
    }

    private static void updateExistingPackageInfo(Path packageInfoPath, String packageName) throws IOException {
        JavaParser parser = new JavaParser();
        String content = Files.readString(packageInfoPath);
        Optional<CompilationUnit> result = parser.parse(content).getResult();

        if (result.isEmpty()) {
            LOGGER.log(Level.WARNING, "Skipping {0}: could not be parsed as valid Java source", packageInfoPath);
            return;
        }

        CompilationUnit cu = result.get();
        @Var boolean modified = false;

        if (cu.getImports().stream().noneMatch(imp -> imp.getNameAsString().equals(ANNOTATION_IMPORT))) {
            cu.addImport(ANNOTATION_IMPORT);
            modified = true;
        }

        if (cu.getPackageDeclaration().isEmpty()) {
            // A package-info.java with no package declaration has nowhere to attach an
            // annotation to; give it one matching its location so the fix below actually sticks.
            cu.setPackageDeclaration(packageName);
            modified = true;
        }
        NodeList<AnnotationExpr> annotations =
                cu.getPackageDeclaration().orElseThrow().getAnnotations();

        boolean alreadyAnnotated =
                annotations.stream().anyMatch(a -> a.getNameAsString().equals(REQUIRED_ANNOTATION));
        if (!alreadyAnnotated) {
            annotations.add(new MarkerAnnotationExpr(new Name(REQUIRED_ANNOTATION)));
            modified = true;
        }

        if (modified) {
            String updatedContent = cu.toString();
            Files.writeString(packageInfoPath, updatedContent);
            LOGGER.log(Level.INFO, "Updated {0}", packageInfoPath);
        }
    }

    private static void generateNewPackageInfo(Path packageInfoPath, String packageName) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("@NullMarked\n");
        content.append("package ").append(packageName).append(";\n\n");
        content.append("import org.jspecify.annotations.NullMarked;\n");

        Files.writeString(packageInfoPath, content.toString());
        LOGGER.log(Level.INFO, "Generated new package-info.java for {0}", packageName);
    }

    private static List<String> findPackages(Path rootDir, String basePackage) throws IOException {
        String basePath = basePackage.replace('.', '/');
        Path realRoot = Paths.get(rootDir.toString(), basePath);

        try (Stream<Path> pathStream = Files.walk(realRoot)) {
            return pathStream
                    .filter(Files::isDirectory)
                    .map(path -> realRoot.relativize(path).toString())
                    // An empty relative path means `path` is realRoot itself - i.e. basePackage - which
                    // must be included, not just its subpackages (see PackageInfoGeneratorTest for the
                    // regression this covers: this method used to silently skip basePackage entirely).
                    .map(relative -> relative.isEmpty() ? basePackage : basePackage + "." + relative.replace('/', '.'))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Usage: {@code PackageInfoGenerator.main(rootPackage)} or
     * {@code PackageInfoGenerator.main(rootPackage, rootDir)}. Defaults {@code rootDir} to
     * {@value #DEFAULT_ROOT_DIR}.
     */
    public static void main(String... args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Usage: PackageInfoGenerator.main(rootPackage) or PackageInfoGenerator.main(rootPackage, rootDir)");
        }
        String basePackage = args[0];
        String rootDir = args.length > 1 ? args[1] : DEFAULT_ROOT_DIR;
        updatePackageInfoFiles(rootDir, basePackage);
    }
}
