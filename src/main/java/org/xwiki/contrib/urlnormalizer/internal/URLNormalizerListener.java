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

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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
        normalizeDocumentContent(document);

        // TODO: Use {@link XWikiDocument#getObjectDiff()}

    }

    /**
     * Normalize the content of the given document.
     * @param document the {@link XWikiDocument} to normalize
     */
    private void normalizeDocumentContent(XWikiDocument document)
    {
        XDOM xdom = document.getXDOM();

        /**
         * Ensure that we have a correct renderer for the syntax of the given document. If no renderer
         * is found, {@link XWikiDocument#setContent(XDOM)} will fail.
         */
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())) {
            try {
                List<LinkBlock> linkBlocks = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                        Block.Axes.DESCENDANT_OR_SELF);

                if (linkBlocks.size() > 0) {
                    normalizeLinkBlocks(linkBlocks);

                    document.setContent(xdom);
                }
            } catch (XWikiException e) {
                logger.warn(String.format("Unable to normalize URLs for document [%s]", document.getTitle()));
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

}
