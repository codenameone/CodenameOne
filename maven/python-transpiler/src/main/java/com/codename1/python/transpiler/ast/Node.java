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
package com.codename1.python.transpiler.ast;

import java.util.ArrayList;
import java.util.List;

/// The Python abstract syntax tree, close to CPython's `ast` module.
///
/// Nodes are plain mutable holders; the parser builds them, the scope pass
/// annotates them, and the emitter walks them.
public abstract class Node {
    public int line;
    public int col;

    public <T extends Node> T at(int l, int c) {
        this.line = l;
        this.col = c;
        @SuppressWarnings("unchecked")
        T self = (T) this;
        return self;
    }

    // =====================================================================
    // Expressions
    // =====================================================================

    public abstract static class Expr extends Node {
    }

    public static final class Name extends Expr {
        public final String id;

        public Name(String id) {
            this.id = id;
        }
    }

    /// `None`, `True`, `False`, `...`, numbers, str and bytes.
    public static final class Constant extends Expr {
        public enum Kind { NONE, TRUE, FALSE, ELLIPSIS, INT, FLOAT, STR, BYTES }

        public final Kind kind;
        /// Source text for numbers, decoded text for strings.
        public final String text;

        public Constant(Kind kind, String text) {
            this.kind = kind;
            this.text = text;
        }
    }

    /// An f-string: literal pieces and replacement fields, in order.
    public static final class JoinedStr extends Expr {
        public final List<Expr> parts = new ArrayList<Expr>();
    }

    /// One `{value!conv:spec}` field of an f-string. `spec` is itself a
    /// JoinedStr (it may hold nested fields) or null.
    public static final class FormattedValue extends Expr {
        public final Expr value;
        public final char conversion;
        public final JoinedStr spec;

        public FormattedValue(Expr value, char conversion, JoinedStr spec) {
            this.value = value;
            this.conversion = conversion;
            this.spec = spec;
        }
    }

    public static final class Attribute extends Expr {
        public final Expr value;
        public final String attr;

        public Attribute(Expr value, String attr) {
            this.value = value;
            this.attr = attr;
        }
    }

    public static final class Subscript extends Expr {
        public final Expr value;
        public final Expr index;

        public Subscript(Expr value, Expr index) {
            this.value = value;
            this.index = index;
        }
    }

    public static final class Slice extends Expr {
        public final Expr lower;
        public final Expr upper;
        public final Expr step;

        public Slice(Expr lower, Expr upper, Expr step) {
            this.lower = lower;
            this.upper = upper;
            this.step = step;
        }
    }

    public static final class Starred extends Expr {
        public final Expr value;

        public Starred(Expr value) {
            this.value = value;
        }
    }

    public static final class BinOp extends Expr {
        public final Expr left;
        public final String op;
        public final Expr right;

        public BinOp(Expr left, String op, Expr right) {
            this.left = left;
            this.op = op;
            this.right = right;
        }
    }

    public static final class UnaryOp extends Expr {
        public final String op;
        public final Expr operand;

        public UnaryOp(String op, Expr operand) {
            this.op = op;
            this.operand = operand;
        }
    }

    /// `a and b and c` / `a or b`.
    public static final class BoolOp extends Expr {
        public final boolean isAnd;
        public final List<Expr> values;

        public BoolOp(boolean isAnd, List<Expr> values) {
            this.isAnd = isAnd;
            this.values = values;
        }
    }

    /// `a < b <= c`: `ops.size() == comparators.size()`.
    public static final class Compare extends Expr {
        public final Expr left;
        public final List<String> ops = new ArrayList<String>();
        public final List<Expr> comparators = new ArrayList<Expr>();

        public Compare(Expr left) {
            this.left = left;
        }
    }

    public static final class IfExp extends Expr {
        public final Expr test;
        public final Expr body;
        public final Expr orelse;

        public IfExp(Expr test, Expr body, Expr orelse) {
            this.test = test;
            this.body = body;
            this.orelse = orelse;
        }
    }

