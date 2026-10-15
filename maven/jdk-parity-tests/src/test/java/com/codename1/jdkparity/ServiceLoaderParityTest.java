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
package com.codename1.jdkparity;

import com.codename1.impl.CoreJdkHostInstaller;
import com.codename1.impl.JdkHost;
import jdkparity.java.util.Optional;
import jdkparity.java.util.ServiceLoader;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.codename1.jdkparity.Junit4Style.assertEquals;
import static com.codename1.jdkparity.Junit4Style.assertFalse;
import static com.codename1.jdkparity.Junit4Style.assertNotSame;
import static com.codename1.jdkparity.Junit4Style.assertSame;
import static com.codename1.jdkparity.Junit4Style.assertTrue;
import static com.codename1.jdkparity.Junit4Style.fail;

/// Holds JavaAPI's `ServiceLoader` against `java.util.ServiceLoader` over the
/// same services files (`src/test/resources/META-INF/services`): the order
/// of the providers, what a file may hold besides names, when a provider is
/// created and how often, read through the core's host, which with no
/// display reads the class path. What the JDK running the tests does not
/// have -- `findFirst` and `stream` came later -- is held against its
/// documentation. Then the loader over a registry, as a built application
/// has one.
public class ServiceLoaderParityTest {

    /// The service.
    public interface Greeter {
        String greet();
    }

    /// A service whose file lists nothing.
    public interface Empty {
    }

    /// A service with no file.
    public interface Unlisted {
    }

    private static final List<String> CREATED = new ArrayList<String>();

    public static class English implements Greeter {
        public English() {
            CREATED.add("English");
        }

        @Override
        public String greet() {
            return "hello";
        }
    }

    public static class French implements Greeter {
        public French() {
            CREATED.add("French");
        }

        @Override
        public String greet() {
            return "bonjour";
        }
    }

    public static class German implements Greeter {
        public German() {
            CREATED.add("German");
        }

        @Override
        public String greet() {
            return "hallo";
        }
    }

    /// Listed in the file behind a `#`, so never a provider.
    public static class Commented implements Greeter {
        public Commented() {
            CREATED.add("Commented");
        }

        @Override
        public String greet() {
            return "never";
        }
    }

    @BeforeEach
    public void noBuildStepRan() {
        CoreJdkHostInstaller.install();
        JdkHost.setRegistry(null);
        CREATED.clear();
    }

    @AfterEach
    public void forget() {
        JdkHost.setRegistry(null);
        JdkHost.install(null);
    }

    private static List<String> created() {
        List<String> out = new ArrayList<String>(CREATED);
        CREATED.clear();
        return out;
    }

    @Test
    public void theProvidersComeInFileOrderWithoutCommentsBlanksOrRepeats() {
        List<String> theirs = new ArrayList<String>();
        for (Greeter g : java.util.ServiceLoader.load(Greeter.class)) {
            theirs.add(g.getClass().getName() + "=" + g.greet());
        }
        List<String> theirOrder = created();
        List<String> mine = new ArrayList<String>();
        for (Greeter g : ServiceLoader.load(Greeter.class)) {
            mine.add(g.getClass().getName() + "=" + g.greet());
        }
        assertEquals(theirs, mine);
        assertEquals(theirOrder, created());
        assertEquals(3, mine.size());
        assertTrue(mine.get(0), mine.get(0).endsWith("English=hello"));
        assertTrue(mine.get(2), mine.get(2).endsWith("German=hallo"));
    }

    /// One step of a walk over both loaders, as text.
    private static String step(Iterator<Greeter> it, String what) {
        String answer;
        if ("hasNext".equals(what)) {
            answer = String.valueOf(it.hasNext());
        } else {
            try {
                answer = it.next().greet();
            } catch (NoSuchElementException e) {
                answer = "NoSuchElementException";
            }
        }
        return what + "=" + answer + " created " + created();
    }

