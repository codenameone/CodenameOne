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
package com.codename1.fxcompat.runtime;

import java.util.concurrent.atomic.AtomicBoolean;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/// Asks the user for a file through the platform's own picker and waits
/// for the answer, which is how a JavaFX file chooser behaves: the call
/// returns the file or `null`.
///
/// Codename One's picker answers later, through a callback. The event
/// thread keeps running on a nested loop meanwhile, as it does for a
/// window shown with `showAndWait()`.
public final class FilePicker {

    private static Source source;

    private FilePicker() {
    }

    /// What answers in place of the platform's picker.
    public interface Source {
        /// Returns the path chosen for a list of extensions, or `null` for
        /// a user who cancelled.
        String choose(String accept);
    }

    /// Puts something in place of the platform's picker, or with `null`
    /// the picker back. A test has no user to choose a file.
    public static void setSource(Source value) {
        source = value;
    }

    /// Returns the path of the file the user chose, as the file system of
    /// Codename One names it, or `null` when the user cancelled. `accept`
    /// is a comma separated list of extensions, or `null` for any file.
    public static String open(String accept) {
        Source s = source;
        if (s != null) {
            return s.choose(accept);
        }
        if (!Display.isInitialized()) {
            return null;
        }
        final String[] chosen = new String[1];
        final AtomicBoolean answered = new AtomicBoolean();
        Display.getInstance().openFileChooser(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Object path = evt == null ? null : evt.getSource();
                if (path instanceof String) {
                    chosen[0] = (String) path;
                }
                answered.set(true);
            }
        }, accept);
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (!answered.get()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException interrupted) {
                        return;
                    }
                }
            }
        });
        return chosen[0];
    }

    /// Returns the directory the application may write to, which is where
    /// a file to save goes when there is no picker to ask.
    public static String home() {
        if (!Display.isInitialized()) {
            return "";
        }
        String home = FileSystemStorage.getInstance().getAppHomePath();
        return home == null ? "" : home;
    }
}
