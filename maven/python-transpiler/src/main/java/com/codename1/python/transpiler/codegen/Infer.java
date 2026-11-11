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
import com.codename1.python.transpiler.ast.Scope;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Whole-module type inference, for every module of every program.
///
/// The transpiler compiles the whole application, so what other modules
/// do to this one is known: `WholeProgram` hands in the attribute names
/// stored from outside and whether the module's namespace is read as a
/// dict, and those names are left untyped. Every other value that reaches
/// a parameter, a field, a list element or a global is one this pass
/// sees, and the types it gives are sound: typed code never needs to
/// check them again, and a function is entered typed after one check of
/// its arguments at the door.
///
/// It is a flow-insensitive fixpoint. Each round walks the whole module --
/// nested functions, lambdas and class bodies included, where names are
/// simply `DYN` -- joining the type of every stored value into where it is
/// stored: a local of a compiled function, a parameter at each call site
/// that names its callee statically, a field, the elements of the lists
/// created at an allocation site, a global. A value that reaches somewhere
/// the pass cannot follow escapes: a function or class used as a value gets
/// `DYN` parameters, and a list that escapes gets `DYN` elements.
final class Infer {
    /// A function the pass types: a compiled method, or a plain function at
    /// the top of the module with a stable name.
    static final class Fn {
        final FunctionDef def;
        /// The class, for a method.
        final Layouts.Cls cls;
        final List<String> names;
        final Ty[] params;
        final Map<String, Ty> locals = new LinkedHashMap<String, Ty>();
        Ty ret = Ty.BOT;
        boolean escapes;

        Fn(FunctionDef def, Layouts.Cls cls) {
            this.def = def;
            this.cls = cls;
            this.names = def.args.names();
            this.params = new Ty[names.size()];
            for (int i = 0; i < params.length; i++) {
                params[i] = Ty.BOT;
            }
        }

        Ty param(String n) {
            int i = names.indexOf(n);
            return i < 0 ? null : params[i];
        }
    }

    /// What every module's pass shares: one heap for the whole program. A
    /// field, a list site or a parameter has one type, whichever module
    /// stores into it or calls it, and the fixpoint is over all of them.
    /// One place where reached code reads or writes an attribute the build
    /// cannot name on an object it cannot tell the class of.
    static final class Problem {
        final String module;
        final int line;
        /// `function` or `Class.method`, or null at module level.
        final String function;
        final String what;
        /// Whether it is a store: that costs typed fields, where a read
        /// only makes more functions reachable.
        final boolean store;

        Problem(String module, int line, String function, String what) {
            this.module = module;
            this.line = line;
            this.function = function;
            this.what = what;
            this.store = !what.startsWith("reads");
        }
    }

    static final class Program {
        /// Dotted module name -> its view.
        final Map<String, Infer> modules = new LinkedHashMap<String, Infer>();
        final Map<FunctionDef, Fn> fns = new IdentityHashMap<FunctionDef, Fn>();
        /// The module each compiled class and each typed function is in.
        final Map<Layouts.Cls, Infer> home = new IdentityHashMap<Layouts.Cls, Infer>();
        final Map<Fn, Infer> fnHome = new IdentityHashMap<Fn, Infer>();
        /// Field name -> every compiled class of the program declaring it.
        final Map<String, List<Layouts.Cls>> fieldOwners = new HashMap<String, List<Layouts.Cls>>();
        final Map<Layouts.Cls, Map<String, Ty>> fields = new LinkedHashMap<Layouts.Cls, Map<String, Ty>>();
        final Map<Layouts.Cls, Set<String>> definite = new HashMap<Layouts.Cls, Set<String>>();
        final Set<Layouts.Cls> open = new HashSet<Layouts.Cls>();
        final Map<Expr, Integer> siteOf = new IdentityHashMap<Expr, Integer>();
        final List<Ty> siteElem = new ArrayList<Ty>();
        final List<Ty> siteKey = new ArrayList<Ty>();
        final Set<Integer> escapedSites = new HashSet<Integer>();
        final Map<Integer, Ty[]> siteSlots = new HashMap<Integer, Ty[]>();
        final Set<Integer> unslotted = new HashSet<Integer>();
        final Map<Call, Ty> siteRet = new HashMap<Call, Ty>();
        /// Calls of a known function that go through its door (`callFn`).
        final Set<Call> doors = new HashSet<Call>();
        final Map<FunctionDef, Fn> generic = new IdentityHashMap<FunctionDef, Fn>();
        /// The compiled functions something in the program reaches: by a
        /// direct call, as a value, or by a name the rest of the program
        /// or the runtime may look up. The body of any other is never
        /// walked -- what it would store or call does not happen -- and
        /// never emitted (see [Infer#removed]).
        final Set<FunctionDef> reached = java.util.Collections.newSetFromMap(new IdentityHashMap<FunctionDef, Boolean>());
        /// Every name the Java side of the program may look up in a
        /// module or on an instance: the identifiers among the strings of
        /// the runtime's classes. Null when the runtime was not at hand
        /// to read, and then every function counts as reached.
        Set<String> runtimeNames;
        /// Functions the build was told to keep although nothing in the
        /// Python sources reaches them: `module.function` and
        /// `module.Class.method`.
        final Set<String> keep = new HashSet<String>();
        /// What reached code does that the build cannot hold to the types
        /// it computed, by the place it happens: each is a build error
        /// unless the function is replaced (see [Problem]).
        final Map<Object, Problem> problems = new IdentityHashMap<Object, Problem>();
        /// The classes an instance of which is held somewhere as a value
        /// of no known class, with the classes related to them (see
        /// [Infer#loosen]); and the ones the first set was built from.
        final Set<Layouts.Cls> loose = java.util.Collections.newSetFromMap(new IdentityHashMap<Layouts.Cls, Boolean>());
        final Set<Layouts.Cls> looseRoots = java.util.Collections.newSetFromMap(new IdentityHashMap<Layouts.Cls, Boolean>());
        /// The modules held somewhere as a value, and whether reached code
        /// can get at any module (`sys.modules`).
        final Set<Infer> looseModules = java.util.Collections.newSetFromMap(new IdentityHashMap<Infer, Boolean>());
        boolean allModulesLoose;
        /// Whether reached code reads an attribute by a computed name from
        /// an object of no known class: a method of a loose class, or a
        /// function of a loose module, may be what it finds and calls.
        boolean anyRead;
        /// Whether reached code takes the class of an object of no known
        /// class (`type(x)`, `x.__class__`) to do more with than name or
        /// compare it: any loose class may then be constructed there.
        boolean anyClassOf;
        /// The scopes of methods of classes without a compiled layout:
        /// their first parameter is never a compiled instance.
        final Set<Scope> plainSelf = java.util.Collections.newSetFromMap(new IdentityHashMap<Scope, Boolean>());
        final Map<Scope, Set<String>> compCells = new IdentityHashMap<Scope, Set<String>>();
        boolean allListsEscape;
        boolean changed;
        /// Set once the fixpoint holds: the emitters then only read the
        /// result (see [Infer#escape]).
        boolean done;
        int rounds;

        /// Adds a module; every module is added before [#run].
        Infer add(String name, boolean pkg, List<Stmt> body, Layouts layouts, Scope moduleScope,
                WholeProgram.Module facts) {
            Infer in = new Infer(this, name, pkg, body, layouts, moduleScope, facts);
            modules.put(name, in);
            return in;
        }

        /// Runs the pass over every module, to the fixpoint. The types only
        /// ever widen and there are finitely many, so it ends; the bound is
        /// there for a fault in the pass, which is reported, never worked
        /// around by leaving the program untyped.
        void run() {
            for (Infer in : modules.values()) {
                in.collect(in.body);
            }
            for (Infer in : modules.values()) {
                in.bindImports();
            }
            for (rounds = 0; rounds < 2000; rounds++) {
                changed = false;
                for (Infer in : modules.values()) {
                    in.walkBlock(in.body, null, in.moduleScope);
                    in.unify();
                }
                if (!modules.isEmpty()) {
                    modules.values().iterator().next().sweep();
                }
                if (!changed) {
                    for (Infer in : modules.values()) {
                        in.uncalled();
                    }
                }
                if (!changed) {
                    done = true;
                    if (System.getProperty("cn1.python.dumpProblems") != null) {
                        for (Problem q : problems.values()) {
                            System.err.println("problem " + q.module + ":" + q.line + " " + q.function + ": " + q.what);
                        }
                        System.err.println("reached " + reached.size() + " of " + fns.size() + " functions; loose " + loose.size() + " of " + home.size() + " classes, " + looseModules.size() + " modules");
                    }
                    for (Infer in : modules.values()) {
                        in.localize();
                    }
                    return;
                }
            }
            throw new IllegalStateException("the Python type inference did not settle after " + rounds
                    + " rounds; this is a fault in the transpiler");
        }
    }

    final Program p;
    final String moduleName;
    /// Whether the module is a package's `__init__`.
    final boolean pkg;
    final List<Stmt> body;
    final Layouts layouts;
    final Scope moduleScope;
    /// This module's own typed functions; `p.fns` has every module's.
    final Map<FunctionDef, Fn> fns = new LinkedHashMap<FunctionDef, Fn>();
    /// Stable global name -> the function or class it is bound to, once.
    final Map<String, Fn> globalFns = new HashMap<String, Fn>();
    final Map<String, Layouts.Cls> globalClasses = new HashMap<String, Layouts.Cls>();
    /// The type a call of an expression function (see `exprBody`) has at
    /// that call site, from the argument types there; where it is narrower
    /// than the function's own return type, typed code relabels the result.
    final Map<Call, Ty> siteRet;
    final Set<Call> doors;
    /// Globals bound once, by `import math`: calls of its functions on
    /// numbers are typed (see `MATH_FNS`) and run as `PyMath` directly.
    final Set<String> mathNames = new HashSet<String>();
    /// The globals only `import struct` binds.
    final Set<String> structNames = new HashSet<String>();
    /// `math` functions typed code calls, with their arity.
    static final Map<String, Integer> MATH_FNS = new HashMap<String, Integer>();
    /// `math` constants, all floats.
    static final Set<String> MATH_CONSTS = new HashSet<String>();

    static {
        String[] unary = {"sqrt", "sin", "cos", "tan", "asin", "acos", "atan", "exp", "fabs", "log", "log10"};
        for (String f : unary) {
            MATH_FNS.put(f, Integer.valueOf(1));
        }
        MATH_FNS.put("fmod", Integer.valueOf(2));
        MATH_FNS.put("atan2", Integer.valueOf(2));
        for (String c : new String[] {"pi", "e", "tau", "inf", "nan"}) {
            MATH_CONSTS.add(c);
        }
    }
    final Map<String, Ty> globals = new HashMap<String, Ty>();
    /// Field name -> type, per declaring class.
    final Map<Layouts.Cls, Map<String, Ty>> fields;
    /// Fields every instance has set once its `__init__` returned.
    final Map<Layouts.Cls, Set<String>> definite;
    /// Classes something the pass cannot see may subclass or patch.
    final Set<Layouts.Cls> open;
    final Map<Expr, Integer> siteOf;
    final List<Ty> siteElem;
    /// The keys a dict site holds; `siteElem` holds its values.
    final List<Ty> siteKey;
    final Set<Integer> escapedSites;
    /// The type of each position of a tuple or list display, by allocation
    /// site (see [#slot]). A tuple's never change once made; a list's hold
    /// only while nothing reorders or resizes it -- anything but a store at
    /// a constant index puts its site in [#unslotted].
    final Map<Integer, Ty[]> siteSlots;
    final Set<Integer> unslotted;

    private final Map<String, Integer> moduleBindings = new HashMap<String, Integer>();

    private final WholeProgram.Module facts;

    private Infer(Program p, String moduleName, boolean pkg, List<Stmt> body, Layouts layouts,
            Scope moduleScope, WholeProgram.Module facts) {
        this.p = p;
        this.moduleName = moduleName;
        this.pkg = pkg;
        this.body = body;
        this.layouts = layouts;
        this.moduleScope = moduleScope;
        this.facts = facts;
        this.siteRet = p.siteRet;
        this.doors = p.doors;
        this.fields = p.fields;
        this.definite = p.definite;
        this.open = p.open;
        this.siteOf = p.siteOf;
        this.siteElem = p.siteElem;
        this.siteKey = p.siteKey;
        this.escapedSites = p.escapedSites;
        this.siteSlots = p.siteSlots;
        this.unslotted = p.unslotted;
        this.generic = p.generic;
        this.compCells = p.compCells;
    }

    /// Infers one module on its own: a program of that module alone.
    static Infer run(List<Stmt> body, Layouts layouts, Scope moduleScope, WholeProgram.Module facts) {
        Program p = new Program();
        Infer in = p.add("", false, body, layouts, moduleScope, facts);
        p.run();
        return in;
    }

    /// Gives a type to every parameter no call in the module reaches. The
    /// function is still called: from another module, or through a value.
    /// Those calls enter through the guard that checks every argument
    /// (TypedEmit.entry), which is what makes a parameter's type sound
    /// whoever calls -- but only a parameter that has a type is checked, so
    /// one left without would take whatever arrives as whatever its uses
    /// went on to assume.
    private void uncalled() {
        for (Fn f : fns.values()) {
            for (int i = f.cls != null ? 1 : 0; i < f.params.length; i++) {
                if (f.params[i].k == Ty.K.BOT) {
                    f.params[i] = joinInto(f.params[i], guess(f.def, f.names.get(i)));
                }
            }
        }
    }

    /// Set while a function body is walked as its generic version runs it
    /// (see [#genericView]).
    private boolean shadow;
    private boolean shadowMiss;
    private final Map<FunctionDef, Fn> generic;

    /// Whether `f` can be entered by a call this pass does not see: one
    /// through a value, or one from another module. Such a call goes
    /// through the guard at the door, and when its arguments are not what
    /// the direct calls pass, the generic version of the body runs.
    private boolean entered(Fn f) {
        if (f.escapes) {
            return true;
        }
        if (p.anyRead && (f.cls != null ? p.loose.contains(f.cls)
                : p.allModulesLoose || p.looseModules.contains(p.fnHome.get(f)))) {
            return true;
        }
        String n = f.def.name;
        if (f.cls == null) {
            return facts.reachable(n);
        }
        if ("__init__".equals(n)) {
            // Constructed by whoever can name the class or get at it.
            if (facts.classValues) {
                return true;
            }
            for (Layouts.Cls k : layouts.classes.values()) {
                if (isSelfOrAncestor(f.cls, k) && facts.reachable(k.def.name)) {
                    return true;
                }
            }
            return false;
        }
        // The runtime calls a special method itself, with anything.
        return n.startsWith("__") || facts.calledByName(n);
    }

    /// Walks the body of `f` as its generic version runs it: with
    /// parameters that hold anything. The typed version is sound whatever
    /// reaches the door, since the guard there sends what does not fit to
    /// the generic one -- which then stores those values into fields, lists
    /// and globals all the same. This walk is what puts them there.
    private void genericView(Fn f) {
        Fn g = generic.get(f.def);
        if (g == null) {
            g = new Fn(f.def, f.cls);
            for (int i = 0; i < g.params.length; i++) {
                g.params[i] = i == 0 && f.cls != null ? f.params[0] : Ty.DYN;
            }
            generic.put(f.def, g);
        }
        if (f.cls != null) {
            g.params[0] = f.params[0];
        }
        for (int i = 0; i < g.params.length; i++) {
            g.locals.put(g.names.get(i), mix(g.locals.get(g.names.get(i)), g.params[i]));
        }
        shadow = true;
        try {
            walkBlock(f.def.body, g, f.def.scope);
        } finally {
            shadow = false;
        }
    }

