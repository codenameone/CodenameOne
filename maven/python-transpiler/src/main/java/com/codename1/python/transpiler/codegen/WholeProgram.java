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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// What the rest of the program does to a module, for `Infer`.
///
/// The transpiler sees every module of the application, so a module is
/// never typed on the assumption that nothing outside it matters, and
/// never left untyped because something might: the stores that reach it
/// from elsewhere are found here, by name, and handed to its analysis.
///
/// Three things are all that typed code relies on beyond its own module.
/// A typed field holds only what the stores `Infer` saw put there; a
/// method called directly is the one the class body bound; a global bound
/// once still has that value. Each fact below takes one of those away for
/// one name, and nothing else changes:
///
/// - an attribute name another module stores through anything but a
///   method's own receiver (`x.n = v`, `setattr(x, "n", v)`) is no typed
///   field and no directly called method here, and a global of that name
///   is not stable;
/// - an attribute name any module deletes (`del x.n`) is no field typed
///   code reads without asking whether it is set;
/// - an attribute name this module stores through anything but a
///   method's own receiver is no directly called method;
/// - a module that reads its own namespace (`globals()`, `vars()`,
///   `locals()`) has no stable globals.
///
/// Nothing else needs a fact. A class with a base from another module, a
/// decorator, a metaclass or `__new__` has no compiled layout (see
/// `Layouts`), and a subclass defined anywhere gets ordinary instances
/// (`PyType.layout`), so typed code never receives one: its entry checks
/// the Java class of every argument. Importing `copy`, `dataclasses` or
/// `inspect` changes nothing either -- what they do to an instance goes
/// through the same stores as any other code. A store whose name is
/// computed (`setattr(o, name, v)`, `o.__dict__[k] = v`,
/// `object.__setattr__`, a write through `vars(o)`) is followed like any
/// other: the inference joins what it stores into every field it could
/// name -- those of the receiver's class and the classes related to it,
/// or, for a receiver of no known class, those of every class whose
/// instances are held that way (`Infer.computedStore`). The build notes
/// each such store that cost a field its type. The generic store of a
/// field still checks the value against the field's type, so a store the
/// inference could not see raises where it happens and is never read back
/// as a value of the wrong type.
///
/// The same pass decides what the program reaches (`Infer.Program.reached`):
/// a function nothing calls, names or can find by a computed name is left
/// out of the build, with a body that says so. `Directives` is how a build
/// overrides that by name, in either direction.
public final class WholeProgram {
    /// `unittest.mock`'s patchers, which store an attribute named by a
    /// string: `patch`, `patch.object`, `patch.multiple`.
    private static final Set<String> PATCHERS = new HashSet<String>(Arrays.asList("patch", "object", "multiple"));
    private static final Set<String> NAMESPACE = new HashSet<String>(Arrays.asList("globals", "vars", "locals"));

    /// The facts of one module.
    public static final class Module {
        /// Attribute names stored from outside the module, or by a literal
        /// `setattr` inside it.
        final Set<String> foreign = new HashSet<String>();
        /// Attribute names the module stores through anything but the
        /// receiver of the method the store is in.
        final Set<String> loose = new HashSet<String>();
        /// Whether the module reads its own namespace as a dict.
        boolean namespace;
        /// Attribute names any module of the program deletes (`del x.n`,
        /// `delattr(x, "n")`): a field of that name may be unset on an
        /// instance that had it.
        Set<String> deleted = new HashSet<String>();
        /// Names other modules mention: an attribute they read, a name
        /// they import. What they can call or construct by name.
        final Set<String> mentioned = new HashSet<String>();
        /// The names this module mentions.
        final Set<String> mentions = new HashSet<String>();
        /// Whether another module takes all of this one's names
        /// (`from m import *`).
        boolean starImported;
        /// Whether the module reads an attribute whose name it computes
        /// (`getattr(o, name)`): any method of its classes may be the one.
        boolean computedRead;
        /// Whether the module gets at the class of a value (`type(x)`,
        /// `x.__class__`, a `classmethod`'s first parameter), which it may
        /// then call.
        boolean classValues;
        /// Whether the module takes the attributes of an instance as a
        /// dict (`o.__dict__`, `vars(o)`), through which it may remove any
        /// of them: no field of its classes is then known to be set.
        boolean dictViews;
        final List<String> stars = new java.util.ArrayList<String>();

