# AEM Forms Modernization Tool

This tool helps AEM development teams convert legacy AEM Forms (AF1 — foundation components) to AEM Forms with Core Components (AF2). It extends the [AEM Modernize Tools suite](https://github.com/adobe/aem-modernize-tools) and is designed specifically for AEM Forms developers upgrading their forms.

## Goal

Provide a flexible, customizable framework for migrating AF1 forms to AF2 with minimal manual effort. Users can run conversions with only the rewrite rules relevant to their project.

## Modules

| Module | Purpose |
| --- | --- |
| `core` | OSGi services, component rewrite rules, listeners |
| `ui.apps` | JCR rule definitions (`/apps/forms-modernizer/rules`, `/apps/forms-modernizer/proxy-rules`) |
| `ui.config` | Author-mode OSGi configuration (rule search path, CORS) |
| `ui.tests` | Cypress E2E tests |
| `all` | Aggregated content package for deployment |

---

## Prerequisites

Before building or running this tool, ensure the following are in place:

### 1. AEM Instance with Core Components

Your AEM instance must have an AEM project with **Core Components** installed. If you do not already have a project set up, generate one using the AEM Project Archetype:

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.3.1:generate \
  -D archetypeGroupId=com.adobe.aem \
  -D archetypeArtifactId=aem-project-archetype \
  -D archetypeVersion=56 \
  -D appTitle="Dev Forms" \
  -D appId="devform" \
  -D groupId="com.devform" \
  -D includeFormsenrollment=y \
  -D aemVersion="cloud"
```

> Use the **latest available `archetypeVersion`**. To find it, check [aem-project-archetype releases](https://github.com/adobe/aem-project-archetype/releases). The `includeFormsenrollment=y` flag is required — it generates the Forms-specific template and component scaffolding.

Build and deploy the generated project to your AEM author instance before installing forms-modernizer.

### 2. Form Template and Fragment Template

The tool requires two editable templates on your AEM instance:
- A **form template** (used as the base for converted AF2 forms)
- A **fragment template** (used as the base for converted AF2 form fragments)

**If you ran the archetype with `includeFormsenrollment=y`**, these templates are already created for you.

Their JCR paths follow the pattern:
```
/conf/<appId>/settings/wcm/templates/<template-name>
```

Example paths for an `appId` of `mysite`:
```
/conf/mysite/settings/wcm/templates/blank-af-v2
/conf/mysite/settings/wcm/templates/blank-af-fragment-template
```

**If templates do not exist**, create them via `Tools > General > Templates > Create`. You need one of type *Adaptive Form (Core Components)* and one of type *Adaptive Form Fragment (Core Components)*.


---

## How to Build

The build requires three Maven properties. These map to the OSGi rule configuration that tells the tool which templates to use when creating AF2 forms:

| Property | Description                                             |
| --- |---------------------------------------------------------|
| `appId` | Your project's app ID (matches the archetype `appId`).  |
| `formTemplatePath` | Full JCR path of of your AF2 form template.             |
| `fragmentTemplatePath` | Full JCR path of your AF2 fragment template.            |

**Build for AEMaaCS (Java 11):**
```bash
mvn clean install \
  -DappId=mysite \
  -DformTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-template \
  -DfragmentTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-fragment-template
```

**Build for AEM 6.5 (Java 11, validation only):**
```bash
mvn clean install -Paem65 \
  -DappId=mysite \
  -DformTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-template \
  -DfragmentTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-fragment-template
```

**Deploy to local AEM author (port 4502):**
```bash
mvn clean install -PautoInstallSinglePackage \
  -DappId=mysite \
  -DformTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-template \
  -DfragmentTemplatePath=/conf/mysite/settings/wcm/templates/blank-af-fragment-template
```

---

## How to Run the Tool

1. Build and deploy the `all` package as described above.
2. Go to **Tools → AEM Modernize Tools → Forms Conversion**.
3. Click **Create**. Provide a job name. Choose a form handling type:
   - **None** — In-place conversion.
   - **Restore** — Restore the previous version and re-apply rules (useful after rule updates).
   - **Copy to Target** — Copy the form to a target location, then convert the copy. Provide:
     - *Source Path* — Folder containing the AF1 form(s).
     - *Target Path* — Destination folder for the converted AF2 form(s).
4. Select the AF1 form(s) you want to convert, click **Schedule Job**, then **Convert**.
5. The conversion status page opens. Refresh to see the latest status.
6. Once conversion shows **Complete**, open the converted form's properties, save, and close. This adds any properties the form editor requires on first open.

---

## Converting Forms with Fragments (Nested Forms)

> Fragments must be converted **before** the forms that reference them. The tool does not automatically follow `fragmentRef` links.

Follow this order:

### Step 1 — Convert all fragments first

Run the tool against your fragment forms only (forms stored under the fragments path). Wait for all fragment conversions to show **Complete** before proceeding.

### Step 2 — Convert the parent forms

Run the tool against your parent forms. Fragment placeholder panels in the converted form will reference the **old AF1 fragment path** — this is expected and corrected in the next step.

### Step 3 — Update fragment references

After conversion, open each converted form in the AF2 editor. For each Fragment component, open its properties and update the **Fragment Reference** (`fragmentRef`) to point to the converted AF2 fragment path.

The converted fragment path follows the same structure as the original, under your target root:
```
Original AF1 fragment:  /content/forms/af/mysite/my-fragment
Converted AF2 fragment: /content/forms/af/mysite-converted/my-fragment   (if using Copy to Target)
```

---

## Component Mappings

### Components converted to a core components equivalent

| AF1 Component | AF2 Component |
| --- | --- |
| Numeric Stepper | Number Input |
| Date Input | Date Picker |
| Password Box | Text Input |
| Table | Panel (with responsive grid) |

### Components removed during conversion (not available in core components)

The following components are deleted from the converted form. Custom components with no rules defined are also deleted.

- Adobe Sign Block
- Chart
- File Attachment Listing
- Footnote Placeholder
- Image Choice
- Next Button / Previous Button
- Scribble Signature
- Summary Step
- Toolbar

Deleted components are logged with their path so you can track what was removed.

### What is NOT converted

- **Scripts and custom functions** — must be rewritten manually in the code editor. Alternatively, use the [Content Transfer Tool](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/forms/setup-configure-migrate/migrate-to-forms-as-a-cloud-service#prerequisites).
- **Form themes** — must be rewritten using BEM notation.
- **Visual rules** — support is in progress.

---

## How It Works

The tool applies two layers of rules in sequence:

1. **Service-based rules** (`ComponentRewriteRule` OSGi services) — handle container-level structural rewrites: `guideContainer`, `guideContainerWrapper`, `guideFragmentContainer`, root panel, panels, and common components.

2. **Node-based rules** (`/apps/forms-modernizer/rules` or `/apps/forms-modernizer/proxy-rules`) — declarative JCR XML rules applied to every matching component node.

When `appId` is provided at build time, the `proxy-rules` are copied into the `rules` folder with `appId`, `formTemplatePath`, and `fragmentTemplatePath` substituted in. If `appId` is absent, the default `rules` folder (targeting `forms-components-examples`) is used.

The `ComponentRewriteRuleService` OSGi configuration (`com.adobe.aem.modernize.component.impl.ComponentRewriteRuleServiceImpl`) points to `/apps/forms-modernizer/rules` as the search path.

---

## Points to Note

1. **Large forms**: conversion time scales with the number of components and nesting depth. A form with hundreds of fields and fragments may take several minutes.
2. **Do not open a form while its conversion job is running.** The form page is created early; wait until status shows Complete or Failed.
3. **Fragment order matters.** See [Converting Forms with Fragments](#converting-forms-with-fragments-nested-forms).
4. **Templates must exist** before building. The build will fail if `formTemplatePath` or `fragmentTemplatePath` do not exist on the target instance or if the properties are not provided.

---

## Testing

### Unit tests
```bash
mvn clean test
```

### UI tests (Cypress, requires local AEM)
```bash
mvn clean verify -Pui-tests-local-execution
```

---

## Pending Tasks

1. Some components are still in progress and not yet supported.
2. Visual rules support is in progress.
3. Form theme migration requires manual BEM rewrite.

---

## Maven Settings

The project uses the Adobe public Maven repository. Configure it in your Maven settings:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

---

## Contributing

See [Contributing Guide](.github/CONTRIBUTING.md).

## License

Apache V2 — see [LICENSE](LICENSE).
