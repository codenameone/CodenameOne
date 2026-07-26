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
package com.codename1.impl.javase.health;

import com.codename1.impl.javase.HealthSimulatorHooks;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Guards on the shipped hooks file and the hook class.
 *
 * <p>The interesting one is that the real, shipped
 * {@code simulator-hooks.properties} parses with two namespaces. A
 * classpath entry can only carry one copy of that resource, so adding the
 * health menu required extending the loader with a {@code groups=} form;
 * parsing the actual file is what proves the extension works rather than
 * only the synthetic fixtures.</p>
 */
class HealthSimulatorHooksTest {

    private static final String HOOKS =
            "../../Ports/JavaSE/src/META-INF/codenameone/"
                    + "simulator-hooks.properties";

    private static Properties shipped() throws IOException {
        File f = new File(HOOKS);
        assertTrue(f.isFile(), "missing " + f.getAbsolutePath());
        Properties p = new Properties();
        BufferedReader r = new BufferedReader(new FileReader(f));
        try {
            p.load(r);
        } finally {
            r.close();
        }
        return p;
    }

    @Test
    void shippedFileDeclaresBothGroups() throws Exception {
        Properties p = shipped();
        String groups = p.getProperty("groups");
        assertNotNull(groups, "the shipped file must use the groups= form");
        assertTrue(groups.contains("bluetooth"));
        assertTrue(groups.contains("health"));
    }

    /**
     * The Bluetooth menu predates the groups= form; it must keep working
     * unchanged now that it is expressed as a prefixed group.
     */
    @Test
    void bluetoothGroupStillDeclaresItsItems() throws Exception {
        Properties p = shipped();
        assertEquals("Bluetooth", p.getProperty("bluetooth.name"));
        assertEquals("bluetooth", p.getProperty("bluetooth.namespace"));
        assertNotNull(p.getProperty("bluetooth.item1"));
        assertNotNull(p.getProperty("bluetooth.label1"));
        assertNotNull(p.getProperty("bluetooth.item8"),
                "the API-only failure hook must survive the rewrite");
    }

    @Test
    void healthGroupDeclaresItsItems() throws Exception {
        Properties p = shipped();
        assertEquals("Health", p.getProperty("health.name"));
        assertEquals("health", p.getProperty("health.namespace"));
        assertNotNull(p.getProperty("health.item1"));
    }

    /**
     * Items are positional and the loader stops at the first gap, so a
     * missing number silently truncates the menu.
     */
    @Test
    void healthItemsAreContiguous() throws Exception {
        Properties p = shipped();
        int n = 1;
        while (p.getProperty("health.item" + n) != null) {
            n++;
        }
        assertTrue(n > 12, "expected at least 12 health items, found "
                + (n - 1));
    }

    /**
     * The trap hook is the one worth guaranteeing exists, since it is what
     * a developer clicks to discover the behaviour before App Review does.
     */
    @Test
    void theReadAuthTrapHookIsPresentAndLabelled() throws Exception {
        Properties p = shipped();
        assertEquals("com.codename1.impl.javase.HealthSimulatorHooks"
                        + "#grantWriteDenyReadSilently",
                p.getProperty("health.item3"));
        assertEquals("Grant Write, Silently Deny Read",
                p.getProperty("health.label3"));
    }

    /** Every referenced hook must resolve to a public static void no-arg. */
    @Test
    void everyHealthHookResolvesToTheRequiredSignature() throws Exception {
        Properties p = shipped();
        int n = 1;
        String action;
        while ((action = p.getProperty("health.item" + n)) != null) {
            int hash = action.indexOf('#');
            assertTrue(hash > 0, "malformed action: " + action);
            String fqcn = action.substring(0, hash);
            String methodName = action.substring(hash + 1);
            Class<?> cls = Class.forName(fqcn);
            Method m = cls.getDeclaredMethod(methodName);
            assertTrue(Modifier.isStatic(m.getModifiers()),
                    methodName + " must be static");
            assertTrue(Modifier.isPublic(m.getModifiers()),
                    methodName + " must be public");
            assertEquals(void.class, m.getReturnType(),
                    methodName + " must return void");
            n++;
        }
        assertTrue(n > 1, "no health hooks were checked");
    }

    /**
     * The hooks class runs inside the simulator process but must not drag
     * in a UI toolkit, matching the guard the Bluetooth simulation carries.
     */
    @Test
    void hooksClassIsHeadless() throws Exception {
        File src = new File("../../Ports/JavaSE/src/com/codename1/impl/"
                + "javase/HealthSimulatorHooks.java");
        assertTrue(src.isFile());
        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(src));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            br.close();
        }
        String content = sb.toString();
        assertFalse(content.contains("javax.swing"),
                "hooks must not use Swing");
        assertFalse(content.contains("javafx."),
                "hooks must not use JavaFX");
    }

    /** Loading the demo dataset must be deterministic across runs. */
    @Test
    void syntheticDataIsDeterministicForASeed() {
        long end = 1_767_225_600_000L;
        int a = new SyntheticHealthData(42).generateWeek(end).size();
        int b = new SyntheticHealthData(42).generateWeek(end).size();
        assertEquals(a, b);
        assertTrue(a > 0, "a week of synthetic data should not be empty");
    }

    /**
     * Nothing in the generator is a recording of anyone's data, so it must
     * produce values inside plausible physiological bounds rather than
     * replaying a trace.
     */
    @Test
    void syntheticHeartRatesStayInPhysiologicalBounds() {
        java.util.List<com.codename1.health.HealthSample> samples =
                new SyntheticHealthData(7).generateWeek(1_767_225_600_000L);
        int checked = 0;
        for (com.codename1.health.HealthSample s : samples) {
            if (s.getType() != com.codename1.health.HealthDataType.HEART_RATE) {
                continue;
            }
            double bpm = ((com.codename1.health.QuantitySample) s)
                    .getValue(com.codename1.health.HealthUnit
                            .COUNT_PER_MINUTE);
            assertTrue(bpm >= 35 && bpm <= 205,
                    "implausible synthetic heart rate: " + bpm);
            checked++;
        }
        assertTrue(checked > 0, "no heart rate samples were generated");
    }

    @Test
    void hooksClassExposesTheSimulatedStoreFactory() throws Exception {
        Method m = HealthSimulatorHooks.class
                .getDeclaredMethod("createSimulatedHealth");
        assertTrue(Modifier.isStatic(m.getModifiers()));
    }
}
