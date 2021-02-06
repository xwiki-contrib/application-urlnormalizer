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

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Transform local links found in some Macros in the passed XDOM into wiki links. See
 * {@link MarkupContainingMacroBlockMatcher} for the list of supported Macros.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("macro")
@Singleton
public class MacroXDOMNormalizer implements XDOMNormalizer
{
    @Inject
    private Logger logger;

    @Inject
    @Named("link")
    private XDOMNormalizer linkXDOMNormalizer;

    @Override
    public boolean normalize(XDOM xdom, Parser parser, BlockRenderer blockRenderer)
    {
        List<MacroBlock> macroBlocks =
            xdom.getBlocks(new MarkupContainingMacroBlockMatcher(), Block.Axes.DESCENDANT_OR_SELF);

        if (macroBlocks.size() > 0) {
            return normalize(macroBlocks, parser, blockRenderer);
        }

        return false;
    }

    private boolean normalize(List<MacroBlock> macroBlocks, Parser parser, BlockRenderer blockRenderer)
    {
        boolean modifiedLinks = false;

        for (int i = 0; i < macroBlocks.size(); i++) {
            MacroBlock macroBlock = macroBlocks.get(i);
            String content = macroBlock.getContent();
            try {
                XDOM xdom = parser.parse(new StringReader(content));

                boolean modified = this.linkXDOMNormalizer.normalize(xdom, parser, blockRenderer);
                if (modified) {
                    WikiPrinter wikiPrinter = new DefaultWikiPrinter();
                    blockRenderer.render(xdom, wikiPrinter);

                    String normalizedContent = wikiPrinter.toString();

                    // Create a new MacroBlock with the normalized content
                    MacroBlock newMacroBlock = new MacroBlock(macroBlock.getId(), macroBlock.getParameters(),
                        normalizedContent, macroBlock.isInline());

                    // Replace the MacroBlock in the XDOM
                    macroBlock.getParent().replaceChild(newMacroBlock, macroBlock);

                    // Update the list given in parameter
                    macroBlocks.set(i, newMacroBlock);

                    modifiedLinks = true;
                }
            } catch (ParseException e) {
                // The parser for the syntax of the document may not fit the syntax used in a Macro.
                // Since this shouldn't happen we need to log an error. It means there's an important problem
                this.logger.error("Failed to normalize URLs in content of Macro [{}]", macroBlock.getId(), e);
            }
        }

        return modifiedLinks;
    }
}
