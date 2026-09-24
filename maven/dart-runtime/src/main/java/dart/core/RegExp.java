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

import com.codename1.util.regex.RE;
import com.codename1.util.regex.RESyntaxException;

/**
 * Dart's {@code dart:core} {@code RegExp}.
 *
 * <p>Backed by Codename One's own regex engine ({@link RE}) rather than
 * {@code java.util.regex}: the latter does not exist on every Codename One
 * target — an iOS build fails at runtime with "Pattern.compile() not
 * implemented on this platform" — and a transpiled app must behave the same on
 * all of them. {@link RE} is plain Java that translates like any app class.</p>
 *
 * <p>Dart's grammar is JavaScript-flavoured ECMAScript, which overlaps with
 * {@link RE}'s Perl5 syntax for the constructs apps actually use: anchors,
 * character classes, quantifiers, alternation and capturing groups. The flag
 * surface Dart exposes is mapped where the engine has an equivalent —
 * {@code multiLine} and {@code caseSensitive}; {@code unicode} and
 * {@code dotAll} are accepted and recorded but have no engine counterpart, so
 * they are inert rather than silently changing the match.</p>
 */
public final class RegExp {

    private final String source;
    private boolean multiLine;
    private boolean caseSensitive = true;
    private boolean unicode;
    private boolean dotAll;
    private RE compiled;

    public RegExp(String source) {
        this.source = source == null ? "" : source;
    }

    public RegExp(String source, boolean multiLine, boolean caseSensitive,
                  boolean unicode, boolean dotAll) {
        this.source = source == null ? "" : source;
        this.multiLine = multiLine;
        this.caseSensitive = caseSensitive;
        this.unicode = unicode;
        this.dotAll = dotAll;
    }

    // Named-argument setters (used when the transpiler lowers named ctor args
    // to post-construction assignments). Each invalidates the cached pattern.
    public void multiLine(boolean value) {
        this.multiLine = value;
        this.compiled = null;
    }

    public void caseSensitive(boolean value) {
        this.caseSensitive = value;
        this.compiled = null;
    }

    public void unicode(boolean value) {
        this.unicode = value;
        this.compiled = null;
    }

    public void dotAll(boolean value) {
        this.dotAll = value;
        this.compiled = null;
    }

    private RE engine() {
        if (compiled == null) {
            int flags = RE.MATCH_NORMAL;
            if (multiLine) {
                flags |= RE.MATCH_MULTILINE;
            }
            if (!caseSensitive) {
                flags |= RE.MATCH_CASEINDEPENDENT;
            }
            if (dotAll) {
                flags |= RE.MATCH_SINGLELINE;   // '.' also matches line terminators
            }
            try {
                compiled = new RE(source, flags);
            } catch (RESyntaxException e) {
                throw new FormatException("Invalid regular expression: /" + source + "/: "
                        + e.getMessage());
            }
        }
        return compiled;
    }

    /** Dart's {@code RegExp.pattern} getter — the original source string. */
    public String pattern() {
        return source;
    }

    /** Legacy alias for {@link #pattern()}. */
    public String getPattern() {
        return source;
    }

    /** Dart's {@code RegExp.hasMatch(input)}. */
    public boolean hasMatch(String input) {
        return input != null && engine().match(input);
    }

    /** Dart's {@code RegExp.firstMatch(input)} — null when there is no match. */
    public RegExpMatch firstMatch(String input) {
        if (input == null) {
            return null;
        }
        RE re = engine();
        return re.match(input) ? snapshot(re, input) : null;
    }

    /** Dart's {@code RegExp.stringMatch(input)} — the matched substring or null. */
    public String stringMatch(String input) {
        RegExpMatch m = firstMatch(input);
        return m == null ? null : m.group(0);
    }

    /** Dart's {@code RegExp.allMatches(input)}. */
    public DartIterable<RegExpMatch> allMatches(String input) {
        DartList<RegExpMatch> out = new DartList<RegExpMatch>();
        if (input != null) {
            int from = 0;
            RegExpMatch m;
            while ((m = matchFrom(input, from)) != null) {
                out.add(m);
                int start = (int) m.start();
                int end = (int) m.end();
                // An empty match must still advance, or this never terminates -- and
                // it must advance past ITS OWN position, not the previous search
                // offset. Comparing the end with `from` let an empty match found
                // later than `from` (RegExp(r'$') on "abc", found at 3 from 0) set
                // `from` to 3 and be found again there.
                from = end > start ? end : end + 1;
            }
        }
        return out.asIterable();
    }

    /**
     * The first match that starts at or after {@code from}, or null. Searching
     * FROM the offset, not filtering allMatches by it: the two differ where
     * matches overlap -- RegExp('aa') in "aaa" from 1 matches at 1, while the
     * matches found from 0 are only the one at 0. String's Pattern methods
     * (indexOf with a start, lastIndexOf) need the former.
     */
    RegExpMatch matchFrom(String input, int from) {
        if (input == null || from < 0 || from > input.length()) {
            return null;
        }
        RE re = engine();
        return re.match(input, from) ? snapshot(re, input) : null;
    }

    /**
     * Copies the engine's current match out of it: group text plus offsets.
     * The engine reuses its state on the next match, so a Match that read
     * through to it would change under the caller.
     */
    private static RegExpMatch snapshot(RE re, String input) {
        int count = Math.max(1, re.getParenCount());
        String[] groups = new String[count];
        int[] starts = new int[count];
        int[] ends = new int[count];
        for (int i = 0; i < count; i++) {
            groups[i] = re.getParen(i);
            starts[i] = re.getParenStart(i);
            ends[i] = re.getParenEnd(i);
        }
        return new RegExpMatch(groups, starts, ends, input);
    }

    @Override
    public String toString() {
        return "RegExp/" + source + "/";
    }
}
