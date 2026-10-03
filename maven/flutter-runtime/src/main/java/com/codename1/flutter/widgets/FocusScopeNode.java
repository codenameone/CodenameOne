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
package com.codename1.flutter.widgets;

/**
 * A node in the focus tree that establishes a focus scope — Flutter's
 * {@code FocusScopeNode}. There is one scope, the whole app, so it forwards to
 * the node holding the primary focus.
 */
public class FocusScopeNode {

    private String debugLabel;

    public FocusScopeNode() {
    }

    public void debugLabel(String v) {
        this.debugLabel = v;
    }

    /**
     * Whether a node holds the focus. The whole app is one scope here, so that is
     * whether any node does; these methods were no-ops, so the common
     * {@code FocusScope.of(context).unfocus()} left the keyboard up.
     */
    public boolean hasFocus() {
        return com.codename1.flutter.FocusNode.primaryFocus() != null;
    }

    public void requestFocus() {
    }

    public void requestFocus(Object node) {
        if (node instanceof com.codename1.flutter.FocusNode) {
            ((com.codename1.flutter.FocusNode) node).requestFocus();
        }
    }

    public void unfocus() {
        com.codename1.flutter.FocusNode f = com.codename1.flutter.FocusNode.primaryFocus();
        if (f != null) {
            f.unfocus();
        }
    }

    public void unfocus(Object disposition) {
        unfocus();
    }
}
