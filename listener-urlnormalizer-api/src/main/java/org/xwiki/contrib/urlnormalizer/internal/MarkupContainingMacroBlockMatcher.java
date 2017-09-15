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
import java.util.List;

import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.match.ClassBlockMatcher;

/**
 * The supported list of macros is static and correspond to well-known macros that are known to contain wiki markup
 * (e.g. info, warning, error  macros). In the future this should be improved thanks to
 * <a href="https://jira.xwiki.org/browse/XRENDERING-116">XRENDERING-116</a>.
 *
 * @version $Id$
 * @since 1.3
 */
public class MarkupContainingMacroBlockMatcher extends ClassBlockMatcher
{
    /**
     * Names of macros that support wiki markup and the markup syntax is that of the current document.
     */
    private static final List<String> SUPPORTED_MACRO = Arrays.asList(
        "message",
        "warning",
        "info",
        "error",
        "success",
        "cache",
        "context",
        "box",
        "comment",
        "container",
        "todo");

    /**
     * Default constructor.
     */
    public MarkupContainingMacroBlockMatcher()
    {
        super(MacroBlock.class);
    }

    @Override
    public boolean match(Block block)
    {
        return super.match(block) && SUPPORTED_MACRO.contains(((MacroBlock) block).getId());
    }
}
