# forms-modernizer — CLAUDE.md

Everything a developer needs to work in this repo without re-deriving context from scratch.

> User-facing documentation (how to build, how to run, component mappings, fragment handling) lives in [README.md](README.md). This file covers implementation internals.

---

## What this project does

Converts AEM Adaptive Forms built on Foundation Components (AF1) to Core Components (AF2). It is a thin extension on top of [aem-modernize-tools](https://github.com/adobe/aem-modernize-tools), which owns the rewrite engine.

The conversion entry point is the Sling Jobs system. The job topic is `com/adobe/aem/modernize/job/topic/convert/form`, handled by `FormConversionJobExecutor` in aem-modernize-tools.

---

## Module layout

| Module | Purpose |
| --- | --- |
| `parent` | Shared Maven config (deps, plugin versions, profiles). All non-reactor modules have `<parent>` pointing here. |
| `core` | OSGi services — 6 `ComponentRewriteRule` implementations + utils |
| `ui.apps` | JCR rule definitions under `/apps/forms-modernizer/rules` and `/apps/forms-modernizer/proxy-rules` |
| `ui.config` | OSGi config: rule search path (`ComponentRewriteRuleServiceImpl`) and CORS |
| `ui.tests` | Cypress E2E tests |
| `all` | Aggregated content package for deployment |
| `pom.xml` (root) | **Reactor only** — no `<parent>`, no dependency management, just `<modules>` and `maven-release-plugin` |

**Critical**: the reactor POM must stay standalone (no `<parent>`). `mvn release:prepare` strips `<relativePath>`, which breaks if the reactor references a `parent` module that is also listed in `<modules>`.

---

## How Maven properties wire into the code

Three Maven properties are required at build time:

| Property | Where it lands |
| --- | --- |
| `appId` | `AdaptiveFormConstants.APP_ID` — controls component resource type prefix |
| `formTemplatePath` | `AdaptiveFormConstants.CORE_FORM_TEMPLATE_PATH` — used by `AdaptiveFormGuideContainerRewriterRule` |
| `fragmentTemplatePath` | `AdaptiveFormConstants.CORE_FRAGMENT_TEMPLATE_PATH` — used by `AdaptiveFormGuideContainerRewriterRule` |

Maven substitutes these literals at build time via resource filtering. `proxy-rules` XML files use `${appId}`, `${formTemplatePath}`, `${fragmentTemplatePath}` as placeholders and are copied to `rules/` with substitution applied.

If `appId` is absent at runtime (i.e. the placeholder was never substituted), `AdaptiveFormConstants` detects this via `APP_ID.contains("${")` and defaults `COMPONENT_PATH_PREFIX` to `forms-components-examples/components/form/`. This means builds without `appId` silently produce a working but non-customized package.

---

## Conversion pipeline (execution order)

1. **`FormConversionJobExecutor.doProcess()`** ([source](https://github.com/adobe/aem-modernize-tools/blob/main/core/src/main/java/com/adobe/aem/modernize/form/job/FormConversionJobExecutor.java)) — orchestrates the job loop. For each form path: optionally restores a prior version, creates a pre-conversion JCR version, optionally copies to target, then hands off to `ComponentRewriteRuleService.apply()`.

2. **`ComponentRewriteRuleService.apply()`** — collects `ComponentRewriteRule` OSGi services and calls `ComponentTreeRewriter.rewrite()`.

3. **`ComponentTreeRewriter.rewrite()`** ([source](https://github.com/adobe/aem-modernize-tools/blob/main/core/src/main/java/com/adobe/aem/modernize/component/impl/ComponentTreeRewriter.java)) — depth-first tree traversal. Applies each rule to each node; after any match, breaks out of the current traversal pass and restarts from the top. Continues until a full pass produces no matches.

4. **Service-based rules** (in `core/`) are applied in this order (by `service.ranking`):
   - `AdaptiveFormGuideContainerRewriterRule` (ranking=20) — rewrites `guideContainer`, `guideContainerWrapper`, `guideFragmentContainer` → core component equivalents; sets `cq:template`, `fd:version`, etc.
   - `AdaptiveFormGuideRootPanelRewriterRule` (ranking=20) — moves root panel content into the new container.
   - `AdaptiveFormGuidePanelRewriterRule` — rewrites nested panels; maps layout types (wizard, tabs, accordion).
   - `AdaptiveFormGuideDefaultRewriterRule` — rewrites leaf components (text box, email, checkbox, dropdown, etc.).
   - `AdaptiveFormGuideTableRewriterRule` — rewrites `table` components to panel with responsive grid.
   - `AdaptiveFormCommonGuideComponentsRewriterRule` — handles shared component properties.

5. **Node-based rules** (`/apps/forms-modernizer/rules/`) — declarative JCR XML rules applied by `NodeBasedRewriteRule` for components that don't need Java logic.

---

## Key classes

### In this repo (`forms-modernizer`)

| Class | Package | Role |
| --- | --- | --- |
| `AdaptiveFormConstants` | `com.adobe.aem.core.utils` | All string constants: resource types, property names, paths, template paths |
| `AdaptiveFormUtils` | `com.adobe.aem.core.utils` | Shared helpers: node copy, layout mapping, delete utilities |
| `AbstractAdaptiveFormComponentRewriterRule` | `com.adobe.aem.core.rules` | Base class for all 6 rewrite rules; implements `findMatches`, `matches`, `getId`, `getRanking` |
| `AdaptiveFormGuideContainerRewriterRule` | `com.adobe.aem.core.rules` | Most important rule — rewrites the form container, sets template, moves DAM asset metadata |
| `AdaptiveFormGuideRootPanelRewriterRule` | `com.adobe.aem.core.rules` | Moves root panel children into the new container |
| `AdaptiveFormGuidePanelRewriterRule` | `com.adobe.aem.core.rules` | Recursively rewrites panels |
| `AdaptiveFormGuideDefaultRewriterRule` | `com.adobe.aem.core.rules` | Leaf components (most fields) |
| `AdaptiveFormGuideTableRewriterRule` | `com.adobe.aem.core.rules` | Tables → panel with responsive grid |

### In aem-modernize-tools (upstream)

| Class | Role |
| --- | --- |
| `FormConversionJobExecutor` | Job entry point; handles page copy, versioning, DAM asset copy |
| `AbstractConversionJobExecutor` | Handles login, tracking, bucket updates |
| `ComponentTreeRewriter` | The rewrite loop — see Performance section |
| `NodeBasedRewriteRule` | Applies declarative JCR XML rules |

---

## OSGi configuration

`ui.config/src/main/content/jcr_root/apps/forms-modernizer/osgiconfig/config.author/`

- `com.adobe.aem.modernize.component.impl.ComponentRewriteRuleServiceImpl.cfg.json` — sets `search.paths` to `/apps/forms-modernizer/rules`. This is where the engine looks for node-based rules.
- `com.granite.cors.impl.CORSPolicyImpl~forms-modernizer.cfg.json` — CORS for author.

**Important**: the `ui.apps` filter root `/apps/forms-modernizer` uses `mode="merge"` (see `ui.apps/src/main/content/META-INF/vault/filter.xml`). This prevents ui.apps from wiping the `osgiconfig` subtree when installed after ui.config. If this is ever changed back to default `replace` mode, OSGi configs will be deleted on the next ui.apps install and node-based rules will silently stop working.

---

## JCR rules structure

`ui.apps/src/main/content/jcr_root/apps/forms-modernizer/`

- `rules/` — rules with final values (used by default when no `appId` is given; targets `forms-components-examples`).
- `proxy-rules/` — rules with Maven placeholders (`${appId}`, `${formTemplatePath}`, `${fragmentTemplatePath}`). At build time these are substituted and output to `rules/`.

---

## CI

`.circleci/config.yml` has a boolean pipeline parameter `skip_ui_tests` (default `false`). The `ui-tests` workflow runs on every push unless `skip_ui_tests=true` is passed.

The `cypress-chrome` job has a pre-flight check: if `AEM_AUTHOR_URL` is the default `http://localhost:4502`, it calls `circleci-agent step halt` to exit cleanly rather than timing out.

The `build-aem65` job uses JDK 11 (same executor as the main build) for API compatibility validation against AEM 6.5. This is a validation gate only — JDK 8 is not supported and AEM 6.5 is not a release target.

Cypress test results land at `target/cypress-results/results-[hash].xml` via `cypress-multi-reporters` + `mocha-junit-reporter`.

---

## Dependency on aem-modernize-tools

This project depends on `com.adobe.aem:aem-modernize-tools.core` (and adjacent artifacts) from [github.com/adobe/aem-modernize-tools](https://github.com/adobe/aem-modernize-tools). Any engine or performance fixes go there; any forms-specific rule or mapping changes go here.

To test a local change to aem-modernize-tools alongside forms-modernizer, build aem-modernize-tools with `mvn clean install` first, then build forms-modernizer.
