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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;

import com.adobe.aem.modernize.component.impl.ComponentRewriteRuleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;


@ExtendWith(SlingContextExtension.class)
@Disabled
public class AdaptiveFormTreeRewriteRuleTest {

    // Oak needed to verify order preservation.
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @BeforeEach
    public void beforeEach() {
        context.load().json("/component/form-content.json", "/content/forms/af");
        context.load().json("/component/code-content.json", "/apps");
        context.load().json("/component/test-rules.json", "/var/aem-modernize/rules/component");
    }


    @Test
    public <R extends ResourceResolver, F extends ResourceResolverFactory> void test() throws Exception {

        ResourceResolverFactory resourceResolverFactory = Mockito.mock(ResourceResolverFactory.class);
        ResourceResolver resourceResolver = context.resourceResolver();
        ResourceResolver mockResourceResolver = Mockito.mock(ResourceResolver.class);
        Mockito.when(resourceResolverFactory.getResourceResolver(Mockito.anyMap())).thenReturn(resourceResolver);
        Mockito.when(resourceResolverFactory.getServiceResourceResolver(Mockito.anyMap())).thenReturn(resourceResolver);
        context.registerService(ResourceResolverFactory.class, resourceResolverFactory);
        Mockito.doNothing().when(mockResourceResolver).close();

        ComponentRewriteRuleServiceImpl componentService = new ComponentRewriteRuleServiceImpl();

        //AdaptiveFormRewriteRule rule = new AdaptiveFormRewriteRule();
        Map<String, Object> props = new HashMap<>();
        props.put("corecontainer.resourceType", "forms-components-examples/components/form/container");
        props.put("corepanel.resourceType", "forms-components-examples/components/form/panel");
        props.put("coretextinput.resourceType", "forms-components-examples/components/form/textinput");
        //context.registerInjectActivateService(rule, props);

        props = new HashMap<>();
        props.put("search.paths", new String[] {"/var/aem-modernize/rules/component"});
        context.registerInjectActivateService(componentService, props);

        Resource root = context.resourceResolver().getResource("/content/forms/af/nested/jcr:content/guideContainer");
        Set<String> rules = new HashSet<>();
        //rules.add(rule.getId());

        // isResourceType in test not working as expected, hence adding direct resource types here
        componentService.apply(root, rules,false);

        ResourceResolver rr = root.getResourceResolver();
        rr.commit();
        Resource updated = context.resourceResolver().getResource("/content/forms/af/nested/jcr:content/guideContainer");
        Resource par = updated.getChild("par");
        assertNotNull(par, "Responsive Grid Exists");
        assertEquals("geodemo/components/container", par.getResourceType(), "Responsive grid resource type");

        Iterator<Resource> children = par.listChildren();
        assertEquals("title", children.next().getName(), "Node order preserved.");
        assertEquals("image_1", children.next().getName(), "Node order preserved.");
        assertEquals("title_1", children.next().getName(), "Node order preserved.");
        assertEquals("text_1", children.next().getName(), "Node order preserved.");
        assertEquals("image_0", children.next().getName(), "Node order preserved.");
        assertEquals("title_2", children.next().getName(), "Node order preserved.");
        assertEquals("text_0", children.next().getName(), "Node order preserved.");
        assertEquals("image", children.next().getName(), "Node order preserved.");
    }
}

