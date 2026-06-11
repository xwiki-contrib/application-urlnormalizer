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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
@ComponentTest
class ContentDocumentNormalizerTest
{
    @InjectMockComponents
    private ContentDocumentNormalizer normalizer;

    @MockComponent
    @Named("link")
    private XDOMNormalizer linkXDOMNormalizer;

    @MockComponent
    @Named("image")
    private XDOMNormalizer imageXDOMNormalizer;

    @MockComponent
    @Named("macro")
    private XDOMNormalizer macroXDOMNormalizer;

    private final Parser parser = mock(Parser.class);

    private final BlockRenderer blockRenderer = mock(BlockRenderer.class);

    @Test
    void normalizeWithNoLinksInContent() throws Exception
    {
        XDOM xdom = new XDOM(List.of());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        this.normalizer.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, never()).setContent(any(XDOM.class));
        assertEquals(0, fakeDocument.getXDOM().getBlocks(
            new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF).size());
    }

    @Test
    void normalizeWithOneNormalizedLinkInContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(List.of());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.normalizer.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    void normalizeWithOneNormalizedLinkInMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(List.of());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.normalizer.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    void normalizeWithNormalizedLinksInContentAndMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(List.of());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);
        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.normalizer.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }
}
