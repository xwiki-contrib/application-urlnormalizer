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
import com.xpn.xwiki.objects.classes.PropertyClass;

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

        for (Map.Entry<DocumentReference, List<BaseObject>> objectsMap : document.getXObjects().entrySet()) {
            modified |= normalizeObjects(objectsMap, parser, blockRenderer);
        }

        return modified;
    }

    private boolean normalizeObjects(Map.Entry<DocumentReference, List<BaseObject>> objectsMap, Parser parser,
        BlockRenderer blockRenderer) throws NormalizationException
    {
        // Load the corresponding XClass, check if it contains fields that can be normalized
        // Search for a non-null entry in the objectMap
        Object[] baseProperties = null;
        for (int i = 0; i < objectsMap.getValue().size() && baseProperties == null; i++) {
            if (objectsMap.getValue().get(i) != null) {
                baseProperties = objectsMap.getValue().get(i).getProperties();
            }
        }

        if (baseProperties != null) {
            return normalizeObject(baseProperties, objectsMap, parser, blockRenderer);
        }

        return false;
    }

    private boolean normalizeObject(Object[] baseProperties,
        Map.Entry<DocumentReference, List<BaseObject>> objectsMap, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        boolean modified = false;
        XWikiContext context = xWikiContextProvider.get();
        List<String> propertiesToNormalize = new ArrayList<>();

        for (Object basePropertyObj : baseProperties) {
            if (basePropertyObj instanceof BaseProperty) {
                BaseProperty baseProperty = (BaseProperty) basePropertyObj;
                PropertyClass propertyClass = baseProperty.getPropertyClass(context);
                if (propertyClass != null && propertyClass.getClassType().equals(TEXT_AREA)) {
                    propertiesToNormalize.add(baseProperty.getName());
                }
            }
        }

        for (BaseObject object : objectsMap.getValue()) {
            if (object != null) {
                modified |= normalizeDocumentXObject(object, propertiesToNormalize, parser, blockRenderer);
            }
        }

        return modified;
    }
}
