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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.parser.XDOMBuilder;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link URLNormalizerListener}.
 *
 * @version $Id$
 * @since 1.0
 */
public class URLNormalizerListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<URLNormalizerListener> mocker =
            new MockitoComponentMockingRule<>(URLNormalizerListener.class);

    private ResourceReferenceNormalizer resourceReferenceNormalizer;

    private ComponentManager componentManager;

    private ResourceReference fakeRef1;

    private ResourceReference fakeRef2;

    @Before
    public void setUp() throws Exception
    {
        resourceReferenceNormalizer = mocker.registerMockComponent(ResourceReferenceNormalizer.class);

        componentManager = mocker.registerMockComponent(ComponentManager.class);
        when(componentManager.hasComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString())).thenReturn(true);

        fakeRef1 = mock(ResourceReference.class);

        fakeRef2 = mock(ResourceReference.class);
    }

    /**
     * Mock a new {@link XWikiDocument} to return a given {@link XDOM} object on {@link XWikiDocument#getXDOM()}.
     *
     * @param xdom the {@link XDOM} object to use in the mock
     */
    private XWikiDocument mockXWikiDocument(XDOM xdom) {
        XWikiDocument fakeDocument = mock(XWikiDocument.class);
        when(fakeDocument.getXDOM()).thenReturn(xdom);
        when(fakeDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        return fakeDocument;
    }

    private XDOM mockXDOM(List<ResourceReference> resourceReferences) {
        XDOMBuilder builder = new XDOMBuilder();

        // Create the LinkBlocks corresponding to the ResourceReference
        List<LinkBlock> linkBlocks = new ArrayList<>();
        for (ResourceReference resourceReference : resourceReferences) {
            linkBlocks.add(new LinkBlock(
                    Collections.EMPTY_LIST, resourceReference, true, Collections.EMPTY_MAP));
        }

        // Add the LinkBlocks to a new XDOM
        for (LinkBlock linkBlock : linkBlocks) {
            builder.addBlock(new ParagraphBlock(Arrays.asList(linkBlock)));
        }

        return builder.getXDOM();
    }

    @Test
    public void testWithNoLink() throws Exception
    {
        XDOM xdom = mock(XDOM.class);
        when(xdom.getBlocks(any(ClassBlockMatcher.class), eq(Block.Axes.DESCENDANT_OR_SELF)))
                .thenReturn(Collections.emptyList());

        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, never()).setContent(any(XDOM.class));
        assertEquals(
                xdom.getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF),
                fakeDocument.getXDOM().getBlocks(new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF)
        );
    }

    @Test
    public void testWithOneLink() throws Exception
    {
        ResourceReference originalReference = mock(ResourceReference.class);

        XDOM xdom = mockXDOM(Arrays.asList(originalReference));
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

}
