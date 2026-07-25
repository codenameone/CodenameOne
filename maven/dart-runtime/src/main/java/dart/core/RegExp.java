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
            RE re = engine();
            int from = 0;
            while (from <= input.length() && re.match(input, from)) {
                RegExpMatch m = snapshot(re, input);
                out.add(m);
                int end = (int) m.end();
                // an empty match must still advance, or this never terminates
                from = end > from ? end : from + 1;
            }
        }
        return out.asIterable();
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
