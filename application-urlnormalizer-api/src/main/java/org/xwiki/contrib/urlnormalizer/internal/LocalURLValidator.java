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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Validates if an {@link ExtendedURL} points to a local URL or not.
 *
 * @version $Id:$
 */
@Component
@Singleton
public class LocalURLValidator implements URLValidator<ExtendedURL>
{
    private static final Map<String, Integer> PORTS = new HashMap<>();

    static {
        PORTS.put("http", 80);
        PORTS.put("https", 443);
    }

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private URLConfiguration urlConfiguration;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    private int getPort(URL url)
    {
        int port = url.getPort();

        if (port == -1) {
            port = PORTS.getOrDefault(url.getProtocol().toLowerCase(), -1);
        }

        return port;
    }

    private int getPort(WikiDescriptor wikiDescriptor)
    {
        int port = wikiDescriptor.getPort();

        if (port == -1) {
            port = Boolean.TRUE.equals(wikiDescriptor.isSecure()) ? 443 : 80;
        }

        return port;
    }

    private boolean isKnownDomainAndPort(ExtendedURL extendedURL) throws WikiManagerException, MalformedURLException
    {
        // Check the descriptor
        WikiDescriptor wikiDescriptor = this.wikiDescriptorManager.getByAlias(extendedURL.getWrappedURL().getHost());
        if (wikiDescriptor != null) {
            return getPort(wikiDescriptor) == getPort(extendedURL.getWrappedURL());
        }

        // Check if the URL matches the configured "home URL"
        return isConfiguredHome(extendedURL);
    }

    private boolean isConfiguredHome(ExtendedURL extendedURL) throws MalformedURLException
    {
        String home = this.configurationSource.getProperty("xwiki.home");

        if (home != null) {
            URL homeURL = new URL(home);

            return homeURL.getHost().equals(extendedURL.getWrappedURL().getHost())
                && getPort(homeURL) == getPort(extendedURL.getWrappedURL());
        }

        return false;
    }

    @Override
    public boolean validate(ExtendedURL extendedURL)
    {
        boolean isLocal = false;

        // TODO: Add a new API to check if a URL is a valid local URL in the URL module in the future
        // Note: ATM we only support the "Standard" URL scheme...
        if (this.urlConfiguration.getURLFormatId().equals("standard")) {
            try {
                // Check if the domain/port is known
                return isKnownDomainAndPort(extendedURL);
            } catch (Exception e) {
                // It's not normal to fail here, log a warning
                this.logger.warn("Failed to validate URL [{}]. Root reason [{}]", extendedURL,
                    ExceptionUtils.getRootCauseMessage(e));

                isLocal = false;
            }
        }

        return isLocal;
    }
}
