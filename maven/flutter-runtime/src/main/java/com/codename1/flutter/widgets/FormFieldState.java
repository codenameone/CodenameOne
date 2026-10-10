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
 * The state of a single {@link FormField} — Flutter's {@code FormFieldState<T>}.
 * Reached through a {@code GlobalKey<FormFieldState<T>>().currentState}; the
 * text-field demo reads/writes {@link #value()} and drives
 * didChange/validate/save/reset.
 *
 * @param <T> the field's value type
 */
public class FormFieldState<T> {

    private T value;
    private String errorText;

    public T value() {
        return value;
    }

    public boolean hasError() {
        return errorText != null;
    }

    public boolean isValid() {
        return errorText == null;
    }

    public String errorText() {
        return errorText;
    }

    public void didChange(T value) {
        this.value = value;
    }

    public boolean validate() {
        return errorText == null;
    }

    public void save() {
    }

    public void reset() {
        this.value = null;
        this.errorText = null;
    }
}
