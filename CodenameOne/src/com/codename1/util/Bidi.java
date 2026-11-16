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

package com.codename1.util;

/// The Unicode Bidirectional Algorithm (UAX #9): the embedding level of every
/// character of a paragraph of mixed left-to-right and right-to-left text, and
/// the order the characters are displayed in.
///
/// Every resolution phase is implemented: character types, explicit
/// embeddings, overrides and isolates (X1-X10), the weak types (W1-W7), paired
/// brackets (N0), the neutrals (N1-N2), the implicit levels (I1-I2), the
/// separator and trailing white space reset (L1), reordering (L2) and
/// mirroring (L4). Character types come from compact range tables rather than
/// the full Unicode database. Shaping (Arabic joining, ligatures) is not part
/// of the algorithm and is left to whoever draws the text.
///
/// ```java
/// String line = "abc \u05d0\u05d1\u05d2 123";
/// byte[] levels = Bidi.levels(line, Bidi.direction(line) == Bidi.RTL);
/// int[] order = Bidi.visualOrder(levels);
/// // order[v] is the index in line of the character drawn v-th from the left
/// ```
@com.codename1.impl.SharedWithBackend
public final class Bidi {
    /// Left to right: a paragraph direction, and the answer of [#direction(String)].
    public static final int LTR = 0;
    /// Right to left: a paragraph direction, and the answer of [#direction(String)].
    public static final int RTL = 1;
    /// The answer of [#direction(String)] for text without a strong character.
    public static final int NEUTRAL = -1;

    // Bidirectional character types (a subset of UAX #9 sufficient for the editor).
    static final byte L = 0;    // Left-to-Right (strong)
    static final byte R = 1;    // Right-to-Left (strong)
    static final byte AL = 2;   // Right-to-Left Arabic (strong)
    static final byte EN = 3;   // European Number
    static final byte ES = 4;   // European Number Separator
    static final byte ET = 5;   // European Number Terminator
    static final byte AN = 6;   // Arabic Number
    static final byte CS = 7;   // Common Number Separator
    static final byte NSM = 8;  // Nonspacing Mark
    static final byte BN = 9;   // Boundary Neutral
    static final byte ON = 10;  // Other Neutral
    static final byte WS = 11;  // Whitespace

    private static final String MIRRORS = "()<>[]{}\u00ab\u00bb\u2039\u203a\u2045\u2046\u207d\u207e\u208d\u208e"
            + "\u2208\u220b\u2264\u2265\u2266\u2267\u226a\u226b\u2282\u2283\u2286\u2287\u3008\u3009\u300a\u300b"
            + "\u300c\u300d\u300e\u300f\u3010\u3011\uff08\uff09\uff3b\uff3d\uff5b\uff5d";

    private Bidi() {
    }

    // Explicit formatting characters, separators and paragraph separators (UAX #9 Table 4).
    static final byte LRE = 12;
    static final byte RLE = 13;
    static final byte LRO = 14;
    static final byte RLO = 15;
    static final byte PDF = 16;
    static final byte LRI = 17;
    static final byte RLI = 18;
    static final byte FSI = 19;
    static final byte PDI = 20;
    static final byte B = 21;   // Paragraph Separator
    static final byte S = 22;   // Segment Separator (tab)

    private static final int MAX_DEPTH = 125;
    private static final int MAX_BRACKETS = 63;
    // BD14/BD15 paired brackets (Bidi_Paired_Bracket), opening and closing at the same index.
    private static final String OPEN = "([{\u0F3A\u0F3C\u169B\u2045\u207D\u208D\u2308\u230A\u3008\u300A"
            + "\u300C\u300E\u3010\u3014\u3016\u3018\u301A\uFE59\uFE5B\uFE5D\uFF08\uFF3B\uFF5B\uFF5F\uFF62";
    private static final String CLOSE = ")]}\u0F3B\u0F3D\u169C\u2046\u207E\u208E\u2309\u230B\u3009\u300B"
            + "\u300D\u300F\u3011\u3015\u3017\u3019\u301B\uFE5A\uFE5C\uFE5E\uFF09\uFF3D\uFF5D\uFF60\uFF63";

