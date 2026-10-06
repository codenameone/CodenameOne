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
package com.codename1.tools.javac;

import java.util.List;

/** Visits every node of a tree; subclasses override {@link #scan(Tree)} and call super to descend. */
class TreeScanner {
    void scan(List<? extends Tree> trees) {
        if (trees != null) {
            for (Tree t : trees) {
                scan(t);
            }
        }
    }

    void scan(Tree t) {
        if (t == null) {
            return;
        }
        if (t instanceof Tree.Block) {
            scan(((Tree.Block) t).stats);
        } else if (t instanceof Tree.ExpressionStatement) {
            scan(((Tree.ExpressionStatement) t).expr);
        } else if (t instanceof Tree.VarDef) {
            scan(((Tree.VarDef) t).init);
        } else if (t instanceof Tree.If) {
            Tree.If s = (Tree.If) t;
            scan(s.cond);
            scan(s.thenPart);
            scan(s.elsePart);
        } else if (t instanceof Tree.WhileLoop) {
            scan(((Tree.WhileLoop) t).cond);
            scan(((Tree.WhileLoop) t).body);
        } else if (t instanceof Tree.DoLoop) {
            scan(((Tree.DoLoop) t).body);
            scan(((Tree.DoLoop) t).cond);
        } else if (t instanceof Tree.ForLoop) {
            Tree.ForLoop f = (Tree.ForLoop) t;
            scan(f.init);
            scan(f.cond);
            scan(f.step);
            scan(f.body);
        } else if (t instanceof Tree.ForEach) {
            scan(((Tree.ForEach) t).expr);
            scan(((Tree.ForEach) t).body);
        } else if (t instanceof Tree.Labeled) {
            scan(((Tree.Labeled) t).body);
        } else if (t instanceof Tree.Switch) {
            Tree.Switch sw = (Tree.Switch) t;
            scan(sw.selector);
            for (Tree.Case c : sw.cases) {
                scan(c.labels);
                scan(c.guard);
                scan(c.stats);
            }
        } else if (t instanceof Tree.Return) {
            scan(((Tree.Return) t).expr);
        } else if (t instanceof Tree.Yield) {
            scan(((Tree.Yield) t).value);
        } else if (t instanceof Tree.Throw) {
            scan(((Tree.Throw) t).expr);
        } else if (t instanceof Tree.Try) {
            Tree.Try tr = (Tree.Try) t;
            scan(tr.resources);
            scan(tr.body);
            for (Tree.Catch c : tr.catches) {
                scan(c.body);
            }
            scan(tr.finalizer);
        } else if (t instanceof Tree.Synchronized) {
            scan(((Tree.Synchronized) t).lock);
            scan(((Tree.Synchronized) t).body);
        } else if (t instanceof Tree.Assert) {
            scan(((Tree.Assert) t).cond);
            scan(((Tree.Assert) t).detail);
        } else if (t instanceof Tree.Select) {
            scan(((Tree.Select) t).selected);
        } else if (t instanceof Tree.Parens) {
            scan(((Tree.Parens) t).expr);
        } else if (t instanceof Tree.Assign) {
            scan(((Tree.Assign) t).lhs);
            scan(((Tree.Assign) t).rhs);
        } else if (t instanceof Tree.CompoundAssign) {
            scan(((Tree.CompoundAssign) t).lhs);
            scan(((Tree.CompoundAssign) t).rhs);
        } else if (t instanceof Tree.Unary) {
            scan(((Tree.Unary) t).arg);
        } else if (t instanceof Tree.Binary) {
            scan(((Tree.Binary) t).lhs);
            scan(((Tree.Binary) t).rhs);
        } else if (t instanceof Tree.Conditional) {
            Tree.Conditional c = (Tree.Conditional) t;
            scan(c.cond);
            scan(c.truePart);
            scan(c.falsePart);
        } else if (t instanceof Tree.InstanceOf) {
            scan(((Tree.InstanceOf) t).expr);
            scan(((Tree.InstanceOf) t).pattern);
        } else if (t instanceof Tree.RecordPattern) {
            scan(((Tree.RecordPattern) t).nested);
        } else if (t instanceof Tree.Cast) {
            scan(((Tree.Cast) t).expr);
        } else if (t instanceof Tree.MethodCall) {
            scan(((Tree.MethodCall) t).receiver);
            scan(((Tree.MethodCall) t).args);
        } else if (t instanceof Tree.NewClass) {
            scan(((Tree.NewClass) t).outer);
            scan(((Tree.NewClass) t).args);
            scan(((Tree.NewClass) t).body);
        } else if (t instanceof Tree.ClassDecl) {
            scan(((Tree.ClassDecl) t).members);
        } else if (t instanceof Tree.MethodDecl) {
            scan(((Tree.MethodDecl) t).body);
        } else if (t instanceof Tree.NewArray) {
            scan(((Tree.NewArray) t).dims);
            scan(((Tree.NewArray) t).elems);
        } else if (t instanceof Tree.ArrayAccess) {
            scan(((Tree.ArrayAccess) t).array);
            scan(((Tree.ArrayAccess) t).index);
        } else if (t instanceof Tree.Lambda) {
            scan(((Tree.Lambda) t).body);
        } else if (t instanceof Tree.MethodRef) {
            scan(((Tree.MethodRef) t).qualifier);
        }
    }
}
