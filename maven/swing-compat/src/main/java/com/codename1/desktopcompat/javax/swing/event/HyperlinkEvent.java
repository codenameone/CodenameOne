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
package com.codename1.desktopcompat.javax.swing.event;

import com.codename1.desktopcompat.java.awt.event.InputEvent;
import java.net.URL;
import java.util.EventObject;

/// Something happened to a link of an editor pane. This layer fires the
/// type [EventType#ACTIVATED] only, when a link is clicked.
public class HyperlinkEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    private final EventType type;
    private final URL u;
    private final String desc;
    private final InputEvent inputEvent;

    public HyperlinkEvent(Object source, EventType type, URL u) {
        this(source, type, u, null);
    }

    public HyperlinkEvent(Object source, EventType type, URL u, String desc) {
        this(source, type, u, desc, null, null);
    }

    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            com.codename1.desktopcompat.javax.swing.text.Element sourceElement) {
        this(source, type, u, desc, sourceElement, null);
    }

    public HyperlinkEvent(Object source, EventType type, URL u, String desc,
            com.codename1.desktopcompat.javax.swing.text.Element sourceElement, InputEvent inputEvent) {
        super(source);
        this.type = type;
        this.u = u;
        this.desc = desc;
        this.inputEvent = inputEvent;
    }

    public EventType getEventType() {
        return type;
    }

    /// What the link's `href` says, as it was written.
    public String getDescription() {
        return desc;
    }

    /// The link as a URL, or `null` when its `href` is not an absolute
    /// URL.
    public URL getURL() {
        return u;
    }

    /// Always `null`: there are no styled documents in this layer.
    public com.codename1.desktopcompat.javax.swing.text.Element getSourceElement() {
        return null;
    }

    public InputEvent getInputEvent() {
        return inputEvent;
    }

    /// The kinds of event, compared by identity as on the desktop.
    public static final class EventType {

        public static final EventType ENTERED = new EventType("ENTERED");

        public static final EventType EXITED = new EventType("EXITED");

        public static final EventType ACTIVATED = new EventType("ACTIVATED");

        private final String typeString;

        private EventType(String s) {
            typeString = s;
        }

        @Override
        public String toString() {
            return typeString;
        }
    }
}
