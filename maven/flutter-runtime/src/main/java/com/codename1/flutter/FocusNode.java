package com.codename1.flutter;

import dart.runtime.Funcs;

import java.util.ArrayList;
import java.util.List;

/**
 * An object that can request keyboard focus ({@code FocusNode} in Flutter). The
 * gallery creates these in {@code initState}, hands them to text fields via the
 * {@code focusNode:} parameter, and disposes them. This implementation tracks
 * focus state and listeners; wiring to the actual CN1 component focus is left to
 * the field render elements.
 */
public class FocusNode {

    private String debugLabel;
    private boolean skipTraversal;
    private boolean canRequestFocus = true;
    private boolean hasFocus;
    private final List<Funcs.VoidFunc0> listeners = new ArrayList<Funcs.VoidFunc0>();

    public FocusNode() {
    }

    // Named constructor parameter setters.

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    public void skipTraversal(boolean v) {
        this.skipTraversal = v;
    }

    public void canRequestFocus(boolean v) {
        this.canRequestFocus = v;
    }

    public boolean hasFocus() {
        return hasFocus;
    }

    public boolean hasPrimaryFocus() {
        return hasFocus;
    }

    public void requestFocus(FocusNode node) {
        if (canRequestFocus) {
            setHasFocus(true);
        }
    }

    public void unfocus() {
        setHasFocus(false);
    }

    public void addListener(Funcs.VoidFunc0 listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(Funcs.VoidFunc0 listener) {
        listeners.remove(listener);
    }

    public void dispose() {
        listeners.clear();
    }

    // ------------------------------------------------------------------
    // Framework plumbing
    // ------------------------------------------------------------------

    void setHasFocus(boolean focus) {
        if (this.hasFocus != focus) {
            this.hasFocus = focus;
            for (Funcs.VoidFunc0 l : new ArrayList<Funcs.VoidFunc0>(listeners)) {
                l.call();
            }
        }
    }
}
