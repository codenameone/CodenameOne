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
package com.codename1.maven;

import com.codename1.maven.help.BrowserOpener;
import com.codename1.maven.help.ToolingHelp;
import com.codename1.maven.help.ToolingHelpClient;
import com.codename1.maven.help.ToolingHelpMessages;
import com.codename1.maven.help.ToolingHelpPrompt;
import com.codename1.maven.help.ToolingHelpReport;
import com.codename1.maven.help.ToolingHelpResponse;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.List;

/**
 * The user-initiated "Get help" action: {@code mvn cn1:get-help}.
 *
 * <p>This is what a failed step points the user to. It picks up the most recent local
 * failure (persisted by {@link ToolingHelp#offerAfterFailure}), pre-fills the signed-in
 * account email, opens an interactive dialog to confirm the email and add an optional
 * note, and &mdash; only on an explicit Send &mdash; posts the report to Codename One
 * support. It runs outside a project ({@code requiresProject = false}) so it works even
 * when project creation or tooling install is what failed.</p>
 *
 * <p>Non-interactive callers (CI, scripts) can pass {@code -Dsend=true} to skip the
 * prompt; that flag <em>is</em> the explicit action, so nothing is sent without it.</p>
 */
@Mojo(name = "get-help", requiresProject = false)
public class GetHelpMojo extends AbstractMojo {

    @Parameter(property = "component", defaultValue = ToolingHelp.COMPONENT_MAVEN_PLUGIN)
    private String component;

    @Parameter(property = "step")
    private String step;

    @Parameter(property = "errorSummary")
    private String errorSummary;

    @Parameter(property = "errorDetail")
    private String errorDetail;

    /** Overrides the auto-resolved account email. */
    @Parameter(property = "email")
    private String email;

    /** Pre-fills the optional "what were you trying to do?" note. */
    @Parameter(property = "message")
    private String message;

    /** When true, sends immediately without prompting (the explicit action for CI). */
    @Parameter(property = "send", defaultValue = "false")
    private boolean send;

    /** Endpoint override, primarily for tests. */
    @Parameter(property = "cn1.help.endpoint")
    private String endpoint;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ToolingHelpReport.Builder base = buildBaseReport();
        String resolvedEmail = firstNonEmpty(email, ToolingHelp.resolveEmail());
        ToolingHelpClient client = endpoint == null || endpoint.trim().length() == 0
                ? new ToolingHelpClient()
                : new ToolingHelpClient(endpoint);

        if (send) {
            sendNonInteractive(base, resolvedEmail, client);
            return;
        }
        runInteractive(base, resolvedEmail, client);
    }

    private ToolingHelpReport.Builder buildBaseReport() {
        ToolingHelpReport.Builder base = ToolingHelp.loadLastFailure();
        if (base == null) {
            base = ToolingHelpReport.builder()
                    .step("other")
                    .errorSummary("User-initiated help request (no captured failure)");
        }
        // Apply any explicit overrides on top of the captured context.
        base.component(firstNonEmpty(component, ToolingHelp.COMPONENT_MAVEN_PLUGIN));
        if (notBlank(step)) {
            base.step(step);
        }
        if (notBlank(errorSummary)) {
            base.errorSummary(errorSummary);
        }
        if (notBlank(errorDetail)) {
            base.errorDetail(errorDetail);
        }
        base.toolingVersion(resolveToolingVersion(base));
        return base;
    }

    private String resolveToolingVersion(ToolingHelpReport.Builder base) {
        String captured = base.build().getToolingVersion();
        if (notBlank(captured)) {
            return captured;
        }
        return ToolingHelp.pluginVersion();
    }

    private void sendNonInteractive(ToolingHelpReport.Builder base, String resolvedEmail,
                                    ToolingHelpClient client) {
        ToolingHelpReport report = base
                .email(resolvedEmail)
                .message(message)
                .build();
        getLog().info("Sending help request to Codename One support…");
        ToolingHelpResponse response = client.submit(report);
        // No-email path: open the token-gated support chat that already carries the report.
        boolean browserOpened = false;
        if (response.getOutcome() == ToolingHelpResponse.Outcome.NO_EMAIL) {
            browserOpened = BrowserOpener.DESKTOP.open(response.getChatDeepLink());
        }
        printResult(response, browserOpened);
        clearOnDelivery(response);
    }

    private void runInteractive(ToolingHelpReport.Builder base, String resolvedEmail,
                                ToolingHelpClient client) throws MojoExecutionException {
        Charset charset = Charset.defaultCharset();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, charset));
        PrintWriter writer = new PrintWriter(new java.io.OutputStreamWriter(System.out, charset), true);
        ToolingHelpPrompt prompt = new ToolingHelpPrompt(reader, writer, client);
        try {
            ToolingHelpResponse response = prompt.run(base, resolvedEmail, message);
            clearOnDelivery(response);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to read console input for get-help", ex);
        }
    }

    private ToolingHelpResponse printResult(ToolingHelpResponse response, boolean browserOpened) {
        List<String> lines = ToolingHelpMessages.resultLines(response, browserOpened);
        for (int i = 0; i < lines.size(); i++) {
            getLog().info(lines.get(i));
        }
        return response;
    }

    /** Once the report is delivered, drop the persisted failure so it isn't resent. */
    private void clearOnDelivery(ToolingHelpResponse response) {
        if (response != null && response.wasDelivered()) {
            ToolingHelp.clearLastFailure();
        }
    }

    private static String firstNonEmpty(String a, String b) {
        if (notBlank(a)) {
            return a.trim();
        }
        return notBlank(b) ? b.trim() : null;
    }

    private static boolean notBlank(String s) {
        return s != null && s.trim().length() > 0;
    }
}
