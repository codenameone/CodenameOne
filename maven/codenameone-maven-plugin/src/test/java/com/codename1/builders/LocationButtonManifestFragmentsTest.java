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
import org.junit.jupiter.api.function.Executable;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the manifest fragments injected for
 * {@code com.codename1.location.LocationButton}.
 *
 * <p>Four properties matter. {@code USE_LOCATION_BUTTON} is unconditional,
 * because an app that asks for the control needs the permission that draws it.
 * {@code onlyForLocationButton} is never inferred, because getting it wrong in
 * that direction silently breaks every persistent location call in the app.
 * The location permissions come out uncapped even when Bluetooth or Wi-Fi
 * already declared them with a {@code maxSdkVersion}, because a button wired
 * to a permission the manifest stops granting at API 30 draws perfectly and
 * returns nothing. And a build that asserts exclusivity while also using
 * background location is refused rather than shipped.</p>
 */
class LocationButtonManifestFragmentsTest {

    private static int count(String haystack, String needle) {
        int count = 0;
        int idx = haystack.indexOf(needle);
        while (idx >= 0) {
            count++;
            idx = haystack.indexOf(needle, idx + needle.length());
        }
        return count;
    }

    @Test
    void declaresTheButtonPermissionAndBothLocationPermissions() {
        String out = LocationButtonManifestFragments.inject("", false);
        assertTrue(out.contains("android.permission.USE_LOCATION_BUTTON"), out);
        assertTrue(out.contains("android.permission.ACCESS_FINE_LOCATION"), out);
        assertTrue(out.contains("android.permission.ACCESS_COARSE_LOCATION"),
                out);
    }

    @Test
    void onlyForLocationButtonIsNeverInferred() {
        String out = LocationButtonManifestFragments.inject("", false);
        assertFalse(out.contains("onlyForLocationButton"), out);
    }

    @Test
    void exclusiveFlagsFineLocationOnly() {
        String out = LocationButtonManifestFragments.inject("", true);
        int at = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        int close = out.indexOf('>', at);
        assertTrue(out.substring(at, close).contains(
                "android:usesPermissionFlags=\"onlyForLocationButton\""), out);
        // Coarse location is not restricted: the approximate grant is not what
        // the policy is about, and flagging it would refuse the app a
        // permission it is entitled to hold.
        int coarse = out.indexOf("android.permission.ACCESS_COARSE_LOCATION");
        int coarseClose = out.indexOf('>', coarse);
        assertFalse(out.substring(coarse, coarseClose)
                .contains("onlyForLocationButton"), out);
    }

