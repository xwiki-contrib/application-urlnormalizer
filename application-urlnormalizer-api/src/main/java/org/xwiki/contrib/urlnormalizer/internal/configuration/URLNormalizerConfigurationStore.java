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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.contrib.urlnormalizer.URLNormalizerFilter;
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
public class URLNormalizerConfigurationStore implements Initializable, Disposable
{
    /**
     * The name of the page containing the wiki configuration of the normalizer.
     */
    public static final String CONFIGURATION_NAME = "Configuration";

    /**
     * The local reference of the page containing the wiki configuration of the normalizer.
     */
    private static final LocalDocumentReference CONFIGURATION_REFERENCE = new LocalDocumentReference(CONFIGURATION_NAME,
        new EntityReference(URLNormalizerFilterClassInitializer.CODE_SPACE_NAME, EntityType.SPACE,
            new EntityReference(URLNormalizerFilterClassInitializer.URLNORMALIZER_SPACE_NAME, EntityType.SPACE)));

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private Logger logger;

    private Cache<CacheEntry> cache;

    private class CacheEntry
    {
        private final boolean enabled;

        private final List<URLNormalizerFilter> writeFilters;

        private final List<URLNormalizerFilter> readFilters;

        CacheEntry(boolean enabled, List<URLNormalizerFilter> filters)
        {
            this.enabled = enabled;
            this.writeFilters = filters;
            this.readFilters = Collections.unmodifiableList(this.writeFilters);
        }
    }

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
     * @param wiki the wiki for which to get the configuration
     * @return true if the normalization is globally enabled
     * @throws NormalizationException when failing to load the configuration
     */
    public boolean isEnabled(WikiReference wiki) throws NormalizationException
    {
        return getConfiguration(wiki).enabled;
    }

    /**
     * @param wiki the wiki for which to get the configuration
     * @return the filters
     * @throws NormalizationException when failing to load the configuration
     */
    public List<URLNormalizerFilter> getFilters(WikiReference wiki) throws NormalizationException
    {
        return getConfiguration(wiki).readFilters;
    }

    /**
     * @param wiki the identifier of wiki
     * @return the configuration for the passed wiki
     * @throws NormalizationException when failing to load the configuration
     */
    private CacheEntry getConfiguration(WikiReference wiki) throws NormalizationException
    {
        CacheEntry entry = this.cache.get(wiki.getName());

        if (entry == null) {
            try {
                entry = loadConfiguration(wiki);

                this.cache.set(wiki.getName(), entry);
            } catch (Exception e) {
                throw new NormalizationException(
                    "Failed to load the normalization configuration for wiki [" + wiki.getName() + "]", e);
            }
        }

        return entry;
    }

    private CacheEntry loadConfiguration(WikiReference wiki) throws XWikiException, NormalizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        // Load current wiki filters
        CacheEntry configuration = loadConfiguration(wiki, xcontext);

        // Load main wiki filters if not already in main wiki
        if (!xcontext.isMainWiki(wiki.getName())) {
            CacheEntry mainConfiguration = getConfiguration(new WikiReference(xcontext.getMainXWiki()));
            configuration.writeFilters.addAll(mainConfiguration.writeFilters);
        }

        return configuration;
    }

    private CacheEntry loadConfiguration(WikiReference wikiReference, XWikiContext xcontext)
        throws XWikiException, NormalizationException
    {
        XWikiDocument filtersDocument =
            xcontext.getWiki().getDocument(new DocumentReference(CONFIGURATION_REFERENCE, wikiReference), xcontext);

        // Enabled/disabled

        BaseObject xobject = filtersDocument.getXObject(URLNormalizerConfigurationClassInitializer.CLASS_REFERENCE);

        // Resolve the default value (enabled for main wiki and whatever is the main wiki value otherwise)
        int defaultValue;
        if (xcontext.isMainWiki(wikiReference.getName())) {
            defaultValue = 1;
        } else {
            defaultValue = isEnabled(new WikiReference(xcontext.getMainXWiki())) ? 1 : 0;
        }

        // Get the value stored in the wiki
        int enabledValue = xobject != null
            ? xobject.getIntValue(URLNormalizerConfigurationClassInitializer.FIELD_ENABLED, defaultValue)
            : defaultValue;

        // Convert the value into a boolean
        boolean enabled = enabledValue != 0;

        // Filters

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

        return new CacheEntry(enabled, filters);
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

    @Override
    public void dispose()
    {
        this.cache.dispose();
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
