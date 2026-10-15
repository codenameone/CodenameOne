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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/// Members an application compiled against the JDK calls, which the class
/// library carries under their own names: the sequenced methods of `List`, `CharSequence.chars` and
/// `codePoints`, `Throwable.printStackTrace(PrintWriter)`, the lazy `Files`
/// streams, `BufferedReader.lines`, `ServiceLoader.findFirst` and the URL
/// coders. Run on HotSpot (the JDK's own classes) and on ParparVM; the
/// RESULT lines must match. A case answers a string, or the class of what it
/// threw. Java 21 API.
public class SwitchoverMembersApp {

    static final int CASES = 40;

    static Path dir;

    /// Runs every case; the bulk program has installed its file system host
    /// on the ParparVM side (the translator allows one `main` per program).
    public static void fixtureMain(String[] args) throws Exception {
        dir = Files.createTempDirectory("switchover");
        Files.write(dir.resolve("a.txt"), Arrays.asList("x", "y", "z"), StandardCharsets.UTF_8);
        Files.createDirectories(dir.resolve("sub"));
        Files.write(dir.resolve("sub").resolve("b.txt"), Arrays.asList("one"), StandardCharsets.UTF_8);
        Files.write(dir.resolve("sub").resolve("c.log"), Arrays.asList("two"), StandardCharsets.UTF_8);
        StringBuilder all = new StringBuilder();
        for (int i = 0; i < CASES; i++) {
            String v;
            try {
                v = run(i);
            } catch (Throwable t) {
                v = "!" + t.getClass().getName();
            }
            if (v == null) {
                continue;
            }
            System.out.println("case " + i + " = " + v);
            all.append(i).append('=').append(v).append('|');
        }
        System.out.println("RESULT=" + all);
    }

    static List<String> list(String... values) {
        return new ArrayList<String>(Arrays.asList(values));
    }

    /// A list with no overrides of its own, so the interface's defaults run.
    static List<String> plain(String... values) {
        final List<String> backing = list(values);
        return new java.util.AbstractList<String>() {
            public String get(int index) {
                return backing.get(index);
            }

            public int size() {
                return backing.size();
            }

            public void add(int index, String s) {
                backing.add(index, s);
            }

            public String remove(int index) {
                return backing.remove(index);
            }

            public String set(int index, String s) {
                return backing.set(index, s);
            }
        };
    }

    /// Paths under the temporary directory, relative to it and sorted, so
    /// neither the directory's name nor the listing order shows.
    static String names(Stream<Path> paths) {
        try {
            final String root = dir.toString();
            return paths.map(new Function<Path, String>() {
                public String apply(Path p) {
                    String s = p.toString();
                    return s.length() > root.length() ? s.substring(root.length() + 1) : ".";
                }
            }).sorted().collect(Collectors.joining(","));
        } finally {
            paths.close();
        }
    }

    static String ints(java.util.stream.IntStream s) {
        StringBuilder sb = new StringBuilder();
        java.util.PrimitiveIterator.OfInt it = s.iterator();
        while (it.hasNext()) {
            sb.append(Integer.toHexString(it.nextInt())).append(',');
        }
        return sb.toString();
    }

