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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Which top-level statements of the program ever matter, before anything
/// else reads it: a module that is imported for one function keeps that
/// function and what it needs, not the module's every definition and every
/// import those definitions would have needed.
///
/// A top-level statement is *pure* when running it only binds names:
/// a `def` or `class` with no decorator that could register it somewhere,
/// an import of modules that are pure themselves, an assignment of a value
/// built from constants, names and the calls in [#PURE_CALLS]. Anything
/// else is *effectful* and runs whenever its module does. A pure statement
/// is kept only when a name it binds is live -- read by kept code of its
/// module, imported from it by kept code of another, or named by a string
/// where the module may look a name up. Liveness is followed whole-program:
/// the names an import binds lead to the names of the module imported, so
/// one live name of a package keeps the one import that binds it.
///
/// What can reach a module's names other than by reading them makes the
/// whole module live: the module used as a value (`vars(m)`, `m` passed
/// on), `getattr` on it with a computed name, `globals()`, a star import of
/// it, an import of it by a computed name, and the modules the Java runtime
/// reads (`copyreg`, `io`, `builtins`). Statements are kept or dropped
/// whole, and only at the top level of a module; a kept class keeps its
/// whole body (its methods are left to the inference's reachability).
public final class Liveness {
    /// Calls whose result is a value and nothing else.
    private static final Set<String> PURE_CALLS = new HashSet<String>(Arrays.asList(
            "TypeVar", "ParamSpec", "TypeVarTuple", "NewType", "namedtuple", "NamedTuple", "TypedDict",
            "getLogger", "compile", "frozenset", "set", "dict", "list", "tuple", "object", "field", "Field",
            "cast", "TypeAliasType", "partial", "ConfigDict", "PropertyInfo", "cached_property", "property",
            "staticmethod", "classmethod", "lru_cache", "str", "int", "float", "bool", "bytes", "Struct",
            "defaultdict", "OrderedDict", "deque", "ContextVar", "Lock", "RLock", "MappingProxyType",
            "frozendict", "Literal", "Union", "Optional", "Annotated", "Sentinel", "sentinel", "range",
            "Decimal", "timedelta", "datetime", "date", "time", "Path", "PurePath", "Enum", "IntEnum",
            "StrEnum", "Flag", "IntFlag", "auto", "Pattern", "escape", "join", "Event", "Condition",
            "Semaphore", "local", "WeakValueDictionary", "WeakKeyDictionary", "WeakSet", "count", "Counter",
            "ChainMap", "Template", "Formatter", "TypeAdapter", "Discriminator", "Tag", "Required",
            "NotRequired", "ReadOnly", "Unpack", "deprecated", "final", "dataclass", "make_dataclass",
            "getenv", "get", "format", "replace", "encode", "decode", "lower", "upper", "split", "strip"));
    /// Decorators that wrap or mark what they decorate and register it
    /// nowhere.
    private static final Set<String> PURE_DECORATORS = new HashSet<String>(Arrays.asList(
            "dataclass", "overload", "property", "_cn1_property", "staticmethod", "classmethod", "wraps", "lru_cache", "cache",
            "cached_property", "contextmanager", "asynccontextmanager", "runtime_checkable", "final",
            "override", "total_ordering", "abstractmethod", "deprecated", "unique", "dataclass_transform",
            "setter", "getter", "deleter", "no_type_check", "singledispatch", "functools", "typing",
            "typing_extensions", "model_validator", "field_validator", "validator", "root_validator",
            "computed_field", "field_serializer", "model_serializer", "type_check_only", "coroutine",
            "frozen", "define", "attrs", "attrib", "s", "contextlib", "abc"));
    /// The modules the Java runtime imports by name and reads names of.
    private static final String[] RUNTIME_MODULES = {"builtins", "copyreg", "io"};

    private static final class Mod {
        final String name;
        final boolean pkg;
        final List<Stmt> body;
        /// Name -> the top-level statements binding it.
        final Map<String, List<Stmt>> binders = new HashMap<String, List<Stmt>>();
        /// Names bound to a module by a top-level import: the import, and
        /// the alias it binds.
        final Map<String, List<Object[]>> importsOf = new HashMap<String, List<Object[]>>();
        boolean effects;
        boolean importsByName;
        final List<String> strings = new ArrayList<String>();
        final List<String> prefixes = new ArrayList<String>();
        /// `"Name": (".module", "Symbol")` and `"Name": ".module"`: the
        /// table a module-level `__getattr__` imports a name it does not
        /// bind from.
        final Map<String, List<String[]>> lazy = new HashMap<String, List<String[]>>();
        /// Holds the module once its strings were all taken as prefixes.
        /// Whether its globals are read only by the names of `__all__`
        /// (see #indexedByAll); null until asked.
        Boolean allIndexed;
        boolean allRead;
        final Set<String> prefixStrings = new HashSet<String>();

        Mod(String name, boolean pkg, List<Stmt> body) {
            this.name = name;
            this.pkg = pkg;
            this.body = body;
        }
    }

    private final Map<String, Mod> mods = new LinkedHashMap<String, Mod>();
    private final Set<Mod> live = java.util.Collections.newSetFromMap(new IdentityHashMap<Mod, Boolean>());
    private final Set<Mod> full = java.util.Collections.newSetFromMap(new IdentityHashMap<Mod, Boolean>());
    private final Map<Mod, Set<String>> names = new IdentityHashMap<Mod, Set<String>>();
    private final Set<Stmt> kept = java.util.Collections.newSetFromMap(new IdentityHashMap<Stmt, Boolean>());
    private final Deque<Runnable> work = new ArrayDeque<Runnable>();
    private final Map<Stmt, Mod> homeOf = new IdentityHashMap<Stmt, Mod>();
    private boolean distributionsRead;
    /// Every attribute name live code reads (and every identifier of its
    /// strings, and what the Java runtime looks up by name): a method of a
    /// kept class whose name is in none of them is never called.
    private final Set<String> attrs = new HashSet<String>();
    /// Method name -> the methods of kept classes waiting for it.
    private final Map<String, List<Object[]>> waiting = new HashMap<String, List<Object[]>>();
    private final Set<FunctionDef> members = java.util.Collections.newSetFromMap(new IdentityHashMap<FunctionDef, Boolean>());
    private final Set<ClassDef> classes = java.util.Collections.newSetFromMap(new IdentityHashMap<ClassDef, Boolean>());
    /// Whether methods are pruned: only when the runtime's names are known.
    private boolean pruneMembers;

    private Liveness() {
    }

    /// Cuts every module body after a top-level `import` of a module that
    /// cannot exist when the program runs: one whose top-level package is
    /// not in `known` (compiled into the program or written in Java) and
    /// is not named anywhere in the runtime. That import raises
    /// ModuleNotFoundError where it stands, so nothing after it ever runs;
    /// the import itself stays, and raises as it did. bs4's html5lib
    /// builder imports html5lib, which a program need not ship: bs4
    /// catches the error, and the module's classes were analysed for
    /// nothing. A program that adds to `sys.modules` itself could make
    /// such an import succeed, so a name is only taken as missing when no
    /// string of a module that writes there names it, and nothing is cut
    /// when one of
    /// those additions is keyed by anything but a name or a constant.
    public static void truncateDoomed(List<List<Stmt>> bodies, Set<String> known, Set<String> runtimeNames) {
        Set<String> strings = new HashSet<String>();
        for (List<Stmt> body : bodies) {
            boolean[] w = {false};
            boolean[] any = {false};
            for (Stmt s : body) {
                sysModulesWrite(s, w);
                anyModulesWrite(s, any);
            }
            if (w[0]) {
                return;
            }
            if (any[0]) {
                for (Stmt s : body) {
                    stringTokens(s, strings);
                }
            }
        }
        known = new HashSet<String>(known);
        known.addAll(strings);
        for (List<Stmt> body : bodies) {
            for (int i = 0; i < body.size(); i++) {
                Stmt s = body.get(i);
                boolean doomed = false;
                if (s instanceof Import) {
                    for (Alias a : ((Import) s).names) {
                        doomed |= missing(a.name, known, runtimeNames);
                    }
                } else if (s instanceof ImportFrom && ((ImportFrom) s).level == 0 && ((ImportFrom) s).module != null) {
                    doomed = missing(((ImportFrom) s).module, known, runtimeNames);
                }
                if (doomed) {
                    body.subList(i + 1, body.size()).clear();
                    break;
                }
            }
        }
    }

    private static boolean missing(String module, Set<String> known, Set<String> runtimeNames) {
        int dot = module.indexOf('.');
        String top = dot < 0 ? module : module.substring(0, dot);
        return !known.contains(top) && !runtimeNames.contains(top) && !"java".equals(top)
                && !"__future__".equals(top);
    }

