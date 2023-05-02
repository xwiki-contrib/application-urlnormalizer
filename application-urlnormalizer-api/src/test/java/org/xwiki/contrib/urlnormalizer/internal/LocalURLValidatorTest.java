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

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalURLValidator}.
 *
 * @version $Id:$
 */
@ComponentTest
class LocalURLValidatorTest
{
    @MockComponent
    private URLConfiguration urlConfiguration;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @InjectMockComponents
    private LocalURLValidator validator;

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.urlConfiguration.getURLFormatId()).thenReturn("standard");
    }

    @Test
    void validateWithUnknownDomain() throws Exception
    {
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("https://unknowndomain/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("https://unknowndomain:443/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("http://unknowndomain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithDescriptorWithSecure() throws Exception
    {
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptor.getPort()).thenReturn(-1);
        when(wikiDescriptor.isSecure()).thenReturn(true);
        when(this.wikiDescriptorManager.getByAlias("domain")).thenReturn(wikiDescriptor);

        assertTrue(this.validator.validate(new ExtendedURL(new URL("https://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertTrue(this.validator.validate(new ExtendedURL(new URL("https://domain:443/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(this.validator.validate(new ExtendedURL(new URL("http://domain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithDescriptorWithNotSecure() throws Exception
    {
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptor.getPort()).thenReturn(-1);
        when(wikiDescriptor.isSecure()).thenReturn(false);
        when(this.wikiDescriptorManager.getByAlias("domain")).thenReturn(wikiDescriptor);

        assertTrue(this.validator.validate(new ExtendedURL(new URL("http://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertTrue(this.validator.validate(new ExtendedURL(new URL("http://domain:80/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(this.validator.validate(new ExtendedURL(new URL("https://domain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithDescriptorWithCustomPort() throws Exception
    {
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptor.getPort()).thenReturn(8080);
        when(this.wikiDescriptorManager.getByAlias("domain")).thenReturn(wikiDescriptor);

        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("https://domain:8080/xwiki/bin/view/A/B"), "xwiki")));
        assertTrue(this.validator.validate(new ExtendedURL(new URL("http://domain:8080/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(this.validator.validate(new ExtendedURL(new URL("https://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(this.validator.validate(new ExtendedURL(new URL("http://domain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithConfiguredHomeWithDefaultHTTPPort() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.home")).thenReturn("http://domain/xwiki/bin/view/A/B");

        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("http://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("http://domain:80/xwiki/bin/view/A/B"), "xwiki")));

        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("http://domain:82/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("http://otherdomain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithConfiguredHomeWithDefaultHTTPSPort() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.home")).thenReturn("https://domain/xwiki/bin/view/A/B");

        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("https://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("https://domain:443/xwiki/bin/view/A/B"), "xwiki")));

        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("https://domain:450/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("https://otherdomain/xwiki/bin/view/A/B"), "xwiki")));
    }

    @Test
    void validateWithConfiguredHomeWithCustomPort() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.home")).thenReturn("https://domain:8080/xwiki/bin/view/A/B");

        assertTrue(
            this.validator.validate(new ExtendedURL(new URL("http://domain:8080/xwiki/bin/view/A/B"), "xwiki")));

        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("http://domain/xwiki/bin/view/A/B"), "xwiki")));
        assertFalse(
            this.validator.validate(new ExtendedURL(new URL("http://otherdomain:8080/xwiki/bin/view/A/B"), "xwiki")));
    }
}
