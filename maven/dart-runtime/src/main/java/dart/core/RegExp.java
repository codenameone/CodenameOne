package dart.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dart's {@code dart:core} {@code RegExp}, backed by {@link java.util.regex}.
 *
 * <p>Dart's regular-expression grammar is JavaScript-flavoured ECMAScript,
 * which overlaps almost entirely with Java's {@link Pattern} for the class of
 * patterns the new_gallery app uses (character classes, anchors, quantifiers,
 * capturing groups). The mapping below wires up the flag surface Dart exposes:
 * {@code multiLine}, {@code caseSensitive} (inverse of Java's
 * CASE_INSENSITIVE), {@code unicode} and {@code dotAll}.</p>
 *
 * <p>The named constructor parameters Dart declares are threaded by the
 * transpiler either as constructor arguments or as post-construction setter
 * calls; both shapes are supported here ({@link #multiLine(boolean)} etc.),
 * recompiling the underlying {@link Pattern} lazily on next use.</p>
 */
public final class RegExp {

    private final String source;
    private boolean multiLine;
    private boolean caseSensitive = true;
    private boolean unicode;
    private boolean dotAll;
    private Pattern compiled;

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

    private Pattern compiledPattern() {
        if (compiled == null) {
            int flags = 0;
            if (multiLine) {
                flags |= Pattern.MULTILINE;
            }
            if (!caseSensitive) {
                flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            }
            if (unicode) {
                flags |= Pattern.UNICODE_CASE;
            }
            if (dotAll) {
                flags |= Pattern.DOTALL;
            }
            compiled = Pattern.compile(source, flags);
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
        return input != null && compiledPattern().matcher(input).find();
    }

    /** Dart's {@code RegExp.firstMatch(input)} — null when there is no match. */
    public RegExpMatch firstMatch(String input) {
        if (input == null) {
            return null;
        }
        Matcher m = compiledPattern().matcher(input);
        if (m.find()) {
            return new RegExpMatch(m.toMatchResult(), input);
        }
        return null;
    }

    /** Dart's {@code RegExp.stringMatch(input)} — the matched substring or null. */
    public String stringMatch(String input) {
        RegExpMatch m = firstMatch(input);
        return m == null ? null : m.group(0);
    }

    /** Dart's {@code RegExp.allMatches(input)}. */
    public DartIterable<RegExpMatch> allMatches(String input) {
        DartList<RegExpMatch> out = new DartList<>();
        if (input != null) {
            Matcher m = compiledPattern().matcher(input);
            while (m.find()) {
                out.add(new RegExpMatch(m.toMatchResult(), input));
            }
        }
        return out.asIterable();
    }

    @Override
    public String toString() {
        return "RegExp/" + source + "/";
    }
}