    /// Sets `w[0]` when `o` stores into, deletes from or adds to some
    /// `x.modules` (taken as `sys.modules`; a plain read does not count).
    private static void sysModulesWrite(Object o, boolean[] w) {
        if (w[0]) {
            return;
        }
        List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                : o instanceof AugAssign ? java.util.Collections.singletonList(((AugAssign) o).target)
                : o instanceof Delete ? ((Delete) o).targets : java.util.Collections.<Expr>emptyList();
        for (Expr t : targets) {
            if (t instanceof Subscript && modulesAttr(((Subscript) t).value)
                    && !(((Subscript) t).index instanceof Name || ((Subscript) t).index instanceof Constant)) {
                w[0] = true;
                return;
            }
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && modulesAttr(((Attribute) ((Call) o).func).value)
                && ("update".equals(((Attribute) ((Call) o).func).attr)
                || "__setitem__".equals(((Attribute) ((Call) o).func).attr)
                || "setdefault".equals(((Attribute) ((Call) o).func).attr)
                && !((Call) o).args.isEmpty() && !(((Call) o).args.get(0) instanceof Name
                || ((Call) o).args.get(0) instanceof Constant))) {
            w[0] = true;
            return;
        }
        Infer.children(o, x -> sysModulesWrite(x, w));
    }

