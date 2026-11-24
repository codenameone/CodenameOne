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
package com.codename1.spellcheck;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A Hunspell dictionary -- an `.aff` affix file and a `.dic` word list -- read for spelling
/// checks and suggestions. Hunspell dictionaries exist for most languages under open licenses
/// (LibreOffice ships them); none is bundled with Codename One, so an application supplies the
/// pair, typically from its resources.
///
/// Supported: `SET` (UTF-8, ISO8859-1, ISO8859-2), `FLAG` (character, `long`, `num`), `AF`
/// aliases, `PFX`/`SFX` rules with strip, add, condition and cross products, continuation
/// classes (two suffixes in a row), `FORBIDDENWORD`, `NEEDAFFIX`, `ONLYINCOMPOUND`, compounding
/// by `COMPOUNDFLAG`/`COMPOUNDBEGIN`/`COMPOUNDMIDDLE`/`COMPOUNDEND` with `COMPOUNDMIN` and
/// `COMPOUNDWORDMAX`, `ICONV`/`OCONV`, `TRY`, `MAP` and `REP`. Words that need
/// `COMPOUNDRULE` or the `CHECKCOMPOUND*` restrictions are reported as misspelled.
///
/// ```java
/// // the .aff and .dic files of a Hunspell dictionary, here as two tiny strings
/// byte[] aff = "SET UTF-8\nSFX S Y 1\nSFX S 0 s .\n".getBytes();
/// byte[] dic = "2\ncat/S\ndog/S\n".getBytes();
/// HunspellDictionary d = HunspellDictionary.fromBytes(aff, dic);
/// SpellChecker spell = SpellChecker.create(d);
/// Log.p("cats: " + spell.isCorrect("cats") + ", cots: " + spell.isCorrect("cots"));
/// ```
public final class HunspellDictionary {
    private static final String LATIN2 =
            "\u00a0\u0104\u02d8\u0141\u00a4\u013d\u015a\u00a7\u00a8\u0160\u015e\u0164\u0179\u00ad\u017d\u017b"
            + "\u00b0\u0105\u02db\u0142\u00b4\u013e\u015b\u02c7\u00b8\u0161\u015f\u0165\u017a\u02dd\u017e\u017c"
            + "\u0154\u00c1\u00c2\u0102\u00c4\u0139\u0106\u00c7\u010c\u00c9\u0118\u00cb\u011a\u00cd\u00ce\u010e"
            + "\u0110\u0143\u0147\u00d3\u00d4\u0150\u00d6\u00d7\u0158\u016e\u00da\u0170\u00dc\u00dd\u0162\u00df"
            + "\u0155\u00e1\u00e2\u0103\u00e4\u013a\u0107\u00e7\u010d\u00e9\u0119\u00eb\u011b\u00ed\u00ee\u010f"
            + "\u0111\u0144\u0148\u00f3\u00f4\u0151\u00f6\u00f7\u0159\u016f\u00fa\u0171\u00fc\u00fd\u0163\u02d9";

    // Suggestion costs, lower ranks first.
    private static final int COST_CASE = 0;
    private static final int COST_ADD_DIACRITIC = 4;
    private static final int COST_OTHER_MAP = 7;
    private static final int COST_DROP_DIACRITIC = 11;
    private static final int COST_REP = 10;
    private static final int COST_SPLIT = 12;
    private static final int COST_SPLIT_RARE = 18;
    private static final int COST_SWAP = 14;
    private static final int COST_DROP_OR_ADD = 15;
    private static final int COST_MISTYPE = 16;
    private static final int COST_SCAN = 20;
    private static final int MAX_MAP_CHANGES = 4;
    private static final int MAX_MAP_VARIANTS = 4096;
    private static final int SCAN_STEMS = 48;
    private static final String[] NO_FLAGS = new String[0];

    private static final class Affix {
        final String flag;
        final boolean cross;
        final String strip;
        final String add;
        final String[] cond;
        final String[] cont;

