/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.help;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure text rendering for the "Get help" flow. Kept UI-agnostic (returns plain lines)
 * so the console prompt, a future Swing/CN1 dialog, or a test can all share the exact
 * same wording &mdash; and so the wording can be asserted directly.
 */
public final class ToolingHelpMessages {

    /** The one-line privacy note shown before Send. */
    public static final String PRIVACY_NOTE =
            "This sends the complete report shown above to Codename One support so we can help. "
            + "Nothing is sent unless you click Send.";

    private ToolingHelpMessages() {
    }

    /**
     * The banner printed next to a local failure telling the user help is available.
     * This is the "Get help" affordance for the CLI/Maven surface &mdash; it appears on
     * failure but sends nothing; the user opts in by running the shown command.
     */
    public static List<String> failureAffordance(String step) {
        List<String> lines = new ArrayList<String>();
        lines.add("");
        lines.add("── Get help ──");
        lines.add("This step failed. To send it to Codename One support and get help, run:");
        lines.add("    mvn cn1:get-help");
        lines.add("The complete report is shown for review before you confirm — nothing is sent automatically.");
        lines.add("");
        return lines;
    }

    /**
     * Renders the result of a submission into user-facing lines, implementing the two
     * response paths plus the unreachable fallback.
     *
     * @param browserOpened for the no-email path, whether the chat page was actually
     *                      launched (so we say "opened" vs. hand over the URL)
     */
    public static List<String> resultLines(ToolingHelpResponse response, boolean browserOpened) {
        List<String> lines = new ArrayList<String>();
        switch (response.getOutcome()) {
            case EMAILED:
                // Path A: email supplied -> the answer arrives by email.
                lines.add(emailedHeadline(response));
                lines.add("The reply arrives by email — you don't need to keep anything open.");
                lines.add("Prefer real-time? Chat live: " + response.getChatDeepLink());
                break;
            case NO_EMAIL:
                // Path B: no email -> route straight into a token-gated live chat that
                // already carries the report. One action: open the browser.
                if (browserOpened) {
                    lines.add("Opening a live chat with Codename One support in your browser…");
                    lines.add("If it doesn't open, go to: " + response.getChatDeepLink());
                } else {
                    lines.add("Chat live with Codename One support — we've attached your report:");
                    lines.add("    " + response.getChatDeepLink());
                }
                break;
            case UNREACHABLE:
            default:
                // Network failure -> never a dead end. Fall back to self-serve + chat.
                lines.add("Couldn't reach Codename One support right now — here's help you can use immediately:");
                lines.add("Self-serve guide for this step: " + response.getHelpArticleUrl());
                lines.add("Chat live:                      " + response.getChatUrl());
                break;
        }
        return lines;
    }

    private static String emailedHeadline(ToolingHelpResponse response) {
        String email = response.getEmail();
        Integer ticket = response.getTicketId();
        if (ticket != null) {
            return "We've got it — #" + ticket + ". We'll reply to you at " + email + ".";
        }
        // Delivered but the server didn't echo a reference; still a true promise.
        return "We've got it. We'll reply to you at " + email + ".";
    }
}
