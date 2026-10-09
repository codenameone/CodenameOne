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
import java.util.List;
import java.util.function.Function;

/// `java.util.regex.Matcher` over this module's [Pattern].
///
/// Everything a match is read through is here -- `matches`, `lookingAt`,
/// `find`, groups by number and by name, regions, and the replacement
/// methods with the JDK's `$1` and `${name}` syntax. `hitEnd`,
/// `requireEnd` and the two bounds switches are not provided.
public final class Matcher implements MatchResult {

    private Pattern pattern;
    CharSequence text;
    int from;
    int to;

    /// The state the nodes of a pattern read and write while matching.
    boolean requireEnd;
    int matchEnd;
    int endAt = -1;
    int subEnd;
    int oldLast = -1;
    int[] starts;
    int[] ends;
    int[] opened;
    int[] loopCount;
    int[] loopStart;

    private int first = -1;
    private int last;
    private int appendPosition;

    Matcher(Pattern pattern, CharSequence text) {
        if (text == null) {
            throw new NullPointerException();
        }
        this.text = text;
        use(pattern);
        reset();
    }

    private void use(Pattern p) {
        pattern = p;
        starts = new int[p.groupCount + 1];
        ends = new int[p.groupCount + 1];
        opened = new int[p.groupCount + 1];
        loopCount = new int[p.loopCount];
        loopStart = new int[p.loopCount];
    }

    int[] saveGroups() {
        int n = starts.length;
        if (n == 1) {
            return null;
        }
        int[] saved = new int[n * 2];
        System.arraycopy(starts, 0, saved, 0, n);
        System.arraycopy(ends, 0, saved, n, n);
        return saved;
    }

    void restoreGroups(int[] saved) {
        if (saved != null) {
            int n = starts.length;
            System.arraycopy(saved, 0, starts, 0, n);
            System.arraycopy(saved, n, ends, 0, n);
        }
    }

    private void clearGroups() {
        for (int g = 0; g < starts.length; g++) {
            starts[g] = -1;
            ends[g] = -1;
            opened[g] = -1;
        }
    }

    public Pattern pattern() {
        return pattern;
    }

    public Matcher usePattern(Pattern newPattern) {
        if (newPattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }
        use(newPattern);
        clearGroups();
        first = -1;
        return this;
    }

    public Matcher reset() {
        first = -1;
        last = 0;
        oldLast = -1;
        appendPosition = 0;
        from = 0;
        to = text.length();
        clearGroups();
        return this;
    }

    public Matcher reset(CharSequence input) {
        if (input == null) {
            throw new NullPointerException();
        }
        text = input;
        return reset();
    }

    public Matcher region(int start, int end) {
        if (start < 0 || start > text.length() || end < 0 || end > text.length() || start > end) {
            throw new IndexOutOfBoundsException("start " + start + ", end " + end);
        }
        reset();
        from = start;
        to = end;
        return this;
    }

    public int regionStart() {
        return from;
    }

    public int regionEnd() {
        return to;
    }

    private boolean attempt(int start, boolean whole) {
        clearGroups();
        requireEnd = whole;
        endAt = -1;
        if (oldLast < 0) {
            oldLast = start;
        }
        boolean matched = pattern.root.match(this, start);
        if (matched) {
            first = start;
            last = matchEnd;
            starts[0] = start;
            ends[0] = matchEnd;
        } else {
            first = -1;
            clearGroups();
        }
        oldLast = last;
        return matched;
    }

    public boolean matches() {
        return attempt(from, true);
    }

    public boolean lookingAt() {
        return attempt(from, false);
    }

    private boolean search(int start) {
        if (oldLast < 0) {
            oldLast = start;
        }
        requireEnd = false;
        endAt = -1;
        for (int i = start; i <= to; i++) {
            clearGroups();
            if (pattern.root.match(this, i)) {
                first = i;
                last = matchEnd;
                starts[0] = i;
                ends[0] = matchEnd;
                oldLast = last;
                return true;
            }
        }
        first = -1;
        clearGroups();
        oldLast = last;
        return false;
    }

    public boolean find() {
        int next = last;
        if (next == first) {
            next++;
        }
        if (next < from) {
            next = from;
        }
        if (next > to) {
            first = -1;
            clearGroups();
            return false;
        }
        return search(next);
    }