        /// Whether the top-level function or class `name` can be reached
        /// from outside the module by its name.
        boolean reachable(String name) {
            return starImported || namespace || mentioned.contains(name);
        }

        /// Whether a method `name` of a class of this module may be called
        /// by code that does not name the class: through an attribute of
        /// that name read anywhere else, or one this module computes.
        boolean calledByName(String name) {
            return computedRead || mentioned.contains(name);
        }

        /// Whether attribute `name` of an instance may hold a value this
        /// module's stores do not account for.
        boolean foreign(String name) {
            return foreign.contains(name);
        }

        /// Whether a class attribute `name` may be rebound after the class
        /// body ran.
        boolean patched(String name) {
            return foreign.contains(name) || loose.contains(name) || deleted.contains(name);
        }
    }

    private final Map<String, Module> modules = new HashMap<String, Module>();

    WholeProgram() {
    }

    /// The facts of `module`; of a module the program does not have, none.
    public Module module(String module) {
        Module m = modules.get(module);
        return m == null ? new Module() : m;
    }

    /// The types of the whole program: every module's pass over one heap.
    final Infer.Program types = new Infer.Program();

    /// Infers the types of every module entered (`Emitter.prepare`).
    /// What the Java side of the program may look up by name (see
    /// `ClassPathReader.names`), and the functions the build keeps on
    /// request. Without the first, no function is left out.
    public void roots(Set<String> runtimeNames, java.util.Collection<String> keep) {
        types.runtimeNames = runtimeNames;
        types.keep.addAll(keep);
    }

