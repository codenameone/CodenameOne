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
package com.codename1.spreadsheet;

/// Carries an error value out of the middle of a function. It is always thrown explicitly and
/// caught where the function was called, which turns it back into the `CellError` it holds.
/// One instance per error is reused: nothing reads its stack trace.
final class EvalError extends RuntimeException {
    static final EvalError VALUE = new EvalError(CellError.VALUE);
    static final EvalError NUM = new EvalError(CellError.NUM);
    static final EvalError NA = new EvalError(CellError.NA);
    static final EvalError DIV0 = new EvalError(CellError.DIV0);
    static final EvalError REF = new EvalError(CellError.REF);
    static final EvalError NAME = new EvalError(CellError.NAME);
    static final EvalError NULL = new EvalError(CellError.NULL);
    static final EvalError CIRCULAR = new EvalError(CellError.CIRCULAR);
    /// Thrown by Evaluator.scalar when a function's argument is an array where the function
    /// wants one value: the call is then made once per element, as Excel 365 makes it.
    static final EvalError LIFT = new EvalError(CellError.VALUE);

    final transient CellError error;

    private EvalError(CellError error) {
        super(error.toString());
        this.error = error;
    }

    static EvalError of(CellError e) {
        if (e == CellError.VALUE) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return VALUE;
        }
        if (e == CellError.NUM) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return NUM;
        }
        if (e == CellError.NA) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return NA;
        }
        if (e == CellError.DIV0) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return DIV0;
        }
        if (e == CellError.REF) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return REF;
        }
        if (e == CellError.NAME) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return NAME;
        }
        if (e == CellError.NULL) { //NOPMD CompareObjectsWithEquals - an error value is a constant
            return NULL;
        }
        return CIRCULAR;
    }
}
