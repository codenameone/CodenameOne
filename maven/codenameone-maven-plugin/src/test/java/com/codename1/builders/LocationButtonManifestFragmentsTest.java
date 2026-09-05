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
        assertFalse(out.contains("android:maxSdkVersion"), out);
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
        assertFalse(out.contains("maxSdkVersion"),
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
        assertFalse(out.contains("maxSdkVersion"),
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
        assertFalse(out.contains("android:maxSdkVersion"), out);
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
                true, false, false));
        assertNull(LocationButtonManifestFragments.exclusiveConflict(
                false, true, true));
        assertNotNull(LocationButtonManifestFragments.exclusiveConflict(
                true, true, false));
        assertNotNull(LocationButtonManifestFragments.exclusiveConflict(
                true, false, true));
        assertTrue(LocationButtonManifestFragments.exclusiveConflict(
                true, true, true).contains("android.locationButton.exclusive"));
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
                true, false, true),
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
                true, false, true);
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
        assertFalse(out.indexOf("remove") > -1,
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
     * An anonymous inner class still is. A Java identifier cannot begin with a
     * digit, so $1 is only ever the compiler's.
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
        assertFalse(out.indexOf("remove") > -1,
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
        assertFalse(out.indexOf("remove") > -1, out);
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
