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

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.contrib.urlnormalizer.DocumentNormalizer;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.contrib.urlnormalizer.URLNormalizationManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation for the {@link URLNormalizationManager}.
 *
 * @version $Id$
 * @since 1.4
 */
@Singleton
@Component
public class DefaultURLNormalizationManager implements URLNormalizationManager
{
    private static final List<String> DEFAULT_NORMALIZERS = Arrays.asList(ContentDocumentNormalizer.HINT,
        ObjectDocumentNormalizer.HINT);

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ContextualLocalizationManager localizationManager;

    @Inject
    private Logger logger;

    @Override
    public boolean normalize(XWikiDocument document) throws NormalizationException
    {
        return normalize(document, DEFAULT_NORMALIZERS);
    }

    @Override
    public boolean normalize(XWikiDocument document, List<String> normalizers) throws NormalizationException
    {
        return normalize(document, normalizers, false, false);
    }

    @Override
    public boolean normalize(DocumentReference documentReference) throws NormalizationException
    {
        return normalize(documentReference, DEFAULT_NORMALIZERS);
    }

    @Override
    public boolean normalize(DocumentReference documentReference, List<String> normalizers)
        throws NormalizationException
    {
        return normalize(documentReference, normalizers, true);
    }

    @Override
    public boolean normalize(DocumentReference documentReference, List<String> normalizers, boolean createNewVersion)
        throws NormalizationException
    {
        XWikiContext context = xWikiContextProvider.get();

        try {
            XWikiDocument document = context.getWiki().getDocument(documentReference, context);

            return normalize(document, normalizers, true, createNewVersion);
        } catch (XWikiException e) {
            throw new NormalizationException(String.format("Failed to load document [%s] for normalization",
                documentReference), e);
        }
    }

    private boolean normalize(XWikiDocument document, List<String> normalizers, boolean save, boolean createNewVersion)
        throws NormalizationException
    {
        boolean modified = false;

        this.logger.debug("Normalizing local URLs for [{}]...", document.getDocumentReference());

        // For performance persons, we check early and only perform processing if there's a parser and renderer for the
        // syntax of the document that was modified as otherwise we won't be able to find links and normalize them.
        if (componentManager.hasComponent(BlockRenderer.class, document.getSyntax().toIdString())
            && componentManager.hasComponent(Parser.class, document.getSyntax().toIdString())) {
            try {
                // Retrieve the parser and the renderer that should be used in order to normalize XProperty contents
                // and Macro contents.
                Parser parser = componentManager.getInstance(Parser.class, document.getSyntax().toIdString());
                BlockRenderer blockRenderer = componentManager.getInstance(BlockRenderer.class,
                    document.getSyntax().toIdString());

                // If no normalizers are provided, fallback on the default
                modified = applyNormalizers(document,
                    (normalizers.isEmpty()) ? DEFAULT_NORMALIZERS : normalizers,
                    parser, blockRenderer);
            } catch (ComponentLookupException e) {
                logger.warn(
                    "Unable to load a parser or a renderer for the syntax [{}] of the document [{}]. Root cause : [{}]",
                    document.getSyntax().toIdString(), document.getDocumentReference(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        } else {
            throw new NormalizationException(
                String.format("The syntax [%s] of the document [%s] cannot be parsed or rendered",
                    document.getSyntax().toIdString(), document.getDocumentReference()));
        }

        if (modified && save)
        {
            if (!createNewVersion) {
                document.setMetaDataDirty(false);
                document.setContentDirty(false);
            }

            XWikiContext context = xWikiContextProvider.get();

            try {
                context.getWiki().saveDocument(document,
                    localizationManager.getTranslationPlain(SAVE_COMMENT_KEY), context);
            } catch (XWikiException e) {
                throw new NormalizationException(
                    String.format("Failed to save document [%s] after normalization",
                        document.getDocumentReference()), e);
            }
        }

        return modified;
    }

    private boolean applyNormalizers(XWikiDocument document, List<String> normalizers, Parser parser,
        BlockRenderer blockRenderer) throws NormalizationException
    {
        boolean modified = false;

        for (String normalizerHint : normalizers) {
            if (componentManager.hasComponent(DocumentNormalizer.class, normalizerHint)) {
                try {
                    DocumentNormalizer normalizer = componentManager.getInstance(DocumentNormalizer.class,
                        normalizerHint);

                    modified |= normalizer.normalize(document, parser, blockRenderer);
                } catch (ComponentLookupException e) {
                    // This shouldn't happen as we are checking if the component instance exists before fetching it.
                    throw new NormalizationException(
                        String.format("Failed to load normalizer [%s] for document [%s]", normalizerHint,
                            document.getDocumentReference()), e);
                }
            } else {
                logger.warn("Could not find a normalizer with hint [{}] for document [{}]", normalizerHint,
                    document.getDocumentReference());
            }
        }

        return modified;
    }
}