        Affix(String flag, boolean cross, String strip, String add, String[] cond, String[] cont) {
            this.flag = flag;
            this.cross = cross;
            this.strip = strip;
            this.add = add;
            this.cond = cond;
            this.cont = cont;
        }
    }

    private final Map<String, List<String[]>> roots = new HashMap<String, List<String[]>>();
    private final List<Affix> prefixes = new ArrayList<Affix>();
    private final List<Affix> suffixes = new ArrayList<Affix>();
    private final List<String[]> rep = new ArrayList<String[]>();
    private final List<String> map = new ArrayList<String>();
    private final List<String[]> iconv = new ArrayList<String[]>();
    private final List<String[]> oconv = new ArrayList<String[]>();
    private final List<String> aliases = new ArrayList<String>();
    private String tryChars = "";
    private String flagMode = "";
    private String forbidden;
    private String noSuggest;
    private String needAffix;
    private String onlyInCompound;
    private String compoundFlag;
    private String compoundBegin;
    private String compoundMiddle;
    private String compoundEnd;
    private int compoundMin = 3;
    private int compoundWordMax;
    private Set<String> leadingWords = new HashSet<String>();
    private Set<String> trailingWords = new HashSet<String>();

    /// Reads a dictionary from the text of its two files.
    ///
    /// #### Parameters
    ///
    /// - `aff`: the affix file
    ///
    /// - `dic`: the word list
    public HunspellDictionary(String aff, String dic) {
        parseAff(aff);
        parseDic(dic);
    }

    /// Reads a dictionary from the raw bytes of its two files, decoded by the `SET` line.
    ///
    /// #### Parameters
    ///
    /// - `aff`: the affix file
    ///
    /// - `dic`: the word list
    ///
    /// #### Returns
    ///
    /// the dictionary
    public static HunspellDictionary fromBytes(byte[] aff, byte[] dic) {
        String set = encodingOf(aff);
        return new HunspellDictionary(decode(aff, set), decode(dic, set));
    }

