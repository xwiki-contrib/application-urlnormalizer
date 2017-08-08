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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Normalize {@link BaseObject} and interfaces with {@link LinkBlockNormalizer}.
 *
 * @version $Id$
 * @since 1.2
 */
@Component(roles = BaseObjectNormalizer.class)
@Singleton
public class BaseObjectNormalizer
{
    @Inject
    private LinkBlockNormalizer linkBlockNormalizer;

    /**
     * Normalize the content of the given {@link BaseObject}.
     * We assume that {@link BaseObject#getLargeStringValue(String)} will work on this {@link BaseObject}.
     *
     * @param baseObject the {@link BaseObject} to normalize
     * @param propertyName the name of the XProperty
     * @param parser the parser to use
     * @param blockRenderer the renderer to use
     */
    public void normalizeBaseObject(BaseObject baseObject, String propertyName, Parser parser,
            BlockRenderer blockRenderer) {
        // Get the content of the given XProperty
        String content = baseObject.getLargeStringValue(propertyName);

        try {
            XDOM xdom = parser.parse(new StringReader(content));

            List<LinkBlock> linkBlocks = xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class),
                    Block.Axes.DESCENDANT_OR_SELF);

            if (linkBlocks.size() > 0) {
                linkBlockNormalizer.normalizeLinkBlocks(linkBlocks);

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
