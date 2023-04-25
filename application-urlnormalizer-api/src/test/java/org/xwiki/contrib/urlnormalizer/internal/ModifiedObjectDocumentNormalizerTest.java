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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link ModifiedObjectDocumentNormalizer}.
 *
 * @version $Id$
 */
@ComponentTest
class ModifiedObjectDocumentNormalizerTest
{
    @InjectMockComponents
    private ModifiedObjectDocumentNormalizer normalize;

    @MockComponent
    @Named("xwiki/2.1")
    private BlockRenderer blockRenderer;

    @MockComponent
    @Named("xwiki/2.1")
    private Parser parser;

    @MockComponent
    @Named("link")
    private XDOMNormalizer linkXDOMNormalizer;

    @MockComponent
    @Named("macro")
    private XDOMNormalizer macroXDOMNormalizer;

    @Mock
    private XWikiContext context;

    /**
     * Associate an {@link ObjectDiff} with a related {@link BaseObject} to the given {@link XWikiDocument} in order to
     * test {@link ModifiedObjectDocumentNormalizer#normalize(XWikiDocument, Parser, BlockRenderer)}.
     *
     * @param document the document to mock on
     * @param context a mock of the {@link XWikiContext}
     * @param propType the property type of the XObject ("StaticList", "TextArea", ...)
     * @param property the object property
     * @param propertyClass the property class that should be pointed by the {@link ObjectDiff}
     * @throws Exception if an error happens
     */
    private void mockDocumentXObject(XWikiDocument document, XWikiContext context, String propType,
        BaseProperty property, PropertyClass propertyClass) throws Exception
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

        when(property.getValue()).thenReturn("content");
        when(property.getPropertyClass(any())).thenReturn(propertyClass);

        BaseObject baseObject = mock(BaseObject.class);
        when(baseObject.getField("propertyName")).thenReturn(property);
        BaseClass baseClass = mock(BaseClass.class);
        when(baseObject.getXClass(any(XWikiContext.class))).thenReturn(baseClass);

        when(document.getXObject()).thenReturn(baseObject);
        when(document.getXObject(baseObjectClass, 0)).thenReturn(baseObject);

        when(document.getObjectDiff(eq(originalDocument), eq(document), any(XWikiContext.class)))
            .thenReturn(Collections.singletonList(Collections.singletonList(diff)));
    }

    @Test
    void normalizeWithModifiedXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        // Create the property that will be inspected
        LargeStringProperty property = mock(LargeStringProperty.class);
        TextAreaClass propertyClass = mock(TextAreaClass.class);
        when(propertyClass.isWikiContent()).thenReturn(true);

        mockDocumentXObject(fakeDocument, this.context, "TextArea", property, propertyClass);

        // Note: the content of the XDOM doesn't matter for the test since all depends on the return value of
        // the called XDOM normalizer.
        XDOM propertyXDOM = new XDOM(Collections.emptyList());
        when(this.parser.parse(any())).thenReturn(propertyXDOM);

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

        this.normalize.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(property).setValue("normalizedContent");
    }

    @Test
    void normalizeWithModifiedNonTextAreaXProperty() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        // Create the property that will be inspected
        LargeStringProperty property = mock(LargeStringProperty.class);
        StaticListClass propertyClass = mock(StaticListClass.class);

        mockDocumentXObject(fakeDocument, this.context, "StaticList", property, propertyClass);

        this.normalize.normalize(fakeDocument, this.parser, this.blockRenderer);

        verify(this.linkXDOMNormalizer, times(0)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
        verify(this.macroXDOMNormalizer, times(0)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
    }
}
