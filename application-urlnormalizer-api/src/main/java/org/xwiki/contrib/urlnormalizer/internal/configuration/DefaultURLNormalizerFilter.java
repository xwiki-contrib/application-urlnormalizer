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
package org.xwiki.contrib.urlnormalizer.internal.configuration;

import java.util.regex.Pattern;

import org.xwiki.contrib.urlnormalizer.URLNormalizerFilter;
import org.xwiki.rendering.listener.reference.ResourceType;

/**
 * Default implementation of {@link URLNormalizerFilter}.
 * 
 * @version $Id$
 * @since 1.6
 */
public class DefaultURLNormalizerFilter implements URLNormalizerFilter
{
    private final ResourceType linkType;

    private final Pattern linkReference;

    private final ResourceType targetType;

    private final String targetReference;

    /**
     * @param linkType the type of link to match
     * @param linkReference the pattern to produce to match the link
     * @param targetType the type of link to produce
     * @param targetReference the pattern to use to produce the link
     */
    public DefaultURLNormalizerFilter(ResourceType linkType, Pattern linkReference, ResourceType targetType,
        String targetReference)
    {
        this.linkType = linkType;
        this.linkReference = linkReference;
        this.targetType = targetType;
        this.targetReference = targetReference;
    }

    @Override
    public ResourceType getLinkType()
    {
        return this.linkType;
    }

    @Override
    public Pattern getLinkReference()
    {
        return this.linkReference;
    }

    @Override
    public ResourceType getTargetType()
    {
        return this.targetType;
    }

    @Override
    public String getTargetReference()
    {
        return this.targetReference;
    }
}
