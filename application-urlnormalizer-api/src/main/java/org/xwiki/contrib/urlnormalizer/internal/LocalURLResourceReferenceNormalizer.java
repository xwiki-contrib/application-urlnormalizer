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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * Default implementation of {@link ResourceReferenceNormalizer}, supporting only the "standard" URL scheme for the
 * moment.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class LocalURLResourceReferenceNormalizer implements ResourceReferenceNormalizer
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
    private URLValidator<ExtendedURL> localURLValidator;

    @Inject
    private URLValidator<EntityResourceReference> actionURLValidator;

    @Override
    public ResourceReference normalize(ResourceReference reference)
    {
        this.logger.debug("Trying to normalize [{}]", reference.getReference());

        ResourceReference normalizedReference = reference;
        try {
            if (reference.getType().equals(ResourceType.URL) && this.container.getRequest() instanceof ServletRequest) {
                URL referenceURL = new URL(reference.getReference());

                // Ignore URLs with a reference since they are not supported right now
                // FIXME: remove when https://jira.xwiki.org/browse/URLNORMALZ-11 is fixed
                if (StringUtils.isEmpty(referenceURL.getRef())) {
                    ServletRequest servletRequest = (ServletRequest) this.container.getRequest();
                    ExtendedURL extendedURL =
                        new ExtendedURL(referenceURL, servletRequest.getHttpServletRequest().getContextPath());
                    if (this.localURLValidator.validate(extendedURL)) {
                        normalizedReference = resolveReference(extendedURL, reference);
                    }
                }
            }
        } catch (Exception e) {
            // An error happened during normalization. Ideally we should log it as a warning. The problem is that
            // the URL parser we use will generate a CreateResourceReferenceException if the URL to parse is not a
            // local URL. Thus we need to ignore all errors in order to avoid spurious logs for the user.
            // That's why we log it only at debug level.
            this.logger.debug("Failed to normalize URL [{}] into a wiki link", reference.getReference(), e);
        }

        return normalizedReference;
    }

    private ResourceReference resolveReference(ExtendedURL extendedURL, ResourceReference originalReference)
        throws Exception
    {
        ResourceReference normalizedReference = originalReference;

        org.xwiki.resource.ResourceType type = this.typeResolver.resolve(extendedURL, Collections.emptyMap());
        if (type.getId().equals("entity") || type.getId().equals("wiki")) {
            EntityResourceReference err =
                (EntityResourceReference) this.resolver.resolve(extendedURL, type, Collections.emptyMap());

            // At this point we're sure that the URL is pointing to a wiki link but we still need to verify that we
            // point to a URL for a supported action (view or download) since wiki links only support some actions ATM.
            if (this.actionURLValidator.validate(err)) {
                // We need to handle both Attachment and Document resource types
                ResourceType referenceResourceType;
                if (err.getEntityReference().getType().equals(EntityType.ATTACHMENT)) {
                    referenceResourceType = ResourceType.ATTACHMENT;
                } else {
                    referenceResourceType = ResourceType.DOCUMENT;
                }

                normalizedReference =
                    new ResourceReference(this.serializer.serialize(err.getEntityReference()), referenceResourceType);

                // Handle query string parameters.
                //
                // Any query string parameter in the ExtendedURL will find their ways as parameters in
                // EntityResourceReference. We need to copy them in the normalized ResourceReference.
                // Also note that the original ResourceReference could theoretically have existing parameters that we
                // should keep. However in practice this is not possible since there's no markup syntax for that and
                // thus we can safely ignore it.
                for (Map.Entry<String, List<String>> parameter : err.getParameters().entrySet()) {
                    // Note: EntityResourceReference supports having several parameters with the same name but
                    // ResourceReference doesn't.
                    // However since our original input is a ResourceReference, there's no risk of having several
                    // parameters with the same name and we can safely take the first one!...
                    normalizedReference.setParameter(parameter.getKey(), parameter.getValue().get(0));
                }

                // Handle fragments (aka anchors).
                //
                // Since fragments are currently not parsed by the ResourceReferenceResolver<ExtendedURL> component,
                // we're currently simply ignoring normalization in this case.
                if (extendedURL.getURI().getRawFragment() != null) {
                    // Abort normalization
                    normalizedReference = originalReference;
                }
            }
        }

        return normalizedReference;
    }
}
