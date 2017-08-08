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

import java.util.List;

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
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * This listener is used to normalize document content and document XObjects.
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
    private LinkBlockNormalizer linkBlockNormalizer;

    @Inject
    private BaseObjectNormalizer baseObjectNormalizer;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

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

        try {
            normalizeDocumentContent(document);
            normalizeDocumentXObjects(document, context);
        } catch (XWikiException | ComponentLookupException e) {
            logger.warn("Unable to normalize URLs for document [{}]. Root error [{}]", document.getTitle(),
                    ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * Normalize the TextArea fields in the last modified XObjects of a given document.
     *
     * @param document the document to normalize
     * @param context the current {@link XWikiContext}
     * @throws ComponentLookupException if an error happens while fetching a {@link Parser} or a {@link BlockRenderer}
     * from the component manager
     */
    private void normalizeDocumentXObjects(XWikiDocument document, XWikiContext context) throws ComponentLookupException
    {
        /**
         * Just as what is done in {@link #normalizeDocumentContent(XWikiDocument)}, we have to check if a
         * {@link org.xwiki.rendering.parser.Parser} and a {@link BlockRenderer} exist for the given syntax.
         */
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())
                && componentManager.hasComponent(Parser.class, document.getSyntax().toIdString())) {

            // Retrieve the parser and the renderer that should be used in order to normalize XProperty contents
            Parser parser = componentManager.getInstance(Parser.class, document.getSyntax().toIdString());
            BlockRenderer blockRenderer = componentManager.getInstance(BlockRenderer.class,
                    document.getSyntax().toIdString());

            // Get last diff of the document objects
            List<List<ObjectDiff>> documentObjectDiff =
                    document.getObjectDiff(document.getOriginalDocument(), document, context);

            // Go through every object that has been modified
            for (List<ObjectDiff> objectDiffs : documentObjectDiff) {
                // Go through every property modified
                for (ObjectDiff objectDiff : objectDiffs) {
                    if (objectDiff.getPropType().equals("TextArea")) {
                        BaseObject object =
                                document.getXObject(objectDiff.getXClassReference(), objectDiff.getNumber());
                        TextAreaClass property = (TextAreaClass) object.getField(objectDiff.getPropName());

                        /**
                         * As {@link TextAreaClass#getContentType()} returns a String (and not directly a
                         * {@link TextAreaClass.ContentType}), we have no other choice than using
                         * {@link TextAreaClass.ContentType.WIKI_TEXT.toString()}.
                         */
                        if (property.getContentType().equals(TextAreaClass.ContentType.WIKI_TEXT.toString())) {
                            baseObjectNormalizer.normalizeBaseObject(object, objectDiff.getPropName(),
                                    parser, blockRenderer);
                        }
                    }
                }
            }
        }
    }

    /**
     * Normalize the content of the given document.
     *
     * @param document the {@link XWikiDocument} to normalize
     * @throws XWikiException if the document could not be updated
     */
    private void normalizeDocumentContent(XWikiDocument document) throws XWikiException
    {
        /**
         * Ensure that we have a correct renderer for the syntax of the given document. If no renderer
         * is found, {@link XWikiDocument#setContent(XDOM)} will fail.
         */
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())) {
            XDOM xdom = document.getXDOM();

            List<LinkBlock> linkBlocks = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                        Block.Axes.DESCENDANT_OR_SELF);

            if (linkBlocks.size() > 0) {
                linkBlockNormalizer.normalizeLinkBlocks(linkBlocks);

                document.setContent(xdom);
            }
        }
    }
}
