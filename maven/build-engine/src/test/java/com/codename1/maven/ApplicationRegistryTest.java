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

import com.codename1.impl.JdkHost;
import com.codename1.impl.compat.Resources;
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The registry the class library's `ResourceBundle` and `ServiceLoader`
/// read through `JdkHost`: generated, defined, installed and asked, over the
/// core's real `CompatRegistryBase` and `Resources`.
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
        JdkHost.setRegistry(null);
        Resources.cn1ClearIndex();
    }

    /// Defines the generated classes and nothing else; everything they name
    /// resolves from this test's class path.
    private static final class Loader extends ClassLoader {
        private final Map<String, byte[]> classes;

        Loader(Map<String, byte[]> classes) {
            super(ApplicationRegistryTest.class.getClassLoader());
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] b = classes.get(name.replace('.', '/'));
            if (b == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, b, 0, b.length);
        }
    }

    private static void install(Map<String, byte[]> classes) throws Exception {
        new Loader(classes).loadClass(ApplicationRegistry.NAME.replace('/', '.')).getMethod("cn1Install")
                .invoke(null);
    }

    @Test
    public void installingAnswersForEveryClassProviderAndResource() throws Exception {
        List<String[]> services = new ArrayList<String[]>();
        services.add(new String[] {"java.lang.CharSequence", "java/lang/StringBuilder"});
        services.add(new String[] {"java.lang.CharSequence", "java/lang/StringBuffer"});
        // A bundle class that is also a provider gets one number.
        services.add(new String[] {"java.lang.Object", "java/util/ArrayList"});
        services.add(new String[] {"java.lang.CharSequence", "java/lang/StringBuilder"});
        Map<String, byte[]> classes = ApplicationRegistry.generate(Arrays.asList("a/b.txt", "c.properties"),
                Collections.singletonList("java.util.ArrayList"), services);
        assertNull(JdkHost.getRegistry());
        install(classes);

        JdkHost.Registry r = JdkHost.getRegistry();
        assertTrue(r instanceof com.codename1.impl.compat.CompatRegistryBase);
        assertArrayEquals(new String[] {"java.lang.StringBuilder", "java.lang.StringBuffer"},
                r.providers("java.lang.CharSequence"));
        assertArrayEquals(new String[] {"java.util.ArrayList"}, r.providers("java.lang.Object"));
        assertNull(r.providers("java.lang.Runnable"));

        assertTrue(r.newInstance("java.lang.StringBuilder") instanceof StringBuilder);
        assertTrue(r.newInstance("java.lang.StringBuffer") instanceof StringBuffer);
        assertTrue(r.newInstance("java.util.ArrayList") instanceof ArrayList);
        assertNotSame(r.newInstance("java.util.ArrayList"), r.newInstance("java.util.ArrayList"));
        assertNull(r.newInstance("java.util.HashMap"));
        assertSame(StringBuffer.class, r.type("java.lang.StringBuffer"));
        assertSame(ArrayList.class, r.type("java.util.ArrayList"));
        assertNull(r.type("java.util.HashMap"));

        // The host serves what the registry answers.
        assertArrayEquals(new String[] {"java.util.ArrayList"}, JdkHost.get().providers("java.lang.Object"));
        assertSame(ArrayList.class, JdkHost.get().type("java.util.ArrayList"));

        assertEquals(Arrays.asList("a/b.txt", "c.properties"), sorted(Resources.cn1ResourcePaths()));

        // A second install, from a second definition, keeps the first.
        install(ApplicationRegistry.generate(Collections.singletonList("other.txt"),
                Collections.<String>emptyList(), new ArrayList<String[]>()));
        assertSame(r, JdkHost.getRegistry());
        assertEquals(Arrays.asList("a/b.txt", "c.properties"), sorted(Resources.cn1ResourcePaths()));
    }

    @Test
    public void anEmptyApplicationInstallsAnEmptyRegistry() throws Exception {
        Map<String, byte[]> classes = ApplicationRegistry.generate(Collections.<String>emptyList(),
                Collections.<String>emptyList(), new ArrayList<String[]>());
        assertEquals(Collections.singleton(ApplicationRegistry.NAME), classes.keySet());
        install(classes);
        JdkHost.Registry r = JdkHost.getRegistry();
        assertNull(r.providers("java.lang.Runnable"));
        assertNull(r.newInstance("java.lang.Object"));
        assertNull(r.type("java.lang.Object"));
        assertEquals(0, Resources.cn1ResourcePaths().length);
    }

    @Test
    public void registrationsSpreadOverPartsAndStalePartsAreRemoved() throws Exception {
        List<String> resources = new ArrayList<String>();
        for (int i = 0; i < 2500; i++) {
            resources.add("r/" + i + ".txt");
        }
        Map<String, byte[]> big = ApplicationRegistry.generate(resources, Collections.<String>emptyList(),
                new ArrayList<String[]>());
        assertTrue(big.containsKey(ApplicationRegistry.NAME + "$Part2"));
        assertFalse(big.containsKey(ApplicationRegistry.NAME + "$Part3"));
        install(big);
        assertEquals(new java.util.TreeSet<String>(resources), new java.util.TreeSet<String>(sorted(Resources.cn1ResourcePaths())));

        File dir = tmp.newFolder();
        ApplicationRegistry.write(dir, big, log);
        File generated = new File(dir, ApplicationRegistry.NAME + ".class").getParentFile();
        assertEquals(4, generated.list().length);
        Map<String, byte[]> small = ApplicationRegistry.generate(resources.subList(0, 10),
                Collections.<String>emptyList(), new ArrayList<String[]>());
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

    /// The build reads a services file by the rule the runtime reads one.
    @Test
    public void servicesFilesAreReadAsTheCompatRuntimeReadsThem() {
        String text = "# header\r\n\n  org.a.One   # first\norg.a.Two\r\norg.a.One\nnot a name\n\t\n"
                + "9bad\norg.a.$Three\n#org.a.Four\norg.a.Five#x";
        List<String> illegal = new ArrayList<String>();
        List<String> compatIllegal = new ArrayList<String>();
        assertEquals(com.codename1.compat.jdk.ServiceLoader.cn1ProviderNames(text, compatIllegal),
                ServiceProviderNames.of(text, illegal));
        assertEquals(compatIllegal, illegal);
        assertEquals(Arrays.asList("org.a.One", "org.a.Two", "org.a.$Three", "org.a.Five"),
                ServiceProviderNames.of(text, null));
        assertEquals(Arrays.asList("not a name", "9bad"), illegal);
    }

    /// The core's resource names are the compat layer's, which the shipped
    /// applications were flattened by.
    @Test
    public void coreResourceNamesMatchTheCompatOnes() {
        String[] paths = {"a.txt", "a/b.txt", "META-INF/resources/x/y.png", "a__b/c", "x/./y/../z.txt",
            "/lead/slash.txt", "a/b/", "dots.in.dir/f.v.txt"};
        for (String p : paths) {
            assertEquals(p, com.codename1.compat.jdk.ResourceNames.normalize(p),
                    com.codename1.impl.compat.ResourceNames.normalize(p));
            String flat = com.codename1.compat.jdk.ResourceNames.flatName(p);
            assertEquals(p, flat, com.codename1.impl.compat.ResourceNames.flatName(p));
            assertEquals(p, com.codename1.compat.jdk.ResourceNames.nestedPath(flat),
                    com.codename1.impl.compat.ResourceNames.nestedPath(flat));
            assertEquals(p, com.codename1.compat.jdk.ResourceNames.resolve("com.acme.Main", p),
                    com.codename1.impl.compat.ResourceNames.resolve("com.acme.Main", p));
        }
    }
}
