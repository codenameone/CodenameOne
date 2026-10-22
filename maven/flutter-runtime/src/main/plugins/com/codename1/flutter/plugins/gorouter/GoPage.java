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
package com.codename1.flutter.plugins.gorouter;

import com.codename1.flutter.Widget;

/**
 * The {@code Page} a {@code pageBuilder} returns -- {@code MaterialPage},
 * {@code NoTransitionPage} and {@code CustomTransitionPage} all map here. Only the
 * page's child is shown; its transition is the platform's own.
 */
public class GoPage {
    private Widget child;
    private Object key;
    private String name;
    private Object arguments;

    public GoPage() {
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget child() {
        return child;
    }

    public void key(Object v) {
        this.key = v;
    }

    public Object key() {
        return key;
    }

    public void name(String v) {
        this.name = v;
    }

    public String name() {
        return name;
    }

    public void arguments(Object v) {
        this.arguments = v;
    }

    public Object arguments() {
        return arguments;
    }

    /** Accepted for source compatibility; see the class comment. */
    public void transitionsBuilder(Object v) {
    }

    /** Accepted for source compatibility. */
    public void transitionDuration(Object v) {
    }

    /** Accepted for source compatibility. */
    public void fullscreenDialog(Object v) {
    }

    /** Accepted for source compatibility. */
    public void maintainState(Object v) {
    }
}
