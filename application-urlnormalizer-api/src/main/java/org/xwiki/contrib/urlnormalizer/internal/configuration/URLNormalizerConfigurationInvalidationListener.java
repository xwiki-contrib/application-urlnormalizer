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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Listener in charge of invalidating the configuration cache.
 * 
 * @version $Id$
 * @since 1.6
 */
@Component
@Named(URLNormalizerConfigurationInvalidationListener.NAME)
@Singleton
public class URLNormalizerConfigurationInvalidationListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME =
        "org.xwiki.contrib.urlnormalizer.internal.configuration.URLNormalizerConfigurationInvalidationListener";

    private static final String DOCUMENT_REGEX = ".*:" + AbstractURLNormalizerClassInitializer.URLNORMALIZER_SPACE_NAME
        + '.' + AbstractURLNormalizerClassInitializer.CODE_SPACE_NAME + '.'
        + URLNormalizerConfigurationStore.CONFIGURATION_NAME;

    private static final RegexEventFilter DOCUMENT_REGEX_FILTER = new RegexEventFilter(DOCUMENT_REGEX);

    @Inject
    private URLNormalizerConfigurationStore store;

    /**
     * Default constructor.
     */
    public URLNormalizerConfigurationInvalidationListener()
    {
        super(NAME, new WikiDeletedEvent(), new DocumentCreatedEvent(DOCUMENT_REGEX_FILTER),
            new DocumentUpdatedEvent(DOCUMENT_REGEX_FILTER), new DocumentDeletedEvent(DOCUMENT_REGEX_FILTER));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            this.store.invalidate(((WikiDeletedEvent) event).getWikiId());
        } else {
            XWikiDocument document = (XWikiDocument) source;

            this.store.invalidate(document.getDocumentReference().getWikiReference().getName());
        }
    }
}