    /// `target := value`.
    public static final class NamedExpr extends Expr {
        public final Name target;
        public final Expr value;

        public NamedExpr(Name target, Expr value) {
            this.target = target;
            this.value = value;
        }
    }

    public static final class Keyword extends Node {
        /// null for `**mapping`.
        public final String arg;
        public final Expr value;

        public Keyword(String arg, Expr value) {
            this.arg = arg;
            this.value = value;
        }
    }

    public static final class Call extends Expr {
        public final Expr func;
        /// May contain Starred for `*seq`.
        public final List<Expr> args = new ArrayList<Expr>();
        public final List<Keyword> keywords = new ArrayList<Keyword>();

        public Call(Expr func) {
            this.func = func;
        }
    }

    public static final class ListExpr extends Expr {
        public final List<Expr> elts;

        public ListExpr(List<Expr> elts) {
            this.elts = elts;
        }
    }

    public static final class TupleExpr extends Expr {
        public final List<Expr> elts;

        public TupleExpr(List<Expr> elts) {
            this.elts = elts;
        }
    }

    public static final class SetExpr extends Expr {
        public final List<Expr> elts;

        public SetExpr(List<Expr> elts) {
            this.elts = elts;
        }
    }

    /// `{k: v, **m}`: a null key marks `**value`.
    public static final class DictExpr extends Expr {
        public final List<Expr> keys = new ArrayList<Expr>();
        public final List<Expr> values = new ArrayList<Expr>();
    }

    public static final class Comprehension extends Node {
        public final Expr target;
        public final Expr iter;
        public final List<Expr> ifs = new ArrayList<Expr>();
        public final boolean isAsync;

        public Comprehension(Expr target, Expr iter, boolean isAsync) {
            this.target = target;
            this.iter = iter;
            this.isAsync = isAsync;
        }
    }

    /// List, set and dict comprehensions and generator expressions.
    public static final class Comp extends Expr {
        public enum Kind { LIST, SET, DICT, GENERATOR }

        public final Kind kind;
        public final Expr elt;
        /// The value for dict comprehensions.
        public final Expr value;
        public final List<Comprehension> generators = new ArrayList<Comprehension>();
        public Scope scope;

        public Comp(Kind kind, Expr elt, Expr value) {
            this.kind = kind;
            this.elt = elt;
            this.value = value;
        }
    }

    public static final class Lambda extends Expr {
        public final Arguments args;
        public final Expr body;
        public Scope scope;

        public Lambda(Arguments args, Expr body) {
            this.args = args;
            this.body = body;
        }
    }

    public static final class Await extends Expr {
        public final Expr value;

        public Await(Expr value) {
            this.value = value;
        }
    }

    public static final class Yield extends Expr {
        public final Expr value;
        public final boolean from;

        public Yield(Expr value, boolean from) {
            this.value = value;
            this.from = from;
        }
    }

    // =====================================================================
    // Function signatures
    // =====================================================================

    public static final class Arg extends Node {
        public final String name;
        public final Expr annotation;

        public Arg(String name, Expr annotation) {
            this.name = name;
            this.annotation = annotation;
        }
    }

    public static final class Arguments extends Node {
        /// Positional-only followed by positional-or-keyword.
        public final List<Arg> positional = new ArrayList<Arg>();
        public int positionalOnly;
        /// Defaults for the last `defaults.size()` positional parameters.
        public final List<Expr> defaults = new ArrayList<Expr>();
        public Arg vararg;
        public final List<Arg> kwonly = new ArrayList<Arg>();
        /// Parallel to kwonly; null where required.
        public final List<Expr> kwDefaults = new ArrayList<Expr>();
        public Arg kwarg;

        /// Every parameter name in binding order.
        public List<String> names() {
            List<String> r = new ArrayList<String>();
            for (Arg a : positional) {
                r.add(a.name);
            }
            if (vararg != null) {
                r.add(vararg.name);
            }
            for (Arg a : kwonly) {
                r.add(a.name);
            }
            if (kwarg != null) {
                r.add(kwarg.name);
            }
            return r;
        }
    }

