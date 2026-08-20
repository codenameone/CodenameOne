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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.db.Database;
import com.codename1.db.DatabaseConfig;
import com.codename1.testing.DatabaseConformanceSuite;

/** Encrypted opens, wrong keys, re-keying and the bytes on disk. */
public class DatabaseEncryptionTest extends DatabaseConformanceTest {

    /**
     * Makes this application a user of the encryption API, which is what the builders gate the
     * native payload on.
     *
     * The gate scans the application's own classes for a reference to DatabaseConfig. Everything
     * this test actually does goes through DatabaseConformanceSuite, which lives in the core jar
     * and is therefore not scanned -- so before this, the suite ran on a build with no cipher in
     * it, Database.isEncryptionSupported() answered false, and the whole group skipped. The
     * encryption path was shipping untested on Android as a result.
     *
     * Reading a field of a config is enough to create the reference and cheap enough to run.
     */
    private static boolean referenceTheEncryptionApi() {
        return DatabaseConfig.passphrase("gate").isEncrypted();
    }

    @Override
    protected String testName() {
        return "DatabaseEncryptionTest";
    }

    @Override
    protected int mode() {
        return DatabaseConformanceSuite.MODE_STRICT;
    }

    @Override
    protected void runGroup(final int mode, final DatabaseConformanceSuite.Reporter reporter)
            throws Exception {
        reporter.info("encryption api referenced=" + referenceTheEncryptionApi());
        DatabaseConformanceSuite.runEncryption("cn1-conformance-enc.db", mode, reporter);
    }
}
