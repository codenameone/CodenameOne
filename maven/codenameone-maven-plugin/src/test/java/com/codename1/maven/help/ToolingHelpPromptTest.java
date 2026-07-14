/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
                .errorSummary("archetype:generate failed");
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
        assertTrue(text.contains("Opening a live chat"));
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
        assertTrue(text.contains("Chat live"));
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
        assertTrue(text.contains("Chat live"));
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
