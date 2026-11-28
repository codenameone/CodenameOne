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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Compiled layouts for the classes a module defines at its top level.
///
/// Such a class gets a Java subclass of `PyInstance` whose attributes are
/// fields, and whose plain methods are static Java methods reached through
/// a virtual `m_<name>` per method name and arity. Generated code then reads
/// `self.x` as an `instanceof` and a field load, and calls `t.run(p)` as an
/// `instanceof` and a virtual call, instead of going through an inline cache.
///
/// What is planned here is only ever a fast path: every access still falls
/// back to the generic one when the receiver is not of the expected Java
/// class, a field is unset, or the class was changed after it was built
/// (`PyType.patched`). A field is a name the class's own methods store on
/// their first parameter, and never a name any compiled class binds in its
/// body, since a class attribute -- a property in particular -- must keep
/// deciding what a store to that name does.
final class Layouts {
    /// The most parameters a function compiled typed may take. Java's own
    /// limit is 255 slots; this keeps generated signatures readable. bs4's
    /// Tag.__init__ takes 17, and a cap of 8 left every field it sets
    /// unanalysed -- the class every HTML converter walks.
    static final int MAX_TYPED_PARAMS = 24;

    /// Per program: for each class name, the names a subclass anywhere
    /// below it binds by a decorated definition or a call in its body
    /// (see [#subclassBindings]). Set by the transpiler before emitting.
    private static final ThreadLocal<Map<String, Set<String>>> SUBCLASS_BINDINGS =
            new ThreadLocal<Map<String, Set<String>>>();

    /// Records, for every class of `modules`, what its subclasses bind over
    /// it, so a field a subclass of another module turns into a property is
    /// planned as no field of the base ([#plan]).
    static void subclassBindings(java.util.Collection<?> modules) {
        final Map<String, Set<String>> own = new HashMap<String, Set<String>>();
        final Map<String, Set<String>> subs = new HashMap<String, Set<String>>();
        for (Object m : modules) {
            collectBindings(m, own, subs);
        }
        Map<String, Set<String>> all = new HashMap<String, Set<String>>();
        for (Map.Entry<String, Set<String>> sb : subs.entrySet()) {
            String b = sb.getKey();
            Set<String> r = new HashSet<String>();
            java.util.Deque<String> todo = new java.util.ArrayDeque<String>(sb.getValue());
            Set<String> seen = new HashSet<String>();
            while (!todo.isEmpty()) {
                String c = todo.pop();
                if (!seen.add(c)) {
                    continue;
                }
                if (own.containsKey(c)) {
                    r.addAll(own.get(c));
                }
                if (subs.containsKey(c)) {
                    todo.addAll(subs.get(c));
                }
            }
            if (!r.isEmpty()) {
                all.put(b, r);
            }
        }
        SUBCLASS_BINDINGS.set(all);
    }

