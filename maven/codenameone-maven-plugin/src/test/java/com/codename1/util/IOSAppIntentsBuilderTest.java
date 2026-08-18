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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The generated Swift cannot be compiled here, so these pin the properties that would otherwise
 * only fail on a Mac: availability fencing, the app-name token Apple requires in a phrase,
 * entity queries matching what the app actually declared, and never emitting an empty shortcuts
 * provider.
 */
class IOSAppIntentsBuilderTest {

    private static Map<String, Object> intent(String id, String title, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("id", id);
        m.put("title", title);
        m.put("description", "");
        m.put("headless", Boolean.TRUE);
        m.put("discoverable", Boolean.TRUE);
        m.put("destructive", Boolean.FALSE);
        m.put("opensRoute", "");
        m.put("phrases", new ArrayList<String>());
        m.put("params", new ArrayList<Map<String, Object>>());
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> param(String name, String type, Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("name", name);
        m.put("title", name);
        m.put("type", type);
        m.put("required", Boolean.TRUE);
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Map<String, Object> entity(String type, String title, String... queries) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("type", type);
        m.put("title", title);
        m.put("indexed", Boolean.FALSE);
        m.put("queries", Arrays.asList(queries));
        return m;
    }

    private static String intentsSwift(List<Map<String, Object>> intents,
                                        List<Map<String, Object>> entities) {
        return new IOSAppIntentsBuilder(intents, entities)
                .buildAppTargetFileMap().get("CN1AppIntents.swift");
    }

    /// Options are application strings, so two legal and quite different ones reduce to the
    /// same Swift identifier -- and a duplicated `case` does not degrade at runtime, it fails
    /// to compile the iOS target, in a file the developer never wrote.
    @Test
    void collidingOptionsGetDistinctSwiftCases() {
        Map<String, Object> p = param("kind", "string",
                "options", Arrays.asList("in-person", "in person", "in_person"));
        Map<String, Object> i = intent("book_it", "Book it");
        i.put("params", Arrays.asList(p));

        String swift = intentsSwift(Arrays.asList(i), new ArrayList<Map<String, Object>>());

        // Every raw value survives -- that is what the platform matches on.
        assertTrue(swift.contains("= \"in-person\""));
        assertTrue(swift.contains("= \"in person\""));
        assertTrue(swift.contains("= \"in_person\""));

        // And no identifier is declared twice.
        List<String> cases = new ArrayList<String>();
        for (String line : swift.split("\n")) {
            String t = line.trim();
            if (t.startsWith("case ") && t.contains(" = \"")) {
                String name = t.substring(5, t.indexOf(" = \""));
                assertFalse(cases.contains(name),
                        "duplicate Swift case \"" + name + "\" would not compile:\n" + swift);
                cases.add(name);
            }
        }
        assertEquals(3, cases.size());
    }

    /// An explicitly written exposure = {} chose no platform consumer, and the processor
    /// preserves it. Treating it as the default here emitted an executable App Intent for a
    /// capability that had asked for none of this.
    @Test
    void anEmptyExposureProducesNoAppIntent() {
        Map<String, Object> internal = intent("internal_only", "Internal");
        internal.put("exposure", new ArrayList<Object>());

        String swift = intentsSwift(
                Arrays.asList(internal, intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("internal_only"),
                "an empty exposure selected no consumer:\n" + swift);
        assertTrue(swift.contains("log_workout"));
    }

    /// Apple allows a provider ten app shortcuts. Emitting more does not get more -- the
    /// excess is rejected -- and which ten survived was decided by whatever order the scanner
    /// walked the disk in, so a rebuild that changed nothing could change which phrases work.
    @Test
    void appShortcutsAreCappedAtApplesLimitAndTheRestAreNamed() {
        List<Map<String, Object>> declared = new ArrayList<Map<String, Object>>();
        String[] ids = {"m1", "m2", "m3", "a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "z1"};
        for (String id : ids) {
            Map<String, Object> i = intent(id, "Title " + id);
            i.put("phrases", Arrays.asList("Do " + id + " in ${applicationName}"));
            declared.add(i);
        }

        IOSAppIntentsBuilder gen =
                new IOSAppIntentsBuilder(declared, new ArrayList<Map<String, Object>>());
        String swift = gen.buildAppTargetFileMap().get("CN1AppIntents.swift");

        int shortcuts = swift.split("AppShortcut\\(").length - 1;
        assertEquals(10, shortcuts, "Apple allows ten:\n" + swift);
        assertEquals(Arrays.asList("z1", "m3"), gen.getOmittedShortcutIds(),
                "chosen by id, and the omitted ones are named rather than dropped quietly");
    }

    @Test
    void everythingIsAvailabilityFencedSoALowerTargetStillBuilds() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("#if canImport(AppIntents)"),
                "without the import fence an older SDK cannot compile this at all");
        assertTrue(swift.contains("@available(iOS 16.0, *)"),
                "without availability fencing every app declaring an intent is forced to raise "
                        + "its deployment target");
        assertTrue(swift.trim().endsWith("#endif"));
    }

    @Test
    void anIntentBecomesAnAppIntentStructThatDelegatesToTheBridge() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout",
                        "params", Arrays.asList(param("minutes", "int")))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("struct CN1Intent_log_workout: AppIntent"));
        assertTrue(swift.contains("@Parameter(title: \"minutes\")"));
        assertTrue(swift.contains("var `minutes`: Int"));
        assertTrue(swift.contains("CN1IntentBridge.run(id: \"log_workout\""),
                "the Swift is a shell; the behaviour has to cross to Java");
    }

    @Test
    void theApplicationNameTokenBecomesApplesPhrasePlaceholder() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout",
                        "phrases", Arrays.asList("Log a workout in ${applicationName}"))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("\\(.applicationName)"),
                "a phrase that keeps the literal token is rejected by App Review");
        assertFalse(swift.contains("${applicationName}"));
    }

    @Test
    void anAppWithNoPhrasesGetsNoShortcutsProvider() {
        // An AppShortcutsProvider with an empty body does not compile on Apple's side, so an
        // app whose intents are Shortcuts-only must not get one.
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("AppShortcutsProvider"));
    }

    @Test
    void anIntentThatOpensARouteRunsInTheForeground() {
        String swift = intentsSwift(
                Arrays.asList(intent("show_order", "Show order", "opensRoute", "/orders/{id}")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("openAppWhenRun: Bool = true"));
    }

    @Test
    void anIntentThatAnswersInPlaceDoesNotOpenTheApp() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("openAppWhenRun: Bool = false"));
    }

    /// The flag handed to Java has to describe what actually happens. openAppWhenRun is
    /// (route || !headless), so a routed intent is opened before perform() runs -- and calling
    /// it headless would send the handler down the no-UI path in a foregrounded app. Android
    /// already marks routed invocations non-headless.
    @Test
    void aRoutedIntentIsNotReportedAsHeadless() {
        String swift = intentsSwift(
                Arrays.asList(intent("show_order", "Show order", "headless", Boolean.TRUE,
                        "opensRoute", "/orders/{id}")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("openAppWhenRun: Bool = true"), swift);
        assertTrue(swift.contains("headless: false"),
                "iOS opens the app for a routed intent, so the handler must not be told "
                        + "otherwise: " + swift);
    }

    @Test
    void anUnroutedHeadlessIntentStillReportsHeadless() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout", "headless", Boolean.TRUE)),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("headless: true"), swift);
    }

    /// headless is what decides this, not opensRoute alone. A handler that did not declare
    /// headless is allowed to touch a Form -- that is the whole meaning of the flag -- so
    /// running it with no foreground window hands it a Display with nothing on screen, inside
    /// Siri, where there is nowhere to report the failure.
    @Test
    void anIntentThatIsNotHeadlessOpensTheApp() {
        String swift = intentsSwift(
                Arrays.asList(intent("show_summary", "Show summary", "headless", Boolean.FALSE)),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("openAppWhenRun: Bool = true"));
    }

    /// A closed vocabulary the platform does not know about is a vocabulary the platform will
    /// not offer: Shortcuts accepts any text and the Java `oneOf` check only rejects it after
    /// the interaction is over, which is the worst moment to tell someone their answer was
    /// wrong. An AppEnum makes the picker offer exactly the declared values.
    @Test
    void aClosedVocabularyBecomesAnAppEnum() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout",
                        "params", Arrays.asList(param("kind", "string",
                                "options", Arrays.asList("run", "ride"))))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("enum CN1Choice_log_workout_kind: String, AppEnum"));
        assertTrue(swift.contains("case `run` = \"run\""));
        assertTrue(swift.contains("case `ride` = \"ride\""));
        assertTrue(swift.contains("var `kind`: CN1Choice_log_workout_kind"),
                "the parameter has to be typed as the enum or the platform still takes free text");
        assertTrue(swift.contains("`kind`.rawValue"),
                "Java declared a String, so that is what must cross");
    }

    @Test
    void aStringWithoutOptionsStaysAPlainString() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout",
                        "params", Arrays.asList(param("note", "string")))),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("AppEnum"));
        assertTrue(swift.contains("var `note`: String"));
    }

    /// IntentResult.entity(...) writes no "value", so returning outcome.value gave Shortcuts an
    /// empty string for a result form the API advertises as feeding a following action.
    @Test
    void anEntityResultIsNotReturnedAsAnEmptyString() {
        String swift = intentsSwift(
                Arrays.asList(intent("find_order", "Find order")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("value: outcome.resultValue"));
        assertFalse(swift.contains("value: outcome.value ?? \"\""));
    }

    @Test
    void anEntityParameterCrossesAsItsIdOnly() {
        String swift = intentsSwift(
                Arrays.asList(intent("play_list", "Play",
                        "params", Arrays.asList(
                                param("playlist", "entity", "entityType", "playlist")))),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID")));

        assertTrue(swift.contains("var `playlist`: CN1Entity_playlist"));
        assertTrue(swift.contains("`playlist`.id"),
                "the app's own object never crosses; only its id does");
    }

    /// An @EntityImage that never reaches a DisplayRepresentation is an annotation the
    /// application wrote and the platform ignores, which is indistinguishable from a bug in
    /// their code. Every construction site has to carry it, not just the by-id one.
    @Test
    void anEntityCarriesItsThumbnailIntoEveryPicker() {
        String swift = new IOSAppIntentsBuilder(
                Arrays.asList(intent("play_list", "Play")),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID", "SUGGESTED", "SEARCH")))
                .buildAppTargetFileMap().get("CN1AppEntities.swift");

        assertTrue(swift.contains("let thumbnail: Data?"));
        assertTrue(swift.contains("image: thumbnail.map { DisplayRepresentation.Image(data: $0) }"),
                "the thumbnail has to reach the display representation to be shown at all");
        int constructed = swift.split("thumbnail: \\$0\\.image", -1).length - 1;
        assertEquals(3, constructed,
                "by-id, suggested and search all build entities and all three must carry it");
    }

    @Test
    void searchIsOnlyOfferedWhenTheEntityDeclaredIt() {
        IOSAppIntentsBuilder withSearch = new IOSAppIntentsBuilder(
                Arrays.asList(intent("play_list", "Play")),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID", "SEARCH")));
        IOSAppIntentsBuilder without = new IOSAppIntentsBuilder(
                Arrays.asList(intent("play_list", "Play")),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID")));

        String a = withSearch.buildAppTargetFileMap().get("CN1AppEntities.swift");
        String b = without.buildAppTargetFileMap().get("CN1AppEntities.swift");

        assertTrue(a.contains("EntityStringQuery"));
        assertTrue(a.contains("entities(matching string: String)"));
        assertFalse(b.contains("EntityStringQuery"),
                "conforming to string search without a SEARCH query offers the user a box that "
                        + "always comes back empty");
        assertFalse(b.contains("entities(matching"));
    }

    @Test
    void suggestedEntitiesAreOnlyOfferedWhenDeclared() {
        String swift = new IOSAppIntentsBuilder(
                Arrays.asList(intent("play_list", "Play")),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID", "SUGGESTED")))
                .buildAppTargetFileMap().get("CN1AppEntities.swift");

        assertTrue(swift.contains("suggestedEntities()"));
        assertTrue(swift.contains("kind: \"suggested\""));
    }

    @Test
    void anAppWithNoEntitiesGetsNoEntitiesFile() {
        Map<String, String> files = new IOSAppIntentsBuilder(
                Arrays.asList(intent("log_workout", "Log")),
                new ArrayList<Map<String, Object>>()).buildAppTargetFileMap();

        assertFalse(files.containsKey("CN1AppEntities.swift"));
    }

    @Test
    void aFailedHandlerFailsTheIntentRatherThanReportingSuccess() {
        // A dialog-only success made Shortcuts record the action as successful and run every
        // action after it, which is worse than the original error.
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("if !outcome.ok"), swift);
        assertTrue(swift.contains("throw CN1IntentFailure(message: outcome.spoken)"), swift);
    }

    @Test
    void aReturnedValueReachesTheNextShortcutAction() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("ReturnsValue<String>"), swift);
        // resultValue is outcome.value, falling back to a returned entity's title -- see
        // anEntityResultIsNotReturnedAsAnEmptyString.
        assertTrue(swift.contains(".result(value: outcome.resultValue"), swift);
    }

    @Test
    void aNonDiscoverableIntentIsNotOfferedBeforeItIsDonated() {
        Map<String, Object> hidden = intent("secret_one", "Secret");
        hidden.put("discoverable", Boolean.FALSE);

        String swift = intentsSwift(Arrays.asList(hidden), new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("isDiscoverable: Bool = false"), swift);
    }

    @Test
    void aDiscoverableIntentGetsNoOverride() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("isDiscoverable"), swift);
    }

    @Test
    void aDestructiveIntentAsksBeforeItActs() {
        Map<String, Object> destructive = intent("delete_all", "Delete everything");
        destructive.put("destructive", Boolean.TRUE);

        String swift = intentsSwift(Arrays.asList(destructive),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("try await requestConfirmation()"), swift);
    }

    @Test
    void anOptionalParameterBecomesAnOptionalSwiftType() {
        // A non-optional Swift type makes the system prompt for a value the handler was happy
        // to do without, and the Java-side default never applies.
        Map<String, Object> optional = param("note", "string", "required", Boolean.FALSE);
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log", "params", Arrays.asList(optional))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("var `note`: String?"), swift);
        // Absent means absent: nothing is put in the map, so Java applies its own default.
        assertTrue(swift.contains("if let v = `note`"), swift);
    }

    @Test
    void aRequiredParameterStaysNonOptional() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log",
                        "params", Arrays.asList(param("minutes", "int")))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("var `minutes`: Int\n"), swift);
        assertFalse(swift.contains("var `minutes`: Int?"), swift);
    }

    @Test
    void aModelOnlyIntentDoesNotBecomeAnAppIntent() {
        Map<String, Object> modelOnly = intent("model_one", "Model");
        modelOnly.put("exposure", Arrays.asList("MODEL"));
        Map<String, Object> normal = intent("public_one", "Public");
        normal.put("exposure", Arrays.asList("ASSISTANT"));

        String swift = intentsSwift(Arrays.asList(modelOnly, normal),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("CN1Intent_public_one"));
        assertFalse(swift.contains("CN1Intent_model_one"),
                "exposure is a restriction, not a hint");
    }

    @Test
    void anIntentWithNoExposureListedKeepsTheDefault() {
        // An absent list is the default, which is platform exposure; treating it as "no
        // consumers" would silently drop every intent built before the field existed.
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("CN1Intent_log_workout"));
    }

    @Test
    void aPhraseParameterBindsToTheGeneratedProperty() {
        // Left as literal text the phrase reads the placeholder back to the user and never
        // supplies the argument, so the documented parameterized phrases could not invoke.
        // A key path, not a plain member: AppShortcutPhraseToken only carries applicationName,
        // and the interpolation that takes a parameter is declared over
        // KeyPath<Intent, IntentParameter<Value>>. Verified by building against the iOS SDK.
        Map<String, Object> withParams = intent("play_list", "Play",
                "phrases", Arrays.asList("Play ${playlist} in ${applicationName}"),
                "params", Arrays.asList(param("playlist", "entity", "entityType", "playlist")));

        String swift = intentsSwift(Arrays.asList(withParams),
                Arrays.asList(entity("playlist", "Playlist", "BY_ID")));

        assertTrue(swift.contains("\\(\\.$playlist)"), swift);
        assertTrue(swift.contains("\\(.applicationName)"), swift);
        assertFalse(swift.contains("${"), swift);
    }

    @Test
    void aPlaceholderThatNamesNoParameterIsLeftAlone() {
        // Emitting an interpolation of a property that does not exist would not compile, which
        // is a worse outcome than a phrase reading slightly oddly.
        Map<String, Object> odd = intent("log_workout", "Log",
                "phrases", Arrays.asList("Log ${nonsense} in ${applicationName}"));

        String swift = intentsSwift(Arrays.asList(odd), new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("${nonsense}"), swift);
        assertFalse(swift.contains(".$nonsense"), swift);
    }

    /// A parameter name is application text. One beginning with a digit is not a Swift
    /// identifier at all, and two distinct names can reduce to one -- either way the generated
    /// iOS target fails to compile, for a file the developer never wrote.
    @Test
    void illegalOrCollidingParameterNamesStillProduceCompilableSwift() {
        String swift = intentsSwift(
                Arrays.asList(intent("ship_it", "Ship",
                        "params", Arrays.asList(param("1st", "int"),
                                param("ship-to", "string"),
                                param("ship to", "string")))),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("var 1st"),
                "a property cannot begin with a digit:\n" + swift);

        List<String> declared = new ArrayList<String>();
        String[] lines = swift.split("\n");
        for (int i = 1; i < lines.length; i++) {
            String t = lines[i].trim();
            // Only the declared parameters: the body has a `var params` of its own.
            if (lines[i - 1].trim().startsWith("@Parameter(") && t.startsWith("var ")) {
                String name = t.substring(4, t.indexOf(": "));
                assertTrue(Character.isLetter(name.charAt(0)) || name.charAt(0) == '`',
                        "illegal Swift identifier \"" + name + "\":\n" + swift);
                assertFalse(declared.contains(name),
                        "duplicate property \"" + name + "\" would not compile:\n" + swift);
                declared.add(name);
            }
        }
        assertEquals(3, declared.size());

        // Every declared name still reaches Java under the name the application declared.
        assertTrue(swift.contains("params[\"1st\"]"));
        assertTrue(swift.contains("params[\"ship-to\"]"));
        assertTrue(swift.contains("params[\"ship to\"]"));
    }

    /// A closed vocabulary makes the collision worse: the enum type is named after the
    /// parameter too, so two colliding names declared the same type twice.
    @Test
    void collidingParameterNamesDoNotDeclareOneChoiceEnumTwice() {
        String swift = intentsSwift(
                Arrays.asList(intent("ship_it", "Ship",
                        "params", Arrays.asList(
                                param("ship-to", "string", "options", Arrays.asList("home")),
                                param("ship to", "string", "options", Arrays.asList("work"))))),
                new ArrayList<Map<String, Object>>());

        List<String> enums = new ArrayList<String>();
        for (String line : swift.split("\n")) {
            String t = line.trim();
            if (t.startsWith("enum ") && t.contains(":")) {
                String name = t.substring(5, t.indexOf(":")).trim();
                assertFalse(enums.contains(name),
                        "duplicate enum \"" + name + "\" would not compile:\n" + swift);
                enums.add(name);
            }
        }
        assertEquals(2, enums.size());
    }

    /// Every generated identifier is back-quoted, keyword or not. A table of keywords was the
    /// wrong shape: it was missing associatedtype, inout and precedencegroup, and Swift adds
    /// more with every release, so it could only ever be missing more later -- each omission an
    /// illegal declaration in a file the developer never wrote.
    @Test
    void everyGeneratedParameterIdentifierIsEscaped() {
        // Parameter names come from application code, so a Swift keyword is reachable rather
        // than theoretical, and an unescaped one is a compile error on a Mac only.
        String swift = intentsSwift(
                Arrays.asList(intent("do_thing", "Do",
                        "params", Arrays.asList(param("repeat", "int")))),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("var `repeat`: Int"));

        // And one that is not a keyword at all, so no table decides it. associatedtype was
        // missing from the old list; nothing is missing from "always".
        String other = intentsSwift(
                Arrays.asList(intent("do_thing", "Do",
                        "params", Arrays.asList(param("associatedtype", "string")))),
                new ArrayList<Map<String, Object>>());
        assertTrue(other.contains("var `associatedtype`: String"), other);
    }

    /// Swift ends a single-line literal at a carriage return exactly as it does at a newline,
    /// so one written through unterminates the string and the iOS target fails to compile.
    /// A description typed on Windows carries them in pairs.
    @Test
    void controlCharactersCannotBreakOutOfTheStringLiteral() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log",
                        "description", "First\r\nSecond\tthird\u0001")),
                new ArrayList<Map<String, Object>>());

        assertFalse(swift.contains("\r"), "a raw carriage return ends the literal:\n" + swift);
        assertFalse(swift.contains("\t"), "a raw tab is not spelled in a literal:\n" + swift);
        assertFalse(swift.contains(String.valueOf((char) 1)),
                "no control character may be written raw");
        assertTrue(swift.contains("First\\r\\nSecond\\tthird\\" + "u{1}"),
                "each one keeps its meaning as an escape:\n" + swift);
    }

    @Test
    void quotesInATitleCannotBreakOutOfTheStringLiteral() {
        String swift = intentsSwift(
                Arrays.asList(intent("log_workout", "Log a \"hard\" workout")),
                new ArrayList<Map<String, Object>>());

        assertTrue(swift.contains("Log a \\\"hard\\\" workout"));
    }
}