    /// One line for each place where reached code does to an object of
    /// no known class what the typed fields of every class it may be have
    /// to allow for: `module:line: function stores ...`.
    public List<String> notes() {
        List<String> out = new java.util.ArrayList<String>();
        for (Infer.Problem q : types.problems.values()) {
            if (q.store) {
                out.add(q.module + ":" + q.line + ": " + (q.function == null ? "the module" : q.function) + " "
                        + q.what);
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    public void infer() {
        types.run();
    }

    /// Reads every module of the program (dotted name to body).
    public static WholeProgram of(Map<String, List<Stmt>> bodies) {
        WholeProgram p = new WholeProgram();
        Map<String, Set<String>> stores = new HashMap<String, Set<String>>();
        Map<String, Integer> count = new HashMap<String, Integer>();
        Set<String> deleted = new HashSet<String>();
        for (Map.Entry<String, List<Stmt>> e : bodies.entrySet()) {
            Module m = new Module();
            m.deleted = deleted;
            Set<String> all = new HashSet<String>();
            scan(e.getValue(), null, m, all);
            p.modules.put(e.getKey(), m);
            stores.put(e.getKey(), all);
            for (String n : all) {
                Integer c = count.get(n);
                count.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
        }
        Map<String, Integer> mcount = new HashMap<String, Integer>();
        boolean allStarred = false;
        Set<String> starred = new HashSet<String>();
        for (Module m : p.modules.values()) {
            for (String n : m.mentions) {
                Integer c = mcount.get(n);
                mcount.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
            for (String st : m.stars) {
                allStarred |= st.isEmpty();
                starred.add(st);
            }
        }
        for (Map.Entry<String, Module> e : p.modules.entrySet()) {
            Module m = e.getValue();
            for (Map.Entry<String, Integer> c : mcount.entrySet()) {
                if (c.getValue().intValue() > 1 || !m.mentions.contains(c.getKey())) {
                    m.mentioned.add(c.getKey());
                }
            }
            // A relative star import names its module by a suffix: every
            // module ending in it is taken to be the one.
            m.starImported = allStarred;
            for (String st : starred) {
                m.starImported |= e.getKey().equals(st) || e.getKey().endsWith("." + st);
            }
        }
        // A name is foreign to a module when a module other than it stores
        // it: stored by two modules, or by one that is not this one.
        for (Map.Entry<String, Module> e : p.modules.entrySet()) {
            Set<String> own = stores.get(e.getKey());
            for (Map.Entry<String, Integer> c : count.entrySet()) {
                if (c.getValue().intValue() > 1 || !own.contains(c.getKey())) {
                    e.getValue().foreign.add(c.getKey());
                }
            }
        }
        return p;
    }

    /// `self` is the receiver of the method `o` is in, or null.
    private static void scan(Object o, String self, Module m, Set<String> all) {
        if (o instanceof FunctionDef) {
            List<String> names = ((FunctionDef) o).args.names();
            String first = names.isEmpty() ? null : names.get(0);
            Infer.children(o, x -> scan(x, first, m, all));
            return;
        }
        if (o instanceof Lambda || o instanceof ClassDef) {
            Infer.children(o, x -> scan(x, null, m, all));
            return;
        }
        if (o instanceof Name && NAMESPACE.contains(((Name) o).id)) {
            m.namespace = true;
        }
        if (o instanceof Attribute) {
            m.mentions.add(((Attribute) o).attr);
            m.classValues |= "__class__".equals(((Attribute) o).attr);
            m.dictViews |= "__dict__".equals(((Attribute) o).attr);
        } else if (o instanceof Name) {
            m.classValues |= "classmethod".equals(((Name) o).id);
            m.dictViews |= "vars".equals(((Name) o).id);
        } else if (o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            for (Alias a : f.names) {
                if ("*".equals(a.name)) {
                    m.stars.add(f.module == null ? "" : f.module);
                } else {
                    m.mentions.add(a.name);
                }
            }
        }
        if (o instanceof Call) {
            Call c = (Call) o;
            String f = c.func instanceof Name ? ((Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            m.classValues |= "type".equals(f) && c.func instanceof Name && c.args.size() == 1;
            if (("getattr".equals(f) || "hasattr".equals(f) || "attrgetter".equals(f) || "methodcaller".equals(f)
                    || "__getattribute__".equals(f)) && !c.args.isEmpty()) {
                boolean literal = false;
                for (Expr a : c.args) {
                    if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
                        m.mentions.add(((Constant) a).text);
                        // A name read by a literal in this module may be
                        // one of its own methods.
                        m.mentioned.add(((Constant) a).text);
                        literal = true;
                    }
                }
                m.computedRead |= !literal && !"hasattr".equals(f);
            }
            if ("setattr".equals(f) || "delattr".equals(f) || "__setattr__".equals(f) || "__delattr__".equals(f)
                    || PATCHERS.contains(f)) {
                // The name is the argument before the value: the second of
                // setattr(o, n, v) and of object.__setattr__(o, n, v), the
                // first of o.__setattr__(n, v), and either of mock's
                // patch("mod.n") and patch.object(o, "n"). Every literal
                // among the arguments is taken; one too many costs a typed
                // field.
                for (Expr a : c.args) {
                    if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
                        // `patch("pkg.mod.name")` names its target by a path.
                        String n = ((Constant) a).text;
                        n = n.substring(n.lastIndexOf('.') + 1);
                        if (f.startsWith("del") || f.startsWith("__del")) {
                            m.deleted.add(n);
                        } else {
                            all.add(n);
                            m.foreign.add(n);
                        }
                    }
                }
            }
        }
        if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                if (t instanceof Attribute) {
                    m.deleted.add(((Attribute) t).attr);
                }
            }
        } else if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                target(t, self, m, all);
            }
        } else if (o instanceof AugAssign) {
            target(((AugAssign) o).target, self, m, all);
        } else if (o instanceof AnnAssign) {
            target(((AnnAssign) o).target, self, m, all);
        } else if (o instanceof For) {
            target(((For) o).target, self, m, all);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            target(((WithItem) o).vars, self, m, all);
        } else if (o instanceof Comp) {
            for (Comprehension g : ((Comp) o).generators) {
                target(g.target, self, m, all);
            }
        }
        Infer.children(o, x -> scan(x, self, m, all));
    }

    private static void target(Expr t, String self, Module m, Set<String> all) {
        if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            // A method storing on its own receiver stores on an instance
            // of its own class or of a subclass, and no compiled class has
            // a parent outside its module: that store is the class's own
            // business. (Called unbound on a stranger it is a computed
            // store like any other, checked where it happens.)
            if (self == null || !(a.value instanceof Name) || !((Name) a.value).id.equals(self)) {
                m.loose.add(a.attr);
                all.add(a.attr);
            }
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                target(e, self, m, all);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                target(e, self, m, all);
            }
        } else if (t instanceof Starred) {
            target(((Starred) t).value, self, m, all);
        }
    }
}
