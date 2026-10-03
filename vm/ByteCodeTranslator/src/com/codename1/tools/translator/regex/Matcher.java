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
package com.codename1.tools.translator.regex;

import java.util.Arrays;

/**
 * Matches a {@link Pattern} against a character sequence, with the
 * {@code java.util.regex.Matcher} contract for the operations the translator
 * uses: {@code find}, {@code lookingAt}, {@code matches}, groups, regions with
 * opaque, anchoring bounds, and replacement with {@code $n} references.
 */
public final class Matcher {
    private final Pattern pattern;
    final CharSequence text;
    /** Region bounds. */
    int from;
    int to;
    /** Start/end of each group, -1 when unset; index 0 is the whole match. */
    int[] groups;
    int[] pendingStart;
    int[] loopCount;
    int[] loopStart;
    boolean requireEnd;
    int matchEnd;
    /** Bounds of the last match, -1 when there is none. */
    private int first = -1;
    private int last;
    /** Where the last appendReplacement left off. */
    private int appendPosition;

    Matcher(Pattern pattern, CharSequence text) {
        this.pattern = pattern;
        this.text = text;
        int groupSlots = (pattern.groupCount + 1) * 2;
        groups = new int[groupSlots];
        pendingStart = new int[pattern.groupCount + 1];
        loopCount = new int[pattern.loopCount];
        loopStart = new int[pattern.loopCount];
        reset();
    }

    public Matcher reset() {
        from = 0;
        to = text.length();
        first = -1;
        last = 0;
        appendPosition = 0;
        Arrays.fill(groups, -1);
        return this;
    }

    /** Restricts matching to [start, end); resets the match state. */
    public Matcher region(int start, int end) {
        if (start < 0 || start > text.length() || end < start || end > text.length()) {
            throw new IndexOutOfBoundsException("region " + start + ", " + end);
        }
        reset();
        from = start;
        to = end;
        last = start;
        return this;
    }

    public Pattern pattern() {
        return pattern;
    }

    /**
     * Region bounds always anchor {@code ^} and {@code $} here, which is also the
     * {@code java.util.regex} default; asking for anything else is unsupported.
     */
    public Matcher useAnchoringBounds(boolean anchoring) {
        if (!anchoring) {
            throw new UnsupportedOperationException("Non-anchoring bounds are not supported");
        }
        return this;
    }

    /** Attempts a match starting exactly at position i; records it on success. */
    private boolean matchAt(int i, boolean anchoredToEnd) {
        Arrays.fill(groups, -1);
        Arrays.fill(loopCount, 0);
        Arrays.fill(loopStart, -1);
        requireEnd = anchoredToEnd;
        if (pattern.root.match(this, i)) {
            first = i;
            last = matchEnd;
            groups[0] = first;
            groups[1] = last;
            return true;
        }
        first = -1;
        return false;
    }

    private boolean search(int start) {
        String prefix = pattern.prefix;
        for (int i = start; i <= to; i++) {
            if (prefix != null) {
                int found = indexOf(prefix, i);
                if (found < 0) {
                    break;
                }
                i = found;
            }
            if (matchAt(i, false)) {
                return true;
            }
        }
        first = -1;
        Arrays.fill(groups, -1);
        return false;
    }

    private int indexOf(String prefix, int start) {
        int limit = to - prefix.length();
        char head = prefix.charAt(0);
        for (int i = start; i <= limit; i++) {
            if (text.charAt(i) != head) {
                continue;
            }
            int k = 1;
            while (k < prefix.length() && text.charAt(i + k) == prefix.charAt(k)) {
                k++;
            }
            if (k == prefix.length()) {
                return i;
            }
        }
        return -1;
    }

    /** The next match after the previous one; an empty match advances one character. */
    public boolean find() {
        int next = last;
        if (next == first) {
            next++;
        }
        if (next < from) {
            next = from;
        }
        if (next > to) {
            Arrays.fill(groups, -1);
            first = -1;
            return false;
        }
        return search(next);
    }

