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

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

/**
 * Glue between the Codename One tooling and the {@link ToolingHelpReport}/
 * {@link ToolingHelpClient} core: capturing a local failure, persisting it so the
 * separate {@code cn1:get-help} goal can pick it up, resolving the signed-in email,
 * and reading the plugin's own version.
 *
 * <p>The failure hook deliberately <b>does not</b> read stdin or send anything. It
 * records the context and prints the "Get help" affordance; the user then opts in by
 * running {@code mvn cn1:get-help}, which is where the interactive Send lives. Keeping
 * the two apart avoids blocking a running (possibly unattended) build on a prompt and
 * guarantees nothing is ever sent without an explicit, separate action.</p>
 */
public final class ToolingHelp {

    /** Component identifier for the wire contract. */
    public static final String COMPONENT_MAVEN_PLUGIN = "maven-plugin";

    /** Preferences node holding the signed-in Codename One account (shared with the wizard). */
    private static final String PREFS_NODE = "/com/codename1/ui";

    private static final String LAST_FAILURE_RELATIVE =
            ".codenameone" + File.separator + "tooling-help" + File.separator + "last-failure.properties";

    private ToolingHelp() {
    }

    /**
     * Records a local failure and prints the "Get help" affordance. Best-effort and
     * completely silent-safe: any problem here must never mask the original failure,
     * so everything is guarded.
     */
    public static void offerAfterFailure(Log log, String component, String step,
                                         String toolingVersion, Throwable failure) {
        offerAfterFailure(log, component, step, null, toolingVersion, failure);
    }

    /**
     * As {@link #offerAfterFailure(Log, String, String, String, Throwable)} but with the
     * exact failing command/action (e.g. {@code mvn cn1:build -Dcodename1.platform=ios}),
     * which is folded into the reproduction so support can re-run it.
     */
    public static void offerAfterFailure(Log log, String component, String step, String action,
                                         String toolingVersion, Throwable failure) {
        try {
            ToolingHelpReport report = ToolingHelpReport.builder()
                    .component(component)
                    .toolingVersion(toolingVersion)
                    .step(step)
                    .errorSummary(summaryOf(failure))
                    .errorDetail(composeErrorDetail(step, action, component, toolingVersion, failure))
                    .build();
            persistFailure(report);
            if (log != null) {
                List<String> lines = ToolingHelpMessages.failureAffordance(step);
                for (int i = 0; i < lines.size(); i++) {
                    log.error(lines.get(i));
                }
            }
        } catch (Throwable ignore) {
            // Never let the help affordance itself break the build.
        }
    }

    /** First line of the throwable message, or the exception's simple name as a fallback. */
    static String summaryOf(Throwable failure) {
        if (failure == null) {
            return "";
        }
        String message = failure.getMessage();
        if (message != null && message.trim().length() > 0) {
            String firstLine = message.trim();
            int nl = firstLine.indexOf('\n');
            if (nl >= 0) {
                firstLine = firstLine.substring(0, nl).trim();
            }
            if (firstLine.length() > 0) {
                return firstLine;
            }
        }
        return failure.getClass().getSimpleName();
    }

    /** Full stack trace (truncation to the wire cap happens in {@link ToolingHelpReport}). */
    static String detailOf(Throwable failure) {
        if (failure == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            failure.printStackTrace(pw);
        } finally {
            pw.flush();
        }
        return sw.toString();
    }

    /**
     * Builds the {@code errorDetail} reproduction. This field IS what support reads in
     * the token-gated Full Report, so it is self-contained: a small environment/command
     * header (so they can reproduce without hunting through the log) followed by the full
     * stack trace / build log. {@link ToolingHelpReport} truncates it to the 16&nbsp;KB cap.
     */
    static String composeErrorDetail(String step, String action, String component,
                                     String toolingVersion, Throwable failure) {
        StringBuilder sb = new StringBuilder();
        sb.append("== Codename One tooling failure ==\n");
        appendLine(sb, "Step", step);
        appendLine(sb, "Command", action);
        appendLine(sb, "Component", component);
        appendLine(sb, "Tooling version", toolingVersion);
        appendLine(sb, "OS", trimJoin(System.getProperty("os.name"), System.getProperty("os.version")));
        appendLine(sb, "Java", trimJoin(System.getProperty("java.version"),
                "(java.home=" + System.getProperty("java.home") + ")"));
        appendLine(sb, "JAVA_HOME", firstNonEmpty(System.getenv("JAVA_HOME"), "(unset)"));
        appendLine(sb, "Proxy", proxyDescription());
        sb.append("\n");
        if (failure != null) {
            sb.append(detailOf(failure));
        }
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String label, String value) {
        if (value != null && value.trim().length() > 0) {
            sb.append(label).append(": ").append(value.trim()).append('\n');
        }
    }

