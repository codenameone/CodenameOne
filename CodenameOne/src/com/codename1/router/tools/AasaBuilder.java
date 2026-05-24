/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.router.tools;

import java.util.ArrayList;
import java.util.List;

/// Generates an `apple-app-site-association` (AASA) JSON payload for iOS
/// Universal Links. The output is intended to be hosted at
/// `https://your.domain/.well-known/apple-app-site-association` (and at the root
/// `/apple-app-site-association` for older OS versions), served over HTTPS with
/// `Content-Type: application/json` and no redirects.
///
/// Apple validates the file against the app's entitlements at install time.
/// Apps must have the **Associated Domains** capability enabled with an entry
/// of the form `applinks:your.domain`.
///
/// #### Example
///
/// ```java
/// String json = new AasaBuilder()
///     .appId("ABCD1234.com.example.app")
///     .addPath("/users/*")
///     .addPath("NOT /admin/*")     // exclude pattern
///     .addPath("/share/?id=*")
///     .build();
/// // Write `json` to https://example.com/.well-known/apple-app-site-association
/// ```
///
/// #### Reference
///
/// Apple's documentation: <https://developer.apple.com/documentation/xcode/supporting-associated-domains>
///
/// #### Since 8.0
public final class AasaBuilder {

    private final List<App> apps = new ArrayList<App>();
    private App pending;

    /// Begins a new app entry. Required: bundle prefix (10-character team ID) +
    /// bundle identifier, joined by a period. Repeat the call to add multiple
    /// apps that share the same domain.
    public AasaBuilder appId(String teamIdAndBundleId) {
        if (teamIdAndBundleId == null || teamIdAndBundleId.length() == 0) {
            throw new IllegalArgumentException("appId required");
        }
        pending = new App(teamIdAndBundleId);
        apps.add(pending);
        return this;
    }

    /// Adds a path pattern. Prefix with `NOT ` to exclude. Supports `*` wildcards
    /// (single segment) and `?` query-match notation per Apple's syntax.
    public AasaBuilder addPath(String pattern) {
        if (pending == null) {
            throw new IllegalStateException("call appId(...) before addPath(...)");
        }
        if (pattern == null || pattern.length() == 0) {
            return this;
        }
        pending.paths.add(pattern);
        return this;
    }

    /// Convenience: convert a `Router`-style pattern (`/users/:id`) to AASA's
    /// wildcard syntax (`/users/*`) and add it. Catch-all `**` becomes `*`.
    public AasaBuilder addRouterPattern(String routerPattern) {
        return addPath(toAasaPath(routerPattern));
    }

    /// Builds the JSON string. UTF-8 encoded, formatted for readability.
    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"applinks\": {\n");
        sb.append("    \"details\": [\n");
        for (int i = 0; i < apps.size(); i++) {
            App a = apps.get(i);
            sb.append("      {\n");
            sb.append("        \"appIDs\": [\"").append(jsonEscape(a.appId)).append("\"],\n");
            sb.append("        \"components\": [\n");
            for (int j = 0; j < a.paths.size(); j++) {
                String p = a.paths.get(j);
                sb.append("          ").append(toComponent(p));
                if (j < a.paths.size() - 1) {
                    sb.append(',');
                }
                sb.append('\n');
            }
            sb.append("        ]\n");
            sb.append("      }");
            if (i < apps.size() - 1) {
                sb.append(',');
            }
            sb.append('\n');
        }
        sb.append("    ]\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    static String toAasaPath(String routerPattern) {
        if (routerPattern == null) {
            return "/*";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        if (routerPattern.length() == 0 || routerPattern.charAt(0) != '/') {
            sb.append('/');
        }
        while (i < routerPattern.length()) {
            char c = routerPattern.charAt(i);
            if (c == ':') {
                // skip :name token
                sb.append('*');
                while (i < routerPattern.length() && routerPattern.charAt(i) != '/') {
                    i++;
                }
            } else if (c == '*') {
                sb.append('*');
                while (i < routerPattern.length() && routerPattern.charAt(i) == '*') {
                    i++;
                }
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    private static String toComponent(String pattern) {
        boolean exclude = false;
        String p = pattern;
        if (p.startsWith("NOT ")) {
            exclude = true;
            p = p.substring(4);
        }
        StringBuilder sb = new StringBuilder("{ \"/\": \"").append(jsonEscape(p)).append("\"");
        if (exclude) {
            sb.append(", \"exclude\": true");
        }
        sb.append(" }");
        return sb.toString();
    }

    private static String jsonEscape(String s) {
        StringBuilder sb = new StringBuilder(s.length() + 2);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class App {
        final String appId;
        final List<String> paths = new ArrayList<String>();
        App(String id) {
            this.appId = id;
        }
    }
}
