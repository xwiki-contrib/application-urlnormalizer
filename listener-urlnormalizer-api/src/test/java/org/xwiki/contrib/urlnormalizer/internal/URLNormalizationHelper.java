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

import java.util.Arrays;
import java.util.List;

import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMBuilder;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class URLNormalizationHelper
{
    /**
     * Create a new {@link XDOM} by introducing a given list of {@link LinkBlock} in it.
     *
     * @param linkBlocks the blocks to use
     * @return the result XDOM
     */
    public static XDOM mockXDOM(List<LinkBlock> linkBlocks)
    {
        XDOMBuilder builder = new XDOMBuilder();

        // Add the LinkBlocks to a new XDOM
        for (LinkBlock linkBlock : linkBlocks) {
            builder.addBlock(new ParagraphBlock(Arrays.asList(linkBlock)));
        }

        return builder.getXDOM();
    }

    /**
     * Mock a new {@link XWikiDocument} to return a given {@link XDOM} object on {@link XWikiDocument#getXDOM()}.
     *
     * @param xdom the {@link XDOM} object to use in the mock
     * @return the mocked document
     */
    public static XWikiDocument mockXWikiDocument(XDOM xdom)
    {
        XWikiDocument fakeDocument = mock(XWikiDocument.class);
        when(fakeDocument.getXDOM()).thenReturn(xdom);
        when(fakeDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        return fakeDocument;
    }
}
