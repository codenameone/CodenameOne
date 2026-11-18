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

/// One node of a parsed formula. A formula is kept as a tree so that it can be evaluated,
/// moved (a shared formula, an inserted row) and written back in either file dialect.
final class Node {
    static final int NUM = 0;
    static final int STR = 1;
    static final int BOOL = 2;
    static final int ERR = 3;
    static final int REF = 4;
    static final int NAME = 5;
    static final int FUNC = 6;
    static final int BINARY = 7;
    static final int UNARY = 8;
    static final int PERCENT = 9;
    static final int ARRAY = 10;
    static final int MISSING = 11;
    static final int PAREN = 12;

    static final int MAX_ROW = 1048575;
    static final int MAX_COL = 16383;

    final int kind;
    /// The literal of NUM, STR, BOOL and ERR; the name of NAME and FUNC; the operator of
    /// BINARY and UNARY.
    Object value;
    Node[] args;
    /// The rows of an ARRAY constant.
    Object[][] array;

    // REF only. Rows and columns are zero based; a whole column spans row 0 to MAX_ROW.
    String sheet;
    String sheet2;
    int r1;
    int c1;
    int r2;
    int c2;
    boolean absR1;
    boolean absC1;
    boolean absR2;
    boolean absC2;
    boolean area;
    boolean wholeCol;
    boolean wholeRow;

    Node(int kind) {
        this.kind = kind;
    }

    Node(int kind, Object value, Node... args) {
        this.kind = kind;
        this.value = value;
        this.args = args;
    }

    String name() {
        return (String) value;
    }

    Node copyRef() {
        Node n = new Node(REF);
        n.sheet = sheet;
        n.sheet2 = sheet2;
        n.r1 = r1;
        n.c1 = c1;
        n.r2 = r2;
        n.c2 = c2;
        n.absR1 = absR1;
        n.absC1 = absC1;
        n.absR2 = absR2;
        n.absC2 = absC2;
        n.area = area;
        n.wholeCol = wholeCol;
        n.wholeRow = wholeRow;
        return n;
    }

    /// Whether evaluating this node may answer a reference, which is what the range and
    /// intersection operators take.
    boolean isReferenceLike() {
        switch (kind) {
            case REF:
            case NAME:
            case FUNC:
                return true;
            case PAREN:
                return args[0].isReferenceLike();
            case BINARY:
                String op = name();
                return ":".equals(op) || " ".equals(op) || ",".equals(op);
            default:
                return false;
        }
    }
}
