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
package org.xwiki.contrib.urlnormalizer.internal.configuration;

import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;

/**
 * Base class to initialize variable normalization related classes.
 * 
 * @version $Id$
 * @since 1.8.0
 */
public abstract class AbstractURLNormalizerClassInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The name of the space containing all URL Normalizer pages.
     */
    public static final String URLNORMALIZER_SPACE_NAME = "URLNormalizer";

    /**
     * The name of the space containing all URL Normalizer technical pages.
     */
    public static final String CODE_SPACE_NAME = "Code";

    /**
     * @param reference the reference of the document to update. Can be either local or absolute depending if the
     *            document is associated to a specific wiki or not
     * @param title the title of the document
     */
    public AbstractURLNormalizerClassInitializer(EntityReference reference, String title)
    {
        super(reference, title);
    }

    @Override
    protected boolean isMainWikiOnly()
    {
        return true;
    }
}