    /// Bidi type of a UTF-16 code unit, including the explicit embedding, override and isolate controls,
    /// segment and paragraph separators, boundary neutrals and paired brackets.
    static byte typeOf(char c) {
        switch (c) {
            case 0x202A: return LRE;
            case 0x202B: return RLE;
            case 0x202D: return LRO;
            case 0x202E: return RLO;
            case 0x202C: return PDF;
            case 0x2066: return LRI;
            case 0x2067: return RLI;
            case 0x2068: return FSI;
            case 0x2069: return PDI;
            case 0x200E: return L;
            case 0x200F: return R;
            case 0x061C: return AL;
            case '\t': case 0x0B: case 0x1F: return S;
            case '\n': case '\r': case 0x1C: case 0x1D: case 0x1E: case 0x85: case 0x2029: return B;
            case 0x200B: case 0x200C: case 0x200D: case 0x2060: case 0xFEFF: case 0x00AD: return BN;
            case 0x2329: case 0x232A: return ON;
            default: break;
        }
        if (OPEN.indexOf(c) >= 0 || CLOSE.indexOf(c) >= 0) {
            return ON;
        }
        return baseTypeOf(c);
    }

    /// The resolved embedding level of every character of the paragraph `text`, laid out right to left
    /// when `rtl` is true: even levels are left to right, odd ones right to left. Characters removed by
    /// X9 (the explicit formatting characters) take the level of the preceding character, so a caller can
    /// still draw them.
    public static byte[] levels(String text, boolean rtl) {
        int n = text.length();
        byte[] levels = new byte[n];
        if (n == 0) {
            return levels;
        }
        byte para = (byte) (rtl ? 1 : 0);
        byte[] initial = new byte[n];
        for (int i = 0; i < n; i++) {
            initial[i] = typeOf(text.charAt(i));
        }
        // BD9: matching isolate initiators and PDIs.
        int[] matchingPdi = new int[n];
        int[] matchingInit = new int[n];
        int[] open = new int[n];
        int openCount = 0;
        for (int i = 0; i < n; i++) {
            matchingPdi[i] = -1;
            matchingInit[i] = -1;
            byte t = initial[i];
            if (t == LRI || t == RLI || t == FSI) {
                matchingPdi[i] = n;
                open[openCount++] = i;
            } else if (t == PDI && openCount > 0) {
                int o = open[--openCount];
                matchingPdi[o] = i;
                matchingInit[i] = o;
            } else if (t == B) {
                openCount = 0;
            }
        }
        // X1-X8: the directional status stack.
        byte[] types = new byte[n];
        System.arraycopy(initial, 0, types, 0, n);
        int[] stackLevel = new int[MAX_DEPTH + 2];
        byte[] stackOverride = new byte[MAX_DEPTH + 2];
        boolean[] stackIsolate = new boolean[MAX_DEPTH + 2];
        stackLevel[0] = para;
        stackOverride[0] = ON;
        int sp = 1;
        int overflowIsolates = 0;
        int overflowEmbeddings = 0;
        int validIsolates = 0;
        for (int i = 0; i < n; i++) {
            byte t = initial[i];
            int cur = stackLevel[sp - 1];
            if (t == RLE || t == LRE || t == RLO || t == LRO || t == RLI || t == LRI || t == FSI) {
                boolean isolate = t == RLI || t == LRI || t == FSI;
                boolean toRtl;
                if (t == FSI) {
                    toRtl = firstStrongRtl(initial, matchingPdi, i + 1, matchingPdi[i]);
                } else {
                    toRtl = t == RLE || t == RLO || t == RLI;
                }
                levels[i] = (byte) cur;
                if (isolate && stackOverride[sp - 1] != ON) {
                    types[i] = stackOverride[sp - 1];
                }
                int next = toRtl ? (cur + 1) | 1 : (cur + 2) & ~1;
                if (next <= MAX_DEPTH && overflowIsolates == 0 && overflowEmbeddings == 0) {
                    if (isolate) {
                        validIsolates++;
                    }
                    stackLevel[sp] = next;
                    stackOverride[sp] = t == LRO ? L : t == RLO ? R : ON;
                    stackIsolate[sp] = isolate;
                    sp++;
                } else if (isolate) {
                    overflowIsolates++;
                } else if (overflowIsolates == 0) {
                    overflowEmbeddings++;
                }
            } else if (t == PDI) {
                if (overflowIsolates > 0) {
                    overflowIsolates--;
                } else if (validIsolates > 0) {
                    overflowEmbeddings = 0;
                    while (!stackIsolate[sp - 1]) {
                        sp--;
                    }
                    sp--;
                    validIsolates--;
                }
                levels[i] = (byte) stackLevel[sp - 1];
                if (stackOverride[sp - 1] != ON) {
                    types[i] = stackOverride[sp - 1];
                }
            } else if (t == PDF) {
                if (overflowIsolates == 0) {
                    if (overflowEmbeddings > 0) {
                        overflowEmbeddings--;
                    } else if (!stackIsolate[sp - 1] && sp >= 2) {
                        sp--;
                    }
                }
                levels[i] = (byte) stackLevel[sp - 1];
            } else if (t == B) {
                levels[i] = para;
            } else {
                levels[i] = (byte) cur;
                if (t != BN && stackOverride[sp - 1] != ON) {
                    types[i] = stackOverride[sp - 1];
                }
            }
        }
        // X9/X10: level runs over the retained characters, chained into isolating run sequences.
        int[] kept = new int[n];
        int keptCount = 0;
        for (int i = 0; i < n; i++) {
            if (!removed(initial[i])) {
                kept[keptCount++] = i;
            }
        }
        int[] runStart = new int[keptCount + 1];
        int[] runOf = new int[n];
        int runCount = 0;
        for (int k = 0; k < keptCount; k++) {
            if (k == 0 || levels[kept[k]] != levels[kept[k - 1]]) {
                runStart[runCount++] = k;
            }
            runOf[kept[k]] = runCount - 1;
        }
        runStart[runCount] = keptCount;
        int[] seq = new int[keptCount];
        for (int r = 0; r < runCount; r++) {
            int first = kept[runStart[r]];
            if (initial[first] == PDI && matchingInit[first] >= 0) {
                continue;
            }
            int len = 0;
            int run = r;
            while (true) {
                for (int k = runStart[run]; k < runStart[run + 1]; k++) {
                    seq[len++] = kept[k];
                }
                int last = seq[len - 1];
                byte lt = initial[last];
                if ((lt == LRI || lt == RLI || lt == FSI) && matchingPdi[last] < n) {
                    run = runOf[matchingPdi[last]];
                } else {
                    break;
                }
            }
            resolveSequence(text, seq, len, initial, types, levels, para, n);
        }
        // X9 removed characters take the level of their predecessor.
        for (int i = 0; i < n; i++) {
            if (removed(initial[i])) {
                levels[i] = i == 0 ? para : levels[i - 1];
            }
        }
        // L1: separators, and whitespace before them or at the end of the paragraph, reset to the base.
        boolean reset = true;
        for (int i = n - 1; i >= 0; i--) {
            byte t = initial[i];
            if (t == S || t == B) {
                levels[i] = para;
                reset = true;
            } else if (reset && (t == WS || t == LRI || t == RLI || t == FSI || t == PDI || removed(t))) {
                levels[i] = para;
            } else {
                reset = false;
            }
        }
        return levels;
    }

