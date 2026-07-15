/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.maven.help;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ToolingHelpPromptTest {

    private ToolingHelpReport.Builder base() {
        return ToolingHelpReport.builder()
                .component("maven-plugin")
                .step("create_project")
                .toolingVersion("7.0.153")
                .errorSummary("archetype:generate failed")
                .errorDetail("Command: mvn package\nJAVA_HOME: /opt/jdk8\nProxy: https=proxy.example:443\n"
                        + "java.lang.IllegalStateException: failed at /Users/alice/private-project");
    }

    @Test
    public void keepsPrefilledEmail_andSends_showsPathA() throws Exception {
        RecordingClient client = RecordingClient.emailing(4213, "tok-a");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        // Enter (keep email) / message / y (send)
        ToolingHelpResponse r = rawRun(client, browser, out, "\nBuilding my first app\ny\n", "dev@example.com");

        assertEquals("submit must be called exactly once", 1, client.submitCount);
        assertEquals("dev@example.com", client.lastReport.getEmail());
        assertEquals("Building my first app", client.lastReport.getMessage());
        assertTrue(r != null);
        assertTrue(out.toString().contains("We've got it — #4213"));
        assertTrue(out.toString().contains("reply to you at dev@example.com"));
        // Email path must NOT auto-open a browser.
        assertNull(browser.openedUrl);
    }

    @Test
    public void showsCompleteDiagnosticPayloadBeforeConfirmation() throws Exception {
        RecordingClient client = RecordingClient.emailing(8, "t");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();

        rawRun(client, browser, out, "\n\nn\n", "dev@example.com");

        assertEquals(0, client.submitCount);
        String text = out.toString();
        assertTrue(text.contains("Complete report to be sent"));
        assertTrue(text.contains("stack traces, local file paths, JAVA_HOME, and proxy settings"));
        assertTrue(text.contains("JAVA_HOME: /opt/jdk8"));
        assertTrue(text.contains("Proxy: https=proxy.example:443"));
        assertTrue(text.contains("/Users/alice/private-project"));
    }

    @Test
    public void keepsConfiguredMessageWhenInteractiveInputIsBlank() throws Exception {
        RecordingClient client = RecordingClient.emailing(9, "t");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        BufferedReader reader = new BufferedReader(new StringReader("\n\ny\n"));
        PrintWriter writer = new PrintWriter(out);
        ToolingHelpPrompt prompt = new ToolingHelpPrompt(reader, writer, client, browser);

        prompt.run(base(), "dev@example.com", "Configured Maven note");
        writer.flush();

        assertEquals(1, client.submitCount);
        assertEquals("Configured Maven note", client.lastReport.getMessage());
        assertTrue(out.toString().contains("[Configured Maven note]"));
        assertTrue(out.toString().contains("Your note: Configured Maven note"));
    }

    @Test
    public void eofBeforeSend_sendsNothing() throws Exception {
        RecordingClient client = RecordingClient.emailing(1, "t");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        ToolingHelpResponse response = rawRun(client, browser, out, "", "dev@example.com");

        assertNull(response);
        assertEquals("nothing may be sent on EOF", 0, client.submitCount);
        assertNull(browser.openedUrl);
        assertTrue(out.toString().contains("Cancelled"));
    }

    @Test
    public void declineAtConfirm_sendsNothing() throws Exception {
        RecordingClient client = RecordingClient.emailing(1, "t");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        ToolingHelpResponse response = rawRun(client, browser, out, "\n\nn\n", "dev@example.com");

        assertNull(response);
        assertEquals(0, client.submitCount);
        assertNull(browser.openedUrl);
        assertTrue(out.toString().contains("Cancelled"));
    }

    @Test
    public void typingNoneClearsEmail_opensChatDeepLink() throws Exception {
        RecordingClient client = RecordingClient.noEmail("tok-b");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        // none (clear email) / skip / y
        rawRun(client, browser, out, "none\n\ny\n", "dev@example.com");

        assertEquals(1, client.submitCount);
        assertFalse("email must be cleared", client.lastReport.hasEmail());
        // The no-email path opens the token-gated chat page.
        String expectedDeepLink = ToolingHelpClient.DEFAULT_CHAT_URL + "#t=tok-b";
        assertEquals(expectedDeepLink, browser.openedUrl);
        String text = out.toString();
        assertTrue(text.contains("Opening Codename One support chat"));
        assertTrue(text.contains("not staffed 24 hours a day"));
        assertTrue(text.contains("We always respond"));
        assertFalse("must not promise a reply when no email", text.contains("reply to you at"));
    }

    @Test
    public void noEmail_whenBrowserCannotOpen_printsDeepLink() throws Exception {
        RecordingClient client = RecordingClient.noEmail("tok-c");
        RecordingBrowser browser = new RecordingBrowser(false); // headless: open() returns false
        StringWriter out = new StringWriter();
        rawRun(client, browser, out, "none\n\ny\n", "dev@example.com");

        String text = out.toString();
        assertTrue(text.contains(ToolingHelpClient.DEFAULT_CHAT_URL + "#t=tok-c"));
        assertTrue(text.contains("Open Codename One support chat"));
        assertTrue(text.contains("We always respond"));
    }

    @Test
    public void typingNewEmailOverridesPrefill() throws Exception {
        RecordingClient client = RecordingClient.emailing(7, "t");
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        rawRun(client, browser, out, "new@example.com\n\ny\n", "old@example.com");

        assertEquals(1, client.submitCount);
        assertEquals("new@example.com", client.lastReport.getEmail());
    }

    @Test
    public void networkFailureAtSend_showsFallbackNotDeadEnd() throws Exception {
        RecordingClient client = RecordingClient.unreachable();
        RecordingBrowser browser = new RecordingBrowser(true);
        StringWriter out = new StringWriter();
        rawRun(client, browser, out, "\n\ny\n", "dev@example.com");

        assertEquals(1, client.submitCount);
        assertNull("unreachable must not try to open a browser", browser.openedUrl);
        String text = out.toString();
        assertTrue(text.contains("Couldn't reach"));
        assertTrue(text.contains("Support chat"));
        assertTrue(text.contains("We always respond"));
        assertTrue(text.contains("codenameone.com"));
    }

    private ToolingHelpResponse rawRun(RecordingClient client, RecordingBrowser browser, StringWriter out,
                                       String input, String email) throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(input));
        PrintWriter writer = new PrintWriter(out);
        ToolingHelpPrompt prompt = new ToolingHelpPrompt(reader, writer, client, browser);
        ToolingHelpResponse response = prompt.run(base(), email);
        writer.flush();
        return response;
    }

    /** A browser opener that records the URL instead of launching anything. */
    private static final class RecordingBrowser implements BrowserOpener {
        String openedUrl;
        private final boolean succeeds;

        RecordingBrowser(boolean succeeds) {
            this.succeeds = succeeds;
        }

        @Override
        public boolean open(String url) {
            this.openedUrl = url;
            return succeeds;
        }
    }

    /** A client that records the submitted report instead of hitting the network. */
    private static final class RecordingClient extends ToolingHelpClient {
        int submitCount;
        ToolingHelpReport lastReport;
        private final ToolingHelpResponse.Outcome outcome;
        private final Integer ticketId;
        private final String token;

        private RecordingClient(ToolingHelpResponse.Outcome outcome, Integer ticketId, String token) {
            super("http://unused.invalid/");
            this.outcome = outcome;
            this.ticketId = ticketId;
            this.token = token;
        }

        static RecordingClient emailing(int ticketId, String token) {
            return new RecordingClient(ToolingHelpResponse.Outcome.EMAILED, Integer.valueOf(ticketId), token);
        }

        static RecordingClient noEmail(String token) {
            return new RecordingClient(ToolingHelpResponse.Outcome.NO_EMAIL, null, token);
        }

        static RecordingClient unreachable() {
            return new RecordingClient(ToolingHelpResponse.Outcome.UNREACHABLE, null, null);
        }

        @Override
        public ToolingHelpResponse submit(ToolingHelpReport report) {
            submitCount++;
            lastReport = report;
            String article = ToolingHelpClient.helpArticleUrl(report.getStep());
            String email = outcome == ToolingHelpResponse.Outcome.EMAILED ? report.getEmail() : null;
            return new ToolingHelpResponse(outcome, ticketId, token, email, DEFAULT_CHAT_URL, article);
        }
    }
}
