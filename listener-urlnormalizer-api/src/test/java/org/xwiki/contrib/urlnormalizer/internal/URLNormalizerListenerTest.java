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
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.parser.XDOMBuilder;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import static org.junit.Assert.assertEquals;
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

    private ComponentManager componentManager;

    private LinkBlockNormalizer linkBlockNormalizer;

    private BaseObjectNormalizer baseObjectNormalizer;

    // A mock of an XWikiDocument initialized in setUp() that can be used in tests
    private XWikiDocument document;

    // A mock of an XWikiContext initialized in setUp() that can be used in tests
    private XWikiContext context;

    @Before
    public void setUp() throws Exception
    {
        linkBlockNormalizer = mocker.registerMockComponent(LinkBlockNormalizer.class);

        baseObjectNormalizer = mocker.registerMockComponent(BaseObjectNormalizer.class);

        componentManager = mocker.registerMockComponent(ComponentManager.class);
        when(componentManager.hasComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString())).thenReturn(true);
        when(componentManager.hasComponent(Parser.class, Syntax.XWIKI_2_1.toIdString())).thenReturn(true);

        document = mock(XWikiDocument.class);
        when(document.getSyntax()).thenReturn(Syntax.XWIKI_2_1);
        context = mock(XWikiContext.class);
    }

    /**
     * Mock a new {@link XWikiDocument} to return a given {@link XDOM} object on {@link XWikiDocument#getXDOM()}.
     *
     * @param xdom the {@link XDOM} object to use in the mock
     * @return the mocked document
     */
    private XWikiDocument mockXWikiDocument(XDOM xdom)
    {
        XWikiDocument fakeDocument = mock(XWikiDocument.class);
        when(fakeDocument.getXDOM()).thenReturn(xdom);
        when(fakeDocument.getSyntax()).thenReturn(Syntax.XWIKI_2_1);

        return fakeDocument;
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

    /**
     * Associate an {@link ObjectDiff} with a related {@link BaseObject} to the given {@link XWikiDocument} in order to
     * test {@link URLNormalizerListener#normalizeDocumentXObjects(XWikiDocument, XWikiContext)}.
     *
     * @param document the document to mock on
     * @param context a mock of the {@link XWikiContext}
     * @param propType the property type of the XObject ("StaticList", "TextArea", ...)
     * @param property the property that should be pointed by the {@link ObjectDiff}
     * @throws Exception if an error happens
     */
    private void mockDocumentXObject(XWikiDocument document, XWikiContext context, String propType,
            PropertyClass property) throws Exception
    {
        XWikiDocument originalDocument = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(originalDocument);

        // Mock a parser and a renderer
        Parser parser = mock(Parser.class);
        BlockRenderer blockRenderer = mock(BlockRenderer.class);
        when(componentManager.getInstance(Parser.class, Syntax.XWIKI_2_1.toIdString())).thenReturn(parser);
        when(componentManager.getInstance(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString()))
                .thenReturn(blockRenderer);

        DocumentReference baseObjectClass = new DocumentReference("xwiki", "xwiki", "mock");

        // Mock the ObjectDiff that we’ll use
        ObjectDiff diff = mock(ObjectDiff.class);
        when(diff.getPropType()).thenReturn(propType);
        when(diff.getPropName()).thenReturn("Mocked prop name");
        when(diff.getNumber()).thenReturn(0);
        when(diff.getXClassReference()).thenReturn(baseObjectClass);

        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getField("Mocked prop name")).thenReturn(property);

        when(document.getXObject(baseObjectClass, 0)).thenReturn(baseObject);

        when(document.getObjectDiff(originalDocument, document, context)).thenReturn(
                Collections.singletonList(Collections.singletonList(diff)));
    }

    @Test
    public void onEventWithNoLink() throws Exception
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
    public void onEventWithOneLink() throws Exception
    {
        XDOM xdom = mockXDOM(Arrays.asList(mock(LinkBlock.class)));
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void onEventWithModifiedXProperty() throws Exception
    {
        // Ensure that the document we’ll mock has an empty content
        XDOM documentXDOM = mockXDOM(Collections.EMPTY_LIST);
        when(document.getXDOM()).thenReturn(documentXDOM);

        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("FullyRenderedText");

        mockDocumentXObject(document, context, "TextArea", property);

        mocker.getComponentUnderTest().onEvent(null, document, context);

        verify(baseObjectNormalizer, times(1)).normalizeBaseObject(
                any(BaseObject.class), eq("Mocked prop name"), any(Parser.class), any(BlockRenderer.class));
    }

    @Test
    public void onEventWithModifiedStaticListXProperty() throws Exception
    {
        // Ensure that the document we’ll mock has an empty content
        XDOM documentXDOM = mockXDOM(Collections.EMPTY_LIST);
        when(document.getXDOM()).thenReturn(documentXDOM);

        // Create the property that will be inspected
        StaticListClass property = mock(StaticListClass.class);

        List<LinkBlock> linkBlocks = Arrays.asList(mock(LinkBlock.class), mock(LinkBlock.class));

        mockDocumentXObject(document, context, "StaticList", property);

        mocker.getComponentUnderTest().onEvent(null, document, context);

        verify(baseObjectNormalizer, never()).normalizeBaseObject(
                any(BaseObject.class), any(String.class), any(Parser.class), any(BlockRenderer.class));
    }

    @Test
    public void onEventWithModifiedXPropertyAndWrongContentType() throws Exception
    {
        // Ensure that the document we’ll mock has an empty content
        XDOM documentXDOM = mockXDOM(Collections.EMPTY_LIST);
        when(document.getXDOM()).thenReturn(documentXDOM);

        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("AnotherTypeOfContent");

        List<LinkBlock> linkBlocks = Arrays.asList(mock(LinkBlock.class), mock(LinkBlock.class));

        mockDocumentXObject(document, context, "TextArea", property);

        mocker.getComponentUnderTest().onEvent(null, document, context);

        verify(linkBlockNormalizer, times(0)).normalizeLinkBlocks(linkBlocks);
    }
}
