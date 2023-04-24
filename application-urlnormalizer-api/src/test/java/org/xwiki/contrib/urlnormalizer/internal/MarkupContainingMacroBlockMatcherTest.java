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

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.MacroLookupException;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.syntax.Syntax;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link MarkupContainingMacroBlockMatcher}.
 *
 * @version $Id$
 */
class MarkupContainingMacroBlockMatcherTest
{
    public static final Syntax SYNTAX = Syntax.XWIKI_2_1;

    private MacroManager macroManager = mock(MacroManager.class);

    public static void mockMacro(MacroManager macroManager, String macroName, boolean wiki) throws MacroLookupException
    {
        mockMacro(macroManager, macroName, wiki, wiki);
    }

    public static void mockMacro(MacroManager macroManager, String macroName, boolean content, boolean wiki)
        throws MacroLookupException
    {
        Macro macro = mock(Macro.class);
        when(macroManager.getMacro(new MacroId(macroName, SYNTAX))).thenReturn(macro);
        MacroDescriptor macroDescriptor = mock(MacroDescriptor.class);
        when(macro.getDescriptor()).thenReturn(macroDescriptor);

        if (content) {
            ContentDescriptor contentDescriptor = mock(ContentDescriptor.class);
            when(macroDescriptor.getContentDescriptor()).thenReturn(contentDescriptor);
            when(contentDescriptor.getType()).thenReturn(wiki ? Block.LIST_BLOCK_TYPE : String.class);
        }
    }

    @Test
    void matchNonMacroBlock()
    {
        assertFalse(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX).match(new WordBlock("whatever")));
    }

    @Test
    void matchNotExistMacro()
    {
        assertFalse(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("notexist", Collections.emptyMap(), false)));
    }

    @Test
    void matchNoContentMacro() throws MacroLookupException
    {
        mockMacro(macroManager, "nocontent", false);

        assertFalse(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("nocontent", Collections.emptyMap(), false)));
    }

    @Test
    void matchNotWikiContentMacro() throws MacroLookupException
    {
        mockMacro(macroManager, "notwiki", true, false);

        assertFalse(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("notwiki", Collections.emptyMap(), false)));
    }

    @Test
    void matchWikiContentMacro() throws MacroLookupException
    {
        mockMacro(macroManager, "wiki", true);

        assertTrue(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("wiki", Collections.emptyMap(), false)));
    }

    @Test
    void matchHTMLMacroWhenNotContainingWikiSyntax()
    {
        assertFalse(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("html", Collections.emptyMap(), false)));
    }

    @Test
    void matchHTMLMacroWhenContainingWikiSyntax()
    {
        assertTrue(new MarkupContainingMacroBlockMatcher(this.macroManager, SYNTAX)
            .match(new MacroBlock("html", Collections.singletonMap("wiki", "true"), false)));
    }
}
