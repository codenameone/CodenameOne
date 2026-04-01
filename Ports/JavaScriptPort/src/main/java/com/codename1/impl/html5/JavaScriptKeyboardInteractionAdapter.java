/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

public final class JavaScriptKeyboardInteractionAdapter {
    private JavaScriptKeyboardInteractionAdapter() {
    }

    public interface KeyEventView {
        int getKeyCode();
        int getCharCode();
        boolean isShiftKey();
    }

    public interface EditingState {
        boolean isEditing();
    }

    public interface BacksideHooks {
        void installBacksideHooksInUserInteraction();
    }

    public interface KeyDispatch {
        void preventDefault();
        void nativeCallSerially(Runnable runnable);
        void callSerially(Runnable runnable);
        void setShiftKeyDown(boolean down);
        void setLastCharCode(int code);
        int translateKeyCode(KeyEventView event);
        void keyPressed(int code);
        void keyReleased(int code);
        void editFocusedTextArea(KeyEventView event);
    }

    public static boolean shouldPreventDefaultOnKeyDown(boolean editing, KeyEventView event) {
        return !editing && (event.getKeyCode() == 9 || event.getKeyCode() == 11);
    }

    public static void handleKeyDown(final EditingState editingState, final BacksideHooks hooks, final KeyDispatch dispatch, final KeyEventView event) {
        if (shouldPreventDefaultOnKeyDown(editingState.isEditing(), event)) {
            dispatch.preventDefault();
        }
        hooks.installBacksideHooksInUserInteraction();
        dispatch.nativeCallSerially(new Runnable() {
            @Override
            public void run() {
                if (event.getKeyCode() == 16) {
                    dispatch.setShiftKeyDown(true);
                }
                if (event.getCharCode() != 0) {
                    dispatch.setLastCharCode(event.getCharCode());
                }
                dispatch.keyPressed(dispatch.translateKeyCode(event));
            }
        });
    }

    public static void handleKeyUp(final BacksideHooks hooks, final KeyDispatch dispatch, final KeyEventView event) {
        hooks.installBacksideHooksInUserInteraction();
        dispatch.nativeCallSerially(new Runnable() {
            @Override
            public void run() {
                if (event.getKeyCode() == 16) {
                    dispatch.setShiftKeyDown(false);
                }
                dispatch.keyReleased(dispatch.translateKeyCode(event));
            }
        });
    }

    public static void handleKeyPress(final EditingState editingState, final BacksideHooks hooks, final KeyDispatch dispatch, final KeyEventView event) {
        if (editingState.isEditing()) {
            return;
        }
        if (event.getCharCode() != 0) {
            dispatch.setLastCharCode(event.getCharCode());
        }
        hooks.installBacksideHooksInUserInteraction();
        dispatch.nativeCallSerially(new Runnable() {
            @Override
            public void run() {
                if (event.getCharCode() != 0) {
                    dispatch.setLastCharCode(event.getCharCode());
                }
            }
        });
        dispatch.callSerially(new Runnable() {
            @Override
            public void run() {
                dispatch.editFocusedTextArea(event);
            }
        });
    }
}