    private static String trimJoin(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        if (left.length() == 0) {
            return right;
        }
        if (right.length() == 0) {
            return left;
        }
        return left + " " + right;
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && a.trim().length() > 0 ? a.trim() : b;
    }

    /** Describes any configured JVM proxy (a common cause of "can't reach the cloud"). */
    private static String proxyDescription() {
        String http = proxyEndpoint("http");
        String https = proxyEndpoint("https");
        if (http == null && https == null) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        if (http != null) {
            sb.append("http=").append(http);
        }
        if (https != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append("https=").append(https);
        }
        return sb.toString();
    }

    private static String proxyEndpoint(String scheme) {
        String host = System.getProperty(scheme + ".proxyHost");
        if (host == null || host.trim().length() == 0) {
            return null;
        }
        String port = System.getProperty(scheme + ".proxyPort");
        return port == null || port.trim().length() == 0 ? host.trim() : host.trim() + ":" + port.trim();
    }

    /**
     * Reads the signed-in Codename One account email from preferences (the same
     * {@code user} key the certificate wizard writes on login). Returns {@code null}
     * when unknown &mdash; callers must not fabricate one.
     */
    public static String resolveEmail() {
        try {
            Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
            String user = prefs.get("user", null);
            if (user != null && user.trim().length() > 0) {
                return user.trim();
            }
        } catch (Throwable ignore) {
            // Preferences can be unavailable in locked-down environments; that's fine.
        }
        return null;
    }

    /** Reads the running plugin's version from its bundled Maven descriptor. */
    public static String pluginVersion() {
        InputStream in = ToolingHelp.class.getResourceAsStream(
                "/META-INF/maven/com.codenameone/codenameone-maven-plugin/pom.xml");
        if (in != null) {
            try {
                Model model = new MavenXpp3Reader().read(in);
                String version = model.getVersion();
                if (version == null && model.getParent() != null) {
                    version = model.getParent().getVersion();
                }
                if (version != null && version.trim().length() > 0) {
                    return version.trim();
                }
            } catch (Exception ex) {
                // fall through to the unknown marker
            } finally {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
        return "unknown";
    }

    static File lastFailureFile() {
        // Overridable so tests don't touch the real home directory.
        String override = System.getProperty("cn1.help.lastFailureFile");
        if (override != null && override.trim().length() > 0) {
            return new File(override.trim());
        }
        return new File(System.getProperty("user.home"), LAST_FAILURE_RELATIVE);
    }

    /** Persists the captured context so {@code cn1:get-help} can replay it. */
    static void persistFailure(ToolingHelpReport report) throws IOException {
        File file = lastFailureFile();
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        Properties props = new Properties();
        putIfPresent(props, "component", report.getComponent());
        putIfPresent(props, "toolingVersion", report.getToolingVersion());
        putIfPresent(props, "step", report.getStep());
        putIfPresent(props, "os", report.getOs());
        putIfPresent(props, "osVersion", report.getOsVersion());
        putIfPresent(props, "javaVersion", report.getJavaVersion());
        putIfPresent(props, "errorSummary", report.getErrorSummary());
        putIfPresent(props, "errorDetail", report.getErrorDetail());
        FileOutputStream out = new FileOutputStream(file);
        try {
            props.store(out, "Codename One last local failure (used by cn1:get-help)");
        } finally {
            out.close();
        }
    }

    /**
     * Loads the last persisted failure into a report builder, or returns {@code null}
     * if none exists. Email/message are intentionally not stored; they're gathered
     * fresh at send time.
     */
    public static ToolingHelpReport.Builder loadLastFailure() {
        File file = lastFailureFile();
        if (!file.isFile()) {
            return null;
        }
        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            props.load(in);
        } catch (IOException ex) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }
        return ToolingHelpReport.builder()
                .component(props.getProperty("component"))
                .toolingVersion(props.getProperty("toolingVersion"))
                .step(props.getProperty("step"))
                .os(props.getProperty("os"))
                .osVersion(props.getProperty("osVersion"))
                .javaVersion(props.getProperty("javaVersion"))
                .errorSummary(props.getProperty("errorSummary"))
                .errorDetail(props.getProperty("errorDetail"));
    }

    private static void putIfPresent(Properties props, String key, String value) {
        if (value != null && value.length() > 0) {
            props.setProperty(key, value);
        }
    }

    /** For tests / re-run hygiene: removes the persisted failure. */
    public static void clearLastFailure() {
        File file = lastFailureFile();
        if (file.isFile()) {
            file.delete();
        }
    }
}
