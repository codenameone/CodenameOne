package com.codename1.flutter;

/**
 * A range of characters in a string of text ({@code TextRange} in Flutter). The
 * base type for {@link TextSelection}; {@link TextEditingValue#composing()} is a
 * bare {@code TextRange}. A collapsed range has {@code start == end}; an invalid
 * range uses {@code -1} for both.
 *
 * <p>Transpiler surface: the named {@code start:}/{@code end:} constructor
 * parameters map to the {@link #start(int)}/{@link #end(int)} setters, the Dart
 * getters to the zero-arg {@link #start()}/{@link #end()} accessors.</p>
 */
public class TextRange {

    /** An invalid, empty range (Dart's {@code TextRange.empty}). */
    public static final TextRange empty = new TextRange(-1, -1);

    private long start = -1;
    private long end = -1;

    public TextRange() {
    }

    public TextRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    // Named-parameter setters.
    public void start(long v) {
        this.start = v;
    }

    public void end(long v) {
        this.end = v;
    }

    // Named constructor / static getter.
    public static TextRange collapsed(long offset) {
        return new TextRange(offset, offset);
    }

    // Getters.
    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public boolean isValid() {
        return start >= 0 && end >= 0;
    }

    public boolean isCollapsed() {
        return start == end;
    }
}
