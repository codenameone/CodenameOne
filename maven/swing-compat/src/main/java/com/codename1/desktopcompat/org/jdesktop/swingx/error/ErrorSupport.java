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
package com.codename1.desktopcompat.org.jdesktop.swingx.error;

import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

/// Keeps the error listeners of a component and tells them of an error,
/// always on the event thread.
public class ErrorSupport {

    private final List<ErrorListener> listeners = new ArrayList<ErrorListener>();
    private final Object source;

    public ErrorSupport(Object source) {
        this.source = source;
    }

    public void addErrorListener(ErrorListener listener) {
        listeners.add(listener);
    }

    public void removeErrorListener(ErrorListener listener) {
        listeners.remove(listener);
    }

    public ErrorListener[] getErrorListeners() {
        return listeners.toArray(new ErrorListener[listeners.size()]);
    }

    /// Tells every listener of `throwable`, now when called on the event
    /// thread and from it later otherwise.
    public void fireErrorEvent(Throwable throwable) {
        ErrorEvent event = new ErrorEvent(throwable, source);
        ErrorListener[] ls = getErrorListeners();
        if (SwingUtilities.isEventDispatchThread()) {
            for (int i = 0; i < ls.length; i++) {
                ls[i].errorOccured(event);
            }
            return;
        }
        SwingUtilities.invokeLater(new Tell(ls, event));
    }

    /// Tells the listeners there were when the error was fired.
    private static final class Tell implements Runnable {

        private final ErrorListener[] listeners;
        private final ErrorEvent event;

        Tell(ErrorListener[] listeners, ErrorEvent event) {
            this.listeners = listeners;
            this.event = event;
        }

        @Override
        public void run() {
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].errorOccured(event);
            }
        }
    }
}
