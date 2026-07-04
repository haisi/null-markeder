# null-markeder

A library and sample test to ensure that each package in your java project contains a `package-info.java` file
**with** the jspecify `@NullMarked` annotation.

## Usage

Add dependency

```xml
<dependency>
    <groupId>li.selman</groupId>
    <artifactId>null-markeder</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

You'll also need `archunit-junit5`, `jspecify` and `assertj-core` (as `test` scope) in your own project if you
don't already have them.

Add an ArchUnit test

```java
@AnalyzeClasses(packages = "com.example", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String ROOT_PACKAGE = "com.example";

    /** Enforce that all packages contain a `package-info.java` annotated with `@NullMarked`.
     * Run {@link PackageInfoGenerator} to fix. */
    @ArchTest
    void packagesShouldBeAnnotated(JavaClasses classes) throws IOException {
        var rootPackage = classes.getPackage(ROOT_PACKAGE);
        List<String> violations = rootPackage.getSubpackagesInTree().stream()
                .filter(pkg -> !pkg.isAnnotatedWith(NullMarked.class))
                .map(pkg -> pkg.getDescription() + " is not annotated with @" + NullMarked.class.getSimpleName())
                .toList();

        if (!violations.isEmpty()) {
            PackageInfoGenerator.main(ROOT_PACKAGE);
        }

        assertThat(violations)
                .as("Not all packages contain a package-info.java file with the required nullability annotations. "
                        + "Ran PackageInfoGenerator to fix - re-run the build.")
                .isEmpty();
    }
}
```

## Building

```shell
mvn verify
```

## Releasing

Releases are published to Maven Central via [JReleaser](https://jreleaser.org). Pushing a tag matching `v*`
(e.g. `v1.0.0`) triggers `.github/workflows/release.yml`, which stages the build artifacts and hands them to
JReleaser to sign and deploy to the [Central Portal](https://central.sonatype.com).

```shell
git tag v1.0.0
git push origin v1.0.0
```

See `jreleaser.yml` for the deployment configuration.