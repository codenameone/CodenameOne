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
package com.codename1.compat.jdk;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The classpath resource lookups: how a name is resolved, the flat name it
/// ships under, and what the runtime answers from a build's index and a
/// bundle that holds only flat names.
public class ResourcesTest {

    /// A bundle with no directories: flat name to contents.
    private final Map<String, String> bundle = new HashMap<String, String>();
    private final List<String> asked = new ArrayList<String>();

    @Before
    public void installFlatBundle() {
        ResourceBundle.cn1Reset();
        Resources.cn1ClearIndex();
        Resources.cn1SetProvider(new Resources.Provider() {
            @Override
            public InputStream open(String flatName) throws IOException {
                asked.add(flatName);
                String text = bundle.get(flatName);
                return text == null ? null : new ByteArrayInputStream(text.getBytes("UTF-8"));
            }
        });
    }

    @After
    public void restore() {
        Resources.cn1SetProvider(null);
        Resources.cn1ClearIndex();
        Resources.setContextClassLoader(Thread.currentThread(), null);
        ResourceBundle.cn1Reset();
    }

    /// Ships `path` the way the build does: indexed, under its flat name.
    private void ship(String path, String text) {
        Resources.cn1AddResource(path);
        bundle.put(ResourceNames.flatName(path), text);
    }

    private static String read(InputStream in) throws IOException {
        assertNotNull(in);
        StringBuilder out = new StringBuilder();
        int c = in.read();
        while (c >= 0) {
            out.append((char) c);
            c = in.read();
        }
        in.close();
        return out.toString();
    }

    // ------------------------------------------------------------------
    // Names
    // ------------------------------------------------------------------

    @Test
    public void aRelativeNameIsResolvedAgainstTheClassPackage() {
        assertEquals("com/example/img/a.png", ResourceNames.resolve("com.example.Main", "img/a.png"));
        assertEquals("com/example/img/a.png", ResourceNames.resolve("com/example/Main", "img/a.png"));
        assertEquals("com/example/Main$1.txt", ResourceNames.resolve("com.example.Main$Inner", "Main$1.txt"));
        assertEquals("a.png", ResourceNames.resolve("Main", "a.png"));
        assertEquals("x/y.txt", ResourceNames.resolve("com.example.Main", "/x/y.txt"));
        assertEquals("x/y.txt", ResourceNames.resolve(null, "x/y.txt"));
        assertNull(ResourceNames.resolve("com.example.Main", null));
    }

    @Test
    public void dotSegmentsAreResolvedAndClimbingOutIsNothing() {
        assertEquals("com/img/a.png", ResourceNames.resolve("com.example.Main", "../img/a.png"));
        assertEquals("com/example/a.png", ResourceNames.resolve("com.example.Main", "./a.png"));
        assertEquals("a/c", ResourceNames.normalize("/a//b/./../c"));
        assertEquals("a.png", ResourceNames.resolve("com.example.Main", "../../a.png"));
        assertNull(ResourceNames.resolve("com.example.Main", "../../../a.png"));
        assertNull(ResourceNames.normalize("/"));
        assertNull(ResourceNames.normalize(""));
        assertNull(ResourceNames.normalize("a/.."));
        assertNull(ResourceNames.normalize(null));
    }

    @Test
    public void theFlatNameIsReadableStableAndLeavesRootNamesAlone() {
        assertEquals("logo.png", ResourceNames.flatName("logo.png"));
        assertEquals("my_logo.png", ResourceNames.flatName("my_logo.png"));
        assertEquals("com__example__img__a.png", ResourceNames.flatName("com/example/img/a.png"));
        assertEquals("a__my_ulogo.png", ResourceNames.flatName("a/my_logo.png"));
        assertEquals("a/my_logo.png", ResourceNames.nestedPath("a__my_ulogo.png"));
        assertNull(ResourceNames.nestedPath("logo.png"));
        assertNull(ResourceNames.nestedPath("my_logo.png"));
    }