    @Test
    public void aProviderIsCreatedWhenTheIterationReachesItAndOncePerLoader() {
        String[] walk = {"next", "hasNext", "hasNext", "next", "next", "hasNext", "next"};
        java.util.ServiceLoader<Greeter> theirs = java.util.ServiceLoader.load(Greeter.class);
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        assertEquals("Loading creates nothing", Arrays.asList(), created());

        List<String> expected = new ArrayList<String>();
        Iterator<Greeter> a = theirs.iterator();
        for (String what : walk) {
            expected.add(step(a, what));
        }
        List<String> actual = new ArrayList<String>();
        Iterator<Greeter> b = mine.iterator();
        for (String what : walk) {
            actual.add(step(b, what));
        }
        assertEquals(expected.toString().replace("], ", "],\n"), actual.toString().replace("], ", "],\n"));
        assertEquals("next=hello created [English]", actual.get(0));

        // A second iteration answers the same instances and creates nothing.
        Greeter first = mine.iterator().next();
        assertSame(first, mine.iterator().next());
        assertSame(theirs.iterator().next(), theirs.iterator().next());
        assertEquals(Arrays.asList(), created());
    }

    /// Not held against the JDK running these tests: the one that came with
    /// `findFirst` shares the created providers between the iterations of a
    /// loader by position, which is what its documentation always said, and
    /// the older one handed each provider to whichever iteration asked
    /// first. The compat layer's `JdkShimsRemapTest` holds this behaviour against a
    /// current JDK.
    @Test
    public void twoIterationsShareWhatEitherCreated() {
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        List<String> actual = new ArrayList<String>();
        Iterator<Greeter> b1 = mine.iterator();
        Iterator<Greeter> b2 = mine.iterator();
        actual.add(step(b1, "next"));
        actual.add(step(b2, "next"));
        actual.add(step(b2, "next"));
        actual.add(step(b1, "next"));
        actual.add(step(b1, "next"));
        actual.add(step(b2, "next"));
        actual.add(step(b2, "hasNext"));
        actual.add(step(b1, "next"));
        assertEquals(Arrays.asList("next=hello created [English]", "next=hello created []",
                "next=bonjour created [French]", "next=bonjour created []", "next=hallo created [German]",
                "next=hallo created []", "hasNext=false created []", "next=NoSuchElementException created []"),
                actual);
    }

    @Test
    public void reloadCreatesTheProvidersAgain() {
        java.util.ServiceLoader<Greeter> theirs = java.util.ServiceLoader.load(Greeter.class);
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        Greeter theirFirst = theirs.iterator().next();
        Greeter myFirst = mine.iterator().next();
        created();
        theirs.reload();
        mine.reload();
        assertEquals(Arrays.asList(), created());
        assertNotSame(theirFirst, theirs.iterator().next());
        List<String> theirOrder = created();
        assertNotSame(myFirst, mine.iterator().next());
        assertEquals(theirOrder, created());
    }

    /// An iterator taken before a reload refuses to go on: its place counts
    /// providers that are no longer there, and going on would create again
    /// one it has already handed out.
    @Test
    public void anIteratorFromBeforeAReloadRefusesToGoOn() {
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        java.util.Iterator<Greeter> before = mine.iterator();
        before.next();
        mine.reload();
        try {
            before.hasNext();
            fail("hasNext after reload");
        } catch (java.util.ConcurrentModificationException expected) {
            // as the JDK
        }
        try {
            before.next();
            fail("next after reload");
        } catch (java.util.ConcurrentModificationException expected) {
            // as the JDK
        }
        assertTrue(mine.iterator().hasNext());
    }

    @Test
    public void aServiceWithNoProviderHasNone() {
        assertFalse(java.util.ServiceLoader.load(Empty.class).iterator().hasNext());
        assertFalse(ServiceLoader.load(Empty.class).iterator().hasNext());
        assertFalse(java.util.ServiceLoader.load(Unlisted.class).iterator().hasNext());
        assertFalse(ServiceLoader.load(Unlisted.class).iterator().hasNext());
        assertFalse(ServiceLoader.load(Unlisted.class).findFirst().isPresent());
        assertFalse(ServiceLoader.load(Empty.class).findFirst().isPresent());
        assertEquals(0, ServiceLoader.load(Empty.class).stream().count());
        // What the application ships is never "installed in the platform".
        assertFalse(java.util.ServiceLoader.loadInstalled(Greeter.class).iterator().hasNext());
        ServiceLoader<Greeter> installed = ServiceLoader.loadInstalled(Greeter.class);
        assertFalse(installed.iterator().hasNext());
        installed.reload();
        assertFalse(installed.findFirst().isPresent());
        assertEquals(Arrays.asList(), created());
    }

