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

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link ImageXDOMNormalizer}.
 *
 * @since 1.9.0
 * @version $Id$
 */
class ImageXDOMNormalizerTest extends AbstractResourceReferenceXDOMNormalizerTest
{
    @InjectMockComponents
    private ImageXDOMNormalizer normalizer;

    private List<ImageBlock> mockImageBlocks(List<ResourceReference> resourceReferences,
        Map<String, String> imageBlockParameters)
    {
        // Create the ImageBlocks corresponding to the ResourceReference
        List<ImageBlock> imageBlocks = new ArrayList<>();
        for (ResourceReference resourceReference : resourceReferences) {
            ImageBlock newBlock = new ImageBlock(resourceReference, true, imageBlockParameters);
            imageBlocks.add(newBlock);
            this.parentBlock.addChild(newBlock);
        }

        return imageBlocks;
    }

    @Test
    void normalizeImageBlocksWithOneExternalLink()
    {
        XDOM xdom = new XDOM(mockImageBlocks(List.of(this.externalLinkReference), Map.of()));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertTrue(modified);
        assertInstanceOf(ImageBlock.class, xdom.getChildren().get(0));
        assertNotEquals(this.externalLinkReference, ((ImageBlock) xdom.getChildren().get(0)).getReference());
    }

    @Test
    void normalizeImageBlocksWithOneInternalLink()
    {
        XDOM xdom = new XDOM(mockImageBlocks(List.of(this.internalLinkReference), Map.of()));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertFalse(modified);
        assertInstanceOf(ImageBlock.class, xdom.getChildren().get(0));
        assertEquals(this.internalLinkReference, ((ImageBlock) xdom.getChildren().get(0)).getReference());
    }
}