    /// Two different nested paths never share a flat name, whatever
    /// underscores they hold: the mapping has an inverse.
    @Test
    public void theFlatNameIsInjective() {
        Random random = new Random(42);
        String alphabet = "ab_/.";
        Map<String, String> byFlat = new HashMap<String, String>();
        Set<String> paths = new HashSet<String>();
        for (int i = 0; i < 20000; i++) {
            StringBuilder raw = new StringBuilder();
            int n = 2 + random.nextInt(9);
            for (int j = 0; j < n; j++) {
                raw.append(alphabet.charAt(random.nextInt(alphabet.length())));
            }
            String path = ResourceNames.normalize(raw.toString());
            if (path == null || path.indexOf('/') < 0 || !paths.add(path)) {
                continue;
            }
            String flat = ResourceNames.flatName(path);
            assertTrue(flat, flat.indexOf('/') < 0);
            String other = byFlat.put(flat, path);
            assertNull(path + " and " + other + " both flatten to " + flat, other);
            assertEquals(path, ResourceNames.nestedPath(flat));
        }
        assertTrue("too few distinct paths: " + paths.size(), paths.size() > 2000);
    }

    // ------------------------------------------------------------------
    // Lookups against a build's index
    // ------------------------------------------------------------------

    @Test
    public void aNestedResourceIsReadUnderItsFlatName() throws Exception {
        Resources.cn1BeginIndex();
        ship("com/codename1/compat/jdk/img/a.png", "PNG");
        ship("x/y.txt", "why");

        assertEquals("PNG", read(Resources.getResourceAsStream(ResourcesTest.class, "img/a.png")));
        assertEquals("why", read(Resources.getResourceAsStream(ResourcesTest.class, "/x/y.txt")));
        assertEquals("why", read(Resources.getResourceAsStream(ResourcesTest.class, "../../../../x/y.txt")));
        assertTrue(asked.toString(), asked.contains("com__codename1__compat__jdk__img__a.png"));
        assertTrue(asked.toString(), asked.contains("x__y.txt"));
    }

    /// Absence is decided from the index: nothing is opened to find out.
    @Test
    public void anUnindexedNestedResourceIsAbsentWithoutOpeningAnything() {
        Resources.cn1BeginIndex();
        ship("x/y.txt", "why");
        // In the bundle, but not something the build shipped as a resource.
        bundle.put("x__z.txt", "stray");

        assertNull(Resources.getResource(ResourcesTest.class, "/x/z.txt"));
        assertNull(Resources.getResourceAsStream(ResourcesTest.class, "/x/z.txt"));
        assertNull(Resources.getResource(ResourcesTest.class, "missing.png"));
        assertNull(Resources.getResourceAsStream(ResourcesTest.class, "/../x/y.txt"));
        assertFalse(Resources.exists("/x/z.txt"));
        assertTrue(Resources.exists("/x/y.txt"));
        assertTrue(Resources.exists("x/y.txt"));
        assertTrue(asked.toString(), asked.isEmpty());
    }

    /// A file at the root is not flattened, and may come from outside the
    /// desktop resources, so it is looked for whether or not it is indexed.
    @Test
    public void aRootNameIsLookedForInTheBundle() throws Exception {
        Resources.cn1BeginIndex();
        bundle.put("theme.res", "T");
        assertEquals("T", read(Resources.getResourceAsStream(ResourcesTest.class, "/theme.res")));
        assertNotNull(Resources.getResource(ResourcesTest.class, "/theme.res"));
        assertNull(Resources.getResource(ResourcesTest.class, "/nothing.res"));
    }

    @Test
    public void theUrlIsStableAndOpens() throws Exception {
        Resources.cn1BeginIndex();
        ship("com/codename1/compat/jdk/img/my logo.png", "LOGO");

        URL url = Resources.getResource(ResourcesTest.class, "img/my logo.png");
        assertNotNull(url);
        assertEquals("cn1res:/com/codename1/compat/jdk/img/my%20logo.png", url.toString());
        assertEquals(url.toString(), url.toExternalForm());
        assertEquals("cn1res", url.getProtocol());
        assertEquals("/com/codename1/compat/jdk/img/my%20logo.png", url.getPath());
        assertEquals("/com/codename1/compat/jdk/img/my%20logo.png", url.getFile());
        assertEquals("LOGO", read(url.openStream()));
        // What a loader handed the external form does with it.
        assertEquals("LOGO", read(new URL(url.toExternalForm()).openStream()));
        assertEquals(url, Resources.getResource(ResourcesTest.class, "/com/codename1/compat/jdk/img/my logo.png"));
    }

