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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Abstract for object document normalizers. These normalizers only look for TextArea properties within the
 * XWiki documents.
 *
 * @version $Id$
 * @since 1.4
 */
public abstract class AbstractObjectDocumentNormalizer implements DocumentNormalizer
{
    protected static final String TEXT_AREA = "TextArea";

    @Inject
    @Named("link")
    protected XDOMNormalizer linkXDOMNormalizer;

    @Inject
    @Named("macro")
    protected XDOMNormalizer macroXDOMNormalizer;

    @Inject
    protected Provider<XWikiContext> xWikiContextProvider;

    /**
     * Normalize the given XObject properties.
     *
     * @param baseObject the object to normalize
     * @param propertiesToNormalize the list of properties that should be normalized
     * @param parser the parser to use
     * @param blockRenderer the block renderer to use
     * @return true if the object has been modified, false otherwise
     * @throws NormalizationException if an error happens
     */
    protected boolean normalizeDocumentXObject(BaseObject baseObject, List<String> propertiesToNormalize, Parser parser,
        BlockRenderer blockRenderer) throws NormalizationException
    {
        boolean modified = false;

        for (String propertyToNormalize : propertiesToNormalize) {
            modified |= normalizeDocumentXObject(baseObject, propertyToNormalize, parser, blockRenderer);
        }

        return modified;
    }

    /**
     * Normalize the given XObject property
     *
     * @param baseObject the object to normalize
     * @param propertyName the property that should be normalized
     * @param parser the parser to use
     * @param blockRenderer the block renderer to use
     * @return true if the object has been modified, false otherwise
     * @throws NormalizationException if an error happens
     */
    protected boolean normalizeDocumentXObject(BaseObject baseObject, String propertyName, Parser parser,
        BlockRenderer blockRenderer) throws NormalizationException
    {
        boolean modified;

        // Get the content of the given XProperty
        String content = baseObject.getLargeStringValue(propertyName);

        try {
            XDOM xdom = parser.parse(new StringReader(content));

            modified = this.linkXDOMNormalizer.normalize(xdom, parser, blockRenderer);
            modified |= this.macroXDOMNormalizer.normalize(xdom, parser, blockRenderer);

            if (modified) {
                WikiPrinter wikiPrinter = new DefaultWikiPrinter();
                blockRenderer.render(xdom, wikiPrinter);
                String normalizedContent = wikiPrinter.toString();
                baseObject.setLargeStringValue(propertyName, normalizedContent);
            }
        } catch (ParseException e) {
            // The parser for the syntax of the document may not fit the syntax used in a XProperty.
            throw new NormalizationException(String.format(
                "Failed to normalize URLs in TextArea property [%s] in document [%s]",
                    propertyName, baseObject.getDocumentReference().toString()), e);
        }

        return modified;
    }
}
