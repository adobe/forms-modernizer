/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.aem.core.rules;

import com.adobe.aem.modernize.component.ComponentRewriteRule;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.*;

import static com.adobe.aem.core.utils.AdaptiveFormConstants.*;
import static com.adobe.aem.core.utils.AdaptiveFormUtils.deleteFormNodes;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(service = {ComponentRewriteRule.class}, property = {"service.ranking=20"})
public class AdaptiveFormGuideDefaultRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormGuideDefaultRewriterRule.class);
    private static final Map<String, String> KNOWN_COMPONENT_RESOURCE_TYPE_MAP;
    private static final Map<String, String> OTHER_KNOWN_RESOURCE_TYPE_MAP;
    static {
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP = new HashMap<>();
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_CONTAINER_RESOURCE_TYPE, "guide-container");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_CONTAINER_WRAPPER_RESOURCE_TYPE, "guide-container-wrapper");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_FRAGMENT_CONTAINER_RESOURCE_TYPE, "guide-fragment-container");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(ROOT_PANEL, "root-panel");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_PANEL, "guide-panel");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_TABLE, "guide-table");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_TEXT_BOX, "guide-text-box");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_NUMERIC_BOX, "guide-numeric-box");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(AF_FORM_TITLE, "af-form-title");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_TEXT_DRAW, "guide-text-draw");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_EMAIL, "guide-email");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_CHECK_BOX, "guide-check-box");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_DROPDOWN_LIST, "guide-dropdown-list");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_RADIO_BUTTON, "guide-radio-button");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_SWITCH, "guide-switch");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(SUBMIT_ACTION, "submit-action");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(RESET_ACTION, "reset-action");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_BUTTON, "guide-button");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_DATE_PICKER, "guide-date-picker");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_TELEPHONE, "guide-telephone");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_IMAGE, "guide-image");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_CAPTCHA, "guide-captcha");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_FILE_UPLOAD, "guide-file-upload");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_HEADER, "guide-header");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_FOOTER, "guide-footer");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_TERMS_AND_CONDITIONS, "guide-terms-and-conditions");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_SEPARATOR, "guide-separator");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_DATE_INPUT, "guide-date-input");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_NUMERIC_STEPPER, "guide-numeric-stepper");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put(GUIDE_PASSWORD_BOX, "guide-password-box");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put("fd/af/components/tableHeader", "guide-table-header");
        KNOWN_COMPONENT_RESOURCE_TYPE_MAP.put("fd/af/components/tableRow", "guide-table-row");
    }
    static {
        OTHER_KNOWN_RESOURCE_TYPE_MAP = new HashMap<>();
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put(WIZARD_RESOURCE_TYPE,"guide-wizard");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put(VERTICAL_TABS_RESOURCE_TYPE, "guide-vertical-tabs");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put(TABS_ON_TOP_RESOURCE_TYPE, "guide-tabs-on-top");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put(ACCORDION_RESOURCE_TYPE, "guide-accordion");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/components/page2/aftemplatedpage", "guide-container-form-page");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/components/page/basePageForFragment", "guide-container-fragment-page");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/defaultGuideLayout", "guide-layout-type-1");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/gridFluidLayout2", "guide-layout-type-2");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/gridFluidLayout", "guide-layout-type-3");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/tableLayout", "guide-table-layout");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/table/headerLayout", "guide-table-header-layout");
        OTHER_KNOWN_RESOURCE_TYPE_MAP.put("fd/af/layouts/table/rowLayout", "guide-table-row-layout");
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormGuideDefaultRewriterRule.class.getName();
        this.init(context, new ArrayList<>(), this.resourceResolverFactory);
    }

    @Override
    public boolean matches(@NotNull Node node) throws RepositoryException {
        if (!node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            return false;
        }
        return !isKnownComponent(node);
    }

    private boolean isKnownComponent(@NotNull Node node) throws RepositoryException {
        Session session = node.getSession();
        try (ResourceResolver rr = this.resourceResolverFactory.getResourceResolver(Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Resource formResource = rr.getResource(node.getPath());
            return KNOWN_COMPONENT_RESOURCE_TYPE_MAP.containsKey(formResource.getResourceType()) || OTHER_KNOWN_RESOURCE_TYPE_MAP.containsKey(formResource.getResourceType());
        } catch (Exception e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
            return false;
        }
    }

    @Override
    protected @Nullable Node processComponent(@NotNull Node root, @NotNull Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try {
            Node grandParent = root.getParent().getParent();
            if (grandParent.hasProperty(V2_COMPONENT_TEMP_PATH)) {
                logger.info("!----------------------------------------------------------------!");
                logger.info("Cannot find rule for component: {} with path: {}, it will be deleted", root.getProperty(SLING_RESOURCE_TYPE_PROPERTY), root.getPath());
                logger.info("!----------------------------------------------------------------!");
                root.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                deleteFormNodes(root, session);
            }
        } catch (RepositoryException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    @Override
    public boolean hasPattern(@NotNull String... slingResourceTypes) {
        if (slingResourceTypes == null) {
            return false;
        }
        return Arrays.stream(slingResourceTypes)
                .noneMatch(type -> KNOWN_COMPONENT_RESOURCE_TYPE_MAP.containsKey(type) || OTHER_KNOWN_RESOURCE_TYPE_MAP.containsKey(type));
    }
}
