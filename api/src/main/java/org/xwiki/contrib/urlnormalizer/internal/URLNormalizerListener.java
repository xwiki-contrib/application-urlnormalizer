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

import java.util.Arrays;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.contrib.urlnormalizer.URLNormalizationManager;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Normalize links found in document content and document XObjects.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Named(URLNormalizerListener.NAME)
public class URLNormalizerListener extends AbstractEventListener
{
    /**
     * The listener name.
     */
    static final String NAME = "URLNormalizer";

    @Inject
    private Logger logger;

    @Inject
    private URLNormalizationManager urlNormalizationManager;

    /**
     * Builds a new {@link URLNormalizerListener}.
     */
    public URLNormalizerListener()
    {
        super(NAME, new DocumentCreatingEvent(), new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        try {
            urlNormalizationManager.normalize(document, Arrays.asList(ContentDocumentNormalizer.HINT,
                ModifiedObjectDocumentNormalizer.HINT));
        } catch (NormalizationException e) {
            this.logger.warn("Unable to normalize URLs for document [{}]. Root error [{}]",
                document.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
