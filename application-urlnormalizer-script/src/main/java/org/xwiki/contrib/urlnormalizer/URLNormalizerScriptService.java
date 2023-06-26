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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.contrib.urlnormalizer.internal.job.NormalizeJob;
import org.xwiki.contrib.urlnormalizer.internal.job.NormalizeJobRequest;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.context.XWikiContextContextStore;

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
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private JobExecutor jobs;

    @Inject
    private JobStatusStore jobStore;

    @Inject
    private ContextStoreManager contextStore;

    @Inject
    private URLNormalizationManager urlNormalizationManager;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private Logger logger;

    /**
     * @return the {@link URLNormalizationManager}, null if the user is not a programmer.
     * @throws AccessDeniedException the current author does not have programming right
     */
    public URLNormalizationManager getUrlNormalizationManager() throws AccessDeniedException
    {
        this.contextualAuthorizationManager.checkAccess(Right.PROGRAM);

        return this.urlNormalizationManager;
    }

    /**
     * Normalize the given document. The current user should have edit rights on the document in order to normalize
     * while creating a new version.
     *
     * @param documentReference the document to normalize
     * @return true if the document has been modified, false otherwise
     * @throws AccessDeniedException the current author or current user is not allowed to modify a document without
     *             incrementing the version
     */
    public boolean normalize(DocumentReference documentReference) throws AccessDeniedException
    {
        return normalize(documentReference, true);
    }

    /**
     * Normalize the given document. The current user should have edit rights on the document in order to normalize
     * while creating a new version, and the user should be admin of the document in order to normalize without creating
     * a new version.
     *
     * @param documentReference the document to normalize
     * @param createNewVersion whether a new version of the document should be created
     * @return true if the document has been modified, false otherwise
     * @throws AccessDeniedException the current author or current user is not allowed to modify a document without
     *             incrementing the version
     */
    public boolean normalize(DocumentReference documentReference, boolean createNewVersion) throws AccessDeniedException
    {
        checkCreateNewVersionRight(documentReference, createNewVersion);

        if (this.contextualAuthorizationManager.hasAccess(Right.EDIT, documentReference)) {
            return normalizeInternal(documentReference, createNewVersion);
        } else {
            this.logger.error("The user [{}] doesn't have the right to normalize the document [{}]",
                this.documentAccessBridge.getCurrentUserReference(), documentReference);

            return false;
        }
    }

    /**
     * Perform a simple normalization on the given document.
     *
     * @param documentReference the document to normalize.
     * @param createNewVersion whether a new version of the document should be created
     * @return true if the document has been modified, false otherwise
     */
    private boolean normalizeInternal(DocumentReference documentReference, boolean createNewVersion)
    {
        try {
            return this.urlNormalizationManager.normalize(documentReference, Collections.emptyList(), createNewVersion);
        } catch (NormalizationException e) {
            this.logger.error("Failed to normalize document [{}]", documentReference, e);

            return false;
        }
    }

    /**
     * @param wiki the wiki associated with the job
     * @return the status of the current or last wiki normalize job
     * @since 1.7.0
     */
    public JobStatus getNormalizeJobStatus(WikiReference wiki)
    {
        List<String> jobId = NormalizeJobRequest.toJobId(wiki);

        // Try running job
        Job job = this.jobs.getJob(jobId);

        if (job != null) {
            return job.getStatus();
        }

        // Try serialized job
        return this.jobStore.getJobStatus(jobId);
    }

    /**
     * @param wiki the wiki to normalize
     * @param createNewVersion whether a new version of the document should be created
     * @return the started job
     * @throws NormalizationException when failing to start the job
     * @throws JobException when failing to start the job
     * @throws AccessDeniedException the current author or current user is not allowed to modify a document without
     *             incrementing the version
     * @since 1.7.0
     */
    public Job startNormalizeJob(WikiReference wiki, boolean createNewVersion)
        throws NormalizationException, JobException, AccessDeniedException
    {
        checkCreateNewVersionRight(wiki, createNewVersion);

        NormalizeJobRequest request = new NormalizeJobRequest(wiki, createNewVersion);

        // Pass current user and author to the job
        try {
            request.setContext(this.contextStore
                .save(Arrays.asList(XWikiContextContextStore.PROP_USER, XWikiContextContextStore.PROP_SECURE_AUTHOR)));
        } catch (ComponentLookupException e) {
            throw new NormalizationException("Failed to get the current user and author", e);
        }

        return this.jobs.execute(NormalizeJob.JOBTYPE, request);
    }

    private void checkCreateNewVersionRight(EntityReference entity, boolean createNewVersion)
        throws AccessDeniedException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (!createNewVersion) {
            // Only wiki admins are allowed to modify a document without incrementing the version
            this.authorizationManager.checkAccess(Right.ADMIN, xcontext.getAuthorReference(), entity);
        }
    }
}
