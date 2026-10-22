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

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The harness of the bulk conformance program (BulkConformanceIntegrationTest).
///
/// The program runs twice: on HotSpot, where every `java.*` class is the JDK's,
/// and translated by ParparVM, where every one is JavaAPI's. Each named check
/// prints a header line and then what it observed; the test compares the two
/// runs check by check and names every check whose lines differ.
///
/// Rules for a check, so that the two runs are comparable at all:
///
/// - print values, never identity hash codes, timings, absolute paths
///   ([#rel]) or the iteration order of a hash collection ([#sorted]);
/// - a throw the check expects is reported with [#thrown], which compares
///   by type the way `assertThrows` does; an unexpected one ends the check
///   with a `THREW` line and the program goes on with the next check;
/// - behaviour that is Codename One's own and not the JDK's goes in a
///   [#cn1Only] check, which asserts with [#expect] and runs on ParparVM only;
/// - a check that compares with a JDK behaviour that changed after Java 8
///   calls [#skipOnHotSpot] when the HotSpot run is a later JDK.
public final class Bulk {
    /// A check's body.
    public interface Body {
        /// Runs the check.
        void run() throws Throwable;
    }

    private static final boolean HOTSPOT = "hotspot".equals(System.getProperty("bulk.host"));
    private static final Set<String> NAMES = new HashSet<String>();
    private static int count;

    private Bulk() {
    }

    /// True in the HotSpot run, where the JDK answers.
    public static boolean onHotSpot() {
        return HOTSPOT;
    }

    /// Called first by the generated main class.
    public static void begin() {
        if (!HOTSPOT) {
            BulkHost.installDefault();
        }
    }

    /// Called last by the generated main class. Exits, so that shutdown hooks
    /// run and a pool a check forgot cannot keep the program alive.
    public static void end() {
        header("(end)");
        System.out.println("checks=" + count);
        System.out.flush();
        System.exit(0);
    }

    private static void header(String name) {
        if (!NAMES.add(name)) {
            throw new IllegalStateException("Two checks are named " + name);
        }
        System.out.println("### " + name);
        System.out.flush();
    }

    /// Runs `body` as the check `name`.
    public static void check(String name, Body body) {
        header(name);
        count++;
        try {
            body.run();
        } catch (Throwable t) {
            System.out.println("THREW " + t.getClass().getName() + ": " + esc(String.valueOf(t.getMessage())));
        }
        after();
    }

    /// Runs `body` as the check `name` on ParparVM only: Codename One
    /// behaviour that the JDK does not share, asserted with [#expect]. The
    /// HotSpot run prints `CN1-ONLY`, and the test then requires the ParparVM
    /// run to print no `FAIL` and no `THREW` line for it.
    public static void cn1Only(String name, Body body) {
        header(name);
        count++;
        if (HOTSPOT) {
            System.out.println("CN1-ONLY");
            return;
        }
        try {
            body.run();
        } catch (Throwable t) {
            System.out.println("THREW " + t.getClass().getName() + ": " + esc(String.valueOf(t.getMessage())));
        }
        after();
    }

    private static void after() {
        // A check must not leave the next one interrupted or on its own host.
        Thread.interrupted();
        if (!HOTSPOT) {
            BulkHost.installDefault();
        }
        System.out.flush();
    }

    /// In a [#cn1Only] check: prints `FAIL what` unless `ok`.
    public static void expect(boolean ok, String what) {
        if (!ok) {
            System.out.println("FAIL " + esc(what));
        }
    }

    /// In a [#cn1Only] check: prints `FAIL` unless `expected` equals `actual`.
    public static void expectEquals(Object expected, Object actual, String what) {
        boolean same = expected == null ? actual == null : expected.equals(actual);
        if (!same) {
            System.out.println("FAIL " + esc(what) + ": expected <" + fmt(expected) + "> but was <" + fmt(actual) + ">");
        }
    }

    /// On HotSpot, when `skip`, prints `SKIPPED reason` and answers true: the
    /// test then ignores the check. ParparVM always answers false and runs it.
    public static boolean skipOnHotSpot(boolean skip, String reason) {
        if (HOTSPOT && skip) {
            System.out.println("SKIPPED " + reason);
            return true;
        }
        return false;
    }

