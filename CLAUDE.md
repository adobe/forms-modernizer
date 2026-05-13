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

## Performance

### The problem (fixed in aem-modernize-tools)

`ComponentTreeRewriter.rewrite()` had an O(n²) JCR write pattern:
- The loop called `node.getParent().orderBefore(node.getName(), null)` for **every node** in **every traversal pass**, even after `matched=true`.
- For a 1000-field form with ~3000 nodes needing ~3000 passes: **~9 million JCR `orderBefore` writes**.
- This caused 2–3 hour migrations and apparent hangs with no output.

### The fix

In `ComponentTreeRewriter.rewrite()`, the `if (matched) { break; }` check was moved to run **before** the `orderBefore` call, and `continue` was changed to `break`. Nodes not visited in the current pass will have `orderBefore` called on the next pass when they are actually visited — ordering correctness is preserved.

Result: O(n) total node visits instead of O(n²) JCR writes. Expected 100–1000x speedup for large forms.

See: [`ComponentTreeRewriter.java`](https://github.com/adobe/aem-modernize-tools/blob/main/core/src/main/java/com/adobe/aem/modernize/component/impl/ComponentTreeRewriter.java)

### Progress visibility

`FormConversionJobExecutor` logs via `context.log()` at each step (per-form progress counter, elapsed time, error messages). These appear in the AEM Sling Jobs console on the conversion status page.

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

## Bug fixes applied

### Node-based rules missing from conversion jobs

**Symptom**: After a conversion, leaf components (text fields, plain-layout panels) remained as AF1 resource types. Only components handled by service-based rules (e.g. `verticalTabbedPanelLayout` panels) were converted correctly.

**Root cause**: The `ui.apps` filter used default `replace` mode for the `/apps/forms-modernizer` root. Since ui.apps is installed after ui.config (both are embedded in the `all` package), the replace-mode install deleted `/apps/forms-modernizer/osgiconfig`. Without a deployed OSGi config, `ComponentRewriteRuleServiceImpl.getSearchPaths()` returned an empty array and the QueryBuilder in `searchRules()` never ran — so no node-based rules were discovered or included in job `componentRules`.

**Fix**: `ui.apps/src/main/content/META-INF/vault/filter.xml` — changed to `mode="merge"`:
```xml
<filter root="/apps/forms-modernizer" mode="merge"/>
```

---

### Plain panels getting wrong resource type

**Symptom**: Panels whose layout was not one of the four mapped types (wizard, tabs-on-top, vertical-tabs, accordion) were copied to the new container with `fd/af/components/panel` still as their resource type.

**Root cause**: In `AdaptiveFormUtils.handleLayoutNode()`, the `else if (!isRootPanel)` branch called `createNodeAndCopyProperties` but never overwrote `sling:resourceType`, so the old AF1 type was carried over.

**Fix**: `core/src/main/java/com/adobe/aem/core/utils/AdaptiveFormUtils.java`:
```java
} else if (!isRootPanel) {
    newPanel = createNodeAndCopyProperties(panelContainer, newContainer, panelContainer.getName());
    newPanel.setProperty(SLING_RESOURCE_TYPE_PROPERTY, CORE_PANEL_RESOURCE_TYPE);
    newPanel.setProperty(FIELD_TYPE, PANEL);
}
```

---

### Fragment containers not converting (11 fragments)

**Symptom**: All AF1 fragments remained unconverted after a job that successfully converted the parent form. CRX showed fragment nodes at old paths with AF2 resource types set in-place by node-based rules.

**Root cause 1 (primary)**: `AdaptiveFormGuideContainerRewriterRule.processComponent()` called `container.getProperty(GUIDE_NODE_CLASS).remove()` and `container.getProperty(GUIDE_CSS).remove()` unconditionally. Fragment containers often lack these optional UI properties, so a `PathNotFoundException` was thrown. The `catch (RepositoryException | LoginException e)` block swallowed it silently. `processComponent` returned without ever setting `v2Path`, causing `RootPanelRule` and `CommonGuideComponentsRewriterRule` to skip processing entirely. Node-based rules then fired in-place at the old location.

**Root cause 2 (secondary)**: `AdaptiveFormUtils.deleteFormNodes()` special-cased cleanup only for `"guideContainer"` and `"guideContainerWrapper"`. Fragment containers are named `"guideFragmentContainer"` — not in the list — so the rename step (remove old container, rename temp `container` node) never fired for fragments even when the pipeline otherwise worked.

**Fix**: `core/src/main/java/com/adobe/aem/core/rules/AdaptiveFormGuideContainerRewriterRule.java`:
```java
if (container.hasProperty(GUIDE_NODE_CLASS)) container.getProperty(GUIDE_NODE_CLASS).remove();
if (container.hasProperty(GUIDE_CSS)) container.getProperty(GUIDE_CSS).remove();
```

`core/src/main/java/com/adobe/aem/core/utils/AdaptiveFormUtils.java` — `deleteFormNodes()`:
```java
if (parent.getName().equals(GUIDE_CONTAINER) || parent.getName().equals(GUIDE_CONTAINER_WRAPPER) || parent.getName().equals(GUIDE_FRAGMENT_CONTAINER)) {
```

Additional defensive `hasProperty()` guards applied in `handleLayoutNode`, `AdaptiveFormGuideTableRewriterRule`, and `AdaptiveFormGuidePanelRewriterRule.handleFragmentPanel()` — all used the same unsafe `.getProperty(X).remove()` pattern.

---

## Dependency on aem-modernize-tools

This project depends on `com.adobe.aem:aem-modernize-tools.core` (and adjacent artifacts) from [github.com/adobe/aem-modernize-tools](https://github.com/adobe/aem-modernize-tools). Any engine or performance fixes go there; any forms-specific rule or mapping changes go here.

To test a local change to aem-modernize-tools alongside forms-modernizer, build aem-modernize-tools with `mvn clean install` first, then build forms-modernizer.
