package dart.core;

/**
 * Dart's {@code dart:core} {@code RegExpMatch} (a {@code Match}).
 *
 * <p>Group text and offsets are SNAPSHOTTED at construction. The regex engine
 * behind {@link RegExp} keeps its match state on the compiled pattern object
 * and overwrites it on the next match, so a Match holding a reference to it
 * would silently change under the caller — a Dart Match is a value.</p>
 *
 * <p>Dart group indices are 0-based with group 0 being the whole match;
 * unmatched groups are {@code null}.</p>
 */
public final class RegExpMatch {

    private final String[] groups;
    private final int[] starts;
    private final int[] ends;
    private final String input;

    RegExpMatch(String[] groups, int[] starts, int[] ends, String input) {
        this.groups = groups;
        this.starts = starts;
        this.ends = ends;
        this.input = input;
    }

    /** Dart's {@code Match.group(index)} — null for an unmatched group. */
    public String group(long index) {
        int i = (int) index;
        if (i < 0 || i >= groups.length) {
            throw new RangeError("group index out of range: " + index);
        }
        return groups[i];
    }

    /** Dart's {@code match[index]} operator. */
    public String idx(long index) {
        return group(index);
    }

    /** Dart's {@code Match.groupCount} getter — number of capturing groups. */
    public long groupCount() {
        return groups.length - 1;
    }

    /** Dart's {@code Match.start} getter. */
    public long start() {
        return starts[0];
    }

    /** Dart's {@code Match.end} getter. */
    public long end() {
        return ends[0];
    }

    /** Dart's {@code Match.input} getter. */
    public String input() {
        return input;
    }

    /** Dart's {@code Match.groups(indices)} — the listed groups in order. */
    public DartList<String> groups(java.util.List<? extends Number> indices) {
        DartList<String> out = new DartList<String>();
        for (Number n : indices) {
            out.add(group(n.longValue()));
        }
        return out;
    }
}
