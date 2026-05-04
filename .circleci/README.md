# CircleCI for `forms-modernizer`

This directory contains the CircleCI 2.1 pipeline for the project. The
design was inspired by the [`adobe/aem-core-forms-components`][ref] CircleCI
setup and trimmed down to fit this project's actual structure.

[ref]: https://github.com/adobe/aem-core-forms-components/tree/master/.circleci

## Files

| File           | Purpose                                                             |
| -------------- | ------------------------------------------------------------------- |
| `config.yml`   | CircleCI workflow definition (build, coverage, cypress, release).   |
| `settings.xml` | Maven settings: Adobe public repo + Sonatype credentials.           |
| `codecov.yml`  | Codecov status / coverage flag scoping (core bundle).               |
| `README.md`    | This document.                                                      |

## Workflow

```
                 ┌──────────────────┐
                 │  build-java-11   │──┐
   on every      ├──────────────────┤  ├──▶  coverage
   push / PR ──▶ │  build-aem65     │  │
                 └──────────────────┘  └──▶  cypress-chrome
                                       │
                                       └──▶  release  (tag only)
```

* **build-java-11** — Builds the reactor on JDK 11 with the default `aemaacs`
  profile and runs all unit tests.
* **build-aem65** — Builds with the `aem65` (on-prem) profile on JDK 11 to
  validate API compatibility with AEM 6.5. JDK 8 is **not supported**; the
  `aem65` profile is a build-validation gate only and does not produce a
  separately released artifact.
* **coverage** — Builds the `core` bundle and uploads JaCoCo coverage to
  Codecov. Requires the `jacoco-maven-plugin` — see [Enabling code
  coverage](#enabling-code-coverage). Exits early without failing if the
  report is missing.
* **cypress-chrome** — Runs the Cypress E2E suite in `ui.tests/test-module`
  against a live AEM author instance. See [Cypress tests](#cypress-tests).
* **release** — Runs only when a tag matching `forms-modernizer-X.Y.Z` is
  pushed. Deploys to Maven Central (GPG-signed) and creates a GitHub release.

## Build parameters

The root `pom.xml` enforces three properties via `maven-enforcer-plugin`.
The pipeline supplies safe defaults so the build works out-of-the-box.
Override them per project by setting these CircleCI **project environment
variables**:

| Env var                  | Default                                                                           |
| ------------------------ | --------------------------------------------------------------------------------- |
| `APP_ID`                 | `forms-modernizer`                                                                |
| `FORM_TEMPLATE_PATH`     | `/conf/forms-modernizer/settings/wcm/templates/blank-form-template/structure`     |
| `FRAGMENT_TEMPLATE_PATH` | `/conf/forms-modernizer/settings/wcm/templates/blank-fragment-template/structure` |

> The defaults are **placeholders** that satisfy the enforcer. For a real
> end-to-end conversion, point them at templates that exist on your AEM
> instance.

## Java version matrix

| Job             | JDK | Maven profile | Notes                                  |
| --------------- | --- | ------------- | -------------------------------------- |
| `build-java-11` | 11  | `aemaacs`     |                                        |
| `build-aem65`   | 11  | `aem65`       | Build-validation only; no JDK 8 support |
| `coverage`      | 11  | `aemaacs`     |                                        |
| `cypress-chrome`| 11  | _(none)_      | Runs by default; skips cleanly if no AEM configured |
| `release`       | 11  | `aemaacs`     |                                        |

> **JDK 8 / AEM 6.5 release support**: The `aem65` profile validates that the
> code compiles against the AEM 6.5 API. It does **not** produce or publish a
> JDK 8-compatible artifact. JDK 8 is not a supported build target for this
> project.

The root POM enforcer requires Java ≥ 1.8 globally. An additional
`enforce-jdk11-for-aemaacs` execution inside the `aemaacs` profile raises the
minimum to Java 11 for that profile only.

## Cypress tests

The `cypress-chrome` job runs on every push/PR by default. It requires a
**live AEM author instance** — when `AEM_AUTHOR_URL` is not configured (still
pointing to localhost), the job detects this and exits cleanly so contributor
PRs are not blocked by E2E timeouts.

Set these CircleCI **project environment variables** to point at a real instance:

| Env var                | Default                  | Purpose                    |
| ---------------------- | ------------------------ | -------------------------- |
| `AEM_AUTHOR_URL`       | `http://localhost:4502`  | Author instance base URL   |
| `AEM_AUTHOR_USERNAME`  | `admin`                  | Author credentials         |
| `AEM_AUTHOR_PASSWORD`  | `admin`                  | Author credentials         |

### Test suites

| Suite                                  | What it tests                                             |
| -------------------------------------- | --------------------------------------------------------- |
| `bundle/bundle-status.cy.js`           | OSGi bundle is Active; rules/proxy-rules nodes are present |
| `modernizer/modernizer-ui.cy.js`       | Modernizer tool UI loads and wizard is accessible         |

### Running Cypress locally

```bash
# From ui.tests/test-module/
npm install
AEM_AUTHOR_URL=http://localhost:4502 npm run cypress:open   # interactive
AEM_AUTHOR_URL=http://localhost:4502 npm run cypress:run    # headless
```

Or via Maven from the repository root:

```bash
mvn -pl ui.tests -Pcypress-ci \
  -DAEM_AUTHOR_URL=http://localhost:4502 \
  verify
```

## Required environment variables for the `release` job

| Env var             | Purpose                                                            |
| ------------------- | ------------------------------------------------------------------ |
| `SONATYPE_USERNAME` | Sonatype Central Portal username (Maven Central deploy).           |
| `SONATYPE_PASSWORD` | Sonatype Central Portal password / token.                          |
| `GPG_PASSPHRASE`    | Passphrase for the GPG key used to sign release artifacts.         |
| `GPG_PRIVATE_KEY`   | Base64-encoded GPG private key (`gpg --export-secret-keys ... \| base64`). |
| `GITHUB_TOKEN`      | Token used to create the GitHub release and upload the `all` zip.  |

Optional (for the `coverage` job):

| Env var          | Purpose                                                           |
| ---------------- | ----------------------------------------------------------------- |
| `CODECOV_TOKEN`  | Codecov upload token (only needed for private repos / forks).     |

## Enabling code coverage

The `coverage` job uploads `core/target/site/jacoco/jacoco.xml` to Codecov.
Add the JaCoCo plugin to `core/pom.xml`:

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```

Until then the job exits early without failing the pipeline.

## Triggering a release

1. Bump the project version with `mvn release:prepare`.
2. Tag the commit `forms-modernizer-X.Y.Z` and push the tag.
3. CircleCI runs both build jobs; if they pass, `release` publishes to Maven
   Central and creates a GitHub release.

## Validating the config locally

```bash
circleci config validate .circleci/config.yml
```

## Status badge

```markdown
[![CircleCI](https://circleci.com/gh/adobe/forms-modernizer/tree/main.svg?style=svg)](https://circleci.com/gh/adobe/forms-modernizer/tree/main)
```
