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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLConfiguration;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LocalURLValidator}.
 *
 * @version $Id:$
 */
public class LocalURLValidatorTest
{
    @Rule
    public MockitoComponentMockingRule<LocalURLValidator> mocker =
        new MockitoComponentMockingRule<>(LocalURLValidator.class);

    @Before
    public void setUp() throws Exception
    {
        URLConfiguration urlConfiguration = this.mocker.getInstance(URLConfiguration.class);
        when(urlConfiguration.getURLFormatId()).thenReturn("standard");
    }

    @Test
    public void validateLocalURLInDomainBased() throws Exception
    {
        StandardURLConfiguration standardURLConfiguration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(false);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptorManager.getByAlias("my.some.domain")).thenReturn(wikiDescriptor);

        ExtendedURL url = new ExtendedURL(new URL("http://my.some.domain/xwiki/bin/view/A/B"), "xwiki");
        assertTrue(this.mocker.getComponentUnderTest().validate(url));
    }

    @Test
    public void validateLocalURLInPathBased() throws Exception
    {
        StandardURLConfiguration standardURLConfiguration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(true);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptorManager.getByAlias("mywiki")).thenReturn(wikiDescriptor);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("currentwiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainwiki");

        ExtendedURL url = new ExtendedURL(new URL("http://whatever/xwiki/wiki/mywiki/view/A/B"), "xwiki");
        assertTrue(this.mocker.getComponentUnderTest().validate(url));
    }

    @Test
    public void validateLocalURLInPathBasedAndMainWiki() throws Exception
    {
        StandardURLConfiguration standardURLConfiguration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(true);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        WikiDescriptor wikiDescriptor = mock(WikiDescriptor.class);
        when(wikiDescriptorManager.getByAlias("domain")).thenReturn(wikiDescriptor);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainwiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainwiki");

        ExtendedURL url = new ExtendedURL(new URL("http://domain/xwiki/wiki/mywiki/view/A/B"), "xwiki");
        assertTrue(this.mocker.getComponentUnderTest().validate(url));
    }

    @Test
    public void validateNonLocalURLInDomainBased() throws Exception
    {
        StandardURLConfiguration standardURLConfiguration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(standardURLConfiguration.isPathBasedMultiWiki()).thenReturn(false);

        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getByAlias("some.other.domain")).thenReturn(null);

        ExtendedURL url = new ExtendedURL(new URL("http://some.other.domain/xwiki/bin/view/A/B"), "xwiki");
        assertFalse(this.mocker.getComponentUnderTest().validate(url));
    }
}
