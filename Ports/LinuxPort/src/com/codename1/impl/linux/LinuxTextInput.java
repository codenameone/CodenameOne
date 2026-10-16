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
package com.codename1.impl.linux;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextInputClient;
import com.codename1.ui.TextInputConfig;
import com.codename1.ui.TextInputState;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.events.FocusListener;

/** GTK input-method binding. Native events carry a session and sequence, and run only on the EDT. */
final class LinuxTextInput {
    private Binding current;
    private int nextSession;

    private static final class Binding {
        final int id;
        final TextInputClient client;
        final TextInputConfig config;
        final Component owner;
        int sequence;
        FocusListener focusListener;
        final Composition composition;

        Binding(int id, TextInputClient client, TextInputConfig config, Component owner) {
            this.id = id;
            this.client = client;
            this.config = config;
            this.owner = owner;
            this.composition = new Composition(client);
        }
    }

    Object start(TextInputClient client, TextInputConfig config) {
        if (client == null) {
            throw new IllegalArgumentException("Text input client is required");
        }
        stop(current);
        if (nextSession == Integer.MAX_VALUE) {
            throw new IllegalStateException("Text input session space exhausted");
        }
        TopLevelContainer top = Display.getInstance().getCurrentTopLevel();
        Component owner = client instanceof Component ? (Component) client
                : top == null ? null : top.getFocused();
        if (owner == null && top != null) {
            owner = top.asContainer();
        }
        final Binding binding = new Binding(++nextSession, client,
                config == null ? new TextInputConfig() : config, owner);
        int slot = owner == null ? LinuxImplementation.MAIN_WINDOW_SLOT
                : LinuxWindowManager.slotForComponent(owner);
        if (!startNative(binding.id, slot, binding.config.getConstraint(),
                binding.config.isAutoCorrect(), binding.config.isAutoCapitalize())) {
            return null;
        }
        current = binding;
        if (owner != null) {
            binding.focusListener = new FocusListener() {
                public void focusGained(Component component) {
                }
                public void focusLost(Component component) {
                    stop(binding);
                }
            };
            owner.addFocusListener(binding.focusListener);
        }
        client.inputFocusGained();
        update(binding, client.getEditingState());
        return binding;
    }

    void update(Object handle, TextInputState state) {
        if (handle == null || handle != current || state == null) {
            return;
        }
        Binding binding = current;
        int[] rect = binding.client.getCaretRect();
        if (rect == null || rect.length < 4) {
            rect = new int[] {0, 0, 1, 1};
        }
        updateNative(binding.id, binding.sequence, state.getText(), state.getSelectionFocus(),
                rect[0], rect[1], Math.max(1, rect[2]), Math.max(1, rect[3]));
    }

    void stopCurrent() {
        stop(current);
    }

    void stop(Object handle) {
        if (handle == null || handle != current) {
            return;
        }
        Binding binding = current;
        current = null;
        if (binding.owner != null && binding.focusListener != null) {
            binding.owner.removeFocusListener(binding.focusListener);
        }
        stopNative(binding.id);
        binding.composition.cancel();
        binding.client.inputFocusLost();
    }

    /** Called by the GTK pump. No document or component state is read on that thread. */
    void drain() {
        while (true) {
            final String event = pollNative();
            if (event == null) {
                return;
            }
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    dispatch(event);
                }
            });
        }
    }

    private void dispatch(String event) {
        // Five numeric fields followed by arbitrary UTF-8 text, including delimiters/newlines.
        int[] fields = new int[5];
        int start = 0;
        for (int i = 0; i < fields.length; i++) {
            int end = event.indexOf(';', start);
            if (end < 0) {
                return;
            }
            try {
                fields[i] = Integer.parseInt(event.substring(start, end));
            } catch (NumberFormatException invalid) {
                return;
            }
            start = end + 1;
        }
        Binding binding = current;
        if (binding == null || fields[0] != binding.id || fields[1] <= binding.sequence) {
            return;
        }
        binding.sequence = fields[1];
        TextInputClient client = binding.client;
        String text = event.substring(start);
        switch (fields[2]) {
            case 0:
                binding.composition.commit(text);
                break;
            case 1:
                binding.composition.preedit(text, fields[3]);
                break;
            case 2:
                binding.composition.cancel();
                break;
            case 3:
                client.onKeyCommand(fields[3], fields[4]);
                break;
            case 4:
                if (binding.config.isMultiline()) {
                    client.commitText("\n");
                } else {
                    client.onEditorAction(binding.config.getActionType());
                }
                break;
            case 5:
                TextInputState state = client.getEditingState();
                int[] deletion = surroundingDeletion(state, fields[3], fields[4]);
                if (deletion != null) {
                    client.deleteSurroundingText(deletion[0], deletion[1]);
                }
                break;
            case 6:
                stop(binding);
                return;
            default:
                return;
        }
        if (current == binding) {
            update(binding, client.getEditingState());
        }
    }

    /** Owns rollback state until an explicit GTK commit, independently of signal ordering. */
    static final class Composition {
        private final TextInputClient client;
        private TextInputState before;
        private String selected;

        Composition(TextInputClient client) {
            this.client = client;
        }

        void preedit(String text, int cursor) {
            if (before == null) {
                if (text.length() == 0) {
                    return;
                }
                before = client.getEditingState();
                selected = client.getTextRange(Math.min(before.getSelectionStart(), before.getSelectionEnd()),
                        Math.max(before.getSelectionStart(), before.getSelectionEnd()));
            }
            client.setComposingText(text, moveCodePoints(text, 0, cursor));
        }

        void commit(String text) {
            client.commitText(text);
            before = null;
            selected = null;
        }

        void cancel() {
            if (before == null) {
                return;
            }
            TextInputState original = before;
            String replacement = selected;
            before = null;
            selected = null;
            if (client instanceof TextInputClient.CompositionCancellation) {
                ((TextInputClient.CompositionCancellation) client).cancelComposing();
            } else {
                client.setComposingText(replacement, replacement.length());
                client.finishComposing();
                client.setSelectionRange(original.getSelectionAnchor(), original.getSelectionFocus());
            }
        }
    }

    static int[] surroundingDeletion(TextInputState state, int offset, int count) {
        if (offset > 0 || count < 0 || (long) offset + count < 0) {
            return null;
        }
        int caret = Math.max(0, Math.min(state.getText().length(), state.getSelectionFocus()));
        int from = moveCodePoints(state.getText(), caret, offset);
        int to = moveCodePoints(state.getText(), caret, offset + count);
        return new int[] {caret - from, to - caret};
    }

    /** GTK counts Unicode characters; the public input API counts UTF-16 units. */
    static int moveCodePoints(String text, int offset, int count) {
        int cursor = Math.max(0, Math.min(text.length(), offset));
        while (count > 0 && cursor < text.length()) {
            char first = text.charAt(cursor++);
            if (Character.isHighSurrogate(first) && cursor < text.length()
                    && Character.isLowSurrogate(text.charAt(cursor))) {
                cursor++;
            }
            count--;
        }
        while (count < 0 && cursor > 0) {
            char last = text.charAt(--cursor);
            if (Character.isLowSurrogate(last) && cursor > 0
                    && Character.isHighSurrogate(text.charAt(cursor - 1))) {
                cursor--;
            }
            count++;
        }
        return cursor;
    }

    private static native boolean startNative(int session, int slot, int constraint,
            boolean autoCorrect, boolean autoCapitalize);
    private static native void updateNative(int session, int sequence, String text, int caret,
            int x, int y, int width, int height);
    private static native void stopNative(int session);
    private static native String pollNative();
}
