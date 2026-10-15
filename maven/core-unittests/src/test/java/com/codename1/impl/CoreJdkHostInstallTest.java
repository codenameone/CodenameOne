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
package com.codename1.impl;

import com.codename1.ui.Display;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The core installs its host over the class library's `JdkHost`, which this
/// module compiles from the `Ports/CLDC11` copy -- the copy the core jar is
/// built against.
class CoreJdkHostInstallTest {

    /// Another test in the same JVM may have left a Display running; this
    /// one is about the host with none.
    @BeforeEach
    void withoutADisplay() {
        if (Display.isInitialized()) {
            Display.deinitialize();
        }
        JdkHost.install(null);
    }

    @AfterEach
    void restoreDefaults() {
        JdkHost.install(null);
    }

    @Test
    void installerInstallsTheCoreHost() {
        assertFalse(JdkHost.get() instanceof CoreJdkHost, "nothing installed yet");
        CoreJdkHostInstaller.install();
        assertTrue(JdkHost.get() instanceof CoreJdkHost);
    }

    @Test
    void coreHostWithoutADisplay() {
        CoreJdkHostInstaller.install();
        JdkHost host = JdkHost.get();
        // No Display here: the host must answer without one and never block.
        assertFalse(host.isUiThread());
        final int[] ran = new int[1];
        host.blockUi(new Runnable() {
            @Override
            public void run() {
                ran[0]++;
            }
        });
        assertEquals(1, ran[0], "blockUi off the EDT runs the task in place");
        assertNull(host.property("os.name"));
        assertNull(host.localeNumberSymbols());
    }
}
