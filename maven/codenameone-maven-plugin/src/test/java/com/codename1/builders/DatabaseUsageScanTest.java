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
package com.codename1.builders;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The bundled SQLite engine, and its cipher, ship only for applications that use the database.
 *
 * <p>That promise is what pays for the whole arrangement: the iOS amalgamation replaces the system
 * libsqlite3, the Android AAR carries minSdk 23, and the browser bundle is about 1.5MB. An
 * application that never opens a database must carry none of it.
 *
 * <p>The gate is easy to get wrong in the direction that costs nothing to notice and everything to
 * ship: the tree a builder scans is the application merged with the framework, and
 * {@code Display} alone declares {@code openOrCreate(String, DatabaseConfig)}. A scan that cannot
 * say which class made a reference therefore answers yes for every application ever built, and the
 * gate silently stops gating -- every build still succeeds, just fatter, and on Android with a
 * higher minimum SDK. Only a test that asserts the negative direction catches it.
 */
class DatabaseUsageScanTest {

    /** Executor is abstract; none of these hooks are exercised here. */
    private static final class TestExecutor extends Executor {
        @Override
        protected String getDeviceIdCode() {
            return "";
        }

        @Override
        protected String generatePeerComponentCreationCode(String methodCallString) {
            return "";
        }

        @Override
        protected String convertPeerComponentToNative(String param) {
            return "";
        }

        @Override
        public boolean build(File sourceZip, BuildRequest request) {
            return false;
        }
    }

    private final TestExecutor executor = new TestExecutor();

    private File root;

    @BeforeEach
    void setUp() throws IOException {
        setUpRoot();
    }

    /** A fresh scan root, so a test can run the scan more than once. */
    private void setUpRoot() throws IOException {
        root = File.createTempFile("cn1-db-scan", "");
        assertTrue(root.delete());
        assertTrue(root.mkdirs());
    }

    @AfterEach
    void tearDown() {
        delete(root);
    }

    private static void delete(File f) {
        if (f == null) {
            return;
        }
        File[] children = f.listFiles();
        if (children != null) {
            for (int iter = 0; iter < children.length; iter++) {
                delete(children[iter]);
            }
        }
        f.delete();
    }

