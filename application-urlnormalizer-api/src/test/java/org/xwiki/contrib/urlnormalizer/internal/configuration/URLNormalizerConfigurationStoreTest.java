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
package org.xwiki.contrib.urlnormalizer.internal.configuration;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.contrib.urlnormalizer.URLNormalizerFilter;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link URLNormalizerConfigurationStore}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class URLNormalizerConfigurationStoreTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private CacheManager cacheManager;

    @InjectMockComponents
    private URLNormalizerConfigurationStore store;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        when(this.cacheManager.createNewCache(any())).thenReturn(mock(Cache.class));
    }

    private void addFilterObject(XWikiDocument document, String sourceType, String sourceReference, String targetType,
        String targetReference)
    {
        BaseObject filterObject = new BaseObject();
        filterObject.setXClassReference(URLNormalizerFilterClassInitializer.CLASS_REFERENCE);
        filterObject.setStringValue(URLNormalizerFilterClassInitializer.FIELD_LINK_TYPE, sourceType);
        filterObject.setLargeStringValue(URLNormalizerFilterClassInitializer.FIELD_LINK_REFERENCE, sourceReference);
        filterObject.setStringValue(URLNormalizerFilterClassInitializer.FIELD_TARGET_TYPE, targetType);
        filterObject.setLargeStringValue(URLNormalizerFilterClassInitializer.FIELD_TARGET_REFERENCE, targetReference);
        document.addXObject(filterObject);
    }

    @Test
    void getFiltersIgnoresFilterWithoutLinkReference() throws Exception
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        WikiReference wiki = new WikiReference(xcontext.getMainXWiki());

        XWikiDocument configurationDocument = new XWikiDocument(
            new DocumentReference(wiki.getName(), Arrays.asList("URLNormalizer", "Code"), "Configuration"));
        addFilterObject(configurationDocument, "", "", "", "");
        addFilterObject(configurationDocument, "url", "https://example.com/(.*)", "doc", "Main.${1}");
        this.oldcore.getSpyXWiki().saveDocument(configurationDocument, xcontext);

        List<URLNormalizerFilter> filters = this.store.getFilters(wiki);

        assertEquals(1, filters.size());
        URLNormalizerFilter filter = filters.get(0);
        assertEquals(ResourceType.URL, filter.getLinkType());
        assertEquals("https://example.com/(.*)", filter.getLinkReference().pattern());
        assertEquals(ResourceType.DOCUMENT, filter.getTargetType());
        assertEquals("Main.${1}", filter.getTargetReference());

        assertEquals("Ignoring the URL normalizer filter located in object"
            + " [Object xwiki:URLNormalizer.Code.Configuration^URLNormalizer.Code.FilterClass[0]] since it has no link"
            + " reference pattern. It should be either filled or removed.", this.logCapture.getMessage(0));
    }
}
