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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ImageXDOMNormalizer}.
 *
 * @since 1.9.0
 * @version $Id$
 */
public class ImageXDOMNormalizerTest extends AbstractResourceReferenceXDOMNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<ImageXDOMNormalizer> mocker =
        new MockitoComponentMockingRule<>(ImageXDOMNormalizer.class);

    @Before
    public void setUp() throws Exception
    {
        resourceReferenceNormalizer = mocker.registerMockComponent(ResourceReferenceNormalizer.class);
        super.setUp();
    }

    private List<ImageBlock> mockImageBlocks(List<ResourceReference> resourceReferences,
        Map<String, String> imageBlockParameters)
    {
        // Create the ImageBlocks corresponding to the ResourceReference
        List<ImageBlock> imageBlocks = new ArrayList<>();
        for (ResourceReference resourceReference : resourceReferences) {
            ImageBlock newBlock = new ImageBlock(resourceReference, true, imageBlockParameters);
            imageBlocks.add(newBlock);
            parentBlock.addChild(newBlock);
        }

        return imageBlocks;
    }

    @Test
    public void normalizeImageBlocksWithOneExternalLink() throws Exception
    {
        XDOM xdom = new XDOM(mockImageBlocks(Arrays.asList(externalLinkReference), Collections.emptyMap()));

        boolean modified = mocker.getComponentUnderTest().normalize(xdom, null, null);

        assertTrue(modified);
        assertTrue(xdom.getChildren().get(0) instanceof ImageBlock);
        assertNotEquals(externalLinkReference, ((ImageBlock) xdom.getChildren().get(0)).getReference());
    }

    @Test
    public void normalizeLinkBlocksWithOneInternalLink() throws Exception
    {
        XDOM xdom = new XDOM(mockImageBlocks(Arrays.asList(internalLinkReference), Collections.emptyMap()));

        boolean modified = mocker.getComponentUnderTest().normalize(xdom, null, null);

        assertFalse(modified);
        assertTrue(xdom.getChildren().get(0) instanceof ImageBlock);
        assertEquals(internalLinkReference, ((ImageBlock) xdom.getChildren().get(0)).getReference());
    }
}
