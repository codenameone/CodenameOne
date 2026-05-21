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
package com.codename1.impl.javase.simulator;

import com.codename1.system.SimulatorHookExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parse-level coverage for {@link SimulatorHookLoader}.
 *
 * <p>The contract cn1libs depend on: items are positional ({@code item1},
 * {@code item2}, ...), the loop stops at the first missing index, labels
 * are optional (API-only), and the resulting executor keys are exactly
 * {@code namespace:itemN}. JavaSE-side {@code Display.execute} intercepts
 * those keys and routes them to the hook.</p>
 *
 * <p>The {@code Display.callSeriallyAndWait} dispatch wrapper inside each
 * Runnable is intentionally not exercised here (would require a running
 * Display); the resolved {@code Method} is checked indirectly by relying
 * on the loader to skip entries with unresolvable or non-static targets.</p>
 */
class SimulatorHookLoaderTest {

    @Test
    void parsesWellFormedFile(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=Alpha\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#beta\n"
                + "label2=Beta\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(2, hooks.size());
        SimulatorHook first = hooks.get(0);
        assertEquals("Bluetooth", first.getMenuName());
        assertEquals("bluetooth", first.getNamespace(), "namespace should default to slugified name");
        assertEquals(1, first.getIndex());
        assertEquals("Alpha", first.getLabel());
        assertEquals("bluetooth:item1", first.getExecutorKey());
        assertTrue(first.hasMenuLabel());
        assertNotNull(first.getInvoke());
        assertEquals(2, hooks.get(1).getIndex());
        assertEquals("Beta", hooks.get(1).getLabel());
        assertEquals("bluetooth:item2", hooks.get(1).getExecutorKey());
    }

    @Test
    void honorsExplicitNamespace(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=Toggle\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("bt", hooks.get(0).getNamespace());
        assertEquals("bt:item1", hooks.get(0).getExecutorKey());
    }

    @Test
    void slugifiesMultiWordName(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Push Notifications!\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=Send\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals("push-notifications", hooks.get(0).getNamespace());
        assertEquals("push-notifications:item1", hooks.get(0).getExecutorKey());
    }

    @Test
    void itemsAreReadInPositionalOrder(@TempDir Path tempDir) throws Exception {
        // Even if listed in the file out of numeric order, positional iteration
        // visits item1 then item2 then item3.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item3=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label3=Third\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=First\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label2=Second\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(3, hooks.size());
        assertEquals("First", hooks.get(0).getLabel());
        assertEquals("Second", hooks.get(1).getLabel());
        assertEquals("Third", hooks.get(2).getLabel());
    }

    @Test
    void loopStopsAtFirstMissingItem(@TempDir Path tempDir) throws Exception {
        // item3 declared but item2 missing → loop stops after item1.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=First\n"
                + "item3=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label3=Third (unreachable)\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size(),
                "loop must stop at the first missing itemN — item3 is unreachable past missing item2");
        assertEquals("First", hooks.get(0).getLabel());
    }

    @Test
    void apiOnlyHookHasNullLabelButIsCallable(@TempDir Path tempDir) throws Exception {
        // item1 has no label1: registered with the executor, hidden from menu.
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertFalse(hooks.get(0).hasMenuLabel(), "label-less item must be hidden from menu");
        assertNull(hooks.get(0).getLabel());
        assertTrue(SimulatorHookExecutor.execute("bt:item1"));
    }

    @Test
    void executorReceivesEveryRegisteredHook(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "namespace=bt\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=Alpha\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#beta\n");

        SimulatorHookLoader.load(classloaderFor(tempDir));

        assertTrue(SimulatorHookExecutor.isRegistered("bt:item1"));
        assertTrue(SimulatorHookExecutor.isRegistered("bt:item2"));
        assertFalse(SimulatorHookExecutor.isRegistered("bt:item3"));
        assertFalse(SimulatorHookExecutor.execute("bt:item3"),
                "execute() must return false for unknown ids without throwing");
    }

    @Test
    void skipsFileWithoutName(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label1=Orphan\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertTrue(hooks.isEmpty(), "expected zero hooks but got: " + hooks);
    }

    @Test
    void skipsUnknownClassButContinuesScan(@TempDir Path tempDir) throws Exception {
        // item1 fails to resolve → still proceeds to item2 (since item1 was
        // declared, the loop continues; the failed lookup just yields no hook).
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1=com.example.DoesNotExist#nope\n"
                + "label1=Missing\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label2=Alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    @Test
    void skipsNonStaticMethod(@TempDir Path tempDir) throws Exception {
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#instanceOnly\n"
                + "label1=Instance\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label2=Alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    @Test
    void skipsMalformedActionString(@TempDir Path tempDir) throws Exception {
        // No '#' separator in item1.
        writeProps(tempDir, "name=Bluetooth\n"
                + "item1=not_a_method_reference\n"
                + "label1=Bad\n"
                + "item2=com.codename1.impl.javase.simulator.SimulatorHookLoaderTestFixture#alpha\n"
                + "label2=Alpha\n");

        List<SimulatorHook> hooks = SimulatorHookLoader.load(classloaderFor(tempDir));

        assertEquals(1, hooks.size());
        assertEquals("Alpha", hooks.get(0).getLabel());
    }

    @Test
    void slugifyHandlesEdgeCases() {
        assertEquals("bluetooth", SimulatorHookLoader.slugify("Bluetooth"));
        assertEquals("push-notifications", SimulatorHookLoader.slugify("Push Notifications!"));
        assertEquals("a-b-c", SimulatorHookLoader.slugify("A__B__C"));
        assertEquals("foo123", SimulatorHookLoader.slugify("foo123"));
        assertEquals("", SimulatorHookLoader.slugify("###"));
    }

    /** Writes the fixture to {@code <tempDir>/META-INF/codenameone/simulator-hooks.properties}. */
    private static void writeProps(Path tempDir, String content) throws Exception {
        Path metaInf = tempDir.resolve("META-INF").resolve("codenameone");
        Files.createDirectories(metaInf);
        File f = metaInf.resolve("simulator-hooks.properties").toFile();
        FileOutputStream out = new FileOutputStream(f);
        try {
            Writer w = new OutputStreamWriter(out, "UTF-8");
            w.write(content);
            w.flush();
        } finally {
            out.close();
        }
    }

    /**
     * Classloader whose only "extra" root is the temp dir, so
     * {@code getResources("META-INF/codenameone/simulator-hooks.properties")}
     * sees exactly the fixture and the fixture class is resolvable via parent
     * delegation. Avoids polluting the surrounding test classpath with a
     * resource that other tests would also discover.
     */
    private static ClassLoader classloaderFor(Path tempDir) throws Exception {
        URL url = tempDir.toUri().toURL();
        return new URLClassLoader(new URL[]{url}, SimulatorHookLoaderTest.class.getClassLoader());
    }
}
