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

import java.util.Arrays;
import java.util.regex.Pattern;

import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.contrib.urlnormalizer.internal.configuration.DefaultURLNormalizerFilter;
import org.xwiki.contrib.urlnormalizer.internal.configuration.URLNormalizerConfigurationStore;
import org.xwiki.model.reference.AttachmentReference;
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
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link LocalURLResourceReferenceNormalizer}.
 *
 * @version $Id$
 */
@ComponentTest
class LocalURLResourceReferenceNormalizerTest
{
    @InjectMockComponents
    private LocalURLResourceReferenceNormalizer normalizer;

    @MockComponent
    private Container container;

    @MockComponent
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @MockComponent
    private ResourceReferenceResolver<ExtendedURL> resolver;

    @MockComponent
    private URLValidator<ExtendedURL> localURLValidator;

    @MockComponent
    private URLValidator<EntityResourceReference> actionURLValidator;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactwikiSerializer;

    @MockComponent
    private URLNormalizerConfigurationStore store;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        ServletRequest request = mock(ServletRequest.class);
        when(this.container.getRequest()).thenReturn(request);
        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);
        when(httpRequest.getContextPath()).thenReturn("xwiki");
    }

    @Test
    void normalizeWhenURLPointsToWikiLink() throws Exception
    {
        assertNormalizeWhenURLPointsToWikiLink(true);
    }

    void assertNormalizeWhenURLPointsToWikiLink(boolean expectConverted) throws Exception
    {
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        EntityReference entityReference = new DocumentReference("wiki", "A", "B");
        EntityResourceReference err = new EntityResourceReference(entityReference, new EntityResourceAction("view"));
        when(this.resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        when(this.localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        when(this.actionURLValidator.validate(err)).thenReturn(true);

        when(this.compactwikiSerializer.serialize(entityReference)).thenReturn("A.B");

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        if (expectConverted) {
            assertEquals(ResourceType.DOCUMENT, normalizedReference.getType());
            assertEquals("A.B", normalizedReference.getReference());
        } else {
            assertSame(reference, normalizedReference);
        }
    }

    @Test
    void normalizeWhenURLContainsAReference() throws Exception
    {
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        EntityReference entityReference = new DocumentReference("wiki", "A", "B");
        EntityResourceReference err = new EntityResourceReference(entityReference, new EntityResourceAction("view"));
        when(this.resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        when(this.localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        when(this.actionURLValidator.validate(err)).thenReturn(true);

        when(this.compactwikiSerializer.serialize(entityReference)).thenReturn("A.B");

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B#reference", ResourceType.URL);
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        assertEquals(ResourceType.URL, normalizedReference.getType());
        assertEquals("http://my.some.domain/xwiki/bin/view/A/B#reference", normalizedReference.getReference());
    }

    @Test
    void normalizeWhenURLPointsToWikiLinkWithQueryString() throws Exception
    {
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        EntityReference entityReference = new DocumentReference("wiki", "A", "B");
        EntityResourceReference err = new EntityResourceReference(entityReference, new EntityResourceAction("view"));
        err.addParameter("a", "b");
        when(this.resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        when(this.localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        when(this.actionURLValidator.validate(err)).thenReturn(true);

        when(this.compactwikiSerializer.serialize(entityReference)).thenReturn("A.B");

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        reference.setParameter("a", "b");
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        assertEquals(ResourceType.DOCUMENT, normalizedReference.getType());
        assertEquals("A.B", normalizedReference.getReference());
        assertEquals("b", normalizedReference.getParameter("a"));
    }

    @Test
    void normalizeWhenURLPointsToWikiLinkButNotView() throws Exception
    {
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        EntityReference entityReference = new DocumentReference("wiki", "A", "B");
        EntityResourceReference err = new EntityResourceReference(entityReference, new EntityResourceAction("view"));
        when(this.resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        when(this.localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        when(this.actionURLValidator.validate(err)).thenReturn(false);

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        assertEquals("http://my.some.domain/xwiki/bin/view/A/B", normalizedReference.getReference());
    }

    @Test
    void normalizeWhenUnsupportedResourceType() throws Exception
    {
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenThrow(new CreateResourceTypeException("error"));

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/view/A/B", ResourceType.URL);
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        assertEquals("http://my.some.domain/xwiki/bin/view/A/B", normalizedReference.getReference());
    }

    @Test
    void normalizeDownloadURL() throws Exception
    {
        org.xwiki.resource.ResourceType type = new org.xwiki.resource.ResourceType("entity");
        when(this.typeResolver.resolve(any(ExtendedURL.class), any())).thenReturn(type);

        EntityReference entityReference =
            new AttachmentReference("attachment", new DocumentReference("wiki", "A", "B"));
        EntityResourceReference err =
            new EntityResourceReference(entityReference, new EntityResourceAction("download"));
        when(this.resolver.resolve(any(ExtendedURL.class), eq(type), any())).thenReturn(err);

        when(this.localURLValidator.validate(any(ExtendedURL.class))).thenReturn(true);

        when(this.actionURLValidator.validate(err)).thenReturn(true);

        when(this.compactwikiSerializer.serialize(entityReference)).thenReturn("A.B@attachment");

        ResourceReference reference =
            new ResourceReference("http://my.some.domain/xwiki/bin/download/A/B/attachment", ResourceType.URL);
        ResourceReference normalizedReference = this.normalizer.normalize(reference);

        assertEquals(ResourceType.ATTACHMENT, normalizedReference.getType());
        assertEquals("A.B@attachment", normalizedReference.getReference());
    }

    @Test
    void normalizeWithAFilter() throws Exception
    {
        when(this.store.getFilters(null)).thenReturn(Arrays.asList(
            new DefaultURLNormalizerFilter(ResourceType.ATTACHMENT, Pattern.compile("re(.*)"),
                ResourceType.DOCUMENT, "filtered-${1}"),
            new DefaultURLNormalizerFilter(ResourceType.ATTACHMENT, Pattern.compile("otherrefe(?<name>.*)"),
                ResourceType.DOCUMENT, "filtered-${name}")));

        assertEquals(new ResourceReference("reference", ResourceType.DATA), this.normalizer.normalize(new ResourceReference("reference", ResourceType.DATA)));
        assertEquals(new ResourceReference("filtered-ference", ResourceType.DOCUMENT), this.normalizer.normalize(new ResourceReference("reference", ResourceType.ATTACHMENT)));
        assertEquals(new ResourceReference("filtered-rence", ResourceType.DOCUMENT), this.normalizer.normalize(new ResourceReference("otherreference", ResourceType.ATTACHMENT)));
    }

    @Test
    void normalizeWithAFilterWithNullLinkType() throws Exception
    {
        when(this.store.getFilters(null)).thenReturn(Arrays.asList(
            new DefaultURLNormalizerFilter(null, Pattern.compile("re(.*)"), ResourceType.DOCUMENT, "filtered-${1}")));

        assertEquals(new ResourceReference("filtered-ference", ResourceType.DOCUMENT),
            this.normalizer.normalize(new ResourceReference("reference", ResourceType.DOCUMENT)));
        assertEquals(new ResourceReference("filtered-ference", ResourceType.DOCUMENT),
            this.normalizer.normalize(new ResourceReference("reference", ResourceType.DATA)));
    }

    @Test
    void normalizeWhenDisablingURLPointsToWikiLink() throws Exception
    {
        when(this.store.getFilters(null)).thenReturn(Arrays.asList(
            new DefaultURLNormalizerFilter(ResourceType.URL, Pattern.compile("http://my.some.domain/xwiki/bin/view/A/B"),
                null, null)));

        assertNormalizeWhenURLPointsToWikiLink(false);
    }
}
