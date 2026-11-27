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

import java.util.ArrayList;
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
/// - a module that hands its own namespace out (`globals()`, `vars()`,
///   `locals()` passed, stored or written through) has no stable globals.
///   Only reading it -- `globals()[name]`, `.get`, `in`, iterating it --
///   keeps them: such a read can reach any of its names, but rebinds none.
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
        /// Whether the module hands out its own namespace as a dict, through
        /// which any global may be bound again.
        boolean namespace;
        /// Whether the module reads its own namespace, which can reach any of
        /// its names but binds none of them (see [#readOnlyNamespace]).
        boolean namespaceRead;
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
        /// Every attribute name the whole program stores or deletes by
        /// name, on any receiver (shared by every module's view).
        Set<String> storedAttrs = new HashSet<String>();
        /// Every class statement of a module with a computed attribute
        /// store (`setattr(o, name, v)` with `name` not a literal): such a
        /// site may rebind any of those classes' attributes unseen (shared
        /// by every module's view, see [#computedStoreReaches]).
        Set<ClassDef> computedStoreClasses = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ClassDef, Boolean>());
        /// The last name of every class base the whole program writes
        /// (`Base`, `mod.Base`), plus "*" once a base is any other
        /// expression or a class may be made by a call (`type`).
        Set<String> baseNames = new HashSet<String>();

        /// Whether some class of the program may derive from a class named
        /// `name`: a statement names it as a base, or a base is computed.
        boolean mayBeBase(String name, Set<FunctionDef> reached) {
            if (baseNames.contains(name) || baseNames.contains("*")) {
                return true;
            }
            for (FunctionDef f : dynamicBases) {
                if (reached.contains(f)) {
                    return true;
                }
            }
            return false;
        }

        /// The functions that make a class from bases the source does not
        /// spell out (shared by every module's view).
        Set<FunctionDef> dynamicBases = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<FunctionDef, Boolean>());

        /// Every name the whole program uses outside the body of the class
        /// it names (shared by every module's view); null when that does not
        /// bound which classes have instances (see [#namedOutside]).
        Set<String> named;
        /// Whether the module reaches classes without naming them, through
        /// `C.__subclasses__()` it does more with than test against.
        boolean subclassReads;
        /// Whether any module of the program imports Java classes, whose
        /// objects answer methods of any name (shared by every module's view
        /// once the program is read).
        boolean javaInterop;
        /// The attribute names a Java object of the program answers
        /// ([JavaInteropGenerator#generate]), shared by every module; null
        /// when not known, and then every name may be one.
        Set<String> javaNames;

        /// Whether a Java object of the program may answer attribute `n`.
        boolean javaAnswers(String n) {
            return javaInterop && (javaNames == null || javaNames.contains(n));
        }
        /// The `__subclasses__` reads whose classes only reach
        /// `issubclass`/`isinstance` (see [#checkedOnly]).
        Set<Object> checkedSubclassReads = new HashSet<Object>();
        /// The names this module imports, which may hold a module.
        final Set<String> imported = new HashSet<String>();
        /// The root names of the receivers this module reads a computed
        /// attribute of: on a module, that may be any of its classes.
        final Set<String> computedRoots = new HashSet<String>();
        /// How deep in class bodies the scan is; a class nested in a class
        /// is reached through the outer one, `getattr(Outer, n)` included,
        /// so it counts as named.
        int classDepth;

        /// Whether class `name` may have instances or be handed out as a
        /// value: something names it outside its own body. An instance is
        /// made by calling the class, which needs the class; `type(x)` and
        /// `__class__` only give back the class of an instance that exists.
        /// A class named nowhere but in its own body is dead, whatever reads
        /// class values. Unknown (true) when the program is not seen whole,
        /// or reads a name it computes or a class's subclasses.
        boolean namedOutside(String name) {
            return named == null || named.contains(name);
        }

        /// Every attribute name the whole program names as a literal to
        /// setattr, `__setattr__` or a mock patcher (shared by every
        /// module's view).
        Set<String> setattrAttrs = new HashSet<String>();

        /// Whether a method `name` may be replaced in a way the type pass
        /// does not see as a store: through setattr or a patcher by a
        /// literal name, or deleted. A plain `x.name = v` is not counted
        /// here: the pass sees it, and decides which classes it can reach
        /// ([Infer.Program#patchedFound]). Counting it by name took the
        /// typed `__init__` from every class of a program as soon as one
        /// decorator anywhere did `cls.__init__ = ...`.
        boolean methodPatched(String name) {
            return setattrAttrs.contains(name) || deleted.contains(name);
        }

        /// Whether any module stores or deletes attribute `name` by name.
        boolean attrStored(String name) {
            return storedAttrs.contains(name);
        }

        /// Whether a computed attribute store of the program may reach the
        /// class `c` declares: `c` is declared in a module holding one.
        boolean computedStoreReaches(ClassDef c) {
            return computedStoreClasses.contains(c);
        }
        final List<String> stars = new ArrayList<String>();
        /// The `globals`/`vars`/`locals` names of this module's calls that
        /// only read the namespace, or (`vars(o)`) are no namespace at all.
        Set<Object> nsReads = new HashSet<Object>();

        /// Whether the top-level function or class `name` can be reached
        /// from outside the module by its name.
        boolean reachable(String name) {
            return starImported || namespace || namespaceRead || mentioned.contains(name);
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
        types.closed = true;
    }

    /// The facts of `module`; of a module the program does not have, none.
    /// Records the attribute names Java objects of the program answer.
    public void javaAnswers(Set<String> names) {
        for (Module m : modules.values()) {
            m.javaNames = names;
        }
    }

    public Module module(String module) {
        Module m = modules.get(module);
        return m == null ? new Module() : m;
    }

    /// The types of the whole program: every module's pass over one heap.
    Infer.Program types = new Infer.Program();

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
        List<String> out = new ArrayList<String>();
        for (Infer.Problem q : types.problems.values()) {
            if (q.store) {
                out.add(q.module + ":" + q.line + ": " + (q.function == null ? "the module" : q.function) + " "
                        + q.what);
            }
        }
        java.util.Collections.sort(out);
        return out;
    }

    private String main;

    /// Runs only what module `main` reaches: a module nothing imports from
    /// there is left out of the build (see `Infer.Program.live`). Without
    /// a call, or with the runtime's names unknown, every module is kept.
    public void main(String main) {
        this.main = main;
    }

    private Set<String> libraryRoots;

    /// The application's own modules, the roots of a build with no main
    /// module (library mode). Every one of them is an entry point, but a
    /// module bundled with the runtime or taken from PyPI runs only when one
    /// of them imports it, as in a build with a main: taking every module of
    /// the program as live judged, and emitted, classes of the bundled
    /// modules that nothing uses.
    public void libraryRoots(Set<String> modules) {
        this.libraryRoots = modules;
    }

    /// Whether module `name` runs, and so is emitted.
    public boolean isLive(String name) {
        Infer m = types.modules.get(name);
        return m == null || types.isLive(m);
    }

    public void infer() {
        boolean library = main == null && libraryRoots != null && !libraryRoots.isEmpty();
        if ((library || main != null && types.modules.containsKey(main)) && types.runtimeNames != null) {
            types.allLive = false;
            if (library) {
                for (String n : libraryRoots) {
                    if (types.modules.containsKey(n)) {
                        types.markLive(n);
                    }
                }
            } else {
                types.markLive(main);
            }
            // What the runtime imports by name (Py.importModule).
            for (String n : new String[] {"builtins", "copyreg", "io"}) {
                types.markLive(n);
            }
            for (String n : types.modules.keySet().toArray(new String[0])) {
                if (types.runtimeNames.contains(n)) {
                    types.markLive(n);
                }
            }
            for (String k : types.keep) {
                for (int dot = k.lastIndexOf('.'); dot > 0; dot = k.lastIndexOf('.', dot - 1)) {
                    if (types.modules.containsKey(k.substring(0, dot))) {
                        types.markLive(k.substring(0, dot));
                        break;
                    }
                }
                if (types.modules.containsKey(k)) {
                    types.markLive(k);
                }
            }
        }
        types.run();
        // A method a store may replace on some instance or class is looked
        // up per call. Which classes a store reaches is known only once the
        // pass is done, so the pass runs again with those methods taken
        // out until it finds no new one. Taking a method out only loosens
        // the program, so the set only grows and this ends.
        while (types.morePatched()) {
            types = types.fresh();
            types.run();
        }
        {
            List<String> errors = new ArrayList<String>();
            for (Infer m : types.modules.values()) {
                errors.addAll(m.strictErrors());
            }
            if (!errors.isEmpty()) {
                StringBuilder b = new StringBuilder("cn1.python.strict: ").append(errors.size())
                        .append(" construct(s) a closed world cannot compile");
                for (String e : errors) {
                    b.append("\n  ").append(e);
                }
                throw new IllegalStateException(b.toString());
            }
        }
    }

    /// Reads every module of the program (dotted name to body).
    public static WholeProgram of(Map<String, List<Stmt>> bodies) {
        WholeProgram p = new WholeProgram();
        Map<String, Set<String>> stores = new HashMap<String, Set<String>>();
        Map<String, Integer> count = new HashMap<String, Integer>();
        Set<String> deleted = new HashSet<String>();
        Set<String> storedAttrs = new HashSet<String>();
        Set<String> setattrAttrs = new HashSet<String>();
        Set<String> named = new HashSet<String>();
        Set<String> baseNames = new HashSet<String>();
        Set<FunctionDef> dynamicBases = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<FunctionDef, Boolean>());
        Set<ClassDef> computedStoreClasses = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<ClassDef, Boolean>());
        for (List<Stmt> b : bodies.values()) {
            attrStores(b, storedAttrs);
            boolean[] computed = new boolean[1];
            List<ClassDef> classes = new ArrayList<ClassDef>();
            computedStores(b, computed, classes);
            if (computed[0]) {
                computedStoreClasses.addAll(classes);
            }
            baseNames(b, null, baseNames, dynamicBases);
        }
        for (Map.Entry<String, List<Stmt>> e : bodies.entrySet()) {
            Module m = new Module();
            m.deleted = deleted;
            m.storedAttrs = storedAttrs;
            m.computedStoreClasses = computedStoreClasses;
            m.baseNames = baseNames;
            m.dynamicBases = dynamicBases;
            m.setattrAttrs = setattrAttrs;
            m.named = named;
            Set<String> all = new HashSet<String>();
            m.nsReads = readOnlyNamespace(e.getValue());
            for (Stmt s : e.getValue()) {
                checkedOnly(s, m.checkedSubclassReads);
            }
            scan(e.getValue(), null, null, m, all);
            p.modules.put(e.getKey(), m);
            stores.put(e.getKey(), all);
            for (String n : all) {
                Integer c = count.get(n);
                count.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
        }
        boolean unnamed = false;
        boolean java = false;
        for (Module m : p.modules.values()) {
            java |= m.javaInterop;
        }
        for (Module m : p.modules.values()) {
            m.javaInterop = java;
        }
        for (Module m : p.modules.values()) {
            unnamed |= m.subclassReads || m.computedRoots.contains("");
            for (String r : m.computedRoots) {
                unnamed |= m.imported.contains(r);
            }
        }
        for (Module m : p.modules.values()) {
            // A name a module computes may be any class's, and
            // `__subclasses__` hands out classes by no name at all.
            m.named = unnamed ? null : named;
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
    /// The attribute names stored or deleted anywhere under `o`: every
    /// target position, and the literal names of `setattr`, `delattr`,
    /// `__setattr__` and `__delattr__` calls. Computed names are Infer's.
    static void attrStores(Object o, Set<String> out) {
        if (o instanceof FunctionDef && ((FunctionDef) o).dataclassInit) {
            // The constructor DataclassInit wrote stores each field once, by
            // its literal name: not a store any other code makes.
            return;
        }
        if (o instanceof Assign && !Layouts.trailingStore(o)) {
            for (Expr t : ((Assign) o).targets) {
                attrTarget(t, out);
            }
        } else if (o instanceof AugAssign) {
            attrTarget(((AugAssign) o).target, out);
        } else if (o instanceof AnnAssign) {
            attrTarget(((AnnAssign) o).target, out);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                attrTarget(t, out);
            }
        } else if (o instanceof For) {
            attrTarget(((For) o).target, out);
        } else if (o instanceof WithItem) {
            attrTarget(((WithItem) o).vars, out);
        } else if (o instanceof Comprehension) {
            attrTarget(((Comprehension) o).target, out);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            String f = c.func instanceof Name ? ((Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            if ("setattr".equals(f) || "delattr".equals(f) || "__setattr__".equals(f) || "__delattr__".equals(f)) {
                for (Expr a : c.args) {
                    if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
                        out.add(((Constant) a).text);
                    }
                }
            }
        }
        Infer.children(o, x -> attrStores(x, out));
    }

    /// Finds every class statement under `o`, and sets `computed[0]` at a
    /// store or delete whose attribute name is not a literal:
    /// `setattr(o, n, v)`, `type.__setattr__(o, n, v)`, `o.__setattr__(n, v)`.
    private static void computedStores(Object o, boolean[] computed, List<ClassDef> classes) {
        if (o instanceof ClassDef) {
            classes.add((ClassDef) o);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            String f = c.func instanceof Name ? ((Name) c.func).id
                    : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
            if ("setattr".equals(f) || "delattr".equals(f) || "__setattr__".equals(f) || "__delattr__".equals(f)) {
                boolean del = f.indexOf("del") >= 0;
                int at = c.args.size() == (del ? 1 : 2) && c.func instanceof Attribute ? 0 : 1;
                Expr n = at < c.args.size() ? c.args.get(at) : null;
                computed[0] |= !(n instanceof Constant && ((Constant) n).kind == Constant.Kind.STR);
            }
        }
        Infer.children(o, x -> computedStores(x, computed, classes));
    }

    /// Collects [Module#baseNames]: every base a class statement names, and
    /// for a call that may make a class from bases it is handed, those
    /// bases. A bases argument the source does not spell out is the name
    /// "*" at module level and the enclosing function in `dyn` otherwise,
    /// which [Module#mayBeBase] counts once that function runs.
    private static void baseNames(Object o, FunctionDef in, Set<String> out, Set<FunctionDef> dyn) {
        if (o instanceof FunctionDef) {
            for (Stmt s : ((FunctionDef) o).body) {
                baseNames(s, (FunctionDef) o, out, dyn);
            }
            return;
        }
        if (o instanceof ClassDef) {
            for (Expr b : ((ClassDef) o).bases) {
                baseExpr(b, in, out, dyn);
            }
        } else if (o instanceof Call) {
            Call c = (Call) o;
            Expr bases = classMakerBases(c);
            if (bases != null) {
                boolean forwarded = in != null && "__new__".equals(in.name) && bases instanceof Name
                        && in.args.positional.size() > 2 && in.args.positional.get(2).name.equals(((Name) bases).id);
                // A metaclass's `__new__` handing on the bases it was given
                // by a class statement, which names them already.
                if (!forwarded) {
                    baseExpr(bases, in, out, dyn);
                }
            }
            for (Keyword k : c.keywords) {
                if ("bases".equals(k.arg)) {
                    baseExpr(k.value, in, out, dyn);
                }
            }
            if (c.args.size() >= 3) {
                // `Meta("X", (Base,), {})`: a metaclass called directly.
                for (Expr a : c.args) {
                    if (a instanceof TupleExpr) {
                        for (Expr e : ((TupleExpr) a).elts) {
                            if (e instanceof Name) {
                                out.add(((Name) e).id);
                            } else if (e instanceof Attribute) {
                                out.add(((Attribute) e).attr);
                            }
                        }
                    }
                }
            }
        }
        Infer.children(o, x -> baseNames(x, in, out, dyn));
    }

    private static void baseExpr(Expr b, FunctionDef in, Set<String> out, Set<FunctionDef> dyn) {
        if (b instanceof Name) {
            out.add(((Name) b).id);
        } else if (b instanceof Attribute) {
            out.add(((Attribute) b).attr);
        } else if (b instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) b).elts) {
                baseExpr(e, in, out, dyn);
            }
        } else if (b instanceof IfExp) {
            baseExpr(((IfExp) b).body, in, out, dyn);
            baseExpr(((IfExp) b).orelse, in, out, dyn);
        } else if (b instanceof Subscript) {
            // `Generic[T]`, `List[str]`: the subscripted class.
            baseExpr(((Subscript) b).value, in, out, dyn);
        } else if (b instanceof Call) {
            // `namedtuple(...)`, `with_metaclass(Meta, Base)`: a class made
            // from what the call is handed, if from anything.
            for (Expr a : ((Call) b).args) {
                if (!(a instanceof Constant)) {
                    baseExpr(a, in, out, dyn);
                }
            }
            for (Keyword k : ((Call) b).keywords) {
                if (!(k.value instanceof Constant)) {
                    baseExpr(k.value, in, out, dyn);
                }
            }
        } else if (b instanceof Constant || b instanceof ListExpr) {
            // A string or a field list (`namedtuple("P", ["x", "y"])`).
            return;
        } else if (in == null) {
            out.add("*");
        } else {
            dyn.add(in);
        }
    }

    /// The bases argument of `type(name, bases, ns)`, `types.new_class(name,
    /// bases)` or `X.__new__(mcs, name, bases, ns)`, or null.
    private static Expr classMakerBases(Call c) {
        String f = c.func instanceof Name ? ((Name) c.func).id
                : c.func instanceof Attribute ? ((Attribute) c.func).attr : null;
        if ("type".equals(f) && c.args.size() == 3 || "new_class".equals(f) && c.args.size() >= 2) {
            return c.args.get(1);
        }
        if ("__new__".equals(f) && c.args.size() >= 4) {
            return c.args.get(2);
        }
        return null;
    }

    private static void attrTarget(Expr t, Set<String> out) {
        if (t instanceof Attribute) {
            out.add(((Attribute) t).attr);
        } else if (t instanceof TupleExpr) {
            for (Expr x : ((TupleExpr) t).elts) {
                attrTarget(x, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr x : ((ListExpr) t).elts) {
                attrTarget(x, out);
            }
        } else if (t instanceof Starred) {
            attrTarget(((Starred) t).value, out);
        }
    }

    /// The names `globals`, `vars` and `locals` in `body` whose call only
    /// reads the module's namespace: `globals()[k]` (not stored or deleted
    /// through), `globals().get(k)` and the other reading methods,
    /// `k in globals()`, iterating it, or copying it (`dict(globals())`). A
    /// `vars(o)` with an argument is the instance's dict, not the module's
    /// (`dictViews` covers it). Matched by identity. bs4's `element` looks a
    /// deprecated name up with `globals()[...]` in its `__getattr__`; taking
    /// that as a namespace handed out made every one of its classes unstable,
    /// so no other module could name `Tag` -- not even in an annotation.
    static Set<Object> readOnlyNamespace(List<Stmt> body) {
        Set<Object> stores = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        storeTargets(body, stores);
        Set<Object> out = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
        nsReadsIn(body, stores, out);
        return out;
    }

    private static final Set<String> NS_READ_METHODS = new HashSet<String>(Arrays.asList(
            "get", "keys", "values", "items", "copy", "__contains__", "__getitem__"));
    private static final Set<String> NS_COPIES = new HashSet<String>(Arrays.asList(
            "len", "list", "sorted", "dict", "set", "tuple", "iter"));

    private static void storeTargets(Object o, Set<Object> stores) {
        if (o instanceof Assign && !Layouts.trailingStore(o)) {
            for (Expr t : ((Assign) o).targets) {
                targetsOf(t, stores);
            }
        } else if (o instanceof AugAssign) {
            targetsOf(((AugAssign) o).target, stores);
        } else if (o instanceof AnnAssign) {
            targetsOf(((AnnAssign) o).target, stores);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                targetsOf(t, stores);
            }
        } else if (o instanceof For) {
            targetsOf(((For) o).target, stores);
        } else if (o instanceof WithItem && ((WithItem) o).vars != null) {
            targetsOf(((WithItem) o).vars, stores);
        }
        Infer.children(o, x -> storeTargets(x, stores));
    }

    private static void targetsOf(Expr t, Set<Object> stores) {
        stores.add(t);
        if (t instanceof TupleExpr) {
            for (Expr x : ((TupleExpr) t).elts) {
                targetsOf(x, stores);
            }
        } else if (t instanceof ListExpr) {
            for (Expr x : ((ListExpr) t).elts) {
                targetsOf(x, stores);
            }
        } else if (t instanceof Starred) {
            targetsOf(((Starred) t).value, stores);
        }
    }

    /// The name of `e` when it is a call of `globals()`, `vars()` or
    /// `locals()` with no arguments; else null.
    private static Name nsCall(Expr e) {
        if (e instanceof Call && ((Call) e).func instanceof Name && ((Call) e).args.isEmpty()
                && ((Call) e).keywords.isEmpty() && NAMESPACE.contains(((Name) ((Call) e).func).id)) {
            return (Name) ((Call) e).func;
        }
        return null;
    }

    private static void nsReadsIn(Object o, Set<Object> stores, Set<Object> out) {
        Name n = null;
        if (o instanceof Subscript && !stores.contains(o)) {
            n = nsCall(((Subscript) o).value);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Attribute && NS_READ_METHODS.contains(((Attribute) c.func).attr)) {
                n = nsCall(((Attribute) c.func).value);
            } else if (c.func instanceof Name && NS_COPIES.contains(((Name) c.func).id) && c.args.size() == 1
                    && c.keywords.isEmpty()) {
                n = nsCall(c.args.get(0));
            } else if (c.func instanceof Name && "vars".equals(((Name) c.func).id) && !c.args.isEmpty()) {
                out.add(c.func);
            }
        } else if (o instanceof Compare) {
            Compare c = (Compare) o;
            for (int i = 0; i < c.ops.size(); i++) {
                String op = c.ops.get(i);
                if (("in".equals(op) || "not in".equals(op)) && nsCall(c.comparators.get(i)) != null) {
                    out.add(nsCall(c.comparators.get(i)));
                }
            }
        } else if (o instanceof For) {
            n = nsCall(((For) o).iter);
        }
        if (n != null) {
            out.add(n);
        }
        Infer.children(o, x -> nsReadsIn(x, stores, out));
    }

    /// Adds to `safe` the `__subclasses__` reads in the iterable of a loop
    /// whose variable is used only as an argument to `issubclass` or
    /// `isinstance`: the classes they hand out are tested, never called,
    /// kept or passed on (`abc`'s subclass check).
    private static void checkedOnly(Object o, Set<Object> safe) {
        if (o instanceof For && ((For) o).target instanceof Name) {
            For f = (For) o;
            String t = ((Name) f.target).id;
            int[] uses = new int[2];
            Infer.children(f, x -> {
                if (x != f.iter && x != f.target) {
                    loopUses(x, t, uses);
                }
            });
            if (uses[0] == uses[1]) {
                subclassReads(f.iter, safe);
            }
        }
        Infer.children(o, x -> checkedOnly(x, safe));
    }

    /// Counts in `uses[0]` every use of name `t` under `o`, and in
    /// `uses[1]` those that are a direct argument of `issubclass` or
    /// `isinstance`.
    private static void loopUses(Object o, String t, int[] uses) {
        if (o instanceof Name && t.equals(((Name) o).id)) {
            uses[0]++;
        }
        if (o instanceof Call && ((Call) o).func instanceof Name
                && ("issubclass".equals(((Name) ((Call) o).func).id)
                || "isinstance".equals(((Name) ((Call) o).func).id))) {
            for (Expr a : ((Call) o).args) {
                if (a instanceof Name && t.equals(((Name) a).id)) {
                    uses[1]++;
                }
            }
        }
        Infer.children(o, x -> loopUses(x, t, uses));
    }

    /// Adds every `__subclasses__` read under `o` to `safe`.
    private static void subclassReads(Object o, Set<Object> safe) {
        if (o instanceof Attribute && "__subclasses__".equals(((Attribute) o).attr)) {
            safe.add(o);
        }
        Infer.children(o, x -> subclassReads(x, safe));
    }

    /// `fs` is the namespace builtins (`NAMESPACE`) a function `o` is in
    /// binds as names of its own, null at the top level. The scopes are
    /// not built yet, so this counts the stores itself; a name a nested
    /// function binds shadows it there too, as Python resolves it.
    private static void scan(Object o, String self, Set<String> fs, Module m, Set<String> all) {
        if (o instanceof FunctionDef) {
            List<String> names = ((FunctionDef) o).args.names();
            String first = names.isEmpty() ? null : names.get(0);
            Set<String> shadow = new HashSet<String>(fs == null ? java.util.Collections.<String>emptySet() : fs);
            for (String n : NAMESPACE) {
                if (Infer.bindsLocally((FunctionDef) o, n)) {
                    shadow.add(n);
                }
            }
            Infer.children(o, x -> scan(x, first, shadow, m, all));
            return;
        }
        if (o instanceof ClassDef && m.named != null) {
            // Its own body naming the class (`super(K, self)`) makes no
            // instance of it.
            Set<String> outer = m.named;
            m.named = new HashSet<String>();
            m.classDepth++;
            Infer.children(o, x -> scan(x, null, fs, m, all));
            m.classDepth--;
            if (m.classDepth == 0) {
                m.named.remove(((ClassDef) o).name);
            }
            outer.addAll(m.named);
            m.named = outer;
            return;
        }
        if (o instanceof Lambda || o instanceof ClassDef) {
            Infer.children(o, x -> scan(x, null, fs, m, all));
            return;
        }
        // Only the builtin hands the namespace out: bs4's `BeautifulSoup`
        // keeps a frame's globals in a local named `globals`, which had made
        // every global of `bs4` unstable -- its imports unbound, and so its
        // `BeautifulSoup(Tag)` without a layout.
        if (o instanceof Name && NAMESPACE.contains(((Name) o).id)
                && (fs == null || !fs.contains(((Name) o).id))) {
            if (m.nsReads.contains(o)) {
                m.namespaceRead = true;
            } else {
                m.namespace = true;
            }
        }
        if (o instanceof Attribute) {
            m.mentions.add(((Attribute) o).attr);
            m.subclassReads |= "__subclasses__".equals(((Attribute) o).attr) && !m.checkedSubclassReads.contains(o);
            if (m.named != null) {
                m.named.add(((Attribute) o).attr);
            }
            m.classValues |= "__class__".equals(((Attribute) o).attr);
            m.dictViews |= "__dict__".equals(((Attribute) o).attr);
        } else if (o instanceof Name) {
            if (m.named != null) {
                m.named.add(((Name) o).id);
            }
            m.classValues |= "classmethod".equals(((Name) o).id);
            m.dictViews |= "vars".equals(((Name) o).id);
        } else if (o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            for (Alias a : f.names) {
                if ("*".equals(a.name)) {
                    m.stars.add(f.module == null ? "" : f.module);
                } else {
                    m.mentions.add(a.name);
                    m.imported.add(a.asname != null ? a.asname : a.name);
                    if (m.named != null) {
                        m.named.add(a.name);
                    }
                }
            }
        }
        if (o instanceof ImportFrom && ((ImportFrom) o).module != null
                && ("java".equals(((ImportFrom) o).module) || ((ImportFrom) o).module.startsWith("java."))) {
            m.javaInterop = true;
        }
        if (o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                m.javaInterop |= "java".equals(a.name) || a.name.startsWith("java.");
                m.imported.add(a.asname != null ? a.asname : a.name.split("\\.")[0]);
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
                if (!literal) {
                    Expr r = "__getattribute__".equals(f) && c.func instanceof Attribute
                            ? ((Attribute) c.func).value : c.args.get(0);
                    while (r instanceof Attribute) {
                        r = ((Attribute) r).value;
                    }
                    // `type(x)` and `super()` are classes of an instance
                    // that exists; any other receiver that is no plain name
                    // may be a module.
                    boolean classOf = r instanceof Call && ((Call) r).func instanceof Name
                            && ("type".equals(((Name) ((Call) r).func).id)
                            || "super".equals(((Name) ((Call) r).func).id));
                    if (!classOf) {
                        m.computedRoots.add(r instanceof Name ? ((Name) r).id : "");
                    }
                }
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
                            m.setattrAttrs.add(n);
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
        Infer.children(o, x -> scan(x, self, fs, m, all));
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
