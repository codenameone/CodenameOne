package com.codename1.flutter.material;

import com.codename1.flutter.Color;

/**
 * Colours for text selection — Flutter's {@code TextSelectionThemeData}.
 *
 * <p>Held rather than applied: Codename One draws selection with its own theme colours, so
 * these are recorded for the styling pass to pick up. Keeping the type is not cosmetic
 * though — a theme that names it must still transpile, and three of the four studies set
 * one.</p>
 */
public class TextSelectionThemeData {

    private Color cursorColor;
    private Color selectionColor;
    private Color selectionHandleColor;

    public void cursorColor(Color v) {
        this.cursorColor = v;
    }

    public void selectionColor(Color v) {
        this.selectionColor = v;
    }

    public void selectionHandleColor(Color v) {
        this.selectionHandleColor = v;
    }

    public Color getCursorColor() {
        return cursorColor;
    }

    public Color getSelectionColor() {
        return selectionColor;
    }

    public Color getSelectionHandleColor() {
        return selectionHandleColor;
    }
}
