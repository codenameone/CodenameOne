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

import com.codename1.build.shared.PlatformFeatureCatalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A library can be the only thing that uses the nearby packages.
 *
 * <p>The class scanner behind the feature flags reads loose {@code .class}
 * files and never opens a jar, so an application that calls a library which
 * calls {@code NearbyTransport} names no nearby class itself and left every
 * flag false. Android then deleted the implementation package out of the
 * generated sources and iOS left the native defines off, so the library
 * called into classes the build had removed.</p>
 */
public class NearbyLibraryScanTest {

    /**
     * A stand-in class file.
     *
     * <p>Not real bytecode, and it does not need to be: the scan is a
     * search of the whole file for the package name, which is how every
     * constant pool stores a reference to a class in it.</p>
     */
    private static byte[] classBytes(String reference) {
        return reference.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void writeJar(File jar, String entry, byte[] body)
            throws Exception {
        OutputStream raw = new FileOutputStream(jar);
        ZipOutputStream out = new ZipOutputStream(raw);
        try {
            out.putNextEntry(new ZipEntry(entry));
            out.write(body);
            out.closeEntry();
        } finally {
            out.close();
        }
    }

    @Test
    public void aTransportReferenceInsideAJarCounts(@TempDir File dir)
            throws Exception {
        writeJar(new File(dir, "mylib.jar"), "com/acme/Wrapper.class",
                classBytes("com/codename1/nearby/transport/NearbyTransport"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.usesTransport(), "a jar entry naming the transport"
                + " package must count as transport use");
        assertFalse(usage.usesRanging(),
                "nothing named the ranging package");
        assertFalse(usage.isEmpty(), "the scan found something");
    }

    @Test
    public void aNestedClassesJarInsideAnAarCounts(@TempDir File dir)
            throws Exception {
        File inner = new File(dir, "inner.jar");
        writeJar(inner, "com/acme/Ranger.class",
                classBytes("com/codename1/nearby/ranging/Ranging"));
        byte[] innerBytes = Files.readAllBytes(inner.toPath());
        assertTrue(inner.delete(), "the staging jar is removed so only the"
                + " archive under test is scanned");
        writeJar(new File(dir, "mylib.aar"), "classes.jar", innerBytes);
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.usesRanging(), "an Android archive keeps its"
                + " bytecode one level further in");
    }

    /**
     * Presence is a call, so the marker is the method name -- and the
     * cleanup call must not match it, for the reason
     * {@code NearbyPresenceScanTest} gives.
     */
    @Test
    public void onlyTheStartCallCountsAsPresence(@TempDir File dir)
            throws Exception {
        writeJar(new File(dir, "stopper.jar"), "com/acme/Stopper.class",
                classBytes("com/codename1/nearby/companion/CompanionDevices"
                        + "stopObservingPresence"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.usesCompanion(), "it does associate");
        assertFalse(usage.usesPresence(),
                "stopObservingPresence is cleanup, not observation");
    }

    /**
     * The framework's own classes are not evidence about the application.
     * A staged framework jar naming these packages would otherwise report
     * every application as using all of them.
     */
    @Test
    public void theFrameworksOwnClassesDoNotCount(@TempDir File dir)
            throws Exception {
        writeJar(new File(dir, "cn1.jar"),
                "com/codename1/nearby/transport/NearbyTransport.class",
                classBytes("com/codename1/nearby/transport/Endpoint"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.isEmpty(),
                "the API's own classes say nothing about the application");
    }

    @Test
    public void anUnreadableArchiveIsNotUsage(@TempDir File dir)
            throws Exception {
        Files.write(new File(dir, "broken.jar").toPath(),
                "not an archive".getBytes(StandardCharsets.ISO_8859_1));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.isEmpty(), "a file that cannot be read must not"
                + " charge the whole apparatus to the application");
    }

    @Test
    public void nothingIsFoundInAnEmptyTree(@TempDir File dir) {
        assertTrue(NearbyManifestFragments.scanForNearbyUsage(dir).isEmpty());
        assertTrue(NearbyManifestFragments.scanForNearbyUsage(null).isEmpty(),
                "a null root is answered rather than thrown at");
    }

    /**
     * The catalog answers to the prefixes the builders feed it.
     *
     * <p>The library scan has no class names to consume -- it works from a
     * search of the whole file, not a resolved reference -- so it feeds the
     * catalog its entry prefix. Nothing else checks that the two agree, and
     * a renamed package would silently stop supplying the dependency, the
     * framework and the minimum SDK while the feature flags stayed on: a
     * build that keeps AndroidUwbRanging.java with nothing to compile it
     * against, and enables the iOS defines with nothing to link.</p>
     */
    @Test
    public void theCatalogAnswersToTheBuildersPrefixes() {
        String[] prefixes = {
            "com/codename1/nearby/ranging/",
            "com/codename1/nearby/transport/",
            "com/codename1/nearby/companion/",
        };
        for (int i = 0; i < prefixes.length; i++) {
            PlatformFeatureCatalog.Accumulator acc =
                    new PlatformFeatureCatalog.Accumulator();
            acc.consume(prefixes[i]);
            assertFalse(acc.hits().isEmpty(),
                    "the catalog must have an entry for " + prefixes[i]
                    + "; the library scan consumes exactly this string");
        }
    }

    /**
     * A class file carrying one Methodref: {@code owner.method()}.
     *
     * <p>Only the constant pool is read, so only the constant pool is
     * built. Hand-assembled because the point of the test is that the
     * owner and the method name are tied together, which is exactly what a
     * flat byte search cannot see.</p>
     */
    private static byte[] callingClass(String owner, String method) {
        java.io.ByteArrayOutputStream out =
                new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream d = new java.io.DataOutputStream(out);
        try {
            d.writeInt(0xCAFEBABE);
            d.writeShort(0);
            d.writeShort(52);
            // 1 owner utf8, 2 method utf8, 3 descriptor utf8, 4 Class,
            // 5 NameAndType, 6 Methodref -- so a count of 7.
            d.writeShort(7);
            d.writeByte(1);
            d.writeUTF(owner);
            d.writeByte(1);
            d.writeUTF(method);
            d.writeByte(1);
            d.writeUTF("(Ljava/lang/String;)Z");
            d.writeByte(7);
            d.writeShort(1);
            d.writeByte(12);
            d.writeShort(2);
            d.writeShort(3);
            d.writeByte(10);
            d.writeShort(4);
            d.writeShort(5);
            d.flush();
        } catch (java.io.IOException never) {
            throw new IllegalStateException(never);
        }
        return out.toByteArray();
    }

    @Test
    public void presenceNeedsTheCallToBeOnTheFacade(@TempDir File dir)
            throws Exception {
        writeJar(new File(dir, "lib.jar"), "com/acme/Watcher.class",
                callingClass("com/codename1/nearby/companion/CompanionDevices",
                        "startObservingPresence"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.usesPresence(),
                "a call to the facade's startObservingPresence is presence");
        assertTrue(usage.usesCompanion(),
                "and naming the class is companion use");
    }

    @Test
    public void someoneElsesMethodOfThatNameIsNotPresence(@TempDir File dir)
            throws Exception {
        writeJar(new File(dir, "lib.jar"), "com/acme/Watcher.class",
                callingClass("com/acme/OwnPresence",
                        "startObservingPresence"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertFalse(usage.usesPresence(), "the name alone is not the API:"
                + " charging an app the exported service and the background"
                + " permissions for a library's own method is a"
                + " store-review conversation");
        assertTrue(usage.isEmpty(), "and nothing else was named either");
    }

    @Test
    public void anUnreadableClassNeverClaimsPresence(@TempDir File dir)
            throws Exception {
        // The package fallback still applies -- keeping an implementation
        // that might be needed costs bytes -- but presence does not fall
        // back, because being wrong there costs permissions.
        writeJar(new File(dir, "lib.jar"), "com/acme/Odd.class",
                classBytes("com/codename1/nearby/companion/CompanionDevices"
                        + "startObservingPresence"));
        NearbyManifestFragments.NearbyUsage usage =
                NearbyManifestFragments.scanForNearbyUsage(dir);
        assertTrue(usage.usesCompanion(), "the package fallback still reads");
        assertFalse(usage.usesPresence(),
                "an unreadable class says nothing about presence");
    }
}