    private static boolean removed(byte t) {
        return t == RLE || t == LRE || t == RLO || t == LRO || t == PDF || t == BN;
    }

    // P2/P3 within an isolate: the first strong type between from and to, skipping nested isolates.
    private static boolean firstStrongRtl(byte[] initial, int[] matchingPdi, int from, int to) {
        for (int j = from; j < to && j < initial.length; j++) {
            byte t = initial[j];
            if (t == L) {
                return false;
            }
            if (t == R || t == AL) {
                return true;
            }
            if (t == LRI || t == RLI || t == FSI) {
                j = matchingPdi[j];
            } else if (t == B) {
                break;
            }
        }
        return false;
    }

    private static boolean isNi(byte t) {
        return t == ON || t == WS || t == S || t == B || t == LRI || t == RLI || t == FSI || t == PDI;
    }

    // Strong direction for N0/N1: numbers count as R; anything else is not strong.
    private static byte strongDir(byte t) {
        if (t == L) {
            return L;
        }
        if (t == R || t == AL || t == EN || t == AN) {
            return R;
        }
        return ON;
    }

    private static int openIndexOf(char c) {
        return OPEN.indexOf(c == 0x2329 ? (char) 0x3008 : c);
    }

    private static int closeIndexOf(char c) {
        return CLOSE.indexOf(c == 0x232A ? (char) 0x3009 : c);
    }

