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
package com.codename1.impl.javase.bluetooth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Source-scan guards (in the style of
 * {@code LocationSimulationTest.testLocationSimulationHasNoJavaFxImports}):
 * the simulated Bluetooth stack must stay headless. No Swing/JavaFX
 * anywhere in the package, and the deterministic core
 * ({@code SimulatedBluetoothStack}, {@code ManualScheduler}) must never
 * touch wall-clock time or sleep -- time flows only through the scheduler.
 */
public class BluetoothSimulationHeadlessTest {

    private static final String PACKAGE_DIR =
            "../../Ports/JavaSE/src/com/codename1/impl/javase/bluetooth";

    /** The pure-Java stack core that must not import Codename One UI. */
    private static final String[] LAYER_A_SOURCES = {
            "SimScheduler.java",
            "AutoScheduler.java",
            "ManualScheduler.java",
            "SimulatedBluetoothStack.java",
            "VirtualPeripheral.java",
            "VirtualService.java",
            "VirtualCharacteristic.java",
            "VirtualDescriptor.java",
            "VirtualCentral.java",
            "SimStreamChannel.java",
            "SimStreamHandler.java",
            "StackEventListener.java",
            "ByteArrays.java"
    };

    private static File packageDir() {
        File dir = new File(PACKAGE_DIR);
        Assertions.assertTrue(dir.isDirectory(),
                "Could not find the bluetooth package sources at "
                        + dir.getAbsolutePath());
        return dir;
    }

    private static String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(f));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            br.close();
        }
        return sb.toString();
    }

    @Test
    public void packageHasNoSwingOrJavaFxImports() throws Exception {
        File[] sources = packageDir().listFiles(
                (dir, name) -> name.endsWith(".java"));
        Assertions.assertNotNull(sources);
        Assertions.assertTrue(sources.length >= 20,
                "expected the full bluetooth package, found "
                        + sources.length + " sources");
        for (File src : sources) {
            String content = read(src);
            Assertions.assertFalse(content.contains("javax.swing"),
                    src.getName() + " must not use Swing");
            Assertions.assertFalse(content.contains("javafx."),
                    src.getName() + " must not use JavaFX");
        }
    }

    @Test
    public void deterministicCoreNeverTouchesWallClockTime()
            throws Exception {
        for (String name : new String[] {"SimulatedBluetoothStack.java",
                "ManualScheduler.java"}) {
            File src = new File(packageDir(), name);
            Assertions.assertTrue(src.exists(), "missing " + name);
            String content = read(src);
            Assertions.assertFalse(content.contains("Thread.sleep"),
                    name + " must not sleep -- time flows only through the"
                            + " scheduler");
            Assertions.assertFalse(
                    content.contains("System.currentTimeMillis"),
                    name + " must not read wall-clock time -- time flows"
                            + " only through the scheduler");
            Assertions.assertFalse(content.contains("System.nanoTime"),
                    name + " must not read wall-clock time -- time flows"
                            + " only through the scheduler");
        }
    }

    @Test
    public void stackCoreStaysFreeOfCodenameOneUi() throws Exception {
        for (String name : LAYER_A_SOURCES) {
            File src = new File(packageDir(), name);
            Assertions.assertTrue(src.exists(), "missing " + name);
            String content = read(src);
            Assertions.assertFalse(
                    content.contains("import com.codename1.ui"),
                    name + " is stack core and must not depend on"
                            + " com.codename1.ui");
        }
    }
}
