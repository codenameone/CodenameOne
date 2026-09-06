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

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The simulator hooks this port actually ships, rather than a fixture.
 *
 * <p>{@link SimulatorHookLoader} is deliberately forgiving: a group naming a class that cannot be
 * loaded, or a method that is not {@code public static void}, is skipped and the scan continues.
 * That is the right behaviour for a cn1lib whose classes may legitimately be absent, and it means
 * a typo in this port's own file costs a whole Simulate menu with nothing said anywhere -- which
 * is exactly the sort of failure nobody notices until someone reaches for the menu and it is not
 * there.</p>
 *
 * <p>So this walks {@code META-INF/codenameone/simulator-hooks.properties} as written and insists
 * every group listed is declared, every declared item resolves, and the numbering has no hole in
 * it. Sibling coverage to {@link SimulatorHookLoaderTest}, which tests the parser against files it
 * writes itself.</p>
 */
class ShippedSimulatorHooksTest {

    private static final String RESOURCE = "META-INF/codenameone/simulator-hooks.properties";

    private static Properties shipped() throws Exception {
        InputStream in = ShippedSimulatorHooksTest.class.getClassLoader()
                .getResourceAsStream(RESOURCE);
        assertNotNull(in, RESOURCE + " is not on the test classpath");
        try {
            Properties props = new Properties();
            props.load(in);
            return props;
        } finally {
            in.close();
        }
    }

    private static List<String> groups(Properties props) {
        List<String> out = new ArrayList<String>();
        String declared = props.getProperty("groups");
        assertNotNull(declared, "the shipped file declares no groups");
        for (String group : declared.split(",")) {
            String trimmed = group.trim();
            if (trimmed.length() > 0) {
                out.add(trimmed);
            }
        }
        return out;
    }

    @Test
    void everyDeclaredGroupHasANameAndAtLeastOneItem() throws Exception {
        Properties props = shipped();
        List<String> groups = groups(props);
        assertFalse(groups.isEmpty(), "no groups declared");
        for (String group : groups) {
            assertNotNull(props.getProperty(group + ".name"), group + " has no name");
            assertNotNull(props.getProperty(group + ".item1"),
                    group + " declares no items, so it would surface as an empty menu");
        }
    }

    /**
     * The loader stops reading a group at its first missing index, so a hole silently truncates
     * the menu: an item9 written after item7 with no item8 is simply never registered.
     */
    @Test
    void itemNumberingHasNoHoles() throws Exception {
        Properties props = shipped();
        for (String group : groups(props)) {
            int highest = 0;
            for (Object key : props.keySet()) {
                String name = (String) key;
                String prefix = group + ".item";
                if (name.startsWith(prefix)) {
                    int n = Integer.parseInt(name.substring(prefix.length()));
                    if (n > highest) {
                        highest = n;
                    }
                }
            }
            for (int i = 1; i <= highest; i++) {
                assertNotNull(props.getProperty(group + ".item" + i),
                        group + ".item" + i + " is missing, so every item after it is dropped");
            }
        }
    }

    /**
     * Every action resolves to a {@code public static void} method that actually exists. A
     * misspelling here is not an error at load time -- the entry is skipped -- so nothing tells
     * anyone until the menu item is missing.
     */
    @Test
    void everyDeclaredActionResolves() throws Exception {
        Properties props = shipped();
        int checked = 0;
        for (String group : groups(props)) {
            for (int i = 1; ; i++) {
                String action = props.getProperty(group + ".item" + i);
                if (action == null) {
                    break;
                }
                int hash = action.indexOf('#');
                assertTrue(hash > 0, action + " is not <class>#<method>");
                String className = action.substring(0, hash);
                String methodName = action.substring(hash + 1);
                Class<?> cls = Class.forName(className);
                Method m = cls.getDeclaredMethod(methodName);
                assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()),
                        action + " is not static");
                assertTrue(java.lang.reflect.Modifier.isPublic(m.getModifiers()),
                        action + " is not public");
                assertEquals(void.class, m.getReturnType(), action + " does not return void");
                checked++;
            }
        }
        assertTrue(checked > 0, "no actions were checked, so this test proved nothing");
    }

    /** Two groups sharing a namespace would make CN.execute ambiguous. */
    @Test
    void namespacesAreUnique() throws Exception {
        Properties props = shipped();
        Set<String> seen = new HashSet<String>();
        for (String group : groups(props)) {
            String namespace = props.getProperty(group + ".namespace");
            if (namespace == null) {
                namespace = SimulatorHookLoader.slugify(props.getProperty(group + ".name"));
            }
            assertTrue(seen.add(namespace), "two groups share the namespace " + namespace);
        }
    }

    /** The group added for state restoration and continuity is present and wired. */
    @Test
    void continuityHooksAreRegistered() throws Exception {
        Properties props = shipped();
        assertTrue(groups(props).contains("continuity"),
                "the continuity group is not in the groups list, so none of it loads");
        assertEquals("continuity", props.getProperty("continuity.namespace"));
        assertEquals("com.codename1.impl.javase.ContinuitySimulatorHooks#continueHere",
                props.getProperty("continuity.item1"));
    }
}
