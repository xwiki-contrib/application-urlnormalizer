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

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.URLValidator;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;

/**
 * Validates if an {@link EntityResourceReference} points to a URL for a supported action ("view", "download") or not.
 *
 * @version $Id:$
 */
@Component
@Singleton
public class SupportedActionURLValidator implements URLValidator<EntityResourceReference>
{
    private static final EntityResourceAction DOWNLOAD_ACTION = new EntityResourceAction("download");

    @Override
    public boolean validate(EntityResourceReference reference)
    {
        return reference.getAction().equals(EntityResourceAction.VIEW) || reference.getAction().equals(DOWNLOAD_ACTION);
    }
}
