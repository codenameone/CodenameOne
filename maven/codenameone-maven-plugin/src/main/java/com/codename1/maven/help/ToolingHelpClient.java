/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.help;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Posts a {@link ToolingHelpReport} to the fixed Codename One support endpoint and
 * turns the reply into a {@link ToolingHelpResponse}.
 *
 * <p>Design rules baked in from the spec:</p>
 * <ul>
 *   <li><b>Never hard-fail.</b> Any network problem or unexpected status degrades to
 *       {@link ToolingHelpResponse.Outcome#UNREACHABLE} (self-serve + chat) &mdash; the
 *       caller always gets a usable response, never an exception.</li>
 *   <li><b>No auth, no retries.</b> Rate limiting is handled server-side.</li>
 *   <li><b>Two paths.</b> Whether an email was supplied decides Path A vs Path B; that
 *       decision lives here so the UI just renders.</li>
 * </ul>
 */
public class ToolingHelpClient {

    /** The fixed production endpoint from the wire contract. */
    public static final String DEFAULT_ENDPOINT = "https://cloud.codenameone.com/api/v2/tooling/help";

    /** Where the console chat widget loads with the user's identity (contract default). */
    public static final String DEFAULT_CHAT_URL = "https://cloud.codenameone.com/console/";

    private static final int CONNECT_TIMEOUT_MS = 20000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_RESPONSE_BYTES = 64 * 1024;

    /**
     * Self-serve help articles keyed by {@code step}. These are the synchronous
     * fallback shown when there is no email (Path B) or support is unreachable.
     *
     * <p>NOTE: confirm the canonical help.codenameone.com article slugs with the team;
     * these point at stable guides that already exist and are safe defaults. Update in
     * one place here when the dedicated articles land.</p>
     */
    private static final Map<String, String> HELP_ARTICLES = new HashMap<String, String>();
    private static final String DEFAULT_HELP_ARTICLE = "https://www.codenameone.com/getting-started.html";
    static {
        HELP_ARTICLES.put("install", "https://www.codenameone.com/getting-started.html");
        HELP_ARTICLES.put("create_project", "https://www.codenameone.com/getting-started.html");
        HELP_ARTICLES.put("configure",
                "https://www.codenameone.com/blog/automatically-generate-provisioning-profiles-certificates-signing-your-app.html");
        HELP_ARTICLES.put("local_run", "https://www.codenameone.com/manual/getting-started.html");
        HELP_ARTICLES.put("build_submit", "https://www.codenameone.com/manual/appendix-build-server.html");
        HELP_ARTICLES.put("other", DEFAULT_HELP_ARTICLE);
    }

    private final String endpoint;

    public ToolingHelpClient() {
        this(DEFAULT_ENDPOINT);
    }

    /** @param endpoint override for tests; production uses {@link #DEFAULT_ENDPOINT}. */
    public ToolingHelpClient(String endpoint) {
        this.endpoint = endpoint == null || endpoint.trim().length() == 0 ? DEFAULT_ENDPOINT : endpoint.trim();
    }

    /** Resolves the self-serve help article for a step; never null. */
    public static String helpArticleUrl(String step) {
        if (step != null) {
            String url = HELP_ARTICLES.get(step.trim());
            if (url != null) {
                return url;
            }
        }
        return DEFAULT_HELP_ARTICLE;
    }

    /**
     * Submits the report. Guaranteed not to throw &mdash; on any failure it returns an
     * {@link ToolingHelpResponse.Outcome#UNREACHABLE} response with the step's self-serve
     * article and the default chat URL.
     */
    public ToolingHelpResponse submit(ToolingHelpReport report) {
        String article = helpArticleUrl(report.getStep());
        try {
            HttpResult result = post(report.toJson(), userAgent(report));
            if (result.code < 200 || result.code >= 300) {
                // Contract says the server always returns 200; anything else means we
                // cannot trust the delivery, so fall back rather than promise a reply.
                return unreachable(article);
            }
            Integer ticketId = extractInt(result.body, "ticketId");
            String token = extractString(result.body, "token");
            String chatUrl = firstNonEmpty(extractString(result.body, "chatUrl"), DEFAULT_CHAT_URL);
            if (report.hasEmail()) {
                return new ToolingHelpResponse(ToolingHelpResponse.Outcome.EMAILED,
                        ticketId, token, report.getEmail(), chatUrl, article);
            }
            return new ToolingHelpResponse(ToolingHelpResponse.Outcome.NO_EMAIL,
                    ticketId, token, null, chatUrl, article);
        } catch (IOException ex) {
            return unreachable(article);
        } catch (RuntimeException ex) {
            return unreachable(article);
        }
    }

    private static ToolingHelpResponse unreachable(String article) {
        return new ToolingHelpResponse(ToolingHelpResponse.Outcome.UNREACHABLE,
                null, null, null, DEFAULT_CHAT_URL, article);
    }

    /** Product name reported in the User-Agent (also useful for server-side triage). */
    static final String USER_AGENT_PRODUCT = "CodenameOne-Tooling-Help";

    /** e.g. {@code CodenameOne-Tooling-Help/8.0.1 (maven-plugin)} */
    static String userAgent(ToolingHelpReport report) {
        String version = report.getToolingVersion();
        String component = report.getComponent();
        StringBuilder sb = new StringBuilder(USER_AGENT_PRODUCT);
        if (version != null && version.length() > 0) {
            sb.append('/').append(version);
        }
        if (component != null && component.length() > 0) {
            sb.append(" (").append(component).append(')');
        }
        return sb.toString();
    }

    private HttpResult post(String jsonBody, String userAgent) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(endpoint).openConnection();
        try {
            con.setRequestMethod("POST");
            con.setConnectTimeout(CONNECT_TIMEOUT_MS);
            con.setReadTimeout(READ_TIMEOUT_MS);
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            // A real User-Agent is REQUIRED: the WAF in front of the endpoint 403s the
            // default "Java/<version>" agent, which would silently turn every send into
            // the offline fallback for exactly this feature's audience (JDK 8 users).
            con.setRequestProperty("User-Agent", userAgent);
            byte[] payload = jsonBody.getBytes(StandardCharsets.UTF_8);
            OutputStream out = con.getOutputStream();
            try {
                out.write(payload);
                out.flush();
            } finally {
                out.close();
            }
            int code = con.getResponseCode();
            String body = readBody(code >= 400 ? con.getErrorStream() : con.getInputStream());
            return new HttpResult(code, body);
        } finally {
            con.disconnect();
        }
    }

    private static String readBody(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            int total = 0;
            while ((read = in.read(chunk)) != -1) {
                int allowed = Math.min(read, MAX_RESPONSE_BYTES - total);
                if (allowed > 0) {
                    buffer.write(chunk, 0, allowed);
                    total += allowed;
                }
                if (total >= MAX_RESPONSE_BYTES) {
                    break;
                }
            }
            return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    /**
     * Extracts a numeric JSON value by key, e.g. {@code "ticketId": 4213}. Deliberately
     * tiny and dependency-free (the response shape is a flat, trusted object) &mdash;
     * mirrors the hand-rolled JSON reading already used elsewhere in the plugin.
     */
    static Integer extractInt(String json, String key) {
        if (json == null) {
            return null;
        }
        int idx = findValueStart(json, key);
        if (idx < 0) {
            return null;
        }
        int start = idx;
        int end = idx;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || (end == start && json.charAt(end) == '-'))) {
            end++;
        }
        if (end == start) {
            return null;
        }
        try {
            return Integer.valueOf(Integer.parseInt(json.substring(start, end)));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** Extracts a JSON string value by key, e.g. {@code "chatUrl": "https://..."}. */
    static String extractString(String json, String key) {
        if (json == null) {
            return null;
        }
        int idx = findValueStart(json, key);
        if (idx < 0 || idx >= json.length() || json.charAt(idx) != '"') {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int i = idx + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    default: sb.append(next);
                }
                i += 2;
                continue;
            }
            if (c == '"') {
                return sb.toString();
            }
            sb.append(c);
            i++;
        }
        return null;
    }

    /** Returns the index just after the colon for {@code "key"}, skipping whitespace. */
    private static int findValueStart(String json, String key) {
        String quoted = "\"" + key + "\"";
        int idx = json.indexOf(quoted);
        if (idx < 0) {
            return -1;
        }
        int colon = json.indexOf(':', idx + quoted.length());
        if (colon < 0) {
            return -1;
        }
        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        return start;
    }

    private static String firstNonEmpty(String a, String b) {
        if (a != null && a.trim().length() > 0) {
            return a.trim();
        }
        return b;
    }

    static final class HttpResult {
        final int code;
        final String body;

        HttpResult(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
