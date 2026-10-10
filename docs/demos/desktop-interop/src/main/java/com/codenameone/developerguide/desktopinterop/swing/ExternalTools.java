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
package com.codenameone.developerguide.desktopinterop.swing;

import com.codename1.ui.CN;

/// A feature the desktop application implemented by starting processes, moved
/// behind an interface so that the user interface no longer knows how it is done.
public class ExternalTools {

    // tag::desktopInteropIsolate[]
    /// What the screens ask for. They hold one of these and nothing else.
    public interface Tools {
        /// Whether the menu item that opens a terminal should be there at all.
        boolean canOpenTerminal();

        void openTerminal(String directory);

        void showInFileManager(String path);
    }

    /// The implementation the Codename One build uses. The desktop project
    /// keeps its own, written with ProcessBuilder, in a class this build
    /// doesn't compile.
    public static class DeviceTools implements Tools {
        @Override
        public boolean canOpenTerminal() {
            return false;
        }

        @Override
        public void openTerminal(String directory) {
            throw new UnsupportedOperationException("No terminal on this device");
        }

        @Override
        public void showInFileManager(String path) {
            String url = path.startsWith("file:") ? path : "file://" + path;
            Boolean can = CN.canExecute(url);
            if (can == null || can.booleanValue()) {
                CN.execute(url);
            }
        }
    }
    // end::desktopInteropIsolate[]
}
