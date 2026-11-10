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

import com.codename1.python.transpiler.ast.Node.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The whole-program check that lets `Infer` type a program at all.
///
/// The transpiler sees every module of the application, so it can tell
/// whether anything in it does what would make static types unsound:
/// change a class or an instance through a name the compiler cannot see,
/// reach a module's globals from outside, or run code it never compiled.
/// One such use anywhere turns typing off for the whole program -- the
/// generic code stays, and is correct for all of it.
public final class Closed {
    private static final Set<String> CALLS = new HashSet<String>(Arrays.asList("setattr", "delattr", "vars",
            "globals", "locals", "exec", "eval", "compile", "__import__"));
    private static final Set<String> ATTRS = new HashSet<String>(Arrays.asList("__dict__", "__class__",
            "__setattr__", "__getattribute__", "__delattr__", "__new__", "__bases__", "__mro__", "__globals__",
            "__code__", "__defaults__", "__closure__", "__init_subclass__", "__set_name__", "__slots__"));
    private static final Set<String> MODULES = new HashSet<String>(Arrays.asList("copy", "pickle", "importlib",
            "inspect", "ctypes", "types", "gc", "builtins", "abc", "dataclasses", "weakref"));

    private boolean open;

    private Closed() {
    }

    /// Whether the program made of `modules` is closed.
    public static boolean check(List<List<Stmt>> modules) {
        Closed c = new Closed();
        for (List<Stmt> m : modules) {
            Set<String> classes = new HashSet<String>();
            for (Stmt s : m) {
                if (s instanceof ClassDef) {
                    classes.add(((ClassDef) s).name);
                } else if (s instanceof Import) {
                    // A module object: stores through it reach its globals.
                    for (Alias a : ((Import) s).names) {
                        classes.add(a.asname != null ? a.asname : root(a.name));
                    }
                }
            }
            c.scan(m, classes);
        }
        return !c.open;
    }

    private void scan(Object o, Set<String> classes) {
        if (open) {
            return;
        }
        if (o instanceof Name && CALLS.contains(((Name) o).id)) {
            // `type` is fine with one argument; the three-argument form makes a class.
            open = true;
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && "type".equals(((Name) ((Call) o).func).id)
                && ((Call) o).args.size() != 1) {
            open = true;
            return;
        }
        if (o instanceof Attribute && ATTRS.contains(((Attribute) o).attr)) {
            open = true;
            return;
        }
        if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                if (t instanceof Attribute) {
                    open = true;
                    return;
                }
            }
        }
        if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                open |= MODULES.contains(root(a.name));
            }
        } else if (o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            open |= f.module != null && MODULES.contains(root(f.module));
        }
        if (o instanceof Assign || o instanceof AugAssign || o instanceof AnnAssign) {
            // A store onto anything but a plain local name or `x.attr` where
            // x is not a module or class: `mod.x = ...` reaches another
            // module's globals, `Cls.x = ...` changes a class.
            List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                    : Arrays.asList(o instanceof AugAssign ? ((AugAssign) o).target : ((AnnAssign) o).target);
            for (Expr t : targets) {
                attrTargets(t, classes);
            }
        }
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            for (Expr b : c.bases) {
                // Subclassing a class of another module, or anything
                // computed, is a class the module's own analysis cannot see.
                if (!(b instanceof Name) || !classes.contains(((Name) b).id) && !builtinBase(((Name) b).id)) {
                    open = true;
                }
            }
            // A class decorator gets the class to change as it pleases.
            open |= !c.keywords.isEmpty() || !c.decorators.isEmpty();
        }
        Infer.children(o, x -> scan(x, classes));
    }

    private void attrTargets(Expr t, Set<String> classes) {
        if (t instanceof Attribute) {
            Expr v = ((Attribute) t).value;
            // A store through any other expression is a store on an instance.
            open |= v instanceof Name && classes.contains(((Name) v).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                attrTargets(e, classes);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                attrTargets(e, classes);
            }
        } else if (t instanceof Starred) {
            attrTargets(((Starred) t).value, classes);
        }
    }

    private static boolean builtinBase(String n) {
        return "object".equals(n) || n.endsWith("Error") || "Exception".equals(n);
    }

    private static String root(String dotted) {
        int d = dotted.indexOf('.');
        return d < 0 ? dotted : dotted.substring(0, d);
    }
}