    private static void resolveSequence(String text, int[] seqIdx, int len, byte[] initial, byte[] types,
            byte[] levels, byte para, int n) {
        int first = seqIdx[0];
        int last = seqIdx[len - 1];
        int level = levels[first];
        int prev = para;
        for (int i = first - 1; i >= 0; i--) {
            if (!removed(initial[i])) {
                prev = levels[i];
                break;
            }
        }
        int next = para;
        byte lt = initial[last];
        if (!(lt == LRI || lt == RLI || lt == FSI)) {
            for (int i = last + 1; i < n; i++) {
                if (!removed(initial[i])) {
                    next = levels[i];
                    break;
                }
            }
        }
        byte sos = (Math.max(prev, level) & 1) != 0 ? R : L;
        byte eos = (Math.max(next, level) & 1) != 0 ? R : L;
        byte[] t = new byte[len];
        for (int k = 0; k < len; k++) {
            t[k] = types[seqIdx[k]];
        }
        // W1
        for (int k = 0; k < len; k++) {
            if (t[k] == NSM) {
                if (k == 0) {
                    t[k] = sos;
                } else {
                    byte p = t[k - 1];
                    t[k] = (p == LRI || p == RLI || p == FSI || p == PDI) ? ON : p;
                }
            }
        }
        // W2, W3
        byte strong = sos;
        for (int k = 0; k < len; k++) {
            byte c = t[k];
            if (c == L || c == R || c == AL) {
                strong = c;
            } else if (c == EN && strong == AL) {
                t[k] = AN;
            }
        }
        for (int k = 0; k < len; k++) {
            if (t[k] == AL) {
                t[k] = R;
            }
        }
        // W4
        for (int k = 1; k < len - 1; k++) {
            if (t[k] == ES && t[k - 1] == EN && t[k + 1] == EN) {
                t[k] = EN;
            } else if (t[k] == CS && t[k - 1] == t[k + 1] && (t[k - 1] == EN || t[k - 1] == AN)) {
                t[k] = t[k - 1];
            }
        }
        // W5
        for (int k = 0; k < len; k++) {
            if (t[k] == ET) {
                int s = k;
                while (k < len && t[k] == ET) {
                    k++;
                }
                if ((s > 0 && t[s - 1] == EN) || (k < len && t[k] == EN)) {
                    for (int j = s; j < k; j++) {
                        t[j] = EN;
                    }
                }
                k--;
            }
        }
        // W6
        for (int k = 0; k < len; k++) {
            if (t[k] == ES || t[k] == ET || t[k] == CS) {
                t[k] = ON;
            }
        }
        // W7
        strong = sos;
        for (int k = 0; k < len; k++) {
            if (t[k] == L || t[k] == R) {
                strong = t[k];
            } else if (t[k] == EN && strong == L) {
                t[k] = L;
            }
        }
        byte e = (level & 1) != 0 ? R : L;
        // N0: BD16 bracket pairs, processed in order of their opening bracket.
        int[] stackChar = new int[MAX_BRACKETS];
        int[] stackPos = new int[MAX_BRACKETS];
        int depth = 0;
        int[] pairOpen = new int[len];
        int[] pairClose = new int[len];
        int pairs = 0;
        for (int k = 0; k < len; k++) {
            if (t[k] != ON) {
                continue;
            }
            char c = text.charAt(seqIdx[k]);
            int o = openIndexOf(c);
            if (o >= 0) {
                if (depth == MAX_BRACKETS) {
                    break;
                }
                stackChar[depth] = o;
                stackPos[depth++] = k;
                continue;
            }
            int cl = closeIndexOf(c);
            if (cl >= 0) {
                for (int d = depth - 1; d >= 0; d--) {
                    if (stackChar[d] == cl) {
                        pairOpen[pairs] = stackPos[d];
                        pairClose[pairs++] = k;
                        depth = d;
                        break;
                    }
                }
            }
        }
        for (int a = 1; a < pairs; a++) {
            int po = pairOpen[a];
            int pc = pairClose[a];
            int b = a - 1;
            while (b >= 0 && pairOpen[b] > po) {
                pairOpen[b + 1] = pairOpen[b];
                pairClose[b + 1] = pairClose[b];
                b--;
            }
            pairOpen[b + 1] = po;
            pairClose[b + 1] = pc;
        }
        for (int p = 0; p < pairs; p++) {
            int o = pairOpen[p];
            int c = pairClose[p];
            boolean embedding = false;
            boolean opposite = false;
            for (int k = o + 1; k < c; k++) {
                byte d = strongDir(t[k]);
                if (d == e) {
                    embedding = true;
                    break;
                }
                if (d != ON) {
                    opposite = true;
                }
            }
            byte resolved = ON;
            if (embedding) {
                resolved = e;
            } else if (opposite) {
                byte before = sos;
                for (int k = o - 1; k >= 0; k--) {
                    byte d = strongDir(t[k]);
                    if (d != ON) {
                        before = d;
                        break;
                    }
                }
                resolved = before;
            }
            if (resolved != ON) {
                t[o] = resolved;
                t[c] = resolved;
                for (int k = o + 1; k < len && initial[seqIdx[k]] == NSM; k++) {
                    t[k] = resolved;
                }
                for (int k = c + 1; k < len && initial[seqIdx[k]] == NSM; k++) {
                    t[k] = resolved;
                }
            }
        }
        // N1/N2
        for (int k = 0; k < len; k++) {
            if (isNi(t[k])) {
                int s = k;
                while (k < len && isNi(t[k])) {
                    k++;
                }
                byte before = s == 0 ? sos : strongDir(t[s - 1]);
                byte after = k >= len ? eos : strongDir(t[k]);
                byte resolved = before == after ? before : e;
                for (int j = s; j < k; j++) {
                    t[j] = resolved;
                }
                k--;
            }
        }
        // I1/I2
        for (int k = 0; k < len; k++) {
            byte c = t[k];
            int lv = level;
            if ((level & 1) == 0) {
                if (c == R) {
                    lv++;
                } else if (c == AN || c == EN) {
                    lv += 2;
                }
            } else if (c == L || c == EN || c == AN) {
                lv++;
            }
            levels[seqIdx[k]] = (byte) lv;
        }
    }

