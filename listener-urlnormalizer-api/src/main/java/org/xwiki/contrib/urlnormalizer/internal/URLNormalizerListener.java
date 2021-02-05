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
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
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
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    @Named("content")
    private DocumentNormalizer contentNormalizer;

    @Inject
    @Named("object/modified")
    private DocumentNormalizer modifiedObjectNormalizer;

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
        XWikiContext context = (XWikiContext) data;

        this.logger.debug("Normalizing local URLs for [{}]...", document.getDocumentReference());

        // For performance persons, we check early and only perform processing if there's a parser and renderer for the
        // syntax of the document that was modified as otherwise we won't be able to find links and normalize them.
        if (this.componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())
            && this.componentManager.hasComponent(Parser.class, document.getSyntax().toIdString()))
        {
            try {
                // Retrieve the parser and the renderer that should be used in order to normalize XProperty contents
                // and Macro contents.
                Parser parser = this.componentManager.getInstance(Parser.class, document.getSyntax().toIdString());
                BlockRenderer blockRenderer = this.componentManager.getInstance(BlockRenderer.class,
                    document.getSyntax().toIdString());

                contentNormalizer.normalize(document, parser, blockRenderer);
                modifiedObjectNormalizer.normalize(document, parser, blockRenderer);
            } catch (ComponentLookupException | NormalizationException e) {
                this.logger.warn("Unable to normalize URLs for document [{}]. Root error [{}]",
                    document.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
