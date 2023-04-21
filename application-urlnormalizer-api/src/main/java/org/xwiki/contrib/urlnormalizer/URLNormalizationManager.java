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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide high-level APIs to normalize documents.
 *
 * @since 1.4
 * @version $Id$
 */
@Role
@Unstable
public interface URLNormalizationManager
{
    /**
     * The save comment used when saving a document after normalization.
     */
    String SAVE_COMMENT_KEY = "application.urlnormalizer.document.saveComment";

    /**
     * Normalize the given {@link XWikiDocument}, do not save the document after the normalization. The document will
     * not be saved after normalization.
     *
     * @param document the document to normalize
     * @return true if the document has been modified
     * @throws NormalizationException if an error happens
     */
    boolean normalize(XWikiDocument document) throws NormalizationException;

    /**
     * Normalize the given {@link XWikiDocument} using the given normalizers. The document will not be saved after
     * normalziation.
     *
     * @param document the document to normalize
     * @param normalizers a list of hints for {@link DocumentNormalizer} components to be used on the document. If
     * the list is empty, the normalizers to be used will be chosen by the implementation of this component.
     * @return true if the document has been modified
     * @throws NormalizationException if an error happens
     */
    boolean normalize(XWikiDocument document, List<String> normalizers) throws NormalizationException;

    /**
     * Normalize the given {@link DocumentReference} and save it. The save comment should be the value of the
     * translation key {@link #SAVE_COMMENT_KEY}.
     *
     * @param documentReference the document to normalize
     * @return true if the document has been modified
     * @throws NormalizationException if an error happens
     */
    boolean normalize(DocumentReference documentReference) throws NormalizationException;

    /**
     * Normalize the given {@link DocumentReference} and save it. The save comment should be the value of the
     * translation key {@link #SAVE_COMMENT_KEY}.
     *
     * @param documentReference the document to normalize
     * @param normalizers a list of hints for {@link DocumentNormalizer} components to be used on the document. See
     * {@link #normalize(XWikiDocument, List)}.
     * @return true if the document has been modified
     * @throws NormalizationException if an error happens
     */
    boolean normalize(DocumentReference documentReference, List<String> normalizers) throws NormalizationException;

    /**
     * Normalize the given {@link DocumentReference}. See {@link #normalize(DocumentReference, List, boolean)}.
     *
     * @param documentReference the document to normalize
     * @param normalizers a list of hints for {@link DocumentNormalizer} components to be used on the document. See
     * {@link #normalize(DocumentReference, List)}.
     * @param createNewVersion defines whether a new document version should be created if the document is saved.
     * @return true if the document has been modified
     * @throws NormalizationException if an error happens
     */
    boolean normalize(DocumentReference documentReference, List<String> normalizers, boolean createNewVersion)
        throws NormalizationException;
}