    // =====================================================================
    // Statements
    // =====================================================================

    public abstract static class Stmt extends Node {
    }

    public static final class ExprStmt extends Stmt {
        public final Expr value;

        public ExprStmt(Expr value) {
            this.value = value;
        }
    }

    /// `a = b = value`.
    public static final class Assign extends Stmt {
        public final List<Expr> targets;
        public final Expr value;

        public Assign(List<Expr> targets, Expr value) {
            this.targets = targets;
            this.value = value;
        }
    }

    public static final class AugAssign extends Stmt {
        public final Expr target;
        public final String op;
        public final Expr value;

        public AugAssign(Expr target, String op, Expr value) {
            this.target = target;
            this.op = op;
            this.value = value;
        }
    }

    public static final class AnnAssign extends Stmt {
        public final Expr target;
        public final Expr annotation;
        public final Expr value;

        public AnnAssign(Expr target, Expr annotation, Expr value) {
            this.target = target;
            this.annotation = annotation;
            this.value = value;
        }
    }

    public static final class Return extends Stmt {
        public final Expr value;

        public Return(Expr value) {
            this.value = value;
        }
    }

    public static final class Delete extends Stmt {
        public final List<Expr> targets;

        public Delete(List<Expr> targets) {
            this.targets = targets;
        }
    }

    public static final class Pass extends Stmt {
    }

    public static final class Break extends Stmt {
    }

    public static final class Continue extends Stmt {
    }

    public static final class Raise extends Stmt {
        public final Expr exc;
        public final Expr cause;

        public Raise(Expr exc, Expr cause) {
            this.exc = exc;
            this.cause = cause;
        }
    }

    public static final class Global extends Stmt {
        public final List<String> names;
        public final boolean nonlocal;

        public Global(List<String> names, boolean nonlocal) {
            this.names = names;
            this.nonlocal = nonlocal;
        }
    }

    public static final class Assert extends Stmt {
        public final Expr test;
        public final Expr msg;

        public Assert(Expr test, Expr msg) {
            this.test = test;
            this.msg = msg;
        }
    }

    public static final class Alias extends Node {
        public final String name;
        public final String asname;

        public Alias(String name, String asname) {
            this.name = name;
            this.asname = asname;
        }
    }

    public static final class Import extends Stmt {
        public final List<Alias> names;

        public Import(List<Alias> names) {
            this.names = names;
        }
    }

    public static final class ImportFrom extends Stmt {
        public final String module;
        public final int level;
        /// A single "*" alias for `from m import *`.
        public final List<Alias> names;

        public ImportFrom(String module, int level, List<Alias> names) {
            this.module = module;
            this.level = level;
            this.names = names;
        }
    }

    public static final class If extends Stmt {
        public final Expr test;
        public final List<Stmt> body;
        public final List<Stmt> orelse;

        public If(Expr test, List<Stmt> body, List<Stmt> orelse) {
            this.test = test;
            this.body = body;
            this.orelse = orelse;
        }
    }

    public static final class While extends Stmt {
        public final Expr test;
        public final List<Stmt> body;
        public final List<Stmt> orelse;

        public While(Expr test, List<Stmt> body, List<Stmt> orelse) {
            this.test = test;
            this.body = body;
            this.orelse = orelse;
        }
    }

    public static final class For extends Stmt {
        public final Expr target;
        public final Expr iter;
        public final List<Stmt> body;
        public final List<Stmt> orelse;
        public final boolean isAsync;

        public For(Expr target, Expr iter, List<Stmt> body, List<Stmt> orelse, boolean isAsync) {
            this.target = target;
            this.iter = iter;
            this.body = body;
            this.orelse = orelse;
            this.isAsync = isAsync;
        }
    }

    public static final class WithItem extends Node {
        public final Expr context;
        public final Expr vars;

        public WithItem(Expr context, Expr vars) {
            this.context = context;
            this.vars = vars;
        }
    }

    public static final class With extends Stmt {
        public final List<WithItem> items;
        public final List<Stmt> body;
        public final boolean isAsync;

