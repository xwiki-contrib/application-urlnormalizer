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

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkBlockNormalizer}.
 *
 * @since 1.1
 * @version $Id$
 */
public class LinkBlockNormalizerTest
{
    @Rule
    public final MockitoComponentMockingRule<LinkBlockNormalizer> mocker =
            new MockitoComponentMockingRule<>(LinkBlockNormalizer.class);

    private ResourceReferenceNormalizer resourceReferenceNormalizer;

    // A link to the wiki written as a standard wiki link
    private ResourceReference internalLinkReference;

    // A link to the wiki written with an absolute URL
    private ResourceReference externalLinkReference;

    private ResourceReference normalizedExternalLinkReference;

    private Block parentBlock;

    @Before
    public void setUp() throws Exception
    {
        resourceReferenceNormalizer = mocker.registerMockComponent(ResourceReferenceNormalizer.class);

        parentBlock = new ParagraphBlock(new ArrayList<>());

        internalLinkReference = mock(ResourceReference.class);
        when(internalLinkReference.getReference()).thenReturn("Internal reference");

        externalLinkReference = mock(ResourceReference.class);
        when(externalLinkReference.getReference()).thenReturn("External reference");

        normalizedExternalLinkReference = mock(ResourceReference.class);
        when(normalizedExternalLinkReference.getReference()).thenReturn("External normalized link reference");

        when(resourceReferenceNormalizer.normalize(internalLinkReference)).thenReturn(internalLinkReference);
        when(resourceReferenceNormalizer.normalize(externalLinkReference)).thenReturn(normalizedExternalLinkReference);
    }

    private List<LinkBlock> mockLinkBlocks(List<ResourceReference> resourceReferences)
    {
        // Create the LinkBlocks corresponding to the ResourceReference
        List<LinkBlock> linkBlocks = new ArrayList<>();
        for (ResourceReference resourceReference : resourceReferences) {
            LinkBlock newBlock = new LinkBlock(
                    Collections.EMPTY_LIST, resourceReference, true, Collections.EMPTY_MAP);
            linkBlocks.add(newBlock);
            parentBlock.addChild(newBlock);
        }

        return linkBlocks;
    }

    @Test
    public void normalizeLinkBlocksWithOneExternalLink() throws Exception
    {
        List<LinkBlock> linkBlocks = mockLinkBlocks(Arrays.asList(externalLinkReference));

        mocker.getComponentUnderTest().normalizeLinkBlocks(linkBlocks);

        assertNotEquals(externalLinkReference, linkBlocks.get(0).getReference());
    }

    @Test
    public void normalizeLinkBlocksWithOneInternalLink() throws Exception
    {
        List<LinkBlock> linkBlocks = mockLinkBlocks(Arrays.asList(internalLinkReference));

        mocker.getComponentUnderTest().normalizeLinkBlocks(linkBlocks);

        assertEquals(internalLinkReference, linkBlocks.get(0).getReference());
    }
}
