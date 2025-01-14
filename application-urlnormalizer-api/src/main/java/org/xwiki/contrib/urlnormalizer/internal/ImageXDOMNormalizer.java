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
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Transform local images found in the passed XDOM into wiki images.
 *
 * @version $Id$
 * @since 1.9.0
 */
@Component
@Singleton
@Named("image")
public class ImageXDOMNormalizer extends AbstractResourceReferenceXDOMNormalizer<ImageBlock>
{
    @Override
    protected Class<ImageBlock> getTypeParameterClass()
    {
        return ImageBlock.class;
    }

    @Override
    protected boolean normalize(List<ImageBlock> blocks)
    {
        boolean normalized = false;

        for (int i = 0; i < blocks.size(); i++) {
            ImageBlock imageBlock = blocks.get(i);

            ResourceReference originalReference = imageBlock.getReference();
            ResourceReference newReference = this.resourceReferenceNormalizer.normalize(originalReference);

            // If no normalization happened then don't perform any change to the block!
            if (newReference == imageBlock.getReference()) {
                continue;
            }

            // Handle query string parameters
            boolean shouldAbortNormalization = handleQueryStringParameters(imageBlock, newReference);

            if (!shouldAbortNormalization) {
                ImageBlock newImageBlock =
                    new ImageBlock(newReference, false, imageBlock.getParameters());

                // Replace the previous ImageBlock in the XDOM
                imageBlock.getParent().replaceChild(newImageBlock, imageBlock);

                // Update the list given in parameter
                blocks.set(i, newImageBlock);

                normalized = true;
            }

            this.logger.debug("Normalized link to [{}] into [{}]", originalReference, newReference);
        }

        return normalized;
    }
}
