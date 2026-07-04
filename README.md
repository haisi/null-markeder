# null-markeder

[![CI](https://github.com/haisi/null-markeder/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/haisi/null-markeder/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen)](.github/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/li.selman/null-markeder.svg)](https://central.sonatype.com/artifact/li.selman/null-markeder)
[![Javadoc](https://javadoc.io/badge2/li.selman/null-markeder/javadoc.svg)](https://javadoc.io/doc/li.selman/null-markeder)
[![License](https://img.shields.io/github/license/haisi/null-markeder)](LICENSE)

A library and sample test to ensure that each package in your java project contains a `package-info.java` file
**with** the jspecify `@NullMarked` annotation.

[**Website**](https://selman.li/null-markeder/)

## Usage

Add dependency

```xml
<dependency>
    <groupId>li.selman</groupId>
    <artifactId>null-markeder</artifactId>
    <version>VERSION</version>
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
./mvnw verify
```

Test coverage is enforced at 100% (line and branch) via JaCoCo; `verify` fails if it drops below that. Run
`open target/site/jacoco/index.html` after a build to see the report.

`verify` also runs Spotless (palantir-java-format + sorted `pom.xml`), Checkstyle, and Error Prone/NullAway via
the compiler plugin. Run `./mvnw spotless:apply` to auto-format before committing.

## Releasing

Releases are published to Maven Central via [JReleaser](https://jreleaser.org). Pushing a tag matching `v*`
(e.g. `v1.0.0`) triggers `.github/workflows/release.yml`, which stages the build artifacts and hands them to
JReleaser to sign and deploy to the [Central Portal](https://central.sonatype.com).

```shell
./bumpPomVersion.sh
./release.sh
```

## Contributing

Bug reports, feature requests and pull requests are welcome — see [CONTRIBUTING.md](CONTRIBUTING.md). This
project follows a [Code of Conduct](CODE_OF_CONDUCT.md); by participating you agree to abide by it.

## License

`null-markeder` is licensed under the [Apache License, Version 2.0](LICENSE).

See `jreleaser.yml` for the deployment configuration.
