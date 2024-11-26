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
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
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
public class AdaptiveFormGuidePanelRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {
    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormGuidePanelRewriterRule.class);
    private static final List<String> COMPONENT_RESOURCE_TYPES = Arrays.asList(GUIDE_PANEL);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormGuidePanelRewriterRule.class.getName();
        this.init(context, COMPONENT_RESOURCE_TYPES, this.resourceResolverFactory);

        populateResourceTypeForLayout(CORE_WIZARD_RESOURCE_TYPE, CORE_TABS_ON_TOP_RESOURCE_TYPE,
                CORE_VERTICAL_TABS_RESOURCE_TYPE, CORE_ACCORDION_RESOURCE_TYPE);
    }

    protected Node processComponent(Node root, Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try (ResourceResolver rr = resourceResolverFactory.getResourceResolver(Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Node grandParent = root.getParent().getParent();
            if (grandParent.hasProperty(V2_COMPONENT_TEMP_PATH)) {
                String newContainerPath = grandParent.getProperty(V2_COMPONENT_TEMP_PATH).getValue().getString();
                Node container = session.getNode(newContainerPath);

                root.setProperty(V2_COMPONENT_TEMP_PATH,
                        processPanelNode(root, container).getPath());
                root.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                // call function to delete
                deleteFormNodes(root, session);
            }
        } catch (RepositoryException | LoginException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    public Node processPanelNode(Node panelContainer, Node newContainer) throws RepositoryException {
        newContainer = handleLayoutNode(panelContainer, newContainer, false);

        NodeIterator children = panelContainer.getNodes();
        while (children.hasNext()) {
            Node node = children.nextNode();
            if (!PANEL_NODES_TO_IGNORE.contains(node.getName())) {
                JcrUtil.copy(node, newContainer, node.getName());
            }
        }

        updateHideTitle(panelContainer, newContainer);
        handleFragmentPanel(newContainer);

        String bindRef = fetchBindRef(panelContainer);
        if (bindRef != null) {
            updateBindRefForPanel(panelContainer, newContainer, bindRef,
                    isRepeatablePanel(panelContainer.getParent().getParent()));

            String containerResourceType = newContainer.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getString();
            //set bindRef to dataRef if resourceType is of below layout
            if (containerResourceType.equals(CORE_ACCORDION_RESOURCE_TYPE) ||
                    containerResourceType.equals(CORE_TABS_ON_TOP_RESOURCE_TYPE) ||
                    containerResourceType.equals(CORE_VERTICAL_TABS_RESOURCE_TYPE) ||
                    containerResourceType.equals(CORE_WIZARD_RESOURCE_TYPE)) {
                newContainer.setProperty(DATA_REF, newContainer.getProperty(BIND_REF).getString());
                newContainer.getProperty(BIND_REF).remove();
            }
        }
        return newContainer;
    }

    private void updateHideTitle (Node panelContainer, Node newContainer) throws RepositoryException {
        if (panelContainer.hasProperty(JCR_TITLE) && panelContainer.hasProperty(JCR_DESCRIPTION)
                && panelContainer.getProperty(JCR_TITLE).getString().equals(panelContainer.getProperty(JCR_DESCRIPTION).getString())) {
            newContainer.setProperty(HIDE_TITLE, false);
        } else {
            newContainer.setProperty(HIDE_TITLE, true);
        }
    }

    private void updateBindRefForPanel(Node panelContainer, Node newContainer,
                                       String bindRef, boolean isParentRepeatable) throws RepositoryException {
        String oldParentBindRef = fetchBindRef(panelContainer.getParent().getParent());
        if (isParentRepeatable) {
            updateBindRefForRepeatableParent(newContainer, bindRef, oldParentBindRef);
        } else {
            updateBindRefForNonRepeatableParent(newContainer, bindRef, oldParentBindRef, fetchBindRef(newContainer.getParent()));
        }
    }

    private void handleFragmentPanel(Node panelContainer) throws RepositoryException {
        // check if panel contains fragRef property, then convert panel to fragment container along with other rule changes
        if (panelContainer.hasProperty(FRAGMENT_REF)) {
            panelContainer.setProperty(SLING_RESOURCE_TYPE_PROPERTY, CORE_FRAGMENT_RESOURCE_TYPE);
            panelContainer.setProperty(FIELD_TYPE, PANEL);
            panelContainer.setProperty(FRAGMENT_PATH, panelContainer.getProperty(FRAGMENT_REF).getString());
            panelContainer.getProperty(FRAGMENT_REF).remove();
            panelContainer.getProperty(GUIDE_NODE_CLASS).remove();
        }
    }
}
