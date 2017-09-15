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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.internal.parser.XDOMBuilder;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
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

//    private ComponentManager componentManager;

    private XDOMNormalizer linkXDOMNormalizer;

    private XDOMNormalizer macroXDOMNormalizer;

    private XWikiDocument document;

    private XWikiContext context;

    private BlockRenderer blockRenderer;

    private Parser parser;

    @Before
    public void setUp() throws Exception
    {
        this.parser = this.mocker.registerMockComponent(Parser.class, Syntax.XWIKI_2_1.toIdString());
        this.blockRenderer = this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());
        this.linkXDOMNormalizer = this.mocker.getInstance(XDOMNormalizer.class, "link");
        this.macroXDOMNormalizer = this.mocker.getInstance(XDOMNormalizer.class, "macro");

        this.context = mock(XWikiContext.class);
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
     * test {@link URLNormalizerListener#normalizeDocumentXObjects}.
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

        DocumentReference baseObjectClass = new DocumentReference("wiki", "space", "page");

        // Mock the ObjectDiff that we’ll use
        ObjectDiff diff = mock(ObjectDiff.class);
        when(diff.getPropType()).thenReturn(propType);
        when(diff.getPropName()).thenReturn("propertyName");
        when(diff.getNumber()).thenReturn(0);
        when(diff.getXClassReference()).thenReturn(baseObjectClass);

        BaseObject baseObject = mock(BaseObject.class);
        BaseClass baseClass = mock(BaseClass.class);
        when(baseObject.getXClass(any(XWikiContext.class))).thenReturn(baseClass);
        when(baseClass.getField("propertyName")).thenReturn(property);
        when(baseObject.getLargeStringValue("propertyName")).thenReturn("content");

        when(document.getXObject()).thenReturn(baseObject);
        when(document.getXObject(baseObjectClass, 0)).thenReturn(baseObject);

        when(document.getObjectDiff(originalDocument, document, context)).thenReturn(
                Collections.singletonList(Collections.singletonList(diff)));
    }

    @Test
    public void onEventWithNoLinkAnywhere() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, never()).setContent(any(XDOM.class));
        assertEquals(0, fakeDocument.getXDOM().getBlocks(
            new ClassBlockMatcher(LinkBlock.class), Block.Axes.DESCENDANT_OR_SELF).size());
    }

    @Test
    public void onEventWithOneNormalizedLinkInContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void onEventWithOneNormalizedLinkInMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void onEventWithNormalizedLinksInContentAndMacroContent() throws Exception
    {
        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        when(this.linkXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);
        when(this.macroXDOMNormalizer.normalize(xdom, this.parser, this.blockRenderer)).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, null);

        verify(fakeDocument, times(1)).setContent(any(XDOM.class));
    }

    @Test
    public void onEventWithModifiedXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("FullyRenderedText");

        mockDocumentXObject(fakeDocument, this.context, "TextArea", property);

        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM propertyXDOM = new XDOM(Collections.emptyList());
        when(this.parser.parse(any(Reader.class))).thenReturn(propertyXDOM);

        when(this.linkXDOMNormalizer.normalize(propertyXDOM, this.parser, this.blockRenderer)).thenReturn(true);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation)
            {
                ((DefaultWikiPrinter) invocation.getArguments()[1]).print("normalizedContent");
                return null;
            }
        }).when(this.blockRenderer).render(any(Block.class), any(WikiPrinter.class));

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, this.context);

        verify(fakeDocument.getXObject()).setLargeStringValue("propertyName", "normalizedContent");
    }

    @Test
    public void onEventWithModifiedNonTextAreaXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        // Create the property that will be inspected
        StaticListClass property = mock(StaticListClass.class);

        mockDocumentXObject(fakeDocument, this.context, "StaticList", property);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, this.context);

        // We check that the XDOM normalizers are called only once (on the Content), since the xproperty is not a
        // TextArea.
        verify(this.linkXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
        verify(this.macroXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
    }

    @Test
    public void onEventWithModifiedXPropertyAndWrongContentType() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = mockXWikiDocument(xdom);

        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("AnotherTypeOfContent");

        mockDocumentXObject(fakeDocument, this.context, "TextArea", property);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, this.context);

        // We check that the XDOM normalizers are called only once (on the Content), since the xproperty is a
        // TextArea but not of the right type (i.e. not a TextAreaClass.ContentType.WIKI_TEXT).
        verify(this.linkXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
        verify(this.macroXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
    }
}
