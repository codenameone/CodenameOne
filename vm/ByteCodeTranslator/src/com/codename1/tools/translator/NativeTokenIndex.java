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
package com.codename1.tools.translator;

/// The same question as [NativeSymbolIndex] -- does this identifier occur inside some
/// identifier token of the native text -- answered by the same suffix automaton over the
/// distinct tokens, but stored in primitive arrays.
///
/// NativeSymbolIndex keeps a boxed `HashMap<Character, Integer>` per automaton state.
/// Over the C runtime's 1.3 MB of native text that is about half a million maps, and
/// ReachabilityCull built two of those indexes on every translation: together they were
/// most of the cull's time, and allocation-heavy work is exactly what the self-hosted
/// translator runs relatively slower than HotSpot, so the perf gate's translator ratio
/// moved by more than the JDK arm's added time. Here a state's transitions are a linked
/// list in shared int arrays: no boxing and a handful of allocations in total. The root
/// state alone is a dense table: it has a transition for nearly every identifier
/// character, and every suffix-link walk ends there. Tokens are deduplicated by hashing
/// them where they lie in the text, so the ~150,000 identifier occurrences in the runtime
/// cost no String each; only a token seen for the first time is fed to the automaton.
///
/// A token shorter than every query cannot contain one, so the caller passes the shortest
/// length it will ask about and shorter tokens stay out of the automaton -- about half its
/// size for the runtime's natives, whose short tokens are C keywords and locals. That
/// length is a hint, never a correctness condition: a shorter query is answered by
/// scanning the short tokens directly.
///
/// NativeSymbolIndex itself is left as it is. It predates the cull and is part of the
/// translation the perf gate has always measured; replacing it would change that
/// benchmark's workload, which is a separate decision from paying for the cull.
final class NativeTokenIndex {
    private int[] len;
    private int[] link;
    private int[] head;
    private int states;

    private char[] edgeChar;
    private int[] edgeTo;
    private int[] edgeNext;
    private int edges;

    /// The root state's transitions, indexed by character; -1 for none. Every character
    /// the automaton sees is ASCII: identifier characters and the '\n' separator.
    private final int[] rootNext = new int[128];

    private int last;

    /// The distinct tokens seen so far, as (text, start, length) triples, and an
    /// open-addressed table of their indexes (+1; 0 is empty).
    private int[] tokText = new int[1024];
    private int[] tokStart = new int[1024];
    private int[] tokLen = new int[1024];
    private int tokens;
    private int[] table = new int[2048];

    private final String[] texts;
    private final int minQuery;

    /// @param texts the native text to index
    /// @param minQuery the length of the shortest string [#contains(String)] will be asked
    ///     about; a performance hint only
    NativeTokenIndex(String[] texts, int minQuery) {
        this.texts = texts;
        this.minQuery = Math.max(1, minQuery);
        java.util.Arrays.fill(rootNext, -1);
        long size = 0;
        if (texts != null) {
            for (String s : texts) {
                size += s == null ? 0 : s.length();
            }
        }
        // Distinct tokens are about a tenth of the text in practice; the arrays grow if not.
        int cap = (int) Math.min(Integer.MAX_VALUE / 4, Math.max(64, size / 4));
        len = new int[cap];
        link = new int[cap];
        head = new int[cap];
        edgeChar = new char[cap];
        edgeTo = new int[cap];
        edgeNext = new int[cap];
        last = newState(0, -1);
        if (texts == null) {
            return;
        }
        for (int f = 0; f < texts.length; f++) {
            String s = texts[f];
            if (s == null) {
                continue;
            }
            int n = s.length();
            int i = 0;
            while (i < n) {
                if (!isIdent(s.charAt(i))) {
                    i++;
                    continue;
                }
                int j = i;
                int h = 0;
                while (j < n && isIdent(s.charAt(j))) {
                    h = 31 * h + s.charAt(j);
                    j++;
                }
                if (addToken(texts, f, i, j - i, h) && j - i >= this.minQuery) {
                    for (int k = i; k < j; k++) {
                        extend(s.charAt(k));
                    }
                    extend('\n');
                }
                i = j;
            }
        }
        table = null;
    }

