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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContentDocumentNormalizer}.
 *
 * @version $Id$
 * @since 1.4
 */
public class ContentDocumentNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<ContentDocumentNormalizer> mocker =
        new MockitoComponentMockingRule<>(ContentDocumentNormalizer.class);


    private BlockRenderer blockRenderer;

    private Parser parser;

    private XDOMNormalizer linkXDOMNormalizer;

    private XDOMNormalizer macroXDOMNormalizer;

    @Before
    public void setUp() throws Exception
    {
        this.parser = this.mocker.registerMockComponent(Parser.class, Syntax.XWIKI_2_1.toIdString());
        this.blockRenderer = this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());
        this.linkXDOMNormalizer = this.mocker.registerMockComponent(XDOMNormalizer.class, "link");
        this.macroXDOMNormalizer = this.mocker.registerMockComponent(XDOMNormalizer.class, "macro");
    }

    @Test
    public void normalizeWithNoLinksInContent() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        this.mocker.getComponentUnderTest().normalize(fakeDocument, parser, blockRenderer);

        verify(fakeDocument, never()).setContent(any(XDOM.class));
        assertEquals(0, fakeDocument.getXDOM().getBlocks(
            new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF).size());
    }

    @Test
    public void normalizeWithOneNormalizedLinkInContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void normalizeWithOneNormalizedLinkInMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void normalizeWithNormalizedLinksInContentAndMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);
        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }
}
