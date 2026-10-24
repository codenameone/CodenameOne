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

import com.codename1.desktopcompat.javax.swing.text.Document;
import com.codename1.desktopcompat.javax.swing.text.Element;

/// A change of a document: where it happened, how many characters it
/// covers and of what kind it was. Element changes are never reported.
public interface DocumentEvent {

    int getOffset();

    int getLength();

    Document getDocument();

    EventType getType();

    ElementChange getChange(Element elem);

    /// The kind of a document change.
    final class EventType {

        public static final EventType INSERT = new EventType("INSERT");

        public static final EventType REMOVE = new EventType("REMOVE");

        public static final EventType CHANGE = new EventType("CHANGE");

        private final String typeString;

        private EventType(String s) {
            typeString = s;
        }

        @Override
        public String toString() {
            return typeString;
        }
    }

    /// The change made to one element of a document's structure.
    interface ElementChange {

        Element getElement();

        int getIndex();

        Element[] getChildrenRemoved();

        Element[] getChildrenAdded();
    }
}
