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
package org.xwiki.contrib.urlnormalizer.test.ui;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.Assert.assertEquals;

/**
 * Test if a document and its XObjects is correctly normalized.
 *
 * @version $Id$
 * @since 1.1.1
 */
public class URLNormalizerTest extends AbstractTest
{
    private static final String ABSOLUTE_INTERNAL_URL = "http://localhost:8080/xwiki/bin/view/Main";

    private static final String ESCAPED_ABSOLUTE_INTERNAL_URL = "http:~~/~~/localhost:8080/xwiki/bin/view/Main";

    private static final String ABSOLUTE_EXTERNAL_URL = "http://example.org/some/other/random/link";

    @Before
    public void cleanUpPages() throws Exception
    {
        getUtil().deletePage(getTestClassName(), "Main");
    }

    @Test
    public void onDocumentUpdate() throws Exception
    {
        /**
         * We want the test to be as fast as possible, therefore, we’ll test every link combination in one single page.
         */

        StringBuilder builder = new StringBuilder();
        StringBuilder expectedResultBuilder = new StringBuilder();

        // Test for a simple copy-pasted external URL
        builder.append(String.format("%s\n", ABSOLUTE_EXTERNAL_URL));
        expectedResultBuilder.append(String.format("%s\n", ABSOLUTE_EXTERNAL_URL));

        // Test for a simple copy-pasted internal URL
        builder.append(String.format("%s\n", ABSOLUTE_INTERNAL_URL));
        expectedResultBuilder.append(String.format("[[%s>>doc:Main.WebHome]]\n", ESCAPED_ABSOLUTE_INTERNAL_URL));

        // Test for a wiki link with an external URL
        builder.append(String.format("[[example.org>>%s]]\n", ABSOLUTE_EXTERNAL_URL));
        expectedResultBuilder.append(String.format("[[example.org>>%s]]\n", ABSOLUTE_EXTERNAL_URL));

        // Test for a wiki link with an internal URL
        builder.append(String.format("[[Main page>>%s]]\n", ABSOLUTE_INTERNAL_URL));
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]]\n");

        // Test for a classic wiki link
        builder.append("[[Main page>>doc:Main.WebHome]]\n");
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]]\n");

        String content = builder.toString();
        String expectedContent = expectedResultBuilder.toString();

        ViewPage page = getUtil().createPage(getTestClassName(), "Main", content, "URL Normalizer Tests");

        WikiEditPage editPage = page.editWiki();

        assertEquals(expectedContent, editPage.getContent());
    }
}
