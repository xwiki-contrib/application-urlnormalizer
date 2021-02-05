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

import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.contrib.urlnormalizer.XDOMNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MacroXDOMNormalizer}.
 *
 * @since 1.3
 * @version $Id$
 */
public class MacroXDOMNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<MacroXDOMNormalizer> mocker =
        new MockitoComponentMockingRule<>(MacroXDOMNormalizer.class);

    @Test
    public void normalizeWhenNoMacros() throws Exception
    {
        assertFalse(this.mocker.getComponentUnderTest().normalize(new XDOM(Collections.emptyList()), null, null));
    }

    @Test
    public void normalizeWithMacroAndNoLinks() throws Exception
    {
        XDOM xdom = new XDOM(Collections.singletonList(
            new MacroBlock("info", Collections.emptyMap(), "content", false)));

        Parser parser = mock(Parser.class);
        XDOM macroXDOM = new XDOM(Collections.singletonList(new WordBlock("content")));
        when(parser.parse(any(Reader.class))).thenReturn(macroXDOM);

        assertFalse(this.mocker.getComponentUnderTest().normalize(xdom, parser, null));
    }

    @Test
    public void normalizeWithSupportedMacroAndLink() throws Exception
    {
        XDOM xdom = new XDOM(Collections.singletonList(
            new MacroBlock("info", Collections.emptyMap(), "content", false)));

        Parser parser = mock(Parser.class);
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

        XDOMNormalizer linkBlockNormalizer = this.mocker.getInstance(XDOMNormalizer.class, "link");
        when(linkBlockNormalizer.normalize(
            any(XDOM.class), any(Parser.class), any(BlockRenderer.class))).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().normalize(xdom, parser, blockRenderer));
        MacroBlock resultMacroBlock = (MacroBlock) xdom.getChildren().get(0);
        assertEquals("normalizedMacroContent", resultMacroBlock.getContent());
    }
}
