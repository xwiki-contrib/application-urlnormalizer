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

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.xwiki.contrib.urlnormalizer.ResourceReferenceNormalizer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;

/**
 * Abstract providing tools to normalize blocks relying on resource references.
 *
 * @param <T> the type of block handled by the normalizer
 * @version $Id$
 * @since 1.9.0
 */
public abstract class AbstractResourceReferenceXDOMNormalizer<T extends Block> implements XDOMNormalizer
{
    @Inject
    protected Logger logger;

    @Inject
    protected ResourceReferenceNormalizer resourceReferenceNormalizer;

    @Override
    public boolean normalize(XDOM xdom, Parser parser, BlockRenderer blockRenderer)
    {
        boolean normalized = false;

        List<T> blocks =
            xdom.getBlocks(new ClassBlockMatcher(getTypeParameterClass()), Block.Axes.DESCENDANT_OR_SELF);

        if (!blocks.isEmpty()) {
            normalized |= normalize(blocks);
        }

        return normalized;
    }

    /**
     * @return the class of the type parameter.
     */
    protected abstract Class<T> getTypeParameterClass();

    /**
     * Update the given list of blocks with normalized URLs.
     *
     * @param blocks the list of blocks using resource references which will be updated and normalized
     */
    protected abstract boolean normalize(List<T> blocks);

    protected boolean handleQueryStringParameters(T block, ResourceReference newReference)
    {
        // Note: We need to merge the query string parameters coming from the URL (and stored as parameters in
        // the normalized ResourceReference) with any existing "queryString" Block parameters.
        // If a query string parameter name exists in the original link and the same parameter name also exists in
        // the URL but the values are different then skip the normalization for this link block in order not to
        // cause any loss of data! (even though having link block parameters for a URL has no meaning).

        boolean shouldAbortNormalization = false;

        // Parse the query string into a data structure to which we can easily add new items to
        List<NameValuePair> queryStringParameters;
        String queryString = block.getParameter(DocumentResourceReference.QUERY_STRING);
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
                block.setParameter(DocumentResourceReference.QUERY_STRING, newQueryString);
            }

            // Remove the parameters from the newReference since we've now moved them as link block parameters
            newReference.removeParameter(DocumentResourceReference.QUERY_STRING);
        }

        return shouldAbortNormalization;
    }

    protected boolean hasDifferentValue(NameValuePair newParameter, List<NameValuePair> queryStringParameters)
    {
        boolean hasDifferentValue = false;

        for (NameValuePair pair : queryStringParameters) {
            if (newParameter.getName().equals(pair.getName()) && !newParameter.getValue().equals(pair.getValue())) {
                hasDifferentValue = true;

                break;
            }
        }

        return hasDifferentValue;
    }

    protected String formatQueryString(List<NameValuePair> queryStringParameters)
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