    /** Resets, then finds the first match at or after {@code start}. */
    public boolean find(int start) {
        if (start < 0 || start > text.length()) {
            throw new IndexOutOfBoundsException("Illegal start index");
        }
        reset();
        return search(start);
    }

    /** Matches a prefix of the region. */
    public boolean lookingAt() {
        return matchAt(from, false);
    }

    /** Matches the entire region. */
    public boolean matches() {
        return matchAt(from, true);
    }

    private void requireMatch() {
        if (first < 0) {
            throw new IllegalStateException("No match available");
        }
    }

    public int start() {
        requireMatch();
        return first;
    }

    public int end() {
        requireMatch();
        return last;
    }

    public int start(int group) {
        requireMatch();
        checkGroup(group);
        return groups[2 * group];
    }

    public int end(int group) {
        requireMatch();
        checkGroup(group);
        return groups[2 * group + 1];
    }

    public String group() {
        return group(0);
    }

    public String group(int group) {
        requireMatch();
        checkGroup(group);
        int start = groups[2 * group];
        if (start < 0) {
            return null;
        }
        return text.subSequence(start, groups[2 * group + 1]).toString();
    }

    public int groupCount() {
        return pattern.groupCount;
    }

    private void checkGroup(int group) {
        if (group < 0 || group > pattern.groupCount) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    /** Escapes {@code \} and {@code $} so a string is substituted literally. */
    public static String quoteReplacement(String s) {
        if (s.indexOf('\\') < 0 && s.indexOf('$') < 0) {
            return s;
        }
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '$') {
                b.append('\\');
            }
            b.append(c);
        }
        return b.toString();
    }

    /** Appends the text since the last append, then the expanded replacement. */
    public Matcher appendReplacement(StringBuffer sb, String replacement) {
        requireMatch();
        StringBuilder expanded = new StringBuilder();
        appendExpanded(expanded, replacement);
        sb.append(text, appendPosition, first);
        sb.append(expanded);
        appendPosition = last;
        return this;
    }

    public Matcher appendReplacement(StringBuilder sb, String replacement) {
        requireMatch();
        StringBuilder expanded = new StringBuilder();
        appendExpanded(expanded, replacement);
        sb.append(text, appendPosition, first);
        sb.append(expanded);
        appendPosition = last;
        return this;
    }

    public StringBuffer appendTail(StringBuffer sb) {
        sb.append(text, appendPosition, text.length());
        return sb;
    }

    public StringBuilder appendTail(StringBuilder sb) {
        sb.append(text, appendPosition, text.length());
        return sb;
    }

    /** Replaces every match; {@code $n} names a group and {@code \} quotes the next character. */
    public String replaceAll(String replacement) {
        reset();
        if (!find()) {
            return text.toString();
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        do {
            appendReplacement(sb, replacement);
        } while (find());
        appendTail(sb);
        return sb.toString();
    }

    private void appendExpanded(StringBuilder out, String replacement) {
        int i = 0;
        int n = replacement.length();
        while (i < n) {
            char c = replacement.charAt(i);
            if (c == '\\') {
                i++;
                if (i >= n) {
                    throw new IllegalArgumentException("character to be escaped is missing");
                }
                out.append(replacement.charAt(i));
                i++;
            } else if (c == '$') {
                i++;
                if (i >= n) {
                    throw new IllegalArgumentException("Illegal group reference: group index is missing");
                }
                char d = replacement.charAt(i);
                if (d < '0' || d > '9') {
                    throw new IllegalArgumentException("Illegal group reference");
                }
                int ref = d - '0';
                if (ref > pattern.groupCount) {
                    throw new IndexOutOfBoundsException("No group " + ref);
                }
                i++;
                // The longest run of digits that still names a group.
                while (i < n) {
                    char e = replacement.charAt(i);
                    if (e < '0' || e > '9') {
                        break;
                    }
                    int longer = ref * 10 + (e - '0');
                    if (longer > pattern.groupCount) {
                        break;
                    }
                    ref = longer;
                    i++;
                }
                String value = group(ref);
                if (value != null) {
                    out.append(value);
                }
            } else {
                out.append(c);
                i++;
            }
        }
    }
}
