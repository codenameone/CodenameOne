/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler.codegen;

import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.Stmt;
import java.util.List;

/// What [GenLowering] asks of the emitter whose statements fill its states:
/// the generic [Emitter], or [TypedEmit] for a generator whose locals have
/// Java types. Values pass as Java expressions of type Object.
interface GenTarget {
    /// Emits `s`, which cannot suspend, as Java statements.
    void stmt(Stmt s, StringBuilder out, int ind);

    /// Stores `value` into the target `t`.
    void store(Expr t, String value, StringBuilder out, int ind);

    /// Stores `value` into the name `n`.
    void storeName(String n, String value, StringBuilder out, int ind);

    /// The Java lvalue that holds the name `n`.
    String nameLvalue(String n);

    /// `e`, which cannot suspend, as a Java expression of type Object.
    String expr(Expr e);

    /// `e` as a Java boolean expression: its truth.
    String cond(Expr e);

    /// A fresh Java identifier starting with `prefix`.
    String fresh(String prefix);

    /// The error for a construct the lowering cannot take.
    RuntimeException unsupported(Node n, String what);

    /// The exceptions of the enclosing handlers, innermost last; a bare
    /// `raise` re-raises the last.
    List<String> handlers();
}
