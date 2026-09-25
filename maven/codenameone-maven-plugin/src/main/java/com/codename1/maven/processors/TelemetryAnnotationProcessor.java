/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.AbstractAnnotationProcessor;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationValues;
import com.codename1.maven.annotations.JavaSourceCompiler;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Build-time `@OpenTelemetry` processor for the app.
///
/// Emits a single `cn1app.TelemetryBootstrap` whose constructor installs
/// `com.codename1.telemetry.Telemetry` with the annotation's settings. The per-build
/// application stub instantiates it before `Display.init` (see
/// `Executor.annotationFrameworksInstallSource`), and the JavaSE port finds it by
/// name for the simulator. That constructor is the ONLY reference to the telemetry
/// package, which is how an app that does not enable it ends up without it: the
/// translator and R8 both keep only what is reachable.
///
/// Validated here rather than at run time, because the mistakes are in source the
/// build can read: no endpoint at all, both a relay and a direct endpoint, a header
/// that is not `Name: value`, a ratio outside 0..1.
public final class TelemetryAnnotationProcessor extends AbstractAnnotationProcessor {

    public static final String OPEN_TELEMETRY_DESC = "Lcom/codename1/annotations/OpenTelemetry;";

    static final String BOOTSTRAP_BINARY = "cn1app.TelemetryBootstrap";
    static final String BOOTSTRAP_SIMPLE = "TelemetryBootstrap";

    /// The one accepted annotation, and the class it was on.
    private AnnotationValues accepted;
    private String owner;

    @Override
    public Set<String> getAnnotationDescriptors() {
        return Collections.singleton(OPEN_TELEMETRY_DESC);
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        accepted = null;
        owner = null;
    }

