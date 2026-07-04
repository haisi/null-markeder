package li.selman.nullmarkeder;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.google.errorprone.annotations.Var;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates or updates {@code package-info.java} files so that every package in a tree is annotated
 * with JSpecify's {@code @NullMarked} annotation.
 */
public final class PackageInfoGenerator {

    private static final String DEFAULT_ROOT_DIR = "src/main/java";

    private static final Set<String> REQUIRED_ANNOTATIONS = Set.of("NullMarked");

    private static final Set<String> ANNOTATION_IMPORTS = Set.of("org.jspecify.annotations.NullMarked");

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

        if (result.isPresent()) {
            CompilationUnit cu = result.get();
            @Var boolean modified = false;

            for (String importName : ANNOTATION_IMPORTS) {
                if (cu.getImports().stream()
                        .noneMatch(imp -> imp.getNameAsString().equals(importName))) {
                    cu.addImport(importName);
                    modified = true;
                }
            }

            if (cu.getPackageDeclaration().isEmpty()) {
                // A package-info.java with no package declaration has nowhere to attach an
                // annotation to; give it one matching its location so the fix below actually sticks.
                cu.setPackageDeclaration(packageName);
                modified = true;
            }
            NodeList<AnnotationExpr> annotations =
                    cu.getPackageDeclaration().orElseThrow().getAnnotations();

            Set<String> existingAnnotations =
                    annotations.stream().map(AnnotationExpr::getNameAsString).collect(Collectors.toSet());

            for (String required : REQUIRED_ANNOTATIONS) {
                if (!existingAnnotations.contains(required)) {
                    annotations.add(new MarkerAnnotationExpr(new Name(required)));
                    modified = true;
                }
            }

            if (modified) {
                String updatedContent = cu.toString();
                Files.writeString(packageInfoPath, updatedContent);
                System.out.println("Updated " + packageInfoPath);
            }
        }
    }

    private static void generateNewPackageInfo(Path packageInfoPath, String packageName) throws IOException {
        StringBuilder content = new StringBuilder();
        content.append("@NullMarked\n");
        content.append("package ").append(packageName).append(";\n\n");
        content.append("import org.jspecify.annotations.NullMarked;\n");

        Files.writeString(packageInfoPath, content.toString());
        System.out.println("Generated new package-info.java for " + packageName);
    }

    private static List<String> findPackages(Path rootDir, String basePackage) throws IOException {
        String basePath = basePackage.replace('.', '/');
        Path realRoot = Paths.get(rootDir.toString(), basePath);

        try (Stream<Path> pathStream = Files.walk(realRoot)) {
            return pathStream
                    .filter(Files::isDirectory)
                    .map(path -> realRoot.relativize(path).toString())
                    .filter(path -> !path.isEmpty())
                    .map(path -> path.replace('/', '.'))
                    .map(path -> basePackage + "." + path)
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
