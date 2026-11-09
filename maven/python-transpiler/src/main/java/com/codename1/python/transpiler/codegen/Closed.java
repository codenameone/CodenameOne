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
    private static final Set<String> ATTRS = new HashSet<String>(Arrays.asList("__dict__",
            "__setattr__", "__getattribute__", "__delattr__", "__new__", "__bases__", "__mro__", "__globals__",
            "__code__", "__defaults__", "__closure__", "__init_subclass__", "__set_name__", "__slots__"));
    private static final Set<String> MODULES = new HashSet<String>(Arrays.asList("copy", "pickle", "importlib",
            "inspect", "ctypes", "types", "gc", "builtins", "abc", "dataclasses", "weakref"));

    private boolean open;
    private String why;
    private boolean library;

    private Closed() {
    }

    /// Whether the program made of `modules` is closed.
    public static boolean check(List<List<Stmt>> modules) {
        return reason(modules) == null;
    }

    /// What makes the program made of `modules` open, or null when it is closed.
    public static String reason(List<List<Stmt>> modules) {
        return reason(modules, false);
    }

    /// As [#reason(List)], for a module of the bundled standard library
    /// when `library` is true. Those may import `copy`: their own uses of it
    /// copy containers, and their own classes through `__copy__` and
    /// `__deepcopy__`, which build the copy with the constructor. A program
    /// that imports `copy` itself is still open, since `copy.copy` of an
    /// instance of its classes bypasses theirs.
    public static String reason(List<List<Stmt>> modules, boolean library) {
        Closed c = new Closed();
        c.library = library;
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
        return c.open ? (c.why == null ? "open" : c.why) : null;
    }

    /// The modules of `modules` (dotted name to body) that another of them
    /// reaches into: subclasses one of its classes, or stores onto it or
    /// onto one of its classes. Such a module cannot be typed on its own
    /// even when it is closed, since its classes and globals change from
    /// code its analysis does not see. "*" among the answer means a base
    /// class or a store target was computed and could be any module's.
    public static Set<String> reachedInto(java.util.Map<String, List<Stmt>> modules) {
        Set<String> out = new HashSet<String>();
        for (java.util.Map.Entry<String, List<Stmt>> e : modules.entrySet()) {
            java.util.Map<String, String> names = new java.util.HashMap<String, String>();
            Set<String> local = new HashSet<String>();
            List<String> stars = new java.util.ArrayList<String>();
            imports(e.getKey(), e.getValue(), names, local, stars);
            reach(e.getValue(), names, local, stars, out);
        }
        return out;
    }

    private static void imports(String self, Object o, java.util.Map<String, String> names, Set<String> local,
            List<String> stars) {
        if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                if (a.asname != null) {
                    names.put(a.asname, a.name);
                } else {
                    names.put(root(a.name), root(a.name));
                }
            }
        } else if (o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            String from = f.module == null ? "" : f.module;
            if (f.level > 0) {
                String base = self;
                for (int i = 0; i < f.level; i++) {
                    int d = base.lastIndexOf('.');
                    base = d < 0 ? "" : base.substring(0, d);
                }
                from = base.isEmpty() ? from : from.isEmpty() ? base : base + "." + from;
            }
            for (Alias a : f.names) {
                if ("*".equals(a.name)) {
                    stars.add(from);
                } else {
                    names.put(a.asname != null ? a.asname : a.name, from + "." + a.name);
                }
            }
        } else if (o instanceof ClassDef) {
            local.add(((ClassDef) o).name);
        }
        Infer.children(o, x -> imports(self, x, names, local, stars));
    }

    private static void reach(Object o, java.util.Map<String, String> names, Set<String> local, List<String> stars,
            Set<String> out) {
        if (o instanceof ClassDef) {
            for (Expr b : ((ClassDef) o).bases) {
                if (b instanceof Name && (local.contains(((Name) b).id) || builtinBase(((Name) b).id))
                        && !names.containsKey(((Name) b).id)) {
                    continue;
                }
                target(b, names, stars, out);
            }
        }
        if (o instanceof Assign || o instanceof AugAssign || o instanceof AnnAssign || o instanceof Delete) {
            List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                    : o instanceof Delete ? ((Delete) o).targets
                    : Arrays.asList(o instanceof AugAssign ? ((AugAssign) o).target : ((AnnAssign) o).target);
            for (Expr t : targets) {
                stores(t, names, stars, out);
            }
        }
        Infer.children(o, x -> reach(x, names, local, stars, out));
    }

    private static void stores(Expr t, java.util.Map<String, String> names, List<String> stars, Set<String> out) {
        if (t instanceof Attribute) {
            Expr v = ((Attribute) t).value;
            if (v instanceof Name && !names.containsKey(((Name) v).id)) {
                // A local, a parameter or one of this module's own classes.
                return;
            }
            if (v instanceof Name || v instanceof Attribute) {
                target(v, names, stars, out);
            }
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                stores(e, names, stars, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                stores(e, names, stars, out);
            }
        } else if (t instanceof Starred) {
            stores(((Starred) t).value, names, stars, out);
        }
    }

    /// Marks every module `e` (a base class or a store target) may name.
    private static void target(Expr e, java.util.Map<String, String> names, List<String> stars, Set<String> out) {
        String dotted = dotted(e);
        if (dotted == null) {
            out.add("*");
            return;
        }
        int d = dotted.indexOf('.');
        String head = d < 0 ? dotted : dotted.substring(0, d);
        String known = names.get(head);
        if (known == null) {
            // Not imported by name: one of the star imports', if any.
            out.addAll(stars);
            return;
        }
        String full = d < 0 ? known : known + dotted.substring(d);
        // Every prefix may be the module: `a.b.C` is class C of a.b, or
        // module a.b.C; both are marked.
        for (int i = full.indexOf('.'); i > 0; i = full.indexOf('.', i + 1)) {
            out.add(full.substring(0, i));
        }
        out.add(full);
    }

    private static String dotted(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            String v = dotted(((Attribute) e).value);
            return v == null ? null : v + "." + ((Attribute) e).attr;
        }
        return null;
    }

    private void scan(Object o, Set<String> classes) {
        boolean was = open;
        scanNode(o, classes);
        if (!was && open && why == null) {
            why = o.getClass().getSimpleName() + " at line " + line(o);
        }
    }

    private void scanNode(Object o, Set<String> classes) {
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
                open |= module(root(a.name));
            }
        } else if (o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            open |= f.module != null && module(root(f.module));
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

    private boolean module(String root) {
        return MODULES.contains(root) && !(library && "copy".equals(root));
    }

    private void attrTargets(Expr t, Set<String> classes) {
        if (t instanceof Attribute) {
            Expr v = ((Attribute) t).value;
            // Reading `__class__` is harmless; storing it changes an
            // instance's class, and storing through it changes a class.
            open |= "__class__".equals(((Attribute) t).attr)
                    || v instanceof Attribute && "__class__".equals(((Attribute) v).attr);
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

    private static int line(Object o) {
        return o instanceof Node ? ((Node) o).line : -1;
    }

    /// A builtin a class may derive from. A class with any base but one of
    /// its own module's gets no layout, so `Infer` types its instances as
    /// dynamic; what the check has to exclude is deriving from a class of
    /// another module, whose typed methods would be bound to a subclass
    /// they never saw.
    private static boolean builtinBase(String n) {
        return BUILTIN_BASES.contains(n) || n.endsWith("Error") || n.endsWith("Exception") || n.endsWith("Warning");
    }

    private static final Set<String> BUILTIN_BASES = new HashSet<String>(Arrays.asList("object", "str", "int",
            "float", "complex", "bytes", "bytearray", "list", "tuple", "dict", "set", "frozenset"));

    private static String root(String dotted) {
        int d = dotted.indexOf('.');
        return d < 0 ? dotted : dotted.substring(0, d);
    }
}
