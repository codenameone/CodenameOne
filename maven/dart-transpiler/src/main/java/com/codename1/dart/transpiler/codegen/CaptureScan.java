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
package com.codename1.dart.transpiler.codegen;

import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.ast.Ast.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Conservative capture analysis for one method body: a local must be boxed
 * into a Ref holder when it is referenced inside a closure AND assigned
 * anywhere in the method (Java lambdas require effectively-final captures).
 *
 * <p>Conservative means: a name declared inside the closure itself that is
 * also assigned gets boxed too — semantically correct, marginally less
 * pretty output.</p>
 */
final class CaptureScan {

    private final Set<String> assigned = new HashSet<String>();
    private final Set<String> referencedInLambda = new HashSet<String>();
    private final Set<String> allReferenced = new HashSet<String>();
    private int lambdaDepth;

    private CaptureScan() {
    }

    static Set<String> boxedLocals(Block body) {
        CaptureScan scan = new CaptureScan();
        if (body != null) {
            scan.walkBlock(body);
        }
        Set<String> boxed = new HashSet<String>(scan.assigned);
        boxed.retainAll(scan.referencedInLambda);
        return boxed;
    }

    static Set<String> boxedLocals(Expr exprBody) {
        CaptureScan scan = new CaptureScan();
        if (exprBody != null) {
            scan.walkExpr(exprBody);
        }
        Set<String> boxed = new HashSet<String>(scan.assigned);
        boxed.retainAll(scan.referencedInLambda);
        return boxed;
    }

    /**
     * True when {@code name} is referenced from inside a closure within
     * {@code body}. A C-style for loop's index is reassigned by the loop's
     * update clause, so a closure that captures it needs a per-iteration
     * effectively-final copy (Dart binds the loop variable fresh each pass).
     */
    static boolean readInLambda(Stmt body, String name) {
        CaptureScan scan = new CaptureScan();
        scan.walkStmt(body);
        return scan.referencedInLambda.contains(name);
    }

    /** Every identifier name referenced anywhere in an expression. */
    static Set<String> referencedNames(Expr e) {
        CaptureScan scan = new CaptureScan();
        if (e != null) {
            scan.walkExpr(e);
        }
        return scan.allReferenced;
    }

    private void walkBlock(Block b) {
        for (Stmt s : b.statements) {
            walkStmt(s);
        }
    }

    private void walkStmt(Stmt s) {
        if (s == null) {
            return;
        }
        if (s instanceof Block) {
            walkBlock((Block) s);
        } else if (s instanceof ExprStmt) {
            walkExpr(((ExprStmt) s).expr);
        } else if (s instanceof VarDeclStmt) {
            VarDeclStmt v = (VarDeclStmt) s;
            if (v.initializer != null) {
                walkExpr(v.initializer);
            }
        } else if (s instanceof IfStmt) {
            IfStmt i = (IfStmt) s;
            walkExpr(i.condition);
            walkStmt(i.thenStmt);
            walkStmt(i.elseStmt);
        } else if (s instanceof WhileStmt) {
            walkExpr(((WhileStmt) s).condition);
            walkStmt(((WhileStmt) s).body);
        } else if (s instanceof ForStmt) {
            ForStmt f = (ForStmt) s;
            walkStmt(f.init);
            walkExpr(f.condition);
            for (Expr e : f.updates) {
                walkExpr(e);
            }
            walkStmt(f.body);
        } else if (s instanceof ForInStmt) {
            ForInStmt f = (ForInStmt) s;
            walkExpr(f.iterable);
            walkStmt(f.body);
        } else if (s instanceof ReturnStmt) {
            walkExpr(((ReturnStmt) s).value);
        } else if (s instanceof Ast.LocalFunc) {
            // A nested function is lowered to a lambda, so its body is a closure
            // context: outer locals it references-and-mutates must be boxed too.
            Ast.LocalFunc lf = (Ast.LocalFunc) s;
            lambdaDepth++;
            if (lf.body != null) {
                walkBlock(lf.body);
            }
            walkExpr(lf.exprBody);
            lambdaDepth--;
        }
    }

    private void walkExprs(List<Expr> list) {
        for (Expr e : list) {
            walkExpr(e);
        }
    }

    private void walkExpr(Expr e) {
        if (e == null) {
            return;
        }
        if (e instanceof Ident) {
            String nm = ((Ident) e).name;
            allReferenced.add(nm);
            if (lambdaDepth > 0) {
                referencedInLambda.add(nm);
            }
        } else if (e instanceof Assign) {
            Assign a = (Assign) e;
            if (a.lhs instanceof Ident) {
                assigned.add(((Ident) a.lhs).name);
            }
            walkExpr(a.lhs);
            walkExpr(a.rhs);
        } else if (e instanceof IncDec) {
            IncDec i = (IncDec) e;
            if (i.operand instanceof Ident) {
                assigned.add(((Ident) i.operand).name);
            }
            walkExpr(i.operand);
        } else if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            lambdaDepth++;
            if (l.body != null) {
                walkBlock(l.body);
            }
            walkExpr(l.exprBody);
            lambdaDepth--;
        } else if (e instanceof Binary) {
            walkExpr(((Binary) e).left);
            walkExpr(((Binary) e).right);
        } else if (e instanceof Unary) {
            walkExpr(((Unary) e).operand);
        } else if (e instanceof Conditional) {
            Conditional c = (Conditional) e;
            walkExpr(c.condition);
            walkExpr(c.thenExpr);
            walkExpr(c.elseExpr);
        } else if (e instanceof PropertyGet) {
            walkExpr(((PropertyGet) e).target);
        } else if (e instanceof Call) {
            Call c = (Call) e;
            if (c.name != null) {
                allReferenced.add(c.name);
            }
            walkExpr(c.target);
            walkExprs(c.args.positional);
            for (NamedArg na : c.args.named) {
                walkExpr(na.value);
            }
        } else if (e instanceof CtorCall) {
            CtorCall c = (CtorCall) e;
            walkExprs(c.args.positional);
            for (NamedArg na : c.args.named) {
                walkExpr(na.value);
            }
        } else if (e instanceof IndexGet) {
            walkExpr(((IndexGet) e).target);
            walkExpr(((IndexGet) e).index);
        } else if (e instanceof ListLit) {
            walkExprs(((ListLit) e).elements);
        } else if (e instanceof MapLit) {
            walkExprs(((MapLit) e).keys);
            walkExprs(((MapLit) e).values);
        } else if (e instanceof StringLit) {
            for (Object part : ((StringLit) e).parts) {
                if (part instanceof Expr) {
                    walkExpr((Expr) part);
                }
            }
        } else if (e instanceof NotNullAssert) {
            walkExpr(((NotNullAssert) e).operand);
        } else if (e instanceof IsTest) {
            walkExpr(((IsTest) e).operand);
        } else if (e instanceof AsCast) {
            walkExpr(((AsCast) e).operand);
        } else if (e instanceof ParenExpr) {
            walkExpr(((ParenExpr) e).inner);
        }
    }
}
