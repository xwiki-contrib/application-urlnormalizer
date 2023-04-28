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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;
import org.xwiki.test.ui.po.editor.ObjectEditPage;
import org.xwiki.test.ui.po.editor.ObjectEditPane;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Test if a document and its XObjects is correctly normalized.
 *
 * @version $Id$
 */
@UITest
class URLNormalizerIT
{
    private String base;
    
    private String absoluteInternalUrl;

    private String escapedAbsoluteInternalUrl;

    private String absoluteExternalUrl;

    @BeforeEach
    public void setUp(TestUtils setup, TestReference testReference) throws Exception
    {
        setup.loginAsSuperAdmin();

        setup.deletePage(testReference);

        this.base = "http://localhost:8080/xwiki";

        this.absoluteInternalUrl = this.base + "/view/Main";

        this.escapedAbsoluteInternalUrl = this.absoluteInternalUrl.replaceAll("://", ":~~/~~/");

        this.absoluteExternalUrl = "http://example.org/some/other/random/link";
    }

    @Test
    void onDocumentUpdate(TestUtils setup, TestReference testReference) throws Exception
    {
        // We want the test to be as fast as possible, therefore, we’ll test every link combination in one single page.
        StringBuilder content = new StringBuilder();
        StringBuilder expectedResultBuilder = new StringBuilder();

        // Test for a simple copy-pasted external URL
        content.append(String.format("%s\n", absoluteExternalUrl));
        expectedResultBuilder.append(String.format("%s\n", absoluteExternalUrl));

        // Test for a simple copy-pasted internal URL
        content.append(String.format("%s\n", absoluteInternalUrl));
        expectedResultBuilder.append(String.format("[[%s>>doc:Main.WebHome]]\n", escapedAbsoluteInternalUrl));

        // Test for a wiki link with an external URL
        content.append(String.format("[[example.org>>%s]]\n", absoluteExternalUrl));
        expectedResultBuilder.append(String.format("[[example.org>>%s]]\n", absoluteExternalUrl));

        // Test for a wiki link with an internal URL
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl));
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]]\n");

        // Test for a wiki link with an internal URL with a query string
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl + "?xpage=xml"));
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome||queryString=\"xpage=xml\"]]\n");

        // Test that we ignore fragments FTM, see https://jira.xwiki.org/browse/URLNORMALZ-11
        content.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl + "#anchor"));
        expectedResultBuilder.append(String.format("[[Main page>>%s]]\n", absoluteInternalUrl + "#anchor"));

        // Test for a classic wiki link
        content.append("[[Main page>>doc:Main.WebHome]]\n");
        expectedResultBuilder.append("[[Main page>>doc:Main.WebHome]]\n");

        // Test for a download wiki link
        content.append(String.format("[[Attachment>>%sbin/download/Main/WebHome/image.png]]\n", this.base));
        expectedResultBuilder.append("[[Attachment>>attach:Main.WebHome@image.png]]\n");

        // Test for a non-view wiki link
        String nonViewWikiLink = String.format("%sbin/edit/Main", this.base);
        content.append(String.format("[[Label>>%s]]\n", nonViewWikiLink));
        expectedResultBuilder.append(String.format("[[Label>>%s]]\n", nonViewWikiLink));

        // Test for a wiki link in an info macro
        content.append(String.format("{{info}}[[Label>>%s]]{{/info}}\n", absoluteInternalUrl));
        expectedResultBuilder.append("{{info}}[[Label>>doc:Main.WebHome]]{{/info}}\n");

        // Test for a wiki link in an HTML macro with wiki=true
        content.append(String.format("\n{{html wiki='true'}}[[Label>>%s]]{{/html}}", absoluteInternalUrl));
        expectedResultBuilder.append("\n{{html wiki=\"true\"}}\n[[Label>>doc:Main.WebHome]]\n{{/html}}");

        ViewPage page = setup.createPage(testReference, "", "URL Normalizer Tests");

        WikiEditPage editPage = page.editWiki();

        editPage.setContent(content.toString());
        editPage.clickSaveAndView(true);

        // We add an xproperty to verify that it's also normalized fine
        setup.addObject(testReference, "XWiki.XWikiComments", "comment", absoluteInternalUrl);

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
