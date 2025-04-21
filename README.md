# AEM Forms Modernization Tool 

This tool helps AEM development teams convert legacy AEM Forms to the latest version. 
It extends the [AEM Modernize Tools suite](https://github.com/adobe/aem-modernize-tools) and is designed specifically for AEM Forms developers to upgrade their forms to core components based AEM Forms. 

Features include: 
* Forms Conversion Tool (Legacy -> Modern/Core) 

## Goal 

The goal of this project is to provide a framework for converting legacy AEM Forms to the current capabilities. 
The tool is designed to help AEM Forms developers upgrade their foundation based AEM Forms to core components based AEM Forms. 
It is flexible and customizable to fit the needs of different projects. Users should be able to run conversions with minimal necessary rewrite rules for their forms.

## Modules

The main parts of the template are:

* core: Java bundle containing all core functionality like OSGi services, listeners or schedulers, as well as component-related Java code such as servlets or request filters.
* ui.apps: contains the /apps (and /etc) parts of the project, ie JS&CSS clientlibs, components, and templates
* ui.config: contains runmode specific OSGi configs for the project
* ui.tests: Selenium based UI tests
* all: a single content package that embeds all the compiled modules (bundles and content packages) including any vendor dependencies

## How to build

To build all the modules, run the following command with Maven 3 and Java 11+ in the project root directory, specifying the project appId and template paths:

    mvn clean install -DappId=${appId} -DformTemplatePath=${formTemplatePath} -DfragmentTemplatePath=${fragmentTemplatePath}

To build for AEM 6.5, use the `aem65` profile:

    mvn clean install -Paem65 -DappId=${appId} -DformTemplatePath=${formTemplatePath} -DfragmentTemplatePath=${fragmentTemplatePath}

To build all the modules and deploy the `all` package to a local instance of AEM, run in the project root directory the following command:

    mvn clean install -PautoInstallSinglePackage

Or alternatively

    mvn clean install -PautoInstallSinglePackage -Daem.port=4503

Or to deploy only the bundle to the author, run

    mvn clean install -PautoInstallBundle

Or to deploy only a single content package, run in the sub-module directory (i.e `ui.apps`)

    mvn clean install -PautoInstallPackage

## Pre-requisites
1. Your instance should have core components installed via archetype.
2. The tool requires the user to create a core component form template and fragment template, and provide their paths using the build parameter. Failure to do so will result in build errors.
3. Tool is currently compatible with AEM as a cloud service. You first need to migrate your AEM Forms to AEM as a cloud service. For more information, see [Migrate to AEM Forms as a Cloud Service](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/forms/setup-configure-migrate/migrate-to-forms-as-a-cloud-service#prerequisites)


## How to run the tool

1. Build the project and deploy the `all` package to a local instance of AEM as described above.
2. Goto Tools-> AEM Modernize Tools -> Forms Conversion.
3. Click `Create`. Provide a Job name. Choose a form handling type from the provided types.
   - *None* - Select this for an inplace forms conversion.
   - *Restore* - Select this to restore to previous version and re-apply rules(modified or new).
   - *Copy to Target* - Select this to convert the form by copying the form to a target location.
     - *Source Path* - Provide the source folder path containing the form.
     - *Target Path* - Provide the target folder path where the form will be copied and converted.
4. Choose foundation based AF form(s) you want to convert and click on `Schedule Job`, followed by `Convert` button.
5. The tool will navigate you to a page that displays the conversion status. To see the most recent status of the selected form(s), you need to refresh this page.
6. Access the target folder to view the converted form.
7. Before accessing the core component converted form, first open its properties, save them, and then close. This process will add any necessary additional properties to the form.

### Points to note

1. The conversion time for more intricate forms may be longer due to the number of components and their nested structure. 
2. The conversion process for the form(s) is carried out in the background by the tool. Although the creation of the form might be visible, it's recommended not to access it until the status indicates that the conversion has either completed or failed.
3. The tool expects the user to create a core component form and fragment template, providing the path via the build parameter. If not specified, the tool will throw an error.
4. If your forms contain fragments, you must convert them first before converting the forms. The tool does not automatically convert the fragments mentioned in the form. After converting the form, you need to open it and update the fragment path to the newly converted fragment path.
5. Currently, the tool maps the following OOTB components to these core components, as they are not available in core components:
    - `af1:Numeric Stepper` -> `af2:Number Input`
    - `af1:Date Input` -> `af2:Date Picker`
    - `af1:Password Box` -> `af2:Text Input`
    - `af1:Table` -> `af2:Panel`

6. The following OOTB components are not yet supported in core components, so the tool will delete them in the newly created forms. The same applies to custom components if no rules are defined for them:
    - `Adobe Sign Block`
    - `Chart`
    - `File Attachment Listing`
    - `Footnote Placeholder`
    - `Image Choice`
    - `Next Button`
    - `Previous Button`
    - `Scribble Signature`
    - `Summary Step`
    - `Toolbar`

7. Deleted components are logged with relevant information. The tool will only delete nodes for which rules are not defined.
8. Tool only converts structure of the form and its components. It does not convert scripts, custom functions, or form themes. These need to be rewritten by the user.

### How it works under the hood

Based on the provided configurations for both node-based and service-based rules, the tool determines which components are needed to be converted.
When a user selects a page, the tool identifies all applicable rules and components that will undergo conversion.
When user confirms to execute conversion, the tool using Service Based rules (Container Rewriter Rule) for specific components,
implements the necessary structural modifications required to transition the component.
Next, all the node-based rules (Component Rewrite Rule Service) are executed for every component present in the form.

For the node-based conversion rules, there are two files:
- `proxy-rules` - This file holds the rules for the proxy components.
- `rules` - This file holds the rules for the default components.
During the project build process, the `ui.apps` module will utilize either of these files, depending on the parameter specified in the build command.

### Pending tasks

The current state of the tool is a work in progress. The following tasks are pending:

1. Some components are in progress and are not yet supported by the tool.
2. Visual rules will be supported by the tool, but this is currently in progress.
3. It is anticipated that the user will rewrite scripts in the code editor and custom functions. Alternatively, these can be migrated using the content transfer tool, as mentioned in point 4 of this [guide](https://experienceleague.adobe.com/en/docs/experience-manager-cloud-service/content/forms/setup-configure-migrate/migrate-to-forms-as-a-cloud-service#prerequisites).
4. Form theme is needed to be rewritten by the user using BEM notation.

## Testing

There are three levels of testing contained in the project:

### Unit tests

This show-cases classic unit testing of the code contained in the bundle. To
test, execute:

    mvn clean test

### Integration tests

This allows running integration tests that exercise the capabilities of AEM via
HTTP calls to its API. To run the integration tests, run:

    mvn clean verify -Plocal

Test classes must be saved in the `src/main/java` directory (or any of its
subdirectories), and must be contained in files matching the pattern `*IT.java`.

The configuration provides sensible defaults for a typical local installation of
AEM. If you want to point the integration tests to different AEM author and
publish instances, you can use the following system properties via Maven's `-D`
flag.

| Property | Description | Default value |
| --- | --- | --- |
| `it.author.url` | URL of the author instance | `http://localhost:4502` |
| `it.author.user` | Admin user for the author instance | `admin` |
| `it.author.password` | Password of the admin user for the author instance | `admin` |
| `it.publish.url` | URL of the publish instance | `http://localhost:4503` |
| `it.publish.user` | Admin user for the publish instance | `admin` |
| `it.publish.password` | Password of the admin user for the publish instance | `admin` |

The integration tests in this archetype use the [AEM Testing
Clients](https://github.com/adobe/aem-testing-clients) and showcase some
recommended [best
practices](https://github.com/adobe/aem-testing-clients/wiki/Best-practices) to
be put in use when writing integration tests for AEM.

## Static Analysis

The `analyse` module performs static analysis on the project for deploying into AEMaaCS. It is automatically
run when executing

    mvn clean install

from the project root directory. Additional information about this analysis and how to further configure it
can be found here https://github.com/adobe/aemanalyser-maven-plugin

### UI tests

They will test the UI layer of your AEM application using Selenium technology. 

To run them locally:

    mvn clean verify -Pui-tests-local-execution

This default command requires:
* an AEM author instance available at http://localhost:4502 (with the whole project built and deployed on it, see `How to build` section above)
* Chrome browser installed at default location

Check README file in `ui.tests` module for more details.

## Maven settings

The project comes with the auto-public repository configured. To setup the repository in your Maven settings, refer to:

    http://helpx.adobe.com/experience-manager/kb/SetUpTheAdobeMavenRepository.html

## Contributing

Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.

---
