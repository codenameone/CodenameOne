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

import com.codename1.desktopcompat.shared.CompatRegistryBase;
import com.codename1.desktopcompat.shared.Resources;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The registry the desktop runtime's `ResourceBundle` creates bundle classes
/// through: generated, defined, installed and asked, over the desktop
/// runtime's real `CompatRegistryBase` and `Resources`.
public class ApplicationRegistryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final List<String> warned = new ArrayList<String>();

    private final com.codename1.build.Log log = new com.codename1.build.Log() {
        public void debug(CharSequence c) { }
        public void debug(CharSequence c, Throwable e) { }
        public void debug(Throwable e) { }
        public void info(CharSequence c) { }
        public void info(CharSequence c, Throwable e) { }
        public void info(Throwable e) { }
        public void warn(CharSequence c) { warned.add(c.toString()); }
        public void warn(CharSequence c, Throwable e) { }
        public void warn(Throwable e) { }
        public void error(CharSequence c) { }
        public void error(CharSequence c, Throwable e) { }
        public void error(Throwable e) { }
        public boolean isDebugEnabled() { return false; }
        public boolean isInfoEnabled() { return true; }
        public boolean isWarnEnabled() { return true; }
        public boolean isErrorEnabled() { return true; }
    };

    @After
    public void reset() {
        CompatRegistryBase.cn1ClearInstalled();
        Resources.cn1ClearIndex();
    }

    /// Defines the generated classes and nothing else; everything they name
    /// resolves from this test's class path. The generated classes come
    /// first: the layers' jars on that class path carry the placeholder of
    /// the same name.
    private static final class Loader extends ClassLoader {
        private final Map<String, byte[]> classes;

        Loader(Map<String, byte[]> classes) {
            super(ApplicationRegistryTest.class.getClassLoader());
            this.classes = classes;
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            byte[] b = classes.get(name.replace('.', '/'));
            if (b == null) {
                return super.loadClass(name, resolve);
            }
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                c = defineClass(name, b, 0, b.length);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    private static void install(Map<String, byte[]> classes) throws Exception {
        new Loader(classes).loadClass(ApplicationRegistry.NAME.replace('/', '.')).getMethod("cn1Install")
                .invoke(null);
    }

    @Test
    public void installingAnswersForEveryClassAndResource() throws Exception {
        Map<String, byte[]> classes = ApplicationRegistry.generate(Arrays.asList("a/b.txt", "c.properties"),
                Arrays.asList("java.lang.StringBuilder", "java.lang.StringBuffer", "java.util.ArrayList",
                        "java.lang.StringBuilder"));
        assertFalse(CompatRegistryBase.cn1Installed());
        install(classes);
        assertTrue(CompatRegistryBase.cn1Installed());

        assertTrue(CompatRegistryBase.cn1NewInstance("java.lang.StringBuilder") instanceof StringBuilder);
        assertTrue(CompatRegistryBase.cn1NewInstance("java.lang.StringBuffer") instanceof StringBuffer);
        assertTrue(CompatRegistryBase.cn1NewInstance("java.util.ArrayList") instanceof ArrayList);
        assertNotSame(CompatRegistryBase.cn1NewInstance("java.util.ArrayList"),
                CompatRegistryBase.cn1NewInstance("java.util.ArrayList"));
        assertNull(CompatRegistryBase.cn1NewInstance("java.util.HashMap"));
        assertSame(StringBuffer.class, CompatRegistryBase.cn1TypeOf("java.lang.StringBuffer"));
        assertSame(ArrayList.class, CompatRegistryBase.cn1TypeOf("java.util.ArrayList"));
        assertNull(CompatRegistryBase.cn1TypeOf("java.util.HashMap"));

        assertEquals(Arrays.asList("a/b.txt", "c.properties"), sorted(Resources.cn1ResourcePaths()));

        // A second install, from a second definition, keeps the first.
        install(ApplicationRegistry.generate(Collections.singletonList("other.txt"),
                Collections.<String>emptyList()));
        assertTrue(CompatRegistryBase.cn1NewInstance("java.util.ArrayList") instanceof ArrayList);
        assertEquals(Arrays.asList("a/b.txt", "c.properties"), sorted(Resources.cn1ResourcePaths()));
    }

    @Test
    public void anEmptyApplicationInstallsAnEmptyRegistry() throws Exception {
        Map<String, byte[]> classes = ApplicationRegistry.generate(Collections.<String>emptyList(),
                Collections.<String>emptyList());
        assertEquals(Collections.singleton(ApplicationRegistry.NAME), classes.keySet());
        install(classes);
        assertTrue(CompatRegistryBase.cn1Installed());
        assertNull(CompatRegistryBase.cn1NewInstance("java.lang.Object"));
        assertNull(CompatRegistryBase.cn1TypeOf("java.lang.Object"));
        assertEquals(0, Resources.cn1ResourcePaths().length);
    }

    @Test
    public void registrationsSpreadOverPartsAndStalePartsAreRemoved() throws Exception {
        List<String> resources = new ArrayList<String>();
        for (int i = 0; i < 2500; i++) {
            resources.add("r/" + i + ".txt");
        }
        Map<String, byte[]> big = ApplicationRegistry.generate(resources, Collections.<String>emptyList());
        assertTrue(big.containsKey(ApplicationRegistry.NAME + "$Part2"));
        assertFalse(big.containsKey(ApplicationRegistry.NAME + "$Part3"));
        install(big);
        assertEquals(new java.util.TreeSet<String>(resources), new java.util.TreeSet<String>(sorted(Resources.cn1ResourcePaths())));

        File dir = tmp.newFolder();
        ApplicationRegistry.write(dir, big, log);
        File generated = new File(dir, ApplicationRegistry.NAME + ".class").getParentFile();
        assertEquals(4, generated.list().length);
        Map<String, byte[]> small = ApplicationRegistry.generate(resources.subList(0, 10),
                Collections.<String>emptyList());
        ApplicationRegistry.write(dir, small, log);
        assertEquals(Arrays.asList("CompatRegistry$Part0.class", "CompatRegistry.class"),
                sorted(generated.list()));
        assertEquals(Collections.<String>emptyList(), warned);
    }

    private static List<String> sorted(String[] names) {
        List<String> out = new ArrayList<String>(Arrays.asList(names));
        Collections.sort(out);
        return out;
    }
}
