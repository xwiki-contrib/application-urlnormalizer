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
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Normalizer that will only look at the last modified XObjects of the given document.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("object/modified")
@Singleton
public class ModifiedObjectDocumentNormalizer extends AbstractObjectDocumentNormalizer
{
    @Override
    public boolean normalize(XWikiDocument document, Parser parser, BlockRenderer blockRenderer)
        throws NormalizationException
    {
        XWikiContext context = xWikiContextProvider.get();
        boolean modified = false;

        // Get diffs of the document objects
        List<List<ObjectDiff>> documentObjectDiff =
            document.getObjectDiff(document.getOriginalDocument(), document, context);

        // Go through every object that has been modified
        for (List<ObjectDiff> objectDiffs : documentObjectDiff) {
            // Go through every property modified
            for (ObjectDiff objectDiff : objectDiffs) {
                if (objectDiff.getPropType().equals(TEXT_AREA)) {
                    BaseObject object =
                        document.getXObject(objectDiff.getXClassReference(), objectDiff.getNumber());
                    TextAreaClass property =
                        (TextAreaClass) object.getXClass(context).getField(objectDiff.getPropName());

                    // We only perform normalization if the TextArea contains markup (if it's pure text or velocity
                    // content we won't know how to parse it anyway!).
                    if (property.getContentType().equalsIgnoreCase(TextAreaClass.ContentType.WIKI_TEXT.toString())) {
                        modified |= normalizeDocumentXObject(object, objectDiff.getPropName(), parser, blockRenderer);
                    }
                }
            }
        }

        return modified;
    }
}
