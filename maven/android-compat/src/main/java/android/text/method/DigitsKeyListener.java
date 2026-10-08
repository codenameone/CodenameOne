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
package android.text.method;

import android.text.InputType;

/// Restricts input to digits (and optionally sign and decimal point).
public class DigitsKeyListener implements KeyListener {
    private final boolean sign;
    private final boolean decimal;

    public DigitsKeyListener() {
        this(false, false);
    }

    public DigitsKeyListener(boolean sign, boolean decimal) {
        this.sign = sign;
        this.decimal = decimal;
    }

    public static DigitsKeyListener getInstance() {
        return new DigitsKeyListener();
    }

    public static DigitsKeyListener getInstance(boolean sign, boolean decimal) {
        return new DigitsKeyListener(sign, decimal);
    }

    public static DigitsKeyListener getInstance(String accepted) {
        return new DigitsKeyListener(accepted.indexOf('-') >= 0, accepted.indexOf('.') >= 0);
    }

    @Override
    public int getInputType() {
        int t = InputType.TYPE_CLASS_NUMBER;
        if (sign) {
            t |= InputType.TYPE_NUMBER_FLAG_SIGNED;
        }
        if (decimal) {
            t |= InputType.TYPE_NUMBER_FLAG_DECIMAL;
        }
        return t;
    }
}
