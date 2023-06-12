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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.internal.URLNormalizerFilterClassInitializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

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
        "org.xwiki.contrib.urlnormalizer.internal.filter.URLNormalizerConfigurationInvalidationListener";

    @Inject
    private URLNormalizerConfigurationStore store;

    /**
     * Default constructor.
     */
    public URLNormalizerConfigurationInvalidationListener()
    {
        super(NAME, new WikiDeletedEvent(),
            new XObjectAddedEvent(BaseObjectReference.any(URLNormalizerFilterClassInitializer.CLASS_FULLNAME)),
            new XObjectUpdatedEvent(BaseObjectReference.any(URLNormalizerFilterClassInitializer.CLASS_FULLNAME)),
            new XObjectDeletedEvent(BaseObjectReference.any(URLNormalizerFilterClassInitializer.CLASS_FULLNAME)));
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
