# Contributing

Thanks for considering a contribution to `null-markeder`.

## Reporting bugs / requesting features

Open an [issue](https://github.com/haisi/null-markeder/issues) with a minimal repro (a package layout and the
`package-info.java` state that triggers the problem) for bugs, or a short description of the use case for
feature requests.

## Making changes

1. Fork the repo and create a branch off `main`.
2. Build and test:
   ```shell
   ./mvnw verify
   ```
3. Keep the change focused — this is a small, single-purpose library. If you're proposing something larger
   (a new public API, a new dependency), open an issue first to discuss the approach before writing code.
4. Follow the existing code style (see `.editorconfig`); there's no separate formatter enforced yet.
5. Open a pull request against `main` describing the change and why it's needed. CI (`.github/workflows/ci.yml`)
   runs the build and test suite on every PR.

## Releasing

Releases are cut by a maintainer by pushing a `vX.Y.Z` tag — see the [Releasing](README.md#releasing) section
of the README. Contributors don't need to worry about versioning or publishing.

## Code of Conduct

This project follows the [Code of Conduct](CODE_OF_CONDUCT.md).