    private static void collectBindings(Object o, final Map<String, Set<String>> own,
            final Map<String, Set<String>> subs) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            Set<String> n = new HashSet<String>();
            for (Stmt s : c.body) {
                if (s instanceof FunctionDef && !((FunctionDef) s).decorators.isEmpty()) {
                    n.add(((FunctionDef) s).name);
                } else if (s instanceof Assign && ((Assign) s).value instanceof Call) {
                    for (Expr t : ((Assign) s).targets) {
                        if (t instanceof Name) {
                            n.add(((Name) t).id);
                        }
                    }
                }
            }
            if (!n.isEmpty()) {
                Set<String> e = own.get(c.name);
                if (e == null) {
                    own.put(c.name, n);
                } else {
                    e.addAll(n);
                }
            }
            for (Expr b : c.bases) {
                String bn = b instanceof Name ? ((Name) b).id
                        : b instanceof Attribute ? ((Attribute) b).attr : null;
                if (bn != null && !bn.equals(c.name)) {
                    Set<String> l = subs.get(bn);
                    if (l == null) {
                        l = new HashSet<String>();
                        subs.put(bn, l);
                    }
                    l.add(c.name);
                }
            }
        }
        Infer.children(o, x -> collectBindings(x, own, subs));
    }

    static final class Cls {
        final ClassDef def;
        final String javaName;
        final String typeVar;
        /// `javaName` qualified by the module's Java class: how code in any
        /// module of the program names this class.
        String ref;
        /// The typed code of the module declaring the class, once its typed
        /// fields are decided: whether a field is held typed, and how.
        TypedEmit typed;
        final Cls parent;
        /// False for a class given no Java layout (see [Layouts#analysed]):
        /// the inference follows its instances, fields and methods like any
        /// other class's, and generated code holds them as plain objects.
        boolean compiled = true;
        /// The plan this class is part of: a parent of another module's
        /// plan is a compiled class another module declares.
        Layouts home;
        final List<String> fields = new ArrayList<String>();
        /// Every field, the inherited ones first: the index order the
        /// generated `fieldIndex` uses.
        final List<String> allFields = new ArrayList<String>();
        final Set<String> bodyNames;
        final Map<String, FunctionDef> methods = new LinkedHashMap<String, FunctionDef>();
        /// `methods`, plus the plain methods with too many parameters for a
        /// generic dispatch: those `Infer` and the typed code still see.
        final Map<String, FunctionDef> typable = new LinkedHashMap<String, FunctionDef>();

        /// Names the extra bases of this class bind, which the class
        /// reaches through its MRO after the compiled chain (see
        /// [#neutralMixin]), and so may not be fields of it.
        final Set<String> mixinNames = new HashSet<String>();
        /// The compiled classes this class names as bases, in order: the
        /// compiled parent (if any) and every compiled extra base. The C3
        /// MRO of a subclass is computed over them ([#c3]).
        final List<Cls> bases = new ArrayList<Cls>();
        /// The compiled classes of this class's MRO that are not in its
        /// compiled chain: their fields are copied into this layout, and
        /// their methods reached through the MRO ([#mergeExtras]).
        final List<Cls> mixedFrom = new ArrayList<Cls>();
        /// The names this class's MRO resolves in a merged class rather than
        /// in its compiled chain; added to [#bodyNames], and withdrawn from
        /// the typed dispatch of every class above it.
        final Set<String> mroNames = new HashSet<String>();
        /// Whether the class has a base besides its compiled parent: the
        /// generated code then asks the runtime to confirm the extra bases
        /// add nothing to an instance before giving the layout.
        boolean extraBases;
        /// The builtin type the instances extend (`str` for bs4's
        /// `NavigableString`), held in `PyInstance.base`; inherited from the
        /// compiled parent. Null for none.
        String builtin;

        /// Whether `enum.EnumType` makes the class: its body's definitions
        /// become members when it is made (runtime class `EnumMeta`), and
        /// calling it looks a member up rather than constructing one.
        boolean enumMeta;
        /// Whether this class is a metaclass ([Layouts#metaclassDef]): its
        /// instances are classes, never of a Java class of its own.
        boolean metaclass;
        /// Whether calling this class runs a metaclass `__call__`
        /// ([Layouts#LAYOUT_SAFE_HOOKS]), its own or inherited: typed code
        /// constructs it by calling the class, and the result is any object.
        boolean metaCall;
        /// Whether typed code must construct the class by calling it.
        boolean callConstructed() {
            return exception || metaCall;
        }
        /// Whether `builtin` is this class's own first base, which the
        /// generated code then has the runtime confirm
        /// (`PyType.builtinBased`).
        boolean builtinOwn;
        /// The builtin exception types among this class's own bases, by
        /// their canonical name (`IOError` is `OSError`), in base order. An
        /// exception instance is a plain `PyInstance` whose `args`,
        /// `__cause__` and the like live in its attribute table, so the
        /// layout extends the compiled parent (or none) like any class and
        /// the type object keeps every builtin base for `except` and
        /// `isinstance`; the generated code has the runtime confirm them
        /// (`PyType.exceptionBased`).
        final List<String> excBases = new ArrayList<String>();
        /// Whether the class derives, directly or through its compiled
        /// chain, from a builtin exception: BaseException's `__new__` then
        /// fills `args` from the constructor's arguments, so typed code
        /// constructs it by calling the class, never by `new` and `__init__`.
        boolean exception;

        /// Whether this class is, or extends, a frozen dataclass: the
        /// `__setattr__` the decorator adds decides every store on its
        /// instances, so generated code never writes their fields directly.
        boolean frozenStores() {
            for (Cls c = this; c != null; c = c.parent) {
                if (c.def.dataclassFrozen) {
                    return true;
                }
            }
            return false;
        }

        Cls(ClassDef def, String javaName, String typeVar, Cls parent) {
            this.def = def;
            this.javaName = javaName;
            this.typeVar = typeVar;
            this.parent = parent;
            this.bodyNames = bodyBindings(def);
        }
    }

    /// The names a class body binds. A bare annotation (`name_rules: List[Rule]`)
    /// binds nothing -- it only records the annotation -- so a name that only
    /// appears that way is left out, and stays free to be a field: bs4 annotates
    /// every instance attribute of `Tag` and `SoupStrainer` in the class body. A body
    /// with any statement whose bindings are not read here keeps the scope's set.
    static Set<String> bodyBindings(ClassDef def) {
        Set<String> bare = new HashSet<String>();
        Set<String> bound = new HashSet<String>();
        for (Stmt s : def.body) {
            if (s instanceof AnnAssign && ((AnnAssign) s).value == null) {
                if (((AnnAssign) s).target instanceof Name) {
                    bare.add(((Name) ((AnnAssign) s).target).id);
                }
            } else if (s instanceof AnnAssign) {
                names(((AnnAssign) s).target, bound);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    names(t, bound);
                }
            } else if (s instanceof AugAssign) {
                names(((AugAssign) s).target, bound);
            } else if (s instanceof FunctionDef) {
                bound.add(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                bound.add(((ClassDef) s).name);
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass)) {
                return def.scope.assigned;
            }
        }
        bare.removeAll(bound);
        if (bare.isEmpty()) {
            return def.scope.assigned;
        }
        Set<String> r = new HashSet<String>(def.scope.assigned);
        r.removeAll(bare);
        return r;
    }

    /// The names one statement of a class body binds, as [#bodyBindings]
    /// reads them; null for a statement it does not read, which may bind
    /// any name.
    static Set<String> bodyBindingsOf(Stmt s) {
        Set<String> bound = new HashSet<String>();
        if (s instanceof AnnAssign && ((AnnAssign) s).value != null || s instanceof AugAssign) {
            names(s instanceof AnnAssign ? ((AnnAssign) s).target : ((AugAssign) s).target, bound);
        } else if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                names(t, bound);
            }
        } else if (s instanceof FunctionDef) {
            bound.add(((FunctionDef) s).name);
        } else if (s instanceof ClassDef) {
            bound.add(((ClassDef) s).name);
        } else if (!(s instanceof ExprStmt) && !(s instanceof Pass) && !bareAnnotation(s)) {
            return null;
        }
        return bound;
    }

    private static boolean bareAnnotation(Stmt s) {
        return s instanceof AnnAssign && ((AnnAssign) s).value == null;
    }

    final Map<ClassDef, Cls> classes = new LinkedHashMap<ClassDef, Cls>();
    /// The names the module assigns at top level ([#plan]).
    private Set<String> moduleAssigned = java.util.Collections.<String>emptySet();
    /// Whether a global of this module holds a constant literal for the
    /// whole run (Infer#constantGlobal); null before whole-program planning.
    private java.util.function.Predicate<String> constantGlobal;
    /// Whether a global of this module is bound once and never rebound by
    /// any module (Infer#fixedGlobal); null before whole-program planning.
    private java.util.function.Predicate<String> fixedGlobal;

    /// Whether the module assigns `name` at top level (a builtin it shadows).
    boolean moduleBinds(String name) {
        return moduleAssigned.contains(name);
    }
    /// The classes of [#refused] that the inference still follows: a class
    /// the emitter cannot lay out is a class all the same, whose instances,
    /// fields and methods have types. Never emitted as Java classes.
    final Map<ClassDef, Cls> analysed = new LinkedHashMap<ClassDef, Cls>();

    /// Every class the inference follows: the compiled ones, then the
    /// analysed ones.
    Map<ClassDef, Cls> all() {
        if (all == null) {
            all = new LinkedHashMap<ClassDef, Cls>(classes);
            all.putAll(analysed);
        }
        return all;
    }

    private Map<ClassDef, Cls> all;
    /// The classes given no layout, with why: the strict mode's report.
    final Map<ClassDef, String> refused = new LinkedHashMap<ClassDef, String>();
    /// Field name -> the classes that declare it (not inherit it).
    final Map<String, List<Cls>> fieldOwners = new LinkedHashMap<String, List<Cls>>();
    /// `name/arity` of every compiled method, arity without the receiver.
    final Set<String> dispatch = new LinkedHashSet<String>();
    /// The classes that put each entry into [#dispatch].
    private final Map<String, Set<Cls>> dispatchBy = new HashMap<String, Set<Cls>>();
    final Map<FunctionDef, String> staticNames = new LinkedHashMap<FunctionDef, String>();
    /// The class each compiled method belongs to.
    final Map<FunctionDef, Cls> methodClass = new LinkedHashMap<FunctionDef, Cls>();
    /// The methods given a static entry only for build-time-bound calls.
    final Set<FunctionDef> directEntries = new HashSet<FunctionDef>();
    /// Every instance method a class body defines -> that class: the class
    /// its first parameter is, as `self`. Only a hint for the typed copy of
    /// that parameter, which is guarded by `instanceof`, so an unusual call
    /// (`K.m(other)`) is slower, never wrong.
    final Map<FunctionDef, Cls> receiverClass = new LinkedHashMap<FunctionDef, Cls>();

    /// Field name -> every class of the whole program declaring it, typed
    /// (`Cls.typed`) or not. The program is closed: a class compiled in
    /// one module is a class code in every other module may read the
    /// fields of directly (`ShadowTypes`). Filled once every module's
    /// typed fields are known, before any module is emitted.
    static Map<String, List<Cls>> program(List<Layouts> all) {
        Map<String, List<Cls>> r = new LinkedHashMap<String, List<Cls>>();
        for (Layouts l : all) {
            for (Cls k : l.classes.values()) {
                for (String f : k.fields) {
                    List<Cls> o = r.get(f);
                    if (o == null) {
                        o = new ArrayList<Cls>();
                        r.put(f, o);
                    }
                    o.add(k);
                }
            }
        }
        return r;
    }

    /// The top-level classes of the module that some other class of it
    /// names as a base after its first and that a compiled class may have
    /// as such ([#neutralMixin]). pdfminer's `LTChar(LTComponent, LTText)`:
    /// the MRO is LTChar, the compiled chain, LTText, object, so every
    /// field and compiled method resolves in the chain before the mixin is
    /// asked, and the mixin only adds what the chain lacks.
    private static Set<String> mixins(List<Stmt> body) {
        Set<String> used = new HashSet<String>();
        for (Stmt s : body) {
            if (s instanceof ClassDef) {
                List<Expr> b = ((ClassDef) s).bases;
                for (int i = 1; i < b.size(); i++) {
                    if (b.get(i) instanceof Name) {
                        used.add(((Name) b.get(i)).id);
                    }
                }
            }
        }
        Set<String> superNames = new HashSet<String>();
        superAttrs(body, superNames);
        Set<String> r = new HashSet<String>();
        for (String n : used) {
            ClassDef m = classNamed(body, n);
            if (m != null && neutralMixin(m, superNames) && definedOnce(body, n)) {
                r.add(n);
            }
        }
        return r;
    }

    /// Whether `m` adds nothing to an instance and nothing typed code acts
    /// on: no base but `object`, no decorator or keyword, a body of methods
    /// (and docstrings, constants, bare annotations) none of which is
    /// `__init__`, `__new__`, an attribute hook or a special method typed
    /// code assumes, no store to any attribute, and no method a `super()`
    /// call in the module names -- which a compiled chain resolves
    /// without the mixin the MRO puts after it.
    static boolean neutralMixin(ClassDef m, Set<String> superNames) {
        if (!m.decorators.isEmpty() || !m.keywords.isEmpty() || m.bases.size() > 1
                || m.bases.size() == 1 && !(m.bases.get(0) instanceof Name && "object".equals(((Name) m.bases.get(0)).id))) {
            return false;
        }
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                if (n.startsWith("__") && n.endsWith("__") && !NEUTRAL_SPECIALS.contains(n) || superNames.contains(n)) {
                    return false;
                }
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass) && !bareAnnotation(s)
                    && !(s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && ((Assign) s).value instanceof Constant)) {
                return false;
            }
        }
        boolean[] stores = {false};
        attrStore(m.body, stores);
        return !stores[0];
    }

    /// Whether `m`, a class of another module mixed in as a later base, can
    /// stay uncompiled under a compiled subclass: [#neutralMixin]'s shape
    /// (checked again by the runtime, PyType.neutralExtraBases) except that
    /// its methods may store attributes of `self` -- their first parameter
    /// -- and nothing else. Those names are kept out of every subclass's
    /// fields ([#selfStores]), so the generic stores reach the instance's
    /// own attributes and typed code reads them generically.
    /// `superNames` holds every name a `super()` call in the program reads.
    static boolean foreignMixin(ClassDef m, Set<String> superNames) {
        if (!m.decorators.isEmpty() || !m.keywords.isEmpty() || m.bases.size() > 1
                || m.bases.size() == 1 && !(m.bases.get(0) instanceof Name && "object".equals(((Name) m.bases.get(0)).id))) {
            return false;
        }
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                String n = f.name;
                if (n.startsWith("__") && n.endsWith("__") && !NEUTRAL_SPECIALS.contains(n) || superNames.contains(n)) {
                    return false;
                }
                String self = f.args.positional.isEmpty() ? null : f.args.positional.get(0).name;
                boolean[] other = {false};
                otherStore(f.body, self, other);
                if (other[0]) {
                    return false;
                }
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass) && !bareAnnotation(s)
                    && !plainClassBinding(s)) {
                return false;
            }
        }
        return true;
    }

    /// `X = value` or `X: T = value` in a class body, `X` no special name:
    /// a class attribute of an uncompiled mixin, whatever the value (bs4's
    /// `LOOKS_LIKE_HTML: Pattern[str] = re.compile(...)`). The subclass keeps
    /// the name out of its fields, so it always reads the class's value.
    private static boolean plainClassBinding(Stmt s) {
        Expr t = s instanceof Assign && ((Assign) s).targets.size() == 1 ? ((Assign) s).targets.get(0)
                : s instanceof AnnAssign ? ((AnnAssign) s).target : null;
        return t instanceof Name && !((Name) t).id.startsWith("__");
    }

    /// The attribute names `m`'s methods store on `self`.
    static Set<String> selfStores(ClassDef m) {
        Set<String> r = new HashSet<String>();
        for (Stmt s : m.body) {
            if (s instanceof FunctionDef && !((FunctionDef) s).args.names().isEmpty()) {
                selfStores(((FunctionDef) s).body, ((FunctionDef) s).args.names().get(0), r);
            }
        }
        return r;
    }

    /// Sets `r[0]` when a store or delete in `o` has a target other than a
    /// name or an attribute of the name `self`, or a nested function or
    /// class rebinds `self`'s meaning.
    private static void otherStore(Object o, String self, boolean[] r) {
        if (r[0]) {
            return;
        }
        List<Expr> targets = new ArrayList<Expr>();
        if (o instanceof Assign) {
            targets.addAll(((Assign) o).targets);
        } else if (o instanceof AugAssign) {
            targets.add(((AugAssign) o).target);
        } else if (o instanceof AnnAssign) {
            targets.add(((AnnAssign) o).target);
        } else if (o instanceof Delete || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            r[0] = true;
            return;
        }
        for (Expr t : targets) {
            r[0] |= !(t instanceof Name || self != null && t instanceof Attribute
                    && ((Attribute) t).value instanceof Name && ((Name) ((Attribute) t).value).id.equals(self));
            r[0] |= t instanceof Name && ((Name) t).id.equals(self);
        }
        Infer.children(o, x -> otherStore(x, self, r));
    }

    private static final Set<String> NEUTRAL_SPECIALS = new HashSet<String>();

    static {
        // Kept equal to the runtime's PyType.NEUTRAL_SPECIALS, which checks
        // the same of a mixin at run time before the layout is given.
        String[] n = {"__repr__", "__str__", "__format__", "__getstate__", "__setstate__", "__reduce__",
            "__reduce_ex__", "__copy__", "__deepcopy__", "__init_subclass__", "__class_getitem__", "__sizeof__",
            "__dir__", "__enter__", "__exit__"};
        for (String x : n) {
            NEUTRAL_SPECIALS.add(x);
        }
    }

    private static void attrStore(Object o, boolean[] r) {
        if (r[0]) {
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                r[0] |= !(t instanceof Name);
            }
        } else if (o instanceof AugAssign) {
            r[0] |= !(((AugAssign) o).target instanceof Name);
        } else if (o instanceof AnnAssign) {
            r[0] |= !(((AnnAssign) o).target instanceof Name);
        } else if (o instanceof Delete) {
            r[0] = true;
        }
        Infer.children(o, x -> attrStore(x, r));
    }

    static void superAttrs(Object o, Set<String> r) {
        if (o instanceof Attribute && ((Attribute) o).value instanceof Call
                && ((Call) ((Attribute) o).value).func instanceof Name
                && "super".equals(((Name) ((Call) ((Attribute) o).value).func).id)) {
            r.add(((Attribute) o).attr);
        }
        Infer.children(o, x -> superAttrs(x, r));
    }

    /// `typing.Generic[...]`, or bare: a base that only records type
    /// parameters. `Protocol` is not one: its metaclass makes every
    /// subclass's instance checks its own. The runtime confirms the base
    /// adds nothing at class creation (PyType.neutralExtraBases).
    static boolean typingBase(Expr b) {
        Expr e = b instanceof Subscript ? ((Subscript) b).value : b;
        String n = e instanceof Name ? ((Name) e).id : e instanceof Attribute ? ((Attribute) e).attr : null;
        return "Generic".equals(n);
    }

    /// The class a base expression derives from: `LTContainer[LTItemT]`
    /// is `LTContainer`, as `__mro_entries__` of the alias makes it at run
    /// time. `Generic[T]` stays as written ([#typingBase]).
    static Expr origin(Expr b) {
        if (b instanceof Subscript && !typingBase(b)
                && (((Subscript) b).value instanceof Name || ((Subscript) b).value instanceof Attribute)) {
            return ((Subscript) b).value;
        }
        return b;
    }

    static ClassDef classNamed(List<Stmt> body, String name) {
        for (Stmt s : body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(name)) {
                return (ClassDef) s;
            }
        }
        return null;
    }

    private static boolean definedOnce(List<Stmt> body, String name) {
        int k = 0;
        for (Stmt s : body) {
            k += s instanceof ClassDef && ((ClassDef) s).name.equals(name) ? 1 : 0;
        }
        return k == 1;
    }

    /// Whether the module defines a top-level class `name` (one the planner
    /// may have rejected): a base of that name is that class, never an
    /// import of the same name.
    private static boolean moduleClassNamed(List<Stmt> body, String name) {
        for (Stmt s : body) {
            if (s instanceof ClassDef && ((ClassDef) s).name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    /// Whether class `c` may extend `base`, a compiled class of another
    /// module, whose plan is already made and cannot learn of `c`: no name
    /// `c` binds in its body may be a field above it (it would be a class
    /// attribute, maybe a property, that the base reads as a field), and no
    /// name its methods store on `self` may be a method above it (the
    /// base's own plan would have looked that method up per call). A class
    /// failing either keeps the generic instances it has today.
    private static String extendsForeign(ClassDef c, Cls base, Layouts home, Map<String, Cls> byName,
            java.util.function.Function<Expr, Cls> foreignBase) {
        Set<String> body = bodyBindings(c);
        Set<String> stored = new HashSet<String>();
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && !((FunctionDef) s).args.names().isEmpty()) {
                selfStores(((FunctionDef) s).body, ((FunctionDef) s).args.names().get(0), stored);
            }
        }
        // Names the base chain itself stores on `self`: its plan already
        // reads those per instance, whatever a class body above binds them
        // to. bs4's `PageElement` binds `known_xml = None` and `hidden =
        // False` as defaults that `Tag` stores over, and `BeautifulSoup`
        // storing them too kept it -- the parser's every `self.tagStack`,
        // `self.currentTag` -- off a layout.
        Set<String> baseStored = new HashSet<String>();
        for (Cls a = base; a != null; a = a.parent) {
            for (Stmt s : a.def.body) {
                if (s instanceof FunctionDef && !((FunctionDef) s).args.names().isEmpty()) {
                    selfStores(((FunctionDef) s).body, ((FunctionDef) s).args.names().get(0), baseStored);
                }
            }
        }
        for (Cls a = base; a != null; a = a.parent) {
            for (String f : a.allFields) {
                if (body.contains(f) && !plainDefault(c, f, home)
                        && !(bindsNothing(c, f) && extraBasesPlain(c, f, byName, foreignBase))) {
                    return "its body binds " + f + ", a field of " + a.def.name;
                }
            }
            for (String m : stored) {
                if (setterOf(a, m)) {
                    // A property with a setter: the store is a call to it.
                    continue;
                }
                if (a.methods.containsKey(m) || a.typable.containsKey(m)
                        || a.bodyNames.contains(m) && !baseStored.contains(m) && !shadowableDefault(a, m)) {
                    return "it stores self." + m + ", which " + a.def.name + " binds in its class";
                }
            }
        }
        return null;
    }

    /// Whether `c`'s body binds `f` exactly once, to a default that is no
    /// descriptor: a constant, a builtin class (`expected_type = bool`) or
    /// `type(x)`. openpyxl's `Bool(Convertible)` binds `expected_type` and
    /// `allow_none` over what the base `Descriptor` stores as fields. Such a
    /// binding is as harmless here as in one module (the `constants` of
    /// [#plan]): a store binds the instance's own slot, and an unset slot
    /// falls back to the generic read, which looks the name up on the
    /// instance's actual class and finds this default. The base never folds
    /// the read to its own class value, since the field is stored
    /// (Infer#classStatic, Emitter#classConstantGet).
    /// Whether no statement of `c`'s own body binds `f` (it is in the body's
    /// names because an extra base's body binds it).
    private static boolean bindsNothing(ClassDef c, String f) {
        for (Stmt s : c.body) {
            if (bodyBindingsOf(s).contains(f)) {
                return false;
            }
        }
        return true;
    }

    /// Whether every class above `c`'s extra bases (`NestedBoolText(Bool,
    /// NestedText)`) that binds `f` binds it to a [#plainDefault]. An extra
    /// base that cannot be resolved answers false.
    private static boolean extraBasesPlain(ClassDef c, String f, Map<String, Cls> byName,
            java.util.function.Function<Expr, Cls> foreignBase) {
        for (int i = 1; i < c.bases.size(); i++) {
            Expr b = origin(c.bases.get(i));
            Cls x = b instanceof Name ? byName.get(((Name) b).id) : null;
            if (x == null && foreignBase != null) {
                x = foreignBase.apply(b);
            }
            if (x == null) {
                return false;
            }
            for (Cls a = x; a != null; a = a.parent) {
                if (!bindsNothing(a.def, f) && !plainDefault(a.def, f, a.home)) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Stands for an undecorated def in [#plainDefault].
    private static final Expr PLAIN_DEF = new Name("def");

    private static boolean plainDefault(ClassDef c, String f, Layouts home) {
        Set<String> moduleAssigned = home.moduleAssigned;
        Expr found = null;
        for (Stmt s : c.body) {
            Set<String> b = new HashSet<String>();
            Expr v = null;
            if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    names(t, b);
                }
                if (((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name) {
                    v = ((Assign) s).value;
                }
            } else if (s instanceof AnnAssign) {
                if (!bareAnnotation(s)) {
                    names(((AnnAssign) s).target, b);
                    v = ((AnnAssign) s).target instanceof Name ? ((AnnAssign) s).value : null;
                }
            } else if (s instanceof FunctionDef) {
                b.add(((FunctionDef) s).name);
                if (((FunctionDef) s).decorators.isEmpty()) {
                    // A plain def is no data descriptor: a store fills the
                    // slot, and an unset one reads the method generically.
                    // Its calls dispatch generically too (see the plan's
                    // `inheritedField`), so the instance's value wins.
                    v = ((FunctionDef) s).name.isEmpty() ? null : PLAIN_DEF;
                }
            } else if (s instanceof ClassDef) {
                b.add(((ClassDef) s).name);
            } else if (s instanceof AugAssign) {
                names(((AugAssign) s).target, b);
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass)) {
                return false;
            }
            if (b.contains(f)) {
                if (found != null || v == null) {
                    return false;
                }
                found = v;
            }
        }
        if (found == PLAIN_DEF) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        if (found instanceof UnaryOp && ("-".equals(((UnaryOp) found).op) || "+".equals(((UnaryOp) found).op))) {
            found = ((UnaryOp) found).operand;
        }
        if (found instanceof Constant) {
            return true;
        }
        if (found instanceof Name) {
            String id = ((Name) found).id;
            if (home.constantGlobal != null && moduleAssigned.contains(id) && !bodyBindings(c).contains(id)
                    && home.constantGlobal.test(id)) {
                // A global holding one literal for the whole run is that
                // literal: openpyxl's `namespace = REL_NS`.
                return true;
            }
            return DEFAULT_TYPES.contains(id) && !moduleAssigned.contains(id);
        }
        return found instanceof Call && ((Call) found).func instanceof Name
                && "type".equals(((Name) ((Call) found).func).id) && ((Call) found).args.size() == 1
                && ((Call) found).keywords.isEmpty() && !moduleAssigned.contains("type");
    }

    /// Whether `a`'s body binds `m` as a property with a `@m.setter`.
    private static boolean setterOf(Cls a, String m) {
        for (Stmt s : a.def.body) {
            if (s instanceof FunctionDef && m.equals(((FunctionDef) s).name)) {
                for (Expr d : ((FunctionDef) s).decorators) {
                    if (d instanceof Attribute && "setter".equals(((Attribute) d).attr)
                            && ((Attribute) d).value instanceof Name && m.equals(((Name) ((Attribute) d).value).id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// The builtin classes a class body may bind as a plain default
    /// (`expected_type = float`): a class object is no descriptor.
    private static final Set<String> DEFAULT_TYPES = new HashSet<String>(java.util.Arrays.asList("int", "float",
            "str", "bool", "bytes", "tuple", "list", "dict", "set", "frozenset", "object", "type", "complex"));

    /// Whether a store to `self.m` cannot change what the base's plan made
    /// of the binding `a`'s body gives `m`. True for a default that is no
    /// descriptor -- a constant (`idx_base = 0`) or a builtin class
    /// (`expected_type = float`) -- whose every read the base folds only
    /// under the whole-program "nobody stores it" fact (Infer#classStatic,
    /// Emitter#classConstantGet), which the store itself turns off; the
    /// instance then holds the value and the generic read finds it first.
    /// And true for an instance of a data descriptor of `a`'s module
    /// (`number_format = NumberFormatDescriptor()`, a class with
    /// `__set__`): a store goes through `__set__` and the instance never
    /// holds the name, so the base's reads are unchanged.
    private static boolean shadowableDefault(Cls a, String m) {
        Expr v = onceValue(a, m);
        if (v instanceof Constant) {
            return true;
        }
        if (v instanceof Name) {
            return DEFAULT_TYPES.contains(((Name) v).id) && !a.home.moduleBinds(((Name) v).id);
        }
        if (v instanceof Call && ((Call) v).func instanceof Name) {
            String f = ((Name) ((Call) v).func).id;
            if ("type".equals(f) && ((Call) v).args.size() == 1) {
                return !a.home.moduleBinds("type");
            }
            for (Cls d : a.home.classes.values()) {
                if (d.def.name.equals(f) && d.compiled && d.home == a.home) { //NOPMD CompareObjectsWithEquals - identity is meant
                    Cls set = resolver(d, "__set__");
                    return set != null && set.methods.containsKey("__set__");
                }
            }
        }
        return false;
    }

    private static final Set<String> FORBIDDEN = new HashSet<String>();
    /// Methods Python makes static or class methods without a decorator.
    static final Set<String> IMPLICIT_CLASS_LEVEL = new HashSet<String>(java.util.Arrays.asList("__new__",
            "__init_subclass__", "__class_getitem__"));

    static {
        // A class that customises attribute access itself is left alone.
        // `__slots__` is not on the list: it fixes the attribute set, which
        // is what a layout is, and the generic instance does not enforce it
        // either, so the two paths agree.
        FORBIDDEN.add("__setattr__");
        FORBIDDEN.add("__getattribute__");
        FORBIDDEN.add("__delattr__");
        FORBIDDEN.add("__dict__");
        FORBIDDEN.add("__class__");
        // `__new__` is not on the list: typed code constructs such a class
        // through the class itself (TypedEmit#construct), which runs it.
    }

    /// `moduleAssigned` is every name the module binds, imports included.
    /// `attrStores` is the module scope's (`Scope.attrStores`): a field the
    /// module itself assigns on a class is left to the generic path, since
    /// the class attribute -- a property, say -- must then decide its reads.
    /// The builtin types a compiled class may extend, by name, with the
    /// names each binds (CPython's, less the class metadata).
    static final Map<String, Set<String>> BUILTIN_NAMES = new LinkedHashMap<String, Set<String>>();

    static {
        BUILTIN_NAMES.put("str", names("__add__ __contains__ __eq__ __format__ __ge__ __getitem__ __getnewargs__ __gt__ __hash__ "
                + "__iter__ __le__ __len__ __lt__ __mod__ __mul__ __ne__ __new__ __repr__ __rmod__ __rmul__ "
                + "__sizeof__ __str__ capitalize casefold center count encode endswith expandtabs find "
                + "format format_map index isalnum isalpha isascii isdecimal isdigit isidentifier islower "
                + "isnumeric isprintable isspace istitle isupper join ljust lower lstrip maketrans "
                + "partition removeprefix removesuffix replace rfind rindex rjust rpartition rsplit rstrip "
                + "split splitlines startswith strip swapcase title translate upper zfill "));
        BUILTIN_NAMES.put("list", names("__add__ __class_getitem__ __contains__ __delitem__ __eq__ __ge__ __getitem__ __gt__ "
                + "__hash__ __iadd__ __imul__ __init__ __iter__ __le__ __len__ __lt__ __mul__ __ne__ "
                + "__new__ __repr__ __reversed__ __rmul__ __setitem__ __sizeof__ append clear copy count "
                + "extend index insert pop remove reverse sort "));
        BUILTIN_NAMES.put("dict", names("__class_getitem__ __contains__ __delitem__ __eq__ __ge__ __getitem__ __gt__ __hash__ "
                + "__init__ __ior__ __iter__ __le__ __len__ __lt__ __ne__ __new__ __or__ __repr__ "
                + "__reversed__ __ror__ __setitem__ __sizeof__ clear copy fromkeys get items keys pop "
                + "popitem setdefault update values "));
        BUILTIN_NAMES.put("set", names("__and__ __class_getitem__ __contains__ __eq__ __ge__ __gt__ __hash__ __iand__ __init__ "
                + "__ior__ __isub__ __iter__ __ixor__ __le__ __len__ __lt__ __ne__ __new__ __or__ __rand__ "
                + "__reduce__ __repr__ __ror__ __rsub__ __rxor__ __sizeof__ __sub__ __xor__ add clear copy "
                + "difference difference_update discard intersection intersection_update isdisjoint "
                + "issubset issuperset pop remove symmetric_difference symmetric_difference_update union "
                + "update "));
        BUILTIN_NAMES.put("tuple", names("__add__ __class_getitem__ __contains__ __eq__ __ge__ __getitem__ __getnewargs__ __gt__ "
                + "__hash__ __iter__ __le__ __len__ __lt__ __mul__ __ne__ __new__ __repr__ __rmul__ count "
                + "index "));
        BUILTIN_NAMES.put("bytes", names("__add__ __buffer__ __bytes__ __contains__ __eq__ __ge__ __getitem__ __getnewargs__ "
                + "__gt__ __hash__ __iter__ __le__ __len__ __lt__ __mod__ __mul__ __ne__ __new__ __repr__ "
                + "__rmod__ __rmul__ __str__ capitalize center count decode endswith expandtabs find "
                + "fromhex hex index isalnum isalpha isascii isdigit islower isspace istitle isupper join "
                + "ljust lower lstrip maketrans partition removeprefix removesuffix replace rfind rindex "
                + "rjust rpartition rsplit rstrip split splitlines startswith strip swapcase title "
                + "translate upper zfill "));
        BUILTIN_NAMES.put("int", names("__abs__ __add__ __and__ __bool__ __ceil__ __divmod__ __eq__ __float__ __floor__ "
                + "__floordiv__ __format__ __ge__ __getnewargs__ __gt__ __hash__ __index__ __int__ "
                + "__invert__ __le__ __lshift__ __lt__ __mod__ __mul__ __ne__ __neg__ __new__ __or__ "
                + "__pos__ __pow__ __radd__ __rand__ __rdivmod__ __repr__ __rfloordiv__ __rlshift__ "
                + "__rmod__ __rmul__ __ror__ __round__ __rpow__ __rrshift__ __rshift__ __rsub__ "
                + "__rtruediv__ __rxor__ __sizeof__ __sub__ __truediv__ __trunc__ __xor__ as_integer_ratio "
                + "bit_count bit_length conjugate denominator from_bytes imag is_integer numerator real "
                + "to_bytes "));
        BUILTIN_NAMES.put("float", names("__abs__ __add__ __bool__ __ceil__ __divmod__ __eq__ __float__ __floor__ __floordiv__ "
                + "__format__ __ge__ __getformat__ __getnewargs__ __gt__ __hash__ __int__ __le__ __lt__ "
                + "__mod__ __mul__ __ne__ __neg__ __new__ __pos__ __pow__ __radd__ __rdivmod__ __repr__ "
                + "__rfloordiv__ __rmod__ __rmul__ __round__ __rpow__ __rsub__ __rtruediv__ __sub__ "
                + "__truediv__ __trunc__ as_integer_ratio conjugate from_number fromhex hex imag is_integer "
                + "real "));
        BUILTIN_NAMES.put("frozenset", names("__and__ __class_getitem__ __contains__ __eq__ __ge__ __gt__ __hash__ __iter__ __le__ "
                + "__len__ __lt__ __ne__ __new__ __or__ __rand__ __reduce__ __repr__ __ror__ __rsub__ "
                + "__rxor__ __sizeof__ __sub__ __xor__ copy difference intersection isdisjoint issubset "
                + "issuperset symmetric_difference union "));
    }

    private static Set<String> names(String s) {
        return new HashSet<String>(java.util.Arrays.asList(s.trim().split(" +")));
    }

    /// The builtin `b` names as a first base -- `str`, or `list[str]` --
    /// when the module never rebinds the name; null otherwise.
    static String builtinBase(Expr b, Set<String> moduleAssigned) {
        Expr e = b instanceof Subscript ? ((Subscript) b).value : b;
        if (!(e instanceof Name) || moduleAssigned.contains(((Name) e).id)
                || !BUILTIN_NAMES.containsKey(((Name) e).id)) {
            return null;
        }
        return ((Name) e).id;
    }

    /// The builtin exception types a compiled class may derive from, by
    /// the name a base spells, to the canonical name the runtime gives the
    /// type (`IOError` and `EnvironmentError` are `OSError`).
    static final Map<String, String> EXCEPTION_TYPES = new LinkedHashMap<String, String>();

    static {
        for (String n : ("BaseException SystemExit KeyboardInterrupt GeneratorExit Exception StopIteration "
                + "ArithmeticError ZeroDivisionError OverflowError AssertionError AttributeError LookupError "
                + "IndexError KeyError NameError UnboundLocalError RuntimeError NotImplementedError RecursionError "
                + "TypeError ValueError UnicodeError ImportError ModuleNotFoundError OSError FileNotFoundError "
                + "MemoryError UnicodeEncodeError UnicodeDecodeError UnicodeTranslateError FloatingPointError "
                + "StopAsyncIteration EOFError BufferError ReferenceError SystemError SyntaxError IndentationError "
                + "TabError FileExistsError PermissionError IsADirectoryError NotADirectoryError TimeoutError "
                + "InterruptedError BlockingIOError ChildProcessError ProcessLookupError ConnectionError "
                + "BrokenPipeError ConnectionAbortedError ConnectionRefusedError ConnectionResetError Warning "
                + "UserWarning DeprecationWarning PendingDeprecationWarning SyntaxWarning RuntimeWarning "
                + "FutureWarning ImportWarning UnicodeWarning BytesWarning ResourceWarning EncodingWarning").split(" ")) {
            EXCEPTION_TYPES.put(n, n);
        }
        EXCEPTION_TYPES.put("IOError", "OSError");
        EXCEPTION_TYPES.put("EnvironmentError", "OSError");
    }

    /// What every builtin exception binds or stores in an instance's
    /// attribute table, the attributes of the families that set more
    /// (OSError's `errno`, SyntaxError's `lineno`, UnicodeError's `start`)
    /// included for all of them: such a name is never a field, so the
    /// runtime's own stores and reads of it meet the user's.
    static final Set<String> EXCEPTION_NAMES = names("__init__ __new__ __str__ __repr__ __reduce__ __setstate__ "
            + "args with_traceback add_note __notes__ __cause__ __context__ __suppress_context__ __traceback__ "
            + "errno strerror filename filename2 winerror characters_written encoding object start end reason "
            + "code value msg lineno offset text end_lineno end_offset print_file_and_line name path obj "
            + "exceptions message");

    /// The canonical builtin exception `b` names, when the module never
    /// rebinds the name; null otherwise.
    static String exceptionBase(Expr b, Set<String> moduleAssigned) {
        if (!(b instanceof Name) || moduleAssigned.contains(((Name) b).id)) {
            return null;
        }
        return EXCEPTION_TYPES.get(((Name) b).id);
    }

    /// Whether `c`'s one keyword is `metaclass=M`, `M` a class of the
    /// module deriving from `type` alone that does not bind `__call__`: it
    /// shapes the class object (bs4's lazily computed entity tables), and
    /// constructing an instance still runs `type.__call__`, which makes it
    /// with the layout.
    /// Why the keywords of `kc` (named ones other than `metaclass` already
    /// dropped) refuse a layout: a metaclass the build cannot rewrite, or a
    /// `**mapping` that may carry one.
    private static String keywordWhy(ClassDef kc) {
        for (Keyword kw : kc.keywords) {
            if (kw.arg == null) {
                return "its class keywords include **" + text(kw.value) + ", which may carry a metaclass";
            }
        }
        if (kc.keywords.size() == 1) {
            return "its metaclass " + text(kc.keywords.get(0).value) + " is not one defined once in the module,"
                    + " deriving from type and binding no hook besides __call__, __getattr__, __new__ and __init__";
        }
        return "it names more than one metaclass";
    }

    private static boolean plainMetaclass(List<Stmt> body, ClassDef c, Set<String> moduleAssigned) {
        if (c.keywords.size() == 1 && "metaclass".equals(c.keywords.get(0).arg)
                && (abcMeta(body, c.keywords.get(0).value) || enumTypeMeta(body, c.keywords.get(0).value))) {
            return true;
        }
        if (c.keywords.size() != 1 || !"metaclass".equals(c.keywords.get(0).arg)
                || !(c.keywords.get(0).value instanceof Name)) {
            return false;
        }
        return metaclassChain(body, ((Name) c.keywords.get(0).value).id, moduleAssigned, LAYOUT_SAFE_HOOKS, 0);
    }

    /// The hooks a metaclass may bind and still leave its classes a
    /// layout. `__getattr__` runs only for a class attribute that is not
    /// found, so nothing the compiler resolves statically reaches it, and
    /// an instance never consults its metaclass. `__call__` decides what
    /// calling the class answers: such a class is constructed by calling
    /// it ([Cls#metaCall]), never by `new` and `__init__`, and the result
    /// is typed as any object. `__new__` and `__init__` shape the class
    /// when it is made: what they add to it is reached as any class
    /// attribute is, and the runtime refuses one that replaces the class or
    /// rebinds or adds a name the layout compiled (`PyType.finish`).
    private static final Set<String> LAYOUT_SAFE_HOOKS = new HashSet<String>(java.util.Arrays.asList("__call__",
            "__getattr__", "__new__", "__init__"));

    /// Whether `m` is a metaclass of the module, defined once, deriving
    /// from `type` alone or from such a metaclass (each level checked), no
    /// level with class keywords, decorators, or a hook of
    /// [#METACLASS_HOOKS] outside `allowed`.
    private static boolean metaclassChain(List<Stmt> body, String m, Set<String> moduleAssigned, Set<String> allowed,
            int depth) {
        ClassDef md = classNamed(body, m);
        if (depth > 16 || md == null || !definedOnce(body, m) || md.bases.size() != 1 || !md.keywords.isEmpty()
                || !md.decorators.isEmpty() || !(md.bases.get(0) instanceof Name)) {
            return false;
        }
        Set<String> hooks = new HashSet<String>(bodyBindings(md));
        hooks.retainAll(METACLASS_HOOKS);
        hooks.removeAll(allowed);
        if (!hooks.isEmpty()) {
            return false;
        }
        for (Stmt st : md.body) {
            if (!(st instanceof FunctionDef)) {
                continue;
            }
            FunctionDef f = (FunctionDef) st;
            if ("__new__".equals(f.name) && (!keepsClass(f) || !leavesNamespace(f, classBound(body)))
                    || "__init__".equals(f.name) && !leavesNamespace(f, classBound(body))
                    || "__prepare__".equals(f.name) && !plainPrepare(f)) {
                return false;
            }
        }
        String b = ((Name) md.bases.get(0)).id;
        if ("type".equals(b)) {
            return !moduleAssigned.contains("type");
        }
        return metaclassChain(body, b, moduleAssigned, allowed, depth + 1);
    }

    /// Whether a metaclass `__new__` answers the class the class statement
    /// built: every `return` hands back `X.__new__(mcs, name, bases, ...)`
    /// with its own `bases` parameter, never rebound, or a local assigned
    /// only from such a call. Another class (other bases, a class of its
    /// own making) is a class the layout does not describe.
    private static boolean keepsClass(FunctionDef f) {
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
        Map<String, Integer> fwd = new HashMap<String, Integer>();
        for (Stmt s : f.body) {
            madeLocals(s, bases, fwd);
        }
        Set<String> made = new HashSet<String>();
        for (Map.Entry<String, Integer> e : fwd.entrySet()) {
            if (e.getValue().equals(n.get(e.getKey()))) {
                made.add(e.getKey());
            }
        }
        boolean[] ok = {true};
        for (Stmt s : f.body) {
            returnsClass(s, bases, made, ok);
        }
        return ok[0];
    }

    /// Whether a `__prepare__` answers a fresh empty dict on every path
    /// (`return {}` or `return dict()`): any other mapping can rewrite the
    /// names the class body binds, which the layout compiled as written.
    private static boolean plainPrepare(FunctionDef f) {
        boolean[] ok = {true};
        for (Stmt s : f.body) {
            emptyDictReturns(s, ok);
        }
        return ok[0];
    }

    private static void emptyDictReturns(Object o, boolean[] ok) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Return) {
            Expr v = ((Return) o).value;
            ok[0] &= v instanceof DictExpr && ((DictExpr) v).keys.isEmpty()
                    || v instanceof Call && ((Call) v).func instanceof Name && "dict".equals(((Name) ((Call) v).func).id)
                    && ((Call) v).args.isEmpty() && ((Call) v).keywords.isEmpty();
            return;
        }
        Infer.children(o, x -> emptyDictReturns(x, ok));
    }

    /// Reading methods of the namespace dict that never change it.
    private static final Set<String> NAMESPACE_READS = new HashSet<String>(java.util.Arrays.asList("get", "items",
            "keys", "values", "copy", "__contains__", "__getitem__"));

    /// Whether a metaclass `__new__` or `__init__` leaves the class body's
    /// names as the body bound them: it never calls `setattr` or `delattr`,
    /// never deletes an attribute or key, stores an attribute or a namespace
    /// key only by a literal name no class body of the module binds
    /// (`cls._inst = None`, `ns["made_by"] = ...`: an addition, never a
    /// rewrite), never rebinds its namespace parameter, and uses that
    /// parameter otherwise only to read it
    /// (subscript load, `in`, a method of [#NAMESPACE_READS]) or to hand it
    /// unchanged to a `__new__` / `__init__` call. Anything else may rewrite
    /// or wrap a name the layout compiled, so strict refuses the layout at
    /// build time; `PyType.finish` stays as the run-time backstop.
    private static boolean leavesNamespace(FunctionDef f, Set<String> bound) {
        if (!f.decorators.isEmpty()) {
            return false;
        }
        String ns = f.args.positional.size() >= 4 ? f.args.positional.get(3).name : null;
        if (ns != null) {
            Map<String, Integer> n = new HashMap<String, Integer>();
            for (Stmt s : f.body) {
                DataclassInit.count(s, n);
            }
            if (n.containsKey(ns)) {
                return false;
            }
        }
        java.util.IdentityHashMap<Object, Boolean> okUses = new java.util.IdentityHashMap<Object, Boolean>();
        boolean[] ok = {true};
        for (Stmt s : f.body) {
            namespaceUses(s, ns, bound, okUses, ok);
        }
        return ok[0];
    }

    private static boolean isNs(Expr e, String ns) {
        return ns != null && e instanceof Name && ns.equals(((Name) e).id);
    }

    private static void storeTarget(Expr t, String ns, Set<String> bound, boolean[] ok) {
        // `cls._inst = None` or `ns["made_by"] = ...`: a name no class body
        // of the module binds adds to the class and rewrites nothing the
        // layout compiled
        boolean fresh = bound != null && (t instanceof Attribute && ((Attribute) t).value instanceof Name
                && !bound.contains(((Attribute) t).attr)
                || t instanceof Subscript && isNs(((Subscript) t).value, ns)
                && ((Subscript) t).index instanceof Constant
                && ((Constant) ((Subscript) t).index).kind == Constant.Kind.STR
                && !bound.contains(((Constant) ((Subscript) t).index).text));
        if (fresh) {
            return;
        } else if (t instanceof Attribute || t instanceof Subscript) {
            ok[0] = false;
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                storeTarget(e, ns, null, ok);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                storeTarget(e, ns, null, ok);
            }
        } else if (t instanceof Starred) {
            storeTarget(((Starred) t).value, ns, null, ok);
        }
    }

    private static void namespaceUses(Object o, String ns, Set<String> bound,
            java.util.IdentityHashMap<Object, Boolean> okUses, boolean[] ok) {
        if (!ok[0] || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
                // a nested scope that names the namespace may keep it and write it later
                Set<String> names = new HashSet<String>();
                Infer.children(o, x -> collectNames(x, names));
                ok[0] &= ns == null || !names.contains(ns);
            }
            return;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                storeTarget(t, ns, bound, ok);
            }
        } else if (o instanceof AugAssign) {
            storeTarget(((AugAssign) o).target, ns, null, ok);
        } else if (o instanceof AnnAssign) {
            storeTarget(((AnnAssign) o).target, ns, bound, ok);
        } else if (o instanceof Delete) {
            for (Expr t : ((Delete) o).targets) {
                storeTarget(t, ns, null, ok);
            }
        } else if (o instanceof For) {
            storeTarget(((For) o).target, ns, null, ok);
        } else if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Name && ("setattr".equals(((Name) c.func).id)
                    || "delattr".equals(((Name) c.func).id))) {
                ok[0] = false;
                return;
            }
            if (c.func instanceof Attribute && isNs(((Attribute) c.func).value, ns)
                    && NAMESPACE_READS.contains(((Attribute) c.func).attr)) {
                okUses.put(((Attribute) c.func).value, Boolean.TRUE);
            }
            if (c.func instanceof Attribute && ("__new__".equals(((Attribute) c.func).attr)
                    || "__init__".equals(((Attribute) c.func).attr))) {
                for (Expr a : c.args) {
                    if (isNs(a, ns)) {
                        okUses.put(a, Boolean.TRUE);
                    }
                }
            }
        } else if (o instanceof Subscript && isNs(((Subscript) o).value, ns)) {
            // a load, or a store of a fresh literal key storeTarget allowed
            okUses.put(((Subscript) o).value, Boolean.TRUE);
        } else if (o instanceof Compare) {
            Compare c = (Compare) o;
            for (int i = 0; i < c.ops.size(); i++) {
                String op = c.ops.get(i);
                if (("in".equals(op) || "not in".equals(op)) && isNs(c.comparators.get(i), ns)) {
                    okUses.put(c.comparators.get(i), Boolean.TRUE);
                }
            }
        } else if (isNs(o instanceof Expr ? (Expr) o : null, ns) && !okUses.containsKey(o)) {
            ok[0] = false;
            return;
        }
        Infer.children(o, x -> namespaceUses(x, ns, bound, okUses, ok));
    }

    /// Every name some class body of the module binds: a metaclass write
    /// of any other name cannot rewrite what a layout compiled.
    private static Set<String> classBound(List<Stmt> body) {
        Set<String> r = new HashSet<String>();
        for (Stmt s : body) {
            if (s instanceof ClassDef) {
                r.addAll(bodyBindings((ClassDef) s));
            }
        }
        return r;
    }

    private static void collectNames(Object o, Set<String> names) {
        if (o instanceof Name) {
            names.add(((Name) o).id);
        }
        Infer.children(o, x -> collectNames(x, names));
    }

    private static boolean forwardingNew(Expr v, String bases) {
        return v instanceof Call && ((Call) v).func instanceof Attribute
                && "__new__".equals(((Attribute) ((Call) v).func).attr) && ((Call) v).args.size() >= 3
                && ((Call) v).args.get(2) instanceof Name && bases.equals(((Name) ((Call) v).args.get(2)).id);
    }

    /// Counts, per local, the assignments `v = X.__new__(mcs, name, bases, ...)`.
    private static void madeLocals(Object o, String bases, Map<String, Integer> fwd) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Assign && ((Assign) o).targets.size() == 1 && ((Assign) o).targets.get(0) instanceof Name
                && forwardingNew(((Assign) o).value, bases)) {
            String t = ((Name) ((Assign) o).targets.get(0)).id;
            Integer c = fwd.get(t);
            fwd.put(t, c == null ? 1 : c + 1);
        }
        Infer.children(o, x -> madeLocals(x, bases, fwd));
    }

    private static void returnsClass(Object o, String bases, Set<String> made, boolean[] ok) {
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return;
        }
        if (o instanceof Return) {
            Expr v = ((Return) o).value;
            ok[0] &= forwardingNew(v, bases) || v instanceof Name && made.contains(((Name) v).id);
            return;
        }
        Infer.children(o, x -> returnsClass(x, bases, made, ok));
    }

    /// Whether some level of the metaclass chain of `m` ([#metaclassChain])
    /// binds `__call__`.
    private static boolean metaclassCalls(List<Stmt> body, String m, int depth) {
        ClassDef md = classNamed(body, m);
        if (md == null || depth > 16) {
            return false;
        }
        if (bodyBindings(md).contains("__call__")) {
            return true;
        }
        return md.bases.size() == 1 && md.bases.get(0) instanceof Name
                && metaclassCalls(body, ((Name) md.bases.get(0)).id, depth + 1);
    }

    /// Whether `c` is itself a metaclass: a class statement with no
    /// keywords or decorators whose one base is `type` (never rebound) or a
    /// metaclass of the module. Its instances are classes -- `PyType`
    /// objects, whose attributes live in the class's own dictionary -- so it
    /// is laid out as that: no Java class of its own, its methods typed and
    /// entered as dynamic dispatch enters them (by `type.__call__`, a class
    /// statement, an attribute of a class it made).
    static boolean metaclassDef(List<Stmt> body, ClassDef c, Set<String> moduleAssigned, Map<String, Cls> byName) {
        if (!c.keywords.isEmpty() || !c.decorators.isEmpty() || c.bases.size() != 1
                || !(c.bases.get(0) instanceof Name)) {
            return false;
        }
        String b = ((Name) c.bases.get(0)).id;
        if ("type".equals(b)) {
            return !moduleAssigned.contains("type");
        }
        Cls k = byName.get(b);
        return k != null && k.metaclass;
    }

    /// Whether `m` is `abc.ABCMeta`: the name imported once from `abc` (or
    /// from `_cn1op`, where abc.py itself takes it) and never rebound, or
    /// `abc.ABCMeta` through a module imported once as `abc` or an alias.
    /// That metaclass is the runtime's own (runtime class `Abc`): it computes
    /// `__abstractmethods__` when the class is made and checks them on
    /// construction, and type.__call__ still makes the instance, so its
    /// classes keep a layout.
    static boolean abcMeta(List<Stmt> body, Expr m) {
        return runtimeMeta(body, m, "abc", "ABCMeta", null);
    }

    /// Whether `m` is `enum.EnumType` (or its alias `EnumMeta`), the
    /// runtime's own metaclass (runtime class `EnumMeta`), bound as
    /// [#abcMeta] binds `ABCMeta`.
    static boolean enumTypeMeta(List<Stmt> body, Expr m) {
        return runtimeMeta(body, m, "enum", "EnumType", null) || runtimeMeta(body, m, "enum", "EnumMeta", "EnumType");
    }

    /// [#abcMeta] for the metaclass `attr` of module `module`; `real` is the
    /// name `_cn1op` binds it to when that differs from `attr`.
    private static boolean runtimeMeta(List<Stmt> body, Expr m, String module, String attr, String real) {
        String opName = real != null ? real : attr;
        if (m instanceof Attribute && attr.equals(((Attribute) m).attr) && ((Attribute) m).value instanceof Name) {
            String alias = ((Name) ((Attribute) m).value).id;
            int found = 0;
            int binds = 0;
            for (Stmt s : body) {
                if (s instanceof Import) {
                    for (Alias a : ((Import) s).names) {
                        String as = a.asname != null ? a.asname : a.name;
                        if (as.equals(alias)) {
                            binds++;
                            found += module.equals(a.name) ? 1 : 0;
                        }
                    }
                } else if (s instanceof ImportFrom) {
                    for (Alias a : ((ImportFrom) s).names) {
                        binds += (a.asname != null ? a.asname : a.name).equals(alias) ? 1 : 0;
                    }
                } else if (s instanceof Assign || s instanceof AnnAssign || s instanceof ClassDef
                        || s instanceof FunctionDef) {
                    binds += bindsName(s, alias) ? 1 : 0;
                }
            }
            return found == 1 && binds == 1;
        }
        if (!(m instanceof Name)) {
            return false;
        }
        String n = ((Name) m).id;
        int found = 0;
        int binds = 0;
        for (Stmt s : body) {
            if (s instanceof ImportFrom) {
                ImportFrom f = (ImportFrom) s;
                for (Alias a : f.names) {
                    String as = a.asname != null ? a.asname : a.name;
                    if (as.equals(n)) {
                        binds++;
                        found += f.level == 0 && (module.equals(f.module) && attr.equals(a.name)
                                || "_cn1op".equals(f.module) && opName.equals(a.name)) ? 1 : 0;
                    }
                }
            } else if (s instanceof Assign || s instanceof AnnAssign || s instanceof ClassDef
                    || s instanceof FunctionDef || s instanceof Import) {
                binds += bindsName(s, n) ? 1 : 0;
            }
        }
        return found == 1 && binds == 1;
    }

    /// The dunders `EnumType` copies onto a class mixing a builtin into an
    /// enum; `PyType.ENUM_COPIED` in the runtime must name the same ones.
    private static final Set<String> ENUM_COPIED = new HashSet<String>(java.util.Arrays.asList("__repr__",
            "__str__", "__format__", "__hash__"));

    /// What a metaclass may not define for its classes to keep a layout:
    /// construction, and the comparisons typed code makes by identity.
    private static final Set<String> METACLASS_HOOKS = new HashSet<String>(java.util.Arrays.asList("__call__",
            "__eq__", "__ne__", "__hash__", "__instancecheck__", "__subclasscheck__", "__new__", "__init__",
            "__getattribute__", "__getattr__", "__name__", "__qualname__", "__module__"));

    /// The builtins `typing`'s aliases stand for as a base: `class
    /// ResultSet(List[T])` extends `list`.
    private static final Map<String, String> TYPING_ALIASES = new LinkedHashMap<String, String>();

    static {
        TYPING_ALIASES.put("List", "list");
        TYPING_ALIASES.put("Dict", "dict");
        TYPING_ALIASES.put("Set", "set");
        TYPING_ALIASES.put("FrozenSet", "frozenset");
        TYPING_ALIASES.put("Tuple", "tuple");
    }

    /// The builtin `b` stands for when it is `Alias[...]` of a `typing`
    /// alias the module imports under its own name, once, and never rebinds.
    static String typingAliasBase(List<Stmt> body, Expr b) {
        if (!(b instanceof Subscript) || !(((Subscript) b).value instanceof Name)) {
            return null;
        }
        String n = ((Name) ((Subscript) b).value).id;
        String t = TYPING_ALIASES.get(n);
        if (t == null) {
            return null;
        }
        int imports = 0;
        int binds = 0;
        for (Stmt s : body) {
            if (s instanceof ImportFrom) {
                for (Alias a : ((ImportFrom) s).names) {
                    String as = a.asname != null ? a.asname : a.name;
                    if (as.equals(n)) {
                        binds++;
                        imports += "typing".equals(((ImportFrom) s).module) && ((ImportFrom) s).level == 0
                                && a.name.equals(n) ? 1 : 0;
                    }
                }
            } else if (s instanceof Assign || s instanceof AnnAssign || s instanceof ClassDef
                    || s instanceof FunctionDef || s instanceof Import) {
                binds += bindsName(s, n) ? 1 : 0;
            }
        }
        return imports == 1 && binds == 1 ? t : null;
    }

    private static boolean bindsName(Stmt s, String n) {
        if (s instanceof ClassDef) {
            return ((ClassDef) s).name.equals(n);
        }
        if (s instanceof FunctionDef) {
            return ((FunctionDef) s).name.equals(n);
        }
        if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                if ((a.asname != null ? a.asname : a.name.split("\\.")[0]).equals(n)) {
                    return true;
                }
            }
            return false;
        }
        List<Expr> ts = s instanceof Assign ? ((Assign) s).targets : java.util.Collections.singletonList(((AnnAssign) s).target);
        for (Expr t : ts) {
            if (t instanceof Name && ((Name) t).id.equals(n)) {
                return true;
            }
        }
        return false;
    }

    /// A name the chain from `k` up binds that the builtin `builtin` binds
    /// too, or null.
    /// A name the class binds itself is not one: its own binding comes
    /// before both. Nor, below an enum, is a name `EnumType` copies onto
    /// each mixed-in class from the nearest enum (or the mixin, for
    /// `__hash__`) when the class is made, as CPython's does.
    private static String builtinClash(Cls k, String builtin, Set<String> own) {
        Set<String> bn = BUILTIN_NAMES.get(builtin);
        for (Cls a = k; a != null; a = a.parent) {
            for (String n : a.bodyNames) {
                if (bn.contains(n) && !own.contains(n) && !(k.enumMeta && ENUM_COPIED.contains(n))) {
                    return n;
                }
            }
        }
        return null;
    }

    /// Whether every decorator of `c` provably answers the class it is
    /// given, so the name binds the class the layout describes: a module
    /// function `f(p)`, or the `__call__(self, p)` of an instance of a
    /// module class (`@Deco("c")`), never rebinding `p`, whose every path
    /// that a class reaches returns `p` -- all of its returns, or those under
    /// an `if isinstance(p, type):` that ends in one. What the decorator
    /// does to the class besides (`p.__init__ = f`) is a store on a class
    /// like any other, which the program's facts already account for.
    boolean returnsItsClass(List<Stmt> body, ClassDef c) {
        if (fixedGlobal == null) {
            return false;
        }
        for (Expr d : c.decorators) {
            FunctionDef f = null;
            int param = 0;
            if (d instanceof Name && fixedGlobal.test(((Name) d).id)) {
                f = moduleDef(body, ((Name) d).id);
            } else if (d instanceof Call && ((Call) d).func instanceof Name
                    && fixedGlobal.test(((Name) ((Call) d).func).id)) {
                for (Stmt s : body) {
                    if (s instanceof ClassDef && ((ClassDef) s).name.equals(((Name) ((Call) d).func).id)
                            && ((ClassDef) s).decorators.isEmpty() && ((ClassDef) s).bases.isEmpty()) {
                        f = moduleDef(((ClassDef) s).body, "__call__");
                        param = 1;
                    }
                }
            }
            if (f == null || f.args.positional.size() != param + 1 || f.args.vararg != null
                    || !f.args.kwonly.isEmpty() || f.args.kwarg != null || !f.decorators.isEmpty() || f.isAsync) {
                return false;
            }
            String p = f.args.positional.get(param).name;
            if (stores(f.body, p) || !returnsParam(f.body, p)) {
                return false;
            }
        }
        return true;
    }

    private static FunctionDef moduleDef(List<Stmt> body, String name) {
        FunctionDef found = null;
        for (Stmt s : body) {
            if (s instanceof FunctionDef && ((FunctionDef) s).name.equals(name)) {
                if (found != null) {
                    return null;
                }
                found = (FunctionDef) s;
            }
        }
        return found;
    }

    /// Whether `block`, entered with a class in `p`, ends every path in
    /// `return p`.
    private static boolean returnsParam(List<Stmt> block, String p) {
        for (Stmt s : block) {
            if (s instanceof Return) {
                Expr v = ((Return) s).value;
                return v instanceof Name && ((Name) v).id.equals(p);
            }
            if (s instanceof If) {
                If i = (If) s;
                if (isTypeTest(i.test, p)) {
                    // A class never reaches the else branch nor what follows.
                    return returnsParam(i.body, p);
                }
                if (!noReturn(i.body) || !noReturn(i.orelse)) {
                    return false;
                }
            } else if (!noReturn(java.util.Collections.singletonList(s))) {
                return false;
            }
        }
        return false;
    }

    private static boolean isTypeTest(Expr t, String p) {
        if (!(t instanceof Call)) {
            return false;
        }
        Call c = (Call) t;
        return c.func instanceof Name && "isinstance".equals(((Name) c.func).id) && c.args.size() == 2
                && c.keywords.isEmpty() && c.args.get(0) instanceof Name && ((Name) c.args.get(0)).id.equals(p)
                && c.args.get(1) instanceof Name && "type".equals(((Name) c.args.get(1)).id);
    }

    /// Whether no `return`, `yield` or `raise`-free escape of the function
    /// itself is in `o` (nested functions and classes are their own).
    private static boolean noReturn(Object o) {
        if (o instanceof Return || o instanceof Yield || o instanceof Try) {
            return false;
        }
        if (o instanceof FunctionDef || o instanceof ClassDef || o instanceof Lambda) {
            return true;
        }
        boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= noReturn(x));
        return ok[0];
    }

    /// Whether `p` is stored anywhere in the function, nested scopes included.
    private static boolean stores(Object o, String p) {
        if (o instanceof Name) {
            return false;
        }
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (t instanceof Name && ((Name) t).id.equals(p)) {
                    return true;
                }
            }
        }
        if ((o instanceof AugAssign && ((AugAssign) o).target instanceof Name && ((Name) ((AugAssign) o).target).id.equals(p))
                || (o instanceof For && ((For) o).target instanceof Name && ((Name) ((For) o).target).id.equals(p))
                || o instanceof Global || o instanceof NamedExpr) {
            return true;
        }
        boolean[] hit = {false};
        Infer.children(o, x -> hit[0] |= stores(x, p));
        return hit[0];
    }

    static String text(Expr e) {
        if (e instanceof Name) {
            return ((Name) e).id;
        }
        if (e instanceof Attribute) {
            return text(((Attribute) e).value) + "." + ((Attribute) e).attr;
        }
        return e.getClass().getSimpleName();
    }

    static Layouts plan(List<Stmt> body, String prefix, Set<String> attrStores, Set<String> moduleAssigned) {
        return plan(body, prefix, attrStores, moduleAssigned, null);
    }

    static Layouts plan(List<Stmt> body, String prefix, Set<String> attrStores, Set<String> moduleAssigned,
            java.util.function.Function<Expr, Cls> foreignBase) {
        return plan(body, prefix, attrStores, moduleAssigned, foreignBase, java.util.Collections.<String>emptySet(),
                null, null, null);
    }

    /// `foreignBase` names the compiled class of another module a base
    /// expression is, or answers null; it is asked only for a base that is
    /// none of this module's classes. Such a class extends that class's
    /// Java class -- `BeautifulSoup(Tag)` in `bs4` over `bs4.element`'s
    /// `Tag` -- and is planned only when nothing it binds or stores changes
    /// what the base's fields and methods were planned as
    /// ([#extendsForeign]), since that plan is already made.
    ///
    /// `excluded` names this module's classes another module mixes in
    /// ([#foreignMixin]): they stay uncompiled, as a mixin of this module
    /// does. `foreignMixin` names such a class of another module that a base
    /// expression is, or answers null: a class mixing it in is planned with
    /// what it binds and stores kept out of its fields.
    static Layouts plan(List<Stmt> body, String prefix, Set<String> attrStores, Set<String> moduleAssigned,
            java.util.function.Function<Expr, Cls> foreignBase, Set<String> excluded,
            java.util.function.Function<Expr, ClassDef> foreignMixin, java.util.function.Predicate<String> constantGlobal,
            java.util.function.Predicate<String> fixedGlobal) {
        Layouts l = new Layouts();
        l.moduleAssigned = moduleAssigned;
        l.constantGlobal = constantGlobal;
        l.fixedGlobal = fixedGlobal;
        Map<String, Cls> byName = new LinkedHashMap<String, Cls>();
        Set<String> mixins = mixins(body);
        Set<String> superNames = new HashSet<String>();
        superAttrs(body, superNames);
        int n = 0;
        boolean bindsObject = moduleAssigned.contains("object");
        for (Stmt s : body) {
            if (!(s instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) s;
            Cls parent = null;
            // A class another class names as an extra base keeps its own
            // layout: that class copies its fields ([#mergeExtras]), and
            // `isinstance` never narrows to it (Infer#layoutHolds), since
            // the instances of that other class do not extend its Java class.
            // `boundary=` is EnumType's own keyword (`class Flag(Enum,
            // boundary=STRICT)`), consumed when the class is made; the class
            // must turn out to be an enum (checked below).
            boolean boundaryKw = false;
            ClassDef kc = c;
            for (Keyword kw : c.keywords) {
                boundaryKw |= "boundary".equals(kw.arg);
            }
            // Any other named keyword is handed to the metaclass and to
            // `__init_subclass__` when the class is made, which run as they
            // do for a class without keywords (PyType.finish); it shapes no
            // instance. A `**mapping` may hold `metaclass`, and stays.
            boolean namedKw = false;
            for (Keyword kw : c.keywords) {
                namedKw |= kw.arg != null && !"metaclass".equals(kw.arg);
            }
            if (boundaryKw || namedKw) {
                kc = new ClassDef(c.name, c.bases, new ArrayList<Keyword>(), c.body, c.decorators);
                for (Keyword kw : c.keywords) {
                    if (kw.arg == null || "metaclass".equals(kw.arg)) {
                        kc.keywords.add(kw);
                    }
                }
            }
            if (metaclassDef(body, c, moduleAssigned, byName)) {
                Cls mk = byName.get(((Name) c.bases.get(0)).id);
                Cls k = new Cls(c, prefix + "M" + c.line + "_" + Emitter.mangle(c.name),
                        "V" + c.line + "_" + Emitter.mangle(c.name), mk);
                k.compiled = false;
                k.metaclass = true;
                k.home = l;
                l.classes.put(c, k);
                byName.put(c.name, k);
                continue;
            }
            boolean enumKw = kc.keywords.size() == 1 && "metaclass".equals(kc.keywords.get(0).arg)
                    && enumTypeMeta(body, kc.keywords.get(0).value);
            boolean ok = (kc.keywords.isEmpty() || plainMetaclass(body, kc, moduleAssigned)) && (c.decorators.isEmpty() || c.dataclass || l.returnsItsClass(body, c));
            String why = ok ? null : c.decorators.isEmpty() ? keywordWhy(kc) : "it has class decorators";
            Set<String> extraNames = new HashSet<String>();
            List<Cls> extraCls = new ArrayList<Cls>();
            boolean extra = false;
            String builtin = ok && !c.bases.isEmpty() ? builtinBase(c.bases.get(0), moduleAssigned) : null;
            if (builtin == null && ok && !c.bases.isEmpty()) {
                builtin = typingAliasBase(body, c.bases.get(0));
            }
            String foreignWhy = null;
            List<Expr> after = new ArrayList<Expr>();
            for (int i = 1; builtin != null && i < c.bases.size(); i++) {
                if (typingBase(c.bases.get(i))) {
                    // `Generic[T]` adds nothing an instance has.
                    extra = true;
                } else {
                    after.add(origin(c.bases.get(i)));
                }
            }
            if (builtin != null && after.size() > 1) {
                ok = false;
                why = "more than one base after the builtin " + builtin;
            } else if (builtin != null && after.size() == 1) {
                // `class NavigableString(str, PageElement)`: the compiled
                // class is the Java parent, the builtin value the instance's
                // base; the MRO puts the builtin first, so none of the
                // chain's names may be one of the builtin's.
                Expr b = after.get(0);
                parent = b instanceof Name ? byName.get(((Name) b).id) : null;
                if (parent == null && foreignBase != null && !(b instanceof Name && moduleClassNamed(body, ((Name) b).id))) {
                    Cls fp = foreignBase.apply(b);
                    foreignWhy = fp == null ? null : extendsForeign(c, fp, l, byName, foreignBase);
                    parent = fp != null && foreignWhy == null ? fp : null;
                }
                String clash = parent == null ? null : builtinClash(parent, builtin, bodyBindings(c));
                if (parent == null) {
                    ok = false;
                    why = foreignWhy != null ? "it cannot extend " + text(b) + ": " + foreignWhy
                            : "base " + text(b) + " has no layout";
                } else if (parent.builtin != null) {
                    ok = false;
                    why = "base " + text(b) + " extends the builtin " + parent.builtin + " too";
                } else if (clash != null) {
                    ok = false;
                    why = "base " + text(b) + " binds " + clash + ", which the builtin " + builtin + " binds before it";
                }
            }
            List<String> excBases = new ArrayList<String>();
            String firstExc = builtin == null && ok && !c.bases.isEmpty() ? exceptionBase(c.bases.get(0), moduleAssigned) : null;
            if (firstExc != null) {
                excBases.add(firstExc);
            }
            for (int i = 1; ok && builtin == null && i < c.bases.size(); i++) {
                Expr b = origin(c.bases.get(i));
                String eb = exceptionBase(b, moduleAssigned);
                if (eb != null) {
                    // `class InvalidSchema(RequestException, ValueError)`:
                    // the type keeps the builtin, which the MRO puts after
                    // the compiled chain, for `except` and `isinstance`.
                    excBases.add(eb);
                    continue;
                }
                if (firstExc != null || !excBases.isEmpty()) {
                    ok = false;
                    why = "extra base " + text(b) + " beside a builtin exception";
                    break;
                }
                extra = true;
                Cls xb = b instanceof Name ? byName.get(((Name) b).id) : null;
                if (xb == null && foreignBase != null && !(b instanceof Name && moduleClassNamed(body, ((Name) b).id))) {
                    xb = foreignBase.apply(b);
                }
                if (xb != null && xb.compiled) {
                    // A compiled extra base: merged by the MRO below.
                    extraCls.add(xb);
                    continue;
                }
                ClassDef fm = b instanceof Name && moduleClassNamed(body, ((Name) b).id) || foreignMixin == null ? null
                        : foreignMixin.apply(b);
                if (b instanceof Name && mixins.contains(((Name) b).id)) {
                    extraNames.addAll(bodyBindings(classNamed(body, ((Name) b).id)));
                } else if (fm != null) {
                    // bs4's `BeautifulSoupHTMLParser(HTMLParser,
                    // DetectsXMLParsedAsHTML)`: the mixin's methods run as
                    // generic code over this instance, its stores of `self`
                    // landing in the instance's own attributes.
                    extraNames.addAll(bodyBindings(fm));
                    extraNames.addAll(selfStores(fm));
                } else {
                    ok = typingBase(b);
                    why = ok ? why : "extra base " + text(b);
                }
            }
            if (builtin == null && ok && !c.bases.isEmpty() && typingBase(c.bases.get(0))) {
                // `class Box(Generic[T])`: no compiled base, as for `object`.
                extra = true;
            } else if (firstExc != null && ok) {
                // `class UnsupportedOperation(OSError, ValueError)`: no
                // compiled parent, every base a builtin exception.
                parent = null;
            } else if (builtin == null && ok && !c.bases.isEmpty()) {
                Expr b = origin(c.bases.get(0));
                // `class X(object)` is a class with no base: the builtin adds
                // nothing a compiled layout must defer to. Only when the
                // module never rebinds the name, which it then could.
                if (b instanceof Name && "object".equals(((Name) b).id) && !bindsObject) {
                    ok = true;
                } else {
                    parent = b instanceof Name ? byName.get(((Name) b).id) : null;
                    if (parent == null && foreignBase != null && !(b instanceof Name && moduleClassNamed(body, ((Name) b).id))) {
                        Cls fp = foreignBase.apply(b);
                        foreignWhy = fp == null ? null : extendsForeign(c, fp, l, byName, foreignBase);
                        parent = fp != null && foreignWhy == null ? fp : null;
                    }
                    ok = parent != null;
                    why = ok ? why : foreignWhy != null ? "it cannot extend " + text(b) + ": " + foreignWhy
                            : "base " + text(b) + " has no layout";
                }
            }
            if (ok && extra && !excBases.isEmpty()) {
                ok = false;
                why = "a builtin exception base beside another extra base";
            }
            for (String f : FORBIDDEN) {
                if (ok && c.scope.assigned.contains(f)) {
                    ok = false;
                    why = "its body binds " + f;
                }
            }
            if (ok && parent != null && !parent.compiled) {
                ok = false;
                why = "base " + parent.def.name + " has no layout";
            }
            List<Cls> merged = new ArrayList<Cls>();
            Set<String> inheritedNames = new HashSet<String>();
            Cls javaParent = null;
            if (ok && !extraCls.isEmpty()) {
                why = mergeExtras(bodyBindings(c), parent, extraCls, superNames, extraNames, merged, inheritedNames);
                ok = why == null;
                // `class _BaseSlidePlaceholder(_InheritsDimensions, Shape)`:
                // the first base is a mixin and a later one carries the
                // __init__ and the fields. The Java parent need not be the
                // first base: any base whose chain the C3 MRO keeps whole
                // can be it, the others merged by the same rules (their
                // names reached through the MRO, their fields copied). The
                // MRO itself, and so every lookup, is that of the source
                // order, which k.bases keeps.
                for (int xi = 0; !ok && parent != null && parent.builtin == null && !parent.exception
                        && parent.excBases.isEmpty() && xi < extraCls.size(); xi++) {
                    Cls alt = extraCls.get(xi);
                    if (alt.builtin != null || alt.exception || !alt.excBases.isEmpty() || !alt.compiled) {
                        continue;
                    }
                    List<Cls> rest = new ArrayList<Cls>();
                    rest.add(parent);
                    for (Cls e : extraCls) {
                        if (e != alt) { //NOPMD CompareObjectsWithEquals - identity is meant
                            rest.add(e);
                        }
                    }
                    Set<String> tryNames = new HashSet<String>(extraNames);
                    List<Cls> tryMerged = new ArrayList<Cls>();
                    Set<String> tryInherited = new HashSet<String>();
                    if (mergeExtrasOrdered(bodyBindings(c), alt, rest, srcOrder(parent, extraCls), superNames, tryNames,
                            tryMerged, tryInherited) == null) {
                        ok = true;
                        why = null;
                        javaParent = alt;
                        extraNames.addAll(tryNames);
                        merged.addAll(tryMerged);
                        inheritedNames.addAll(tryInherited);
                    }
                }
            }
            if (ok && boundaryKw && !enumKw && (parent == null || !parent.enumMeta)) {
                ok = false;
                why = "its class keyword boundary= is EnumType's, but the class is not an enum";
            }
            if (!ok) {
                l.refused.put(c, why);
                Cls k = new Cls(c, prefix + "A" + l.refused.size() + "_" + Emitter.mangle(c.name),
                        "U" + l.refused.size() + "_" + Emitter.mangle(c.name), analysedParent(c, body, byName, foreignBase));
                k.compiled = false;
                k.home = l;
                l.classes.put(c, k);
                byName.put(c.name, k);
                continue;
            }
            n++;
            Cls srcParent = parent;
            if (javaParent != null) {
                parent = javaParent;
            }
            Cls k = new Cls(c, prefix + "L" + n + "_" + Emitter.mangle(c.name), "T" + n + "_" + Emitter.mangle(c.name), parent);
            k.mixinNames.addAll(extraNames);
            if (srcParent != null) {
                k.bases.add(srcParent);
            }
            k.bases.addAll(extraCls);
            k.mixedFrom.addAll(merged);
            k.bodyNames.addAll(inheritedNames);
            k.mroNames.addAll(inheritedNames);
            k.extraBases = extra;
            k.builtin = builtin != null ? builtin : parent != null ? parent.builtin : null;
            k.builtinOwn = builtin != null;
            k.enumMeta = enumKw || parent != null && parent.enumMeta;
            k.excBases.addAll(excBases);
            k.exception = !excBases.isEmpty() || parent != null && parent.exception;
            k.metaCall = parent != null && parent.metaCall || kc.keywords.size() == 1
                    && kc.keywords.get(0).value instanceof Name
                    && metaclassCalls(body, ((Name) kc.keywords.get(0).value).id, 0);
            for (Cls x : extraCls) {
                k.metaCall |= x.metaCall;
            }
            if (!excBases.isEmpty()) {
                // After the class's own body (and its compiled chain) in the
                // MRO: what BaseException binds is found there, generically.
                k.bodyNames.addAll(EXCEPTION_NAMES);
            }
            if (builtin != null) {
                // Next in the MRO after the class's own body: every check of
                // what the chain binds sees the builtin's names.
                k.bodyNames.addAll(BUILTIN_NAMES.get(builtin));
            }
            k.home = l;
            l.classes.put(c, k);
            byName.put(c.name, k);
        }
        // A name a class body binds cannot be a field of that class, of any
        // class above it, or of any compiled class below it (whose instances
        // pass the `instanceof` of every class above). An unrelated class
        // binding the same name changes nothing: only the exact compiled
        // class gets the layout, so no instance of it reaches that body.
        // Module-wide, a property `name` on `LoggerAdapter` cost `Logger`
        // every field set after `name`, which then all lived in the shape.
        Map<Cls, Set<String>> related = new LinkedHashMap<Cls, Set<String>>();
        for (Cls k : l.classes.values()) {
            Set<String> r = new HashSet<String>();
            for (Cls c : l.classes.values()) {
                if ((c.compiled == k.compiled) && (ancestorOf(k, c) || ancestorOf(c, k))) {
                    r.addAll(c.bodyNames);
                    r.addAll(c.mixinNames);
                }
            }
            // A base of another module binds names in its body too.
            for (Cls a = k.parent; a != null; a = a.parent) {
                if (a.home != l) { //NOPMD CompareObjectsWithEquals - identity is meant
                    r.addAll(a.bodyNames);
                }
            }
            related.put(k, r);
        }
        // `tag = None` in a class body is a default, not a descriptor: a
        // constant has no `__set__` or `__get__`, so a store to the name on an
        // instance binds the instance's own attribute, which a field holds,
        // and an unset field falls back to the generic read, which finds the
        // class's value. Only a name that every class binds that way and
        // nothing else rebinds is freed.
        Set<String> constants = new HashSet<String>();
        Set<String> otherwise = new HashSet<String>();
        for (Cls k : l.classes.values()) {
            if (!k.compiled) {
                continue;
            }
            for (Stmt s : k.def.body) {
                if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                        && ((Assign) s).value instanceof Constant) {
                    constants.add(((Name) ((Assign) s).targets.get(0)).id);
                } else if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name
                        && ((AnnAssign) s).value instanceof Constant) {
                    // `known_xml: Optional[bool] = None`: the annotation
                    // binds nothing, the constant is the same default.
                    constants.add(((Name) ((AnnAssign) s).target).id);
                } else {
                    Set<String> b = new HashSet<String>();
                    if (s instanceof FunctionDef) {
                        b.add(((FunctionDef) s).name);
                    } else if (s instanceof ClassDef) {
                        b.add(((ClassDef) s).name);
                    } else if (s instanceof Assign) {
                        for (Expr t : ((Assign) s).targets) {
                            names(t, b);
                        }
                    } else if (s instanceof AugAssign) {
                        names(((AugAssign) s).target, b);
                    } else if (s instanceof AnnAssign) {
                        if (!bareAnnotation(s)) {
                            names(((AnnAssign) s).target, b);
                        }
                    } else if (!(s instanceof ExprStmt) && !(s instanceof Pass)) {
                        b.addAll(k.bodyNames);
                    }
                    otherwise.addAll(b);
                }
            }
        }
        constants.removeAll(otherwise);
        for (Set<String> r : related.values()) {
            r.removeAll(constants);
        }
        // A subclass in another module that binds a property (or any
        // decorated definition, or a descriptor call) over a field demotes
        // that field in its whole chain above: the base was planned first
        // and its compiled `self.raw` would read the slot past the property.
        // Only the names so bound leave the layout; every other field keeps
        // its slot. Keyed by simple class name, so a collision only costs a
        // fast path.
        Map<String, Set<String>> demoted = SUBCLASS_BINDINGS.get();
        if (demoted != null) {
            for (Map.Entry<Cls, Set<String>> e : related.entrySet()) {
                Set<String> d = demoted.get(e.getKey().def.name);
                if (d != null) {
                    e.getValue().addAll(d);
                }
            }
        }
        boolean noFields = false;
        for (String st : attrStores) {
            int dot = st.indexOf('.');
            Cls owner = byName.get(st.substring(0, dot));
            if (owner != null) {
                String a = st.substring(dot + 1);
                for (Map.Entry<Cls, Set<String>> e : related.entrySet()) {
                    if (ancestorOf(e.getKey(), owner) || ancestorOf(owner, e.getKey())) {
                        e.getValue().add(a);
                    }
                }
                // An attribute hook added from outside sees every access.
                noFields |= "*".equals(a) || FORBIDDEN.contains(a);
            }
        }
        for (Cls k : l.classes.values()) {
            Map<String, Integer> defined = new LinkedHashMap<String, Integer>();
            for (Stmt s : k.def.body) {
                if (s instanceof FunctionDef) {
                    String nm = ((FunctionDef) s).name;
                    defined.put(nm, defined.containsKey(nm) ? defined.get(nm) + 1 : 1);
                }
            }
            Set<String> rebound = new HashSet<String>();
            for (Stmt s : k.def.body) {
                if (s instanceof Assign) {
                    for (Expr t : ((Assign) s).targets) {
                        names(t, rebound);
                    }
                } else if (s instanceof AugAssign) {
                    names(((AugAssign) s).target, rebound);
                } else if (s instanceof AnnAssign) {
                    if (!bareAnnotation(s)) {
                        names(((AnnAssign) s).target, rebound);
                    }
                } else if (s instanceof For) {
                    names(((For) s).target, rebound);
                } else if (s instanceof ClassDef) {
                    rebound.add(((ClassDef) s).name);
                } else if (!(s instanceof FunctionDef) && !(s instanceof ExprStmt) && !(s instanceof Pass)) {
                    // Anything else might bind a method's name too.
                    rebound.addAll(k.bodyNames);
                }
            }
            Set<String> stored = new LinkedHashSet<String>();
            for (Stmt s : k.def.body) {
                if (s instanceof FunctionDef && !((FunctionDef) s).args.names().isEmpty()) {
                    selfStores(((FunctionDef) s).body, ((FunctionDef) s).args.names().get(0), stored);
                }
            }
            // A method the instance may shadow (`self.default = default`)
            // is looked up per call, here and in every class above.
            for (Cls a = k.parent; a != null; a = a.parent) {
                if (!k.compiled && a.compiled) {
                    // An analysed class's instances are not the compiled
                    // class's: they reach its methods generically.
                    break;
                }
                a.typable.keySet().removeAll(stored);
                a.methods.keySet().removeAll(stored);
                a.typable.keySet().removeAll(k.mroNames);
                a.methods.keySet().removeAll(k.mroNames);
            }
            for (Stmt s : k.def.body) {
                if (!(s instanceof FunctionDef)) {
                    continue;
                }
                FunctionDef f = (FunctionDef) s;
                List<String> names = f.args.names();
                if (names.isEmpty()) {
                    continue;
                }
                if (IMPLICIT_CLASS_LEVEL.contains(f.name)) {
                    // Takes the class, not an instance: no typed receiver,
                    // and no dispatch on an instance.
                    continue;
                }
                if (!decoratedAs(f, "staticmethod") && !decoratedAs(f, "classmethod")) {
                    l.receiverClass.put(f, k);
                }
                // Plain but for keyword-only parameters: still one fixed list
                // of parameters a call site can bind at build time.
                // A field of a base of another module (extendsForeign let
                // this def over it): an instance may hold its own value, so
                // the method is looked up per call.
                boolean inheritedField = k.parent != null && k.parent.allFields.contains(f.name);
                boolean plainKw = !stored.contains(f.name) && !inheritedField && f.decorators.isEmpty() && !f.isAsync
                        && f.args.vararg == null && f.args.kwarg == null && defined.get(f.name) == 1
                        && !rebound.contains(f.name) && !hasFree(f);
                boolean plain = plainKw && f.args.kwonly.isEmpty();
                if (plainKw && names.size() <= MAX_TYPED_PARAMS) {
                    // Keyword-only parameters follow the positional ones in
                    // the typed body's list; a call binds them at build time
                    // (Infer#staticArgs) or goes through the door.
                    k.typable.put(f.name, f);
                }
                if (plain && names.size() <= 4) {
                    k.methods.put(f.name, f);
                    l.dispatch.add(f.name + "/" + (names.size() - 1));
                    l.dispatchBy.computeIfAbsent(f.name + "/" + (names.size() - 1), x -> new HashSet<Cls>()).add(k);
                    l.staticNames.put(f, "M_" + k.javaName.substring(prefix.length()) + "_" + Emitter.mangle(f.name));
                    l.methodClass.put(f, k);
                }
                if (plainKw && names.size() <= 14 && !l.staticNames.containsKey(f)) {
                    // A static entry with every parameter its own Java one,
                    // for call sites that bind their arguments at build time
                    // (Emitter.boundInit). Not dispatched by arity: a
                    // positional call must not fill a keyword-only parameter.
                    l.staticNames.put(f, "D_" + k.javaName.substring(prefix.length()) + "_" + Emitter.mangle(f.name));
                    l.methodClass.put(f, k);
                    l.directEntries.add(f);
                }
            }
            if (k.parent != null) {
                k.allFields.addAll(k.parent.allFields);
            }
            for (String f : stored) {
                if (noFields || f.startsWith("__") && f.endsWith("__") || related.get(k).contains(f) || k.allFields.contains(f)) {
                    continue;
                }
                k.fields.add(f);
                k.allFields.add(f);
                List<Cls> o = l.fieldOwners.get(f);
                if (o == null) {
                    o = new ArrayList<Cls>();
                    l.fieldOwners.put(f, o);
                }
                o.add(k);
            }
            // Copied slots: the fields of every compiled class of the MRO
            // outside the compiled chain, which its methods (run over this
            // instance generically) store and read by name.
            for (Cls x : k.mixedFrom) {
                for (String f : x.allFields) {
                    if (noFields || related.get(k).contains(f) || k.allFields.contains(f)) {
                        continue;
                    }
                    k.fields.add(f);
                    k.allFields.add(f);
                    l.fieldOwners.computeIfAbsent(f, z -> new ArrayList<Cls>()).add(k);
                }
            }
        }
        l.partition();
        return l;
    }

    /// The class an analysed class `c` derives from, for the inference: its
    /// first base that is a class of this plan or a compiled class of
    /// another module, or null.
    private static Cls analysedParent(ClassDef c, List<Stmt> body, Map<String, Cls> byName,
            java.util.function.Function<Expr, Cls> foreignBase) {
        Expr b = null;
        for (int i = 0; b == null && i < c.bases.size(); i++) {
            b = typingBase(c.bases.get(i)) ? null : origin(c.bases.get(i));
        }
        if (b == null) {
            return null;
        }
        Cls p = b instanceof Name ? byName.get(((Name) b).id) : null;
        if (p == null && foreignBase != null && !(b instanceof Name && moduleClassNamed(body, ((Name) b).id))) {
            p = foreignBase.apply(b);
        }
        return p;
    }

    /// Moves every class not compiled from [#classes] to [#analysed], and
    /// drops what the emitter would generate for it: its methods' entries,
    /// its fields' owners, and its methods' arities.
    private void partition() {
        for (java.util.Iterator<Map.Entry<ClassDef, Cls>> it = classes.entrySet().iterator(); it.hasNext();) {
            Map.Entry<ClassDef, Cls> e = it.next();
            if (!e.getValue().compiled) {
                analysed.put(e.getKey(), e.getValue());
                it.remove();
            }
        }
        if (analysed.isEmpty()) {
            return;
        }
        Set<Cls> gone = new HashSet<Cls>(analysed.values());
        for (Cls k : gone) {
            for (Stmt s : k.def.body) {
                if (s instanceof FunctionDef) {
                    staticNames.remove(s);
                    methodClass.remove(s);
                    directEntries.remove(s);
                    receiverClass.remove(s);
                }
            }
        }
        for (java.util.Iterator<List<Cls>> it = fieldOwners.values().iterator(); it.hasNext();) {
            List<Cls> o = it.next();
            o.removeAll(gone);
            if (o.isEmpty()) {
                it.remove();
            }
        }
        for (Map.Entry<String, Set<Cls>> e : dispatchBy.entrySet()) {
            if (gone.containsAll(e.getValue())) {
                dispatch.remove(e.getKey());
            }
        }
    }

    /// Whether `a` is `k` or one of the compiled classes above it.
    private static boolean ancestorOf(Cls a, Cls k) {
        for (Cls c = k; c != null; c = c.parent) {
            if (c == a) { //NOPMD CompareObjectsWithEquals - identity is meant
                return true;
            }
        }
        return false;
    }

    /// Whether the method uses `super()` or `__class__`, the one way a method
    /// at the top of a module captures anything: its body becomes a static
    /// Java method, which can capture nothing.
    private static boolean hasFree(FunctionDef f) {
        return f.scope.used.contains("super") || f.scope.used.contains("__class__");
    }

    /// The attribute names `self.<name> = ...` stores in `body`, outside
    /// nested functions and classes.
    /// The name `object.__setattr__(self, "name", v)` stores, a plain store
    /// past the class's own `__setattr__` (a frozen dataclass's
    /// constructor); null for any other statement.
    private static String objectSetattr(Stmt s, String self) {
        if (!(s instanceof ExprStmt) || !(((ExprStmt) s).value instanceof Call)) {
            return null;
        }
        Call c = (Call) ((ExprStmt) s).value;
        if (!(c.func instanceof Attribute) || !"__setattr__".equals(((Attribute) c.func).attr)
                || !(((Attribute) c.func).value instanceof Name) || !"object".equals(((Name) ((Attribute) c.func).value).id)
                || c.args.size() != 3 || !c.keywords.isEmpty() || !(c.args.get(0) instanceof Name)
                || !((Name) c.args.get(0)).id.equals(self) || !(c.args.get(1) instanceof Constant)
                || ((Constant) c.args.get(1)).kind != Constant.Kind.STR) {
            return null;
        }
        return ((Constant) c.args.get(1)).text;
    }

    private static void selfStores(List<Stmt> body, String self, Set<String> out) {
        for (Stmt s : body) {
            String named = objectSetattr(s, self);
            if (named != null) {
                out.add(named);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    target(t, self, out);
                }
            } else if (s instanceof AugAssign) {
                target(((AugAssign) s).target, self, out);
            } else if (s instanceof AnnAssign) {
                target(((AnnAssign) s).target, self, out);
            } else if (s instanceof For) {
                target(((For) s).target, self, out);
                selfStores(((For) s).body, self, out);
                selfStores(((For) s).orelse, self, out);
            } else if (s instanceof While) {
                selfStores(((While) s).body, self, out);
                selfStores(((While) s).orelse, self, out);
            } else if (s instanceof If) {
                selfStores(((If) s).body, self, out);
                selfStores(((If) s).orelse, self, out);
            } else if (s instanceof With) {
                for (WithItem w : ((With) s).items) {
                    if (w.vars != null) {
                        target(w.vars, self, out);
                    }
                }
                selfStores(((With) s).body, self, out);
            } else if (s instanceof Try) {
                Try t = (Try) s;
                selfStores(t.body, self, out);
                for (ExceptHandler h : t.handlers) {
                    selfStores(h.body, self, out);
                }
                selfStores(t.orelse, self, out);
                selfStores(t.finalbody, self, out);
            }
        }
    }

    private static void names(Expr t, Set<String> out) {
        if (t instanceof Name) {
            out.add(((Name) t).id);
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                names(e, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                names(e, out);
            }
        } else if (t instanceof Starred) {
            names(((Starred) t).value, out);
        }
    }

    private static void target(Expr t, String self, Set<String> out) {
        if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            if (a.value instanceof Name && ((Name) a.value).id.equals(self)) {
                out.add(a.attr);
            }
        } else if (t instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) t).elts) {
                target(e, self, out);
            }
        } else if (t instanceof ListExpr) {
            for (Expr e : ((ListExpr) t).elts) {
                target(e, self, out);
            }
        } else if (t instanceof Starred) {
            target(((Starred) t).value, self, out);
        }
    }

    /// What `name` resolves to for class `k`: the class whose body binds it
    /// first along the compiled chain, or null when none does.
    static Cls resolver(Cls k, String name) {
        for (Cls c = k; c != null; c = c.parent) {
            if (c.bodyNames.contains(name)) {
                return c;
            }
        }
        return null;
    }

    /// The constant an instance of exactly `k` finds for `name` through its
    /// class: the nearest class body up the chain that binds the name must
    /// bind it once, as `name = <constant>`, and nothing else. Null when it
    /// binds it any other way, or no compiled class binds it at all (the
    /// name may then come from a builtin base, a property, a method).
    /// The value `k`'s own body binds `name` to, when it binds it exactly
    /// once, as a plain `name = value` and nothing else, and no other
    /// statement of the body could bind it unseen. Null otherwise.
    static Expr onceValue(Cls k, String name) {
        if (!k.bodyNames.contains(name)) {
            Map<String, Expr> t = TRAILING.get(k.def);
            return t == null ? null : t.get(name);
        }
        if (trailing(k.def).containsKey(name)) {
            // Bound in the body and again right after it.
            return null;
        }
        Expr found = null;
        int binds = 0;
        for (Stmt s : k.def.body) {
            Set<String> b = new HashSet<String>();
            if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    names(t, b);
                }
            } else if (s instanceof FunctionDef) {
                b.add(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                b.add(((ClassDef) s).name);
            } else if (s instanceof AugAssign) {
                names(((AugAssign) s).target, b);
            } else if (s instanceof AnnAssign) {
                names(((AnnAssign) s).target, b);
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass)) {
                return null;
            }
            if (!b.contains(name)) {
                continue;
            }
            binds++;
            Assign a = s instanceof Assign ? (Assign) s : null;
            found = a != null && a.targets.size() == 1 && a.targets.get(0) instanceof Name ? a.value : null;
        }
        return binds == 1 ? found : null;
    }

    /// Per class, the module-level `C.X = value` statements that directly
    /// follow its class statement (only such statements in between), for a
    /// name the class body does not bind and the run binds once: the class
    /// attributes defined right after the class, as the enumeration members
    /// EnumLowering writes are. Keyed by identity, filled by [#scanTrailing].
    private static final Map<ClassDef, Map<String, Expr>> TRAILING = java.util.Collections.synchronizedMap(
            new java.util.IdentityHashMap<ClassDef, Map<String, Expr>>());
    /// The statements [#TRAILING] holds: no other store of their name.
    private static final Set<Object> TRAILING_STORES = java.util.Collections.synchronizedSet(
            java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>()));

    /// Records the once-bound trailing class attributes of module `m`.
    static void scanTrailing(com.codename1.python.transpiler.ast.Node.Module m) { //NOPMD UnnecessaryFullyQualifiedName - java.lang.Module is ambiguous
        List<Stmt> body = m.body;
        for (int i = 0; i < body.size(); i++) {
            if (!(body.get(i) instanceof ClassDef)) {
                continue;
            }
            ClassDef c = (ClassDef) body.get(i);
            Map<String, Expr> found = new LinkedHashMap<String, Expr>();
            Map<String, Stmt> at = new HashMap<String, Stmt>();
            Set<String> twice = new HashSet<String>();
            for (int j = i + 1; j < body.size(); j++) {
                Stmt s = body.get(j);
                if (!(s instanceof Assign) || ((Assign) s).targets.size() != 1
                        || !(((Assign) s).targets.get(0) instanceof Attribute)) {
                    break;
                }
                Attribute t = (Attribute) ((Assign) s).targets.get(0);
                if (!(t.value instanceof Name) || !((Name) t.value).id.equals(c.name)) {
                    break;
                }
                if (found.containsKey(t.attr) || t.attr.startsWith("__")) {
                    twice.add(t.attr);
                }
                found.put(t.attr, ((Assign) s).value);
                at.put(t.attr, s);
            }
            for (String n : twice) {
                found.remove(n);
                at.remove(n);
            }
            if (!found.isEmpty()) {
                TRAILING.put(c, found);
                TRAILING_STORES.addAll(at.values());
            }
        }
    }

    /// Whether `s` is one of the trailing class attribute stores
    /// ([#scanTrailing]).
    static boolean trailingStore(Object s) {
        return TRAILING_STORES.contains(s);
    }

    /// The trailing class attributes of `c` ([#scanTrailing]), never null.
    static Map<String, Expr> trailing(ClassDef c) {
        Map<String, Expr> t = TRAILING.get(c);
        return t == null ? java.util.Collections.<String, Expr>emptyMap() : t;
    }

    static Constant classConstant(Cls k, String name) {
        Cls c = k;
        while (c != null && !c.bodyNames.contains(name)) {
            c = c.parent;
        }
        if (c == null) {
            return null;
        }
        Constant found = null;
        int binds = 0;
        for (Stmt s : c.def.body) {
            Set<String> b = new HashSet<String>();
            if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    names(t, b);
                }
            } else if (s instanceof FunctionDef) {
                b.add(((FunctionDef) s).name);
            } else if (s instanceof ClassDef) {
                b.add(((ClassDef) s).name);
            } else if (s instanceof AugAssign) {
                names(((AugAssign) s).target, b);
            } else if (s instanceof AnnAssign) {
                names(((AnnAssign) s).target, b);
            } else if (!(s instanceof ExprStmt) && !(s instanceof Pass)) {
                // A loop, an import, a with: what it binds is not read here.
                return null;
            }
            if (!b.contains(name)) {
                continue;
            }
            binds++;
            Assign a = s instanceof Assign ? (Assign) s : null;
            if (a != null && a.targets.size() == 1 && a.targets.get(0) instanceof Name && a.value instanceof Constant) {
                found = (Constant) a.value;
            }
        }
        return binds == 1 ? found : null;
    }

    /// Judges a class whose compiled parent is `parent` (or none) and whose
    /// other bases are the compiled classes `extras`: answers why it cannot
    /// take a layout, or null, having put the names the merged classes bind
    /// into `names` (kept out of the fields) and the classes of the C3 MRO
    /// outside the compiled chain into `merged`. Every name the chain binds
    /// must still resolve in the chain -- no merged class binds a name a
    /// chain class after it in the MRO binds -- so typed code, which
    /// resolves along the Java chain, agrees with the MRO; a merged class's
    /// own names are reached generically through the MRO, and its methods
    /// read its fields by name, which the subclass holds as copied slots.
    static String mergeExtras(Set<String> own, Cls parent, List<Cls> extras, Set<String> superNames, Set<String> names,
            List<Cls> merged, Set<String> inherited) {
        List<Cls> bs = new ArrayList<Cls>();
        if (parent != null) {
            bs.add(parent);
        }
        bs.addAll(extras);
        return mergeExtrasOrdered(own, parent, extras, bs, superNames, names, merged, inherited);
    }

    /// The bases of a class in source order: `first`, then `extras`.
    static List<Cls> srcOrder(Cls first, List<Cls> extras) {
        List<Cls> bs = new ArrayList<Cls>();
        bs.add(first);
        bs.addAll(extras);
        return bs;
    }

    /// [#mergeExtras] with the Java parent `parent` taken from anywhere in
    /// the source-ordered bases `bs`, whose C3 MRO is the one judged.
    static String mergeExtrasOrdered(Set<String> own, Cls parent, List<Cls> extras, List<Cls> bs, Set<String> superNames,
            Set<String> names, List<Cls> merged, Set<String> inherited) {
        List<Cls> mro = c3(bs);
        if (mro == null) {
            return "its bases have no consistent MRO";
        }
        Set<Cls> chain = new HashSet<Cls>();
        for (Cls a = parent; a != null; a = a.parent) {
            chain.add(a);
        }
        for (int i = 0; i < mro.size(); i++) {
            Cls x = mro.get(i);
            if (chain.contains(x)) {
                continue;
            }
            String xn = x.def.name;
            // requests' `JSONDecodeError(InvalidJSONError, json.JSONDecodeError)`:
            // an extra base made directly over builtin exceptions merges
            // into a chain that derives from a builtin exception too. An
            // exception instance keeps args and the like in its attribute
            // table whatever its builtin, so the extra builtins add nothing
            // to the layout; the type's MRO keeps them for `except` and
            // `isinstance`, and the runtime judges them the same way
            // (PyType.neutralExtraBases).
            boolean excMixin = parent != null && parent.exception && x.compiled && x.builtin == null && x.parent == null
                    && !x.excBases.isEmpty();
            if (!excMixin && (!x.compiled || x.builtin != null || x.exception || !x.excBases.isEmpty())) {
                return "extra base " + xn + " has a builtin base or no layout";
            }
            for (String nm : x.bodyNames) {
                if ("__setattr__".equals(nm) || "__getattribute__".equals(nm) || "__delattr__".equals(nm)
                        || "__getattr__".equals(nm) || "__slots__".equals(nm) && !emptySlots(x.def)) {
                    return "extra base " + xn + " binds " + nm;
                }
                // Shadowed: a class before x in the MRO binds the name (the
                // class itself, a chain class or an earlier merged class).
                boolean shadowed = own.contains(nm);
                for (int j = 0; j < i && !shadowed; j++) {
                    shadowed = mro.get(j).bodyNames.contains(nm);
                }
                // A name a super() call reads may be bound here too: a
                // super() call is resolved statically only where every
                // class below its own has one base (Infer.superTarget), and
                // this class has two, so every one that can reach x's
                // binding runs through the MRO of type(self). Likewise an
                // unshadowed __init__ is x's: the class then binds no def of
                // its own for it (inherited below), so construction takes
                // the MRO's. Only __new__, which makes the Java instance,
                // must come from the chain.
                if (!shadowed && "__new__".equals(nm) && !abstractIn(x.def, nm)) {
                    return "extra base " + xn + " binds " + nm + ", which no class before it in the MRO binds";
                }
                if (!shadowed && !"__slots__".equals(nm)) {
                    // The MRO finds x's binding: the class binds the name
                    // as far as typed code is concerned, with no def of
                    // its own, so every use of it on an instance goes
                    // through the MRO -- also where a chain class after x
                    // binds it, whose typed dispatch the class withdraws.
                    inherited.add(nm);
                }
            }
            names.addAll(x.bodyNames);
            merged.add(x);
        }
        return null;
    }

    /// The C3 linearization of a class with bases `bs`, the class itself
    /// left out, or null when the bases admit none.
    static List<Cls> c3(List<Cls> bs) {
        List<List<Cls>> seqs = new ArrayList<List<Cls>>();
        for (Cls b : bs) {
            List<Cls> m = new ArrayList<Cls>();
            m.add(b);
            List<Cls> up = c3(b.bases);
            if (up == null) {
                return null;
            }
            m.addAll(up);
            seqs.add(m);
        }
        seqs.add(new ArrayList<Cls>(bs));
        List<Cls> r = new ArrayList<Cls>();
        while (true) {
            Cls next = null;
            for (List<Cls> sq : seqs) {
                if (sq.isEmpty()) {
                    continue;
                }
                Cls h = sq.get(0);
                boolean inTail = false;
                for (List<Cls> o : seqs) {
                    if (o.indexOf(h) > 0) {
                        inTail = true;
                        break;
                    }
                }
                if (!inTail) {
                    next = h;
                    break;
                }
            }
            if (next == null) {
                for (List<Cls> sq : seqs) {
                    if (!sq.isEmpty()) {
                        return null;
                    }
                }
                return r;
            }
            r.add(next);
            for (List<Cls> sq : seqs) {
                if (!sq.isEmpty() && sq.get(0) == next) { //NOPMD CompareObjectsWithEquals - identity is meant
                    sq.remove(0);
                }
            }
        }
    }

    /// Whether `def` binds `__slots__` only to an empty tuple or list.
    private static boolean emptySlots(ClassDef def) {
        for (Stmt s : def.body) {
            if (s instanceof Assign && ((Assign) s).targets.size() == 1 && ((Assign) s).targets.get(0) instanceof Name
                    && "__slots__".equals(((Name) ((Assign) s).targets.get(0)).id)) {
                Expr v = ((Assign) s).value;
                if (!(v instanceof TupleExpr && ((TupleExpr) v).elts.isEmpty() || v instanceof ListExpr && ((ListExpr) v).elts.isEmpty())) {
                    return false;
                }
            }
        }
        return true;
    }

    /// Whether `def` defines `name` as an `@abstractmethod` (no instance
    /// reaches such a method: a concrete class overrides it earlier).
    private static boolean abstractIn(ClassDef def, String name) {
        for (Stmt s : def.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                boolean abs = false;
                for (Expr d : ((FunctionDef) s).decorators) {
                    String dn = d instanceof Name ? ((Name) d).id : d instanceof Attribute ? ((Attribute) d).attr : null;
                    abs |= "abstractmethod".equals(dn);
                }
                return abs;
            }
        }
        return false;
    }

    private static boolean decoratedAs(FunctionDef f, String name) {
        for (Expr d : f.decorators) {
            if (d instanceof Name && name.equals(((Name) d).id)) {
                return true;
            }
        }
        return false;
    }
}