    @Test
    void widensACappedFineLocationRatherThanAddingASecond() {
        // What BluetoothManifestFragments leaves behind for a scanning app.
        String bluetooth = "    <uses-permission android:name=\""
                + "android.permission.ACCESS_FINE_LOCATION\""
                + " android:maxSdkVersion=\"30\" />\n";
        String out = LocationButtonManifestFragments.inject(bluetooth, false);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                out);
        assertFalse(out.contains("android:maxSdkVersion=\""), out);
    }

    @Test
    void aUriThatMerelyStartsWithOursBindsNothing() throws Exception {
        // The backward read validated everything BEFORE the URI and nothing
        // after it, so indexOf matching a longer value was accepted:
        // ".../apk/res/android-fake" bound its prefix to the Android
        // namespace. An element could then put a real permission under a
        // prefix the merger reads as somebody else's, and a build that asks
        // for nothing of the sort is refused.
        File root = tempDir("cn1-lb-uri");
        String manifest = "<manifest xmlns:android=\"http://schemas.android"
                + ".com/apk/res/android\" xmlns:a=\"http://schemas.android"
                + ".com/apk/res/android-fake\">"
                + "<uses-permission android:name=\"android.permission."
                + "INTERNET\" a:name=\"android.permission."
                + "ACCESS_BACKGROUND_LOCATION\"/></manifest>";
        writeAar(new File(root, "lookalike.aar"), manifest);
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "a namespace that merely starts with ours is not ours");
    }

    @Test
    void aDollarNamedTopLevelClasssPersistentCallIsTheApplications()
            throws Exception {
        // The bytecode scan attributes by stripping a class name at the first
        // dollar, so an application's own legal top-level
        // com/codename1/location/LocationManager$999 is read as a framework
        // inner class and its setLocationListener call is dropped -- and
        // exclusive mode is then accepted over a request that really is being
        // made. This scan reads the metadata instead, so it gets it right, and
        // the builder now consumes that answer.
        File root = tempDir("cn1-lb-dollar-persistent");
        writePoolClass(new File(root,
                        "com/codename1/location/LocationManager$999.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("setLocationListener"),
                cpUtf8("(Lcom/codename1/location/LocationListener;)V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesPersistentLocation(),
                "a top-level class that merely has a dollar in its name is the "
                + "application's, and its tracking call is the application's");
    }

    @Test
    void anUppercasedClassSuffixIsNotBytecode() throws Exception {
        // A class loader asks a jar for com/example/Sample.class by that exact
        // name and gets nothing back for an entry called Sample.CLASS, so such
        // an entry is a resource nothing loads. Reading a button reference out
        // of one refused a build over code that never runs.
        File root = tempDir("cn1-lb-classcase");
        File jar = new File(root, "shouty.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Sample.CLASS"));
            zip.write(constantPoolEntry(
                    "com/codename1/location/LocationButton"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "Sample.CLASS is not loadable bytecode: " + jar);

        // The ordinary spelling in the same shape still is, so this test is
        // about the suffix rather than about the fixture.
        File live = tempDir("cn1-lb-classcase-live");
        File liveJar = new File(live, "quiet.jar");
        ZipOutputStream liveZip = new ZipOutputStream(
                new FileOutputStream(liveJar));
        try {
            liveZip.putNextEntry(new ZipEntry("com/example/Sample.class"));
            liveZip.write(constantPoolEntry(
                    "com/codename1/location/LocationButton"));
            liveZip.closeEntry();
        } finally {
            liveZip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(live)
                        .usesButton(),
                "the ordinary spelling is the classpath");
    }

    @Test
    void anUppercasedClassesJarIsNotTheClasspath() throws Exception {
        // ZIP names are case-sensitive and the Android layout recognises
        // classes.jar in exactly that spelling, so CLASSES.JAR is a resource
        // nothing loads -- and reading a button reference out of one refuses a
        // build over code that never runs, which is the same mistake as
        // scanning assets/sample.jar.
        File root = tempDir("cn1-lb-case");
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        ZipOutputStream innerZip = new ZipOutputStream(inner);
        try {
            innerZip.putNextEntry(new ZipEntry("com/example/AarForm.class"));
            innerZip.write(constantPoolEntry(
                    "com/codename1/location/LocationButton"));
            innerZip.closeEntry();
        } finally {
            innerZip.close();
        }
        File aar = new File(root, "shouty.aar");
        ZipOutputStream outerZip = new ZipOutputStream(
                new FileOutputStream(aar));
        try {
            outerZip.putNextEntry(new ZipEntry("CLASSES.JAR"));
            outerZip.write(inner.toByteArray());
            outerZip.closeEntry();
        } finally {
            outerZip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "CLASSES.JAR is not on the classpath, so what is in it is not "
                + "the application's use of the button");
    }

    @Test
    void aRemoveAllMarkerIsARemovalToo() {
        // removeAll is a removal to the merger. Reading it as an ordinary
        // declaration was worse than missing it: declareUncapped took the
        // marker for the active declaration it wanted, added no real
        // permission, and the merger then applied the removal -- so the button
        // shipped with the one permission it exists to obtain stripped out.
        String removeAll = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" tools:node=\"removeAll\""
                + " />\n";
        String out = LocationButtonManifestFragments.inject(removeAll, false);
        assertFalse(out.contains("removeAll"),
                "the marker has to go, not be mistaken for a request: " + out);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                "and a real declaration takes its place: " + out);
    }

    @Test
    void aSelectorScopedRemovalDoesNotClearTheAggregate() {
        // tools:selector restricts the removal to the one dependency it names,
        // so another submitted archive's declaration survives into the merged
        // manifest. Clearing an aggregate flag on that basis accepted exclusive
        // mode beside a permission that is still there.
        String scoped = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" tools:node=\""
                + "remove\" tools:selector=\"com.example.first\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesBackgroundLocation(scoped).isEmpty(),
                "a scoped removal is not a blanket one: " + scoped);
        // The unscoped form still clears it.
        String blanket = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" tools:node=\""
                + "remove\" />\n";
        assertTrue(LocationButtonManifestFragments
                .removesBackgroundLocation(blanket).contains(
                        "uses-permission"));
    }

    @Test
    void aCommentedRootIsNotTheRoot() throws Exception {
        // An aar may open with an example in a comment. Taking the first
        // textual <manifest let that example's namespaces decide how the real
        // root's attributes were read, which discards the real Android prefix
        // and loses the live declaration underneath it.
        File root = tempDir("cn1-lb-commentroot");
        String manifest = "<!-- like this: <manifest xmlns:android=\"urn:fake"
                + "\"> -->\n<manifest xmlns:android=\"http://schemas.android"
                + ".com/apk/res/android\"><uses-permission android:name=\""
                + "android.permission.ACCESS_BACKGROUND_LOCATION\"/>"
                + "</manifest>";
        writeAar(new File(root, "commentroot.aar"), manifest);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "the real root's namespaces are the ones in scope");
    }

    @Test
    void aPrefixTheRootReboundIsNotOursEither() throws Exception {
        // The rebinding lives where a manifest actually declares its
        // namespaces -- on the root -- and the element binds nothing itself.
        // Checking only the element left the conventional prefix unexamined,
        // so this decoy android:name was read as a real background-location
        // request and an exclusive build was refused over a permission the
        // merger never sees.
        File root = tempDir("cn1-lb-rootbind");
        String manifest = "<manifest xmlns:android=\"urn:fake\""
                + " xmlns:a=\"http://schemas.android.com/apk/res/android\">"
                + "<uses-permission android:name=\"android.permission."
                + "ACCESS_BACKGROUND_LOCATION\" a:name=\"android.permission."
                + "INTERNET\"/></manifest>";
        writeAar(new File(root, "rootbind.aar"), manifest);
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "a prefix the root bound elsewhere is not the Android "
                + "namespace, however conventional its spelling");
    }

    @Test
    void aRemovalOfOneElementTypeDoesNotCoverTheOther() throws Exception {
        // uses-permission and uses-permission-sdk-23 are distinct element types
        // to the merger, and a removal marker on one does not remove the other.
        // The archive's declaration therefore survives, and clearing the
        // aggregate on the mismatched removal accepted an exclusive build
        // beside a permission that is still in the merged manifest.
        File root = tempDir("cn1-lb-sdk23");
        writeAar(new File(root, "sdk23.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission-sdk-23 android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\"/></manifest>");
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(usage.declaresBackgroundLocation());
        assertTrue(usage.backgroundElements().contains(
                        "uses-permission-sdk-23"),
                "the element that declared it travels with the fact: "
                + usage.backgroundElements());

        // A removal of the PLAIN form does not cover it.
        String plain = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" tools:node=\""
                + "remove\" />\n";
        assertFalse(LocationButtonManifestFragments
                        .removesBackgroundLocation(plain)
                        .containsAll(usage.backgroundElements()),
                "a uses-permission removal does not remove a "
                + "uses-permission-sdk-23 declaration");

        // The matching form does.
        String matching = "    <uses-permission-sdk-23 android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" tools:node=\""
                + "remove\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesBackgroundLocation(matching)
                        .containsAll(usage.backgroundElements()),
                "and the matching one does");
    }

    @Test
    void anApplicationRemovalOfBackgroundLocationIsRecognised() {
        // tools:node="remove" in the project's own block outranks a library
        // that contributed the permission, and the merger honours it. The
        // builder consults this before folding an archive's declaration into
        // the exclusivity check, so a developer who removed the transitive
        // request is not refused the hint they now qualify for.
        String removal = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" tools:node=\""
                + "remove\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesBackgroundLocation(removal)
                        .contains("uses-permission"),
                "an active removal of background location is a removal");
        // And a plain declaration is not a removal, nor is a commented-out one
        // either kind.
        assertTrue(LocationButtonManifestFragments.removesBackgroundLocation(
                "    <uses-permission android:name=\"android.permission."
                + "ACCESS_BACKGROUND_LOCATION\" />\n").isEmpty(),
                "a plain declaration removes nothing");
        assertTrue(LocationButtonManifestFragments.removesBackgroundLocation(
                "    <!-- <uses-permission android:name=\"android.permission."
                + "ACCESS_BACKGROUND_LOCATION\" tools:node=\"remove\" /> -->"
                + "\n").isEmpty(),
                "a commented-out removal removes nothing");
        assertTrue(LocationButtonManifestFragments
                .removesBackgroundLocation(null).isEmpty());
    }

    @Test
    void anSdk23RemovalIsClosedWithItsOwnTag() {
        // declaresPermissionAt accepts any tag beginning "uses-permission",
        // which includes uses-permission-sdk-23. A hard-coded
        // </uses-permission> does not consume </uses-permission-sdk-23>, so
        // the opening tag was spliced out and its closing tag left behind --
        // and an orphan closing tag stops the manifest parsing at all.
        String sdk23 = "    <uses-permission-sdk-23 android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "</uses-permission-sdk-23>\n";
        String out = LocationButtonManifestFragments.inject(sdk23, false);
        assertFalse(out.contains("</uses-permission-sdk-23>"),
                "no closing tag may be left without its opening one: " + out);
        assertFalse(out.contains("tools:node"),
                "the removal itself is gone: " + out);
    }

    @Test
    void aRelocatedClassIsNotMistakenForTheFrameworks() throws Exception {
        // The loose-file path used to be normalised by searching the whole
        // filesystem path for com/codename1/ and truncating there, so an
        // application's own relocated class read as the framework's and was
        // skipped -- and it can hold the only reference to the real button,
        // which then leaves the bridge deleted under an app that uses it. A
        // build directory that merely contained those segments did the same to
        // everything beneath it.
        File root = tempDir("cn1-lb-relocated");
        // The path has to truncate onto a listed name for the old form to
        // skip it, which is exactly the shape a relocation produces: the
        // package is rewritten in front and the class keeps its name.
        writeClass(new File(root,
                        "org/acme/com/codename1/location/LocationButton.class"),
                "com/codename1/location/LocationButton");
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a class under a relocated package is the application's");
    }

    @Test
    void aPrefixTheElementReboundIsNotOurs() {
        // The element takes the conventional prefix for a namespace of its own
        // and carries the real Android one under an alias. Offering "android"
        // unconditionally read android:name as a real fine-location
        // declaration, so inject() added none -- and the button was left
        // without the permission it exists to obtain. A binding ON the element
        // is the innermost one in scope for its own attributes.
        String rebound = "    <uses-permission xmlns:android=\"urn:fake\""
                + " xmlns:a=\"http://schemas.android.com/apk/res/android\""
                + " android:name=\"android.permission.ACCESS_FINE_LOCATION\""
                + " a:name=\"android.permission.INTERNET\" />\n";
        String out = LocationButtonManifestFragments.inject(rebound, false);
        // COUNTED, not searched for. The element already contains that exact
        // text -- in urn:fake -- so "is it present" is true whether or not a
        // real declaration was added, and the first version of this test
        // passed with the bug in place.
        assertEquals(2, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                "a real fine-location declaration has to be added beside the "
                + "one in urn:fake: " + out);
    }

    @Test
    void anInsertedFlagUsesThePrefixThatNamedThePermission() {
        // The element rebinds the conventional prefix to something else and
        // carries the real Android namespace under an alias. Inserting
        // android:usesPermissionFlags there puts the flag in the rebound
        // namespace, where the merger never looks -- so the permission keeps
        // ordinary precise access while the build reports itself exclusive.
        String rebound = "    <uses-permission xmlns:android=\"urn:fake\""
                + " xmlns:a=\"http://schemas.android.com/apk/res/android\""
                + " a:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                rebound, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertTrue(out.contains("a:usesPermissionFlags=\""
                        + "onlyForLocationButton\""),
                "the flag goes under the prefix that named the permission: "
                + out);
        assertFalse(out.contains("android:usesPermissionFlags"),
                "and not under the rebound one: " + out);
    }

    @Test
    void aReplaceInstructionGoesWithTheCapItNamed() {
        // A project fighting a library's cap writes the pair: its own value,
        // and tools:replace saying "override theirs with mine". Taking the
        // value away and leaving the instruction is a replacement with nothing
        // to replace, which the merger refuses -- and removeCapAcrossMerge then
        // adds a tools:remove for the same attribute besides.
        String pair = "    <uses-permission android:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"32\""
                + " tools:replace=\"android:maxSdkVersion\" />\n";
        String out = LocationButtonManifestFragments.inject(pair, false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        assertFalse(element.contains("maxSdkVersion=\""),
                "the cap is gone: " + element);
        assertFalse(element.contains("replace"),
                "and so is the instruction that named it: " + element);
        assertTrue(element.contains(":remove=\"android:maxSdkVersion\""),
                "leaving the cross-merge marker alone: " + element);
    }

    @Test
    void aReplaceListKeepsTheEntriesThatAreNotTheCap() {
        // The value is a list, and an entry naming something else is somebody's
        // instruction about a different attribute.
        String pair = "    <uses-permission android:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"32\""
                + " tools:replace=\"android:maxSdkVersion,android:required\""
                + " />\n";
        String out = LocationButtonManifestFragments.inject(pair, false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        assertTrue(element.contains("tools:replace=\"android:required\""),
                "the other entry stays: " + element);
    }

    @Test
    void aJarCallingThePlatformLocationManagerWantsPreciseLocation()
            throws Exception {
        // A plain jar has no manifest to declare a permission with -- the
        // application supplies it -- so its bytecode is the only evidence that
        // the library wants precise location for itself.
        File root = tempDir("cn1-lb-platform");
        File jar = new File(root, "sdk.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Tracker.class"));
            zip.write(methodCallClass("android/location/LocationManager",
                    "requestLocationUpdates"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        LocationButtonManifestFragments.LocationUsage bytecode =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(bytecode.callsPreciseLocation(),
                "a platform location call is the library wanting precise "
                + "location of its own");
        // The CALL, not a declaration. They are separate facts because a
        // project's tools:node="remove" can discount a declaration and can
        // take nothing out of this bytecode.
        assertFalse(bytecode.declaresPreciseLocation(),
                "and it is a call, not a manifest declaration");
    }

    @Test
    void aFusedLocationClientIsAPreciseRequestToo() throws Exception {
        // The platform's own manager is not the only provider. Most Android
        // apps that ask for location ask Play services, whose client is an
        // ordinary class in an ordinary jar -- so a library using it names
        // nothing of Android's, and a plain jar has no manifest to declare a
        // permission with either. It was invisible, and exclusivity was
        // accepted over a request the library really makes.
        File root = tempDir("cn1-lb-fused");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "sdk.jar")));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Fused.class"));
            zip.write(methodCallClass(
                    "com/google/android/gms/location/"
                    + "FusedLocationProviderClient",
                    "getLastLocation"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        LocationButtonManifestFragments.LocationUsage fused =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(fused.callsPreciseLocation(),
                "a fused-location call is the library wanting precise "
                + "location of its own");
        assertFalse(fused.declaresPreciseLocation(),
                "and it is a call, not a manifest declaration");
    }

    @Test
    void aFactoryReachesTheClientWithoutNamingIt() throws Exception {
        // LocationServices.getFusedLocationProviderClient(this).lastLocation
        // is how the fused client is ordinarily reached, and it spells
        // FusedLocationProviderClient nowhere -- so the source scan skipped a
        // file that plainly performs a lookup, and exclusivity was accepted
        // over it.
        File root = tempDir("cn1-lb-factory");
        writeSource(new File(root, "com/example/Where.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location.LocationServices\n"
                + "class Where {\n"
                + "  fun last(c: Any) = LocationServices"
                + ".getFusedLocationProviderClient(c).lastLocation\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "the factory path reaches the same client");
    }

    @Test
    void namingTheFactoryWithoutAskingItForAnythingIsNotUse()
            throws Exception {
        // And the factory alone is not a request. A class that obtains a
        // client and only removes updates asks for no location, and refusing
        // its build would be refusing it over an import.
        File root = tempDir("cn1-lb-factory-only");
        writeSource(new File(root, "com/example/Stopper.java"),
                "package com.example;\n"
                + "import com.google.android.gms.location.LocationServices;\n"
                + "public class Stopper {\n"
                + "  void stop(Object c) {\n"
                + "    LocationServices.getFusedLocationProviderClient(c)"
                + ".removeLocationUpdates(null);\n"
                + "  }\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "obtaining a client is not asking it for a location");
    }

    @Test
    void aLibrarysOwnMethodOfThatNameIsNotAFusedCall() throws Exception {
        // Attributed by OWNER, like every other marker here. A library with a
        // method of its own called getLastLocation is not calling Play
        // services, and reading it that way refuses a build over a name.
        File root = tempDir("cn1-lb-fused-name");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "own.jar")));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Own.class"));
            zip.write(methodCallClass("com/example/Own", "getLastLocation"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .callsPreciseLocation(),
                "a method of the library's own name is not a fused call");
    }

    @Test
    void aCachedPlatformLookupCountsAsPreciseUseToo() throws Exception {
        // getLastKnownLocation returns what the permission allows, so under the
        // hint it comes back approximate and the library's answer quietly loses
        // its accuracy. Our own marker list has said so about the Codename One
        // equivalent from the start; the platform list disagreed with it.
        File root = tempDir("cn1-lb-lastknown");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "cache.jar")));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Cache.class"));
            zip.write(methodCallClass("android/location/LocationManager",
                    "getLastKnownLocation"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .callsPreciseLocation(),
                "a cached platform lookup is precise use as well");
    }

    @Test
    void anAppRemovalOfTheLibrarysFineLocationIsHonoured() throws Exception {
        // The developer took the library's request out of the merged manifest,
        // and inject() then declares fine location itself with
        // onlyForLocationButton -- so the library cannot obtain ordinary
        // precise location either way and nothing contradicts.
        File root = tempDir("cn1-lb-fineremoval");
        writeAar(new File(root, "native.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\"/></manifest>");
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(usage.preciseElements().contains("uses-permission"),
                "the declaring element travels with the fact: "
                + usage.preciseElements());

        String removal = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" tools:node=\"remove\""
                + " />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesPermission(removal,
                                LocationButtonManifestFragments.FINE_LOCATION)
                        .containsAll(usage.preciseElements()),
                "and an unscoped removal of the same type covers it");
    }

    @Test
    void aSubclassInsideALibraryIsStillLocationManager() throws Exception {
        // javac records the STATIC type of the receiver as a method's owner, so
        // a cn1lib that subclasses LocationManager and calls an inherited
        // setLocationListener through its own type names the SUBCLASS. The
        // loose application scan resolves that through declaresType; without
        // the same resolution here, the same library was seen one way in a
        // merged tree and another inside its own jar -- and exclusive mode was
        // accepted over a request it really makes.
        File root = tempDir("cn1-lb-subclass");
        // The subclass, declaring its superclass and calling the method on
        // ITSELF, which is what the compiler emits.
        writeSubclassCall(new File(root, "com/example/MyManager.class"),
                "com/example/MyManager",
                "com/codename1/location/LocationManager",
                "setLocationListener");
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesPersistentLocation(),
                "a call through a LocationManager subclass is persistent use");
    }

    @Test
    void aSubclassSuppliedByALibraryResolvesAgainstTheApplication()
            throws Exception {
        // The two halves in different ROOTS. The application calls an
        // inherited setLocationListener through a subclass a cn1lib supplies,
        // so the application tree holds the deferred owner and the library jar
        // holds the "extends LocationManager" that explains it. Each scan
        // resolved its own root alone, neither could answer, and exclusivity
        // was accepted over a lookup the application really makes.
        File appRoot = tempDir("cn1-lb-cross-app");
        writeClassBytes(new File(appRoot, "com/example/Caller.class"),
                methodCallClass("com/mylib/Manager", "setLocationListener"));
        File libRoot = tempDir("cn1-lb-cross-lib");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(libRoot, "mylib.jar")));
        try {
            zip.putNextEntry(new ZipEntry("com/mylib/Manager.class"));
            zip.write(subclassCallClass("com/mylib/Manager",
                    "com/codename1/location/LocationManager", "unrelated"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        LocationButtonManifestFragments.LocationUsage app =
                LocationButtonManifestFragments.scanForLocationUsage(appRoot);
        LocationButtonManifestFragments.LocationUsage lib =
                LocationButtonManifestFragments.scanForLocationUsage(libRoot);
        // The premise, checked rather than trusted: neither root can answer on
        // its own, which is exactly why they have to be pooled.
        assertFalse(app.usesPersistentLocation(),
                "sanity: the application tree has the call and not the "
                + "hierarchy");
        assertFalse(lib.usesPersistentLocation(),
                "sanity: the library has the hierarchy and not the call");
        assertTrue(LocationButtonManifestFragments.resolvesAcross(app, lib),
                "pooled, the call resolves to LocationManager");
    }

    @Test
    void unrelatedScansDoNotResolveAcross() throws Exception {
        // And pooling must not invent one. A library subclassing something
        // else entirely leaves the application's call unexplained, and
        // reporting persistent use there would refuse a build over a
        // hierarchy that does not reach LocationManager.
        File appRoot = tempDir("cn1-lb-cross-app-none");
        writeClassBytes(new File(appRoot, "com/example/Caller.class"),
                methodCallClass("com/mylib/Manager", "setLocationListener"));
        File libRoot = tempDir("cn1-lb-cross-lib-none");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(libRoot, "mylib.jar")));
        try {
            zip.putNextEntry(new ZipEntry("com/mylib/Manager.class"));
            zip.write(subclassCallClass("com/mylib/Manager",
                    "java/lang/Object", "unrelated"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.resolvesAcross(
                        LocationButtonManifestFragments
                                .scanForLocationUsage(appRoot),
                        LocationButtonManifestFragments
                                .scanForLocationUsage(libRoot)),
                "a hierarchy that does not reach LocationManager resolves "
                + "nothing");
    }

    @Test
    void aSubclassInOurNamespaceIsStillTheLibrarys() throws Exception {
        // The same library, having put its implementation in com.codename1.*,
        // which it is allowed to do -- a relocated copy lands there, and so
        // does a cn1lib that simply chose the package. A prefix test dropped
        // the owner before the hierarchy was consulted, so persistent stayed
        // false, android.locationButton.exclusive was accepted, and that
        // library's own tracking silently went approximate. Every other filter
        // here matches exact framework names for this reason.
        File root = tempDir("cn1-lb-subclass-ns");
        writeSubclassCall(new File(root,
                        "com/codename1/impl/mylib/MyManager.class"),
                "com/codename1/impl/mylib/MyManager",
                "com/codename1/location/LocationManager",
                "setLocationListener");
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesPersistentLocation(),
                "a library class in our namespace is still the library's");
    }

    @Test
    void aSubclassInOurNamespaceInsideAJarIsStillTheLibrarys()
            throws Exception {
        // And in the shape a consumed library actually arrives in. The scan
        // walks archives and loose trees through the same inspect(), so this
        // guards the pairing rather than the filter -- an archive entry that
        // stopped contributing to the hierarchy would leave the loose test
        // green and ship the same silent downgrade.
        File root = tempDir("cn1-lb-subclass-ns-jar");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "mylib.jar")));
        try {
            zip.putNextEntry(new ZipEntry(
                    "com/codename1/impl/mylib/MyManager.class"));
            zip.write(subclassCallClass("com/codename1/impl/mylib/MyManager",
                    "com/codename1/location/LocationManager",
                    "setLocationListener"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesPersistentLocation(),
                "a library jar's class in our namespace is still the "
                + "library's");
    }

    @Test
    void aLibraryCapItsRequestBeforeTheButtonIsNoConflict() throws Exception {
        // A legacy library asking for fine location up to API 30 -- the
        // ordinary shape for an old Bluetooth SDK -- stops asking long before
        // the API the location button exists on. It contradicts nothing there,
        // and refusing the hint over it refuses a build with no conflict in
        // it.
        File root = tempDir("cn1-lb-capped");
        File aar = new File(root, "legacy.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:android=\"http://schemas.android.com"
                    + "/apk/res/android\">\n"
                    + "  <uses-permission android:name=\"android.permission"
                    + ".ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"30\""
                    + " />\n"
                    + "</manifest>\n").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresPreciseLocation(),
                "a request capped below the button's API is no conflict");
    }

    @Test
    void theCapIsFoundUnderAnyAndroidAlias() throws Exception {
        // Two prefixes bound to the Android namespace on one element is legal,
        // and nothing says the cap has to travel under the same one that names
        // the permission. Reading only the naming prefix left the cap unseen
        // and refused a build over a permission that had already expired.
        File root = tempDir("cn1-lb-alias-cap");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "aliased.aar")));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:a=\"http://schemas.android.com"
                    + "/apk/res/android\" xmlns:b=\"http://schemas.android"
                    + ".com/apk/res/android\">\n"
                    + "  <uses-permission a:name=\"android.permission"
                    + ".ACCESS_FINE_LOCATION\" b:maxSdkVersion=\"30\" />\n"
                    + "</manifest>\n").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresPreciseLocation(),
                "the cap counts whichever Android alias carries it");
    }

    @Test
    void anExpiredBackgroundRequestIsNoConflictEither() throws Exception {
        // The same bound on the other permission. A library that asks for
        // background location up to API 30 asks for nothing where
        // onlyForLocationButton exists, so it cannot contradict the hint --
        // and only the fine-location path had been given the check.
        File root = tempDir("cn1-lb-capped-bg");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "legacy.aar")));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:android=\"http://schemas.android.com"
                    + "/apk/res/android\">\n"
                    + "  <uses-permission android:name=\"android.permission"
                    + ".ACCESS_BACKGROUND_LOCATION\""
                    + " android:maxSdkVersion=\"30\" />\n"
                    + "</manifest>\n").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "an expired background request is no conflict");
    }

    @Test
    void anUncappedBackgroundRequestStillConflicts() throws Exception {
        // And the direction that must not move: an ordinary background
        // request, with no cap at all, is exactly what exclusivity
        // contradicts.
        File root = tempDir("cn1-lb-live-bg");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "tracker.aar")));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:android=\"http://schemas.android.com"
                    + "/apk/res/android\">\n"
                    + "  <uses-permission android:name=\"android.permission"
                    + ".ACCESS_BACKGROUND_LOCATION\" />\n"
                    + "</manifest>\n").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "a live background request is still a conflict");
    }

    @Test
    void aLibraryCapAtTheButtonsApiStillConflicts() throws Exception {
        // The boundary, in the direction that matters. maxSdkVersion is
        // INCLUSIVE, so a cap at the button's own API is still a live request
        // there -- reading it as spent would accept exclusivity over a
        // permission the merged manifest really asks for.
        File root = tempDir("cn1-lb-capped-at");
        ZipOutputStream zip = new ZipOutputStream(
                new FileOutputStream(new File(root, "current.aar")));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:android=\"http://schemas.android.com"
                    + "/apk/res/android\">\n"
                    + "  <uses-permission android:name=\"android.permission"
                    + ".ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"37\""
                    + " />\n"
                    + "</manifest>\n").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresPreciseLocation(),
                "a cap at the button's API is still a request there");
    }

    @Test
    void aRemovalCannotDiscountTheLibrarysOwnCall() throws Exception {
        // An aar that BOTH declares fine location and calls a provider. The
        // project may take the declaration out of the merged manifest with
        // tools:node="remove", and the builder discounts a declaration it
        // removed -- but the removal takes nothing out of the library's
        // bytecode, which still runs and still asks for a location.
        //
        // Both facts were one boolean, so the removal discounted the call as
        // well: exclusivity was accepted, inject() restored fine location with
        // onlyForLocationButton, and the library's lookup came back
        // approximate with nothing in the build to say so. They are separate
        // now, and this holds them apart.
        File root = tempDir("cn1-lb-removal-call");
        File aar = new File(root, "both.aar");
        aar.getParentFile().mkdirs();
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest xmlns:android=\"http://schemas.android.com"
                    + "/apk/res/android\"><uses-permission android:name="
                    + "\"android.permission.ACCESS_FINE_LOCATION\"/>"
                    + "</manifest>").getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("classes.jar"));
            ByteArrayOutputStream inner = new ByteArrayOutputStream();
            ZipOutputStream innerZip = new ZipOutputStream(inner);
            try {
                innerZip.putNextEntry(new ZipEntry("com/example/Sdk.class"));
                innerZip.write(methodCallClass(
                        "android/location/LocationManager",
                        "requestLocationUpdates"));
                innerZip.closeEntry();
            } finally {
                innerZip.close();
            }
            zip.write(inner.toByteArray());
            zip.closeEntry();
        } finally {
            zip.close();
        }
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(usage.declaresPreciseLocation(),
                "the manifest declaration is seen");
        assertTrue(usage.callsPreciseLocation(),
                "and so is the call, as a fact of its own");
        assertFalse(usage.preciseElements().isEmpty(),
                "the declaration carries its element type, so a removal can "
                + "match it -- which is what used to discount the call too");
    }

    @Test
    void aLibraryThatRestrictsItselfToTheButtonIsNoConflict()
            throws Exception {
        // usesPermissionFlags="onlyForLocationButton" is the exact restriction
        // the hint asserts, so a library that has already written it is
        // agreeing rather than contradicting. Refusing the hint over it
        // refused a build on the strength of a declaration saying the same
        // thing the hint does.
        File root = tempDir("cn1-lb-flagged");
        writeAar(new File(root, "polite.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" android:"
                + "usesPermissionFlags=\"onlyForLocationButton\"/></manifest>");
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresPreciseLocation(),
                "a declaration already restricted to the button is no "
                + "conflict");
    }

    @Test
    void theButtonFlagDoesNotExcuseBackgroundLocation() throws Exception {
        // The scoping, which nothing else can observe. onlyForLocationButton
        // restricts FINE location to the button; it says nothing about
        // whether a library tracks in the background, and Android gives it no
        // meaning on ACCESS_BACKGROUND_LOCATION at all. Letting it excuse a
        // background request -- which a copied manifest can easily carry --
        // would accept exclusivity beside the one use it most clearly
        // contradicts.
        File root = tempDir("cn1-lb-flag-background");
        writeAar(new File(root, "confused.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\" android:"
                + "usesPermissionFlags=\"onlyForLocationButton\"/></manifest>");
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "the button flag cannot restrict background location");
    }

    @Test
    void anUnflaggedRequestBesideAFlaggedOneStillConflicts() throws Exception {
        // And one library agreeing does not excuse another. Two aars, one
        // restricted to the button and one asking for ordinary precise
        // location: the second is exactly what exclusivity contradicts, and
        // reading the pair as settled would downgrade it in silence.
        File root = tempDir("cn1-lb-flagged-mixed");
        writeAar(new File(root, "polite.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" android:"
                + "usesPermissionFlags=\"onlyForLocationButton\"/></manifest>");
        writeAar(new File(root, "greedy.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\"/></manifest>");
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresPreciseLocation(),
                "the unrestricted request still conflicts");
    }

    @Test
    void theProjectsOwnExpiredBackgroundRequestIsNoConflict() {
        // The bound the contributed manifests already had. I scoped it to the
        // LIBRARY read when I added it and wrote that the project's own block
        // was left alone; there was no reason for that. A permission capped
        // below the button's API asks for nothing where onlyForLocationButton
        // means anything, whoever declared it.
        String block = "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" android:maxSdkVersion=\"30\""
                + " />\n";
        assertFalse(LocationButtonManifestFragments
                        .declaresLiveBackgroundLocation(block),
                "an expired background request cannot conflict");
        assertTrue(LocationButtonManifestFragments
                        .declaresBackgroundLocation(block),
                "and the cap-blind reading still sees it, which is what made "
                + "the two paths disagree");
    }

    @Test
    void theProjectsOwnLiveBackgroundRequestStillConflicts() {
        // The direction that must not move: an uncapped request from the
        // project itself is exactly what exclusivity contradicts.
        String block = "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .declaresLiveBackgroundLocation(block),
                "a live background request is still a conflict");
    }

    @Test
    void anUnkeyedRemoveAllCoversALibrarysDeclaration() {
        // <uses-permission tools:node="removeAll" /> carries no permission
        // name, so the keyed walk never reaches it -- and it takes every
        // lower-priority uses-permission with it, so the library's
        // declaration does not survive the merge at all.
        String block = "    <uses-permission tools:node=\"removeAll\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesPermission(block,
                                LocationButtonManifestFragments.FINE_LOCATION)
                        .contains("uses-permission"),
                "an unkeyed removeAll clears the element type");
    }

    @Test
    void anUnkeyedRemoveAllDoesNotReachTheOtherElementType() {
        // uses-permission and uses-permission-sdk-23 are different elements to
        // the merger, so clearing one leaves the other's declarations in
        // place. Reading it as covering both would accept exclusivity over a
        // request that really does survive.
        String block = "    <uses-permission tools:node=\"removeAll\" />\n";
        assertFalse(LocationButtonManifestFragments
                        .removesPermission(block,
                                LocationButtonManifestFragments.FINE_LOCATION)
                        .contains("uses-permission-sdk-23"),
                "removeAll on one element type is not removeAll on the other");
    }

    @Test
    void aRemoveAllOfAnotherPermissionRemovesNothingOfOurs() {
        // The guard that keeps the unkeyed walk honest, and the one direction
        // here that fails dangerously. A removeAll KEYED to another permission
        // clears that permission and nothing else -- reading it as a blanket
        // removal would report a library's fine-location request as gone,
        // accept exclusivity over it, and downgrade a request that really does
        // survive the merge.
        String block = "    <uses-permission android:name=\"android.permission"
                + ".CAMERA\" tools:node=\"removeAll\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesPermission(block,
                                LocationButtonManifestFragments.FINE_LOCATION)
                        .isEmpty(),
                "removing another permission removes nothing of ours");
    }

    @Test
    void aSelectorScopedRemoveAllIsNotABlanketRemoval() {
        // tools:selector scopes the marker to ONE library, so the rest of them
        // keep their declarations. Counting it as a blanket removal would
        // accept the hint over a request nothing took out of the manifest.
        String block = "    <uses-permission tools:node=\"removeAll\""
                + " tools:selector=\"com.example.other\" />\n";
        assertTrue(LocationButtonManifestFragments
                        .removesPermission(block,
                                LocationButtonManifestFragments.FINE_LOCATION)
                        .isEmpty(),
                "a scoped removeAll removes nothing globally");
    }

    @Test
    void aLibrarysOwnPreciseRequestConflictsWithExclusivity() throws Exception {
        // A native SDK inside an aar calls android.location.LocationManager
        // directly, so no marker of ours ever names it -- its manifest asking
        // for ACCESS_FINE_LOCATION is the only evidence there is. Accepting
        // exclusivity over that adds onlyForLocationButton and the library's
        // own precise-location flow is answered with an approximation.
        File root = tempDir("cn1-lb-native");
        writeAar(new File(root, "native.aar"),
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/"
                + "android\"><uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\"/></manifest>");
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(usage.declaresPreciseLocation(),
                "the library's own request is seen");
        assertFalse(usage.declaresBackgroundLocation(),
                "and it is not background location");

        String conflict = LocationButtonManifestFragments.exclusiveConflict(
                true, false, false, true);
        assertNotNull(conflict, "which conflicts with exclusivity");
        assertTrue(conflict.contains("submitted library"),
                "and the message says which of the three it was: " + conflict);
    }

    @Test
    void anArchiveTheBuildWouldNotAddIsNotScanned() throws Exception {
        // The aar loop asks getName().endsWith(".aar") and the gradle include
        // is '*.jar', both case-sensitive, and neither consumes a .zip.
        File root = tempDir("cn1-lb-ext");
        for (String named : new String[] {"sample.JAR", "sample.AAR",
                "sample.zip"}) {
            ByteArrayOutputStream inner = new ByteArrayOutputStream();
            ZipOutputStream innerZip = new ZipOutputStream(inner);
            try {
                innerZip.putNextEntry(new ZipEntry("com/example/Form.class"));
                innerZip.write(constantPoolEntry(
                        "com/codename1/location/LocationButton"));
                innerZip.closeEntry();
            } finally {
                innerZip.close();
            }
            ZipOutputStream zip = new ZipOutputStream(
                    new FileOutputStream(new File(root, named)));
            try {
                zip.putNextEntry(new ZipEntry("classes.jar"));
                zip.write(inner.toByteArray());
                zip.closeEntry();
            } finally {
                zip.close();
            }
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "none of these spellings is added to the build");
    }

    @Test
    void anArchiveBelowTheTopLevelIsNotADependency() throws Exception {
        // The dependency loop reads libsDir.listFiles() and the generated
        // gradle includes fileTree(dir: 'libs', include: ['*.jar']) -- neither
        // descends. An archive submitted at vendor/lib.aar is staged, never
        // added, and never runs, so a reference in it is not the application's.
        File root = tempDir("cn1-lb-nested");
        File nested = new File(root, "vendor");
        nested.mkdirs();
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        ZipOutputStream innerZip = new ZipOutputStream(inner);
        try {
            innerZip.putNextEntry(new ZipEntry("com/example/AarForm.class"));
            innerZip.write(constantPoolEntry(
                    "com/codename1/location/LocationButton"));
            innerZip.closeEntry();
        } finally {
            innerZip.close();
        }
        ZipOutputStream outerZip = new ZipOutputStream(
                new FileOutputStream(new File(nested, "sample.aar")));
        try {
            outerZip.putNextEntry(new ZipEntry("classes.jar"));
            outerZip.write(inner.toByteArray());
            outerZip.closeEntry();
        } finally {
            outerZip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "an archive the build never adds is not the app's use of it");

        // The same archive at the top level IS a dependency, so this is about
        // where it sits and not about the fixture.
        File top = tempDir("cn1-lb-nested-top");
        ZipOutputStream topZip = new ZipOutputStream(
                new FileOutputStream(new File(top, "sample.aar")));
        try {
            topZip.putNextEntry(new ZipEntry("classes.jar"));
            topZip.write(inner.toByteArray());
            topZip.closeEntry();
        } finally {
            topZip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(top)
                        .usesButton(),
                "a top-level archive is a dependency");
    }

    @Test
    void anArrayOfTheButtonIsAReferenceToIt() throws Exception {
        // Foo[].class puts "[Lcom/example/Foo;" in a CONSTANT_Class rather than
        // the bare name, and an array of the button is a reference to it: a
        // factory holding the array type can ask it for its component type and
        // build one. Missing it deletes the bridge.
        File root = tempDir("cn1-lb-array");
        writePoolClass(new File(root, "com/example/Uses.class"),
                cpUtf8("[Lcom/codename1/location/LocationButton;"),
                cpOne(7, 1));
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "an array class literal names the button");
    }

    @Test
    void aToolsPrefixIsBoundWhenTheElementTookTheConventionalOne() {
        // The element rebinds tools to something of its own and offers no
        // alias, so there is NO prefix here for the real tools namespace.
        // Falling back to the conventional spelling writes the marker into the
        // rebound namespace, where the merger never looks -- so a prefix has to
        // be bound rather than assumed.
        String rebound = "    <uses-permission xmlns:tools=\"urn:fake\""
                + " android:name=\"android.permission.ACCESS_FINE_LOCATION\""
                + " />\n";
        String out = LocationButtonManifestFragments.inject(rebound, false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        // With the leading space, so this is the ATTRIBUTE and not a suffix
        // of another prefix: "cn1tools:remove=" contains "tools:remove=", and
        // the first version of this assertion failed on the correct output.
        assertFalse(element.contains(" tools:remove="),
                "not under the prefix the element took: " + element);
        assertTrue(element.contains(
                        "=\"http://schemas.android.com/tools\""),
                "a prefix for the real tools namespace is bound: " + element);
        assertTrue(element.contains(":remove=\"android:maxSdkVersion\""),
                "and the marker uses it: " + element);
    }

    @Test
    void aSiblingsAliasIsNotInScopeOnThisElement() {
        // A sibling binds an alias for the tools namespace and the fine
        // element rebinds the conventional prefix, so the document contains a
        // usable-looking prefix that is not in scope HERE. candidatePrefixes
        // collects document-wide on purpose -- the readers want every spelling
        // -- and writing under that alias emitted a:remove with nothing
        // binding a, which is a manifest no XML parser accepts. A fragment is
        // where it bites, because there is no root element to fall back on.
        String fragment = "    <uses-permission"
                + " xmlns:a=\"http://schemas.android.com/tools\""
                + " android:name=\"android.permission.CAMERA\" />\n"
                + "    <uses-permission xmlns:tools=\"urn:fake\""
                + " android:name=\"android.permission.ACCESS_FINE_LOCATION\""
                + " />\n";
        String out = LocationButtonManifestFragments.inject(fragment, false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        int marker = element.indexOf(":remove=\"android:maxSdkVersion\"");
        assertTrue(marker > 0, "the cap is removed at all: " + element);
        // Read the prefix back off the output and demand THAT one is bound
        // here, rather than naming the prefix this is expected to choose.
        int from = marker;
        while (from > 0 && element.charAt(from - 1) != ' ') {
            from--;
        }
        String prefix = element.substring(from, marker);
        assertTrue(element.contains(" xmlns:" + prefix
                        + "=\"http://schemas.android.com/tools\""),
                "the prefix it wrote is bound on this element: " + element);
    }

    @Test
    void theRemoveMarkerNamesTheAttributeInTheRightNamespace() {
        // The value of tools:remove is a QName. On an element that rebound the
        // conventional prefix and carries the real Android namespace under an
        // alias, a literal "android:maxSdkVersion" names an attribute in
        // somebody else's namespace -- so a library's real cap merges in
        // untouched and the button loses fine location above it.
        String rebound = "    <uses-permission xmlns:android=\"urn:fake\""
                + " xmlns:a=\"http://schemas.android.com/apk/res/android\""
                + " a:name=\"android.permission.ACCESS_FINE_LOCATION\" />\n";
        String out = LocationButtonManifestFragments.inject(rebound, false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        assertTrue(element.contains("tools:remove=\"a:maxSdkVersion\""),
                "the marker names the attribute under the prefix that named "
                + "the permission: " + element);
    }

    @Test
    void theButtonsOwnPermissionIsUncappedToo() {
        // A capped USE_LOCATION_BUTTON is the one permission whose absence
        // makes the control itself unavailable, on the very platform that
        // introduced it -- and a plain add suppresses the duplicate and keeps
        // the cap.
        String capped = "    <uses-permission android:name=\"android."
                + "permission.USE_LOCATION_BUTTON\" android:maxSdkVersion=\""
                + "36\" />\n";
        String out = LocationButtonManifestFragments.inject(capped, false);
        int at = out.indexOf("android.permission.USE_LOCATION_BUTTON");
        String element = out.substring(out.lastIndexOf('<', at),
                out.indexOf('>', at));
        assertFalse(element.contains("maxSdkVersion=\""),
                "the cap is gone: " + element);
        assertTrue(element.contains("tools:remove=\"android:maxSdkVersion\""),
                "and a library's cap cannot merge in either: " + element);
        assertEquals(1, count(out, "android.permission.USE_LOCATION_BUTTON"),
                "declared once: " + out);
    }

    @Test
    void theCapIsRemovedAcrossTheMergeToo() {
        // declareUncapped settles what is in THIS block. The merger takes the
        // union of an element's attributes, so a library that declares the
        // same permission with its own maxSdkVersion has it merged INTO ours
        // -- and the button ships against a permission the manifest stops
        // granting. tools:remove is the merger's own answer, and it reaches
        // caps this scan never sees at all.
        String out = LocationButtonManifestFragments.inject("", false);
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        int close = out.indexOf('>', fine);
        assertTrue(out.substring(fine, close).contains(
                        "tools:remove=\"android:maxSdkVersion\""),
                "the fine-location declaration carries the instruction: "
                + out);
        int coarse = out.indexOf("android.permission.ACCESS_COARSE_LOCATION");
        int coarseClose = out.indexOf('>', coarse);
        assertTrue(out.substring(coarse, coarseClose).contains(
                        "tools:remove=\"android:maxSdkVersion\""),
                "and so does coarse, which is granted alongside it: " + out);
    }

    @Test
    void anExistingToolsRemoveIsAddedToRatherThanDuplicated() {
        // tools:remove is a comma-separated list, and two of them on one
        // element is not a thing a manifest may contain.
        String existing = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" tools:remove=\""
                + "android:usesPermissionFlags\" />\n";
        String out = LocationButtonManifestFragments.inject(existing, false);
        // Within the FINE element: coarse gets an instruction of its own, so a
        // count over the whole block would be two for a correct result.
        int fine = out.indexOf("android.permission.ACCESS_FINE_LOCATION");
        String element = out.substring(out.lastIndexOf('<', fine),
                out.indexOf('>', fine));
        assertEquals(1, count(element, "tools:remove"),
                "one list, not two: " + element);
        assertTrue(element.contains("android:usesPermissionFlags,"
                        + "android:maxSdkVersion"),
                "and the existing entry is kept: " + element);
    }

    @Test
    void everySpellingOfTheCapIsStripped() {
        // A decoy cap under the conventional prefix, and the real one under
        // another prefix bound to the Android namespace. Stripping the first
        // match removed the decoy and left the real cap in place, which is the
        // silent loss of fine location above API 30 that widening exists to
        // prevent.
        String decoyed = "    <uses-permission xmlns:a=\"http://schemas."
                + "android.com/apk/res/android\" a:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" android:maxSdkVersion=\"99\""
                + " a:maxSdkVersion=\"30\" />\n";
        String out = LocationButtonManifestFragments.inject(decoyed, false);
        assertFalse(out.contains("maxSdkVersion=\""),
                "no spelling of the cap may survive: " + out);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                out);
    }

    @Test
    void everySpellingOfTheFlagIsMergedInto() {
        // Same shape for the flag being added: writing it into a decoy while
        // the attribute the merger reads goes without it gives away the
        // permission the hint exists to restrict.
        String decoyed = "    <uses-permission xmlns:a=\"http://schemas."
                + "android.com/apk/res/android\" a:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" android:usesPermissionFlags=\""
                + "neverForLocation\" a:usesPermissionFlags=\""
                + "neverForLocation\" />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                decoyed, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertEquals(2, count(out, "neverForLocation|onlyForLocationButton"),
                "both spellings carry the flag, so whichever one is really "
                + "the Android namespace has it: " + out);
    }

    @Test
    void aDecoyUnderAReboundPrefixHidesNothing() throws Exception {
        // Bindings are collected document-wide rather than resolved in the
        // element's scope, so a manifest can rebind the conventional prefix on
        // one element. If the check acted on the FIRST spelling that carries a
        // name attribute, a decoy under that prefix would answer for the
        // element and the real permission under a second prefix would never be
        // examined -- and the exclusive build would be accepted over an aar
        // that does contribute background location.
        File root = tempDir("cn1-lb-decoy");
        String manifest = "<manifest xmlns:android=\"http://schemas.android"
                + ".com/apk/res/android\" xmlns:b=\"http://schemas.android"
                + ".com/apk/res/android\">"
                + "<uses-permission xmlns:android=\"http://example.com/not"
                + "-android\" android:name=\"android.permission.INTERNET\""
                + " b:name=\"android.permission.ACCESS_BACKGROUND_LOCATION\""
                + "/></manifest>";
        writeAar(new File(root, "decoy.aar"), manifest);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "the real permission must be seen even when another spelling "
                + "on the same element answers first");
    }

    @Test
    void anAnnotationClassValueIsUseOfTheButton() throws Exception {
        // @Widget(LocationButton.class) creates NO CONSTANT_Class: javac puts
        // the field descriptor in a Utf8 and points the annotation's
        // element_value at it. A scan that reads only the class table sees an
        // application that never mentions the button, and the bridge is deleted
        // under a control it really does build.
        //
        // Built with ASM's own annotation visitor rather than by hand, so the
        // bytes are the shape a compiler emits and not the shape I assumed.
        File root = tempDir("cn1-lb-annotation");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Annotated",
                null, "java/lang/Object", null);
        AnnotationVisitor a = w.visitAnnotation("Lcom/example/Widget;", true);
        a.visit("value",
                Type.getObjectType("com/codename1/location/LocationButton"));
        a.visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();

        // The premise, checked rather than trusted: no CONSTANT_Class for the
        // button anywhere in this file. If ASM ever started emitting one the
        // test would still pass while testing nothing.
        assertFalse(new String(bytes, StandardCharsets.ISO_8859_1)
                        .contains("com/codename1/location/LocationButton\u0000"),
                "sanity: the annotated form is a descriptor, not a bare name");

        File at = new File(root, "com/example/Annotated.class");
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a class referenced only from an annotation is still used");
    }

    @Test
    void nativeAndroidSourcesAskingForLocationAreUse() throws Exception {
        // A native interface implemented in Android .java or .kt is compiled
        // by Gradle out of the staged sources: it is in no jar and no class
        // tree, so neither bytecode scan can see it. Calling the platform's
        // location manager there needs precise location as surely as calling
        // ours, and accepting android.locationButton.exclusive over it
        // downgrades those requests to approximate results silently.
        File root = tempDir("cn1-lb-native-src");
        writeSource(new File(root, "com/example/Tracker.java"),
                "package com.example;\n"
                + "import android.location.LocationManager;\n"
                + "public class Tracker {\n"
                + "  void go(LocationManager m) {\n"
                + "    m.requestLocationUpdates(null, 0, 0, null);\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "native sources calling the platform manager are location use");
    }

    @Test
    void kotlinNativeSourcesCountTheSameWay() throws Exception {
        // Android native interfaces may be written in Kotlin, and a scan that
        // read only .java would miss exactly the same call.
        File root = tempDir("cn1-lb-native-kt");
        writeSource(new File(root, "com/example/Tracker.kt"),
                "package com.example\n"
                + "import android.location.LocationManager\n"
                + "class Tracker {\n"
                + "  fun last(m: LocationManager) = "
                + "m.getLastKnownLocation(\"gps\")\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "Kotlin native sources are read too");
    }

    @Test
    void nativeSourcesUsingTheFusedClientCountToo() throws Exception {
        // Same provider, same reasoning, in the half no bytecode scan reaches.
        File root = tempDir("cn1-lb-native-fused");
        writeSource(new File(root, "com/example/Fused.java"),
                "package com.example;\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient;\n"
                + "public class Fused {\n"
                + "  void go(FusedLocationProviderClient c) {\n"
                + "    c.getLastLocation();\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "native sources using the fused client are location use");
    }

    @Test
    void aWildcardImportNamesTheProviderToo() throws Exception {
        // import android.location.* is ordinary Java and ordinary Kotlin, and
        // it leaves the qualified name nowhere in the file. Requiring it read
        // a native implementation calling LocationManager.requestLocationUpdates
        // as naming nothing at all.
        File root = tempDir("cn1-lb-wildcard");
        writeSource(new File(root, "com/example/Wild.java"),
                "package com.example;\n"
                + "import android.location.*;\n"
                + "public class Wild {\n"
                + "  void go(LocationManager m) {\n"
                + "    m.requestLocationUpdates(null, 0, 0, null);\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a wildcard import still names the provider");
    }

    @Test
    void aWildcardImportOfSomethingElseIsNotTheProvider() throws Exception {
        // The narrow half of the wildcard rule. Importing the package for
        // Location or Criteria is not importing LocationManager, so the simple
        // name has to be there as well -- a build is refused on this answer.
        File root = tempDir("cn1-lb-wildcard-other");
        writeSource(new File(root, "com/example/Other.java"),
                "package com.example;\n"
                + "import android.location.*;\n"
                + "public class Other {\n"
                + "  Location last;\n"
                + "  void getCurrentLocation() { }\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "importing the package is not naming the manager");
    }

    @Test
    void aCallDescribedInACommentIsNotACall() throws Exception {
        // A developer who documents deleting the thing had the build refused
        // for saying so: the line carries the provider AND the marker and
        // compiles to nothing at all. Both line and block comments, since a
        // removed feature is as often commented out in bulk.
        File root = tempDir("cn1-lb-commented");
        writeSource(new File(root, "com/example/Gone.java"),
                "package com.example;\n"
                + "public class Gone {\n"
                + "  // android.location.LocationManager"
                + ".requestLocationUpdates was removed\n"
                + "  /* and android.location.LocationManager"
                + ".getLastKnownLocation with it */\n"
                + "  Object nothing;\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "prose about a call is not a call");
    }

    @Test
    void aDiagnosticStringNamingTheApiIsNotACall() throws Exception {
        // Consuming a literal is not the same as reading it. It has to be
        // consumed, so the // in "http://host" cannot eat the line -- but its
        // TEXT is not code, and a log line naming the provider and one of its
        // methods was read as a call and refused the build.
        File root = tempDir("cn1-lb-literal");
        writeSource(new File(root, "com/example/Logs.java"),
                "package com.example;\n"
                + "public class Logs {\n"
                + "  static final String NOTE = \"android.location"
                + ".LocationManager.requestLocationUpdates is not used\";\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a string naming the API is not a call to it");
    }

    @Test
    void aKotlinTemplateInsideAStringIsStillCode() throws Exception {
        // And the limit of that masking. What sits inside ${...} is compiled,
        // so "at ${client.lastLocation}" really does ask for a location --
        // blanking it would lose a real request in the direction that
        // downgrades one in silence.
        File root = tempDir("cn1-lb-template");
        writeSource(new File(root, "com/example/Log.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class Log(val client: FusedLocationProviderClient) {\n"
                + "  fun note() = \"at ${client.lastLocation}\"\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a template is an expression, not text");
    }

    @Test
    void aSlashInAStringDoesNotHideTheCallAfterIt() throws Exception {
        // The half that makes stripping comments safe. "http://host" holds a
        // // that begins no comment, and treating it as one eats the rest of
        // the line -- taking a real call with it and accepting exclusivity
        // over a request that is really made.
        File root = tempDir("cn1-lb-url");
        writeSource(new File(root, "com/example/Mixed.java"),
                "package com.example;\n"
                + "import android.location.LocationManager;\n"
                + "public class Mixed {\n"
                + "  void go(LocationManager m) {\n"
                + "    String url = \"http://host/x\"; "
                + "m.requestLocationUpdates(null, 0, 0, null);\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a slash inside a string literal begins no comment");
    }

    @Test
    void aKotlinPropertyIsTheGetterCall() throws Exception {
        // Kotlin reaches a no-argument Java getter as a property, so
        // client.lastLocation compiles to getLastLocation() and the getter's
        // name appears nowhere in the file. That is how the call is usually
        // written, so requiring the Java spelling missed the ordinary case and
        // accepted exclusivity over a real request.
        File root = tempDir("cn1-lb-kt-property");
        writeSource(new File(root, "com/example/Fused.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class Fused(val client: FusedLocationProviderClient) {\n"
                + "  fun last() = client.lastLocation\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a Kotlin property call is still the getter");
    }

    @Test
    void aJavaFileIsNotReadForKotlinProperties() throws Exception {
        // And only for Kotlin. In Java "lastLocation" is a field name and
        // nothing more, and reading it as a call would refuse a build over an
        // identifier the language gives no such meaning.
        File root = tempDir("cn1-lb-java-property");
        writeSource(new File(root, "com/example/Holder.java"),
                "package com.example;\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient;\n"
                + "public class Holder {\n"
                + "  FusedLocationProviderClient client;\n"
                + "  Object lastLocation;\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a Java field of that name is not a call");
    }

    @Test
    void nativeSourcesCallingOurOwnManagerCountToo() throws Exception {
        // An Android native implementation can call Codename One's
        // LocationManager as readily as the platform's -- it is on the
        // classpath the generated project compiles against -- and that source
        // is compiled by Gradle, so it reaches neither bytecode scan. The
        // source scan covered the platform providers and not ours.
        File root = tempDir("cn1-lb-native-ours");
        writeSource(new File(root, "com/example/Native.java"),
                "package com.example;\n"
                + "import com.codename1.location.LocationManager;\n"
                + "public class Native {\n"
                + "  void go(LocationManager m) {\n"
                + "    m.setLocationListener(null);\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "our own manager is a provider too");
    }

    @Test
    void theSourceOwnersCoverEveryPersistentMarker() throws Exception {
        // The list is DERIVED from PERSISTENT_MARKERS rather than written out
        // again, and this is what makes that worth doing: a marker added there
        // is covered here without anybody remembering to, and a future edit
        // that hard-codes the list fails this instead of going quiet.
        for (String marker : LocationButtonManifestFragments
                .persistentMarkers()) {
            File root = tempDir("cn1-lb-marker-" + marker);
            writeSource(new File(root, "com/example/Each.java"),
                    "package com.example;\n"
                    + "import com.codename1.location.LocationManager;\n"
                    + "public class Each {\n"
                    + "  void go(LocationManager m) {\n"
                    + "    m." + marker + "(null);\n"
                    + "  }\n"
                    + "}\n");
            assertTrue(LocationButtonManifestFragments
                            .sourcesCallPlatformLocation(root),
                    marker + " must be seen in a native source");
        }
    }

    @Test
    void aTemplateWithBracesOfItsOwnIsStillCode() throws Exception {
        // A template may hold braces. Taking the first '}' as its end masked
        // the rest of the expression -- including the call -- and lost a real
        // lookup, which is the direction that downgrades a request in
        // silence.
        File root = tempDir("cn1-lb-nested-template");
        writeSource(new File(root, "com/example/Pick.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class Pick(val client: FusedLocationProviderClient,\n"
                + "           val ok: Boolean, val cached: Any) {\n"
                + "  fun note() = \"at ${if (ok) { cached } else "
                + "{ client.lastLocation }}\"\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "the whole template is code, braces and all");
    }

    @Test
    void aStringNestedInsideATemplateDoesNotHideTheCall() throws Exception {
        // The case that ended the attempt to lex templates. A nested string
        // may hold a quote or a brace -- "${if (ok) "}" else client
        // .lastLocation}" holds both -- and every partial lexer got it wrong
        // in the same direction, masking the real lookup. A literal carrying
        // a template is copied through whole now, so no amount of nesting
        // inside one can hide what it contains.
        File root = tempDir("cn1-lb-nested-string");
        writeSource(new File(root, "com/example/Odd.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class Odd(val client: FusedLocationProviderClient,\n"
                + "          val ok: Boolean) {\n"
                + "  fun note() = \"at ${if (ok) \\\"}\\\" else "
                + "client.lastLocation}\"\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "a nested string cannot hide the call after it");
    }

    @Test
    void theSourceScanStaysAheadOfThePortStaging() throws Exception {
        // A constraint that lives in two files and is enforced by nothing.
        //
        // sourcesCallPlatformLocation reads whatever is staged in srcDir, and
        // the Codename One Android port is unpacked into that same directory
        // later in the build. The port's own sources call the platform's
        // location manager, so a scan that runs after they arrive reports
        // every application as a persistent-location user and refuses every
        // build that sets the hint.
        //
        // The scan has already moved once, to run as late as possible so it
        // does not fail builds that never read its answer. This holds the
        // other end of that range: it may move down, but not past the port.
        File builder = new File("src/main/java/com/codename1/builders/"
                + "AndroidGradleBuilder.java");
        assertTrue(builder.isFile(),
                "the builder source must be readable from the module dir: "
                + builder.getAbsolutePath());
        String text = new String(Files.readAllBytes(builder.toPath()),
                StandardCharsets.ISO_8859_1);
        int scan = text.indexOf(".sourcesCallPlatformLocation(");
        int port = text.indexOf("unzip(androidPortSrcJar");
        assertTrue(scan > 0, "the source scan call must be present");
        assertTrue(port > 0, "the port staging must be present");
        assertTrue(scan < port,
                "the staged-source scan must run BEFORE the Android port is "
                + "unpacked into srcDir, or the port's own location calls are "
                + "read as the application's");
    }

    @Test
    void nativeSourcesUsingTheFrameworkWrappersCountToo() throws Exception {
        // GeofenceManager and MapComponent make their LocationManager calls
        // INSIDE the framework, so a native source that uses one names no
        // marker method at all -- which is why the bytecode side counts a
        // reference alone, and why this pass missing them accepted
        // exclusivity over a geofence.
        File fences = tempDir("cn1-lb-native-fences");
        writeSource(new File(fences, "com/example/Fences.java"),
                "package com.example;\n"
                + "import com.codename1.location.GeofenceManager;\n"
                + "public class Fences {\n"
                + "  GeofenceManager manager;\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(fences),
                "geofencing through the wrapper is persistent use");

        File maps = tempDir("cn1-lb-native-maps");
        writeSource(new File(maps, "com/example/Maps.kt"),
                "package com.example\n"
                + "import com.codename1.maps.MapComponent\n"
                + "class Maps { val map = MapComponent() }\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(maps),
                "a map centres itself on the last known location");
    }

    @Test
    void nativeSourcesNamingTheButtonCount() throws Exception {
        // A native implementation can reach the component -- it is on the
        // classpath the generated project compiles against -- and Gradle
        // compiles that source, so it is in no jar and no class tree. Without
        // this the flag stayed false and the build deleted the bridge package
        // out from under a button the application really does construct.
        File root = tempDir("cn1-lb-native-button");
        writeSource(new File(root, "com/example/Native.java"),
                "package com.example;\n"
                + "import com.codename1.location.LocationButton;\n"
                + "public class Native {\n"
                + "  LocationButton make() { return new LocationButton(); }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesNameTheButton(root),
                "a native source naming the button uses it");
    }

    @Test
    void aSourceTooLargeToBufferIsStillSearched() throws Exception {
        // The file the scan budget refuses. Skipping it was my first answer,
        // justified by saying a missed reference costs an unused package --
        // it does not. usesLocationButton false also leaves gpsPermission
        // false and drops the AndroidX dependency, so an app whose only
        // location use is the button gets no bridge, no dependency and no
        // permission, and then prompts for one the manifest never declares.
        //
        // Over the per-entry cap deliberately, so the streaming path is the
        // one under test rather than the ordinary read.
        File root = tempDir("cn1-lb-huge");
        File src = new File(root, "com/example/Huge.java");
        src.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(src);
        try {
            out.write("package com.example;\n".getBytes("UTF-8"));
            byte[] filler = new byte[64 * 1024];
            java.util.Arrays.fill(filler, (byte) ' ');
            for (int written = 0; written < 17 * 1024 * 1024;
                    written += filler.length) {
                out.write(filler);
            }
            out.write(("class Huge { com.codename1.location.LocationButton b; }"
                    + "\n").getBytes("UTF-8"));
        } finally {
            out.close();
        }
        assertTrue(src.length() > 16L * 1024 * 1024,
                "sanity: the file must exceed the per-entry cap, or this "
                + "tests the ordinary path");
        assertTrue(LocationButtonManifestFragments.sourcesNameTheButton(root),
                "a source too large to buffer is still searched");
    }

    @Test
    void aNameStraddlingAReadBoundaryIsStillFound() throws Exception {
        // Why the streaming search carries a tail between reads. With the
        // chunk given, the boundary is known: the name starts ten characters
        // before the end of the first read and finishes in the second, so a
        // search looking at each read alone sees neither half.
        //
        // The chunk is a parameter for exactly this. A test cannot place a
        // name across a 64 KiB boundary it does not control -- a Reader may
        // return short -- and an earlier version of this test passed with the
        // carry removed, proving nothing.
        String marker = "com.codename1.location.LocationButton";
        int chunk = 64;
        StringBuilder source = new StringBuilder();
        while (source.length() < chunk + marker.length() - 10) {
            source.append(' ');
        }
        source.append(marker).append(" b;");
        assertTrue(LocationButtonManifestFragments.namesButtonInStream(
                        new java.io.StringReader(source.toString()), chunk),
                "a name split across two reads is still found");
    }

    @Test
    void reflectionElsewhereDoesNotMakeACommentButtonUse() throws Exception {
        // The reflective rule reads a string's TEXT, which is the point --
        // Class.forName's argument is a literal. It was reading the raw
        // source to do it, so a comment counted too: any class that reflects
        // at all and mentions the button in prose became button use, and the
        // toolchain gate turns that into a refused build.
        //
        // Comments are stripped for that check now; literals are not. Found
        // by crossing the reflective rule against comment stripping.
        File root = tempDir("cn1-lb-reflect-comment");
        writeSource(new File(root, "com/example/Other.java"),
                "package com.example;\n"
                + "public class Other {\n"
                + "  // com.codename1.location.LocationButton, one day\n"
                + "  Object f() throws Exception {\n"
                + "    return Class.forName(\"com.example.Thing\");\n"
                + "  }\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesNameTheButton(root),
                "reflection elsewhere does not make a comment into use");
    }

    @Test
    void anInterpolatedLogLineIsNotButtonUse() throws Exception {
        // Found by crossing the rules rather than by review. A literal holding
        // ${...} is kept WHOLE for the provider scan, because what is inside a
        // template is compiled; the button scan inherited that and read the
        // rest of the string as code. So this line was button use and the same
        // line without the interpolation was not -- adding a ${} to a message
        // decided whether the project compiled, since the toolchain gate
        // refuses a build that uses the button.
        File root = tempDir("cn1-lb-interpolated");
        writeSource(new File(root, "com/example/Log.kt"),
                "package com.example\n"
                + "class Log(val n: Int) {\n"
                + "  fun f() = \"building ${n} of "
                + "com.codename1.location.LocationButton\"\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesNameTheButton(root),
                "a message that interpolates is still a message");
    }

    @Test
    void aTemplateStillCarriesAProviderCall() throws Exception {
        // And the rule the button scan departs from stays where it was. The
        // provider scan keeps template-bearing literals whole on purpose: what
        // is inside ${...} is compiled, and masking it would lose a real
        // lookup in the direction that downgrades a request in silence.
        File root = tempDir("cn1-lb-template-still");
        writeSource(new File(root, "com/example/Note.kt"),
                "package com.example\n"
                + "import com.google.android.gms.location"
                + ".FusedLocationProviderClient\n"
                + "class Note(val c: FusedLocationProviderClient) {\n"
                + "  fun f() = \"at ${c.lastLocation}\"\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "the provider scan still reads inside a template");
    }

    @Test
    void aReflectiveLookupInNativeSourceIsUse() throws Exception {
        // Where two of my own changes met. The button's name lives in a string
        // literal here, and masking literal text -- added so prose about an
        // API would stop refusing builds -- took the real lookup with it, so
        // the bridge package was deleted from an app that builds the
        // component reflectively.
        File root = tempDir("cn1-lb-native-reflect");
        writeSource(new File(root, "com/example/Reflect.java"),
                "package com.example;\n"
                + "public class Reflect {\n"
                + "  Object make() throws Exception {\n"
                + "    return Class.forName("
                + "\"com.codename1.location.LocationButton\")"
                + ".newInstance();\n"
                + "  }\n"
                + "}\n");
        assertTrue(LocationButtonManifestFragments
                        .sourcesNameTheButton(root),
                "a reflective lookup in a native source builds the button");
    }

    @Test
    void aButtonNamedOnlyInProseIsNotUse() throws Exception {
        // Same stripping as the provider pass: a comment or a string naming
        // the component is not constructing it, and this flag drives a
        // toolchain gate that refuses the build.
        File root = tempDir("cn1-lb-native-button-prose");
        writeSource(new File(root, "com/example/Note.java"),
                "package com.example;\n"
                + "public class Note {\n"
                + "  // com.codename1.location.LocationButton is not used\n"
                + "  String s = \"com.codename1.location.LocationButton\";\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesNameTheButton(root),
                "prose about the button is not the button");
    }

    @Test
    void namingThePlatformManagerWithoutCallingItIsNotUse() throws Exception {
        // The narrow half. This scan refuses builds, and a source file is
        // prose as much as code: an import left behind by a deleted feature,
        // or the class named in a comment, is not a location request. Both
        // halves have to be there.
        File root = tempDir("cn1-lb-native-mention");
        writeSource(new File(root, "com/example/Leftover.java"),
                "package com.example;\n"
                + "// android.location.LocationManager was used here once.\n"
                + "import android.location.LocationManager;\n"
                + "public class Leftover {\n"
                + "  Object unused;\n"
                + "}\n");
        assertFalse(LocationButtonManifestFragments
                        .sourcesCallPlatformLocation(root),
                "naming the class is not asking it for a location");
    }

    @Test
    void aReflectiveLookupIsUseOfTheButton() throws Exception {
        // Class.forName("com.codename1.location.LocationButton") creates no
        // CONSTANT_Class and no descriptor: the DOTTED name sits in a string
        // constant, which both other tests miss. The bridge package was then
        // deleted out from under a component the application really builds,
        // and API 37 silently fell back to the ordinary prompt.
        File root = tempDir("cn1-lb-reflective");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Reflect", null,
                "java/lang/Object", null);
        MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC, "make",
                "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitLdcInsn("com.codename1.location.LocationButton");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class",
                "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();
        // The premise, checked rather than trusted: the SLASHED name appears
        // nowhere, so nothing but the dotted string can be answering here.
        assertFalse(new String(bytes, StandardCharsets.ISO_8859_1)
                        .contains("com/codename1/location/LocationButton"),
                "sanity: the reflective form is dotted, not slashed");
        writeClassBytes(new File(root, "com/example/Reflect.class"), bytes);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a reflective lookup builds the button too");
    }

    @Test
    void theNameInAConstantIsNotAReflectiveLookup() throws Exception {
        // The other half of the reflective test, and the one that matters
        // most: usesLocationButton drives the TOOLCHAIN GATE, which refuses
        // the build until this builder moves to AGP 9. So a library holding
        // the dotted name in a diagnostic or configuration constant, and never
        // reflecting at all, would have failed every Android build that
        // consumed it. The first version of that check accepted every matching
        // string and its comment claimed the cost was an unused package.
        File root = tempDir("cn1-lb-constant");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Help", null,
                "java/lang/Object", null);
        w.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "HELP",
                "Ljava/lang/String;", null,
                "com.codename1.location.LocationButton").visitEnd();
        w.visitEnd();
        writeClassBytes(new File(root, "com/example/Help.class"),
                w.toByteArray());
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a class that never reflects is not building the button");
    }

    @Test
    void aLoaderLookupOfTheNameIsUseOfTheButton() throws Exception {
        // And ClassLoader.loadClass counts as well as Class.forName -- a
        // library that resolves through a loader of its own reaches the same
        // component, and missing it deletes the bridge.
        File root = tempDir("cn1-lb-loader");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Loader", null,
                "java/lang/Object", null);
        MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC, "make",
                "(Ljava/lang/ClassLoader;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("com.codename1.location.LocationButton");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader",
                "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        w.visitEnd();
        writeClassBytes(new File(root, "com/example/Loader.class"),
                w.toByteArray());
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a loader lookup builds the button too");
    }

    @Test
    void aStringLiteralOfTheDescriptorIsNotUse() throws Exception {
        // The other half of the same fact. A Utf8 holding
        // "Lcom/codename1/location/LocationButton;" is byte for byte what an
        // annotation class value points at AND what a string LITERAL of that
        // text points at -- reflection and bytecode-handling code writes such
        // literals -- so accepting every match read a library that merely
        // spells the name as a user of the button and refused its whole
        // Android build at the toolchain gate. referencesClass has said this
        // about the plain name from the start; the descriptor form had the
        // same hole.
        File root = tempDir("cn1-lb-literal");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Speller", null,
                "java/lang/Object", null);
        w.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "NAME",
                "Ljava/lang/String;", null,
                "Lcom/codename1/location/LocationButton;").visitEnd();
        w.visitEnd();
        writeClassBytes(new File(root, "com/example/Speller.class"),
                w.toByteArray());
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "spelling the descriptor in a string is not using the button");
    }

    @Test
    void aFieldOfThatTypeSurvivesAStringOfTheSameText() throws Exception {
        // The trap the string gate opened. A pool holds ONE Utf8 per text, so
        // a class that declares a LocationButton field and also spells
        // "Lcom/codename1/location/LocationButton;" in a literal points its
        // own descriptor_index and a CONSTANT_String at the same entry --
        // and sending that through the annotation-only branch deleted the
        // bridge package from a library whose field is its only reference.
        File root = tempDir("cn1-lb-field");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Holder", null,
                "java/lang/Object", null);
        w.visitField(Opcodes.ACC_PUBLIC, "button",
                "Lcom/codename1/location/LocationButton;", null, null)
                .visitEnd();
        w.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "NAME",
                "Ljava/lang/String;", null,
                "Lcom/codename1/location/LocationButton;").visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        int first = text.indexOf("Lcom/codename1/location/LocationButton;");
        assertTrue(first > 0 && text.indexOf(
                        "Lcom/codename1/location/LocationButton;", first + 1)
                        < 0,
                "sanity: the descriptor is one shared Utf8");
        writeClassBytes(new File(root, "com/example/Holder.class"), bytes);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "a field of that type is use of the button");
    }

    @Test
    void readingAFieldOfThatTypeSurvivesAStringOfTheSameText()
            throws Exception {
        // And the same for a class that only READS such a field. The Fieldref
        // names the OWNER class, not the type, so LocationButton appears
        // nowhere but in the NameAndType's descriptor -- which is the same
        // shared Utf8 the string literal points at.
        File root = tempDir("cn1-lb-fieldref");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Reader", null,
                "java/lang/Object", null);
        w.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "NAME",
                "Ljava/lang/String;", null,
                "Lcom/codename1/location/LocationButton;").visitEnd();
        MethodVisitor mv = w.visitMethod(Opcodes.ACC_PUBLIC, "read",
                "(Lcom/example/Other;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.GETFIELD, "com/example/Other", "button",
                "Lcom/codename1/location/LocationButton;");
        mv.visitInsn(Opcodes.POP);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();
        // The premise: no CONSTANT_Class for the button anywhere -- the
        // Fieldref names com/example/Other -- and one shared Utf8 for the
        // descriptor.
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        assertFalse(text.contains(
                        "com/codename1/location/LocationButton\u0000"),
                "sanity: no bare class name in this file");
        int first = text.indexOf("Lcom/codename1/location/LocationButton;");
        assertTrue(first > 0 && text.indexOf(
                        "Lcom/codename1/location/LocationButton;", first + 1)
                        < 0,
                "sanity: the descriptor is one shared Utf8");
        writeClassBytes(new File(root, "com/example/Reader.class"), bytes);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "reading a field of that type is use of the button");
    }

    @Test
    void anAnnotationTextValueIsNotATypeReference() throws Exception {
        // The shape isStringConstant cannot see. An annotation's STRING value
        // points straight at a Utf8 and creates no CONSTANT_String at all, so
        // @Note("Lcom/codename1/location/LocationButton;") -- text, not a type
        // -- was read as use of the button and refused an unrelated Android
        // build at the toolchain gate.
        File root = tempDir("cn1-lb-annotation-text");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Noted", null,
                "java/lang/Object", null);
        AnnotationVisitor a = w.visitAnnotation("Lcom/example/Note;", true);
        a.visit("value", "Lcom/codename1/location/LocationButton;");
        a.visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();
        // The premise, checked rather than trusted: the descriptor is in the
        // file, and nothing points at it but the annotation's text value.
        assertTrue(new String(bytes, StandardCharsets.ISO_8859_1)
                        .contains("Lcom/codename1/location/LocationButton;"),
                "sanity: the text really is in the constant pool");
        writeClassBytes(new File(root, "com/example/Noted.class"), bytes);
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "an annotation's text value is not a type reference");
    }

    @Test
    void anAnnotationValueSurvivesAStringOfTheSameText() throws Exception {
        // And the case that makes the string test insufficient on its own: a
        // constant pool holds ONE Utf8 for a given text, so a class that both
        // annotates with LocationButton.class and spells the descriptor in a
        // literal points a CONSTANT_String and an element_value at the same
        // entry. Deciding on the string alone would delete the bridge from an
        // app that really does build the button, which is the failure this
        // whole check has to avoid -- so the annotation structures are read.
        File root = tempDir("cn1-lb-both");
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, "com/example/Both", null,
                "java/lang/Object", null);
        AnnotationVisitor a = w.visitAnnotation("Lcom/example/Widget;", true);
        a.visit("value",
                Type.getObjectType("com/codename1/location/LocationButton"));
        a.visitEnd();
        w.visitField(Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "NAME",
                "Ljava/lang/String;", null,
                "Lcom/codename1/location/LocationButton;").visitEnd();
        w.visitEnd();
        byte[] bytes = w.toByteArray();
        // The premise, checked rather than trusted: one Utf8, two referrers.
        // If ASM ever stopped sharing it the test would pass while testing
        // nothing.
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        int first = text.indexOf("Lcom/codename1/location/LocationButton;");
        assertTrue(first > 0 && text.indexOf(
                        "Lcom/codename1/location/LocationButton;", first + 1)
                        < 0,
                "sanity: the descriptor is one shared Utf8");
        writeClassBytes(new File(root, "com/example/Both.class"), bytes);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .usesButton(),
                "the annotation still names the button");
    }

    @Test
    void anEndTagPaddedBeforeItsDelimiterIsStillConsumed() {
        // </uses-permission   > is valid XML. Matching "</name>" exactly left
        // it behind while the opening tag was spliced out, and an orphan end
        // tag is a manifest that no longer parses -- produced by the path
        // meant to be tidying it up.
        String block = "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "</uses-permission   >\n";
        String out = LocationButtonManifestFragments.inject(block, false);
        assertFalse(out.contains("</uses-permission"),
                "no orphan end tag may survive: " + out);
    }

    @Test
    void aMismatchedEndTagIsLeftAloneRatherThanHalfEaten() {
        // Why the '>' is REQUIRED after the name rather than assumed. On
        // malformed input -- an opening uses-permission closed by
        // </uses-permission-sdk-23> -- matching the name alone would consume
        // "</uses-permission" and leave "-sdk-23>" behind, turning a manifest
        // that was merely wrong into one that is wrong AND mangled by us.
        // Nothing else can observe this check: elementName always derives the
        // element's own name, so well-formed input never reaches the case.
        String block = "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "</uses-permission-sdk-23>\n";
        String out = LocationButtonManifestFragments.inject(block, false);
        assertFalse(out.contains("-sdk-23>") && !out.contains("</uses"),
                "a mismatched end tag is left whole, not half consumed: "
                + out);
    }

    @Test
    void anEndTagOfTheOtherElementTypeIsNotConsumed() {
        // And the boundary the padding must not blur. </uses-permission> must
        // not eat </uses-permission-sdk-23>: they are different elements, and
        // consuming the wrong one truncates a declaration that should stay.
        String block = "    <uses-permission-sdk-23 android:name=\"android"
                + ".permission.ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "</uses-permission-sdk-23>\n"
                + "    <uses-permission android:name=\"android.permission"
                + ".CAMERA\" />\n";
        String out = LocationButtonManifestFragments.inject(block, false);
        assertTrue(out.contains("android.permission.CAMERA"),
                "the unrelated declaration survives: " + out);
        assertFalse(out.contains("</uses-permission-sdk-23>"),
                "and its own end tag went with it: " + out);
    }

    @Test
    void aRemovalCarryingItsReasonIsDeletedWholly() {
        // A removal may explain itself. Skipping only whitespace between the
        // tags left the closing one behind while the opening one was spliced
        // out, and an orphan </uses-permission> is a fragment that no longer
        // parses -- produced by the path that exists to tidy it up.
        String withReason = "    <uses-permission android:name=\"android."
                + "permission.ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "<!-- the button owns this grant --></uses-permission>\n";
        String out = LocationButtonManifestFragments.inject(withReason, false);
        assertFalse(out.contains("</uses-permission>"),
                "no closing tag may be left without its opening one: " + out);
        assertFalse(out.contains("tools:node"),
                "the removal itself is gone: " + out);
        assertFalse(out.contains("the button owns this grant"),
                "and so is the comment inside it: " + out);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                "the permission is declared once, by us: " + out);
    }

    @Test
    void anAliasedFlagIsAddedToRatherThanDuplicated() {
        // Same shape as the test above, with the namespace bound to an alias.
        // The literal lookup found no existing flags on it and wrote a SECOND
        // attribute beside the one already there -- two usesPermissionFlags on
        // one element, which is not a thing a manifest may contain.
        String existing = "    <uses-permission xmlns:a=\"http://schemas."
                + "android.com/apk/res/android\" a:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" a:usesPermissionFlags=\""
                + "neverForLocation\" />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                existing, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertTrue(out.contains("neverForLocation|onlyForLocationButton"), out);
        assertEquals(1, count(out, "usesPermissionFlags"),
                "one flags attribute on the element, not two: " + out);
    }

    @Test
    void widensAnAliasedCapAndFlagsAnAliasedDeclaration() {
        // android.xpermissions is the developer's own XML, and binding the
        // Android namespace to an alias in it is valid. activePermissionIndex
        // already FOUND such a declaration, so a literal lookup for the cap
        // beside it missed the attribute and returned the block untouched --
        // no widening, and no uncapped duplicate added either, which is the
        // silent loss of fine location above API 30 that this whole method
        // exists to prevent.
        String aliased = "    <uses-permission xmlns:a=\"http://schemas."
                + "android.com/apk/res/android\" a:name=\"android.permission."
                + "ACCESS_FINE_LOCATION\" a:maxSdkVersion=\"30\" />\n";
        String out = LocationButtonManifestFragments.inject(aliased, true);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                out);
        assertFalse(out.contains("maxSdkVersion=\""),
                "the aliased cap must be removed, not left in place: " + out);
        assertTrue(out.contains("onlyForLocationButton"),
                "and the aliased declaration is the one that gets flagged: "
                + out);
        assertEquals(1, count(out, "onlyForLocationButton"),
                "flagged once, not once per spelling: " + out);
    }

    @Test
    void widensACappedFineLocationAndStillFlagsIt() {
        String wifi = "    <uses-permission android:name=\""
                + "android.permission.ACCESS_FINE_LOCATION\""
                + " android:maxSdkVersion=\"32\" />\n";
        String out = LocationButtonManifestFragments.inject(wifi, true);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                out);
        assertFalse(out.contains("android:maxSdkVersion=\""), out);
        assertTrue(out.contains("onlyForLocationButton"), out);
    }

    @Test
    void nothingIsDeclaredTwiceWhenTheFragmentIsInjectedAgain() {
        String once = LocationButtonManifestFragments.inject("", true);
        String twice = LocationButtonManifestFragments.inject(once, true);
        assertEquals(1, count(twice, "android.permission.USE_LOCATION_BUTTON"),
                twice);
        assertEquals(1, count(twice, "android.permission.ACCESS_FINE_LOCATION"),
                twice);
        assertEquals(1, count(twice, "onlyForLocationButton"), twice);
    }

    @Test
    void anExistingPermissionFlagIsAddedToRatherThanReplaced() {
        String existing = "    <uses-permission android:name=\""
                + "android.permission.ACCESS_FINE_LOCATION\""
                + " android:usesPermissionFlags=\"neverForLocation\" />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                existing, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertTrue(out.contains("neverForLocation|onlyForLocationButton"), out);
    }

    @Test
    void aFlagAlreadyPresentIsNotAddedAgain() {
        String existing = "    <uses-permission android:name=\""
                + "android.permission.ACCESS_FINE_LOCATION\""
                + " android:usesPermissionFlags=\"onlyForLocationButton\" />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                existing, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertEquals(1, count(out, "onlyForLocationButton"), out);
    }

    @Test
    void aPermissionDeclaredWithSingleQuotesIsStillFound() {
        // An app writing android.xpermissions by hand is as likely to use
        // single quotes, and missing the declaration means adding a SECOND
        // element for the same permission.
        String existing = "    <uses-permission android:name="
                + "'android.permission.ACCESS_FINE_LOCATION' />\n";
        String out = LocationButtonManifestFragments.addPermissionFlag(
                existing, LocationButtonManifestFragments.FINE_LOCATION,
                LocationButtonManifestFragments.ONLY_FOR_LOCATION_BUTTON);
        assertEquals(1, count(out, "android.permission.ACCESS_FINE_LOCATION"),
                out);
        assertTrue(out.contains("onlyForLocationButton"), out);
    }

    @Test
    void hintParsingIsForgivingOfWhitespaceAndCase() {
        assertTrue(LocationButtonManifestFragments.isExclusive("true"));
        assertTrue(LocationButtonManifestFragments.isExclusive(" TRUE "));
        assertFalse(LocationButtonManifestFragments.isExclusive("false"));
        assertFalse(LocationButtonManifestFragments.isExclusive(null));
        assertFalse(LocationButtonManifestFragments.isExclusive(""));
    }

    @Test
    void exclusiveIsRefusedAlongsidePersistentLocation() {
        assertNull(LocationButtonManifestFragments.exclusiveConflict(
                true, false, false, false));
        assertNull(LocationButtonManifestFragments.exclusiveConflict(
                false, true, true, false));
        assertNotNull(LocationButtonManifestFragments.exclusiveConflict(
                true, true, false, false));
        assertNotNull(LocationButtonManifestFragments.exclusiveConflict(
                true, false, true, false));
        assertTrue(LocationButtonManifestFragments.exclusiveConflict(
                true, true, true, false).contains("android.locationButton.exclusive"));
    }

    @Test
    void theButtonRaisesTheCompileSdkToWhatTheLibraryDemands() {
        // Unreachable while the toolchain guard refuses these builds, and
        // asserted anyway: the guard is conditioned on the pinned AGP version,
        // so raising that pin lets location-button builds through, and without
        // this raise they reach checkAarMetadata with a compile SDK the ladder
        // caps at 36. It was removed once for being unreachable and this is
        // what catches that.
        assertEquals(AndroidGradleBuilder.LOCATION_BUTTON_MIN_COMPILE_SDK,
                AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                        false, false, false, false, true));
        // And nothing moves for an app that does not use it.
        assertEquals(28, AndroidGradleBuilder.compileSdkInt("28", "28", "28",
                false, false, false, false, false));
    }

    // ------------------------------------------------------------------
    // Library bytecode
    // ------------------------------------------------------------------

    /**
     * A stand-in for a compiled class.
     *
     * <p>Names are written the way a real constant pool stores them -- two
     * big-endian length bytes then the bytes themselves -- because that framing
     * is exactly what the scan reads to tell a whole entry from a longer symbol
     * that merely starts the same way. A fixture that wrote bare strings would
     * pass a scanner that had stopped checking.</p>
     *
     * @param at    the file to write
     * @param names the constant-pool entries it should contain
     */
    /**
     * Writes a real class file whose constant pool is exactly {@code entries}.
     *
     * <p>{@link #writeClass} deliberately writes something that is NOT a
     * parseable class -- it has no constant_pool_count -- so it exercises the
     * name-search fallback. These fixtures exercise the pool walk itself, which
     * is where the difference between a class reference and a string constant
     * lives.</p>
     */
    /** Writes a staged source file, creating the package directories. */
    private static void writeSource(File at, String text) throws Exception {
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    /** Writes finished class bytes, creating the package directories. */
    private static void writeClassBytes(File at, byte[] bytes)
            throws Exception {
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    private static void writePoolClass(File at, byte[]... entries)
            throws Exception {
        at.getParentFile().mkdirs();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba,
                (byte) 0xbe, 0, 0, 0, 52});
        int count = entries.length + 1;
        body.write((count >> 8) & 0xff);
        body.write(count & 0xff);
        for (byte[] entry : entries) {
            body.write(entry);
        }
        // access_flags, this_class, super_class, and four empty counts. The
        // walk stops at the end of the pool, but a fixture that is a real
        // class file cannot be accused of passing by accident.
        body.write(new byte[] {0, 33, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0});
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(body.toByteArray());
        } finally {
            out.close();
        }
    }

    /**
     * Writes a real NESTED class: {@code inner} declared as a member of
     * {@code outer}, with the InnerClasses attribute that says so.
     *
     * <p>Needed because that attribute is now the only thing that marks a class
     * as the framework's own inner class -- a pool-only fixture is, correctly,
     * treated as somebody's top-level class whatever its name looks like.</p>
     */
    private static void writeInnerClass(File at, String inner, String outer)
            throws Exception {
        at.getParentFile().mkdirs();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba,
                (byte) 0xbe, 0, 0, 0, 52});
        byte[][] entries = {
            cpUtf8(inner),                 // 1
            cpOne(7, 1),                   // 2  Class inner
            cpUtf8(outer),                 // 3
            cpOne(7, 3),                   // 4  Class outer
            cpUtf8("InnerClasses"),        // 5
            cpUtf8("java/lang/Object"),    // 6
            cpOne(7, 6),                   // 7  Class Object
        };
        int count = entries.length + 1;
        body.write((count >> 8) & 0xff);
        body.write(count & 0xff);
        for (byte[] entry : entries) {
            body.write(entry);
        }
        body.write(new byte[] {0, 33});              // access_flags
        body.write(new byte[] {0, 2});               // this_class  = #2
        body.write(new byte[] {0, 7});               // super_class = #7
        body.write(new byte[] {0, 0});               // interfaces
        body.write(new byte[] {0, 0});               // fields
        body.write(new byte[] {0, 0});               // methods
        body.write(new byte[] {0, 1});               // attributes
        body.write(new byte[] {0, 5});               // name = InnerClasses
        body.write(new byte[] {0, 0, 0, 10});        // length
        body.write(new byte[] {0, 1});               // one entry
        body.write(new byte[] {0, 2, 0, 4, 0, 0, 0, 0});
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(body.toByteArray());
        } finally {
            out.close();
        }
    }

    /**
     * Writes a class that EXTENDS {@code superName} and calls {@code method} on
     * itself, which is the shape javac emits for an inherited call made through
     * a subclass-typed reference.
     */
    private static void writeSubclassCall(File at, String name,
            String superName, String method) throws Exception {
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            out.write(subclassCallClass(name, superName, method));
        } finally {
            out.close();
        }
    }

    /** The same class as bytes, for writing straight into an archive. */
    private static byte[] subclassCallClass(String name, String superName,
            String method) throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba,
                (byte) 0xbe, 0, 0, 0, 52});
        byte[][] entries = {
            cpUtf8(name), cpOne(7, 1),
            cpUtf8(superName), cpOne(7, 3),
            cpUtf8(method),
            cpUtf8("(Lcom/codename1/location/LocationListener;)V"),
            cpTwo(12, 5, 6),
            cpTwo(10, 2, 7),
        };
        int count = entries.length + 1;
        body.write((count >> 8) & 0xff);
        body.write(count & 0xff);
        for (byte[] entry : entries) {
            body.write(entry);
        }
        // access_flags, this_class=2, super_class=4, and no members.
        body.write(new byte[] {0, 33, 0, 2, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0});
        return body.toByteArray();
    }

    /** A class whose pool holds a Methodref for {@code owner.method}. */
    private static byte[] methodCallClass(String owner, String method)
            throws Exception {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba,
                (byte) 0xbe, 0, 0, 0, 52});
        byte[][] entries = {
            cpUtf8(owner), cpOne(7, 1),
            cpUtf8(method), cpUtf8("()V"), cpTwo(12, 3, 4), cpTwo(10, 2, 5),
            cpUtf8("java/lang/Object"), cpOne(7, 7),
        };
        int count = entries.length + 1;
        body.write((count >> 8) & 0xff);
        body.write(count & 0xff);
        for (byte[] entry : entries) {
            body.write(entry);
        }
        body.write(new byte[] {0, 33, 0, 8, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0});
        return body.toByteArray();
    }

    /** CONSTANT_Utf8. */
    private static byte[] cpUtf8(String text) throws Exception {
        byte[] raw = text.getBytes("ISO-8859-1");
        byte[] out = new byte[3 + raw.length];
        out[0] = 1;
        out[1] = (byte) ((raw.length >> 8) & 0xff);
        out[2] = (byte) (raw.length & 0xff);
        System.arraycopy(raw, 0, out, 3, raw.length);
        return out;
    }

    /** A one-operand entry: CONSTANT_Class, CONSTANT_String. */
    private static byte[] cpOne(int tag, int index) {
        return new byte[] {(byte) tag, (byte) ((index >> 8) & 0xff),
                (byte) (index & 0xff)};
    }

    /** A two-operand entry: CONSTANT_Methodref, CONSTANT_NameAndType. */
    private static byte[] cpTwo(int tag, int first, int second) {
        return new byte[] {(byte) tag, (byte) ((first >> 8) & 0xff),
                (byte) (first & 0xff), (byte) ((second >> 8) & 0xff),
                (byte) (second & 0xff)};
    }

    private static void writeClass(File at, String... names) throws Exception {
        at.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(at);
        try {
            // A plausible class-file header, so the bytes before the first
            // entry are not themselves a length prefix by accident.
            out.write(new byte[] {(byte) 0xca, (byte) 0xfe, (byte) 0xba,
                    (byte) 0xbe, 0, 0, 0, 52});
            for (String name : names) {
                byte[] raw = name.getBytes("ISO-8859-1");
                out.write(1); // CONSTANT_Utf8
                out.write((raw.length >> 8) & 0xff);
                out.write(raw.length & 0xff);
                out.write(raw);
            }
        } finally {
            out.close();
        }
    }

    /** One CONSTANT_Utf8 entry, framed the way a class file frames it. */
    private static byte[] constantPoolEntry(String name) throws Exception {
        byte[] raw = name.getBytes("ISO-8859-1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(1);
        out.write((raw.length >> 8) & 0xff);
        out.write(raw.length & 0xff);
        out.write(raw);
        return out.toByteArray();
    }

    private static File tempDir(String name) throws Exception {
        return Files.createTempDirectory(name).toFile();
    }

    @Test
    void aLooseClassReferencingTheButtonIsFound() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-loose").toFile();
        writeClass(new File(root, "com/example/MyForm.class"),
                "com/codename1/location/LocationButton");
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
    }

    @Test
    void theFrameworksOwnClassesDoNotCount() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-framework").toFile();
        // The component itself and its Android implementation both name the
        // class; a framework jar staged beside the libraries would otherwise
        // report every application as using the button.
        writeClass(new File(root, "com/codename1/location/LocationButton.class"),
                "com/codename1/location/LocationButton");
        writeClass(new File(root, "com/codename1/impl/android/locationbutton/"
                + "AndroidLocationButtonBridge.class"),
                "com/codename1/location/LocationButton");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
    }

    @Test
    void aClassInsideACn1libIsFound() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-jar").toFile();
        File jar = new File(root, "mylib.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
        try {
            zip.putNextEntry(new ZipEntry("com/example/LibForm.class"));
            zip.write(constantPoolEntry("com/codename1/location/LocationButton"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
    }

    @Test
    void aClassInsideAnAarsNestedJarIsFound() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-aar").toFile();
        ByteArrayOutputStream inner = new ByteArrayOutputStream();
        ZipOutputStream innerZip = new ZipOutputStream(inner);
        try {
            innerZip.putNextEntry(new ZipEntry("com/example/AarForm.class"));
            innerZip.write(constantPoolEntry(
                    "com/codename1/location/LocationButton"));
            innerZip.closeEntry();
        } finally {
            innerZip.close();
        }
        File aar = new File(root, "mylib.aar");
        ZipOutputStream outerZip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            outerZip.putNextEntry(new ZipEntry("classes.jar"));
            outerZip.write(inner.toByteArray());
            outerZip.closeEntry();
        } finally {
            outerZip.close();
        }
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
    }

    @Test
    void aLibraryThatGeofencesIsReportedAsPersistent() throws Exception {
        // The half android.locationButton.exclusive turns on. A cn1lib that
        // shows the button AND geofences must not let exclusivity through: the
        // attributed method scan reads the loose class tree only, so without
        // this the conflict check never saw the library and onlyForLocationButton
        // went into the manifest of an app whose library needs the grant to
        // outlive the session.
        File root = tempDir("cn1-lb-persistent");
        writeClass(new File(root, "com/example/LibTracker.class"),
                "com/codename1/location/LocationButton", "addGeoFencing");
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertTrue(usage.usesButton(), "the button reference was missed");
        assertTrue(usage.usesPersistentLocation(),
                "addGeoFencing was missed");
    }

    @Test
    void aBackgroundListenerCountsAsPersistentToo() throws Exception {
        File root = tempDir("cn1-lb-background");
        writeClass(new File(root, "com/example/LibBackground.class"),
                "setBackgroundLocationListener");
        LocationButtonManifestFragments.LocationUsage usage =
                LocationButtonManifestFragments.scanForLocationUsage(root);
        assertFalse(usage.usesButton());
        assertTrue(usage.usesPersistentLocation());
    }

    @Test
    void aCustomerHelperInsideAFrameworkNamespaceIsStillInspected()
            throws Exception {
        // The filter used to be the package prefixes com/codename1/location/
        // and com/codename1/impl/, which also covered a cn1lib's own helper
        // living there -- and libraries do put native-interface implementations
        // under com.codename1.impl. If such a helper was the library's only
        // reference to the button, the scan missed it and the bridge package
        // was deleted out from under it.
        File root = tempDir("cn1-lb-customerhelper");
        writeClass(new File(root, "com/codename1/impl/android/MyLibHelper.class"),
                "com/codename1/location/LocationButton");
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "a customer class in a framework namespace must be inspected");
    }

    @Test
    void theButtonsOwnInnerClassesAreNotApplicationUse() {
        // The P0 shape. LocationButton's anonymous inner classes call back into
        // the outer class, so a scan that asks only "was this name referenced"
        // answers yes for every application ever built once the framework is in
        // the tree -- and the flag now gates a build-refusing toolchain check,
        // so that would reject every Android build in existence. Both the
        // loose-class callback in AndroidGradleBuilder and the library scan
        // filter through isFrameworkClass, and these are the names it has to
        // cover.
        for (String inner : new String[] {
                "com/codename1/location/LocationButton$1",
                "com/codename1/location/LocationButton$2",
                "com/codename1/location/LocationButton$5", }) {
            assertTrue(LocationButtonManifestFragments.isFrameworkOwner(inner),
                    inner + " must not count as application use");
            // And by NAME only, which is all the loose scan ever has: the
            // exact-match form must NOT claim it, because that form is what
            // the library scan uses to decide whether to read a class at all.
            assertFalse(
                    LocationButtonManifestFragments.isFrameworkClass(inner),
                    inner + " must still be read by the library scan");
        }
        // Null is what scanningType reports before the first class is
        // announced; it must not be mistaken for a framework class.
        assertFalse(LocationButtonManifestFragments.isFrameworkOwner(null));
        assertFalse(LocationButtonManifestFragments.isFrameworkClass(null));
    }

    @Test
    void everyFrameworkClassOnTheListIsFilteredIncludingItsInnerClasses() {
        for (String name : LocationButtonManifestFragments.frameworkClasses()) {
            assertTrue(LocationButtonManifestFragments
                    .isFrameworkClass(name + ".class"), name);
            // The anonymous listeners inside LocationButton carry the same
            // constant-pool entries as the class itself. By NAME that is the
            // owner variant's job -- the loose scan has nothing else -- while
            // the exact form must decline, so the library scan still reads a
            // class whose name merely looks nested.
            assertTrue(LocationButtonManifestFragments
                    .isFrameworkOwner(name + "$1.class"), name + "$1");
            assertFalse(LocationButtonManifestFragments
                    .isFrameworkClass(name + "$1.class"), name + "$1");
        }
        assertFalse(LocationButtonManifestFragments
                .isFrameworkClass("com/example/MyForm.class"));
    }

    @Test
    void aClassNestedTwoLevelsDeepInsideTheFrameworkIsStillTheFrameworks()
            throws Exception {
        // Nesting is not one level. scheduleStaleWake puts a Runnable inside a
        // TimerTask, so the framework really does contain LocationButton$7$1,
        // whose InnerClasses attribute names LocationButton$7 as its outer --
        // and THAT is not on the list, only the top-level type is. Resolving
        // one level and stopping reported the class as application code, which
        // is the P0 direction: every Android build refused.
        //
        // Built here rather than read from the framework tree so the guard does
        // not evaporate the day that anonymous class is refactored away. The
        // sibling LocationButtonMarkerCoverageTest covers the real one.
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_SUPER,
                "com/codename1/location/LocationButton$7$1", null,
                "java/lang/Object", new String[] {"java/lang/Runnable"});
        w.visitInnerClass("com/codename1/location/LocationButton$7$1",
                "com/codename1/location/LocationButton$7", null, 0);
        // The enclosing anonymous class is listed too, with neither an outer
        // nor a name because that is what anonymous means here. Not decoration:
        // javac is required to list every class in the pool that is not a
        // member of a package, and the real LocationButton$7$1 carries exactly
        // this entry -- it is the only thing that distinguishes the framework's
        // own $7 from a library's top-level class of the same name.
        w.visitInnerClass("com/codename1/location/LocationButton$7", null, null,
                Opcodes.ACC_STATIC);
        w.visitEnd();
        String text = new String(w.toByteArray(), StandardCharsets.ISO_8859_1);
        assertTrue(LocationButtonManifestFragments
                .isNestedInsideFramework(text),
                "a class nested two levels inside LocationButton is the "
                + "framework's, however deep the nesting goes");

        // And the predicate still declines a class that is genuinely nobody
        // else's: nesting inside an application class stays application code.
        ClassWriter app = new ClassWriter(0);
        app.visit(Opcodes.V1_8, Opcodes.ACC_SUPER, "com/example/MyForm$1$1",
                null, "java/lang/Object", null);
        app.visitInnerClass("com/example/MyForm$1$1", "com/example/MyForm$1",
                null, 0);
        app.visitInnerClass("com/example/MyForm$1", null, null,
                Opcodes.ACC_STATIC);
        app.visitEnd();
        assertFalse(LocationButtonManifestFragments.isNestedInsideFramework(
                new String(app.toByteArray(), StandardCharsets.ISO_8859_1)));
    }

    @Test
    void aLibrarysDollarNamedTopLevelClassKeepsItsChildren() throws Exception {
        // A dollar is an ordinary identifier character, so a library may
        // legally declare a TOP-LEVEL class called LocationButton$Adapter, in
        // the framework's own package, with a nested class of its own that is
        // the only place it touches the button.
        //
        // Walking outward by stripping the name to the first dollar reaches
        // LocationButton, charges the child to the framework and drops the
        // reference -- the toolchain gate never fires and the bridge the app
        // needs is deleted. So the walk steps up only where the InnerClasses
        // table says the outer is itself nested, and a top-level class is a
        // member of a package and is never listed there.
        ClassWriter w = new ClassWriter(0);
        w.visit(Opcodes.V1_8, Opcodes.ACC_SUPER,
                "com/codename1/location/LocationButton$Adapter$1", null,
                "java/lang/Object", null);
        w.visitInnerClass("com/codename1/location/LocationButton$Adapter$1",
                "com/codename1/location/LocationButton$Adapter", null, 0);
        w.visitEnd();
        assertFalse(LocationButtonManifestFragments.isNestedInsideFramework(
                new String(w.toByteArray(), StandardCharsets.ISO_8859_1)),
                "a library's own top-level LocationButton$Adapter is not the "
                + "framework, so neither are its children");

        // The distinguishing bit is the table, not the shape of the name: the
        // SAME name, listed as nested, is the framework's.
        ClassWriter nested = new ClassWriter(0);
        nested.visit(Opcodes.V1_8, Opcodes.ACC_SUPER,
                "com/codename1/location/LocationButton$Adapter$1", null,
                "java/lang/Object", null);
        nested.visitInnerClass("com/codename1/location/LocationButton$Adapter$1",
                "com/codename1/location/LocationButton$Adapter", null, 0);
        nested.visitInnerClass("com/codename1/location/LocationButton$Adapter",
                "com/codename1/location/LocationButton", "Adapter", 0);
        nested.visitEnd();
        assertTrue(LocationButtonManifestFragments.isNestedInsideFramework(
                new String(nested.toByteArray(), StandardCharsets.ISO_8859_1)),
                "the framework's own member class and its children are the "
                + "framework's");
    }

    @Test
    void theFrameworksOwnLocationClassesDoNotCountAsPersistent()
            throws Exception {
        // LocationManager declares both methods, so a framework jar staged
        // beside the libraries would report every application as tracking.
        File root = tempDir("cn1-lb-fwpersistent");
        writeClass(new File(root,
                "com/codename1/location/LocationManager.class"),
                "addGeoFencing", "setBackgroundLocationListener");
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).isEmpty());
    }

    @Test
    void aCompressionBombIsRefusedRatherThanInflated() throws Exception {
        // The scan runs over a directory of libraries the customer uploaded,
        // beside other people's builds on the hosted daemon. An entry that
        // inflates without bound is a heap exhaustion away from taking the JVM
        // with it -- which is why Executor.PermScanBudget exists at all, and
        // its readEntry contract records that having happened once already.
        File root = tempDir("cn1-lb-bomb");
        File jar = new File(root, "bomb.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
        try {
            // Highly compressible and far past the per-entry allowance.
            byte[] zeros = new byte[1024 * 1024];
            zip.putNextEntry(new ZipEntry("com/example/Big.class"));
            for (int i = 0; i < 32; i++) {
                zip.write(zeros);
            }
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertThrows(java.io.IOException.class, new Executable() {
            public void execute() throws Throwable {
                LocationButtonManifestFragments.scanForLocationUsage(root);
            }
        }, "an entry past the budget must be refused, not inflated");
    }

    @Test
    void anOversizedLooseClassIsRefusedRatherThanRead() throws Exception {
        // The archive paths went through the budget from the start and the
        // loose-file path did not, so a single huge .class dropped in the
        // library tree still inflated without bound. A loose file cannot lie
        // about its size the way a compressed entry can, but the aggregate cap
        // is what stops a tree of merely large ones adding up to the same heap
        // exhaustion on a shared build host.
        File root = tempDir("cn1-lb-bigloose");
        File big = new File(root, "com/example/Huge.class");
        big.getParentFile().mkdirs();
        OutputStream out = new FileOutputStream(big);
        try {
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < 24; i++) {
                out.write(chunk);
            }
        } finally {
            out.close();
        }
        assertThrows(java.io.IOException.class, new Executable() {
            public void execute() throws Throwable {
                LocationButtonManifestFragments.scanForLocationUsage(root);
            }
        }, "a loose class past the budget must be refused, not read");
    }

    @Test
    void aLongerSymbolThatStartsTheSameWayIsNotAMatch() throws Exception {
        // A substring search called this a LocationButton reference. Harmless
        // when the cost was a spare permission; not harmless now that the flag
        // gates a check which refuses the whole Android build, so a library
        // that merely ships a similarly named helper would stop it.
        File root = tempDir("cn1-lb-longer");
        writeClass(new File(root, "com/example/Lib.class"),
                "com/codename1/location/LocationButtonHelper");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "LocationButtonHelper is not LocationButton");

        File methods = tempDir("cn1-lb-longermethod");
        writeClass(new File(methods, "com/example/Lib2.class"),
                "addGeoFencingLater");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(methods).usesPersistentLocation(),
                "addGeoFencingLater is not addGeoFencing");
    }

    @Test
    void anOrdinaryLibraryIsNotCharged() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-none").toFile();
        writeClass(new File(root, "com/example/Other.class"),
                "com/codename1/location/LocationManager");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(null).usesButton());
    }

    /**
     * A background permission the project declared by HAND has to reach the
     * exclusivity check, which otherwise sees only the bytecode-derived flag.
     */
    @Test
    void aHandDeclaredBackgroundPermissionIsRecognised() {
        assertTrue(LocationButtonManifestFragments.declaresBackgroundLocation(
                "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" android:required=\"false\" />"));
        assertFalse(LocationButtonManifestFragments
                .declaresBackgroundLocation(null));
        assertFalse(LocationButtonManifestFragments
                .declaresBackgroundLocation(""));
        // Fine location is not background location, and a check that answered
        // yes here would refuse every exclusive build.
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" />"));
    }
    /** A CONSTANT_Class naming the button is what use of it looks like. */
    @Test
    void aClassReferenceToTheButtonIsUse() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-cref").toFile();
        writePoolClass(new File(root, "com/example/Uses.class"),
                cpUtf8("com/codename1/location/LocationButton"),
                cpOne(7, 1));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton());
    }

    /**
     * The same text as a STRING constant is not use of the button, and reading
     * it as use refused that library's whole Android build.
     */
    @Test
    void aStringConstantNamingTheButtonIsNotUse() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-sconst").toFile();
        writePoolClass(new File(root, "com/example/Mentions.class"),
                cpUtf8("com/codename1/location/LocationButton"),
                cpOne(8, 1));
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "a string literal is not a class reference");
    }

    /** A real call to LocationManager.addGeoFencing is persistent use. */
    @Test
    void aCallToLocationManagerIsPersistent() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-call").toFile();
        writePoolClass(new File(root, "com/example/Fences.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("addGeoFencing"),
                cpUtf8("(Lcom/codename1/location/Geofence;)V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation());
    }

    /**
     * A library's OWN method of the same name is not, which is the false
     * positive that refused android.locationButton.exclusive for a project
     * that never geofenced anything.
     */
    @Test
    void aLibrarysOwnAddGeoFencingIsNotPersistent() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-own").toFile();
        writePoolClass(new File(root, "com/example/Maps.class"),
                cpUtf8("addGeoFencing"),
                cpUtf8("setBackgroundLocationListener"));
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "declaring a method of that name is not calling ours");
    }

    /**
     * A class the walk cannot parse still goes through the name search. The
     * fallback answers too generously rather than not at all, which is the
     * direction a half-read tree has to fail in.
     */
    @Test
    void anUnparseableClassFallsBackToTheNameSearch() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-fallback").toFile();
        // writeClass emits no constant_pool_count, so the walk refuses it.
        writeClass(new File(root, "com/example/Odd.class"),
                "com/codename1/location/LocationButton");
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "the fallback must still find the marker");
    }
    /**
     * Foreground tracking needs precise location the button does not give it,
     * so exclusive mode has to see it. onlyForLocationButton downgrades every
     * non-button request to approximate rather than only the persistent ones.
     */
    @Test
    void foregroundTrackingCountsAgainstExclusiveMode() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-track").toFile();
        writePoolClass(new File(root, "com/example/Navigates.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("setLocationListener"),
                cpUtf8("(Lcom/codename1/location/LocationListener;)V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "continuous updates need the grant the hint gives away");
        assertNotNull(LocationButtonManifestFragments.exclusiveConflict(
                true, false, true, false),
                "and the build must refuse the combination");
    }

    /**
     * The framework calls setLocationListener itself, underneath
     * getCurrentLocationSync. If that counted, an app whose only precise
     * location IS the button could never set the hint -- which is the whole
     * point of the hint.
     */
    @Test
    void theFrameworksOwnTrackingCallDoesNotCount() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-fwtrack").toFile();
        writePoolClass(new File(root,
                        "com/codename1/location/LocationManager.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("setLocationListener"),
                cpUtf8("(Lcom/codename1/location/LocationListener;)V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "the framework's own call must not charge the application");
    }

    /** The refusal names the API that actually clashes, not only geofencing. */
    @Test
    void theConflictMessageNamesForegroundTrackingToo() {
        String message = LocationButtonManifestFragments.exclusiveConflict(
                true, false, true, false);
        assertNotNull(message);
        assertTrue(message.indexOf("setLocationListener") > -1,
                "a navigating app must be told what clashes: " + message);
    }
    /** The cached lookup is a non-button precise request like any other. */
    @Test
    void aLastKnownLookupCountsAgainstExclusiveMode() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-last").toFile();
        writePoolClass(new File(root, "com/example/Cached.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("getLastKnownLocation"),
                cpUtf8("()Lcom/codename1/location/Location;"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "a cached lookup is still precise location outside the button");
    }

    /**
     * MapComponent calls getLastKnownLocation and is staged beside every
     * application, so charging the app for it would refuse exclusive mode
     * everywhere.
     */
    @Test
    void theFrameworksOwnCachedLookupDoesNotCount() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-map").toFile();
        writePoolClass(new File(root, "com/codename1/maps/MapComponent.class"),
                cpUtf8("com/codename1/location/LocationManager"),
                cpOne(7, 1),
                cpUtf8("getLastKnownLocation"),
                cpUtf8("()Lcom/codename1/location/Location;"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "MapComponent's own call must not charge the application");
    }
    /**
     * A library that geofences through the documented GeofenceManager API names
     * no marker METHOD at all -- the calls are GeofenceManager's own -- so the
     * class reference is what has to count.
     */
    @Test
    void referencingGeofenceManagerIsPersistentUse() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-geo").toFile();
        writePoolClass(new File(root, "com/example/Fences.class"),
                cpUtf8("com/codename1/location/GeofenceManager"),
                cpOne(7, 1));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "geofencing through the wrapper is still persistent use");
    }

    /** The wrapper's own inner class must not charge the application. */
    @Test
    void geofenceManagersOwnInnerClassDoesNotCount() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-geoin").toFile();
        writeInnerClass(new File(root,
                        "com/codename1/location/GeofenceManager$Listener.class"),
                "com/codename1/location/GeofenceManager$Listener",
                "com/codename1/location/GeofenceManager");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "the wrapper's own inner class is framework code");
    }
    /**
     * tools:node="remove" strips a permission a library contributed. Reading it
     * as a declaration refused the build of the developer who did exactly the
     * right thing to qualify for exclusive mode.
     */
    @Test
    void aRemovalDirectiveIsNotADeclaration() {
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "    <uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" tools:node=\"remove\" />"),
                "a removal directive must not read as a request");
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name='android.permission"
                + ".ACCESS_BACKGROUND_LOCATION' tools:node='remove'/>"),
                "single quotes are a manifest's business too");
    }

    /**
     * A removal of one permission must not excuse a genuine request for the
     * same one elsewhere in the block.
     */
    @Test
    void aRealRequestBesideARemovalStillCounts() {
        assertTrue(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" tools:node=\"remove\" />\n"
                + "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" />"),
                "the second element is a real request");
    }
    /**
     * A removal directive for the button's own permission has to be DELETED,
     * not merely declared around: the merger honours the removal, so leaving it
     * shipped a button that could never be granted.
     */
    @Test
    void injectionDeletesAFineLocationRemoval() {
        String out = LocationButtonManifestFragments.inject(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\" />\n", false);
        assertFalse(out.indexOf("node=\"remove\"") > -1,
                "the removal must be gone: " + out);
        assertTrue(out.indexOf("ACCESS_FINE_LOCATION") > -1,
                "and the permission actually declared: " + out);
    }

    /** Constructing a map is a non-button location lookup of its own. */
    @Test
    void constructingAMapWithNoCentreIsPersistentUse() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-map2").toFile();
        writePoolClass(new File(root, "com/example/Screen.class"),
                cpUtf8("com/codename1/maps/MapComponent"),
                cpOne(7, 1),
                cpUtf8("<init>"),
                cpUtf8("()V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "a centreless map looks up the last known location");
    }

    /**
     * The overload that takes a centre counts too, because that centre may be
     * null -- a supported value MapComponent answers by looking a location up,
     * and one no descriptor can distinguish from a real Coord.
     */
    @Test
    void constructingAMapWithACentreIsAlsoPersistentUse() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-map4").toFile();
        writePoolClass(new File(root, "com/example/Centred.class"),
                cpUtf8("com/codename1/maps/MapComponent"),
                cpOne(7, 1),
                cpUtf8("<init>"),
                cpUtf8("(Lcom/codename1/maps/MapProvider;"
                        + "Lcom/codename1/maps/Coord;I)V"),
                cpTwo(12, 3, 4),
                cpTwo(10, 2, 5));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "a centre argument may be null, which looks a location up");
    }

    /** The map's own inner classes must not charge the application. */
    @Test
    void mapComponentsOwnInnerClassDoesNotCount() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-map3").toFile();
        writeInnerClass(new File(root, "com/codename1/maps/MapComponent$1.class"),
                "com/codename1/maps/MapComponent$1",
                "com/codename1/maps/MapComponent");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesPersistentLocation(),
                "the map's own inner class is framework code");
    }
    /**
     * The two scans have to look for the same wrappers.
     *
     * <p>{@code AndroidGradleBuilder} reads the loose class tree and this class
     * reads the submitted archives; they answer ONE question about two trees. A
     * wrapper added to only one of them is a hole in whichever tree the
     * application happens to use, and that is not hypothetical:
     * {@code MapComponent} went into the archive list first, and an app that
     * built a map in its own code went on reporting no location use at all.</p>
     */
    @Test
    void bothScansLookForTheSameWrappers() {
        assertArrayEquals(
                LocationButtonManifestFragments.NON_BUTTON_LOCATION_CLASSES,
                AndroidGradleBuilder.NON_BUTTON_LOCATION_CLASSES,
                "the loose scan and the archive scan must agree on which "
                + "framework wrappers count as non-button location use");
        assertTrue(
                LocationButtonManifestFragments
                        .NON_BUTTON_LOCATION_CLASSES.length >= 1,
                "the list looks unreadable, which would make this pass on two "
                + "empty arrays");
        // The map is matched by CONSTRUCTOR rather than by class reference, so
        // its class name and its lookup descriptors have to agree across the
        // two scans as well -- the same drift, one level down.
        assertEquals(LocationButtonManifestFragments.MAP_COMPONENT_CLASS,
                AndroidGradleBuilder.mapComponentClassForTest(),
                "both scans must agree on the map's class name");
        assertTrue(LocationButtonManifestFragments
                .MAP_COMPONENT_ANY_CONSTRUCTOR,
                "every map constructor counts; a descriptor cannot tell a null "
                + "centre from a real one");
    }
    /** XML allows spaces around the equals, and a manifest may use them. */
    @Test
    void aSpacedRemovalDirectiveIsStillARemoval() {
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" tools:node = \"remove\" />"),
                "whitespace around the attribute must not hide the removal");
        String out = LocationButtonManifestFragments.inject(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node = 'remove' />\n", false);
        // The removal DIRECTIVE, not the word: inject now also writes
        // tools:remove="android:maxSdkVersion", which is an instruction to the
        // merger rather than a removal of this element.
        assertFalse(out.indexOf("tools:node") > -1,
                "the spaced removal must be deleted too: " + out);
    }

    /** A node attribute that is not a removal leaves the element alone. */
    @Test
    void aNonRemovalNodeAttributeIsNotARemoval() {
        assertTrue(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" tools:node=\"merge\" />"),
                "tools:node=merge is a declaration, not a removal");
    }
    /**
     * A note ABOUT the permission is not a request for it. Documenting that
     * background location must not be asked for is exactly what a project
     * qualifying for exclusive mode would write, and reading it as a request
     * refused that project's build.
     */
    @Test
    void aPermissionNamedInACommentIsNotADeclaration() {
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<!-- deliberately not requesting android.permission"
                + ".ACCESS_BACKGROUND_LOCATION here -->"),
                "an XML comment is not a uses-permission element");
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<!-- see android.permission.ACCESS_BACKGROUND_LOCATION -->\n"
                + "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" />"),
                "a comment beside an unrelated permission is still no request");
    }

    /** A real element beside the comment is still found. */
    @Test
    void aRealRequestBesideACommentStillCounts() {
        assertTrue(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<!-- android.permission.ACCESS_BACKGROUND_LOCATION -->\n"
                + "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" />"),
                "the element after the comment is a real request");
    }

    /** A different permission whose name contains this one is not this one. */
    @Test
    void aLongerPermissionNameIsNotAMatch() {
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION_EXTRA\" />"),
                "a longer permission name is a different permission");
    }
    /**
     * A commented-out ELEMENT, not just a commented mention. Looking backwards
     * for the nearest '<' lands on the uses-permission inside the comment and
     * reports live markup.
     */
    @Test
    void aCommentedOutElementIsNotADeclaration() {
        assertFalse(LocationButtonManifestFragments.declaresBackgroundLocation(
                "<!-- <uses-permission android:name=\"android.permission"
                + ".ACCESS_BACKGROUND_LOCATION\" /> -->"),
                "a parked declaration is not an active one");
    }

    /**
     * And a commented-out fine-location declaration must not count as already
     * present -- the button would then ship with no permission to be granted.
     */
    @Test
    void injectionIgnoresACommentedOutDeclaration() {
        String out = LocationButtonManifestFragments.inject(
                "<!-- <uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" /> -->\n", false);
        int comment = out.indexOf("<!--");
        int live = out.indexOf("<uses-permission android:name=\"android"
                + ".permission.ACCESS_FINE_LOCATION\"");
        assertTrue(live >= 0, "a real declaration must be added: " + out);
        assertTrue(comment < 0 || live < comment,
                "the added element must be outside the comment: " + out);
    }
    /**
     * The exclusive flag has to land on the LIVE element. A commented-out
     * declaration sitting before the real one used to win the lookup, so the
     * flag went inside the comment and the build claimed an exclusivity the
     * manifest never had.
     */
    @Test
    void theExclusiveFlagSkipsACommentedDeclaration() {
        String out = LocationButtonManifestFragments.inject(
                "<!-- <uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" /> -->\n"
                + "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" />\n", true);
        int comment = out.indexOf("<!--");
        int commentEnd = out.indexOf("-->");
        int flag = out.indexOf("onlyForLocationButton");
        assertTrue(flag >= 0, "the flag must be written: " + out);
        assertTrue(comment < 0 || flag > commentEnd,
                "the flag must land outside the comment: " + out);
    }
    /**
     * A library's own TOP-LEVEL class whose name contains a dollar is not the
     * framework's class of the same prefix. '$' is a legal identifier
     * character, and truncating at it skipped exactly the class a library's
     * only button reference might live in.
     */
    @Test
    void aDollarNamedLibraryClassIsStillInspected() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-dollar").toFile();
        writePoolClass(new File(root,
                        "com/codename1/location/LocationButton$Adapter.class"),
                cpUtf8("com/codename1/location/LocationButton"),
                cpOne(7, 1));
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "a library class named ...LocationButton$Adapter is not the "
                + "framework's LocationButton");
    }

    /**
     * An anonymous inner class still is -- established by its InnerClasses
     * METADATA, not by the shape of its name.
     *
     * <p>An earlier version of this comment argued that "$7 is only ever the
     * compiler's, because a Java identifier cannot begin with a digit". That
     * is wrong: a digit is a legal identifier PART, so
     * {@code LocationManager$999} is a name a library may legally give a
     * top-level class, and reading it as the framework's is how a real call
     * gets attributed away. The fixture below writes the InnerClasses
     * attribute for that reason.</p>
     */
    @Test
    void anAnonymousFrameworkInnerClassIsStillFiltered() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-anon").toFile();
        writeInnerClass(new File(root,
                        "com/codename1/location/LocationButton$7.class"),
                "com/codename1/location/LocationButton$7",
                "com/codename1/location/LocationButton");
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "the framework's own anonymous class must not charge the app");
    }

    /**
     * A removal written with a separate closing tag has to go entirely.
     * Deleting only the opening element left {@code </uses-permission>}
     * stranded and the fragment no longer parsed.
     */
    @Test
    void aNonSelfClosingRemovalIsDeletedWhole() {
        String out = LocationButtonManifestFragments.inject(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\">"
                + "</uses-permission>\n", false);
        assertFalse(out.indexOf("</uses-permission>") > -1,
                "the closing tag must go with its element: " + out);
        assertFalse(out.indexOf("tools:node") > -1,
                "and the removal itself: " + out);
        assertTrue(out.indexOf("ACCESS_FINE_LOCATION") > -1,
                "with a real declaration in its place: " + out);
    }

    /** A self-closing removal keeps working, closing tag or not. */
    @Test
    void aSelfClosingRemovalIsStillDeleted() {
        String out = LocationButtonManifestFragments.inject(
                "<uses-permission android:name=\"android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\" />\n", false);
        assertFalse(out.indexOf("tools:node") > -1, out);
        assertTrue(out.indexOf("ACCESS_FINE_LOCATION") > -1, out);
    }
    /**
     * The loose scan must never apply the EXACT predicate to the class it is
     * reading.
     *
     * <p>{@code isFrameworkClass} declines {@code LocationButton$1} on purpose,
     * so that the library scan still reads a class whose name merely looks
     * nested. The loose scan wants the opposite: it is handed the name of the
     * class making a reference and no bytes, and the framework's own inner
     * classes reference the framework constantly. Pairing the exact form with
     * {@code scanningLocationType} therefore marks every application as using
     * the location button and refuses every Android build.</p>
     *
     * <p>Checked over the SOURCE because the call sites live inside an
     * anonymous scanner in a method thousands of lines long, and because the
     * way this last went wrong was a sweep that replaced the single-line
     * spelling and missed four line-wrapped ones. A regex sees both.</p>
     */
    @Test
    void theLooseScanUsesTheOwnerPredicate() throws Exception {
        File builder = new File(
                "src/main/java/com/codename1/builders/AndroidGradleBuilder.java");
        assertTrue(builder.isFile(),
                "expected to find " + builder.getAbsolutePath());
        String text = new String(Files.readAllBytes(builder.toPath()),
                StandardCharsets.UTF_8);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "isFrameworkClass\\s*\\(\\s*scanningLocationType").matcher(text);
        assertFalse(m.find(),
                "the loose scan must call isFrameworkOwner, not "
                + "isFrameworkClass, on scanningLocationType");
        // Non-vacuous: the owner form really is used there.
        assertTrue(java.util.regex.Pattern.compile(
                "isFrameworkOwner\\s*\\(\\s*scanningLocationType")
                .matcher(text).find(),
                "the loose scan should be attributing at all");
    }
    /**
     * A removal of a DIFFERENT permission whose name contains ours must be left
     * alone -- deleting it would let the permission it suppresses back into the
     * merged manifest.
     */
    @Test
    void aRemovalOfALongerPermissionIsLeftAlone() {
        String in = "<uses-permission android:name=\"com.example.android"
                + ".permission.ACCESS_FINE_LOCATION\" tools:node=\"remove\" />\n";
        String out = LocationButtonManifestFragments.inject(in, false);
        assertTrue(out.indexOf("com.example.android.permission"
                + ".ACCESS_FINE_LOCATION\" tools:node=\"remove\"") > -1,
                "somebody else's removal must survive: " + out);
    }
    /**
     * An aar's own manifest asking for background location is a request the
     * application ships, and it calls nothing of ours -- a native location SDK
     * does its own work -- so only the manifest can reveal it.
     */
    @Test
    void anArchiveManifestRequestingBackgroundLocationIsSeen()
            throws Exception {
        File root = Files.createTempDirectory("cn1-lb-aar").toFile();
        File aar = new File(root, "native-location.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest><uses-permission android:name=\"android"
                    + ".permission.ACCESS_BACKGROUND_LOCATION\" />"
                    + "</manifest>").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                .declaresBackgroundLocation(),
                "a contributed manifest's request must be seen");
    }

    /** An archive that parks the permission in a comment is not asking. */
    @Test
    void anArchiveManifestCommentIsNotARequest() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-aar2").toFile();
        File aar = new File(root, "polite.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(("<manifest><!-- <uses-permission android:name=\"android"
                    + ".permission.ACCESS_BACKGROUND_LOCATION\" /> -->"
                    + "</manifest>").getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                .declaresBackgroundLocation(),
                "a commented-out request is not a request");
    }
    /**
     * Only the ROOT manifest of an AAR is merged, so a template buried in its
     * resources -- or a manifest inside an ordinary jar -- asks for nothing.
     */
    @Test
    void aNonRootOrNonAarManifestIsIgnored() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-aar3").toFile();
        String request = "<manifest><uses-permission android:name=\"android"
                + ".permission.ACCESS_BACKGROUND_LOCATION\" /></manifest>";
        File aar = new File(root, "templates.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("assets/example/AndroidManifest.xml"));
            zip.write(request.getBytes("UTF-8"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        File jar = new File(root, "ordinary.jar");
        ZipOutputStream jz = new ZipOutputStream(new FileOutputStream(jar));
        try {
            jz.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            jz.write(request.getBytes("UTF-8"));
            jz.closeEntry();
        } finally {
            jz.close();
        }
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                .declaresBackgroundLocation(),
                "only an aar's root manifest is merged");
    }
    /**
     * A jar parked in an aar's resources is not on the classpath, so code in it
     * never runs and must not decide anything about the build.
     */
    @Test
    void aNonClasspathJarInsideAnAarIsIgnored() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-assetjar").toFile();
        File aar = new File(root, "tooling.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("assets/sample.jar"));
            zip.write(buttonReferencingJar());
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "assets/sample.jar is not on the classpath");
    }

    /** classes.jar is, and is still read. */
    @Test
    void anAarsClassesJarIsStillRead() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-classesjar").toFile();
        File aar = new File(root, "real.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("classes.jar"));
            zip.write(buttonReferencingJar());
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "classes.jar is the aar's classpath");
    }

    /** A jar holding one class that references the button by CONSTANT_Class. */
    private static byte[] buttonReferencingJar() throws Exception {
        File tmp = File.createTempFile("cn1-inner", ".class");
        writePoolClass(tmp, cpUtf8("com/codename1/location/LocationButton"),
                cpOne(7, 1));
        byte[] cls = Files.readAllBytes(tmp.toPath());
        tmp.delete();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream jar = new ZipOutputStream(out);
        try {
            jar.putNextEntry(new ZipEntry("com/example/Uses.class"));
            jar.write(cls);
            jar.closeEntry();
        } finally {
            jar.close();
        }
        return out.toByteArray();
    }
    /**
     * A loose class file inside an aar is not on the classpath either. An
     * aar's bytecode lives in classes.jar; a .class sitting elsewhere in the
     * archive is a resource nothing loads.
     */
    @Test
    void aLooseClassInsideAnAarIsIgnored() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-looseclass").toFile();
        File tmp = File.createTempFile("cn1-loose", ".class");
        writePoolClass(tmp, cpUtf8("com/codename1/location/LocationButton"),
                cpOne(7, 1));
        byte[] cls = Files.readAllBytes(tmp.toPath());
        tmp.delete();
        File aar = new File(root, "resources.aar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Loose.class"));
            zip.write(cls);
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertFalse(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "a loose .class in an aar is not on the classpath");
    }

    /** In a plain jar the class entries ARE the classpath, and still count. */
    @Test
    void aClassInsideAPlainJarIsStillRead() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-plainjar").toFile();
        File tmp = File.createTempFile("cn1-plain", ".class");
        writePoolClass(tmp, cpUtf8("com/codename1/location/LocationButton"),
                cpOne(7, 1));
        byte[] cls = Files.readAllBytes(tmp.toPath());
        tmp.delete();
        File jar = new File(root, "ordinary.jar");
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jar));
        try {
            zip.putNextEntry(new ZipEntry("com/example/Uses.class"));
            zip.write(cls);
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments
                .scanForLocationUsage(root).usesButton(),
                "a plain jar's classes are its classpath");
    }
    /** A UTF-16 manifest asks for the permission just as plainly. */
    /** An aar carrying nothing but the given root manifest. */
    private static void writeAar(File at, String manifest) throws Exception {
        at.getParentFile().mkdirs();
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(at));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(manifest.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } finally {
            zip.close();
        }
    }

    @Test
    void anAliasedAndroidNamespaceIsStillTheAndroidNamespace() throws Exception {
        // A submitted aar is somebody else's file, and binding the Android
        // namespace to a prefix of its own is perfectly valid XML. Looking
        // only for the literal "android:" missed the permission it requests --
        // and missing it fails in the dangerous direction: exclusive mode is
        // accepted, and Gradle then merges the background permission in beside
        // onlyForLocationButton.
        File root = tempDir("cn1-lb-ns");
        String manifest = "<manifest xmlns:a=\"http://schemas.android.com/"
                + "apk/res/android\"><uses-permission a:name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\"/></manifest>";
        writeAar(new File(root, "aliased.aar"), manifest);
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "an aar that binds the Android namespace to its own prefix is "
                + "still requesting background location");
    }

    @Test
    void anUnprefixedNameIsNotAnAndroidAttribute() throws Exception {
        // The default namespace does NOT reach attributes: an unprefixed
        // attribute is in no namespace whatever xmlns= says. Reading a bare
        // name= as android:name would refuse builds over an attribute that
        // means nothing to the merger.
        File root = tempDir("cn1-lb-ns-default");
        String manifest = "<manifest xmlns=\"http://schemas.android.com/"
                + "apk/res/android\"><uses-permission name=\"android."
                + "permission.ACCESS_BACKGROUND_LOCATION\"/></manifest>";
        writeAar(new File(root, "default-ns.aar"), manifest);
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "an unprefixed attribute is in no namespace");
    }

    @Test
    void theUriInsideACommentIsNotABinding() throws Exception {
        // The prefix is discovered by reading BACKWARDS from the URI, so an
        // occurrence that is not an xmlns attribute must not create one.
        File root = tempDir("cn1-lb-ns-comment");
        String manifest = "<manifest xmlns:android=\"http://schemas.android"
                + ".com/apk/res/android\">"
                + "<!-- see http://schemas.android.com/apk/res/android -->"
                + "<uses-permission q:name=\"android.permission."
                + "ACCESS_BACKGROUND_LOCATION\"/></manifest>";
        writeAar(new File(root, "commented.aar"), manifest);
        assertFalse(LocationButtonManifestFragments.scanForLocationUsage(root)
                        .declaresBackgroundLocation(),
                "a URI in a comment binds nothing, so q: is just a prefix "
                + "nobody declared");
    }

    @Test
    void aUtf16ManifestIsDecoded() throws Exception {
        File root = Files.createTempDirectory("cn1-lb-utf16").toFile();
        File aar = new File(root, "utf16.aar");
        String xml = "<manifest><uses-permission android:name=\"android"
                + ".permission.ACCESS_BACKGROUND_LOCATION\" /></manifest>";
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(aar));
        try {
            zip.putNextEntry(new ZipEntry("AndroidManifest.xml"));
            zip.write(xml.getBytes("UnicodeBig"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        assertTrue(LocationButtonManifestFragments.scanForLocationUsage(root)
                .declaresBackgroundLocation(),
                "a UTF-16 manifest must be read, not mangled");
    }
}
