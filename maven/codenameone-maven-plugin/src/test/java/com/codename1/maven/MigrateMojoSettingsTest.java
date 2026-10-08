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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The migrate goals start the project's classes from a different main than the server's, so the
 * configuration the server gets compiled in has to reach them another way. This holds what is
 * passed to the forked JVM.
 */
class MigrateMojoSettingsTest {

    @Test
    void passesCn1PropertiesAndPointsAtTheProcessedConfiguration(@TempDir File classes) throws Exception {
        Files.write(new File(classes, "application.properties").toPath(), "cn1.datasource.url=app.db\n".getBytes("UTF-8"));
        Properties given = new Properties();
        given.setProperty("cn1.flyway.baselineVersion", "12");
        given.setProperty("maven.test.skip", "true");
        List<String> options = AbstractMigrateMojo.settings(classes, given.entrySet());
        assertTrue(options.contains("-Dcn1.flyway.baselineVersion=12"), options.toString());
        assertTrue(options.contains("-Dcn1.config.location=" + classes.getAbsolutePath()), options.toString());
        // Nothing that is not the server's own configuration leaks into its JVM.
        assertEquals(2, options.size(), options.toString());
    }

    @Test
    void aGivenConfigurationLocationWins(@TempDir File classes) throws Exception {
        Files.write(new File(classes, "application.properties").toPath(), new byte[0]);
        Properties given = new Properties();
        given.setProperty("cn1.config.location", "/etc/app");
        List<String> options = AbstractMigrateMojo.settings(classes, given.entrySet());
        assertEquals("[-Dcn1.config.location=/etc/app]", options.toString());
    }

    @Test
    void aModuleWithoutAConfigurationFileGetsNoLocation(@TempDir File classes) {
        assertTrue(AbstractMigrateMojo.settings(classes, new Properties().entrySet()).isEmpty());
    }
}
