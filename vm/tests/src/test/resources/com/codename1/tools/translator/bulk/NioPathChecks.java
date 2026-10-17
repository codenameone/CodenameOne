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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/// Every method of java.nio.file.Path that only rearranges names, on the
/// same inputs, as a Unix file system answers them. Every path here is
/// described, never opened, so none of them is a real directory.
public final class NioPathChecks {
    private NioPathChecks() {
    }

    private static final String[] PATHS = {"", "/", "a", "a/b", "/a", "/a/b", "/a/b/c", "a/b/c/d", ".", "..", "./a",
        "../a", "a/..", "a/./b", "a/../b", "/..", "/../a", "/a/../..", "a/b/../../..", "//a//b//", "a/", "/a/b/",
        "a.txt", "dir/a.txt", "/dir/sub/a.tar.gz", ".hidden", "/a/b/../c/./d", "x/y", "a/b/c", "b", "/b", "..//..",
        "a b/c d", "/usr/local/bin", "/usr/local", "local/bin", "bin"};

    private interface Probe {
        String run(Path a, Path b);
    }

    private static String text(Object o) {
        return o == null ? "null" : o.toString();
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

    private static String run(Probe probe, String a, String b) {
        try {
            return probe.run(Paths.get(a), Paths.get(b));
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    private static final Probe DESCRIBE = (a, b) -> a + "|" + a.isAbsolute() + "|" + text(a.getRoot()) + "|"
            + text(a.getFileName()) + "|" + text(a.getParent()) + "|" + a.getNameCount() + "|" + names(a.iterator())
            + "|" + a.normalize() + "|" + a.toFile().getPath() + "|" + a.hashCode() % 1;

    private static final Probe SUBPATHS = (a, b) -> {
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
    };

    private static final Probe COMBINE = (a, b) -> a.resolve(b) + "|" + a.resolve(b.toString()) + "|"
            + a.resolveSibling(b) + "|" + a.resolveSibling(b.toString()) + "|" + a.startsWith(b) + "|"
            + a.startsWith(b.toString()) + "|" + a.endsWith(b) + "|" + a.endsWith(b.toString()) + "|"
            + sign(a.compareTo(b)) + "|" + a.equals(b);

    private static final Probe RELATIVIZE = (a, b) -> a.relativize(b).toString();

    /// `relativize` between a path with `.` or `..` names and another is
    /// where JDK releases disagree with each other, so those pairs are left
    /// to [#run]'s `relativizeNormalizesDottedPaths`.
    private static boolean dotted(String p) {
        return p.equals(".") || p.equals("..") || p.startsWith("./") || p.startsWith("../") || p.contains("/./")
                || p.contains("/../") || p.endsWith("/..") || p.endsWith("/.");
    }

    private static String ofUri(String uri) {
        try {
            return Paths.get(URI.create(uri)).toString();
        } catch (IllegalArgumentException e) {
            return "IllegalArgumentException: " + e.getMessage();
        }
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("NioPath.oneDescribedPathAnswersAsTheJdkDoes", () -> {
            for (String a : PATHS) {
                Bulk.p("describe[" + a + "]", run(DESCRIBE, a, ""));
            }
            for (String a : PATHS) {
                Bulk.p("subpaths[" + a + "]", run(SUBPATHS, a, ""));
            }
        });
        Bulk.check("NioPath.twoPathsCombineAsInTheJdk", () -> {
            for (String a : PATHS) {
                for (String b : PATHS) {
                    Bulk.p("[" + a + "][" + b + "]", run(COMBINE, a, b));
                    if (!dotted(a) && !dotted(b)) {
                        Bulk.p("relativize[" + a + "][" + b + "]", run(RELATIVIZE, a, b));
                    }
                }
            }
        });
        // What a current JDK answers for the dotted pairs the comparison above
        // leaves out: both sides are normalized first, so a name that cancels
        // the one before it is no step away from anything.
        Bulk.check("NioPath.relativizeNormalizesDottedPaths", () -> {
            String[][] cases = {{"a/..", "b"}, {"a/../c", "c/d"}, {"a/b", "a/./c"}, {"/a/../b", "/b/c"},
                {"a", "../b"}, {"a/./b", "a/b"}, {"a/b/..", "a"}, {"", "a/../b"}};
            for (String[] c : cases) {
                Bulk.p("[" + c[0] + "][" + c[1] + "]", Paths.get(c[0]).relativize(Paths.get(c[1])).toString());
            }
            // A `..` that nothing cancels still leads nowhere that can be named.
            for (String from : new String[] {"../a", "a/../..", ".."}) {
                String answer;
                try {
                    answer = "none: " + Paths.get(from).relativize(Paths.get("b"));
                } catch (IllegalArgumentException expected) {
                    answer = "IAE " + String.valueOf(expected.getMessage()).startsWith("Unable to compute");
                }
                Bulk.p("[" + from + "][b]", answer);
            }
        });
        Bulk.check("NioPath.severalNamesJoinIntoOnePath", () -> {
            String[][] cases = {{"a", "b", "c"}, {"/", "a"}, {"", "a"}, {"a", "", "b"}, {"/a/", "/b/"}, {"a"},
                {"", ""}, {"a/b", "c/d"}};
            for (String[] c : cases) {
                String[] more = new String[c.length - 1];
                System.arraycopy(c, 1, more, 0, more.length);
                Bulk.p("get" + Bulk.fmt(c), Paths.get(c[0], more).toString());
                Bulk.p("of" + Bulk.fmt(c), Path.of(c[0], more).toString());
            }
        });
        // The form Codename One names files in is a root like any other; the
        // JDK reads `file:` as a relative name.
        Bulk.cn1Only("NioPath.aFileSchemePathHasItsSchemeForARoot", () -> {
            Path p = Paths.get("file:///var/data/app/notes.txt");
            Bulk.expect(p.isAbsolute(), "absolute");
            Bulk.expectEquals("file:///", p.getRoot().toString(), "root");
            Bulk.expectEquals("notes.txt", p.getFileName().toString(), "file name");
            Bulk.expectEquals("file:///var/data/app", p.getParent().toString(), "parent");
            Bulk.expectEquals(Integer.valueOf(4), Integer.valueOf(p.getNameCount()), "name count");
            Bulk.expectEquals("file:///var/data/app/other.txt", p.resolveSibling("other.txt").toString(),
                    "resolveSibling");
            Bulk.expectEquals("app/notes.txt", Paths.get("file:///var/data").relativize(p).toString(), "relativize");
            Bulk.expectEquals("file:///var/notes.txt",
                    Paths.get("file:///var/data/../notes.txt").normalize().toString(), "normalize");
            Bulk.expectEquals("/var/data/app/notes.txt",
                    Paths.get(URI.create("file:///var/data/app/notes.txt")).toString(), "of a URI");
        });
        // A name with a character a URI escapes has the URI the JDK gives it.
        Bulk.check("NioPath.aPathWithASpaceOrAHashHasAnEscapedUri", () -> {
            String name = "/var/data/my notes#1 50%.txt";
            URI uri = Paths.get(name).toUri();
            // Compared with URI.equals, as the original did: the JDK writes an
            // empty authority (`file:///`) and JavaAPI none (`file:/`), which
            // URI.equals treats as the same URI.
            Bulk.p("uriEquals", uri.equals(URI.create("file:///var/data/my%20notes%231%2050%25.txt")));
            Bulk.p("scheme", uri.getScheme());
            Bulk.p("authority", uri.getRawAuthority());
            Bulk.p("rawPath", uri.getRawPath());
            Bulk.p("path", uri.getPath());
        });
        // Only a `file:` URI that is a path and nothing else is one: a host, a
        // query or a fragment is refused, not dropped.
        Bulk.check("NioPath.aFileUriWithMoreThanAPathIsRefused", () -> {
            for (String uri : new String[] {"file:/var/data/a.txt", "file:///var/data/a.txt",
                "file:/var/my%20data/a.txt", "file:/", "file:/var/data/a.txt?version=2", "file:/var/data/a.txt#top",
                "file://server/share/a.txt", "file://user@server:8080/a.txt", "file:relative/a.txt", "file:/a?b",
                "file:/a#b", "file://host/a", "file:a"}) {
                Bulk.p(uri, ofUri(uri));
            }
        });
    }
}
