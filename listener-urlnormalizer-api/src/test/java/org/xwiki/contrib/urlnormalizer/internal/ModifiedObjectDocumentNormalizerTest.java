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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ModifiedObjectDocumentNormalizerTest}.
 *
 * @version $Id$
 * @since 1.4
 */
public class ModifiedObjectDocumentNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<ModifiedObjectDocumentNormalizer> mocker =
        new MockitoComponentMockingRule<>(ModifiedObjectDocumentNormalizer.class);


    private XWikiContext context;

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

        this.context = mock(XWikiContext.class);
    }

    /**
     * Associate an {@link ObjectDiff} with a related {@link BaseObject} to the given {@link XWikiDocument} in order to
     * test {@link ModifiedObjectDocumentNormalizer#normalize(XWikiDocument, Parser, BlockRenderer)}.
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

        when(document.getObjectDiff(eq(originalDocument), eq(document), any(XWikiContext.class))).thenReturn(
            Collections.singletonList(Collections.singletonList(diff)));
    }

    @Test
    public void normalizeWithModifiedXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

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

        this.mocker.getComponentUnderTest().normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(fakeDocument.getXObject()).setLargeStringValue("propertyName", "normalizedContent");
    }

    @Test
    public void normalizeWithModifiedNonTextAreaXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        // Create the property that will be inspected
        StaticListClass property = mock(StaticListClass.class);

        mockDocumentXObject(fakeDocument, this.context, "StaticList", property);

        this.mocker.getComponentUnderTest().normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(this.linkXDOMNormalizer, times(0)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
        verify(this.macroXDOMNormalizer, times(0)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
    }
}
