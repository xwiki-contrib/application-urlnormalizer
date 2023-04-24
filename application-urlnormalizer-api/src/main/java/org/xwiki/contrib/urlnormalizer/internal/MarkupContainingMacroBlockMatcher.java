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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Filter macros which contains wiki syntax content.
 *
 * @version $Id$
 * @since 1.3
 */
public class MarkupContainingMacroBlockMatcher extends ClassBlockMatcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkupContainingMacroBlockMatcher.class);

    private static final MetadataBlockMatcher SYNTAX_MATCHER = new MetadataBlockMatcher(MetaData.SYNTAX);

    private final MacroManager macroManager;

    private final Syntax defaultSyntax;

    /**
     * @param macroManager the component to search for macro descriptors
     * @param defaultSyntax the default syntax of the content
     * @since 1.5
     */
    public MarkupContainingMacroBlockMatcher(MacroManager macroManager, Syntax defaultSyntax)
    {
        super(MacroBlock.class);

        this.macroManager = macroManager;
        this.defaultSyntax = defaultSyntax;
    }

    private Syntax getSyntax(MacroBlock macro)
    {
        Syntax currentSyntax = this.defaultSyntax;

        MetaDataBlock metaDataBlock = macro.getFirstBlock(SYNTAX_MATCHER, Axes.ANCESTOR_OR_SELF);

        if (metaDataBlock != null) {
            currentSyntax = (Syntax) metaDataBlock.getMetaData().getMetaData(MetaData.SYNTAX);
        }

        return currentSyntax;
    }

    private Macro<?> getMacro(MacroBlock macroBlock)
    {
        MacroId macroId = new MacroId(macroBlock.getId(), getSyntax(macroBlock));
        try {
            return this.macroManager.getMacro(macroId);
        } catch (MacroLookupException e) {
            // if the macro cannot be found or instantiated we shouldn't raise an exception, just ignore that macro.
            LOGGER.debug("Cannot get macro with id [{}]: [{}]", macroId, ExceptionUtils.getRootCauseMessage(e));

            return null;
        }
    }

    private boolean shouldMacroContentBeParsed(MacroBlock macroBlock)
    {
        Macro<?> macro = getMacro(macroBlock);

        if (macro != null) {
            ContentDescriptor contentDescriptor = macro.getDescriptor().getContentDescriptor();

            return contentDescriptor != null && Block.LIST_BLOCK_TYPE.equals(contentDescriptor.getType());
        }

        return false;
    }

    @Override
    public boolean match(Block block)
    {
        boolean match = false;

        if (super.match(block)) {
            MacroBlock macroBlock = (MacroBlock) block;
            if (shouldMacroContentBeParsed(macroBlock)) {
                // Macro with content of type wiki
                match = true;
            } else if (macroBlock.getId().equals("html") && Boolean.parseBoolean(macroBlock.getParameter("wiki"))) {
                // HTML macro with enabled wiki content
                match = true;
            }
        }

        return match;
    }
}
