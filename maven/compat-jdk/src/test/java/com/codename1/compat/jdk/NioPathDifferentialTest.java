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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Every method of `Path` that only rearranges names, run on the same inputs
/// through this module's class and through the JDK's on a Unix file system.
public class NioPathDifferentialTest {

    private static final String[] PATHS = {"", "/", "a", "a/b", "/a", "/a/b", "/a/b/c", "a/b/c/d", ".", "..", "./a",
        "../a", "a/..", "a/./b", "a/../b", "/..", "/../a", "/a/../..", "a/b/../../..", "//a//b//", "a/", "/a/b/",
        "a.txt", "dir/a.txt", "/dir/sub/a.tar.gz", ".hidden", "/a/b/../c/./d", "x/y", "a/b/c", "b", "/b", "..//..",
        "a b/c d", "/usr/local/bin", "/usr/local", "local/bin", "bin"};

    private static String text(Object o) {
        return o == null ? "null" : o.toString();
    }

    private interface Probe {
        String jdk(java.nio.file.Path a, java.nio.file.Path b);

        String shim(Path a, Path b);
    }

    private static String run(Probe probe, boolean jdk, String a, String b) {
        try {
            return jdk ? probe.jdk(java.nio.file.Paths.get(a), java.nio.file.Paths.get(b))
                    : probe.shim(Paths.get(a), Paths.get(b));
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private static int sign(int n) {
        return n < 0 ? -1 : n > 0 ? 1 : 0;
    }

    private static String names(Iterator<?> it) {
        StringBuilder sb = new StringBuilder();
        while (it.hasNext()) {
            sb.append('<').append(it.next()).append('>');
        }
        return sb.toString();
    }

    private static List<Probe> unary() {
        List<Probe> out = new ArrayList<Probe>();
        out.add(new Probe() {
            @Override
            public String jdk(java.nio.file.Path a, java.nio.file.Path b) {
                return a + "|" + a.isAbsolute() + "|" + text(a.getRoot()) + "|" + text(a.getFileName()) + "|"
                        + text(a.getParent()) + "|" + a.getNameCount() + "|" + names(a.iterator()) + "|"
                        + a.normalize() + "|" + a.toFile().getPath() + "|" + a.hashCode() % 1;
            }

            @Override
            public String shim(Path a, Path b) {
                return a + "|" + a.isAbsolute() + "|" + text(a.getRoot()) + "|" + text(a.getFileName()) + "|"
                        + text(a.getParent()) + "|" + a.getNameCount() + "|" + names(a.iterator()) + "|"
                        + a.normalize() + "|" + a.toFile().getPath() + "|" + a.hashCode() % 1;
            }
        });
        out.add(new Probe() {
            @Override
            public String jdk(java.nio.file.Path a, java.nio.file.Path b) {
                StringBuilder sb = new StringBuilder();
                for (int i = -1; i <= a.getNameCount(); i++) {
                    try {
                        sb.append(a.getName(i)).append(',');
                    } catch (IllegalArgumentException e) {
                        sb.append("IAE,");
                    }
                    for (int j = i; j <= a.getNameCount() + 1; j++) {
                        try {
                            sb.append(a.subpath(i, j)).append(';');
                        } catch (IllegalArgumentException e) {
                            sb.append("IAE;");
                        }
                    }
                }
                return sb.toString();
            }

            @Override
            public String shim(Path a, Path b) {
                StringBuilder sb = new StringBuilder();
                for (int i = -1; i <= a.getNameCount(); i++) {
                    try {
                        sb.append(a.getName(i)).append(',');
                    } catch (IllegalArgumentException e) {
                        sb.append("IAE,");
                    }
                    for (int j = i; j <= a.getNameCount() + 1; j++) {
                        try {
                            sb.append(a.subpath(i, j)).append(';');
                        } catch (IllegalArgumentException e) {
                            sb.append("IAE;");
                        }
                    }
                }
                return sb.toString();
            }
        });
        return out;
    }

    private static List<Probe> binary() {
        List<Probe> out = new ArrayList<Probe>();
        out.add(new Probe() {
            @Override
            public String jdk(java.nio.file.Path a, java.nio.file.Path b) {
                return a.resolve(b) + "|" + a.resolve(b.toString()) + "|" + a.resolveSibling(b) + "|"
                        + a.resolveSibling(b.toString()) + "|" + a.startsWith(b) + "|" + a.startsWith(b.toString())
                        + "|" + a.endsWith(b) + "|" + a.endsWith(b.toString()) + "|" + sign(a.compareTo(b)) + "|"
                        + a.equals(b);
            }

            @Override
            public String shim(Path a, Path b) {
                return a.resolve(b) + "|" + a.resolve(b.toString()) + "|" + a.resolveSibling(b) + "|"
                        + a.resolveSibling(b.toString()) + "|" + a.startsWith(b) + "|" + a.startsWith(b.toString())
                        + "|" + a.endsWith(b) + "|" + a.endsWith(b.toString()) + "|" + sign(a.compareTo(b)) + "|"
                        + a.equals(b);
            }
        });
        out.add(new Probe() {
            @Override
            public String jdk(java.nio.file.Path a, java.nio.file.Path b) {
                return a.relativize(b).toString();
            }

            @Override
            public String shim(Path a, Path b) {
                return a.relativize(b).toString();
            }
        });
        return out;
    }

    @Test
    public void oneDescribedPathAnswersAsTheJdkDoes() {
        int compared = 0;
        for (Probe probe : unary()) {
            for (String a : PATHS) {
                assertEquals("on \"" + a + "\"", run(probe, true, a, ""), run(probe, false, a, ""));
                compared++;
            }
        }
        assertTrue(compared > 60);
    }

    /// `relativize` between a path with `.` or `..` names and another is
    /// where JDK releases disagree with each other, so those pairs are left
    /// to the other probes.
    private static boolean dotted(String p) {
        return p.equals(".") || p.equals("..") || p.startsWith("./") || p.startsWith("../") || p.contains("/./")
                || p.contains("/../") || p.endsWith("/..") || p.endsWith("/.");
    }

    @Test
    public void twoPathsCombineAsInTheJdk() {
        int compared = 0;
        List<Probe> probes = binary();
        for (String a : PATHS) {
            for (String b : PATHS) {
                assertEquals("\"" + a + "\" with \"" + b + "\"", run(probes.get(0), true, a, b),
                        run(probes.get(0), false, a, b));
                compared++;
                if (!dotted(a) && !dotted(b)) {
                    assertEquals("\"" + a + "\" relativize \"" + b + "\"", run(probes.get(1), true, a, b),
                            run(probes.get(1), false, a, b));
                    compared++;
                }
            }
        }
        assertTrue(compared > 1500);
    }

    /// What a current JDK answers for the dotted pairs the comparison above
    /// leaves out: both sides are normalized first, so a name that cancels
    /// the one before it is no step away from anything. The JDK these tests
    /// run on predates that and answers `../../b` for the first.
    @Test
    public void relativizeNormalizesDottedPaths() {
        String[][] cases = {{"a/..", "b", "b"}, {"a/../c", "c/d", "d"}, {"a/b", "a/./c", "../c"},
            {"/a/../b", "/b/c", "c"}, {"a", "../b", "../../b"}, {"a/./b", "a/b", ""}, {"a/b/..", "a", ""},
            {"", "a/../b", "a/../b"}};
        for (String[] c : cases) {
            assertEquals("\"" + c[0] + "\" relativize \"" + c[1] + "\"", c[2],
                    Paths.get(c[0]).relativize(Paths.get(c[1])).toString());
        }
        // A `..` that nothing cancels still leads nowhere that can be named.
        for (String from : new String[] {"../a", "a/../..", ".."}) {
            try {
                Paths.get(from).relativize(Paths.get("b"));
                fail(from);
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage(), expected.getMessage().startsWith("Unable to compute"));
            }
        }
    }

    @Test
    public void severalNamesJoinIntoOnePath() {
        String[][] cases = {{"a", "b", "c"}, {"/", "a"}, {"", "a"}, {"a", "", "b"}, {"/a/", "/b/"}, {"a"},
            {"", ""}, {"a/b", "c/d"}};
        for (String[] c : cases) {
            String[] more = new String[c.length - 1];
            System.arraycopy(c, 1, more, 0, more.length);
            assertEquals(java.nio.file.Paths.get(c[0], more).toString(), Paths.get(c[0], more).toString());
            assertEquals(java.nio.file.Paths.get(c[0], more).toString(), Path.of(c[0], more).toString());
        }
    }

    /// The form Codename One names files in is a root like any other.
    @Test
    public void aFileSchemePathHasItsSchemeForARoot() {
        Path p = Paths.get("file:///var/data/app/notes.txt");
        assertTrue(p.isAbsolute());
        assertEquals("file:///", p.getRoot().toString());
        assertEquals("notes.txt", p.getFileName().toString());
        assertEquals("file:///var/data/app", p.getParent().toString());
        assertEquals(4, p.getNameCount());
        assertEquals("file:///var/data/app/other.txt", p.resolveSibling("other.txt").toString());
        assertEquals("app/notes.txt", Paths.get("file:///var/data").relativize(p).toString());
        assertEquals("file:///var/notes.txt", Paths.get("file:///var/data/../notes.txt").normalize().toString());
        assertEquals("file:///var/data/app/notes.txt", new File("file:///var/data/app/notes.txt").toPath().toString());
        assertEquals("/var/data/app/notes.txt",
                Paths.get(java.net.URI.create("file:///var/data/app/notes.txt")).toString());
    }

    /// A name with a character a URI escapes still has a URI, and the
    /// escaped one: the storage path as typed is not a legal URI.
    @Test
    public void aPathWithASpaceOrAHashHasAnEscapedUri() {
        java.net.URI uri = Paths.get("file:///var/data/my notes#1 50%.txt").toUri();
        assertEquals("file", uri.getScheme());
        assertEquals("/var/data/my%20notes%231%2050%25.txt", uri.getRawPath());
        assertEquals("/var/data/my notes#1 50%.txt", uri.getPath());
        assertEquals(uri, new File("file:///var/data/my notes#1 50%.txt").toURI());
        assertEquals("/var/data/app/notes.txt", Paths.get("file:///var/data/app/notes.txt").toUri().getPath());
    }

    private static String ofUri(boolean jdk, String uri) {
        try {
            return jdk ? java.nio.file.Paths.get(java.net.URI.create(uri)).toString()
                    : Paths.get(java.net.URI.create(uri)).toString();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage();
        }
    }

    /// Only a `file:` URI that is a path and nothing else is one: a host, a
    /// query or a fragment is refused, not dropped.
    @Test
    public void aFileUriWithMoreThanAPathIsRefused() {
        for (String uri : new String[] {"file:/var/data/a.txt", "file:///var/data/a.txt", "file:/var/my%20data/a.txt",
            "file:/", "file:/var/data/a.txt?version=2", "file:/var/data/a.txt#top", "file://server/share/a.txt",
            "file://user@server:8080/a.txt", "file:relative/a.txt"}) {
            assertEquals(uri, ofUri(true, uri), ofUri(false, uri));
        }
        assertEquals("IllegalArgumentException: URI has a query component", ofUri(false, "file:/a?b"));
        assertEquals("IllegalArgumentException: URI has a fragment component", ofUri(false, "file:/a#b"));
        assertEquals("IllegalArgumentException: URI has an authority component", ofUri(false, "file://host/a"));
        assertEquals("IllegalArgumentException: URI is not hierarchical", ofUri(false, "file:a"));
    }
}
