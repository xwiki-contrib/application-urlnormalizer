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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Initialize the class {@code URLNormalizer.Code.FilterClass}.
 * 
 * @version $Id$
 * @since 1.6
 */
@Component
@Named(URLNormalizerConfigurationClassInitializer.CLASS_FULLNAME)
@Singleton
public class URLNormalizerConfigurationClassInitializer extends AbstractURLNormalizerClassInitializer
{
    /**
     * The name of the class defining the a global URL filter.
     */
    public static final String CLASS_NAME = "ConfigurationClass";

    /**
     * The String reference of the class defining the object which contains a Replication Instance metadata.
     */
    public static final String CLASS_FULLNAME = URLNORMALIZER_SPACE_NAME + '.' + CODE_SPACE_NAME + '.' + CLASS_NAME;

    /**
     * The reference of the class defining the object which contains a Replication Instance metadata.
     */
    public static final LocalDocumentReference CLASS_REFERENCE =
        new LocalDocumentReference(CLASS_NAME, new EntityReference(CODE_SPACE_NAME, EntityType.SPACE,
            new EntityReference(URLNORMALIZER_SPACE_NAME, EntityType.SPACE)));

    /**
     * The name of the property indicating if URL normalization is globally enabled or not.
     */
    public static final String FIELD_ENABLED = "enabled";

    /**
     * Default constructor.
     */
    public URLNormalizerConfigurationClassInitializer()
    {
        super(CLASS_REFERENCE, "URL Normalizer Configuration Class");
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addBooleanField(FIELD_ENABLED, "Enabled", "select");
    }
}
