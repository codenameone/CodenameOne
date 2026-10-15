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

import java.util.AbstractList;
import java.util.List;

/// Members of `String`, `StringBuilder`, `StringBuffer` and the Java 21
/// sequenced `List` the device lacks, as static methods that take the
/// receiver first.
public final class JdkText {

    private JdkText() {
    }

    public static boolean isEmpty(CharSequence s) {
        return s.length() == 0;
    }

    private static void checkCodePoint(int codePoint) {
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            StringBuilder hex = new StringBuilder("Not a valid Unicode code point: 0x");
            boolean started = false;
            for (int shift = 28; shift >= 0; shift -= 4) {
                int digit = (codePoint >>> shift) & 0xF;
                if (digit != 0 || started || shift == 0) {
                    started = true;
                    hex.append("0123456789ABCDEF".charAt(digit));
                }
            }
            throw new IllegalArgumentException(hex.toString());
        }
    }

    public static StringBuilder appendCodePoint(StringBuilder sb, int codePoint) {
        checkCodePoint(codePoint);
        if (codePoint < 0x10000) {
            return sb.append((char) codePoint);
        }
        int v = codePoint - 0x10000;
        return sb.append((char) (0xD800 + (v >> 10))).append((char) (0xDC00 + (v & 0x3FF)));
    }

    public static StringBuffer appendCodePoint(StringBuffer sb, int codePoint) {
        checkCodePoint(codePoint);
        if (codePoint < 0x10000) {
            return sb.append((char) codePoint);
        }
        int v = codePoint - 0x10000;
        return sb.append((char) (0xD800 + (v >> 10))).append((char) (0xDC00 + (v & 0x3FF)));
    }

    /// `Character.toString(int)`.
    public static String codePointToString(int codePoint) {
        return appendCodePoint(new StringBuilder(2), codePoint).toString();
    }

    /// The code points of `s`: a surrogate pair is one element, a
    /// surrogate without its other half is itself. Read as the stream asks
    /// for them, as `chars()` is, so a StringBuilder that grows between the
    /// call and the traversal is read as it is then, as the JDK's is.
    public static IntStream codePoints(final CharSequence s) {
        if (s == null) {
            throw new NullPointerException();
        }
        return new IntPipeline(ObjPipeline.over(new PullIterator<Integer>() {
            private int at;

            @Override
            boolean advance() {
                int n = s.length();
                if (at >= n) {
                    return false;
                }
                char c = s.charAt(at++);
                if (c >= 0xD800 && c <= 0xDBFF && at < n) {
                    char d = s.charAt(at);
                    if (d >= 0xDC00 && d <= 0xDFFF) {
                        at++;
                        emit(Integer.valueOf(0x10000 + ((c - 0xD800) << 10) + (d - 0xDC00)));
                        return true;
                    }
                }
                emit(Integer.valueOf(c));
                return true;
            }
        }));
    }

    // ---------------------------------------------------------------
    // The sequenced List of Java 21
    // ---------------------------------------------------------------

    public static <E> E getFirst(List<E> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.get(0);
    }

    public static <E> E getLast(List<E> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.get(list.size() - 1);
    }

    public static <E> E removeFirst(List<E> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.remove(0);
    }

    public static <E> E removeLast(List<E> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.remove(list.size() - 1);
    }

    public static <E> void addFirst(List<E> list, E element) {
        list.add(0, element);
    }

    public static <E> void addLast(List<E> list, E element) {
        list.add(element);
    }

    /// `list` seen last to first: a view, so a change made through either
    /// shows in the other.
    public static <E> List<E> reversed(final List<E> list) {
        return new AbstractList<E>() {
            @Override
            public int size() {
                return list.size();
            }

            @Override
            public E get(int index) {
                return list.get(list.size() - 1 - index);
            }

            @Override
            public E set(int index, E element) {
                return list.set(list.size() - 1 - index, element);
            }

            @Override
            public void add(int index, E element) {
                list.add(list.size() - index, element);
            }

            @Override
            public E remove(int index) {
                return list.remove(list.size() - 1 - index);
            }
        };
    }
}
