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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;

/**
 * Normalizer that will try to normalize every object of the given document.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("object")
@Singleton
public class ObjectDocumentNormalizer extends AbstractObjectDocumentNormalizer
{
    @Override
    public boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        XWikiContext context = xWikiContextProvider.get();
        boolean modified = false;

        for (Map.Entry<DocumentReference, List<BaseObject>> objectsMap : document.getXObjects().entrySet()) {
            // Load the corresponding XClass, check if it contains fields that can be normalized
            Object[] baseProperties = objectsMap.getValue().get(0).getProperties();
            List<String> propertiesToNormalize = new ArrayList<>();

            for (Object basePropertyObj : baseProperties) {
                if (basePropertyObj instanceof BaseProperty) {
                    BaseProperty baseProperty = (BaseProperty) basePropertyObj;
                    if (baseProperty.getPropertyClass(context).getClassType().equals(TEXT_AREA)) {
                        propertiesToNormalize.add(baseProperty.getName());
                    }
                }
            }

            for (BaseObject object : objectsMap.getValue()) {
                modified |= normalizeDocumentXObject(object, propertiesToNormalize, parser, blockRenderer);
            }
        }

        return modified;
    }
}
