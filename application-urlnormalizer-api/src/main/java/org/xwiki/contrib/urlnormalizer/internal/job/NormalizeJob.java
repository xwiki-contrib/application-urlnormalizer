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
package org.xwiki.contrib.urlnormalizer.internal.job;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.contrib.urlnormalizer.NormalizationException;
import org.xwiki.contrib.urlnormalizer.URLNormalizationManager;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Apply the normalization on all existing documents.
 * 
 * @version $Id$
 * @since 1.7.0
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(NormalizeJob.JOBTYPE)
public class NormalizeJob extends AbstractJob<NormalizeJobRequest, JobStatus>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "urlnormalizer";

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private JobProgressManager progress;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private URLNormalizationManager urlNormalizationManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    protected NormalizeJobRequest castRequest(Request request)
    {
        NormalizeJobRequest normalizeRequest;
        if (request instanceof NormalizeJobRequest) {
            normalizeRequest = (NormalizeJobRequest) request;
        } else {
            normalizeRequest = new NormalizeJobRequest(request);
        }

        return normalizeRequest;
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        List<String> documents = this.queryManager.getNamedQuery("getAllDocuments").execute();

        XWikiContext xcontext = this.xcontextProvider.get();

        if (getRequest().isVerbose()) {
            this.logger.info("Found [{}] documents.", documents.size());
        }

        this.progress.pushLevelProgress(documents.size(), this);

        try {
            for (String document : documents) {
                this.progress.startStep(document);

                try {
                    normalize(this.resolver.resolve(document, getRequest().getWikiReference()), xcontext);
                } finally {
                    this.progress.endStep(document);
                }
            }
        } finally {
            this.progress.popLevelProgress(this);
        }
    }

    private void normalize(DocumentReference documentReference, XWikiContext xcontext)
    {
        // Make sure the current author is allowed to modify the document
        if (!this.authorizationManager.hasAccess(Right.EDIT, xcontext.getAuthorReference(), documentReference)) {
            this.logger.error("The author [{}] doesn't have the right to normalize the document [{}].",
                xcontext.getAuthorReference(), documentReference);

            return;
        }

        // Make sure the current user is allowed to modify the document
        if (!this.authorizationManager.hasAccess(Right.EDIT, xcontext.getUserReference(), documentReference)) {
            this.logger.error("The user [{}] doesn't have the right to normalize the document [{}].",
                xcontext.getAuthorReference(), documentReference);

            return;
        }

        // Normalize the document
        try {
            if (this.urlNormalizationManager.normalize(documentReference, Collections.emptyList(),
                getRequest().isCreateNewVersion())) {
                this.logger.info("The document [{}] has been normalized.", documentReference);
            } else {
                this.logger.info("There is nothing to normalize in document [{}].", documentReference);
            }
        } catch (NormalizationException e) {
            this.logger.error("Failed to normalize document [{}].", documentReference, e);
        }
    }
}