    static String decode(byte[] bytes, String set) {
        String s = set == null ? "" : asciiLower(set);
        if (s.startsWith("utf")) {
            try {
                return new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e.toString()); //NOPMD PreserveStackTrace - CLDC has no cause constructor
            }
        }
        boolean l2 = s.indexOf("8859-2") >= 0;
        char[] c = new char[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xff;
            c[i] = l2 && b >= 0xa0 ? LATIN2.charAt(b - 0xa0) : (char) b;
        }
        return new String(c);
    }

    static String encodingOf(byte[] aff) {
        for (String line : split(decode(aff, "ISO8859-1"), '\n')) {
            String t = line.trim();
            if (t.startsWith("SET ")) {
                return t.substring(4).trim();
            }
        }
        return "";
    }

    private static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }

    static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<String>();
        int from = 0;
        for (int i = s.indexOf(sep); i >= 0; i = s.indexOf(sep, from)) {
            out.add(s.substring(from, i));
            from = i + 1;
        }
        out.add(s.substring(from));
        return out;
    }

    /// Lower cases a word letter by letter, which unlike `String.toLowerCase` does not depend
    /// on the device's locale.
    static String lower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            c[i] = Character.toLowerCase(c[i]);
        }
        return new String(c);
    }

    static String upper(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            c[i] = Character.toUpperCase(c[i]);
        }
        return new String(c);
    }

    static String capitalize(String w) {
        return w.length() == 0 ? w : Character.toUpperCase(w.charAt(0)) + w.substring(1);
    }

    private static List<String> fields(String line) {
        List<String> out = new ArrayList<String>();
        for (String f : split(line.replace('\t', ' '), ' ')) {
            if (f.length() > 0) {
                out.add(f);
            }
        }
        return out;
    }

    private static String strip(String raw) {
        String line = raw.trim();
        return line.length() > 0 && line.charAt(0) == '\ufeff' ? line.substring(1) : line;
    }

    private void parseAff(String aff) {
        Map<String, Boolean> pending = new HashMap<String, Boolean>();
        for (String raw : split(aff, '\n')) {
            String line = strip(raw);
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            List<String> f = fields(line);
            String k = f.get(0);
            if (f.size() < 2) {
                continue;
            }
            String v = f.get(1);
            if ("PFX".equals(k) || "SFX".equals(k)) {
                affix(k, f, pending);
            } else if ("ICONV".equals(k) || "OCONV".equals(k) || "REP".equals(k)) {
                if (f.size() > 2) {
                    String[] pair = {v.replace('_', ' '), f.get(2).replace('_', ' ')};
                    ("ICONV".equals(k) ? iconv : "OCONV".equals(k) ? oconv : rep).add(pair);
                }
            } else {
                option(k, v);
            }
        }
    }

    private void option(String k, String v) {
        if ("FLAG".equals(k)) {
            flagMode = v;
        } else if ("TRY".equals(k)) {
            tryChars = v;
        } else if ("FORBIDDENWORD".equals(k)) {
            forbidden = v;
        } else if ("NOSUGGEST".equals(k)) {
            noSuggest = v;
        } else if ("NEEDAFFIX".equals(k)) {
            needAffix = v;
        } else if ("ONLYINCOMPOUND".equals(k)) {
            onlyInCompound = v;
        } else if ("COMPOUNDFLAG".equals(k)) {
            compoundFlag = v;
        } else if ("COMPOUNDBEGIN".equals(k)) {
            compoundBegin = v;
        } else if ("COMPOUNDMIDDLE".equals(k)) {
            compoundMiddle = v;
        } else if ("COMPOUNDEND".equals(k)) {
            compoundEnd = v;
        } else if ("COMPOUNDMIN".equals(k) && isNumber(v)) {
            compoundMin = Math.max(1, Integer.parseInt(v));
        } else if ("COMPOUNDWORDMAX".equals(k) && isNumber(v)) {
            compoundWordMax = Integer.parseInt(v);
        } else if ("AF".equals(k) && !isNumber(v)) {
            aliases.add(v);
        } else if ("MAP".equals(k) && !isNumber(v)) {
            map.add(v);
        }
    }

    private void affix(String k, List<String> f, Map<String, Boolean> pending) {
        if (f.size() < 4) {
            return;
        }
        String flag = f.get(1);
        if (!pending.containsKey(flag + k) && f.size() == 4) {
            pending.put(flag + k, Boolean.valueOf("Y".equals(f.get(2))));
            return;
        }
        Boolean cross = pending.get(flag + k);
        String strip = "0".equals(f.get(2)) ? "" : f.get(2);
        String add = f.get(3);
        int slash = add.indexOf('/');
        String[] cont = NO_FLAGS;
        if (slash >= 0) {
            cont = flags(add.substring(slash + 1));
            add = add.substring(0, slash);
        }
        if ("0".equals(add)) {
            add = "";
        }
        String cond = f.size() > 4 ? f.get(4) : ".";
        Affix a = new Affix(flag, cross != null && cross.booleanValue(), strip, add,
                conditions(cond), cont);
        if ("PFX".equals(k)) {
            prefixes.add(a);
        } else {
            suffixes.add(a);
        }
    }

    private static boolean isNumber(String s) {
        if (s.length() == 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) < '0' || s.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    /// A condition as one entry per character: "." any, "abc" one of, "^abc" none of.
    private static String[] conditions(String c) {
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < c.length(); i++) {
            char ch = c.charAt(i);
            if (ch == '[') {
                int j = c.indexOf(']', i);
                if (j < 0) {
                    j = c.length();
                }
                out.add(c.substring(i + 1, j));
                i = j;
            } else {
                out.add(String.valueOf(ch));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static boolean matches(String[] cond, String s, int from) {
        if (from < 0 || from + cond.length > s.length()) {
            return false;
        }
        for (int i = 0; i < cond.length; i++) {
            String c = cond[i];
            if (".".equals(c)) {
                continue;
            }
            boolean neg = c.length() > 0 && c.charAt(0) == '^';
            boolean in = (neg ? c.substring(1) : c).indexOf(s.charAt(from + i)) >= 0;
            if (in == neg) {
                return false;
            }
        }
        return true;
    }

    private String[] flags(String str) {
        String s = str;
        if (!aliases.isEmpty() && isNumber(s)) {
            int n = Integer.parseInt(s);
            if (n >= 1 && n <= aliases.size()) {
                s = aliases.get(n - 1);
            }
        }
        List<String> out = new ArrayList<String>();
        if ("long".equals(flagMode)) {
            for (int i = 0; i + 1 < s.length(); i += 2) {
                out.add(s.substring(i, i + 2));
            }
        } else if ("num".equals(flagMode)) {
            for (String p : split(s, ',')) {
                if (p.length() > 0) {
                    out.add(p);
                }
            }
        } else {
            for (int i = 0; i < s.length(); i++) {
                out.add(String.valueOf(s.charAt(i)));
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private void parseDic(String dic) {
        boolean first = true;
        for (String raw : split(dic, '\n')) {
            String line = strip(raw);
            if (first) {
                first = false;
                if (isNumber(line)) {
                    continue;
                }
            }
            if (line.length() == 0 || line.charAt(0) == '#') {
                continue;
            }
            int tab = line.indexOf('\t');
            int sp = line.indexOf(' ');
            int cut = tab < 0 ? sp : sp < 0 ? tab : Math.min(tab, sp);
            if (cut >= 0) {
                line = line.substring(0, cut);
            }
            int slash = -1;
            for (int i = 0; i < line.length(); i++) {
                if (line.charAt(i) == '/' && (i == 0 || line.charAt(i - 1) != '\\')) {
                    slash = i;
                    break;
                }
            }
            String word = slash < 0 ? line : line.substring(0, slash);
            String[] fl = slash < 0 ? NO_FLAGS : flags(line.substring(slash + 1));
            word = word.replace("\\/", "/");
            if (word.length() == 0) {
                continue;
            }
            List<String[]> list = roots.get(word);
            if (list == null) {
                list = new ArrayList<String[]>(1);
                roots.put(word, list);
            }
            list.add(fl);
        }
    }

    private static boolean has(String[] fl, String f) {
        if (f == null) {
            return false;
        }
        for (String x : fl) {
            if (x.equals(f)) {
                return true;
            }
        }
        return false;
    }

    /// Does a root carry the given flags? A null `flagA` asks for a bare root; `need` is a
    /// flag the root must carry too; ONLYINCOMPOUND roots count only inside a compound.
    private boolean root(String stem, String flagA, String flagB, String need, boolean inCompound) {
        List<String[]> list = roots.get(stem);
        if (list == null) {
            return false;
        }
        for (String[] fl : list) {
            boolean rejected = has(fl, forbidden) || !inCompound && has(fl, onlyInCompound)
                    || flagA == null && has(fl, needAffix)
                    || flagA != null && !has(fl, flagA)
                    || flagB != null && !has(fl, flagB)
                    || need != null && !has(fl, need);
            if (!rejected) {
                return true;
            }
        }
        return false;
    }

    private boolean formHasFlag(String w, String bit, int depth, String need, boolean inCompound) {
        if (root(w, bit, null, need, inCompound)) {
            return true;
        }
        if (depth == 0) {
            return false;
        }
        for (Affix s : suffixes) {
            if (!has(s.cont, bit)) {
                continue;
            }
            String stem = unSuffix(s, w);
            if (stem != null && formHasFlag(stem, s.flag, depth - 1, need, inCompound)) {
                return true;
            }
        }
        return false;
    }

    private boolean isForbidden(String w) {
        List<String[]> list = roots.get(w);
        if (list == null || forbidden == null) {
            return false;
        }
        for (String[] fl : list) {
            if (has(fl, forbidden)) {
                return true;
            }
        }
        return false;
    }

    private static String unSuffix(Affix a, String w) {
        if (!w.endsWith(a.add) || w.length() - a.add.length() + a.strip.length() == 0) {
            return null;
        }
        String stem = w.substring(0, w.length() - a.add.length()) + a.strip;
        return matches(a.cond, stem, stem.length() - a.cond.length) ? stem : null;
    }

    private static String unPrefix(Affix a, String w) {
        if (!w.startsWith(a.add) || w.length() - a.add.length() + a.strip.length() == 0) {
            return null;
        }
        String stem = a.strip + w.substring(a.add.length());
        return matches(a.cond, stem, 0) ? stem : null;
    }

    /// Is this exact form, with its case as given, in the dictionary or derived by its rules?
    ///
    /// #### Parameters
    ///
    /// - `w`: the word
    ///
    /// #### Returns
    ///
    /// true when it is
    public boolean lookup(String w) {
        if (w.length() == 0 || isForbidden(w)) {
            return false;
        }
        return simple(w, null, false) || compound(w, 0, 0);
    }

    private boolean simple(String w, String need, boolean inCompound) {
        if (root(w, null, null, need, inCompound)) {
            return true;
        }
        for (Affix s : suffixes) {
            String stem = unSuffix(s, w);
            if (stem == null) {
                continue;
            }
            if (formHasFlag(stem, s.flag, 1, need, inCompound)) {
                return true;
            }
            if (s.cross) {
                for (Affix p : prefixes) {
                    if (!p.cross) {
                        continue;
                    }
                    String st2 = unPrefix(p, stem);
                    if (st2 != null && root(st2, s.flag, p.flag, need, inCompound)) {
                        return true;
                    }
                }
            }
        }
        for (Affix p : prefixes) {
            String stem = unPrefix(p, w);
            if (stem != null && formHasFlag(stem, p.flag, 1, need, inCompound)) {
                return true;
            }
        }
        return false;
    }

    private boolean part(String p, int pos) {
        if (p.length() < compoundMin || isForbidden(p)) {
            return false;
        }
        String specific = pos == 0 ? compoundBegin : pos == 1 ? compoundMiddle : compoundEnd;
        return compoundFlag != null && simple(p, compoundFlag, true)
                || specific != null && simple(p, specific, true);
    }

    private boolean compound(String w, int from, int count) {
        if (compoundFlag == null && compoundBegin == null
                || compoundWordMax > 0 && count >= compoundWordMax) {
            return false;
        }
        for (int i = from + compoundMin; i <= w.length() - compoundMin; i++) {
            if (!part(w.substring(from, i), count == 0 ? 0 : 1)) {
                continue;
            }
            boolean lastOk = compoundWordMax == 0 || count + 2 <= compoundWordMax;
            if (lastOk && part(w.substring(i), 2) || compound(w, i, count + 1)) {
                return true;
            }
        }
        return false;
    }

    private static String convert(List<String[]> table, String w) {
        if (table.isEmpty()) {
            return w;
        }
        StringBuilder b = new StringBuilder();
        int i = 0;
        while (i < w.length()) {
            String[] best = null;
            for (String[] r : table) {
                if (r[0].length() > 0 && w.startsWith(r[0], i)
                        && (best == null || r[0].length() > best[0].length())) {
                    best = r;
                }
            }
            if (best == null) {
                b.append(w.charAt(i));
                i++;
            } else {
                b.append(best[1]);
                i += best[0].length();
            }
        }
        return b.toString();
    }

    /// Short words that often run into the next word (leading) or the one before (trailing):
    /// a missing space next to one ranks before other splits in `suggest`.
    ///
    /// #### Parameters
    ///
    /// - `leading`: words that run into the next word, or null
    ///
    /// - `trailing`: words that run into the previous one, or null
    public void setSplitWords(Set<String> leading, Set<String> trailing) {
        leadingWords = leading == null ? new HashSet<String>() : leading;
        trailingWords = trailing == null ? new HashSet<String>() : trailing;
    }

    /// Is the word correct? A capitalized or all capitals word is also accepted in lower case
    /// (sentence starts, headings), but not the reverse: a name listed capitalized is wrong in
    /// lower case.
    ///
    /// #### Parameters
    ///
    /// - `word`: the word
    ///
    /// #### Returns
    ///
    /// true when it is spelled correctly
    public boolean check(String word) {
        String w = convert(iconv, word);
        if (w.length() == 0) {
            return false;
        }
        if (lookup(w)) {
            return true;
        }
        String l = lower(w);
        if (!l.equals(w)) {
            if (lookup(l)) {
                return true;
            }
            String cap = capitalize(l);
            return !cap.equals(w) && lookup(cap);
        }
        return false;
    }

    /// Candidate collector: suggestion to {cost, order found}.
    private final class Collector {
        final String input;
        final Set<String> tested = new HashSet<String>();
        final Map<String, int[]> found = new HashMap<String, int[]>();
        int seq;

        Collector(String input) {
            this.input = input;
        }

        void offer(String cand, int baseCost) {
            if (cand.length() == 0 || cand.equals(input) || !tested.add(cand)) {
                return;
            }
            int cost = baseCost;
            StringBuilder b = new StringBuilder();
            for (String part : split(cand, ' ')) {
                String w;
                if (part.length() > 0 && lookup(convert(iconv, part))) {
                    w = part;
                } else {
                    String c = capitalize(part);
                    if (part.length() == 0 || !lookup(convert(iconv, c))) {
                        return;
                    }
                    w = c;
                    // a name ranks after a common word at the same distance
                    cost++;
                }
                if (b.length() > 0) {
                    b.append(' ');
                }
                b.append(w);
            }
            if (suggestable(cand)) {
                keep(b.toString(), cost);
            }
        }

        void keep(String text, int cost) {
            seq++;
            int[] e = found.get(text);
            if (e == null || cost < e[0]) {
                found.put(text, new int[] {cost, e == null ? seq : e[1]});
            }
        }

        int countWithin(int cost) {
            int n = 0;
            for (int[] e : found.values()) {
                if (e[0] <= cost) {
                    n++;
                }
            }
            return n;
        }
    }

    /// False for a word whose every root entry carries the NOSUGGEST flag: such words are
    /// accepted when typed but never offered.
    private boolean suggestable(String cand) {
        if (noSuggest == null) {
            return true;
        }
        for (String part : split(cand, ' ')) {
            for (String form : new String[] {part, capitalize(part)}) {
                List<String[]> list = roots.get(form);
                if (list == null) {
                    continue;
                }
                boolean any = false;
                for (String[] fl : list) {
                    any |= !has(fl, noSuggest);
                }
                if (!any) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean ascii(char c) {
        return c < 128;
    }

    private static int mapCost(char from, char to) {
        if (ascii(from) && !ascii(to)) {
            return COST_ADD_DIACRITIC;
        }
        if (!ascii(from) && ascii(to)) {
            return COST_DROP_DIACRITIC;
        }
        return COST_OTHER_MAP;
    }

    private String alternatives(char c) {
        StringBuilder b = new StringBuilder();
        for (String g : map) {
            if (g.indexOf(c) < 0) {
                continue;
            }
            for (int i = 0; i < g.length(); i++) {
                char x = g.charAt(i);
                if (x != c && x != '(' && x != ')' && b.indexOf(String.valueOf(x)) < 0) {
                    b.append(x);
                }
            }
        }
        return b.toString();
    }

    private void mapVariants(char[] chars, String[] alts, int i, int changes, int cost, char[] buf,
            List<Object[]> out) {
        if (out.size() >= MAX_MAP_VARIANTS) {
            return;
        }
        if (i == chars.length) {
            if (changes > 0) {
                out.add(new Object[] {new String(buf), Integer.valueOf(cost)});
            }
            return;
        }
        buf[i] = chars[i];
        mapVariants(chars, alts, i + 1, changes, cost, buf, out);
        if (changes >= MAX_MAP_CHANGES) {
            return;
        }
        for (int k = 0; k < alts[i].length(); k++) {
            buf[i] = alts[i].charAt(k);
            mapVariants(chars, alts, i + 1, changes + 1, cost + mapCost(chars[i], buf[i]), buf,
                    out);
        }
        buf[i] = chars[i];
    }

    /// Suggestions for a misspelled word, best first: a case change, `MAP` slips (a missing
    /// diacritic first), `REP` confusions, a missing space, one typo with the `TRY` letters,
    /// and when those find too few, the dictionary words that start like it within two typos.
    ///
    /// #### Parameters
    ///
    /// - `word`: the misspelled word
    ///
    /// - `max`: the most suggestions to return
    ///
    /// #### Returns
    ///
    /// the suggestions, in the case the word was written in
    public List<String> suggest(String word, int max) {
        String w = convert(iconv, word.trim());
        List<String> res = new ArrayList<String>();
        int n = w.length();
        if (n == 0 || n > 40 || max <= 0 || w.indexOf(' ') >= 0) {
            return res;
        }
        String input = lower(w);
        boolean initial = Character.isUpperCase(w.charAt(0))
                && lower(w.substring(1)).equals(w.substring(1));
        boolean upperCase = n > 1 && upper(w).equals(w) && !input.equals(w);
        Collector col = new Collector(input);
        if (lookup(input) && !w.equals(input)) {
            col.keep(input, COST_CASE);
        } else if (lookup(capitalize(input)) && !w.equals(capitalize(input))) {
            col.keep(capitalize(input), COST_CASE);
        }
        char[] chars = input.toCharArray();
        String[] alts = new String[n];
        for (int i = 0; i < n; i++) {
            alts[i] = alternatives(chars[i]);
        }
        List<Object[]> variants = new ArrayList<Object[]>();
        mapVariants(chars, alts, 0, 0, 0, new char[n], variants);
        for (Object[] v : variants) {
            col.offer((String) v[0], ((Integer) v[1]).intValue());
        }
        for (String[] r : rep) {
            if (r[0].length() == 0) {
                continue;
            }
            for (int i = input.indexOf(r[0]); i >= 0; i = input.indexOf(r[0], i + 1)) {
                col.offer(input.substring(0, i) + r[1] + input.substring(i + r[0].length()),
                        COST_REP);
            }
        }
        splits(col, input);
        typos(col, input, chars);
        if (col.countWithin(COST_MISTYPE) < max) {
            scan(col, input);
        }
        List<Map.Entry<String, int[]>> found =
                new ArrayList<Map.Entry<String, int[]>>(col.found.entrySet());
        Collections.sort(found, new ByCost());
        for (Map.Entry<String, int[]> e : found) {
            String s = e.getKey();
            if (initial) {
                s = capitalize(s);
            } else if (upperCase) {
                s = upper(s);
            }
            s = convert(oconv, s);
            if (!s.equals(w) && !res.contains(s)) {
                res.add(s);
            }
            if (res.size() >= max) {
                break;
            }
        }
        return res;
    }

    private void splits(Collector col, String input) {
        int n = input.length();
        for (int i = 1; i < n; i++) {
            String a = input.substring(0, i);
            String b = input.substring(i);
            int cost;
            if (leadingWords.contains(a) && b.length() >= 3
                    || a.length() >= 3 && trailingWords.contains(b)) {
                cost = COST_SPLIT;
            } else if (a.length() >= 3 && b.length() >= 3) {
                cost = COST_SPLIT_RARE;
            } else {
                continue;
            }
            col.offer(a + " " + b, cost);
        }
    }

    private void typos(Collector col, String input, char[] chars) {
        int n = chars.length;
        StringBuilder letters = new StringBuilder();
        for (int i = 0; i < tryChars.length(); i++) {
            if (Character.isLowerCase(tryChars.charAt(i))) {
                letters.append(tryChars.charAt(i));
            }
        }
        String t = letters.length() > 0 ? letters.toString() : "abcdefghijklmnopqrstuvwxyz";
        for (int i = 0; i + 1 < n; i++) {
            if (chars[i] != chars[i + 1]) {
                col.offer(input.substring(0, i) + chars[i + 1] + chars[i] + input.substring(i + 2),
                        COST_SWAP);
            }
        }
        for (int i = 0; i < n; i++) {
            col.offer(input.substring(0, i) + input.substring(i + 1), COST_DROP_OR_ADD);
        }
        for (int i = 0; i <= n; i++) {
            for (int k = 0; k < t.length(); k++) {
                col.offer(input.substring(0, i) + t.charAt(k) + input.substring(i),
                        COST_DROP_OR_ADD);
            }
        }
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < t.length(); k++) {
                if (chars[i] != t.charAt(k)) {
                    col.offer(input.substring(0, i) + t.charAt(k) + input.substring(i + 1),
                            COST_MISTYPE);
                }
            }
        }
    }

    private static final class ByCost implements Comparator<Map.Entry<String, int[]>> {
        @Override
        public int compare(Map.Entry<String, int[]> x, Map.Entry<String, int[]> y) {
            int[] a = x.getValue();
            int[] b = y.getValue();
            return a[0] != b[0] ? a[0] - b[0] : a[1] - b[1];
        }
    }

    private static final class ByShared implements Comparator<Object[]> {
        @Override
        public int compare(Object[] x, Object[] y) {
            return ((Integer) y[1]).intValue() - ((Integer) x[1]).intValue();
        }
    }

    private char fold(char c) {
        for (String g : map) {
            if (g.indexOf(c) >= 0) {
                for (int i = 0; i < g.length(); i++) {
                    char x = g.charAt(i);
                    if (ascii(x) && x != '(' && x != ')') {
                        return x;
                    }
                }
            }
        }
        return c;
    }

    private void scan(Collector col, String input) {
        char first = fold(input.charAt(0));
        List<Object[]> best = new ArrayList<Object[]>();
        for (String r : roots.keySet()) {
            if (r.length() == 0 || fold(Character.toLowerCase(r.charAt(0))) != first
                    || Math.abs(r.length() - input.length()) > 2) {
                continue;
            }
            int shared = 0;
            for (int i = 0; i + 1 < input.length(); i++) {
                if (r.indexOf(input.substring(i, i + 2)) >= 0) {
                    shared++;
                }
            }
            best.add(new Object[] {r, Integer.valueOf(shared)});
        }
        Collections.sort(best, new ByShared());
        for (int k = 0; k < best.size() && k < SCAN_STEMS; k++) {
            String r = (String) best.get(k)[0];
            String l = lower(r);
            int d = weightedDistance(input, l, 4);
            if (d <= 4) {
                col.offer(l, COST_SCAN + d);
            }
        }
    }

    /// Damerau distance in half steps: a typo costs 2, a MAP (diacritic) slip 1.
    private int weightedDistance(String a, String b, int limit) {
        int m = a.length();
        int n = b.length();
        if (Math.abs(m - n) * 2 > limit) {
            return limit + 1;
        }
        int[][] d = new int[m + 1][n + 1];
        for (int i = 0; i <= m; i++) {
            d[i][0] = 2 * i;
        }
        for (int j = 0; j <= n; j++) {
            d[0][j] = 2 * j;
        }
        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                char x = a.charAt(i - 1);
                char y = b.charAt(j - 1);
                int sub = x == y ? 0 : fold(x) == fold(y) ? 1 : 2;
                int v = Math.min(Math.min(d[i - 1][j] + 2, d[i][j - 1] + 2), d[i - 1][j - 1] + sub);
                if (i > 1 && j > 1 && x == b.charAt(j - 2) && a.charAt(i - 2) == y) {
                    v = Math.min(v, d[i - 2][j - 2] + 2);
                }
                d[i][j] = v;
            }
        }
        return d[m][n];
    }

    /// The number of words in the word list, before affixes.
    ///
    /// #### Returns
    ///
    /// the root count
    public int size() {
        return roots.size();
    }
}
