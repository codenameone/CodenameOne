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
package com.codename1.desktopcompat.rt;

import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/// The platform's own file picker, as `JFileChooser` uses it to open a
/// file: the document picker of a phone, the file dialog of a desktop.
///
/// The picker answers through a callback; [#pick(String)] waits for it the
/// way a modal dialog waits, so events keep running while it is open.
public final class FilePicker {

    /// What opens the picker.
    public interface Source {
        /// Opens the picker; `response` receives an event whose source is
        /// the path chosen, or `null` when the user cancelled. `accept` is
        /// a comma separated list of extensions, or `null` for every file.
        void open(ActionListener<ActionEvent> response, String accept);
    }

    private static final Source NATIVE = new Source() {
        @Override
        public void open(ActionListener<ActionEvent> response, String accept) {
            Display.getInstance().openFileChooser(response, accept);
        }
    };

    private static final Runnable PAUSE = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    };

    private static Source source = NATIVE;

    private FilePicker() {
    }

    /// Replaces what opens the picker; `null` restores the platform's.
    /// For tests.
    public static void setSource(Source s) {
        source = s == null ? NATIVE : s;
    }

    /// Whether there is a display to show a picker on.
    public static boolean available() {
        return Display.isInitialized() && Display.getInstance().isEdt();
    }

    /// Shows the picker and waits for the answer: the path chosen, or
    /// `null` when the user cancelled.
    public static String pick(String accept) {
        final Object[] answer = new Object[2];
        source.open(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object src = evt == null ? null : evt.getSource();
                answer[1] = src instanceof String ? src : null;
                answer[0] = Boolean.TRUE;
            }
        }, accept);
        while (answer[0] == null) {
            Display.getInstance().invokeAndBlock(PAUSE);
        }
        return answer[1] instanceof String ? (String) answer[1] : null;
    }
}
