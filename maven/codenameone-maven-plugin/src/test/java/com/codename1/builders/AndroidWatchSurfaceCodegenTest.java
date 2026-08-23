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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The decisions the Wear complication codegen makes, as pure functions so they can be pinned
/// without generating a project.
class AndroidWatchSurfaceCodegenTest {

    private static BuildRequest request() {
        BuildRequest req = new BuildRequest();
        req.setMainClass("MyApp");
        req.setPackageName("com.mycompany.myapp");
        req.setDisplayName("My App");
        req.setVersion("1.0");
        return req;
    }

    // --- which module is the watch product ------------------------------------

    /// A project that has not asked for a watch must be left completely alone, which is what
    /// every branch downstream keys on.
    @Test
    void noWatchMainMeansNoWatchModule() {
        assertNull(AndroidGradleBuilder.watchModuleName(request()));
    }

    /// Standalone: the single APK IS the watch app, so the services go in the phone module's
    /// place -- there is no phone app to keep them out of.
    @Test
    void standaloneMakesTheAppModuleTheWatchProduct() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");
        req.putArgument("watchStandalone", "true");

        assertEquals("app", AndroidGradleBuilder.watchModuleName(req));
    }

    /// Companion: a second module beside the phone one.
    @Test
    void companionGeneratesASeparateWearModule() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");

        assertEquals("wear", AndroidGradleBuilder.watchModuleName(req));
    }

    /// A project that wants the wearable link but no watch app of its own can say so, and its
    /// phone build is then exactly what it was.
    @Test
    void theWearModuleCanBeDeclined() {
        BuildRequest req = request();
        req.putArgument("watchMain", "com.mycompany.myapp.Watch");
        req.putArgument("android.watchModule", "false");

        assertNull(AndroidGradleBuilder.watchModuleName(req));
    }

    // --- where the wear libraries land ------------------------------------------

    /// The androidx.wear complication and Tile libraries declare minSdk 26, and only the WATCH
    /// module is raised to it. In a companion build the phone module keeps its own floor, so a
    /// shared dependency list makes its manifest merge fail against libraries it never uses --
    /// which is exactly what happened: a phone app on API 24 stopped building the moment a watch
    /// family was declared.
    @Test
    void theWearLibrariesNeverReachAPhoneModule() {
        BuildRequest companion = request();
        companion.putArgument("watchMain", "com.mycompany.myapp.Watch");

        // The companion phone module is not the watch product, so nothing wear-specific may be
        // added to the dependency hint it shares.
        assertEquals("wear", AndroidGradleBuilder.watchModuleName(companion));
        assertEquals("", companion.getArg("gradleDependencies", ""),
                "a companion phone module must carry no androidx.wear dependency");
    }

    /// A standalone build is the other way round: that single module IS the watch, so the
    /// libraries and the 26 floor both belong to it.
    @Test
    void aStandaloneModuleIsTheWatchProductAndTakesBoth() {
        BuildRequest standalone = request();
        standalone.putArgument("watchMain", "com.mycompany.myapp.Watch");
        standalone.putArgument("watchStandalone", "true");

        assertEquals("app", AndroidGradleBuilder.watchModuleName(standalone));
    }

    // --- where the generated services land --------------------------------------

    /// The wear module shares the phone's source directory, so a service written to the phone's
    /// root is compiled by BOTH -- and these import androidx.wear, whose dependencies belong to
    /// the wear module alone. A companion build therefore failed compiling the phone module
    /// against imports it has no libraries for.
    @Test
    void aCompanionBuildGeneratesTheServicesInTheWearModule() {
        File appSrc = new File("/tmp/proj/app/src/main/java");

        File watchSrc = AndroidGradleBuilder.watchSourceRoot("wear", appSrc);

        assertEquals(new File("/tmp/proj/wear/src/main/java"), watchSrc);
    }

    /// A standalone build has one module, which IS the watch, so they belong where they are.
    @Test
    void aStandaloneBuildGeneratesThemInPlace() {
        File appSrc = new File("/tmp/proj/app/src/main/java");

        assertEquals(appSrc, AndroidGradleBuilder.watchSourceRoot("app", appSrc));
    }

    // --- which services get copied ----------------------------------------------

    /// Gradle compiles every source in the tree whether or not a generated subclass names it,
    /// and the tiles/protolayout dependencies are added only for a rectangular family -- so a
    /// complication-only build that copied the Tile service failed on unresolved imports.
    @Test
    void aComplicationOnlyBuildDoesNotCopyTheTileService() {
        List<String[]> kinds = new ArrayList<String[]>();
        kinds.add(new String[] {"a", "A", "watchCircular,watchInline,watchCorner"});

        List<String> sources = AndroidGradleBuilder.watchSurfaceSources(kinds);

        assertEquals(1, sources.size(), sources.toString());
        assertEquals("CN1ComplicationDataSource.java", sources.get(0));
    }

    @Test
    void aRectangularFamilyBringsTheTileServiceWithIt() {
        List<String[]> kinds = new ArrayList<String[]>();
        kinds.add(new String[] {"a", "A", "watchCircular"});
        kinds.add(new String[] {"b", "B", "watchRectangular"});

        List<String> sources = AndroidGradleBuilder.watchSurfaceSources(kinds);

        assertEquals(2, sources.size(), sources.toString());
        assertTrue(sources.contains("CN1SurfaceTileService.java"), sources.toString());
    }

    // --- family to ComplicationData mapping ------------------------------------

    /// A watch face asks a data source for ONE type and gets nothing if the source does not
    /// offer it, so this list is what decides whether a complication can be placed at all.
    @Test
    void circularOffersAGaugeAGlyphAndAReadout() {
        assertEquals("RANGED_VALUE,MONOCHROMATIC_IMAGE,SHORT_TEXT",
                AndroidGradleBuilder.complicationTypes("watchCircular"));
    }

    /// Wear OS has no corner slot at all; a corner complication is round, so it offers exactly
    /// what the circular family does -- which is what WidgetSize already documents.
    @Test
    void cornerIsTreatedAsCircular() {
        assertEquals(AndroidGradleBuilder.complicationTypes("watchCircular"),
                AndroidGradleBuilder.complicationTypes("watchCorner"));
    }

    @Test
    void inlineIsShortTextOnly() {
        assertEquals("SHORT_TEXT", AndroidGradleBuilder.complicationTypes("watchInline"));
    }

    @Test
    void rectangularIsTheRoomyOne() {
        assertEquals("LONG_TEXT,SHORT_TEXT",
                AndroidGradleBuilder.complicationTypes("watchRectangular"));
    }

    /// Declaring several families offers the union, deduped, so one data source can fill every
    /// slot the developer designed for.
    @Test
    void severalFamiliesOfferTheUnionWithoutRepeats() {
        String types = AndroidGradleBuilder.complicationTypes(
                "watchCircular,watchRectangular,watchInline");

        assertTrue(types.contains("RANGED_VALUE"), types);
        assertTrue(types.contains("LONG_TEXT"), types);
        assertEquals(types.indexOf("SHORT_TEXT"), types.lastIndexOf("SHORT_TEXT"), types);
    }

    /// A phone family contributes nothing: a home-screen size is not a watch-face slot.
    @Test
    void phoneFamiliesContributeNoComplicationTypes() {
        assertEquals("", AndroidGradleBuilder.complicationTypes("small,medium,large,lockscreen"));
    }

    // --- version codes -----------------------------------------------------------

    /// A watch APK must outrank the phone's. On a watch, Play picks among the APKs the device
    /// supports by version code; on a phone the required watch feature filters the wear one out
    /// entirely, so the phone APK still wins there.
    @Test
    void theWearArtifactOutranksThePhoneOne() throws BuildException {
        assertEquals(100 + AndroidGradleBuilder.DEFAULT_WATCH_VERSION_CODE_OFFSET,
                AndroidGradleBuilder.wearVersionCode(request(), 100));
    }

    @Test
    void theWearVersionCodeCanBeSetOutright() throws BuildException {
        BuildRequest req = request();
        req.putArgument("android.watchVersionCode", "5000");

        assertEquals(5000, AndroidGradleBuilder.wearVersionCode(req, 100));
    }

    @Test
    void theOffsetCanBeWidenedForAProjectThatNumbersItsBuildsTightly() throws BuildException {
        BuildRequest req = request();
        req.putArgument("android.watchVersionCodeOffset", "50");

        assertEquals(150, AndroidGradleBuilder.wearVersionCode(req, 100));
    }

    /// A malformed hint must not produce a version code that silently reorders the two artifacts.
    @Test
    void aMalformedVersionHintFallsBackToTheDefault() throws BuildException {
        BuildRequest req = request();
        req.putArgument("android.watchVersionCodeOffset", "not a number");

        assertEquals(100 + AndroidGradleBuilder.DEFAULT_WATCH_VERSION_CODE_OFFSET,
                AndroidGradleBuilder.wearVersionCode(req, 100));
    }

    /// A hint that puts the watch at or below the phone cannot be honoured: on a watch that also
    /// supports the phone APK, Play would install the phone build and the user would see it
    /// running on their wrist. That is not a build error anyone would trace back to a hint, so
    /// the build refuses it instead.
    @Test
    void aWearVersionCodeThatDoesNotOutrankThePhoneIsRefused() {
        BuildRequest tooLow = request();
        tooLow.putArgument("android.watchVersionCode", "99");
        assertThrows(BuildException.class, () -> AndroidGradleBuilder.wearVersionCode(tooLow, 100));

        BuildRequest equal = request();
        equal.putArgument("android.watchVersionCode", "100");
        assertThrows(BuildException.class, () -> AndroidGradleBuilder.wearVersionCode(equal, 100));

        BuildRequest zeroOffset = request();
        zeroOffset.putArgument("android.watchVersionCodeOffset", "0");
        assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.wearVersionCode(zeroOffset, 100));

        BuildRequest negativeOffset = request();
        negativeOffset.putArgument("android.watchVersionCodeOffset", "-5");
        assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.wearVersionCode(negativeOffset, 100));
    }

    /// Play refuses a version code it has already seen for an applicationId, and the two
    /// artifacts share one -- so an offset of 1 hands the Wear artifact the code the NEXT phone
    /// release needs, and a project on sequential codes cannot upload its second release at all.
    /// The default partitions the space instead.
    @Test
    void theDefaultOffsetDoesNotConsumeTheNextReleasesCode() throws BuildException {
        int phone = 100;
        int wear = AndroidGradleBuilder.wearVersionCode(request(), phone);

        assertTrue(wear > phone + 1000,
                "an offset a sequential project would reach is not a partition: " + wear);
        assertTrue(wear <= AndroidGradleBuilder.MAX_PLAY_VERSION_CODE, "over Play's ceiling");
    }

    /// A project whose own codes are already enormous -- a date-derived code -- would be pushed
    /// over Play's ceiling, and is told to choose its own offset rather than having one silently
    /// truncated.
    @Test
    void aVersionCodeOverPlaysCeilingIsRefused() {
        assertThrows(BuildException.class,
                () -> AndroidGradleBuilder.wearVersionCode(request(), 2090000000));
    }

    /// The generated class name has to tell two kinds apart, because one class cannot serve two
    /// kinds: the second overwrote the first and both manifest entries pointed at it, so one kind
    /// served the other kind's data. Asserted over the whole declared SET, which is the level the
    /// property belongs to -- the fold on its own is deliberately not injective, so that a kind
    /// keeps the name shipped builds already use.
    @Test
    void twoKindsNeverShareAGeneratedClassName() {
        List<String> ids = Arrays.asList(
                "status", "status_", "_status", "a_b", "ab_", "a__b", "a_b_", "_a_b");
        Map<String, String> named = AndroidGradleBuilder.surfaceKindClassSuffixes(ids);
        assertEquals(ids.size(), named.size(), "every declared kind must be named");
        assertEquals(ids.size(), new java.util.HashSet<String>(named.values()).size(),
                "two ids produced the same class suffix: " + named);
    }

    /// An id without underscores keeps the name it has always had, so no existing project is
    /// renamed by this.
    @Test
    void anIdWithoutUnderscoresIsUnchanged() {
        assertEquals("Status", AndroidGradleBuilder.surfaceKindClassSuffix("status"));
        assertEquals("Weather2", AndroidGradleBuilder.surfaceKindClassSuffix("weather2"));
    }

    /// A malformed explicit code is refused rather than silently replaced. Substituting
    /// intVersion + 1 hid the typo AND recreated the collision this method exists to prevent, with
    /// the developer looking at a hint that says something else entirely.
    @Test
    void aMalformedExplicitVersionCodeIsRefused() {
        BuildRequest req = request();
        req.putArgument("android.watchVersionCode", "not a number");
        assertThrows(BuildException.class, () -> AndroidGradleBuilder.wearVersionCode(req, 100));

        BuildRequest blank = request();
        blank.putArgument("android.watchVersionCode", "   ");
        assertThrows(BuildException.class, () -> AndroidGradleBuilder.wearVersionCode(blank, 100));
    }

    /// The name a shipped build already uses must not move. Android remembers a pinned widget
    /// by its provider ComponentName, so renaming the receiver of an existing kind makes the
    /// widget the user pinned name a receiver that is gone, and the home screen drops it.
    @Test
    void aKindKeepsTheClassNameItAlreadyShipsUnder() {
        assertEquals("DeliveryStatus", AndroidGradleBuilder.surfaceKindClassSuffix("delivery_status"));
        assertEquals("Status", AndroidGradleBuilder.surfaceKindClassSuffix("status"));
        assertEquals("BatteryLevel", AndroidGradleBuilder.surfaceKindClassSuffix("battery_level"));

        // And a whole declared set of ordinary ids resolves to exactly those names.
        Map<String, String> named = AndroidGradleBuilder.surfaceKindClassSuffixes(
                Arrays.asList("delivery_status", "status", "battery_level"));
        assertEquals("DeliveryStatus", named.get("delivery_status"));
        assertEquals("Status", named.get("status"));
        assertEquals("BatteryLevel", named.get("battery_level"));
    }

    /// Two ids differing only in where the underscores are fold to one name, and one generated
    /// class cannot serve two kinds -- the second overwrote the first and both manifest entries
    /// pointed at it. The FIRST declared keeps the shipped name; only the newcomer moves.
    @Test
    void acollidingKindTakesTheDisambiguatedNameAndTheFirstKeepsItsOwn() {
        Map<String, String> named = AndroidGradleBuilder.surfaceKindClassSuffixes(
                Arrays.asList("status", "status_", "a_b", "a__b", "ab"));
        assertEquals("Status", named.get("status"));
        assertEquals("Status_6", named.get("status_"));
        assertEquals("AB", named.get("a_b"));
        assertEquals("AB_1_2", named.get("a__b"), "the later collider moves, not the first");
        // Not every underscore is a collision: "ab" folds to Ab, which nothing else claims.
        assertEquals("Ab", named.get("ab"));
        assertEquals(5, new java.util.HashSet<String>(named.values()).size(),
                "every declared kind must end up with its own class");
    }

    /// The runtime cannot recompute which kind won the plain name -- that is a property of the
    /// whole declared set -- so it READS the map the build wrote. It must not guess by probing
    /// for a class that exists either: CN1Widget_Status exists for "status", so "status_"
    /// probing the plain name first would find the other kind's provider and publish into it.
    /// Read out of the port's source rather than called, because the port class needs an Android
    /// runtime this test does not have.
    @Test
    void theRuntimeReadsTheMapRatherThanGuessing() throws java.io.IOException {
        java.io.File bridge = new java.io.File("../../Ports/Android/src/com/codename1/impl/"
                + "android/surfaces/AndroidSurfaceBridge.java");
        assertTrue(bridge.isFile(), "the port must be readable: " + bridge.getAbsolutePath());
        String source = new String(java.nio.file.Files.readAllBytes(bridge.toPath()), "UTF-8");

        int at = source.indexOf("static synchronized String classSuffix(Context ctx, String kindId) {");
        assertTrue(at >= 0, "the runtime must resolve the name from the generated map");
        String body = source.substring(at, source.indexOf("\n    }\n", at));
        assertTrue(body.contains("\"cn1_surface_kind_classes\""),
                "it must read the array the build writes:\n" + body);
        assertFalse(body.contains("getReceiverInfo"),
                "resolving by what exists finds another kind's provider:\n" + body);

        // And the port's fold has to be the SHIPPED one, with no positions folded in.
        int fold = source.indexOf("static String toClassSuffix(String kindId) {");
        String foldBody = source.substring(fold, source.indexOf("\n    }\n", fold));
        assertFalse(foldBody.contains("positions"),
                "toClassSuffix must stay the name shipped builds use:\n" + foldBody);
    }

    /// The map is the build's own table, written out -- not a second computation. The resource
    /// name has to be the one the port looks up, and every declared kind has to be in it.
    @Test
    void theGeneratedMapNamesEveryKind() throws Exception {
        java.io.File builder = new java.io.File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java");
        assertTrue(builder.isFile(), builder.getAbsolutePath());
        String source = new String(java.nio.file.Files.readAllBytes(builder.toPath()), "UTF-8");

        int at = source.indexOf("private void writeSurfaceKindClassMap(File resDir)");
        assertTrue(at >= 0, "the build must write the map");
        String body = source.substring(at, source.indexOf("\n    }\n", at));
        assertTrue(body.contains("cn1_surface_kind_classes"),
                "the array name must match what the port reads:\n" + body);
        assertTrue(body.contains("surfaceKindClassNames.entrySet()"),
                "it must be written from the table that named the classes, not recomputed:\n"
                        + body);
    }

    // --- tiles ------------------------------------------------------------------

    /// Only the rectangular family is roomy enough for a layout rather than a readout, so it is
    /// the only one that earns a Tile.
    @Test
    void onlyTheRectangularFamilyEarnsATile() {
        assertTrue(AndroidGradleBuilder.declaresTile("watchRectangular"));
        assertTrue(AndroidGradleBuilder.declaresTile("small,watchRectangular"));
        assertFalse(AndroidGradleBuilder.declaresTile("watchCircular,watchInline,watchCorner"));
        assertFalse(AndroidGradleBuilder.declaresTile("small,medium"));
    }
}
