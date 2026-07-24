package dart.core;

import java.util.regex.MatchResult;

/**
 * Dart's {@code dart:core} {@code RegExpMatch} (a {@code Match}). Wraps a
 * completed {@link MatchResult} so group/position accessors work after the
 * originating {@link java.util.regex.Matcher} has advanced.
 *
 * <p>Dart group indices are 0-based with group 0 being the whole match, which
 * matches {@link MatchResult#group(int)} exactly. Missing/unmatched groups
 * return {@code null} in Dart, mirrored here.</p>
 */
public final class RegExpMatch {

    private final MatchResult result;
    private final String input;

    RegExpMatch(MatchResult result, String input) {
        this.result = result;
        this.input = input;
    }

    /** Dart's {@code Match.group(index)} — null for an unmatched group. */
    public String group(long index) {
        int i = (int) index;
        if (i < 0 || i > result.groupCount()) {
            throw new RangeError("group index out of range: " + index);
        }
        return result.group(i);
    }

    /** Dart's {@code match[index]} operator. */
    public String idx(long index) {
        return group(index);
    }

    /** Dart's {@code Match.groupCount} getter — number of capturing groups. */
    public long groupCount() {
        return result.groupCount();
    }

    /** Dart's {@code Match.start} getter. */
    public long start() {
        return result.start();
    }

    /** Dart's {@code Match.end} getter. */
    public long end() {
        return result.end();
    }

    /** Dart's {@code Match.input} getter. */
    public String input() {
        return input;
    }

    /** Dart's {@code Match.groups(indices)} — the listed groups in order. */
    public DartList<String> groups(java.util.List<? extends Number> indices) {
        DartList<String> out = new DartList<>();
        for (Number n : indices) {
            out.add(group(n.longValue()));
        }
        return out;
    }
}
