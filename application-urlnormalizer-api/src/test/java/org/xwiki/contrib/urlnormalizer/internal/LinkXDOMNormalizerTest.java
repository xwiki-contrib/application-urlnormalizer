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
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LinkXDOMNormalizer}.
 *
 * @since 1.3
 * @version $Id$
 */
class LinkXDOMNormalizerTest extends AbstractResourceReferenceXDOMNormalizerTest
{
    @InjectMockComponents
    private LinkXDOMNormalizer normalizer;

    private List<LinkBlock> mockLinkBlocks(List<ResourceReference> resourceReferences,
        Map<String, String> linkBlockParameters)
    {
        // Create the LinkBlocks corresponding to the ResourceReference
        List<LinkBlock> linkBlocks = new ArrayList<>();
        for (ResourceReference resourceReference : resourceReferences) {
            LinkBlock newBlock = new LinkBlock(List.of(), resourceReference, true, linkBlockParameters);
            linkBlocks.add(newBlock);
            this.parentBlock.addChild(newBlock);
        }

        return linkBlocks;
    }

    @Test
    void normalizeLinkBlocksWithOneExternalLink()
    {
        XDOM xdom = new XDOM(mockLinkBlocks(List.of(this.externalLinkReference), Map.of()));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertTrue(modified);
        assertInstanceOf(LinkBlock.class, xdom.getChildren().get(0));
        assertNotEquals(this.externalLinkReference, ((LinkBlock) xdom.getChildren().get(0)).getReference());
    }

    @Test
    void normalizeLinkBlocksWithOneInternalLink()
    {
        XDOM xdom = new XDOM(mockLinkBlocks(List.of(this.internalLinkReference), Map.of()));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertFalse(modified);
        assertInstanceOf(LinkBlock.class, xdom.getChildren().get(0));
        assertEquals(this.internalLinkReference, ((LinkBlock) xdom.getChildren().get(0)).getReference());
    }

    @Test
    void normalizeLinkBlocksWhenParametersAndExistingQueryStringBlockParameters()
    {
        ResourceReference reference = new ResourceReference("http://some/url?a=b", ResourceType.URL);

        ResourceReference normalizedReference = new ResourceReference("normalized", ResourceType.URL);
        normalizedReference.setParameter("a", "b");

        when(this.resourceReferenceNormalizer.normalize(reference)).thenReturn(normalizedReference);

        XDOM xdom = new XDOM(mockLinkBlocks(List.of(reference), Map.of("queryString", "c=d")));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertTrue(modified);
        assertInstanceOf(LinkBlock.class, xdom.getChildren().get(0));
        assertEquals("normalized", ((LinkBlock) xdom.getChildren().get(0)).getReference().getReference());
        assertEquals(1, xdom.getChildren().get(0).getParameters().size());
        assertEquals("c=d&a=b", xdom.getChildren().get(0).getParameter("queryString"));
    }

    @Test
    void normalizeLinkBlocksWhenSameParametersAndDifferentValues()
    {
        ResourceReference reference = new ResourceReference("http://some/url?a=b", ResourceType.URL);

        ResourceReference normalizedReference = new ResourceReference("normalized", ResourceType.URL);
        normalizedReference.setParameter("a", "b");

        when(this.resourceReferenceNormalizer.normalize(reference)).thenReturn(normalizedReference);

        XDOM xdom = new XDOM(mockLinkBlocks(List.of(reference), Map.of("queryString", "a=bb")));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertFalse(modified);
        assertInstanceOf(LinkBlock.class, xdom.getChildren().get(0));
        assertEquals("http://some/url?a=b", ((LinkBlock) xdom.getChildren().get(0)).getReference().getReference());
        assertEquals(1, xdom.getChildren().get(0).getParameters().size());
        assertEquals("a=bb", xdom.getChildren().get(0).getParameter("queryString"));
    }

    @Test
    void normalizeLinkBlocksWhenSameParametersAndSameValues()
    {
        ResourceReference reference = new ResourceReference("http://some/url?a=b", ResourceType.URL);

        ResourceReference normalizedReference = new ResourceReference("normalized", ResourceType.URL);
        normalizedReference.setParameter("a", "b");

        when(this.resourceReferenceNormalizer.normalize(reference)).thenReturn(normalizedReference);

        XDOM xdom = new XDOM(mockLinkBlocks(List.of(reference), Map.of("queryString", "a=b")));

        boolean modified = this.normalizer.normalize(xdom, null, null);

        assertTrue(modified);
        assertInstanceOf(LinkBlock.class, xdom.getChildren().get(0));
        assertEquals("normalized", ((LinkBlock) xdom.getChildren().get(0)).getReference().getReference());
        assertEquals(1, xdom.getChildren().get(0).getParameters().size());
        assertEquals("a=b", xdom.getChildren().get(0).getParameter("queryString"));
    }
}
