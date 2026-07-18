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
package org.cef;

import java.io.File;
import java.util.Arrays;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CN1JcefRuntimeTest {

    @AfterEach
    public void clearProperties() {
        System.clearProperty(CN1JcefRuntime.INSTALL_DIR_PROPERTY);
        System.clearProperty(CN1JcefRuntime.MIRRORS_PROPERTY);
    }

    @Test
    public void usesConfiguredInstallDirectory() throws Exception {
        File installDir = new File("target/test-jcef-install").getAbsoluteFile();
        System.setProperty(CN1JcefRuntime.INSTALL_DIR_PROPERTY, installDir.getPath());

        assertEquals(installDir, CN1JcefRuntime.getInstallDir());
    }

    @Test
    public void parsesConfiguredMirrorsInOrder() {
        System.setProperty(CN1JcefRuntime.MIRRORS_PROPERTY,
                " https://one.invalid/{platform} ,\nhttps://two.invalid/{tag}\r\n");

        assertEquals(Arrays.asList(
                "https://one.invalid/{platform}",
                "https://two.invalid/{tag}"), CN1JcefRuntime.getConfiguredMirrors());
    }

    @Test
    public void rejectsHomeDirectoryAsInstallDirectory() {
        System.setProperty(CN1JcefRuntime.INSTALL_DIR_PROPERTY,
                new File(System.getProperty("user.home")).getAbsolutePath());

        assertThrows(java.io.IOException.class, CN1JcefRuntime::getInstallDir);
    }
}
