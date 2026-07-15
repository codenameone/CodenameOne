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

/**
 * Immutable payload for a user-initiated "Get help" support request.
 *
 * <p>This mirrors the fixed wire contract of
 * {@code POST https://cloud.codenameone.com/api/v2/tooling/help}. Every field is
 * optional on the wire, the server truncates oversized values and always returns
 * {@code 200} &mdash; but we still truncate <em>client side</em> (see the caps below)
 * so the payload stays small and predictable.</p>
 *
 * <p>Nothing here talks to the network or a UI; it only captures context and renders
 * JSON. That keeps it trivially unit-testable and reusable from any surface (Maven
 * plugin console, Certificate Wizard Swing dialog, etc.).</p>
 */
public final class ToolingHelpReport {

    /** Cap for the short identifier-ish fields (component, step, os, versions). */
    public static final int SHORT_FIELD_MAX = 64;
    /** Cap for the one-line-ish fields (errorSummary, email, message). */
    public static final int MEDIUM_FIELD_MAX = 512;
    /** Cap for the free-form log/stack field (~16&nbsp;KB). */
    public static final int ERROR_DETAIL_MAX = 16 * 1024;

    private final String component;
    private final String toolingVersion;
    private final String step;
    private final String os;
    private final String osVersion;
    private final String javaVersion;
    private final String errorSummary;
    private final String errorDetail;
    private final String email;
    private final String message;

    private ToolingHelpReport(Builder b) {
        this.component = truncate(b.component, SHORT_FIELD_MAX);
        this.toolingVersion = truncate(b.toolingVersion, SHORT_FIELD_MAX);
        this.step = truncate(b.step, SHORT_FIELD_MAX);
        this.os = truncate(b.os, SHORT_FIELD_MAX);
        this.osVersion = truncate(b.osVersion, SHORT_FIELD_MAX);
        this.javaVersion = truncate(b.javaVersion, SHORT_FIELD_MAX);
        this.errorSummary = truncate(collapseToLine(b.errorSummary), MEDIUM_FIELD_MAX);
        this.errorDetail = truncate(b.errorDetail, ERROR_DETAIL_MAX);
        this.email = truncate(normalizeEmail(b.email), MEDIUM_FIELD_MAX);
        this.message = truncate(b.message, MEDIUM_FIELD_MAX);
    }

    /**
     * Starts a report pre-filled with the current environment (os / osVersion /
     * javaVersion from system properties). Callers only need to add the
     * component, step, version and error details.
     */
    public static Builder builder() {
        return new Builder()
                .os(System.getProperty("os.name"))
                .osVersion(System.getProperty("os.version"))
                .javaVersion(System.getProperty("java.version"));
    }

    public String getComponent() {
        return component;
    }

    public String getToolingVersion() {
        return toolingVersion;
    }

    public String getStep() {
        return step;
    }

    public String getOs() {
        return os;
    }

    public String getOsVersion() {
        return osVersion;
    }

    public String getJavaVersion() {
        return javaVersion;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    /** May be empty/null when the user is not signed in. */
    public String getEmail() {
        return email;
    }

    public boolean hasEmail() {
        return email != null && email.trim().length() > 0;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Renders the payload as a compact JSON object. Fields that are null or empty
     * are omitted entirely &mdash; notably {@code email} is omitted when unknown, as
     * the contract requires ("omit/empty if truly unknown").
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean[] first = new boolean[] { true };
        appendField(sb, first, "component", component);
        appendField(sb, first, "toolingVersion", toolingVersion);
        appendField(sb, first, "step", step);
        appendField(sb, first, "os", os);
        appendField(sb, first, "osVersion", osVersion);
        appendField(sb, first, "javaVersion", javaVersion);
        appendField(sb, first, "errorSummary", errorSummary);
        appendField(sb, first, "errorDetail", errorDetail);
        appendField(sb, first, "email", email);
        appendField(sb, first, "message", message);
        sb.append('}');
        return sb.toString();
    }

    private static void appendField(StringBuilder sb, boolean[] first, String name, String value) {
        if (value == null || value.length() == 0) {
            return;
        }
        if (!first[0]) {
            sb.append(',');
        }
        first[0] = false;
        sb.append('"').append(name).append("\":");
        appendJsonString(sb, value);
    }

    static void appendJsonString(StringBuilder sb, String value) {
        sb.append('"');
        int len = value.length();
        for (int i = 0; i < len; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                default:
                    if (c < 0x20) {
                        sb.append("\\u");
                        String hex = Integer.toHexString(c);
                        for (int p = hex.length(); p < 4; p++) {
                            sb.append('0');
                        }
                        sb.append(hex);
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(0, max);
    }

    /** errorSummary is meant to be one line; flatten any newlines the caller passed. */
    static String collapseToLine(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\s*[\\r\\n]+\\s*", " ").trim();
    }

    private static String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    /** Fluent builder; see {@link ToolingHelpReport#builder()}. */
    public static final class Builder {
        private String component;
        private String toolingVersion;
        private String step;
        private String os;
        private String osVersion;
        private String javaVersion;
        private String errorSummary;
        private String errorDetail;
        private String email;
        private String message;

        public Builder component(String v) {
            this.component = v;
            return this;
        }

        public Builder toolingVersion(String v) {
            this.toolingVersion = v;
            return this;
        }

        public Builder step(String v) {
            this.step = v;
            return this;
        }

        public Builder os(String v) {
            this.os = v;
            return this;
        }

        public Builder osVersion(String v) {
            this.osVersion = v;
            return this;
        }

        public Builder javaVersion(String v) {
            this.javaVersion = v;
            return this;
        }

        public Builder errorSummary(String v) {
            this.errorSummary = v;
            return this;
        }

        public Builder errorDetail(String v) {
            this.errorDetail = v;
            return this;
        }

        public Builder email(String v) {
            this.email = v;
            return this;
        }

        public Builder message(String v) {
            this.message = v;
            return this;
        }

        public ToolingHelpReport build() {
            return new ToolingHelpReport(this);
        }
    }
}
