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

import org.junit.jupiter.api.BeforeEach;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Abstract allowing for running tests on classes implementing {@link AbstractResourceReferenceXDOMNormalizer}.
 *
 * @version $Id$
 * @since 1.9.0
 */
@ComponentTest
abstract class AbstractResourceReferenceXDOMNormalizerTest
{
    @MockComponent
    protected ResourceReferenceNormalizer resourceReferenceNormalizer;

    // A link to the wiki written as a standard wiki link
    protected ResourceReference internalLinkReference;

    // A link to the wiki written with an absolute URL
    protected ResourceReference externalLinkReference;

    protected ResourceReference normalizedExternalLinkReference;

    protected Block parentBlock;

    @BeforeEach
    void setUp()
    {
        this.parentBlock = new ParagraphBlock(new ArrayList<>());

        this.internalLinkReference = mock(ResourceReference.class);
        when(this.internalLinkReference.getReference()).thenReturn("Internal reference");

        this.externalLinkReference = mock(ResourceReference.class);
        when(this.externalLinkReference.getReference()).thenReturn("External reference");

        this.normalizedExternalLinkReference = mock(ResourceReference.class);
        when(this.normalizedExternalLinkReference.getReference()).thenReturn("External normalized link reference");

        when(this.resourceReferenceNormalizer.normalize(this.internalLinkReference))
            .thenReturn(this.internalLinkReference);
        when(this.resourceReferenceNormalizer.normalize(this.externalLinkReference))
            .thenReturn(this.normalizedExternalLinkReference);
    }
}
