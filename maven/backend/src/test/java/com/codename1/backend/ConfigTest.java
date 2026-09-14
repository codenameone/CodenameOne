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
package com.codename1.backend;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The layering that lets one binary run against SQLite on a laptop and
 * PostgreSQL in production.
 *
 * <p>The environment layer cannot be exercised from inside a JVM -- there is no
 * way to set a variable for the current process -- so what is tested here is
 * everything around it: the system property layer above it, the two file layers
 * below it, the profile that chooses one of those files, and the expansion that
 * resolves a reference when the value is read rather than when the file is
 * loaded.
 */
class ConfigTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty(Config.PROFILE);
        System.clearProperty(Config.SERVER_PORT);
        System.clearProperty(Config.DATASOURCE_URL);
        System.clearProperty("cn1.test.value");
    }

    @Test
    @DisplayName("the profile file wins over the base file")
    void profileOverridesBase(@TempDir File dir) throws Exception {
        write(dir, "application.properties",
                "cn1.datasource.url=postgres://app@db/app\ncn1.server.port=8080\n");
        write(dir, "application-dev.properties", "cn1.datasource.url=:memory:\n");
        System.setProperty(Config.PROFILE, "dev");
        Config config = Config.load(dir.getAbsolutePath());
        assertEquals("dev", config.getProfile());
        assertEquals(":memory:", config.get(Config.DATASOURCE_URL));
        // And a key the profile file does not mention still comes from the base.
        assertEquals(8080, config.getInt(Config.SERVER_PORT, 0));
    }

    @Test
    @DisplayName("the base file can name the default profile")
    void theBaseFileChoosesTheProfile(@TempDir File dir) throws Exception {
        // So a project whose default is development says so once, in a file,
        // rather than in every developer's shell.
        write(dir, "application.properties", "cn1.profile=dev\n");
        write(dir, "application-dev.properties", "cn1.datasource.url=:memory:\n");
        Config config = Config.load(dir.getAbsolutePath());
        assertEquals("dev", config.getProfile());
        assertEquals(":memory:", config.get(Config.DATASOURCE_URL));
    }

    @Test
    @DisplayName("a system property wins over both files")
    void systemPropertyWins(@TempDir File dir) throws Exception {
        write(dir, "application.properties", "cn1.server.port=8080\n");
        System.setProperty(Config.SERVER_PORT, "9999");
        assertEquals(9999, Config.load(dir.getAbsolutePath()).getInt(Config.SERVER_PORT, 0));
    }

    @Test
    @DisplayName("no files at all is a configuration, not a failure")
    void missingFilesAreNotAnError(@TempDir File dir) throws Exception {
        // The normal shape of a FROM scratch container: there is nothing next to
        // the binary because there is nothing next to the binary.
        Config config = Config.load(dir.getAbsolutePath());
        assertEquals("default", config.getProfile());
        assertNull(config.get(Config.DATASOURCE_URL));
        assertEquals(8080, config.getInt(Config.SERVER_PORT, 8080));
        assertTrue(config.describe().contains("no properties file"), config.describe());
    }

    @Test
    @DisplayName("a reference is resolved when the value is read, not when it is loaded")
    void expandsReferencesLazily(@TempDir File dir) throws Exception {
        // The committed application.properties names a variable only production
        // sets. Resolving at load time would make the dev profile -- which
        // overrides the key and never reads it -- fail to start.
        write(dir, "application.properties", "cn1.datasource.url=${CN1_NOT_SET_ANYWHERE}\n");
        write(dir, "application-dev.properties", "cn1.datasource.url=:memory:\n");
        System.setProperty(Config.PROFILE, "dev");
        assertEquals(":memory:", Config.load(dir.getAbsolutePath()).get(Config.DATASOURCE_URL));
    }

    @Test
    @DisplayName("an unresolved reference is an error rather than a literal")
    void refusesAnUnresolvedReference(@TempDir File dir) throws Exception {
        // Left alone, the server opens a SQLite file called "${DATABASE_URL}",
        // which succeeds and is empty.
        write(dir, "application.properties", "cn1.datasource.url=${CN1_NOT_SET_ANYWHERE}\n");
        Config config = Config.load(dir.getAbsolutePath());
        IOException err = assertThrows(IOException.class,
                () -> config.get(Config.DATASOURCE_URL));
        assertTrue(err.getMessage().contains("CN1_NOT_SET_ANYWHERE"), err.getMessage());
    }

    @Test
    @DisplayName("a malformed value is reported without printing the value")
    void doesNotPrintTheValueItCannotParse(@TempDir File dir) throws Exception {
        // The value this is reached with is nearly always the datasource URL, and
        // that carries a password. An uncaught start-up failure prints the
        // message into a deployment log, which would undo the care describe()
        // takes about exactly the same string.
        write(dir, "application.properties",
                "cn1.datasource.url=postgres://app:hunter2@db/app${unclosed\n");
        Config config = Config.load(dir.getAbsolutePath());
        IOException err = assertThrows(IOException.class,
                () -> config.get(Config.DATASOURCE_URL));
        assertFalse(err.getMessage().contains("hunter2"), err.getMessage());
        assertTrue(err.getMessage().contains(Config.DATASOURCE_URL), err.getMessage());
        assertTrue(err.getMessage().contains("index"), err.getMessage());
    }

    @Test
    @DisplayName("a reference can carry a fallback, and can name another key")
    void expandsWithFallbacksAndNestedKeys(@TempDir File dir) throws Exception {
        write(dir, "application.properties",
                "cn1.test.host=db.internal\n"
                        + "cn1.datasource.url=postgres://app@${cn1.test.host}/${CN1_UNSET:app}\n");
        assertEquals("postgres://app@db.internal/app",
                Config.load(dir.getAbsolutePath()).get(Config.DATASOURCE_URL));
    }

    @Test
    @DisplayName("a reference that leads back to itself is reported, not overflowed")
    void refusesACycle(@TempDir File dir) throws Exception {
        write(dir, "application.properties", "a=${b}\nb=${a}\n");
        Config config = Config.load(dir.getAbsolutePath());
        IOException err = assertThrows(IOException.class, () -> config.get("a"));
        assertTrue(err.getMessage().contains("refer to each other"), err.getMessage());
    }

    @Test
    @DisplayName("a number or a flag that is neither is refused")
    void refusesMalformedValues(@TempDir File dir) throws Exception {
        write(dir, "application.properties", "cn1.server.port=eighty\ncn1.orm.createTables=ture\n");
        Config config = Config.load(dir.getAbsolutePath());
        assertThrows(IOException.class, () -> config.getInt(Config.SERVER_PORT, 0));
        // "ture" is a setting somebody believes is on, so it is not false.
        assertThrows(IOException.class, () -> config.getBoolean(Config.ORM_CREATE_TABLES, false));
    }

    @Test
    @DisplayName("the spellings a flag accepts")
    void readsFlags(@TempDir File dir) throws Exception {
        write(dir, "application.properties",
                "a=true\nb=YES\nc=on\nd=1\ne=false\nf=No\ng=off\nh=0\n");
        Config config = Config.load(dir.getAbsolutePath());
        assertTrue(config.getBoolean("a", false));
        assertTrue(config.getBoolean("b", false));
        assertTrue(config.getBoolean("c", false));
        assertTrue(config.getBoolean("d", false));
        assertFalse(config.getBoolean("e", true));
        assertFalse(config.getBoolean("f", true));
        assertFalse(config.getBoolean("g", true));
        assertFalse(config.getBoolean("h", true));
    }

    @Test
    @DisplayName("a key maps to the conventional environment variable name")
    void mapsKeysToEnvironmentNames() {
        // Folded by hand rather than with toUpperCase, which is locale sensitive:
        // on a Turkish device the i of cn1 folds to a dotted capital and the
        // variable the deployment set is never found.
        assertEquals("CN1_DATASOURCE_URL", Config.environmentName(Config.DATASOURCE_URL));
        assertEquals("CN1_SERVER_TLS_CERTIFICATE", Config.environmentName(Config.TLS_CERTIFICATE));
        assertEquals("CN1_A_B", Config.environmentName("cn1.a-b"));
    }

    @Test
    @DisplayName("the development profiles are the ones that get a default database")
    void knowsDevelopmentProfiles() {
        assertTrue(Config.of(new Properties(), "dev").isDevelopmentProfile());
        assertTrue(Config.of(new Properties(), "TEST").isDevelopmentProfile());
        assertTrue(Config.of(new Properties(), "local").isDevelopmentProfile());
        assertFalse(Config.of(new Properties(), "default").isDevelopmentProfile());
        assertFalse(Config.of(new Properties(), "production").isDevelopmentProfile());
    }

    @Test
    @DisplayName("what was read is described without any value in it")
    void describesWithoutLeakingValues(@TempDir File dir) throws Exception {
        // A start-up line is the easiest way for a password to reach a log
        // aggregator, and the datasource URL carries one.
        write(dir, "application.properties",
                "cn1.datasource.url=postgres://app:hunter2@db/app\n");
        String described = Config.load(dir.getAbsolutePath()).describe();
        assertFalse(described.contains("hunter2"), described);
        assertTrue(described.contains("application.properties"), described);
    }

    @Test
    @DisplayName("values are read as UTF-8 on both runtimes")
    void readsUtf8(@TempDir File dir) throws Exception {
        // java.util.Properties.load(InputStream) is ISO-8859-1 in the JDK and
        // UTF-8 in the server runtime's own class library, so a password with an
        // accent in it would be a different password in the development loop than
        // in the binary that ships. The reader is given the encoding instead.
        byte[] utf8 = "cn1.test.value=p\u00e4ssword\n".getBytes("UTF-8");
        OutputStream out = new FileOutputStream(new File(dir, "application.properties"));
        try {
            out.write(utf8);
        } finally {
            out.close();
        }
        assertEquals("p\u00e4ssword", Config.load(dir.getAbsolutePath()).get("cn1.test.value"));
    }

    private static void write(File dir, String name, String content) throws IOException {
        OutputStream out = new FileOutputStream(new File(dir, name));
        try {
            out.write(content.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    @Test
    @DisplayName("a configuration file that exists and cannot be read is not an absent one")
    void anUnreadableFileIsRefused(@TempDir File dir) throws Exception {
        File properties = new File(dir, "application.properties");
        write(dir, "application.properties", "cn1.server.port=8080\n");
        // Proven unreadable before anything is asserted: root ignores the
        // permission bits, and a check that cannot fail is no check.
        if(!properties.setReadable(false, false) || properties.canRead()) {
            System.out.println("SKIPPING the unreadable-file check: this user can read a "
                    + "file with no read permission, which is what root does");
            return;
        }
        try {
            IOException err = assertThrows(IOException.class,
                    () -> Config.load(dir.getAbsolutePath()));
            // Named, because the operator has to know WHICH file to fix.
            assertTrue(err.getMessage().indexOf("application.properties") >= 0,
                    err.getMessage());
            // And the file that is simply absent is still fine, which is the
            // distinction being made.
            assertNotNull(Config.load(new File(dir, "empty").getAbsolutePath()));
        } finally {
            properties.setReadable(true, true);
        }
    }
}
