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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

/**
 * Interfaces with a {@link ResourceReferenceNormalizer}.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = LinkBlockNormalizer.class)
@Singleton
public class LinkBlockNormalizer
{
    @Inject
    private ResourceReferenceNormalizer resourceReferenceNormalizer;

    /**
     * Update the given list of link blocks with normalized URLs.
     *
     * @param linkBlocks the list of URL (link) blocks which will be updated and normalized
     */
    public void normalizeLinkBlocks(List<LinkBlock> linkBlocks)
    {
        for (int i = 0; i < linkBlocks.size(); i++) {
            LinkBlock linkBlock = linkBlocks.get(i);

            ResourceReference newReference = resourceReferenceNormalizer.normalize(linkBlock.getReference());

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

            // Create a new LinkBlock
            LinkBlock newLinkBlock =
                new LinkBlock(newBlockChildren, newReference, isFreeStanding, linkBlock.getParameters());

            // Replace the previous LinkBlock in the XDOM
            linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);

            // Update the list given in parameter
            linkBlocks.set(i, newLinkBlock);
        }
    }
}
