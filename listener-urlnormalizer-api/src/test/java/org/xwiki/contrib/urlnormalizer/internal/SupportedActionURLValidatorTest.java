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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link SupportedActionURLValidator}.
 *
 * @version $Id:$
 */
public class SupportedActionURLValidatorTest
{
    @Rule
    public MockitoComponentMockingRule<SupportedActionURLValidator> mocker =
        new MockitoComponentMockingRule<>(SupportedActionURLValidator.class);

    @Test
    public void validateWhenViewURL() throws Exception
    {
        EntityResourceReference reference =
            new EntityResourceReference(new DocumentReference("wiki", "space", "page"), EntityResourceAction.VIEW);
        assertTrue(this.mocker.getComponentUnderTest().validate(reference));
    }

    @Test
    public void validateWhenDownloadURL() throws Exception
    {
        EntityResourceReference reference = new EntityResourceReference(
            new AttachmentReference("attachment", new DocumentReference("wiki", "space", "page")),
            new EntityResourceAction("download"));
        assertTrue(this.mocker.getComponentUnderTest().validate(reference));
    }

    @Test
    public void validateWhenEditURL() throws Exception
    {
        EntityResourceReference reference = new EntityResourceReference(
            new DocumentReference("wiki", "space", "page"), new EntityResourceAction("edit"));
        assertFalse(this.mocker.getComponentUnderTest().validate(reference));
    }
}
