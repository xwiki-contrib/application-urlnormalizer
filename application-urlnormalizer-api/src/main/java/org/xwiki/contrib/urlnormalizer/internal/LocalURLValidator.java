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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Validates if an {@link ExtendedURL} points to a local URL or not.
 *
 * @version $Id:$
 */
@Component
@Singleton
public class LocalURLValidator implements URLValidator<ExtendedURL>
{
    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private URLConfiguration urlConfiguration;

    @Inject
    private StandardURLConfiguration standardURLConfiguration;

    @Override
    public boolean validate(ExtendedURL extendedURL)
    {
        boolean isLocal = false;

        try {
            // Verify that the URL points to a local URL by checking its domain.
            // TODO: Add a new API to check if a URL is a valid local URL in the URL module in the future
            // Note: ATM we only support the "Standard" URL scheme...
            if (this.urlConfiguration.getURLFormatId().equals("standard")) {
                String wikiAlias;
                if (this.standardURLConfiguration.isPathBasedMultiWiki()) {
                    if (this.wikiDescriptorManager.getMainWikiId()
                        .equals(this.wikiDescriptorManager.getCurrentWikiId()))
                    {
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
                this.logger.debug("Wiki descriptor for [{}]: {}", wikiAlias, isLocal);
            }
        } catch (Exception e) {
            // It's not normal to fail here, log a warning
            this.logger.warn("Failed to validate URL [{}]. Root reason [{}]", extendedURL,
                ExceptionUtils.getRootCauseMessage(e));
            isLocal = false;
        }
        return isLocal;
    }
}
