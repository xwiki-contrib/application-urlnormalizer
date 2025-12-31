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

import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Abstract allowing for running tests on classes implementing {@link AbstractResourceReferenceXDOMNormalizer}.
 *
 * @version $Id$
 * @since 1.9.0
 */
public abstract class AbstractResourceReferenceXDOMNormalizerTest
{
    protected ResourceReferenceNormalizer resourceReferenceNormalizer;

    // A link to the wiki written as a standard wiki link
    protected ResourceReference internalLinkReference;

    // A link to the wiki written with an absolute URL
    protected ResourceReference externalLinkReference;

    protected ResourceReference normalizedExternalLinkReference;

    protected Block parentBlock;

    public void setUp() throws Exception
    {
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
}
