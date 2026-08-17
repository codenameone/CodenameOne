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
package com.codename1.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Generates the Swift App Intents declarations for an application, from the `intents.json` the
/// annotation processor emitted.
///
/// #### Why any Swift at all
///
/// Most of this feature is Objective-C: Core Spotlight carries indexing, `NSUserActivity`
/// carries donation and continuation, and both predate the port's deployment floor. App Intents
/// is the exception and it is Swift-only by construction -- `AppIntent` is a protocol with
/// associated types, `perform()` returns an opaque type, `@Parameter` is a property wrapper and
/// `AppShortcutsProvider` uses a result builder. None of those have an Objective-C
/// representation, and the metadata the system reads is produced by a build phase that analyses
/// Swift. The Objective-C-compatible predecessor, SiriKit, was deprecated in favour of this.
///
/// So the generated Swift is kept to a declarative shell: it names the intent, describes its
/// parameters, and immediately hands off to `CN1IntentBridge`. Every line of behaviour stays in
/// Objective-C and Java.
///
/// #### Availability
///
/// Everything emitted is wrapped `#if canImport(AppIntents)` plus `@available(iOS 16.0, *)`, the
/// same shape the surfaces builder uses for WidgetKit. That is what allows an application to
/// keep a lower deployment target and simply not offer intents on older devices, rather than
/// forcing every app that declares one to abandon them.
public final class IOSAppIntentsBuilder {

    private final List<Map<String, Object>> intents;
    private final List<Map<String, Object>> entities;

    public IOSAppIntentsBuilder(List<Map<String, Object>> intents,
                                 List<Map<String, Object>> entities) {
        this.intents = intents == null ? new ArrayList<Map<String, Object>>() : intents;
        this.entities = entities == null ? new ArrayList<Map<String, Object>>() : entities;
    }

    /// The files to write into `<Main>-src`, keyed by file name. The build's existing scheme
    /// script sweeps every `.swift` there into the app target, so nothing else is needed to
    /// compile them.
    public Map<String, String> buildAppTargetFileMap() {
        Map<String, String> out = new LinkedHashMap<String, String>();
        out.put("CN1AppIntents.swift", buildIntents());
        if (!entities.isEmpty()) {
            out.put("CN1AppEntities.swift", buildEntities());
        }
        return out;
    }

    /// True when there is anything worth generating. An app that only indexes content declares
    /// no intents, and must not be given an empty `AppShortcutsProvider` -- an empty shortcuts
    /// provider is a build error on Apple's side.
    public boolean hasIntents() {
        return !intents.isEmpty();
    }

    private String buildIntents() {
        StringBuilder sb = new StringBuilder();
        header(sb);
        sb.append("#if canImport(AppIntents)\n");
        sb.append("import AppIntents\n");
        sb.append("import Foundation\n\n");

        for (Map<String, Object> intent : intents) {
            // An intent that only offered itself to a language model must not become an App
            // Intent; exposure is a restriction, not a hint.
            if (isExposedToAssistant(intent)) {
                appendIntent(sb, intent);
            }
        }
        appendShortcutsProvider(sb);
        sb.append("#endif\n");
        return sb.toString();
    }