    /**
     * Writes a stand-in for a compiled class carrying the given constant-pool strings.
     *
     * The scan is a byte search over the class file, so a file containing the internal names is
     * indistinguishable from a real class that references them, which is the whole of what is
     * under test here.
     */
    /**
     * A class file whose constant pool holds one method reference to DatabaseConfig.
     *
     * Real bytes rather than a string soup, because the question the scanner asks -- which factory
     * is being called -- can only be answered from a constant pool. Just enough of one for the
     * reference to be found: the class, the name and type, and the method reference tying them.
     */
    private void writeClassCalling(String path, String factory) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        OutputStream raw = new FileOutputStream(f);
        try {
            raw.write(classCalling(factory));
        } finally {
            raw.close();
        }
    }

    /** The same bytes, for a caller that puts them somewhere other than a loose file. */
    private byte[] classCalling(String factory) throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
        try {
            out.writeInt(0xCAFEBABE);
            out.writeShort(0);
            out.writeShort(52);
            out.writeShort(7);
            out.writeByte(1);
            out.writeUTF("com/codename1/db/DatabaseConfig");
            out.writeByte(7);
            out.writeShort(1);
            out.writeByte(1);
            out.writeUTF(factory);
            out.writeByte(1);
            out.writeUTF("()Lcom/codename1/db/DatabaseConfig;");
            out.writeByte(12);
            out.writeShort(3);
            out.writeShort(4);
            out.writeByte(10);
            out.writeShort(2);
            out.writeShort(5);
            out.writeShort(0x0021);
            out.writeShort(2);
            out.writeShort(2);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(0);
            out.writeShort(0);
        } finally {
            out.close();
        }
        return bytes.toByteArray();
    }

    @Test
    void anApplicationClassInsideTheFrameworkPackageIsStillScanned() throws IOException {
        // The package is the framework's by convention, not by ownership. Skipping the whole
        // directory made a helper an application or a library put there invisible, so it could
        // configure encryption and the build would still drop the cipher.
        writeClassCalling("com/codename1/db/AppKeyHelper.class", "passphrase");
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "a class there still counts");
        assertTrue(usage.usesDatabaseCipher(), "and its key configuration counts too");
    }

    @Test
    void aPortImplementationCountsWhichIsWhyTheScanRunsBeforeThePortIsStaged() throws IOException {
        // Not a rule so much as the record of one: a port's own database implementation extends
        // Database, so a tree that already contains it answers yes for every application ever
        // built and the gate silently stops gating. Every builder therefore scans the application
        // tree before it merges its port into it. Reverse that order and this is what it sees.
        writeClass("com/codename1/impl/html5/database/DatabaseImpl.class",
                "com/codename1/db/Database");
        assertTrue(executor.scanForDatabaseUsage(root).usesDatabase(),
                "a port implementation is indistinguishable from an application that uses one");
    }

    @Test
    void theFrameworksOwnDatabaseClassesStillDoNotCount() throws IOException {
        writeClass("com/codename1/db/Database.class", "com/codename1/db/DatabaseConfig");
        writeClass("com/codename1/db/ThreadSafeDatabase.class", "com/codename1/db/DatabaseConfig");
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertFalse(usage.usesDatabase(), "the framework referring to itself is not an application");
    }

    @Test
    void aLibraryJarCountsAsUsingTheDatabase() throws IOException {
        // A library can be the only thing that touches the database: the application calls the
        // library and never names Database itself. Android stages the jar into libs and links it,
        // so reading loose class files alone dropped the engine out from under code that runs it.
        File lib = new File(root, "libs");
        assertTrue(lib.mkdirs());
        File jar = new File(lib, "storage.jar");
        java.util.zip.ZipOutputStream zip =
                new java.util.zip.ZipOutputStream(new FileOutputStream(jar));
        try {
            zip.putNextEntry(new java.util.zip.ZipEntry("com/vendor/Storage.class"));
            zip.write(classCalling("passphrase"));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "the library uses the database");
        assertTrue(usage.usesDatabaseCipher(), "and encrypts it");
    }

    @Test
    void anAndroidArchiveCountsThroughItsNestedClassesJar() throws IOException {
        // An AAR keeps its bytecode one level further in, and the generated gradle links it like
        // any other dependency, so encryption configured inside one has to count.
        File lib = new File(root, "libs");
        assertTrue(lib.mkdirs());
        java.io.ByteArrayOutputStream inner = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream innerZip = new java.util.zip.ZipOutputStream(inner);
        try {
            innerZip.putNextEntry(new java.util.zip.ZipEntry("com/vendor/Secure.class"));
            innerZip.write(classCalling("rawKey"));
            innerZip.closeEntry();
        } finally {
            innerZip.close();
        }
        java.util.zip.ZipOutputStream aar = new java.util.zip.ZipOutputStream(
                new FileOutputStream(new File(lib, "secure.aar")));
        try {
            aar.putNextEntry(new java.util.zip.ZipEntry("classes.jar"));
            aar.write(inner.toByteArray());
            aar.closeEntry();
        } finally {
            aar.close();
        }
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "the archive uses the database");
        assertTrue(usage.usesDatabaseCipher(), "and encrypts it");
    }

    @Test
    void anExplicitlyPlainConfigIsNotEncryption() throws IOException {
        // plain() is the documented way to say a database is not encrypted. Reading a reference to
        // DatabaseConfig as encryption charged that application the cipher library and, on
        // Android, every device below API 23 -- for asking for no encryption at all.
        writeClassCalling("com/example/App.class", "plain");
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "it still uses the database");
        assertFalse(usage.usesDatabaseCipher(), "but it configures no key");
    }

    @Test
    void eachEncryptingFactoryCountsAsEncryption() throws IOException {
        String[] factories = {"passphrase", "rawKey", "managed"};
        for (int iter = 0; iter < factories.length; iter++) {
            setUpRoot();
            writeClassCalling("com/example/App.class", factories[iter]);
            Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
            assertTrue(usage.usesDatabaseCipher(), factories[iter] + " configures a key");
        }
    }

    @Test
    void anUnreadableClassIsTreatedAsEncrypting() throws IOException {
        // The safe direction: an application that encrypts must not ship without a cipher because
        // its class file could not be walked.
        writeClass("com/example/App.class", "com/codename1/db/DatabaseConfig");
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabaseCipher(), "unreadable means assume the cipher is needed");
    }

    private void writeClass(String path, String... references) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            for (int iter = 0; iter < references.length; iter++) {
                out.write(references[iter].getBytes("UTF-8"));
            }
        } finally {
            out.close();
        }
    }

    /** Display declares openOrCreate(String, DatabaseConfig), so it names both. */
    private void writeFramework() throws IOException {
        writeClass("com/codename1/ui/Display.class",
                "com/codename1/db/Database", "com/codename1/db/DatabaseConfig");
        writeClass("com/codename1/db/Database.class", "com/codename1/db/DatabaseConfig");
        writeClass("com/codename1/impl/CodenameOneImplementation.class",
                "com/codename1/db/DatabaseConfig");
        writeClass("com/codename1/orm/EntityManager.class", "com/codename1/db/Database");
        writeClass("com/codename1/orm/Dao.class", "com/codename1/db/Database");
        writeClass("com/codename1/properties/SQLMap.class", "com/codename1/db/Database");
        writeClass("com/codename1/properties/SQLMap$SqlType$8.class", "com/codename1/db/Database");
        writeClass("com/codename1/impl/AbstractDBCursor.class", "com/codename1/db/Row");
        writeClass("com/codename1/testing/DatabaseConformanceSuite.class",
                "com/codename1/db/DatabaseConfig");
    }

    @Test
    void anApplicationThatNeverTouchesTheDatabaseGetsNoEngine() throws IOException {
        writeFramework();
        writeClass("com/example/MyApp.class", "com/codename1/ui/Form");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertFalse(usage.usesDatabase(),
                "the framework's own reference is not the application's");
        assertFalse(usage.usesDatabaseCipher(),
                "nor is the framework's reference to DatabaseConfig");
    }

    @Test
    void anApplicationThatOpensADatabaseGetsTheEngineButNotTheCipher() throws IOException {
        writeFramework();
        // Calling Display.openOrCreate(String) puts the return type's descriptor in the caller's
        // constant pool, so an application never naming the package still counts as using it.
        writeClass("com/example/MyApp.class", "com/codename1/db/Database");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase());
        assertFalse(usage.usesDatabaseCipher(), "encryption was never configured");
    }

    @Test
    void anApplicationThatConfiguresEncryptionGetsBoth() throws IOException {
        writeFramework();
        writeClass("com/example/MyApp.class", "com/codename1/db/DatabaseConfig");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "DatabaseConfig is in the database package");
        assertTrue(usage.usesDatabaseCipher());
    }

    @Test
    void aClassInADeeperApplicationPackageStillCounts() throws IOException {
        writeFramework();
        writeClass("com/example/deep/nested/Dao.class", "com/codename1/db/Database");

        assertTrue(executor.scanForDatabaseUsage(root).usesDatabase());
    }

    @Test
    void anApplicationClassUnderAFrameworkNamespaceIsStillScanned() throws IOException {
        // The framework is named class by class rather than package by package. A package says
        // nothing reliable about who wrote a class, and skipping com/codename1/ui wholesale would
        // skip an application or library class living under it -- leaving the engine out of a
        // build that needs it, which fails at runtime rather than here.
        writeFramework();
        writeClass("com/codename1/ui/extensions/Dao.class", "com/codename1/db/DatabaseConfig");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase());
        assertTrue(usage.usesDatabaseCipher());
    }

    @Test
    void reachingTheDatabaseThroughAFacadeCounts() throws IOException {
        // EntityManager and SQLMap exist so an application does not have to name
        // com.codename1.db, so its constant pool holds neither Database nor DatabaseConfig. The
        // engine still has to ship: this is the direction that fails at runtime.
        writeFramework();
        writeClass("com/example/MyApp.class", "com/codename1/orm/EntityManager");
        assertTrue(executor.scanForDatabaseUsage(root).usesDatabase(),
                "an application using the ORM facade is a database user");

        delete(new File(root, "com/example"));
        writeClass("com/example/Other.class", "com/codename1/properties/SQLMap");
        assertTrue(executor.scanForDatabaseUsage(root).usesDatabase(),
                "an application using SQLMap is a database user");
        assertFalse(executor.scanForDatabaseUsage(root).usesDatabaseCipher(),
                "using a facade is not configuring encryption");
    }

    @Test
    void theFacadesOwnReferenceIsStillNotTheApplications() throws IOException {
        // SQLMap and EntityManager reference Database themselves; that alone is the framework
        // talking, exactly as Display is.
        writeFramework();
        writeClass("com/example/MyApp.class", "com/codename1/ui/Form");

        assertFalse(executor.scanForDatabaseUsage(root).usesDatabase());
    }

    @Test
    void anEmptyOrMissingTreeIsNotADatabaseUser() throws IOException {
        assertFalse(executor.scanForDatabaseUsage(root).usesDatabase());
        assertFalse(executor.scanForDatabaseUsage(new File(root, "nope")).usesDatabase());
        assertFalse(executor.scanForDatabaseUsage(null).usesDatabase());
    }
}
