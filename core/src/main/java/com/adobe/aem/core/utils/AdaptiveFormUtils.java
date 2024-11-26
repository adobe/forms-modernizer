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
package com.adobe.aem.core.utils;

import javax.jcr.*;

import java.util.HashMap;
import java.util.Map;

import static com.adobe.aem.core.utils.AdaptiveFormConstants.*;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

public class AdaptiveFormUtils {
    private static Map<String, String> LAYOUT_RESOURCE_TYPE_MAPPINGS = new HashMap<>();

    public static Node createNodeAndCopyProperties(Node source, Node destination, String name) throws RepositoryException {
        Node newNode = destination.addNode(name != null ? name : source.getName(), NT_UNSTRUCTURED);
        PropertyIterator propIter = source.getProperties();
        while(propIter.hasNext()) {
            Property prop = propIter.nextProperty();
            // skip protected properties
            if (prop.getDefinition().isProtected()) {
                continue;
            }
            prop = prop.isMultiple() ? newNode.setProperty(prop.getName(), prop.getValues()) :
                    newNode.setProperty(prop.getName(), prop.getValue());
        }
        return newNode;
    }

    public static boolean isRepeatablePanel(Node panelNode) throws RepositoryException {
        return (panelNode.hasProperty(MIN_OCCUR) || panelNode.hasProperty(MAX_OCCUR));
    }

    public static void deleteFormNodes(Node node, Session session) throws RepositoryException {
        if (checkIfNodeCanBeDeleted(node)) {
            Node parent = node.getParent();
            node.remove();
            if (parent.getName().equals(GUIDE_CONTAINER) || parent.getName().equals(GUIDE_CONTAINER_WRAPPER)) {
                String initialName = parent.getName();
                Node guideContainerParent = parent.getParent();
                parent.remove();
                //rename container to guideContainer
                Node newContainer = guideContainerParent.getNode(TEMP_CONTAINER_NODE);
                newContainer.setProperty(IS_MIGRATED, true);
                session.move(newContainer.getPath(), guideContainerParent.getPath() + "/" + initialName);
            } else {
                deleteFormNodes(parent, session);
            }
        }
    }

    public static Node handleLayoutNode(Node panelContainer, Node newContainer, boolean isRootPanel) throws RepositoryException {
        Node layoutNode = panelContainer.hasNode(LAYOUT_PROPERTY) ? panelContainer.getNode(LAYOUT_PROPERTY) : null;
        if (layoutNode != null) {
            String layoutType = layoutNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
            Node newPanel = newContainer;
            if (LAYOUT_RESOURCE_TYPE_MAPPINGS.get(layoutType) != null) {
                newPanel = createNodeAndCopyProperties(panelContainer, newContainer, panelContainer.getName());
                newPanel.setProperty(FIELD_TYPE, PANEL);
                newPanel.setProperty(HIDE_TITLE, true);
                newPanel.setProperty(SLING_RESOURCE_TYPE_PROPERTY, LAYOUT_RESOURCE_TYPE_MAPPINGS.get(layoutType));
                newPanel.getProperty(GUIDE_NODE_CLASS).remove();
            } else if (!isRootPanel) {
                newPanel = createNodeAndCopyProperties(panelContainer, newContainer, panelContainer.getName());
            }
            if (isRepeatablePanel(panelContainer)) {
                newPanel.setProperty(REPEATABLE, true);
            }
            newContainer = newPanel;
        }
        return newContainer;
    }

    public static void populateResourceTypeForLayout(String coreWizardResourceType, String coreTabsOnTopResourceType,
                                                     String coreVerticalTabsResourceType, String coreAccordionResourceType) {
        LAYOUT_RESOURCE_TYPE_MAPPINGS.put(WIZARD_RESOURCE_TYPE, coreWizardResourceType);
        LAYOUT_RESOURCE_TYPE_MAPPINGS.put(VERTICAL_TABS_RESOURCE_TYPE, coreTabsOnTopResourceType);
        LAYOUT_RESOURCE_TYPE_MAPPINGS.put(TABS_ON_TOP_RESOURCE_TYPE, coreVerticalTabsResourceType);
        LAYOUT_RESOURCE_TYPE_MAPPINGS.put(ACCORDION_RESOURCE_TYPE, coreAccordionResourceType);
    }

    public static String fetchBindRef(Node node) throws RepositoryException {
        return node.hasProperty(BIND_REF) ? node.getProperty(BIND_REF).getString() :
                node.hasProperty(DATA_REF) ? node.getProperty(DATA_REF).getString() : null;
    }

    public static void updateBindRefForRepeatableParent(Node newContainer,
                                                        String bindRef, String oldParentBindRef) throws RepositoryException {
        if (oldParentBindRef != null && bindRef.contains(oldParentBindRef)) {
            newContainer.setProperty(BIND_REF, "#." + bindRef.substring(oldParentBindRef.length() + 1));
        } else {
            newContainer.setProperty(BIND_REF, bindRef.replaceFirst("/", "\\$.").replace("/", "."));
        }
    }

    public static void updateBindRefForNonRepeatableParent(Node newContainer, String bindRef,
                                                           String oldParentBindRef, String newParentBindRef) throws RepositoryException {
        if (newParentBindRef != null && newParentBindRef.startsWith("#")) {
            newContainer.setProperty(BIND_REF, newParentBindRef + "." + bindRef.substring(oldParentBindRef.length() + 1));
        } else {
            newContainer.setProperty(BIND_REF, bindRef.replaceFirst("/", "\\$.").replace("/", "."));
        }
    }

    private static boolean checkIfNodeCanBeDeleted(Node node) throws RepositoryException {
        if (!node.hasNodes()) {
            return true;
        }
        NodeIterator children = node.getNodes();
        while(children.hasNext()) {
            Node child = children.nextNode();
            if (!PANEL_NODES_TO_IGNORE.contains(child.getName()) && child.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
                return false;
            }
            if (child.getName().equals(ITEMS) && !checkIfNodeCanBeDeleted(child)) {
                return false;
            }
        }
        return true;
    }
}
