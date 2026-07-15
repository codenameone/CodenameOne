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

import java.util.ArrayList;
import java.util.List;

/**
 * Pure text rendering for the "Get help" flow. Kept UI-agnostic (returns plain lines)
 * so the console prompt, a future Swing/CN1 dialog, or a test can all share the exact
 * same wording &mdash; and so the wording can be asserted directly.
 */
public final class ToolingHelpMessages {

    public static final String CHAT_RESPONSE_NOTE =
            "Codename One support is not staffed 24 hours a day, so an operator may take some time to respond. "
            + "We always respond.";

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
                lines.add("Prefer chat? Open Codename One support chat: " + response.getChatDeepLink());
                lines.add(CHAT_RESPONSE_NOTE);
                break;
            case NO_EMAIL:
                // Path B: no email -> route straight into a token-gated support chat that
                // already carries the report. One action: open the browser.
                if (browserOpened) {
                    lines.add("Opening Codename One support chat in your browser…");
                    lines.add("If it doesn't open, go to: " + response.getChatDeepLink());
                } else {
                    lines.add("Open Codename One support chat — we've attached your report:");
                    lines.add("    " + response.getChatDeepLink());
                }
                lines.add(CHAT_RESPONSE_NOTE);
                break;
            case UNREACHABLE:
            default:
                // Network failure -> never a dead end. Fall back to self-serve + chat.
                lines.add("Couldn't reach Codename One support right now — here's help you can use immediately:");
                lines.add("Self-serve guide for this step: " + response.getHelpArticleUrl());
                lines.add("Support chat:                   " + response.getChatUrl());
                lines.add(CHAT_RESPONSE_NOTE);
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
