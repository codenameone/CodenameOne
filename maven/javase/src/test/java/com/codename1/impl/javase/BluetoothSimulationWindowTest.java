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
package com.codename1.impl.javase;

import com.codename1.bluetooth.BluetoothUuid;
import com.codename1.impl.javase.bluetooth.ManualScheduler;
import com.codename1.impl.javase.bluetooth.SimulatedBluetoothStack;
import com.codename1.impl.javase.bluetooth.VirtualCharacteristic;
import com.codename1.impl.javase.bluetooth.VirtualPeripheral;
import com.codename1.impl.javase.bluetooth.VirtualService;
import com.codename1.impl.javase.simulator.SimulatorHook;
import com.codename1.impl.javase.simulator.SimulatorHookLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless-safe checks of the Simulate&nbsp;&rarr;&nbsp;Bluetooth
 * Simulation window and its scripting hooks (style of
 * {@link LocationSimulationTest}): source-scan guards instead of
 * instantiating any Swing window, reflection over the hook contract, the
 * shipped {@code simulator-hooks.properties} parsed through the real
 * {@link SimulatorHookLoader}, and the canonical demo peripheral verified
 * against a fresh deterministic stack.
 */
public class BluetoothSimulationWindowTest {

    private static final String[] HOOK_METHODS = {
        "toggleAdapter", "addDemoPeripheral", "pushDemoNotification",
        "disconnectAll", "clearPeripherals", "switchToSimulatorBackend",
        "switchToNativeBackend", "primeReadFailure"
    };

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
    public void windowHasNoJavaFxImports() throws Exception {
        File src = new File("../../Ports/JavaSE/src/com/codename1/impl/"
                + "javase/BluetoothSimulation.java");
        Assertions.assertTrue(src.exists(),
                "Could not find BluetoothSimulation source file");
        Assertions.assertFalse(read(src).contains("javafx."),
                "BluetoothSimulation must not use JavaFX");
    }

    @Test
    public void hooksClassStaysHeadless() throws Exception {
        File src = new File("../../Ports/JavaSE/src/com/codename1/impl/"
                + "javase/BluetoothSimulatorHooks.java");
        Assertions.assertTrue(src.exists(),
                "Could not find BluetoothSimulatorHooks source file");
        String content = read(src);
        Assertions.assertFalse(content.contains("javax.swing"),
                "hooks must stay Swing-free -- they run on the CN1 EDT");
        Assertions.assertFalse(content.contains("javafx."),
                "BluetoothSimulatorHooks must not use JavaFX");
    }

    @Test
    public void hookMethodsArePublicStaticNoArgVoid() throws Exception {
        Class<?> hooks = BluetoothSimulatorHooks.class;
        for (String name : HOOK_METHODS) {
            Method m = hooks.getDeclaredMethod(name, new Class<?>[0]);
            Assertions.assertTrue(Modifier.isPublic(m.getModifiers()),
                    name + " must be public");
            Assertions.assertTrue(Modifier.isStatic(m.getModifiers()),
                    name + " must be static");
            Assertions.assertEquals(void.class, m.getReturnType(),
                    name + " must return void");
        }
    }

    @Test
    public void shippedHooksFileParsesWithBluetoothNamespace() {
        List<SimulatorHook> all = SimulatorHookLoader.load(
                BluetoothSimulationWindowTest.class.getClassLoader());
        List<SimulatorHook> bluetooth = new ArrayList<SimulatorHook>();
        for (SimulatorHook h : all) {
            if ("bluetooth".equals(h.getNamespace())) {
                bluetooth.add(h);
            }
        }
        Assertions.assertEquals(HOOK_METHODS.length, bluetooth.size(),
                "expected every declared bluetooth hook to resolve; got "
                        + bluetooth);
        int labeled = 0;
        for (SimulatorHook h : bluetooth) {
            Assertions.assertEquals("Bluetooth", h.getMenuName());
            Assertions.assertEquals("bluetooth:item" + h.getIndex(),
                    h.getExecutorKey());
            if (h.hasMenuLabel()) {
                labeled++;
            }
        }
        Assertions.assertEquals(HOOK_METHODS.length - 1, labeled,
                "exactly one hook (primeReadFailure) is API-only");
        // primeReadFailure is the last, label-less item
        SimulatorHook last = bluetooth.get(bluetooth.size() - 1);
        Assertions.assertFalse(last.hasMenuLabel(),
                "the last bluetooth item must be API-only");
    }

    @Test
    public void demoPeripheralHelperBuildsTheCanonicalDevice() {
        VirtualPeripheral p = BluetoothSimulatorHooks.createDemoPeripheral();
        Assertions.assertEquals("AA:BB:CC:DD:EE:01", p.getAddress());
        Assertions.assertEquals("SimulatedSensor", p.getName());
        Assertions.assertTrue(p.getAdvertisedServiceUuids().contains(
                BluetoothUuid.fromShort(0x180D)));

        VirtualService service =
                p.getService(BluetoothUuid.fromShort(0x180D));
        Assertions.assertNotNull(service, "demo service 0x180D missing");

        VirtualCharacteristic notify = service.getCharacteristic(
                BluetoothUuid.fromShort(0x2A37));
        Assertions.assertNotNull(notify, "0x2A37 characteristic missing");
        Assertions.assertTrue(notify.canRead());
        Assertions.assertTrue(notify.canWrite());
        Assertions.assertTrue(notify.canNotifyOrIndicate());
        Assertions.assertNotNull(notify.getDescriptor(BluetoothUuid.CCCD),
                "0x2A37 must carry a CCCD");

        VirtualCharacteristic control = service.getCharacteristic(
                BluetoothUuid.fromShort(0x2A39));
        Assertions.assertNotNull(control, "0x2A39 characteristic missing");
        Assertions.assertTrue(control.canWrite());
        Assertions.assertFalse(control.canNotifyOrIndicate());
    }

    @Test
    public void demoPeripheralRegistersInAFreshStack() {
        ManualScheduler scheduler = new ManualScheduler();
        SimulatedBluetoothStack stack =
                new SimulatedBluetoothStack(scheduler);
        stack.addPeripheral(BluetoothSimulatorHooks.createDemoPeripheral());
        scheduler.advance(10000);
        Assertions.assertTrue(
                stack.isPeripheralRegistered("AA:BB:CC:DD:EE:01"),
                "the canonical demo device must register");
        Assertions.assertNotNull(
                stack.getPeripheral("AA:BB:CC:DD:EE:01").getService(
                        BluetoothUuid.fromShort(0x180D)));
    }
}
