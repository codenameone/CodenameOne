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
package com.codenameone.examples.brickyard.luau;

import java.util.ArrayList;

/// The syntax tree the parser builds and the compiler walks.
final class Ast { // NOPMD MissingStaticMethodInNonInstantiatableClass - a namespace for the node classes
    private Ast() {
    }

    abstract static class Expr { // NOPMD AbstractClassWithoutAnyMethod - a base type for the tree nodes
        int line;
    }

    static final class Nil extends Expr {
    }

    static final class Bool extends Expr {
        final boolean v;

        Bool(boolean v) {
            this.v = v;
        }
    }

    static final class Num extends Expr {
        final double v;

        Num(double v) {
            this.v = v;
        }
    }

    static final class Str extends Expr {
        final String v;

        Str(String v) {
            this.v = v;
        }
    }

    static final class Vararg extends Expr {
    }

    /// A name, resolved by the compiler to a local, an upvalue or a global.
    static final class Name extends Expr {
        final String name;

        Name(String name) {
            this.name = name;
        }
    }

    static final class Index extends Expr {
        final Expr obj;
        final Expr key;

        Index(Expr obj, Expr key) {
            this.obj = obj;
            this.key = key;
        }
    }

    static final class Call extends Expr {
        final Expr fn;
        final ArrayList<Expr> args;
        /// Non-null for `obj:method(...)`.
        final String method;

        Call(Expr fn, String method, ArrayList<Expr> args) {
            this.fn = fn;
            this.method = method;
            this.args = args;
        }
    }

    static final class Function extends Expr {
        final ArrayList<String> params = new ArrayList<String>();
        boolean vararg;
        Block body;
        String name = "anonymous";
    }

    static final class Binop extends Expr {
        final String op;
        final Expr a;
        final Expr b;

        Binop(String op, Expr a, Expr b) {
            this.op = op;
            this.a = a;
            this.b = b;
        }
    }

    static final class Unop extends Expr {
        final String op;
        final Expr a;

        Unop(String op, Expr a) {
            this.op = op;
            this.a = a;
        }
    }

    static final class Paren extends Expr {
        final Expr e;

        Paren(Expr e) {
            this.e = e;
        }
    }

    static final class IfExpr extends Expr {
        final ArrayList<Expr> conds = new ArrayList<Expr>();
        final ArrayList<Expr> values = new ArrayList<Expr>();
        Expr orElse;
    }

    static final class Interp extends Expr {
        final ArrayList<String> parts = new ArrayList<String>();
        final ArrayList<Expr> exprs = new ArrayList<Expr>();
    }

    static final class TableCons extends Expr {
        /// Null key means a positional item.
        final ArrayList<Expr> keys = new ArrayList<Expr>();
        final ArrayList<Expr> values = new ArrayList<Expr>();
    }

    abstract static class Stat { // NOPMD AbstractClassWithoutAnyMethod - a base type for the tree nodes
        int line;
    }

    static final class Block {
        final ArrayList<Stat> stats = new ArrayList<Stat>();
    }

    static final class Local extends Stat {
        final ArrayList<String> names = new ArrayList<String>();
        final ArrayList<Expr> values = new ArrayList<Expr>();
    }

    static final class LocalFunction extends Stat {
        final String name;
        final Function fn;

        LocalFunction(String name, Function fn) {
            this.name = name;
            this.fn = fn;
        }
    }

    static final class Assign extends Stat {
        final ArrayList<Expr> targets = new ArrayList<Expr>();
        final ArrayList<Expr> values = new ArrayList<Expr>();
    }

    static final class CompoundAssign extends Stat {
        final String op;
        final Expr target;
        final Expr value;

        CompoundAssign(String op, Expr target, Expr value) {
            this.op = op;
            this.target = target;
            this.value = value;
        }
    }

    static final class ExprStat extends Stat {
        final Call call;

        ExprStat(Call call) {
            this.call = call;
        }
    }

    static final class Do extends Stat {
        final Block body;

        Do(Block body) {
            this.body = body;
        }
    }

    static final class While extends Stat {
        final Expr cond;
        final Block body;

        While(Expr cond, Block body) {
            this.cond = cond;
            this.body = body;
        }
    }

    static final class Repeat extends Stat {
        final Block body;
        final Expr cond;

        Repeat(Block body, Expr cond) {
            this.body = body;
            this.cond = cond;
        }
    }

    static final class If extends Stat {
        final ArrayList<Expr> conds = new ArrayList<Expr>();
        final ArrayList<Block> blocks = new ArrayList<Block>();
        Block orElse;
    }

    static final class NumFor extends Stat {
        String var;
        Expr start;
        Expr limit;
        Expr step;
        Block body;
    }

    static final class GenFor extends Stat {
        final ArrayList<String> vars = new ArrayList<String>();
        final ArrayList<Expr> exprs = new ArrayList<Expr>();
        Block body;
    }

    static final class Return extends Stat {
        final ArrayList<Expr> values = new ArrayList<Expr>();
    }

    static final class Break extends Stat {
    }

    static final class Continue extends Stat {
    }
}
