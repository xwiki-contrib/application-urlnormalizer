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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Transform local links found in the passed XDOM into wiki links.
 *
 * @version $Id$
 * @since 1.3
 */
@Component
@Named("link")
@Singleton
public class LinkXDOMNormalizer extends AbstractResourceReferenceXDOMNormalizer<LinkBlock>
{
    @Override
    protected Class<LinkBlock> getTypeParameterClass()
    {
        return LinkBlock.class;
    }

    @Override
    protected boolean normalize(List<LinkBlock> linkBlocks)
    {
        boolean normalized = false;

        for (int i = 0; i < linkBlocks.size(); i++) {
            LinkBlock linkBlock = linkBlocks.get(i);

            ResourceReference originalReference = linkBlock.getReference();
            ResourceReference newReference = this.resourceReferenceNormalizer.normalize(originalReference);

            // If no normalization happened then don't perform any change to the LinkBlock!
            if (newReference == linkBlock.getReference()) {
                continue;
            }

            boolean isFreeStanding;
            List<Block> newBlockChildren = new ArrayList<>(linkBlock.getChildren());

            // If we have normalized a free standing block, we have to turn it into a non free standing block and
            // generate a label that corresponds to the original URL of the link.
            if (!linkBlock.getReference().equals(newReference) && linkBlock.isFreeStandingURI()) {
                newBlockChildren.add(new WordBlock(linkBlock.getReference().getReference()));
                isFreeStanding = false;
            } else {
                isFreeStanding = linkBlock.isFreeStandingURI();
            }

            // Handle query string parameters
            boolean shouldAbortNormalization = handleQueryStringParameters(linkBlock, newReference);

            if (!shouldAbortNormalization) {
                LinkBlock newLinkBlock =
                    new LinkBlock(newBlockChildren, newReference, isFreeStanding, linkBlock.getParameters());

                // Replace the previous LinkBlock in the XDOM
                linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);

                // Update the list given in parameter
                linkBlocks.set(i, newLinkBlock);

                normalized = true;
            }

            this.logger.debug("Normalized link to [{}] into [{}]", originalReference, newReference);
        }

        return normalized;
    }
}
