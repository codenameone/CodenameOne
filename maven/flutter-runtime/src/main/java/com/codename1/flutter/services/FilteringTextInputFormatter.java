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
package com.codename1.flutter.services;

import com.codename1.flutter.TextEditingValue;

/**
 * Filters edited text against a pattern — Flutter's
 * {@code FilteringTextInputFormatter}. new_gallery uses the {@link #digitsOnly}
 * preset; the {@link #allow}/{@link #deny} factories and
 * {@link #singleLineFormatter} are provided for API fidelity. This milestone
 * captures the API shape; the actual character filtering is deferred, so the
 * default {@link #formatEditUpdate} passes the edit through unchanged.
 */
public class FilteringTextInputFormatter extends TextInputFormatter {

    /** Allows only decimal digits ({@code 0-9}). */
    public static final FilteringTextInputFormatter digitsOnly = new FilteringTextInputFormatter();

    /** Collapses newlines so the field stays single-line. */
    public static final FilteringTextInputFormatter singleLineFormatter = new FilteringTextInputFormatter();

    public FilteringTextInputFormatter() {
    }

    /** Dart's {@code FilteringTextInputFormatter.allow} named constructor. */
    public static FilteringTextInputFormatter allow(Object filterPattern, String replacementString) {
        return new FilteringTextInputFormatter();
    }

    /** Dart's {@code FilteringTextInputFormatter.deny} named constructor. */
    public static FilteringTextInputFormatter deny(Object filterPattern, String replacementString) {
        return new FilteringTextInputFormatter();
    }

    @Override
    public TextEditingValue formatEditUpdate(TextEditingValue oldValue, TextEditingValue newValue) {
        return newValue;
    }
}
