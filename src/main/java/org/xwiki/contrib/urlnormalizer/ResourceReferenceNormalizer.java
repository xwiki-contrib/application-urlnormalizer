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
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Normalize a {@link ResourceReference} by checking if it is a URL pointing to a wiki link and if so transform it
 * into a Document Resource Reference.
 *
 * @version $Id$
 */
@Role
public interface ResourceReferenceNormalizer
{
    /**
     * Normalize the given {@link ResourceReference}.
     * @param reference the reference to normalize
     * @return the new reference
     */
    ResourceReference normalize(ResourceReference reference);
}