    /// Adds to `out` every top-level package name a string constant under
    /// `o` could stand for: the run of identifier characters up to its
    /// first dot.
    private static void stringTokens(Object o, Set<String> out) {
        if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            String t = ((Constant) o).text;
            int start = -1;
            for (int i = 0; i <= t.length(); i++) {
                char c = i < t.length() ? t.charAt(i) : ' ';
                boolean id = c == '_' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9';
                if (id && start < 0) {
                    start = i;
                } else if (!id && start >= 0) {
                    out.add(t.substring(start, i));
                    start = -1;
                }
            }
            return;
        }
        Infer.children(o, x -> stringTokens(x, out));
    }

    /// Sets `w[0]` when `o` writes `x.modules` in any way at all.
    private static void anyModulesWrite(Object o, boolean[] w) {
        if (w[0]) {
            return;
        }
        List<Expr> targets = o instanceof Assign ? ((Assign) o).targets
                : o instanceof Delete ? ((Delete) o).targets : java.util.Collections.<Expr>emptyList();
        for (Expr t : targets) {
            if (t instanceof Subscript && modulesAttr(((Subscript) t).value)) {
                w[0] = true;
                return;
            }
        }
        if (o instanceof Call && ((Call) o).func instanceof Attribute
                && modulesAttr(((Attribute) ((Call) o).func).value)) {
            w[0] = true;
            return;
        }
        Infer.children(o, x -> anyModulesWrite(x, w));
    }

    private static boolean modulesAttr(Expr e) {
        return e instanceof Attribute && "modules".equals(((Attribute) e).attr);
    }

    /// Prunes `trees` (module name to body, and whether it is a package)
    /// to what module `main` reaches, in place. Answers the modules that
    /// never run; their bodies are left alone.
    public static Set<String> prune(Map<String, List<Stmt>> bodies, Set<String> packages, String main,
            Collection<String> keep, Set<String> runtimeNames) {
        return prune(bodies, packages, java.util.Collections.singleton(main), keep, runtimeNames);
    }

    /// [#prune] from several roots, each kept whole: the application's own
    /// modules of a build with no main module (library mode). What they
    /// import is pruned as it is from a main.
    public static Set<String> prune(Map<String, List<Stmt>> bodies, Set<String> packages, Collection<String> roots,
            Collection<String> keep, Set<String> runtimeNames) {
        Liveness l = new Liveness();
        l.pruneMembers = runtimeNames != null;
        if (runtimeNames != null) {
            l.attrs.addAll(runtimeNames);
        }
        for (Map.Entry<String, List<Stmt>> e : bodies.entrySet()) {
            l.mods.put(e.getKey(), new Mod(e.getKey(), packages.contains(e.getKey()), e.getValue()));
        }
        for (Mod m : l.mods.values()) {
            for (Stmt s : m.body) {
                l.homeOf.put(s, m);
                l.bind(m, s, s);
            }
            if (m.binders.containsKey("__getattr__")) {
                for (Stmt s : m.body) {
                    lazyTables(s, m);
                }
            }
        }
        l.effects();
        for (String root : roots) {
            l.full(l.mods.get(root));
        }
        for (String r : RUNTIME_MODULES) {
            l.full(l.mods.get(r));
        }
        for (Mod m : l.mods.values()) {
            if (usesJava(m)) {
                // Its classes are named by the generated Java interop.
                l.full(m);
            }
        }
        for (String k : keep) {
            for (int dot = k.lastIndexOf('.'); dot > 0; dot = k.lastIndexOf('.', dot - 1)) {
                Mod m = l.mods.get(k.substring(0, dot));
                if (m != null) {
                    String rest = k.substring(dot + 1);
                    int d2 = rest.indexOf('.');
                    l.name(m, d2 < 0 ? rest : rest.substring(0, d2));
                    break;
                }
            }
        }
        l.drain();
        boolean more = true;
        while (more) {
            // Both run every round: each can make the other find more.
            more = l.entryPoints();
            more = l.trimImports(false) || more;
            l.drain();
        }
        l.trimImports(true);
        Set<String> dead = new HashSet<String>();
        for (Mod m : l.mods.values()) {
            if (!l.live.contains(m)) {
                dead.add(m.name);
            } else if (!l.full.contains(m)) {
                m.body.removeIf(s -> !l.kept.contains(s));
                l.replaceImports(m.body);
            }
        }
        if (l.pruneMembers) {
            for (ClassDef c : l.classes) {
                c.body.removeIf(s -> s instanceof FunctionDef && prunable((FunctionDef) s) && !l.members.contains(s));
                if (c.body.isEmpty()) {
                    c.body.add(new Pass());
                }
            }
        }
        return dead;
    }

    /// Reads the lazy export tables of a module with a `__getattr__` (the
    /// dict literals with string keys outside its functions).
    private static void lazyTables(Object o, Mod m) {
        if (o instanceof FunctionDef || o instanceof ClassDef) {
            return;
        }
        if (o instanceof DictExpr) {
            DictExpr d = (DictExpr) o;
            for (int i = 0; i < d.keys.size(); i++) {
                Expr k = d.keys.get(i);
                Expr v = d.values.get(i);
                if (!isStr(k)) {
                    continue;
                }
                String[] target = null;
                if (isStr(v)) {
                    target = new String[] {((Constant) v).text};
                } else if (v instanceof TupleExpr && !((TupleExpr) v).elts.isEmpty()) {
                    List<Expr> es = ((TupleExpr) v).elts;
                    target = new String[es.size()];
                    for (int j = 0; j < es.size(); j++) {
                        target[j] = isStr(es.get(j)) ? ((Constant) es.get(j)).text : null;
                    }
                }
                if (target != null && target[0] != null) {
                    List<String[]> l = m.lazy.get(((Constant) k).text);
                    if (l == null) {
                        l = new ArrayList<String[]>();
                        m.lazy.put(((Constant) k).text, l);
                    }
                    l.add(target);
                }
            }
        }
        Infer.children(o, c -> lazyTables(c, m));
    }

    /// Decorators a method may carry and still be found by its name only.
    private static final Set<String> BY_NAME = new HashSet<String>(Arrays.asList(
            "property", "_cn1_property", "cached_property", "staticmethod", "classmethod", "overload", "override",
            "abstractmethod", "setter", "getter", "deleter", "final", "wraps", "lru_cache", "cache",
            "contextmanager", "asynccontextmanager", "deprecated"));

    /// Whether a method is reached only through its name: no dunder (the
    /// runtime calls those by protocol), and no decorator that may hand it
    /// on (a validator, a registration).
    static boolean prunable(FunctionDef f) {
        if (f.name.startsWith("__") && f.name.endsWith("__")) {
            return false;
        }
        for (Expr d : f.decorators) {
            String last = lastName(d instanceof Call ? ((Call) d).func : d);
            if (last == null || !BY_NAME.contains(last)) {
                return false;
            }
        }
        return true;
    }

    /// Method name prefixes live code may match names against: what it
    /// passes to `startswith`, and the constant start of a name it formats
    /// (`"test"`, `"visit_" + kind`, `f"_assert_{t}"`).
    private final Set<String> namePrefixes = new HashSet<String>();

    private void prefix(String p) {
        String id = leadingIdentifier(p);
        if (id.length() < p.length() && p.charAt(id.length()) != '%' && p.charAt(id.length()) != '{') {
            // A character no identifier holds follows (`f"to-unicode-{n}"`):
            // nothing the string starts can be a method name. Only a `%`
            // directive or a `{` field can still add identifier characters.
            return;
        }
        if (id.length() < 2 || !namePrefixes.add(id)) {
            return;
        }
        for (String n : new ArrayList<String>(waiting.keySet())) {
            if (n.startsWith(id)) {
                wake(waiting.remove(n));
            }
        }
    }

    private static String heldName(Expr a) {
        return a instanceof Name ? ((Name) a).id : ((Attribute) a).attr;
    }

    /// Takes as prefixes the strings stored under `held`: `held = "test"`,
    /// `self.held = ...`, `held: str = ...`, a tuple of them included.
    private void storedStrings(Object o, String held) {
        if (o instanceof Assign) {
            for (Expr t : ((Assign) o).targets) {
                if (held.equals(boundName(t))) {
                    allStrings(((Assign) o).value);
                }
            }
        } else if (o instanceof AnnAssign && ((AnnAssign) o).value != null
                && held.equals(boundName(((AnnAssign) o).target))) {
            allStrings(((AnnAssign) o).value);
        }
        Infer.children(o, c -> storedStrings(c, held));
    }

    private static String boundName(Expr t) {
        return t instanceof Name ? ((Name) t).id : t instanceof Attribute ? ((Attribute) t).attr : null;
    }

    private void allStrings(Object o) {
        if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            prefix(((Constant) o).text);
        }
        Infer.children(o, this::allStrings);
    }

    private static String leadingIdentifier(String p) {
        int i = 0;
        while (i < p.length() && (p.charAt(i) == '_' || Character.isLetterOrDigit(p.charAt(i)))) {
            i++;
        }
        return p.substring(0, i);
    }

    private boolean byPrefix(String n) {
        for (String p : namePrefixes) {
            if (n.startsWith(p)) {
                return true;
            }
        }
        return false;
    }

    /// A method named bare in its own class body (`x = property(_get)`,
    /// `@_get.setter`) is read the way an attribute is. A name read
    /// anywhere else is a variable, and keeps no method of that name.
    private void bareNames(Object o) {
        if (o instanceof Name) {
            attr(((Name) o).id);
        } else if (!(o instanceof FunctionDef) && !(o instanceof ClassDef) && !(o instanceof Lambda)) {
            Infer.children(o, this::bareNames);
        }
    }

    private void attr(String n) {
        if (!attrs.add(n)) {
            return;
        }
        wake(waiting.remove(n));
    }

    private void wake(List<Object[]> ws) {
        if (ws == null) {
            return;
        }
        for (Object[] w : ws) {
            Mod m = (Mod) w[0];
            FunctionDef f = (FunctionDef) w[1];
            @SuppressWarnings("unchecked")
            Map<String, String> local = (Map<String, String>) w[2];
            if (members.add(f)) {
                work.add(() -> {
                    localImports(m, f, local, false);
                    visit(m, f, local);
                });
            }
        }
    }

    private static boolean usesJava(Mod m) {
        for (Stmt s : m.body) {
            if (s instanceof ImportFrom && ((ImportFrom) s).module != null
                    && ("java".equals(((ImportFrom) s).module) || ((ImportFrom) s).module.startsWith("java."))) {
                return true;
            }
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    if ("java".equals(a.name) || a.name.startsWith("java.")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /// Takes the names nothing reads out of each kept top-level
    /// `from m import ...`: the statement imports every name it lists, so
    /// a name left in must be one `m` keeps. One kept only for running its
    /// module keeps its first name. Answers whether a name became live,
    /// which needs another drain.
    private boolean trimImports(boolean apply) {
        boolean more = false;
        for (Mod m : mods.values()) {
            if (!live.contains(m) || full.contains(m)) {
                continue;
            }
            for (Stmt s : m.body) {
                if (kept.contains(s)) {
                    more |= trim(m, s, apply);
                }
            }
        }
        return more;
    }

    /// Imports kept only for running their module, as `import x as _`.
    private final Map<Stmt, Stmt> replacements = new IdentityHashMap<Stmt, Stmt>();

    private void replaceImports(List<Stmt> b) {
        for (int i = 0; i < b.size(); i++) {
            Stmt s = b.get(i);
            Stmt r = replacements.get(s);
            if (r != null) {
                b.set(i, r);
            } else if (s instanceof If) {
                replaceImports(((If) s).body);
                replaceImports(((If) s).orelse);
            } else if (s instanceof Try) {
                replaceImports(((Try) s).body);
                for (ExceptHandler h : ((Try) s).handlers) {
                    replaceImports(h.body);
                }
                replaceImports(((Try) s).orelse);
                replaceImports(((Try) s).finalbody);
            }
        }
    }

    /// With `apply` false only makes sure each import keeps a name, and
    /// answers whether that made one live; with it true, once liveness
    /// settled, takes the dead names out.
    private boolean trim(Mod m, Stmt s, boolean apply) {
        boolean more = false;
        if (s instanceof If) {
            for (Stmt x : ((If) s).body) {
                more |= trim(m, x, apply);
            }
            for (Stmt x : ((If) s).orelse) {
                more |= trim(m, x, apply);
            }
        } else if (s instanceof Try) {
            for (Stmt x : ((Try) s).body) {
                more |= trim(m, x, apply);
            }
            for (ExceptHandler h : ((Try) s).handlers) {
                for (Stmt x : h.body) {
                    more |= trim(m, x, apply);
                }
            }
            for (Stmt x : ((Try) s).orelse) {
                more |= trim(m, x, apply);
            }
        } else if (s instanceof ImportFrom && !"__future__".equals(((ImportFrom) s).module)) {
            ImportFrom f = (ImportFrom) s;
            Set<String> ns = names.get(m);
            List<Alias> keepNames = new ArrayList<Alias>();
            for (Alias a : f.names) {
                if ("*".equals(a.name) || ns.contains(a.asname != null ? a.asname : a.name)) {
                    keepNames.add(a);
                }
            }
            if (keepNames.isEmpty() && !f.names.isEmpty()) {
                String base = absolute(m, f.level, f.module);
                boolean submodule = base == null;
                for (Alias a : f.names) {
                    submodule |= base != null && mods.containsKey(base + "." + a.name);
                }
                if (submodule) {
                    // `from . import x` runs the submodule x: keep x.
                    Alias a = f.names.get(0);
                    name(m, a.asname != null ? a.asname : a.name);
                    return true;
                }
                if (apply) {
                    // Kept only to run the module: binding a name of it
                    // would keep that name's definition too.
                    Import i = new Import(new ArrayList<Alias>(java.util.Collections.singletonList(
                            new Alias(base, "_cn1_imported"))));
                    i.line = f.line;
                    replacements.put(f, i);
                }
                return more;
            }
            if (apply) {
                f.names.retainAll(keepNames);
            }
        }
        return more;
    }

    // ---- what each statement binds -----------------------------------

    /// Records the names top-level statement `top` binds through `s`
    /// (itself, or a statement nested in its `if`/`try` blocks).
    private void bind(Mod m, Stmt top, Stmt s) {
        if (s instanceof FunctionDef) {
            binder(m, ((FunctionDef) s).name, top);
        } else if (s instanceof ClassDef) {
            binder(m, ((ClassDef) s).name, top);
        } else if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                targets(m, t, top);
            }
        } else if (s instanceof AnnAssign) {
            targets(m, ((AnnAssign) s).target, top);
        } else if (s instanceof AugAssign) {
            targets(m, ((AugAssign) s).target, top);
        } else if (s instanceof TypeAlias) {
            binder(m, ((TypeAlias) s).name, top);
        } else if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                String n = a.asname != null ? a.asname : first(a.name);
                binder(m, n, top);
                importOf(m, n, s, a);
            }
        } else if (s instanceof ImportFrom) {
            for (Alias a : ((ImportFrom) s).names) {
                if (!"*".equals(a.name)) {
                    String n = a.asname != null ? a.asname : a.name;
                    binder(m, n, top);
                    importOf(m, n, s, a);
                }
            }
        } else if (s instanceof If) {
            if (!typeChecking(((If) s).test)) {
                for (Stmt x : ((If) s).body) {
                    bind(m, top, x);
                }
            }
            for (Stmt x : ((If) s).orelse) {
                bind(m, top, x);
            }
        } else if (s instanceof Try) {
            Try t = (Try) s;
            for (Stmt x : t.body) {
                bind(m, top, x);
            }
            for (ExceptHandler h : t.handlers) {
                if (h.name != null) {
                    binder(m, h.name, top);
                }
                for (Stmt x : h.body) {
                    bind(m, top, x);
                }
            }
            for (Stmt x : t.orelse) {
                bind(m, top, x);
            }
            for (Stmt x : t.finalbody) {
                bind(m, top, x);
            }
        } else if (s instanceof For) {
            targets(m, ((For) s).target, top);
            for (Stmt x : ((For) s).body) {
                bind(m, top, x);
            }
        } else if (s instanceof With) {
            for (WithItem w : ((With) s).items) {
                if (w.vars != null) {
                    targets(m, w.vars, top);
                }
            }
            for (Stmt x : ((With) s).body) {
                bind(m, top, x);
            }
        } else if (s instanceof While) {
            for (Stmt x : ((While) s).body) {
                bind(m, top, x);
            }
        }
    }

    private void targets(Mod m, Expr t, Stmt top) {
        if (t instanceof Name) {
            binder(m, ((Name) t).id, top);
        } else if (t instanceof TupleExpr || t instanceof ListExpr || t instanceof Starred) {
            Infer.children(t, c -> {
                if (c instanceof Expr) {
                    targets(m, (Expr) c, top);
                }
            });
        }
    }

    private static void binder(Mod m, String n, Stmt top) {
        List<Stmt> l = m.binders.get(n);
        if (l == null) {
            l = new ArrayList<Stmt>();
            m.binders.put(n, l);
        }
        if (!l.contains(top)) {
            l.add(top);
        }
    }

    private static void importOf(Mod m, String n, Stmt s, Alias a) {
        List<Object[]> l = m.importsOf.get(n);
        if (l == null) {
            l = new ArrayList<Object[]>();
            m.importsOf.put(n, l);
        }
        l.add(new Object[] {s, a});
    }

    private static String first(String dotted) {
        int dot = dotted.indexOf('.');
        return dot < 0 ? dotted : dotted.substring(0, dot);
    }

    // ---- which statements have effects ---------------------------------

    /// Decides [Mod#effects] for every module: an effectful statement of
    /// its own, or an import of a module that has effects. Settles by
    /// iteration; effects only ever spread.
    private void effects() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Mod m : mods.values()) {
                if (m.effects) {
                    continue;
                }
                for (Stmt s : m.body) {
                    if (effectful(m, s)) {
                        m.effects = true;
                        changed = true;
                        break;
                    }
                }
            }
        }
    }

    private boolean effectful(Mod m, Stmt s) {
        pureIn = m;
        if (EntryPointTable.MODULE.equals(m.name) && EntryPointTable.registers(s) != null) {
            // Kept by #entryPoints once live code spells the group.
            return false;
        }
        // A raise or an assert at import time is an effect: a module that
        // raises ImportError is how a caller's fallback import is chosen.
        if (s instanceof Pass || s instanceof Global) {
            return false;
        }
        if (s instanceof ExprStmt) {
            // A docstring is kept regardless (see #keepAlways).
            return !(((ExprStmt) s).value instanceof Constant);
        }
        if (s instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) s;
            return !pureDecorators(f.decorators) || !pure(f.args) || f.returns != null && !pure(f.returns);
        }
        if (s instanceof ClassDef) {
            ClassDef c = (ClassDef) s;
            if (!pureDecorators(c.decorators) || !c.keywords.isEmpty()) {
                // A metaclass= (or any class keyword) runs code of its own.
                return true;
            }
            for (Expr b : c.bases) {
                if (!pure(b) || hooked().contains(lastName(b))) {
                    // A base's __init_subclass__ runs for every subclass made.
                    return true;
                }
            }
            for (Stmt st : c.body) {
                // Methods only build functions; anything else in the body
                // runs now (a print, a __set_name__ descriptor, a call).
                if (!(st instanceof FunctionDef) && effectful(m, st)) {
                    return true;
                }
            }
            return false;
        }
        if (s instanceof Assign) {
            for (Expr t : ((Assign) s).targets) {
                if (!plainTarget(t)) {
                    return true;
                }
            }
            return !pure(((Assign) s).value) && !pureInstance(m, ((Assign) s).value);
        }
        if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            return !plainTarget(a.target) || a.value != null && !pure(a.value) && !pureInstance(m, a.value);
        }
        if (s instanceof TypeAlias) {
            return false;
        }
        if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                for (String p = a.name;; p = p.substring(0, p.lastIndexOf('.'))) {
                    Mod t = mods.get(p);
                    if (t != null && t.effects) {
                        return true;
                    }
                    if (p.indexOf('.') < 0) {
                        break;
                    }
                }
            }
            return false;
        }
        if (s instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) s;
            if ("__future__".equals(f.module)) {
                return false;
            }
            String base = absolute(m, f.level, f.module);
            if (base == null) {
                return false;
            }
            for (String p = base;; p = p.substring(0, p.lastIndexOf('.'))) {
                Mod t = mods.get(p);
                if (t != null && t.effects) {
                    return true;
                }
                if (p.indexOf('.') < 0) {
                    break;
                }
            }
            for (Alias a : f.names) {
                Mod t = mods.get(base + "." + a.name);
                if (t != null && t.effects) {
                    return true;
                }
            }
            return false;
        }
        if (s instanceof If) {
            If i = (If) s;
            if (typeChecking(i.test)) {
                return effectful(m, i.orelse);
            }
            return !pure(i.test) || effectful(m, i.body) || effectful(m, i.orelse);
        }
        if (s instanceof Try) {
            Try t = (Try) s;
            if (effectful(m, t.body) || effectful(m, t.orelse) || effectful(m, t.finalbody)) {
                return true;
            }
            for (ExceptHandler h : t.handlers) {
                if (h.type != null && !pure(h.type) || effectful(m, h.body)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    private boolean effectful(Mod m, List<Stmt> b) {
        for (Stmt s : b) {
            if (effectful(m, s)) {
                return true;
            }
        }
        return false;
    }

    /// A class statement and the module it is in.
    private static final class Cls {
        final Mod mod;
        final ClassDef def;

        Cls(Mod mod, ClassDef def) {
            this.mod = mod;
            this.def = def;
        }
    }

    /// Bases a class may have and still be built without running code.
    private static final Set<String> PLAIN_BASES = new HashSet<String>(Arrays.asList(
            "object", "Generic", "ABC", "Protocol"));

    /// Whether `e` is `C()` or `C().m()` with constant-free pure arguments,
    /// where `C` is a class of the program that defines no `__init__`,
    /// `__new__` or metaclass anywhere in its bases, and `m` is a plain
    /// method whose body only returns a pure value. Building such an
    /// instance runs nothing of the program: openai's module-level
    /// `chat = ChatProxy().__as_proxied__()` is one.
    private boolean pureInstance(Mod m, Expr e) {
        if (!(e instanceof Call) || !pureArgs((Call) e)) {
            return false;
        }
        Call c = (Call) e;
        String meth = null;
        Expr ctor = c;
        if (c.func instanceof Attribute && ((Attribute) c.func).value instanceof Call) {
            meth = ((Attribute) c.func).attr;
            ctor = ((Attribute) c.func).value;
            if (!pureArgs((Call) ctor)) {
                return false;
            }
        }
        Expr f = ((Call) ctor).func;
        if (!(f instanceof Name)) {
            return false;
        }
        Cls cls = classOf(m, ((Name) f).id, 0);
        if (cls == null) {
            return false;
        }
        List<Cls> chain = new ArrayList<Cls>();
        if (!plainClass(cls, chain, 0)) {
            return false;
        }
        if (meth == null) {
            return true;
        }
        for (Cls k : chain) {
            for (Stmt x : k.def.body) {
                if (x instanceof FunctionDef && meth.equals(((FunctionDef) x).name)) {
                    return returnsOnly((FunctionDef) x);
                }
            }
        }
        return false;
    }

    private boolean returnsOnly(FunctionDef f) {
        if (f.isAsync || !f.decorators.isEmpty()) {
            return false;
        }
        List<Stmt> b = f.body;
        int i = !b.isEmpty() && b.get(0) instanceof ExprStmt && ((ExprStmt) b.get(0)).value instanceof Constant ? 1 : 0;
        return b.size() == i + 1 && b.get(i) instanceof Return && pure(((Return) b.get(i)).value);
    }

    /// Collects `cls` and its program bases, depth first, into `chain`;
    /// false when building one could run code.
    private boolean plainClass(Cls cls, List<Cls> chain, int depth) {
        ClassDef d = cls.def;
        if (depth > 16 || !d.decorators.isEmpty() || !d.keywords.isEmpty()) {
            return false;
        }
        for (Stmt x : d.body) {
            if (x instanceof FunctionDef) {
                String n = ((FunctionDef) x).name;
                if ("__init__".equals(n) || "__new__".equals(n) || "__post_init__".equals(n)) {
                    return false;
                }
            }
        }
        chain.add(cls);
        for (Expr b : d.bases) {
            Expr base = b instanceof Subscript ? ((Subscript) b).value : b;
            String last = lastName(base);
            if (last != null && PLAIN_BASES.contains(last)) {
                continue;
            }
            if (!(base instanceof Name)) {
                return false;
            }
            Cls k = classOf(cls.mod, ((Name) base).id, 0);
            if (k == null || !plainClass(k, chain, depth + 1)) {
                return false;
            }
        }
        return true;
    }

    /// The module and class statement that `n` names in `m`, following
    /// `from x import n` into the program; null for anything else.
    private Cls classOf(Mod m, String n, int depth) {
        List<Stmt> bs = m.binders.get(n);
        if (bs == null || bs.size() != 1 || depth > 8) {
            return null;
        }
        Stmt s = bs.get(0);
        if (s instanceof ClassDef && n.equals(((ClassDef) s).name)) {
            return new Cls(m, (ClassDef) s);
        }
        if (s instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) s;
            String base = absolute(m, f.level, f.module);
            Mod t = base == null ? null : mods.get(base);
            if (t == null) {
                return null;
            }
            for (Alias a : f.names) {
                if (n.equals(a.asname != null ? a.asname : a.name)) {
                    return classOf(t, a.name, depth + 1);
                }
            }
        }
        return null;
    }

    private static boolean plainTarget(Expr t) {
        if (t instanceof Name) {
            return true;
        }
        if (t instanceof TupleExpr || t instanceof ListExpr) {
            final boolean[] ok = {true};
            Infer.children(t, c -> ok[0] &= c instanceof Expr && plainTarget((Expr) c));
            return ok[0];
        }
        return false;
    }

    private boolean pureDecorators(List<Expr> ds) {
        for (Expr d : ds) {
            Expr f = d instanceof Call ? ((Call) d).func : d;
            String last = lastName(f);
            if (last == null || !PURE_DECORATORS.contains(last) && !PURE_DECORATORS.contains(rootName(f))
                    || d instanceof Call && !pureArgs((Call) d)) {
                return false;
            }
        }
        return true;
    }

    /// The module whose statement [#pure] is judging.
    private Mod pureIn;
    /// Classes whose subclassing runs code; see [#hooked].
    private Set<String> hooked;

    /// Whether `name` is a function or class the judged module defines
    /// itself: a user `def get()` is not dict.get, whatever PURE_CALLS says.
    private boolean definedHere(String name) {
        List<Stmt> b = pureIn == null ? null : pureIn.binders.get(name);
        if (b != null) {
            for (Stmt st : b) {
                if (st instanceof FunctionDef || st instanceof ClassDef) {
                    return true;
                }
            }
        }
        return false;
    }

    /// Names of the program's classes whose subclasses run code when made:
    /// one defining `__init_subclass__`, made with a `metaclass=`, or below
    /// such a class.
    private Set<String> hooked() {
        if (hooked != null) {
            return hooked;
        }
        Set<String> out = new HashSet<String>();
        List<ClassDef> all = new ArrayList<ClassDef>();
        for (Mod m : mods.values()) {
            for (Stmt st : m.body) {
                if (st instanceof ClassDef) {
                    all.add((ClassDef) st);
                }
            }
        }
        for (ClassDef c : all) {
            if (!c.keywords.isEmpty()) {
                out.add(c.name);
            }
            for (Stmt st : c.body) {
                if (st instanceof FunctionDef && "__init_subclass__".equals(((FunctionDef) st).name)) {
                    out.add(c.name);
                }
            }
        }
        boolean grew = true;
        while (grew) {
            grew = false;
            for (ClassDef c : all) {
                if (out.contains(c.name)) {
                    continue;
                }
                for (Expr b : c.bases) {
                    if (out.contains(lastName(b))) {
                        out.add(c.name);
                        grew = true;
                        break;
                    }
                }
            }
        }
        hooked = out;
        return out;
    }

    private static String lastName(Expr e) {
        return e instanceof Name ? ((Name) e).id : e instanceof Attribute ? ((Attribute) e).attr : null;
    }

    private static String rootName(Expr e) {
        while (e instanceof Attribute) {
            e = ((Attribute) e).value;
        }
        return e instanceof Name ? ((Name) e).id : null;
    }

    private boolean pureArgs(Call c) {
        for (Expr a : c.args) {
            if (!pure(a)) {
                return false;
            }
        }
        for (Keyword k : c.keywords) {
            if (!pure(k.value)) {
                return false;
            }
        }
        return true;
    }

    /// Whether evaluating `o` only computes a value.
    private boolean pure(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof Lambda) {
            // Builds a function; its body runs when called.
            return true;
        }
        if (o instanceof Await || o instanceof Yield || o instanceof NamedExpr) {
            return false;
        }
        if (o instanceof Call) {
            Call c = (Call) o;
            String last = lastName(c.func);
            if (last == null || !PURE_CALLS.contains(last) || !pure(c.func) || definedHere(last)) {
                return false;
            }
            return pureArgs(c);
        }
        if (o instanceof Subscript && ((Subscript) o).index instanceof Constant
                && ((Constant) ((Subscript) o).index).kind != Constant.Kind.NONE) {
            // CONFIG["key"] raises KeyError at import when the key is absent;
            // a type subscript (Optional[int]) names a type, not a constant.
            return false;
        }
        final boolean[] ok = {true};
        Infer.children(o, c -> ok[0] &= pure(c));
        return ok[0];
    }

    /// Whether `test` is typing's `TYPE_CHECKING`, False when the program
    /// runs: what it guards binds and imports nothing.
    static boolean typeChecking(Expr test) {
        return test instanceof Name && "TYPE_CHECKING".equals(((Name) test).id)
                || test instanceof Attribute && "TYPE_CHECKING".equals(((Attribute) test).attr);
    }

    // ---- liveness -------------------------------------------------------

    private void drain() {
        while (!work.isEmpty()) {
            work.poll().run();
        }
    }

    private void module(Mod m) {
        if (m == null || !live.add(m)) {
            return;
        }
        // A package's __init__ runs before any module under it.
        int dot = m.name.lastIndexOf('.');
        if (dot > 0) {
            module(mods.get(m.name.substring(0, dot)));
        }
        names.put(m, new HashSet<String>());
        for (Stmt s : m.body) {
            if (keepAlways(m, s)) {
                keep(m, s);
            }
        }
        for (String n : m.binders.keySet()) {
            if (n.startsWith("__") && n.endsWith("__")) {
                // __all__, __getattr__, __version__: the import system and
                // the runtime read them by name.
                name(m, n);
            }
        }
        distributions(m);
    }

    private boolean keepAlways(Mod m, Stmt s) {
        if (s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant) {
            return true;
        }
        if (s instanceof ImportFrom && "__future__".equals(((ImportFrom) s).module)) {
            return true;
        }
        return effectful(m, s);
    }

    private void full(Mod m) {
        if (m == null || !full.add(m)) {
            return;
        }
        module(m);
        for (Stmt s : m.body) {
            keep(m, s);
        }
        for (String n : m.binders.keySet()) {
            name(m, n);
        }
    }

    private void name(Mod m, String n) {
        if (m == null) {
            return;
        }
        module(m);
        if (!names.get(m).add(n)) {
            return;
        }
        List<Stmt> bs = m.binders.get(n);
        if (bs != null) {
            for (Stmt s : bs) {
                keep(m, s);
            }
        }
        List<Object[]> is = m.importsOf.get(n);
        if (is != null) {
            for (Object[] i : is) {
                importAlias(m, (Stmt) i[0], (Alias) i[1]);
            }
        }
        if (bs == null) {
            List<String[]> lz = m.lazy.get(n);
            if (lz != null) {
                // Imported by the module's __getattr__ when read.
                for (String[] t : lz) {
                    Mod target = mods.get(relativeToken(m, t[0]));
                    module(target);
                    if (target != null && t.length > 1 && t[1] != null) {
                        name(target, t[1]);
                    }
                }
            }
        }
        // An attribute of a package may be its submodule, bound by
        // importing it.
        Mod sub = mods.get(m.name + "." + n);
        if (sub != null && !m.binders.containsKey(n)) {
            module(sub);
        }
    }

    /// What a top-level import binding live alias `a` reads.
    private void importAlias(Mod m, Stmt s, Alias a) {
        if (s instanceof Import) {
            modulePath(a.name);
            return;
        }
        ImportFrom f = (ImportFrom) s;
        String base = absolute(m, f.level, f.module);
        if (base == null) {
            return;
        }
        Mod b = mods.get(base);
        module(b);
        Mod sub = mods.get(base + "." + a.name);
        if (sub != null) {
            module(sub);
        }
        if (b != null) {
            name(b, a.name);
        }
    }

    private void modulePath(String dotted) {
        for (int i = dotted.indexOf('.'); ; i = dotted.indexOf('.', i + 1)) {
            module(mods.get(i < 0 ? dotted : dotted.substring(0, i)));
            if (i < 0) {
                return;
            }
        }
    }

    private void keep(Mod m, Stmt s) {
        if (!kept.add(s)) {
            return;
        }
        work.add(() -> reads(m, s));
    }

    /// Follows everything kept top-level statement `s` of `m` reads.
    private void reads(Mod m, Stmt s) {
        Map<String, String> local = new HashMap<String, String>();
        localImports(m, s, local, s instanceof Import || s instanceof ImportFrom);
        if (s instanceof Import || s instanceof ImportFrom) {
            // An import runs the module whether or not its names are
            // read; which names it imports follows from those that are
            // live (see #name).
            if (s instanceof Import) {
                for (Alias a : ((Import) s).names) {
                    modulePath(a.name);
                }
            } else {
                ImportFrom f = (ImportFrom) s;
                String base = absolute(m, f.level, f.module);
                if (base != null) {
                    modulePath(base);
                    for (Alias a : f.names) {
                        if ("*".equals(a.name)) {
                            star(base);
                        } else if (mods.containsKey(base + "." + a.name) && mods.get(base + "." + a.name).effects) {
                            module(mods.get(base + "." + a.name));
                        }
                    }
                }
            }
            return;
        }
        tableStrings = !(s instanceof FunctionDef) && !(s instanceof ClassDef)
                && (!m.lazy.isEmpty() || binds(s, "__all__"));
        try {
            visit(m, s, local);
        } finally {
            tableStrings = false;
        }
    }

    /// Set while the visit is in a module's `__all__`, or in the top-level
    /// code of a module that imports names lazily: those strings name the
    /// module's globals and submodules (`".evals"`, `"Evals"`), which
    /// #name and the lazy tables resolve, never attributes of an object.
    private boolean tableStrings;

    /// Whether every `globals()`/`locals()` of `m`'s top level is bound to a
    /// name read only as `ns[n]`, with `n` the variable of a loop over
    /// `__all__` (openai's loop setting each export's `__module__`), and
    /// nothing else of `m` calls either. Such a module reads its globals by
    /// the names of `__all__` and no others.
    private boolean indexedByAll(Mod m) {
        if (m.allIndexed == null) {
            Set<String> ns = new HashSet<String>();
            boolean ok = m.binders.containsKey("__all__");
            for (Stmt x : m.body) {
                if (x instanceof Assign && ((Assign) x).targets.size() == 1
                        && ((Assign) x).targets.get(0) instanceof Name && namespaceCall(((Assign) x).value)) {
                    ns.add(((Name) ((Assign) x).targets.get(0)).id);
                }
            }
            final boolean[] good = {ok && !ns.isEmpty()};
            for (Stmt x : m.body) {
                if (!good[0]) {
                    break;
                }
                if (x instanceof Assign && ((Assign) x).targets.size() == 1
                        && ((Assign) x).targets.get(0) instanceof Name
                        && ns.contains(((Name) ((Assign) x).targets.get(0)).id)
                        && namespaceCall(((Assign) x).value)) {
                    continue;
                }
                namespaceUses(x, ns, null, good);
            }
            m.allIndexed = good[0];
        }
        return m.allIndexed;
    }

    private static boolean namespaceCall(Expr e) {
        return e instanceof Call && ((Call) e).args.isEmpty() && ((Call) e).func instanceof Name
                && ("globals".equals(((Name) ((Call) e).func).id) || "locals".equals(((Name) ((Call) e).func).id));
    }

    /// Clears `good[0]` on a read of a name of `ns` other than `ns[v]`,
    /// `v` being `loopVar` (a loop over `__all__`), or on any other call of
    /// `globals`/`locals`.
    private static void namespaceUses(Object o, Set<String> ns, String loopVar, boolean[] good) {
        if (!good[0]) {
            return;
        }
        if (o instanceof FunctionDef && "__dir__".equals(((FunctionDef) o).name)) {
            // A module's __dir__ answers dir(module), which lists names
            // and reads none of them.
            return;
        }
        if (o instanceof Assign && ((Assign) o).targets.size() == 1
                && ((Assign) o).targets.get(0) instanceof Subscript
                && namespaceCall(((Subscript) ((Assign) o).targets.get(0)).value)) {
            // `globals()[name] = value` (a lazy __getattr__ caching what it
            // imported) stores a name and reads none.
            namespaceUses(((Subscript) ((Assign) o).targets.get(0)).index, ns, loopVar, good);
            namespaceUses(((Assign) o).value, ns, loopVar, good);
            return;
        }
        if (o instanceof For && ((For) o).iter instanceof Name && "__all__".equals(((Name) ((For) o).iter).id)
                && ((For) o).target instanceof Name) {
            String v = ((Name) ((For) o).target).id;
            for (Stmt x : ((For) o).body) {
                namespaceUses(x, ns, v, good);
            }
            for (Stmt x : ((For) o).orelse) {
                namespaceUses(x, ns, loopVar, good);
            }
            return;
        }
        if (o instanceof Subscript && ((Subscript) o).value instanceof Name
                && ns.contains(((Name) ((Subscript) o).value).id)) {
            if (loopVar == null || !(((Subscript) o).index instanceof Name)
                    || !loopVar.equals(((Name) ((Subscript) o).index).id)) {
                good[0] = false;
            }
            return;
        }
        if (o instanceof Name && (ns.contains(((Name) o).id) || "globals".equals(((Name) o).id)
                || "locals".equals(((Name) o).id) || "vars".equals(((Name) o).id))) {
            good[0] = false;
            return;
        }
        Infer.children(o, c -> namespaceUses(c, ns, loopVar, good));
    }

    /// Makes live every name the module's `__all__` lists.
    private void allNames(Mod m) {
        if (m.allRead) {
            return;
        }
        m.allRead = true;
        for (Stmt x : m.body) {
            if (binds(x, "__all__")) {
                Expr v = x instanceof Assign ? ((Assign) x).value
                        : x instanceof AugAssign ? ((AugAssign) x).value : ((AnnAssign) x).value;
                if (!(v instanceof ListExpr) && !(v instanceof TupleExpr)) {
                    full(m);
                    return;
                }
                Infer.children(v, c -> {
                    if (c instanceof Constant && ((Constant) c).kind == Constant.Kind.STR) {
                        name(m, ((Constant) c).text);
                    } else {
                        full(m);
                    }
                });
            }
        }
    }

    private static boolean binds(Stmt s, String n) {
        List<Expr> ts = s instanceof Assign ? ((Assign) s).targets
                : s instanceof AnnAssign ? java.util.Collections.singletonList(((AnnAssign) s).target)
                : s instanceof AugAssign ? java.util.Collections.singletonList(((AugAssign) s).target)
                : java.util.Collections.<Expr>emptyList();
        for (Expr t : ts) {
            if (t instanceof Name && n.equals(((Name) t).id)) {
                return true;
            }
        }
        return false;
    }

    private void star(String base) {
        // Binds what __all__ names, or every public name: and which of
        // them is read is not followed through the star.
        full(mods.get(base));
        if (mods.get(base) != null) {
            for (Mod x : mods.values()) {
                if (x.name.startsWith(base + ".") && x.name.indexOf('.', base.length() + 1) < 0
                        && mods.get(base).binders.get("__all__") != null) {
                    // __all__ may name a submodule, which the import runs.
                    module(x);
                }
            }
        }
    }

    /// Collects the module aliases imports nested in `s` bind (inside
    /// functions and classes), and runs those imports. A nested import is
    /// followed whole: it runs only if its function does, and then binds
    /// a name the function reads.
    private void localImports(Mod m, Object o, Map<String, String> local, boolean top) {
        if (o instanceof If && typeChecking(((If) o).test)) {
            for (Stmt x : ((If) o).orelse) {
                localImports(m, x, local, false);
            }
            return;
        }
        if (!top && o instanceof Import) {
            for (Alias a : ((Import) o).names) {
                modulePath(a.name);
                if (a.asname != null) {
                    local.put(a.asname, a.name);
                } else {
                    local.put(first(a.name), first(a.name));
                }
            }
            return;
        }
        if (!top && o instanceof ImportFrom) {
            ImportFrom f = (ImportFrom) o;
            String base = absolute(m, f.level, f.module);
            if (base == null || "__future__".equals(base)) {
                return;
            }
            modulePath(base);
            for (Alias a : f.names) {
                if ("*".equals(a.name)) {
                    star(base);
                    continue;
                }
                String n = a.asname != null ? a.asname : a.name;
                if (mods.containsKey(base + "." + a.name)) {
                    module(mods.get(base + "." + a.name));
                    local.put(n, base + "." + a.name);
                } else {
                    name(mods.get(base), a.name);
                    String r = moduleOf(mods.get(base), a.name, 0);
                    if (r != null) {
                        local.put(n, r);
                    }
                }
            }
            return;
        }
        if (o instanceof ClassDef && pruneMembers) {
            // A method that may be pruned runs its imports when it is
            // found live (#wake), not with its class.
            for (Stmt x : ((ClassDef) o).body) {
                if (x instanceof FunctionDef && prunable((FunctionDef) x)) {
                    for (Expr d : ((FunctionDef) x).decorators) {
                        localImports(m, d, local, false);
                    }
                } else {
                    localImports(m, x, local, false);
                }
            }
            for (Expr b : ((ClassDef) o).bases) {
                localImports(m, b, local, false);
            }
            return;
        }
        Infer.children(o, c -> localImports(m, c, local, false));
    }

    /// The module that name `n` of module `m` is bound to by a top-level
    /// import, through re-exports, or null.
    private String moduleOf(Mod m, String n, int depth) {
        if (m == null || depth > 12) {
            return null;
        }
        List<Object[]> is = m.importsOf.get(n);
        if (is == null) {
            return null;
        }
        for (Object[] i : is) {
            Stmt s = (Stmt) i[0];
            Alias a = (Alias) i[1];
            if (s instanceof Import) {
                return a.asname != null ? a.name : first(a.name);
            }
            ImportFrom f = (ImportFrom) s;
            String base = absolute(m, f.level, f.module);
            if (base == null) {
                continue;
            }
            if (mods.containsKey(base + "." + a.name)) {
                return base + "." + a.name;
            }
            String r = moduleOf(mods.get(base), a.name, depth + 1);
            if (r != null) {
                return r;
            }
        }
        return null;
    }

    private String aliasModule(Mod m, String n, Map<String, String> local) {
        if (local.containsKey(n)) {
            return local.get(n);
        }
        return moduleOf(m, n, 0);
    }

    private void visit(Mod m, Object o, Map<String, String> local) {
        if (tableStrings && (o instanceof FunctionDef || o instanceof Lambda || o instanceof ClassDef)) {
            // The body of a function runs on its own terms.
            tableStrings = false;
            try {
                visitNode(m, o, local);
            } finally {
                tableStrings = true;
            }
            return;
        }
        visitNode(m, o, local);
    }

    private void visitNode(Mod m, Object o, Map<String, String> local) {
        if (o instanceof If && typeChecking(((If) o).test)) {
            visit(m, ((If) o).test, local);
            for (Stmt x : ((If) o).orelse) {
                visit(m, x, local);
            }
            return;
        }
        if (o instanceof Import || o instanceof ImportFrom) {
            // Run in localImports; their names are read where used.
            return;
        }
        if (o instanceof ClassDef && pruneMembers) {
            ClassDef c = (ClassDef) o;
            classes.add(c);
            for (Expr d : c.decorators) {
                visit(m, d, local);
            }
            for (Expr b : c.bases) {
                visit(m, b, local);
            }
            for (Keyword k : c.keywords) {
                visit(m, k.value, local);
            }
            for (Stmt x : c.body) {
                if (x instanceof FunctionDef && prunable((FunctionDef) x) && !attrs.contains(((FunctionDef) x).name)
                        && !byPrefix(((FunctionDef) x).name)) {
                    List<Object[]> ws = waiting.get(((FunctionDef) x).name);
                    if (ws == null) {
                        ws = new ArrayList<Object[]>();
                        waiting.put(((FunctionDef) x).name, ws);
                    }
                    ws.add(new Object[] {m, x, local});
                } else {
                    if (x instanceof FunctionDef) {
                        members.add((FunctionDef) x);
                        if (prunable((FunctionDef) x)) {
                            localImports(m, x, local, false);
                        }
                        for (Expr d : ((FunctionDef) x).decorators) {
                            bareNames(d);
                        }
                    } else if (!(x instanceof ClassDef)) {
                        bareNames(x);
                    }
                    visit(m, x, local);
                }
            }
            return;
        }
        if (o instanceof Attribute) {
            List<String> chain = new ArrayList<String>();
            Expr e = (Expr) o;
            while (e instanceof Attribute) {
                chain.add(0, ((Attribute) e).attr);
                attr(((Attribute) e).attr);
                e = ((Attribute) e).value;
            }
            if (chain.contains("import_module") || chain.contains("load") && e instanceof Name
                    && "_cn1imp".equals(((Name) e).id)) {
                importsByName(m);
            }
            if (e instanceof Name) {
                String base = ((Name) e).id;
                String mod = aliasModule(m, base, local);
                if (mod != null && mods.containsKey(mod)) {
                    if (!local.containsKey(base)) {
                        name(m, base);
                    }
                    attributes(mod, chain);
                    return;
                }
            }
        }
        if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Name && ("getattr".equals(((Name) c.func).id) || "hasattr".equals(((Name) c.func).id))
                    && c.args.size() >= 2 && c.args.get(0) instanceof Name && c.args.get(1) instanceof Constant) {
                String base = ((Name) c.args.get(0)).id;
                String mod = aliasModule(m, base, local);
                if (mod != null && mods.containsKey(mod)) {
                    if (!local.containsKey(base)) {
                        name(m, base);
                    }
                    name(mods.get(mod), ((Constant) c.args.get(1)).text);
                    attr(((Constant) c.args.get(1)).text);
                    for (int i = 2; i < c.args.size(); i++) {
                        visit(m, c.args.get(i), local);
                    }
                    return;
                }
            }
            if (c.args.size() == 2 && isStr(c.args.get(0)) && isStr(c.args.get(1))) {
                String rel = ((Constant) c.args.get(0)).text;
                if (rel.startsWith(".")) {
                    String base = ((Constant) c.args.get(1)).text;
                    int level = 0;
                    while (level < rel.length() && rel.charAt(level) == '.') {
                        level++;
                    }
                    for (int i = 1; i < level && base.lastIndexOf('.') > 0; i++) {
                        base = base.substring(0, base.lastIndexOf('.'));
                    }
                    m.strings.add(level == rel.length() ? base : base + "." + rel.substring(level));
                }
            }
        }
        if (o instanceof Call && constantImport(m, (Call) o, local)) {
            return;
        }
        if (o instanceof Name) {
            String id = ((Name) o).id;
            if (importer(id)) {
                importsByName(m);
            }
            if (("globals".equals(id) || "locals".equals(id)) && indexedByAll(m)) {
                // Only ever `ns[name]` for the names of __all__.
                allNames(m);
            } else if ("globals".equals(id) || "vars".equals(id) || "locals".equals(id) || "_getframe".equals(id)
                    || "currentframe".equals(id)) {
                full(m);
            }
            String mod = aliasModule(m, id, local);
            if (mod != null && mods.containsKey(mod)) {
                // The module as a value: any of its names may be read.
                full(mods.get(mod));
            }
            if (!local.containsKey(id)) {
                name(m, id);
            }
            return;
        }
        if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            String t = ((Constant) o).text;
            m.strings.add(t);
            if (m.importsByName) {
                readStrings(m);
            }
            if (hasSpace(t) || tableStrings || t.startsWith(".")) {
                // Prose (a docstring, a message), a relative module path or
                // a table of the module's own names: it names no attribute.
                return;
            }
            // A forward reference, an annotation as a string, a name
            // looked up by getattr on the module: any identifier in the
            // string that the module binds.
            for (String tok : tokens(t)) {
                for (String part : tok.split("\\.")) {
                    attr(part);
                    if (m.binders.containsKey(part)) {
                        name(m, part);
                    }
                }
            }
            if (m.importsByName) {
                readStrings(m);
            }
            return;
        }
        if (o instanceof JoinedStr && !((JoinedStr) o).parts.isEmpty() && isStr(((JoinedStr) o).parts.get(0))) {
            m.prefixes.add(((Constant) ((JoinedStr) o).parts.get(0)).text);
            prefix(((Constant) ((JoinedStr) o).parts.get(0)).text);
        } else if (o instanceof BinOp && ("+".equals(((BinOp) o).op) || "%".equals(((BinOp) o).op))
                && isStr(((BinOp) o).left)) {
            m.prefixes.add(((Constant) ((BinOp) o).left).text);
            prefix(((Constant) ((BinOp) o).left).text);
        } else if (o instanceof Call && ((Call) o).func instanceof Attribute
                && "format".equals(((Attribute) ((Call) o).func).attr) && isStr(((Attribute) ((Call) o).func).value)) {
            // `"do_{}".format(op)`: pdfminer's interpreter finds its
            // operators this way.
            Constant fmt = (Constant) ((Attribute) ((Call) o).func).value;
            m.prefixes.add(fmt.text);
            prefix(fmt.text);
        } else if (o instanceof Call && ((Call) o).func instanceof Attribute
                && "startswith".equals(((Attribute) ((Call) o).func).attr) && !((Call) o).args.isEmpty()) {
            Expr a = ((Call) o).args.get(0);
            if (isStr(a)) {
                prefix(((Constant) a).text);
            } else if (a instanceof TupleExpr) {
                for (Expr x : ((TupleExpr) a).elts) {
                    if (isStr(x)) {
                        prefix(((Constant) x).text);
                    }
                }
            } else if ((a instanceof Name || a instanceof Attribute) && m.prefixStrings.add(heldName(a))) {
                // A prefix held in a variable (`self.testMethodPrefix`):
                // any string the module stores under that name may be it.
                // Every string of the module was once taken instead, and a
                // `v.startswith(p)` over a parameter made each constant of
                // pandas (`"str"`) a prefix that kept `structure_tree`. A
                // parameter is filled by the caller, whose own strings are
                // read where it calls.
                String held = heldName(a);
                for (Stmt st : m.body) {
                    storedStrings(st, held);
                }
            }
        }
        Infer.children(o, c -> visit(m, c, local));
    }

    /// Reads `mod.a.b...`: the first name that is not a submodule is the
    /// name read.
    private void attributes(String mod, List<String> chain) {
        String cur = mod;
        for (String a : chain) {
            if ("__dict__".equals(a)) {
                full(mods.get(cur));
                return;
            }
            Mod c = mods.get(cur);
            name(c, a);
            String sub = moduleOf(c, a, 0);
            if (sub == null && mods.containsKey(cur + "." + a)) {
                sub = cur + "." + a;
            }
            if (sub == null || !mods.containsKey(sub)) {
                return;
            }
            module(mods.get(sub));
            cur = sub;
        }
    }

    private static boolean hasSpace(String t) {
        for (int i = 0; i < t.length(); i++) {
            if (Character.isWhitespace(t.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /// `import_module("a.b")`, `import_module(".x", __name__)` or
    /// `import_module(".x", "pkg")`: imports the one module it names, so
    /// the module making the call does not import by computed names. The
    /// module comes back as a value, so all of it is live.
    private boolean constantImport(Mod m, Call c, Map<String, String> local) {
        if (!"import_module".equals(lastName(c.func)) || c.args.isEmpty() || c.args.size() > 2
                || !c.keywords.isEmpty() || !isStr(c.args.get(0))) {
            return false;
        }
        String target = ((Constant) c.args.get(0)).text;
        if (target.startsWith(".")) {
            if (c.args.size() != 2) {
                return false;
            }
            Expr anchor = c.args.get(1);
            String base;
            if (isStr(anchor)) {
                base = ((Constant) anchor).text;
            } else if (anchor instanceof Name && "__name__".equals(((Name) anchor).id)) {
                base = m.name;
            } else if (anchor instanceof Name && "__package__".equals(((Name) anchor).id)) {
                base = m.pkg ? m.name : m.name.substring(0, Math.max(0, m.name.lastIndexOf('.')));
            } else {
                return false;
            }
            int level = 0;
            while (level < target.length() && target.charAt(level) == '.') {
                level++;
            }
            for (int i = 1; i < level && base.lastIndexOf('.') > 0; i++) {
                base = base.substring(0, base.lastIndexOf('.'));
            }
            target = level == target.length() ? base : base + "." + target.substring(level);
        } else if (c.args.size() == 2 && !pure(c.args.get(1))) {
            return false;
        }
        Mod t = mods.get(target);
        if (t == null) {
            return false;
        }
        full(t);
        if (c.func instanceof Attribute) {
            // `importlib.import_module`: the module importlib is read.
            visit(m, ((Attribute) c.func).value, local);
        } else if (!local.containsKey(((Name) c.func).id)) {
            name(m, ((Name) c.func).id);
        }
        return true;
    }

    private static boolean importer(String n) {
        return "import_module".equals(n) || "__import__".equals(n) || "_cn1imp".equals(n);
    }

    private static boolean isStr(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.STR;
    }

    private void importsByName(Mod m) {
        if (!m.lazy.isEmpty()) {
            // What its __getattr__ imports by name is in its tables, read
            // a name at a time (see #name).
            return;
        }
        if (!m.importsByName) {
            m.importsByName = true;
            readStrings(m);
        }
    }

    private void distributions(Mod m) {
        if ("importlib.metadata".equals(m.name) && !distributionsRead) {
            distributionsRead = true;
            Mod d = mods.get("_cn1_distributions");
            if (d != null) {
                // The table is kept whole, but its strings are not read as
                // imports: they name every installed package and every
                // console script, and a program reaches a package's code
                // through its own imports. What the metadata hands out by
                // name is entry points, followed by #entryPoints.
                full(d);
                EntryPointScan scan = new EntryPointScan();
                for (Stmt x : d.body) {
                    scan.on(x);
                }
            }
        }
    }

    /// The groups whose EntryPointTable registration is kept.
    private final Set<String> keptGroups = new HashSet<String>();

    /// Group -> the `module` or `module:attr` targets of its entry points.
    private final Map<String, List<String>> entryGroups = new HashMap<String, List<String>>();

    /// Reads the `entry_points.txt` texts out of `_cn1_distributions`.
    private final class EntryPointScan implements Infer.Visit {
        @Override
        public void on(Object o) {
            if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
                String group = null;
                for (String line : ((Constant) o).text.split("\n")) {
                    String t = line.trim();
                    if (t.startsWith("[") && t.endsWith("]")) {
                        group = t.substring(1, t.length() - 1).trim();
                    } else if (group != null && t.indexOf('=') > 0) {
                        String target = t.substring(t.indexOf('=') + 1).trim();
                        int sp = target.indexOf(' ');
                        if (sp > 0) {
                            target = target.substring(0, sp);
                        }
                        List<String> l = entryGroups.get(group);
                        if (l == null) {
                            l = new ArrayList<String>();
                            entryGroups.put(group, l);
                        }
                        l.add(target);
                    }
                }
            } else {
                Infer.children(o, this);
            }
        }
    }

    /// Follows the entry points of every group whose name live code has
    /// spelled (pluggy's `load_setuptools_entrypoints("app")`); true when
    /// it reached something new.
    private boolean entryPoints() {
        boolean any = false;
        Mod table = mods.get(EntryPointTable.MODULE);
        if (table != null && live.contains(table)) {
            for (Stmt x : table.body) {
                String g = EntryPointTable.registers(x);
                if (g != null && attrs.contains(g) && keptGroups.add(g)) {
                    keep(table, x);
                    any = true;
                }
            }
        }
        for (java.util.Iterator<Map.Entry<String, List<String>>> it = entryGroups.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<String, List<String>> e = it.next();
            if (!attrs.contains(e.getKey())) {
                continue;
            }
            it.remove();
            any = true;
            for (String target : e.getValue()) {
                int colon = target.indexOf(':');
                String mod = colon < 0 ? target : target.substring(0, colon);
                Mod t = mods.get(mod);
                if (t == null) {
                    continue;
                }
                if (colon < 0) {
                    full(t);
                } else {
                    module(t);
                    String attr = target.substring(colon + 1);
                    int dot = attr.indexOf('.');
                    name(t, dot < 0 ? attr : attr.substring(0, dot));
                }
            }
        }
        return any;
    }

    /// What a module that imports by computed names may import: every
    /// module its kept strings name, and every module under the constant
    /// start of a formatted one. Such a module is reached by name, so all
    /// of its names are.
    private void readStrings(Mod m) {
        List<String> ss = new ArrayList<String>(m.strings);
        m.strings.clear();
        List<String> ps = new ArrayList<String>(m.prefixes);
        m.prefixes.clear();
        for (String t : ss) {
            for (String tok : tokens(t)) {
                String n = relativeToken(m, tok);
                if (n != null && mods.containsKey(n)) {
                    full(mods.get(n));
                }
            }
        }
        for (String t : ps) {
            List<String> toks = tokens(t);
            if (toks.isEmpty() || !t.endsWith(toks.get(toks.size() - 1))) {
                continue;
            }
            String n = relativeToken(m, toks.get(toks.size() - 1));
            if (n != null && n.indexOf('.') > 0) {
                for (Mod x : new ArrayList<Mod>(mods.values())) {
                    if (x.name.startsWith(n)) {
                        full(x);
                    }
                }
            }
        }
        if (!m.importsByName) {
            // Kept for when the module turns out to import by name.
            m.strings.addAll(ss);
            m.prefixes.addAll(ps);
        }
    }

    private String relativeToken(Mod m, String tok) {
        int level = 0;
        while (level < tok.length() && tok.charAt(level) == '.') {
            level++;
        }
        if (level == 0) {
            return tok;
        }
        String rest = tok.substring(level);
        return absolute(m, level, rest.isEmpty() ? null : rest);
    }

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

    /// The absolute name of `from <dots><module> import` in `m`, or null
    /// when it climbs out of the top-level package.
    private static String absolute(Mod m, int level, String module) {
        if (level == 0) {
            return module;
        }
        String base = m.name;
        int up = m.pkg ? level - 1 : level;
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
}
