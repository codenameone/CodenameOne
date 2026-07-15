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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * The interactive "Get help" dialog for the console/CLI surface. It is the terminal
 * equivalent of the described dialog: it shows the captured error, pre-fills the email,
 * offers the optional "what were you trying to do?" box, shows the privacy note, and
 * has Send / Cancel.
 *
 * <p>IO is injected (a {@link BufferedReader} and {@link PrintWriter}) so the whole
 * conversation is unit-testable and so the caller controls where it reads/writes. The
 * {@link ToolingHelpClient} is injected too so tests can point it at a local server.</p>
 *
 * <p><b>Never auto-sends.</b> If the input stream reaches EOF at any prompt (e.g. a
 * non-interactive/piped invocation), or the user declines at the Send confirmation, the
 * method returns {@code null} and nothing is transmitted.</p>
 */
public class ToolingHelpPrompt {

    private final BufferedReader in;
    private final PrintWriter out;
    private final ToolingHelpClient client;
    private final BrowserOpener browser;

    public ToolingHelpPrompt(BufferedReader in, PrintWriter out, ToolingHelpClient client) {
        this(in, out, client, BrowserOpener.DESKTOP);
    }

    public ToolingHelpPrompt(BufferedReader in, PrintWriter out, ToolingHelpClient client, BrowserOpener browser) {
        this.in = in;
        this.out = out;
        this.client = client;
        this.browser = browser;
    }

    /**
     * Runs the dialog.
     *
     * @param base           the captured failure context (component/step/version/error)
     * @param prefilledEmail the signed-in account email, or null if unknown
     * @return the submission result, or {@code null} if the user cancelled / input ended
     *         before an explicit Send (nothing was transmitted in that case)
     */
    public ToolingHelpResponse run(ToolingHelpReport.Builder base, String prefilledEmail) throws IOException {
        return run(base, prefilledEmail, null);
    }

    /**
     * Runs the dialog with optional pre-filled contact details and user note.
     *
     * @param base             the captured failure context
     * @param prefilledEmail   the signed-in account email, or null if unknown
     * @param prefilledMessage the configured note, or null if none was supplied
     * @return the submission result, or {@code null} if the user cancelled
     */
    public ToolingHelpResponse run(ToolingHelpReport.Builder base, String prefilledEmail,
                                   String prefilledMessage) throws IOException {
        // 1. Show a short failure context before gathering the editable fields.
        ToolingHelpReport preview = base.build();
        out.println();
        out.println("Get help from Codename One support");
        out.println("──────────────────────────────────");
        if (preview.getErrorSummary() != null && preview.getErrorSummary().length() > 0) {
            out.println("Error: " + preview.getErrorSummary());
        }
        out.println("Step:  " + orDash(preview.getStep()));
        out.println("Env:   " + orDash(preview.getOs()) + " " + orDash(preview.getOsVersion())
                + " · Java " + orDash(preview.getJavaVersion())
                + " · tooling " + orDash(preview.getToolingVersion()));
        out.println();

        // 2. Email (pre-filled, editable). Enter keeps it; "none" clears it.
        // null == input ended (EOF); "" == the user chose no email.
        String email = promptEmail(prefilledEmail);
        if (email == null) {
            return cancelled();
        }

        // 3. Optional free-text box (pre-filled when -Dmessage was supplied).
        String message = promptMessage(prefilledMessage);
        if (message == null) {
            return cancelled();
        }

        ToolingHelpReport report = base
                .email(email.length() == 0 ? null : email)
                .message(message.length() == 0 ? null : message)
                .build();

        // 4. Show every field that will be transmitted, including the full diagnostic
        // detail, before asking for consent.
        printReportPreview(report);
        out.println();
        out.println(ToolingHelpMessages.PRIVACY_NOTE);

        // 5. Send / Cancel.
        out.print("Send to Codename One support now? [y/N]: ");
        out.flush();
        String answer = readLine();
        if (!isYes(answer)) {
            return cancelled();
        }

        out.println();
        out.println("Sending…");
        ToolingHelpResponse response = client.submit(report);
        // No-email path: open the token-gated support chat that already carries the report.
        boolean browserOpened = false;
        if (response.getOutcome() == ToolingHelpResponse.Outcome.NO_EMAIL) {
            browserOpened = browser != null && browser.open(response.getChatDeepLink());
        }
        printResult(response, browserOpened);
        return response;
    }

    private String promptMessage(String prefilledMessage) throws IOException {
        String prefill = prefilledMessage == null ? "" : prefilledMessage.trim();
        out.println();
        if (prefill.length() > 0) {
            out.print("What were you trying to do? [" + prefill
                    + "] (Enter to keep, or type a new note, or 'none'): ");
        } else {
            out.print("What were you trying to do? (optional, press Enter to skip): ");
        }
        out.flush();
        String typed = readLine();
        if (typed == null) {
            return null;
        }
        typed = typed.trim();
        if (typed.length() == 0) {
            return prefill;
        }
        if ("none".equalsIgnoreCase(typed) || "-".equals(typed)) {
            return "";
        }
        return typed;
    }

    private void printReportPreview(ToolingHelpReport report) {
        out.println();
        out.println("Complete report to be sent");
        out.println("──────────────────────────");
        out.println("Diagnostics can contain stack traces, local file paths, JAVA_HOME, and proxy settings.");
        printField("Component", report.getComponent());
        printField("Tooling version", report.getToolingVersion());
        printField("Step", report.getStep());
        printField("OS", report.getOs());
        printField("OS version", report.getOsVersion());
        printField("Java version", report.getJavaVersion());
        printField("Error summary", report.getErrorSummary());
        printField("Reply email", report.getEmail());
        printField("Your note", report.getMessage());
        out.println("Error detail:");
        out.println(orDash(report.getErrorDetail()));
        out.flush();
    }

    private void printField(String label, String value) {
        out.println(label + ": " + orDash(value));
    }

    private String promptEmail(String prefilledEmail) throws IOException {
        String prefill = prefilledEmail == null ? "" : prefilledEmail.trim();
        if (prefill.length() > 0) {
            out.print("Reply email [" + prefill + "] (Enter to keep, or type a new one, or 'none'): ");
        } else {
            out.print("Reply email (leave blank to open Codename One support chat): ");
        }
        out.flush();
        String typed = readLine();
        if (typed == null) {
            return null; // EOF — the caller cancels
        }
        typed = typed.trim();
        if (typed.length() == 0) {
            return prefill;
        }
        if ("none".equalsIgnoreCase(typed) || "-".equals(typed)) {
            return "";
        }
        return typed;
    }

    private void printResult(ToolingHelpResponse response, boolean browserOpened) {
        out.println();
        List<String> lines = ToolingHelpMessages.resultLines(response, browserOpened);
        for (int i = 0; i < lines.size(); i++) {
            out.println(lines.get(i));
        }
        out.flush();
    }

    private ToolingHelpResponse cancelled() {
        out.println();
        out.println("Cancelled — nothing was sent.");
        out.flush();
        return null;
    }

    private String readLine() throws IOException {
        return in == null ? null : in.readLine();
    }

    private static boolean isYes(String answer) {
        if (answer == null) {
            return false;
        }
        String a = answer.trim().toLowerCase();
        return a.equals("y") || a.equals("yes");
    }

    private static String orDash(String value) {
        return value == null || value.length() == 0 ? "-" : value;
    }
}
