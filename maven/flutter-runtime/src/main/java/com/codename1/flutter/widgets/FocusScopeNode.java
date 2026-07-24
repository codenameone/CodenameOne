package com.codename1.flutter.widgets;

/**
 * A node in the focus tree that establishes a focus scope — Flutter's
 * {@code FocusScopeNode}. API-shape only for this milestone.
 */
public class FocusScopeNode {

    private String debugLabel;

    public FocusScopeNode() {
    }

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    public boolean hasFocus() {
        return false;
    }

    public void requestFocus() {
    }

    public void requestFocus(Object node) {
    }

    public void unfocus() {
    }

    public void unfocus(Object disposition) {
    }
}
