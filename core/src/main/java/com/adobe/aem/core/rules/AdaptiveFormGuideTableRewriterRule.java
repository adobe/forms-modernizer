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
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import java.util.*;

import static com.adobe.aem.core.utils.AdaptiveFormConstants.*;
import static com.adobe.aem.core.utils.AdaptiveFormUtils.*;
import static org.apache.jackrabbit.JcrConstants.NT_UNSTRUCTURED;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

@Component(
        service = {ComponentRewriteRule.class},
        property = {
                "service.ranking=20"
        }
)
public class AdaptiveFormGuideTableRewriterRule extends AbstractAdaptiveFormComponentRewriterRule {

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveFormGuideTableRewriterRule.class);
    private static final List<String> COMPONENT_RESOURCE_TYPES = Arrays.asList(GUIDE_TABLE);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Activate
    @Modified
    protected void activate(ComponentContext context) {
        this.id = AdaptiveFormGuideTableRewriterRule.class.getName();
        this.init(context, COMPONENT_RESOURCE_TYPES, this.resourceResolverFactory);
    }

    @Override
    protected @Nullable Node processComponent(@NotNull Node root, @NotNull Set<String> finalPaths) throws RepositoryException {
        Session session = root.getSession();
        try {
            Node grandParent = root.getParent().getParent();
            if (grandParent.hasProperty(V2_COMPONENT_TEMP_PATH)) {
                String newContainerPath = grandParent.getProperty(V2_COMPONENT_TEMP_PATH).getValue().getString();
                Node container = session.getNode(newContainerPath);

                root.setProperty(V2_COMPONENT_TEMP_PATH,
                        processTableNode(root, container).getPath());
                root.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                // call function to delete node
                deleteFormNodes(root, session);
            }
        } catch (RepositoryException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
        }
        return root;
    }

    private Node processTableNode(Node tableComponent, Node tableContainer) throws RepositoryException {
        tableContainer = getTableContainer(tableComponent, tableContainer);
        Node tableRows = tableComponent.hasNode(ITEMS) ? tableComponent.getNode(ITEMS) : null;

        if (tableRows != null) {
            NodeIterator tableRowIterator = tableRows.getNodes();
            while (tableRowIterator.hasNext()) {
                Node tableRowComponent = tableRowIterator.nextNode();
                Node tableRowContainer = getTableRowContainer(tableRowComponent, tableContainer);
                Node rowItemNode = tableRowComponent.hasNode(ITEMS) ? tableRowComponent.getNode(ITEMS) : null;
                int rowItemSize = (int) rowItemNode.getNodes().getSize();
                int width = 12 / rowItemSize;
                if(width == 0) {
                    width = 1;
                }
                if (rowItemNode != null) {
                    NodeIterator rowItems = rowItemNode.getNodes();
                    while (rowItems.hasNext()) {
                        Node rowNode = rowItems.nextNode();
                        addLayoutProperties(rowNode, width);
                        JcrUtil.copy(rowNode, tableRowContainer, rowNode.getName());
                        String bindRef = fetchBindRef(rowNode);
                        if (bindRef != null) {
                            updateBindRefForNonRepeatableParent(tableRowContainer.getNode(rowNode.getName()), bindRef, null, null);
                        }
                        rowNode.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                    }
                }
                tableRowComponent.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
                copyTableNodeChildren(tableRowComponent, tableRowContainer);
            }
        }

        copyTableNodeChildren(tableComponent, tableContainer);

        return tableContainer;
    }

    private Node getTableContainer(Node tableComponent, Node tableContainer) throws RepositoryException {
        tableContainer = createNodeAndCopyProperties(tableComponent, tableContainer, tableComponent.getName());
        tableContainer.getProperty(GUIDE_NODE_CLASS).remove();
        tableContainer.setProperty(FIELD_TYPE, PANEL);
        tableContainer.setProperty(HIDE_TITLE, true);
        tableContainer.setProperty(LAYOUT_PROPERTY, RESPONSIVE_GRID);
        tableContainer.setProperty(SLING_RESOURCE_TYPE_PROPERTY, GUIDE_PANEL);
        String bindRef = fetchBindRef(tableComponent);
        if (bindRef != null) {
            updateBindRefForNonRepeatableParent(tableContainer, bindRef, null, null);
        }

        return tableContainer;
    }

    private Node getTableRowContainer(Node tableRowComponent, Node tableContainer) throws RepositoryException {
        Node tableRowContainer = tableContainer.addNode(tableRowComponent.getName(), NT_UNSTRUCTURED);
        tableRowContainer.setProperty(FIELD_TYPE, PANEL);
        tableRowContainer.setProperty(HIDE_TITLE, true);
        tableRowContainer.setProperty(LAYOUT_PROPERTY, RESPONSIVE_GRID);
        tableRowContainer.setProperty(SLING_RESOURCE_TYPE_PROPERTY, GUIDE_PANEL);
        if(tableRowComponent.hasProperty(JCR_TITLE)){
            tableRowContainer.setProperty(JCR_TITLE, tableRowComponent.getProperty(JCR_TITLE).getValue());
        }
        if(tableRowComponent.hasProperty(NAME)){
            tableRowContainer.setProperty(NAME, tableRowComponent.getProperty(NAME).getValue());
        }

        String bindRef = fetchBindRef(tableRowComponent);
        if (bindRef != null) {
            updateBindRefForNonRepeatableParent(tableRowContainer, bindRef, null, null);
        }
        return tableRowContainer;
    }

    private void addLayoutProperties(Node rowNode, int width) throws RepositoryException {
        Node responsiveNode = rowNode.addNode(CQ_RESPONSIVE, NT_UNSTRUCTURED);
        Node defaultNode = responsiveNode.addNode(DEFAULT, NT_UNSTRUCTURED);
        defaultNode.setProperty(OFFSET, "0");
        defaultNode.setProperty(WIDTH, Integer.toString(width));
    }

    private void copyTableNodeChildren(Node tableNode, Node panelNode) throws RepositoryException {
        NodeIterator nodeIterator = tableNode.getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            if(!PANEL_NODES_TO_IGNORE.contains(node.getName())) {
                JcrUtil.copy(node, panelNode, node.getName());
                node.getProperty(SLING_RESOURCE_TYPE_PROPERTY).remove();
            }
        }
    }
}
