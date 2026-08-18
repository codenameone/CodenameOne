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

    /** A file that starts like a class and then is not one, for the unreadable case. */
    private void writeUnreadableClass(String path) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE,
                0, 0, 0, 52, 0x7F, (byte) 0xFF, 1, 2, 3});
        } finally {
            out.close();
        }
    }

    @Test
    void aStringLiteralNamingTheDatabasePackageIsNotAReference() throws IOException {
        // What reading bytes instead of bytecode could not tell apart. A class whose only mention
        // of com/codename1/db is inside a string -- a log line, a class name it reflects on, a
        // help URL -- referenced nothing, and counting it shipped the engine, and on Android the
        // cipher and an API 23 floor, to an application that never touched a database.
        writeFramework();
        writeClassWithStringLiteral("com/example/Chatty.class",
                "see com/codename1/db/DatabaseConfig for details");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertFalse(usage.usesDatabase(), "a mention in a string is not a use");
        assertFalse(usage.usesDatabaseCipher(), "and certainly not encryption");
    }

    @Test
    void aClassThatOnlyNamesTheConfigTypeIsNotEncrypting() throws IOException {
        // Holding a DatabaseConfig is not configuring a key: the class that decides is the one
        // that calls a factory, and this one may well have been handed a plain() from elsewhere.
        // It still uses the database, so the engine ships -- the cipher does not.
        writeFramework();
        writeClass("com/example/Holder.class", "com/codename1/db/DatabaseConfig");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "it names a database type");
        assertFalse(usage.usesDatabaseCipher(), "but calls no factory that configures a key");
    }

    /** A real class carrying the given text as a constant, and referring to nothing. */
    private void writeClassWithStringLiteral(String path, String literal) throws IOException {
        writeClassWithStringLiteral(org.objectweb.asm.Opcodes.V1_8, path, literal);
    }

    /** The same class, in a class file of the given version. */
    private void writeClassWithStringLiteral(int version, String path, String literal)
            throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        String internalName = path.substring(0, path.length() - ".class".length());
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(version, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internalName, null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "describe", "()Ljava/lang/String;", null, null);
        m.visitCode();
        m.visitLdcInsn(literal);
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 1);
        m.visitEnd();
        w.visitEnd();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }
    }

    /** Writes {@link #classCalling} to a file, for the cases that scan a directory. */
    private void writeClassCalling(String path, String factory) throws IOException {
        writeClassCalling(org.objectweb.asm.Opcodes.V1_8, path, factory);
    }

    /** The same file, in a class file of the given version. */
    private void writeClassCalling(int version, String path, String factory) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        OutputStream raw = new FileOutputStream(f);
        try {
            raw.write(classCalling(version, factory));
        } finally {
            raw.close();
        }
    }

    /**
     * A real class with a real call to one of DatabaseConfig's factories.
     *
     * Emitted with ASM rather than by hand-writing a constant pool. The scan reads bytecode, so
     * what it needs to see is a call site: which factory is invoked is the whole question, and a
     * pool entry alone cannot answer it -- DatabaseConfig.plain() and DatabaseConfig.passphrase()
     * put the identical class name there.
     */
    private byte[] classCalling(String factory) throws IOException {
        return classCalling(org.objectweb.asm.Opcodes.V1_8, factory);
    }

    /** The same call site, in a class file of the given version. */
    private byte[] classCalling(int version, String factory) throws IOException {
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(version, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "com/example/Caller", null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "configure", "()V", null, null);
        m.visitCode();
        m.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKESTATIC,
                "com/codename1/db/DatabaseConfig", factory,
                "()Lcom/codename1/db/DatabaseConfig;", false);
        m.visitInsn(org.objectweb.asm.Opcodes.POP);
        m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        m.visitMaxs(1, 1);
        m.visitEnd();
        w.visitEnd();
        return w.toByteArray();
    }

    @Test
    void anUnreadableNestedArchiveDoesNotHideTheClassesAfterIt() throws IOException {
        // A name ending in .jar need not be an archive: a truncated or opaque resource carries that
        // name perfectly well. Letting the failure out abandoned the rest of a library that was
        // otherwise fine, and what it dropped is exactly what this scan exists to find -- silently,
        // because the build then prunes the cipher from an application that needs it and the
        // failure appears on the device.
        File lib = new File(root, "libs");
        assertTrue(lib.mkdirs() || lib.isDirectory(), "the library directory has to exist");
        File jar = new File(lib, "vendor.jar");
        java.util.zip.ZipOutputStream zip =
                new java.util.zip.ZipOutputStream(new FileOutputStream(jar));
        try {
            // Ordered deliberately: the unreadable entry comes first, so a scan that stops on it
            // never reaches the class that matters.
            zip.putNextEntry(new java.util.zip.ZipEntry("assets/truncated.jar"));
            zip.write(truncatedArchive());
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("com/vendor/Storage.class"));
            zip.write(classCalling("passphrase"));
            zip.closeEntry();
        } finally {
            zip.close();
        }

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "the class after the unreadable entry still counts");
        assertTrue(usage.usesDatabaseCipher(), "and its encryption still counts");
    }

    /**
     * The bytes of an archive that begins well and stops in the middle of an entry.
     *
     * <p>Garbage would not do: a zip reader given bytes that are not an archive at all reports no
     * entries and raises nothing, so it would exercise none of this. What fails is data that
     * starts as an archive and runs out -- the reader accepts the header, begins inflating, and
     * meets the end of the stream.
     */
    private byte[] truncatedArchive() throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        java.util.zip.ZipOutputStream inner = new java.util.zip.ZipOutputStream(bytes);
        try {
            inner.putNextEntry(new java.util.zip.ZipEntry("com/vendor/Big.class"));
            byte[] filler = new byte[4096];
            for (int i = 0; i < filler.length; i++) {
                filler[i] = (byte) (i % 251);
            }
            inner.write(filler);
            inner.closeEntry();
        } finally {
            inner.close();
        }
        byte[] whole = bytes.toByteArray();
        byte[] cut = new byte[whole.length / 2];
        System.arraycopy(whole, 0, cut, 0, cut.length);
        return cut;
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
        // its class file could not be walked. Genuinely unreadable bytes -- a header and then
        // nothing that parses -- since the other fixtures here now emit real classes.
        writeUnreadableClass("com/example/App.class");
        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabaseCipher(), "unreadable means assume the cipher is needed");
    }

    /**
     * Writes a real class that refers to the given internal names.
     *
     * Emitted with ASM. The names go in as things the class actually uses -- a field of that
     * type and a method that instantiates it -- because the scan reads bytecode, and a file that
     * merely contains the characters is exactly what it must not count: a string literal
     * mentioning com/codename1/db is not a database reference, and one of these fixtures used to
     * be indistinguishable from one.
     */
    private void writeClass(String path, String... references) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        String internalName = path.substring(0, path.length() - ".class".length());
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internalName, null, "java/lang/Object", null);
        for (int iter = 0; iter < references.length; iter++) {
            w.visitField(org.objectweb.asm.Opcodes.ACC_PRIVATE, "ref" + iter,
                    "L" + references[iter] + ";", null, null).visitEnd();
        }
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "use", "()V", null, null);
        m.visitCode();
        for (int iter = 0; iter < references.length; iter++) {
            m.visitTypeInsn(org.objectweb.asm.Opcodes.NEW, references[iter]);
            m.visitInsn(org.objectweb.asm.Opcodes.POP);
        }
        m.visitInsn(org.objectweb.asm.Opcodes.RETURN);
        m.visitMaxs(2, 1);
        m.visitEnd();
        w.visitEnd();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(w.toByteArray());
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
        writeClassCalling("com/example/MyApp.class", "passphrase");

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
        writeClassCalling("com/codename1/ui/extensions/Dao.class", "managed");

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
    /**
     * Java 17 bytecode, which applications are routinely compiled to.
     *
     * The answers must not depend on the class file version. They do the moment the ASM in use
     * stops short of what the toolchain emits, because an unreadable class counts as encrypting --
     * so this is the test that says this ASM has not fallen behind.
     */
    private static final int MODERN_CLASS_FILE = 61;

    @Test
    void aModernClassIsReadLikeAnyOther() throws IOException {
        writeClassCalling(MODERN_CLASS_FILE, "com/example/App.class", "passphrase");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "it opens a database");
        assertTrue(usage.usesDatabaseCipher(), "and it configures a key");
    }

    @Test
    void aModernClassThatNeverTouchesTheDatabaseGetsNothing() throws IOException {
        writeFramework();
        writeClassWithStringLiteral(MODERN_CLASS_FILE, "com/example/Chatty.class",
                "see com/codename1/db/DatabaseConfig for details");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertFalse(usage.usesDatabase(), "a mention in a string is not a use");
        assertFalse(usage.usesDatabaseCipher(), "and certainly not encryption");
    }

    @Test
    void aMethodReferenceToAFactoryIsEncryption() throws IOException {
        // Supplier<DatabaseConfig> keys = DatabaseConfig::managed. There is no call instruction:
        // the factory travels as a method handle in an invokedynamic's bootstrap arguments, so a
        // scan that only reads call sites reports no encryption and the build drops the cipher --
        // on Android by deleting the package outright. The application then fails to open its own
        // database on the device.
        writeClassReferencing("com/example/App.class", "managed");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "it names the config type");
        assertTrue(usage.usesDatabaseCipher(), "and a method reference configures a key");
    }

    @Test
    void aMethodReferenceToPlainIsStillNotEncryption() throws IOException {
        // The same shape, and the same distinction the call site path draws.
        writeClassReferencing("com/example/App.class", "plain");

        Executor.DatabaseUsage usage = executor.scanForDatabaseUsage(root);
        assertTrue(usage.usesDatabase(), "it names the config type");
        assertFalse(usage.usesDatabaseCipher(), "plain() configures no key");
    }

    /** A class that captures DatabaseConfig::factory as a lambda, rather than calling it. */
    private void writeClassReferencing(String path, String factory) throws IOException {
        File f = new File(root, path.replace('/', File.separatorChar));
        assertTrue(f.getParentFile().isDirectory() || f.getParentFile().mkdirs());
        String internalName = path.substring(0, path.length() - ".class".length());
        org.objectweb.asm.ClassWriter w = new org.objectweb.asm.ClassWriter(0);
        w.visit(org.objectweb.asm.Opcodes.V1_8, org.objectweb.asm.Opcodes.ACC_PUBLIC,
                internalName, null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor m = w.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC,
                "keys", "()Ljava/util/concurrent/Callable;", null, null);
        m.visitCode();
        org.objectweb.asm.Handle metafactory = new org.objectweb.asm.Handle(
                org.objectweb.asm.Opcodes.H_INVOKESTATIC,
                "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;",
                false);
        org.objectweb.asm.Handle target = new org.objectweb.asm.Handle(
                org.objectweb.asm.Opcodes.H_INVOKESTATIC,
                "com/codename1/db/DatabaseConfig", factory,
                "()Lcom/codename1/db/DatabaseConfig;", false);
        m.visitInvokeDynamicInsn("call", "()Ljava/util/concurrent/Callable;", metafactory,
                org.objectweb.asm.Type.getType("()Ljava/lang/Object;"),
                target,
                org.objectweb.asm.Type.getType("()Lcom/codename1/db/DatabaseConfig;"));
        m.visitInsn(org.objectweb.asm.Opcodes.ARETURN);
        m.visitMaxs(1, 1);
        m.visitEnd();
        w.visitEnd();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(w.toByteArray());
        } finally {
            out.close();
        }
    }

}
