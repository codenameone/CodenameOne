/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter;

/**
 * Base class of the widget hierarchy. Widgets are write-once configuration
 * objects: transpiled Dart code allocates a widget, calls its named-parameter
 * setter methods, and hands it to the framework. The element tree (see
 * {@link Element}) is the retained structure; widgets are cheap descriptions
 * that are diffed against the previous configuration on every rebuild.
 */
public abstract class Widget {
    private Key key;

    /**
     * Named parameter setter for the Dart {@code key:} parameter.
     */
    public void key(Key v) {
        this.key = v;
    }

    public Key getKey() {
        return key;
    }

    /**
     * Flutter's Widget.canUpdate: an existing element can absorb a new widget
     * when the runtime type and key both match.
     */
    public static boolean canUpdate(Widget oldWidget, Widget newWidget) {
        if (oldWidget == null || newWidget == null) {
            return false;
        }
        return oldWidget.getClass() == newWidget.getClass()
                && eq(oldWidget.getKey(), newWidget.getKey());
    }

    static boolean eq(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Inflates this widget's configuration into an element. Framework widget
     * subclasses supply this; application widgets inherit it from
     * StatelessWidget/StatefulWidget.
     */
    public abstract Element createElement();
}
