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

import java.io.StringReader;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;

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
    public static final String NAME = "URLNormalizer";

    @Inject
    private ResourceReferenceNormalizer resourceReferenceNormalizer;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Builds a new {@link URLNormalizerListener}.
     */
    public URLNormalizerListener()
    {
        super(NAME, new DocumentUpdatingEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        normalizeDocumentContent(document);
        normalizeDocumentXObjects(document, context);
    }


    private void normalizeDocumentXObjects(XWikiDocument document, XWikiContext context)
    {
        // Get last diff of the document objects
        List<List<ObjectDiff>> documentObjectDiff =
                document.getObjectDiff(document.getOriginalDocument(), document, context);

        /**
         * Just as what is done in {@link #normalizeDocumentContent(XWikiDocument)}, we have to check if a
         * {@link org.xwiki.rendering.parser.Parser} and a {@link BlockRenderer} exist for the given syntax.
         */
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())
                && componentManager.hasComponent(Parser.class, document.getSyntax().toIdString())) {

            try {
                // Retrieve the parser and the renderer that should be used in order to normalize XProperty contents
                Parser parser = componentManager.getInstance(Parser.class, document.getSyntax().toIdString());
                BlockRenderer blockRenderer = componentManager.getInstance(BlockRenderer.class,
                        document.getSyntax().toIdString());

                // Go through every object that has been modified
                for (List<ObjectDiff> objectDiffs : documentObjectDiff) {
                    // Go through every property modified
                    for (ObjectDiff objectDiff : objectDiffs) {
                        // We only normalize TextArea properties
                        if (objectDiff.getPropType().equals("TextArea")) {
                            BaseObject object =
                                    document.getXObject(objectDiff.getXClassReference(), objectDiff.getNumber());

                            normalizeBaseObject(object, objectDiff.getPropName(), parser, blockRenderer);
                        }
                    }
                }
            } catch (ComponentLookupException e) {
            }
        }
    }

    /**
     * Normalize the content of the given document.
     * @param document the {@link XWikiDocument} to normalize
     */
    private void normalizeDocumentContent(XWikiDocument document)
    {
        /**
         * Ensure that we have a correct renderer for the syntax of the given document. If no renderer
         * is found, {@link XWikiDocument#setContent(XDOM)} will fail.
         */
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())) {
            XDOM xdom = document.getXDOM();

            try {
                List<LinkBlock> linkBlocks = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                        Block.Axes.DESCENDANT_OR_SELF);

                if (linkBlocks.size() > 0) {
                    normalizeLinkBlocks(linkBlocks);

                    document.setContent(xdom);
                }
            } catch (XWikiException e) {
                logger.warn("Unable to normalize URLs for document [{}]. Root error [{}]", document.getTitle(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Update the given list of link blocks with normalized URLs.
     * @param linkBlocks a list of URL (link) blocks
     */
    private void normalizeLinkBlocks(List<LinkBlock> linkBlocks)
    {
        for (LinkBlock linkBlock : linkBlocks) {
            ResourceReference newReference = resourceReferenceNormalizer.normalize(linkBlock.getReference());

            // Create a new LinkBlock
            LinkBlock newLinkBlock = new LinkBlock(linkBlock.getChildren(), newReference,
                    linkBlock.isFreeStandingURI(), linkBlock.getParameters());

            // Replace the previous LinkBlock in the XDOM
            linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);
        }
    }

    /**
     * Normalize the content of the given {@link BaseObject}.
     * We assume that {@link BaseObject#getLargeStringValue(String)} will work on this {@link BaseObject}.
     *
     * @param baseObject the {@link BaseObject} to normalize
     * @param propertyName the name of the XProperty
     * @param parser the parser to use
     * @param blockRenderer the renderer to use
     */
    private void normalizeBaseObject(BaseObject baseObject, String propertyName, Parser parser,
            BlockRenderer blockRenderer) {
        // Get the content of the given XProperty
        String content = baseObject.getLargeStringValue(propertyName);

        try {
            XDOM xdom = parser.parse(new StringReader(content));

            List<LinkBlock> linkBlocks = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                    Block.Axes.DESCENDANT_OR_SELF);

            if (linkBlocks.size() > 0) {
                normalizeLinkBlocks(linkBlocks);

                WikiPrinter wikiPrinter = new DefaultWikiPrinter();
                blockRenderer.render(xdom, wikiPrinter);

                String normalizedContent = wikiPrinter.toString();

                baseObject.setLargeStringValue(propertyName, normalizedContent);
            }
        } catch (ParseException e) {
            // The parser for the syntax of the document may no fit the syntax used in a XProperty.
        }
    }

}
