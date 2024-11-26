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

import com.adobe.aem.core.utils.AdaptiveFormUtils;
import com.adobe.aem.modernize.component.ComponentRewriteRule;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.jackrabbit.value.DateValue;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.resource.LoginException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

import static com.adobe.aem.core.utils.AdaptiveFormConstants.*;
import static com.adobe.aem.core.utils.AdaptiveFormUtils.*;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(
        service = {ComponentRewriteRule.class},
        property = {
                "service.ranking=20"
        }
)
public class AdaptiveFormCommonGuideComponentsRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormCommonGuideComponentsRewriterRule.class);
    private static final List<String> COMPONENT_RESOURCE_TYPES = Arrays.asList(GUIDE_TEXT_BOX,
            GUIDE_NUMERIC_BOX, AF_FORM_TITLE, GUIDE_TEXT_DRAW, GUIDE_EMAIL, GUIDE_CHECK_BOX, GUIDE_DROPDOWN_LIST,
            GUIDE_RADIO_BUTTON, GUIDE_SWITCH, SUBMIT_ACTION, RESET_ACTION, GUIDE_BUTTON, GUIDE_DATE_PICKER,
            GUIDE_TELEPHONE, GUIDE_IMAGE, GUIDE_CAPTCHA, GUIDE_FILE_UPLOAD, GUIDE_HEADER, GUIDE_FOOTER,
            GUIDE_TERMS_AND_CONDITIONS, GUIDE_SEPARATOR, GUIDE_DATE_INPUT, GUIDE_NUMERIC_STEPPER, GUIDE_PASSWORD_BOX);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormCommonGuideComponentsRewriterRule.class.getName();
        this.init(context, COMPONENT_RESOURCE_TYPES, this.resourceResolverFactory);
    }

    protected Node processComponent(Node root, Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try (ResourceResolver rr = resourceResolverFactory.getResourceResolver(Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Node grandParent = root.getParent().getParent();
            if (grandParent.hasProperty(V2_COMPONENT_TEMP_PATH)) {
                String newContainerPath = grandParent.getProperty(V2_COMPONENT_TEMP_PATH).getValue().getString();
                Node container = session.getNode(newContainerPath);

                // modify some properties of the component for conversion
                modifyComponentPropertiesForConversion(root, rr);

                JcrUtil.copy(root, container, root.getName());

                // if itemsChild has cq:responsive node then add layout=responsive to parent panel
                if (root.hasNode(CQ_RESPONSIVE) && !container.hasProperty(LAYOUT_PROPERTY)) {
                    container.setProperty(LAYOUT_PROPERTY, RESPONSIVE_GRID);
                }

                String bindRef = fetchBindRef(root);
                if (bindRef != null) {
                    updateBindRef(root, container, bindRef, isRepeatablePanel(root.getParent().getParent()));
                }

                root.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                // call function to delete
                deleteFormNodes(root, session);
            }
        } catch (RepositoryException | LoginException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    private void updateBindRef(Node component, Node newContainer, String bindRef, boolean isParentRepeatable) throws RepositoryException {
        String oldParentBindRef = fetchBindRef(component.getParent().getParent());
        if (isParentRepeatable) {
            updateBindRefForRepeatableParent(newContainer.getNode(component.getName()), bindRef, oldParentBindRef);
        } else {
            updateBindRefForNonRepeatableParent(newContainer.getNode(component.getName()), bindRef,
                    oldParentBindRef, fetchBindRef(newContainer));
        }
    }

    private void modifyComponentPropertiesForConversion(Node node, ResourceResolver rr) throws RepositoryException {
        // change options to enum/enumNames for radio/dropdown/checkboxgroup/switch
        if (rr.getResource(node.getPath()).isResourceType(GUIDE_CHECK_BOX)
                || rr.getResource(node.getPath()).isResourceType(GUIDE_DROPDOWN_LIST)
                || rr.getResource(node.getPath()).isResourceType(GUIDE_SWITCH)
                || rr.getResource(node.getPath()).isResourceType(GUIDE_RADIO_BUTTON)) {
            // options is string[], should convert this to enum(string[]) and enumNames(string[])
            if (node.hasProperty(OPTIONS)) {
                Value[] options = node.getProperty(OPTIONS).getValues();
                String[] enums = new String[options.length];
                String[] enumNames = new String[options.length];
                for(int i = 0; i < options.length; i++) {
                    String[] optionsArray = options[i].getString().split("=");
                    enums[i] = optionsArray[0];
                    //If enumName is unspecified, The enumName is assumed same as enum.
                    enumNames[i] = (optionsArray.length == 1)? enums[i] : optionsArray[1];
                }
                node.setProperty(ENUM, enums);
                node.setProperty(ENUM_NAMES, enumNames);
                node.getProperty(OPTIONS).remove();
                node.getSession().save();
            }

            if (node.hasProperty(ALIGNMENT)) {
                Value alignment = node.getProperty(ALIGNMENT).getValue();
                if(OLD_HORIZONTAL_ALIGNMENT.equals(alignment.getString())) {
                    alignment = node.getSession().getValueFactory().createValue(NEW_HORIZONTAL_ALIGNMENT);
                } else if(OLD_VERTICAL_ALIGNMENT.equals(alignment.getString())) {
                    alignment = node.getSession().getValueFactory().createValue(NEW_VERTICAL_ALIGNMENT);
                }
                node.setProperty(ALIGNMENT, alignment);
                node.getSession().save();
            }
        }

        if (rr.getResource(node.getPath()).isResourceType(GUIDE_CHECK_BOX)) {
            if (node.hasProperty(_VALUE)) {
                Value options = node.getProperty(_VALUE).getValue();
                String[] defaultValues = options.getString().split(",");
                node.setProperty(DEFAULT, defaultValues);
                node.getProperty(_VALUE).remove();
                node.getSession().save();
            }
        }

        if (rr.getResource(node.getPath()).isResourceType(GUIDE_DATE_PICKER) || rr.getResource(node.getPath()).isResourceType(GUIDE_DATE_INPUT)) {
            updateDateProperty(node, MAXIMUM, MAXIMUM_DATE);
            updateDateProperty(node, MINIMUM, MINIMUM_DATE);
        }

        if (rr.getResource(node.getPath()).isResourceType(GUIDE_TERMS_AND_CONDITIONS)) {
            if(node.hasNode(ITEMS) && node.getNode(ITEMS).hasNode(REVIEW_STATUS)) {
                Node itemNode = node.getNode(ITEMS);
                Node reviewStatusNode = itemNode.getNode(REVIEW_STATUS);
                updateStringProperty(reviewStatusNode, node, MANDATORY, REQUIRED);
                updateStringProperty(reviewStatusNode, node, MANDATORY_MESSAGE, MANDATORY_MESSAGE);
                updateStringProperty(reviewStatusNode, node, VALIDATE_EXP_MESSAGE, VALIDATE_EXP_MESSAGE);
                itemNode.remove();
            }
        }

        if(node.hasProperty(IS_DISPLAY_SAME_AS_VALIDATE) && node.hasProperty(DISPLAY_PICTURE_CLAUSE)
                && Boolean.TRUE.toString().equals(node.getProperty(IS_DISPLAY_SAME_AS_VALIDATE).getValue().getString())) {
            node.setProperty(VALIDATE_PICTURE_CLAUSE, node.getProperty(DISPLAY_PICTURE_CLAUSE).getValue());
            node.setProperty(VALIDATION_PATTERN_TYPE, CUSTOM);
            node.getSession().save();
        }

        //handling for button rules and events node
        if (rr.getResource(node.getPath()).isResourceType(GUIDE_BUTTON) && node.hasProperty(TYPE)) {
            Session session = node.getSession();
            String type = node.getProperty(TYPE).getString();
            String buttonPath;

            if (ACTION_SUBMIT.equals(type)) {
                buttonPath = "/apps/" + CORE_SUBMIT_ACTION_RESOURCE_TYPE + "/cq:template/";
            } else if (type.equals(ACTION_RESET)) {
                buttonPath = "/apps/" + CORE_RESET_ACTION_RESOURCE_TYPE + "/cq:template/";
            } else {
                return; // If type is neither ACTION_SUBMIT nor ACTION_RESET, we exit the function early
            }

            Node rulesNode = session.getNode(buttonPath + FD_RULES);
            Node eventsNode = session.getNode(buttonPath + FD_EVENTS);

            if(node.hasNode(FD_RULES)) {
                node.getNode(FD_RULES).remove();
            }

            Node newRulesNode = AdaptiveFormUtils.createNodeAndCopyProperties(rulesNode, node, rulesNode.getName());
            convertMultiValuePropertyToStringArray(rulesNode.getProperty("fd:click"), newRulesNode);

            Node newEventsNode = AdaptiveFormUtils.createNodeAndCopyProperties(eventsNode, node, eventsNode.getName());
            convertMultiValuePropertyToStringArray(eventsNode.getProperty("click"), newEventsNode);

            session.save();
        }
    }

    private void convertMultiValuePropertyToStringArray(Property prop, Node node) throws RepositoryException {
        if(prop.isMultiple()){
            node.getProperty(prop.getName()).remove();
            String[] stringArray = new String[prop.getValues().length];
            for(int i = 0; i < prop.getValues().length; i++) {
                stringArray[i] = prop.getValues()[i].getString();
            }
            node.setProperty(prop.getName(), stringArray);
        }
    }

    private void updateDateProperty(Node node, String originalProperty, String newProperty) throws RepositoryException {
        if (node.hasProperty(originalProperty)) {
            Value value = node.getProperty(originalProperty).getValue();
            LocalDate localDate = LocalDate.parse(value.getString());
            Calendar calendarDate = GregorianCalendar.from(localDate.atStartOfDay(ZoneId.systemDefault()));
            DateValue dateValue = new DateValue(calendarDate);
            node.setProperty(newProperty, dateValue);
            node.getProperty(originalProperty).remove();
            node.getSession().save();
        }
    }

    private void updateStringProperty(Node sourceNode, Node destinationNode, String originalProperty, String newProperty) throws RepositoryException {
        if (sourceNode.hasProperty(originalProperty)) {
            Value value = sourceNode.getProperty(originalProperty).getValue();
            destinationNode.setProperty(newProperty, value);
            sourceNode.getProperty(originalProperty).remove();
            sourceNode.getSession().save();
            destinationNode.getSession().save();
        }
    }
}