        public With(List<WithItem> items, List<Stmt> body, boolean isAsync) {
            this.items = items;
            this.body = body;
            this.isAsync = isAsync;
        }
    }

    public static final class ExceptHandler extends Node {
        public final Expr type;
        public final String name;
        public final List<Stmt> body;

        public ExceptHandler(Expr type, String name, List<Stmt> body) {
            this.type = type;
            this.name = name;
            this.body = body;
        }
    }

    public static final class Try extends Stmt {
        public final List<Stmt> body;
        public final List<ExceptHandler> handlers;
        public final List<Stmt> orelse;
        public final List<Stmt> finalbody;
        public final boolean star;

        public Try(List<Stmt> body, List<ExceptHandler> handlers, List<Stmt> orelse, List<Stmt> finalbody,
                boolean star) {
            this.body = body;
            this.handlers = handlers;
            this.orelse = orelse;
            this.finalbody = finalbody;
            this.star = star;
        }
    }

    public static final class FunctionDef extends Stmt {
        public final String name;
        public final Arguments args;
        public final List<Stmt> body;
        public final List<Expr> decorators;
        public final Expr returns;
        public final boolean isAsync;
        public Scope scope;

        public FunctionDef(String name, Arguments args, List<Stmt> body, List<Expr> decorators, Expr returns,
                boolean isAsync) {
            this.name = name;
            this.args = args;
            this.body = body;
            this.decorators = decorators;
            this.returns = returns;
            this.isAsync = isAsync;
        }
    }

    public static final class ClassDef extends Stmt {
        public final String name;
        public final List<Expr> bases;
        public final List<Keyword> keywords;
        public final List<Stmt> body;
        public final List<Expr> decorators;
        public Scope scope;

        public ClassDef(String name, List<Expr> bases, List<Keyword> keywords, List<Stmt> body,
                List<Expr> decorators) {
            this.name = name;
            this.bases = bases;
            this.keywords = keywords;
            this.body = body;
            this.decorators = decorators;
        }
    }

    /// `match subject:` with its cases.
    public static final class Match extends Stmt {
        public final Expr subject;
        public final List<MatchCase> cases;

        public Match(Expr subject, List<MatchCase> cases) {
            this.subject = subject;
            this.cases = cases;
        }
    }

    public static final class MatchCase extends Node {
        public final Pattern pattern;
        public final Expr guard;
        public final List<Stmt> body;

        public MatchCase(Pattern pattern, Expr guard, List<Stmt> body) {
            this.pattern = pattern;
            this.guard = guard;
            this.body = body;
        }
    }

    /// A `match` pattern.
    public static final class Pattern extends Node {
        public enum Kind { VALUE, SINGLETON, CAPTURE, WILDCARD, SEQUENCE, STAR, MAPPING, CLASS, OR, AS }

        public final Kind kind;
        /// VALUE/SINGLETON: the value; CLASS: the class expression.
        public Expr value;
        /// CAPTURE/STAR/AS/MAPPING-rest: the bound name (null for `_`).
        public String name;
        /// SEQUENCE/OR/CLASS-positional/MAPPING-values/AS-inner.
        public final List<Pattern> patterns = new ArrayList<Pattern>();
        /// MAPPING keys.
        public final List<Expr> keys = new ArrayList<Expr>();
        /// CLASS keyword attribute names, parallel to `kwdPatterns`.
        public final List<String> kwdAttrs = new ArrayList<String>();
        public final List<Pattern> kwdPatterns = new ArrayList<Pattern>();

        public Pattern(Kind kind) {
            this.kind = kind;
        }
    }

    /// `type Name[T] = value`.
    public static final class TypeAlias extends Stmt {
        public final String name;
        public final Expr value;

        public TypeAlias(String name, Expr value) {
            this.name = name;
            this.value = value;
        }
    }

    public static final class Module extends Node {
        public final List<Stmt> body;

        public Module(List<Stmt> body) {
            this.body = body;
        }
    }
}
