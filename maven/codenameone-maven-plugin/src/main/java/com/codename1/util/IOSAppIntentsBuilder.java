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
import java.util.Collections;
import java.util.Comparator;
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

    /// Orders declarations by intent id.
    ///
    /// Named and static rather than anonymous: an anonymous class declared inside an instance
    /// method holds a reference to the enclosing builder, which it has no use for and which
    /// SpotBugs reports.
    private static final class ById
            implements Comparator<Map<String, Object>>, java.io.Serializable {
        /// Serializable because SpotBugs asks every Comparator to be: one can end up inside a
        /// TreeMap that someone serializes. Nothing here does, and nothing here has state.
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Map<String, Object> a, Map<String, Object> b) {
            return str(a, "id").compareTo(str(b, "id"));
        }
    }

    private static final Comparator<Map<String, Object>> BY_ID = new ById();

    /// Apple's limit on one AppShortcutsProvider.
    private static final int MAX_APP_SHORTCUTS = 10;

    /// Intents whose phrases did not fit that limit, for the build to report.
    private final List<String> omittedShortcuts = new ArrayList<String>();

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

    /// The ids whose phrases were left out because the provider was full, in the order they
    /// were dropped. Empty until buildAppTargetFileMap has run.
    public List<String> getOmittedShortcutIds() {
        return omittedShortcuts;
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
        sb.append("import Foundation\n");
        // SwiftUI is not decoration here: the result overload that carries a snippet view lives
        // in the _AppIntents_SwiftUI cross-import overlay, which only activates when both
        // modules are imported. Without it .result(value:dialog:view:) does not exist.
        sb.append("import SwiftUI\n\n");

        for (Map<String, Object> intent : intents) {
            // An intent that only offered itself to a language model must not become an App
            // Intent; exposure is a restriction, not a hint.
            if (isExposedToAssistant(intent)) {
                appendChoiceEnums(sb, intent);
                appendIntent(sb, intent);
            }
        }
        appendShortcutsProvider(sb);
        sb.append("#endif\n");
        return sb.toString();
    }

    /// True when this parameter declared a closed vocabulary the platform should offer as a
    /// choice list rather than as free text.
    private static boolean hasChoices(Map<String, Object> p) {
        return "string".equals(str(p, "type")) && !options(p).isEmpty();
    }

    /// The Swift enum name backing one parameter's declared `options`.
    static String choiceEnumName(String intentId, String paramName) {
        return "CN1Choice_" + sanitize(intentId) + "_" + sanitize(paramName);
    }

    /// Emits an `AppEnum` per closed-vocabulary parameter.
    ///
    /// Without this the parameter is a plain `String`, so Shortcuts and Siri accept any text
    /// and the declared vocabulary is enforced only by the Java `oneOf` check -- after the user
    /// has finished the interaction, which is the worst possible moment to reject it. An
    /// AppEnum makes the platform offer exactly the declared values and never produce another.
    private void appendChoiceEnums(StringBuilder sb, Map<String, Object> intent) {
        String id = str(intent, "id");
        for (Map<String, Object> p : params(intent)) {
            if (!hasChoices(p)) {
                continue;
            }
            String name = choiceEnumName(id, paramIdentifier(intent, str(p, "name")));
            List<String> opts = options(p);
            List<String> cases = caseNames(opts);
            sb.append("@available(iOS 16.0, *)\n");
            sb.append("enum ").append(name).append(": String, AppEnum {\n");
            for (int i = 0; i < opts.size(); i++) {
                sb.append("    case ").append(cases.get(i)).append(" = \"")
                        .append(swift(opts.get(i))).append("\"\n");
            }
            sb.append("\n    static var typeDisplayRepresentation: TypeDisplayRepresentation =\n");
            sb.append("        TypeDisplayRepresentation(name: \"")
                    .append(swift(str(p, "title"))).append("\")\n");
            sb.append("    static var caseDisplayRepresentations: [").append(name)
                    .append(": DisplayRepresentation] = [\n");
            for (int i = 0; i < opts.size(); i++) {
                sb.append("        .").append(cases.get(i)).append(": \"")
                        .append(swift(opts.get(i))).append("\",\n");
            }
            sb.append("    ]\n");
            sb.append("}\n\n");
        }
    }

    /// Legal, unique Swift case names for a parameter's options, in the order given.
    ///
    /// Sanitizing each option independently was not enough to keep the promise below. Options
    /// are application strings, so two legal and quite different ones -- "in-person" and
    /// "in person" -- reduce to the same identifier, and the generated enum then declared
    /// `case in_person` twice. That does not degrade at runtime: the iOS target fails to
    /// compile, for a Swift file the developer did not write and cannot easily read.
    ///
    /// Disambiguated by position rather than by content so the result is stable: the same
    /// declaration always produces the same enum, which matters because these names appear in
    /// metadata the system keeps.
    /// Members the generated AppEnum already declares, which a case may not also be called.
    ///
    /// An enum case shares a namespace with the type's static members, so an option that
    /// sanitizes to one of these is an invalid redeclaration and the iOS build fails on an
    /// otherwise legal Java declaration. Backticks do not help: the collision is the name, not
    /// its spelling. `rawValue` and `allCases` are here for the same reason -- the raw-value
    /// conformance synthesizes one and AppEnum's CaseIterable the other.
    private static final String[] RESERVED_ENUM_MEMBERS = {
        "typeDisplayRepresentation", "caseDisplayRepresentations", "rawValue", "allCases",
    };

    /// Identifiers the generated AppIntent struct already spells, which a parameter may not
    /// reuse.
    ///
    /// `perform` is the protocol method every generated struct declares, and Swift does not let
    /// a property and a method of the same type share a name -- so a parameter innocently named
    /// "perform" was an invalid redeclaration. `params` is the dictionary perform() builds; a
    /// property of that name is shadowed by the local, which compiles, silently serializes the
    /// accumulator into itself, and sends the handler an empty value instead of the user's.
    ///
    /// Only instance-level names belong here. A parameter named "title" or "description" is
    /// fine: those are static members, and Swift allows a static and an instance member to
    /// share a name.
    private static final String[] RESERVED_INTENT_MEMBERS = {"perform", "params"};

    private static List<String> caseNames(List<String> options) {
        List<String> out = new ArrayList<String>();
        List<String> taken = new ArrayList<String>();
        for (int i = 0; i < RESERVED_ENUM_MEMBERS.length; i++) {
            taken.add(RESERVED_ENUM_MEMBERS[i]);
        }
        for (String option : options) {
            String base = legalIdentifier(sanitize(option));
            String candidate = base;
            for (int n = 2; taken.contains(candidate); n++) {
                candidate = base + "_" + n;
            }
            taken.add(candidate);
            out.add(escaped(candidate));
        }
        return out;
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
        // openAppWhenRun is the switch between "answer in place" and "continue in the app", and
        // headless is what decides it. A handler that did not declare headless is allowed to
        // touch a Form -- that is the entire meaning of the flag -- so running it with no
        // foreground window would hand it a Display with nothing on screen and fail inside
        // Siri, where there is nowhere to report it. A headless intent that names a route
        // opens anyway: the route is the point.
        sb.append("    static var openAppWhenRun: Bool = ")
                .append(opensApp || !headless).append("\n");
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
            String declaredType = hasChoices(p)
                    ? choiceEnumName(str(intent, "id"), paramIdentifier(intent, str(p, "name")))
                    : swiftType(type, str(p, "entityType"));
            // No `default:` is projected here, and a required parameter never needs one:
            // AppIntentAnnotationProcessor rejects required + defaultValue outright, naming
            // this very divergence as the reason -- the generated coercion treats a defaulted
            // parameter as optional, so Android would publish a parameterless shortcut and run
            // the default while this Swift kept the parameter non-optional and prompted for it.
            // The developer is told to set required = false or drop the default, so the
            // combination cannot reach a generated manifest.
            //
            // An optional parameter's default is deliberately not projected either: it is
            // emitted as an Optional and simply left out of the payload when nil, so the one
            // authority on what an omitted value becomes is the declaration the Java side
            // reads. Restating it in Swift would put the same constant in two places, and the
            // two would disagree the first time a default changed.
            sb.append("    @Parameter(title: \"").append(swift(str(p, "title")))
                    .append("\"").append(inclusiveRange(type, hasChoices(p))).append(")\n");
            sb.append("    var ").append(varName(paramIdentifier(intent, str(p, "name"))))
                    .append(": ")
                    .append(declaredType).append(optional).append("\n\n");
        }

        // ReturnsValue is declared unconditionally so the signature does not vary with the
        // declaration: the wire value is a string, absent becomes empty, and Shortcuts can pipe
        // it into a following action either way.
        // ShowsSnippetView is declared unconditionally so the signature does not vary with the
        // declaration: a result with no snippet renders an empty view, which costs nothing and
        // keeps every generated struct the same shape.
        sb.append("    func perform() async throws -> some IntentResult & ProvidesDialog"
                + " & ReturnsValue<String> & ShowsSnippetView {\n");
        if (bool(intent, "destructive")) {
            // Declared destructive, so the platform confirms before anything happens.
            sb.append("        try await requestConfirmation()\n");
        }
        sb.append("        var params: [String: Any] = [:]\n");
        for (Map<String, Object> p : params) {
            String name = str(p, "name");
            String type = str(p, "type");
            String var = varName(paramIdentifier(intent, name));
            boolean required = bool(p, "required");
            // An absent optional parameter must not be sent at all, so the Java side applies the
            // declared default rather than receiving a null it would have to guess about.
            String read = required ? var : "v";
            if (!required) {
                sb.append("        if let v = ").append(var).append(" {\n    ");
            }
            if (hasChoices(p)) {
                // The enum exists so the platform offers the declared vocabulary; Java still
                // receives the plain string it declared.
                sb.append("        params[\"").append(swift(name)).append("\"] = ")
                        .append(read).append(".rawValue\n");
            } else if ("entity".equals(type)) {
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
        // The flag reported to Java has to describe what actually happens, not what was
        // declared. openAppWhenRun above is (route || !headless), so an intent that declares
        // headless *and* a route is opened by iOS before perform() runs -- and telling the
        // handler it is headless would send it down the no-UI path in a foregrounded app.
        // Android already marks routed invocations non-headless; this is the same rule.
        sb.append("        let outcome = await CN1IntentBridge.run(id: \"").append(swift(id))
                .append("\", params: params, headless: ").append(headless && !opensApp)
                .append(")\n");
        // A failed handler has to fail the intent. Returning a dialog-only success made
        // Shortcuts record success and run the following actions anyway.
        sb.append("        if !outcome.ok {\n");
        sb.append("            throw CN1IntentFailure(message: outcome.spoken)\n");
        sb.append("        }\n");
        // A result carrying a route is navigated by the framework once the app is up; this only
        // has to make sure the app is up, which openAppWhenRun already did.
        sb.append("        return .result(value: outcome.resultValue,\n");
        sb.append("                       dialog: IntentDialog(stringLiteral: outcome.spoken),\n");
        sb.append("                       view: CN1IntentSnippetView(node: outcome.snippet,\n");
        sb.append("                                                  imagesDir: outcome.imagesDir))\n");
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
        // Apple allows a provider ten app shortcuts. Emitting more does not get more: the
        // excess is rejected, and which ten survived was decided by whatever order the
        // annotation scanner walked the disk in -- so a rebuild that changed nothing could
        // change which phrases work. Sorted by intent id and cut at ten, with the rest named
        // rather than dropped quietly.
        Collections.sort(withPhrases, BY_ID);
        while (withPhrases.size() > MAX_APP_SHORTCUTS) {
            omittedShortcuts.add(str(withPhrases.remove(withPhrases.size() - 1), "id"));
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
                sb.append("                \"").append(phraseLiteral(ph.get(i), intent))
                        .append("\"");
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

    /// Turns a declared phrase into a Swift phrase literal.
    ///
    /// Two different interpolations live in the same string. `${applicationName}` is Apple's own
    /// token and becomes `\(.applicationName)`. A `${name}` naming one of the intent's own
    /// parameters becomes `\(\.$name)` -- a key path, because that is what
    /// `AppShortcutPhraseToken` accepts for a parameter: the enum itself only carries
    /// `applicationName`, and the interpolation that takes a parameter is declared over
    /// `KeyPath<Intent, IntentParameter<Value>>`. Left as literal text the phrase reads the
    /// placeholder back to the user and never supplies the argument.
    private String phraseLiteral(String phrase, Map<String, Object> intent) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < phrase.length()) {
            int open = phrase.indexOf("${", i);
            if (open < 0) {
                out.append(swift(phrase.substring(i)));
                break;
            }
            int close = phrase.indexOf('}', open);
            if (close < 0) {
                out.append(swift(phrase.substring(i)));
                break;
            }
            out.append(swift(phrase.substring(i, open)));
            String name = phrase.substring(open + 2, close);
            if ("applicationName".equals(name)) {
                out.append("\\(.applicationName)");
            } else if (declaresParameter(intent, name)) {
                out.append("\\(\\.$").append(paramIdentifier(intent, name)).append(")");
            } else {
                // Not a parameter and not Apple's token: leave it alone rather than emitting an
                // interpolation of something that does not exist, which would not compile.
                out.append(swift(phrase.substring(open, close + 1)));
            }
            i = close + 1;
        }
        return out.toString();
    }

    private boolean declaresParameter(Map<String, Object> intent, String name) {
        for (Map<String, Object> p : params(intent)) {
            if (name.equals(str(p, "name"))) {
                return true;
            }
        }
        return false;
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
            sb.append("    let detail: String?\n");
            // The @EntityImage thumbnail, carried so a picker row shows the entity the way the
            // application declared it rather than as a bare line of text.
            sb.append("    let thumbnail: Data?\n\n");
            sb.append("    static var typeDisplayRepresentation: TypeDisplayRepresentation =\n");
            sb.append("        TypeDisplayRepresentation(name: \"")
                    .append(swift(str(entity, "title"))).append("\")\n\n");
            sb.append("    var displayRepresentation: DisplayRepresentation {\n");
            sb.append("        DisplayRepresentation(title: \"\\(name)\", subtitle: "
                    + "detail.map { \"\\($0)\" },\n");
            sb.append("                              image: thumbnail.map "
                    + "{ DisplayRepresentation.Image(data: $0) })\n");
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
                    .append("(id: $0.id, name: $0.title, detail: $0.subtitle, thumbnail: $0.image) }\n");
            sb.append("    }\n\n");
            if (queries.contains("SUGGESTED")) {
                sb.append("    func suggestedEntities() async throws -> [").append(struct)
                        .append("] {\n");
                sb.append("        CN1IntentBridge.entities(type: \"").append(swift(type))
                        .append("\", kind: \"suggested\", argument: nil)\n");
                sb.append("            .map { ").append(struct)
                        .append("(id: $0.id, name: $0.title, detail: $0.subtitle, thumbnail: $0.image) }\n");
                sb.append("    }\n\n");
            }
            if (search) {
                sb.append("    func entities(matching string: String) async throws -> [")
                        .append(struct).append("] {\n");
                sb.append("        CN1IntentBridge.entities(type: \"").append(swift(type))
                        .append("\", kind: \"search\", argument: string)\n");
                sb.append("            .map { ").append(struct)
                        .append("(id: $0.id, name: $0.title, detail: $0.subtitle, thumbnail: $0.image) }\n");
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

    /// The range clause for a parameter whose Swift type is wider than the Java one.
    ///
    /// Swift's Int is 64-bit and its Double is a double, so a Java int or float was offered to
    /// Siri and the Shortcuts app without bounds -- the platform accepted 5000000000 or 1e100,
    /// completed the whole interaction, and the generated coercion then refused it before the
    /// handler ran. Bounding the parameter moves that refusal to where the value is entered.
    ///
    /// Float is not an option: App Intents rejects it as a parameter type outright
    /// (IntentParameter requires _IntentValue, which Float does not conform to), so a Java
    /// float stays a Double with a float's range on it.
    ///
    /// One case is not expressible. requiredFloat also refuses a non-zero value that rounds to
    /// zero, and inclusiveRange cannot describe an interval with a hole in it -- so 1e-100 is
    /// still accepted here and rejected at dispatch, with a message that says why.
    private static String inclusiveRange(String type, boolean hasChoices) {
        if (hasChoices) {
            return "";
        }
        if ("int".equals(type)) {
            return ", inclusiveRange: (-2147483648, 2147483647)";
        }
        if ("float".equals(type)) {
            return ", inclusiveRange: (-3.4028234663852886e38, 3.4028234663852886e38)";
        }
        return "";
    }

    /// Legal, unique Swift identifiers for one intent's parameters, keyed by declared name.
    ///
    /// Sanitizing each name where it was needed was not enough, in two ways that both end as a
    /// compile error in generated Swift the developer never sees. A name beginning with a digit
    /// -- "1st" -- is not an identifier at all. And two distinct names may reduce to one:
    /// "ship-to" and "ship to" both become ship_to, which declared the property twice and, for
    /// a parameter with a closed vocabulary, the choice enum twice as well.
    ///
    /// Computed for the whole intent rather than per site so every place that spells a
    /// parameter agrees: the property, its type when that is a generated enum, the body that
    /// reads it, and a phrase that interpolates it. Recomputed at each site rather than
    /// threaded through, because it is a pure function of the intent and disagreement between
    /// two copies is exactly the failure being fixed.
    private static Map<String, String> paramIdentifiers(List<Map<String, Object>> params) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        List<String> taken = new ArrayList<String>();
        for (int i = 0; i < RESERVED_INTENT_MEMBERS.length; i++) {
            taken.add(RESERVED_INTENT_MEMBERS[i]);
        }
        for (Map<String, Object> p : params) {
            String raw = str(p, "name");
            String base = legalIdentifier(sanitize(raw));
            String candidate = base;
            for (int n = 2; taken.contains(candidate); n++) {
                candidate = base + "_" + n;
            }
            taken.add(candidate);
            out.put(raw, candidate);
        }
        return out;
    }

    /// The identifier this intent spells one of its parameters with.
    private static String paramIdentifier(Map<String, Object> intent, String rawName) {
        String id = paramIdentifiers(params(intent)).get(rawName);
        return id == null ? legalIdentifier(sanitize(rawName)) : id;
    }

    /// Back-quotes a generated identifier, always.
    ///
    /// This used to consult a table of Swift keywords, and a table is the wrong shape for the
    /// problem: the list was missing `associatedtype`, `inout`, `precedencegroup` and more, and
    /// Swift adds keywords with every release, so it could only ever be missing more later.
    /// Each omission is an illegal declaration in a generated file and a failed iOS build.
    ///
    /// Backticks are legal around *any* identifier, keyword or not -- checked against swiftc in
    /// all three positions this emits: a property declaration, an enum case declaration, and a
    /// case reference. So there is nothing to keep current.
    ///
    /// Deliberately not applied to a phrase's key path. A property wrapper's projected value is
    /// `$name`, which is not the keyword, and swiftc accepts `\.$associatedtype` bare.
    private static String escaped(String identifier) {
        return "`" + identifier + "`";
    }

    /// Makes an identifier that can start a Swift name. Sanitizing leaves digits and an empty
    /// string alone, and neither can begin one.
    private static String legalIdentifier(String s) {
        if (s.length() == 0 || !Character.isLetter(s.charAt(0))) {
            return "v" + s;
        }
        return s;
    }

    /// Back-quotes an identifier that collides with a Swift keyword.
    ///
    /// Only the declaration needs it. A property wrapper's projected value is `$name`, which is
    /// not the keyword, so a phrase interpolating it is written bare -- checked against the
    /// Swift compiler rather than assumed, since guessing wrong here fails only on a Mac.
    private static String varName(String identifier) {
        return escaped(identifier);
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
            } else if (c == '\r') {
                // Swift ends a single-line literal at a carriage return exactly as it does at a
                // newline, so letting one through unterminates the string and the iOS target
                // fails to compile. A description written on Windows carries them in pairs.
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else if (c == '\0') {
                sb.append("\\0");
            } else if (c < 0x20 || c == 0x7f) {
                // Every other control character has no literal spelling. Swift's numeric
                // escape covers them all, and is what it documents for exactly this. Spelled
                // in two pieces because Java expands a backslash-u sequence during lexing --
                // in comments as well as in code, which is why this note avoids writing one.
                sb.append("\\").append("u{").append(Integer.toHexString(c)).append("}");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Manifest access
    // ------------------------------------------------------------------

    /// True when the intent offered itself to the platform. An absent list means the default,
    /// which is platform exposure.
    @SuppressWarnings("unchecked")
    public static boolean isExposedToAssistant(Map<String, Object> intent) {
        Object exposure = intent.get("exposure");
        if (!(exposure instanceof List)) {
            // No exposure key at all: an older manifest, or a declaration that never said. The
            // default is what an omitted element means.
            return true;
        }
        // An empty list is not the same as no list. A declaration that wrote exposure = {} chose
        // no platform consumer, and the processor preserves that -- so treating it as the
        // default here generated an executable App Intent for a capability that had asked for
        // none of this.
        List<Object> list = (List<Object>) exposure;
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

    /// A parameter's declared closed vocabulary, or empty.
    @SuppressWarnings("unchecked")
    private static List<String> options(Map<String, Object> param) {
        Object o = param.get("options");
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
