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
package org.xwiki.contrib.urlnormalizer.internal.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.contrib.urlnormalizer.URLNormalizerFilter;
import org.xwiki.contrib.urlnormalizer.internal.URLNormalizerFilterClassInitializer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.listener.reference.ResourceType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Manipulate {@link URLNormalizerFilter} instances.
 * 
 * @version $Id$
 * @since 1.6
 */
@Component(roles = URLNormalizerConfigurationStore.class)
@Singleton
public class URLNormalizerConfigurationStore implements Initializable
{
    private static final LocalDocumentReference CONFIGURATION_REFERENCE = new LocalDocumentReference("Configuration",
        new EntityReference(URLNormalizerFilterClassInitializer.CODE_SPACE_NAME, EntityType.SPACE,
            new EntityReference(URLNormalizerFilterClassInitializer.URLNORMALIZER_SPACE_NAME, EntityType.SPACE)));

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private Logger logger;

    private Cache<List<URLNormalizerFilter>> cache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.cache =
                this.cacheManager.createNewCache(new LRUCacheConfiguration("urlnormalizer.configuration", 100));
        } catch (Exception e) {
            throw new InitializationException("Failed to create URL Normalizer Configuration cache", e);
        }
    }

    /**
     * @param wiki the wiki for which to get the filters
     * @return the filters
     * @throws NormalizationException when failing to load the filters
     */
    public List<URLNormalizerFilter> getFilters(WikiReference wiki) throws NormalizationException
    {
        List<URLNormalizerFilter> filters = this.cache.get(wiki.getName());

        if (filters == null) {
            try {
                filters = loadFilters(wiki);

                this.cache.set(wiki.getName(), filters);
            } catch (Exception e) {
                throw new NormalizationException(
                    "Failed to load link normalization filters for wiki [" + wiki.getName() + "]", e);
            }
        }

        return filters;
    }

    private List<URLNormalizerFilter> loadFilters(WikiReference wiki) throws XWikiException, NormalizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Load current wiki filters
        List<URLNormalizerFilter> filters = loadFilters(wiki, xcontext);

        // Load main wiki filters if not already in main wiki
        if (!xcontext.isMainWiki(wiki.getName())) {
            List<URLNormalizerFilter> mainFilters = getFilters(new WikiReference(xcontext.getMainXWiki()));
            if (!mainFilters.isEmpty()) {
                filters = ListUtils.sum(filters, mainFilters);
            }
        }

        return filters;
    }

    private List<URLNormalizerFilter> loadFilters(WikiReference wikiReference, XWikiContext xcontext)
        throws XWikiException
    {
        XWikiDocument filtersDocument =
            xcontext.getWiki().getDocument(new DocumentReference(CONFIGURATION_REFERENCE, wikiReference), xcontext);

        List<BaseObject> filterObjects =
            filtersDocument.getXObjects(URLNormalizerFilterClassInitializer.CLASS_REFERENCE);

        List<URLNormalizerFilter> filters = new ArrayList<>(filterObjects.size());
        for (BaseObject filterObject : filterObjects) {
            if (filterObject != null) {
                try {
                    filters.add(loadFilter(filterObject));
                } catch (Exception e) {
                    this.logger.warn("Failed to parse filter located in object [{}]: {}", filterObject.getReference(),
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }

        return filters;
    }

    private URLNormalizerFilter loadFilter(BaseObject filterObject)
    {
        String linkReference = filterObject.getStringValue(URLNormalizerFilterClassInitializer.FIELD_LINK_REFERENCE);

        if (StringUtils.isNotEmpty(linkReference)) {
            return new DefaultURLNormalizerFilter(
                new ResourceType(filterObject.getStringValue(URLNormalizerFilterClassInitializer.FIELD_LINK_TYPE)),
                Pattern.compile(linkReference),
                getResourceType(filterObject, URLNormalizerFilterClassInitializer.FIELD_TARGET_TYPE),
                filterObject.getStringValue(URLNormalizerFilterClassInitializer.FIELD_TARGET_REFERENCE));
        }

        return null;
    }

    private ResourceType getResourceType(BaseObject filterObject, String name)
    {
        String value = StringUtils.defaultIfEmpty(filterObject.getStringValue(name), null);

        return StringUtils.isEmpty(value) ? null : new ResourceType(value);
    }

    /**
     * Invalidate cached configuration.
     * 
     * @param wikiId the wiki for which to invalidate the cached filters
     */
    public void invalidate(String wikiId)
    {
        if (this.xcontextProvider.get().isMainWiki(wikiId)) {
            this.cache.removeAll();
        } else {
            this.cache.remove(wikiId);
        }
    }
}