    static String run(int i) throws Exception {
        String smile = new String(new int[] {0x1F600}, 0, 1);
        String eAcute = String.valueOf((char) 0xe9);
        switch (i) {
            // ---- List, sequenced
            case 0: {
                List<String> l = list("b");
                l.addFirst("a");
                l.addLast("c");
                return l + " " + l.getFirst() + l.getLast();
            }
            case 1: {
                List<String> l = plain("b");
                l.addFirst("a");
                l.addLast("c");
                return l + " " + l.getFirst() + l.getLast();
            }
            case 2: {
                List<String> l = plain("a", "b", "c");
                return l.removeFirst() + l.removeLast() + l;
            }
            case 3: return plain().getFirst();
            case 4: return plain().removeLast();
            case 5: return String.valueOf(plain("a", "b", "c").reversed());
            case 6: {
                List<String> l = plain("a", "b", "c");
                List<String> r = l.reversed();
                r.addFirst("d");
                r.removeLast();
                return l + " " + r + " " + r.reversed().equals(l) + " " + r.get(0) + r.size();
            }
            case 7: {
                List<String> l = new LinkedList<String>(Arrays.asList("x", "y"));
                l.addFirst("w");
                return l.reversed() + l.getLast();
            }
            case 8: {
                List<String> l = Collections.unmodifiableList(list("a"));
                l.addFirst("z");
                return String.valueOf(l);
            }
            case 9: return String.valueOf(plain("a", "b").reversed().subList(0, 1));
            // ---- CharSequence
            case 10: return ints("ab".chars());
            case 11: return ints(("a" + smile + "b").chars());
            case 12: return ints(("a" + smile + "b").codePoints());
            case 13: return ints(new StringBuilder("x" + smile).codePoints());
            case 14: return "" + new StringBuilder("hello").chars().filter(new java.util.function.IntPredicate() {
                public boolean test(int c) {
                    return c == 'l';
                }
            }).count();
            case 15: {
                // A sequence that is neither a String nor a builder: the
                // interface's defaults, a lone surrogate included.
                final String s = "q" + smile.charAt(0);
                CharSequence cs = new CharSequence() {
                    public int length() {
                        return s.length();
                    }

                    public char charAt(int index) {
                        return s.charAt(index);
                    }

                    public CharSequence subSequence(int start, int end) {
                        return s.subSequence(start, end);
                    }

                    public String toString() {
                        return s;
                    }
                };
                return ints(cs.chars()) + "/" + ints(cs.codePoints());
            }
            // ---- Throwable.printStackTrace(PrintWriter)
            case 16: {
                StringWriter w = new StringWriter();
                PrintWriter pw = new PrintWriter(w);
                new IllegalStateException("boom").printStackTrace(pw);
                pw.flush();
                String text = w.toString();
                int nl = text.indexOf('\n');
                return (nl < 0 ? text : text.substring(0, nl)).trim();
            }
            case 17: {
                StringWriter w = new StringWriter();
                PrintWriter pw = new PrintWriter(w);
                new RuntimeException("outer", new IOException("inner")).printStackTrace(pw);
                pw.flush();
                return "" + w.toString().contains("Caused by: java.io.IOException: inner");
            }
            // ---- Files, lazily
            case 18: {
                Stream<String> s = Files.lines(dir.resolve("a.txt"));
                try {
                    return s.collect(Collectors.joining(","));
                } finally {
                    s.close();
                }
            }
            case 19: {
                Stream<String> s = Files.lines(dir.resolve("a.txt"), StandardCharsets.UTF_8);
                try {
                    return "" + s.skip(1).findFirst().get();
                } finally {
                    s.close();
                }
            }
            case 20: return names(Files.list(dir));
            case 21: return names(Files.walk(dir));
            case 22: return names(Files.walk(dir, 1));
            case 23: return names(Files.find(dir, 10, new BiPredicate<Path, BasicFileAttributes>() {
                public boolean test(Path p, BasicFileAttributes a) {
                    return a.isRegularFile() && p.toString().endsWith(".txt");
                }
            }));
            case 24: return names(Files.find(dir, 0, new BiPredicate<Path, BasicFileAttributes>() {
                public boolean test(Path p, BasicFileAttributes a) {
                    return true;
                }
            }));
            case 25: return names(Files.list(dir.resolve("missing")));
            case 26: return names(Files.list(dir.resolve("a.txt")));
            case 27: return "" + Files.lines(dir.resolve("missing.txt")).count();
            // ---- BufferedReader.lines
            case 28: return new BufferedReader(new StringReader("a\nb\r\nc\rd")).lines()
                    .collect(Collectors.joining(","));
            case 29: {
                BufferedReader r = new BufferedReader(new StringReader("first\nsecond\nthird"));
                String first = r.readLine();
                return first + ":" + r.lines().count();
            }
            case 30: return "" + new BufferedReader(new StringReader("")).lines().count();
            // ---- ServiceLoader
            case 31: return "" + ServiceLoader.load(Runnable.class).findFirst().isPresent();
            // ---- URLEncoder and URLDecoder
            case 32: return URLEncoder.encode("a b&c=d/e~*._-" + eAcute, "UTF-8");
            case 33: return URLEncoder.encode("x+y " + smile, StandardCharsets.UTF_8);
            case 34: return ints(URLDecoder.decode("a+b%26c%3Dd%2F%C3%A9", "UTF-8").codePoints());
            case 35: return ints(URLDecoder.decode("%F0%9F%98%80+", StandardCharsets.UTF_8).codePoints());
            case 36: return URLDecoder.decode("%zz", "UTF-8");
            case 37: return URLDecoder.decode("50%", "UTF-8");
            case 38: return URLEncoder.encode("x", "no-such-charset");
            case 39: return URLDecoder.decode(URLEncoder.encode("round trip?" + eAcute, "UTF-8"), "UTF-8")
                    .equals("round trip?" + eAcute) ? "same" : "differs";
            default: return null;
        }
    }
}