    @Override
    public void processClass(AnnotatedClass cls, ProcessorContext ctx) throws ProcessingException {
        AnnotationValues otel = cls.getClassAnnotation(OPEN_TELEMETRY_DESC);
        if (otel == null || cls.isSynthetic()) {
            return;
        }
        // A deleted class whose .class is still in target/classes must not keep
        // telemetry on: the same orphan rule every generator here follows.
        if (!BuildHintAnnotationProcessor.hasBackingSource(cls, ctx.getCompileSourceRoots(),
                ctx.getSourceEncoding())) {
            return;
        }
        if (accepted != null) {
            ctx.error(cls, "@OpenTelemetry is on both " + owner + " and " + cls.getBinaryName()
                    + ". An app installs telemetry once; keep it on the main class.");
            return;
        }
        String relay = otel.getStringOrDefault("relay", "").trim();
        String endpoint = otel.getStringOrDefault("endpoint", "").trim();
        if (relay.length() == 0 && endpoint.length() == 0) {
            ctx.error(cls, "@OpenTelemetry on " + cls.getBinaryName() + " names neither a relay "
                    + "nor an endpoint, so there is nowhere to send spans. Set relay to the "
                    + "app's Codename One backend, or endpoint to an OTLP/HTTP collector.");
            return;
        }
        if (relay.length() > 0 && endpoint.length() > 0) {
            ctx.error(cls, "@OpenTelemetry on " + cls.getBinaryName() + " sets both relay and "
                    + "endpoint. Spans go one way: through the backend (relay) or straight "
                    + "to a collector (endpoint).");
            return;
        }
        if (!isHttpUrl(relay.length() > 0 ? relay : endpoint)) {
            ctx.error(cls, "@OpenTelemetry on " + cls.getBinaryName() + " must name an http or "
                    + "https URL");
            return;
        }
        int position = 0;
        for (String header : strings(otel.get("headers"))) {
            position++;
            int colon = header.indexOf(':');
            if (colon <= 0 || colon == header.length() - 1) {
                // By position, never quoted: a malformed entry is still usually a
                // credential ("Authorization Bearer ..."), and this message goes to
                // compiler and CI logs. The later checks name only the header.
                ctx.error(cls, "@OpenTelemetry header #" + position + " is not \"Name: value\" "
                        + "(its value is not shown, since it is usually a credential)");
                return;
            }
            // The whole field, not just the colon. The export is fail-silent by
            // design, so a header the platform refuses -- a name that is not an
            // HTTP token -- loses every span with nothing said, and a control
            // character in the value is a header injection on a lenient transport.
            // This is the one place the mistake is visible.
            String name = header.substring(0, colon).trim();
            String value = header.substring(colon + 1).trim();
            if (!isToken(name)) {
                ctx.error(cls, "@OpenTelemetry header name \"" + name + "\" is not a valid HTTP "
                        + "header name (letters, digits and !#$%&'*+-.^_`|~ only)");
                return;
            }
            // The ones TelemetryConfig refuses at run time, where the generated
            // bootstrap would throw before Display.init.
            if (name.equalsIgnoreCase("Content-Type") || name.equalsIgnoreCase("Content-Length")
                    || name.equalsIgnoreCase("Host") || name.equalsIgnoreCase("Transfer-Encoding")) {
                ctx.error(cls, "@OpenTelemetry header " + name + " is set by the exporter from "
                        + "what it sends and cannot be configured");
                return;
            }
            if (value.length() == 0 || hasControl(value)) {
                ctx.error(cls, "@OpenTelemetry header " + name + " has an empty value or one "
                        + "containing a control character such as a line break");
                return;
            }
        }
        if (relay.length() > 0 && !strings(otel.get("headers")).isEmpty()) {
            ctx.error(cls, "@OpenTelemetry on " + cls.getBinaryName() + " sets headers for a "
                    + "relay. The relay holds the collector's credentials; the app sends only "
                    + "relayToken. Move the headers to the backend's cn1.otel.headers.");
            return;
        }
        // TelemetryConfig.relayToken refuses a control character at run time, where
        // the generated bootstrap would throw before Display.init; refused here
        // instead, where the build can say so.
        if (hasControl(otel.getStringOrDefault("relayToken", ""))) {
            ctx.error(cls, "@OpenTelemetry relayToken contains a control character such as a "
                    + "line break; it is sent as a header and no header may carry one");
            return;
        }
        double ratio = ratio(otel.get("sampleRatio"));
        if (!(ratio >= 0 && ratio <= 1)) {
            ctx.error(cls, "@OpenTelemetry sampleRatio must be between 0 and 1, not " + ratio);
            return;
        }
        accepted = otel;
        owner = cls.getBinaryName();
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        if (ctx.hasErrors()) {
            return;
        }
        if (accepted == null) {
            // NOTHING ASKS FOR TELEMETRY NOW, so a bootstrap an earlier build left in
            // target/classes has to go. The builders install whatever bootstrap class
            // is present rather than asking whether the annotation still is, and
            // Maven keeps target/classes across a build without clean -- so removing
            // @OpenTelemetry, or deleting the class it was on, went on shipping the
            // old endpoint, token and consent setting until someone ran clean.
            File stale = new File(ctx.getOutputClassDir(),
                    BOOTSTRAP_BINARY.replace('.', File.separatorChar) + ".class");
            if (stale.isFile() && !stale.delete()) {
                ctx.getLog().warn("cn1: could not remove the stale " + stale + "; a build "
                        + "without clean may still install telemetry the project no longer asks for");
            }
            return;
        }
        Map<String, String> sources = new LinkedHashMap<String, String>();
        sources.put(BOOTSTRAP_BINARY, generateBootstrapSource(accepted));
        try {
            List<File> cp = new ArrayList<File>();
            cp.add(ctx.getOutputClassDir());
            for (String element : ctx.getCompileClasspath()) {
                cp.add(new File(element));
            }
            JavaSourceCompiler.compile(sources, ctx.getOutputClassDir(), cp);
        } catch (IOException ioe) {
            throw new ProcessingException("Could not compile the generated telemetry bootstrap: "
                    + ioe.getMessage(), ioe);
        }
        ctx.getLog().info("cn1: generated " + BOOTSTRAP_BINARY + " from @OpenTelemetry on " + owner);
    }

