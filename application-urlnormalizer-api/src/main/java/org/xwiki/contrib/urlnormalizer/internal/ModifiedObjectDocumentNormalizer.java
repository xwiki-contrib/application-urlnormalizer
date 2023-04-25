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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;

/**
 * Normalizer that will only look at the last modified XObjects of the given document.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named(ModifiedObjectDocumentNormalizer.HINT)
@Singleton
public class ModifiedObjectDocumentNormalizer extends AbstractObjectDocumentNormalizer
{
    /**
     * The hint of the component.
     */
    public static final String HINT = "object/modified";

    @Override
    public boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        boolean modified = false;

        // Get diffs of the document objects
        List<List<ObjectDiff>> documentObjectDiff =
            document.getObjectDiff(document.getOriginalDocument(), document, xcontext);

        // Go through every object that has been modified
        for (List<ObjectDiff> objectDiffs : documentObjectDiff) {
            // Go through every property modified
            for (ObjectDiff objectDiff : objectDiffs) {
                if (objectDiff.getPropType().equals(TEXT_AREA)) {
                    BaseObject object = document.getXObject(objectDiff.getXClassReference(), objectDiff.getNumber());
                    BaseProperty<?> property = (BaseProperty) object.getField(objectDiff.getPropName());

                    modified |= normalize(property, parser, blockRenderer);
                }
            }
        }

        return modified;
    }
}