    /// True when `text`, in a paragraph laid out right to left when `rtl` is true, displays in its
    /// logical order with no reordering: the fast path for the common case.
    public static boolean isLeftToRight(String text, boolean rtl) {
        if (rtl) {
            return text.length() == 0;
        }
        for (int i = 0; i < text.length(); i++) {
            byte t = typeOf(text.charAt(i));
            if (t == R || t == AL || t == AN || (t >= LRE && t <= PDI)) {
                return false;
            }
        }
        return true;
    }

    /// The bidirectional type of a code unit other than the explicit formatting characters and paired
    /// brackets, which [#typeOf(char)] handles first: compact ranges of the Unicode data, defaulting to L.
    private static byte baseTypeOf(char c) {
        if (c < 0x80) {
            if (c >= '0' && c <= '9') {
                return EN;
            }
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z') {
                return L;
            }
            switch (c) {
                case ' ':
                    return WS;
                case '\t':
                case 0x0b:
                case 0x1f:
                    return S;
                case '\n':
                case '\r':
                case 0x1c:
                case 0x1d:
                case 0x1e:
                    return B;
                case '+':
                case '-':
                    return ES;
                case '#':
                case '$':
                case '%':
                    return ET;
                case ',':
                case '.':
                case '/':
                case ':':
                    return CS;
                default:
                    return c < 0x20 || c == 0x7f ? BN : ON;
            }
        }
        if (c < 0x0300) {
            if (c == 0xa0) {
                return CS;
            }
            if (c >= 0xa2 && c <= 0xa5 || c == 0xb0 || c == 0xb1) {
                return ET;
            }
            if (c == 0xad) {
                return BN;
            }
            if (c >= 0xa1 && c <= 0xbf && c != 0xaa && c != 0xb5 && c != 0xba || c == 0xd7 || c == 0xf7) {
                return ON;
            }
            return c < 0xa0 ? BN : L;
        }
        if (c <= 0x036f || c >= 0x0483 && c <= 0x0489 || c >= 0x20d0 && c <= 0x20ff
                || c >= 0xfe00 && c <= 0xfe0f || c >= 0xfe20 && c <= 0xfe2f) {
            return NSM;
        }
        if (c >= 0x0590 && c <= 0x05ff) {
            if (c >= 0x0591 && c <= 0x05bd || c == 0x05bf || c == 0x05c1 || c == 0x05c2 || c == 0x05c4
                    || c == 0x05c5 || c == 0x05c7) {
                return NSM;
            }
            return R;
        }
        if (c >= 0x0600 && c <= 0x07bf) {
            if (c >= 0x0660 && c <= 0x0669 || c == 0x066b || c == 0x066c || c <= 0x0605
                    || c == 0x06dd) {
                return AN;
            }
            if (c >= 0x06f0 && c <= 0x06f9) {
                return EN;
            }
            if (c == 0x060c) {
                return CS;
            }
            if (c == 0x066a) {
                return ET;
            }
            if (c >= 0x0610 && c <= 0x061a || c >= 0x064b && c <= 0x065f || c == 0x0670
                    || c >= 0x06d6 && c <= 0x06dc || c >= 0x06df && c <= 0x06e4 || c == 0x06e7 || c == 0x06e8
                    || c >= 0x06ea && c <= 0x06ed || c == 0x0711 || c >= 0x0730 && c <= 0x074a
                    || c >= 0x07a6 && c <= 0x07b0) {
                return NSM;
            }
            return AL;
        }
        if (c >= 0x07c0 && c <= 0x085f) {
            return R;
        }
        if (c >= 0x0860 && c <= 0x08ff) {
            return c >= 0x08d3 ? NSM : AL;
        }
        if (c >= 0x2000 && c <= 0x206f) {
            if (c <= 0x200a || c == 0x2028) {
                return WS;
            }
            if (c == 0x2029) {
                return B;
            }
            if (c == 0x200e) {
                return L;
            }
            if (c == 0x200f) {
                return R;
            }
            if (c >= 0x2030 && c <= 0x2034) {
                return ET;
            }
            if (c == 0x202f) {
                return CS;
            }
            if (c <= 0x200d || c >= 0x202a && c <= 0x202e || c >= 0x2060) {
                return BN;
            }
            return ON;
        }
        if (c >= 0x20a0 && c <= 0x20cf) {
            return ET;
        }
        if (c >= 0x2190 && c <= 0x2bff || c >= 0x3001 && c <= 0x3003 || c >= 0x3008 && c <= 0x3011
                || c >= 0x2e00 && c <= 0x2e7f || c >= 0xfe50 && c <= 0xfe6f && c != 0xfe52 && c != 0xfe55
                || c >= 0xff01 && c <= 0xff0f && c != 0xff03 && c != 0xff04 && c != 0xff05
                || c >= 0xff1a && c <= 0xff20 || c >= 0xff3b && c <= 0xff40 || c >= 0xff5b && c <= 0xff65) {
            if (c == 0xff0c || c == 0xff0e || c == 0xff1a) {
                return CS;
            }
            return c == 0x2212 ? ES : ON;
        }
        if (c == 0x3000) {
            return WS;
        }
        if (c >= 0xff10 && c <= 0xff19) {
            return EN;
        }
        if (c >= 0xfb1d && c <= 0xfb4f) {
            return c == 0xfb1e ? NSM : R;
        }
        if (c >= 0xfb50 && c <= 0xfdff || c >= 0xfe70 && c <= 0xfefe) {
            return AL;
        }
        if (c == 0xfeff) {
            return BN;
        }
        if (c >= 0xd800 && c <= 0xdfff) {
            // A surrogate half: emoji and other astral symbols, neutral so
            // they do not split a right-to-left run.
            return ON;
        }
        return L;
    }

    /// The direction of the first strong character of `text` outside isolates (P2, P3): [#LTR], [#RTL],
    /// or [#NEUTRAL] when it has none.
    public static int direction(String text) {
        for (int i = 0; i < text.length(); i++) {
            byte t = typeOf(text.charAt(i));
            if (t == L) {
                return LTR;
            }
            if (t == R || t == AL) {
                return RTL;
            }
            if (t == LRI || t == RLI || t == FSI) {
                int depth = 1;
                while (depth > 0 && ++i < text.length()) {
                    byte u = typeOf(text.charAt(i));
                    if (u == LRI || u == RLI || u == FSI) {
                        depth++;
                    } else if (u == PDI) {
                        depth--;
                    }
                }
            }
        }
        return NEUTRAL;
    }

    /// The direction of `c` when it is a strong character: [#LTR], [#RTL], or [#NEUTRAL] for anything
    /// else (digits, punctuation, white space, marks).
    public static int strongDirection(char c) {
        byte t = typeOf(c);
        if (t == L) {
            return LTR;
        }
        return t == R || t == AL ? RTL : NEUTRAL;
    }

    /// True when `c` is a nonspacing mark (NSM), such as a Hebrew point or an Arabic vowel sign, which
    /// takes the direction of the character it follows.
    public static boolean isMark(char c) {
        return typeOf(c) == NSM;
    }

    /// The mirror image of `c` for right-to-left display (L4), such as `)` for `(`, or `c` itself.
    public static char mirror(char c) {
        int i = MIRRORS.indexOf(c);
        if (i < 0) {
            return c;
        }
        return MIRRORS.charAt(i ^ 1);
    }

    /// `text` in display order, left to right, for a paragraph laid out right to left when `rtl` is
    /// true, with the characters of right-to-left runs mirrored. A line without explicit formatting
    /// characters is its own inverse, so this also restores the logical order of text that was
    /// captured in display order.
    public static String reorder(String text, boolean rtl) {
        if (isLeftToRight(text, rtl)) {
            return text;
        }
        byte[] lv = levels(text, rtl);
        int[] idx = visualOrder(lv);
        StringBuilder sb = new StringBuilder(text.length());
        for (int i : idx) {
            char c = text.charAt(i);
            sb.append((lv[i] & 1) != 0 ? mirror(c) : c);
        }
        return sb.toString();
    }

    /// Applies reordering rule L2 to produce the visual order of the characters. Returns an array
    /// `visualToLogical` where element `v` is the logical index of the character drawn at visual position
    /// `v` (left to right on screen).
    public static int[] visualOrder(byte[] levels) {
        int n = levels.length;
        int[] order = new int[n];
        for (int i = 0; i < n; i++) {
            order[i] = i;
        }
        if (n == 0) {
            return order;
        }
        byte highest = 0;
        byte lowestOdd = Byte.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            byte lv = levels[i];
            if (lv > highest) {
                highest = lv;
            }
            if ((lv & 1) != 0 && lv < lowestOdd) {
                lowestOdd = lv;
            }
        }
        if (lowestOdd == Byte.MAX_VALUE) {
            // all even levels: already in logical == visual order
            return order;
        }
        for (int level = highest; level >= lowestOdd; level--) {
            int i = 0;
            while (i < n) {
                if (levels[order[i]] >= level) {
                    int start = i;
                    while (i < n && levels[order[i]] >= level) {
                        i++;
                    }
                    reverse(order, start, i - 1);
                } else {
                    i++;
                }
            }
        }
        return order;
    }

    private static void reverse(int[] a, int from, int to) {
        while (from < to) {
            int t = a[from];
            a[from] = a[to];
            a[to] = t;
            from++;
            to--;
        }
    }
}