    /// True when the HotSpot run is Java 8.
    public static boolean hotSpotIsJava8() {
        String v = System.getProperty("java.specification.version");
        return v != null && v.startsWith("1.");
    }

    /// Prints `key=value`.
    public static void p(String key, Object value) {
        System.out.println(key + "=" + fmt(value));
    }

    /// Prints `key=value` for a boolean.
    public static void p(String key, boolean value) {
        System.out.println(key + "=" + value);
    }

    /// Prints `key=value` for an int.
    public static void p(String key, int value) {
        System.out.println(key + "=" + value);
    }

    /// Prints `key=value` for a long.
    public static void p(String key, long value) {
        System.out.println(key + "=" + value);
    }

    /// Prints `key=value` for a double.
    public static void p(String key, double value) {
        System.out.println(key + "=" + value);
    }

    /// Prints `key=value` for a char.
    public static void p(String key, char value) {
        System.out.println(key + "=" + esc(String.valueOf(value)));
    }

    /// What `body` threw, as `assertThrows(expected, body)` sees it: the
    /// simple name of `expected` when it threw one, `none` when it threw
    /// nothing, otherwise `other:` and the class it threw.
    public static String thrown(Class<?> expected, Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            if (expected.isInstance(t)) {
                return expected.getSimpleName();
            }
            return "other:" + t.getClass().getName();
        }
        return "none";
    }

    /// Prints `key=` and what [#thrown] answers.
    public static void pThrown(String key, Class<?> expected, Body body) {
        p(key, thrown(expected, body));
    }

    /// The class name of what `body` threw, or `none`: for a check that
    /// compares the exact type.
    public static String thrownClass(Body body) {
        try {
            body.run();
        } catch (Throwable t) {
            return t.getClass().getName();
        }
        return "none";
    }

    /// The elements of `c` in their natural order, so a hash collection
    /// prints the same on both runs.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> sorted(Collection<?> c) {
        List list = new ArrayList(c);
        Collections.sort(list);
        return list;
    }

    /// The entries of `m` as `k=v`, sorted by key.
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String sortedMap(Map<?, ?> m) {
        List keys = new ArrayList(m.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(fmt(keys.get(i))).append('=').append(fmt(m.get(keys.get(i))));
        }
        return sb.append('}').toString();
    }

    /// `path` relative to `root`, with forward slashes, so a check never
    /// prints a temporary directory.
    public static String rel(String root, String path) {
        String r = root.replace('\\', '/');
        String p = path.replace('\\', '/');
        if (p.equals(r)) {
            return ".";
        }
        if (p.startsWith(r + "/")) {
            return p.substring(r.length() + 1);
        }
        return p;
    }

    /// The value as the checks print it: arrays by content, every character
    /// outside printable ASCII escaped so that a line stays one line and the
    /// two runs' output encodings cannot differ.
    public static String fmt(Object v) {
        if (v == null) {
            return "null";
        }
        String s;
        if (v instanceof Object[]) {
            s = Arrays.deepToString((Object[]) v);
        } else if (v instanceof int[]) {
            s = Arrays.toString((int[]) v);
        } else if (v instanceof long[]) {
            s = Arrays.toString((long[]) v);
        } else if (v instanceof double[]) {
            s = Arrays.toString((double[]) v);
        } else if (v instanceof byte[]) {
            s = Arrays.toString((byte[]) v);
        } else if (v instanceof char[]) {
            s = new String((char[]) v);
        } else if (v instanceof boolean[]) {
            s = Arrays.toString((boolean[]) v);
        } else if (v instanceof float[]) {
            s = Arrays.toString((float[]) v);
        } else if (v instanceof short[]) {
            s = Arrays.toString((short[]) v);
        } else {
            s = String.valueOf(v);
        }
        return esc(s);
    }

    /// `s` with every character outside printable ASCII written as a
    /// backslash, `u` and four hex digits.
    public static String esc(String s) {
        StringBuilder sb = null;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x20 || c > 0x7e) {
                if (sb == null) {
                    sb = new StringBuilder(s.length() + 16);
                    sb.append(s, 0, i);
                }
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int k = hex.length(); k < 4; k++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? s : sb.toString();
    }
}