    private void appendIntent(StringBuilder sb, Map<String, Object> intent) {
        String id = str(intent, "id");
        String struct = structName(id);
        boolean opensApp = str(intent, "opensRoute").length() > 0;
        boolean headless = bool(intent, "headless");

        sb.append("@available(iOS 16.0, *)\n");
        sb.append("struct ").append(struct).append(": AppIntent {\n");
        sb.append("    static var title: LocalizedStringResource = \"")
                .append(swift(str(intent, "title"))).append("\"\n");
        String description = str(intent, "description");
        if (description.length() > 0) {
            sb.append("    static var description = IntentDescription(\"")
                    .append(swift(description)).append("\")\n");
        }
        // openAppWhenRun is the switch between "answer in place" and "continue in the app".
        // A headless intent that also names a route still opens: the route is the point.
        sb.append("    static var openAppWhenRun: Bool = ").append(opensApp).append("\n");
        // discoverable=false means the capability is donation-only. Without this override App
        // Intents keeps its discoverable default and lists it in the Shortcuts catalog before it
        // has ever been donated, which is the opposite of what the declaration asked for.
        if (!bool(intent, "discoverable")) {
            sb.append("    static var isDiscoverable: Bool = false\n");
        }
        sb.append("\n");

        List<Map<String, Object>> params = params(intent);
        for (Map<String, Object> p : params) {
            String type = str(p, "type");
            // An optional declaration has to produce an optional Swift type, or the system keeps
            // prompting for a value the handler was willing to do without.
            String optional = bool(p, "required") ? "" : "?";
            sb.append("    @Parameter(title: \"").append(swift(str(p, "title"))).append("\")\n");
            sb.append("    var ").append(varName(str(p, "name"))).append(": ")
                    .append(swiftType(type, str(p, "entityType"))).append(optional).append("\n\n");
        }

        // ReturnsValue is declared unconditionally so the signature does not vary with the
        // declaration: the wire value is a string, absent becomes empty, and Shortcuts can pipe
        // it into a following action either way.
        sb.append("    func perform() async throws -> some IntentResult & ProvidesDialog"
                + " & ReturnsValue<String> {\n");
        if (bool(intent, "destructive")) {
            // Declared destructive, so the platform confirms before anything happens.
            sb.append("        try await requestConfirmation()\n");
        }
        sb.append("        var params: [String: Any] = [:]\n");
        for (Map<String, Object> p : params) {
            String name = str(p, "name");
            String type = str(p, "type");
            String var = varName(name);
            boolean required = bool(p, "required");
            // An absent optional parameter must not be sent at all, so the Java side applies the
            // declared default rather than receiving a null it would have to guess about.
            String read = required ? var : "v";
            if (!required) {
                sb.append("        if let v = ").append(var).append(" {\n    ");
            }
            if ("entity".equals(type)) {
                // Entities cross as their id and nothing else; the Java side resolves it
                // through the type's own BY_ID query.
                sb.append("        params[\"").append(swift(name)).append("\"] = ")
                        .append(read).append(".id\n");
            } else if ("date".equals(type)) {
                sb.append("        params[\"").append(swift(name)).append("\"] = ")
                        .append("Int(").append(read).append(".timeIntervalSince1970 * 1000)\n");
            } else {
                sb.append("        params[\"").append(swift(name)).append("\"] = ")
                        .append(read).append("\n");
            }
            if (!required) {
                sb.append("        }\n");
            }
        }
        sb.append("        let outcome = await CN1IntentBridge.run(id: \"").append(swift(id))
                .append("\", params: params, headless: ").append(headless).append(")\n");
        // A failed handler has to fail the intent. Returning a dialog-only success made
        // Shortcuts record success and run the following actions anyway.
        sb.append("        if !outcome.ok {\n");
        sb.append("            throw CN1IntentFailure(message: outcome.spoken)\n");
        sb.append("        }\n");
        // A result carrying a route is navigated by the framework once the app is up; this only
        // has to make sure the app is up, which openAppWhenRun already did.
        sb.append("        return .result(value: outcome.value ?? \"\",\n");
        sb.append("                       dialog: IntentDialog(stringLiteral: outcome.spoken))\n");
        sb.append("    }\n");
        sb.append("}\n\n");
    }

