package li.selman.nullmarkeder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PackageInfoGeneratorTest {

    @Test
    void generatesPackageInfoForPackageMissingOne(@TempDir Path rootDir) throws IOException {
        Path pkgDir = rootDir.resolve("com/example/foo");
        Files.createDirectories(pkgDir);

        PackageInfoGenerator.updatePackageInfoFiles(rootDir.toString(), "com.example");

        Path packageInfo = pkgDir.resolve("package-info.java");
        assertThat(packageInfo).exists();
        assertThat(Files.readString(packageInfo))
                .contains("@NullMarked")
                .contains("package com.example.foo;")
                .contains("import org.jspecify.annotations.NullMarked;");
    }

    @Test
    void addsAnnotationToExistingPackageInfoMissingIt(@TempDir Path rootDir) throws IOException {
        Path pkgDir = rootDir.resolve("com/example/bar");
        Files.createDirectories(pkgDir);
        Files.writeString(pkgDir.resolve("package-info.java"), "package com.example.bar;\n");

        PackageInfoGenerator.updatePackageInfoFiles(rootDir.toString(), "com.example");

        String content = Files.readString(pkgDir.resolve("package-info.java"));
        assertThat(content).contains("@NullMarked").contains("import org.jspecify.annotations.NullMarked;");
    }

    @Test
    void isIdempotentForAlreadyAnnotatedPackage(@TempDir Path rootDir) throws IOException {
        Path pkgDir = rootDir.resolve("com/example/baz");
        Files.createDirectories(pkgDir);
        String original = """
                @NullMarked
                package com.example.baz;

                import org.jspecify.annotations.NullMarked;
                """;
        Files.writeString(pkgDir.resolve("package-info.java"), original);

        PackageInfoGenerator.updatePackageInfoFiles(rootDir.toString(), "com.example");

        String content = Files.readString(pkgDir.resolve("package-info.java"));
        assertThat(content).containsOnlyOnce("@NullMarked").containsOnlyOnce("import org.jspecify.annotations.NullMarked;");
    }
}
