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
import java.util.LinkedHashSet;
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
/// simply `OBJECT` -- joining the type of every stored value into where it is
/// stored: a local of a compiled function, a parameter at each call site
/// that names its callee statically, a field, the elements of the lists
/// created at an allocation site, a global. A value that reaches somewhere
/// the pass cannot follow escapes: a function or class used as a value gets
/// `OBJECT` parameters, and a list that escapes gets `OBJECT` elements.
final class Infer {
    /// The Python line the pass is walking.
    private int line;

    /// Strict mode: reflection found in this module (an attribute named by
    /// a string the build cannot enumerate), reported by [#strictErrors].
    private final Set<String> reflective = new LinkedHashSet<String>();

    /// A function the pass types: a compiled method, or a plain function at
    /// the top of the module with a stable name.
    static final class Fn {
        /// A method of an analysed class (Layouts#analysed): walked and
        /// typed like any other, never emitted (see [Infer#emitFns]).
        boolean analysisOnly;
        final FunctionDef def;
        /// The class, for a method.
        final Layouts.Cls cls;
        final List<String> names;
        final Ty[] params;
        final Map<String, Ty> locals = new LinkedHashMap<String, Ty>();
        Ty ret = Ty.BOT;
        /// The type a function that may return a guessed value (see
        /// [Program#guessedReturns]) returns when every guess holds; its
        /// `ret` is then an object, so that one that does not can return
        /// too, and the caller takes the typed form back (TypedEmit).
        Ty guessRet;
        /// What iterating a call of this function gives, as far as its body
        /// says: the values a generator yields, or the elements of the
        /// generator expression a plain function returns (bs4's
        /// `Tag.children`, `(x for x in self.contents)`). Only ever read as
        /// a guess ([Infer#iterElem]): the generic code may hand the loop any
        /// iterator, and every element is checked as it arrives.
        Ty yields = Ty.BOT;
        boolean escapes;
        /// A method of an open class: its `self` may be an instance of a
        /// class the planner rejected, so `self` as a value is of no class
        /// the pass can tell (see [Infer#selfRecv]).
        boolean openSelf;

        Fn(FunctionDef def, Layouts.Cls cls) {
            this.def = def;
            this.cls = cls;
            this.names = def.args.names();
            this.params = new Ty[names.size()];
            for (int i = 0; i < params.length; i++) {
                params[i] = Ty.BOT;
            }
            if (def.scope.hasYield) {
                // A call answers the generator object, whatever the body returns.
                ret = Ty.OBJECT;
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
        /// Every compiled class a `CLS` type has named, by id (see
        /// [Ty#cls]).
        final List<Layouts.Cls> clsById = new ArrayList<Layouts.Cls>();
        final Map<Layouts.Cls, Integer> clsId = new IdentityHashMap<Layouts.Cls, Integer>();
        /// Every compiled method a `METH` type has named, by id (see
        /// [Ty#meth]).
        final List<Fn> methById = new ArrayList<Fn>();
        /// The functions a `FUNC` type names, by id.
        final List<Fn> funcById = new ArrayList<Fn>();
        final Map<Fn, Integer> funcId = new IdentityHashMap<Fn, Integer>();
        /// Each closure variable's type, by the scope that owns it: every
        /// store to it, from that scope or a function nested in it, joins
        /// here, and every read answers it.
        final Map<Scope, Map<String, Ty>> cells = new IdentityHashMap<Scope, Map<String, Ty>>();
        /// Container sites a value went into that the pass does not keep
        /// (see [#siteJoin]): what is read there may be anything.
        final Set<Integer> lostSites = new HashSet<Integer>();
        /// What a class body binds to each name, by class scope: the
        /// functions `getattr` finds there ([#methodsNamed]). A name not
        /// here yet has not been walked.
        final Map<Scope, Map<String, Ty>> classVals = new IdentityHashMap<Scope, Map<String, Ty>>();
        private Map<Scope, Fn> fnByScope;
        private int fnByScopeSize = -1;

        /// The function whose own scope is `sc`, or null when it has none.
        Fn fnOf(Scope sc) {
            if (fnByScopeSize != fns.size()) {
                fnByScope = new IdentityHashMap<Scope, Fn>();
                for (Fn f : fns.values()) {
                    fnByScope.put(f.def.scope, f);
                }
                fnByScopeSize = fns.size();
            }
            return fnByScope.get(sc);
        }
        final Map<Fn, Integer> methId = new IdentityHashMap<Fn, Integer>();
        /// Dotted module name -> its view.
        final Map<String, Infer> modules = new LinkedHashMap<String, Infer>();
        /// See [Infer#registeredNames]; null until first asked.
        Set<String> registered;
        final Map<FunctionDef, Fn> fns = new IdentityHashMap<FunctionDef, Fn>();
        /// The methods of [#fns] by name, for [#escapeMethods], which runs
        /// per attribute read; rebuilt when [#fns], which only grows, has.
        private final Map<String, List<Fn>> methodsByName = new HashMap<String, List<Fn>>();
        private int methodsIndexed = -1;

        List<Fn> methodsNamed(String name) {
            if (methodsIndexed != fns.size()) {
                methodsByName.clear();
                for (Fn f : fns.values()) {
                    if (f.cls != null) {
                        methodsByName.computeIfAbsent(f.def.name, x -> new ArrayList<Fn>()).add(f);
                    }
                }
                methodsIndexed = fns.size();
            }
            List<Fn> l = methodsByName.get(name);
            return l == null ? java.util.Collections.<Fn>emptyList() : l;
        }        /// `super().m(...)` calls resolved to the one method they run
        /// ([#superTarget]), for the emitter to call directly.
        final Map<Call, Fn> superCalls = new IdentityHashMap<Call, Fn>();
        /// The method names [#escapeMethods] let go: a method of that name
        /// escapes when its class becomes live.
        final Set<String> escapedNames = new HashSet<String>();
        /// The names of the program's functions and classes, at any depth,
        /// and whether a class of it answers attributes it does not define
        /// (a `__getattr__` that returns): see [Infer#closedName]. Null
        /// until first asked; the set of functions is fixed by then.
        Set<String> defNames;
        /// The `__getattr__`/`__getattribute__` methods that answer a name
        /// with something other than the same name read off another object.
        List<Fn> answering;
        /// The calls typed through a method slot (see [Infer#closedName]):
        /// their value is what the program's methods of the name return.
        final Set<Call> slotCalls = java.util.Collections.newSetFromMap(new IdentityHashMap<Call, Boolean>());
        /// Classes the program constructs, or calls a method on through the
        /// class itself (a classmethod's `cls(...)`): see [#classLive].
        final Set<Layouts.Cls> constructed = java.util.Collections.newSetFromMap(
                new IdentityHashMap<Layouts.Cls, Boolean>());
        /// The module each compiled class and each typed function is in.
        final Map<Layouts.Cls, Infer> home = new IdentityHashMap<Layouts.Cls, Infer>();
        final Map<Fn, Infer> fnHome = new IdentityHashMap<Fn, Infer>();
        /// Field name -> every compiled class of the program declaring it.
        final Map<String, List<Layouts.Cls>> fieldOwners = new HashMap<String, List<Layouts.Cls>>();
        final Map<Layouts.Cls, Map<String, Ty>> fields = new LinkedHashMap<Layouts.Cls, Map<String, Ty>>();
        final Map<Layouts.Cls, Set<String>> definite = new HashMap<Layouts.Cls, Set<String>>();
        final Set<Layouts.Cls> open = new HashSet<Layouts.Cls>();
        /// Every name the body of a class the planner rejected binds, by
        /// each compiled class it derives from (and their ancestors): on an
        /// instance of such an open class the name may answer what the
        /// rejected class defines instead of the compiled one. Kept per
        /// class because one program-wide set made bs4's
        /// `self.filter(...)` generic -- and every field of SoupStrainer
        /// untyped -- when some unrelated rejected class defined `filter`.
        final Map<Layouts.Cls, Set<String>> rejectedNames = new HashMap<Layouts.Cls, Set<String>>();
        /// The names of a rejected class whose base names no class the pass
        /// can find: it may derive from any open class.
        final Set<String> rejectedAnywhere = new HashSet<String>();
        final Map<Expr, Integer> siteOf = new IdentityHashMap<Expr, Integer>();
        /// The sites the generic versions' walks give a function's private
        /// lists (see Infer#privateLiterals), apart from the typed ones'.
        final Map<Expr, Integer> shadowSiteOf = new IdentityHashMap<Expr, Integer>();
        /// The literals of lists no code but their own function's sees, and
        /// the tuple literals put in them (Infer#privateLiterals).
        Set<Expr> privateLits;
        /// What calls from generic bodies pass a function whose guard they
        /// miss, by parameter: its generic version is walked with these
        /// (see [#genericView]) instead of the arguments being let go.
        final Map<FunctionDef, Ty[]> missed = new IdentityHashMap<FunctionDef, Ty[]>();
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
        /// Method names read off a class nobody can name [#anyClassAttr]:
        /// each loose class's methods of the name escape, including a class
        /// loosened later [#loosen].
        final Set<String> anyClassNames = new HashSet<String>();
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
        /// Classes whose hierarchy [Infer#loosen] made loose.
        final Set<Layouts.Cls> loosened = java.util.Collections.newSetFromMap(new IdentityHashMap<Layouts.Cls, Boolean>());
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
        /// Classes an instance store with a computed name (`setattr`, an
        /// attribute dict handed on) may reach, and whether one was on an
        /// object of no class the build can tell -- which, as for the
        /// field types, reaches the loose classes. For [#classConstant].
        final Set<Layouts.Cls> computedOn = new HashSet<Layouts.Cls>();
        boolean computedOnAny;
        /// The scopes of methods of classes without a compiled layout:
        /// their first parameter is never a compiled instance.
        final Set<Scope> plainSelf = java.util.Collections.newSetFromMap(new IdentityHashMap<Scope, Boolean>());
        /// The guarded fields, per class: a store the pass cannot type reaches
        /// them -- a value nothing knows, a receiver of no known class, a
        /// computed name, the generic body of a function -- or a store of a
        /// guessed value (see [Infer#guessed]). Such a store does not widen
        /// the field: it keeps what the typed stores put there, as a guess,
        /// in a typed slot beside an object that replaces it when a value
        /// arrives that does not fit (TypedEmit). Reads of it are guessed.
        final Map<Layouts.Cls, Set<String>> unknownStores = new IdentityHashMap<Layouts.Cls, Set<String>>();
        /// The expressions whose value is a guess: the typed value the pass
        /// gives them holds only while every guessed value they are made of
        /// fits its guess. Typed code keeps such a value as its typed form
        /// beside the object that replaces it, and computes it twice, the
        /// typed way and the generic way, picking by the guesses it read.
        final Set<Expr> guessedExprs = java.util.Collections.newSetFromMap(new IdentityHashMap<Expr, Boolean>());
        /// Locals that may be bound to a guessed value, per function.
        final Map<Fn, Set<String>> guessedLocals = new IdentityHashMap<Fn, Set<String>>();
        /// Functions that may return a guessed value.
        final Set<Fn> guessedReturns = java.util.Collections.newSetFromMap(new IdentityHashMap<Fn, Boolean>());
        /// The parameters a call passed a value of no evidence (Ty#UNSEEN):
        /// one the source says takes several kinds ([#declared]) is then
        /// held as an object, since what that call passes is not known.
        final Map<Fn, boolean[]> unseenArgs = new IdentityHashMap<Fn, boolean[]>();

        /// The functions a guarded call passed a value of no known type:
        /// called, though no parameter has a type from it.
        final Set<Fn> objectArgs = java.util.Collections.newSetFromMap(new IdentityHashMap<Fn, Boolean>());
        /// Module globals that may be bound to a guessed value, per module.
        final Map<Infer, Set<String>> guessedGlobals = new IdentityHashMap<Infer, Set<String>>();
        /// List, tuple and dict sites whose elements may be guessed values:
        /// a guessed value went in, or a guessed value of the site was read
        /// (a guarded field may hold a list of any site that passes for it).
        final Set<Integer> guessedSites = new HashSet<Integer>();
        /// The scopes of methods of compiled classes that the pass walks
        /// without a typed view (`**kwargs`, too many parameters...), with
        /// their class: the first parameter is an instance of it or of a
        /// subclass, as it is for a typed method, so a store through it
        /// reaches that hierarchy's fields and not every field of the name.
        final Map<Scope, Layouts.Cls> untypedSelf = new IdentityHashMap<Scope, Layouts.Cls>();
        final Map<Scope, Set<String>> compCells = new IdentityHashMap<Scope, Set<String>>();
        boolean allListsEscape;
        /// The modules whose bodies run: the main module, what the Java
        /// side imports by name, and every module reached code imports.
        /// Only their bodies are walked and only they are emitted; the
        /// rest of the sources never run (see [#markLive]). With
        /// `allLive` set (a library build, or no main module) every
        /// module counts.
        final Set<Infer> live = java.util.Collections.newSetFromMap(new IdentityHashMap<Infer, Boolean>());
        boolean allLive = true;
        /// Set when every module of the program is in it ([WholeProgram]):
        /// a store from another module is then walked here like any other
        /// and reaches the field through [Infer#storeField], so a field
        /// name another module stores needs no blanket widening.
        boolean closed;
        /// Set once a module that imports by a computed name is live: from
        /// then on a string of a live module that names a module, or a
        /// constant prefix of a formatted one, makes it live too.
        boolean computedImports;

        /// Makes module `name` and each package above it live.
        void markLive(String name) {
            if (name == null) {
                return;
            }
            int from = 0;
            while (true) {
                int dot = name.indexOf('.', from);
                Infer m = modules.get(dot < 0 ? name : name.substring(0, dot));
                if (m != null && live.add(m)) {
                    changed = true;
                    if ("importlib".equals(m.moduleName) || "_cn1imp".equals(m.moduleName)) {
                        computedImports = true;
                        for (Infer l : new ArrayList<Infer>(live)) {
                            l.scanStrings();
                        }
                    } else if (computedImports) {
                        m.scanStrings();
                    }
                }
                if (dot < 0) {
                    return;
                }
                from = dot + 1;
            }
        }

        /// Makes live every module whose name starts with `prefix`.
        void markPrefix(String prefix) {
            for (String n : new ArrayList<String>(modules.keySet())) {
                if (n.startsWith(prefix)) {
                    markLive(n);
                }
            }
        }

        boolean isLive(Infer m) {
            return allLive || live.contains(m);
        }

        /// Methods taken out of their class's typed dispatch because a store
        /// reaches them ([#patchedFound]), by class: shared by every run of
        /// the program, and only ever grown.
        Map<ClassDef, Set<String>> patched = new IdentityHashMap<ClassDef, Set<String>>();
        /// Attribute names stored on a value of no known class.
        final Set<String> dynStored = new HashSet<String>();
        /// Names a setattr takes from a known set of strings that is no
        /// literal argument (a loop over a tuple of names).
        final Set<String> computedStoredNames = new HashSet<String>();
        /// Attribute names stored on an instance of a known class, by class.
        final Map<Layouts.Cls, Set<String>> clsStored = new IdentityHashMap<Layouts.Cls, Set<String>>();

        /// Every typed method a store of this run may replace: a store on a
        /// value of no known class reaches the classes whose instances or
        /// class object went where the pass does not follow (the loose
        /// ones; [Infer#escapeClass] loosens a class whose object escapes);
        /// one on an instance of a known class reaches that class, its
        /// ancestors and the classes below it.
        Map<ClassDef, Set<String>> patchedFound() {
            Map<ClassDef, Set<String>> r = new IdentityHashMap<ClassDef, Set<String>>();
            for (Map.Entry<Layouts.Cls, Infer> h : home.entrySet()) {
                Layouts.Cls k = h.getKey();
                for (String m : k.typable.keySet()) {
                    // Only a name the program stores by name somewhere but
                    // on its own `self` can be replaced at all: that test
                    // reads the source, this one the types, and a store
                    // that reaches the method passes both.
                    boolean named = h.getValue().facts.patched(m) || computedStoredNames.contains(m);
                    boolean hit = named && loose.contains(k) && dynStored.contains(m);
                    for (Map.Entry<Layouts.Cls, Set<String>> e : clsStored.entrySet()) {
                        if (hit) {
                            break;
                        }
                        hit = e.getValue().contains(m) && named
                                && (isSelfOrAncestor(e.getKey(), k) || isSelfOrAncestor(k, e.getKey()));
                    }
                    if (hit) {
                        Set<String> s = r.get(k.def);
                        if (s == null) {
                            s = new HashSet<String>();
                            r.put(k.def, s);
                        }
                        s.add(m);
                    }
                }
            }
            return r;
        }

        /// Adds what this run found to [#patched]; whether any of it is new.
        boolean morePatched() {
            boolean grew = false;
            for (Map.Entry<ClassDef, Set<String>> e : patchedFound().entrySet()) {
                Set<String> s = patched.get(e.getKey());
                if (s == null) {
                    s = new HashSet<String>();
                    patched.put(e.getKey(), s);
                }
                grew |= s.addAll(e.getValue());
            }
            return grew;
        }

        /// A program over the same modules, as they were entered, to run
        /// again with [#patched] taken out.
        Program fresh() {
            Program n = new Program();
            n.closed = closed;
            n.runtimeNames = runtimeNames;
            n.keep.addAll(keep);
            n.patched = patched;
            n.allLive = allLive;
            for (Infer in : modules.values()) {
                Infer c = n.add(in.moduleName, in.pkg, in.body, in.initialLayouts, in.moduleScope, in.facts);
                c.replanner = in.replanner;
            }
            if (!allLive) {
                for (Infer in : live) {
                    n.live.add(n.modules.get(in.moduleName));
                }
            }
            return n;
        }

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

        /// Compiled class -> the compiled classes of the whole program below
        /// it, in module and plan order; built on first use, once every
        /// module is collected.
        private Map<Layouts.Cls, List<Layouts.Cls>> below;

        private static boolean javaBelow(Layouts.Cls c, Layouts.Cls k) {
            for (Layouts.Cls d = c; d != null; d = d.parent) {
                if (d == k) { //NOPMD CompareObjectsWithEquals - identity is meant
                    return true;
                }
            }
            return false;
        }

        List<Layouts.Cls> below(Layouts.Cls k) {
            if (below == null) {
                below = new IdentityHashMap<Layouts.Cls, List<Layouts.Cls>>();
                for (Infer in : modules.values()) {
                    for (Layouts.Cls c : in.layouts.all().values()) {
                        // Above c: its Java chain, and every class merged
                        // into c or a class of that chain
                        // (Layouts#mergeExtras) with its own chain -- typed
                        // code of a merged class runs over c's instances.
                        Set<Layouts.Cls> above = java.util.Collections.newSetFromMap(
                                new IdentityHashMap<Layouts.Cls, Boolean>());
                        List<Layouts.Cls> todo = new ArrayList<Layouts.Cls>();
                        for (Layouts.Cls d = c; d != null; d = d.parent) {
                            todo.addAll(d.mixedFrom);
                            above.add(d);
                        }
                        while (!todo.isEmpty()) {
                            Layouts.Cls x = todo.remove(todo.size() - 1);
                            for (Layouts.Cls d = x; d != null; d = d.parent) {
                                if (above.add(d)) {
                                    todo.addAll(d.mixedFrom);
                                }
                            }
                        }
                        above.remove(c);
                        for (Layouts.Cls a : above) {
                            List<Layouts.Cls> l = below.get(a);
                            if (l == null) {
                                l = new ArrayList<Layouts.Cls>();
                                below.put(a, l);
                            }
                            l.add(c);
                        }
                    }
                }
            }
            List<Layouts.Cls> l = below.get(k);
            return l == null ? java.util.Collections.<Layouts.Cls>emptyList() : l;
        }

        /// Plans again every module with a class whose base is a compiled
        /// class of another module, so that class is compiled as that
        /// class's subclass ([Layouts#plan]). A base must be final before
        /// a class extends it, so modules go in the order their bases
        /// allow; a module in a cycle of such bases takes what is final by
        /// then, and a base that is not stays generic, as before.
        private void adoptForeignBases() {
            Map<Layouts, Infer> byLayouts = new IdentityHashMap<Layouts, Infer>();
            List<Infer> pending = new ArrayList<Infer>();
            Map<Infer, Set<String>> excluded = new IdentityHashMap<Infer, Set<String>>();
            Set<Infer> mixing = java.util.Collections.newSetFromMap(new IdentityHashMap<Infer, Boolean>());
            Set<ClassDef> mixins = foreignMixins(excluded, mixing);
            for (Infer in : modules.values()) {
                byLayouts.put(in.layouts, in);
                if (in.replanner != null && (in.mayExtendForeign() || excluded.containsKey(in) || mixing.contains(in))) {
                    pending.add(in);
                }
            }
            Set<Infer> waiting = java.util.Collections.newSetFromMap(new IdentityHashMap<Infer, Boolean>());
            waiting.addAll(pending);
            boolean progress = true;
            while (!pending.isEmpty()) {
                boolean force = !progress;
                progress = false;
                for (java.util.Iterator<Infer> it = pending.iterator(); it.hasNext();) {
                    Infer in = it.next();
                    if (!force && in.waitsOn(waiting)) {
                        continue;
                    }
                    Layouts before = in.layouts;
                    Set<String> mine = excluded.get(in);
                    Layouts after = in.replanner.plan(b -> {
                        Layouts.Cls k = in.foreignClass(b);
                        Infer h = k == null ? null : byLayouts.get(k.home);
                        return h == null || waiting.contains(h) ? null : k;
                    }, mine == null ? java.util.Collections.<String>emptySet() : mine, b -> {
                        ClassDef m = in.foreignClassDef(b);
                        return m != null && mixins.contains(m) ? m : null;
                    });
                    byLayouts.remove(before);
                    byLayouts.put(after, in);
                    in.layouts = after;
                    // The classes are new objects: what was below the old ones
                    // is asked again.
                    below = null;
                    waiting.remove(in);
                    it.remove();
                    progress = true;
                    if (force) {
                        // One module of the cycle planned with what is final
                        // is enough to break it; the rest wait again for
                        // the bases that plan just made final.
                        break;
                    }
                }
            }
        }

        /// The classes mixed in, as a later base, by a class of another
        /// module, and fit to stay uncompiled under it
        /// ([Layouts#foreignMixin]); `excluded` gets each one's name under
        /// its module and `mixing` every module with a class mixing one in.
        /// A class any module also uses as a first base keeps its compiled
        /// layout: its subclasses extend it.
        private Set<ClassDef> foreignMixins(Map<Infer, Set<String>> excluded, Set<Infer> mixing) {
            Set<String> superNames = new HashSet<String>();
            Set<ClassDef> first = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
            for (Infer in : modules.values()) {
                Layouts.superAttrs(in.body, superNames);
                for (Stmt s : in.body) {
                    if (s instanceof ClassDef && !((ClassDef) s).bases.isEmpty()) {
                        Expr b0 = ((ClassDef) s).bases.get(0);
                        ClassDef d = b0 instanceof Name ? Layouts.classNamed(in.body, ((Name) b0).id) : null;
                        if (d == null) {
                            d = in.foreignClassDef(b0);
                        }
                        if (d != null) {
                            first.add(d);
                        }
                    }
                }
            }
            Set<ClassDef> r = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
            for (Infer in : modules.values()) {
                for (Stmt s : in.body) {
                    if (!(s instanceof ClassDef)) {
                        continue;
                    }
                    List<Expr> bases = ((ClassDef) s).bases;
                    for (int i = 1; i < bases.size(); i++) {
                        Expr b = bases.get(i);
                        if (b instanceof Name && Layouts.classNamed(in.body, ((Name) b).id) != null) {
                            continue;
                        }
                        ClassDef m = in.foreignClassDef(b);
                        Infer h = in.classHome(b);
                        if (m == null || h == null || h == in || h.replanner == null || first.contains(m) //NOPMD CompareObjectsWithEquals - identity is meant
                                || !Layouts.foreignMixin(m, superNames)) {
                            continue;
                        }
                        r.add(m);
                        excluded.computeIfAbsent(h, x -> new HashSet<String>()).add(m.name);
                        mixing.add(in);
                    }
                }
            }
            return r;
        }

        /// Runs the pass over every module, to the fixpoint. The types only
        /// ever widen and there are finitely many, so it ends; the bound is
        /// there for a fault in the pass, which is reported, never worked
        /// around by leaving the program untyped.
        void run() {
            for (Infer in : modules.values()) {
                in.countGlobals(in.body);
            }
            for (Infer in : modules.values()) {
                in.bindImports();
            }
            adoptForeignBases();
            for (Infer in : modules.values()) {
                in.collect(in.body);
            }
            for (Infer in : modules.values()) {
                in.markOpenForeign(in.body);
            }
            // A method of an open class also runs for instances of a class
            // the planner rejected, whose `self` is no instance of a
            // compiled class: its stores of `self` hold that, so `self` is
            // typed as what it may be. (The typed copy's entry admits only
            // the compiled class, but the generic copy stores too.)
            for (Fn f : fns.values()) {
                if (f.cls != null && open.contains(f.cls) && f.params.length > 0) {
                    f.openSelf = true;
                }
            }
            // A method of a class merged into others' layouts
            // (Layouts#mergeExtras) runs over their instances too, which
            // are no Java subclass of it: its `self` is the union of the
            // class and every class that merges it (below() counts them),
            // whichever path enters it -- a typed call, or its door from a
            // computed attribute name.
            for (Fn f : fns.values()) {
                if (f.cls == null || f.params.length == 0 || f.params[0].k != Ty.K.INST || f.params[0].cls != f.cls) { //NOPMD CompareObjectsWithEquals - identity is meant
                    continue;
                }
                // A Java subclass is already an instance of f.cls; joining it
                // would only widen a precise `self` into a union and cost
                // every typed path below. Only a class that merges f.cls
                // without extending it is added.
                Ty self = f.params[0];
                boolean merged = false;
                for (Layouts.Cls c : below(f.cls)) {
                    if (!javaBelow(c, f.cls)) {
                        self = Ty.join(self, Ty.inst(c, false));
                        merged = true;
                    }
                }
                if (merged) {
                    f.params[0] = self;
                }
            }
            // An analysed class was refused a layout for what makes its
            // dispatch dynamic (a metaclass, a decorator, a builtin base): its
            // methods are run from where the analysis cannot see, so each is
            // entered as by an unknown caller. Their bodies are still typed.
            // A nested function or a lambda is analysisOnly too, but is
            // entered only through the values the pass follows.
            for (Fn f : new ArrayList<Fn>(fns.values())) {
                if (f.analysisOnly && f.cls != null) {
                    fnHome.get(f).escapeFn(f);
                }
            }
            for (rounds = 0; rounds < 2000; rounds++) {
                changed = false;
                for (Infer in : new ArrayList<Infer>(modules.values())) {
                    if (isLive(in)) {
                        in.walkBlock(in.body, null, in.moduleScope);
                        in.unify();
                    }
                }
                if (computedImports) {
                    for (Infer l : new ArrayList<Infer>(live)) {
                        l.scanStrings();
                    }
                }
                if (!modules.isEmpty()) {
                    modules.values().iterator().next().sweep();
                }
                if (!escapedNames.isEmpty() && !modules.isEmpty()) {
                    modules.values().iterator().next().deadMethods();
                }
                // What a function entered from an unseen caller returns goes
                // back to that caller: a function it returns is called there.
                // A call the pass walks (a duck call by the same name) joins
                // the result where it is made instead, and a join that
                // loses it escapes it there (see [#mix]).
                for (Fn f : new ArrayList<Fn>(fns.values())) {
                    if (f.ret != null && fnHome.get(f).unseenCaller(f)) {
                        fnHome.get(f).escapeFuncs(f.ret);
                    }
                }
                if (!changed) {
                    for (Infer in : modules.values()) {
                        if (isLive(in)) {
                            in.uncalled();
                        }
                    }
                }
                if (!changed) {
                    done = true;
                    for (Fn f : guessedReturns) {
                        if (f.ret.k != Ty.K.INT && guessable(f.ret)) {
                            f.guessRet = f.ret;
                            f.ret = Ty.OBJECT;
                        }
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
    Layouts layouts;
    /// `k` and every compiled class of the program below it.
    List<Layouts.Cls> selfAndBelow(Layouts.Cls k) {
        List<Layouts.Cls> r = new ArrayList<Layouts.Cls>();
        r.add(k);
        r.addAll(p.below(k));
        return r;
    }

    /// Plans the module's layouts again with the compiled classes of other
    /// modules its classes may extend (set by the emitter that planned
    /// them); null when the module cannot be planned again.
    Replanner replanner;

    /// Plans a module again: `foreignBase` and `foreignMixin` resolve a base
    /// expression to a compiled class or a mixed-in class of another module
    /// (or null), and `excluded` names the module's own classes another
    /// module mixes in. See [Layouts#plan].
    interface Replanner {
        Layouts plan(java.util.function.Function<Expr, Layouts.Cls> foreignBase, Set<String> excluded,
                java.util.function.Function<Expr, ClassDef> foreignMixin);
    }
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
    /// Global names bound by `import` to a module of [#NATIVE_RET], to the
    /// module's dotted name.
    final Map<String, String> nativeMods = new HashMap<String, String>();
    /// Calls of [#NATIVE_RET] functions and what each answers, as the last
    /// pass typed them (TypedEmit reads it).
    final Map<Call, Ty> nativeCalls = new IdentityHashMap<Call, Ty>();
    /// What library functions answer, by dotted name: the runtime's own
    /// (StdModules) and the stdlib's path and environment helpers. The
    /// value is the type when every argument is a `str` (the functions
    /// that take one); a call with any other argument stays untyped.
    static final Map<String, Ty> NATIVE_RET = new HashMap<String, Ty>();
    static {
        for (String f : new String[] {"perf_counter", "time", "monotonic", "process_time"}) {
            NATIVE_RET.put("time." + f, Ty.FLOAT);
        }
        String[] paths = {"join", "basename", "dirname", "abspath", "normpath", "expanduser", "realpath", "normcase"};
        for (String f : paths) {
            NATIVE_RET.put("os.path." + f, Ty.STR);
        }
        NATIVE_RET.put("os.getcwd", Ty.STR);
        // One argument: the value or None; a str default: always a str.
        NATIVE_RET.put("os.environ.get", Ty.STR.orNone());
        NATIVE_RET.put("os.getenv", Ty.STR.orNone());
    }
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

    /// The plan the module was entered with, before a base in another
    /// module replanned it: what a run again starts from ([Program#fresh]).
    private final Layouts initialLayouts;

    private Infer(Program p, String moduleName, boolean pkg, List<Stmt> body, Layouts layouts,
            Scope moduleScope, WholeProgram.Module facts) {
        this.p = p;
        this.moduleName = moduleName;
        this.pkg = pkg;
        this.body = body;
        this.layouts = layouts;
        this.initialLayouts = layouts;
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
            if (p.closed && f.cls != null && !f.escapes && !f.openSelf && !classLive(f.cls)) {
                // A method of a class no instance of which can exist, in a
                // program nothing outside extends: it never runs. Walking
                // it as entered by anyone let bs4's never-built
                // BeautifulStoneSoup loosen BeautifulSoup through its
                // `super().__init__(*args, **kwargs)`.
                continue;
            }
            if (p.closed && f.cls != null && !f.escapes && !f.openSelf && !p.loose.contains(f.cls)
                    && !open.contains(f.cls) && noCallReached(f) && f.def.decorators.isEmpty()) {
                // A method nothing calls, of a class whose every instance
                // the pass follows: a call of it on one would be seen, and
                // one through a value or by the runtime escapes it. It
                // never runs. Walked as entered by anyone, warnings'
                // `deprecated.__call__` read `arg.__init__` off an untyped
                // `arg` and every `__init__` of the program lost its types.
                continue;
            }
            for (int i = f.cls != null ? 1 : 0; i < f.params.length; i++) {
                if (f.params[i].k == Ty.K.BOT) {
                    Ty pin = pin(f, i);
                    f.params[i] = joinInto(f.params[i], seed(f, i, pin));
                } else if (!f.params[i].wide() && widen(f, i)) {
                    f.params[i] = joinInto(f.params[i], Ty.OBJECT);
                }
            }
        }
    }

    /// Whether no call has reached `f`: a parameter past the receiver, and
    /// every such one still without a type.
    private boolean noCallReached(Fn f) {
        if (f.params.length < 2 || p.unseenArgs.containsKey(f) || p.objectArgs.contains(f)) {
            // A call passing values of no known type reached it.
            return false;
        }
        for (int i = 1; i < f.params.length; i++) {
            if (f.params[i].k != Ty.K.BOT) {
                return false;
            }
        }
        return true;
    }

    /// Whether parameter `i` of `f`, which the visible calls gave a type,
    /// is better left untyped. When the source says it takes several kinds
    /// ([#declared]): the calls supplied a default and nothing more --
    /// `__get__(self, obj, type: Any = None)` held `type: None`, and the
    /// interpreter, calling it with the owner class, missed the guard on
    /// every access. And when the calls a function entered through its
    /// guard can see pass only None: a parameter that is always None buys
    /// typed code nothing, and every other caller misses.
    private boolean widen(Fn f, int i) {
        Ty d = declared(f, i);
        return d != null && d.wide() || f.escapes && f.params[i].k == Ty.K.NONE;
    }

    /// Set while a function body is walked as its generic version runs it
    /// (see [#genericView]).
    private boolean shadow;
    private boolean shadowMiss;
    /// [#localModule]'s answers, by function scope.
    private final Map<Scope, Map<String, Infer>> localModules = new IdentityHashMap<Scope, Map<String, Infer>>();
    private final Map<FunctionDef, Fn> generic;

    /// Whether `f` is called from code the pass does not walk: it escaped,
    /// or its instances reached code that reads attributes by computed name.
    private boolean unseenCaller(Fn f) {
        return f.escapes || p.anyRead && (f.cls != null ? p.loose.contains(f.cls)
                : p.allModulesLoose || p.looseModules.contains(p.fnHome.get(f)));
    }

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
                for (Layouts.Cls k : layouts.all().values()) {
                    if (isSelfOrAncestor(f.cls, k) && facts.namedOutside(k.def.name)) {
                        return true;
                    }
                }
                return false;
            }
            for (Layouts.Cls k : layouts.all().values()) {
                if (isSelfOrAncestor(f.cls, k) && facts.reachable(k.def.name)) {
                    return true;
                }
            }
            return false;
        }
        if ("__call__".equals(n) && followedCall(f)) {
            // Every call of it the pass sees. Walked as entered with
            // anything, warnings' `deprecated.__call__` read `arg.__init__`
            // off an untyped `arg` and every `__init__` of the program lost
            // its types. Taken off the class as a value (`K.__call__`), it
            // escapes and is entered above.
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
            generic.put(f.def, g);
        }
        // Entered from where the pass cannot see, a parameter holds
        // anything; entered only by generic bodies this pass walked, it
        // holds what those passed.
        boolean outside = entered(f);
        Ty[] m = p.missed.get(f.def);
        for (int i = 0; i < g.params.length; i++) {
            g.params[i] = i == 0 && f.cls != null ? f.params[0]
                    : outside || m == null ? Ty.OBJECT : Ty.join(g.params[i], m[i]);
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
        Set<String> star = moduleScope.hasStarImport ? starBound() : java.util.Collections.<String>emptySet();
        if (star == null) {
            // A star import of a module the build cannot see may bind any
            // name again.
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
                    Infer sub = p.modules.get(from + "." + a.name);
                    if (sub != null && !m.moduleScope.assigned.contains(a.name) && stable(n)) {
                        // `from lxml import etree`: a submodule the package
                        // binds nothing over is that module, as `import
                        // lxml.etree as etree` would bind it.
                        moduleNames.put(n, sub);
                    } else if (!"*".equals(a.name) && stable(n) && (!star.contains(n) || sameStarBinding(n, m, a.name))) {
                        importedNames.put(n, new Extern(m, a.name));
                    }
                }
            }
        }
    }

    private Set<String> starBoundCache;
    private boolean starBoundDone;

    /// Every name this module's star imports may bind, or null when one of
    /// them is of a module the build cannot see, or exports names it cannot
    /// enumerate. A name outside it is bound by the module's own statements
    /// alone, as in a module with no star import.
    private Set<String> starBound() {
        if (!starBoundDone) {
            starBoundDone = true;
            Set<String> r = new HashSet<String>();
            for (Stmt st : body) {
                Infer m = starSource(st);
                Set<String> e = m == this ? null : m == null ? r : m.starExports(0); //NOPMD CompareObjectsWithEquals - identity is meant
                if (e == null) {
                    r = null;
                    break;
                }
                if (e != r) { //NOPMD CompareObjectsWithEquals - identity is meant
                    r.addAll(e);
                }
            }
            starBoundCache = r;
        }
        return starBoundCache;
    }

    /// The module `from m import *` statement `st` imports, null when `st`
    /// is no star import, and this module when it is of a module the build
    /// cannot see.
    private Infer starSource(Stmt st) {
        if (!(st instanceof ImportFrom) || !starImport((ImportFrom) st)) {
            return null;
        }
        ImportFrom f = (ImportFrom) st;
        String from = f.level == 0 ? f.module : relative(f.level, f.module);
        Infer m = from == null ? null : p.modules.get(from);
        return m == null ? this : m;
    }

    /// Whether every star import here that binds `n` binds it to the class
    /// `from m import name` binds: openpyxl's chart modules import
    /// `Serialisable` and then star-import modules that import it too.
    private boolean sameStarBinding(String n, Infer m, String name) {
        Infer home = n.equals(name) ? m.classSource(name, 0) : null;
        if (home == null) {
            return false;
        }
        for (Stmt st : body) {
            Infer src = starSource(st);
            if (src != null && src.starExports(0).contains(n) && src.classSource(n, 0) != home) { //NOPMD CompareObjectsWithEquals - identity is meant
                return false;
            }
        }
        return true;
    }

    /// The module defining the class global `n` holds, read from the
    /// statements alone (imports need not be bound yet): a class bound once
    /// here, or one `from m import n` imports, followed; or null.
    private Infer classSource(String n, int depth) {
        if (depth > 8 || !stable(n)) {
            return null;
        }
        for (Stmt st : body) {
            if (st instanceof ClassDef && ((ClassDef) st).name.equals(n)) {
                return moduleScope.hasStarImport && classAfterStars(n) == null ? null : this;
            }
            if (st instanceof ImportFrom && !starImport((ImportFrom) st)) {
                ImportFrom f = (ImportFrom) st;
                for (Alias a : f.names) {
                    if (n.equals(a.asname != null ? a.asname : a.name)) {
                        String from = f.level == 0 ? f.module : relative(f.level, f.module);
                        Infer m = from == null ? null : p.modules.get(from);
                        return m == null || p.modules.containsKey(from + "." + a.name) ? null : m.classSource(a.name, depth + 1);
                    }
                }
            }
        }
        return null;
    }

    /// The module whose star import alone binds global `n` here: openpyxl's
    /// `Typed`, which `openpyxl.descriptors` has from `from .base import *`.
    /// Null when the module binds `n` itself, or a star import of a module
    /// the build cannot see might.
    private Infer starProvider(String n) {
        if (!moduleScope.hasStarImport || moduleScope.assigned.contains(n) || starBound() == null || !starBound().contains(n)) {
            return null;
        }
        Infer last = null;
        for (Stmt st : body) {
            Infer m = starSource(st);
            if (m != null && m.starExports(0).contains(n)) {
                last = m;
            }
        }
        return last;
    }

    /// The names `from <this module> import *` binds: a literal `__all__`
    /// bound once, or else every public global, its own star imports' too;
    /// null when they cannot be enumerated.
    private Set<String> starExports(int depth) {
        if (depth > 8) {
            return null;
        }
        if (moduleScope.assigned.contains("__all__")) {
            if (!stable("__all__")) {
                return null;
            }
            for (Stmt st : body) {
                if (st instanceof Assign && ((Assign) st).targets.size() == 1 && ((Assign) st).targets.get(0) instanceof Name
                        && "__all__".equals(((Name) ((Assign) st).targets.get(0)).id)) {
                    Expr v = ((Assign) st).value;
                    List<Expr> elts = v instanceof ListExpr ? ((ListExpr) v).elts : v instanceof TupleExpr ? ((TupleExpr) v).elts : null;
                    if (elts == null) {
                        return null;
                    }
                    Set<String> r = new HashSet<String>();
                    for (Expr x : elts) {
                        if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.STR) {
                            return null;
                        }
                        r.add(((Constant) x).text);
                    }
                    return r;
                }
            }
            return null;
        }
        Set<String> r = new HashSet<String>();
        for (String n : moduleScope.assigned) {
            if (!n.startsWith("_")) {
                r.add(n);
            }
        }
        for (Stmt st : body) {
            Infer m = starSource(st);
            if (m == this) { //NOPMD CompareObjectsWithEquals - identity is meant
                return null;
            }
            if (m != null) {
                Set<String> e = m.starExports(depth + 1);
                if (e == null) {
                    return null;
                }
                for (String n : e) {
                    if (!n.startsWith("_")) {
                        r.add(n);
                    }
                }
            }
        }
        return r;
    }

    /// Makes live what an import statement reached code runs imports.
    private void liveImports(Stmt s) {
        if (p.allLive || typeCheckingDepth > 0) {
            return;
        }
        if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                p.markLive(a.name);
            }
            return;
        }
        ImportFrom f = (ImportFrom) s;
        String base = f.level == 0 ? f.module : relative(f.level, f.module);
        if (base == null) {
            return;
        }
        p.markLive(base);
        for (Alias a : f.names) {
            if ("*".equals(a.name)) {
                // __all__ may name submodules, which the import then runs.
                p.markPrefix(base + ".");
            } else {
                p.markLive(base + "." + a.name);
            }
        }
    }

    /// How many `if TYPE_CHECKING:` blocks the walk is inside.
    private int typeCheckingDepth;

    /// Whether `test` is typing's `TYPE_CHECKING`, which is False when the
    /// program runs: the name imported from typing (or typing_extensions)
    /// and never rebound, or the attribute read off the module.
    private boolean typeChecking(Expr test) {
        if (test instanceof Name && "TYPE_CHECKING".equals(((Name) test).id)) {
            Extern e = importedNames.get("TYPE_CHECKING");
            if (e != null) {
                return "TYPE_CHECKING".equals(e.name) && typingModule(e.module.moduleName);
            }
            // A module with a star import records no import bindings (see
            // bindImports); the one binding is then read off the body. A
            // star import that bound it again would bind typing's False.
            if (!stable("TYPE_CHECKING")) {
                return false;
            }
            for (Stmt st : body) {
                if (st instanceof ImportFrom && ((ImportFrom) st).level == 0
                        && typingModule(((ImportFrom) st).module)) {
                    for (Alias a : ((ImportFrom) st).names) {
                        if ("TYPE_CHECKING".equals(a.name) && a.asname == null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        if (test instanceof Attribute && "TYPE_CHECKING".equals(((Attribute) test).attr)
                && ((Attribute) test).value instanceof Name) {
            String n = ((Name) ((Attribute) test).value).id;
            Infer m = moduleNames.get(n);
            if (m != null) {
                return typingModule(m.moduleName);
            }
            if (!typingModule(n) || !stable(n)) {
                return false;
            }
            for (Stmt st : body) {
                if (st instanceof Import) {
                    for (Alias a : ((Import) st).names) {
                        if (n.equals(a.name) && a.asname == null) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        return false;
    }

    private static boolean typingModule(String name) {
        return "typing".equals(name) || "typing_extensions".equals(name);
    }

    /// The parts of this module [#scanStrings] has read: statements of
    /// the body and functions, each once.
    private final Set<String> pendingExact = new LinkedHashSet<String>();
    private final Set<String> pendingPrefixes = new LinkedHashSet<String>();
    /// Whether reached code of this module imports by a computed name.
    private boolean importsByName;
    private final Set<Object> stringsSeen = java.util.Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    /// What a computed import in this module may name: every string that
    /// is a module's name (absolute, or relative with leading dots), and
    /// the constant start of a formatted string or a concatenation, which
    /// keeps every module under it. Only code that runs is read: the body,
    /// and each function once it is reached. Called again every round,
    /// it reads what was reached since.
    void scanStrings() {
        StringScan v = new StringScan(p, stringsSeen);
        v.on(body);
        pendingExact.addAll(v.exact);
        pendingPrefixes.addAll(v.prefixes);
        importsByName |= v.importsByName;
        // A string names a module to import only where the module imports
        // by a name it computes, or (the entry points importlib.metadata
        // loads) in the table of installed distributions.
        boolean applies = importsByName || "_cn1_distributions".equals(moduleName)
                && p.modules.containsKey("importlib.metadata") && p.live.contains(p.modules.get("importlib.metadata"));
        if (!applies) {
            return;
        }
        List<String> exact = new ArrayList<String>(pendingExact);
        List<String> prefixes = new ArrayList<String>(pendingPrefixes);
        pendingExact.clear();
        pendingPrefixes.clear();
        for (String t : exact) {
            for (String tok : tokens(t)) {
                String n = absolute(tok);
                if (n != null && p.modules.containsKey(n)) {
                    p.markLive(n);
                }
            }
        }
        for (String t : prefixes) {
            List<String> toks = tokens(t);
            if (toks.isEmpty() || !t.endsWith(toks.get(toks.size() - 1))) {
                continue;
            }
            String n = absolute(toks.get(toks.size() - 1));
            if (n != null && n.indexOf('.') > 0) {
                p.markPrefix(n);
            }
        }
    }

    /// Gathers the strings [#scanStrings] reads.
    private static final class StringScan implements Visit {
        final List<String> exact = new ArrayList<String>();
        final List<String> prefixes = new ArrayList<String>();
        boolean importsByName;
        private final Program p;
        private final Set<Object> seen;

        StringScan(Program p, Set<Object> seen) {
            this.p = p;
            this.seen = seen;
        }

        @Override
        public void on(Object o) {
            if (o instanceof Stmt || o instanceof Lambda) {
                if (seen.contains(o) || o instanceof FunctionDef && p.fns.containsKey(o) && !p.reached.contains(o)) {
                    // Read already, or a function nothing reaches yet.
                    return;
                }
                if (o instanceof FunctionDef) {
                    seen.add(o);
                }
            }
            if (o instanceof Name && importer(((Name) o).id)
                    || o instanceof Attribute && importer(((Attribute) o).attr)) {
                importsByName = true;
            }
            if (o instanceof Call && ((Call) o).args.size() == 2 && isStr(((Call) o).args.get(0))
                    && isStr(((Call) o).args.get(1))) {
                // import_module(".name", "package"): relative to the
                // package named, not to this module.
                String rel = ((Constant) ((Call) o).args.get(0)).text;
                String base = ((Constant) ((Call) o).args.get(1)).text;
                int level = 0;
                while (level < rel.length() && rel.charAt(level) == '.') {
                    level++;
                }
                for (int i = 1; i < level && base.lastIndexOf('.') > 0; i++) {
                    base = base.substring(0, base.lastIndexOf('.'));
                }
                if (level > 0) {
                    exact.add(level == rel.length() ? base : base + "." + rel.substring(level));
                }
            }
            if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
                exact.add(((Constant) o).text);
            } else if (o instanceof JoinedStr && !((JoinedStr) o).parts.isEmpty()
                    && ((JoinedStr) o).parts.get(0) instanceof Constant) {
                prefixes.add(((Constant) ((JoinedStr) o).parts.get(0)).text);
            } else if (o instanceof BinOp && "+".equals(((BinOp) o).op) && ((BinOp) o).left instanceof Constant
                    && ((Constant) ((BinOp) o).left).kind == Constant.Kind.STR) {
                prefixes.add(((Constant) ((BinOp) o).left).text);
            }
            children(o, this);
        }
    }

    /// The names through which Python code imports by a computed name:
    /// importlib's, the builtin, and the registry's own (`_cn1imp.load`).
    private static boolean importer(String n) {
        return "import_module".equals(n) || "__import__".equals(n) || "_cn1imp".equals(n);
    }

    private static boolean isStr(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR;
    }

    /// A dotted token of a string resolved as an import of this module
    /// would resolve it: leading dots climb from its package.
    private String absolute(String tok) {
        int level = 0;
        while (level < tok.length() && tok.charAt(level) == '.') {
            level++;
        }
        if (level == 0) {
            return tok;
        }
        String rest = tok.substring(level);
        return relative(level, rest.isEmpty() ? null : rest);
    }

    /// The runs of identifier characters and dots in `s`.
    private static List<String> tokens(String s) {
        List<String> out = new ArrayList<String>();
        int start = -1;
        for (int i = 0; i <= s.length(); i++) {
            char c = i < s.length() ? s.charAt(i) : ' ';
            boolean id = c == '.' || c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
            if (id && start < 0) {
                start = i;
            } else if (!id && start >= 0) {
                out.add(s.substring(start, i));
                start = -1;
            }
        }
        return out;
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
        if (g != null && g.module == this) { //NOPMD CompareObjectsWithEquals - identity is meant
            // `from . import m` in a package's own `__init__`: its module m,
            // not the name m of the package, which this import is.
            Infer sub = p.modules.get(moduleName.isEmpty() ? g.name : moduleName + "." + g.name);
            if (sub != null) {
                return sub;
            }
        }
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
        if (e instanceof Name && sc.resolve(((Name) e).id) == Scope.Binding.LOCAL) {
            return localModule(((Name) e).id, sc);
        }
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

    /// The module a local of function scope `sc` names: one bound once in
    /// the body, by an `import` of a module the program has, and nowhere
    /// else (`import functools as _ft` inside typing_extensions'
    /// `deprecated.__call__`). Null otherwise.
    private Infer localModule(String n, Scope sc) {
        Map<String, Infer> m = localModules.get(sc);
        if (m == null) {
            m = new HashMap<String, Infer>();
            localModules.put(sc, m);
            Fn f = p.fnOf(sc);
            if (f != null) {
                List<String> bound = new ArrayList<String>();
                Map<String, Infer> imported = new HashMap<String, Infer>();
                for (Stmt s : f.def.body) {
                    boundIn(s, bound);
                    collectImports(s, imported);
                }
                for (Map.Entry<String, Infer> en : imported.entrySet()) {
                    String k = en.getKey();
                    if (bound.indexOf(k) == bound.lastIndexOf(k) && !sc.params.contains(k)
                            && !sc.globals.contains(k) && !sc.nonlocals.contains(k) && !rebound(sc, k)) {
                        m.put(k, en.getValue());
                    }
                }
            }
        }
        return m.get(n);
    }

    /// Whether a scope nested in `sc` stores into its `n` (`nonlocal n`).
    private static boolean rebound(Scope sc, String n) {
        for (Scope c : sc.children) {
            if (c.nonlocals.contains(n) || !c.assigned.contains(n) && rebound(c, n)) {
                return true;
            }
        }
        return false;
    }

    /// The modules the `import` statements under `o` bind, by local name,
    /// outside nested scopes.
    private void collectImports(Object o, Map<String, Infer> out) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda || o instanceof Comp) {
            return;
        }
        if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                int dot = a.name.indexOf('.');
                String n = a.asname != null ? a.asname : dot < 0 ? a.name : a.name.substring(0, dot);
                Infer im = p.modules.get(a.asname != null || dot < 0 ? a.name : n);
                if (im != null) {
                    out.put(n, im);
                }
            }
            return;
        }
        children(o, x -> collectImports(x, out));
    }

    /// `t` as this module's code holds it. An instance of a class another
    /// module compiled is typed like one of this module's: typed code names
    /// that class by its qualified Java name and reads its typed fields as
    /// their own module holds them (TypedEmit.typedField). Only a class of a
    /// module nothing live imports, which is never emitted, is held as any
    /// object. bs4's `Tag` is compiled in `bs4.element` and walked by
    /// `bs4.filter`, `bs4.builder` and markdownify: held as any object there,
    /// every `tag.name` went through an attribute cache.
    private Ty loc(Ty t) {
        if (!p.done || t == null) {
            return t;
        }
        if (t.k == Ty.K.UNION) {
            for (Ty m : t.members()) {
                if (loc(m).k == Ty.K.OBJECT) {
                    return Ty.OBJECT;
                }
            }
            return t;
        }
        if (t.k == Ty.K.CLS) {
            for (int s : t.sites) {
                if (!p.clsById.get(s).compiled) {
                    return Ty.OBJECT;
                }
            }
            return t;
        }
        if (t.k == Ty.K.METH) {
            for (int s : t.sites) {
                if (p.methById.get(s).analysisOnly) {
                    return Ty.OBJECT;
                }
            }
            return t;
        }
        if (t.k == Ty.K.FUNC) {
            // The inference's own: generated code holds a function as an
            // object, and calls it generically.
            return Ty.OBJECT;
        }
        if (t.k != Ty.K.INST) {
            return t;
        }
        Infer h = p.home.get(t.cls);
        // An analysed class has no Java class: generated code holds its
        // instances as objects.
        return h == null || !p.isLive(h) || !t.cls.compiled ? Ty.OBJECT : t;
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
        for (Layouts.Cls k : layouts.all().values()) {
            for (Map.Entry<String, Ty> e : fields.get(k).entrySet()) {
                e.setValue(loc(e.getValue()));
            }
        }
        for (Map.Entry<String, Ty> e : globals.entrySet()) {
            e.setValue(loc(e.getValue()));
        }
    }

    // ---- setup -----------------------------------------------------------

    /// How often each global is bound, which decides what the imports may
    /// follow ([#stable]); before any module's classes are collected.
    private void countGlobals(List<Stmt> body) {
        countBindings(body);
        for (String n : moduleScope.assigned) {
            // A global another module stores (`mod.n = v`), or any global
            // of a module that hands its namespace out as a dict, is bound
            // more often than this module shows and holds anything.
            if (facts.namespace || facts.patched(n)) {
                moduleBindings.put(n, Integer.valueOf(99));
                globals.put(n, Ty.OBJECT);
            }
        }
    }

    /// Every function nested in another, and every lambda, gets a Fn of
    /// the inference's own (see [#nested]).
    private void nestedFns(Object o, boolean inFunction) {
        if (o instanceof Lambda) {
            Lambda l = (Lambda) o;
            if (l.scope != null && !lambdaDefs.containsKey(l)) {
                List<Stmt> body = new ArrayList<Stmt>();
                Return r = new Return(l.body);
                r.line = l.line;
                body.add(r);
                FunctionDef d = new FunctionDef("<lambda>", l.args, body, new ArrayList<Expr>(), null, false);
                d.scope = l.scope;
                d.line = l.line;
                lambdaDefs.put(l, d);
                addNested(d);
            }
        } else if (o instanceof FunctionDef && inFunction && !fns.containsKey(o)) {
            addNested((FunctionDef) o);
        }
        boolean in = o instanceof FunctionDef || o instanceof Lambda || inFunction && !(o instanceof ClassDef);
        children(o, x -> nestedFns(x, in));
    }

    private void addNested(FunctionDef d) {
        if (d.scope == null || !doorOnly(d)) {
            return;
        }
        compCells.put(d.scope, compOnlyCells(d));
        Fn fn = new Fn(d, null);
        fn.analysisOnly = true;
        starParams(fn);
        fns.put(d, fn);
        p.fns.put(d, fn);
        p.fnHome.put(fn, this);
    }

    private final Map<Lambda, FunctionDef> lambdaDefs = new IdentityHashMap<Lambda, FunctionDef>();

    private void collect(List<Stmt> body) {
        intConstants(body);
        nestedFns(body, false);
        for (Layouts.Cls k : layouts.all().values()) {
            // A method a store may replace is looked up per call, as one
            // the class's own methods store on `self` is (Layouts.plan):
            // one named to setattr or deleted, and one an earlier run of
            // the pass found a store reaching (Program#patchedFound).
            Set<String> pk = p.patched.get(k.def);
            for (java.util.Iterator<String> it = k.typable.keySet().iterator(); it.hasNext();) {
                String pn = it.next();
                if (facts.methodPatched(pn) || pk != null && pk.contains(pn)) {
                    it.remove();
                }
            }
        }
        for (Layouts.Cls k : layouts.all().values()) {
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
                // module puts there, which a pass without that module does
                // not see.
                fields.get(k).put(f, !p.closed && facts.foreign(f) ? Ty.OBJECT : Ty.BOT);
            }
            for (FunctionDef f : k.typable.values()) {
                compCells.put(f.scope, compOnlyCells(f));
                Fn fn = new Fn(f, k);
                fn.analysisOnly = !k.compiled;
                fn.params[0] = Ty.inst(k, false);
                fns.put(f, fn);
                p.fns.put(f, fn);
                p.fnHome.put(fn, this);
            }
            for (Stmt st : k.def.body) {
                // Every other method with a fixed parameter list -- a
                // property, a class or static method, one shadowed or
                // defined twice -- gets a typed body too, entered only by the
                // door in its generic body, which checks every argument: no
                // call site binds it directly, so what the decorator or the
                // lookup does with it changes nothing.
                if (st instanceof FunctionDef && !fns.containsKey(st) && doorOnly((FunctionDef) st)
                        && !((FunctionDef) st).args.names().isEmpty()) {
                    FunctionDef f = (FunctionDef) st;
                    compCells.put(f.scope, compOnlyCells(f));
                    Fn fn = new Fn(f, k);
                    fn.analysisOnly = !k.compiled;
                    // `__new__` takes the class: what each construction
                    // passes it ([#callKnown]).
                    fn.params[0] = "__new__".equals(f.name) ? Ty.BOT : plainDecorators(f) ? Ty.inst(k, false) : Ty.OBJECT;
                    starParams(fn);
                    fns.put(f, fn);
                    p.fns.put(f, fn);
                    p.fnHome.put(fn, this);
                }
            }
        }
        for (Stmt s : body) {
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                Set<String> cc = compOnlyCells(f);
                if (stable(f.name) && f.decorators.isEmpty() && !f.isAsync
                        && f.args.vararg == null && f.args.kwarg == null
                        && f.args.names().size() <= Layouts.MAX_TYPED_PARAMS) {
                    compCells.put(f.scope, cc);
                    Fn fn = new Fn(f, null);
                    fns.put(f, fn);
                    p.fns.put(f, fn);
                    p.fnHome.put(fn, this);
                    globalFns.put(f.name, fn);
                } else if (doorOnly(f)) {
                    // Rebound or decorated: a typed body behind its door only.
                    compCells.put(f.scope, cc);
                    Fn fn = new Fn(f, null);
                    starParams(fn);
                    fns.put(f, fn);
                    p.fns.put(f, fn);
                    p.fnHome.put(fn, this);
                }
            } else if (s instanceof ClassDef) {
                Layouts.Cls k = layouts.all().get(s);
                if (k != null && stable(((ClassDef) s).name)) {
                    globalClasses.put(((ClassDef) s).name, k);
                }
            } else if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    if ("math".equals(a.name) && stable(n)) {
                        mathNames.add(n);
                    }
                    String root = a.asname != null ? a.name : a.name.split("\\.")[0];
                    if (("time".equals(root) || "os".equals(root)) && stable(n)) {
                        nativeMods.put(n, root);
                    }
                    if ("struct".equals(a.name) && stable(n)) {
                        structNames.add(n);
                    }
                }
            }
        }
        markOpen(body);
        for (Layouts.Cls k : layouts.all().values()) {
            definite.put(k, initDefinite(k, new HashSet<Layouts.Cls>()));
        }
    }

    /// Whether `f` may have a typed body entered only by its door: a
    /// bounded number of parameters of any kind (the door hands over what the generic
    /// body bound, `*args` and `**kwargs` included), and no cell a closure
    /// shares.
    private static boolean doorOnly(FunctionDef f) {
        // A Java method takes at most 255 slots, and an int parameter
        // takes three (a long and its object).
        return !f.isAsync && f.args.names().size() <= 60;
    }

    /// A door-only function's `*args` and `**kwargs`: a tuple and a dict
    /// the generic body made, of no type a call site settles.
    private static void starParams(Fn fn) {
        Arguments a = fn.def.args;
        if (a.vararg != null) {
            fn.params[fn.names.indexOf(a.vararg.name)] = Ty.OBJECT;
        }
        if (a.kwarg != null) {
            fn.params[fn.names.indexOf(a.kwarg.name)] = Ty.OBJECT;
        }
    }

    /// Whether every decorator of method `f` hands the function the instance
    /// as its first argument: none, a property's getter, setter or deleter,
    /// and the descriptors modelled on it. With any other the first parameter
    /// is typed unknown, and the door checks it like any other.
    private static boolean plainDecorators(FunctionDef f) {
        for (Expr d : f.decorators) {
            boolean ok = d instanceof Name && ("property".equals(((Name) d).id) || PROPERTY_ALIAS.equals(((Name) d).id) || "lazyproperty".equals(((Name) d).id)
                    || "cached_property".equals(((Name) d).id))
                    || d instanceof Attribute && ("setter".equals(((Attribute) d).attr)
                    || "deleter".equals(((Attribute) d).attr));
            if (!ok) {
                return false;
            }
        }
        return true;
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

    /// The type a call of a [#NATIVE_RET] function answers with these
    /// argument types, or null when `c` is not one or an argument is not a
    /// plain `str`.
    Ty nativeRet(Call c, List<Ty> args, Scope sc) {
        if (!c.keywords.isEmpty() || anyStarred(c.args) || c.args.size() > 2) {
            return null;
        }
        StringBuilder d = new StringBuilder();
        Expr e = c.func;
        while (e instanceof Attribute) {
            d.insert(0, "." + ((Attribute) e).attr);
            e = ((Attribute) e).value;
        }
        if (!(e instanceof Name) || sc.resolve(((Name) e).id) != Scope.Binding.GLOBAL) {
            return null;
        }
        String m = nativeMods.get(((Name) e).id);
        Ty r = m == null ? null : NATIVE_RET.get(m + d);
        if (r == null) {
            return null;
        }
        for (Ty t : args) {
            if (t.k != Ty.K.STR || t.opt) {
                return null;
            }
        }
        if (r.opt && args.size() == 2) {
            return Ty.STR;
        }
        return r.k == Ty.K.FLOAT ? (args.isEmpty() ? r : null) : args.isEmpty() && !"os.getcwd".equals(m + d) ? null : r;
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
    private final Set<String> topImported = new HashSet<String>();

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
                    String top = dot < 0 ? n : n.substring(0, dot);
                    // `import xml.sax` then `import xml.sax.handler`: each
                    // binds `xml` to the one package, so it is one binding.
                    if (a.asname != null || topImported.add(top)) {
                        names.add(top);
                    }
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

    /// Whether function `f` binds `n` itself -- a parameter, or a store
    /// in its body outside nested scopes -- and declares it neither
    /// `global` nor `nonlocal`. For passes that run before the scopes are
    /// built.
    static boolean bindsLocally(FunctionDef f, String n) {
        if (f.args.names().contains(n)) {
            return true;
        }
        List<String> out = new ArrayList<String>();
        boolean[] declared = new boolean[1];
        for (Stmt s : f.body) {
            boundIn(s, out);
            declaresIn(s, n, declared);
        }
        return out.contains(n) && !declared[0];
    }

    private static void declaresIn(Object o, String n, boolean[] r) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda || o instanceof Comp) {
            return;
        }
        if (o instanceof Global && ((Global) o).names.contains(n)) {
            r[0] = true;
        }
        children(o, x -> declaresIn(x, n, r));
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

    /// Whether a top-level class here the planner rejected has one base
    /// that another module defines.
    boolean mayExtendForeign() {
        for (Stmt s : body) {
            // Any base may be another module's: the compiled parent, a
            // mixin ([Layouts#foreignMixin]) or the class after a builtin
            // first base (`class BaseEnum(int, enum.Enum)`).
            if (s instanceof ClassDef && !((ClassDef) s).decorators.isEmpty() && layouts.refused.containsKey(s)) {
                // A decorated class is planned again once the globals are
                // known for good: a decorator that answers its argument
                // (Layouts#returnsItsClass) keeps the layout.
                return true;
            }
            if (s instanceof ClassDef && !layouts.classes.containsKey(s)) {
                for (Expr b : ((ClassDef) s).bases) {
                    Infer h = classHome(b);
                    if (h != null && h != this) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Why the build found no class of another module that base `b` names
    /// for good, for the strict report; null when it found one (the base
    /// then has no layout of its own) or `b` is not a module's name.
    private String noHomeWhy(Expr base) {
        Expr b = Layouts.origin(base);
        Infer m;
        String n;
        if (b instanceof Attribute) {
            m = moduleOf(((Attribute) b).value, 0);
            n = ((Attribute) b).attr;
            if (m == null) {
                return Layouts.text(((Attribute) b).value) + " is not a module the build resolves for good";
            }
        } else if (b instanceof Name) {
            Extern g = moduleScope.resolve(((Name) b).id) == Scope.Binding.GLOBAL ? importedNames.get(((Name) b).id) : null;
            if (g == null) {
                return null;
            }
            m = g.module;
            n = g.name;
        } else {
            return null;
        }
        if (m.definer(n, 0) != null) {
            return null;
        }
        if (!m.moduleScope.assigned.contains(n)) {
            return m.moduleName + " binds no global " + n;
        }
        Integer c = m.moduleBindings.get(n);
        if (c != null && c.intValue() == 99) {
            return m.facts.namespace ? m.moduleName + " hands its namespace out, so " + n + " may be rebound"
                    : m.moduleName + "." + n + " may be rebound: the program stores an attribute named " + n
                    + " on a value the build cannot tell from the module";
        }
        if (c != null && c.intValue() > 1) {
            return m.moduleName + " binds " + n + " " + c + " times";
        }
        return m.moduleName + "." + n + " is not a class the build can follow there";
    }

    /// Whether a base [#mayExtendForeign] found is in a module still to be
    /// planned again.
    boolean waitsOn(Set<Infer> waiting) {
        for (Stmt s : body) {
            if (s instanceof ClassDef) {
                for (Expr b : ((ClassDef) s).bases) {
                    Infer h = classHome(b);
                    if (h != null && h != this && waiting.contains(h)) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// The module whose body defines the class base expression `b` names,
    /// by the same rules as [#foreignClass], or null.
    private Infer classHome(Expr base) {
        Expr b = Layouts.origin(base);
        if (b instanceof Name) {
            String n = ((Name) b).id;
            Extern g = moduleScope.resolve(n) == Scope.Binding.GLOBAL ? importedNames.get(n) : null;
            if (g == null) {
                // A star import binds it, as in [#foreignClass].
                Infer sp = starProvider(n);
                return sp == null ? null : sp.definer(n, 0);
            }
            return g.module.definer(g.name, 0);
        }
        if (b instanceof Attribute) {
            Infer m = moduleOf(((Attribute) b).value, 0);
            return m == null ? null : m.definer(((Attribute) b).attr, 0);
        }
        return null;
    }

    /// The module of the program expression `e` names for good, or null: a
    /// global an `import` binds to a module, or a dotted attribute of one
    /// (`xml.sax.handler` in `class X(xml.sax.handler.ContentHandler)`)
    /// naming a submodule of the build whose package binds nothing over that
    /// name and whose name no statement of the program stores as an
    /// attribute, so the attribute is the submodule whenever it is read.
    private Infer moduleOf(Expr e, int depth) {
        if (e instanceof Name) {
            String n = ((Name) e).id;
            return moduleScope.resolve(n) == Scope.Binding.GLOBAL ? moduleNames.get(n) : null;
        }
        if (!(e instanceof Attribute) || depth > 8) {
            return null;
        }
        String a = ((Attribute) e).attr;
        Infer pkg = moduleOf(((Attribute) e).value, depth + 1);
        if (pkg == null || pkg.moduleScope.assigned.contains(a) || pkg.moduleScope.hasStarImport
                || facts.attrStored(a)) {
            return null;
        }
        return p.modules.get(pkg.moduleName + "." + a);
    }

    /// The top-level class `n` this module defines after its last star
    /// import, or null.
    private ClassDef classAfterStars(String n) {
        ClassDef d = null;
        for (Stmt s : body) {
            if (s instanceof ImportFrom && starImport((ImportFrom) s)) {
                d = null;
            } else if (s instanceof ClassDef && ((ClassDef) s).name.equals(n)) {
                d = (ClassDef) s;
            }
        }
        return d;
    }

    private static boolean starImport(ImportFrom s) {
        for (Node.Alias a : s.names) {
            if ("*".equals(a.name)) {
                return true;
            }
        }
        return false;
    }

    private Infer definer(String n, int depth) {
        Infer via = depth > 8 ? null : starProvider(n);
        if (via != null) {
            return via.definer(n, depth + 1);
        }
        if (depth > 8 || !moduleScope.assigned.contains(n) || !stable(n)) {
            return null;
        }
        if (moduleScope.hasStarImport) {
            // A star import may bind any name, but not over a class
            // defined after it: the definition binds the name last.
            ClassDef d = classAfterStars(n);
            if (d != null) {
                return this;
            }
            Extern si = importedNames.get(n);
            return si == null ? null : si.module.definer(si.name, depth + 1);
        }
        for (Stmt s : body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(n)) {
                return this;
            }
        }
        Expr alias = specialAliasExpr(n);
        if (alias != null) {
            return classHome(alias);
        }
        Extern g = importedNames.get(n);
        return g == null ? null : g.module.definer(g.name, depth + 1);
    }

    /// The compiled class of another module base expression `b` of a
    /// top-level class names, or null: an imported name, or an attribute
    /// of an imported module, bound once on each side. Needs the imports
    /// bound, and none of the classes collected.
    Layouts.Cls foreignClass(Expr base) {
        Expr b = Layouts.origin(base);
        if (b instanceof Name) {
            String n = ((Name) b).id;
            Extern g = moduleScope.resolve(n) == Scope.Binding.GLOBAL ? importedNames.get(n) : null;
            if (g == null) {
                // `from enum import *` then `class C(Enum)`: the last star
                // import exporting the name binds it, when the module binds
                // it nowhere itself ([#starProvider]).
                Infer sp = starProvider(n);
                return sp == null ? null : sp.exportedClass(n, 0);
            }
            return g.module.exportedClass(g.name, 0);
        }
        if (b instanceof Attribute) {
            Infer m = moduleOf(((Attribute) b).value, 0);
            return m == null ? null : m.exportedClass(((Attribute) b).attr, 0);
        }
        return null;
    }

    /// Whether global `n` of this module holds a constant literal for the
    /// whole run: bound once, here to a [#constantExpr] or by a from-import of such
    /// a global of another module, never stored as an attribute by name
    /// anywhere in the program, and no star import that could rebind it.
    /// openpyxl's `Relation(String)` binds `namespace = REL_NS`, imported
    /// from `openpyxl.xml.constants` ([Layouts#plainDefault]).
    /// Whether global `n` is bound once, at the top level of this module,
    /// and no module stores it as an attribute or through a namespace.
    boolean fixedGlobal(String n) {
        return moduleScope.resolve(n) == Scope.Binding.GLOBAL && !moduleScope.hasStarImport
                && moduleScope.assigned.contains(n) && stable(n) && !facts.attrStored(n);
    }

    boolean constantGlobal(String n) {
        return moduleScope.resolve(n) == Scope.Binding.GLOBAL && constantGlobal(n, 0);
    }

    private boolean constantGlobal(String n, int depth) {
        if (depth > 8 || moduleScope.hasStarImport || !moduleScope.assigned.contains(n) || !stable(n)
                || facts.attrStored(n)) {
            return false;
        }
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && ((Name) ((Assign) s).targets.get(0)).id.equals(n)) {
                return constantExpr(((Assign) s).value, depth);
            }
        }
        Extern g = importedNames.get(n);
        return g != null && g.module.constantGlobal(g.name, depth + 1);
    }

    /// The one string `e` is at build time: a str constant, a local
    /// [#constString], a [#constantGlobal] of this module bound to such a
    /// string, or `+` of two such; null when it is not one known string.
    String foldedString(Expr e, Fn fn, Scope sc, int depth) {
        if (depth > 8) {
            return null;
        }
        String one = constString(e, fn, sc);
        if (one != null) {
            return one;
        }
        if (e instanceof BinOp && "+".equals(((BinOp) e).op)) {
            String l = foldedString(((BinOp) e).left, fn, sc, depth + 1);
            String r = l == null ? null : foldedString(((BinOp) e).right, fn, sc, depth + 1);
            return r == null ? null : l + r;
        }
        if (e instanceof Call) {
            return foldedCall((Call) e, fn, sc, depth);
        }
        if (!(e instanceof Name) || sc == null || sc.resolve(((Name) e).id) != Scope.Binding.GLOBAL
                || !constantGlobal(((Name) e).id)) {
            return null;
        }
        String n = ((Name) e).id;
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && isName(((Assign) s).targets.get(0), n)) {
                return foldedString(((Assign) s).value, null, moduleScope, depth + 1);
            }
        }
        return null;
    }

    /// `g(a, b)`, a call of a function this module defines once at its top
    /// level, undecorated, whose whole body returns a string made of its
    /// parameters, literals, `+` and `"sep".join(args)` of its `*args`,
    /// with arguments that fold to strings: the string it returns, computed
    /// at build time. Null for anything else.
    private String foldedCall(Call c, Fn fn, Scope sc, int depth) {
        if (!(c.func instanceof Name) || !c.keywords.isEmpty() || sc == null
                || sc.resolve(((Name) c.func).id) != Scope.Binding.GLOBAL || !stable(((Name) c.func).id)) {
            return null;
        }
        String g = ((Name) c.func).id;
        FunctionDef d = null;
        for (Stmt s : body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(g)) {
                d = (FunctionDef) s;
            }
        }
        if (d == null || !d.decorators.isEmpty() || d.isAsync || d.body.size() != 1 || !(d.body.get(0) instanceof Return)
                || ((Return) d.body.get(0)).value == null || !d.args.kwonly.isEmpty() || d.args.kwarg != null
                || !d.args.defaults.isEmpty() || c.args.size() < d.args.positional.size()
                || d.args.vararg == null && c.args.size() != d.args.positional.size()) {
            return null;
        }
        Map<String, String> env = new HashMap<String, String>();
        List<String> rest = new ArrayList<String>();
        for (int i = 0; i < c.args.size(); i++) {
            if (c.args.get(i) instanceof Starred) {
                return null;
            }
            String v = foldedString(c.args.get(i), fn, sc, depth + 1);
            if (v == null) {
                return null;
            }
            if (i < d.args.positional.size()) {
                env.put(d.args.positional.get(i).name, v);
            } else {
                rest.add(v);
            }
        }
        return evalString(((Return) d.body.get(0)).value, env, d.args.vararg == null ? null : d.args.vararg.name, rest);
    }

    private static String evalString(Expr e, Map<String, String> env, String vararg, List<String> rest) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR) {
            return ((Constant) e).text;
        }
        if (e instanceof Name) {
            return env.get(((Name) e).id);
        }
        if (e instanceof BinOp && "+".equals(((BinOp) e).op)) {
            String l = evalString(((BinOp) e).left, env, vararg, rest);
            String r = l == null ? null : evalString(((BinOp) e).right, env, vararg, rest);
            return r == null ? null : l + r;
        }
        if (e instanceof Call && ((Call) e).func instanceof Attribute && ((Call) e).keywords.isEmpty()
                && ((Call) e).args.size() == 1 && vararg != null && isName(((Call) e).args.get(0), vararg)
                && "join".equals(((Attribute) ((Call) e).func).attr)
                && ((Attribute) ((Call) e).func).value instanceof Constant
                && ((Constant) ((Attribute) ((Call) e).func).value).kind == Constant.Kind.STR) {
            return String.join(((Constant) ((Attribute) ((Call) e).func).value).text, rest);
        }
        return null;
    }

    /// A literal, a [#constantGlobal] of this module, or `+` of two such
    /// (openpyxl's `REL_NS = DOC_NS + "relationships"`): its value is one
    /// immutable literal, computed once at import.
    private boolean constantExpr(Expr e, int depth) {
        if (e instanceof Constant) {
            return true;
        }
        if (e instanceof Name) {
            return constantGlobal(((Name) e).id, depth + 1);
        }
        return e instanceof BinOp && "+".equals(((BinOp) e).op) && depth < 8
                && constantExpr(((BinOp) e).left, depth + 1) && constantExpr(((BinOp) e).right, depth + 1);
    }

    /// The class statement of another module a base expression names, as
    /// [#foreignClass] finds it, whether or not it was compiled; or null.
    ClassDef foreignClassDef(Expr base) {
        Expr b = Layouts.origin(base);
        if (b instanceof Name) {
            String n = ((Name) b).id;
            Extern g = moduleScope.resolve(n) == Scope.Binding.GLOBAL ? importedNames.get(n) : null;
            return g == null ? null : g.module.exportedDef(g.name, 0);
        }
        if (b instanceof Attribute) {
            Infer m = moduleOf(((Attribute) b).value, 0);
            return m == null ? null : m.exportedDef(((Attribute) b).attr, 0);
        }
        return null;
    }

    private ClassDef exportedDef(String n, int depth) {
        Infer via = depth > 8 ? null : starProvider(n);
        if (via != null) {
            return via.exportedDef(n, depth + 1);
        }
        if (depth > 8 || !moduleScope.assigned.contains(n) || !stable(n)) {
            return null;
        }
        if (moduleScope.hasStarImport) {
            // A star import may bind any name, but not over a class
            // defined after it: the definition binds the name last.
            ClassDef d = classAfterStars(n);
            Extern si = d == null ? importedNames.get(n) : null;
            return si == null ? d : si.module.exportedDef(si.name, depth + 1);
        }
        for (Stmt s : body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(n)) {
                return (ClassDef) s;
            }
        }
        Extern g = importedNames.get(n);
        return g == null ? null : g.module.exportedDef(g.name, depth + 1);
    }

    private Layouts.Cls exportedClass(String n, int depth) {
        Infer via = depth > 8 ? null : starProvider(n);
        if (via != null) {
            return via.exportedClass(n, depth + 1);
        }
        if (depth > 8 || !moduleScope.assigned.contains(n) || !stable(n)) {
            return null;
        }
        if (moduleScope.hasStarImport) {
            // As [#exportedDef]: openpyxl's `Strict`, defined after
            // `from .base import *`, is the class the name holds.
            // A from-import [#bindImports] kept is the name's one binding:
            // no star import can bind it (openpyxl.descriptors' `from
            // .sequence import Sequence` beside `from .base import *`).
            ClassDef d = classAfterStars(n);
            Extern si = d == null ? importedNames.get(n) : null;
            return si != null ? si.module.exportedClass(si.name, depth + 1) : d == null ? null : layouts.all().get(d);
        }
        for (Stmt s : body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(n)) {
                return layouts.all().get(s);
            }
        }
        Layouts.Cls alias = specialAliasOrigin(n);
        if (alias != null) {
            return alias;
        }
        Extern g = importedNames.get(n);
        return g == null ? null : g.module.exportedClass(g.name, depth + 1);
    }

    /// `typing`'s `Mapping = _SpecialGenericAlias(_abc.Mapping, "Mapping", 2)`:
    /// the alias's `__mro_entries__` answers its origin alone, so a class
    /// deriving from `Mapping[K, V]` has the `collections.abc` class as its
    /// only base. The runtime confirms that base before it gives the
    /// subclass its layout (`bases[0] == parent`), so nothing is assumed.
    private Layouts.Cls specialAliasOrigin(String n) {
        Expr e = specialAliasExpr(n);
        return e == null ? null : foreignClass(e);
    }

    /// The origin expression (`_abc.Mapping`) of [#specialAliasOrigin], or null.
    private Expr specialAliasExpr(String n) {
        if (!"typing".equals(moduleName)) {
            return null;
        }
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && ((Name) ((Assign) s).targets.get(0)).id.equals(n) && ((Assign) s).value instanceof Call) {
                Call c = (Call) ((Assign) s).value;
                if (c.func instanceof Name && "_SpecialGenericAlias".equals(((Name) c.func).id) && !c.args.isEmpty()
                        && c.args.get(0) instanceof Attribute) {
                    return c.args.get(0);
                }
                return null;
            }
        }
        return null;
    }

    /// [#markOpen] for a base another module compiled: a class here the
    /// planner rejected (or one built inside a function) that derives from
    /// it makes it and its ancestors open just the same. Its instances run
    /// the compiled methods through the generic path with a `self` of no
    /// compiled class, which then reaches every field those methods store.
    /// Needs the imports bound, so it runs after them.
    void markOpenForeign(Object o) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            if (!layouts.classes.containsKey(c)) {
                Scope bsc = c.scope != null && c.scope.parent != null ? c.scope.parent : moduleScope;
                Scope at = c.scope != null && c.scope.parent != null ? c.scope.parent : moduleScope;
                // Only a class deriving from a class of the program can be
                // an instance of a compiled class's subclass: the compiled
                // classes its bases lead to take its names, followed through
                // bases the planner rejected too, in this module or another.
                // Only a base that cannot be followed may be any class, and
                // poisons its names everywhere. Exceptions, a str subclass
                // (bs4's NavigableString) and their own subclasses lead to
                // no compiled class; 2,090 such classes had put `name`,
                // `prefix` and every other name their bodies bind on the
                // program-wide list, and no `self.name` of any class -- bs4's
                // Tag among them -- was typed.
                Set<Layouts.Cls> anc = new LinkedHashSet<Layouts.Cls>();
                boolean known = true;
                for (Expr b : c.bases) {
                    known &= compiledBases(b, bsc, c, new HashSet<Object>(), anc);
                }
                if (known) {
                    for (Layouts.Cls k : anc) {
                        rejectedBelow(k, c.scope.assigned);
                    }
                } else {
                    p.rejectedAnywhere.addAll(c.scope.assigned);
                }
                for (Expr b : c.bases) {
                    Object f = foreign(b, at);
                    if (f instanceof Layouts.Cls) {
                        openWithAncestors((Layouts.Cls) f);
                        continue;
                    }
                    // Not a name the pass can follow (a module whose globals
                    // are not stable, an alias): every compiled class of the
                    // program by that name may be the base.
                    String n = b instanceof Name ? ((Name) b).id : b instanceof Attribute ? ((Attribute) b).attr : null;
                    if (n != null && !(b instanceof Name && byName(n) != null)) {
                        for (Layouts.Cls k : p.home.keySet()) {
                            if (k.def.name.equals(n)) {
                                openWithAncestors(k);
                            }
                        }
                    }
                }
            }
        }
        children(o, x -> markOpenForeign(x));
    }

    /// Adds to `out` every compiled class an instance of a class deriving
    /// from base `b` (written in scope `at` of this module) may be an
    /// instance of; false when that cannot be told.
    private boolean compiledBases(Expr b, Scope at, Stmt site, Set<Object> seen, Set<Layouts.Cls> out) {
        Expr e = b instanceof Subscript ? ((Subscript) b).value : b;
        if (e instanceof Name) {
            String n = ((Name) e).id;
            Scope.Binding bd = at.resolve(n);
            if (bd == Scope.Binding.BUILTIN) {
                return true;
            }
            if (bd != Scope.Binding.GLOBAL) {
                return false;
            }
            ClassDef d = classBinding(n, site);
            if (d != null) {
                return classBases(d, seen, out);
            }
            Extern g = importedNames.get(n);
            if (g != null && lastBinder(n, site) instanceof ImportFrom) {
                return followBase(foreign(e, at), seen, out)
                        || g.module.localClassBases(g.name, seen, out);
            }
            return followBase(foreign(e, at), seen, out);
        }
        if (e instanceof Attribute) {
            return followBase(foreign(e, at), seen, out);
        }
        return false;
    }

    private static boolean followBase(Object f, Set<Object> seen, Set<Layouts.Cls> out) {
        if (f instanceof Layouts.Cls) {
            out.add((Layouts.Cls) f);
            return true;
        }
        return f instanceof Extern && ((Extern) f).module.localClassBases(((Extern) f).name, seen, out);
    }

    /// [#compiledBases] for top-level name `n` of this module as another
    /// module imports it: the class statement that binds it last.
    private boolean localClassBases(String n, Set<Object> seen, Set<Layouts.Cls> out) {
        ClassDef d = classBinding(n, null);
        if (d != null) {
            return classBases(d, seen, out);
        }
        Extern g = importedNames.get(n);
        return g != null && lastBinder(n, null) instanceof ImportFrom && seen.add(g)
                && g.module.localClassBases(g.name, seen, out);
    }

    private boolean classBases(ClassDef d, Set<Object> seen, Set<Layouts.Cls> out) {
        Layouts.Cls k = layouts.all().get(d);
        if (k != null) {
            out.add(k);
            return true;
        }
        if (!seen.add(d)) {
            return true;
        }
        Scope bsc = d.scope != null && d.scope.parent != null ? d.scope.parent : moduleScope;
        for (Expr b : d.bases) {
            if (!compiledBases(b, bsc, d, seen, out)) {
                return false;
            }
        }
        return true;
    }

    /// The class statement of this module's top level that `n` names just
    /// before statement `upTo` runs (at the end of the module when `upTo`
    /// is null or not a top-level statement), or null when that is not one
    /// class statement or cannot be told. A name bound more than once
    /// still resolves: minidom imports `Node` and then defines its own
    /// `class Node`, which every later class derives from; openpyxl defines
    /// `Strict` after a star import. Unknowable when a function declares
    /// the name `global` or the module writes its namespace.
    private ClassDef classBinding(String n, Stmt upTo) {
        Stmt s = lastBinder(n, upTo);
        return s instanceof ClassDef ? (ClassDef) s : null;
    }

    /// The last top-level statement before `upTo` that may bind `n`; a star
    /// import counts, and a compound one stands for what it may bind.
    /// Null when there is none or it cannot be told.
    private Stmt lastBinder(String n, Stmt upTo) {
        Integer c = moduleBindings.get(n);
        if (c == null || c.intValue() >= 99 || facts.namespace) {
            return null;
        }
        Stmt last = null;
        for (Stmt s : body) {
            if (s == upTo) { //NOPMD CompareObjectsWithEquals - identity is meant
                break;
            }
            if (s instanceof ImportFrom && isStar((ImportFrom) s) || binds(s, n)) {
                last = s;
            }
        }
        return last;
    }

    private static boolean isStar(ImportFrom s) {
        for (Alias a : s.names) {
            if ("*".equals(a.name)) {
                return true;
            }
        }
        return false;
    }

    private void rejectedBelow(Layouts.Cls k, Set<String> names) {
        for (Layouts.Cls a = k; a != null; a = a.parent) {
            Set<String> s = p.rejectedNames.get(a);
            if (s == null) {
                s = new HashSet<String>();
                p.rejectedNames.put(a, s);
            }
            s.addAll(names);
        }
    }

    private boolean anyRejected(String attr) {
        for (Set<String> s : p.rejectedNames.values()) {
            if (s.contains(attr)) {
                return true;
            }
        }
        return false;
    }

    /// Whether a rejected class deriving from `k` may bind `attr`.
    private boolean rejected(Layouts.Cls k, String attr) {
        if (p.rejectedAnywhere.contains(attr)) {
            return true;
        }
        Set<String> s = k == null ? null : p.rejectedNames.get(k);
        return s != null && s.contains(attr);
    }

    private void openWithAncestors(Layouts.Cls k) {
        for (; k != null; k = k.parent) {
            p.open.add(k);
        }
    }

    private Layouts.Cls byName(String n) {
        for (Layouts.Cls k : layouts.all().values()) {
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
        if (f == null) {
            // Not compiled typed -- bs4's Tag takes 17 parameters, its
            // SoupStrainer **kwargs -- but what it sets is read off the
            // statements alone: the one `__init__` the class body defines,
            // undecorated, never shadowed by a store.
            f = soleInit(owner);
        }
        if (f == null || f.args.names().isEmpty()) {
            return r;
        }
        String self = f.args.names().get(0);
        if (f.scope.assigned.contains(self) || f.args.vararg != null && f.args.vararg.name.equals(self)) {
            return r;
        }
        definiteIn(f.body, new InitWalk(k, owner, self, seen), r);
        return r;
    }

    /// The `__init__` class `k`'s body defines once, without decorators,
    /// or null.
    private static FunctionDef soleInit(Layouts.Cls k) {
        FunctionDef r = null;
        for (Stmt st : k.def.body) {
            if (st instanceof FunctionDef && "__init__".equals(((FunctionDef) st).name)) {
                if (r != null || !((FunctionDef) st).decorators.isEmpty() || ((FunctionDef) st).isAsync) {
                    return null;
                }
                r = (FunctionDef) st;
            } else if (!(st instanceof FunctionDef || st instanceof ClassDef)) {
                List<String> names = new ArrayList<String>();
                if (st instanceof Assign) {
                    for (Expr t : ((Assign) st).targets) {
                        targetNames(t, names);
                    }
                } else if (st instanceof AnnAssign) {
                    targetNames(((AnnAssign) st).target, names);
                }
                if (names.contains("__init__")) {
                    return null;
                }
            }
        }
        return r;
    }

    /// How a run of `__init__` statements ended, for [#definiteIn].
    private enum InitEnd {
        /// It fell through: later statements run after it.
        ON,
        /// It always raises: nothing after it runs on that path, so what
        /// that path set is irrelevant to the fields the object ends with.
        RAISES,
        /// It reached code that may observe the object -- or ended the
        /// constructor early: nothing set after it counts.
        STOP
    }

    /// What [#definiteIn] needs: the class, the `__init__`'s own class,
    /// the name of its first parameter, and the initializers already
    /// followed (a cycle of base `__init__` calls).
    private static final class InitWalk {
        final Layouts.Cls k;
        final Layouts.Cls owner;
        final String self;
        final Set<Layouts.Cls> seen;

        InitWalk(Layouts.Cls k, Layouts.Cls owner, String self, Set<Layouts.Cls> seen) {
            this.k = k;
            this.owner = owner;
            this.self = self;
            this.seen = seen;
        }
    }

    /// Adds to `r` every field `body` sets on every path that falls
    /// through, before anything may read the object. Structured, not
    /// straight-line: a field both branches of an `if` set is set, a
    /// branch that raises sets nothing anyone sees, and an expression may
    /// read a field already set or call a classmethod or staticmethod
    /// through the instance -- neither observes a field not yet set. bs4's
    /// SoupStrainer sets `name_rules` on both branches of an `if`, from a
    /// classmethod called through `self`, and Tag sets `name` and `prefix`
    /// after an `if` and a branch that raises; a walk that stopped at the
    /// first `if` left all of them untyped. What a path sets is only ever
    /// counted when every path sets it, so a read of a field no path set
    /// yet still stops the walk.
    private InitEnd definiteIn(List<Stmt> body, InitWalk w, Set<String> r) {
        for (Stmt s : body) {
            InitEnd e = definiteStmt(s, w, r);
            if (e != InitEnd.ON) {
                return e;
            }
        }
        return InitEnd.ON;
    }

    private InitEnd definiteStmt(Stmt s, InitWalk w, Set<String> r) {
        if (s instanceof Pass || s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant) {
            return InitEnd.ON;
        }
        Expr target = null;
        Expr value = null;
        if (s instanceof Assign && ((Assign) s).targets.size() == 1) {
            target = ((Assign) s).targets.get(0);
            value = ((Assign) s).value;
        } else if (s instanceof AnnAssign && ((AnnAssign) s).value != null) {
            target = ((AnnAssign) s).target;
            value = ((AnnAssign) s).value;
        }
        if (target instanceof Attribute && ((Attribute) target).value instanceof Name
                && ((Name) ((Attribute) target).value).id.equals(w.self) && observesNothing(value, w, r)) {
            if (!w.k.allFields.contains(((Attribute) target).attr)) {
                // An attribute kept outside the fields. From here on the
                // instance lists its attributes in the order they were
                // set, and a field first set after this one is kept with
                // them, not in the Java field (PyInstance.store): typed
                // code, which reads the field without asking, must not
                // count on it.
                r.add(EXTRA);
                return InitEnd.STOP;
            }
            r.add(((Attribute) target).attr);
            return InitEnd.ON;
        }
        if (s instanceof Raise && observesNothing(s, w, r)) {
            return InitEnd.RAISES;
        }
        if (s instanceof If && observesNothing(((If) s).test, w, r)) {
            If i = (If) s;
            Set<String> a = new HashSet<String>(r);
            Set<String> b = new HashSet<String>(r);
            InitEnd ea = definiteIn(i.body, w, a);
            InitEnd eb = definiteIn(i.orelse, w, b);
            if (a.contains(EXTRA) || b.contains(EXTRA)) {
                // An attribute outside the fields on either path: what
                // follows is ordered with it (see above).
                r.add(EXTRA);
                return InitEnd.STOP;
            }
            if (ea == InitEnd.RAISES && eb == InitEnd.RAISES) {
                return InitEnd.RAISES;
            }
            if (ea == InitEnd.RAISES) {
                r.addAll(b);
                return eb;
            }
            if (eb == InitEnd.RAISES) {
                r.addAll(a);
                return ea;
            }
            a.retainAll(b);
            r.addAll(a);
            return ea == InitEnd.STOP || eb == InitEnd.STOP ? InitEnd.STOP : InitEnd.ON;
        }
        if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Call) {
            Call c = (Call) ((ExprStmt) s).value;
            if (c.func instanceof Attribute && "__init__".equals(((Attribute) c.func).attr)
                    && ((Attribute) c.func).value instanceof Name && c.keywords.isEmpty() && !c.args.isEmpty()
                    && c.args.get(0) instanceof Name && ((Name) c.args.get(0)).id.equals(w.self)) {
                Layouts.Cls base = globalClasses.get(((Name) ((Attribute) c.func).value).id);
                boolean clean = base != null && isAncestor(base, w.owner);
                for (int i = 1; i < c.args.size() && clean; i++) {
                    clean = observesNothing(c.args.get(i), w, r);
                }
                if (clean) {
                    Set<String> up = initDefinite(base, w.seen);
                    r.addAll(up);
                    return up.contains(EXTRA) ? InitEnd.STOP : InitEnd.ON;
                }
            }
        }
        if (!(s instanceof Return) && observesNothing(s, w, r)) {
            return InitEnd.ON;
        }
        return InitEnd.STOP;
    }

    /// Whether evaluating `o` cannot observe a field of the instance not
    /// yet set: it names the instance only to read a field in `set`, or to
    /// call, through it, a method every class it may be (the class, its
    /// bases, its subclasses) binds as a classmethod or staticmethod. Any
    /// other mention -- passing the instance on, reading another
    /// attribute, calling an instance method -- may.
    private boolean observesNothing(Object o, InitWalk w, Set<String> set) {
        if (o == null) {
            return true;
        }
        if (o instanceof Attribute && ((Attribute) o).value instanceof Name
                && ((Name) ((Attribute) o).value).id.equals(w.self)) {
            return set.contains(((Attribute) o).attr);
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ((Attribute) ((Call) o).func).value instanceof Name
                && ((Name) ((Attribute) ((Call) o).func).value).id.equals(w.self)
                && notBoundToInstance(w.k, ((Attribute) ((Call) o).func).attr)) {
            Call c = (Call) o;
            for (Expr a : c.args) {
                if (!observesNothing(a, w, set)) {
                    return false;
                }
            }
            for (Keyword kw : c.keywords) {
                if (!observesNothing(kw.value, w, set)) {
                    return false;
                }
            }
            return true;
        }
        if (o instanceof Name) {
            return !((Name) o).id.equals(w.self);
        }
        if (o instanceof FunctionDef || o instanceof Lambda || o instanceof ClassDef) {
            // A nested function may run later and read anything.
            return !mentions(o, w.self);
        }
        boolean[] ok = {true};
        children(o, x -> ok[0] &= observesNothing(x, w, set));
        return ok[0];
    }

    /// Whether `m`, looked up on an instance of `k` or of any subclass,
    /// is always a classmethod or staticmethod a class body defines:
    /// no instance field or computed attribute may carry the name, and no
    /// class in the way has a base the program does not compile.
    private boolean notBoundToInstance(Layouts.Cls k, String m) {
        List<Layouts.Cls> all = new ArrayList<Layouts.Cls>(p.below(k));
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            all.add(c);
        }
        boolean found = false;
        for (Layouts.Cls c : all) {
            if (c.extraBases || c.allFields.contains(m) || c.bodyNames.contains("__getattribute__")) {
                return false;
            }
            for (Stmt st : c.def.body) {
                if (st instanceof FunctionDef && ((FunctionDef) st).name.equals(m)) {
                    if (!classOrStatic((FunctionDef) st, c)) {
                        return false;
                    }
                    found = true;
                } else if (st instanceof Assign || st instanceof AnnAssign) {
                    List<String> names = new ArrayList<String>();
                    targetNames(st instanceof Assign ? ((Assign) st).targets.get(0) : ((AnnAssign) st).target,
                            names);
                    if (names.contains(m)) {
                        return false;
                    }
                }
            }
        }
        return found;
    }

    /// Whether method `f` of class `c` is decorated with nothing but the
    /// builtin `classmethod` or `staticmethod`: the name must still mean the
    /// builtin where the class body runs, since a module may rebind it.
    private static boolean classOrStatic(FunctionDef f, Layouts.Cls c) {
        if (f.decorators.size() != 1 || !(f.decorators.get(0) instanceof Name)) {
            return false;
        }
        String d = ((Name) f.decorators.get(0)).id;
        return ("classmethod".equals(d) || "staticmethod".equals(d))
                && c.def.scope.parent.resolve(d) == Scope.Binding.BUILTIN;
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
        if (!definite.get(owner).contains(f)) {
            return false;
        }
        for (Layouts.Cls k : p.below(owner)) {
            {
                Infer kh = p.home.get(k);
                if (kh.facts.dictViews || kh.facts.deleted.contains(f) || !definite.get(k).contains(f)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Whether field `f` of `owner`, which no constructor is known to set
    /// on every instance, may still be held typed as `t`: a reference type,
    /// whose slot holds no value of its own when absent, kept as a guarded
    /// pair whose object half starts as Typed.UNSET (TypedEmit#openField).
    /// Not a field any module deletes, nor one of a class whose `__dict__`
    /// code may take: those change the field behind both halves. bs4's
    /// BeautifulSoup is a Tag whose constructor reaches Tag.__init__ only
    /// through `self.reset()`, which left `name` and `prefix` of every Tag
    /// held as plain objects behind an attribute cache.
    boolean openable(Layouts.Cls owner, String f, Ty t) {
        if (t.k != Ty.K.STR && t.k != Ty.K.LIST && t.k != Ty.K.INST) {
            return false;
        }
        for (Infer m : p.modules.values()) {
            if (m.facts.deleted.contains(f)) {
                return false;
            }
        }
        if (p.home.get(owner).facts.dictViews) {
            return false;
        }
        for (Layouts.Cls k : p.below(owner)) {
            if (p.home.get(k).facts.dictViews) {
                return false;
            }
        }
        return true;
    }

    /// Whether every instance `isinstance(x, o)` passes is of `o`'s Java
    /// class. Not when a class derives from `o` through a base besides its
    /// compiled parent: python-pptx's `_BaseSlidePlaceholder(
    /// _InheritsDimensions, Shape)` is a `BaseShape` to Python and an
    /// `_InheritsDimensions` to Java, and the cast a narrowing makes fails.
    /// A refused class counts too: its instances are made of the Java class
    /// of its analysed parent. A base is matched by name, which may refuse
    /// more than it must.
    private boolean layoutHolds(Layouts.Cls o) {
        for (Layouts.Cls x = o; x != null; x = x.parent) {
            if (definesMethod(x, "__subclasshook__") || definesMethod(x, "__instancecheck__")) {
                // An ABC's __subclasshook__ passes isinstance for any class
                // it accepts -- `hasattr(C, "get")` -- of whatever Java class.
                return false;
            }
        }
        if (ProtocolChecks.isProtocol(o.def)) {
            // A protocol passes any instance with its members
            // (ProtocolChecks), of whatever Java class.
            return false;
        }
        if (o.enumMeta) {
            // A member of an enumeration with a builtin mixin (IntEnum,
            // `class Role(str, Enum)`) is made by the builtin's __new__,
            // which need not extend the class's Java class; so is one of a
            // class below it. Every other member is made once, after the
            // class statement, as an instance of exactly its class
            // (EnumLowering).
            for (Layouts.Cls c : selfAndBelow(o)) {
                if (c.builtin != null || !c.compiled) {
                    return false;
                }
            }
        }
        Set<String> reg = registeredNames();
        for (Layouts.Cls c : p.home.keySet()) {
            if (reg.contains(c.def.name) && isSelfOrAncestor(o, c)) {
                // `Integral.register(int)`: an int passes isinstance for
                // Integral and every class above it, with no layout at all.
                return false;
            }
        }
        for (Layouts.Cls c : p.home.keySet()) {
            if (c.def.bases.size() < 2 || isSelfOrAncestor(o, c)) {
                continue;
            }
            for (Expr be : c.def.bases) {
                // `Generic[T]`, `Base[int]`: the class subscripted is the base.
                Expr bx = be instanceof Subscript ? ((Subscript) be).value : be;
                String bn = bx instanceof Name ? ((Name) bx).id : bx instanceof Attribute ? ((Attribute) bx).attr : null;
                if (bn == null) {
                    return false;
                }
                for (Layouts.Cls d : p.home.keySet()) {
                    if (d.def.name.equals(bn) && isSelfOrAncestor(o, d)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /// Whether `c`'s own class body defines or assigns `name`.
    private static boolean definesMethod(Layouts.Cls c, String name) {
        for (Stmt st : c.def.body) {
            if (st instanceof FunctionDef && name.equals(((FunctionDef) st).name)) {
                return true;
            }
            if (st instanceof Assign) {
                // `__subclasshook__ = classmethod(_hook)`
                for (Expr t : ((Assign) st).targets) {
                    if (t instanceof Name && name.equals(((Name) t).id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// The names `X.register(...)` (an ABC's virtual subclass) is called
    /// on, or decorates with, anywhere in the program.
    private Set<String> registeredNames() {
        if (p.registered == null) {
            Set<String> r = new HashSet<String>();
            for (Infer m : p.modules.values()) {
                for (Stmt st : m.body) {
                    registerCalls(st, r);
                }
            }
            p.registered = r;
        }
        return p.registered;
    }

    private static void registerCalls(Object o, Set<String> r) {
        Expr f = o instanceof Call ? ((Call) o).func : o instanceof Attribute ? (Expr) o : null;
        if (f instanceof Attribute && "register".equals(((Attribute) f).attr) && ((Attribute) f).value instanceof Name) {
            r.add(((Name) ((Attribute) f).value).id);
        }
        Infer.children(o, x -> registerCalls(x, r));
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
        if (r.k != Ty.K.LIST && r.k != Ty.K.TUPLE && r.k != Ty.K.DICT && r.k != Ty.K.INST && r.k != Ty.K.CLS
                && r.k != Ty.K.UNION && r.k != Ty.K.METH && r.k != Ty.K.FUNC) {
            // A callable keeps its trail: the calls made through what it
            // is stored in are followed to it.
            escape(v);
        }
        return r;
    }

    /// The join of two types. An instance that joins into a value of no
    /// one class is from then on held where the pass does not follow it.
    private Ty mix(Ty a, Ty b) {
        Ty r = Ty.join(a, b);
        if (r.k == Ty.K.OBJECT && !p.done) {
            if (a != null && a.k == Ty.K.INST) {
                loosen(a.cls);
            }
            if (b != null && b.k == Ty.K.INST) {
                loosen(b.cls);
            }
            if (a != null && a.k == Ty.K.CLS) {
                escape(a);
            }
            if (b != null && b.k == Ty.K.CLS) {
                escape(b);
            }
            // A function either may be is from here on called where the
            // pass does not follow it. Not so in the generic view of a body,
            // where every value is an object by construction: the typed walk
            // of the same code follows the function, and a generic call
            // enters it through the door's guard.
            if (a != null && !shadow) {
                escapeFuncs(a);
            }
            if (b != null && !shadow) {
                escapeFuncs(b);
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
        // A compiled class merged into another's layout as an extra base
        // (Layouts#mergeExtras) has its methods reached through that
        // class's MRO, generically and with arguments nobody tracks.
        for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.home.keySet())) {
            for (Layouts.Cls x : k.mixedFrom) {
                for (Stmt st : x.def.body) {
                    if (st instanceof FunctionDef) {
                        escapeFn(p.fns.get((FunctionDef) st));
                    }
                }
            }
        }
        if (p.anyClassOf) {
            for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.loose)) {
                Infer in = p.home.get(k);
                // A call of a class nobody can name reaches k only through
                // an instance of k (`type(x)(...)`) or k's class object held
                // as a value. With neither -- never constructed, never a
                // class value, its module never read by a computed name --
                // its constructor is not one: bs4's deprecated
                // BeautifulStoneSoup, never built, forwarded *args into
                // BeautifulSoup.__init__ and widened every parameter. A
                // class value counts only when it escaped (#looseRoot): one
                // merely named -- `isinstance(x, Tag)`, or held by typing's
                // TypeVar (#callKnown) -- is not a value unknown code holds.
                if (in != null && !p.constructed.contains(k) && !p.looseRoots.contains(k)
                        && !p.allModulesLoose && !p.looseModules.contains(in)) {
                    continue;
                }
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

    /// Whether an instance of compiled class `k` can never hold an
    /// attribute of its own under a name no module stores by name: no
    /// computed-name store reaches it. The caller checks the names
    /// ([WholeProgram.Module#attrStored]) and, at run time, that nothing
    /// wrote to the class (`PyType.classWritten`).
    /// Whether `e` is certainly a class object, not an instance: a store
    /// through it changes a class, which `PyType.classWritten` answers for
    /// at run time, and no instance's own attributes.
    private boolean classReceiver(Expr e, Fn fn, Scope sc) {
        if (e == null) {
            return false;
        }
        if (classOwner(e, sc) != null) {
            return true;
        }
        if (!(e instanceof Name)) {
            return false;
        }
        String n = ((Name) e).id;
        if (globalClasses.containsKey(n) && sc.resolve(n) == Scope.Binding.GLOBAL && !moduleScope.hasStarImport) {
            return true;
        }
        // A decorated function is no typed Fn: found by its scope.
        FunctionDef def = fn != null && fn.def != null ? fn.def : defOf(sc);
        if (def == null || def.scope != sc || sc.assigned.contains(n) || sc.resolve(n) != Scope.Binding.LOCAL) { //NOPMD CompareObjectsWithEquals - identity is meant
            return false;
        }
        List<String> params = def.args.names();
        if (params.isEmpty() || !params.get(0).equals(n)) {
            return false;
        }
        for (Expr d : def.decorators) {
            if (d instanceof Name && "classmethod".equals(((Name) d).id)
                    && sc.resolve("classmethod") == Scope.Binding.BUILTIN) {
                return true;
            }
        }
        return false;
    }

    /// The compiled class `func` names when called in scope `sc`: a global
    /// of this module, a name imported from another, or `module.Cls`; and
    /// only where the module never rebinds it. Null otherwise.
    Layouts.Cls calledClass(Expr func, Scope sc) {
        Layouts.Cls k = calledClassOrEnum(func, sc);
        // Calling an enum class looks a member up (EnumType.__call__), and
        // its attributes are the members the metaclass made of the body's
        // values: neither is what the class statement says.
        return k == null || k.enumMeta ? null : k;
    }

    private Layouts.Cls calledClassOrEnum(Expr func, Scope sc) {
        if (func instanceof Name && sc.resolve(((Name) func).id) == Scope.Binding.GLOBAL) {
            Layouts.Cls k = globalClasses.get(((Name) func).id);
            if (k != null) {
                return k;
            }
            Object o = foreign(func, sc);
            return o instanceof Layouts.Cls ? (Layouts.Cls) o : null;
        }
        if (func instanceof Attribute && foreign(((Attribute) func).value, sc) instanceof Infer) {
            Object o = foreign(func, sc);
            return o instanceof Layouts.Cls ? (Layouts.Cls) o : null;
        }
        return null;
    }

    /// The module's class named `n` if generated code has a Java class for
    /// it, or null: an analysed class is held as a plain object.
    Layouts.Cls emitClass(String n) {
        Layouts.Cls k = globalClasses.get(n);
        return k == null || !k.compiled ? null : k;
    }

    /// [#calledClass], for generated code: null for an analysed class.
    Layouts.Cls emitCalledClass(Expr func, Scope sc) {
        Layouts.Cls k = calledClass(func, sc);
        return k == null || !k.compiled ? null : k;
    }

    /// Every compiled class of the program below `k`.
    List<Layouts.Cls> subclasses(Layouts.Cls k) {
        return p.below(k);
    }

    /// The layouts of the module that compiled `k`, or null.
    Layouts layoutsOf(Layouts.Cls k) {
        Infer m = p.home.get(k);
        return m == null ? null : m.layouts;
    }

    private Map<Scope, FunctionDef> scopeDefs;

    /// The function whose own scope is `sc`, or null.
    private FunctionDef defOf(Scope sc) {
        if (scopeDefs == null) {
            scopeDefs = new IdentityHashMap<Scope, FunctionDef>();
            collectDefs(body);
        }
        return scopeDefs.get(sc);
    }

    private void collectDefs(Object o) {
        if (o instanceof FunctionDef) {
            scopeDefs.put(((FunctionDef) o).scope, (FunctionDef) o);
        }
        children(o, this::collectDefs);
    }

    boolean instanceAttrsClosed(Layouts.Cls k) {
        for (Layouts.Cls o : p.computedOn) {
            if (isSelfOrAncestor(o, k) || isSelfOrAncestor(k, o)) {
                return false;
            }
        }
        if (p.computedOnAny) {
            for (Layouts.Cls o : p.loose) {
                if (isSelfOrAncestor(o, k) || isSelfOrAncestor(k, o)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Whether every call of the `__call__` method `f` is one the pass
    /// sees: a call of an instance typed as its class [#call0] binds it
    /// like any method, and only an instance that went where the pass does
    /// not follow -- its class loose, which [#loosen] spreads up and down
    /// the parent chain -- can be called unseen. A class that is a later
    /// base of another is reached off that chain, so it is not followed.
    private boolean followedCall(Fn f) {
        if (f.cls == null || p.loose.contains(f.cls) || open.contains(f.cls) || !f.def.decorators.isEmpty()) {
            return false;
        }
        String name = f.cls.def.name;
        Infer home = p.fnHome.get(f);
        if (home == null || home.globalClasses.get(name) != f.cls) { //NOPMD CompareObjectsWithEquals - identity is meant
            // A class its module does not bind once at top level with a
            // layout: a call of it is not a construction the pass follows,
            // so its instances are never typed and every call of one is
            // unseen (dateutil's `weekday`, made in a comprehension).
            return false;
        }
        if (!facts.mayBeBase(name, p.reached)) {
            return true;
        }
        for (Infer in : p.modules.values()) {
            for (Stmt s : in.body) {
                if (!(s instanceof ClassDef)) {
                    continue;
                }
                List<Expr> bases = ((ClassDef) s).bases;
                for (int i = 1; i < bases.size(); i++) {
                    Expr x = bases.get(i);
                    ClassDef d = x instanceof Name ? Layouts.classNamed(in.body, ((Name) x).id) : null;
                    if (d == null) {
                        d = in.foreignClassDef(x);
                    }
                    if (d == f.cls.def) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return false;
                    }
                    String last = x instanceof Name ? ((Name) x).id : x instanceof Attribute ? ((Attribute) x).attr : null;
                    if (d == null && (last == null || last.equals(name))) {
                        // Unresolved, and may be this class: only a base
                        // spelled with another last name is surely not.
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /// An instance of k went where the pass does not follow it: what is
    /// done with it there is not seen, so k's hierarchy is loose. It makes
    /// no class live as itself -- what holds an instance makes more only of
    /// the class it already has (`type(x)(...)`, a copy), and that class is
    /// live. Only a class held as a value is a root of instances the pass
    /// does not see ([#escapeClass], [#exactLive]). When the escape made a
    /// root, bs4 passing `self._most_recent_element` to a tag factory made
    /// PageElement live as itself, and every `else` of an isinstance test
    /// on one read a PageElement.
    private void loosen(Layouts.Cls k) {
        if (k != null && p.loosened.add(k)) {
            looseHierarchy(k);
        }
    }

    /// k is held as a value: whoever holds it makes instances of it.
    private void looseRoot(Layouts.Cls k) {
        if (k != null && p.looseRoots.add(k)) {
            p.changed = true;
            loosen(k);
        }
    }

    private void looseHierarchy(Layouts.Cls k) {
        for (Layouts.Cls o : p.home.keySet()) {
            if ((isSelfOrAncestor(o, k) || isSelfOrAncestor(k, o)) && p.loose.add(o)) {
                p.changed = true;
                if (!p.anyClassNames.isEmpty()) {
                    for (Fn f : new ArrayList<Fn>(p.fns.values())) {
                        if (f.cls == o && p.anyClassNames.contains(f.def.name)) { //NOPMD CompareObjectsWithEquals - identity is meant
                            escapeFn(f);
                        }
                    }
                }
            }
        }
    }

    /// A value went somewhere the pass does not follow.

    void escape(Ty v) {
        if (p.done) {
            return;
        }
        if (v != null && v.k == Ty.K.UNION) {
            for (Ty m : v.members()) {
                escape(m);
            }
            return;
        }
        if (v != null && v.k == Ty.K.METH) {
            noMeth(v);
            return;
        }
        if (v != null && v.k == Ty.K.FUNC) {
            // Called from where the pass cannot see.
            for (Fn f : funcsOf(v)) {
                escapeFn(f);
            }
            return;
        }
        if (v != null && v.k == Ty.K.INST) {
            loosen(v.cls);
            return;
        }
        if (v != null && v.k == Ty.K.CLS) {
            if (v.sites.length == 0) {
                anyClass();
            }
            for (Layouts.Cls k : classesOf(v)) {
                if (typingHeld) {
                    loosen(k);
                } else {
                    escapeClass(k);
                }
            }
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
        } else if (e instanceof Call && ((Call) e).func instanceof Attribute
                && "format".equals(((Attribute) ((Call) e).func).attr)
                && ((Attribute) ((Call) e).func).value instanceof Constant
                && ((Constant) ((Attribute) ((Call) e).func).value).kind == Constant.Kind.STR) {
            // `"do_{}".format(n)`: the text before the first field.
            r = ((Constant) ((Attribute) ((Call) e).func).value).text;
            int i = r.indexOf('{');
            r = i < 0 || r.startsWith("{{", i) ? null : r.substring(0, i);
        }
        return r == null || r.isEmpty() ? null : r;
    }

    /// Builtins that read the lists they are given and never store into
    /// them, nor hand out anything that can.
    private static final Set<String> READ_ONLY_BUILTINS = new HashSet<String>(java.util.Arrays.asList("reversed",
            "enumerate", "iter", "sorted", "tuple", "set", "frozenset", "sum", "min", "max", "any", "all", "zip",
            "len", "map", "filter", "repr", "str", "bool", "print", "dict"));
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

    /// The builtins whose subscription makes a `types.GenericAlias`.
    private static final Set<String> GENERIC_BUILTINS = new HashSet<String>(java.util.Arrays.asList("list", "dict",
            "tuple", "set", "frozenset", "type"));

    /// Attribute `attr` read off a class nobody can name. A method read so
    /// is called with arguments nobody tracks; a class method is handed the
    /// class too, and only then is the class let go.
    private Ty anyClassAttr(String attr) {
        for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.home.keySet())) {
            if (k.bodyNames.contains(attr)) {
                FunctionDef d = methodOrAlias(k, attr);
                if (d == null || classOrStatic(d, k) || Layouts.IMPLICIT_CLASS_LEVEL.contains(attr)
                        || !d.decorators.isEmpty()) {
                    anyClass();
                    break;
                }
            }
        }
        // A class nobody can name is one that went where the pass does not
        // follow: a loose one (a class whose instance is followed is named,
        // [#classesBelow], or loosened where it is not).
        p.anyClassNames.add(attr);
        for (Fn f : new ArrayList<Fn>(p.fns.values())) {
            if (f.cls != null && f.def.name.equals(attr) && p.loose.contains(f.cls)) {
                escapeFn(f);
            }
        }
        return Ty.UNSEEN;
    }

    /// The method `attr` names in `k`'s body: its def, or the def of the
    /// one name it is bound to there (`__copy__ = copy`). Null otherwise.
    private static FunctionDef methodOrAlias(Layouts.Cls k, String attr) {
        FunctionDef d = bodyDef(k, attr);
        if (d != null) {
            return d;
        }
        String to = null;
        for (Stmt st : k.def.body) {
            if (st instanceof Assign && ((Assign) st).targets.size() == 1 && ((Assign) st).targets.get(0) instanceof Name
                    && ((Name) ((Assign) st).targets.get(0)).id.equals(attr)) {
                if (to != null || !(((Assign) st).value instanceof Name)) {
                    return null;
                }
                to = ((Name) ((Assign) st).value).id;
            }
        }
        return to == null || to.equals(attr) ? null : bodyDef(k, to);
    }

    /// A class nobody can name was called or let go: whatever is loose may
    /// be made, with arguments nobody tracks ([#sweep]).
    private void anyClass() {
        if (!p.anyClassOf) {
            p.anyClassOf = true;
            p.changed = true;
        }
    }

    /// The class value of an instance of `t`: every compiled class at or
    /// below its static class. Null when that is not known -- an optional
    /// or untyped value, or a program another module may extend.
    private Ty classesBelow(Ty t) {
        if (t.k != Ty.K.INST || t.opt || !p.closed) {
            return null;
        }
        Ty r = Ty.BOT;
        for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.home.keySet())) {
            // Only a class an instance can be of: one constructed, or let
            // go where the pass stops following it. bs4's `type(self)(...)`
            // otherwise counted the never-built BeautifulStoneSoup, whose
            // `*args` forwarding widened every BeautifulSoup parameter.
            if (isSelfOrAncestor(t.cls, k) && exactLive(k)) {
                r = Ty.join(r, classValue(k));
            }
        }
        // None yet: nothing so far, until a construction makes one.
        return r.k == Ty.K.CLS || r.k == Ty.K.BOT && !p.done ? r : null;
    }

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
        }
        if (r.wide() && !p.anyClassOf) {
            // Whatever is loose by the end: the sweep asks again each round.
            p.anyClassOf = true;
            p.changed = true;
        }
    }

    /// The `x.__dict__` reads of this module that only read the dict: the
    /// receiver of `.items()`, `.keys()`, `.values()`, `.get(...)` or
    /// `.copy()`, the right side of `in`/`not in`, the iterable of a `for`,
    /// or the argument of `len` or of a copying builtin. None of them can store an attribute, so the
    /// fields of the program keep their types (enum's `_finish` iterating
    /// a class's dict guarded every field of every class).
    private Set<Node> readDicts() {
        if (readDicts == null) {
            Set<Node> r = java.util.Collections.newSetFromMap(new IdentityHashMap<Node, Boolean>());
            if (body != null) {
                for (Stmt st : body) {
                    findReadDicts(st, r);
                }
            }
            readDicts = r;
        }
        return readDicts;
    }

    private Set<Node> readDicts;

    private static boolean isDictRead(Expr e) {
        return e instanceof Attribute && "__dict__".equals(((Attribute) e).attr);
    }

    private static void findReadDicts(Object o, Set<Node> r) {
        if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Attribute && isDictRead(((Attribute) c.func).value)
                    && READ_DICT_METHODS.contains(((Attribute) c.func).attr)) {
                r.add(((Attribute) c.func).value);
            }
            if (c.func instanceof Attribute && "update".equals(((Attribute) c.func).attr) && c.args.size() == 1
                    && c.keywords.isEmpty() && isDictRead(c.args.get(0))) {
                // Copied into another dict (logging's `values.update(record.__dict__)`).
                r.add(c.args.get(0));
            }
            if (c.func instanceof Name && READ_DICT_FUNCS.contains(((Name) c.func).id) && c.args.size() == 1
                    && c.keywords.isEmpty() && isDictRead(c.args.get(0))) {
                r.add(c.args.get(0));
            }
        } else if (o instanceof Compare) {
            Compare c = (Compare) o;
            for (int i = 0; i < c.ops.size(); i++) {
                if (("in".equals(c.ops.get(i)) || "not in".equals(c.ops.get(i))) && isDictRead(c.comparators.get(i))) {
                    r.add(c.comparators.get(i));
                }
            }
        } else if (o instanceof For && isDictRead(((For) o).iter)) {
            r.add(((For) o).iter);
        }
        children(o, x -> findReadDicts(x, r));
    }

    private static final Set<String> READ_DICT_METHODS = new HashSet<String>(java.util.Arrays.asList("items", "keys", "values", "get", "copy",
            // These only remove: no value reaches a field (bs4's decompose
            // clears every element's dict).
            "clear", "pop", "popitem"));

    /// Builtins whose one argument is only read: `dict(x.__dict__)` is a
    /// copy (bs4's `__getstate__`), and writing the copy sets nothing on `x`.
    private static final Set<String> READ_DICT_FUNCS = new HashSet<String>(java.util.Arrays.asList("len", "dict", "list", "sorted", "set", "tuple"));

    /// The attribute dict of `owner` used for anything but a read: handed
    /// on, it may be written under any name.
    private void dictOf(Expr at, Expr owner, Fn fn, Scope sc) {
        if (classFacts.contains(at) || readDicts().contains(at)) {
            if (readDicts().contains(at)) {
                type(owner, fn, sc);
            }
            return;
        }
        if (owner instanceof Name && globalClasses.containsKey(((Name) owner).id)
                && sc.resolve(((Name) owner).id) == Scope.Binding.GLOBAL) {
            // A class's own dict is read-only, and holds no instance's
            // attributes.
            return;
        }
        computedStore(type(owner, fn, sc), owner, null, Ty.OBJECT, at, fn, sc,
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
        if (recv.k == Ty.K.UNION) {
            for (Ty m : recv.members()) {
                computedStore(m.nonNone(), recvExpr, name, v, at, fn, sc, what);
            }
            return;
        }
        if (recv.k == Ty.K.INST && name instanceof Constant && !plainReceiver(recvExpr, sc)) {
            // `object.__setattr__(self, "a", v)` on an instance of a known
            // class (a frozen dataclass's constructor): the store `self.a
            // = v` is, past the class's own __setattr__, and v goes no
            // further than that field.
            List<String> named = constStrings(name, fn, sc);
            if (named != null) {
                for (String k : named) {
                    storeField(recv, k, v);
                }
                return;
            }
        }
        escape(v);
        if (recv.k != Ty.K.INST && !recv.wide() || plainReceiver(recvExpr, sc)) {
            return;
        }
        List<String> known = name == null ? null : constStrings(name, fn, sc);
        if (known != null) {
            if (!(name instanceof Constant)) {
                // Names the source does not spell as a setattr argument,
                // which WholeProgram's facts read: counted here instead.
                p.computedStoredNames.addAll(known);
            }
            for (String k : known) {
                storeField(recv, k, v);
            }
            return;
        }
        String pre = name == null ? null : prefix(name);
        computing = true;
        try {
            computedFields(recv, recvExpr, pre, v, at, fn, sc, what);
        } finally {
            computing = false;
        }
    }

    /// Whether the stores being joined are a computed store's (see
    /// [#storeOne]).
    private boolean computing;

    private void computedFields(Ty recv, Expr recvExpr, String pre, Ty v, Node at, Fn fn, Scope sc, String what) {
        if (recv.k == Ty.K.INST) {
            p.computedOn.add(recv.cls);
        } else if (!classReceiver(recvExpr, fn, sc)) {
            p.computedOnAny = true;
        }
        {
            boolean cost = false;
            for (Map.Entry<Layouts.Cls, Map<String, Ty>> e : fields.entrySet()) {
                Layouts.Cls o = e.getKey();
                if (recv.k == Ty.K.INST && !isSelfOrAncestor(o, recv.cls) && !isSelfOrAncestor(recv.cls, o)) {
                    continue;
                }
                // A receiver of no known class may be an instance of any
                // class, loosened or not: an unseen value is no evidence
                // against one (Ty#UNSEEN). The store guards the fields it
                // may reach (#storeOne), which keeps them typed.
                cost |= recv.k != Ty.K.INST && pre == null && !e.getValue().isEmpty() && p.loose.contains(o);
                for (String f : new ArrayList<String>(e.getValue().keySet())) {
                    if (pre == null || f.startsWith(pre)) {
                        storeOne(o, f, v, recv);
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
        if (recv.k == Ty.K.UNION) {
            for (Ty m : recv.members()) {
                computedRead(m.nonNone(), recvExpr, name, at, fn, sc);
            }
            return;
        }
        if (recv.k != Ty.K.INST && !recv.wide() || plainReceiver(recvExpr, sc)) {
            return;
        }
        List<String> known = recv.k == Ty.K.INST ? constStrings(name, fn, sc) : null;
        if (known != null) {
            // Each name the read can be, as `pre` narrows below. Only on an
            // instance: a read from a module, or from a value of no known
            // class, also reaches globals and classes ([#sweep]).
            for (Fn f : new ArrayList<Fn>(p.fns.values())) {
                if (known.contains(f.def.name) && (recv.k != Ty.K.INST || f.cls != null
                        && (isSelfOrAncestor(f.cls, recv.cls) || isSelfOrAncestor(recv.cls, f.cls)))) {
                    escapeFn(f);
                }
            }
            return;
        }
        String pre = prefix(name);
        if (pre == null) {
            // markdownify's `convert_fn_name = "convert_%s" % ...` and then
            // `getattr(self, convert_fn_name, None)`: without following the
            // name, every method of the converter escaped -- `process_tag`,
            // `process_element` and the rest took and returned only objects.
            Expr v = soleValue(name, fn, sc);
            pre = v == null ? null : prefix(v);
        }
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

    /// `getattr(obj, name)` on an instance of `k`, the name computed:
    /// the bound methods of `k`'s class tree that the name can be -- one of
    /// the strings it is seen to hold, or any name starting with the
    /// literal it is built on. Null when the read could find something
    /// else: a field, a static or class method or property, an attribute
    /// stored under a computed name, or a class answering `__getattr__`.
    private Ty methodsNamed(Layouts.Cls k, Expr name, Fn fn, Scope sc) {
        List<String> known = constStrings(name, fn, sc);
        String pre = null;
        if (known == null) {
            pre = prefix(name);
            if (pre == null) {
                Expr v = soleValue(name, fn, sc);
                pre = v == null ? null : prefix(v);
            }
            if (pre == null || pre.isEmpty()) {
                return null;
            }
        }
        return attrsNamed(k, known, pre);
    }

    /// What reading one of `known` -- or, when that is null, any name
    /// starting with `pre` -- on an instance of `k` finds: the bound
    /// methods and functions the class tree binds to it, joined with the
    /// fields of that name. Null when the read could find something the
    /// pass does not follow; BOT while a class body is not walked yet.
    private Ty attrsNamed(Layouts.Cls k, List<String> known, String pre) {
        List<Layouts.Cls> tree = new ArrayList<Layouts.Cls>();
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            tree.add(c);
        }
        tree.addAll(p.below(k));
        for (Layouts.Cls c : tree) {
            if (!c.compiled || c.bodyNames.contains("__getattr__") || c.bodyNames.contains("__getattribute__")) {
                return null;
            }
            Set<String> u = p.unknownStores.get(c);
            if (u != null) {
                for (String s : u) {
                    if ((known != null ? known.contains(s) : s.startsWith(pre)) && !c.allFields.contains(s)) {
                        // Stored where the pass cannot see what: it may
                        // shadow a method of that name.
                        return null;
                    }
                }
            }
        }
        Ty fields = Ty.BOT;
        for (Layouts.Cls c : tree) {
            for (String f : c.allFields) {
                if (known != null ? known.contains(f) : f.startsWith(pre)) {
                    // A field the name can be as well: its value joins in.
                    Layouts.Cls o = fieldOwner(c, f);
                    // As the field keeps it: what a store the pass could not
                    // type put there went wide, and any function in it with
                    // it (see [#mix]).
                    Ty ft = o == null ? null : this.fields.get(o).get(f);
                    fields = mix(fields, ft == null ? Ty.OBJECT : loc(ft));
                }
            }
        }
        for (String s : p.dynStored) {
            if (known != null ? known.contains(s) : s.startsWith(pre)) {
                return null;
            }
        }
        List<FunctionDef> defs = new ArrayList<FunctionDef>();
        for (Fn f : p.fns.values()) {
            if (f.cls == null || f.analysisOnly || !(known != null ? known.contains(f.def.name) : f.def.name.startsWith(pre))
                    || !tree.contains(f.cls)) {
                continue;
            }
            if (!plainMethod(f.def) || !f.def.decorators.isEmpty()) {
                return null;
            }
            defs.add(f.def);
        }
        List<Fn> bound = new ArrayList<Fn>();
        for (Fn f : p.fns.values()) {
            if (defs.contains(f.def)) {
                bound.add(f);
            }
        }
        for (Layouts.Cls c : tree) {
            for (Stmt s : c.def.body) {
                if (!(s instanceof Assign)) {
                    continue;
                }
                for (Expr t : ((Assign) s).targets) {
                    if (!(t instanceof Name && (known != null ? known.contains(((Name) t).id) : ((Name) t).id.startsWith(pre)))) {
                        continue;
                    }
                    // A class attribute of that name: another method of the
                    // tree under a second name (`convert_kbd = convert_code`),
                    // or a function a call made (markdownify's
                    // `abstract_inline_conversion(...)`), bound to the
                    // instance when read through it.
                    Expr v = ((Assign) s).value;
                    Fn alias = v instanceof Name ? methodOf(tree, ((Name) v).id) : null;
                    if (alias != null) {
                        bound.add(alias);
                        continue;
                    }
                    Map<String, Ty> cv = p.classVals.get(c.def.scope);
                    String vn = ((Name) t).id;
                    Ty val = cv == null ? null : cv.get(vn);
                    for (int hop = 0; val == null && cv != null && v instanceof Name && hop < 8; hop++) {
                        // `convert_i = convert_em`, itself a stored function.
                        vn = ((Name) v).id;
                        val = cv.get(vn);
                        v = valueOf(c.def.body, vn);
                    }
                    List<Fn> fs = new ArrayList<Fn>();
                    List<Integer> fr = new ArrayList<Integer>();
                    if (val == null || val.k == Ty.K.BOT) {
                        // The class body is not walked yet: nothing is
                        // known, so nothing is given up.
                        return Ty.BOT;
                    }
                    if (!callables(val.nonNone(), fs, fr)) {
                        return null;
                    }
                    for (int i = 0; i < fs.size(); i++) {
                        if (fr.get(i).intValue() != 0 || fs.get(i).params.length == 0) {
                            return null;
                        }
                        bound.add(fs.get(i));
                    }
                }
            }
        }
        Ty r = methValueOf(bound);
        // Not every instance has every name: the rest raise, or take the
        // default.
        if (r == null) {
            return fields.k == Ty.K.BOT ? null : fields;
        }
        return mix(r, fields);
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
        if (p.runtimeNames == null || entered(f)) {
            return true;
        }
        if (p.runtimeNames.contains(f.def.name) && (f.cls == null || classLive(f.cls))) {
            // A method the runtime calls by name (`__init__`, `__eq__`)
            // runs only on an instance, so only once something makes one:
            // bs4's deprecated BeautifulStoneSoup, which nothing
            // constructs, forwarded `*args` to BeautifulSoup.__init__.
            return true;
        }
        return p.keep.contains(moduleName + "." + (f.cls == null ? "" : f.cls.def.name + ".") + f.def.name);
    }

    /// Whether an instance of exactly `k` can exist: it is constructed, it
    /// went where the pass stops following (a class held as a value is
    /// constructed by whoever holds it), or the runtime makes it.
    private boolean exactLive(Layouts.Cls k) {
        Infer home = p.home.get(k);
        return home == null || RUNTIME_MODULES.contains(home.moduleName)
                || p.runtimeNames != null && p.runtimeNames.contains(home.moduleName)
                || p.constructed.contains(k) || p.looseRoots.contains(k);
    }

    /// Whether an instance of `k` can exist: it, or a class below it, is
    /// live ([#exactLive]).
    private boolean classLive(Layouts.Cls k) {
        Infer home = p.home.get(k);
        if (home == null || RUNTIME_MODULES.contains(home.moduleName)
                || p.runtimeNames != null && p.runtimeNames.contains(home.moduleName)) {
            // The runtime makes these itself (`open()` builds io's file
            // classes, `sys.stdout` is one), where no call site shows it.
            return true;
        }
        boolean named = false;
        for (Layouts.Cls c : selfAndBelow(k)) {
            Infer h = p.home.get(c);
            named |= h == null || h.facts == null || h.facts.namedOutside(c.def.name);
        }
        if (!named) {
            // Named nowhere but in their own bodies: nothing makes one, or
            // holds one as a value (WholeProgram.Module#namedOutside).
            return false;
        }
        for (Layouts.Cls c : selfAndBelow(k)) {
            if (p.constructed.contains(c) || p.looseRoots.contains(c) || p.clsId.containsKey(c)) {
                // Made, let go, or held as a class value: whoever holds it
                // may call it.
                return true;
            }
        }
        return false;
    }

    /// The modules the runtime imports by name (WholeProgram#infer).
    private static final Set<String> RUNTIME_MODULES = new HashSet<String>(
            java.util.Arrays.asList("builtins", "copyreg", "io"));

    private void constructs(Layouts.Cls k) {
        if (k != null && p.constructed.add(k)) {
            p.changed = true;
        }
    }

    /// Whether `d` is a compiled function nothing in the program reaches.
    /// Its body is not emitted: the function raises, naming itself, if a
    /// path this pass has no way to see calls it after all.
    boolean removed(FunctionDef d) {
        Fn f = p.fns.get(d);
        return p.done && f != null && !f.analysisOnly && !p.reached.contains(d);
    }

    private void escapeFn(Fn f) {
        if (f != null && !f.escapes) {
            f.escapes = true;
            p.changed = true;
        }
        if (f != null) {
            reach(f);
        }
        // Callers through a value are unknown, but the typed version is
        // entered from the generic one only through a guard that checks
        // every argument (TypedEmit.entry): a parameter keeps what its
        // direct callers pass, and one nobody calls directly takes the type
        // its uses suggest, once the pass has settled [#uncalled]. Guessed
        // here, at the escape, a guess of OBJECT came before the direct calls
        // were seen and every typed argument they passed then joined into
        // it (a registration record's `__init__`).
    }

    /// The type the uses of parameter `p` in `def` suggest: INT when it is
    /// a `range` bound, a list repeat count or an operand next to an int
    /// constant; FLOAT next to a float constant; OBJECT when the uses say
    /// nothing or disagree.
    static Ty guess(FunctionDef def, String p) {
        Ty[] g = {null};
        boolean[] bad = {false};
        for (Stmt s : def.body) {
            guessIn(s, p, g, bad);
        }
        return bad[0] || g[0] == null ? Ty.OBJECT : g[0];
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

    /// The compiled `__new__` constructing `k` runs, or null when it runs
    /// the builtin one.
    Fn newMethod(Layouts.Cls k) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.bodyNames.contains("__new__")) {
                FunctionDef d = bodyDef(c, "__new__");
                return d == null ? null : p.fns.get(d);
            }
        }
        return null;
    }

    /// `X.__new__(cls, ...)` for a builtin `X` or `object`, or
    /// `super().__new__(cls, ...)` reaching one: a new instance of the
    /// class `cls` is, made without running its `__init__`. Null when `c`
    /// is no such call.
    private Ty builtinNew(Call c, Fn fn, Scope sc) {
        if (!(c.func instanceof Attribute) || !"__new__".equals(((Attribute) c.func).attr) || c.args.isEmpty()
                || c.args.get(0) instanceof Starred) {
            return null;
        }
        Expr owner = ((Attribute) c.func).value;
        boolean builtin = owner instanceof Name && sc.resolve(((Name) owner).id) == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport
                && (Layouts.BUILTIN_NAMES.containsKey(((Name) owner).id) || "object".equals(((Name) owner).id));
        boolean sup = owner instanceof Call && ((Call) owner).func instanceof Name
                && "super".equals(((Name) ((Call) owner).func).id) && sc.resolve("super") == Scope.Binding.BUILTIN
                && fn != null && fn.cls != null && newMethod(fn.cls.parent) == null;
        if (!builtin && !sup && !inheritedNew(owner, fn, sc)) {
            return null;
        }
        Ty ct = type(c.args.get(0), fn, sc);
        for (int i = 1; i < c.args.size(); i++) {
            Expr x = c.args.get(i);
            escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        if (ct.k != Ty.K.CLS || ct.opt || ct.sites.length == 0) {
            escape(ct);
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        for (Layouts.Cls k : classesOf(ct)) {
            r = mix(r, Ty.inst(k, false));
        }
        return r;
    }

    /// Whether `owner.__new__` is the builtin one whatever `owner` holds:
    /// `owner` is an instance or class of compiled classes none of which,
    /// nor any subclass, defines or may be given a `__new__`
    /// (`self.__new__(self.__class__)` in a `__copy__`).
    private boolean inheritedNew(Expr owner, Fn fn, Scope sc) {
        if (!(owner instanceof Name) && !(owner instanceof Attribute)) {
            return false;
        }
        Ty o = type(owner, fn, sc);
        List<Layouts.Cls> ks = new ArrayList<Layouts.Cls>();
        if (o.k == Ty.K.INST && !o.opt) {
            ks.add(o.cls);
        } else if (o.k == Ty.K.CLS && !o.opt && o.sites.length > 0) {
            ks.addAll(classesOf(o));
        } else {
            return false;
        }
        for (Layouts.Cls k : ks) {
            if (newMethod(k) != null || !closedFor(k, "__new__")) {
                return false;
            }
            for (Layouts.Cls s : p.below(k)) {
                if (newMethod(s) != null || !closedFor(s, "__new__")) {
                    return false;
                }
            }
        }
        return true;
    }

    /// The type of the class object `k`.
    Ty classValue(Layouts.Cls k) {
        Integer id = p.clsId.get(k);
        if (id == null) {
            id = Integer.valueOf(p.clsById.size());
            p.clsById.add(k);
            p.clsId.put(k, id);
        }
        return Ty.cls(new int[] {id.intValue()});
    }

    /// Whether `k`, a class above it or one below it defines `__class__`.
    private boolean ownClassAttr(Layouts.Cls k) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.bodyNames.contains("__class__")) {
                return true;
            }
        }
        for (Layouts.Cls c : selfAndBelow(k)) {
            if (c.bodyNames.contains("__class__")) {
                return true;
            }
        }
        return false;
    }

    /// The type of class attribute `attr` of `k` when it compiles to a typed
    /// static field (Emitter#classStatics): `k`'s own body binds it exactly
    /// once, the whole program never stores the name as an attribute (strict
    /// mode refuses the computed stores that could), nothing between `type`
    /// and `k` can change how the class answers a read (no metaclass, no
    /// class decorator, one compiled base chain), and the value's type is
    /// known from the expression alone. Null otherwise.
    Ty classStatic(Layouts.Cls k, String attr) {
        // A computed store (`setattr(o, name, v)`) could rebind it unseen
        // when it may reach `k`, a class `k` inherits from or one below it
        // ([WholeProgram.Module#computedStoreReaches]); decided from the
        // closed world, whether or not strict mode refuses such stores.
        if (facts == null || k == null || !k.compiled || facts.attrStored(attr) || attr.startsWith("__")
                || k.def.decorators.size() > 0) {
            return null;
        }
        if (k.enumMeta) {
            // A member the compiler defined (EnumLowering): stored once,
            // right after the class, as an instance of exactly the class.
            List<String> mem = EnumLowering.members(k.def);
            return mem != null && mem.contains(attr) && Layouts.trailing(k.def).containsKey(attr) && !k.builtinOwn
                    && layoutHolds(k) ? Ty.inst(k, false) : null;
        }
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (!c.def.keywords.isEmpty() || c.def.bases.size() > 1 || c.builtin != null || c.extraBases
                    || c.parent == null && !c.def.bases.isEmpty() && !isName(c.def.bases.get(0), "object")) {
                return null;
            }
        }
        for (Layouts.Cls c : selfAndBelow(k)) {
            if (facts.computedStoreReaches(c.def)) {
                return null;
            }
        }
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (facts.computedStoreReaches(c.def)) {
                return null;
            }
        }
        Expr v = Layouts.onceValue(k, attr);
        if (v instanceof Constant) {
            Constant cv = (Constant) v;
            switch (cv.kind) {
                case STR:
                    return Ty.STR;
                case FLOAT:
                    return Ty.FLOAT;
                case TRUE:
                case FALSE:
                    return Ty.BOOL;
                default:
                    // An int stays on the class-constant fold
                    // (Layouts#classConstant): typed code holds an int in
                    // two parts, which one static field does not.
                    return null;
            }
        }
        if (v instanceof Call && ((Call) v).func instanceof Name && !k.bodyNames.contains(((Name) ((Call) v).func).id)) {
            Layouts.Cls o = calledClass(((Call) v).func, moduleScope);
            if (o != null && o.compiled && o.builtin == null && layoutHolds(o) && Layouts.resolver(o, "__get__") == null
                    && Layouts.resolver(o, "__new__") == null) {
                return Ty.inst(o, false);
            }
        }
        return v == null || v instanceof Constant ? null : staticExprType(k, v);
    }

    /// Class statics whose value expression is being typed, against a
    /// value that names another class's static naming this one.
    private final Set<Expr> typingStatic = java.util.Collections.newSetFromMap(new IdentityHashMap<Expr, Boolean>());

    /// The type of once-bound class attribute value `v` when the inference
    /// knows it from the module's scope: it names nothing of the class body
    /// (whose scope the module's is not), and is a str, float, bool, int or
    /// an instance of a compiled class that is no descriptor. Null otherwise.
    private Ty staticExprType(Layouts.Cls k, Expr v) {
        boolean[] local = new boolean[1];
        namesIn(v, k.bodyNames, local);
        if (local[0] || !typingStatic.add(v)) {
            return null;
        }
        try {
            Ty t = type(v, null, moduleScope);
            if (t == null || t.opt) {
                return null;
            }
            switch (t.k) {
                case STR:
                case FLOAT:
                case BOOL:
                case INT:
                    return t.k == Ty.K.INT ? Ty.INT : t;
                case INST:
                    Layouts.Cls o = t.cls;
                    return o != null && o.compiled && o.builtin == null && !o.builtinOwn && layoutHolds(o)
                            && Layouts.resolver(o, "__get__") == null ? t : null;
                default:
                    return null;
            }
        } finally {
            typingStatic.remove(v);
        }
    }

    private static void namesIn(Object o, Set<String> names, boolean[] hit) {
        if (o instanceof Name && names.contains(((Name) o).id) || o instanceof Lambda || o instanceof Comp) {
            hit[0] = true;
            return;
        }
        children(o, x -> namesIn(x, names, hit));
    }

    /// The compiled class `recv` names when `recv.attr` reads one of its
    /// typed static fields ([#classStatic]), or null.
    Layouts.Cls classStaticOf(Expr recv, String attr, Scope sc) {
        Layouts.Cls k = calledClassOrEnum(recv, sc);
        return k != null && classStatic(k, attr) != null ? k : null;
    }

    /// Whether the class body that binds `m` nearest up from `k` binds it
    /// by an assignment, not a `def`.
    private static boolean classAttr(Layouts.Cls k, String m) {
        Layouts.Cls res = Layouts.resolver(k, m);
        if (res == null) {
            return false;
        }
        for (Stmt s : res.def.body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(m)) {
                return false;
            }
        }
        return valueOf(res.def.body, m) != null;
    }

    /// What the last plain `n = value` of a class body assigns, or null.
    private static Expr valueOf(List<Stmt> body, String n) {
        Expr r = null;
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && ((Name) ((Assign) s).targets.get(0)).id.equals(n)) {
                r = ((Assign) s).value;
            }
        }
        return r;
    }

    /// The method of one of the classes of `tree` named `n`, or null.
    private Fn methodOf(List<Layouts.Cls> tree, String n) {
        for (Layouts.Cls c : tree) {
            for (Stmt s : c.def.body) {
                if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(n) && plainMethod((FunctionDef) s)
                        && ((FunctionDef) s).decorators.isEmpty()) {
                    Fn f = p.fns.get(s);
                    return f == null || f.analysisOnly ? null : f;
                }
            }
        }
        return null;
    }

    /// A bound method of one of `fs` -- a method, or a function stored in a
    /// class body, which reading it through an instance binds the same
    /// way -- or null when there is none.
    private Ty methValueOf(List<Fn> fs) {
        if (fs.isEmpty()) {
            return null;
        }
        Set<Integer> ids = new java.util.TreeSet<Integer>();
        for (Fn f : fs) {
            Integer id = p.methId.get(f);
            if (id == null) {
                id = Integer.valueOf(p.methById.size());
                p.methById.add(f);
                p.methId.put(f, id);
            }
            ids.add(id);
        }
        int[] r = new int[ids.size()];
        int i = 0;
        for (Integer id : ids) {
            r[i++] = id.intValue();
        }
        return Ty.meth(r);
    }

    /// A bound method of one of `fs`, or null when one has no id to give.
    private Ty methValue(List<FunctionDef> defs) {
        int[] ids = new int[defs.size()];
        for (int i = 0; i < ids.length; i++) {
            Fn f = p.fns.get(defs.get(i));
            if (f == null || f.cls == null) {
                return null;
            }
            Integer id = p.methId.get(f);
            if (id == null) {
                id = Integer.valueOf(p.methById.size());
                p.methById.add(f);
                p.methId.put(f, id);
            }
            ids[i] = id.intValue();
        }
        java.util.Arrays.sort(ids);
        return Ty.meth(ids);
    }

    /// Parameter `i` of `f` as its body reads it. The guard at the door
    /// cannot test which function a value is -- every one is an object to
    /// it -- so a body entered through it holds a function parameter as
    /// the generic object, and the functions it was seen to be are called
    /// from there.
    private Ty paramIn(Fn f, int i) {
        Ty t = f.params[i];
        if (entered(f) && hasFunc(t)) {
            escapeFuncs(t);
            return Ty.OBJECT;
        }
        return t;
    }

    private static boolean hasFunc(Ty t) {
        if (t.k == Ty.K.FUNC || t.k == Ty.K.METH) {
            return true;
        }
        if (t.k == Ty.K.UNION) {
            for (Ty m : t.members()) {
                if (m.k == Ty.K.FUNC || m.k == Ty.K.METH) {
                    return true;
                }
            }
        }
        return false;
    }

    /// A call of a value that is one of some functions or bound methods:
    /// each takes the arguments bound to its own parameters. Null when the
    /// value may be anything else, or the call is starred.
    private Ty callThrough(Call c, Ty ft, Fn fn, Scope sc) {
        List<Fn> callees = new ArrayList<Fn>();
        List<Integer> froms = new ArrayList<Integer>();
        if (!anyStarred(c.args) && callables(ft.nonNone(), callees, froms)) {
            // One of these functions or bound methods: each takes the
            // arguments bound to its own parameters, keywords by name, as a
            // direct call of it would. A None the value may also be raises.
            Ty ret = Ty.BOT;
            boolean bound = false;
            boolean door = false;
            for (int i = 0; i < callees.size(); i++) {
                Fn f = callees.get(i);
                int from = froms.get(i).intValue();
                if (raises(c, f.def, from)) {
                    // This one cannot take these arguments: the call raises
                    // TypeError before it runs.
                    continue;
                }
                bound |= from == 1;
                List<Expr> given = staticArgs(c, f.def, from);
                if (given == null) {
                    for (Expr x : c.args) {
                        escape(type(x, fn, sc));
                    }
                    for (Keyword kw : c.keywords) {
                        escape(type(kw.value, fn, sc));
                    }
                    escapeFn(f);
                } else {
                    List<Ty> args = new ArrayList<Ty>();
                    for (Expr x : given) {
                        args.add(type(x, fn, sc));
                    }
                    if (!callFn(f, args, from, binds(f, args.size() + from), true)) {
                        // An argument the typed version cannot be sure of:
                        // its guard picks the version that runs.
                        door = true;
                    }
                }
                ret = mix(ret, retOf(f, f.ret));
            }
            if (bound || door) {
                // Made generically: the bound method's guard picks the
                // version that runs.
                doors.add(c);
            }
            return ret;
        }
        return null;
    }

    /// The functions (from 0) and bound methods (from 1) `t` may be, into
    /// `fs` and `froms`; false when it may be anything else.
    private boolean callables(Ty t, List<Fn> fs, List<Integer> froms) {
        Ty[] ms = t.k == Ty.K.UNION ? t.members() : new Ty[] {t};
        for (Ty m : ms) {
            if (noCall(m)) {
                // Calling it raises: nothing runs.
                continue;
            }
            if (m.k == Ty.K.FUNC) {
                for (Fn f : funcsOf(m)) {
                    fs.add(f);
                    froms.add(Integer.valueOf(0));
                }
            } else if (m.k == Ty.K.METH) {
                for (Fn f : methsOf(m)) {
                    fs.add(f);
                    froms.add(Integer.valueOf(1));
                }
            } else {
                return false;
            }
        }
        return !fs.isEmpty();
    }

    /// Whether `t` is one of some functions or bound methods, or a value
    /// never called (None, a dict the same name reads): a
    /// container keeps it as it is, and a call through what it reads is
    /// followed to each (generated code holds it as an object; see
    /// [#loc]).
    private boolean callableSet(Ty t) {
        if (t == null || t.k == Ty.K.BOT) {
            return false;
        }
        Ty[] ms = t.k == Ty.K.UNION ? t.members() : new Ty[] {t};
        boolean any = false;
        for (Ty m : ms) {
            if (m.k == Ty.K.FUNC || m.k == Ty.K.METH) {
                any = true;
            } else if (!noCall(m)) {
                return false;
            }
        }
        return any;
    }

    /// Whether a value of `t` is never callable: calling one raises.
    private static boolean noCall(Ty t) {
        switch (t.k) {
            case NONE:
            case INT:
            case BOOL:
            case FLOAT:
            case STR:
            case BYTES:
            case LIST:
            case TUPLE:
            case DICT:
                return true;
            default:
                return false;
        }
    }

    /// Escapes the functions `t` may be, and nothing else of it.
    private void escapeFuncs(Ty t) {
        if (t.k == Ty.K.FUNC || t.k == Ty.K.METH) {
            escape(t);
        } else if (t.k == Ty.K.UNION) {
            for (Ty m : t.members()) {
                escapeFuncs(m);
            }
        }
    }

    /// Function `f` as a value.
    private Ty funcValue(Fn f) {
        Integer id = p.funcId.get(f);
        if (id == null) {
            id = Integer.valueOf(p.funcById.size());
            p.funcById.add(f);
            p.funcId.put(f, id);
        }
        return Ty.func(new int[] {id.intValue()});
    }

    /// The functions a `FUNC` type may be.
    private List<Fn> funcsOf(Ty t) {
        List<Fn> r = new ArrayList<Fn>();
        for (int id : t.sites) {
            r.add(p.funcById.get(id));
        }
        return r;
    }

    /// Whether `f` is a function nested in another or a lambda: analysed,
    /// entered only by the calls its value reaches.
    private static boolean nested(Fn f) {
        return f.analysisOnly && f.cls == null;
    }

    /// The scope owning closure variable `n` read or stored in `sc`, or null.
    private static Scope cellOwner(String n, Scope sc, Scope.Binding b) {
        Scope o = b == Scope.Binding.CELL ? sc : sc.owner(n);
        return o != null && (o.kind == Scope.Kind.FUNCTION || o.kind == Scope.Kind.LAMBDA) ? o : null;
    }

    /// The methods a `METH` type may be.
    private List<Fn> methsOf(Ty t) {
        List<Fn> r = new ArrayList<Fn>();
        for (int id : t.sites) {
            r.add(p.methById.get(id));
        }
        return r;
    }

    /// `v`, unless it is a bound method: those stay in locals and results,
    /// and one that would go further lets its methods go and is an object
    /// from there on.
    private Ty noMeth(Ty v) {
        if (v != null && v.k == Ty.K.METH) {
            for (Fn f : methsOf(v)) {
                escapeFn(f);
            }
            return Ty.UNSEEN;
        }
        return v;
    }

    /// The classes a `CLS` type may be.
    List<Layouts.Cls> classesOf(Ty t) {
        List<Layouts.Cls> r = new ArrayList<Layouts.Cls>();
        for (int s : t.sites) {
            r.add(p.clsById.get(s));
        }
        return r;
    }

    /// A call of a class object of type `t`: it constructs one of those
    /// classes, each as a direct call would.
    private Ty constructAny(Call c, Ty t, Fn fn, Scope sc, boolean simple) {
        Ty r = Ty.BOT;
        for (Layouts.Cls k : classesOf(t)) {
            r = mix(r, callKnown(c, null, k, fn, sc, simple));
        }
        return r;
    }

    private void escapeClass(Layouts.Cls k) {
        // Whoever holds the class makes instances of it.
        looseRoot(k);
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        if (o != null && o.typable.get("__init__") != null) {
            escapeFn(p.fns.get(o.typable.get("__init__")));
        }
    }

    /// The one compiled method `super().m` / `super(K, self).m` runs in a
    /// method of compiled class K, or null. Static only where the MRO is
    /// the parent chain: K and every compiled class below it have at most
    /// one base (a subclass `S(K, Mixin)` would put Mixin after K), the
    /// method is the parent chain's own and its class is not open (a
    /// store could replace it). Unresolved, every `super().__init__(...)`
    /// was a dynamic call and the parent's constructor took only objects.
    private Fn superTarget(Attribute a, Fn fn, Scope sc) {
        if (fn == null || fn.cls == null || sc != fn.def.scope || fn.names.isEmpty() //NOPMD CompareObjectsWithEquals - identity is meant
                || !(a.value instanceof Call) || !(((Call) a.value).func instanceof Name)
                || !"super".equals(((Name) ((Call) a.value).func).id)
                || sc.resolve("super") != Scope.Binding.BUILTIN) {
            return null;
        }
        Call s = (Call) a.value;
        if (!s.keywords.isEmpty() || !(s.args.isEmpty() || s.args.size() == 2
                && s.args.get(0) instanceof Name && calledClass(s.args.get(0), sc) == fn.cls //NOPMD CompareObjectsWithEquals - identity is meant
                && s.args.get(1) instanceof Name && ((Name) s.args.get(1)).id.equals(fn.names.get(0)))) {
            return null;
        }
        if (!singleChain(fn.cls)) {
            return mroSuperTarget(a.attr, fn.cls);
        }
        Layouts.Cls k = fn.cls;
        if (k.parent == null) {
            return null;
        }
        Layouts.Cls o = Layouts.resolver(k.parent, a.attr);
        FunctionDef m = o == null ? null : o.typable.get(a.attr);
        if (m == null || open.contains(o) || open.contains(k.parent)) {
            return null;
        }
        // The chain from the parent to the method's class is compiled and
        // single-based too, or the MRO could leave it.
        for (Layouts.Cls c = k.parent; c != o; c = c.parent) { //NOPMD CompareObjectsWithEquals - identity is meant
            if (c == null || c.def.bases.size() > 1) {
                return null;
            }
        }
        return p.fns.get(m);
    }

    /// `super().name` in a method of K where some class at or below K has
    /// several bases, so the MRO is not the parent chain. Every compiled
    /// class whose Java chain holds K has a C3 MRO known here; when the
    /// first class after K in each of them that binds `name` is one and
    /// the same O, on K's own Java chain, every instance that can run the
    /// method reaches O's method, and the call is made to it directly.
    /// Null where they differ, where an MRO holds a base that is not
    /// compiled (it could bind the name), where a class below K holds K
    /// only as a merged base (its instance is no Java K), or where a class
    /// between K and O, or O itself, is open to stores (a patch could
    /// rebind the name, as for the parent-chain case).
    private Fn mroSuperTarget(String name, Layouts.Cls k) {
        List<Layouts.Cls> cs = new ArrayList<Layouts.Cls>();
        cs.add(k);
        cs.addAll(p.below(k));
        Layouts.Cls target = null;
        for (Layouts.Cls c : cs) {
            boolean onChain = false;
            for (Layouts.Cls d = c; d != null && !onChain; d = d.parent) {
                onChain = d == k; //NOPMD CompareObjectsWithEquals - identity is meant
            }
            if (!onChain) {
                return null;
            }
            List<Layouts.Cls> bs = new ArrayList<Layouts.Cls>();
            bs.add(c);
            List<Layouts.Cls> mro = Layouts.c3(bs);
            if (mro == null) {
                return null;
            }
            for (Layouts.Cls x : mro) {
                if (compiledBases(x.def) != x.bases.size()) {
                    return null;
                }
            }
            int i = mro.indexOf(k);
            Layouts.Cls o = null;
            for (int j = i + 1; j < mro.size() && o == null; j++) {
                Layouts.Cls x = mro.get(j);
                if (open.contains(x)) {
                    return null;
                }
                if (Layouts.bodyBindings(x.def).contains(name)) {
                    o = x;
                }
            }
            if (o == null || target != null && target != o) { //NOPMD CompareObjectsWithEquals - identity is meant
                return null;
            }
            target = o;
        }
        boolean onK = false;
        for (Layouts.Cls d = k.parent; d != null && !onK; d = d.parent) {
            onK = d == target; //NOPMD CompareObjectsWithEquals - identity is meant
        }
        FunctionDef m = onK ? target.typable.get(name) : null;
        return m == null ? null : p.fns.get(m);
    }

    /// The bases of `d` that put a class in its MRO which binds names:
    /// every one but a typing marker and `object`.
    private static int compiledBases(ClassDef d) {
        int n = 0;
        for (Expr e : d.bases) {
            if (!Layouts.typingBase(e) && !(e instanceof Name && "object".equals(((Name) e).id))) {
                n++;
            }
        }
        return n;
    }

    /// Whether K's MRO is its parent chain for every instance: K, each
    /// class above it and each compiled class below it has one base.
    /// The bases of `d` that take part in its MRO at run time:
    /// `Generic[T]` adds only `typing.Generic`, which binds no method a
    /// compiled class could reach.
    private static int realBases(ClassDef d) {
        int n = 0;
        for (Expr e : d.bases) {
            if (!Layouts.typingBase(e)) {
                n++;
            }
        }
        return n;
    }

    /// The first base of `d` that is not a typing marker, or null.
    private static Expr firstRealBase(ClassDef d) {
        for (Expr e : d.bases) {
            if (!Layouts.typingBase(e)) {
                return e;
            }
        }
        return null;
    }

    /// What the builtin `n` answers whatever it is given, when that is
    /// one type; the emitter unboxes the generic call's result to it.
    /// Whether `t` is a value the runtime compares and adds as a primitive:
    /// a bool, an int, a float or a str, never None.
    private static boolean primitive(Ty t) {
        return !t.opt && (t.k == Ty.K.BOOL || t.k == Ty.K.INT || t.k == Ty.K.FLOAT || t.k == Ty.K.STR);
    }

    /// Whether every keyword of `c` is one of `names`.
    private static boolean keywordsAmong(Call c, String... names) {
        for (Keyword kw : c.keywords) {
            if (kw.arg == null || !java.util.Arrays.asList(names).contains(kw.arg)) {
                return false;
            }
        }
        return true;
    }

    /// What a builtin answers over values it handles as primitives:
    /// `sorted`, `min`, `max`, `sum` and `round` of bools, ints, floats and
    /// strs, which the runtime orders and adds with no method call. Null
    /// for anything else, which takes the general path.
    private Ty builtinValue(String n, Call c, List<Ty> args, Fn fn, Scope sc) {
        switch (n) {
            case "sorted": {
                if (args.size() != 1 || !keywordsAmong(c, "key", "reverse")) {
                    return null;
                }
                Ty a0 = args.get(0);
                Ty e = a0.k == Ty.K.LIST && !a0.opt ? elem(a0) : a0.k == Ty.K.STR && !a0.opt ? Ty.STR
                        : a0.k == Ty.K.DICT && !a0.opt && a0.sites.length > 0 ? dictKeyOf(a0) : null;
                if (e == null || e.k == Ty.K.BOT) {
                    return null;
                }
                for (Keyword kw : c.keywords) {
                    // The key function is called with each element.
                    escape(type(kw.value, fn, sc));
                }
                if (!primitive(e) && (c.keywords.isEmpty() || !"key".equals(c.keywords.get(0).arg)
                        || c.keywords.size() != 1)) {
                    // Ordered by their own comparison methods.
                    escape(e);
                }
                int s = site(c);
                siteJoin(s, e);
                return Ty.list(new int[] {s}, false);
            }
            case "min":
            case "max": {
                if (args.isEmpty() || !c.keywords.isEmpty()) {
                    return null;
                }
                Ty r = Ty.BOT;
                if (args.size() == 1) {
                    Ty a0 = args.get(0);
                    r = a0.k == Ty.K.LIST && !a0.opt ? elem(a0) : a0.k == Ty.K.STR && !a0.opt ? Ty.STR : null;
                } else {
                    for (Ty t : args) {
                        r = primitive(t) ? mix(r, t) : null;
                        if (r == null) {
                            break;
                        }
                    }
                }
                return r != null && primitive(r) ? r : null;
            }
            case "sum": {
                if (args.isEmpty() || args.size() > 2 || !c.keywords.isEmpty()) {
                    return null;
                }
                Ty a0 = args.get(0);
                Ty e = a0.k == Ty.K.LIST && !a0.opt ? elem(a0) : null;
                Ty start = args.size() == 2 ? args.get(1) : Ty.INT;
                if (e == null || !numeric(e) || !numeric(start)) {
                    return null;
                }
                return e.k == Ty.K.FLOAT || start.k == Ty.K.FLOAT ? Ty.FLOAT : Ty.INT;
            }
            case "round": {
                if (args.isEmpty() || args.size() > 2 || !c.keywords.isEmpty() || !numeric(args.get(0))) {
                    return null;
                }
                if (args.size() == 1) {
                    return Ty.INT;
                }
                return args.get(1).k == Ty.K.INT && !args.get(1).opt
                        ? (args.get(0).k == Ty.K.FLOAT ? Ty.FLOAT : Ty.INT) : null;
            }
            default:
                return null;
        }
    }

    /// A bool, an int or a float, never None.
    private static boolean numeric(Ty t) {
        return !t.opt && (t.k == Ty.K.BOOL || t.k == Ty.K.INT || t.k == Ty.K.FLOAT);
    }

    static Ty fixedReturn(String n) {
        switch (n) {
            case "hasattr":
            case "callable":
            case "issubclass":
            case "isinstance":
            case "all":
            case "any":
            case "bool":
                return Ty.BOOL;
            case "print":
                return Ty.NONE;
            case "len":
            case "hash":
            case "id":
            case "ord":
            case "int":
                // Each answers an int or raises: `__len__`, `__hash__`,
                // `__index__` and `__int__` must return one.
                return Ty.INT;
            case "float":
                return Ty.FLOAT;
            case "str":
                // `str(x)` calls `__str__`, which must return a str.
            case "repr":
            case "ascii":
            case "chr":
            case "format":
            case "hex":
            case "oct":
            case "bin":
                return Ty.STR;
            default:
                return null;
        }
    }

    private boolean singleChain(Layouts.Cls k) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (realBases(c.def) > 1) {
                return false;
            }
        }
        for (Layouts.Cls c : p.below(k)) {
            if (realBases(c.def) > 1) {
                return false;
            }
        }
        return true;
    }

    /// Whether `super().m` in `fn` runs no compiled method: the MRO is the
    /// parent chain and nothing compiled on it binds `m`, so it is the
    /// builtin base's (`type.__call__` under abc's ABCMeta, `object`'s).
    /// Treated as an unknown receiver, it reached every compiled `m` of the
    /// program: ABCMeta's `super().__call__(*args)` took Tag.__call__'s
    /// parameters with it.
    private boolean superBuiltin(Attribute a, Fn fn, Scope sc) {
        if ((fn == null || fn.cls == null) && sc.kind == Scope.Kind.FUNCTION && sc.parent != null
                && sc.parent.kind == Scope.Kind.CLASS && sc.parent.parent != null
                && sc.parent.parent.kind == Scope.Kind.MODULE && a.value instanceof Call
                && ((Call) a.value).func instanceof Name && "super".equals(((Name) ((Call) a.value).func).id)
                && sc.resolve("super") == Scope.Binding.BUILTIN) {
            // A class the build does not lay out (abc's ABCMeta, a
            // metaclass): builtin when every base is a builtin name.
            for (Stmt s : body) {
                if (s instanceof ClassDef && ((ClassDef) s).name.equals(sc.parent.name)) {
                    boolean builtin = ((ClassDef) s).keywords.isEmpty();
                    for (Expr e : ((ClassDef) s).bases) {
                        builtin &= e instanceof Name && moduleScope.resolve(((Name) e).id) == Scope.Binding.BUILTIN;
                    }
                    return builtin;
                }
            }
            return false;
        }
        if (fn == null || fn.cls == null || sc != fn.def.scope || !(a.value instanceof Call) //NOPMD CompareObjectsWithEquals - identity is meant
                || !(((Call) a.value).func instanceof Name) || !"super".equals(((Name) ((Call) a.value).func).id)
                || sc.resolve("super") != Scope.Binding.BUILTIN || !singleChain(fn.cls)) {
            return false;
        }
        return fn.cls.parent == null && realBases(fn.cls.def) <= 1
                && (firstRealBase(fn.cls.def) == null || calledClass(firstRealBase(fn.cls.def), sc) == null)
                || fn.cls.parent != null && Layouts.resolver(fn.cls.parent, a.attr) == null
                && rootIsBuiltin(fn.cls.parent);
    }

    private boolean rootIsBuiltin(Layouts.Cls k) {
        Layouts.Cls c = k;
        while (c.parent != null) {
            c = c.parent;
        }
        return firstRealBase(c.def) == null
                || p.home.get(c).calledClass(firstRealBase(c.def), p.home.get(c).moduleScope) == null;
    }

    /// The method a `super()` call `c` was resolved to, for the emitter.
    Fn superCall(Call c) {
        return p.superCalls.get(c);
    }

    /// `r.name(args)` on a receiver the pass cannot name: a call through
    /// the interface every compiled method `name` implements. Each one a
    /// receiver of that class could reach takes the arguments into its
    /// parameters, as a resolved call's callee does, so a caller reaching it
    /// through its door passes the guard into the typed body. Escaping all
    /// of them instead (what this replaced) left every method of a common
    /// name -- bs4's `get`, `find`, `append` -- taking and returning only
    /// objects, and their objects made the next receiver unknown in turn.
    /// The arguments still escape (the caller did that): the receiver may
    /// be no compiled class at all, a list whose `append` keeps them.
    /// Set while [#duckCall] runs for `super().m(...)` in a method of
    /// this class (see [#superOf]).
    private Layouts.Cls superClass;
    /// Whether the duck call being made is `super().m(...)`: it reaches a
    /// class only after the caller's in the MRO of `type(self)`, a class
    /// that derives from the target, so never one no class derives from.
    private boolean superCall;
    /// While [#duckCall] runs for a receiver known to be an instance of a
    /// class with no layout or of a subclass [#receiverBound]: the class
    /// definitions whose methods it can run. Null when nothing is known.
    private Set<ClassDef> duckBound;

    /// The compiled class K of `super().m` / `super(K, self).m` in one of
    /// K's methods, or null. Such a call never runs K's own `m` or one of a
    /// class below K: an MRO puts every subclass before its bases.
    private Layouts.Cls superOf(Attribute a, Fn fn, Scope sc) {
        if (fn == null || fn.cls == null || !(a.value instanceof Call) || !(((Call) a.value).func instanceof Name)
                || !"super".equals(((Name) ((Call) a.value).func).id)
                || sc.resolve("super") != Scope.Binding.BUILTIN) {
            return null;
        }
        Call s = (Call) a.value;
        if (s.args.isEmpty() && sc == fn.def.scope //NOPMD CompareObjectsWithEquals - identity is meant
                || s.args.size() == 2 && s.args.get(0) instanceof Name && calledClass(s.args.get(0), sc) == fn.cls) { //NOPMD CompareObjectsWithEquals - identity is meant
            return fn.cls;
        }
        return null;
    }

    /// Whether `super().m(...)` in a method of K runs only methods of K's
    /// compiled ancestors: every class above K is compiled, with no base
    /// besides its compiled parent, up to one deriving from object alone.
    /// The receiver then binds the first parameter of a method the pass
    /// knows, and goes nowhere else: escaping it made K and every class
    /// with it loose (bs4's `super(BeautifulStoneSoup, self).__init__(
    /// *args, **kwargs)` loosened BeautifulSoup).
    private boolean superWithin(Attribute a, Fn fn, Scope sc) {
        Layouts.Cls k = superOf(a, fn, sc);
        return k != null && chainKnown(k);
    }

    /// Whether `k` and every class above it are compiled, with no base
    /// besides the compiled parent and no attribute hook, up to a class
    /// deriving from object alone: every method an MRO walk from `k` can
    /// find is one the pass knows.
    private static boolean chainKnown(Layouts.Cls k) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (!c.compiled || c.extraBases || !c.def.keywords.isEmpty()
                    || c.bodyNames.contains("__getattr__") || c.bodyNames.contains("__getattribute__")) {
                return false;
            }
            if (c.parent == null && !(c.def.bases.isEmpty() || c.def.bases.size() == 1
                    && c.def.bases.get(0) instanceof Name && "object".equals(((Name) c.def.bases.get(0)).id))) {
                return false;
            }
        }
        return true;
    }

    /// Every name a string literal of the Java runtime spells
    /// (runtime-names.txt): the only methods a runtime object can answer.
    private static Set<String> runtimeNames;

    private static synchronized Set<String> runtimeMethodNames() {
        if (runtimeNames == null) {
            Set<String> s = new HashSet<String>();
            try (java.io.InputStream in = Infer.class.getResourceAsStream("/com/codename1/python/transpiler/runtime-names.txt")) {
                if (in == null) {
                    throw new IllegalStateException("runtime-names.txt is missing");
                }
                try (java.io.BufferedReader r = new java.io.BufferedReader(
                        new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))) {
                    for (String l = r.readLine(); l != null; l = r.readLine()) {
                        if (!l.isEmpty()) {
                            s.add(l);
                        }
                    }
                }
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
            runtimeNames = s;
        }
        return runtimeNames;
    }

    /// Whether a method called `n` can only be one of the program's own
    /// methods -- or what an answering `__getattr__` returns, called
    /// ([#answeredCall]) -- whatever object it is called on. Every object then has it
    /// as a slot of one interface: the program's methods of that name are
    /// its implementations, and the default -- the generic lookup -- raises
    /// AttributeError. So the call reaches those methods and nothing else:
    /// its arguments go to their parameters, its result is theirs. Not so
    /// when a runtime object may answer the name (a string literal of the
    /// runtime spells it), when Java classes are imported (their objects
    /// answer anything), when a module function or class of that name may
    /// be what a module value hands out, when an attribute of that name is
    /// stored (it may hold any callable), or when a class answers names it
    /// does not define.
    /// Whether `c` was typed through a method slot ([#closedName]).
    boolean slotCall(Call c) {
        return p.slotCalls.contains(c);
    }

    boolean closedName(String n) {
        if (!p.closed || facts == null || facts.javaAnswers(n) || runtimeMethodNames().contains(n)
                || facts.attrStored(n) || facts.methodPatched(n)) {
            return false;
        }
        if (p.defNames == null) {
            Set<String> names = new HashSet<String>();
            List<Fn> answering = new ArrayList<Fn>();
            for (Fn f : p.fns.values()) {
                if (f.cls == null) {
                    names.add(f.def.name);
                } else if (("__getattr__".equals(f.def.name) || "__getattribute__".equals(f.def.name))
                        && answers(f.def)) {
                    answering.add(f);
                }
            }
            // A module attribute that is not a function may hold any
            // callable (`mod.handler()`): every name a module's own
            // statements mention is open, as a class body's are.
            for (Infer m : p.modules.values()) {
                if (m.body != null) {
                    for (Stmt s : m.body) {
                        classBodyNames(s, names);
                    }
                }
            }
            for (Layouts.Cls k : p.home.keySet()) {
                names.add(k.def.name);
                // A class attribute that is not a method (`suiteClass =
                // TestSuite`) may hold any callable: every name the class
                // body's own statements mention is open.
                for (Stmt s : k.def.body) {
                    classBodyNames(s, names);
                }
            }
            p.answering = answering;
            p.defNames = names;
        }
        return !p.defNames.contains(n);
    }

    /// What a closed-name call returns on an object whose class answers
    /// names it lacks: each answering `__getattr__` of a live class
    /// returns a value that is then called with the arguments. A function
    /// or bound method is followed; an instance runs its `__call__`; a
    /// value no call accepts raises; anything else is not known, and its
    /// arguments go where they please.
    private Ty answeredCall(Ty recv, List<Ty> args, boolean positional, Fn fn, Scope sc) {
        Ty ret = Ty.BOT;
        for (Fn g : p.answering) {
            if (g.cls == null || !classLive(g.cls) || !mayBe(recv, g.cls)) {
                continue;
            }
            Ty t = retOf(g, g.ret);
            if (t == null || t.k == Ty.K.BOT) {
                continue;
            }
            for (Ty m : t.k == Ty.K.UNION ? t.members() : new Ty[] {t}) {
                List<Fn> fs = new ArrayList<Fn>();
                List<Integer> froms = new ArrayList<Integer>();
                if (noCall(m)) {
                    continue;
                }
                if (m.k == Ty.K.INST) {
                    ret = mix(ret, duckCall(m, "__call__", args, positional, null, fn, sc));
                } else if (callables(m, fs, froms)) {
                    for (int i = 0; i < fs.size(); i++) {
                        Fn f = fs.get(i);
                        int from = froms.get(i).intValue();
                        callFn(f, args, from, positional && binds(f, args.size() + from), true);
                        ret = mix(ret, retOf(f, f.ret));
                    }
                } else {
                    for (Ty a : args) {
                        escape(a);
                    }
                    return Ty.UNSEEN;
                }
            }
        }
        return ret;
    }

    /// Whether a value of type `t` may be an instance of `k` or of a class
    /// derived from it: only then can `k`'s `__getattr__` answer for it. A
    /// value of no known class may be anything; a builtin value (a str, a
    /// list) is never an instance of a compiled class.
    private boolean mayBe(Ty t, Layouts.Cls k) {
        for (Ty m : t.members()) {
            if (m.k == Ty.K.OBJECT || m.k == Ty.K.INST && (m.cls == null || isSelfOrAncestor(k, m.cls)
                    || isSelfOrAncestor(m.cls, k))) {
                return true;
            }
        }
        return false;
    }

    /// Whether a live class answers names it lacks ([#answeredCall]).
    private boolean answeringLive(Ty recv) {
        for (Fn g : p.answering) {
            if (g.cls != null && classLive(g.cls) && mayBe(recv, g.cls)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `__getattr__` `d` returns anything but `getattr(x, name)`
    /// of its own name parameter: such a return forwards the same slot to
    /// another object, which adds no method to it.
    private static boolean answers(FunctionDef d) {
        if (d.args.positional.size() < 2) {
            return returnsValue(d.body);
        }
        String nameParam = d.args.positional.get(1).name;
        boolean[] r = {false};
        for (Stmt s : d.body) {
            answers(s, nameParam, r);
        }
        return r[0];
    }

    private static void answers(Object o, String nameParam, boolean[] r) {
        if (o instanceof Return && ((Return) o).value != null) {
            Expr v = ((Return) o).value;
            if (!(v instanceof Call && ((Call) v).func instanceof Name
                    && "getattr".equals(((Name) ((Call) v).func).id) && ((Call) v).args.size() == 2
                    && ((Call) v).keywords.isEmpty() && ((Call) v).args.get(1) instanceof Name
                    && nameParam.equals(((Name) ((Call) v).args.get(1)).id))) {
                r[0] = true;
            }
            return;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        Infer.children(o, x -> answers(x, nameParam, r));
    }

    /// Whether a statement of the class body `sc` (not a nested function's
    /// body) mentions `n`.
    private boolean classBodyReads(Scope sc, String n) {
        Set<String> names = classBodyMentions.get(sc);
        return names != null && names.contains(n);
    }

    /// The attribute read each literal `getattr(x, "name")` is typed as.
    private final Map<Call, Attribute> literalGetattr = new IdentityHashMap<Call, Attribute>();

    /// What each class body's own statements mention, by class scope.
    private final Map<Scope, Set<String>> classBodyMentions = new IdentityHashMap<Scope, Set<String>>();

    /// Every name `o` mentions or imports, nested functions and classes
    /// not looked into.
    private static void classBodyNames(Object o, Set<String> names) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Name) {
            names.add(((Name) o).id);
        } else if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                names.add(a.asname != null ? a.asname : a.name);
            }
        } else if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                names.add(a.asname != null ? a.asname : a.name);
            }
        }
        Infer.children(o, x -> classBodyNames(x, names));
    }

    /// Whether a `return` with a value is among `body`'s statements, nested
    /// functions and classes not counted.
    private static boolean returnsValue(List<Stmt> body) {
        boolean[] r = {false};
        for (Stmt s : body) {
            returnsValue(s, r);
        }
        return r[0];
    }

    private static void returnsValue(Object o, boolean[] r) {
        if (o instanceof Return && ((Return) o).value != null) {
            r[0] = true;
            return;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        Infer.children(o, x -> returnsValue(x, r));
    }

    /// The class definitions an instance `recv` holds may take a method
    /// from, when `recv` is known to be an instance of the class `C` whose
    /// method `fn` is, or of a subclass of `C`: `C`'s own definition and
    /// every ancestor's, plus whatever derives from `C` [#derivesFrom]. That
    /// holds for `fn`'s first parameter when nothing rebinds it, and for a
    /// local bound once, to `X.__new__(self.__class__)`. Null when not
    /// known, or when an ancestor is not a class definition of the program.
    private Set<ClassDef> receiverBound(Expr recv, Fn fn) {
        if (!(recv instanceof Name) || fn == null || fn.cls == null || fn.def.args.positional.isEmpty()
                || !fn.def.decorators.isEmpty() || fn.def.scope.parent == null
                || fn.def.scope.parent.kind != Scope.Kind.CLASS || fn.def.scope.parent.parent != moduleScope) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        String self = fn.def.args.positional.get(0).name;
        Map<String, Integer> n = new HashMap<String, Integer>();
        for (Stmt s : fn.def.body) {
            DataclassInit.count(s, n);
        }
        String r = ((Name) recv).id;
        if (n.containsKey(self) || fn.def.scope.cells.contains(self)) {
            return null;
        }
        if (!r.equals(self)) {
            if (n.getOrDefault(r, 0) != 1 || fn.def.scope.cells.contains(r) || !newOfSelfClass(fn.def.body, r, self)) {
                return null;
            }
        }
        String cn = fn.def.scope.parent.name;
        if (!stable(cn)) {
            return null;
        }
        ClassDef c = Layouts.classNamed(body, cn);
        if (c == null) {
            return null;
        }
        Set<ClassDef> out = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
        return ancestors(c, this, out, 0) ? out : null;
    }

    /// Whether operand `i` of `c` (-1 the left one) is `Cls.attr`, a
    /// class of this module's attribute, compared only by identity: enum's
    /// `init is not Enum.__init__` calls nothing, where reading the method
    /// as a value escapes every `__init__` that may be it.
    private boolean identityOperand(Compare c, int i, Scope sc) {
        Expr x = i < 0 ? c.left : c.comparators.get(i);
        if (!(x instanceof Attribute) || !(((Attribute) x).value instanceof Name)) {
            return false;
        }
        String id = ((Name) ((Attribute) x).value).id;
        if (!globalClasses.containsKey(id) || sc.resolve(id) != Scope.Binding.GLOBAL) {
            return false;
        }
        for (int j = i < 0 ? 0 : i; j <= i + 1 && j < c.ops.size(); j++) {
            String op = c.ops.get(j);
            if (!"is".equals(op) && !"is not".equals(op)) {
                return false;
            }
        }
        return true;
    }

    /// Whether the body of class `d` defines a function named `name`.
    private static boolean definesFunction(ClassDef d, String name) {
        for (Stmt s : d.body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /// Whether `k` and every class up its parent chain name one base at
    /// most: nothing off that chain is in its MRO.
    private static boolean singleBased(Layouts.Cls k) {
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            if (c.def.bases.size() > 1 || !c.def.keywords.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private final Map<Layouts.Cls, Map<Layouts.Cls, Boolean>> sharedBelowSeen =
            new IdentityHashMap<Layouts.Cls, Map<Layouts.Cls, Boolean>>();

    /// Whether some class of the program derives from both `a` and `b`, so
    /// that an instance typed `a` can resolve a method of `b` through its
    /// MRO although neither is on the other's compiled chain: a mixin
    /// sibling (itsdangerous' `URLSafeTimedSerializer(URLSafeSerializerMixin,
    /// TimedSerializer)` reaches the mixin's `load_payload` from
    /// `Serializer.loads`).
    private boolean sharedBelow(Layouts.Cls a, Layouts.Cls b) {
        Map<Layouts.Cls, Boolean> m = sharedBelowSeen.computeIfAbsent(a,
                x -> new IdentityHashMap<Layouts.Cls, Boolean>());
        Boolean r = m.get(b);
        if (r == null) {
            r = Boolean.FALSE;
            for (Layouts.Cls k : new ArrayList<Layouts.Cls>(p.home.keySet())) {
                if (k != a && k != b && derivesFrom(k, a.def) && derivesFrom(k, b.def)) { //NOPMD CompareObjectsWithEquals - identity is meant
                    r = Boolean.TRUE;
                    break;
                }
            }
            m.put(b, r);
        }
        return r;
    }

    /// Whether `k`'s class derives from `c`, or may: true too when one of
    /// its ancestors is not a class definition this program resolves.
    private boolean derivesFrom(Layouts.Cls k, ClassDef c) {
        Set<ClassDef> a = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
        Infer h = p.home.get(k);
        return h == null || !ancestors(k.def, h, a, 0) || a.contains(c);
    }

    /// `K.m(self, ...)` on a class `K` of this module whose name the pass
    /// does not type (a class without a layout): the call is unbound, so
    /// every argument is the method's own, the first one included. Bound
    /// as `K`'s instance method it put `self` in the first parameter of
    /// every `m` of the program, and `Text.__init__(self)` typed another
    /// class's `__init__(self, v)` parameter as the caller's class. The
    /// one method `K` resolves is called when nothing can replace it;
    /// otherwise every method of the name is entered with anything.
    private Ty unboundCall(Call c, Attribute a, List<Ty> args, boolean simple, Fn fn, Scope sc) {
        Ty viaBase = baseCall(c, a, args, fn, sc);
        if (viaBase != null) {
            return viaBase;
        }
        if (!(a.value instanceof Name) || sc.resolve(((Name) a.value).id) != Scope.Binding.GLOBAL
                || globalClasses.containsKey(((Name) a.value).id) || !stable(((Name) a.value).id)) {
            return null;
        }
        ClassDef d = Layouts.classNamed(body, ((Name) a.value).id);
        if (d == null) {
            return null;
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        Layouts.Cls k = layouts.all().get(d);
        if (k == null && "__init__".equals(a.attr) && d.decorators.isEmpty() && d.keywords.isEmpty()
                && (d.bases.isEmpty() || d.bases.size() == 1 && d.bases.get(0) instanceof Name
                        && "object".equals(((Name) d.bases.get(0)).id))
                && !Layouts.bodyBindings(d).contains("__init__") && !classValueRead(d.name)) {
            // A class without a layout (pdfminer's `LTText`, a mixin) that
            // derives from object alone and binds no `__init__`, and whose
            // class object nothing holds as a value, so nothing can give it
            // one: `LTText.__init__(self)` is object's.
            for (Ty t : args) {
                escape(t);
            }
            return Ty.NONE;
        }
        if (k != null && simple && c.keywords.isEmpty() && closedFor(k, a.attr) && (singleBased(k) || definesFunction(d, a.attr))
                && !layouts.fieldOwners.containsKey(a.attr)) {
            Layouts.Cls o = Layouts.resolver(k, a.attr);
            FunctionDef m = o == null ? null : o.typable.get(a.attr);
            Fn f = m == null ? null : p.fns.get(m);
            if (f != null && !classLevel(o, a.attr)) {
                callFn(f, args, 0, binds(f, args.size()), true);
                return retOf(f, f.ret);
            }
            if (o == null && "__init__".equals(a.attr) && singleBased(k) && fromObject(k) && !chainInitStored(k)) {
                // Nothing up the chain binds it: object's, which does
                // nothing with its arguments (pdfminer's
                // `LTText.__init__(self)`).
                for (Ty t : args) {
                    escape(t);
                }
                return Ty.NONE;
            }
        }
        for (Ty t : args) {
            escape(t);
        }
        escapeMethods(a.attr);
        return Ty.OBJECT;
    }

    /// Whether any module may hold the class named `n` as a value: it is
    /// named anywhere but as a base, as the receiver of an attribute read
    /// or call, or as the class `isinstance`/`issubclass` tests against;
    /// imported under another name; or read off a module (`m.n`). A store
    /// to one of its attributes counts as holding it.
    private boolean classValueRead(String n) {
        boolean[] hit = {false};
        for (Infer in : p.modules.values()) {
            starFrom = in;
            for (Stmt s : in.body) {
                classValue(s, n, hit, false);
            }
        }
        starFrom = null;
        return hit[0];
    }

    private void classValue(Object o, String n, boolean[] hit, boolean storing) {
        if (hit[0]) {
            return;
        }
        if (o instanceof Assign || o instanceof AugAssign || o instanceof AnnAssign || o instanceof Node.Delete) {
            List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                    : o instanceof AnnAssign ? java.util.Collections.singletonList(((AnnAssign) o).target)
                    : o instanceof AugAssign ? java.util.Collections.singletonList(((AugAssign) o).target)
                    : ((Node.Delete) o).targets;
            for (Expr t : targets) {
                classValue(t, n, hit, true);
            }
            Expr v = o instanceof Assign ? ((Assign) o).value : o instanceof AnnAssign ? ((AnnAssign) o).value
                    : o instanceof AugAssign ? ((AugAssign) o).value : null;
            if (v != null) {
                classValue(v, n, hit, false);
            }
            return;
        }
        if (o instanceof ImportFrom) {
            for (Alias al : ((ImportFrom) o).names) {
                if (n.equals(al.name) && al.asname != null && !n.equals(al.asname) || "*".equals(al.name) && starBinds((ImportFrom) o, n)) {
                    hit[0] = true;
                }
            }
            return;
        }
        if (o instanceof Attribute) {
            Attribute a = (Attribute) o;
            if (n.equals(a.attr) || storing && a.value instanceof Name && n.equals(((Name) a.value).id)) {
                hit[0] = true;
                return;
            }
            if (a.value instanceof Name && n.equals(((Name) a.value).id)) {
                return;
            }
            classValue(a.value, n, hit, false);
            return;
        }
        if (o instanceof Call && ((Call) o).args.size() == 2 && ((Call) o).args.get(0) instanceof Name
                && (((Call) o).func instanceof Name && "cast".equals(((Name) ((Call) o).func).id)
                        || ((Call) o).func instanceof Attribute && "cast".equals(((Attribute) ((Call) o).func).attr))) {
            // `typing.cast(K, x)` only names the type.
            classValue(((Call) o).func, n, hit, false);
            classValue(((Call) o).args.get(1), n, hit, false);
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && ((Call) o).args.size() == 2
                && ("isinstance".equals(((Name) ((Call) o).func).id) || "issubclass".equals(((Name) ((Call) o).func).id))) {
            Call c = (Call) o;
            classValue(c.args.get(0), n, hit, false);
            Expr t = c.args.get(1);
            List<Expr> ks = t instanceof TupleExpr ? ((TupleExpr) t).elts : java.util.Collections.singletonList(t);
            for (Expr x : ks) {
                if (!(x instanceof Name)) {
                    classValue(x, n, hit, false);
                }
            }
            for (Keyword kw : c.keywords) {
                classValue(kw.value, n, hit, false);
            }
            return;
        }
        if (o instanceof ClassDef) {
            ClassDef d = (ClassDef) o;
            for (Expr x : d.bases) {
                if (!(x instanceof Name)) {
                    classValue(x, n, hit, false);
                }
            }
            for (Keyword kw : d.keywords) {
                classValue(kw.value, n, hit, false);
            }
            for (Expr x : d.decorators) {
                classValue(x, n, hit, false);
            }
            for (Stmt s : d.body) {
                classValue(s, n, hit, false);
            }
            return;
        }
        if (o instanceof Name) {
            if (n.equals(((Name) o).id)) {
                hit[0] = true;
            }
            return;
        }
        Infer.children(o, x -> classValue(x, n, hit, false));
    }

    /// Whether `from m import *` may bring in `n`: `m` is a module of the
    /// program that binds it at its top level, or star-imports in turn. A
    /// module outside the program cannot hold one of its classes.
    private boolean starBinds(ImportFrom f, String n) {
        return starFrom == null || starBinds(starFrom, f, n,
                java.util.Collections.newSetFromMap(new IdentityHashMap<Infer, Boolean>()));
    }

    private boolean starBinds(Infer from, ImportFrom f, String n, Set<Infer> seen) {
        List<Infer> targets = new ArrayList<Infer>();
        if (f.level == 0) {
            targets.add(p.modules.get(f.module));
        } else {
            // Relative: against the importer's package, which is the module
            // itself when it is a package's `__init__` -- both are tried.
            String[] parts = from.moduleName.split("\\.");
            for (int drop = f.level - 1; drop <= f.level; drop++) {
                if (drop > parts.length) {
                    continue;
                }
                StringBuilder q = new StringBuilder();
                for (int i = 0; i < parts.length - drop; i++) {
                    q.append(q.length() == 0 ? "" : ".").append(parts[i]);
                }
                if (f.module != null) {
                    q.append(q.length() == 0 ? "" : ".").append(f.module);
                }
                targets.add(p.modules.get(q.toString()));
            }
        }
        for (Infer m : targets) {
            if (m == null || !seen.add(m)) {
                continue;
            }
            if (m.moduleScope.assigned.contains(n)) {
                return true;
            }
            List<ImportFrom> stars = new ArrayList<ImportFrom>();
            for (Stmt s : m.body) {
                stars(s, stars);
            }
            for (ImportFrom s : stars) {
                if (starBinds(m, s, n, seen)) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Adds every `from x import *` under `o` to `out`.
    private static void stars(Object o, List<ImportFrom> out) {
        if (o instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) o).names) {
                if ("*".equals(a.name)) {
                    out.add((ImportFrom) o);
                }
            }
            return;
        }
        Infer.children(o, x -> stars(x, out));
    }

    /// The module [#classValueRead] is scanning.
    private Infer starFrom;

    /// Whether some module stores or deletes `X.__init__` by name for a
    /// class `X` on the parent chain of `k` ([#initStore]).
    private boolean chainInitStored(Layouts.Cls k) {
        Set<String> names = new HashSet<String>();
        for (Layouts.Cls c = k; c != null; c = c.parent) {
            names.add(c.def.name);
        }
        for (Infer in : p.modules.values()) {
            boolean[] hit = {false};
            for (Stmt s : in.body) {
                initStore(s, names, hit);
            }
            if (hit[0]) {
                return true;
            }
        }
        return false;
    }

    /// Whether the parent chain of `k` ends in a class whose only base is
    /// `object`, written or implied, and no class on it has a decorator.
    private static boolean fromObject(Layouts.Cls k) {
        Layouts.Cls c = k;
        while (true) {
            if (!c.def.decorators.isEmpty()) {
                return false;
            }
            if (c.parent == null) {
                break;
            }
            c = c.parent;
        }
        List<Expr> bases = c.def.bases;
        return bases.isEmpty() || bases.size() == 1 && bases.get(0) instanceof Name
                && "object".equals(((Name) bases.get(0)).id);
    }

    /// Whether `k.__init__` is object's: `k` names, once, a class this
    /// module defines with no decorator, every ancestor a class definition
    /// of the program none of which binds `__init__`, and no store anywhere
    /// in the program can give one an `__init__` by name.
    private boolean objectInit(Expr k, Scope sc) {
        if (!(k instanceof Name) || sc.resolve(((Name) k).id) != Scope.Binding.GLOBAL || moduleScope.hasStarImport
                || !stable(((Name) k).id) || p.dynStored.contains("__init__")
                || p.computedStoredNames.contains("__init__")) {
            return false;
        }
        ClassDef c = Layouts.classNamed(body, ((Name) k).id);
        Set<ClassDef> all = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
        if (c == null || !ancestors(c, this, all, 0)) {
            return false;
        }
        Set<String> names = new HashSet<String>();
        for (ClassDef d : all) {
            if (!d.decorators.isEmpty() || Layouts.bodyBindings(d).contains("__init__")) {
                return false;
            }
            names.add(d.name);
        }
        for (Infer in : p.modules.values()) {
            boolean[] hit = {false};
            for (Stmt s : in.body) {
                initStore(s, names, hit);
            }
            if (hit[0]) {
                return false;
            }
        }
        return true;
    }

    /// Whether `o` stores `X.__init__` (or deletes it) for a name `X` in
    /// `names`: a class object read by its name, which no type records.
    private static void initStore(Object o, Set<String> names, boolean[] hit) {
        if (hit[0]) {
            return;
        }
        List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                : o instanceof AnnAssign ? java.util.Collections.singletonList(((AnnAssign) o).target)
                : o instanceof AugAssign ? java.util.Collections.singletonList(((AugAssign) o).target)
                : o instanceof Node.Delete ? ((Node.Delete) o).targets : null;
        if (targets != null) {
            for (Expr t : targets) {
                if (t instanceof Attribute && "__init__".equals(((Attribute) t).attr)
                        && ((Attribute) t).value instanceof Name && names.contains(((Name) ((Attribute) t).value).id)) {
                    hit[0] = true;
                    return;
                }
            }
        }
        Infer.children(o, x -> initStore(x, names, hit));
    }

    /// Whether `body` binds `r`, at its top level, as
    /// `r = X.__new__(self.__class__)`.
    private static boolean newOfSelfClass(List<Stmt> body, String r, String self) {
        for (Stmt s : body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && r.equals(((Name) ((Assign) s).targets.get(0)).id) && ((Assign) s).value instanceof Call) {
                Call c = (Call) ((Assign) s).value;
                return c.func instanceof Attribute && "__new__".equals(((Attribute) c.func).attr)
                        && c.args.size() == 1 && c.keywords.isEmpty() && c.args.get(0) instanceof Attribute
                        && "__class__".equals(((Attribute) c.args.get(0)).attr)
                        && ((Attribute) c.args.get(0)).value instanceof Name
                        && self.equals(((Name) ((Attribute) c.args.get(0)).value).id);
            }
        }
        return false;
    }

    /// Adds `c` and every class it derives from to `out`; false when a base
    /// is not a class definition this program resolves.
    private static boolean ancestors(ClassDef c, Infer home, Set<ClassDef> out, int depth) {
        if (depth > 32 || !c.keywords.isEmpty() && !home.keepsBases(c)) {
            return false;
        }
        if (!out.add(c)) {
            return true;
        }
        for (Expr b : c.bases) {
            if (b instanceof Name && "object".equals(((Name) b).id)
                    && home.moduleScope.resolve("object") == Scope.Binding.BUILTIN) {
                continue;
            }
            ClassDef d = null;
            Infer h = null;
            if (b instanceof Name && home.stable(((Name) b).id)) {
                d = Layouts.classNamed(home.body, ((Name) b).id);
                h = d == null ? null : home;
            }
            if (d == null) {
                d = home.foreignClassDef(b);
                h = home.classHome(b);
            }
            if (d == null || h == null || !ancestors(d, h, out, depth + 1)) {
                return false;
            }
        }
        return true;
    }

    /// Whether the keywords of `c` leave its bases as written: only a
    /// `metaclass=` naming a class of the program that derives from `type`
    /// alone, defines no `mro`, and whose `__new__`, if any, hands its own
    /// bases parameter unchanged to every `__new__` it returns (openpyxl's
    /// `MetaStrict`).
    private boolean keepsBases(ClassDef c) {
        for (Keyword k : c.keywords) {
            if (!"metaclass".equals(k.arg)) {
                return false;
            }
            ClassDef m = null;
            if (k.value instanceof Name && stable(((Name) k.value).id)) {
                m = Layouts.classNamed(body, ((Name) k.value).id);
            }
            if (m == null) {
                m = foreignClassDef(k.value);
            }
            if (m == null || !m.keywords.isEmpty() || m.bases.size() != 1 || !(m.bases.get(0) instanceof Name)
                    || !"type".equals(((Name) m.bases.get(0)).id)) {
                return false;
            }
            for (Stmt s : m.body) {
                if (s instanceof FunctionDef && "mro".equals(((FunctionDef) s).name)) {
                    return false;
                }
                if (s instanceof FunctionDef && "__new__".equals(((FunctionDef) s).name)
                        && !forwardsBases((FunctionDef) s)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean forwardsBases(FunctionDef f) {
        if (f.args.positional.size() < 3 || !f.decorators.isEmpty()) {
            return false;
        }
        String bases = f.args.positional.get(2).name;
        Map<String, Integer> n = new HashMap<String, Integer>();
        for (Stmt s : f.body) {
            DataclassInit.count(s, n);
        }
        if (n.containsKey(bases)) {
            return false;
        }
        boolean[] ok = {true};
        forwardsBases(f.body, bases, ok);
        return ok[0];
    }

    private static void forwardsBases(Object o, String bases, boolean[] ok) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Return) {
            Expr v = ((Return) o).value;
            ok[0] &= v instanceof Call && ((Call) v).func instanceof Attribute
                    && "__new__".equals(((Attribute) ((Call) v).func).attr) && ((Call) v).args.size() >= 3
                    && ((Call) v).args.get(2) instanceof Name && bases.equals(((Name) ((Call) v).args.get(2)).id);
            return;
        }
        Infer.children(o, x -> forwardsBases(x, bases, ok));
    }

    private Ty duckCall(Ty r, String name, List<Ty> args0, boolean positional, Call kwCall, Fn fn, Scope sc) {
        Ty ret = Ty.BOT;
        Layouts.Cls sup = superClass;
        boolean chain = sup != null && singleChain(sup);
        for (Fn f : new ArrayList<Fn>(p.fns.values())) {
            if (f.cls == null || !f.def.name.equals(name) || r.k == Ty.K.INST
                    && !isSelfOrAncestor(f.cls, r.cls) && !isSelfOrAncestor(r.cls, f.cls)
                    && !sharedBelow(r.cls, f.cls)) {
                continue;
            }
            if (kwCall != null && raises(kwCall, f.def, 1) && raises(kwCall, f.def, 0)) {
                // These arguments bind to this method neither on an instance
                // nor through its class (`K.m(obj, ...)`): the call raises
                // TypeError before its body runs, so it is not a target.
                continue;
            }
            if (duckBound != null && !duckBound.contains(f.cls.def) && !derivesFrom(f.cls, fn.cls.def)
                    && !sharedBelow(fn.cls, f.cls)) {
                // The receiver is an instance of a class (or a subclass of
                // it) whose MRO this method's class is not on.
                continue;
            }
            if (superCall && !facts.mayBeBase(f.cls.def.name, p.reached)) {
                // Nothing in the program derives from the method's class,
                // so no MRO has it after another class.
                continue;
            }
            if (sup != null && (isSelfOrAncestor(sup, f.cls) || chain && !isSelfOrAncestor(f.cls, sup))) {
                // Not where super() goes: K itself or below it, or, when
                // the MRO is K's parent chain, anything off that chain.
                continue;
            }
            if (p.closed && !classLive(f.cls)) {
                // No instance can be the receiver: nothing at or below the
                // method's class is made. A later round that makes one
                // makes this call again.
                continue;
            }
            List<Ty> args = args0;
            boolean bound = positional;
            if (!positional && kwCall != null && !kwCall.keywords.isEmpty()) {
                // Keywords bind to this candidate's parameters by name,
                // as a resolved call's do (see [#staticArgs]).
                List<Expr> given = staticArgs(kwCall, f.def, 1);
                if (given != null) {
                    args = new ArrayList<Ty>();
                    for (Expr e : given) {
                        args.add(type(e, fn, sc));
                    }
                    bound = true;
                }
            }
            int np = f.def.args.positional.size();
            if (slotDuck && r.wide()) {
                // A closed-name call types its value from its targets, so
                // the receiver being the class itself counts too:
                // `K.m(obj, ...)` passes `obj` as self.
                unboundCall(f, args0, positional, kwCall, fn, sc);
                ret = mix(ret, retOf(f, f.ret));
            }
            if (positional && f.def.args.vararg == null
                    && (args.size() + 1 > np || args.size() + 1 < np - f.def.args.defaults.size())) {
                // These arguments cannot bind to this method: a call of it
                // raises before its body runs, so it is not what the call
                // reaches.
                continue;
            }
            // A receiver of no one type is any object, called through the
            // method's virtual slot (TypedEmit `tv_`), whose default runs the
            // generic lookup, and the method's guard picks the version that
            // runs. Every method of the name is a target. An argument is
            // evidence for a parameter nothing else says anything of; one
            // that would widen what the parameter already holds is not --
            // the guard sends a call it does not fit to the generic body,
            // where joining it in untyped the method for every caller.
            if (r.wide() && !shadow) {
                List<Ty> kept = new ArrayList<Ty>(args.size());
                for (int i = 0; i < args.size(); i++) {
                    Ty a = args.get(i);
                    Ty prm = i + 1 < f.params.length ? f.params[i + 1] : null;
                    if (prm == null || prm.k == Ty.K.BOT || prm.unseen() || Ty.join(prm, a).equals(prm)) {
                        kept.add(a);
                    } else {
                        // Not followed into this method: it reaches it as
                        // any object, and is entered from there.
                        escape(a);
                        kept.add(Ty.UNSEEN);
                    }
                }
                args = kept;
            }
            callFn(f, args, 1, bound && binds(f, args.size() + 1), true);
            ret = mix(ret, retOf(f, f.ret));
        }
        return ret;
    }

    /// Set while [#duckCall] types a closed-name call ([#closedName]).
    private boolean slotDuck;

    /// `f` called through its class, the arguments from its first
    /// parameter on, when they can bind there.
    private void unboundCall(Fn f, List<Ty> args0, boolean positional, Call kwCall, Fn fn, Scope sc) {
        List<Ty> args = args0;
        boolean bound = positional;
        if (!positional && kwCall != null && !kwCall.keywords.isEmpty()) {
            List<Expr> given = staticArgs(kwCall, f.def, 0);
            if (given == null) {
                callFn(f, args, 0, false, true);
                return;
            }
            args = new ArrayList<Ty>();
            for (Expr e : given) {
                args.add(type(e, fn, sc));
            }
            bound = true;
        }
        int np = f.def.args.positional.size();
        if (positional && f.def.args.vararg == null
                && (args.size() > np || args.size() < np - f.def.args.defaults.size())) {
            return;
        }
        callFn(f, args, 0, bound && binds(f, args.size()), true);
    }

    /// The methods of the names [#escapeMethods] let go whose class has
    /// become live since: each escapes as the others did.
    private void deadMethods() {
        for (Fn f : new ArrayList<Fn>(p.fns.values())) {
            if (f.cls != null && !f.escapes && p.escapedNames.contains(f.def.name) && classLive(f.cls)) {
                p.fnHome.get(f).escapeFn(f);
            }
        }
    }

    /// `Base.m(self, ...)` in a method of class `C` that names `Base` as
    /// one of its bases, the class not known here (defusedxml's
    /// `_XMLParser.__init__(self, ...)`, `_XMLParser` taken from a module
    /// imported by a computed name): `m` is found in `C`'s ancestors, never
    /// in another class. Each compiled one in the chain is followed as an
    /// unbound call, and when the method is past what the build knows of
    /// the chain, the arguments go there and nothing else does -- that
    /// method is entered through its guard like any call the pass does not
    /// follow. As a duck call, every `__init__` of the program escaped.
    /// Null when the call is not of that shape.
    private Ty baseCall(Call c, Attribute a, List<Ty> args, Fn fn, Scope sc) {
        if (fn == null || !(a.value instanceof Name) || c.args.isEmpty() || anyStarred(c.args)
                || !(c.args.get(0) instanceof Name) || fn.def.args.positional.isEmpty()
                || !((Name) c.args.get(0)).id.equals(fn.def.args.positional.get(0).name)
                || !fn.def.decorators.isEmpty() || shadow) {
            return null;
        }
        String base = ((Name) a.value).id;
        if (sc.resolve(base) != Scope.Binding.GLOBAL || globalClasses.containsKey(base)) {
            return null;
        }
        Ty bt = type(a.value, fn, sc);
        if (!(bt.wide() || bt.k == Ty.K.CLS && bt.sites.length == 0)) {
            // A class the pass knows: its method is followed as it is.
            return null;
        }
        ClassDef owner = enclosingClass(fn.def);
        if (owner == null || !owner.decorators.isEmpty() || !owner.keywords.isEmpty()) {
            return null;
        }
        boolean named = false;
        for (Expr e : owner.bases) {
            named |= e instanceof Name && base.equals(((Name) e).id);
        }
        if (!named || owner.bases.size() != 1 || Layouts.bodyBindings(owner).contains(base)) {
            return null;
        }
        // The first argument is the receiver of the method being walked,
        // never rebound in it.
        if (fn.def.scope.assigned.contains(((Name) c.args.get(0)).id)) {
            return null;
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        for (Ty t : args) {
            escape(t);
        }
        return "__init__".equals(a.attr) ? Ty.NONE : Ty.OBJECT;
    }

    /// The class whose body defines method `d` at module level, or null.
    private ClassDef enclosingClass(FunctionDef d) {
        for (Stmt s : body) {
            if (s instanceof ClassDef) {
                for (Stmt m : ((ClassDef) s).body) {
                    if (m == d) { //NOPMD CompareObjectsWithEquals - identity is meant
                        return (ClassDef) s;
                    }
                }
            }
        }
        return null;
    }

    /// Every compiled method `name` may be reached with arguments the pass
    /// cannot type.
    private void escapeMethods(String name) {
        for (Fn f : p.methodsNamed(name)) {
            if (!p.closed || classLive(f.cls)) {
                escapeFn(f);
            }
        }
        if (p.closed) {
            // A method of a class nothing makes yet escapes once something
            // does (see [#deadMethods]).
            p.escapedNames.add(name);
        }
    }

    /// Set once the fixpoint holds: the emitters then only read the result.
    /// An escape from a read of theirs would change types the pass already
    /// handed out, after the code using them was decided.
    Ty elem(Ty list) {
        return loc(elem0(list));
    }

    private Ty elem0(Ty list) {
        if (list.k != Ty.K.LIST || list.sites.length == 0) {
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        // A list generic code may have filled holds what typed code put in
        // it as a guess: the rest replace it (see [#siteJoin]).
        boolean g = p.allListsEscape;
        for (int s : list.sites) {
            Integer key = Integer.valueOf(s);
            g |= escapedSites.contains(key) || p.guessedSites.contains(key);
            r = mix(r, siteElem.get(s));
        }
        r = loc(r);
        if (g) {
            r = guessRead(r);
        }
        if (r.wide() && !p.done) {
            // A display's mixed positions read as one dynamic element: what
            // they hold is no longer followed.
            for (int s : list.sites) {
                Ty[] sl = siteSlots.get(Integer.valueOf(s));
                if (sl != null) {
                    for (Ty x : sl) {
                        escape(x);
                    }
                }
                // And what the elements were joined to: the read hands it
                // out untyped, where a call of it is not seen (dateutil's
                // `MO, TU, ... = [weekday(i) ...]` called `MO(+2)`).
                Ty e = siteElem.get(s);
                if (e != null && !e.wide()) {
                    escape(e);
                }
            }
        }
        return r;
    }

    /// `d[k] = v` on a dict of these sites.
    private void storeDict(Ty d, Ty k0, Ty v0) {
        Ty k = noMeth(k0);
        Ty v = noMeth(v0);
        if (d.sites.length == 0) {
            escape(d);
            escape(k);
            escape(v);
            return;
        }
        for (int s : d.sites) {
            siteKey.set(s, joinInto(siteKey.get(s), k));
            siteJoin(s, v);
        }
    }

    /// What a read of a dict of these sites answers.
    Ty dictVal(Ty d) {
        return loc(dictVal0(d));
    }

    private Ty dictVal0(Ty d) {
        if (d.k != Ty.K.DICT || d.sites.length == 0) {
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        boolean g = p.allListsEscape;
        for (int s : d.sites) {
            Integer key = Integer.valueOf(s);
            g |= escapedSites.contains(key) || p.guessedSites.contains(key);
            r = mix(r, siteElem.get(s));
        }
        r = loc(r);
        if (g && exactRead(r, d.sites) && !guessedAny(d.sites)) {
            // (A guessed dict may be any dict: what was seen stored into
            // this one is no answer for its elements. inspect's
            // `ann = getattr(f, "__annotations__", {}) or {}` is a guess
            // of the literal's dict, and `ann.get(n, _empty)` typed every
            // annotation `_empty`.)
            return r;
        }
        return g ? guessRead(r) : r;
    }

    /// What a read of the keys of a dict of these sites answers.
    Ty dictKeyOf(Ty d) {
        if (d.k != Ty.K.DICT || d.sites.length == 0) {
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        for (int s : d.sites) {
            r = mix(r, siteKey.get(s));
        }
        return loc(r);
    }

    /// The builtin type a call of the builtin `n` makes, when it is a type
    /// whose instances the pass has no richer kind for; null otherwise.
    static String madeBuiltin(String n) {
        if ("set".equals(n) || "frozenset".equals(n) || "object".equals(n) || "bytearray".equals(n)
                || "range".equals(n) || "slice".equals(n) || "property".equals(n) || "staticmethod".equals(n)
                || "classmethod".equals(n)) {
            return n;
        }
        return n.endsWith("Error") || n.endsWith("Exception") || n.endsWith("Warning") || "StopIteration".equals(n)
                || "KeyboardInterrupt".equals(n) || "SystemExit".equals(n) || "GeneratorExit".equals(n)
                || "StopAsyncIteration".equals(n) ? n : null;
    }

    /// A key a dict only looks up: hashed and compared, never kept. A
    /// compiled class compares and hashes as `type` does, by identity (its
    /// metaclass is `type` or one without comparisons, see
    /// Layouts#plainMetaclass), so it goes nowhere; anything else may run
    /// its own `__eq__` or `__hash__`. bs4's
    /// `self.element_classes.get(Tag, Tag)` let `Tag` be constructed from
    /// anywhere, and every subclass -- `BeautifulSoup` among them -- went
    /// loose with it.
    private void lookupKey(Ty k) {
        if (k.k == Ty.K.CLS && !k.opt && k.sites.length > 0) {
            return;
        }
        escape(k);
    }

    /// The dict methods that keep the values followed, or null for any
    /// other, which lets the dict go.
    private Ty dictMethod(Ty d, String m, List<Ty> args) {
        switch (m) {
            case "get":
                if (args.size() == 1 || args.size() == 2) {
                    lookupKey(args.get(0));
                    Ty dflt = args.size() == 2 ? args.get(1) : Ty.NONE;
                    Ty r = mix(dictVal(d), dflt);
                    if (r.wide()) {
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

    /// Joins `v` into what the elements of `site` hold. A value nothing
    /// knows, or one a generic body puts there, does not widen them: it
    /// makes their reads guesses (see [Program#guessedSites]).
    private void siteJoin(int site, Ty v0) {
        Ty v = callableSet(v0) ? v0 : noMeth(v0);
        // The generic view (shadow) stores what it still knows like any
        // other code: those are values the container really gets.
        if (v.wide() && !callableSet(v) || guessSeen) {
            if (p.guessedSites.add(Integer.valueOf(site))) {
                p.changed = true;
            }
            if (v.wide() && !callableSet(v)) {
                // Not kept: a function it may be is entered from where the
                // pass no longer follows it.
                if (p.lostSites.add(Integer.valueOf(site))) {
                    p.changed = true;
                }
                escapeFuncs(v);
                // What it held before is read through the lost site too,
                // and called from where the pass no longer follows it.
                Ty had = siteElem.get(site);
                if (had != null) {
                    escapeFuncs(had);
                }
                return;
            }
        }
        siteElem.set(site, joinInto(siteElem.get(site), v));
    }

    /// Whether a guessed container read may answer what its sites hold, as
    /// it is: no store into them was lost, and what they hold so far is
    /// nothing, None, or functions a call through it is followed to --
    /// none of which typed code keeps in a slot of its own. Answering OBJECT
    /// for it in an early round would stick for good.
    private boolean exactRead(Ty t, int[] sites) {
        if (p.done || p.allListsEscape || t == null || anyLost(sites)) {
            return false;
        }
        return t.k == Ty.K.BOT || t.k == Ty.K.NONE || callableSet(t);
    }

    private boolean guessedAny(int[] sites) {
        for (int s : sites) {
            if (p.guessedSites.contains(Integer.valueOf(s))) {
                return true;
            }
        }
        return false;
    }

    private boolean anyLost(int[] sites) {
        for (int s : sites) {
            Integer k = Integer.valueOf(s);
            if (p.lostSites.contains(k) || escapedSites.contains(k)) {
                return true;
            }
        }
        return false;
    }

    private void storeElem(Ty list, Ty v) {
        if (list.k != Ty.K.LIST || list.sites.length == 0) {
            escape(v);
            return;
        }
        unslot(list);
        for (int s : list.sites) {
            siteJoin(s, v);
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
    private void joinSlot(int site, int n, int i, Ty v0, boolean tuple) {
        Ty v = noMeth(v0);
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
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        for (int s : t.sites) {
            Ty[] sl = siteSlots.get(Integer.valueOf(s));
            if (sl == null || i >= sl.length) {
                return t.k == Ty.K.LIST ? elem(t) : Ty.UNSEEN;
            }
            r = mix(r, sl[i]);
        }
        return siteRead(loc(r), t.sites);
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
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        for (int i = 0; i < n; i++) {
            r = mix(r, slot(t, i));
        }
        return r;
    }

    private int site(Expr e) {
        // A generic version's own private list is a different object at
        // run time from the typed version's, and no typed code ever reads
        // it: what the generic walk puts in it stays out of the typed one.
        Map<Expr, Integer> of = shadow && privateLiterals().contains(e) ? p.shadowSiteOf : siteOf;
        Integer s = of.get(e);
        if (s == null) {
            s = Integer.valueOf(siteElem.size());
            siteElem.add(Ty.BOT);
            siteKey.add(Ty.BOT);
            of.put(e, s);
        }
        return s.intValue();
    }

    /// The list literals assigned to a local the function uses only
    /// through the list's own methods, `len`, a truth test or a `for` --
    /// none of which hands the list itself to other code -- and the tuple
    /// literals put into those lists, over every function.
    private Set<Expr> privateLiterals() {
        if (p.privateLits == null) {
            Set<Expr> out = java.util.Collections.newSetFromMap(new IdentityHashMap<Expr, Boolean>());
            for (FunctionDef d : p.fns.keySet()) {
                Set<String> names = new HashSet<String>();
                for (Stmt s : d.body) {
                    listAssigns(s, names);
                }
                for (String n : names) {
                    boolean[] ok = {true};
                    List<Expr> lits = new ArrayList<Expr>();
                    for (Stmt s : d.body) {
                        privateUse(s, n, ok, lits);
                    }
                    if (ok[0]) {
                        out.addAll(lits);
                    }
                }
            }
            p.privateLits = out;
        }
        return p.privateLits;
    }

    private static void listAssigns(Object o, Set<String> names) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Assign && ((Assign) o).targets.size() == 1 && ((Assign) o).targets.get(0) instanceof Name
                && ((Assign) o).value instanceof ListExpr) {
            names.add(((Name) ((Assign) o).targets.get(0)).id);
        } else if (o instanceof AnnAssign && ((AnnAssign) o).target instanceof Name
                && ((AnnAssign) o).value instanceof ListExpr) {
            names.add(((Name) ((AnnAssign) o).target).id);
        }
        Infer.children(o, x -> listAssigns(x, names));
    }

    /// The list methods that neither return the list nor a new list of
    /// its elements.
    private static final Set<String> PRIVATE_LIST_METHODS = new HashSet<String>(java.util.Arrays.asList(
            "append", "extend", "pop", "insert", "clear", "reverse", "remove", "index", "count"));

    private static void privateUse(Object o, String n, boolean[] ok, List<Expr> lits) {
        if (!ok[0]) {
            return;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            // A nested scope sees the list too: give up on it.
            boolean[] seen = {false};
            mentions(o, n, seen);
            ok[0] &= !seen[0];
            return;
        }
        Expr made = o instanceof Assign && ((Assign) o).targets.size() == 1 && ((Assign) o).targets.get(0) instanceof Name
                && n.equals(((Name) ((Assign) o).targets.get(0)).id) ? ((Assign) o).value
                : o instanceof AnnAssign && ((AnnAssign) o).target instanceof Name
                && n.equals(((Name) ((AnnAssign) o).target).id) ? ((AnnAssign) o).value : null;
        if (made != null || o instanceof AnnAssign && ((AnnAssign) o).target instanceof Name
                && n.equals(((Name) ((AnnAssign) o).target).id)) {
            if (!(made instanceof ListExpr)) {
                ok[0] = false;
                return;
            }
            lits.add(made);
            for (Expr x : ((ListExpr) made).elts) {
                if (x instanceof TupleExpr) {
                    lits.add(x);
                }
            }
            privateUse(made, n, ok, lits);
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ((Attribute) ((Call) o).func).value instanceof Name
                && n.equals(((Name) ((Attribute) ((Call) o).func).value).id)) {
            String m = ((Attribute) ((Call) o).func).attr;
            if (!PRIVATE_LIST_METHODS.contains(m)) {
                ok[0] = false;
                return;
            }
            for (Expr a : ((Call) o).args) {
                if (("append".equals(m) || "insert".equals(m)) && a instanceof TupleExpr) {
                    lits.add(a);
                } else if ("extend".equals(m) && a instanceof Comp && ((Comp) a).kind == Comp.Kind.GENERATOR
                        && ((Comp) a).elt instanceof TupleExpr) {
                    lits.add(((Comp) a).elt);
                }
                privateUse(a, n, ok, lits);
            }
            for (Keyword k : ((Call) o).keywords) {
                privateUse(k.value, n, ok, lits);
            }
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && "len".equals(((Name) ((Call) o).func).id)
                && ((Call) o).args.size() == 1 && ((Call) o).args.get(0) instanceof Name
                && n.equals(((Name) ((Call) o).args.get(0)).id)) {
            return;
        }
        if ((o instanceof While || o instanceof If) && (o instanceof While ? ((While) o).test : ((If) o).test) instanceof Name
                && n.equals(((Name) (o instanceof While ? ((While) o).test : ((If) o).test)).id)) {
            for (Stmt s : o instanceof While ? ((While) o).body : ((If) o).body) {
                privateUse(s, n, ok, lits);
            }
            for (Stmt s : o instanceof While ? ((While) o).orelse : ((If) o).orelse) {
                privateUse(s, n, ok, lits);
            }
            return;
        }
        if (o instanceof For && ((For) o).iter instanceof Name && n.equals(((Name) ((For) o).iter).id)) {
            privateUse(((For) o).target, n, ok, lits);
            for (Stmt s : ((For) o).body) {
                privateUse(s, n, ok, lits);
            }
            for (Stmt s : ((For) o).orelse) {
                privateUse(s, n, ok, lits);
            }
            return;
        }
        if (o instanceof Name && n.equals(((Name) o).id)) {
            // The list itself, handed on or rebound.
            ok[0] = false;
            return;
        }
        Infer.children(o, x -> privateUse(x, n, ok, lits));
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
            boolean guess = false;
            for (Fn f : g) {
                r = mix(r, f.ret);
                guess |= p.guessedReturns.contains(f);
            }
            for (Fn f : g) {
                f.ret = joinInto(f.ret, r);
                // One way of returning for the group: a virtual call
                // reads each the same.
                if (guess && p.guessedReturns.add(f)) {
                    p.changed = true;
                }
            }
        }
    }

    // ---- walking ---------------------------------------------------------

    /// `fn` is the compiled function being walked, or null where names are
    /// not tracked (the module body tracks globals instead).
    private void walkBlock(List<Stmt> body, Fn fn, Scope sc) {
        Map<Map.Entry<Scope, String>, Ty> saved = null;
        try {
            for (int i = 0; i < body.size(); i++) {
                Stmt s = body.get(i);
                walk(s, fn, sc);
                // `if not isinstance(x, K): raise ...` -- below, x is a K.
                if (s instanceof If && ((If) s).orelse.isEmpty() && endsAbruptly(((If) s).body)) {
                    Map.Entry<String, Ty> t = isinstanceTest(((If) s).test, true, fn, sc);
                    if (t != null && !storesIn(body.subList(i + 1, body.size()), t.getKey())) {
                        if (saved == null) {
                            saved = new HashMap<Map.Entry<Scope, String>, Ty>(narrowing);
                        }
                        narrowing.put(nk(sc, t.getKey()), t.getValue());
                    }
                }
            }
        } finally {
            if (saved != null) {
                narrowing.clear();
                narrowing.putAll(saved);
            }
        }
    }

    /// Walks `body`, the branch of `test` taken when it holds (fails, when
    /// `negated`), with what the test proves of a local in force.
    private void narrowed(Expr test, boolean negated, List<Stmt> body, Fn fn, Scope sc) {
        List<Map.Entry<String, Ty>> ts = new ArrayList<Map.Entry<String, Ty>>();
        if (!negated && test instanceof BoolOp && ((BoolOp) test).isAnd) {
            for (Expr v : ((BoolOp) test).values) {
                Map.Entry<String, Ty> t = isinstanceTest(v, false, fn, sc);
                if (t != null) {
                    ts.add(t);
                }
            }
        } else {
            Map.Entry<String, Ty> t = isinstanceTest(test, negated, fn, sc);
            if (t != null) {
                ts.add(t);
            }
        }
        Map<Map.Entry<Scope, String>, Ty> saved = new HashMap<Map.Entry<Scope, String>, Ty>(narrowing);
        try {
            for (Map.Entry<String, Ty> t : ts) {
                if (!storesIn(body, t.getKey())) {
                    narrowing.put(nk(sc, t.getKey()), t.getValue());
                }
            }
            walkBlock(body, fn, sc);
        } finally {
            narrowing.clear();
            narrowing.putAll(saved);
        }
    }

    /// The local names narrowed by an enclosing `isinstance` test, and to
    /// what ([#isinstanceTest]); only while their region is walked.
    private final Map<Map.Entry<Scope, String>, Ty> narrowing = new HashMap<Map.Entry<Scope, String>, Ty>();

    private static Map.Entry<Scope, String> nk(Scope sc, String n) {
        return new java.util.AbstractMap.SimpleImmutableEntry<Scope, String>(sc, n);
    }
    /// Each read of a narrowed name and the type it has there, kept for
    /// the emitters, which read types after the walk.
    final Map<Name, Ty> narrowedAt = new IdentityHashMap<Name, Ty>();

    /// For `isinstance(x, K)` (or, with `negated`, `not isinstance(x, K)`)
    /// on a plain local x of this function: x and what it is when the
    /// test holds (fails, when negated). K must be compiled classes, or a
    /// builtin that x holds among values of no compiled class -- a str
    /// subclass instance is a str, but not a Java String. Null otherwise.
    private Map.Entry<String, Ty> isinstanceTest(Expr test, boolean negated, Fn fn, Scope sc) {
        if (negated) {
            if (!(test instanceof UnaryOp) || !"not".equals(((UnaryOp) test).op)) {
                return failedIsinstance(test, fn, sc);
            }
            test = ((UnaryOp) test).operand;
        }
        if (fn == null || sc != fn.def.scope || !(test instanceof Call)) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        Call c = (Call) test;
        if (!isName(c.func, "isinstance") || sc.resolve("isinstance") == Scope.Binding.LOCAL
                || moduleScope.assigned.contains("isinstance") || c.args.size() != 2 || !c.keywords.isEmpty()
                || !(c.args.get(0) instanceof Name)) {
            return null;
        }
        String x = ((Name) c.args.get(0)).id;
        if (sc.resolve(x) != Scope.Binding.LOCAL || fn.openSelf && fn.names.get(0).equals(x)) {
            return null;
        }
        Ty k = type(c.args.get(1), fn, sc);
        if (k.k == Ty.K.CLS && !k.opt && k.sites.length > 0) {
            Ty r = Ty.BOT;
            for (Layouts.Cls o : classesOf(k)) {
                if (!layoutHolds(o)) {
                    // An instance the test passes may not be of o's Java
                    // class: held as it was.
                    return null;
                }
                r = Ty.join(r, Ty.inst(o, false));
            }
            if (r.k == Ty.K.INST) {
                tested(fn, x, r);
            }
            // A class with no Java layout is held as a plain object.
            return r.k == Ty.K.INST && r.cls.compiled ? new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, r) : null;
        }
        Ty cur = fn.locals.get(x);
        if (cur == null) {
            int pi = fn.names.indexOf(x);
            cur = pi >= 0 && pi < fn.params.length ? fn.params[pi] : null;
        }
        if (cur == null || cur.k == Ty.K.OBJECT || !(c.args.get(1) instanceof Name)
                || sc.resolve(((Name) c.args.get(1)).id) != Scope.Binding.GLOBAL
                || moduleScope.assigned.contains(((Name) c.args.get(1)).id)) {
            return null;
        }
        String bn = ((Name) c.args.get(1)).id;
        Ty hit = null;
        for (Ty m : cur.members()) {
            if (m.k == Ty.K.INST || m.k == Ty.K.OBJECT) {
                return null;
            }
            if ("str".equals(bn) && m.k == Ty.K.STR || "list".equals(bn) && m.k == Ty.K.LIST) {
                hit = m.nonNone();
            }
        }
        return hit == null ? null : new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, hit);
    }

    /// Where `isinstance(x, K)` failed, x a plain local of compiled class C:
    /// x is of a live class at or below C and not at or below K, so it is
    /// the join of those -- the program is closed, every class is known.
    /// markdownify's `process_element` hands the `else` of
    /// `isinstance(node, NavigableString)` to `process_tag`, which then
    /// reads a `Tag`, not a `PageElement` that has no `children`. Null when
    /// that is no narrower than C, or a class involved has no Java layout.
    private Map.Entry<String, Ty> failedIsinstance(Expr test, Fn fn, Scope sc) {
        if (fn == null || sc != fn.def.scope || !(test instanceof Call)) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        Call c = (Call) test;
        if (!isName(c.func, "isinstance") || sc.resolve("isinstance") == Scope.Binding.LOCAL
                || moduleScope.assigned.contains("isinstance") || c.args.size() != 2 || !c.keywords.isEmpty()
                || !(c.args.get(0) instanceof Name)) {
            return null;
        }
        String x = ((Name) c.args.get(0)).id;
        if (sc.resolve(x) != Scope.Binding.LOCAL || fn.openSelf && fn.names.get(0).equals(x)) {
            return null;
        }
        Ty cur = fn.locals.get(x);
        if (cur == null) {
            int pi = fn.names.indexOf(x);
            cur = pi >= 0 && pi < fn.params.length ? fn.params[pi] : null;
        }
        if (cur != null && cur.k != Ty.K.INST && cur.k != Ty.K.OBJECT && cur.k != Ty.K.BOT) {
            return failedBuiltin(x, cur, c.args.get(1), sc);
        }
        if (cur == null || cur.k != Ty.K.INST || !cur.cls.compiled) {
            return null;
        }
        Ty k = type(c.args.get(1), fn, sc);
        if (k.k != Ty.K.CLS || k.opt || k.sites.length == 0) {
            return null;
        }
        Set<Layouts.Cls> out = java.util.Collections.newSetFromMap(new IdentityHashMap<Layouts.Cls, Boolean>());
        for (Layouts.Cls o : classesOf(k)) {
            // Only what is left is read as a Java class: the classes the
            // test passes need no layout of their own.
            out.add(o);
            out.addAll(p.below(o));
        }
        List<Layouts.Cls> all = new ArrayList<Layouts.Cls>();
        all.add(cur.cls);
        all.addAll(p.below(cur.cls));
        Ty r = Ty.BOT;
        for (Layouts.Cls o : all) {
            if (out.contains(o) || !exactLive(o)) {
                continue;
            }
            if (!layoutHolds(o)) {
                return null;
            }
            r = Ty.join(r, Ty.inst(o, false));
        }
        if (r.k == Ty.K.BOT && !cur.opt && !p.done) {
            // No live class gets there yet: what reaches it so far is no
            // evidence for what it is passed. Otherwise the first pass's
            // guess sticks -- markdownify's children read as Comment,
            // process_tag widened to PageElement, whose `children` resolves
            // to nothing, and nothing ever grows past it.
            return new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, Ty.BOT);
        }
        if (r.k != Ty.K.INST || r.cls == cur.cls || !r.cls.compiled) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        return new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, cur.opt ? Ty.inst(r.cls, true) : r);
    }

    /// [#failedIsinstance] for a local of builtin kinds and a test naming
    /// builtin types: what is left are the members the test does not
    /// match -- a Java String is always a str, a PyList a list. With none
    /// left, nothing typed reaches the `else` yet. itsdangerous's
    /// `_secret_keys` iterated its key after `isinstance(secret_key, (str,
    /// bytes))` returned, and an early pass's str yielded the str that
    /// typed every later key, bytes included.
    private Map.Entry<String, Ty> failedBuiltin(String x, Ty cur, Expr kinds, Scope sc) {
        Set<Ty.K> hit = java.util.EnumSet.noneOf(Ty.K.class);
        for (Expr e : kinds instanceof TupleExpr ? ((TupleExpr) kinds).elts : java.util.Collections.singletonList(kinds)) {
            if (!(e instanceof Name) || sc.resolve(((Name) e).id) != Scope.Binding.GLOBAL
                    && sc.resolve(((Name) e).id) != Scope.Binding.BUILTIN
                    || moduleScope.assigned.contains(((Name) e).id)) {
                return null;
            }
            Ty.K kk = BUILTIN_KINDS.get(((Name) e).id);
            if (kk != null) {
                hit.add(kk);
            }
        }
        Ty r = Ty.BOT;
        for (Ty m : cur.members()) {
            if (!BUILTIN_KINDS.containsValue(m.k)) {
                return null;
            }
            if (!hit.contains(m.k)) {
                r = Ty.join(r, m.nonNone());
            }
        }
        if (r.k == Ty.K.BOT) {
            return cur.opt || p.done ? null : new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, Ty.BOT);
        }
        if (cur.opt) {
            r = r.orNone();
        }
        return r.equals(cur) ? null : new java.util.AbstractMap.SimpleImmutableEntry<String, Ty>(x, r);
    }

    /// The builtin types whose every instance is held as one Java kind;
    /// int and bool are not among them (a bool is an int).
    private static final Map<String, Ty.K> BUILTIN_KINDS = new HashMap<String, Ty.K>();

    static {
        BUILTIN_KINDS.put("str", Ty.K.STR);
        BUILTIN_KINDS.put("bytes", Ty.K.BYTES);
        BUILTIN_KINDS.put("list", Ty.K.LIST);
        BUILTIN_KINDS.put("tuple", Ty.K.TUPLE);
        BUILTIN_KINDS.put("dict", Ty.K.DICT);
    }

    /// `isinstance(x, K)` is evidence about `x` as much as a store is: the
    /// code expects a K there, from callers or sources the pass may not
    /// see, so K joins the parameter's or local's type.
    private void tested(Fn fn, String x, Ty k) {
        int pi = fn.names.indexOf(x);
        if (pi >= 0 && pi < fn.params.length) {
            if (!fn.params[pi].unseen() && fn.params[pi].k != Ty.K.BOT) {
                fn.params[pi] = joinInto(fn.params[pi], k);
            }
        } else {
            Ty cur = fn.locals.get(x);
            if (cur != null && cur.k != Ty.K.BOT && !cur.unseen()) {
                fn.locals.put(x, joinInto(cur, k));
            }
        }
    }

    private static boolean storesIn(List<Stmt> body, String n) {
        List<String> out = new ArrayList<String>();
        for (Stmt s : body) {
            boundIn(s, out);
            if (deletes(s, n)) {
                return true;
            }
        }
        return out.contains(n);
    }

    private static boolean deletes(Object o, String n) {
        if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                if (t instanceof Name && ((Name) t).id.equals(n)) {
                    return true;
                }
            }
        }
        boolean[] r = {false};
        children(o, x -> r[0] |= deletes(x, n));
        return r[0];
    }

    private void walk(Stmt s, Fn fn, Scope sc) {
        line = s.line;
        // What a statement stores is a guess only by what it read itself.
        guessSeen = false;
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
            // the result to a later pass, where OBJECT now would stay for good.
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
                Ty ge = !(r.value instanceof Comp) ? null : shadow ? Ty.OBJECT : genElems.get(r.value);
                if (ge != null) {
                    yieldInto(fn, ge);
                }
                fn.ret = joinInto(fn.ret, v);
                if (guessSeen && !shadow && p.guessedReturns.add(fn)) {
                    p.changed = true;
                }
            } else {
                escape(v);
            }
        } else if (s instanceof If) {
            If i = (If) s;
            type(i.test, fn, sc);
            boolean typeOnly = typeChecking(i.test);
            if (typeOnly) {
                // Imports for annotations only: they never run, so they
                // keep no module live. The block is still typed.
                typeCheckingDepth++;
            }
            try {
                narrowed(i.test, false, i.body, fn, sc);
            } finally {
                if (typeOnly) {
                    typeCheckingDepth--;
                }
            }
            narrowed(i.test, true, i.orelse, fn, sc);
        } else if (s instanceof While) {
            While w = (While) s;
            type(w.test, fn, sc);
            walkBlock(w.body, fn, sc);
            walkBlock(w.orelse, fn, sc);
        } else if (s instanceof For) {
            For f = (For) s;
            Ty it = iterElem(f.iter, fn, sc);
            // An element of an iterable nothing knows is unseen: the target
            // holds what its other stores say, as a guess (see #type).
            guessSeen |= it.unseen() && !shadow;
            store(f.target, it, null, fn, sc);
            walkBlock(f.body, fn, sc);
            walkBlock(f.orelse, fn, sc);
        } else if (s instanceof FunctionDef) {
            FunctionDef d = (FunctionDef) s;
            for (Expr e : d.decorators) {
                // Called with the function, where the pass does not follow.
                escape(type(e, fn, sc));
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
                if (!d.decorators.isEmpty() && !followedDescriptor(inner, sc)
                        || inner.cls == null && !globalFns.containsValue(inner) && !nested(inner)) {
                    escapeFn(inner);
                }
                if (!d.decorators.isEmpty() && followedDescriptor(inner, sc) && !p.reached.contains(d)) {
                    // Not escaped, but reachable as an escaped one was: a
                    // read or call of the name on any object may run it.
                    reach(inner);
                }
                if (inner.def.name.startsWith("__") && !"__init__".equals(inner.def.name)
                        && !("__call__".equals(inner.def.name) && followedCall(inner))) {
                    // The runtime calls these itself (an operator on an
                    // instance), through the door: the guard checks what
                    // it passes. What the body returns is still what it
                    // returns -- the door boxes it -- so the return keeps
                    // its type; forcing it to an object made every
                    // `__contains__`, `__getitem__` and `__eq__` caller
                    // untyped.
                    escapeFn(inner);
                }
                if (!p.reached.contains(d) && rooted(inner)) {
                    reach(inner);
                }
                if (p.reached.contains(d)) {
                    for (int i = 0; i < inner.params.length; i++) {
                        inner.locals.put(inner.names.get(i), mix(inner.locals.get(inner.names.get(i)), paramIn(inner, i)));
                    }
                    walkBlock(d.body, inner, d.scope);
                    if (!endsAbruptly(d.body)) {
                        inner.ret = joinInto(inner.ret, Ty.NONE);
                    }
                    if (!shadow && !nested(inner) && (entered(inner) || p.missed.containsKey(d))) {
                        genericView(inner);
                    }
                }
            } else {
                walkBlock(d.body, null, d.scope);
            }
            boolean asValue = inner != null && d.decorators.isEmpty() && !p.done
                    && (nested(inner) || inner.cls == null && sc.kind == Scope.Kind.MODULE);
            if (inner != null && fn == null && sc.kind == Scope.Kind.CLASS && !p.reached.contains(d)
                    && classBodyReads(sc, d.name)) {
                // A name the class body itself reads back, as fractions'
                // `__add__, __radd__ = _operator_fallbacks(_add, operator.add)`
                // does: what it calls or passes there is not followed, so it
                // escapes.
                escapeFn(inner);
            }
            if (sc.kind == Scope.Kind.MODULE && (fns.get(d) == null || !globalFns.containsValue(inner))
                    || fn != null && sc.kind != Scope.Kind.MODULE || inner != null && nested(inner)) {
                storeName(d.name, asValue ? funcValue(inner) : Ty.OBJECT, fn, sc);
            }
        } else if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            for (Expr b : c.bases) {
                if (!(b instanceof Name && globalClasses.containsKey(((Name) b).id))) {
                    type(b, fn, sc);
                }
            }
            for (Expr d : c.decorators) {
                escape(type(d, fn, sc));
            }
            Layouts.Cls ck = layouts.all().get(c);
            if (ck != null && ck.compiled) {
                for (Stmt m : c.body) {
                    if (m instanceof FunctionDef && layouts.receiverClass.get(m) == ck //NOPMD CompareObjectsWithEquals - identity is meant
                            && !((FunctionDef) m).args.names().isEmpty()) {
                        Scope ms = ((FunctionDef) m).scope;
                        String self = ((FunctionDef) m).args.names().get(0);
                        if (!ms.assigned.contains(self) && !ms.deleted.contains(self) && !ms.cells.contains(self)) {
                            p.untypedSelf.put(ms, ck);
                        }
                    }
                }
            } else {
                for (Stmt m : c.body) {
                    if (m instanceof FunctionDef && plainMethod((FunctionDef) m)) {
                        p.plainSelf.add(((FunctionDef) m).scope);
                    }
                }
            }
            if (!classBodyMentions.containsKey(c.scope)) {
                Set<String> names = new HashSet<String>();
                for (Stmt m : c.body) {
                    classBodyNames(m, names);
                }
                classBodyMentions.put(c.scope, names);
            }
            walkBlock(c.body, null, c.scope);
            if (sc.kind == Scope.Kind.MODULE && !globalClasses.containsKey(c.name)
                    || sc.kind == Scope.Kind.FUNCTION) {
                // A class without a layout, or one a function makes, is a
                // value like any other.
                storeName(c.name, Ty.OBJECT, fn, sc);
            }
        } else if (s instanceof Try) {
            Try t = (Try) s;
            walkBlock(t.body, fn, sc);
            for (ExceptHandler h : t.handlers) {
                Ty ht = h.type == null ? null : type(h.type, fn, sc);
                if (h.name != null) {
                    storeName(h.name, caught(ht), fn, sc);
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
                    store(i.vars, Ty.OBJECT, null, fn, sc);
                }
            }
            walkBlock(w.body, fn, sc);
        } else if (s instanceof Import || s instanceof ImportFrom) {
            liveImports(s);
            // `import m` always binds a module, and `from p import m` does
            // when m is a module of the program. A package that rebinds the
            // name loses nothing: a module value is only an object here.
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    String n = a.asname != null ? a.asname : a.name;
                    int dot = n.indexOf('.');
                    storeName(dot < 0 ? n : n.substring(0, dot), Ty.obj("module"), fn, sc);
                }
            } else {
                ImportFrom f = (ImportFrom) s;
                String base = f.level == 0 ? f.module : relative(f.level, f.module);
                for (Alias a : f.names) {
                    boolean module = base != null && p.modules.containsKey(base + "." + a.name);
                    storeName(a.asname != null ? a.asname : a.name, module ? Ty.obj("module") : Ty.OBJECT, fn, sc);
                }
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
        Ty lowered = builtinIter(it, fn, sc);
        if (lowered != null) {
            return lowered;
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
        if (t.wide()) {
            List<Fn> src = iterSources(it, fn, sc);
            if (src != null) {
                Ty y = Ty.BOT;
                for (Fn f : src) {
                    y = Ty.join(y, f.yields);
                }
                if (y.k == Ty.K.BOT) {
                    return Ty.BOT;
                }
                return guessRead(y);
            }
        }
        return Ty.UNSEEN;
    }

    /// What a sequence yields when iterated, for a list, a tuple or a str;
    /// null for anything else.
    private Ty seqElem(Ty t) {
        if (t.opt) {
            return null;
        }
        switch (t.k) {
            case LIST:
                return elem(t);
            case TUPLE:
                return tupleElem(t);
            case STR:
                return Ty.STR;
            default:
                return null;
        }
    }

    /// What a loop over `enumerate`, `zip` or `reversed` of sequences, or
    /// over a dict's `keys()`, `values()` or `items()`, gets: the runtime
    /// walks them as it walks the sequences, and the elements keep their
    /// types. Null for any other iterable.
    private Ty builtinIter(Expr it, Fn fn, Scope sc) {
        if (!(it instanceof Call) || !((Call) it).keywords.isEmpty() || anyStarred(((Call) it).args)) {
            return null;
        }
        Call c = (Call) it;
        if (c.func instanceof Attribute && c.args.isEmpty()) {
            String m = ((Attribute) c.func).attr;
            if (!"keys".equals(m) && !"values".equals(m) && !"items".equals(m)) {
                return null;
            }
            Expr recv = ((Attribute) c.func).value;
            if (dictOwner(recv, sc) != null) {
                return null;
            }
            Ty d = type(recv, fn, sc);
            if (d.k != Ty.K.DICT || d.opt || d.sites.length == 0) {
                return null;
            }
            if ("keys".equals(m)) {
                return dictKeyOf(d);
            }
            if ("values".equals(m)) {
                return dictVal(d);
            }
            int s = site(c);
            joinSlot(s, 2, 0, dictKeyOf(d), true);
            joinSlot(s, 2, 1, dictVal(d), true);
            return Ty.tuple(new int[] {s});
        }
        if (!(c.func instanceof Name) || sc.resolve(((Name) c.func).id) != Scope.Binding.BUILTIN
                || moduleScope.hasStarImport) {
            return null;
        }
        String n = ((Name) c.func).id;
        List<Ty> args = new ArrayList<Ty>();
        for (Expr x : c.args) {
            args.add(type(x, fn, sc));
        }
        switch (n) {
            case "reversed":
                return args.size() == 1 && args.get(0).k != Ty.K.STR ? seqElem(args.get(0)) : null;
            case "enumerate": {
                if (args.isEmpty() || args.size() > 2 || args.size() == 2 && !numeric(args.get(1))) {
                    return null;
                }
                Ty e = seqElem(args.get(0));
                if (e == null) {
                    return null;
                }
                int s = site(c);
                joinSlot(s, 2, 0, Ty.INT, true);
                joinSlot(s, 2, 1, e, true);
                return Ty.tuple(new int[] {s});
            }
            case "zip": {
                if (args.isEmpty()) {
                    return null;
                }
                Ty[] es = new Ty[args.size()];
                for (int i = 0; i < es.length; i++) {
                    es[i] = seqElem(args.get(i));
                    if (es[i] == null) {
                        return null;
                    }
                }
                int s = site(c);
                for (int i = 0; i < es.length; i++) {
                    joinSlot(s, es.length, i, es[i], true);
                }
                return Ty.tuple(new int[] {s});
            }
            default:
                return null;
        }
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

    /// The type an argument of a call passes on: a starred one is spread
    /// into parameters the pass cannot match, so OBJECT -- but its value is
    /// still walked and escaped. Answering OBJECT without walking it left
    /// every call inside unreached, and the dead-code pass dropped a method
    /// whose only caller was `f(*self.m(...))` (SQLAlchemy's
    /// `Session._pk_where`); and its elements reach a callee that sees them
    /// as OBJECT, which may store into them.
    private Ty starArg(Expr x, Fn fn, Scope sc) {
        if (x instanceof Starred) {
            escape(type(((Starred) x).value, fn, sc));
            return Ty.OBJECT;
        }
        return type(x, fn, sc);
    }

    private static boolean anyStarred(List<Expr> l) {
        for (Expr e : l) {
            if (e instanceof Starred) {
                return true;
            }
        }
        return false;
    }

    private static final Set<String> MODULE_KEY_METHODS = new HashSet<String>();

    static {
        MODULE_KEY_METHODS.add("pop");
        MODULE_KEY_METHODS.add("get");
        MODULE_KEY_METHODS.add("setdefault");
        MODULE_KEY_METHODS.add("__contains__");
    }

    /// The strings `e` can be: a str constant; or a local of `fn` (not a
    /// parameter), read in `fn`'s own scope, that its body binds exactly
    /// once -- to a str constant, or as the target of a `for` over a
    /// literal tuple or list of str constants (bs4's
    /// `for attr in ("can_be_empty_element", "hidden"): setattr(...)`).
    /// Null when that is not known. Inside a comprehension or a nested
    /// function the same name is another variable, which is why the scope
    /// must be `fn`'s and the name a plain local there.
    static List<String> constStrings(Expr e, Fn fn, Scope sc) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR) {
            return java.util.Collections.singletonList(((Constant) e).text);
        }
        if (e instanceof Name && fn != null && sc == fn.def.scope && !fn.names.contains(((Name) e).id) //NOPMD CompareObjectsWithEquals - identity is meant
                && sc.resolve(((Name) e).id) == Scope.Binding.CELL) {
            String one = cellString(fn.def, ((Name) e).id, e);
            return one == null ? null : java.util.Collections.singletonList(one);
        }
        if (!(e instanceof Name) || fn == null || sc != fn.def.scope || fn.names.contains(((Name) e).id) //NOPMD CompareObjectsWithEquals - identity is meant
                || sc.resolve(((Name) e).id) != Scope.Binding.LOCAL) {
            return null;
        }
        String n = ((Name) e).id;
        List<List<String>> r = new ArrayList<List<String>>();
        r.add(null);
        int[] binds = {0};
        constBinds(fn.def.body, n, r, binds);
        return binds[0] == 1 ? r.get(0) : null;
    }

    /// The string the cell `n` holds where `use` reads it, when that is
    /// settled by the straight-line statements of `f` before the one
    /// holding `use`: the nearest earlier one is `n = "lit"`, or a call
    /// `g()` of a function `f` defines whose whole body is `nonlocal n` and
    /// assignments of string literals to `n`, and every statement between
    /// them leaves `n` alone. Every function that names `n` must be such a
    /// `g`, defined once and used only as such a call statement, so nothing
    /// else can run its store. Null otherwise.
    private static String cellString(FunctionDef f, String n, Expr use) {
        List<Stmt> body = f.body;
        int at = -1;
        for (int i = 0; i < body.size() && at < 0; i++) {
            if (holds(body.get(i), use)) {
                at = i;
            }
        }
        if (at < 0 || !(body.get(at) instanceof ExprStmt || body.get(at) instanceof Assign
                || body.get(at) instanceof Return)) {
            return null;
        }
        Map<String, String> setters = new HashMap<String, String>();
        for (Stmt s : body) {
            if (s instanceof FunctionDef && cellMentions(s, n)) {
                String v = setterValue((FunctionDef) s, n);
                if (v == null || setters.put(((FunctionDef) s).name, v) != null) {
                    return null;
                }
            }
        }
        if (nestedMentions(body, n, setters)) {
            return null;
        }
        for (String g : setters.keySet()) {
            int[] uses = {0};
            int calls = 0;
            countName(f.body, g, uses);
            for (Stmt s : body) {
                if (callOf(s, g)) {
                    calls++;
                }
            }
            if (uses[0] != calls) {
                return null;
            }
        }
        if (mentionsAny(body.get(at), setters.keySet()) || hasWalrus(body.get(at))) {
            return null;
        }
        for (int i = at - 1; i >= 0; i--) {
            Stmt s = body.get(i);
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && isName(((Assign) s).targets.get(0), n)) {
                Expr v = ((Assign) s).value;
                return v instanceof Constant && ((Constant) v).kind == Constant.Kind.STR ? ((Constant) v).text : null;
            }
            for (Map.Entry<String, String> g : setters.entrySet()) {
                if (callOf(s, g.getKey())) {
                    return g.getValue();
                }
            }
            if (s instanceof FunctionDef && setters.containsKey(((FunctionDef) s).name)) {
                continue;
            }
            if (cellMentions(s, n) || mentionsAny(s, setters.keySet())) {
                return null;
            }
        }
        return null;
    }

    private static boolean holds(Object o, Object target) {
        if (o == target) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        boolean[] r = {false};
        Infer.children(o, x -> {
            if (!r[0] && holds(x, target)) {
                r[0] = true;
            }
        });
        return r[0];
    }

    private static boolean cellMentions(Object o, String n) {
        if (o instanceof Name && ((Name) o).id.equals(n)
                || o instanceof Node.Global && ((Node.Global) o).names.contains(n)
                || o instanceof Arg && ((Arg) o).name.equals(n)
                || o instanceof FunctionDef && ((FunctionDef) o).name.equals(n)
                || o instanceof ClassDef && ((ClassDef) o).name.equals(n)) {
            return true;
        }
        boolean[] r = {false};
        Infer.children(o, x -> {
            if (!r[0] && cellMentions(x, n)) {
                r[0] = true;
            }
        });
        return r[0];
    }

    private static boolean mentionsAny(Object o, Set<String> ns) {
        for (String n : ns) {
            if (cellMentions(o, n)) {
                return true;
            }
        }
        return false;
    }

    /// Whether a function, class or lambda nested in `body`, other than a
    /// top-level setter, names `n`.
    private static boolean nestedMentions(List<Stmt> body, String n, Map<String, String> setters) {
        for (Stmt s : body) {
            if (s instanceof FunctionDef && setters.containsKey(((FunctionDef) s).name)) {
                continue;
            }
            if (nestedMention(s, n, false)) {
                return true;
            }
        }
        return false;
    }

    private static boolean nestedMention(Object o, String n, boolean inner) {
        boolean nest = inner || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda
                || o instanceof Node.Comprehension;
        if (nest && cellMentions(o, n)) {
            return true;
        }
        boolean[] r = {false};
        Infer.children(o, x -> {
            if (!r[0] && nestedMention(x, n, nest)) {
                r[0] = true;
            }
        });
        return r[0];
    }

    private static String setterValue(FunctionDef g, String n) {
        if (!g.decorators.isEmpty() || g.isAsync || !g.args.names().isEmpty() || g.body.size() < 2
                || !(g.body.get(0) instanceof Node.Global) || !((Node.Global) g.body.get(0)).nonlocal
                || !((Node.Global) g.body.get(0)).names.equals(java.util.Collections.singletonList(n))) {
            return null;
        }
        String v = null;
        for (int i = 1; i < g.body.size(); i++) {
            Stmt s = g.body.get(i);
            if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1 || !isName(((Assign) s).targets.get(0), n)
                    || !(((Assign) s).value instanceof Constant)
                    || ((Constant) ((Assign) s).value).kind != Constant.Kind.STR) {
                return null;
            }
            v = ((Constant) ((Assign) s).value).text;
        }
        return v;
    }

    private static boolean callOf(Stmt s, String g) {
        return s instanceof ExprStmt && ((ExprStmt) s).value instanceof Call
                && isName(((Call) ((ExprStmt) s).value).func, g) && ((Call) ((ExprStmt) s).value).args.isEmpty()
                && ((Call) ((ExprStmt) s).value).keywords.isEmpty();
    }

    private static void countName(Object o, String n, int[] c) {
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            c[0]++;
        }
        Infer.children(o, x -> countName(x, n, c));
    }

    private static boolean hasWalrus(Object o) {
        if (o instanceof Node.NamedExpr) {
            return true;
        }
        boolean[] r = {false};
        Infer.children(o, x -> {
            if (!r[0] && hasWalrus(x)) {
                r[0] = true;
            }
        });
        return r[0];
    }

    /// The expression local `e` is bound to when one plain assignment in the
    /// function binds it and nothing else does, or null.
    static Expr soleValue(Expr e, Fn fn, Scope sc) {
        if (!(e instanceof Name) || fn == null || sc != fn.def.scope || fn.names.contains(((Name) e).id) //NOPMD CompareObjectsWithEquals - identity is meant
                || sc.resolve(((Name) e).id) != Scope.Binding.LOCAL) {
            return null;
        }
        String n = ((Name) e).id;
        List<List<String>> ignored = new ArrayList<List<String>>();
        ignored.add(null);
        int[] binds = {0};
        constBinds(fn.def.body, n, ignored, binds);
        if (binds[0] != 1) {
            return null;
        }
        Expr[] r = {null};
        assignedValue(fn.def.body, n, r);
        return r[0];
    }

    private static void assignedValue(Object o, String n, Expr[] r) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (isName(t, n)) {
                    r[0] = ((Assign) o).value;
                }
            }
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        Infer.children(o, x -> assignedValue(x, n, r));
    }

    /// Whether every bound of slice `sl` is absent, `None` or an int.
    private boolean sliceOfInts(Slice sl, Fn fn, Scope sc) {
        for (Expr x : new Expr[] {sl.lower, sl.upper, sl.step}) {
            if (x == null) {
                continue;
            }
            Ty t = type(x, fn, sc);
            if (t.k != Ty.K.INT && t.k != Ty.K.BOOL && t.k != Ty.K.NONE && t.k != Ty.K.BOT) {
                return false;
            }
        }
        return true;
    }

    /// The one string [#constStrings] knows `e` to be, or null.
    static String constString(Expr e, Fn fn, Scope sc) {
        List<String> r = constStrings(e, fn, sc);
        return r != null && r.size() == 1 ? r.get(0) : null;
    }

    private static List<String> literalStrings(Expr e) {
        List<Expr> elts = e instanceof TupleExpr ? ((TupleExpr) e).elts : e instanceof ListExpr ? ((ListExpr) e).elts : null;
        if (elts == null || elts.isEmpty()) {
            return null;
        }
        List<String> r = new ArrayList<String>();
        for (Expr x : elts) {
            if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.STR) {
                return null;
            }
            r.add(((Constant) x).text);
        }
        return r;
    }

    private static void constBinds(Object o, String n, List<List<String>> r, int[] binds) {
        if (o instanceof Assign) {
            Assign a = (Assign) o;
            for (Expr t : a.targets) {
                if (isName(t, n)) {
                    binds[0]++;
                    r.set(0, a.value instanceof Constant && ((Constant) a.value).kind == Constant.Kind.STR
                            ? java.util.Collections.singletonList(((Constant) a.value).text) : null);
                } else if (!(t instanceof Name || t instanceof Attribute || t instanceof Subscript)) {
                    // A tuple or starred target binds it too when it names it
                    // (pdfminer's `(_, obj) = parser.nextobject()` does not).
                    boolean[] in = {false};
                    nameIn(t, n, in);
                    if (in[0]) {
                        binds[0] += 2;
                    }
                }
            }
        } else if (o instanceof For && isName(((For) o).target, n)) {
            binds[0]++;
            r.set(0, ((For) o).isAsync ? null : literalStrings(((For) o).iter));
        } else {
            Object target = o instanceof AugAssign ? ((AugAssign) o).target
                    : o instanceof AnnAssign ? ((AnnAssign) o).target
                    : o instanceof For ? ((For) o).target
                    : o instanceof NamedExpr ? ((NamedExpr) o).target
                    : o instanceof Delete ? ((Delete) o).targets
                    : o instanceof WithItem ? ((WithItem) o).vars : null;
            boolean[] names = {false};
            if (target != null) {
                nameIn(target, n, names);
            }
            if (names[0] || o instanceof ExceptHandler && n.equals(((ExceptHandler) o).name)
                    || o instanceof Global && ((Global) o).names.contains(n)
                    || o instanceof Import && importBinds(((Import) o).names, n)
                    || o instanceof ImportFrom && importBinds(((ImportFrom) o).names, n)) {
                // Bound another way, or maybe: not a constant.
                binds[0] += 2;
            }
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        Infer.children(o, x -> constBinds(x, n, r, binds));
    }

    private static boolean importBinds(List<Alias> names, String n) {
        for (Alias a : names) {
            String b = a.asname != null ? a.asname : a.name.indexOf('.') < 0 ? a.name : a.name.substring(0, a.name.indexOf('.'));
            if (b.equals(n) || "*".equals(a.name)) {
                return true;
            }
        }
        return false;
    }

    private static void nameIn(Object o, String n, boolean[] r) {
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            r[0] = true;
            return;
        }
        Infer.children(o, x -> nameIn(x, n, r));
    }

    private static boolean sysModules(Expr e, Scope sc) {
        return e instanceof Attribute && "modules".equals(((Attribute) e).attr) && ((Attribute) e).value instanceof Name
                && "sys".equals(((Name) ((Attribute) e).value).id)
                && sc.resolve("sys") == Scope.Binding.GLOBAL;
    }

    private void store(Expr t, Ty v, Expr value, Fn fn, Scope sc) {
        if (t instanceof Name) {
            storeName(((Name) t).id, v, fn, sc);
        } else if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            Ty r = selfRecv(a.value, a.attr, fn, sc);
            Layouts.Cls us = fn == null && a.value instanceof Name ? p.untypedSelf.get(sc) : null;
            if (us != null && r.wide() && sc.params.iterator().next().equals(((Name) a.value).id)
                    && closedFor(us, a.attr)) {
                r = Ty.inst(us, false);
            }
            if (!p.done && r.k == Ty.K.CLS && r.sites != null) {
                // `K.__init__ = f` on the class object itself replaces what
                // its instances and its subclasses' resolve. Only the written
                // name: a computed `setattr(cls, name, v)` (a class
                // decorator's) is the dynamic stores' concern.
                for (int id : r.sites) {
                    p.clsStored.computeIfAbsent(p.clsById.get(id), x -> new HashSet<String>()).add(a.attr);
                }
            }
            storeField(r, a.attr, v);
        } else if (t instanceof Subscript && !(((Subscript) t).index instanceof Slice)
                && dictOwner(((Subscript) t).value, sc) != null) {
            // `x.__dict__[k] = v`: setattr(x, k, v).
            Expr owner = dictOwner(((Subscript) t).value, sc);
            type(((Subscript) t).index, fn, sc);
            computedStore(type(owner, fn, sc), owner, ((Subscript) t).index, v, t, fn, sc);
        } else if (t instanceof Subscript && sysModules(((Subscript) t).value, sc)) {
            // `sys.modules[name] = m` registers one module under a name; it
            // reads no module out, as a read of `sys.modules` does.
            // pptx's `sys.modules["pptx.exceptions"] = exceptions` made every
            // module of the program loose.
            type(((Subscript) t).index, fn, sc);
            escape(v);
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
                        siteJoin(site, v);
                        joinSlot(site, slotCount(r), ci, v, false);
                    }
                } else if (r.k == Ty.K.LIST) {
                    storeElem(r, v);
                } else if (r.k != Ty.K.BOT && !(shadow && callableSet(v))) {
                    // The generic view of a body sees every receiver as an
                    // object; the typed walk of the same statement records
                    // the functions for the container its receiver is, and
                    // a call through any other container enters them
                    // through the door's guard. Escaping them here made
                    // every one of markdownify's convert_* methods untyped.
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
            Ty each = Ty.OBJECT;
            if (value != null && rangeCall(value, sc)) {
                each = Ty.INT;
            } else if (v.k == Ty.K.BOT) {
                // Nothing reaches the value yet: a later pass decides, where
                // OBJECT now would stay OBJECT for good.
                each = Ty.BOT;
            } else if (v.k == Ty.K.TUPLE) {
                each = tupleElem(v);
            } else if (v.k == Ty.K.LIST) {
                each = elem(v);
            } else {
                escape(v);
            }
            for (Expr e : elts) {
                store(e instanceof Starred ? ((Starred) e).value : e, e instanceof Starred ? Ty.OBJECT : each, null, fn,
                        sc);
            }
        } else if (t instanceof Starred) {
            store(((Starred) t).value, Ty.OBJECT, null, fn, sc);
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
                    if (guessSeen && !shadow) {
                        Set<String> gl = p.guessedLocals.get(fn);
                        if (gl == null) {
                            gl = new HashSet<String>();
                            p.guessedLocals.put(fn, gl);
                        }
                        if (gl.add(n)) {
                            p.changed = true;
                        }
                    }
                } else {
                    classVal(n, v, sc);
                    escape(v);
                }
                return;
            case GLOBAL:
                globals.put(n, joinInto(globals.get(n), v));
                if (guessSeen && !shadow) {
                    Set<String> gg = p.guessedGlobals.get(this);
                    if (gg == null) {
                        gg = new HashSet<String>();
                        p.guessedGlobals.put(this, gg);
                    }
                    if (gg.add(n)) {
                        p.changed = true;
                    }
                }
                return;
            case CELL:
            case FREE: {
                Scope o = cellOwner(n, sc, b);
                if (o != null && p.fnOf(o) != null) {
                    Map<String, Ty> m = p.cells.get(o);
                    if (m == null) {
                        m = new HashMap<String, Ty>();
                        p.cells.put(o, m);
                    }
                    Ty old = m.get(n);
                    Ty j = joinInto(old, v);
                    if (!j.equals(old)) {
                        m.put(n, j);
                        p.changed = true;
                    }
                    return;
                }
                escape(v);
                return;
            }
            default:
                classVal(n, v, sc);
                escape(v);
        }
    }

    /// Records a function a class body binds to `n` (see
    /// [Program#classVals]).
    private void classVal(String n, Ty v, Scope sc) {
        if (sc.kind != Scope.Kind.CLASS || v == null) {
            return;
        }
        Map<String, Ty> m = p.classVals.get(sc);
        if (m == null) {
            m = new HashMap<String, Ty>();
            p.classVals.put(sc, m);
        }
        Ty j = Ty.join(m.get(n), v);
        if (!j.equals(m.get(n))) {
            m.put(n, j);
            p.changed = true;
        }
    }

    /// Joins a store of `v` into field `attr` of `o`. A store the pass
    /// cannot type -- of a value nothing knows, through a receiver of no
    /// known class, under a computed name, or made by a generic body --
    /// does not widen the field: it guards it (see [Program#unknownStores]),
    /// and the slot takes the value when it fits the guess, the object
    /// beside it when not. A guessed value (see [#guessSeen]) is typed for
    /// the typed way of computing it, and guards the field for the other.
    private void storeOne(Layouts.Cls o, String attr, Ty v0, Ty recv) {
        Ty v = noMeth(v0);
        Map<String, Ty> m = fields.get(o);
        // The generic view (shadow) runs the same body with untyped
        // parameters: a value it still knows is one the field really gets.
        boolean slow = recv.k != Ty.K.INST || computing || v.wide();
        if (slow || guessSeen) {
            Set<String> u = p.unknownStores.get(o);
            if (u == null) {
                u = new HashSet<String>();
                p.unknownStores.put(o, u);
            }
            if (u.add(attr)) {
                p.changed = true;
            }
        }
        Ty cur = m.get(attr);
        if (!slow) {
            cur = joinInto(cur, v);
        }
        m.put(attr, cur);
    }

    void storeField(Ty recv, String attr, Ty v) {
        if (recv.k == Ty.K.UNION) {
            for (Ty m : recv.members()) {
                storeField(m.nonNone(), attr, v);
            }
            return;
        }
        if (recv.k == Ty.K.BOT || recv.k == Ty.K.NONE) {
            return;
        }
        if (!p.done) {
            // What the store may replace if `attr` names a method
            // ([Program#patchedFound]).
            if (recv.k == Ty.K.INST) {
                Set<String> s = p.clsStored.get(recv.cls);
                if (s == null) {
                    s = new HashSet<String>();
                    p.clsStored.put(recv.cls, s);
                }
                s.add(attr);
            } else if (p.dynStored.add(attr)) {
                // [#objectInit] reads it.
                p.changed = true;
            }
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
                storeOne(o, attr, v, recv);
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

    /// A field `k` does not have but classes below it store
    /// (`ParserBase.updatepos` reading `self.rawdata`, which only
    /// HTMLParser sets): what those stores put there, joined. An instance
    /// that has none raises on the read, which is no value. Null when no
    /// class below stores it, or `k`'s chain binds the name to anything.
    private Ty subclassField(Layouts.Cls k, String attr) {
        if (Layouts.resolver(k, attr) != null) {
            return null;
        }
        Ty t = null;
        for (Layouts.Cls s : p.below(k)) {
            if (s == k || !s.fields.contains(attr) || fieldOwner(s.parent, attr) != null) { //NOPMD CompareObjectsWithEquals - identity is meant
                continue;
            }
            if (Layouts.resolver(s, attr) != null) {
                return null;
            }
            Ty ft = fieldType(s, attr);
            if (guarded(s, attr)) {
                ft = guessRead(ft);
            }
            t = t == null ? ft : mix(t, ft);
        }
        return t;
    }

    Ty fieldType(Layouts.Cls owner, String attr) {
        Ty t = fields.get(owner).get(attr);
        if (t == null && !p.done && !guarded(owner, attr)) {
            // Not stored yet, by any store: nothing so far. A OBJECT given in
            // an early round, before the constructor is walked, would stick.
            return Ty.BOT;
        }
        if (t == null || guarded(owner, attr) && !guessable(t) && !guessedDict(t)) {
            return Ty.UNSEEN;
        }
        if (!p.done && guessedDict(t) && guarded(owner, attr)) {
            // Whoever reads it reads its elements as guesses, by every path
            // a field is read (a constant getattr, a subclass's field...).
            // The field may hold another dict altogether, filled by stores
            // the pass never saw: what its sites hold is no answer yet.
            guessSites(t.sites);
            for (int st : t.sites) {
                if (p.lostSites.add(Integer.valueOf(st))) {
                    p.changed = true;
                }
            }
        }
        return loc(t);
    }

    /// Whether a guarded field may keep dict `t` as it is: typed code holds
    /// a dict as an object, and every typed dict operation checks that it
    /// has one (`Typed.dgetv` and the rest fall back to the generic
    /// protocol), so what it may really be is only a question for its
    /// elements, which [#guessRead] makes guesses. Every computed setattr
    /// on an object of no known class guards every field of every class:
    /// markdownify's converter cache read as an object, and every
    /// `convert_*` method it holds went with it.
    private static boolean guessedDict(Ty t) {
        return t.k == Ty.K.DICT && !t.opt && t.sites.length > 0;
    }

    /// Whether a guarded field may keep `t` as its guess: a type typed code
    /// holds in a slot of its own, beside the object that replaces it.
    static boolean guessable(Ty t) {
        switch (t.k) {
            case INT:
            case BOOL:
            case FLOAT:
            case STR:
            case LIST:
                return true;
            case INST:
                // An analysed class has no Java class to hold a guess in.
                return t.cls.compiled;
            default:
                return false;
        }
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
            if (res.home != home) { //NOPMD CompareObjectsWithEquals - identity is meant
                // A method of a base in another module: typed code calls
                // that module's typed copy (TypedEmit#foreignCall) when it
                // is the only one an instance can run -- html.parser's
                // `self.updatepos(i, j)` of `_markupbase`, bs4's parser on
                // `Tag` -- and that module's own field of the name would
                // shadow it on an instance.
                if (res.home.fieldOwners.containsKey(m)) {
                    return null;
                }
                for (Layouts.Cls c : p.below(k)) {
                    if (c.bodyNames.contains(m)) {
                        return null;
                    }
                }
                for (Layouts.Cls c = k; c != res; c = c.parent) { //NOPMD CompareObjectsWithEquals - identity is meant
                    if (c.bodyNames.contains(m)) {
                        return null;
                    }
                }
                return java.util.Collections.singletonList(f);
            }
            r.add(f);
        }
        for (Layouts.Cls c : p.below(k)) {
            if (c.bodyNames.contains(m)) {
                if (c.home != home) { //NOPMD CompareObjectsWithEquals - identity is meant
                    // Overridden in a subclass of another module, which
                    // this module's typed code cannot dispatch to.
                    return null;
                }
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

    /// What `except K as e` binds `e` to: an instance of `K` when `K` is
    /// one of the compiled classes, since the handler runs only for a value
    /// that is one; anything else is not known. Classes with no common
    /// base are not joined (that would loosen them): the binding is then
    /// simply not known.
    private Ty caught(Ty ht) {
        if (ht == null || ht.k != Ty.K.CLS || ht.opt || ht.sites.length == 0) {
            return Ty.UNSEEN;
        }
        Ty r = Ty.BOT;
        for (Layouts.Cls k : classesOf(ht)) {
            r = Ty.join(r, Ty.inst(k, false));
        }
        return r.k == Ty.K.INST ? r : Ty.OBJECT;
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

    /// The type of `module.attr` read by name, when `a.value` names a module
    /// of the program; null otherwise. Only the attribute goes where the
    /// pass does not follow, never the module: a read by a fixed name
    /// cannot reach the module's other globals or classes. Treating the
    /// module as a value made every class of `bs4.element` loose because
    /// soupsieve reads `bs4.element.NavigableString`, and every class of a
    /// module escaped the same way wherever `module.function(...)` named a
    /// function no direct call resolved.
    private Ty moduleMember(Attribute a, Scope sc) {
        Object m = foreign(a.value, sc);
        if (!(m instanceof Infer)) {
            return null;
        }
        Ty t = valueOf(foreign(a, sc));
        if (t != null) {
            return t;
        }
        // Not stable, or not known: whatever the global holds escapes.
        Infer mod = (Infer) m;
        Ty g = mod.globals.get(a.attr);
        if (g != null) {
            mod.escape(g);
        }
        Layouts.Cls k = mod.globalClasses.get(a.attr);
        if (k != null) {
            mod.escapeClass(k);
        }
        Fn f = mod.globalFns.get(a.attr);
        if (f != null) {
            mod.escapeFn(f);
        }
        Infer sub = p.modules.get(mod.moduleName.isEmpty() ? a.attr : mod.moduleName + "." + a.attr);
        if (sub != null && p.looseModules.add(sub)) {
            p.changed = true;
        }
        return Ty.UNSEEN;
    }

    /// The type of `v` as the receiver of `.attr`. The `self` of an open
    /// class's method is its class there, unless a rejected class binds
    /// `attr`: an instance of one finds the compiled fields and methods
    /// for every other name, and a rejected class's own stores reach the
    /// compiled fields by name, as any store on an unknown object does.

    private Ty selfRecv(Expr v, String attr, Fn fn, Scope sc) {
        if (v instanceof Name && fn != null && fn.openSelf && sc == fn.def.scope //NOPMD CompareObjectsWithEquals - identity is meant
                && fn.names.get(0).equals(((Name) v).id)) {
            Ty t = nameType(((Name) v).id, fn, sc);
            return t.k == Ty.K.INST ? rejected(t.cls, attr) ? Ty.OBJECT : loc(t)
                    : p.rejectedAnywhere.contains(attr) || !p.rejectedNames.isEmpty() && anyRejected(attr) ? Ty.OBJECT : loc(t);
        }
        return type(v, fn, sc);
    }

    /// Whether a call of `attr` on an instance of `k` runs a compiled
    /// method the pass sees: `k` is not open, or no rejected class binds
    /// `attr` (an instance typed `k` is one of `k`'s Java class).
    /// Whether `m`, read on an instance of `k`, is a method that never
    /// receives the instance: the class body that binds it nearest up the
    /// chain binds it once, as a function decorated with nothing but the
    /// builtin `classmethod` or `staticmethod`, no compiled class below
    /// `k` binds the name again, and no instance holds it as a field.
    private boolean classLevel(Layouts.Cls k, String m) {
        Layouts.Cls res = Layouts.resolver(k, m);
        if (res == null || p.home.get(k).layouts.fieldOwners.containsKey(m)) {
            return false;
        }
        for (Layouts.Cls c : p.below(k)) {
            if (c.bodyNames.contains(m)) {
                return false;
            }
        }
        FunctionDef d = null;
        for (Stmt st : res.def.body) {
            if (st instanceof FunctionDef && ((FunctionDef) st).name.equals(m)) {
                if (d != null) {
                    return false;
                }
                d = (FunctionDef) st;
            } else if (Layouts.bodyBindingsOf(st) == null || Layouts.bodyBindingsOf(st).contains(m)) {
                return false;
            }
        }
        return d != null && classOrStatic(d, res);
    }

    /// The pass's function for `d`, of whichever module defines it.
    Fn anyFn(FunctionDef d) {
        Fn f = p.fns.get(d);
        return f == null || f.analysisOnly ? null : f;
    }

    /// This module's functions that generated code has a typed body for:
    /// [#fns] without the analysed classes' methods.
    Map<FunctionDef, Fn> emitFns() {
        if (emitFns == null) {
            emitFns = new LinkedHashMap<FunctionDef, Fn>();
            for (Map.Entry<FunctionDef, Fn> e : fns.entrySet()) {
                if (!e.getValue().analysisOnly) {
                    emitFns.put(e.getKey(), e.getValue());
                }
            }
        }
        return emitFns;
    }

    private Map<FunctionDef, Fn> emitFns;

    private boolean closedFor(Layouts.Cls k, String attr) {
        return !open.contains(k) || !rejected(k, attr);
    }

    Ty nameType(String n, Fn fn, Scope sc) {
        Scope.Binding b = sc.resolve(n);
        if (b == Scope.Binding.CELL && compOnlyCell(sc, n)) {
            b = Scope.Binding.LOCAL;
        }
        switch (b) {
            case LOCAL:
                if (fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Ty t = fn.locals.get(n);
                    return t == null ? Ty.BOT : guessedLocal(fn, n) ? guessRead(t) : t;
                }
                if (sc.kind == Scope.Kind.COMPREHENSION) {
                    Ty t = compLocal(sc, n);
                    if (t != null) {
                        return shadow ? Ty.OBJECT : guessedCompVar(sc, n) ? guessRead(t) : t;
                    }
                }
                return classRead(n, sc);
            case FREE:
            case CELL: {
                // A local of the function a comprehension (or one nested in
                // it) runs in: read where it stands, as its own type. What the
                // comprehension does with it is typed like any other use.
                Fn o = compOuter;
                if (o != null && sc.kind == Scope.Kind.COMPREHENSION && sc.owner(n) == o.def.scope //NOPMD CompareObjectsWithEquals - identity is meant
                        && !(o.def.scope.cells.contains(n) && !compOnlyCell(o.def.scope, n))) {
                    // A cell a closure shares is any value: read as OBJECT below.
                    Ty t = o.locals.get(n);
                    if (t == null) {
                        return Ty.BOT;
                    }
                    return guessedLocal(o, n) ? guessRead(t) : t;
                }
                Scope ow = sc.owner(n);
                if (b == Scope.Binding.FREE && ow != null && ow.kind == Scope.Kind.COMPREHENSION) {
                    // A target of a comprehension this one is nested in.
                    Ty t = compLocal(ow, n);
                    if (t != null) {
                        return shadow ? Ty.OBJECT : guessedCompVar(ow, n) ? guessRead(t) : t;
                    }
                }
                Scope co = cellOwner(n, sc, b);
                Fn of = co == null ? null : p.fnOf(co);
                if (of != null && !p.done) {
                    // Every store to the variable, wherever it is made, and
                    // what the calls pass when it is a parameter.
                    Map<String, Ty> m = p.cells.get(co);
                    Ty t = m == null ? null : m.get(n);
                    int pi = of.names.indexOf(n);
                    Ty pt = pi < 0 || pi >= of.params.length ? null : paramIn(of, pi);
                    if (pt != null) {
                        t = Ty.join(t, pt);
                    }
                    if (t != null && pt != null && pt.unseen()) {
                        // A parameter whose callers are unseen, shared by a
                        // closure: the join ignored it, but a cell has no
                        // guard at the door, so what the stores say is not
                        // even a guess. Read as anything (csv's
                        // `_Dialect.__init__` passed a dialect's name on to
                        // `get_dialect` as a `_Frozen`).
                        escapeFuncs(t);
                        return Ty.OBJECT;
                    }
                    return t == null ? Ty.BOT : t;
                }
                return Ty.OBJECT;
            }
            case GLOBAL: {
                Fn f = globalFns.get(n);
                if (f != null) {
                    if (sc.kind == Scope.Kind.MODULE) {
                        // Module code runs in order: the read may come
                        // before the def, while the name is still a builtin.
                        escapeFn(f);
                        return Ty.OBJECT;
                    }
                    // Followed to where it is called (see [#funcValue]).
                    return p.done ? Ty.OBJECT : funcValue(f);
                }
                Layouts.Cls k = globalClasses.get(n);
                if (k != null && k.enumMeta) {
                    // The class protocol of an enumeration (iteration, item
                    // lookup, `in`) is EnumType's, which no layout holds.
                    return Ty.OBJECT;
                }
                if (k != null) {
                    return classValue(k);
                }
                if (importedNames.containsKey(n)) {
                    Ty t = valueOf(resolve(n, 0));
                    if (t != null) {
                        return t;
                    }
                }
                Ty t = globals.get(n);
                return t == null ? Ty.BOT : guessedGlobal(n) ? guessRead(t) : t;
            }
            default:
                return classRead(n, sc);
        }
    }

    /// A name read in a class body: a function the body bound to it
    /// earlier (`convert_i = convert_em`) is the value it is.
    private Ty classRead(String n, Scope sc) {
        if (sc.kind == Scope.Kind.CLASS) {
            Map<String, Ty> cv = p.classVals.get(sc);
            Ty t = cv == null ? null : cv.get(n);
            if (t != null && (t.k == Ty.K.BOT || callables(t.nonNone(), new ArrayList<Fn>(), new ArrayList<Integer>()))) {
                // Nothing yet (a factory whose result is not known) stays
                // nothing until it is.
                return t;
            }
        }
        return Ty.UNSEEN;
    }

    Ty type(Expr e, Fn fn, Scope sc) {
        if (shadow || p.done) {
            return loc(type0(e, fn, sc));
        }
        boolean saved = guessSeen;
        guessSeen = false;
        Ty t = loc(type0(e, fn, sc));
        boolean g = guessSeen && !construction(e, t, sc);
        // An unseen value is no evidence of a slot's type (Ty#UNSEEN), but
        // it is a value the slot really gets: whatever it lands in holds
        // what the other sources say as a guess, guarded, the generic way
        // of computing it taking over when the guess misses.
        guessSeen = saved || g || t.unseen() && !shadow;
        if (g) {
            p.guessedExprs.add(e);
            if ((t.k == Ty.K.LIST || t.k == Ty.K.TUPLE || t.k == Ty.K.DICT) && t.sites.length > 0) {
                guessSites(t.sites);
            }
            // The generic way of computing it, which runs when a guess
            // fails: as the generic code does, over values of no known type.
            shadow = true;
            try {
                type0(e, fn, sc);
            } finally {
                shadow = false;
            }
        }
        return t;
    }

    /// Whether `e`, of type `t`, constructs a known class: its type is that
    /// class whatever its arguments were, so a guess among them (each
    /// still checked where it is read) does not make the result one.
    private boolean construction(Expr e, Ty t, Scope sc) {
        if (!(e instanceof Call) || t.k != Ty.K.INST || t.opt) {
            return false;
        }
        Expr f = ((Call) e).func;
        Object k = null;
        if (f instanceof Name && sc.resolve(((Name) f).id) == Scope.Binding.GLOBAL) {
            k = globalClasses.get(((Name) f).id);
            if (k == null) {
                k = foreign(f, sc);
            }
        } else if (f instanceof Attribute && foreign(((Attribute) f).value, sc) instanceof Infer) {
            k = foreign(f, sc);
        }
        return k == t.cls; //NOPMD CompareObjectsWithEquals - identity is meant
    }

    /// Whether a guessed value was read since the statement began (see
    /// [#guessed]).
    private boolean guessSeen;

    /// A guessed value read: the value itself (`t`) in typed code, which
    /// keeps the typed form; nothing known in the generic code.
    private Ty guessRead(Ty t) {
        // Only a type typed code holds in a slot of its own is a guess:
        // with nothing typed known, or a value of another kind, any value
        // may be there.
        if (callableSet(t)) {
            // Never in a typed slot of its own (see [#loc]): what it may be
            // is what a call through it is followed to.
            return t;
        }
        if (!shadow && guessedDict(t)) {
            guessSites(t.sites);
            return t;
        }
        if (shadow || !guessable(t)) {
            // Read as anything: a function it may be is called where the
            // pass does not follow it.
            if (t != null) {
                escapeFuncs(t);
            }
            return Ty.OBJECT;
        }
        guessSeen = true;
        return t;
    }

    private void guessSites(int[] sites) {
        for (int st : sites) {
            if (p.guessedSites.add(Integer.valueOf(st))) {
                p.changed = true;
            }
        }
    }

    /// An element of a container of these sites read as `t`.
    private Ty siteRead(Ty t, int[] sites) {
        if (exactRead(t, sites)) {
            return t;
        }
        for (int st : sites) {
            if (p.guessedSites.contains(Integer.valueOf(st))) {
                return guessRead(t);
            }
        }
        return t;
    }

    /// Whether `e`, as typed code computes it, is a guess (see
    /// [Program#guessedExprs]).
    boolean guessed(Expr e) {
        return p.guessedExprs.contains(e);
    }

    /// Whether the elements of a container of type `t` may be values
    /// typed code did not put there: read as guesses.
    boolean guessedSite(Ty t) {
        if (p.allListsEscape) {
            return true;
        }
        for (int st : t.sites) {
            Integer key = Integer.valueOf(st);
            if (escapedSites.contains(key) || p.guessedSites.contains(key)) {
                return true;
            }
        }
        return false;
    }

    boolean guessedLocal(Fn f, String n) {
        Set<String> s = p.guessedLocals.get(f);
        return s != null && s.contains(n);
    }

    boolean guessedReturn(Fn f) {
        return p.guessedReturns.contains(f);
    }

    boolean guessedGlobal(String n) {
        Set<String> s = p.guessedGlobals.get(this);
        return s != null && s.contains(n);
    }

    /// Whether field `attr` of `owner` is guarded (see
    /// [Program#unknownStores]).
    boolean guarded(Layouts.Cls owner, String attr) {
        Set<String> u = p.unknownStores.get(owner);
        return u != null && u.contains(attr);
    }

    /// The result of a call that runs `f`.
    private Ty retOf(Fn f, Ty t) {
        // Either version may run, the guard picks: the generic one's
        // result joins in (see [#call]).
        Infer h = p.fnHome.get(f);
        Fn g = h == null ? null : h.generic.get(f.def);
        Ty r = g == null || g == f ? t : mix(t, g.ret); //NOPMD CompareObjectsWithEquals - identity is meant
        return p.guessedReturns.contains(f) ? guessRead(r) : r;
    }

    /// The type of a read of `o` (see [#resolve]) as a value, or null when
    /// `o` says nothing. A function or a class read as a value goes where
    /// the pass does not follow.
    private Ty valueOf(Object o) {
        if (o instanceof Fn) {
            escapeFn((Fn) o);
            return Ty.UNSEEN;
        }
        if (o instanceof Layouts.Cls) {
            // The class as a value: followed to where it is called, which
            // constructs it, or to where the pass stops following it, which
            // lets it go then ([#escape]).
            return classValue((Layouts.Cls) o);
        }
        if (o instanceof Infer && p.looseModules.add((Infer) o)) {
            p.changed = true;
        }
        if (o instanceof Extern) {
            Extern g = (Extern) o;
            Ty t = g.module.globals.get(g.name);
            return t == null ? Ty.BOT : g.module.guessedGlobal(g.name) ? guessRead(loc(t)) : loc(t);
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
                    return ((Constant) e).text.endsWith("j") || ((Constant) e).text.endsWith("J") ? Ty.obj("complex")
                            : Ty.FLOAT;
                case STR:
                    return Ty.STR;
                case BYTES:
                    return Ty.BYTES;
                case ELLIPSIS:
                    return Ty.obj("ellipsis");
                default:
                    return Ty.OBJECT;
            }
        }
        if (e instanceof Name) {
            if (fn != null && fn.openSelf && sc == fn.def.scope && fn.names.get(0).equals(((Name) e).id)) { //NOPMD CompareObjectsWithEquals - identity is meant
                // An instance of a rejected class, perhaps: as a value it
                // reaches wherever this may go, so it is of no known class.
                return Ty.OBJECT;
            }
            Ty nt = narrowedAt.get(e);
            if (fn != null && sc == fn.def.scope && narrowing.containsKey(nk(sc, ((Name) e).id))) { //NOPMD CompareObjectsWithEquals - identity is meant
                nt = narrowing.get(nk(sc, ((Name) e).id));
                narrowedAt.put((Name) e, nt);
            }
            if (nt != null) {
                // Still typed as the local is, so its stores stay joined;
                // but the test checked the value, so whether that type was
                // a guess (a cache's `get`) no longer matters.
                boolean g = guessSeen;
                nameType(((Name) e).id, fn, sc);
                guessSeen = g;
                return nt;
            }
            return nameType(((Name) e).id, fn, sc);
        }
        if (e instanceof Subscript && sysModules(((Subscript) e).value, sc) && (((Subscript) e).index instanceof Constant
                && ((Constant) ((Subscript) e).index).kind == Constant.Kind.STR || isName(((Subscript) e).index, "__name__")
                && sc.resolve("__name__") != Scope.Binding.LOCAL && !moduleScope.assigned.contains("__name__"))) {
            // `sys.modules["warnings"]`: that one module, as a value;
            // `sys.modules[__name__]` this one (bs4.builder).
            Infer m = ((Subscript) e).index instanceof Name ? this
                    : p.modules.get(((Constant) ((Subscript) e).index).text);
            if (m != null && !p.done && p.looseModules.add(m)) {
                p.changed = true;
            }
            // A module, as `import` binds one: a computed store on it sets
            // a global (loosened above), never a field of an instance. As
            // unseen, bs4's `setattr(this_module, name, obj)` guarded every
            // field of the program.
            return m != null ? Ty.obj("module") : Ty.UNSEEN;
        }
        if (e instanceof Attribute) {
            Layouts.Cls sk = classStaticOf(((Attribute) e).value, ((Attribute) e).attr, sc);
            if (sk != null) {
                return classStatic(sk, ((Attribute) e).attr);
            }
        }
        if (e instanceof Attribute) {
            // `Color.RED`: a member the compiler defined (EnumLowering),
            // one instance of the class itself.
            Layouts.Cls ek = calledClassOrEnum(((Attribute) e).value, sc);
            List<String> mem = ek == null ? null : EnumLowering.members(ek.def);
            if (mem != null && mem.contains(((Attribute) e).attr) && ek.compiled && !ek.builtinOwn) {
                return Ty.inst(ek, false);
            }
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
            // As `type(x)` (#call1).
            Ty o = type(((Attribute) e).value, fn, sc);
            Ty known = classesBelow(o);
            if (known != null) {
                return known;
            }
            if (o.wide() || o.k == Ty.K.INST) {
                if (o.k == Ty.K.INST) {
                    // Not one class this pass names: let go, so the
                    // nameless class it becomes [#anyClassAttr] covers it.
                    loosen(o.cls);
                }
                return Ty.cls(new int[0]);
            }
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
                // with arguments nobody tracks. It is the one method the
                // class resolves, when nothing can replace it.
                Layouts.Cls k = globalClasses.get(((Name) a.value).id);
                if (k != null && closedFor(k, a.attr) && !p.home.get(k).layouts.fieldOwners.containsKey(a.attr)) {
                    Layouts.Cls o = Layouts.resolver(k, a.attr);
                    FunctionDef m = o == null ? null : o.typable.get(a.attr);
                    boolean first = o == k || singleBased(k); //NOPMD CompareObjectsWithEquals - identity is meant
                    if (first && (o == null || m != null && p.fns.get(m) != null
                            || m == null && definesFunction(o.def, a.attr))) {
                        // (An untyped method, `Element.__init__(self, tag,
                        // attrib={}, **extra)`, takes anything already.)
                        if (m != null) {
                            escapeFn(p.fns.get(m));
                        }
                        return Ty.OBJECT;
                    }
                }
                escapeMethods(a.attr);
                return Ty.OBJECT;
            }
            Ty mm = moduleMember(a, sc);
            if (mm != null) {
                return mm;
            }
            Ty r = selfRecv(a.value, a.attr, fn, sc);
            if (r.wide() && a.value instanceof Name && sc.resolve(((Name) a.value).id) == Scope.Binding.BUILTIN
                    && !moduleScope.hasStarImport && !moduleScope.assigned.contains(((Name) a.value).id)
                    && (Layouts.BUILTIN_NAMES.containsKey(((Name) a.value).id) || "object".equals(((Name) a.value).id)
                    || "type".equals(((Name) a.value).id))) {
                // An attribute of a builtin class (`object.__init__`,
                // `str.join`): the builtin's own, never a method of the
                // program.
                return Ty.OBJECT;
            }
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
                siteJoin(s, mix(elem(l), elem(r)));
                return Ty.list(new int[] {s}, false);
            }
            if (l.k == Ty.K.BOT || r.k == Ty.K.BOT) {
                return Ty.BOT;
            }
            Ty t = realPow(b, l, r) ? Ty.FLOAT : binop(b.op, l, r, b.right);
            if (t.wide()) {
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
            return Ty.OBJECT;
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
            Ty l = identityOperand(c, -1, sc) ? Ty.OBJECT : type(c.left, fn, sc);
            boolean plain = true;
            for (int i = 0; i < c.ops.size(); i++) {
                Ty r = identityOperand(c, i, sc) ? Ty.OBJECT : type(c.comparators.get(i), fn, sc);
                String op = c.ops.get(i);
                if (!("is".equals(op) || "is not".equals(op) || "in".equals(op) || "not in".equals(op)
                        || plainCompare(l) && plainCompare(r))) {
                    plain = false;
                }
                if ("in".equals(op) || "not in".equals(op)) {
                    if (r.k == Ty.K.LIST) {
                        // A list's `in` compares its elements and stores
                        // nothing: they leave (to `__eq__`), the list does
                        // not. bs4's `Tag.__contains__` escaped every tag's
                        // `contents` this way.
                        escape(elem(r));
                    } else if (r.k == Ty.K.TUPLE && slotCount(r) >= 0) {
                        // Each element compares itself with the left side
                        // and keeps nothing; a class does so by identity.
                        for (int si = 0; si < slotCount(r); si++) {
                            Ty st = slot(r, si);
                            if (st.k != Ty.K.CLS) {
                                escape(st);
                            }
                        }
                    } else if (r.k != Ty.K.DICT) {
                        // A dict's `in` reads its keys and keeps nothing.
                        escape(r);
                    }
                    if (l.k != Ty.K.CLS) {
                        // A compiled class compares as `type` compares
                        // it, by identity: its metaclass is `type` or one
                        // without comparisons (Layouts#plainMetaclass).
                        escape(l);
                    }
                }
                l = r;
            }
            return plain ? Ty.BOOL : Ty.OBJECT;
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            type(i.test, fn, sc);
            Ty a = type(i.body, fn, sc);
            Ty b = type(i.orelse, fn, sc);
            Ty r = mix(a, b);
            if (r.wide()) {
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
            return Ty.UNSEEN;
        }
        if (e instanceof Subscript && ((Subscript) e).value instanceof Name
                && GENERIC_BUILTINS.contains(((Name) ((Subscript) e).value).id)
                && sc.resolve(((Name) ((Subscript) e).value).id) == Scope.Binding.BUILTIN && !moduleScope.hasStarImport) {
            // `list[int]`: a `types.GenericAlias`, whatever the parameters.
            escape(type(((Subscript) e).index, fn, sc));
            return Ty.obj("types.GenericAlias");
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
                return Ty.UNSEEN;
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
            if (r.k == Ty.K.LIST && s.index instanceof Slice && sliceOfInts((Slice) s.index, fn, sc)) {
                // A new list of the same elements. Typed as one from the same
                // sites, a store into the copy widens the original's
                // elements, which is sound. Escaping the list instead made
                // bs4's `self.contents[i:]` turn every tag's `contents` --
                // and so every child, every `node` markdownify walks -- into
                // untyped objects.
                return Ty.list(r.sites, false);
            }
            if (r.k == Ty.K.LIST) {
                // A read never writes the list: what leaves is its elements.
                escape(elem(r));
                escape(i);
                return Ty.UNSEEN;
            }
            escape(r);
            escape(i);
            return Ty.UNSEEN;
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
                    siteJoin(s, t.k == Ty.K.LIST ? elem(t) : Ty.OBJECT);
                    escape(t);
                } else {
                    Ty t = type(x, fn, sc);
                    if (starred) {
                        siteJoin(s, t);
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
            FunctionDef d = lambdaDefs.get(l);
            Fn inner = d == null ? null : fns.get(d);
            if (inner == null) {
                for (Expr x : l.args.defaults) {
                    escape(type(x, fn, sc));
                }
                escape(type(l.body, null, l.scope));
                return Ty.OBJECT;
            }
            int first = inner.params.length - l.args.defaults.size();
            for (int i = 0; i < l.args.defaults.size(); i++) {
                inner.params[first + i] = joinInto(inner.params[first + i],
                        type(l.args.defaults.get(i), fn, sc));
            }
            if (p.reached.contains(d)) {
                for (int i = 0; i < inner.params.length; i++) {
                    inner.locals.put(inner.names.get(i), mix(inner.locals.get(inner.names.get(i)), paramIn(inner, i)));
                }
                inner.ret = joinInto(inner.ret, type(l.body, inner, l.scope));
            }
            return p.done ? Ty.OBJECT : funcValue(inner);
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
        if (e instanceof JoinedStr) {
            // An f-string: a str, whatever its fields format.
            for (Expr x : ((JoinedStr) e).parts) {
                type(x, fn, sc);
            }
            return Ty.STR;
        }
        if (e instanceof FormattedValue) {
            // One field: its value goes to format(), which takes anything.
            FormattedValue f = (FormattedValue) e;
            escape(type(f.value, fn, sc));
            if (f.spec != null) {
                type(f.spec, fn, sc);
            }
            return Ty.STR;
        }
        if (e instanceof Node.SetExpr) {
            // A set, as `set(...)` makes one (see [#madeBuiltin]): its
            // elements are hashed and compared, and go where a set's go.
            for (Expr x : ((Node.SetExpr) e).elts) {
                escape(type(x, fn, sc));
            }
            return Ty.obj("set");
        }
        if (e instanceof Yield && fn != null && sc == fn.def.scope) { //NOPMD CompareObjectsWithEquals - identity is meant
            Yield y = (Yield) e;
            Ty v = y.value == null ? Ty.NONE : type(y.value, fn, sc);
            escape(v);
            yieldInto(fn, y.from ? Ty.OBJECT : v);
            return Ty.UNSEEN;
        }
        anyChildren(e, fn, sc);
        return Ty.UNSEEN;
    }

    /// The element type of each generator expression, as its last pass
    /// typed it (see [Fn#yields]).
    private final Map<Comp, Ty> genElems = new IdentityHashMap<Comp, Ty>();

    private void yieldInto(Fn fn, Ty v) {
        Ty r = Ty.join(fn.yields, v);
        if (!r.equals(fn.yields)) {
            fn.yields = r;
            p.changed = true;
        }
    }

    /// The functions iterating `it` runs the body of: a method called on,
    /// or a property read off, a name of one known class whose every
    /// candidate is compiled here. Null when that is not known.
    private List<Fn> iterSources(Expr it, Fn fn, Scope sc) {
        Expr recv;
        String attr;
        boolean call;
        if (it instanceof Call && ((Call) it).func instanceof Name) {
            String n = ((Name) ((Call) it).func).id;
            Fn g = sc.resolve(n) == Scope.Binding.GLOBAL ? globalFns.get(n) : null;
            // Only a generator's yields are what the loop gets: any other
            // function returns an iterable of its own.
            return g == null || !yieldsItems(g.def) ? null : java.util.Collections.singletonList(g);
        }
        if (it instanceof Call && ((Call) it).func instanceof Attribute) {
            recv = ((Attribute) ((Call) it).func).value;
            attr = ((Attribute) ((Call) it).func).attr;
            call = true;
        } else if (it instanceof Attribute) {
            recv = ((Attribute) it).value;
            attr = ((Attribute) it).attr;
            call = false;
        } else {
            return null;
        }
        if (!(recv instanceof Name)) {
            return null;
        }
        Ty r = selfRecv(recv, attr, fn, sc);
        if (r.k != Ty.K.INST || r.opt || !closedFor(r.cls, attr)) {
            return null;
        }
        List<Layouts.Cls> owners = new ArrayList<Layouts.Cls>();
        Layouts.Cls res = Layouts.resolver(r.cls, attr);
        if (res == null) {
            return null;
        }
        owners.add(res);
        for (Layouts.Cls c : p.below(r.cls)) {
            if (c.bodyNames.contains(attr)) {
                owners.add(c);
            }
        }
        List<Fn> out = new ArrayList<Fn>();
        for (Layouts.Cls c : owners) {
            FunctionDef d = bodyDef(c, attr);
            Fn f = d == null ? null : p.fns.get(d);
            if (f == null || isProperty(d) == call || !yieldsItems(d)) {
                return null;
            }
            out.add(f);
        }
        return out;
    }

    /// Whether what `d` returns is a generator of its own yields: it is a
    /// generator, or it returns only generator expressions. Any other
    /// function returns an iterable whose items its yields do not say.
    private static boolean yieldsItems(FunctionDef d) {
        if (d.isAsync) {
            return false;
        }
        if (d.scope.hasYield) {
            return true;
        }
        boolean[] r = {false, false};
        genReturns(d.body, r);
        return r[0] && !r[1];
    }

    /// Sets `r[0]` for a return of a generator expression, `r[1]` for any
    /// other return, outside nested functions and classes.
    private static void genReturns(Object o, boolean[] r) {
        if (o instanceof Return) {
            Expr v = ((Return) o).value;
            if (v instanceof Comp && ((Comp) v).kind == Comp.Kind.GENERATOR) {
                r[0] = true;
            } else {
                r[1] = true;
            }
            return;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        Infer.children(o, x -> genReturns(x, r));
    }

    /// The function class `c`'s body binds `name` to, when its only binding
    /// of the name is one `def` (a property's setter beside its getter is
    /// left out); null for any other binding.
    private static FunctionDef bodyDef(Layouts.Cls c, String name) {
        FunctionDef r = null;
        for (Stmt st : c.def.body) {
            if (st instanceof FunctionDef && ((FunctionDef) st).name.equals(name)) {
                FunctionDef d = (FunctionDef) st;
                if (setterOrDeleter(d)) {
                    continue;
                }
                if (r != null) {
                    return null;
                }
                r = d;
            } else if (binds(st, name)) {
                return null;
            }
        }
        return r;
    }

    private static boolean setterOrDeleter(FunctionDef d) {
        for (Expr x : d.decorators) {
            if (x instanceof Attribute && ("setter".equals(((Attribute) x).attr)
                    || "deleter".equals(((Attribute) x).attr))) {
                return true;
            }
        }
        return false;
    }

    /// The getter a read of property `attr` off a non-None `r` runs, when
    /// it is one this module compiles and no compiled subclass of `r`'s
    /// class overrides the name; null otherwise.
    Fn propertyGetter(Ty r, String attr) {
        if (r.k != Ty.K.INST || r.opt || !closedFor(r.cls, attr)) {
            return null;
        }
        Layouts.Cls res = Layouts.resolver(r.cls, attr);
        if (res == null || res.home != p.home.get(r.cls).layouts) { //NOPMD CompareObjectsWithEquals - identity is meant
            return null;
        }
        for (Layouts.Cls c : p.below(r.cls)) {
            if (c.bodyNames.contains(attr)) {
                return null;
            }
        }
        FunctionDef d = bodyDef(res, attr);
        if (d == null || !isProperty(d) || d.decorators.size() != 1
                || !propertyBinding(((Name) d.decorators.get(0)).id, res.def.scope.parent)) {
            return null;
        }
        return p.fns.get(d);
    }

    /// Whether method `f`'s one decorator is the builtin `property` or
    /// `classmethod` and its name is closed ([#closedName]): every call then
    /// reaches it through a path that follows it -- a property read hands
    /// the getter an instance of its class, a call of the name binds its
    /// arguments to each method of that name (see [#duckCall]) -- and a read
    /// of it as a value lets it go where that read is typed.
    private boolean followedDescriptor(Fn f, Scope sc) {
        if (f.cls == null || f.def.decorators.size() != 1 || !(f.def.decorators.get(0) instanceof Name)) {
            return false;
        }
        String d = ((Name) f.def.decorators.get(0)).id;
        return ("classmethod".equals(d) && sc.resolve(d) == Scope.Binding.BUILTIN || propertyBinding(d, sc))
                && closedName(f.def.name);
    }

    /// The module-level alias of the builtin `property` a lowering
    /// decorates with when the class body binds `property` itself
    /// ([DeclarativeMap]); the alias is a module global no class body can
    /// shadow, and the name is reserved to the compiler.
    static final String PROPERTY_ALIAS = "_cn1_property";

    /// Whether decorator name `d`, read in scope `sc`, is the builtin
    /// `property`: the builtin itself, or the compiler's module-level alias.
    private static boolean propertyBinding(String d, Scope sc) {
        return "property".equals(d) ? sc.resolve(d) == Scope.Binding.BUILTIN
                : PROPERTY_ALIAS.equals(d) && sc.resolve(d) == Scope.Binding.GLOBAL;
    }

    private static boolean isProperty(FunctionDef d) {
        for (Expr x : d.decorators) {
            if (x instanceof Name && ("property".equals(((Name) x).id) || PROPERTY_ALIAS.equals(((Name) x).id))) {
                return true;
            }
        }
        return false;
    }

    /// The function the comprehension being typed runs in, or null.
    private Fn compOuter;

    /// The type of each name target of a comprehension, per comprehension
    /// scope: what its iterable yields, joined over every pass. A target
    /// only ever holds what its own clause binds, so this is exact; an
    /// iterable whose elements are guesses binds OBJECT instead.
    private final Map<Scope, Map<String, Ty>> compLocals = new IdentityHashMap<Scope, Map<String, Ty>>();

    /// The type of comprehension target `n` of comprehension scope `sc`, or
    /// null when `n` is not one.
    Ty compLocal(Scope sc, String n) {
        Map<String, Ty> m = compLocals.get(sc);
        return m == null ? null : loc(m.get(n));
    }

    /// The comprehension targets bound from guessed elements (see [#comp]),
    /// by comprehension scope.
    private final Map<Scope, Set<String>> guessedComp = new IdentityHashMap<Scope, Set<String>>();

    /// Whether target `n` of comprehension scope `sc` holds a guess.
    boolean guessedCompVar(Scope sc, String n) {
        Set<String> s = guessedComp.get(sc);
        return s != null && s.contains(n);
    }

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
        Set<String> out = new LinkedHashSet<String>(d.scope.cells);
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
        Set<String> out = new LinkedHashSet<String>();
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

    /// Generator expressions a typed list's `extend` runs to their end on
    /// the spot (see [#extendedGenerator]): lowered as list comprehensions,
    /// their elements are the list's, not escaped.
    private final Set<Comp> extendGens = java.util.Collections.newSetFromMap(new IdentityHashMap<Comp, Boolean>());

    /// Whether the emitter lowers `g` as a list comprehension.
    boolean extendsEagerly(Comp g) {
        return extendGens.contains(g);
    }

    /// The generator expression of `name.extend(<generator>)`, when the
    /// generator does not mention `name`: list.extend consumes it before
    /// returning, so only a generator that reads the list being grown could
    /// tell it from a list of the same items. Null otherwise.
    static Comp extendedGenerator(Call c) {
        if (!(c.func instanceof Attribute) || !"extend".equals(((Attribute) c.func).attr)
                || !(((Attribute) c.func).value instanceof Name) || c.args.size() != 1 || !c.keywords.isEmpty()
                || !(c.args.get(0) instanceof Comp) || ((Comp) c.args.get(0)).kind != Comp.Kind.GENERATOR) {
            return null;
        }
        String n = ((Name) ((Attribute) c.func).value).id;
        boolean[] seen = {false};
        mentions(c.args.get(0), n, seen);
        return seen[0] ? null : (Comp) c.args.get(0);
    }

    private static void mentions(Object o, String n, boolean[] seen) {
        if (o instanceof Name && n.equals(((Name) o).id)) {
            seen[0] = true;
            return;
        }
        Infer.children(o, x -> mentions(x, n, seen));
    }

    private final Map<Comprehension, Layouts.Cls> narrowed = new IdentityHashMap<Comprehension, Layouts.Cls>();

    /// The class the target of `g` is narrowed to by a filter
    /// `isinstance(target, C)` of its own, or null. The emitter skips any
    /// other element before binding the target (TypedEmit#compStore).
    Layouts.Cls narrowedBy(Comprehension g) {
        Layouts.Cls k = narrowed.get(g);
        return k == null || !k.compiled ? null : k;
    }

    /// The one compiled class `isinstance(n, C)` among the filters of `g`
    /// names, or null.
    private Layouts.Cls isinstanceFilter(Comprehension g, String n, Scope sc) {
        for (Expr x : g.ifs) {
            if (x instanceof Call && ((Call) x).func instanceof Name
                    && "isinstance".equals(((Name) ((Call) x).func).id) && ((Call) x).args.size() == 2
                    && ((Call) x).keywords.isEmpty() && ((Call) x).args.get(0) instanceof Name
                    && n.equals(((Name) ((Call) x).args.get(0)).id)
                    && sc.resolve("isinstance") == Scope.Binding.BUILTIN) {
                Ty ct = type(((Call) x).args.get(1), null, sc);
                if (ct.k == Ty.K.CLS && !ct.opt && ct.sites.length == 1 && layoutHolds(classesOf(ct).get(0))) {
                    // Not for an extra base of another class, whose
                    // instances the filter passes and its Java class lacks.
                    return classesOf(ct).get(0);
                }
            }
        }
        return null;
    }

    private Ty comp(Comp c, Fn fn, Scope sc) {
        {
            boolean first = true;
            for (Comprehension g : c.generators) {
                // The first iterable is evaluated in the enclosing scope.
                boolean seen = guessSeen;
                guessSeen = false;
                Ty el = iterElem(g.iter, first ? fn : null, first ? sc : c.scope);
                if (guessSeen) {
                    if (identityComp(c) && c.kind != Comp.Kind.DICT && c.kind != Comp.Kind.SET) {
                        // `(x for x in xs)` over a guessed list: what it
                        // yields is that guess, and every read of it checks
                        // one (a generator's yields are read as guesses, a
                        // list's site is marked below). bs4's Tag.children
                        // yielded objects, and markdownify's walk was untyped.
                        guessElt = el;
                    } else {
                        guessElt = null;
                    }
                    if (!shadow && el.k == Ty.K.INST && !el.opt && guessable(el) && g.target instanceof Name) {
                        // Held as a guess, as a for loop's target is: the
                        // typed slot while the element is of the class, the
                        // object beside it when not, and the expressions
                        // over it written both ways (TypedEmit#split).
                        // markdownify's `[self.process_element(el, ...) for
                        // el in children_to_convert]` was a generic call.
                        if (guessedComp.computeIfAbsent(c.scope, x -> new HashSet<String>())
                                .add(((Name) g.target).id)) {
                            p.changed = true;
                        }
                    } else {
                        escape(el);
                        el = Ty.OBJECT;
                    }
                }
                guessSeen |= seen;
                first = false;
                if (g.target instanceof Name && !g.isAsync) {
                    Map<String, Ty> m = compLocals.computeIfAbsent(c.scope, x -> new HashMap<String, Ty>());
                    String n = ((Name) g.target).id;
                    Layouts.Cls nk = isinstanceFilter(g, n, c.scope);
                    if (nk != null && (el.wide() || el.k == Ty.K.INST)) {
                        // Only elements of that class get past the filter.
                        if (el.wide()) {
                            el = Ty.inst(nk, false);
                        } else {
                            Ty j = Ty.join(el, Ty.inst(nk, false));
                            el = j.k == Ty.K.INST && j.cls == el.cls ? Ty.inst(nk, false) : el; //NOPMD CompareObjectsWithEquals - identity is meant
                        }
                        if (el.k == Ty.K.INST && el.cls == nk && narrowed.put(g, nk) == null) { //NOPMD CompareObjectsWithEquals - identity is meant
                            p.changed = true;
                        }
                    }
                    Ty was = m.get(n);
                    // The generic view holds every target as an object (see
                    // the reads in #nameType): what it meets is no evidence
                    // for the typed one's.
                    Ty now = shadow ? (was == null ? Ty.BOT : was) : joinInto(was, el);
                    if (shadow) {
                        escape(el);
                    }
                    if (!now.equals(was)) {
                        m.put(n, now);
                        p.changed = true;
                    }
                } else {
                    escape(el);
                }
                store(g.target, Ty.OBJECT, null, null, c.scope);
                for (Expr x : g.ifs) {
                    type(x, null, c.scope);
                }
            }
            Ty elt = c.elt != null ? type(c.elt, null, c.scope) : Ty.OBJECT;
            Ty ge = guessElt;
            guessElt = null;
            if (ge != null && !shadow && elt.wide()) {
                elt = ge;
            }
            Set<String> gc = guessedComp.get(c.scope);
            if ((ge != null || gc != null && !gc.isEmpty()) && !shadow && c.kind != Comp.Kind.GENERATOR) {
                // What the generic half puts there is whatever it met: a
                // target that missed its guess computes the element over an
                // object.
                guessSites(new int[] {site(c)});
            }
            if (c.kind != Comp.Kind.LIST && !extendGens.contains(c)) {
                // A list comprehension's elements are its site's (below),
                // an eagerly extended generator's the list's.
                escape(elt);
            }
            if (c.value != null) {
                escape(type(c.value, null, c.scope));
            }
            if (c.kind == Comp.Kind.GENERATOR && !shadow) {
                // The typed walk's: the generic view's yields are its own
                // function's (see the Return in [#walk]).
                Ty was = genElems.get(c);
                genElems.put(c, was == null ? elt : Ty.join(was, elt));
            }
            if (c.kind != Comp.Kind.LIST) {
                return Ty.OBJECT;
            }
            // A list of the element type, its length unknown (unslotted).
            int s = site(c);
            if (unslotted.add(Integer.valueOf(s))) {
                p.changed = true;
            }
            if (shadow && elt.wide()) {
                // The generic view's elements are objects by construction:
                // the site holds guesses, every read of it checked, rather
                // than objects the typed view never puts there.
                guessSites(new int[] {s});
                return Ty.list(new int[] {s}, false);
            }
            Ty j = joinInto(siteElem.get(s), elt);
            if (!j.equals(siteElem.get(s))) {
                siteElem.set(s, j);
                p.changed = true;
            }
            return Ty.list(new int[] {s}, false);
        }
    }

    /// What a comprehension over a guessed iterable passes on unchanged
    /// (see [#comp]), while its generators are walked.
    private Ty guessElt;

    /// `[x for x in xs if c]` or `(x for x in xs)`: one generator, a name
    /// target, the element itself (a filter only drops some).
    private static boolean identityComp(Comp c) {
        if (c.generators.size() != 1 || c.value != null) {
            return false;
        }
        Comprehension g = c.generators.get(0);
        return !g.isAsync && g.target instanceof Name && c.elt instanceof Name
                && ((Name) g.target).id.equals(((Name) c.elt).id);
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
        for (Layouts.Cls c : selfAndBelow(k)) {
            {
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
        if (r.k == Ty.K.UNION) {
            // Each type it may be answers for itself; None raises.
            Ty t = Ty.BOT;
            for (Ty m : r.members()) {
                t = mix(t, attrType(m.nonNone(), attr));
            }
            return t;
        }
        if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
            // Nothing reached the receiver yet, or None (which raises): no value.
            // Nothing reached the receiver yet: a later round decides.
            return Ty.BOT;
        }
        if (r.k == Ty.K.INST) {
            Layouts.Cls o = fieldOwner(r.cls, attr);
            if (o != null) {
                Ty ft = fieldType(o, attr);
                if (guarded(o, attr)) {
                    ft = guessRead(ft);
                    if (ft.k == Ty.K.LIST && ft.sites.length > 0 && !shadow) {
                        // Any list may be there, of whatever elements.
                        guessSites(ft.sites);
                    }
                }
                return ft;
            }
            Ty below = subclassField(r.cls, attr);
            if (below != null) {
                return below;
            }
            // A bound method, or something only a subclass has.
            if (!r.opt && closedFor(r.cls, attr) && !classLevel(r.cls, attr)) {
                List<FunctionDef> cand = candidates(r.cls, attr);
                if (cand == null || cand.isEmpty()) {
                    // Overridden in another module: a call of the value goes
                    // through each one's door all the same.
                    cand = doorCandidates(r.cls, attr);
                }
                Ty mt = cand == null || cand.isEmpty() ? null : methValue(cand);
                if (mt != null) {
                    return mt;
                }
            }
            escapeMethods(attr);
            return Ty.UNSEEN;
        }
        if (r.k == Ty.K.OBJ && !r.opt && "module".equals(r.name) && "__name__".equals(attr)) {
            // Every module has its dotted name.
            return Ty.STR;
        }
        // On None the read raises, so an optional class reads the same.
        if (r.k == Ty.K.CLS && ("__name__".equals(attr) || "__qualname__".equals(attr)
                || "__module__".equals(attr))) {
            // What `type` keeps for every class; a layout's metaclass
            // defines none of them (Layouts#plainMetaclass).
            return Ty.STR;
        }
        if (r.k == Ty.K.CLS && !r.opt && r.sites.length == 0) {
            return anyClassAttr(attr);
        }
        escape(r);
        if (r.wide()) {
            escapeMethods(attr);
        }
        return Ty.UNSEEN;
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
            return Ty.UNSEEN;
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
                            : Ty.OBJECT;
                default:
                    return Ty.UNSEEN;
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
                    return Ty.UNSEEN;
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
        if ((l.k == Ty.K.STR || l.k == Ty.K.BYTES) && "%".equals(op) && (right instanceof TupleExpr
                || builtinOperand(r))) {
            // str % value formats, or raises: no builtin type has an
            // __rmod__ that takes a str.
            return l.k == Ty.K.STR ? Ty.STR : Ty.BYTES;
        }
        return Ty.UNSEEN;
    }

    /// Whether every type `t` may be is a builtin one, none of which has a
    /// reflected operator taking a str or bytes on the left.
    private static boolean builtinOperand(Ty t) {
        switch (t.k) {
            case NONE:
            case INT:
            case BOOL:
            case FLOAT:
            case STR:
            case BYTES:
            case LIST:
            case TUPLE:
            case DICT:
                return true;
            case UNION:
                for (Ty m : t.members()) {
                    if (!builtinOperand(m)) {
                        return false;
                    }
                }
                return true;
            default:
                return false;
        }
    }

    private Ty call(Call c, Fn fn, Scope sc) {
        if (!shadow) {
            return builtinSig(c, call0(c, fn, sc), fn, sc);
        }
        boolean saved = shadowMiss;
        shadowMiss = false;
        Ty t = call0(c, fn, sc);
        boolean miss = shadowMiss;
        shadowMiss = saved;
        // A call the guard sends to the callee's generic version has that
        // version's result, which the result of a call of a known callee
        // already joins (see [#retOf]). Anything else is not known.
        return builtinSig(c, miss && !fnCallee(c, fn, sc) ? Ty.UNSEEN : t, fn, sc);
    }

    /// Calls whose result [BuiltinSigs] types: the emitters convert the
    /// generic result to that type.
    final Set<Call> builtinCalls = java.util.Collections.newSetFromMap(new IdentityHashMap<Call, Boolean>());

    /// `t`, the type the pass gave call `c`, or what [BuiltinSigs] says the
    /// Java builtin it calls returns when the pass knew nothing: a function
    /// of a module the runtime writes in Java (`_cn1re.sub(...)`), a method
    /// a class binds to one (`search = _cn1re.pattern_search`), or a
    /// method of a builtin object (a match's `group`).
    private Ty builtinSig(Call c, Ty t, Fn fn, Scope sc) {
        if (t.k != Ty.K.OBJECT || !(c.func instanceof Attribute) || !c.keywords.isEmpty()
                || sysModules(((Attribute) c.func).value, sc)) {
            // `sys.modules.pop(name)` reads one module out by name (see
            // [#call0]); typing its receiver here would read them all.
            return t;
        }
        Attribute a = (Attribute) c.func;
        List<Ty> args = new ArrayList<Ty>();
        for (Expr x : c.args) {
            if (x instanceof Starred) {
                return t;
            }
            args.add(type(x, fn, sc));
        }
        String q = null;
        if (a.value instanceof Name) {
            String m = importedModule(((Name) a.value).id, sc);
            if (m != null && p.modules.get(m) == null) {
                q = m + "." + a.attr;
            }
        }
        if (q == null) {
            Ty r = type(a.value, fn, sc);
            if (r.k == Ty.K.INST && !r.opt) {
                q = classBuiltin(r.cls, a.attr);
                args.add(0, r);
            } else if (r.k == Ty.K.OBJ && !r.opt) {
                q = r.name + "." + a.attr;
            }
        }
        Ty s = q == null ? null : BuiltinSigs.ret(q, args, c);
        if (s == null) {
            return t;
        }
        builtinCalls.add(c);
        return s;
    }

    /// The module that `import m` (or `import m as n`) at the top of this
    /// module binds to the global `n`, when nothing else binds it.
    private String importedModule(String n, Scope sc) {
        if (sc.resolve(n) != Scope.Binding.GLOBAL || body == null) {
            return null;
        }
        String found = null;
        for (Stmt s : body) {
            if (s instanceof Import) {
                for (Alias al : ((Import) s).names) {
                    if (n.equals(al.asname) || al.asname == null && n.equals(al.name)) {
                        if (found != null) {
                            return null;
                        }
                        found = al.name;
                    }
                }
            } else {
                List<String> bound = new ArrayList<String>();
                boundIn(s, bound);
                if (bound.contains(n)) {
                    return null;
                }
            }
        }
        return found;
    }

    /// `module.function` when every instance of `k` finds `name` as a class
    /// attribute bound to that function of a Java module
    /// (`search = _cn1re.pattern_search`), so calling it on one calls the
    /// function with the instance first. Null when a subclass may bind it
    /// otherwise, or it is anything else.
    private String classBuiltin(Layouts.Cls k, String name) {
        Layouts.Cls o = Layouts.resolver(k, name);
        if (o == null) {
            return null;
        }
        for (Layouts.Cls s : p.home.keySet()) {
            if (s != o && isSelfOrAncestor(k, s) && s.bodyNames.contains(name)) { //NOPMD CompareObjectsWithEquals - identity is meant
                return null;
            }
        }
        Infer home = p.home.get(o);
        String q = null;
        for (Stmt st : o.def.body) {
            List<String> bound = new ArrayList<String>();
            boundIn(st, bound);
            if (!bound.contains(name)) {
                continue;
            }
            if (q != null || !(st instanceof Assign) || ((Assign) st).targets.size() != 1
                    || !(((Assign) st).value instanceof Attribute)
                    || !(((Attribute) ((Assign) st).value).value instanceof Name) || home == null) {
                return null;
            }
            Attribute v = (Attribute) ((Assign) st).value;
            String m = home.importedModule(((Name) v.value).id, home.moduleScope);
            if (m == null || p.modules.get(m) != null) {
                return null;
            }
            q = m + "." + v.attr;
        }
        return q;
    }

    /// Whether `c` runs only functions the pass knows: a method of a known
    /// instance, or a function or bound method value, every result of
    /// which is [#retOf]'s.
    private boolean fnCallee(Call c, Fn fn, Scope sc) {
        boolean saved = shadowMiss;
        try {
            if (c.func instanceof Attribute) {
                Ty r = type(((Attribute) c.func).value, fn, sc);
                String a = ((Attribute) c.func).attr;
                if (r.k != Ty.K.INST || r.opt || !closedFor(r.cls, a) || classLevel(r.cls, a)) {
                    return false;
                }
                List<FunctionDef> cand = candidates(r.cls, a);
                return cand != null && !cand.isEmpty();
            }
            if (c.func instanceof Name) {
                Ty ft = type(c.func, fn, sc);
                return callableSet(ft);
            }
            return false;
        } finally {
            shadowMiss = saved;
        }
    }

    /// Whether `c` is `typing.cast(T, value)` -- imported from `typing`
    /// by name or reached as `typing.cast`, bound once -- with a type
    /// written as a name, attribute, subscript or string: the call answers
    /// `value`, and typed code gives it `value`'s own type instead of what
    /// every cast of the program joins to. Evaluating `T` has no effect
    /// worth keeping, so it is not evaluated. bs4's `name_rules =
    /// cast(List[TagNameMatchRule], list(...))` left the field untyped.
    boolean isCast(Call c, Scope sc) {
        if (c.args.size() != 2 || !c.keywords.isEmpty() || anyStarred(c.args)) {
            return false;
        }
        Expr t = c.args.get(0);
        if (!(t instanceof Name || t instanceof Attribute || t instanceof Subscript
                || t instanceof Constant && ((Constant) t).kind == Constant.Kind.STR)) {
            return false;
        }
        if (c.func instanceof Name) {
            String n = ((Name) c.func).id;
            Extern g = sc.resolve(n) == Scope.Binding.GLOBAL && stable(n) ? importedNames.get(n) : null;
            return g != null && "cast".equals(g.name) && "typing".equals(g.module.moduleName);
        }
        if (c.func instanceof Attribute && ((Attribute) c.func).value instanceof Name
                && "cast".equals(((Attribute) c.func).attr)) {
            String n = ((Name) ((Attribute) c.func).value).id;
            Infer m = sc.resolve(n) == Scope.Binding.GLOBAL ? moduleNames.get(n) : null;
            return m != null && "typing".equals(m.moduleName);
        }
        return false;
    }

    /// The call [#call0] is typing, so [#callFn] can tell which argument
    /// was a guess: no evidence of what the call passes there.
    private Call curCall;

    private Ty call0(Call c, Fn fn, Scope sc) {
        Call savedCall = curCall;
        curCall = c;
        try {
            return call00(c, fn, sc);
        } finally {
            curCall = savedCall;
        }
    }

    private Ty call00(Call c, Fn fn, Scope sc) {
        if (isCast(c, sc)) {
            // typing.cast(T, v) is v: CPython never checks it, and a
            // duck-typed cast (`cast(Base, proxy)`) is common, so T says
            // nothing about the value's layout.
            type(c.args.get(0), fn, sc);
            return type(c.args.get(1), fn, sc);
        }
        if (c.func instanceof Attribute && sysModules(((Attribute) c.func).value, sc) && c.keywords.isEmpty()
                && !c.args.isEmpty() && !anyStarred(c.args)
                && MODULE_KEY_METHODS.contains(((Attribute) c.func).attr)) {
            // `sys.modules.pop(name, None)`, `.get(name)`: the one module
            // named, when the name is known (defusedxml's _get_py3_cls);
            // every module when it is not.
            String key = constString(c.args.get(0), fn, sc);
            for (int i = 1; i < c.args.size(); i++) {
                escape(type(c.args.get(i), fn, sc));
            }
            type(c.args.get(0), fn, sc);
            if (key == null) {
                if (!p.allModulesLoose && !p.done) {
                    p.allModulesLoose = true;
                    p.changed = true;
                }
            } else {
                Infer m = p.modules.get(key);
                if (m != null && !p.done && p.looseModules.add(m)) {
                    p.changed = true;
                }
            }
            return Ty.UNSEEN;
        }
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

    /// Whether calling `d` (from parameter `from` on) as `c` does always
    /// raises TypeError: more positional arguments than parameters, a
    /// keyword naming none, or a parameter without a default left out.
    /// Whether every argument of `c` binds to a parameter at build time: no
    /// `*` or `**` argument. Its keywords then bind by name, per callee.
    private static boolean bindable(Call c) {
        if (anyStarred(c.args)) {
            return false;
        }
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                return false;
            }
        }
        return true;
    }

    static boolean raises(Call c, FunctionDef d, int from) {
        Node.Arguments a = d.args;
        if (hasStarred(c.args)) {
            return false;
        }
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                return false;
            }
        }
        int n = a.positional.size() - from;
        if (n < 0) {
            return true;
        }
        int k = c.args.size();
        if (k > n && a.vararg == null) {
            return true;
        }
        Set<String> given = new HashSet<String>();
        for (Keyword kw : c.keywords) {
            int idx = -1;
            for (int j = Math.max(a.positionalOnly - from, 0); j < n; j++) {
                if (a.positional.get(from + j).name.equals(kw.arg)) {
                    idx = j;
                }
            }
            boolean kwOnly = false;
            for (Node.Arg x : a.kwonly) {
                kwOnly |= x.name.equals(kw.arg);
            }
            if (idx < 0 && !kwOnly && a.kwarg == null || idx >= 0 && idx < k) {
                // Names nothing, or a parameter a positional one filled.
                return true;
            }
            given.add(kw.arg);
        }
        int firstDefault = a.positional.size() - a.defaults.size();
        for (int j = k; j < n; j++) {
            if (from + j < firstDefault && !given.contains(a.positional.get(from + j).name)) {
                return true;
            }
        }
        for (int j = 0; j < a.kwonly.size(); j++) {
            if (a.kwDefaults.get(j) == null && !given.contains(a.kwonly.get(j).name)) {
                return true;
            }
        }
        return false;
    }

    /// [#staticArgs], with `full` running the list to the last parameter.
    static List<Expr> staticArgs(Call c, FunctionDef d, int from, boolean full) {
        if (c.keywords.isEmpty() && d.args.kwonly.isEmpty()) {
            return c.args;
        }
        Node.Arguments a = d.args;
        if (a.vararg != null || a.kwarg != null || hasStarred(c.args)) {
            return null;
        }
        // Keyword-only parameters follow the positional ones, as in
        // Arguments#names; with any, the list is always complete.
        int np = a.positional.size() - from;
        int n = np + a.kwonly.size();
        int k = c.args.size();
        if (np < 0 || n <= 0 || k > np) {
            return null;
        }
        if (!a.kwonly.isEmpty()) {
            full = true;
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
            for (int j = first; j < np; j++) {
                if (a.positional.get(from + j).name.equals(kw.arg)) {
                    idx = j;
                    break;
                }
            }
            for (int j = 0; j < a.kwonly.size() && idx < 0; j++) {
                if (a.kwonly.get(j).name.equals(kw.arg)) {
                    idx = np + j;
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
                Expr dv;
                if (j >= np) {
                    dv = a.kwDefaults.get(j - np);
                } else {
                    int di = from + j - firstDefault;
                    dv = di < 0 ? null : a.defaults.get(di);
                }
                if (!(dv instanceof Constant)) {
                    return null;
                }
                e = dv;
            }
            r.add(e);
        }
        return r;
    }

    /// The methods named `m` an instance of `k` may run, each with no
    /// decorator and a typed body behind its door; null when one is not.
    private List<FunctionDef> doorCandidates(Layouts.Cls k, String m) {
        List<Layouts.Cls> owners = new ArrayList<Layouts.Cls>();
        Layouts.Cls res = Layouts.resolver(k, m);
        if (res == null) {
            return null;
        }
        owners.add(res);
        for (Layouts.Cls c : p.below(k)) {
            if (c.bodyNames.contains(m)) {
                owners.add(c);
            }
        }
        List<FunctionDef> r = new ArrayList<FunctionDef>();
        for (Layouts.Cls c : owners) {
            if (c.home.fieldOwners.containsKey(m)) {
                return null;
            }
            FunctionDef d = bodyDef(c, m);
            if (d == null || !d.decorators.isEmpty() || d.isAsync || p.fns.get(d) == null
                    || Layouts.IMPLICIT_CLASS_LEVEL.contains(m)) {
                return null;
            }
            if (!r.contains(d)) {
                r.add(d);
            }
        }
        return r;
    }

    /// The argument each parameter of `d` (by its index in `f.names`)
    /// gets from call `c`, the receiver aside: a call argument, or the
    /// parameter's constant default. Null at a parameter whose value is not
    /// known here (a default that is not a constant, `*args`, `**kwargs`);
    /// `extra` collects what goes into those. Returns null when the call
    /// does not bind (a starred argument, a keyword twice).
    /// [#bindByName] with the call's first argument at parameter `first`:
    /// 0 for an explicit `Cls.method(obj, ...)`.
    private static Expr[] bindByName(Call c, FunctionDef d, Fn f, List<Expr> extra, int first) {
        Node.Arguments a = d.args;
        Expr[] slot = new Expr[f.names.size()];
        boolean[] unknown = new boolean[slot.length];
        int np = a.positional.size();
        boolean spread = false;
        for (int i = 0; i < c.args.size(); i++) {
            Expr x = c.args.get(i);
            if (x instanceof Starred || spread) {
                // A sequence of unknown length: from here on no positional
                // parameter is known, and the rest may reach `*args`.
                if (!spread) {
                    for (int j = first + i; j < np; j++) {
                        unknown[j] = true;
                    }
                    spread = true;
                }
                if (a.vararg == null && first + i >= np && !(x instanceof Starred)) {
                    return null;
                }
                extra.add(x instanceof Starred ? ((Starred) x).value : x);
            } else if (first + i < np) {
                slot[first + i] = x;
            } else if (a.vararg != null) {
                extra.add(x);
            } else {
                return null;
            }
        }
        for (Keyword kw : c.keywords) {
            if (kw.arg == null) {
                // `**kwargs`: any keyword parameter not given may come
                // from it.
                for (int j = first; j < slot.length; j++) {
                    unknown[j] |= slot[j] == null;
                }
                extra.add(kw.value);
                continue;
            }
            int idx = f.names.indexOf(kw.arg);
            boolean named = idx >= first && (idx < np && idx >= a.positionalOnly || isKwOnly(a, kw.arg));
            if (named) {
                if (slot[idx] != null) {
                    return null;
                }
                slot[idx] = kw.value;
            } else if (a.kwarg != null) {
                extra.add(kw.value);
            } else {
                return null;
            }
        }
        int firstDefault = np - a.defaults.size();
        for (int j = first; j < np; j++) {
            if (slot[j] == null && !unknown[j] && j >= firstDefault && a.defaults.get(j - firstDefault) instanceof Constant) {
                slot[j] = a.defaults.get(j - firstDefault);
            }
        }
        for (int j = 0; j < a.kwonly.size(); j++) {
            int idx = f.names.indexOf(a.kwonly.get(j).name);
            Expr dv = a.kwDefaults.get(j);
            if (slot[idx] == null && !unknown[idx] && dv instanceof Constant) {
                slot[idx] = dv;
            }
        }
        return slot;
    }

    private static boolean isKwOnly(Node.Arguments a, String n) {
        for (Node.Arg x : a.kwonly) {
            if (x.name.equals(n)) {
                return true;
            }
        }
        return false;
    }

    /// A call of method `m` on an instance of `k` that is not plain -- it
    /// passes keywords, or the method takes keyword-only parameters,
    /// defaults or `*args` -- made through each method's door: generic
    /// code makes the call (TypedEmit), and each guard picks the typed body
    /// when the arguments fit. What the call passes is what the parameters
    /// hold; the receiver is not escaped. Null when a method it may run has
    /// no such door, or the call does not bind.
    private Ty doorCall(Call c, Layouts.Cls k, String m, Fn fn, Scope sc) {
        List<FunctionDef> cand = doorCandidates(k, m);
        return cand == null ? null : doorBound(c, cand, 1, fn, sc);
    }

    /// [#doorCall] once the methods are known: `first` is the parameter
    /// the call's first argument binds to.
    private Ty doorBound(Call c, List<FunctionDef> cand, int first, Fn fn, Scope sc) {
        List<Expr[]> binds = new ArrayList<Expr[]>();
        List<Expr> extra = new ArrayList<Expr>();
        for (FunctionDef d : cand) {
            Expr[] s = bindByName(c, d, p.fns.get(d), extra, first);
            if (s == null) {
                return null;
            }
            binds.add(s);
        }
        Map<Expr, Ty> seen = new IdentityHashMap<Expr, Ty>();
        for (Expr x : c.args) {
            Expr v = x instanceof Starred ? ((Starred) x).value : x;
            seen.put(v, type(v, fn, sc));
        }
        for (Keyword kw : c.keywords) {
            seen.put(kw.value, type(kw.value, fn, sc));
        }
        for (Expr x : extra) {
            escape(seen.get(x));
        }
        Ty ret = Ty.BOT;
        for (int i = 0; i < cand.size(); i++) {
            Fn f = p.fns.get(cand.get(i));
            reach(f);
            Expr[] s = binds.get(i);
            for (int j = first; j < s.length; j++) {
                if (f.def.args.vararg != null && f.names.get(j).equals(f.def.args.vararg.name)
                        || f.def.args.kwarg != null && f.names.get(j).equals(f.def.args.kwarg.name)) {
                    continue;
                }
                Ty t = s[j] == null ? Ty.OBJECT : seen.containsKey(s[j]) ? seen.get(s[j]) : type(s[j], fn, sc);
                if (shadow) {
                    if (f.params[j].k != Ty.K.BOT && !Ty.join(f.params[j], t).equals(f.params[j])) {
                        shadowMiss = true;
                        escape(t);
                    }
                } else if (t.k == Ty.K.OBJECT && !nested(f) && (f.params[j].k == Ty.K.BOT || f.params[j].k == Ty.K.INST)) {
                    // No evidence, as in [#callFn]: the call is a door's.
                    objectArg(f, j);
                } else {
                    f.params[j] = joinInto(f.params[j], t);
                }
            }
            Fn g = generic.get(f.def);
            ret = mix(ret, retOf(f, g == null ? f.ret : mix(f.ret, g.ret)));
        }
        doors.add(c);
        return ret;
    }

    /// [#staticArgs] for a call any of methods `cand` may answer: the one
    /// list they all bind it to, or null when there is none or two of them
    /// bind it differently. A gap a default fills must take equal constants
    /// in every one. bs4's `self.match(i, _known_rules=True)`, which
    /// ElementFilter and SoupStrainer both declare with that parameter.
    static List<Expr> sharedArgs(Call c, List<FunctionDef> cand) {
        if (cand == null || cand.isEmpty()) {
            return null;
        }
        List<Expr> r = staticArgs(c, cand.get(0), 1, true);
        for (int i = 1; r != null && i < cand.size(); i++) {
            List<Expr> o = staticArgs(c, cand.get(i), 1, true);
            if (o == null || o.size() != r.size()) {
                return null;
            }
            for (int j = 0; j < r.size(); j++) {
                if (!sameArg(r.get(j), o.get(j))) {
                    return null;
                }
            }
        }
        return r;
    }

    /// Whether every one of `cand` takes only positional parameters.
    private static boolean positionalOnly(List<FunctionDef> cand) {
        for (FunctionDef m : cand) {
            if (!m.args.kwonly.isEmpty() || m.args.vararg != null || m.args.kwarg != null) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameArg(Expr a, Expr b) {
        if (a == b) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        return a instanceof Constant && b instanceof Constant && ((Constant) a).kind == ((Constant) b).kind
                && java.util.Objects.equals(((Constant) a).text, ((Constant) b).text);
    }

    /// The `__init__` a call of class `k` runs, when it is typable.
    private static FunctionDef typableInit(Layouts.Cls k) {
        Layouts.Cls o = Layouts.resolver(k, "__init__");
        return o == null ? null : o.typable.get("__init__");
    }

    /// Set while the arguments of a construction of a class of our own
    /// `typing` module escape: see [#callKnown].
    private boolean typingHeld;

    /// A call of function `f` or of class `k`, whichever is not null.
    /// `typing`'s classes (TypeVar's `bound` and constraints, NewType's
    /// base) keep a class they are given and never call it -- the module
    /// is ours, and says so where it stores them -- so a class escaping
    /// into one is loose but no root of instances ([#looseRoot]). bs4's
    /// `TypeVar("_PageElementT", bound=PageElement)` made PageElement live
    /// as itself, and every `else` of an isinstance test on one read a
    /// PageElement. typing never calls what it holds, so the class value
    /// is no root of the sweep's calls of a class nobody names either.
    private Ty callKnown(Call c, Fn f, Layouts.Cls k, Fn fn, Scope sc, boolean simple) {
        boolean saved = typingHeld;
        Infer home = k == null ? null : p.home.get(k);
        typingHeld = f == null && home != null && "typing".equals(home.moduleName);
        try {
            return callKnown0(c, f, k, fn, sc, simple);
        } finally {
            typingHeld = saved;
        }
    }

    private Ty callKnown0(Call c, Fn f, Layouts.Cls k, Fn fn, Scope sc, boolean simple) {
        if (f == null && k != null && k.enumMeta) {
            // A member lookup (Enum's value table), not a construction:
            // the result is a member of the class (a Flag pseudo-member
            // included) or what _missing_ answers, which must be one. Only
            // the functional API (more arguments) makes a class.
            boolean lookup = c.args.size() == 1 && c.keywords.isEmpty() && !(c.args.get(0) instanceof Starred);
            for (Expr x : c.args) {
                escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            return lookup && k.compiled && !k.builtinOwn ? Ty.inst(k, false) : Ty.OBJECT;
        }
        constructs(k);
        List<Expr> given = c.args;
        FunctionDef d = f != null ? f.def : typableInit(k);
        if (!c.keywords.isEmpty() || d != null && !d.args.kwonly.isEmpty()) {
            List<Expr> b = d == null ? null : staticArgs(c, d, f != null ? 0 : 1);
            if (b != null) {
                given = b;
                simple = true;
            } else if (c.keywords.isEmpty()) {
                // A keyword-only parameter no positional call can fill.
                simple = false;
            }
        }
        List<Ty> args = new ArrayList<Ty>();
        for (Expr a : given) {
            if (a instanceof Starred) {
                // Unpacked into positions the call cannot see: walked, so
                // what it calls is reached, and escaped.
                escape(type(((Starred) a).value, fn, sc));
                args.add(Ty.OBJECT);
            } else {
                args.add(type(a, fn, sc));
            }
        }
        if (given == c.args) { //NOPMD CompareObjectsWithEquals - identity is meant
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
        }
        if (f != null) {
            if (!callFn(f, args, 0, simple && args.size() <= f.params.length
                    && args.size() >= f.params.length - f.def.args.defaults.size(), true)) {
                // Through the door: the typed version's result when the
                // arguments turn out to fit, the generic body's when not.
                siteRet.remove(c);
                doors.add(c);
                Fn g = generic.get(f.def);
                return retOf(f, g == null ? f.ret : mix(f.ret, g.ret));
            }
            // An expression function of this module's: typed
            // code relabels the call's result (TypedEmit).
            Ty site = !shadow && simple && args.size() == f.params.length
                    && p.fnHome.get(f) == this ? siteType(f, args) : null; //NOPMD CompareObjectsWithEquals - identity is meant
            if (site != null) {
                siteRet.put(c, site);
                return retOf(f, site);
            }
            if (!shadow) {
                siteRet.remove(c);
            }
            return retOf(f, f.ret);
        }
        if (open.contains(k)) {
            for (Ty a : args) {
                escape(a);
            }
            escapeClass(k);
            return Ty.UNSEEN;
        }
        if (k.callConstructed()) {
            // Constructed by calling the class (TypedEmit#construct), so
            // BaseException's `__new__` sets `args`: its `__init__` runs
            // through its door, on arguments that escape.
            for (Ty a : args) {
                escape(a);
            }
            Layouts.Cls io = Layouts.resolver(k, "__init__");
            FunctionDef idef = io == null ? null : io.typable.get("__init__");
            if (idef != null) {
                escapeFn(p.fns.get(idef));
            }
            return Ty.OBJECT;
        }
        Fn made = newMethod(k);
        if (made != null) {
            // The class's `__new__` makes the object (the call goes through
            // the class: TypedEmit#construct), and `__init__` runs on it
            // when it is an instance of the class.
            List<Ty> na = new ArrayList<Ty>();
            na.add(classValue(k));
            na.addAll(args);
            callFn(made, na, 0, simple && na.size() <= made.params.length
                    && na.size() >= made.params.length - made.def.args.defaults.size(), true);
            Fn g = generic.get(made.def);
            Ty r = retOf(made, g == null ? made.ret : mix(made.ret, g.ret));
            Layouts.Cls io = Layouts.resolver(k, "__init__");
            FunctionDef idef = io == null ? null : io.typable.get("__init__");
            if (idef != null) {
                Fn fi = p.fns.get(idef);
                if (!shadow) {
                    fi.params[0] = joinInto(fi.params[0], Ty.inst(k, false));
                }
                callFn(fi, args, 1, simple && args.size() + 1 <= fi.params.length
                        && args.size() + 1 >= fi.params.length - idef.args.defaults.size(), true);
            }
            return r;
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
            if (!callFn(fi, args, 1, simple && args.size() + 1 <= fi.params.length
                    && args.size() + 1 >= fi.params.length - init.args.defaults.size(), true) && !shadow) {
                // An argument the typed __init__ cannot be sure of: the
                // class is called generically, through the guard.
                doors.add(c);
            }
        } else {
            for (Ty a : args) {
                escape(a);
            }
        }
        return Ty.inst(k, false);
    }

    private Ty call1(Call c, Fn fn, Scope sc, boolean simple) {
        Ty fresh = builtinNew(c, fn, sc);
        if (fresh != null) {
            return fresh;
        }
        if (isMathCall(c, sc)) {
            boolean numbers = true;
            for (Expr x : c.args) {
                Ty t = type(x, fn, sc);
                numbers &= (t.k == Ty.K.INT || t.k == Ty.K.FLOAT) && !t.opt;
            }
            return numbers ? Ty.FLOAT : Ty.OBJECT;
        }
        if (!c.args.isEmpty() || c.func instanceof Attribute) {
            List<Ty> na = new ArrayList<Ty>();
            for (Expr x : c.args) {
                na.add(starArg(x, fn, sc));
            }
            Ty nr = nativeRet(c, na, sc);
            if (nr != null) {
                nativeCalls.put(c, nr);
                return nr;
            }
            nativeCalls.remove(c);
        }
        if (c.func instanceof Attribute && dictOwner(((Attribute) c.func).value, sc) != null) {
            // A method of `x.__dict__`: a read leaves the attributes as
            // they are, anything else may set or remove any of them.
            Attribute a = (Attribute) c.func;
            Expr owner = dictOwner(a.value, sc);
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                args.add(starArg(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            for (Ty t : args) {
                escape(t);
            }
            Ty r = type(owner, fn, sc);
            // A removal stores no value: no field gets a type it did not
            // have (bs4's decompose clears every element's dict).
            if (!DICT_READS.contains(a.attr) && !"clear".equals(a.attr) && !"pop".equals(a.attr)
                    && !"popitem".equals(a.attr)) {
                boolean named = ("setdefault".equals(a.attr) || "__setitem__".equals(a.attr)) && c.args.size() == 2
                        && !(c.args.get(0) instanceof Starred) && c.keywords.isEmpty();
                computedStore(r, owner, named ? c.args.get(0) : null, named ? args.get(1) : Ty.OBJECT, c, fn, sc);
            }
            return Ty.UNSEEN;
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
                        set ? type(c.args.get(2), fn, sc) : Ty.OBJECT, c, fn, sc);
                Expr owner = ((Attribute) c.func).value;
                if (owner instanceof Name && "object".equals(((Name) owner).id)
                        && sc.resolve("object") == Scope.Binding.BUILTIN && !moduleScope.assigned.contains("object")) {
                    // The builtin's own store, which runs none of the
                    // program's code: the receiver goes nowhere else.
                    return Ty.NONE;
                }
            } else if (c.args.size() == n) {
                Expr owner = ((Attribute) c.func).value;
                boolean viaSuper = owner instanceof Call && ((Call) owner).func instanceof Name
                        && "super".equals(((Name) ((Call) owner).func).id);
                if (!(viaSuper && p.plainSelf.contains(sc))) {
                    computedStore(viaSuper ? Ty.OBJECT : type(owner, fn, sc), owner, c.args.get(0),
                            set ? type(c.args.get(1), fn, sc) : Ty.OBJECT, c, fn, sc);
                }
            }
        }
        if (c.func instanceof Attribute) {
            Fn sup = superTarget((Attribute) c.func, fn, sc);
            if (sup != null) {
                // `super().m(args)`: the parent's `m`, with self first.
                List<Ty> args = new ArrayList<Ty>();
                args.add(nameType(fn.names.get(0), fn, sc));
                for (Expr x : c.args) {
                    args.add(starArg(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                boolean exact = simple && c.keywords.isEmpty() && args.size() <= sup.params.length
                        && args.size() >= sup.params.length - sup.def.args.defaults.size();
                callFn(sup, args, 0, exact, false);
                boolean local = p.fnHome.get(sup) == this; //NOPMD CompareObjectsWithEquals - identity is meant
                if (exact && local && !shadow) {
                    p.superCalls.put(c, sup);
                }
                // Another module's method is called generically (its typed
                // copy is that module's), and the generic body answers
                // when the arguments miss the guard.
                Fn g = local ? null : generic.get(sup.def);
                return retOf(sup, g == null ? sup.ret : mix(sup.ret, g.ret));
            }
        }
        if (c.func instanceof Attribute) {
            Attribute a = (Attribute) c.func;
            if (calledClass(a.value, sc) != null) {
                // `Cls.method(obj, ...)`: an explicit call of a method, of a
                // class of this module or one imported (bs4's
                // `Tag.__init__(self, ...)` in BeautifulSoup, whose Tag is
                // bs4.element's: unresolved, it escaped every `__init__`).
                Layouts.Cls k = calledClass(a.value, sc);
                constructs(k);
                Layouts.Cls o = Layouts.resolver(k, a.attr);
                FunctionDef m = o == null ? null : o.typable.get(a.attr);
                boolean plainCall = m != null && simple && c.keywords.isEmpty() && !hasStarred(c.args);
                if (!plainCall && o != null && !open.contains(k)) {
                    FunctionDef dd = bodyDef(o, a.attr);
                    Ty t = dd == null || !dd.decorators.isEmpty() || dd.isAsync || p.fns.get(dd) == null
                            || Layouts.IMPLICIT_CLASS_LEVEL.contains(a.attr) || o.home.fieldOwners.containsKey(a.attr)
                            ? null : doorBound(c, java.util.Collections.singletonList(dd), 0, fn, sc);
                    if (t != null) {
                        return t;
                    }
                }
                List<Ty> args = new ArrayList<Ty>();
                for (Expr x : c.args) {
                    args.add(starArg(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                if (m != null && !open.contains(k)) {
                    Fn f = p.fns.get(m);
                    boolean d = !callFn(f, args, 0, simple && args.size() <= f.params.length
                            && args.size() >= f.params.length - m.args.defaults.size(), true);
                    if (d) {
                        // An argument the typed version cannot be sure of:
                        // called as generic code calls it, through the
                        // method's guard (TypedEmit).
                        doors.add(c);
                    }
                    // As for super(): another module's method is called
                    // generically, and its generic body may answer.
                    Fn g = !d && p.fnHome.get(f) == this ? null : generic.get(m); //NOPMD CompareObjectsWithEquals - identity is meant
                    return retOf(f, g == null ? f.ret : mix(f.ret, g.ret));
                }
                for (Ty t : args) {
                    escape(t);
                }
                if (m != null) {
                    escapeFn(p.fns.get(m));
                }
                return Ty.UNSEEN;
            }
            if (foreign(a.value, sc) instanceof Infer) {
                // `module.name(...)` of a name no direct call resolved: the
                // value is called as any value is. The module itself is not
                // passed anywhere, so it does not escape.
                moduleMember(a, sc);
                for (Expr x : c.args) {
                    escape(x instanceof Starred ? type(((Starred) x).value, fn, sc) : type(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                return Ty.UNSEEN;
            }
            Ty r = selfRecv(a.value, a.attr, fn, sc);
            if (r.k == Ty.K.INST && closedFor(r.cls, a.attr) && candidates(r.cls, a.attr) == null) {
                Ty t = doorCall(c, r.cls, a.attr, fn, sc);
                if (t != null) {
                    return t;
                }
            }
            Comp eg = r.k == Ty.K.LIST && !r.opt ? extendedGenerator(c) : null;
            if (eg != null && extendGens.add(eg)) {
                p.changed = true;
            }
            Comp was = eg == null ? extendedGenerator(c) : null;
            if (was != null && extendGens.contains(was)) {
                // The list is no longer one this pass follows: what the
                // generator gave it goes where the list went.
                escape(genElems.get(was));
            }
            List<Expr> given = c.args;
            if ((!c.keywords.isEmpty() || kwOnlyCandidate(r, a.attr)) && r.k == Ty.K.INST && closedFor(r.cls, a.attr)) {
                // One method it can be: keywords bound to its parameters,
                // keyword-only ones to their defaults.
                List<Expr> b = sharedArgs(c, candidates(r.cls, a.attr));
                if (b != null) {
                    given = b;
                    simple = true;
                }
            }
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : given) {
                args.add(starArg(x, fn, sc));
            }
            if (given == c.args) { //NOPMD CompareObjectsWithEquals - identity is meant
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
            }
            if (r.k == Ty.K.BOT || r.k == Ty.K.NONE) {
                return Ty.BOT;
            }
            if (r.k == Ty.K.UNION) {
                Ty ret = unionCall(c, r, a.attr, args, simple);
                if (ret != null) {
                    return ret;
                }
            }
            if (r.k == Ty.K.INST && closedFor(r.cls, a.attr)) {
                List<FunctionDef> cand = candidates(r.cls, a.attr);
                if (cand != null) {
                    Ty ret = Ty.BOT;
                    boolean door = false;
                    for (FunctionDef m : cand) {
                        // A call with keywords binds them at build time,
                        // and can be made generically as bound when every
                        // parameter is positional (TypedEmit.inlineDoor).
                        boolean d = !callFn(p.fns.get(m), args, 1,
                                simple && binds(p.fns.get(m), args.size() + 1),
                                c.keywords.isEmpty() || positionalOnly(cand));
                        door |= d;
                    }
                    if (door) {
                        // Through the doors: the call is made as generic
                        // code makes it, and each method's guard picks the
                        // version that runs (TypedEmit).
                        doors.add(c);
                    }
                    for (FunctionDef m : cand) {
                        Fn f = p.fns.get(m);
                        Fn g = door ? generic.get(m) : null;
                        ret = mix(ret, retOf(f, g == null ? f.ret : mix(f.ret, g.ret)));
                    }
                    if (Layouts.resolver(r.cls, a.attr) == null) {
                        // Not every instance has it: the rest raise.
                        ret = mix(ret, Ty.BOT);
                    }
                    return ret;
                }
            }
            if (r.k == Ty.K.LIST && !r.opt) {
                Comp g = extendedGenerator(c);
                if (g != null && extendGens.contains(g)) {
                    Ty ge = genElems.get(g);
                    storeElem(r, ge == null ? Ty.BOT : ge);
                    return Ty.NONE;
                }
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
                siteJoin(s, Ty.STR);
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
            if (r.k == Ty.K.STR && !r.opt) {
                Ty t = strResult(a.attr);
                if (t != null) {
                    // Any other str method: its result is the same type
                    // whatever the arguments, or it raises.
                    for (Ty x : args) {
                        escape(x);
                    }
                    for (Keyword kw : c.keywords) {
                        escape(type(kw.value, fn, sc));
                    }
                    return t;
                }
            }
            if (r.k == Ty.K.INST && !r.opt && closedFor(r.cls, a.attr) && !plainReceiver(a.value, sc)) {
                // `self.convert_s(...)` where the class body binds
                // `convert_s` to a function a call made, or to another
                // method under a second name: each function it is runs,
                // bound to the instance.
                Ty v = Layouts.resolver(r.cls, a.attr) == null || classAttr(r.cls, a.attr)
                        ? attrsNamed(r.cls, java.util.Collections.singletonList(a.attr), null) : null;
                if (v != null && v.k == Ty.K.BOT) {
                    return Ty.BOT;
                }
                List<Fn> fs = new ArrayList<Fn>();
                if (v != null && callables(v.nonNone(), fs, new ArrayList<Integer>())) {
                    Ty t = callThrough(c, v, fn, sc);
                    if (t != null) {
                        return t;
                    }
                    // Starred: each is entered with what it is given.
                    for (Expr x : c.args) {
                        escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
                    }
                    for (Keyword kw : c.keywords) {
                        escape(type(kw.value, fn, sc));
                    }
                    Ty ret = Ty.BOT;
                    for (Fn f : fs) {
                        escapeFn(f);
                        ret = mix(ret, retOf(f, f.ret));
                    }
                    return ret;
                }
            }
            if (r.k == Ty.K.INST && closedFor(r.cls, a.attr) && Layouts.resolver(r.cls, a.attr) == null
                    && p.home.get(r.cls).layouts.fieldOwners.containsKey(a.attr)) {
                // `self.match_function(element)`: a function held in a
                // field. The call gets the field's value, not the
                // instance, so only the value and the arguments escape.
                escape(attrType(r, a.attr));
                for (Ty t : args) {
                    escape(t);
                }
                return Ty.UNSEEN;
            }
            if (r.k == Ty.K.INST && closedFor(r.cls, a.attr) && classLevel(r.cls, a.attr)) {
                // A classmethod or staticmethod: the instance is not
                // passed, only its class (or nothing), so it does not
                // escape. bs4's `self._make_match_rules(...)` in
                // SoupStrainer.__init__ made every field of SoupStrainer
                // untyped.
                for (Ty t : args) {
                    escape(t);
                }
                return Ty.UNSEEN;
            }
            if (r.wide() && "__init__".equals(a.attr) && objectInit(a.value, sc)) {
                // `LTText.__init__(self)` for a class of this module that
                // nothing in its MRO gives an `__init__`: object's, which
                // does nothing with its arguments.
                for (Ty t : args) {
                    escape(t);
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                return Ty.NONE;
            }
            Ty unbound = unboundCall(c, a, args, simple, fn, sc);
            if (unbound != null) {
                return unbound;
            }
            // `ValueError.__init__(self, msg)`: a builtin, unshadowed, runs
            // none of the program's methods, whatever its value is typed.
            boolean builtinRecv = a.value instanceof Name && sc.resolve(((Name) a.value).id) == Scope.Binding.BUILTIN
                    && !moduleScope.assigned.contains(((Name) a.value).id);
            boolean duck = (r.wide() || r.k == Ty.K.INST) && !superBuiltin(a, fn, sc) && !builtinRecv;
            // A keyword reaches an answering __getattr__'s value unseen.
            boolean slot = duck && closedName(a.attr) && bindable(c) && (c.keywords.isEmpty() || !answeringLive(r));
            if (!slot) {
                if (!superWithin(a, fn, sc)) {
                    escape(r);
                }
                for (Ty t : args) {
                    escape(t);
                }
            }
            Ty ret = Ty.OBJECT;
            if (duck) {
                superClass = superOf(a, fn, sc);
                superCall = a.value instanceof Call && ((Call) a.value).func instanceof Name
                        && "super".equals(((Name) ((Call) a.value).func).id)
                        && sc.resolve("super") == Scope.Binding.BUILTIN && !moduleScope.assigned.contains("super");
                try {
                    slotDuck = slot;
                    duckBound = receiverBound(a.value, fn);
                    ret = duckCall(r, a.attr, args, simple && c.keywords.isEmpty(), bindable(c) ? c : null, fn, sc);
                    if (slot) {
                        ret = mix(ret, answeredCall(r, args, simple && c.keywords.isEmpty(), fn, sc));
                    }
                } finally {
                    superClass = null;
                    superCall = false;
                    slotDuck = false;
                    duckBound = null;
                }
            }
            // Only the program's methods answer a closed name: the call's
            // value is what they return (none: it raises AttributeError).
            if (slot && p.slotCalls.add(c)) {
                p.changed = true;
            }
            return slot ? ret : Ty.UNSEEN;
        }
        if (c.func instanceof Name && sc.resolve(((Name) c.func).id) == Scope.Binding.BUILTIN
                && !moduleScope.hasStarImport) {
            String n = ((Name) c.func).id;
            List<Ty> args = new ArrayList<Ty>();
            for (Expr x : c.args) {
                if ("super".equals(n) && args.isEmpty() && c.args.size() == 2 && x instanceof Name
                        && sc.resolve(((Name) x).id) == Scope.Binding.GLOBAL
                        && globalClasses.get(((Name) x).id) != null) {
                    // `super(K, obj)`'s K is looked up, never held: not a
                    // class value (see [#classValue]), which would make K
                    // constructible by whoever holds it.
                    args.add(Ty.cls(new int[0]));
                    continue;
                }
                args.add(starArg(x, fn, sc));
            }
            for (Keyword kw : c.keywords) {
                escape(type(kw.value, fn, sc));
            }
            if (("setattr".equals(n) || "delattr".equals(n) || "hasattr".equals(n)) && c.args.size() >= 2
                    && !(c.args.get(1) instanceof Constant)
                    // `hasattr(self, "do_" + op)` asks the same table of the
                    // class's own methods a getattr through that name reads.
                    && !("hasattr".equals(n) && args.get(0).k == Ty.K.INST && !args.get(0).opt
                    && !plainReceiver(c.args.get(0), sc)
                    && methodsNamed(args.get(0).cls, c.args.get(1), fn, sc) != null)) {
                reflection(n, c.args.get(1), fn, sc);
            }
            if ("setattr".equals(n) && c.args.size() == 3 && c.keywords.isEmpty()) {
                computedStore(args.get(0), c.args.get(0), c.args.get(1), args.get(2), c, fn, sc);
            } else if ("delattr".equals(n) && c.args.size() == 2 && c.keywords.isEmpty()
                    && !(c.args.get(1) instanceof Constant)) {
                // Whichever attribute it was is gone: no type holds for it.
                computedStore(args.get(0), c.args.get(0), c.args.get(1), Ty.OBJECT, c, fn, sc);
            } else if ("vars".equals(n) && c.args.size() == 1 && c.keywords.isEmpty()
                    && !(c.args.get(0) instanceof Starred)) {
                dictOf(c, c.args.get(0), fn, sc);
            } else if ("type".equals(n) && c.args.size() == 1 && c.keywords.isEmpty()
                    && !(c.args.get(0) instanceof Starred)) {
                Ty known = classesBelow(args.get(0));
                if (known != null) {
                    // One of the compiled classes at or below the static
                    // one: a construction through it is tracked
                    // (#constructAny), so nothing escapes.
                    return known;
                }
                if (args.get(0).wide() || args.get(0).k == Ty.K.INST) {
                    // Any class at all. Reading its name, comparing it and
                    // testing with it make nothing of it; what does -- a
                    // call, a store, passing it where it is not followed --
                    // is an escape of it (#escape), and only then is every
                    // loose class taken to be constructible (#anyClass).
                    if (args.get(0).k == Ty.K.INST) {
                        // As for `x.__class__` above.
                        loosen(args.get(0).cls);
                    }
                    return Ty.cls(new int[0]);
                }
                classOf(c, c.args.get(0), fn, sc);
            } else if ("getattr".equals(n) && c.args.size() >= 2 && c.keywords.isEmpty()
                    && !(c.args.get(1) instanceof Constant)) {
                Ty found = args.get(0).k == Ty.K.INST && !args.get(0).opt && !plainReceiver(c.args.get(0), sc)
                        ? methodsNamed(args.get(0).cls, c.args.get(1), fn, sc) : null;
                if (found == null) {
                    reflection("getattr", c.args.get(1), fn, sc);
                }
                if (found != null) {
                    // The methods the name can be, as bound methods: a call
                    // through the result is followed to each of them.
                    return args.size() == 3 ? mix(found, args.get(2)) : found;
                }
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
                // where OBJECT now would stay OBJECT for good.
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
                siteJoin(s, t.k == Ty.K.LIST ? elem(t) : range ? Ty.INT : Ty.OBJECT);
                if (t.k != Ty.K.LIST && !rangeCall(c.args.get(0), sc)) {
                    escape(t);
                }
                return Ty.list(new int[] {s}, false);
            }
            if ("type".equals(n) && args.size() == 1 && simple) {
                // type(x) reads x's class (classOf answers for the class);
                // x itself goes nowhere.
                return Ty.UNSEEN;
            }
            if ("dict".equals(n) && args.size() <= 1 && !anyStarred(c.args)) {
                Ty t = Ty.dict(new int[] {site(c)});
                if (args.size() == 1) {
                    Ty a0 = args.get(0);
                    if (a0.k == Ty.K.DICT && a0.sites.length > 0) {
                        storeDict(t, dictKeyOf(a0), dictVal(a0));
                    } else {
                        escape(a0);
                        storeDict(t, Ty.OBJECT, Ty.OBJECT);
                    }
                }
                for (Keyword kw : c.keywords) {
                    if (kw.arg == null) {
                        Ty kt = type(kw.value, fn, sc);
                        storeDict(t, kt.k == Ty.K.DICT ? dictKeyOf(kt) : Ty.OBJECT, kt.k == Ty.K.DICT ? dictVal(kt) : Ty.OBJECT);
                    } else {
                        storeDict(t, Ty.STR, type(kw.value, fn, sc));
                    }
                }
                return t;
            }
            String made = madeBuiltin(n);
            if (made != null) {
                boolean reads0 = READ_ONLY_BUILTINS.contains(n);
                for (Ty t : args) {
                    escape(reads0 && t.k == Ty.K.LIST ? elem(t) : t);
                }
                return Ty.obj(made);
            }
            boolean reads = simple && READ_ONLY_BUILTINS.contains(n);
            if ("hasattr".equals(n) && args.size() == 2 && c.keywords.isEmpty()
                    && args.get(0).k == Ty.K.CLS && !args.get(0).opt && args.get(0).sites.length == 0
                    && c.args.get(1) instanceof Constant && ((Constant) c.args.get(1)).kind == Constant.Kind.STR) {
                // `hasattr(cls, "__deepcopy__")` reads the attribute and
                // drops it.
                anyClassAttr(((Constant) c.args.get(1)).text);
                return Ty.BOOL;
            }
            if ("getattr".equals(n) && args.size() == 2 && c.keywords.isEmpty() && args.get(0).k == Ty.K.INST
                    && c.args.get(1) instanceof Constant && ((Constant) c.args.get(1)).kind == Constant.Kind.STR
                    && ConstArgSpecialize.ident(((Constant) c.args.get(1)).text)
                    && !((Constant) c.args.get(1)).text.startsWith("__")) {
                // `getattr(x, "name")` is `x.name`: typed as that read, so a
                // list it answers and something appends to widens the field
                // it came from, as `x.name.append(v)` does.
                Attribute same = literalGetattr.get(c);
                if (same == null) {
                    same = new Attribute(c.args.get(0), ((Constant) c.args.get(1)).text).at(c.line, c.col);
                    literalGetattr.put(c, same);
                }
                return type(same, fn, sc);
            }
            if ("getattr".equals(n) && (args.size() == 2 || args.size() == 3) && c.keywords.isEmpty()
                    && args.get(0).k == Ty.K.CLS && !args.get(0).opt && args.get(0).sites.length == 0
                    && c.args.get(1) instanceof Constant && ((Constant) c.args.get(1)).kind == Constant.Kind.STR) {
                // `getattr(cls, "__copy__", None)`: an attribute read.
                if (args.size() == 3) {
                    escape(args.get(2));
                }
                return anyClassAttr(((Constant) c.args.get(1)).text);
            }
            if (("isinstance".equals(n) || "issubclass".equals(n)) && args.size() == 2) {
                // The class only answers a test; nothing holds or calls it.
                // An instance is only asked its type, unless a class it may
                // be answers `__class__` itself.
                if (args.get(0).k != Ty.K.CLS && !(args.get(0).k == Ty.K.INST && !ownClassAttr(args.get(0).cls))) {
                    escape(args.get(0));
                }
                return Ty.BOOL;
            }
            if ("super".equals(n) && args.size() == 2 && args.get(0).k == Ty.K.CLS) {
                // `super(K, obj)` only looks K up in obj's MRO: the class
                // goes nowhere. Escaping it escaped K's own constructor
                // from inside that constructor, and kept it walked.
                // Neither does an instance of K when every class above K
                // is known: a super object stores no attribute and reads
                // none of the instance's, so all it can do with obj is run
                // one of those classes' methods on it, each through its
                // own guard when the pass does not follow the call.
                Ty o = args.get(1);
                List<Layouts.Cls> ks = classesOf(args.get(0));
                boolean known = o.k == Ty.K.INST && !o.opt && ks.size() == 1
                        && isSelfOrAncestor(ks.get(0), o.cls) && chainKnown(ks.get(0));
                if (!known) {
                    escape(o);
                }
                return Ty.UNSEEN;
            }
            Ty lowered = anyStarred(c.args) ? null : builtinValue(n, c, args, fn, sc);
            if (lowered != null) {
                return lowered;
            }
            Ty fixed = fixedReturn(n);
            if (fixed != null) {
                for (Ty t : args) {
                    escape(t);
                }
                return fixed;
            }
            for (Ty t : args) {
                // These iterate a list and never write it: only what it holds
                // leaves. Escaping the list itself made bs4's
                // `reversed(self.contents)` untype every tag's children.
                escape(reads && t.k == Ty.K.LIST ? elem(t) : t);
            }
            return Ty.UNSEEN;
        }
        Ty ft = type(c.func, fn, sc);
        if (ft.k == Ty.K.CLS && !ft.opt && ft.sites.length > 0) {
            return constructAny(c, ft, fn, sc, simple);
        }
        Ty through = callThrough(c, ft, fn, sc);
        if (through != null) {
            return through;
        }
        // Called in a way the pass does not bind (starred arguments, a union
        // of callables): the functions it may be are entered from anywhere.
        escapeFuncs(ft);
        if (ft.k == Ty.K.INST && !ft.opt && closedFor(ft.cls, "__call__") && !anyStarred(c.args)) {
            // `soup(["script"])`: the class's `__call__`, as `soup.__call__(...)`.
            List<FunctionDef> cand = candidates(ft.cls, "__call__");
            if (cand != null && !cand.isEmpty()) {
                List<Ty> args = new ArrayList<Ty>();
                args.add(ft);
                for (Expr x : c.args) {
                    args.add(type(x, fn, sc));
                }
                for (Keyword kw : c.keywords) {
                    escape(type(kw.value, fn, sc));
                }
                Ty ret = Ty.BOT;
                for (FunctionDef m : cand) {
                    Fn f = p.fns.get(m);
                    callFn(f, args, 0, simple && c.keywords.isEmpty() && binds(f, args.size()), true);
                    ret = mix(ret, retOf(f, f.ret));
                }
                doors.add(c);
                return ret;
            }
            Ty viaDoor = doorCall(c, ft.cls, "__call__", fn, sc);
            if (viaDoor != null) {
                return viaDoor;
            }
        }
        escape(ft);
        for (Expr x : c.args) {
            escape(type(x instanceof Starred ? ((Starred) x).value : x, fn, sc));
        }
        for (Keyword kw : c.keywords) {
            escape(type(kw.value, fn, sc));
        }
        return Ty.UNSEEN;
    }

    /// The hidden calls of a miss probe (`Parser.missProbe`): the probe is
    /// the lookup or a marker, the re-read is the lookup.
    private Ty missIntrinsic(String n, Call c, Fn fn, Scope sc) {
        switch (n) {
            case "$probe":
                escape(type(c.args.get(0), fn, sc));
                return Ty.OBJECT;
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

    /// `r.name(args)` on a receiver that is one of several types: each
    /// member's own method takes the call, and the call is made as generic
    /// code makes it (through the doors), which dispatches on the object
    /// that arrives. Null when a member is one this cannot resolve the
    /// method of.
    private Ty unionCall(Call c, Ty r, String name, List<Ty> args, boolean simple) {
        Ty ret = Ty.BOT;
        for (Ty m0 : r.members()) {
            if (m0.k == Ty.K.NONE) {
                // Calling a method of None raises.
                continue;
            }
            Ty m = m0.nonNone();
            Ty t = null;
            if (m.k == Ty.K.INST && closedFor(m.cls, name)) {
                List<FunctionDef> cand = candidates(m.cls, name);
                if (cand != null) {
                    t = Layouts.resolver(m.cls, name) == null ? Ty.BOT : null;
                    for (FunctionDef d : cand) {
                        Fn f = p.fns.get(d);
                        callFn(f, args, 1, false, true);
                        Fn g = generic.get(d);
                        t = mix(t, retOf(f, g == null ? f.ret : mix(f.ret, g.ret)));
                    }
                }
            } else if (m.k == Ty.K.LIST) {
                t = listMethod(m, name, args);
            } else if (m.k == Ty.K.DICT && simple) {
                t = dictMethod(m, name, args);
            } else if (m.k == Ty.K.STR && c.keywords.isEmpty() && !hasStarred(c.args)) {
                t = strMethod(name, args);
            }
            if (t == null) {
                return null;
            }
            ret = mix(ret, t);
        }
        doors.add(c);
        return ret;
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
                    storeElem(r, args.get(0).k == Ty.K.LIST ? elem(args.get(0)) : Ty.OBJECT);
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
        return Ty.UNSEEN;
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
    /// The type every call of the str method `m` answers, whatever its
    /// arguments, or null for a name that is not one.
    static Ty strResult(String m) {
        switch (m) {
            case "format":
            case "format_map":
            case "capitalize":
            case "casefold":
            case "center":
            case "expandtabs":
            case "ljust":
            case "lower":
            case "lstrip":
            case "removeprefix":
            case "removesuffix":
            case "replace":
            case "rjust":
            case "rstrip":
            case "strip":
            case "swapcase":
            case "title":
            case "translate":
            case "upper":
            case "zfill":
            case "join":
                return Ty.STR;
            case "count":
            case "find":
            case "index":
            case "rfind":
            case "rindex":
                return Ty.INT;
            case "endswith":
            case "isalnum":
            case "isalpha":
            case "isascii":
            case "isdecimal":
            case "isdigit":
            case "isidentifier":
            case "islower":
            case "isnumeric":
            case "isprintable":
            case "isspace":
            case "istitle":
            case "isupper":
            case "startswith":
                return Ty.BOOL;
            case "encode":
                return Ty.BYTES;
            default:
                return null;
        }
    }

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
    Ty pin(Fn f, int i) {
        if (i >= f.def.args.positional.size() || f.cls != null && i == 0) {
            return null;
        }
        Infer h = p.fnHome.get(f);
        return (h == null ? this : h).annotated(f.def.args.positional.get(i).annotation, f.def.scope.parent, 0);
    }

    /// The type annotation `a` names, read in scope `sc` of this module, when
    /// it is one the guard at the door can test: `str`, `int`, `bytes`, a
    /// compiled class (imported or not), each also `Optional[...]`,
    /// `... | None` or written as a string. Null otherwise. An annotation is
    /// only a guess: nothing is unsound when it lies, only slower.
    Ty annotated(Expr a, Scope sc, int depth) {
        if (a == null || sc == null || depth > 4) {
            return null;
        }
        if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
            String t = ((Constant) a).text.trim();
            boolean opt = false;
            if (t.endsWith("| None")) {
                t = t.substring(0, t.length() - "| None".length()).trim();
                opt = true;
            } else if (t.startsWith("Optional[") && t.endsWith("]")) {
                t = t.substring("Optional[".length(), t.length() - 1).trim();
                opt = true;
            }
            if (!t.matches("[A-Za-z_][A-Za-z0-9_]*")) {
                return null;
            }
            Ty r = annotated(new Name(t), sc, depth + 1);
            return r == null || !opt ? r : r.orNone();
        }
        if (a instanceof Subscript && (isName(((Subscript) a).value, "Optional")
                || ((Subscript) a).value instanceof Attribute && "Optional".equals(((Attribute) ((Subscript) a).value).attr))) {
            Ty r = annotated(((Subscript) a).index, sc, depth + 1);
            return r == null ? null : r.orNone();
        }
        if (a instanceof BinOp && "|".equals(((BinOp) a).op)) {
            Expr l = ((BinOp) a).left;
            Expr r = ((BinOp) a).right;
            Expr other = isNone(r) ? l : isNone(l) ? r : null;
            Ty t = other == null ? null : annotated(other, sc, depth + 1);
            return t == null ? null : t.orNone();
        }
        if (a instanceof Name) {
            String n = ((Name) a).id;
            if (sc.resolve(n) == Scope.Binding.BUILTIN) {
                switch (n) {
                    case "str":
                        return Ty.STR;
                    case "int":
                        return Ty.INT;
                    case "bytes":
                        return Ty.BYTES;
                    default:
                        break;
                }
            }
        }
        if (a instanceof Name || a instanceof Attribute) {
            Layouts.Cls k = calledClass(a, sc);
            return k == null ? null : Ty.inst(k, false);
        }
        return null;
    }

    private static boolean isNone(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.NONE || isName(e, "None");
    }

    /// Joins a call's arguments into `f`'s parameters, from `from` on. False
    /// when the call goes through the function's door instead, which only a
    /// call site that can make that call asks for (`door`): an argument is
    /// one nothing here knows (OBJECT), or is not what the parameter's
    /// annotation says. Joining it would leave the parameter holding
    /// anything for every caller; instead the parameter keeps what the
    /// other calls pass -- or, when no call passes anything known, what its
    /// uses suggest ([#uncalled]) -- and this call is checked as it
    /// arrives. A wrong guess costs a guard, never correctness: the generic
    /// version runs what does not fit, and [#genericView] walks it.
    /// Whether `n` positional arguments (the receiver counted) bind every
    /// parameter of `f` at build time: each one left out has a default,
    /// whose type the definition already joined into the parameter.
    /// Whether a method `attr` an instance typed `r` may run takes
    /// keyword-only parameters.
    private boolean kwOnlyCandidate(Ty r, String attr) {
        if (r.k != Ty.K.INST) {
            return false;
        }
        List<FunctionDef> cand = candidates(r.cls, attr);
        if (cand != null) {
            for (FunctionDef m : cand) {
                if (!m.args.kwonly.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean binds(Fn f, int n) {
        // `*args` and `**kwargs` are always bound (to what is left over,
        // empty here), and a keyword-only parameter by its default.
        Node.Arguments a = f.def.args;
        if (!a.kwonly.isEmpty() && a.vararg == null && a.kwarg == null && n == f.params.length) {
            // A list [#staticArgs] bound to every parameter, keyword-only
            // ones included.
            return true;
        }
        for (Expr d : a.kwDefaults) {
            if (d == null) {
                return false;
            }
        }
        int np = a.positional.size();
        return n <= np && n >= np - a.defaults.size();
    }

    private boolean callFn(Fn f, List<Ty> args0, int from, boolean exact, boolean door) {
        if (exact && !f.def.args.kwonly.isEmpty() && args0.size() + from != f.params.length) {
            // Positions past the positional parameters are keyword-only:
            // only a list [#staticArgs] bound to every parameter binds them.
            exact = false;
        }
        List<Ty> args = new ArrayList<Ty>(args0.size());
        for (Ty a : args0) {
            // A bound method reaches the parameter as itself: the calls
            // the body makes through it are followed (see [#paramIn]).
            args.add(a);
        }
        reach(f);
        if (p.done) {
            // Asked again while the code is written: the parameters are
            // settled (and localized), so this only answers whether the
            // call fits them.
            for (int i = 0; i < args.size() && from + i < f.params.length; i++) {
                Ty a = args.get(i);
                if (a.unseen() || !Ty.join(f.params[from + i], a).equals(f.params[from + i])) {
                    return false;
                }
            }
            return exact;
        }
        if (shadow) {
            // A call the generic body makes goes through the callee's own
            // guard: it adds nothing to the callee's parameters, and has
            // the typed version's result only when its arguments fit it.
            boolean fit = exact;
            for (int i = 0; fit && i < args.size(); i++) {
                // A probe: `mix` would loosen a class the call leaves alone.
                // A parameter nothing has filled yet is no answer: the pass
                // runs again once one has, and this call is checked then.
                fit = f.params[from + i].k == Ty.K.BOT
                        || Ty.join(f.params[from + i], args.get(i)).equals(f.params[from + i]);
            }
            if (!fit && exact) {
                // The guard sends the call to the generic version, which is
                // walked with what reaches it (see [#genericView]).
                shadowMiss = true;
                Ty[] m = p.missed.get(f.def);
                if (m == null) {
                    m = new Ty[f.params.length];
                    java.util.Arrays.fill(m, Ty.BOT);
                    p.missed.put(f.def, m);
                    p.changed = true;
                }
                for (int i = 0; i < args.size(); i++) {
                    Ty j = Ty.join(m[from + i], args.get(i));
                    if (!j.equals(m[from + i])) {
                        m[from + i] = j;
                        p.changed = true;
                    }
                }
                return true;
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
        if (door) {
            boolean miss = false;
            for (int i = 0; !miss && i < args.size(); i++) {
                Ty pin = pin(f, from + i);
                // A wide argument joins into its parameter (below): the set
                // of types that reach it is what the body is typed for.
                // Escaping the callee instead made every other parameter
                // untyped too. Only an annotation the call contradicts
                // still sends it to the guard.
                miss = pin != null && Ty.join(pin, args.get(i)).k != pin.k;
            }
            if (miss) {
                for (int i = 0; i < f.params.length; i++) {
                    Ty pin = pin(f, i);
                    if (pin != null && f.params[i].k == Ty.K.BOT) {
                        f.params[i] = joinInto(f.params[i], pin);
                    }
                }
                // Entered through the guard, but with nothing for the
                // parameters yet: they are filled once nothing else does.
                if (!f.escapes) {
                    f.escapes = true;
                    p.changed = true;
                }
                for (Ty t : args) {
                    escape(t);
                }
                return false;
            }
        }
        boolean wideArg = false;
        Call cc = curCall;
        boolean aligned = cc != null && cc.keywords.isEmpty() && !anyStarred(cc.args) && cc.args.size() == args.size();
        for (int i = 0; i < args.size(); i++) {
            // A guessed instance is checked where it is read (`instanceof`),
            // so it is one of the kinds the parameter is typed for.
            // Not BOT: nothing has reached that argument yet in this round,
            // and the mark never comes off.
            if (args.get(i).unseen()
                    || aligned && args.get(i).k != Ty.K.INST && guessed(cc.args.get(i))) {
                boolean[] u = p.unseenArgs.computeIfAbsent(f, x -> new boolean[x.params.length]);
                if (!u[from + i]) {
                    u[from + i] = true;
                    p.changed = true;
                }
            }
            Ty held = f.params[from + i];
            if (door && args.get(i).k == Ty.K.OBJECT && !nested(f) && (held.k == Ty.K.BOT || held.k == Ty.K.INST)) {
                // (A nested function has no generic version walked: see
                // the walk of a FunctionDef. Only a parameter of instances:
                // yaml's Node.__init__, given `str | list` values and an
                // untyped one, typed the field `value` a list, and the
                // generic version's str reached a typed read of it.)
                // A value of no known type is no more evidence than an
                // unseen one: the call enters through the guard, which
                // hands the typed version what fits and the generic one
                // the rest. Joined, one such caller made a parameter every
                // precise caller agrees on untyped (markdownify's
                // process_tag, recursing over its own children).
                objectArg(f, from + i);
                wideArg = true;
                continue;
            }
            f.params[from + i] = joinInto(f.params[from + i], args.get(i));
            Ty d = declared(f, from + i);
            if (d != null && !(d.wide() && f.params[from + i].k == Ty.K.INST
                    && forwardedOnly(f, from + i))) {
                f.params[from + i] = joinInto(f.params[from + i], d);
            }
        }
        if (door) {
            for (Ty a : args) {
                if (a.unseen()) {
                    // No evidence for the parameter (Ty#UNSEEN), so it keeps
                    // what the other calls say; this call enters through
                    // the guard, which hands what does not fit to the
                    // generic version.
                    return false;
                }
            }
            if (wideArg) {
                // Entered through the guard: the emitter writes it so only
                // for a call listed here, and some callers of this method
                // drop the answer (a method called on self, a duck call).
                if (cc != null && !shadow) {
                    siteRet.remove(cc);
                    doors.add(cc);
                }
                return false;
            }
        }
        return true;
    }

    /// Whether parameter `i` of `f` is declared untyped only because the
    /// body hands it on ([#onlyForwarded]). That widening is for values
    /// the callee also takes in other kinds; an instance the visible calls
    /// pass keeps its class (markdownify's `convert_soup(self, soup)`
    /// made BeautifulSoup loose), and any other caller meets the guard.
    private boolean forwardedOnly(Fn f, int i) {
        if (i >= f.names.size() || i >= f.def.args.positional.size()) {
            return false;
        }
        String n = f.names.get(i);
        Expr a = f.def.args.positional.get(i).annotation;
        return !wideAnnotation(a) && !isinstanceTested(f.def.body, n) && onlyForwarded(f.def.body, n);
    }

    /// What parameter `i` of `f` says it takes beyond what the calls this
    /// pass sees pass it, joined into it at every call: the calls are only
    /// the ones it can see, and the source often says more. A parameter
    /// the body tests with `isinstance`, or annotated `object`, `Any` or a
    /// union of different kinds, takes values of several types by design:
    /// OBJECT, rather than the one type the visible calls happened to pass
    /// (pdfminer's `resolve1(x: object)` was typed `x: None` from its
    /// callers, and missed its guard on every int and dict). A parameter
    /// annotated one testable type (`s: bytes`) has that type joined in:
    /// calls seen passing only None make it `bytes | None`, not None.
    /// Null when the source says nothing. Like every type of a parameter,
    /// only a guess the door checks: never unsound, only slower or faster.
    private Ty declared(Fn f, int i) {
        Ty[] d = declaredCache.get(f);
        if (d == null) {
            d = new Ty[f.params.length];
            Infer h = p.fnHome.get(f);
            Infer in = h == null ? this : h;
            for (int k = f.cls != null ? 1 : 0; k < d.length; k++) {
                String n = f.names.get(k);
                Expr a = k < f.def.args.positional.size() ? f.def.args.positional.get(k).annotation : null;
                Ty at = a == null || wideAnnotation(a) ? null : in.annotated(a, f.def.scope.parent, 0);
                // A body that tests its parameter with isinstance, only
                // forwards it, or annotates it `object` says it takes
                // values of several kinds -- from callers the pass does not
                // see. While every caller is seen, what they pass is the
                // type (#declared): widening regardless made every wrapper
                // (markdownify's process_element forwarding `node` to
                // process_tag) untyped all the way down.
                if (isinstanceTested(f.def.body, n) || wideAnnotation(a)
                        || onlyForwarded(f.def.body, n) && (at == null || at.k != Ty.K.INST)) {
                    d[k] = WIDE;
                } else if (a != null) {
                    d[k] = at;
                }
            }
            declaredCache.put(f, d);
        }
        Ty r = i < d.length ? d[i] : null;
        if (r == WIDE) { //NOPMD CompareObjectsWithEquals - identity is meant
            boolean[] u = p.unseenArgs.get(f);
            // Only a call that passes a value of no known type is evidence
            // of the other kinds: a caller the pass does not see enters
            // through the guard, which hands what does not fit to the
            // generic version.
            return u != null && i < u.length && u[i] ? Ty.OBJECT : null;
        }
        return r;
    }

    /// [#declared]'s mark for a parameter the source says takes several
    /// kinds: OBJECT once a call passes it a value of no known type.
    private static final Ty WIDE = Ty.obj("<several kinds>");

    /// The type a parameter no visible call fills starts from: OBJECT when
    /// the source says it takes several kinds ([#declared]), else its
    /// annotation, else what its uses suggest ([#guess]).
    private Ty seed(Fn f, int i, Ty pin) {
        Ty d = declared(f, i);
        if (d != null && d.wide()) {
            return Ty.UNSEEN;
        }
        return pin != null ? pin : guess(f.def, f.names.get(i));
    }

    private final Map<Fn, Ty[]> declaredCache = new IdentityHashMap<Fn, Ty[]>();

    /// Whether `a` names a type of several kinds: `object`, `Any`, or a
    /// union of two or more members other than None.
    static boolean wideAnnotation(Expr a) {
        if (a == null) {
            return false;
        }
        if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
            String t = ((Constant) a).text.trim();
            return "object".equals(t) || "Any".equals(t) || t.startsWith("Union[") && t.indexOf(',') > 0
                    || t.replace("| None", "").indexOf('|') >= 0;
        }
        if (isName(a, "object") || isName(a, "Any")
                || a instanceof Attribute && "Any".equals(((Attribute) a).attr)) {
            return true;
        }
        if (a instanceof Subscript && (isName(((Subscript) a).value, "Union")
                || ((Subscript) a).value instanceof Attribute && "Union".equals(((Attribute) ((Subscript) a).value).attr))) {
            Expr ix = ((Subscript) a).index;
            if (!(ix instanceof TupleExpr)) {
                return false;
            }
            int members = 0;
            for (Expr e : ((TupleExpr) ix).elts) {
                members += isNone(e) ? 0 : 1;
            }
            return members > 1;
        }
        if (a instanceof BinOp && "|".equals(((BinOp) a).op)) {
            return !isNone(((BinOp) a).left) && !isNone(((BinOp) a).right);
        }
        return false;
    }

    /// Whether every use of `n` in `body` hands it on as an argument of a
    /// call, and nothing stores it: `re.sub(pattern, ...)` only passes
    /// `pattern` to `_compile`, which takes a str or a compiled pattern. A
    /// type for such a parameter buys the body nothing and its guard turns
    /// away every caller passing what the callee also takes.
    static boolean onlyForwarded(List<Stmt> body, String n) {
        int[] c = {0, 0};
        for (Stmt st : body) {
            forwardIn(st, n, c, false);
        }
        return c[0] > 0 && c[1] == 0;
    }

    /// `c`: uses handed on, other uses (stores included).
    private static void forwardIn(Object o, String n, int[] c, boolean asArg) {
        if (o instanceof Name && ((Name) o).id.equals(n)) {
            // A store target is reached with asArg false: it counts as a
            // use that is not handed on.
            c[asArg ? 0 : 1]++;
            return;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            // A closure over it: a use this does not follow.
            Infer.children(o, x -> forwardIn(x, n, c, false));
            return;
        }
        if (o instanceof Call) {
            Call cl = (Call) o;
            forwardIn(cl.func, n, c, false);
            for (Expr a : cl.args) {
                forwardIn(a, n, c, true);
            }
            for (Keyword k : cl.keywords) {
                forwardIn(k.value, n, c, true);
            }
            return;
        }
        Infer.children(o, x -> forwardIn(x, n, c, false));
    }

    /// Whether `body` (not counting nested functions and classes) calls
    /// `isinstance(n, ...)` or `type(n)`.
    static boolean isinstanceTested(List<Stmt> body, String n) {
        boolean[] r = {false};
        for (Stmt st : body) {
            isinstanceIn(st, n, r);
        }
        return r[0];
    }

    private static void isinstanceIn(Object o, String n, boolean[] r) {
        if (r[0] || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && !((Call) o).args.isEmpty()
                && isName(((Call) o).args.get(0), n)) {
            String fn = ((Name) ((Call) o).func).id;
            if ("isinstance".equals(fn) || "type".equals(fn) && ((Call) o).args.size() == 1) {
                r[0] = true;
                return;
            }
        }
        Infer.children(o, x -> isinstanceIn(x, n, r));
    }

    // ---- reflection-free traversal ---------------------------------------

    interface Visit {
        void on(Object child);
    }

    /// Calls `v` for every AST child (node or list element) of `o`.
    /// An argument of no known type reached parameter `i` of `f` through
    /// its door: no evidence for the typed version, which the guard keeps
    /// it out of. What does not fit runs the generic version, which stores
    /// it into fields and lists all the same, so that is walked with it
    /// (#genericView), as for a generic body's call that misses.
    private void objectArg(Fn f, int i) {
        if (p.objectArgs.add(f)) {
            p.changed = true;
        }
        Ty[] m = p.missed.get(f.def);
        if (m == null) {
            m = new Ty[f.params.length];
            java.util.Arrays.fill(m, Ty.BOT);
            p.missed.put(f.def, m);
            p.changed = true;
        }
        if (i < m.length && m[i].k != Ty.K.OBJECT) {
            m[i] = Ty.OBJECT;
            p.changed = true;
        }
    }

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

    /// The build errors of this module: what a closed world cannot
    /// compile. An attribute named by a string the build cannot enumerate
    /// (a computed getattr/setattr/hasattr/delattr), and a class whose
    /// instances cannot be given a layout. A value the analysis holds as
    /// `Object` is not an error: `Object` is a type, the right one where a
    /// value may be anything, and the generated code handles it as such.
    List<String> strictErrors() {
        List<String> out = new ArrayList<String>();
        out.addAll(reflective);
        refusals(body, out);
        for (Map.Entry<ClassDef, String> r : layouts.refused.entrySet()) {
            if (protocol(r.getKey())) {
                // A typing protocol: instantiating one raises, so no
                // instance of it exists to lay out.
                continue;
            }
            String why = r.getValue();
            String home = null;
            for (Expr base : r.getKey().bases) {
                if (home == null && why.equals("base " + Layouts.text(base) + " has no layout")) {
                    home = noHomeWhy(base);
                }
            }
            why = home == null ? why : why + " (" + home + ")";
            out.add(moduleName + ":" + r.getKey().line + " class " + r.getKey().name + " has no layout: " + why);
        }
        return out;
    }

    /// The refusals a lowering marked on the tree (Node#refusal).
    private void refusals(Object o, List<String> out) {
        if (o instanceof Node && ((Node) o).refusal != null) {
            out.add(moduleName + ":" + ((Node) o).line + " " + ((Node) o).refusal);
        }
        children(o, x -> refusals(x, out));
    }

    /// The builtins that take any object and only hash, compare, test or
    /// print it: a value that reaches nothing else is Java's `Object`
    /// passed to `equals`/`toString`, which costs nothing to leave open.
    private static final Set<String> SINKS = new HashSet<String>(java.util.Arrays.asList(
            "str", "repr", "print", "hash", "id", "isinstance", "type", "bool", "callable", "format", "ascii"));

    /// Whether every read of `n` in `body` is a generic sink: an argument
    /// of one of [#SINKS], an operand of `==`, `!=`, `is` or `is not`, an
    /// f-string field, the right operand of `%` formatting, or a truth
    /// test -- or the receiver of a method of a closed name, called
    /// through its slot. Strict mode allows such a value to stay untyped.
    boolean sinkOnly(List<Stmt> body, String n) {
        boolean[] bad = new boolean[1];
        for (Stmt s : body) {
            sinkUses(s, n, bad);
        }
        return !bad[0];
    }

    private void sinkUses(Object o, String n, boolean[] bad) {
        if (bad[0] || o == null) {
            return;
        }
        if (o instanceof Name) {
            bad[0] |= ((Name) o).id.equals(n);
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (!(t instanceof Name)) {
                    sinkUses(t, n, bad);
                }
            }
            sinkUses(((Assign) o).value, n, bad);
            return;
        }
        if (o instanceof For) {
            For f = (For) o;
            if (!(f.target instanceof Name)) {
                sinkUses(f.target, n, bad);
            }
            sinkUses(f.iter, n, bad);
            sinkUses(f.body, n, bad);
            sinkUses(f.orelse, n, bad);
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && ((Attribute) ((Call) o).func).value instanceof Name
                && ((Name) ((Attribute) ((Call) o).func).value).id.equals(n)
                && closedName(((Attribute) ((Call) o).func).attr)) {
            // A method of a closed name called on it: every object has
            // that slot, the program's methods implement it and the default
            // raises AttributeError (see #closedName). The value is the
            // slot interface's, used through it; what it is handed is
            // still held to a type.
            for (Expr a : ((Call) o).args) {
                sinkUses(a, n, bad);
            }
            for (Keyword k : ((Call) o).keywords) {
                sinkUses(k.value, n, bad);
            }
            return;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name && SINKS.contains(((Name) ((Call) o).func).id)) {
            for (Expr a : ((Call) o).args) {
                sinkOperand(a, n, bad);
            }
            for (Keyword k : ((Call) o).keywords) {
                sinkUses(k.value, n, bad);
            }
            return;
        }
        if (o instanceof Compare) {
            Compare c = (Compare) o;
            boolean eq = true;
            for (String op : c.ops) {
                eq &= "==".equals(op) || "!=".equals(op) || "is".equals(op) || "is not".equals(op);
            }
            if (eq) {
                sinkOperand(c.left, n, bad);
                for (Expr x : c.comparators) {
                    sinkOperand(x, n, bad);
                }
                return;
            }
        }
        if (o instanceof FormattedValue) {
            sinkOperand(((FormattedValue) o).value, n, bad);
            sinkUses(((FormattedValue) o).spec, n, bad);
            return;
        }
        if (o instanceof BinOp && "%".equals(((BinOp) o).op) && ((BinOp) o).left instanceof Constant) {
            sinkOperand(((BinOp) o).right, n, bad);
            return;
        }
        if (o instanceof If) {
            sinkOperand(((If) o).test, n, bad);
            sinkUses(((If) o).body, n, bad);
            sinkUses(((If) o).orelse, n, bad);
            return;
        }
        if (o instanceof While) {
            sinkOperand(((While) o).test, n, bad);
            sinkUses(((While) o).body, n, bad);
            sinkUses(((While) o).orelse, n, bad);
            return;
        }
        if (o instanceof IfExp) {
            sinkOperand(((IfExp) o).test, n, bad);
            sinkUses(((IfExp) o).body, n, bad);
            sinkUses(((IfExp) o).orelse, n, bad);
            return;
        }
        if (o instanceof UnaryOp && "not".equals(((UnaryOp) o).op)) {
            sinkOperand(((UnaryOp) o).operand, n, bad);
            return;
        }
        Infer.children(o, x -> sinkUses(x, n, bad));
    }

    /// `e` where its value only reaches a sink: `n` itself, or a tuple of
    /// such operands, is fine there.
    private void sinkOperand(Expr e, String n, boolean[] bad) {
        if (e instanceof Name && ((Name) e).id.equals(n)) {
            return;
        }
        if (e instanceof TupleExpr) {
            for (Expr x : ((TupleExpr) e).elts) {
                sinkOperand(x, n, bad);
            }
            return;
        }
        sinkUses(e, n, bad);
    }

    /// Strict mode: `what(obj, name)` is allowed only when `name` is one
    /// string known at build time, so the access compiles to a named field.
    /// Several possible names would only become a switch over names at run
    /// time, which is reflection again, so the build refuses it; the fix is
    /// code that names the attribute, or an `override=` of the function.
    private void reflection(String what, Expr name, Fn fn, Scope sc) {
        java.util.Collection<String> one = constStrings(name, fn, sc);
        if (one != null && one.size() == 1 || foldedString(name, fn, sc, 0) != null) {
            return;
        }
        String in = fn == null ? moduleName : moduleName + "." + (fn.cls == null ? "" : fn.cls.def.name + ".")
                + fn.def.name;
        reflective.add(moduleName + ":" + line + " " + what + " with a computed name is reflection; write"
                + " override = " + in + " in " + Directives.FILE + " and its compliant body in "
                + Directives.OVERRIDES + "/" + in + ".py");
    }

    /// Strict mode: every module function and method of `stmts` with no
    /// typed body (no Fn), and why -- a body the pass does not type at all
    /// is untyped through and through, and would otherwise not be counted.
    private void untypedBodies(List<Stmt> stmts, ClassDef owner, List<String> out) {
        for (Stmt s : stmts) {
            if (s instanceof ClassDef) {
                untypedBodies(((ClassDef) s).body, (ClassDef) s, out);
            } else if (s instanceof FunctionDef && fns.get(s) == null) {
                FunctionDef d = (FunctionDef) s;
                Arguments a = d.args;
                String why = !d.decorators.isEmpty() ? "it is decorated"
                        : d.isAsync ? "it is async"
                        : a.vararg != null ? "it takes *" + a.vararg.name
                        : a.kwarg != null ? "it takes **" + a.kwarg.name
                        : !a.kwonly.isEmpty() ? "it takes keyword-only parameters"
                        : owner != null && layouts.all().get(owner) == null ? "its class has no layout"
                        : "the layout leaves it out";
                out.add(moduleName + ":" + d.line + " " + (owner == null ? "" : owner.name + ".") + d.name
                        + " has no typed body: " + why);
            }
        }
    }

    private static boolean protocol(ClassDef c) {
        for (Expr x : c.bases) {
            Expr e = x instanceof Subscript ? ((Subscript) x).value : x;
            if (e instanceof Name && "Protocol".equals(((Name) e).id)
                    || e instanceof Attribute && "Protocol".equals(((Attribute) e).attr)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Expr e) {
        if (e instanceof Call) {
            return "call of " + describe(((Call) e).func);
        }
        if (e instanceof Attribute) {
            Expr v = ((Attribute) e).value;
            return (v instanceof Name ? ((Name) v).id : v instanceof Call ? describe(v) + " " : "...") + "."
                    + ((Attribute) e).attr;
        }
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        return e.getClass().getSimpleName();
    }

    String dump() {
        StringBuilder b = new StringBuilder();
        for (Fn f : fns.values()) {
            b.append(f.cls == null ? "" : f.cls.def.name + ".").append(f.def.name).append('(');
            for (int i = 0; i < f.params.length; i++) {
                b.append(i == 0 ? "" : ", ").append(f.names.get(i)).append(": ").append(f.params[i]);
            }
            b.append(") -> ").append(f.ret).append(f.escapes ? " [escapes]" : "").append(f.yields.k == Ty.K.BOT ? "" : " yields " + f.yields).append("\n    ").append(f.locals)
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