    /// Emits the zero-setup phrases. Only intents that actually declared a phrase appear: an
    /// App Shortcut with no phrase has no way to be spoken, and Apple rejects a phrase that
    /// omits the app name, which the annotation processor already enforces at build time.
    private void appendShortcutsProvider(StringBuilder sb) {
        List<Map<String, Object>> withPhrases = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> intent : intents) {
            if (!phrases(intent).isEmpty() && bool(intent, "discoverable")
                    && isExposedToAssistant(intent)) {
                withPhrases.add(intent);
            }
        }
        if (withPhrases.isEmpty()) {
            return;
        }
        sb.append("@available(iOS 16.0, *)\n");
        sb.append("struct CN1AppShortcuts: AppShortcutsProvider {\n");
        sb.append("    static var appShortcuts: [AppShortcut] {\n");
        for (Map<String, Object> intent : withPhrases) {
            sb.append("        AppShortcut(\n");
            sb.append("            intent: ").append(structName(str(intent, "id"))).append("(),\n");
            sb.append("            phrases: [\n");
            List<String> ph = phrases(intent);
            for (int i = 0; i < ph.size(); i++) {
                // ${applicationName} is Apple's own token; it becomes \(.applicationName) in a
                // Swift phrase literal.
                String phrase = swift(ph.get(i)).replace("${applicationName}",
                        "\\(.applicationName)");
                sb.append("                \"").append(phrase).append("\"");
                sb.append(i == ph.size() - 1 ? "\n" : ",\n");
            }
            sb.append("            ],\n");
            sb.append("            shortTitle: \"").append(swift(str(intent, "title")))
                    .append("\",\n");
            sb.append("            systemImageName: \"sparkles\"\n");
            sb.append("        )\n");
        }
        sb.append("    }\n");
        sb.append("}\n\n");
    }

    private String buildEntities() {
        StringBuilder sb = new StringBuilder();
        header(sb);
        sb.append("#if canImport(AppIntents)\n");
        sb.append("import AppIntents\n");
        sb.append("import Foundation\n\n");

        for (Map<String, Object> entity : entities) {
            String type = str(entity, "type");
            String struct = entityStructName(type);
            List<String> queries = queries(entity);

            sb.append("@available(iOS 16.0, *)\n");
            sb.append("struct ").append(struct).append(": AppEntity {\n");
            sb.append("    let id: String\n");
            sb.append("    let name: String\n");
            sb.append("    let detail: String?\n\n");
            sb.append("    static var typeDisplayRepresentation: TypeDisplayRepresentation =\n");
            sb.append("        TypeDisplayRepresentation(name: \"")
                    .append(swift(str(entity, "title"))).append("\")\n\n");
            sb.append("    var displayRepresentation: DisplayRepresentation {\n");
            sb.append("        DisplayRepresentation(title: \"\\(name)\", subtitle: "
                    + "detail.map { \"\\($0)\" })\n");
            sb.append("    }\n\n");
            sb.append("    static var defaultQuery = ").append(struct).append("Query()\n");
            sb.append("}\n\n");

            sb.append("@available(iOS 16.0, *)\n");
            sb.append("struct ").append(struct).append("Query: ");
            // Only advertise string search when the entity actually declared a SEARCH query;
            // conforming without one would offer the user a search box that always came back
            // empty.
            boolean search = queries.contains("SEARCH");
            sb.append(search ? "EntityStringQuery" : "EntityQuery").append(" {\n");
            sb.append("    func entities(for identifiers: [String]) async throws -> [")
                    .append(struct).append("] {\n");
            sb.append("        identifiers.flatMap { id in\n");
            sb.append("            CN1IntentBridge.entities(type: \"").append(swift(type))
                    .append("\", kind: \"byId\", argument: id)\n");
            sb.append("        }.map { ").append(struct)
                    .append("(id: $0.id, name: $0.title, detail: $0.subtitle) }\n");
            sb.append("    }\n\n");
            if (queries.contains("SUGGESTED")) {
                sb.append("    func suggestedEntities() async throws -> [").append(struct)
                        .append("] {\n");
                sb.append("        CN1IntentBridge.entities(type: \"").append(swift(type))
                        .append("\", kind: \"suggested\", argument: nil)\n");
                sb.append("            .map { ").append(struct)
                        .append("(id: $0.id, name: $0.title, detail: $0.subtitle) }\n");
                sb.append("    }\n\n");
            }
            if (search) {
                sb.append("    func entities(matching string: String) async throws -> [")
                        .append(struct).append("] {\n");
                sb.append("        CN1IntentBridge.entities(type: \"").append(swift(type))
                        .append("\", kind: \"search\", argument: string)\n");
                sb.append("            .map { ").append(struct)
                        .append("(id: $0.id, name: $0.title, detail: $0.subtitle) }\n");
                sb.append("    }\n");
            }
            sb.append("}\n\n");
        }
        sb.append("#endif\n");
        return sb.toString();
    }

    private static void header(StringBuilder sb) {
        sb.append("//\n");
        sb.append("// Generated by Codename One from @AppIntent / @IntentEntity. Do not edit.\n");
        sb.append("//\n\n");
    }

    // ------------------------------------------------------------------
    // Naming and escaping
    // ------------------------------------------------------------------

    static String structName(String intentId) {
        return "CN1Intent_" + sanitize(intentId);
    }

    static String entityStructName(String entityType) {
        return "CN1Entity_" + sanitize(entityType);
    }

    private static String varName(String name) {
        String s = sanitize(name);
        // A Swift keyword as a property name compiles only when back-quoted, and parameter
        // names come from application code, so this is reachable rather than theoretical.
        if (SWIFT_KEYWORDS.contains(s)) {
            return "`" + s + "`";
        }
        return s;
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(Character.isLetterOrDigit(c) || c == '_' ? c : '_');
        }
        return sb.toString();
    }

    private static String swiftType(String type, String entityType) {
        if ("entity".equals(type)) {
            return entityStructName(entityType);
        }
        if ("int".equals(type) || "long".equals(type)) {
            return "Int";
        }
        if ("float".equals(type) || "double".equals(type)) {
            return "Double";
        }
        if ("boolean".equals(type)) {
            return "Bool";
        }
        if ("date".equals(type)) {
            return "Date";
        }
        return "String";
    }

    private static String swift(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final List<String> SWIFT_KEYWORDS = new ArrayList<String>();
    static {
        String[] kw = {"class", "func", "var", "let", "if", "else", "for", "while", "return",
                "struct", "enum", "protocol", "import", "in", "is", "as", "self", "super",
                "true", "false", "nil", "switch", "case", "default", "where", "guard", "defer",
                "repeat", "do", "try", "catch", "throw", "throws", "init", "deinit", "extension",
                "operator", "static", "public", "private", "internal", "open", "final", "lazy",
                "type", "id", "description", "title"};
        for (String k : kw) {
            SWIFT_KEYWORDS.add(k);
        }
    }

    // ------------------------------------------------------------------
    // Manifest access
    // ------------------------------------------------------------------

    /// True when the intent offered itself to the platform. An absent list means the default,
    /// which is platform exposure.
    @SuppressWarnings("unchecked")
    static boolean isExposedToAssistant(Map<String, Object> intent) {
        Object exposure = intent.get("exposure");
        if (!(exposure instanceof List)) {
            return true;
        }
        List<Object> list = (List<Object>) exposure;
        if (list.isEmpty()) {
            return true;
        }
        for (Object o : list) {
            if ("ASSISTANT".equals(o)) {
                return true;
            }
        }
        return false;
    }

    private static String str(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o instanceof String ? (String) o : "";
    }

    private static boolean bool(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (o instanceof Boolean) {
            return ((Boolean) o).booleanValue();
        }
        return "true".equals(String.valueOf(o));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> params(Map<String, Object> intent) {
        Object o = intent.get("params");
        if (o instanceof List) {
            return (List<Map<String, Object>>) o;
        }
        return new ArrayList<Map<String, Object>>();
    }

    @SuppressWarnings("unchecked")
    private static List<String> phrases(Map<String, Object> intent) {
        Object o = intent.get("phrases");
        if (o instanceof List) {
            List<String> out = new ArrayList<String>();
            for (Object s : (List<Object>) o) {
                if (s instanceof String) {
                    out.add((String) s);
                }
            }
            return out;
        }
        return new ArrayList<String>();
    }

    @SuppressWarnings("unchecked")
    private static List<String> queries(Map<String, Object> entity) {
        Object o = entity.get("queries");
        if (o instanceof List) {
            List<String> out = new ArrayList<String>();
            for (Object s : (List<Object>) o) {
                if (s instanceof String) {
                    out.add((String) s);
                }
            }
            return out;
        }
        return new ArrayList<String>();
    }
}
