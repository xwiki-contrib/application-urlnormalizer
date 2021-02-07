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
package org.xwiki.contrib.urlnormalizer;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Script service for accessing URL Normalization APIs.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("urlnormalizer")
@Singleton
public class URLNormalizerScriptService implements ScriptService
{
    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private Logger logger;

    @Inject
    private URLNormalizationManager urlNormalizationManager;

    /**
     * @return the {@link URLNormalizationManager}, null if the user is not a programmer.
     */
    public URLNormalizationManager getUrlNormalizationManager()
    {
        if (contextualAuthorizationManager.hasAccess(Right.PROGRAM)) {
            return urlNormalizationManager;
        } else {
            logger.error("The user [{}] doesn't have the right to access the URLNormalizationManager.",
                documentAccessBridge.getCurrentUserReference());
            return null;
        }
    }

    /**
     * Normalize the given document. The current user should have edit rights on the document in order to normalize
     * while creating a new version.
     *
     * @param documentReference the document to normalize
     * @return true if the document has been modified, false otherwise
     */
    public boolean normalize(DocumentReference documentReference)
    {
        if (contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)) {
            return normalizeInternal(documentReference);
        } else {
            logger.error("The user doesn't have ");
            return false;
        }
    }

    /**
     * Normalize the given document. The current user should have edit rights on the document in order to normalize
     * while creating a new version, and the user should be admin of the document in order to normalize without
     * creating a new version.
     *
     * @param documentReference the document to normalize
     * @param createNewVersion whether a new version of the document should be created
     * @return true if the document has been modified, false otherwise
     */
    public boolean normalize(DocumentReference documentReference, boolean createNewVersion)
    {
        if (createNewVersion) {
            return normalize(documentReference);
        } else if (contextualAuthorizationManager.hasAccess(Right.ADMIN,
            documentAccessBridge.getCurrentDocumentReference())) {
            return normalizeInternal(documentReference);
        } else {
            logger.error("The user [{}] doesn't have the right to normalize the document [{}] "
                + "without creating a new version.", documentReference, documentAccessBridge.getCurrentUserReference());
            return false;
        }
    }

    /**
     * Perform a simple normalization on the given document.
     *
     * @param documentReference the document to normalize.
     * @return true if the document has been modified, false otherwise
     */
    private boolean normalizeInternal(DocumentReference documentReference)
    {
        try {
            return urlNormalizationManager.normalize(documentReference);
        } catch (NormalizationException e) {
            logger.error("Failed to normalize document [{}]", documentReference, e);
            return false;
        }
    }
}
