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
import java.util.Iterator;
import java.util.NoSuchElementException;

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
 * surface Dart exposes is mapped where the engine has an equivalent --
 * {@code multiLine}, {@code caseSensitive} and {@code dotAll}. {@code unicode}
 * has no engine counterpart and is accepted and recorded but inert; see
 * {@link #unicode(boolean)}.</p>
 */
public final class RegExp {

    private final String source;
    // The pattern's DECLARED capturing-group count, not the count a given match
    // reaches. RE.getParenCount() only covers groups the match actually opened --
    // for "(a)|(b)" matching "a", group 2 never opens, so the engine's own count
    // is 1 (just the whole match). Dart's groupCount/group() are keyed off the
    // pattern instead: groupCount answers 2 and group(2) answers null, while
    // group(3) -- past what the pattern declares -- throws RangeError. Computed
    // once from the source text rather than from RE, which has no public accessor
    // for the compiler's own paren count.
    private final int declaredGroupCount;
    private boolean multiLine;
    private boolean caseSensitive = true;
    private boolean unicode;
    private boolean dotAll;
    private RE compiled;

    public RegExp(String source) {
        this.source = source == null ? "" : source;
        this.declaredGroupCount = countCapturingGroups(this.source);
        // Compiled now, as Dart does: RegExp('[') throws FormatException from the
        // constructor. Compiling on first match moved the throw out of a try around
        // the construction -- or dropped it, for a pattern never used.
        engine();
    }

    public RegExp(String source, boolean multiLine, boolean caseSensitive,
                  boolean unicode, boolean dotAll) {
        this.source = source == null ? "" : source;
        this.declaredGroupCount = countCapturingGroups(this.source);
        this.multiLine = multiLine;
        this.caseSensitive = caseSensitive;
        this.unicode = unicode;
        this.dotAll = dotAll;
        engine();   // a malformed pattern throws here, as in Dart
    }

    /**
     * Counts the capturing groups a pattern source declares -- the opening
     * {@code (} of every group that is not one of the non-capturing forms.
     * Skips an escaped {@code \(}, anything inside an unescaped {@code [...]}
     * character class (a literal {@code (} there is not a group), and the
     * non-capturing / lookaround forms {@code (?:} {@code (?=} {@code (?!}
     * {@code (?<=} {@code (?<!}. A named group, {@code (?<name>...)}, is none
     * of those and still counts -- the {@code <} is followed by a name, not
     * {@code =} or {@code !}.
     *
     * <p>Package-private (rather than {@code private}) so the parsing rules
     * can be unit-tested directly: {@link com.codename1.util.regex.RE} itself
     * has no support for lookaround or named groups, so a pattern using them
     * cannot be driven through a real {@link RE} match to exercise this
     * method's handling of them.</p>
     */
    static int countCapturingGroups(String pattern) {
        int count = 0;
        int len = pattern.length();
        boolean inClass = false;
        for (int i = 0; i < len; i++) {
            char c = pattern.charAt(i);
            if (c == '\\') {
                i++;   // skip the escaped character, in or out of a class
                continue;
            }
            if (inClass) {
                if (c == ']') {
                    inClass = false;
                }
                continue;
            }
            if (c == '[') {
                inClass = true;
                continue;
            }
            if (c != '(') {
                continue;
            }
            if (i + 1 < len && pattern.charAt(i + 1) == '?') {
                char after = i + 2 < len ? pattern.charAt(i + 2) : '\0';
                if (after == ':' || after == '=' || after == '!') {
                    continue;   // (?: (?= (?!
                }
                if (after == '<' && i + 3 < len) {
                    char afterLt = pattern.charAt(i + 3);
                    if (afterLt == '=' || afterLt == '!') {
                        continue;   // (?<= (?<!
                    }
                    // else (?<name> ...) -- a named capturing group; falls through to count++.
                }
            }
            count++;
        }
        return count;
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

    /**
     * Records Dart's {@code unicode} flag. The engine matches UTF-16 code units, so in
     * unicode mode a character outside the Basic Multilingual Plane (an emoji, a surrogate
     * pair) still counts as two for {@code .} and quantifiers, and {@code \\u{...}} and
     * {@code \\p{...}} are not understood. It is deliberately not rejected: apps set the flag
     * routinely on patterns over BMP text, where both modes match identically, and failing
     * those would break working code to report a difference they never hit.
     */
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
        return re.match(input) ? snapshot(re, input, declaredGroupCount) : null;
    }

    /** Dart's {@code RegExp.stringMatch(input)} — the matched substring or null. */
    public String stringMatch(String input) {
        RegExpMatch m = firstMatch(input);
        return m == null ? null : m.group(0);
    }

    /**
     * Dart's {@code RegExp.allMatches(input)} -- see {@link #allMatches(String, int)},
     * which this delegates to with {@code start} 0.
     */
    public DartIterable<RegExpMatch> allMatches(final String input) {
        return allMatches(input, 0);
    }

    /**
     * Dart's {@code RegExp.allMatches(input, [start = 0])}: lazy, as Dart's is.
     * Each match is searched for when the iteration reaches it, so {@code
     * allMatches(s).first} or {@code .take(1)} does one search, not one per
     * match in the whole input, and every iteration searches afresh. Matches
     * carry offsets absolute into {@code input}, never relative to {@code
     * start}: the first match is simply the first one found at or after
     * {@code start}, via the same {@link #matchFrom(String, int)} the
     * no-argument overload already used with {@code from} 0.
     *
     * <p>Throws {@link RangeError} when {@code start} is negative or greater
     * than {@code input.length()}, as Dart does. A null {@code input} is
     * accepted leniently as an empty match sequence (the no-argument overload's
     * existing behaviour); there is then no length to range-check {@code
     * start} against, so only a negative {@code start} is rejected.</p>
     */
    public DartIterable<RegExpMatch> allMatches(final String input, final int start) {
        if (input != null) {
            RangeError.checkValueInInterval(start, 0, input.length(), "start");
        } else if (start < 0) {
            throw new RangeError("RangeError (start): Invalid value: Not in inclusive range 0..: " + start);
        }
        return new DartIterable<RegExpMatch>(() -> new Iterator<RegExpMatch>() {
            private int from = start;
            private RegExpMatch pending;
            private boolean exhausted = input == null;

            @Override
            public boolean hasNext() {
                if (pending == null && !exhausted) {
                    pending = matchFrom(input, from);
                    if (pending == null) {
                        exhausted = true;
                    } else {
                        int matchStart = (int) pending.start();
                        int matchEnd = (int) pending.end();
                        // An empty match must still advance, or this never terminates -- and
                        // it must advance past ITS OWN position, not the previous search
                        // offset. Comparing the end with `from` let an empty match found
                        // later than `from` (RegExp(r'$') on "abc", found at 3 from 0) set
                        // `from` to 3 and be found again there.
                        from = matchEnd > matchStart ? matchEnd : matchEnd + 1;
                    }
                }
                return pending != null;
            }

            @Override
            public RegExpMatch next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                RegExpMatch m = pending;
                pending = null;
                return m;
            }
        });
    }

    /** Runs before every search; a seam for tests that count them. */
    static Runnable searchHook;

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
        Runnable hook = searchHook;
        if (hook != null) {
            hook.run();
        }
        RE re = engine();
        return re.match(input, from) ? snapshot(re, input, declaredGroupCount) : null;
    }

    /**
     * Copies the engine's current match out of it: group text plus offsets.
     * The engine reuses its state on the next match, so a Match that read
     * through to it would change under the caller.
     *
     * <p>Sized from {@code declaredGroupCount}, not {@link RE#getParenCount()}:
     * the latter is only the groups this particular match opened. {@link RE}
     * answers null/-1 for a paren index at or past its own {@code parenCount}
     * (see {@link RE#getParen}), so asking it for an index between the two
     * counts correctly comes back as an unmatched group rather than stale
     * data from an earlier match.</p>
     */
    private static RegExpMatch snapshot(RE re, String input, int declaredGroupCount) {
        int count = Math.max(declaredGroupCount + 1, re.getParenCount());
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
