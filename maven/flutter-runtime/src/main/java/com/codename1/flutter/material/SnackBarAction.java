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
package com.codename1.flutter.material;

import com.codename1.flutter.Color;

import dart.runtime.Funcs;

/**
 * An action button shown alongside a {@link SnackBar}'s content — Flutter's
 * {@code SnackBarAction}. Configuration only (label + press callback); the
 * {@link SnackBar} consumes it when shown.
 */
public class SnackBarAction {

    private String label;
    private Funcs.VoidFunc0 onPressed;
    private Color textColor;
    private Color disabledTextColor;

    public void label(String v) {
        this.label = v;
    }

    public void onPressed(Funcs.VoidFunc0 v) {
        this.onPressed = v;
    }

    public void textColor(Color v) {
        this.textColor = v;
    }

    public void disabledTextColor(Color v) {
        this.disabledTextColor = v;
    }

    public String getLabel() {
        return label;
    }

    public String label() {
        return label;
    }

    public Funcs.VoidFunc0 getOnPressed() {
        return onPressed;
    }
}
