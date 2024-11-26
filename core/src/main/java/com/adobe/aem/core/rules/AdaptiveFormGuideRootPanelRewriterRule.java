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
import org.apache.sling.api.resource.*;
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

@Component(
        service = {ComponentRewriteRule.class},
        property = {
                "service.ranking=20"
        }
)
public class AdaptiveFormGuideRootPanelRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormGuideRootPanelRewriterRule.class);
    private static final List<String> COMPONENT_RESOURCE_TYPES = Arrays.asList(ROOT_PANEL);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormGuideRootPanelRewriterRule.class.getName();
        this.init(context, COMPONENT_RESOURCE_TYPES, this.resourceResolverFactory);

        populateResourceTypeForLayout(CORE_WIZARD_RESOURCE_TYPE, CORE_TABS_ON_TOP_RESOURCE_TYPE,
                CORE_VERTICAL_TABS_RESOURCE_TYPE, CORE_ACCORDION_RESOURCE_TYPE);
    }

    protected Node processComponent(Node root, Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try (ResourceResolver rr = resourceResolverFactory.getResourceResolver(Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Node parent = root.getParent();
            if (parent.hasProperty(V2_COMPONENT_TEMP_PATH)) {
                String newContainerPath = parent.getProperty(V2_COMPONENT_TEMP_PATH).getValue().getString();
                Node container = session.getNode(newContainerPath);
                root.setProperty(V2_COMPONENT_TEMP_PATH,
                        processPanelNode(root, container).getPath());

                // call function to delete
                deleteFormNodes(root, session);
            }
        } catch (RepositoryException | LoginException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    public Node processPanelNode(Node panelContainer, Node newContainer) throws RepositoryException {
        newContainer = handleLayoutNode(panelContainer, newContainer, true);

        NodeIterator children = panelContainer.getNodes();
        while (children.hasNext()) {
            Node node = children.nextNode();
            if (!PANEL_NODES_TO_IGNORE.contains(node.getName())) {
                JcrUtil.copy(node, newContainer, node.getName());
            }
        }
        return newContainer;
    }

}
