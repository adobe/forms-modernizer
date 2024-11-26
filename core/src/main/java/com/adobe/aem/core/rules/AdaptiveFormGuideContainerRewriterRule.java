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
import org.apache.commons.lang3.StringUtils;
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
import static com.adobe.aem.core.utils.AdaptiveFormUtils.createNodeAndCopyProperties;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(
        service = {ComponentRewriteRule.class},
        property = {
                "service.ranking=20"
        }
)
public class AdaptiveFormGuideContainerRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormGuideContainerRewriterRule.class);
    private static final List<String> COMPONENT_RESOURCE_TYPES = Arrays.asList(GUIDE_CONTAINER_RESOURCE_TYPE,
            GUIDE_CONTAINER_WRAPPER_RESOURCE_TYPE, GUIDE_FRAGMENT_CONTAINER_RESOURCE_TYPE);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormGuideContainerRewriterRule.class.getName();
        this.init(context, COMPONENT_RESOURCE_TYPES, this.resourceResolverFactory);
    }

    protected Node processComponent(Node root, Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try (ResourceResolver rr = resourceResolverFactory.getResourceResolver(Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Node parent = root.getParent();
            String guideResourceType = root.getProperty(SLING_RESOURCE_TYPE_PROPERTY).getValue().getString();

            // only create and copy properties of container node and return
            String name = JcrUtil.createValidChildName(parent, TEMP_CONTAINER_NODE);
            Node container = createNodeAndCopyProperties(root, parent, name);
            container.setProperty(FD_VERSION, CORE_COMPONENT_VERSION);
            container.setProperty(FIELD_TYPE, FIELD_TYPE_FORM);
            container.setProperty(THEME_REF, CANVAS_THEME_PATH);
            container.getProperty(GUIDE_NODE_CLASS).remove();
            container.getProperty(GUIDE_CSS).remove();
            if (GUIDE_CONTAINER_RESOURCE_TYPE.equals(guideResourceType) || GUIDE_CONTAINER_WRAPPER_RESOURCE_TYPE.equals(guideResourceType)) {
                container.setProperty(SLING_RESOURCE_TYPE_PROPERTY, CORE_FORM_CONTAINER_RESOURCE_TYPE);
            } else if(GUIDE_FRAGMENT_CONTAINER_RESOURCE_TYPE.equals(guideResourceType)){
                container.setProperty(SLING_RESOURCE_TYPE_PROPERTY, CORE_FRAGMENT_CONTAINER_RESOURCE_TYPE);
                container.setProperty(FD_TYPE, FIELD_TYPE_FRAGMENT);
            }
            parent.setProperty(SLING_RESOURCE_TYPE_PROPERTY, CORE_PAGE_RESOURCE_TYPE);
            if (GUIDE_CONTAINER_RESOURCE_TYPE.equals(guideResourceType) || GUIDE_CONTAINER_WRAPPER_RESOURCE_TYPE.equals(guideResourceType)) {
                parent.setProperty(CQ_TEMPLATE, CORE_FORM_TEMPLATE_PATH);
            } else if(GUIDE_FRAGMENT_CONTAINER_RESOURCE_TYPE.equals(guideResourceType)){
                parent.setProperty(CQ_TEMPLATE, CORE_FRAGMENT_TEMPLATE_PATH);
            }

            // structural changes starts from here
            NodeIterator children = root.getNodes();
            while (children.hasNext()) {
                Node node = children.nextNode();
                if (!CONTAINER_NODES_TO_IGNORE.contains(node.getName())) {
                    if (VIEW.equals(node.getName())) {
                        //rename view to fd:view
                        session.move(node.getPath(), container.getPath() + "/" + FD_VIEW);
                    } else {
                        JcrUtil.copy(node, container, node.getName());
                    }
                }
            }

            int index = root.getPath().indexOf(JCR_CONTENT_GUIDE_CONTAINER_PATH);
            if (index != -1) {
                String damPath = root.getPath().substring(0, index);
                String formDamPath = StringUtils.replace(damPath, FORMS_PAGE_PREFIX, FORMS_ASSET_PREFIX);
                updateFormsAssetNode(session.getNode(formDamPath));
            }
            root.setProperty(V2_COMPONENT_TEMP_PATH, container.getPath());
        } catch (RepositoryException | LoginException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    private void updateFormsAssetNode(Node formsAssetNode) throws RepositoryException {
        if (formsAssetNode != null && formsAssetNode.hasNode(JCR_CONTENT) && formsAssetNode.getNode(JCR_CONTENT).hasNode(METADATA)) {
            Node metadataNode = formsAssetNode.getNode(JCR_CONTENT).getNode(METADATA);
            metadataNode.setProperty(FD_VERSION, CORE_COMPONENT_VERSION);
            metadataNode.setProperty(THEME_REF, CANVAS_THEME_PATH);
            metadataNode.setProperty(IS_MIGRATED, true);
            if (metadataNode.hasProperty(TEMPLATE_REF) && V1_FRAGMENT_TEMPLATE_REF.equals(metadataNode.getProperty(TEMPLATE_REF).getString())) {
                metadataNode.setProperty(TEMPLATE_REF, CORE_FRAGMENT_TEMPLATE_PATH);
            }
        }
    }
}
