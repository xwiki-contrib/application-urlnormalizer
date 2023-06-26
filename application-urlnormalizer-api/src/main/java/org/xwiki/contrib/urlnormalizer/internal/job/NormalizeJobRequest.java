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

import java.util.Arrays;
import java.util.List;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.model.reference.WikiReference;

/**
 * Request to use with {@link NormalizeJob}.
 *
 * @version $Id: de4d7e8fe06869f184d054a013fd99546a0e78ce $
 * @since 1.7.0
 */
public class NormalizeJobRequest extends AbstractRequest
{
    /**
     * The name of the property containing the identifier of the wiki to normalize.
     */
    public static final String PROPERTY_WIKI = "wiki";

    /**
     * The name of the property indicating whether a new version of the document should be created.
     */
    public static final String PROPERTY_CREATENEWVERSION = "createNewVersion";

    private static final long serialVersionUID = 1L;

    /**
     * @param wiki the wiki to normalize
     * @param createNewVersion whether a new version of the document should be created
     */
    public NormalizeJobRequest(WikiReference wiki, boolean createNewVersion)
    {
        setId(toJobId(wiki));

        setProperty(PROPERTY_WIKI, wiki);
        setProperty(PROPERTY_CREATENEWVERSION, createNewVersion);
    }

    /**
     * @param request the request to copy
     */
    public NormalizeJobRequest(Request request)
    {
        super(request);
    }

    /**
     * @param wiki the wiki to normalize
     * @return the Job id corresponding to the passed wiki identifier
     */
    public static List<String> toJobId(WikiReference wiki)
    {
        return Arrays.asList(NormalizeJob.JOBTYPE, wiki.getName());
    }

    /**
     * @return the the wiki to normalize
     */
    public WikiReference getWikiReference()
    {
        return getProperty(PROPERTY_WIKI);
    }

    /**
     * @return whether a new version of the document should be created
     */
    public boolean isCreateNewVersion()
    {
        return getProperty(PROPERTY_CREATENEWVERSION, true);
    }
}
