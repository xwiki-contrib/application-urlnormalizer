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
package org.xwiki.contrib.urlnormalizer;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provides endpoints for normalizing parts of a document.
 *
 * @version $Id$
 * @since 1.4
 */
@Role
@Unstable
public interface DocumentNormalizer
{
    /**
     * Normalize the given {@link XWikiDocument}.
     *
     * @param document the document to normalize
     * @param parser the parser to use for parsing the content to be normalized
     * @param blockRenderer the block renderer that will be used after the normalization
     * @return true if the document has been normalized, false otherwise
     * @throws NormalizationException if an error happens
     */
    boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer) throws NormalizationException;
}