    @Test
    public void aClassLoaderTakesAbsoluteNamesWithoutASlash() throws Exception {
        Resources.cn1BeginIndex();
        ship("x/y.txt", "why");
        ClassLoader loader = Resources.getContextClassLoader(Thread.currentThread());
        assertNotNull(loader);

        assertEquals("why", read(Resources.getResourceAsStream(loader, "x/y.txt")));
        assertEquals("cn1res:/x/y.txt", Resources.getResource(loader, "x/y.txt").toString());
        assertNull(Resources.getResource(loader, "/x/y.txt"));
        assertNull(Resources.getResourceAsStream(loader, "/x/y.txt"));
        assertEquals("why", read(Resources.getSystemResourceAsStream("x/y.txt")));
        assertEquals("cn1res:/x/y.txt", Resources.getSystemResource("x/y.txt").toString());

        Enumeration<URL> all = Resources.getResources(loader, "x/y.txt");
        assertTrue(all.hasMoreElements());
        assertEquals("cn1res:/x/y.txt", all.nextElement().toString());
        assertFalse(all.hasMoreElements());
        assertFalse(Resources.getSystemResources("x/none.txt").hasMoreElements());

        try {
            Resources.getResource((ClassLoader) null, "x/y.txt");
            fail("A null loader is a NullPointerException, as calling a method on it would be");
        } catch (NullPointerException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void theContextClassLoaderIsWhatWasSet() {
        ClassLoader mine = new ClassLoader() { };
        Resources.setContextClassLoader(Thread.currentThread(), mine);
        assertSame(mine, Resources.getContextClassLoader(Thread.currentThread()));
        Resources.setContextClassLoader(Thread.currentThread(), null);
        assertNotNull(Resources.getContextClassLoader(Thread.currentThread()));
    }

    // ------------------------------------------------------------------
    // Bundles registered by a build
    // ------------------------------------------------------------------

    /// A bundle class a registry creates with `new`.
    public static final class Labels extends ListResourceBundle {
        @Override
        protected Object[][] getContents() {
            return new Object[][] {{"ok", "OK"}};
        }
    }

    @Test
    public void registeredBundlesAreReadLazilyFromFlatNames() {
        Locale saved = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            Resources.cn1BeginIndex();
            ship("p/Messages.properties", "greeting=Hello\nbye=Bye\n");
            ship("p/Messages_fr.properties", "greeting=Bonjour\n");
            ResourceBundle.cn1RegisterProperties("p.Messages", "/p/Messages.properties");
            ResourceBundle.cn1RegisterProperties("p.Messages_fr", "/p/Messages_fr.properties");
            ResourceBundle.cn1RegisterBundleClass("p.Labels", new ResourceBundle.Cn1Factory() {
                @Override
                public ResourceBundle cn1Create(int id) {
                    return id == 7 ? new Labels() : null;
                }
            }, 7);
            ResourceBundle.cn1Seal();
            assertTrue("Registering reads nothing: " + asked, asked.isEmpty());

            assertEquals("Hello", ResourceBundle.getBundle("p.Messages").getString("greeting"));
            ResourceBundle french = ResourceBundle.getBundle("p.Messages", Locale.FRENCH);
            assertEquals("Bonjour", french.getString("greeting"));
            assertEquals("Bye", french.getString("bye"));
            assertEquals("OK", ResourceBundle.getBundle("p.Labels").getString("ok"));
            assertTrue(asked.toString(), asked.contains("p__Messages_ufr.properties"));

            try {
                // Sealed: an unregistered name is not looked up as a class.
                ResourceBundle.getBundle("com.codename1.compat.jdk.ResourcesTest$Labels");
                fail("A sealed registry answers only for what was registered");
            } catch (MissingResourceException expected) {
                assertNotNull(expected);
            }
        } finally {
            Locale.setDefault(saved);
        }
    }
}
