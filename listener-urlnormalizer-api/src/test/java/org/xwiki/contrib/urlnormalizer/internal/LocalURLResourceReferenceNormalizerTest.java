/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.contrib.urlnormalizer.internal;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.resource.CreateResourceTypeException;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalURLResourceReferenceNormalizer}.
 *
 * @version $Id$
 */
public class LocalURLResourceReferenceNormalizerTest
{
    @Rule
    public MockitoComponentMockingRule<LocalURLResourceReferenceNormalizer> mocker =
        new MockitoComponentMockingRule<>(LocalURLResourceReferenceNormalizer.class);

    @Before
    public void setUp() throws Exception
    {
        Container container = this.mocker.getInstance(Container.class);
        ServletRequest request = mock(ServletRequest.class);
        when(container.getRequest()).thenReturn(request);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getContextPath()).thenReturn("xwiki");
    }

    @Test
    public void normalizeWhenURLPointsToWikiLink() throws Exception
    {
        ResourceTypeResolver<ExtendedURL> typeResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        ResourceReferenceResolver<ExtendedURL> resolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));
        EntityReference entityReference = new DocumentReference("wiki", "A", "B");
        EntityResourceReference err = new EntityResourceReference(entityReference, new EntityResourceAction("view"));
        when(resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        URLValidator<ExtendedURL> localURLValidator = this.mocker.getInstance(
            new DefaultParameterizedType(null, URLValidator.class, ExtendedURL.class));
        when(localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        EntityReferenceSerializer<String> serializer = this.mocker.getInstance(
        new DefaultParameterizedType(null, EntityReferenceSerializer.class, String.class), "compactwiki");
        when(serializer.serialize(entityReference)).thenReturn("A.B");

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        ResourceReference normalizedReference = this.mocker.getComponentUnderTest().normalize(reference);

        assertEquals("A.B", normalizedReference.getReference());
    }

    @Test
    public void normalizeWhenUnsupportedResourceType() throws Exception
    {
        ResourceTypeResolver<ExtendedURL> typeResolver = this.mocker.getInstance(
            new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));
        when(typeResolver.resolve(any(ExtendedURL.class), any())).thenThrow(new CreateResourceTypeException("error"));

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        ResourceReference normalizedReference = this.mocker.getComponentUnderTest().normalize(reference);

        assertEquals("http://my.some.domain/xwiki/bin/view/A/B", normalizedReference.getReference());
    }
}