    // ---- other modules ----------------------------------------------------

    /// A global of another module, read through an import.
    static final class Extern {
        final Infer module;
        final String name;

        Extern(Infer module, String name) {
            this.module = module;
            this.name = name;
        }
    }

    /// Stable globals an `import` binds to a module of the program.
    private final Map<String, Infer> moduleNames = new HashMap<String, Infer>();
    /// Stable globals `from m import n` binds: the module, and the name there.
    private final Map<String, Extern> importedNames = new HashMap<String, Extern>();

    /// Records what the imports at the top of the module bind. A name bound
    /// once here, to a name bound once there, is that module's function,
    /// class or value for good: a call through it is a call of that
    /// function, as direct as one inside its own module.
    private void bindImports() {
        if (moduleScope.hasStarImport) {
            // A star import may bind any name again.
            return;
        }
        for (Stmt s : body) {
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    int dot = a.name.indexOf('.');
                    String n = a.asname != null ? a.asname : dot < 0 ? a.name : a.name.substring(0, dot);
                    Infer m = p.modules.get(a.asname != null || dot < 0 ? a.name : n);
                    if (m != null && stable(n)) {
                        moduleNames.put(n, m);
                    }
                }
            } else if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                String from = f.level == 0 ? f.module : relative(f.level, f.module);
                Infer m = from == null ? null : p.modules.get(from);
                if (m == null) {
                    continue;
                }
                for (Alias a : f.names) {
                    String n = a.asname != null ? a.asname : a.name;
                    if (!"*".equals(a.name) && stable(n)) {
                        importedNames.put(n, new Extern(m, a.name));
                    }
                }
            }
        }
    }

    /// The absolute name of `from <dots><module> import`, or null when it
    /// climbs out of the top-level package.
    private String relative(int level, String module) {
        String base = moduleName;
        int up = pkg ? level - 1 : level;
        for (int i = 0; i < up; i++) {
            int dot = base.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            base = base.substring(0, dot);
        }
        if (module == null) {
            return base;
        }
        return base.isEmpty() ? module : base + "." + module;
    }

    /// What stable global `n` of this module is: an `Fn`, a `Layouts.Cls`,
    /// an `Infer` (a module), a `Extern` (a value of another module's), or
    /// null for a value of this module's own or one nothing is known of.
    private Object resolve(String n, int depth) {
        Fn f = globalFns.get(n);
        if (f != null) {
            return f;
        }
        Layouts.Cls k = globalClasses.get(n);
        if (k != null) {
            return k;
        }
        Infer m = moduleNames.get(n);
        if (m != null) {
            return m;
        }
        Extern g = importedNames.get(n);
        return g == null || depth > 8 ? null : g.module.export(g.name, depth + 1);
    }

    /// What attribute `n` of this module is, to another module.
    private Object export(String n, int depth) {
        if (moduleScope.assigned.contains(n)) {
            if (!stable(n) || moduleScope.hasStarImport) {
                return null;
            }
            Object r = resolve(n, depth);
            return r != null ? r : new Extern(this, n);
        }
        // Not a global of the package: a module in it.
        return moduleScope.hasStarImport ? null : p.modules.get(moduleName.isEmpty() ? n : moduleName + "." + n);
    }

    /// What `e` names in another module, when `e` is an imported name or an
    /// attribute of an imported module; otherwise null.
    private Object foreign(Expr e, Scope sc) {
        if (e instanceof Name) {
            String n = ((Name) e).id;
            if (sc.resolve(n) != Scope.Binding.GLOBAL || !importedNames.containsKey(n) && !moduleNames.containsKey(n)) {
                return null;
            }
            return resolve(n, 0);
        }
        if (e instanceof Attribute) {
            Object o = foreign(((Attribute) e).value, sc);
            return o instanceof Infer ? ((Infer) o).export(((Attribute) e).attr, 0) : null;
        }
        return null;
    }

    /// `t` as this module's code holds it. An instance of a class another
    /// module compiled is held as any object: the typed code of a module
    /// names only its own classes. What the pass knows of such a value
    /// still counts where it is stored -- only the code here does not use it.
    private Ty loc(Ty t) {
        return p.done && t != null && t.k == Ty.K.INST && p.home.get(t.cls) != this ? Ty.DYN : t; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// Rewrites what this module's own code declares -- parameters, locals,
    /// results, fields, globals -- as that code holds it (see [#loc]).
    private void localize() {
        for (Fn f : fns.values()) {
            for (int i = 0; i < f.params.length; i++) {
                f.params[i] = loc(f.params[i]);
            }
            f.ret = loc(f.ret);
            for (Map.Entry<String, Ty> e : f.locals.entrySet()) {
                e.setValue(loc(e.getValue()));
            }
        }
        for (Layouts.Cls k : layouts.classes.values()) {
            for (Map.Entry<String, Ty> e : fields.get(k).entrySet()) {
                e.setValue(loc(e.getValue()));
            }
        }
        for (Map.Entry<String, Ty> e : globals.entrySet()) {
            e.setValue(loc(e.getValue()));
        }
    }

    // ---- setup -----------------------------------------------------------

    private void collect(List<Stmt> body) {
        countBindings(body);
        for (String n : moduleScope.assigned) {
            // A global another module stores (`mod.n = v`), or any global
            // of a module that hands its namespace out as a dict, is bound
            // more often than this module shows and holds anything.
            if (facts.namespace || facts.patched(n)) {
                moduleBindings.put(n, Integer.valueOf(99));
                globals.put(n, Ty.DYN);
            }
        }
        intConstants(body);
        for (Layouts.Cls k : layouts.classes.values()) {
            // A method name stored on a class or an instance somewhere is
            // looked up per call, as one the class's own methods store on
            // `self` is (Layouts.plan).
            for (java.util.Iterator<String> it = k.typable.keySet().iterator(); it.hasNext();) {
                if (facts.patched(it.next())) {
                    it.remove();
                }
            }
        }
        for (Layouts.Cls k : layouts.classes.values()) {
            fields.put(k, new LinkedHashMap<String, Ty>());
            p.home.put(k, this);
            for (String f : k.fields) {
                List<Layouts.Cls> o = p.fieldOwners.get(f);
                if (o == null) {
                    o = new ArrayList<Layouts.Cls>();
                    p.fieldOwners.put(f, o);
                }
                o.add(k);
            }
            for (String f : k.fields) {
                // A field stored from another module holds what that
                // module puts there, which this pass does not see.
                fields.get(k).put(f, facts.foreign(f) ? Ty.DYN : Ty.BOT);
            }
            for (FunctionDef f : k.typable.values()) {
                compCells.put(f.scope, compOnlyCells(f));
                Fn fn = new Fn(f, k);
                fn.params[0] = Ty.inst(k, false);
                fns.put(f, fn);
                p.fns.put(f, fn);
                p.fnHome.put(fn, this);
            }
        }
        for (Stmt s : body) {
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                Set<String> cc = compOnlyCells(f);
                if (stable(f.name) && f.decorators.isEmpty() && !f.isAsync && !f.scope.hasYield
                        && f.args.vararg == null && f.args.kwarg == null && f.args.kwonly.isEmpty()
                        && cc.size() == f.scope.cells.size() && f.args.names().size() <= 8) {
                    compCells.put(f.scope, cc);
                    Fn fn = new Fn(f, null);
                    fns.put(f, fn);
                    p.fns.put(f, fn);
                    p.fnHome.put(fn, this);
                    globalFns.put(f.name, fn);
                }
            } else if (s instanceof ClassDef) {
                Layouts.Cls k = layouts.classes.get(s);
                if (k != null && stable(((ClassDef) s).name)) {
                    globalClasses.put(((ClassDef) s).name, k);
                }
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    if ("math".equals(a.name) && stable(n)) {
                        mathNames.add(n);
                    }
                    if ("struct".equals(a.name) && stable(n)) {
                        structNames.add(n);
                    }
                }
            }
        }
        markOpen(body);
        for (Layouts.Cls k : layouts.classes.values()) {
            definite.put(k, initDefinite(k, new HashSet<Layouts.Cls>()));
        }
    }

    /// Whether `e` is the `struct` module, as `isMath` is for `math`.
    boolean isStruct(Expr e, Scope sc) {
        return e instanceof Name && structNames.contains(((Name) e).id)
                && sc.resolve(((Name) e).id) == Scope.Binding.GLOBAL;
    }

    /// Whether `e` is the `math` module, through a global only `import math`
    /// binds.
    boolean isMath(Expr e, Scope sc) {
        return e instanceof Name && mathNames.contains(((Name) e).id)
                && sc.resolve(((Name) e).id) == Scope.Binding.GLOBAL;
    }

    /// Whether `c` is a `math` function typed code runs, on the right number
    /// of plain positional arguments.
    boolean isMathCall(Call c, Scope sc) {
        if (!(c.func instanceof Attribute) || !c.keywords.isEmpty() || anyStarred(c.args)) {
            return false;
        }
        Attribute a = (Attribute) c.func;
        Integer n = MATH_FNS.get(a.attr);
        return n != null && n.intValue() == c.args.size() && isMath(a.value, sc);
    }

    /// The return type of expression function `f` called with `args`, or
    /// null. Helpers such as `def i32(v): return ((v + B) & M) - B` are
    /// shared by every caller, and one caller passing an untyped value
    /// makes the parameter, and so the result, untyped for all of them. The
    /// body is plain arithmetic on its parameters and global constants, so
    /// its type at one call follows from that call's arguments alone.
    private Ty siteType(Fn f, List<Ty> args) {
        Expr body = exprBody(f.def);
        if (body == null) {
            return null;
        }
        for (Ty t : args) {
            if (t.opt || t.k != Ty.K.INT && t.k != Ty.K.FLOAT && t.k != Ty.K.BOOL) {
                return null;
            }
        }
        Fn site = new Fn(f.def, null);
        for (int i = 0; i < args.size(); i++) {
            site.params[i] = args.get(i);
            site.locals.put(site.names.get(i), args.get(i));
        }
        Ty t = type(body, site, f.def.scope);
        return !t.opt && (t.k == Ty.K.INT || t.k == Ty.K.FLOAT || t.k == Ty.K.BOOL) ? t : null;
    }

    /// The returned expression of a function whose body is only `return
    /// <arithmetic>` (after an optional docstring), the arithmetic made of
    /// operators, constants, its parameters and global names; else null.
    static Expr exprBody(FunctionDef d) {
        List<Stmt> b = d.body;
        int i = 0;
        if (b.size() == 2 && b.get(0) instanceof ExprStmt && ((ExprStmt) b.get(0)).value instanceof Constant) {
            i = 1;
        }
        if (b.size() != i + 1 || !(b.get(i) instanceof Return) || ((Return) b.get(i)).value == null) {
            return null;
        }
        Expr e = ((Return) b.get(i)).value;
        return arithmetic(e, d.scope) ? e : null;
    }

    private static boolean arithmetic(Expr e, Scope sc) {
        if (e instanceof Constant) {
            return true;
        }
        if (e instanceof Name) {
            Scope.Binding r = sc.resolve(((Name) e).id);
            return r == Scope.Binding.GLOBAL || sc.params.contains(((Name) e).id);
        }
        if (e instanceof BinOp) {
            return arithmetic(((BinOp) e).left, sc) && arithmetic(((BinOp) e).right, sc);
        }
        if (e instanceof UnaryOp) {
            return arithmetic(((UnaryOp) e).operand, sc);
        }
        return false;
    }

    /// Module globals bound once, at the top level, to an int literal (`M32 =
    /// 0xFFFFFFFF`): their value is known wherever a function reads them.
    final Map<String, java.math.BigInteger> intConsts = new HashMap<String, java.math.BigInteger>();

    private void intConstants(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1
                    && ((Assign) s).targets.get(0) instanceof Name) {
                String n = ((Name) ((Assign) s).targets.get(0)).id;
                java.math.BigInteger v = IntRanges.literal(((Assign) s).value);
                if (v != null && stable(n)) {
                    intConsts.put(n, v);
                }
            }
        }
    }

    private boolean stable(String n) {
        Integer c = moduleBindings.get(n);
        return c != null && c.intValue() == 1;
    }

    /// Counts the bindings of each global at the top level, and every
    /// `global` declaration in a function as one more (such a store is
    /// invisible to a module-level count).
    private void countBindings(List<Stmt> body) {
        for (Stmt s : body) {
            List<String> names = new ArrayList<String>();
            if (s instanceof FunctionDef) {
                names.add(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                names.add(((ClassDef) s).name);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    targetNames(t, names);
                }
            } else if (s instanceof AugAssign) {
                targetNames(((AugAssign) s).target, names);
                targetNames(((AugAssign) s).target, names);
            } else if (s instanceof AnnAssign) {
                targetNames(((AnnAssign) s).target, names);
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    int dot = n.indexOf('.');
                    names.add(dot < 0 ? n : n.substring(0, dot));
                }
            } else if (s instanceof ImportFrom) {
                for (Alias a : ((ImportFrom) s).names) {
                    names.add(a.asname != null ? a.asname : a.name);
                }
            } else {
                // A compound statement: everything it binds counts twice,
                // since it may run any number of times.
                for (String n : moduleScope.assigned) {
                    if (binds(s, n)) {
                        names.add(n);
                        names.add(n);
                    }
                }
            }
            for (String n : names) {
                Integer c = moduleBindings.get(n);
                moduleBindings.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
        }
        globalDecls(body);
    }

    private void globalDecls(Object o) {
        if (o instanceof Global && !((Global) o).nonlocal) {
            for (String n : ((Global) o).names) {
                moduleBindings.put(n, Integer.valueOf(99));
            }
        }
        children(o, x -> globalDecls(x));
    }

    private static boolean binds(Stmt s, String n) {
        List<String> out = new ArrayList<String>();
        boundIn(s, out);
        return out.contains(n);
    }

    /// The names a statement stores into, outside nested scopes.
    private static void boundIn(Object o, List<String> out) {
        if (o instanceof Lambda || o instanceof Comp) {
            return;
        }
        if (o instanceof FunctionDef) {
            out.add(((FunctionDef) o).name);
            return;
        }
        if (o instanceof ClassDef) {
            out.add(((ClassDef) o).name);
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                targetNames(t, out);
            }
        } else if (o instanceof AugAssign) {
            targetNames(((AugAssign) o).target, out);
        } else if (o instanceof AnnAssign) {
            targetNames(((AnnAssign) o).target, out);
        } else if (o instanceof For) {
            targetNames(((For) o).target, out);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            targetNames(((WithItem) o).vars, out);
        } else if (o instanceof NamedExpr) {
            out.add(((NamedExpr) o).target.id);
        } else if (o instanceof ExceptHandler && ((ExceptHandler) o).name != null) {
            out.add(((ExceptHandler) o).name);
        } else if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                String n = a.asname != null ? a.asname : a.name;
                int dot = n.indexOf('.');
                out.add(dot < 0 ? n : n.substring(0, dot));
            }
        } else if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                out.add(a.asname != null ? a.asname : a.name);
            }
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                targetNames(t, out);
            }
        }
        children(o, x -> boundIn(x, out));
    }

    private static void targetNames(Expr t, List<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                targetNames(e, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                targetNames(e, out);
            }
        } else if (t instanceof Starred) {
            targetNames(((Starred) t).value, out);
        }
    }

    /// A class some class the layout planner rejected derives from: its
    /// instances may run methods this pass does not see, so neither it nor
    /// its ancestors are typed.
    private void markOpen(Object o) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            if (!layouts.classes.containsKey(c)) {
                for (Expr b : c.bases) {
                    if (b instanceof Name) {
                        Layouts.Cls k = byName(((Name) b).id);
                        for (; k != null; k = k.parent) {
                            open.add(k);
                        }
                    }
                }
            }
        }
        children(o, x -> markOpen(x));
    }

    private Layouts.Cls byName(String n) {
        for (Layouts.Cls k : layouts.classes.values()) {
            if (k.def.name.equals(n)) {
                return k;
            }
        }
        return null;
    }

    // ---- definite assignment of fields ------------------------------------

    /// In the answer of `initDefinite`: the `__init__` went on to store an
    /// attribute outside the fields. Not a name an attribute can have.
    private static final String EXTRA = "<extra>";

    private Set<String> initDefinite(Layouts.Cls k, Set<Layouts.Cls> seen) {
        Set<String> r = new HashSet<String>();
        Layouts.Cls owner = Layouts.resolver(k, "__init__");
        if (owner == null || !seen.add(owner)) {
            return r;
        }
        FunctionDef f = owner.typable.get("__init__");
        if (f == null || f.args.names().isEmpty()) {
            return r;
        }
        String self = f.args.names().get(0);
        if (f.scope.assigned.contains(self)) {
            return r;
        }
        for (Stmt s : f.body) {
            if (s instanceof Pass || s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant) {
                continue;
            }
            if (s instanceof Assign && ((Assign) s).targets.size() == 1) {
                Assign a = (Assign) s;
                Expr t = a.targets.get(0);
                if (t instanceof Attribute && ((Attribute) t).value instanceof Name
                        && ((Name) ((Attribute) t).value).id.equals(self) && !mentions(a.value, self)) {
                    if (!k.allFields.contains(((Attribute) t).attr)) {
                        // An attribute kept outside the fields. From here
                        // on the instance lists its attributes in the
                        // order they were set, and a field first set after
                        // this one is kept with them, not in the Java
                        // field (PyInstance.store): typed code, which reads
                        // the field without asking, must not count on it.
                        r.add(EXTRA);
                        break;
                    }
                    r.add(((Attribute) t).attr);
                    continue;
                }
            }
            if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Call) {
                Call c = (Call) ((ExprStmt) s).value;
                if (c.func instanceof Attribute && "__init__".equals(((Attribute) c.func).attr)
                        && ((Attribute) c.func).value instanceof Name && c.keywords.isEmpty() && !c.args.isEmpty()
                        && c.args.get(0) instanceof Name && ((Name) c.args.get(0)).id.equals(self)) {
                    Layouts.Cls base = globalClasses.get(((Name) ((Attribute) c.func).value).id);
                    boolean clean = base != null && isAncestor(base, owner);
                    for (int i = 1; i < c.args.size() && clean; i++) {
                        clean = !mentions(c.args.get(i), self);
                    }
                    if (clean) {
                        Set<String> up = initDefinite(base, seen);
                        r.addAll(up);
                        if (up.contains(EXTRA)) {
                            break;
                        }
                        continue;
                    }
                }
            }
            if (mentions(s, self)) {
                break;
            }
        }
        return r;
    }

    private static boolean isAncestor(Layouts.Cls a, Layouts.Cls k) {
        for (Layouts.Cls x = k.parent; x != null; x = x.parent) {
            if (x == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    static boolean mentions(Object o, String n) {
        boolean[] r = new boolean[1];
        mentionsIn(o, n, r);
        return r[0];
    }

    private static void mentionsIn(Object o, String n, boolean[] r) {
        if (r[0]) {
            return;
        }
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            r[0] = true;
            return;
        }
        children(o, x -> mentionsIn(x, n, r));
    }

    /// Whether field `f` of class `k` (declared there or inherited) is set
    /// on every instance of `k` and of all its subclasses.
    boolean definitelySet(Layouts.Cls owner, String f) {
        Infer h = p.home.get(owner);
        if (h.facts.dictViews || h.facts.deleted.contains(f)) {
            return false;
        }
        for (Layouts.Cls k : h.layouts.classes.values()) {
            if (isSelfOrAncestor(owner, k) && !definite.get(k).contains(f)) {
                return false;
            }
        }
        return true;
    }

    static boolean isSelfOrAncestor(Layouts.Cls a, Layouts.Cls k) {
        for (Layouts.Cls x = k; x != null; x = x.parent) {
            if (x == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    // ---- joins -----------------------------------------------------------

    private Ty joinInto(Ty cur, Ty v) {
        Ty r = mix(cur, v);
        if (!r.equals(cur)) {
            p.changed = true;
        }
        if (r.k != Ty.K.LIST && r.k != Ty.K.TUPLE && r.k != Ty.K.DICT && r.k != Ty.K.INST) {
            escape(v);
        }
        return r;
    }

    /// The join of two types. An instance that joins into a value of no
    /// one class is from then on held where the pass does not follow it.
    private Ty mix(Ty a, Ty b) {
        Ty r = Ty.join(a, b);
        if (r.k == Ty.K.DYN && !p.done) {
            if (a != null && a.k == Ty.K.INST) {
                loosen(a.cls);
            }
            if (b != null && b.k == Ty.K.INST) {
                loosen(b.cls);
            }
        }
        return r;
    }

    /// An instance of `k` reached code that holds it as a value of no
    /// known class. What such code does to an object it cannot name the
    /// class of -- read or store an attribute by a computed name -- it may
    /// do to this one: to a field or a method of `k`, of a class it
    /// derives from, or of one that derives from it.
    /// What an escaped list or dict holds is read back as a value of no
    /// known class.
    private void sweep() {
        if (p.anyClassOf) {
            for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.loose)) {
                Infer in = p.home.get(k);
                if (in != null) {
                    in.escapeClass(k);
                }
            }
        }
        if (p.anyRead) {
            // A computed read from a module held as a value finds its
            // classes and what its globals hold, not only its functions.
            for (Map.Entry<Layouts.Cls, Infer> e : p.home.entrySet()) {
                if (p.allModulesLoose || p.looseModules.contains(e.getValue())) {
                    e.getValue().escapeClass(e.getKey());
                }
            }
            for (Infer in : p.modules.values()) {
                if (p.allModulesLoose || p.looseModules.contains(in)) {
                    for (Ty g : in.globals.values()) {
                        in.escape(g);
                    }
                }
            }
        }
        for (int s = 0; s < siteElem.size(); s++) {
            if (p.allListsEscape || escapedSites.contains(Integer.valueOf(s))) {
                Ty e = siteElem.get(s);
                if (e != null && e.k == Ty.K.INST) {
                    loosen(e.cls);
                }
                Ty k = s < siteKey.size() ? siteKey.get(s) : null;
                if (k != null && k.k == Ty.K.INST) {
                    loosen(k.cls);
                }
            }
        }
    }

    private void loosen(Layouts.Cls k) {
        if (k == null || !p.looseRoots.add(k)) {
            return;
        }
        for (Layouts.Cls o : p.home.keySet()) {
            if ((isSelfOrAncestor(o, k) || isSelfOrAncestor(k, o)) && p.loose.add(o)) {
                p.changed = true;
            }
        }
    }

    /// A value went somewhere the pass does not follow.
    void escape(Ty v) {
        if (p.done) {
            return;
        }
        if (v != null && v.k == Ty.K.INST) {
            loosen(v.cls);
            return;
        }
        if (v != null && v.k == Ty.K.TUPLE) {
            // The tuple itself cannot change; what it holds now goes where
            // the pass does not follow.
            for (int s : v.sites) {
                if (escapedSites.add(Integer.valueOf(s))) {
                    p.changed = true;
                    Ty[] sl = siteSlots.get(Integer.valueOf(s));
                    if (sl != null) {
                        for (Ty x : sl) {
                            escape(x);
                        }
                    }
                }
            }
            return;
        }
        if (v != null && (v.k == Ty.K.LIST || v.k == Ty.K.DICT)) {
            if (v.sites.length == 0) {
                if (!p.allListsEscape) {
                    p.allListsEscape = true;
                    p.changed = true;
                }
            } else {
                for (int s : v.sites) {
                    if (escapedSites.add(Integer.valueOf(s))) {
                        p.changed = true;
                        Ty[] sl = siteSlots.get(Integer.valueOf(s));
                        if (sl != null) {
                            for (Ty x : sl) {
                                escape(x);
                            }
                        }
                    }
                }
            }
        }
    }

    // ---- attributes named at run time -----------------------------------

    private void problem(Node at, Fn fn, Scope sc, String what) {
        if (p.problems.containsKey(at)) {
            return;
        }
        String q = null;
        if (fn != null) {
            q = (fn.cls != null ? fn.cls.def.name + "." : "") + fn.def.name;
        } else if (sc != null && sc.kind != Scope.Kind.MODULE) {
            q = (sc.enclosingClass != null && sc.enclosingClass.name != null ? sc.enclosingClass.name + "." : "") + sc.name;
        }
        p.problems.put(at, new Problem(moduleName, at.line, q, what));
    }

    /// Whether `e` is the receiver of a method of a class without a
    /// compiled layout: an instance of such a class has no typed field.
    private boolean plainReceiver(Expr e, Scope sc) {
        if (!(e instanceof Name) || !p.plainSelf.contains(sc) || sc.params.isEmpty()) {
            return false;
        }
        return sc.params.iterator().next().equals(((Name) e).id) && sc.resolve(((Name) e).id) == Scope.Binding.LOCAL;
    }

    /// The start every value of the str `e` has, when `e` is built on a
    /// literal: `"visit_" + n`, an f-string, `"do_%s" % n`. Null otherwise,
    /// and for an empty start.
    private static String prefix(Expr e) {
        String r = null;
        if (e instanceof BinOp && ("+".equals(((BinOp) e).op) || "%".equals(((BinOp) e).op))
                && ((BinOp) e).left instanceof Constant && ((Constant) ((BinOp) e).left).kind == Constant.Kind.STR) {
            r = ((Constant) ((BinOp) e).left).text;
            if ("%".equals(((BinOp) e).op)) {
                int i = r.indexOf('%');
                r = i < 0 ? r : r.substring(0, i);
            }
        } else if (e instanceof JoinedStr && !((JoinedStr) e).parts.isEmpty()
                && ((JoinedStr) e).parts.get(0) instanceof Constant) {
            r = ((Constant) ((JoinedStr) e).parts.get(0)).text;
        }
        return r == null || r.isEmpty() ? null : r;
    }

    private static final Set<String> DICT_READS = new HashSet<String>(java.util.Arrays.asList("items", "keys",
            "values", "get", "copy", "__contains__", "__len__", "__iter__", "__getitem__"));
    private static final Set<String> CLASS_FACTS = new HashSet<String>(java.util.Arrays.asList("__name__",
            "__qualname__", "__module__", "__doc__"));

    /// `x` when `e` is the attribute dict of `x` -- `x.__dict__` or
    /// `vars(x)` -- and null otherwise. A store through it is a store of an
    /// attribute by a computed name.
    private Expr dictOwner(Expr e, Scope sc) {
        if (e instanceof Attribute && "__dict__".equals(((Attribute) e).attr)) {
            return ((Attribute) e).value;
        }
        if (e instanceof Call && ((Call) e).func instanceof Name && "vars".equals(((Name) ((Call) e).func).id)
                && ((Call) e).args.size() == 1 && ((Call) e).keywords.isEmpty()
                && !(((Call) e).args.get(0) instanceof Starred)
                && sc.resolve("vars") == Scope.Binding.BUILTIN && !moduleScope.hasStarImport) {
            return ((Call) e).args.get(0);
        }
        return null;
    }

    /// `x` when `e` is the class of `x`, `type(x)` or `x.__class__`.
    private Expr classOwner(Expr e, Scope sc) {
        if (e instanceof Attribute && "__class__".equals(((Attribute) e).attr)) {
            return ((Attribute) e).value;
        }
        if (e instanceof Call && ((Call) e).func instanceof Name && "type".equals(((Name) ((Call) e).func).id)
                && ((Call) e).args.size() == 1 && ((Call) e).keywords.isEmpty()
                && !(((Call) e).args.get(0) instanceof Starred)
                && sc.resolve("type") == Scope.Binding.BUILTIN && !moduleScope.hasStarImport) {
            return ((Call) e).args.get(0);
        }
        return null;
    }

    private static boolean classOp(String op) {
        return "is".equals(op) || "is not".equals(op) || "==".equals(op) || "!=".equals(op) || "in".equals(op)
                || "not in".equals(op);
    }

    /// The uses of a class taken off an object that make nothing with it
    /// -- its name, and a comparison -- and the comparisons of an attribute
    /// dict, which only read it.
    private final Set<Node> classFacts = java.util.Collections.newSetFromMap(new IdentityHashMap<Node, Boolean>());

    /// The class of an object, held as a value: whoever holds it may make
    /// instances of it, with arguments nobody tracks. `owner` is the object.
    private void classOf(Expr at, Expr owner, Fn fn, Scope sc) {
        if (classFacts.contains(at) || plainReceiver(owner, sc)) {
            return;
        }
        Ty r = type(owner, fn, sc);
        if (r.k == Ty.K.INST) {
            for (Map.Entry<Layouts.Cls, Infer> e : new ArrayList<Map.Entry<Layouts.Cls, Infer>>(p.home.entrySet())) {
                if (isSelfOrAncestor(e.getKey(), r.cls) || isSelfOrAncestor(r.cls, e.getKey())) {
                    e.getValue().escapeClass(e.getKey());
                }
            }
        } else if (r.k == Ty.K.DYN && !p.anyClassOf) {
            // Whatever is loose by the end: the sweep asks again each round.
            p.anyClassOf = true;
            p.changed = true;
        }
    }

    /// The attribute dict of `owner` used for anything but a read: handed
    /// on, it may be written under any name.
    private void dictOf(Expr at, Expr owner, Fn fn, Scope sc) {
        if (classFacts.contains(at)) {
            return;
        }
        if (owner instanceof Name && globalClasses.containsKey(((Name) owner).id)
                && sc.resolve(((Name) owner).id) == Scope.Binding.GLOBAL) {
            // A class's own dict is read-only, and holds no instance's
            // attributes.
            return;
        }
        computedStore(type(owner, fn, sc), owner, null, Ty.DYN, at, fn, sc,
                "hands on the attribute dict of an object of no class the build can tell, through which any"
                + " attribute may be set");
    }

    /// `setattr(recv, name, v)` and its other spellings. A literal name is
    /// a store like `recv.name = v`. A computed one on an instance of a
    /// known class may be any field of that class or of one related to it.
    /// On an object of no known class it could be any field of the
    /// program, and typing them all for that would type none: that is a
    /// problem the build reports.
    private void computedStore(Ty recv, Expr recvExpr, Expr name, Ty v, Node at, Fn fn, Scope sc) {
        computedStore(recv, recvExpr, name, v, at, fn, sc,
                "stores an attribute whose name is computed, on an object of no class the build can tell");
    }

    private void computedStore(Ty recv, Expr recvExpr, Expr name, Ty v, Node at, Fn fn, Scope sc, String what) {
        escape(v);
        if (recv.k != Ty.K.INST && recv.k != Ty.K.DYN || plainReceiver(recvExpr, sc)) {
            return;
        }
        if (name instanceof Constant && ((Constant) name).kind == Constant.Kind.STR) {
            storeField(recv, ((Constant) name).text, v);
            return;
        }
        String pre = name == null ? null : prefix(name);
        {
            boolean cost = false;
            for (Map.Entry<Layouts.Cls, Map<String, Ty>> e : fields.entrySet()) {
                Layouts.Cls o = e.getKey();
                if (recv.k == Ty.K.INST ? !isSelfOrAncestor(o, recv.cls) && !isSelfOrAncestor(recv.cls, o)
                        : !p.loose.contains(o)) {
                    continue;
                }
                cost |= recv.k != Ty.K.INST && pre == null && !e.getValue().isEmpty();
                for (Map.Entry<String, Ty> f : e.getValue().entrySet()) {
                    if (pre == null || f.getKey().startsWith(pre)) {
                        f.setValue(joinInto(f.getValue(), v));
                    }
                }
            }
            if (cost) {
                problem(at, fn, sc, what);
            }
        }
    }

    /// `getattr(recv, name)` with a computed name: what it finds may be
    /// called, with anything. On an instance of a known class that is a
    /// method of the class or of one related to it; with a name built on a
    /// literal start, whatever is named so. Anything else could be every
    /// function of the program.
    private void computedRead(Ty recv, Expr recvExpr, Expr name, Node at, Fn fn, Scope sc) {
        if (recv.k != Ty.K.INST && recv.k != Ty.K.DYN || plainReceiver(recvExpr, sc)) {
            return;
        }
        String pre = prefix(name);
        if (recv.k != Ty.K.INST && pre == null) {
            // Whatever is loose by the end: `entered` asks again each round.
            if (!p.anyRead) {
                p.anyRead = true;
                p.changed = true;
            }
            problem(at, fn, sc, "reads an attribute whose name is computed, on an object of no class the build can tell");
            return;
        }
        for (Fn f : new ArrayList<Fn>(p.fns.values())) {
            if (pre != null && !f.def.name.startsWith(pre)) {
                continue;
            }
            if (recv.k == Ty.K.INST && (f.cls == null || !isSelfOrAncestor(f.cls, recv.cls) && !isSelfOrAncestor(recv.cls, f.cls))) {
                continue;
            }
            escapeFn(f);
        }
    }

    private static boolean plainMethod(FunctionDef m) {
        for (Expr d : m.decorators) {
            if (d instanceof Name && "staticmethod".equals(((Name) d).id)) {
                return false;
            }
        }
        return true;
    }

    private void reach(Fn f) {
        if (p.reached.add(f.def)) {
            p.changed = true;
        }
    }

    /// Whether `f` is reached by something this pass cannot follow to it:
    /// a name another module or the Java runtime may look up, or the
    /// build's own list.
    private boolean rooted(Fn f) {
        if (p.runtimeNames == null || entered(f) || p.runtimeNames.contains(f.def.name)) {
            return true;
        }
        return p.keep.contains(moduleName + "." + (f.cls == null ? "" : f.cls.def.name + ".") + f.def.name);
    }

    /// Whether `d` is a compiled function nothing in the program reaches.
    /// Its body is not emitted: the function raises, naming itself, if a
    /// path this pass has no way to see calls it after all.
    boolean removed(FunctionDef d) {
        return p.done && p.fns.containsKey(d) && !p.reached.contains(d);
    }

    private void escapeFn(Fn f) {
        if (f != null && !f.escapes) {
            f.escapes = true;
            p.changed = true;
        }
        if (f != null) {
            reach(f);
        }
        if (f != null) {
            // Callers through a value are unknown, but the typed version
            // is entered from the generic one only through a guard that
            // checks every argument (TypedEmit.entry): a parameter keeps
            // what its direct callers pass, and one nobody calls directly
            // takes the type its uses suggest. A wrong guess costs the
            // guard, never correctness.
            for (int i = f.cls != null ? 1 : 0; i < f.params.length; i++) {
                if (f.params[i].k == Ty.K.BOT) {
                    f.params[i] = joinInto(f.params[i], guess(f.def, f.names.get(i)));
                }
            }
        }
    }

    /// The type the uses of parameter `p` in `def` suggest: INT when it is
    /// a `range` bound, a list repeat count or an operand next to an int
    /// constant; FLOAT next to a float constant; DYN when the uses say
    /// nothing or disagree.
    static Ty guess(FunctionDef def, String p) {
        Ty[] g = {null};
        boolean[] bad = {false};
        for (Stmt s : def.body) {
            guessIn(s, p, g, bad);
        }
        return bad[0] || g[0] == null ? Ty.DYN : g[0];
    }

    private static void guessIn(Object o, String p, Ty[] g, boolean[] bad) {
        if (o instanceof Call && ((Call) o).func instanceof Name && "range".equals(((Name) ((Call) o).func).id)) {
            for (Expr a : ((Call) o).args) {
                if (isName(a, p)) {
                    vote(Ty.INT, g, bad);
                }
            }
        } else if (o instanceof BinOp) {
            BinOp b = (BinOp) o;
            if ("*".equals(b.op) && (b.left instanceof ListExpr && isName(b.right, p))) {
                vote(Ty.INT, g, bad);
            } else if (isName(b.left, p) || isName(b.right, p)) {
                Ty c = constType(isName(b.left, p) ? b.right : b.left);
                if (c != null) {
                    vote(c, g, bad);
                }
            }
        } else if (o instanceof Compare && ((Compare) o).ops.size() == 1) {
            Compare c = (Compare) o;
            Expr r = c.comparators.get(0);
            if (isName(c.left, p) || isName(r, p)) {
                Ty t = constType(isName(c.left, p) ? r : c.left);
                if (t != null) {
                    vote(t, g, bad);
                }
            }
        } else if (o instanceof Assign) {
            // Rebinding the parameter: its later uses say nothing of the
            // argument.
            for (Expr t : ((Assign) o).targets) {
                if (isName(t, p)) {
                    bad[0] = true;
                }
            }
        }
        Infer.children(o, x -> guessIn(x, p, g, bad));
    }

    private static boolean isName(Expr e, String p) {
        return e instanceof Name && ((Name) e).id.equals(p);
    }

    private static Ty constType(Expr e) {
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            if (c.kind == Constant.Kind.INT) {
                return Ty.INT;
            }
            if (c.kind == Constant.Kind.FLOAT && !c.text.matches(".*[jJ]")) {
                return Ty.FLOAT;
            }
        }
        return null;
    }

    private static void vote(Ty t, Ty[] g, boolean[] bad) {
        if (g[0] != null && !g[0].equals(t)) {
            bad[0] = true;
        }
        g[0] = t;
    }

    private void escapeClass(Layouts.Cls k) {
        // Whoever holds the class makes instances of it.
        loosen(k);
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        if (o != null && o.typable.get("__init__") != null) {
            escapeFn(p.fns.get(o.typable.get("__init__")));
        }
    }

    /// Every compiled method `name` may be reached with arguments the pass
    /// cannot type.
    private void escapeMethods(String name) {
        for (Fn f : p.fns.values()) {
            if (f.cls != null && f.def.name.equals(name)) {
                escapeFn(f);
            }
        }
    }

    /// Set once the fixpoint holds: the emitters then only read the result.
    /// An escape from a read of theirs would change types the pass already
    /// handed out, after the code using them was decided.
    Ty elem(Ty list) {
        if (list.k != Ty.K.LIST || p.allListsEscape || list.sites.length == 0) {
            return Ty.DYN;
        }
        Ty r = Ty.BOT;
        for (int s : list.sites) {
            if (escapedSites.contains(Integer.valueOf(s))) {
                return Ty.DYN;
            }
            r = mix(r, siteElem.get(s));
        }
        r = loc(r);
        if (r.k == Ty.K.DYN && !p.done) {
            // A display's mixed positions read as one dynamic element: what
            // they hold is no longer followed.
            for (int s : list.sites) {
                Ty[] sl = siteSlots.get(Integer.valueOf(s));
                if (sl != null) {
                    for (Ty x : sl) {
                        escape(x);
                    }
                }
            }
        }
        return r;
    }

    /// `d[k] = v` on a dict of these sites.
    private void storeDict(Ty d, Ty k, Ty v) {
        if (d.sites.length == 0) {
            escape(d);
            escape(k);
            escape(v);
            return;
        }
        for (int s : d.sites) {
            siteKey.set(s, joinInto(siteKey.get(s), k));
            siteElem.set(s, joinInto(siteElem.get(s), v));
        }
    }

    /// What a read of a dict of these sites answers.
    Ty dictVal(Ty d) {
        if (d.k != Ty.K.DICT || p.allListsEscape || d.sites.length == 0) {
            return Ty.DYN;
        }
        Ty r = Ty.BOT;
        for (int s : d.sites) {
            if (escapedSites.contains(Integer.valueOf(s))) {
                return Ty.DYN;
            }
            r = mix(r, siteElem.get(s));
        }
        return loc(r);
    }

    /// The dict methods that keep the values followed, or null for any
    /// other, which lets the dict go.
    private Ty dictMethod(Ty d, String m, List<Ty> args) {
        switch (m) {
            case "get":
                if (args.size() == 1 || args.size() == 2) {
                    escape(args.get(0));
                    Ty dflt = args.size() == 2 ? args.get(1) : Ty.NONE;
                    Ty r = mix(dictVal(d), dflt);
                    if (r.k == Ty.K.DYN) {
                        escape(dflt);
                    }
                    return r;
                }
                return null;
            case "setdefault":
                if (args.size() == 2) {
                    storeDict(d, args.get(0), args.get(1));
                    return dictVal(d);
                }
                return null;
            default:
                return null;
        }
    }

    private void storeElem(Ty list, Ty v) {
        if (list.k != Ty.K.LIST || list.sites.length == 0) {
            escape(v);
            return;
        }
        unslot(list);
        for (int s : list.sites) {
            siteElem.set(s, joinInto(siteElem.get(s), v));
        }
    }

    /// The list may since have been reordered or resized: its positions no
    /// longer say what is where.
    private void unslot(Ty list) {
        if (list.k != Ty.K.LIST) {
            return;
        }
        for (int s : list.sites) {
            if (unslotted.add(Integer.valueOf(s))) {
                p.changed = true;
            }
        }
    }

    /// A display's position `i` takes `v`.
    private void joinSlot(int site, int n, int i, Ty v, boolean tuple) {
        Integer key = Integer.valueOf(site);
        Ty[] sl = siteSlots.get(key);
        if (sl == null || sl.length != n) {
            if (sl != null) {
                unslotted.add(key);
            }
            sl = new Ty[n];
            java.util.Arrays.fill(sl, Ty.BOT);
            siteSlots.put(key, sl);
        }
        Ty r = mix(sl[i], v);
        if (!r.equals(sl[i])) {
            sl[i] = r;
            p.changed = true;
        }
        if (tuple ? escapedSites.contains(key) : r.k != Ty.K.LIST && r.k != Ty.K.TUPLE) {
            escape(v);
        }
    }

    /// What position `i` of a tuple or list holds; for a list whose
    /// positions are not known, its elements.
    Ty slot(Ty t, int i) {
        if (t.k == Ty.K.LIST && slotCount(t) <= i) {
            return elem(t);
        }
        if (t.k != Ty.K.TUPLE && t.k != Ty.K.LIST || t.sites.length == 0 || i < 0) {
            return Ty.DYN;
        }
        Ty r = Ty.BOT;
        for (int s : t.sites) {
            Ty[] sl = siteSlots.get(Integer.valueOf(s));
            if (sl == null || i >= sl.length) {
                return t.k == Ty.K.LIST ? elem(t) : Ty.DYN;
            }
            r = mix(r, sl[i]);
        }
        return loc(r);
    }

    /// How many positions every site of `t` has, when they agree and are
    /// known; otherwise -1.
    int slotCount(Ty t) {
        if (t.k != Ty.K.TUPLE && t.k != Ty.K.LIST || t.sites.length == 0
                || t.k == Ty.K.LIST && p.allListsEscape) {
            return -1;
        }
        int n = -1;
        for (int s : t.sites) {
            Integer key = Integer.valueOf(s);
            Ty[] sl = siteSlots.get(key);
            if (sl == null || t.k == Ty.K.LIST && (unslotted.contains(key) || escapedSites.contains(key))
                    || n >= 0 && n != sl.length) {
                return -1;
            }
            n = sl.length;
        }
        return n;
    }

    /// Every position of a tuple, joined: what iterating it or a variable
    /// index yields.
    Ty tupleElem(Ty t) {
        int n = slotCount(t);
        if (n < 0) {
            return Ty.DYN;
        }
        Ty r = Ty.BOT;
        for (int i = 0; i < n; i++) {
            r = mix(r, slot(t, i));
        }
        return r;
    }

    private int site(Expr e) {
        Integer s = siteOf.get(e);
        if (s == null) {
            s = Integer.valueOf(siteElem.size());
            siteElem.add(Ty.BOT);
            siteKey.add(Ty.BOT);
            siteOf.put(e, s);
        }
        return s.intValue();
    }

    private void unify() {
        // Methods of one name and arity are called through one virtual
        // signature (TypedEmit), so they share their parameter types.
        Map<String, List<Fn>> groups = new LinkedHashMap<String, List<Fn>>();
        for (Fn f : fns.values()) {
            if (f.cls != null && !f.def.name.equals("__init__")) {
                String key = f.def.name + "/" + f.params.length;
                List<Fn> g = groups.get(key);
                if (g == null) {
                    g = new ArrayList<Fn>();
                    groups.put(key, g);
                }
                g.add(f);
            }
        }
        for (List<Fn> g : groups.values()) {
            if (g.size() < 2) {
                continue;
            }
            int n = g.get(0).params.length;
            for (int i = 1; i < n; i++) {
                Ty j = Ty.BOT;
                for (Fn f : g) {
                    j = mix(j, f.params[i]);
                }
                for (Fn f : g) {
                    f.params[i] = joinInto(f.params[i], j);
                }
            }
            Ty r = Ty.BOT;
            for (Fn f : g) {
                r = mix(r, f.ret);
            }
            for (Fn f : g) {
                f.ret = joinInto(f.ret, r);
            }
        }
    }

    // ---- walking ---------------------------------------------------------

    /// `fn` is the compiled function being walked, or null where names are
    /// not tracked (the module body tracks globals instead).
    private void walkBlock(List<Stmt> body, Fn fn, Scope sc) {
        for (Stmt s : body) {
            walk(s, fn, sc);
        }
    }

    private void walk(Stmt s, Fn fn, Scope sc) {
        if (s instanceof ExprStmt) {
            type(((ExprStmt) s).value, fn, sc);
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            Ty v = type(a.value, fn, sc);
            for (Expr t : a.targets) {
                store(t, v, a.value, fn, sc);
            }
        } else if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (a.value != null) {
                store(a.target, type(a.value, fn, sc), a.value, fn, sc);
            } else if (!(a.target instanceof Name)) {
                type(a.target, fn, sc);
            }
        } else if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            Ty cur = type(a.target, fn, sc);
            Ty v = type(a.value, fn, sc);
            // As for a plain operator: an operand nothing reaches yet leaves
            // the result to a later pass, where DYN now would stay for good.
            Ty r = v.k == Ty.K.BOT || cur.k == Ty.K.BOT && !(a.target instanceof Name) ? Ty.BOT
                    : binop(a.op, cur, v, a.value);
            if (cur.k == Ty.K.LIST && "+".equals(a.op)) {
                storeElem(cur, elem(v));
                r = cur;
            }
            store(a.target, r, null, fn, sc);
        } else if (s instanceof Return) {
            Return r = (Return) s;
            Ty v = r.value == null ? Ty.NONE : type(r.value, fn, sc);
            if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                fn.ret = joinInto(fn.ret, v);
            } else {
                escape(v);
            }
        } else if (s instanceof If) {
            If i = (If) s;
            type(i.test, fn, sc);
            walkBlock(i.body, fn, sc);
            walkBlock(i.orelse, fn, sc);
        } else if (s instanceof While) {
            While w = (While) s;
            type(w.test, fn, sc);
            walkBlock(w.body, fn, sc);
            walkBlock(w.orelse, fn, sc);
        } else if (s instanceof For) {
            For f = (For) s;
            Ty it = iterElem(f.iter, fn, sc);
            store(f.target, it, null, fn, sc);
            walkBlock(f.body, fn, sc);
            walkBlock(f.orelse, fn, sc);
        } else if (s instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) s;
            for (Expr e : d.decorators) {
                type(e, fn, sc);
            }
            for (Expr e : d.args.defaults) {
                type(e, fn, sc);
            }
            for (Expr e : d.args.kwDefaults) {
                if (e != null) {
                    type(e, fn, sc);
                }
            }
            Fn inner = fns.get(d);
            if (inner != null) {
                // Defaults fill parameters the call leaves out.
                int first = inner.params.length - d.args.defaults.size();
                for (int i = 0; i < d.args.defaults.size(); i++) {
                    inner.params[first + i] = joinInto(inner.params[first + i], type(d.args.defaults.get(i), fn, sc));
                }
                if (!d.decorators.isEmpty() || inner.cls == null && !globalFns.containsValue(inner)) {
                    escapeFn(inner);
                }
                if (inner.def.name.startsWith("__") && !"__init__".equals(inner.def.name)) {
                    // The runtime calls these itself, with anything.
                    escapeFn(inner);
                    inner.ret = joinInto(inner.ret, Ty.DYN);
                }
                if (!p.reached.contains(d) && rooted(inner)) {
                    reach(inner);
                }
                if (p.reached.contains(d)) {
                    for (int i = 0; i < inner.params.length; i++) {
                        inner.locals.put(inner.names.get(i), mix(inner.locals.get(inner.names.get(i)), inner.params[i]));
                    }
                    walkBlock(d.body, inner, d.scope);
                    if (!endsAbruptly(d.body)) {
                        inner.ret = joinInto(inner.ret, Ty.NONE);
                    }
                    if (!shadow && entered(inner)) {
                        genericView(inner);
                    }
                }
            } else {
                walkBlock(d.body, null, d.scope);
            }
            if (sc.kind == Scope.Kind.MODULE && fns.get(d) == null || fn != null && sc.kind != Scope.Kind.MODULE) {
                storeName(d.name, Ty.DYN, fn, sc);
            }
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            for (Expr b : c.bases) {
                if (!(b instanceof Name && globalClasses.containsKey(((Name) b).id))) {
                    type(b, fn, sc);
                }
            }
            for (Expr d : c.decorators) {
                type(d, fn, sc);
            }
            if (!layouts.classes.containsKey(c)) {
                for (Stmt m : c.body) {
                    if (m instanceof FunctionDef && plainMethod((FunctionDef) m)) {
                        p.plainSelf.add(((FunctionDef) m).scope);
                    }
                }
            }
            walkBlock(c.body, null, c.scope);
            if (sc.kind == Scope.Kind.MODULE && !globalClasses.containsKey(c.name)) {
                // A class without a layout is a value like any other.
                storeName(c.name, Ty.DYN, fn, sc);
            }
        } else if (s instanceof Try) {
            Try t = (Try) s;
            walkBlock(t.body, fn, sc);
            for (ExceptHandler h : t.handlers) {
                if (h.type != null) {
                    type(h.type, fn, sc);
                }
                if (h.name != null) {
                    storeName(h.name, Ty.DYN, fn, sc);
                }
                walkBlock(h.body, fn, sc);
            }
            walkBlock(t.orelse, fn, sc);
            walkBlock(t.finalbody, fn, sc);
        } else if (s instanceof With) {
            With w = (With) s;
            for (WithItem i : w.items) {
                type(i.context, fn, sc);
                if (i.vars != null) {
                    store(i.vars, Ty.DYN, null, fn, sc);
                }
            }
            walkBlock(w.body, fn, sc);
        } else if (s instanceof Import || s instanceof ImportFrom) {
            List<String> names = new ArrayList<String>();
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    int dot = n.indexOf('.');
                    names.add(dot < 0 ? n : n.substring(0, dot));
                }
            } else {
                for (Alias a : ((ImportFrom) s).names) {
                    names.add(a.asname != null ? a.asname : a.name);
                }
            }
            for (String n : names) {
                storeName(n, Ty.DYN, fn, sc);
            }
        } else {
            // Raise, Assert, Delete, Match, Global, Pass, Break, Continue:
            // whatever they evaluate escapes.
            anyChildren(s, fn, sc);
        }
    }

    private static boolean endsAbruptly(List<Stmt> body) {
        if (body.isEmpty()) {
            return false;
        }
        Stmt s = body.get(body.size() - 1);
        if (s instanceof Return || s instanceof Raise) {
            return true;
        }
        if (s instanceof If) {
            If i = (If) s;
            return endsAbruptly(i.body) && endsAbruptly(i.orelse);
        }
        if (s instanceof While) {
            While w = (While) s;
            return w.test instanceof Constant && ((Constant) w.test).kind == Constant.Kind.TRUE && !hasBreak(w.body);
        }
        return false;
    }

    static boolean hasBreak(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof Break) {
                return true;
            }
            if (s instanceof If && (hasBreak(((If) s).body) || hasBreak(((If) s).orelse))) {
                return true;
            }
            if (s instanceof Try) {
                Try t = (Try) s;
                if (hasBreak(t.body) || hasBreak(t.orelse) || hasBreak(t.finalbody)) {
                    return true;
                }
                for (ExceptHandler h : t.handlers) {
                    if (hasBreak(h.body)) {
                        return true;
                    }
                }
            }
            if (s instanceof With && hasBreak(((With) s).body)) {
                return true;
            }
        }
        return false;
    }

    /// The type of the items `for x in it` binds.
    Ty iterElem(Expr it, Fn fn, Scope sc) {
        if (rangeCall(it, sc)) {
            for (Expr a : ((Call) it).args) {
                type(a, fn, sc);
            }
            return Ty.INT;
        }
        Ty t = type(it, fn, sc);
        if (t.k == Ty.K.LIST) {
            return elem(t);
        }
        if (t.k == Ty.K.TUPLE) {
            return tupleElem(t);
        }
        if (t.k == Ty.K.STR && !t.opt) {
            // A str yields one-character strs.
            return Ty.STR;
        }
        if (t.k == Ty.K.BOT) {
            // Nothing reaches the iterable yet; a later pass decides.
            return Ty.BOT;
        }
        escape(t);
        return Ty.DYN;
    }

    boolean rangeCall(Expr e, Scope sc) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        return c.func instanceof Name && "range".equals(((Name) c.func).id) && c.keywords.isEmpty()
                && c.args.size() >= 1 && c.args.size() <= 3 && sc.resolve("range") == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport && !anyStarred(c.args);
    }

    /// A literal int index (`x[2]`, `x[-1]`) as an int; Integer.MIN_VALUE
    /// for anything else.
    static int constIndex(Expr e) {
        boolean neg = false;
        if (e instanceof UnaryOp && "-".equals(((UnaryOp) e).op)) {
            neg = true;
            e = ((UnaryOp) e).operand;
        }
        if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.INT) {
            return Integer.MIN_VALUE;
        }
        String t = ((Constant) e).text.replace("_", "");
        if (t.length() > 6 || !t.matches("[0-9]+")) {
            return Integer.MIN_VALUE;
        }
        int v = Integer.parseInt(t);
        return neg ? -v : v;
    }

    private static boolean anyStarred(List<Expr> l) {
        for (Expr e : l) {
            if (e instanceof Starred) {
                return true;
            }
        }
        return false;
    }

    private void store(Expr t, Ty v, Expr value, Fn fn, Scope sc) {
        if (t instanceof Name) {
            storeName(((Name) t).id, v, fn, sc);
        } else if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            Ty r = type(a.value, fn, sc);
            storeField(r, a.attr, v);
        } else if (t instanceof Subscript && !(((Subscript) t).index instanceof Slice)
                && dictOwner(((Subscript) t).value, sc) != null) {
            // `x.__dict__[k] = v`: setattr(x, k, v).
            Expr owner = dictOwner(((Subscript) t).value, sc);
            type(((Subscript) t).index, fn, sc);
            computedStore(type(owner, fn, sc), owner, ((Subscript) t).index, v, t, fn, sc);
        } else if (t instanceof Subscript) {
            Subscript s = (Subscript) t;
            Ty r = type(s.value, fn, sc);
            if (s.index instanceof Slice) {
                anyChildren(s.index, fn, sc);
                if (r.k == Ty.K.LIST) {
                    storeElem(r, elem(v));
                } else {
                    escape(v);
                }
            } else {
                Ty kt = type(s.index, fn, sc);
                int ci = constIndex(s.index);
                if (r.k == Ty.K.DICT) {
                    storeDict(r, kt, v);
                } else if (r.k == Ty.K.LIST && ci >= 0 && slotCount(r) > ci) {
                    // Same position, same length: the positions still hold.
                    for (int site : r.sites) {
                        siteElem.set(site, joinInto(siteElem.get(site), v));
                        joinSlot(site, slotCount(r), ci, v, false);
                    }
                } else if (r.k == Ty.K.LIST) {
                    storeElem(r, v);
                } else if (r.k != Ty.K.BOT) {
                    escape(v);
                }
            }
        } else if (t instanceof TupleExpr || t instanceof ListExpr) {
            List<Expr> elts = t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts;
            List<Expr> src = value instanceof TupleExpr ? ((TupleExpr) value).elts
                    : value instanceof ListExpr ? ((ListExpr) value).elts : null;
            boolean starred = anyStarred(elts);
            if (src != null && src.size() == elts.size() && !starred && !anyStarred(src)) {
                // `a, b = x, y` pairs each target with its own value.
                for (int i = 0; i < elts.size(); i++) {
                    store(elts.get(i), type(src.get(i), fn, sc), null, fn, sc);
                }
                return;
            }
            if (!starred && (v.k == Ty.K.TUPLE || v.k == Ty.K.LIST) && slotCount(v) == elts.size()) {
                // Each target takes its own position (a length that does
                // not match raises when unpacking).
                for (int i = 0; i < elts.size(); i++) {
                    store(elts.get(i), slot(v, i), null, fn, sc);
                }
                return;
            }
            Ty each = Ty.DYN;
            if (value != null && rangeCall(value, sc)) {
                each = Ty.INT;
            } else if (v.k == Ty.K.BOT) {
                // Nothing reaches the value yet: a later pass decides, where
                // DYN now would stay DYN for good.
                each = Ty.BOT;
            } else if (v.k == Ty.K.TUPLE) {
                each = tupleElem(v);
            } else if (v.k == Ty.K.LIST) {
                each = elem(v);
            } else {
                escape(v);
            }
            for (Expr e : elts) {
                store(e instanceof Starred ? ((Starred) e).value : e, e instanceof Starred ? Ty.DYN : each, null, fn,
                        sc);
            }
        } else if (t instanceof Starred) {
            store(((Starred) t).value, Ty.DYN, null, fn, sc);
        } else {
            anyChildren(t, fn, sc);
            escape(v);
        }
    }

    private void storeName(String n, Ty v, Fn fn, Scope sc) {
        Scope.Binding b = sc.resolve(n);
        if (b == Scope.Binding.CELL && compOnlyCell(sc, n)) {
            b = Scope.Binding.LOCAL;
        }
        switch (b) {
            case LOCAL:
                if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    fn.locals.put(n, joinInto(fn.locals.get(n), v));
                } else {
                    escape(v);
                }
                return;
            case GLOBAL:
                globals.put(n, joinInto(globals.get(n), v));
                return;
            default:
                escape(v);
        }
    }

    void storeField(Ty recv, String attr, Ty v) {
        if (recv.k == Ty.K.BOT || recv.k == Ty.K.NONE) {
            return;
        }
        List<Layouts.Cls> owners = p.fieldOwners.get(attr);
        if (owners == null) {
            escape(v);
            return;
        }
        boolean any = false;
        for (Layouts.Cls o : owners) {
            boolean reach = recv.k != Ty.K.INST || isSelfOrAncestor(o, recv.cls) || isSelfOrAncestor(recv.cls, o);
            if (reach) {
                Map<String, Ty> m = fields.get(o);
                m.put(attr, joinInto(m.get(attr), v));
                any = true;
            }
        }
        if (!any) {
            escape(v);
        }
    }

    /// The declaring class of field `attr` for an instance of `k`.
    static Layouts.Cls fieldOwner(Layouts.Cls k, String attr) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.fields.contains(attr)) {
                return c;
            }
        }
        return null;
    }

    Ty fieldType(Layouts.Cls owner, String attr) {
        Ty t = fields.get(owner).get(attr);
        return t == null ? Ty.DYN : loc(t);
    }

    /// Compiled methods a call `recv.m(...)` on an instance of `k` may run:
    /// the one `k` resolves, and every override in a subclass of `k`. Null
    /// when the name may resolve to anything else (a field, a class
    /// attribute that is not a compiled method).
    List<FunctionDef> candidates(Layouts.Cls k, String m) {
        Layouts home = p.home.get(k).layouts;
        if (home.fieldOwners.containsKey(m)) {
            return null;
        }
        List<FunctionDef> r = new ArrayList<FunctionDef>();
        Layouts.Cls res = Layouts.resolver(k, m);
        if (res != null) {
            FunctionDef f = res.typable.get(m);
            if (f == null) {
                return null;
            }
            r.add(f);
        }
        for (Layouts.Cls c : home.classes.values()) {
            if (c != k && isSelfOrAncestor(k, c) && c.bodyNames.contains(m)) { //NOPMD CompareObjectsWithEquals - identity is meant
                FunctionDef f = c.typable.get(m);
                if (f == null) {
                    return null;
                }
                if (!r.contains(f)) {
                    r.add(f);
                }
            }
        }
        return r.isEmpty() ? null : r;
    }

    private void anyChildren(Object o, Fn fn, Scope sc) {
        children(o, x -> {
            if (x instanceof Expr) {
                escape(type((Expr) x, fn, sc));
            } else if (x instanceof Stmt) {
                walk((Stmt) x, fn, sc);
            } else {
                anyChildren(x, fn, sc);
            }
        });
    }

    // ---- expressions -----------------------------------------------------

    Ty nameType(String n, Fn fn, Scope sc) {
        Scope.Binding b = sc.resolve(n);
        if (b == Scope.Binding.CELL && compOnlyCell(sc, n)) {
            b = Scope.Binding.LOCAL;
        }
        switch (b) {
            case LOCAL:
                if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Ty t = fn.locals.get(n);
                    return t == null ? Ty.BOT : t;
                }
                return Ty.DYN;
            case FREE:
            case CELL: {
                // A local of the function a comprehension (or one nested in
                // it) runs in: read where it stands, as its own type. Anything
                // may happen to it there, so it escapes.
                Fn o = compOuter;
                if (o != null && sc.kind == Scope.Kind.COMPREHENSION && sc.owner(n) == o.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Ty t = o.locals.get(n);
                    if (t == null) {
                        return Ty.BOT;
                    }
                    escape(t);
                    return t;
                }
                return Ty.DYN;
            }
            case GLOBAL: {
                Fn f = globalFns.get(n);
                if (f != null) {
                    escapeFn(f);
                    return Ty.DYN;
                }
                Layouts.Cls k = globalClasses.get(n);
                if (k != null) {
                    escapeClass(k);
                    return Ty.DYN;
                }
                if (importedNames.containsKey(n)) {
                    Ty t = valueOf(resolve(n, 0));
                    if (t != null) {
                        return t;
                    }
                }
                Ty t = globals.get(n);
                return t == null ? Ty.BOT : t;
            }
            default:
                return Ty.DYN;
        }
    }

    Ty type(Expr e, Fn fn, Scope sc) {
        return loc(type0(e, fn, sc));
    }

    /// The type of a read of `o` (see [#resolve]) as a value, or null when
    /// `o` says nothing. A function or a class read as a value goes where
    /// the pass does not follow.
    private Ty valueOf(Object o) {
        if (o instanceof Fn) {
            escapeFn((Fn) o);
            return Ty.DYN;
        }
        if (o instanceof Layouts.Cls) {
            escapeClass((Layouts.Cls) o);
            return Ty.DYN;
        }
        if (o instanceof Infer && p.looseModules.add((Infer) o)) {
            p.changed = true;
        }
        if (o instanceof Extern) {
            Extern g = (Extern) o;
            Ty t = g.module.globals.get(g.name);
            return t == null ? Ty.BOT : loc(t);
        }
        return null;
    }

    private Ty type0(Expr e, Fn fn, Scope sc) {
        if (e instanceof Constant) {
            switch (((Constant) e).kind) {
                case NONE:
                    return Ty.NONE;
                case TRUE:
                case FALSE:
                    return Ty.BOOL;
                case INT:
                    return Ty.INT;
                case FLOAT:
                    return ((Constant) e).text.endsWith("j") || ((Constant) e).text.endsWith("J") ? Ty.DYN
                            : Ty.FLOAT;
                case STR:
                    return Ty.STR;
                case BYTES:
                    return Ty.BYTES;
                default:
                    return Ty.DYN;
            }
        }
        if (e instanceof Name) {
            return nameType(((Name) e).id, fn, sc);
        }
        if (e instanceof Attribute && "modules".equals(((Attribute) e).attr) && !p.allModulesLoose && !p.done) {
            // `sys.modules`: every module of the program, as a value.
            p.allModulesLoose = true;
            p.changed = true;
        }
        if (e instanceof Attribute && CLASS_FACTS.contains(((Attribute) e).attr)
                && classOwner(((Attribute) e).value, sc) != null) {
            classFacts.add(((Attribute) e).value);
        }
        if (e instanceof Attribute && "__dict__".equals(((Attribute) e).attr)) {
            dictOf(e, ((Attribute) e).value, fn, sc);
        } else if (e instanceof Attribute && "__class__".equals(((Attribute) e).attr)) {
            classOf(e, ((Attribute) e).value, fn, sc);
        }
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (MATH_CONSTS.contains(a.attr) && isMath(a.value, sc)) {
                return Ty.FLOAT;
            }
            if (a.value instanceof Name && globalClasses.containsKey(((Name) a.value).id)
                    && sc.resolve(((Name) a.value).id) == Scope.Binding.GLOBAL) {
                // `Cls.attr` as a value: a method taken off the class runs
                // with arguments nobody tracks.
                escapeMethods(a.attr);
                return Ty.DYN;
            }
            if (foreign(a.value, sc) instanceof Infer) {
                // `module.name`.
                Ty t = valueOf(foreign(a, sc));
                if (t != null) {
                    return t;
                }
            }
            Ty r = type(a.value, fn, sc);
            return attrType(r, a.attr);
        }
        if (e instanceof Call) {
            return call((Call) e, fn, sc);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            Ty l = type(b.left, fn, sc);
            Ty r = type(b.right, fn, sc);
            if ("*".equals(b.op) && b.left instanceof ListExpr && l.k == Ty.K.LIST) {
                unslot(l);
                return Ty.list(l.sites, false);
            }
            if ("+".equals(b.op) && l.k == Ty.K.LIST && r.k == Ty.K.LIST) {
                int s = site(b);
                siteElem.set(s, joinInto(siteElem.get(s), mix(elem(l), elem(r))));
                return Ty.list(new int[] {s}, false);
            }
            if (l.k == Ty.K.BOT || r.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            Ty t = realPow(b, l, r) ? Ty.FLOAT : binop(b.op, l, r, b.right);
            if (t.k == Ty.K.DYN) {
                escape(l);
                escape(r);
            }
            return t;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            Ty t = type(u.operand, fn, sc);
            if ("not".equals(u.op)) {
                return Ty.BOOL;
            }
            if (t.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            if (t.k == Ty.K.FLOAT && !t.opt) {
                return Ty.FLOAT;
            }
            if ((t.k == Ty.K.INT || t.k == Ty.K.BOOL) && !"+".equals(u.op)) {
                return Ty.INT;
            }
            if (t.k == Ty.K.INT && "+".equals(u.op)) {
                return Ty.INT;
            }
            escape(t);
            return Ty.DYN;
        }
        if (e instanceof BoolOp) {
            Ty r = Ty.BOT;
            for (Expr v : ((BoolOp) e).values) {
                r = mix(r, type(v, fn, sc));
            }
            return r;
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            for (int i = -1; i < c.ops.size(); i++) {
                // `type(a) is B`, `type(a) == type(b)`: nothing is made.
                Expr side = i < 0 ? c.left : c.comparators.get(i);
                String op = c.ops.get(i < 0 ? 0 : i);
                String next = i + 1 < c.ops.size() ? c.ops.get(i + 1) : op;
                if (classOwner(side, sc) != null && classOp(op) && classOp(next) || dictOwner(side, sc) != null) {
                    // A dict compared is a dict read.
                    classFacts.add(side);
                }
            }
            Ty l = type(c.left, fn, sc);
            boolean plain = true;
            for (int i = 0; i < c.ops.size(); i++) {
                Ty r = type(c.comparators.get(i), fn, sc);
                String op = c.ops.get(i);
                if (!("is".equals(op) || "is not".equals(op) || "in".equals(op) || "not in".equals(op)
                        || plainCompare(l) && plainCompare(r))) {
                    plain = false;
                }
                if ("in".equals(op) || "not in".equals(op)) {
                    if (r.k != Ty.K.DICT) {
                        // A dict's `in` reads its keys and keeps nothing.
                        escape(r);
                    }
                    escape(l);
                }
                l = r;
            }
            return plain ? Ty.BOOL : Ty.DYN;
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            type(i.test, fn, sc);
            Ty a = type(i.body, fn, sc);
            Ty b = type(i.orelse, fn, sc);
            Ty r = mix(a, b);
            if (r.k == Ty.K.DYN) {
                // Neither side is followed past here.
                escape(a);
                escape(b);
            }
            return r;
        }
        if (e instanceof Subscript && !(((Subscript) e).index instanceof Slice)
                && dictOwner(((Subscript) e).value, sc) != null) {
            // `x.__dict__[k]`: a read, which changes no attribute.
            type(dictOwner(((Subscript) e).value, sc), fn, sc);
            type(((Subscript) e).index, fn, sc);
            return Ty.DYN;
        }
        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            Ty r = type(s.value, fn, sc);
            if (s.index instanceof Slice) {
                anyChildren(s.index, fn, sc);
                if (r.k == Ty.K.LIST && !r.opt) {
                    // The slice shares the sites but not the positions.
                    unslot(r);
                    return Ty.list(r.sites, false);
                }
                if (r.k == Ty.K.STR && !r.opt) {
                    // A str's slice is a str, whatever the bounds (a bad
                    // bound raises).
                    return Ty.STR;
                }
                if (r.k == Ty.K.BYTES) {
                    return Ty.BYTES;
                }
                escape(r);
                return Ty.DYN;
            }
            Ty i = type(s.index, fn, sc);
            if (r.k == Ty.K.BOT || r.k == Ty.K.NONE || i.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            int ci = constIndex(s.index);
            if (r.k == Ty.K.LIST && (i.k == Ty.K.INT || i.k == Ty.K.BOOL)) {
                return ci >= 0 ? slot(r, ci) : elem(r);
            }
            if (r.k == Ty.K.TUPLE && (i.k == Ty.K.INT || i.k == Ty.K.BOOL)) {
                int n = slotCount(r);
                if (ci < 0 && ci != Integer.MIN_VALUE && n > 0 && n + ci >= 0) {
                    ci = n + ci;
                }
                return ci >= 0 ? slot(r, ci) : tupleElem(r);
            }
            if (r.k == Ty.K.BYTES && (i.k == Ty.K.INT || i.k == Ty.K.BOOL)) {
                // A byte is an int; a bad index raises.
                return Ty.INT;
            }
            if (r.k == Ty.K.STR && !r.opt && (i.k == Ty.K.INT || i.k == Ty.K.BOOL)) {
                // A character is a str; a bad index raises.
                return Ty.STR;
            }
            if (r.k == Ty.K.DICT) {
                // A key that is not there raises: the read is of the values.
                escape(i);
                return dictVal(r);
            }
            escape(r);
            escape(i);
            return Ty.DYN;
        }
        if (e instanceof DictExpr && !((DictExpr) e).keys.contains(null)) {
            DictExpr d = (DictExpr) e;
            Ty t = Ty.dict(new int[] {site(d)});
            for (int i = 0; i < d.keys.size(); i++) {
                storeDict(t, type(d.keys.get(i), fn, sc), type(d.values.get(i), fn, sc));
            }
            return t;
        }
        if (e instanceof ListExpr) {
            ListExpr l = (ListExpr) e;
            int s = site(l);
            boolean starred = anyStarred(l.elts);
            if (starred && unslotted.add(Integer.valueOf(s))) {
                p.changed = true;
            }
            for (int i = 0; i < l.elts.size(); i++) {
                Expr x = l.elts.get(i);
                if (x instanceof Starred) {
                    Ty t = type(((Starred) x).value, fn, sc);
                    siteElem.set(s, joinInto(siteElem.get(s), t.k == Ty.K.LIST ? elem(t) : Ty.DYN));
                    escape(t);
                } else {
                    Ty t = type(x, fn, sc);
                    if (starred) {
                        siteElem.set(s, joinInto(siteElem.get(s), t));
                    } else {
                        // The position follows the value; a read of the
                        // joined elements escapes it if they lose it (elem).
                        Ty j = mix(siteElem.get(s), t);
                        if (!j.equals(siteElem.get(s))) {
                            siteElem.set(s, j);
                            p.changed = true;
                        }
                        joinSlot(s, l.elts.size(), i, t, false);
                    }
                }
            }
            return Ty.list(new int[] {s}, false);
        }
        if (e instanceof TupleExpr && !anyStarred(((TupleExpr) e).elts)) {
            TupleExpr tu = (TupleExpr) e;
            int s = site(tu);
            for (int i = 0; i < tu.elts.size(); i++) {
                joinSlot(s, tu.elts.size(), i, type(tu.elts.get(i), fn, sc), true);
            }
            return Ty.tuple(new int[] {s});
        }
        if (e instanceof NamedExpr) {
            NamedExpr n = (NamedExpr) e;
            Ty v = type(n.value, fn, sc);
            storeName(n.target.id, v, fn, sc);
            return v;
        }
        if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            for (Expr d : l.args.defaults) {
                escape(type(d, fn, sc));
            }
            escape(type(l.body, null, l.scope));
            return Ty.DYN;
        }
        if (e instanceof Comp) {
            Comp c = (Comp) e;
            Fn savedOuter = compOuter;
            if (fn != null) {
                compOuter = fn;
            }
            try {
                return comp(c, fn, sc);
            } finally {
                compOuter = savedOuter;
            }
        }
        anyChildren(e, fn, sc);
        return Ty.DYN;
    }

    /// The function the comprehension being typed runs in, or null.
    private Fn compOuter;

    private final Map<Scope, Set<String>> compCells;

    /// Whether `n`, a cell of function scope `sc`, is captured only by
    /// comprehensions: those run where they stand and cannot rebind it, so
    /// it is a plain local as far as types go.
    boolean compOnlyCell(Scope sc, String n) {
        Set<String> s = compCells.get(sc);
        return s != null && s.contains(n);
    }

    /// The cells of `d` that only comprehensions capture: every cell but
    /// those a name inside a nested function, lambda or class mentions.
    static Set<String> compOnlyCells(FunctionDef d) {
        Set<String> out = new java.util.LinkedHashSet<String>(d.scope.cells);
        for (Stmt s : d.body) {
            dropCaptured(s, out, false);
        }
        out.addAll(paramCells(d));
        return out;
    }

    /// The cells of `d` that are positional parameters nothing ever stores
    /// into -- not the body, not a nested scope through `nonlocal`, not
    /// `del`. A closure reads one; its value is the argument for the whole
    /// call, so as far as types go it is a plain local, and TypedEmit
    /// keeps the cell beside it.
    static Set<String> paramCells(FunctionDef d) {
        Set<String> out = new java.util.LinkedHashSet<String>();
        if (d.args.vararg != null || d.args.kwarg != null || !d.args.kwonly.isEmpty()) {
            return out;
        }
        Map<String, Integer> stores = TypedEmit.storeCounts(d.body);
        for (String n : d.scope.cells) {
            if (d.args.names().contains(n) && !stores.containsKey(n)) {
                out.add(n);
            }
        }
        return out;
    }

    private static void dropCaptured(Object o, Set<String> out, boolean inside) {
        if (o instanceof Name && inside) {
            out.remove(((Name) o).id);
        }
        if (o instanceof NamedExpr && !inside) {
            // A walrus in a comprehension binds in the function.
            out.remove(((NamedExpr) o).target.id);
        }
        boolean in = inside || o instanceof Lambda || o instanceof FunctionDef || o instanceof ClassDef;
        children(o, x -> dropCaptured(x, out, in));
    }

    private Ty comp(Comp c, Fn fn, Scope sc) {
        {
            boolean first = true;
            for (Comprehension g : c.generators) {
                // The first iterable is evaluated in the enclosing scope.
                escape(type(g.iter, first ? fn : null, first ? sc : c.scope));
                first = false;
                store(g.target, Ty.DYN, null, null, c.scope);
                for (Expr x : g.ifs) {
                    type(x, null, c.scope);
                }
            }
            Ty elt = c.elt != null ? type(c.elt, null, c.scope) : Ty.DYN;
            escape(elt);
            if (c.value != null) {
                escape(type(c.value, null, c.scope));
            }
            if (c.kind != Comp.Kind.LIST) {
                return Ty.DYN;
            }
            // A list of the element type, its length unknown (unslotted).
            int s = site(c);
            if (unslotted.add(Integer.valueOf(s))) {
                p.changed = true;
            }
            Ty j = joinInto(siteElem.get(s), elt);
            if (!j.equals(siteElem.get(s))) {
                siteElem.set(s, j);
                p.changed = true;
            }
            return Ty.list(new int[] {s}, false);
        }
    }

    /// Kinds whose comparisons always answer a bool: no user `__eq__`.
    private boolean plainCompare(Ty t) {
        switch (t.k) {
            case BOT:
            case NONE:
            case BOOL:
            case INT:
            case FLOAT:
            case STR:
                return true;
            case INST:
                for (Layouts.Cls c = t.cls; c != null; c = c.parent) {
                    for (String d : new String[] {"__eq__", "__ne__", "__lt__", "__le__", "__gt__", "__ge__"}) {
                        if (c.bodyNames.contains(d)) {
                            return false;
                        }
                    }
                }
                return !anySubclassDefinesCompare(t.cls);
            default:
                return false;
        }
    }

    private boolean anySubclassDefinesCompare(Layouts.Cls k) {
        for (Layouts.Cls c : p.home.get(k).layouts.classes.values()) {
            if (isSelfOrAncestor(k, c)) {
                for (String d : new String[] {"__eq__", "__ne__", "__lt__", "__le__", "__gt__", "__ge__"}) {
                    if (c.bodyNames.contains(d)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    Ty attrType(Ty r, String attr) {
        if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
            // Nothing reached the receiver yet, or None (which raises): no value.
            // Nothing reached the receiver yet: a later round decides.
            return Ty.BOT;
        }
        if (r.k == Ty.K.INST) {
            Layouts.Cls o = fieldOwner(r.cls, attr);
            if (o != null) {
                return fieldType(o, attr);
            }
            // A bound method, or something only a subclass has.
            escapeMethods(attr);
            return Ty.DYN;
        }
        escape(r);
        if (r.k == Ty.K.DYN) {
            escapeMethods(attr);
        }
        return Ty.DYN;
    }

    /// A float `**` whose answer is real: `binop` cannot see the operands,
    /// and a negative base to a fractional power answers a complex. The
    /// exponent is integral, or the base is a sum of squares (`dx * dx + ...`)
    /// or a non-negative constant.
    static boolean realPow(BinOp b, Ty l, Ty r) {
        boolean ln = l.k == Ty.K.FLOAT || l.k == Ty.K.INT || l.k == Ty.K.BOOL;
        boolean rn = r.k == Ty.K.FLOAT || r.k == Ty.K.INT || r.k == Ty.K.BOOL;
        if (!"**".equals(b.op) || l.opt || r.opt || !ln || !rn || l.k != Ty.K.FLOAT && r.k != Ty.K.FLOAT) {
            return false;
        }
        return r.k != Ty.K.FLOAT && r.k != Ty.K.BOT || nonNegative(b.left);
    }

    private static boolean nonNegative(Expr e) {
        if (e instanceof Constant) {
            Constant c = (Constant) e;
            return (c.kind == Constant.Kind.INT || c.kind == Constant.Kind.FLOAT) && !c.text.startsWith("-");
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            if ("+".equals(b.op)) {
                return nonNegative(b.left) && nonNegative(b.right);
            }
            if ("*".equals(b.op)) {
                return b.left instanceof Name && b.right instanceof Name
                        && ((Name) b.left).id.equals(((Name) b.right).id)
                        || nonNegative(b.left) && nonNegative(b.right);
            }
        }
        return false;
    }

    static Ty binop(String op, Ty l, Ty r, Expr right) {
        if (l.k == Ty.K.BOT || r.k == Ty.K.BOT) {
            return Ty.BOT;
        }
        if (l.opt || r.opt) {
            return Ty.DYN;
        }
        boolean li = l.k == Ty.K.INT || l.k == Ty.K.BOOL;
        boolean ri = r.k == Ty.K.INT || r.k == Ty.K.BOOL;
        boolean lf = l.k == Ty.K.FLOAT;
        boolean rf = r.k == Ty.K.FLOAT;
        if (li && ri) {
            switch (op) {
                case "+":
                case "-":
                case "*":
                case "//":
                case "%":
                case "&":
                case "|":
                case "^":
                case "<<":
                case ">>":
                    return Ty.INT;
                case "/":
                    return Ty.FLOAT;
                case "**":
                    // A negative exponent answers a float.
                    return right instanceof Constant && ((Constant) right).kind == Constant.Kind.INT ? Ty.INT
                            : Ty.DYN;
                default:
                    return Ty.DYN;
            }
        }
        if ((lf || li) && (rf || ri)) {
            switch (op) {
                case "+":
                case "-":
                case "*":
                case "/":
                case "//":
                case "%":
                    return Ty.FLOAT;
                default:
                    // `**` of a negative float answers a complex.
                    return Ty.DYN;
            }
        }
        if (l.k == Ty.K.STR && r.k == Ty.K.STR && "+".equals(op)) {
            return Ty.STR;
        }
        if ("*".equals(op) && (l.k == Ty.K.STR && (r.k == Ty.K.INT || r.k == Ty.K.BOOL)
                || r.k == Ty.K.STR && (l.k == Ty.K.INT || l.k == Ty.K.BOOL))) {
            // A str repeated is a str; a count too big for memory raises.
            return Ty.STR;
        }
        if (l.k == Ty.K.STR && "%".equals(op) && right instanceof TupleExpr) {
            // str % tuple formats, or raises: a tuple has no __rmod__.
            return Ty.STR;
        }
        return Ty.DYN;
    }

    private Ty call(Call c, Fn fn, Scope sc) {
        if (!shadow) {
            return call0(c, fn, sc);
        }
        boolean saved = shadowMiss;
        shadowMiss = false;
        Ty t = call0(c, fn, sc);
        boolean miss = shadowMiss;
        shadowMiss = saved;
        return miss ? Ty.DYN : t;
    }

    private Ty call0(Call c, Fn fn, Scope sc) {
        boolean simple = c.keywords.isEmpty() && !anyStarred(c.args);
        if (c.func instanceof Name && ((Name) c.func).id.startsWith("$")) {
            Ty t = missIntrinsic(((Name) c.func).id, c, fn, sc);
            if (t != null) {
                return t;
            }
        }
        if (c.func instanceof Name && sc.resolve(((Name) c.func).id) == Scope.Binding.GLOBAL) {
            String n = ((Name) c.func).id;
            Fn f = globalFns.get(n);
            Layouts.Cls k = globalClasses.get(n);
            if (f == null && k == null) {
                Object o = foreign(c.func, sc);
                f = o instanceof Fn ? (Fn) o : null;
                k = o instanceof Layouts.Cls ? (Layouts.Cls) o : null;
            }
            if (f != null || k != null) {
                return callKnown(c, f, k, fn, sc, simple);
            }
        }
        if (c.func instanceof Attribute && foreign(((Attribute) c.func).value, sc) instanceof Infer) {
            // `module.f(...)`, `module.Cls(...)`.
            Object o = foreign(c.func, sc);
            if (o instanceof Fn || o instanceof Layouts.Cls) {
                return callKnown(c, o instanceof Fn ? (Fn) o : null,
                        o instanceof Layouts.Cls ? (Layouts.Cls) o : null, fn, sc, simple);
            }
        }
        return call1(c, fn, sc, simple);
    }

    /// `c`'s arguments as one positional list for `d`'s parameters from
    /// `from` on, its keywords bound to the parameters they name, or null
    /// when they cannot be bound before the call runs. The list stops at
    /// the last parameter given; a gap before it takes the parameter's
    /// default, which must be a constant. Keywords must name parameters in
    /// the order they are written, so the list evaluates in source order.
    static List<Expr> staticArgs(Call c, FunctionDef d, int from) {
        return staticArgs(c, d, from, false);
    }

    /// [#staticArgs], with `full` running the list to the last parameter.
    static List<Expr> staticArgs(Call c, FunctionDef d, int from, boolean full) {
        if (c.keywords.isEmpty()) {
            return c.args;
        }
        Node.Arguments a = d.args;
        if (a.vararg != null || a.kwarg != null || !a.kwonly.isEmpty() || hasStarred(c.args)) {
            return null;
        }
        int n = a.positional.size() - from;
        int k = c.args.size();
        if (n <= 0 || k > n) {
            return null;
        }
        Expr[] slot = new Expr[n];
        for (int i = 0; i < k; i++) {
            slot[i] = c.args.get(i);
        }
        int last = k - 1;
        int first = Math.max(a.positionalOnly - from, 0);
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                return null;
            }
            int idx = -1;
            for (int j = first; j < n; j++) {
                if (a.positional.get(from + j).name.equals(kw.arg)) {
                    idx = j;
                    break;
                }
            }
            if (idx < 0 || slot[idx] != null || idx < last) {
                return null;
            }
            slot[idx] = kw.value;
            last = idx;
        }
        int firstDefault = a.positional.size() - a.defaults.size();
        List<Expr> r = new ArrayList<Expr>();
        int end = full ? n - 1 : last;
        for (int j = 0; j <= end; j++) {
            Expr e = slot[j];
            if (e == null) {
                int di = from + j - firstDefault;
                if (di < 0 || !(a.defaults.get(di) instanceof Constant)) {
                    return null;
                }
                e = a.defaults.get(di);
            }
            r.add(e);
        }
        return r;
    }

    /// The `__init__` a call of class `k` runs, when it is typable.
    private static FunctionDef typableInit(Layouts.Cls k) {
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        return o == null ? null : o.typable.get("__init__");
    }

    /// A call of function `f` or of class `k`, whichever is not null.
    private Ty callKnown(Call c, Fn f, Layouts.Cls k, Fn fn, Scope sc, boolean simple) {
        List<Expr> given = c.args;
        if (!c.keywords.isEmpty()) {
            FunctionDef d = f != null ? f.def : typableInit(k);
            List<Expr> b = d == null ? null : staticArgs(c, d, f != null ? 0 : 1);
            if (b != null) {
                given = b;
                simple = true;
            }
        }
        List<Ty> args = new ArrayList<Ty>();
        for (Expr a : given) {
            args.add(a instanceof Starred ? Ty.DYN : type(a, fn, sc));
        }
        if (given == c.args) { //NOPMD CompareObjectsWithEquals - identity is meant
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
        }
        if (f != null) {
            if (!callFn(f, args, 0, simple && args.size() <= f.params.length
                    && args.size() >= f.params.length - f.def.args.defaults.size())) {
                // Through the door: the typed version's result when the
                // arguments turn out to fit, the generic body's when not.
                siteRet.remove(c);
                doors.add(c);
                Fn g = generic.get(f.def);
                return g == null ? f.ret : mix(f.ret, g.ret);
            }
            // An expression function of this module's: typed
            // code relabels the call's result (TypedEmit).
            Ty site = !shadow && simple && args.size() == f.params.length
                    && p.fnHome.get(f) == this ? siteType(f, args) : null; //NOPMD CompareObjectsWithEquals - identity is meant
            if (site != null) {
                siteRet.put(c, site);
                return site;
            }
            if (!shadow) {
                siteRet.remove(c);
            }
            return f.ret;
        }
        if (open.contains(k)) {
            for (Ty a : args) {
                escape(a);
            }
            escapeClass(k);
            return Ty.DYN;
        }
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        FunctionDef init = o == null ? null : o.typable.get("__init__");
        if (o != null && init == null) {
            for (Ty a : args) {
                escape(a);
            }
            return Ty.inst(k, false);
        }
        if (init != null) {
            Fn fi = p.fns.get(init);
            if (!shadow) {
                fi.params[0] = joinInto(fi.params[0], Ty.inst(k, false));
            }
            callFn(fi, args, 1, simple && args.size() + 1 <= fi.params.length
                    && args.size() + 1 >= fi.params.length - init.args.defaults.size());
        } else {
            for (Ty a : args) {
                escape(a);
            }
        }
        return Ty.inst(k, false);
    }

    private Ty call1(Call c, Fn fn, Scope sc, boolean simple) {
        if (isMathCall(c, sc)) {
            boolean numbers = true;
            for (Expr x : c.args) {
                Ty t = type(x, fn, sc);
                numbers &= (t.k == Ty.K.INT || t.k == Ty.K.FLOAT) && !t.opt;
            }
            return numbers ? Ty.FLOAT : Ty.DYN;
        }
        if (c.func instanceof Attribute && dictOwner(((Attribute) c.func).value, sc) != null) {
            // A method of `x.__dict__`: a read leaves the attributes as
            // they are, anything else may set or remove any of them.
            Attribute a = (Attribute) c.func;
            Expr owner = dictOwner(a.value, sc);
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            for (Ty t : args) {
                escape(t);
            }
            Ty r = type(owner, fn, sc);
            if (!DICT_READS.contains(a.attr)) {
                boolean named = ("setdefault".equals(a.attr) || "__setitem__".equals(a.attr)) && c.args.size() == 2
                        && !(c.args.get(0) instanceof Starred) && c.keywords.isEmpty();
                computedStore(r, owner, named ? c.args.get(0) : null, named ? args.get(1) : Ty.DYN, c, fn, sc);
            }
            return Ty.DYN;
        }
        if (c.func instanceof Attribute && ("__setattr__".equals(((Attribute) c.func).attr)
                || "__delattr__".equals(((Attribute) c.func).attr)) && c.keywords.isEmpty() && !anyStarred(c.args)) {
            // `object.__setattr__(x, n, v)` and `x.__setattr__(n, v)`:
            // setattr, past whatever the class defines for it. The call
            // itself is typed as any other, below.
            boolean set = "__setattr__".equals(((Attribute) c.func).attr);
            int n = set ? 2 : 1;
            if (c.args.size() == n + 1) {
                computedStore(type(c.args.get(0), fn, sc), c.args.get(0), c.args.get(1),
                        set ? type(c.args.get(2), fn, sc) : Ty.DYN, c, fn, sc);
            } else if (c.args.size() == n) {
                Expr owner = ((Attribute) c.func).value;
                boolean viaSuper = owner instanceof Call && ((Call) owner).func instanceof Name
                        && "super".equals(((Name) ((Call) owner).func).id);
                if (!(viaSuper && p.plainSelf.contains(sc))) {
                    computedStore(viaSuper ? Ty.DYN : type(owner, fn, sc), owner, c.args.get(0),
                            set ? type(c.args.get(1), fn, sc) : Ty.DYN, c, fn, sc);
                }
            }
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            if (a.value instanceof Name && globalClasses.containsKey(((Name) a.value).id)
                    && sc.resolve(((Name) a.value).id) == Scope.Binding.GLOBAL) {
                // `Cls.method(obj, ...)`: an explicit call of a method.
                Layouts.Cls k = globalClasses.get(((Name) a.value).id);
                Layouts.Cls o = Layouts.resolver(k, a.attr);
                FunctionDef m = o == null ? null : o.typable.get(a.attr);
                List<Ty> args = new ArrayList<Ty>();
                for (Expr x : c.args) {
                    args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                if (m != null && !open.contains(k)) {
                    Fn f = p.fns.get(m);
                    callFn(f, args, 0, simple && args.size() <= f.params.length
                            && args.size() >= f.params.length - m.args.defaults.size());
                    return f.ret;
                }
                for (Ty t : args) {
                    escape(t);
                }
                if (m != null) {
                    escapeFn(p.fns.get(m));
                }
                return Ty.DYN;
            }
            Ty r = type(a.value, fn, sc);
            List<Expr> given = c.args;
            if (!c.keywords.isEmpty() && r.k == Ty.K.INST && !open.contains(r.cls)) {
                // One method it can be: keywords bound to its parameters.
                List<FunctionDef> cand = candidates(r.cls, a.attr);
                List<Expr> b = cand != null && cand.size() == 1 ? staticArgs(c, cand.get(0), 1, true) : null;
                if (b != null) {
                    given = b;
                    simple = true;
                }
            }
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : given) {
                args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
            }
            if (given == c.args) { //NOPMD CompareObjectsWithEquals - identity is meant
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
            }
            if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
                return Ty.BOT;
            }
            if (r.k == Ty.K.INST && !open.contains(r.cls)) {
                List<FunctionDef> cand = candidates(r.cls, a.attr);
                if (cand != null) {
                    Ty ret = Ty.BOT;
                    for (FunctionDef m : cand) {
                        Fn f = p.fns.get(m);
                        callFn(f, args, 1, simple && args.size() + 1 == f.params.length);
                        ret = mix(ret, f.ret);
                    }
                    if (Layouts.resolver(r.cls, a.attr) == null) {
                        // Not every instance has it: the rest raise.
                        ret = mix(ret, Ty.BOT);
                    }
                    return ret;
                }
            }
            if (r.k == Ty.K.LIST && !r.opt) {
                return listMethod(r, a.attr, args);
            }
            if (r.k == Ty.K.DICT && simple) {
                Ty t = dictMethod(r, a.attr, args);
                if (t != null) {
                    return t;
                }
            }
            if (r.k == Ty.K.STR && !r.opt && splitCall(a.attr, args, c)) {
                // A fresh list of strs, from a site of its own: the call.
                int s = site(c);
                siteElem.set(s, joinInto(siteElem.get(s), Ty.STR));
                if (unslotted.add(Integer.valueOf(s))) {
                    p.changed = true;
                }
                return Ty.list(new int[] {s}, false);
            }
            if (codecCall(r, a.attr, args, c)) {
                return "encode".equals(a.attr) ? Ty.BYTES : Ty.STR;
            }
            if (r.k == Ty.K.STR && !r.opt && c.keywords.isEmpty() && !hasStarred(c.args)) {
                Ty t = strMethod(a.attr, args);
                if (t != null) {
                    for (Ty x : args) {
                        escape(x);
                    }
                    return t;
                }
            }
            escape(r);
            for (Ty t : args) {
                escape(t);
            }
            if (r.k == Ty.K.DYN || r.k == Ty.K.INST) {
                escapeMethods(a.attr);
            }
            return Ty.DYN;
        }
        if (c.func instanceof Name && sc.resolve(((Name) c.func).id) == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport) {
            String n = ((Name) c.func).id;
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                args.add(x instanceof Starred ? Ty.DYN : type(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            if ("setattr".equals(n) && c.args.size() == 3 && c.keywords.isEmpty()) {
                computedStore(args.get(0), c.args.get(0), c.args.get(1), args.get(2), c, fn, sc);
            } else if ("delattr".equals(n) && c.args.size() == 2 && c.keywords.isEmpty()
                    && !(c.args.get(1) instanceof Constant)) {
                // Whichever attribute it was is gone: no type holds for it.
                computedStore(args.get(0), c.args.get(0), c.args.get(1), Ty.DYN, c, fn, sc);
            } else if ("vars".equals(n) && c.args.size() == 1 && c.keywords.isEmpty()
                    && !(c.args.get(0) instanceof Starred)) {
                dictOf(c, c.args.get(0), fn, sc);
            } else if ("type".equals(n) && c.args.size() == 1 && c.keywords.isEmpty()
                    && !(c.args.get(0) instanceof Starred)) {
                classOf(c, c.args.get(0), fn, sc);
            } else if ("getattr".equals(n) && c.args.size() >= 2 && c.keywords.isEmpty()
                    && !(c.args.get(1) instanceof Constant)) {
                computedRead(args.get(0), c.args.get(0), c.args.get(1), c, fn, sc);
            }
            if (simple && args.size() == 1 && ("len".equals(n) || "isinstance".equals(n) && false)) {
                // len reads its argument and keeps nothing.
                if (args.get(0).k != Ty.K.LIST && args.get(0).k != Ty.K.TUPLE && args.get(0).k != Ty.K.DICT) {
                    escape(args.get(0));
                }
                return Ty.INT;
            }
            if (simple && "float".equals(n) && args.size() == 1
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT) && !args.get(0).opt) {
                return Ty.FLOAT;
            }
            if (simple && "str".equals(n) && args.size() == 1 && !args.get(0).opt
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT
                    || args.get(0).k == Ty.K.STR || args.get(0).k == Ty.K.BOOL)) {
                return Ty.STR;
            }
            if (simple && args.size() == 1 && args.get(0).k == Ty.K.BOT
                    && ("ord".equals(n) || "abs".equals(n) || "float".equals(n) || "str".equals(n))) {
                // Nothing reaches the argument yet: a later pass decides,
                // where DYN now would stay DYN for good.
                return Ty.BOT;
            }
            if (simple && "ord".equals(n) && args.size() == 1 && args.get(0).k == Ty.K.STR && !args.get(0).opt) {
                return Ty.INT;
            }
            if (simple && "abs".equals(n) && args.size() == 1 && !args.get(0).opt
                    && (args.get(0).k == Ty.K.INT || args.get(0).k == Ty.K.FLOAT)) {
                return args.get(0);
            }
            if (simple && "list".equals(n) && args.size() == 1) {
                int s = site(c);
                Ty t = args.get(0);
                boolean range = rangeCall(c.args.get(0), sc);
                siteElem.set(s, joinInto(siteElem.get(s), t.k == Ty.K.LIST ? elem(t) : range ? Ty.INT : Ty.DYN));
                if (t.k != Ty.K.LIST && !rangeCall(c.args.get(0), sc)) {
                    escape(t);
                }
                return Ty.list(new int[] {s}, false);
            }
            for (Ty t : args) {
                escape(t);
            }
            return Ty.DYN;
        }
        escape(type(c.func, fn, sc));
        for (Expr x : c.args) {
            escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        return Ty.DYN;
    }

    /// The hidden calls of a miss probe (`Parser.missProbe`): the probe is
    /// the lookup or a marker, the re-read is the lookup.
    private Ty missIntrinsic(String n, Call c, Fn fn, Scope sc) {
        switch (n) {
            case "$probe":
                escape(type(c.args.get(0), fn, sc));
                return Ty.DYN;
            case "$unmiss":
                type(c.args.get(0), fn, sc);
                return type(c.args.get(1), fn, sc);
            case "$ismiss":
                type(c.args.get(0), fn, sc);
                return Ty.BOOL;
            default:
                return null;
        }
    }

    private Ty listMethod(Ty r, String m, List<Ty> args) {
        if (!"index".equals(m) && !"count".equals(m) && !"copy".equals(m)) {
            unslot(r);
        }
        switch (m) {
            case "append":
                if (args.size() == 1) {
                    storeElem(r, args.get(0));
                    return Ty.NONE;
                }
                break;
            case "pop":
                if (args.size() <= 1) {
                    return elem(r);
                }
                break;
            case "extend":
                if (args.size() == 1) {
                    storeElem(r, args.get(0).k == Ty.K.LIST ? elem(args.get(0)) : Ty.DYN);
                    if (args.get(0).k != Ty.K.LIST) {
                        escape(args.get(0));
                    }
                    return Ty.NONE;
                }
                break;
            case "insert":
                if (args.size() == 2) {
                    storeElem(r, args.get(1));
                    return Ty.NONE;
                }
                break;
            case "reverse":
            case "clear":
                return Ty.NONE;
            case "copy":
                return Ty.list(r.sites, false);
            case "index":
            case "count":
                return Ty.INT;
            default:
                break;
        }
        escape(r);
        for (Ty t : args) {
            escape(t);
        }
        return Ty.DYN;
    }

    static boolean hasStarred(List<Expr> args) {
        for (Expr x : args) {
            if (x instanceof Starred) {
                return true;
            }
        }
        return false;
    }

    /// `s.encode(...)` on a str or `b.decode(...)` on a bytes, with up to
    /// two str arguments (the encoding and the error handler), positional:
    /// TypedEmit calls the codec directly (see its `codecCall`).
    static boolean codecCall(Ty r, String m, List<Ty> args, Call c) {
        if (r.opt || !c.keywords.isEmpty() || hasStarred(c.args) || args.size() > 2) {
            return false;
        }
        for (Ty t : args) {
            if (t.k != Ty.K.STR || t.opt) {
                return false;
            }
        }
        return r.k == Ty.K.STR && "encode".equals(m) || r.k == Ty.K.BYTES && "decode".equals(m);
    }

    /// `s.split()` or `s.split(sep)` with a str separator.
    static boolean splitCall(String m, List<Ty> args, Call c) {
        return "split".equals(m) && c.keywords.isEmpty() && !hasStarred(c.args)
                && (args.isEmpty() || args.size() == 1 && args.get(0).k == Ty.K.STR && !args.get(0).opt);
    }

    /// Which of the tests of every character `m` is (`Typed.isAll`), or -1.
    static int isAllKind(String m) {
        switch (m) {
            case "isspace":
                return 0;
            case "isdigit":
                return 1;
            case "isdecimal":
                return 2;
            case "isnumeric":
                return 3;
            case "isalpha":
                return 4;
            case "isalnum":
                return 5;
            default:
                return -1;
        }
    }

    /// The type of a str method TypedEmit calls directly (see its
    /// `strMethod`, which must accept exactly these), or null.
    static Ty strMethod(String m, List<Ty> args) {
        int n = args.size();
        if (n == 0 && isAllKind(m) >= 0) {
            return Ty.BOOL;
        }
        if ((n == 2 || n == 3) && args.get(0).k == Ty.K.STR && !args.get(0).opt && args.get(1).k == Ty.K.INT
                && (n == 2 || args.get(2).k == Ty.K.INT)) {
            // With a start, and an end: bounds that are ints, clamped as a
            // slice's are, so nothing here raises.
            switch (m) {
                case "find":
                case "rfind":
                    return Ty.INT;
                case "startswith":
                case "endswith":
                    return Ty.BOOL;
                default:
                    return null;
            }
        }
        for (int i = 0; i < n; i++) {
            if (!"join".equals(m) && (args.get(i).k != Ty.K.STR || args.get(i).opt)) {
                return null;
            }
        }
        switch (m) {
            case "strip":
            case "lstrip":
            case "rstrip":
                return n <= 1 ? Ty.STR : null;
            case "lower":
            case "upper":
                return n == 0 ? Ty.STR : null;
            case "replace":
                return n == 2 ? Ty.STR : null;
            case "find":
            case "rfind":
            case "count":
                return n == 1 ? Ty.INT : null;
            case "startswith":
            case "endswith":
                return n == 1 ? Ty.BOOL : null;
            case "join":
                return n == 1 ? Ty.STR : null;
            default:
                return null;
        }
    }

    /// The type the annotation of parameter `i` of `f` names, when it is one
    /// the guard at the door can test for and a plain function's: or null.
    /// Nothing trusts the annotation. It decides which calls enter the typed
    /// version directly, and the guard decides the rest as they arrive.
    static Ty pin(Fn f, int i) {
        if (f.cls != null || i >= f.def.args.positional.size()) {
            return null;
        }
        Expr a = f.def.args.positional.get(i).annotation;
        if (!(a instanceof Name)) {
            return null;
        }
        String n = ((Name) a).id;
        return "str".equals(n) ? Ty.STR : "int".equals(n) ? Ty.INT : "bytes".equals(n) ? Ty.BYTES : null;
    }

    /// Joins a call's arguments into `f`'s parameters, from `from` on. False
    /// when the call goes through the function's door instead: an argument
    /// is not what the parameter's annotation says, so joining it would
    /// leave the parameter holding anything for every caller. The parameter
    /// keeps the annotated type, and this call is checked as it arrives.
    private boolean callFn(Fn f, List<Ty> args, int from, boolean exact) {
        reach(f);
        if (shadow) {
            // A call the generic body makes goes through the callee's own
            // guard: it adds nothing to the callee's parameters, and has
            // the typed version's result only when its arguments fit it.
            boolean fit = exact;
            for (int i = 0; fit && i < args.size(); i++) {
                fit = mix(f.params[from + i], args.get(i)).equals(f.params[from + i]);
            }
            if (!fit) {
                shadowMiss = true;
                for (Ty t : args) {
                    escape(t);
                }
            }
            return true;
        }
        if (!exact) {
            escapeFn(f);
            for (Ty t : args) {
                escape(t);
            }
            return true;
        }
        if (f.cls == null && from == 0) {
            boolean miss = false;
            for (int i = 0; !miss && i < args.size(); i++) {
                Ty pin = pin(f, i);
                miss = pin != null && Ty.join(pin, args.get(i)).k != pin.k;
            }
            if (miss) {
                for (int i = 0; i < f.params.length; i++) {
                    Ty pin = pin(f, i);
                    if (pin != null && f.params[i].k == Ty.K.BOT) {
                        f.params[i] = joinInto(f.params[i], pin);
                    }
                }
                escapeFn(f);
                for (Ty t : args) {
                    escape(t);
                }
                return false;
            }
        }
        for (int i = 0; i < args.size(); i++) {
            f.params[from + i] = joinInto(f.params[from + i], args.get(i));
        }
        return true;
    }

    // ---- reflection-free traversal ---------------------------------------

    interface Visit {
        void on(Object child);
    }

    /// Calls `v` for every AST child (node or list element) of `o`.
    static void children(Object o, Visit v) {
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                v.on(x);
            }
            return;
        }
        if (!(o instanceof Node)) {
            return;
        }
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) {
                    continue;
                }
                Class<?> t = f.getType();
                if (!Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t)) {
                    continue;
                }
                try {
                    Object x = f.get(o);
                    if (x instanceof List) {
                        for (Object y : (List<?>) x) {
                            v.on(y);
                        }
                    } else if (x != null) {
                        v.on(x);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    String dump() {
        StringBuilder b = new StringBuilder();
        for (Fn f : fns.values()) {
            b.append(f.cls == null ? "" : f.cls.def.name + ".").append(f.def.name).append('(');
            for (int i = 0; i < f.params.length; i++) {
                b.append(i == 0 ? "" : ", ").append(f.names.get(i)).append(": ").append(f.params[i]);
            }
            b.append(") -> ").append(f.ret).append(f.escapes ? " [escapes]" : "").append("\n    ").append(f.locals)
                    .append('\n');
        }
        for (Map.Entry<Layouts.Cls, Map<String, Ty>> e : fields.entrySet()) {
            b.append(e.getKey().def.name).append(' ').append(e.getValue()).append(" definite=")
                    .append(definite.get(e.getKey())).append('\n');
        }
        b.append("globals ").append(globals).append("\nsites ").append(siteElem).append(" escaped ")
                .append(escapedSites).append(p.allListsEscape ? " ALL" : "").append(" unslotted ").append(unslotted)
                .append('\n');
        for (Map.Entry<Integer, Ty[]> e : siteSlots.entrySet()) {
            b.append("  slots ").append(e.getKey()).append(' ').append(java.util.Arrays.toString(e.getValue()))
                    .append('\n');
        }
        return b.toString();
    }
}
