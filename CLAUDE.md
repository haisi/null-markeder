# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

`null-markeder` is a single-class, test-scope Java library (`li.selman:null-markeder`, published to Maven
Central). It gives consumers `PackageInfoGenerator`, a tool that generates or fixes up `package-info.java`
files so every package in a tree carries JSpecify's `@NullMarked` annotation. Consumers pair it with an
ArchUnit test in their own project (pattern shown in the README's Usage section) that fails when a package is
missing the annotation, and calls `PackageInfoGenerator.main(rootPackage)` to self-heal before asserting.
`ArchitectureTest` in this repo dogfoods that exact pattern on the library's own source.

There is intentionally only one public class. Resist adding a second one without a strong reason - the
scope is meant to stay this narrow.

## Commands

```shell
./mvnw verify                 # full build: compile, test, 100% coverage gate, format/style/nullness checks
./mvnw verify -Dquick          # fast loop: compile + test only, skips every quality gate below
./mvnw spotless:apply          # auto-format Java (palantir-java-format) and sort pom.xml - run before committing
./mvnw test -Dtest=PackageInfoGeneratorTest#mainRequiresAtLeastOneArgument   # single test
```

`verify` runs, in order: compile (Error Prone + NullAway), tests, JaCoCo report, package/javadoc/sources jars,
Spotless check, Checkstyle check, JaCoCo coverage check. All of these gate the build - a green `verify` means
all of them passed, not just tests.

All plugins other than the compiler and Surefire (tests) live in the `qa` profile, which is active by
default and only deactivates when the `quick` system property is set (`-Dquick`, any value). With `qa`
inactive, Maven falls back to a bare `javac` compile via the default lifecycle bindings (still respecting
`maven.compiler.release`) - no Error Prone/NullAway, no `-Werror`, no Spotless/Checkstyle, no JaCoCo, no
javadoc/sources jars. Useful while hacking locally; don't rely on a `-Dquick` build for anything you intend
to commit or release - always run a plain `./mvnw verify` before that.

### Releasing

```shell
./bumpPomVersion.sh   # interactive: pick major/minor/patch/free-text, sets version, offers to commit
./release.sh          # pulls, checks current version > latest GitHub release, tags and pushes vX.Y.Z
```

Pushing a `vX.Y.Z` tag triggers `.github/workflows/release.yml`, which sets the version from the tag, builds,
stages artifacts to a local repo, and runs JReleaser (`jreleaser.yml`) to sign and deploy to Maven Central via
the Central Portal, and to create the GitHub Release with a generated changelog.

`dryrun-release.sh` exercises the JReleaser signing/deploy path locally against real GPG keys (`public.asc`/
`private.asc`, gitignored) and Central Portal credentials (`.env`, gitignored) - use it to debug release
config changes without waiting on CI.

## Commit conventions

Commit messages must follow [Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/#specification)
(`<type>[optional scope]: <description>`, e.g. `fix:`, `feat:`, `build:`, `docs:`, `chore:`; append `!` or a
`BREAKING CHANGE:` footer for breaking changes).

## Build enforcement to know about

- **JaCoCo coverage is enforced at 100% (line and branch)**, not just measured. `verify` fails below that.
  This means every branch you add needs a test that hits it - including error paths. Coverage numbers alone
  don't prove correctness though: it's possible to hit a line without the code under test actually doing the
  right thing (this project's git history has a real example - a bug in `PackageInfoGenerator` had 100%
  coverage while the fix it claimed to make was silently dropped).
- **Error Prone's `-Xep:Var:ERROR`** requires every reassigned local variable to carry
  `@com.google.errorprone.annotations.Var`. This is enabled project-wide via `maven-compiler-plugin`.
- **NullAway** is configured with `AnnotatedPackages=li.selman.nullmarkeder` - it statically checks null-safety
  for this package specifically.
- **`.mvn/jvm.config`** carries `--add-exports`/`--add-opens` flags that Error Prone needs to hook into javac
  internals on JDK 16+. Without this file the build fails with an unhelpful error - don't remove it.
- **Checkstyle's `IllegalImport` rule** blocks non-jspecify `@Nullable` imports (Spring's, JetBrains', etc.),
  steering toward `org.jspecify.annotations.Nullable` - fitting, given the library's purpose.
- Java baseline is 25 (`maven.compiler.release`). Since this is a test-scope dependency, bumping it raises the
  minimum JDK for every consumer's build, not just this one - that tradeoff has been deliberately revisited
  before (briefly dropped to 17 for broader compatibility, then reverted back to 25).
