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

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.mock;

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

    private XWikiContext context;

    private BlockRenderer blockRenderer;

    private Parser parser;

    private DocumentNormalizer contentDocumentNormalizer;

    private DocumentNormalizer modifiedObjectDocumentNormalizer;

    @Before
    public void setUp() throws Exception
    {
        this.parser = this.mocker.registerMockComponent(Parser.class, Syntax.XWIKI_2_1.toIdString());
        this.blockRenderer = this.mocker.registerMockComponent(BlockRenderer.class, Syntax.XWIKI_2_1.toIdString());
        this.contentDocumentNormalizer = this.mocker.registerMockComponent(DocumentNormalizer.class, "content");
        this.modifiedObjectDocumentNormalizer =
            this.mocker.registerMockComponent(DocumentNormalizer.class, "object/modified");

        this.context = mock(XWikiContext.class);
    }

    /**
    @Test
    public void onEventWithModifiedXPropertyAndWrongContentType() throws Exception
    {
        XDOM xdom = new XDOM(Collections.emptyList());
        XWikiDocument fakeDocument = URLNormalizationHelper.mockXWikiDocument(xdom);

        // Create the property that will be inspected
        TextAreaClass property = mock(TextAreaClass.class);
        when(property.getContentType()).thenReturn("AnotherTypeOfContent");

        URLNormalizationHelper.mockDocumentXObject(fakeDocument, this.context, "TextArea", property);

        this.mocker.getComponentUnderTest().onEvent(null, fakeDocument, this.context);

        // We check that the XDOM normalizers are called only once (on the Content), since the xproperty is a
        // TextArea but not of the right type (i.e. not a TextAreaClass.ContentType.WIKI_TEXT).
        verify(this.linkXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
        verify(this.macroXDOMNormalizer, times(1)).normalize(any(XDOM.class), any(Parser.class),
            any(BlockRenderer.class));
    }
    **/
}
