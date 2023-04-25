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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * Normalizer that will try to normalize every object of the given document.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named(ObjectDocumentNormalizer.HINT)
@Singleton
public class ObjectDocumentNormalizer extends AbstractObjectDocumentNormalizer
{
    /**
     * The component hint of this normalizer.
     */
    public static final String HINT = "object";

    @Override
    public boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        boolean modified = false;

        for (List<BaseObject> objects : document.getXObjects().values()) {
            for (BaseObject object : objects) {
                for (BaseProperty<?> property : (List<BaseProperty>) object.getFieldList()) {
                    modified |= normalize(property, parser, blockRenderer);
                }
            }
        }

        return modified;
    }
}