    /// Records the token `texts[f]` [start, start + length) unless an equal one was seen.
    ///
    /// @return true when it is new
    private boolean addToken(String[] texts, int f, int start, int length, int hash) {
        int mask = table.length - 1;
        int slot = (hash ^ (hash >>> 16)) & mask;
        while (table[slot] != 0) {
            int t = table[slot] - 1;
            if (tokLen[t] == length
                    && texts[f].regionMatches(start, texts[tokText[t]], tokStart[t], length)) {
                return false;
            }
            slot = (slot + 1) & mask;
        }
        if (tokens == tokText.length) {
            int n = tokens * 2;
            tokText = java.util.Arrays.copyOf(tokText, n);
            tokStart = java.util.Arrays.copyOf(tokStart, n);
            tokLen = java.util.Arrays.copyOf(tokLen, n);
        }
        tokText[tokens] = f;
        tokStart[tokens] = start;
        tokLen[tokens] = length;
        table[slot] = ++tokens;
        if (tokens * 2 > table.length) {
            rehash(texts);
        }
        return true;
    }

    private void rehash(String[] texts) {
        int[] grown = new int[table.length * 2];
        int mask = grown.length - 1;
        for (int t = 0; t < tokens; t++) {
            String s = texts[tokText[t]];
            int h = 0;
            for (int k = tokStart[t], e = k + tokLen[t]; k < e; k++) {
                h = 31 * h + s.charAt(k);
            }
            int slot = (h ^ (h >>> 16)) & mask;
            while (grown[slot] != 0) {
                slot = (slot + 1) & mask;
            }
            grown[slot] = t + 1;
        }
        table = grown;
    }

    /// True iff `pat` occurs as a substring of some native identifier token.
    boolean contains(String pat) {
        if (pat.length() < minQuery) {
            for (int t = 0; t < tokens; t++) {
                String s = texts[tokText[t]];
                int from = tokStart[t];
                int to = from + tokLen[t] - pat.length();
                for (int k = from; k <= to; k++) {
                    if (s.regionMatches(k, pat, 0, pat.length())) {
                        return true;
                    }
                }
            }
            return false;
        }
        int cur = 0;
        for (int i = 0; i < pat.length(); i++) {
            cur = target(cur, pat.charAt(i));
            if (cur < 0) {
                return false;
            }
        }
        return true;
    }

    private int newState(int l, int lnk) {
        if (states == len.length) {
            int n = states * 2;
            len = java.util.Arrays.copyOf(len, n);
            link = java.util.Arrays.copyOf(link, n);
            head = java.util.Arrays.copyOf(head, n);
        }
        len[states] = l;
        link[states] = lnk;
        head[states] = -1;
        return states++;
    }

    private int target(int state, char c) {
        if (state == 0) {
            return c < 128 ? rootNext[c] : -1;
        }
        for (int e = head[state]; e >= 0; e = edgeNext[e]) {
            if (edgeChar[e] == c) {
                return edgeTo[e];
            }
        }
        return -1;
    }

    private void addEdge(int state, char c, int to) {
        if (state == 0) {
            rootNext[c] = to;
            return;
        }
        if (edges == edgeTo.length) {
            int n = edges * 2;
            edgeChar = java.util.Arrays.copyOf(edgeChar, n);
            edgeTo = java.util.Arrays.copyOf(edgeTo, n);
            edgeNext = java.util.Arrays.copyOf(edgeNext, n);
        }
        edgeChar[edges] = c;
        edgeTo[edges] = to;
        edgeNext[edges] = head[state];
        head[state] = edges++;
    }

    /// Repoints an existing transition; the caller has seen it exists.
    private void redirect(int state, char c, int to) {
        if (state == 0) {
            rootNext[c] = to;
            return;
        }
        for (int e = head[state]; e >= 0; e = edgeNext[e]) {
            if (edgeChar[e] == c) {
                edgeTo[e] = to;
                return;
            }
        }
    }

    private void extend(char c) {
        int cur = newState(len[last] + 1, -1);
        int p = last;
        while (p != -1 && target(p, c) < 0) {
            addEdge(p, c, cur);
            p = link[p];
        }
        if (p == -1) {
            link[cur] = 0;
        } else {
            int q = target(p, c);
            if (len[p] + 1 == len[q]) {
                link[cur] = q;
            } else {
                int clone = newState(len[p] + 1, link[q]);
                for (int e = head[q]; e >= 0; e = edgeNext[e]) {
                    addEdge(clone, edgeChar[e], edgeTo[e]);
                }
                while (p != -1 && target(p, c) == q) {
                    redirect(p, c, clone);
                    p = link[p];
                }
                link[q] = clone;
                link[cur] = clone;
            }
        }
        last = cur;
    }

    private static boolean isIdent(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                || (c >= '0' && c <= '9') || c == '_';
    }
}
