/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

/**
 * Dart's {@code dart:core} {@code RegExpMatch} (a {@code Match}).
 *
 * <p>Group text and offsets are SNAPSHOTTED at construction. The regex engine
 * behind {@link RegExp} keeps its match state on the compiled pattern object
 * and overwrites it on the next match, so a Match holding a reference to it
 * would silently change under the caller — a Dart Match is a value.</p>
 *
 * <p>Dart group indices are 0-based with group 0 being the whole match;
 * unmatched groups are {@code null}.</p>
 */
public final class RegExpMatch {

    private final String[] groups;
    private final int[] starts;
    private final int[] ends;
    private final String input;

    private final String[] names;

    /// The Pattern that produced this match: the RegExp, or the String of a
    /// literal String.allMatches. Set once by the producer.
    Object pattern;

    RegExpMatch(String[] groups, int[] starts, int[] ends, String input) {
        this(groups, starts, ends, input, null);
    }

    RegExpMatch(String[] groups, int[] starts, int[] ends, String input, String[] names) {
        this.names = names;
        this.groups = groups;
        this.starts = starts;
        this.ends = ends;
        this.input = input;
    }

    /// Dart's {@code RegExpMatch.namedGroup(name)}: the text of the group named
    /// {@code name}, null when it did not take part; ArgumentError for a name the
    /// pattern does not declare.
    public String namedGroup(String name) {
        if (names != null) {
            for (int i = 1; i < names.length; i++) {
                if (name != null && name.equals(names[i])) {
                    return i < groups.length ? groups[i] : null;
                }
            }
        }
        throw new ArgumentError("Not a capture group name: " + name);
    }

    /// Dart's {@code RegExpMatch.groupNames}, in declaration order.
    public DartList<String> groupNames() {
        DartList<String> out = new DartList<String>(dart.runtime.TypeWitness.of(String.class, false));
        if (names != null) {
            for (int i = 1; i < names.length; i++) {
                if (names[i] != null) {
                    out.add(names[i]);
                }
            }
        }
        return out;
    }

    /** Dart's {@code Match.group(index)} — null for an unmatched group. */
    public String group(long index) {
        // Checked as the Dart int it is, BEFORE narrowing: (int) (1 << 32) is 0, so a
        // narrowed check let group(4294967296) answer group 0 instead of failing.
        // DString and RegExp were swept for the same pattern; every other narrowing
        // there is already preceded by a range check on the long.
        if (index < 0 || index >= groups.length) {
            throw new RangeError("group index out of range: " + index);
        }
        return groups[(int) index];
    }

    /** Dart's {@code match[index]} operator. */
    public String idx(long index) {
        return group(index);
    }

    /** Dart's {@code Match.groupCount} getter — number of capturing groups. */
    /// Dart's {@code operator []} under the transpiler's operator name.
    public String $index(long index) {
        return group(index);
    }

    public long groupCount() {
        return groups.length - 1;
    }

    /** Dart's {@code Match.start} getter. */
    public long start() {
        return starts[0];
    }

    /** Dart's {@code Match.end} getter. */
    public long end() {
        return ends[0];
    }

    /** Dart's {@code Match.input} getter. */
    /// Dart's {@code Match.pattern}.
    public Object pattern() {
        return pattern;
    }

    public String input() {
        return input;
    }

    /** Dart's {@code Match.groups(indices)} — the listed groups in order. */
    public DartList<String> groups(java.util.List<? extends Number> indices) {
        DartList<String> out = new DartList<String>(dart.runtime.TypeWitness.of(String.class, false));
        for (Number n : indices) {
            out.add(group(n.longValue()));
        }
        return out;
    }
}
