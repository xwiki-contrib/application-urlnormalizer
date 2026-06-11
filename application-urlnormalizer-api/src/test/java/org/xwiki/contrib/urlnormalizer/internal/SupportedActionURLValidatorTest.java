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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link SupportedActionURLValidator}.
 *
 * @version $Id:$
 */
@ComponentTest
class SupportedActionURLValidatorTest
{
    @InjectMockComponents
    private SupportedActionURLValidator validator;

    @Test
    void validateWhenViewURL()
    {
        EntityResourceReference reference =
            new EntityResourceReference(new DocumentReference("wiki", "space", "page"), EntityResourceAction.VIEW);
        assertTrue(this.validator.validate(reference));
    }

    @Test
    void validateWhenDownloadURL()
    {
        EntityResourceReference reference = new EntityResourceReference(
            new AttachmentReference("attachment", new DocumentReference("wiki", "space", "page")),
            new EntityResourceAction("download"));
        assertTrue(this.validator.validate(reference));
    }

    @Test
    void validateWhenEditURL()
    {
        EntityResourceReference reference = new EntityResourceReference(
            new DocumentReference("wiki", "space", "page"), new EntityResourceAction("edit"));
        assertFalse(this.validator.validate(reference));
    }
}
