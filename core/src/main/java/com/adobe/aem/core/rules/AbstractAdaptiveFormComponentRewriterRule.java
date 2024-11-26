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
import org.apache.sling.api.resource.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.converter.Converters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.*;

import static org.apache.sling.jcr.resource.api.JcrResourceConstants.AUTHENTICATION_INFO_SESSION;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.SLING_RESOURCE_TYPE_PROPERTY;

public abstract class AbstractAdaptiveFormComponentRewriterRule implements ComponentRewriteRule {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAdaptiveFormComponentRewriterRule.class);
    protected String id;
    protected int ranking = Integer.MAX_VALUE;
    protected List<String> componentResourceTypes;
    protected ResourceResolverFactory resourceResolverFactory;

    protected void init(ComponentContext context, List<String> componentResourceTypes, ResourceResolverFactory resourceResolverFactory) {
        Dictionary<String, Object> props = context.getProperties();
        this.ranking = Converters.standardConverter().convert(props.get("service.ranking")).defaultValue(Integer.MAX_VALUE).to(Integer.class);
        this.id = Converters.standardConverter().convert(props.get("service.pid")).defaultValue(this.id).to(String.class);
        this.componentResourceTypes = componentResourceTypes;
        this.resourceResolverFactory = resourceResolverFactory;
    }

    @Override
    public @NotNull Set<String> findMatches(@NotNull Resource resource) {
        final Set<String> paths = new LinkedHashSet<>();
        ResourceResolver rr = resource.getResourceResolver();
        // visit the entire child hierarchy and find all the panels if required
        new AbstractResourceVisitor() {
            @Override
            protected void visit(@NotNull Resource resource) {
                for (String resourceType : componentResourceTypes) {
                    if (resource.isResourceType(resourceType)) {
                        paths.add(resource.getPath());
                        break;
                    }
                }
            }
        }.accept(resource);
        return paths;
    }

    @Override
    public boolean hasPattern(@NotNull String... slingResourceTypes) {
        List<String> types = Arrays.asList(slingResourceTypes);
        return !Collections.disjoint(types, this.componentResourceTypes);
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public boolean matches(@NotNull Node node) throws RepositoryException {
        if (!node.hasProperty(SLING_RESOURCE_TYPE_PROPERTY)) {
            return false;
        }
        boolean found = false;
        Session session = node.getSession();
        try (ResourceResolver rr = this.resourceResolverFactory.getResourceResolver(
                Collections.singletonMap(AUTHENTICATION_INFO_SESSION, session))) {
            Resource formResource = rr.getResource(node.getPath());
            for (String resourceType : componentResourceTypes) {
                if (formResource.isResourceType(resourceType)) {
                    found = true;
                    break;
                }
            }
        } catch (LoginException e) {
            logger.error("Unable to get a ResourceResolver using Node Session info.", e);
            return false;
        }
        return found;
    }

    @Override
    public int getRanking() {
        return this.ranking;
    }

    @Override
    public @Nullable Node applyTo(@NotNull Node root, @NotNull Set<String> finalPaths) throws RepositoryException {
        return processComponent(root, finalPaths);
    }

    protected abstract @Nullable Node processComponent(@NotNull Node root, @NotNull Set<String> finalPaths) throws RepositoryException;
}
