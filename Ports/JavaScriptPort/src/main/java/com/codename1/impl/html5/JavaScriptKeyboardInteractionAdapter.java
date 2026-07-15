/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
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