    @Test
    public void theOtherWaysToLoadAnswerTheSameProviders() {
        ServiceLoader<Greeter> byLoader = ServiceLoader.load(Greeter.class, getClass().getClassLoader());
        assertEquals("hello", byLoader.iterator().next().greet());
        assertEquals("hello", ServiceLoader.load(Greeter.class, null).iterator().next().greet());
        assertEquals(java.util.ServiceLoader.load(Greeter.class).toString(),
                ServiceLoader.load(Greeter.class).toString());
        try {
            ServiceLoader.load((Class<Greeter>) null);
            fail();
        } catch (NullPointerException expected) {
            // As the JDK.
        }
        try {
            java.util.ServiceLoader.load((Class<Greeter>) null);
            fail();
        } catch (NullPointerException expected) {
            // The reference.
        }
    }

    @Test
    public void findFirstCreatesOnlyTheFirstAndForEachTheRest() {
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        Optional<Greeter> first = mine.findFirst();
        assertTrue(first.isPresent());
        assertEquals("hello", first.get().greet());
        assertEquals(Arrays.asList("English"), created());
        assertSame(first.get(), mine.findFirst().get());
        final List<String> said = new ArrayList<String>();
        mine.forEach(g -> said.add(g.greet()));
        assertEquals(Arrays.asList("hello", "bonjour", "hallo"), said);
        assertEquals(Arrays.asList("French", "German"), created());
        try {
            mine.forEach(null);
            fail();
        } catch (NullPointerException expected) {
            // As Iterable.forEach.
        }
        try {
            mine.iterator().remove();
            fail();
        } catch (UnsupportedOperationException expected) {
            // As the JDK.
        }
    }

    @Test
    public void aStreamCreatesNothingUntilAskedAndThenAnewEachTime() {
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        List<ServiceLoader.Provider<Greeter>> providers = mine.stream().collect(Collectors.toList());
        assertEquals(3, providers.size());
        assertEquals(Arrays.asList(), created());
        assertSame(French.class, providers.get(1).type());
        Greeter one = providers.get(1).get();
        Greeter two = providers.get(1).get();
        assertEquals("bonjour", one.greet());
        assertNotSame(one, two);
        assertEquals(Arrays.asList("French", "French"), created());
    }

    /// What the build generates for an application: the providers of each
    /// service in order, made without reflection.
    private static class Registry implements JdkHost.Registry {
        @Override
        public String[] providers(String service) {
            if (!Greeter.class.getName().equals(service)) {
                return null;
            }
            return new String[] {German.class.getName(), "java.lang.String", "no.such.Provider",
                English.class.getName()};
        }

        @Override
        public Object newInstance(String className) {
            if (German.class.getName().equals(className)) {
                return new German();
            }
            if (English.class.getName().equals(className)) {
                return new English();
            }
            return "java.lang.String".equals(className) ? "not a greeter" : null;
        }

        @Override
        public Class<?> type(String className) {
            if (German.class.getName().equals(className)) {
                return German.class;
            }
            if (English.class.getName().equals(className)) {
                return English.class;
            }
            return "java.lang.String".equals(className) ? String.class : null;
        }
    }

    @Test
    public void aBuiltApplicationHasTheProvidersItsRegistryListsAndNoOthers() {
        JdkHost.setRegistry(new Registry());
        ServiceLoader<Greeter> mine = ServiceLoader.load(Greeter.class);
        assertEquals(Arrays.asList(), created());
        List<String> said = new ArrayList<String>();
        for (Greeter g : mine) {
            said.add(g.greet());
        }
        // The registry's order, not the file's; what is no Greeter, or is not
        // there, is left out rather than handed over under the wrong type.
        assertEquals(Arrays.asList("hallo", "hello"), said);
        assertEquals(Arrays.asList("German", "English"), created());
        assertEquals(2, mine.stream().count());
        assertSame(German.class, mine.stream().findFirst().get().type());

        // A service the registry does not know has no provider, though a
        // file on the class path lists some: the registry is the whole answer.
        assertFalse(ServiceLoader.load(Empty.class).iterator().hasNext());
        JdkHost.setRegistry(new Registry() {
            @Override
            public String[] providers(String service) {
                return null;
            }
        });
        assertFalse(ServiceLoader.load(Greeter.class).iterator().hasNext());
        assertEquals(Arrays.asList(), created());
    }
}
