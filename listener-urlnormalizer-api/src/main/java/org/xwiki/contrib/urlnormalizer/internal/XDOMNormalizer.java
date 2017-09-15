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

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

/**
 * Normalize link found in blocks inside the passed XDOM (convert local links to wiki links).
 *
 * @version $Id$
 * @since 1.3
 */
@Role
public interface XDOMNormalizer
{
    /**
     * Normalize link found in blocks inside the passed XDOM (convert local links to wiki links).
     *
     * @param xdom the xdom to transform
     * @param parser the parser to use when we need to parse content written in wiki markup in some Blocks
     * @param blockRenderer the renderer to use when we need to save the normalized content back into Blocks
     * @return true if the passed XDOM has been modified (i.e. there have been normalizations) or false otherwise
     */
    boolean normalize(XDOM xdom, Parser parser, BlockRenderer blockRenderer);
}
