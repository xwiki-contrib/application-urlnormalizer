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

import java.io.Reader;
import java.util.Collections;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroManager;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * validate {@link MacroXDOMNormalizer}.
 *
 * @version $Id$
 */
@ComponentTest
class MacroXDOMNormalizerTest
{
    @MockComponent
    @Named("link")
    private XDOMNormalizer linkNormalizer;

    @MockComponent
    private MacroManager macroManager;

    @InjectMockComponents
    private MacroXDOMNormalizer macroNormalizer;

    @Test
    void normalizeWhenNoMacros() throws Exception
    {
        assertFalse(this.macroNormalizer.normalize(new XDOM(Collections.emptyList()), null, null));
    }

    @Test
    void normalizeWithMacroAndNoLinks() throws Exception
    {
        XDOM xdom =
            new XDOM(Collections.singletonList(new MacroBlock("info", Collections.emptyMap(), "content", false)));

        Parser parser = mock(Parser.class);
        XDOM macroXDOM = new XDOM(Collections.singletonList(new WordBlock("content")));
        when(parser.parse(any(Reader.class))).thenReturn(macroXDOM);

        assertFalse(this.macroNormalizer.normalize(xdom, parser, null));
    }

    @Test
    void normalizeWithSupportedMacroAndLink() throws Exception
    {
        MarkupContainingMacroBlockMatcherTest.mockMacro(this.macroManager, "info", true);

        XDOM xdom =
            new XDOM(Collections.singletonList(new MacroBlock("info", Collections.emptyMap(), "content", false)));

        Parser parser = mock(Parser.class);
        when(parser.getSyntax()).thenReturn(MarkupContainingMacroBlockMatcherTest.SYNTAX);
        XDOM macroXDOM = new XDOM(Collections.emptyList());
        when(parser.parse(any(Reader.class))).thenReturn(macroXDOM);

        BlockRenderer blockRenderer = mock(BlockRenderer.class);
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                ((DefaultWikiPrinter) invocation.getArguments()[1]).print("normalizedMacroContent");
                return null;
            }
        }).when(blockRenderer).render(any(Block.class), any(WikiPrinter.class));

        when(this.linkNormalizer.normalize(any(XDOM.class), any(Parser.class), any(BlockRenderer.class)))
            .thenReturn(true);

        assertTrue(this.macroNormalizer.normalize(xdom, parser, blockRenderer));
        MacroBlock resultMacroBlock = (MacroBlock) xdom.getChildren().get(0);
        assertEquals("normalizedMacroContent", resultMacroBlock.getContent());
    }
}
