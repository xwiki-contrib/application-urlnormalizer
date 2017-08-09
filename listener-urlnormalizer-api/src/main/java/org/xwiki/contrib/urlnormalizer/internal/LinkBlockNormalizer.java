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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.xwiki.component.annotation.Component;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.text.StringUtils;

/**
 * Interfaces with a {@link ResourceReferenceNormalizer}.
 *
 * @version $Id$
 * @since 1.1
 */
@Component(roles = LinkBlockNormalizer.class)
@Singleton
public class LinkBlockNormalizer
{
    @Inject
    private ResourceReferenceNormalizer resourceReferenceNormalizer;

    /**
     * Update the given list of link blocks with normalized URLs.
     *
     * @param linkBlocks the list of URL (link) blocks which will be updated and normalized
     */
    public void normalizeLinkBlocks(List<LinkBlock> linkBlocks)
    {
        for (int i = 0; i < linkBlocks.size(); i++) {
            LinkBlock linkBlock = linkBlocks.get(i);

            ResourceReference newReference = resourceReferenceNormalizer.normalize(linkBlock.getReference());

            // If no normalization happened then don't perform any change to the LinkBlock!
            if (newReference == linkBlock.getReference()) {
                continue;
            }

            boolean isFreeStanding;
            List<Block> newBlockChildren = new ArrayList<>(linkBlock.getChildren());

            // If we have normalized a free standing block, we have to turn it into a non free standing block and
            // generate a label that corresponds to the original URL of the link.
            if (!linkBlock.getReference().equals(newReference) && linkBlock.isFreeStandingURI()) {
                newBlockChildren.add(new WordBlock(linkBlock.getReference().getReference()));
                isFreeStanding = false;
            } else {
                isFreeStanding = linkBlock.isFreeStandingURI();
            }

            // Handle query string parameters
            boolean shouldAbortNormalization = handleQueryStringParameters(linkBlock, newReference);

            if (!shouldAbortNormalization) {
                LinkBlock newLinkBlock =
                    new LinkBlock(newBlockChildren, newReference, isFreeStanding, linkBlock.getParameters());

                // Replace the previous LinkBlock in the XDOM
                linkBlock.getParent().replaceChild(newLinkBlock, linkBlock);

                // Update the list given in parameter
                linkBlocks.set(i, newLinkBlock);
            }
        }
    }

    private boolean handleQueryStringParameters(LinkBlock linkBlock, ResourceReference newReference)
    {
        // Note: We need to merge the query string parameters coming from the URL (and stored as parameters in
        // the normalized ResourceReference) with any existing "queryString" LinkBlock parameters.
        // If a query string parameter name exists in the original link and the same parameter name also exists in
        // the URL but the values are different then skip the normalization for this link block in order not to
        // cause any loss of data! (even though having link block parameters for a URL has no meaning).

        boolean shouldAbortNormalization = false;

        // Parse the query string into a data structure to which we can easily add new items to
        List<NameValuePair> queryStringParameters;
        String queryString = linkBlock.getParameter(DocumentResourceReference.QUERY_STRING);
        if (StringUtils.isEmpty(queryString)) {
            queryStringParameters = new ArrayList<>();
        } else {
            queryStringParameters = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
        }

        for (Map.Entry<String, String> parameter : newReference.getParameters().entrySet()) {
            NameValuePair newParameter = new BasicNameValuePair(parameter.getKey(), parameter.getValue());
            if (hasDifferentValue(newParameter, queryStringParameters)) {
                shouldAbortNormalization = true;
                break;
            } else if (!queryStringParameters.contains(newParameter)) {
                queryStringParameters.add(newParameter);
            }
        }

        if (!shouldAbortNormalization) {
            // Replace the queryString parameter in the link block parameters with the new value, but only if not
            // empty
            String newQueryString = formatQueryString(queryStringParameters);
            if (!StringUtils.isEmpty(newQueryString)) {
                linkBlock.setParameter(DocumentResourceReference.QUERY_STRING, newQueryString);
            }

            // Remove the parameters from the newReference since we've now moved them as link block parameters
            newReference.removeParameter(DocumentResourceReference.QUERY_STRING);
        }

        return shouldAbortNormalization;
    }

    private boolean hasDifferentValue(NameValuePair newParameter, List<NameValuePair> queryStringParameters)
    {
        boolean hasDifferentValue = false;
        for (NameValuePair pair: queryStringParameters) {
            if (newParameter.getName().equals(pair.getName()) && !newParameter.getValue().equals(pair.getValue())) {
                hasDifferentValue = true;
                break;
            }
        }
        return hasDifferentValue;
    }

    private String formatQueryString(List<NameValuePair> queryStringParameters)
    {
        String newQueryStringDecoded;
        String newQueryStringEncoded = URLEncodedUtils.format(queryStringParameters, StandardCharsets.UTF_8);
        try {
            newQueryStringDecoded = URLDecoder.decode(newQueryStringEncoded, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            // Should never happen since UTF8 is always available but if it does, fallback to not decoding
            newQueryStringDecoded = newQueryStringEncoded;
        }
        return newQueryStringDecoded;
    }
}
