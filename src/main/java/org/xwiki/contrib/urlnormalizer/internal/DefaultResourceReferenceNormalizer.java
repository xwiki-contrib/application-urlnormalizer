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

import java.net.URL;
import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * This is the default implementation of {@link ResourceReferenceNormalizer}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultResourceReferenceNormalizer implements ResourceReferenceNormalizer
{
    @Inject
    private Logger logger;

    @Inject
    private Container container;

    @Inject
    private ResourceTypeResolver<ExtendedURL> typeResolver;

    @Inject
    private ResourceReferenceResolver<ExtendedURL> resolver;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private URLConfiguration urlConfiguration;

    @Inject
    private StandardURLConfiguration standardURLConfiguration;

    @Override
    public ResourceReference normalize(ResourceReference reference)
    {
        ResourceReference normalizedReference = reference;
        try {
            if (reference.getType().equals(ResourceType.URL)) {
                // Create a  ResourceReference object from the Platform URL module
                // TODO: In the future we'll need to merge the ResourceReference from Rendering with the one from
                // Platform
                if (this.container.getRequest() instanceof ServletRequest) {
                    ServletRequest servletRequest = (ServletRequest) this.container.getRequest();
                    ExtendedURL extendedURL = new ExtendedURL(new URL(reference.getReference()),
                        servletRequest.getHttpServletRequest().getContextPath());
                    if (isLocalURL(extendedURL)) {
                        normalizedReference = resolveReference(extendedURL, reference);
                    }
                }
            }
        } catch (Exception e) {
            // An error happened during normalization, log it but continue without doing any normalization
            this.logger.warn("Failed to normalize URL into a wiki link. Error [{}]",
                ExceptionUtils.getRootCauseMessage(e));
        }

        return normalizedReference;
    }

    private ResourceReference resolveReference(ExtendedURL extendedURL, ResourceReference originalReference)
        throws Exception
    {
        ResourceReference normalizedReference = originalReference;
        org.xwiki.resource.ResourceType type =
            this.typeResolver.resolve(extendedURL, Collections.emptyMap());
        if (type.getId().equals("entity") || type.getId().equals("wiki")) {
            EntityResourceReference err =
                (EntityResourceReference) this.resolver.resolve(extendedURL, type, Collections.emptyMap());
            // At this point we're sure that the URL is pointing to a wiki link
            normalizedReference = new ResourceReference(this.serializer.serialize(err.getEntityReference()),
                ResourceType.DOCUMENT);
        }
        return normalizedReference;
    }

    private boolean isLocalURL(ExtendedURL extendedURL) throws Exception
    {
        boolean isLocal = false;

        // Verify that the URL points to a local URL by checking its domain.
        // TODO: Add a new API to check if a URL is a valid local URL in the URL module in the future
        // Note: ATM we only support the "Standard" URL scheme...
        if (this.urlConfiguration.getURLFormatId().equals("standard")) {
            String wikiAlias;
            if (this.standardURLConfiguration.isPathBasedMultiWiki()) {
                if (this.wikiDescriptorManager.getMainWikiId().equals(this.wikiDescriptorManager.getCurrentWikiId())) {
                    // Fall back to domain base in this case
                    wikiAlias = extendedURL.getWrappedURL().getHost();
                } else {
                    // The second segment is the name of the wiki.
                    wikiAlias = extendedURL.getSegments().get(1);
                }
            } else {
                wikiAlias = extendedURL.getWrappedURL().getHost();
            }
            if (this.wikiDescriptorManager.getByAlias(wikiAlias) != null) {
                isLocal = true;
            }
        }
        return isLocal;
    }
}