    /// The bootstrap's source. Package-visible so a test can read it.
    static String generateBootstrapSource(AnnotationValues otel) {
        String relay = otel.getStringOrDefault("relay", "").trim();
        String endpoint = otel.getStringOrDefault("endpoint", "").trim();
        StringBuilder sb = new StringBuilder(1024);
        sb.append("package cn1app;\n\n");
        sb.append("// Auto-generated by cn1:process-annotations from @OpenTelemetry. Do not edit.\n");
        sb.append("@SuppressWarnings({\"all\"})\n");
        sb.append("public final class ").append(BOOTSTRAP_SIMPLE).append(" {\n");
        sb.append("    public ").append(BOOTSTRAP_SIMPLE).append("() {\n");
        sb.append("        com.codename1.telemetry.Telemetry.install(new com.codename1.telemetry.TelemetryConfig()\n");
        if (relay.length() > 0) {
            sb.append("                .relay(").append(quote(relay)).append(")\n");
        } else {
            sb.append("                .direct(").append(quote(endpoint)).append(")\n");
        }
        String service = otel.getStringOrDefault("serviceName", "").trim();
        if (service.length() > 0) {
            sb.append("                .serviceName(").append(quote(service)).append(")\n");
        }
        for (String header : strings(otel.get("headers"))) {
            int colon = header.indexOf(':');
            sb.append("                .header(").append(quote(header.substring(0, colon).trim()))
                    .append(", ").append(quote(header.substring(colon + 1).trim())).append(")\n");
        }
        String token = otel.getStringOrDefault("relayToken", "");
        if (token.length() > 0) {
            sb.append("                .relayToken(").append(quote(token)).append(")\n");
        }
        double ratio = ratio(otel.get("sampleRatio"));
        if (ratio < 1) {
            sb.append("                .sampleRatio(").append(ratio).append(")\n");
        }
        if (!otel.getBoolOrDefault("protobuf", true)) {
            sb.append("                .protobuf(false)\n");
        }
        if (otel.getBoolOrDefault("requireAnalyticsConsent", false)) {
            sb.append("                .requireAnalyticsConsent(true)\n");
        }
        for (String host : strings(otel.get("propagateTo"))) {
            sb.append("                .propagateTo(").append(quote(host.trim())).append(")\n");
        }
        sb.append("        );\n");
        sb.append("    }\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static double ratio(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 1.0;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                if (item instanceof String) {
                    out.add((String) item);
                }
            }
        } else if (value instanceof String) {
            out.add((String) value);
        }
        return out;
    }

    /// RFC 9110 `token`: what a header name may be made of.
    static boolean isToken(String name) {
        if (name.length() == 0) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /// Any control character except horizontal tab, which a field value may carry.
    static boolean hasControl(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((c < 0x20 && c != '\t') || c == 0x7f) {
                return true;
            }
        }
        return false;
    }

    /// An http or https URL WITH A HOST. The scheme alone is not a URL: "https://"
    /// normalized to "https:/v1/traces", and every export -- fail-silent by design --
    /// went nowhere while the build reported the setting as checked.
    static boolean isHttpUrl(String url) {
        // No fragment. HTTP never sends one, so a credential kept there
        // ("#api-key=...") never reached the collector: telemetry installed and
        // every export was refused, silently.
        if (url.indexOf('#') >= 0) {
            return false;
        }
        // The WHOLE URL first: no space, control or DEL anywhere. Only the
        // authority was checked, so a space in the path passed and every export
        // then failed at transport, silently.
        for (int i = 0; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c <= 0x20 || c == 0x7f) {
                return false;
            }
        }
        int start;
        if (url.regionMatches(true, 0, "http://", 0, 7)) {
            start = 7;
        } else if (url.regionMatches(true, 0, "https://", 0, 8)) {
            start = 8;
        } else {
            return false;
        }
        int end = url.length();
        for (int i = start; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                end = i;
                break;
            }
        }
        String authority = url.substring(start, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0 && !validUserinfo(authority.substring(0, at))) {
            return false;
        }
        String hostPort = at >= 0 ? authority.substring(at + 1) : authority;
        String host = hostPort;
        if (hostPort.startsWith("[")) {
            int close = hostPort.indexOf(']');
            if (close <= 1) {
                return false;
            }
            host = hostPort.substring(1, close);
            String rest = hostPort.substring(close + 1);
            if (rest.length() > 0 && !validPort(rest)) {
                return false;
            }
        } else {
            int colon = hostPort.lastIndexOf(':');
            if (colon >= 0) {
                if (!validPort(hostPort.substring(colon))) {
                    return false;
                }
                host = hostPort.substring(0, colon);
            }
        }
        if (host.length() == 0) {
            return false;
        }
        // The same host rule TelemetryConfig applies at run time, and it has to be
        // the same: a URL this accepted and that refused would pass the build and
        // then throw from the generated bootstrap, before Display.init, on every
        // platform that does not guard it -- a crash at start-up for a mistake the
        // build is there to catch. A DNS name or IPv4 address, or a bracketed IPv6
        // literal.
        if (hostPort.startsWith("[")) {
            return isIpv6(host);
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "-._~".indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }

    /// An IPv6 address by its STRUCTURE (RFC 4291 2.2), not its characters: groups
    /// of one to four hex digits, at most one "::", eight groups without it and at
    /// most seven with it, and optionally a dotted IPv4 tail counting as two. A
    /// character check let "[:::]" through, and the transport refused every export.
    static boolean isIpv6(String s) {
        int n = s.length();
        if (n == 0) {
            return false;
        }
        int groups = 0;
        boolean compressed = false;
        int i = 0;
        if (s.startsWith("::")) {
            compressed = true;
            i = 2;
            if (i == n) {
                return true;
            }
        } else if (s.charAt(0) == ':') {
            return false;
        }
        while (i < n) {
            int j = i;
            while (j < n && s.charAt(j) != ':') {
                j++;
            }
            String part = s.substring(i, j);
            if (part.indexOf('.') >= 0) {
                if (j != n || !isIpv4(part)) {
                    return false;
                }
                groups += 2;
            } else {
                if (part.length() < 1 || part.length() > 4) {
                    return false;
                }
                for (int k = 0; k < part.length(); k++) {
                    char c = part.charAt(k);
                    if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                        return false;
                    }
                }
                groups++;
            }
            if (j == n) {
                break;
            }
            if (j + 1 < n && s.charAt(j + 1) == ':') {
                if (compressed) {
                    return false;
                }
                compressed = true;
                i = j + 2;
            } else {
                i = j + 1;
                if (i == n) {
                    return false;
                }
            }
        }
        return compressed ? groups <= 7 : groups == 8;
    }

    /// Four decimal parts, each 0 to 255.
    static boolean isIpv4(String s) {
        int parts = 0;
        int i = 0;
        while (i <= s.length()) {
            int j = s.indexOf('.', i);
            if (j < 0) {
                j = s.length();
            }
            String part = s.substring(i, j);
            if (part.length() < 1 || part.length() > 3) {
                return false;
            }
            int value = 0;
            for (int k = 0; k < part.length(); k++) {
                char c = part.charAt(k);
                if (c < '0' || c > '9') {
                    return false;
                }
                value = value * 10 + (c - '0');
            }
            if (value > 255) {
                return false;
            }
            parts++;
            i = j + 1;
        }
        return parts == 4;
    }

    /// RFC 3986 userinfo: unreserved characters, sub-delims, ':' and complete
    /// percent escapes. Skipped over, a space, a control or a stray '%' in it passed
    /// validation and failed only at transport, where the export fails silently.
    static boolean validUserinfo(String userinfo) {
        for (int i = 0; i < userinfo.length(); i++) {
            char c = userinfo.charAt(i);
            if (c == '%') {
                if (i + 2 >= userinfo.length() || !isHex(userinfo.charAt(i + 1))
                        || !isHex(userinfo.charAt(i + 2))) {
                    return false;
                }
                i += 2;
                continue;
            }
            if (!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')
                    || "-._~!$&'()*+,;=:".indexOf(c) >= 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    /// ":" then a TCP port, 1 through 65535. Five digits alone let 99999 through,
    /// and the generated exporter, which fails silently, then lost every span.
    private static boolean validPort(String colonPort) {
        if (!colonPort.startsWith(":") || colonPort.length() < 2 || colonPort.length() > 6) {
            return false;
        }
        int port = 0;
        for (int i = 1; i < colonPort.length(); i++) {
            char c = colonPort.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
            port = port * 10 + (c - '0');
        }
        return port >= 1 && port <= 65535;
    }

    private static String quote(String value) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
