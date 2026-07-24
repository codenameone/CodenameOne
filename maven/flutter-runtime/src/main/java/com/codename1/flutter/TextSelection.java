package com.codename1.flutter;

/**
 * A selected range within editable text ({@code TextSelection} in Flutter),
 * extending {@link TextRange} with a base/extent (anchor/caret) pair. The
 * new_gallery phone-number formatter reads {@code selection.end} and produces a
 * collapsed selection via {@link #collapsed(int)}.
 */
public class TextSelection extends TextRange {

    private long baseOffset;
    private long extentOffset;

    public TextSelection() {
    }

    // Named-parameter setters.
    public void baseOffset(long v) {
        this.baseOffset = v;
        syncRange();
    }

    public void extentOffset(long v) {
        this.extentOffset = v;
        syncRange();
    }

    /** {@code TextSelection.collapsed(offset: ...)} — a zero-length selection. */
    public static TextSelection collapsed(long offset) {
        TextSelection s = new TextSelection();
        s.baseOffset = offset;
        s.extentOffset = offset;
        s.syncRange();
        return s;
    }

    public long baseOffset() {
        return baseOffset;
    }

    public long extentOffset() {
        return extentOffset;
    }

    private void syncRange() {
        start(Math.min(baseOffset, extentOffset));
        end(Math.max(baseOffset, extentOffset));
    }
}
