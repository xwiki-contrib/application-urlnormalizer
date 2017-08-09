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
import org.openqa.selenium.By;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
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
    private String absoluteInternalUrl;

    private String escapedAbsoluteInternalUrl;

    private String absoluteExternalUrl;

    @Before
    public void setUp() throws Exception
    {
        getUtil().loginAsAdmin();
        getUtil().createUserAndLogin(getTestClassName(), "password", "usertype", "Advanced");

        getUtil().deletePage(getTestClassName(), getTestMethodName());
    }

    @Before
    public void generateURLs() throws Exception
    {
        absoluteInternalUrl = String.format("%sbin/view/Main", getUtil().getBaseURL());

        escapedAbsoluteInternalUrl = String.format("%sbin/view/Main",
                getUtil().getBaseURL().replaceAll("://", ":~~/~~/"));

        absoluteExternalUrl = "http://example.org/some/other/random/link";
    }

    @Test
    public void onDocumentUpdate() throws Exception
    {
        // We want the test to be as fast as possible, therefore, we’ll test every link combination in one single page.
        StringBuilder content = new StringBuilder();
        StringBuilder expectedResultBuilder = new StringBuilder();

        // Test for a simple copy-pasted external URL
        content.append(String.format("%s\n", absoluteExternalUrl));
        expectedResultBuilder.append(String.format("%s ", absoluteExternalUrl));

        // Test for a simple copy-pasted internal URL
        content.append(String.format("%s\n", absoluteInternalUrl));
        expectedResultBuilder.append(String.format("[[%s>>doc:Main.WebHome]] ", escapedAbsoluteInternalUrl));

        // Test for a wiki link with an external URL
        content.append(String.format("[[example.org>>%s]]\n", absoluteExternalUrl));
        expectedResultBuilder.append(String.format("[[example.org>>%s]] ", absoluteExternalUrl));

        // Test for a wiki link with an internal URL
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl));
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]] ");

        // Test for a wiki link with an internal URL with a query string
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl + "?xpage=xml"));
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome||queryString=\"xpage=xml\"]] ");

        // Test that we ignore fragments FTM, see https://jira.xwiki.org/browse/URLNORMALZ-11
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl + "#anchor"));
        expectedResultBuilder.append(String.format("[[Main page>>%s]] ", absoluteInternalUrl + "#anchor"));

        // Test for a classic wiki link
        content.append("[[Main page>>doc:Main.WebHome]]\n");
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]] ");

        // Test for a non-view wiki link
        String nonViewWikiLink = String.format("%sbin/edit/Main", getUtil().getBaseURL());
        content.append(String.format("[[Label>>%s]]\n", nonViewWikiLink));
        expectedResultBuilder.append(String.format("[[Label>>%s]]", nonViewWikiLink));

        ViewPage page = getUtil().createPage(getTestClassName(), getTestMethodName(), "", "URL Normalizer Tests");

        WikiEditPage editPage = page.editWiki();

        editPage.setContent(content.toString());
        editPage.clickSaveAndView(true);

        // We add an xproperty to verify that it's also normalized fine
        getUtil().addObject(getTestClassName(), getTestMethodName(), "XWiki.XWikiComments", "comment",
            absoluteInternalUrl);

        // Return to the edit page
        editPage = (new ViewPage()).editWiki();
        assertEquals(expectedResultBuilder.toString(), editPage.getContent());
        page = editPage.clickCancel();

        ObjectEditPage objectEditPage = page.editObjects();
        ObjectEditPane objectEditPane = objectEditPage.getObjectsOfClass("XWiki.XWikiComments").get(0);
        assertEquals(String.format("[[%s>>doc:Main.WebHome]]", escapedAbsoluteInternalUrl),
            objectEditPane.getFieldValue(By.id("XWiki.XWikiComments_0_comment")));
    }
}