    public boolean find(int start) {
        if (start < 0 || start > text.length()) {
            throw new IndexOutOfBoundsException("Illegal start index");
        }
        reset();
        return search(start);
    }

    /// Whether the last match operation found something.
    public boolean hasMatch() {
        return first >= 0;
    }

    private void requireMatch() {
        if (first < 0) {
            throw new IllegalStateException("No match found");
        }
    }

    private void requireGroup(int group) {
        requireMatch();
        if (group < 0 || group > pattern.groupCount) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    private int named(String name) {
        if (name == null) {
            throw new NullPointerException("Null group name");
        }
        Integer group = pattern.names.get(name);
        if (group == null) {
            throw new IllegalArgumentException("No group with name <" + name + ">");
        }
        return group.intValue();
    }

    @Override
    public int start() {
        requireMatch();
        return first;
    }

    @Override
    public int start(int group) {
        requireGroup(group);
        return starts[group];
    }

    public int start(String name) {
        requireMatch();
        return starts[named(name)];
    }

    @Override
    public int end() {
        requireMatch();
        return last;
    }

    @Override
    public int end(int group) {
        requireGroup(group);
        return ends[group];
    }

    public int end(String name) {
        requireMatch();
        return ends[named(name)];
    }

    @Override
    public String group() {
        return group(0);
    }

    @Override
    public String group(int group) {
        requireGroup(group);
        if (starts[group] < 0 || ends[group] < 0) {
            return null;
        }
        return text.subSequence(starts[group], ends[group]).toString();
    }

    public String group(String name) {
        requireMatch();
        int group = named(name);
        if (starts[group] < 0 || ends[group] < 0) {
            return null;
        }
        return text.subSequence(starts[group], ends[group]).toString();
    }

    @Override
    public int groupCount() {
        return pattern.groupCount;
    }

    /// The match as it is now, unaffected by what this matcher does next.
    public MatchResult toMatchResult() {
        return new Snapshot(this);
    }

    private static final class Snapshot implements MatchResult {
        private final int[] starts;
        private final int[] ends;
        private final String text;
        private final boolean matched;

        Snapshot(Matcher m) {
            matched = m.first >= 0;
            starts = new int[m.starts.length];
            ends = new int[m.ends.length];
            System.arraycopy(m.starts, 0, starts, 0, starts.length);
            System.arraycopy(m.ends, 0, ends, 0, ends.length);
            text = m.text.toString();
        }

        private void check(int group) {
            if (!matched) {
                throw new IllegalStateException("No match found");
            }
            if (group < 0 || group >= starts.length) {
                throw new IndexOutOfBoundsException("No group " + group);
            }
        }

        @Override
        public int start() {
            return start(0);
        }

        @Override
        public int start(int group) {
            check(group);
            return starts[group];
        }

        @Override
        public int end() {
            return end(0);
        }

        @Override
        public int end(int group) {
            check(group);
            return ends[group];
        }

        @Override
        public String group() {
            return group(0);
        }

        @Override
        public String group(int group) {
            check(group);
            return starts[group] < 0 || ends[group] < 0 ? null : text.substring(starts[group], ends[group]);
        }

        @Override
        public int groupCount() {
            return starts.length - 1;
        }
    }

    /// Every match from here on, each as its own result.
    public Stream<MatchResult> results() {
        List<MatchResult> all = new ArrayList<MatchResult>();
        reset();
        while (find()) {
            all.add(toMatchResult());
        }
        return JdkCollections.stream(all);
    }

    /// A replacement in which `$` and backslash stand for themselves.
    public static String quoteReplacement(String s) {
        if (s.indexOf('\\') < 0 && s.indexOf('$') < 0) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void expand(String replacement, StringBuilder out) {
        int cursor = 0;
        int length = replacement.length();
        while (cursor < length) {
            char c = replacement.charAt(cursor);
            if (c == '\\') {
                cursor++;
                if (cursor == length) {
                    throw new IllegalArgumentException("character to be escaped is missing");
                }
                out.append(replacement.charAt(cursor));
                cursor++;
            } else if (c == '$') {
                cursor++;
                if (cursor == length) {
                    throw new IllegalArgumentException("Illegal group reference: group index is missing");
                }
                char next = replacement.charAt(cursor);
                int group;
                if (next == '{') {
                    cursor++;
                    int start = cursor;
                    while (cursor < length) {
                        char n = replacement.charAt(cursor);
                        if (Pattern.asciiLetter(n) || n >= '0' && n <= '9') {
                            cursor++;
                        } else {
                            break;
                        }
                    }
                    if (cursor == start) {
                        throw new IllegalArgumentException("named capturing group has 0 length name");
                    }
                    if (cursor == length || replacement.charAt(cursor) != '}') {
                        throw new IllegalArgumentException("named capturing group is missing trailing '}'");
                    }
                    String name = replacement.substring(start, cursor);
                    char lead = name.charAt(0);
                    if (lead >= '0' && lead <= '9') {
                        throw new IllegalArgumentException("capturing group name {" + name
                                + "} starts with digit character");
                    }
                    Integer found = pattern.names.get(name);
                    if (found == null) {
                        throw new IllegalArgumentException("No group with name {" + name + "}");
                    }
                    group = found.intValue();
                    cursor++;
                } else {
                    group = next - '0';
                    if (group < 0 || group > 9) {
                        throw new IllegalArgumentException("Illegal group reference");
                    }
                    cursor++;
                    if (group > pattern.groupCount) {
                        throw new IndexOutOfBoundsException("No group " + group);
                    }
                    while (cursor < length) {
                        int digit = replacement.charAt(cursor) - '0';
                        if (digit < 0 || digit > 9) {
                            break;
                        }
                        int wider = group * 10 + digit;
                        if (wider > pattern.groupCount) {
                            break;
                        }
                        group = wider;
                        cursor++;
                    }
                }
                if (starts[group] >= 0 && ends[group] >= 0) {
                    out.append(text.subSequence(starts[group], ends[group]).toString());
                }
            } else {
                out.append(c);
                cursor++;
            }
        }
    }

    public Matcher appendReplacement(StringBuilder sb, String replacement) {
        requireMatch();
        StringBuilder expanded = new StringBuilder();
        expand(replacement, expanded);
        sb.append(text.subSequence(appendPosition, first).toString());
        sb.append(expanded.toString());
        appendPosition = last;
        return this;
    }

    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        requireMatch();
        StringBuilder expanded = new StringBuilder();
        expand(replacement, expanded);
        sb.append(text.subSequence(appendPosition, first).toString());
        sb.append(expanded.toString());
        appendPosition = last;
        return this;
    }

    public StringBuilder appendTail(StringBuilder sb) {
        sb.append(text.subSequence(appendPosition, text.length()).toString());
        return sb;
    }

    public StringBuffer appendTail(StringBuffer sb) {
        sb.append(text.subSequence(appendPosition, text.length()).toString());
        return sb;
    }

    public String replaceAll(String replacement) {
        reset();
        if (!find()) {
            return text.toString();
        }
        StringBuilder sb = new StringBuilder();
        do {
            appendReplacement(sb, replacement);
        } while (find());
        return appendTail(sb).toString();
    }

    public String replaceAll(Function<MatchResult, String> replacer) {
        if (replacer == null) {
            throw new NullPointerException();
        }
        reset();
        if (!find()) {
            return text.toString();
        }
        StringBuilder sb = new StringBuilder();
        do {
            appendReplacement(sb, replacer.apply(this));
        } while (find());
        return appendTail(sb).toString();
    }

    public String replaceFirst(String replacement) {
        if (replacement == null) {
            throw new NullPointerException("replacement");
        }
        reset();
        if (!find()) {
            return text.toString();
        }
        StringBuilder sb = new StringBuilder();
        appendReplacement(sb, replacement);
        return appendTail(sb).toString();
    }

    public String replaceFirst(Function<MatchResult, String> replacer) {
        if (replacer == null) {
            throw new NullPointerException();
        }
        reset();
        if (!find()) {
            return text.toString();
        }
        StringBuilder sb = new StringBuilder();
        appendReplacement(sb, replacer.apply(this));
        return appendTail(sb).toString();
    }

    @Override
    public String toString() {
        return "java.util.regex.Matcher[pattern=" + pattern.pattern() + " region=" + from + "," + to
                + " lastmatch=" + (first >= 0 ? group() : "") + "]";
    }
}
