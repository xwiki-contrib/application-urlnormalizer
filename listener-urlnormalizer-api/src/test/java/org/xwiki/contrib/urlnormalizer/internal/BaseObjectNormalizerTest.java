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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.XDOMBuilder;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link BaseObjectNormalizer}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public class BaseObjectNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<BaseObjectNormalizer> mocker =
            new MockitoComponentMockingRule<>(BaseObjectNormalizer.class);

    private LinkBlockNormalizer linkBlockNormalizer;

    private Parser parser;

    private BlockRenderer blockRenderer;

    @Before
    public void setUp() throws Exception
    {
        linkBlockNormalizer = mocker.registerMockComponent(LinkBlockNormalizer.class);

        parser = mock(Parser.class);

        blockRenderer = mock(BlockRenderer.class);
    }

    /**
     * Create a new {@link XDOM} by introducing a given list of {@link LinkBlock} in it.
     *
     * @param linkBlocks the blocks to use
     * @return the result XDOM
     */
    private XDOM mockXDOM(List<LinkBlock> linkBlocks)
    {
        XDOMBuilder builder = new XDOMBuilder();

        // Add the LinkBlocks to a new XDOM
        for (LinkBlock linkBlock : linkBlocks) {
            builder.addBlock(new ParagraphBlock(Arrays.asList(linkBlock)));
        }

        return builder.getXDOM();
    }

    private BaseObject mockBaseObject(String propType, PropertyClass property, List<LinkBlock> linkBlocks)
            throws Exception
    {
        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getLargeStringValue("Mocked prop name")).thenReturn("Content");

        XDOM xdom = mockXDOM(linkBlocks);

        when(parser.parse(any(StringReader.class))).thenReturn(xdom);

        return baseObject;
    }

    @Test
    public void normalizeBaseObjectWithTwoLinks() throws Exception
    {
        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("FullyRenderedText");

        List<LinkBlock> linkBlocks = Arrays.asList(mock(LinkBlock.class), mock(LinkBlock.class));

        BaseObject bo = mockBaseObject("TextArea", property, linkBlocks);

        mocker.getComponentUnderTest().normalizeBaseObject(bo, "Mocked prop name", parser, blockRenderer);

        verify(linkBlockNormalizer, times(1)).normalizeLinkBlocks(linkBlocks);
    }

    @Test
    public void normalizeBaseObjectWithNoLinks() throws Exception
    {
        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("FullyRenderedText");

        List<LinkBlock> linkBlocks = Collections.EMPTY_LIST;

        BaseObject bo = mockBaseObject("TextArea", property, linkBlocks);

        mocker.getComponentUnderTest().normalizeBaseObject(bo, "Mocked prop name", parser, blockRenderer);

        verify(linkBlockNormalizer, never()).normalizeLinkBlocks(linkBlocks);
    }
}
