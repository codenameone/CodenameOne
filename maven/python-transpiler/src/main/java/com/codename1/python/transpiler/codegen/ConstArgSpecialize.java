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
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Constant;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.parser.Lexer;
import com.codename1.python.transpiler.parser.Parser;
import com.codename1.python.transpiler.parser.Token;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Specializes a method on the string it is always called with.
///
/// python-pptx reaches the inherited dimensions of a placeholder through
///
/// ```python
/// def _effective_value(self, attr_name):
///     v = getattr(super(_InheritsDimensions, self), attr_name)
///     ...
///     return self._inherited_value(attr_name)
/// ```
///
/// called as `self._effective_value("height")` and so on: a computed
/// attribute name the strict build refuses, although every caller spells
/// the name out. When a method's parameter is the name argument of a
/// `getattr`/`setattr`/`hasattr` and every call of the method in the closed
/// program passes a string literal for it, the method is cloned once per
/// literal (`_effective_value__cn1_height`), the literal is written in for
/// the parameter, `getattr(x, "height")` becomes `x.height` (and a
/// `setattr` statement an assignment), every call is pointed at its clone
/// and the original, which nothing calls any more, is removed. The calls in
/// a clone are literal again, so the pass repeats until nothing changes and
/// `_inherited_value` is specialized on the next round.
///
/// It refuses anything it cannot prove: a method name defined twice, a
/// method referenced other than by being called, a call with keywords,
/// starred or missing arguments, a decorated method, a parameter the body
/// rebinds, and a method whose tree no longer matches its source text (an
/// earlier pass rewrote it), since the clone is made from that text.
/// No name-dispatch code is generated: each clone is plain code.
public final class ConstArgSpecialize {

    private static final int MAX_ROUNDS = 256;
    private static final String SUFFIX = "__cn1_";

    private ConstArgSpecialize() {
    }

    /// A method, its class body and the text it was made from.
    private static final class Method {
        final String path;
        final ClassDef owner;
        final FunctionDef fn;

        Method(String path, ClassDef owner, FunctionDef fn) {
            this.path = path;
            this.owner = owner;
            this.fn = fn;
        }
    }

    /// Source text of a function, dedented, and the line it starts on.
    public static final class Text {
        final String src;
        final int line;

        Text(String src, int line) {
            this.src = src;
            this.line = line;
        }
    }

    /// Specializes every qualifying method among `trees`; `sources` holds
    /// the text each tree was parsed from, under the same keys.
    /// Returns the text of every clone made, which LoopUnroll reads in
    /// place of the module source the clones are not in.
    public static Map<FunctionDef, Text> rewrite(Map<String, Node.Module> trees, Map<String, String> sources) {
        Map<FunctionDef, Text> made = new IdentityHashMap<FunctionDef, Text>();
        Set<String> refused = new LinkedHashSet<String>();
        for (int i = 0; i < MAX_ROUNDS; i++) {
            if (!round(trees, sources, made, refused)) {
                break;
            }
        }
        return made;
    }

    private static boolean round(Map<String, Node.Module> trees, Map<String, String> sources,
            Map<FunctionDef, Text> made, Set<String> refused) {
        Map<String, Integer> defCount = new HashMap<String, Integer>();
        Map<String, Method> methods = new HashMap<String, Method>();
        Map<String, List<Call>> calls = new HashMap<String, List<Call>>();
        Set<String> escaped = new LinkedHashSet<String>();
        IdentityHashMap<Object, Boolean> callFuncs = new IdentityHashMap<Object, Boolean>();
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            index(e.getKey(), e.getValue(), null, defCount, methods, calls, escaped, callFuncs);
        }
        for (Map.Entry<String, Method> e : methods.entrySet()) {
            String name = e.getKey();
            if (refused.contains(name) || escaped.contains(name) || defCount.get(name).intValue() != 1) {
                continue;
            }
            List<Call> cs = calls.get(name);
            if (cs == null || cs.isEmpty()) {
                continue;
            }
            if (specialize(e.getValue(), cs, defCount, sources, made)
                    || specializeSeq(trees, e.getValue(), cs, defCount, sources, made)) {
                // The clones' calls are literal: what was refused may pass now.
                refused.clear();
                return true;
            }
            refused.add(name);
        }
        return specializeModuleFns(trees, sources, made, refused);
    }

    /// The same specialization for a module-level function called by name
    /// in its own module (`re_engine_`'s `show(p, s, flags=0, how="search")`
    /// with `getattr(re.compile(p, flags), how)`): the name argument may be
    /// passed by position, by keyword or left to a string default, and is
    /// a literal identifier in every call. The function must be the only
    /// definition of its name in the program, be referenced only as the
    /// callee of such calls in its own module, and no scope may bind the
    /// name otherwise (a parameter, an import, a store), so every call is
    /// provably a call of it. No name-dispatch code is generated.
    private static boolean specializeModuleFns(Map<String, Node.Module> trees, Map<String, String> sources,
            Map<FunctionDef, Text> made, Set<String> refused) {
        final Map<String, Integer> defCount = new HashMap<String, Integer>();
        final Set<String> bad = new LinkedHashSet<String>();
        final Map<String, List<Call>> calls = new HashMap<String, List<Call>>();
        final Map<String, String> callPath = new HashMap<String, String>();
        for (final Map.Entry<String, Node.Module> e : trees.entrySet()) {
            final IdentityHashMap<Object, Boolean> callees = new IdentityHashMap<Object, Boolean>();
            scanFns(e.getKey(), e.getValue(), defCount, bad, calls, callPath, callees);
        }
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            List<Stmt> body = e.getValue().body;
            for (int i = 0; i < body.size(); i++) {
                if (!(body.get(i) instanceof FunctionDef)) {
                    continue;
                }
                FunctionDef f = (FunctionDef) body.get(i);
                String key = "fn:" + f.name;
                List<Call> cs = calls.get(f.name);
                if (refused.contains(key) || bad.contains(f.name) || defCount.get(f.name).intValue() != 1
                        || cs == null || cs.isEmpty() || !e.getKey().equals(callPath.get(f.name))) {
                    continue;
                }
                if (specializeFn(e.getKey(), body, i, f, cs, defCount, sources, made)) {
                    refused.clear();
                    return true;
                }
                refused.add(key);
            }
        }
        return false;
    }

    /// Maps each call that is the whole of an expression statement, in
    /// `body` and every statement list nested in it, to that list.
    static void stmtCalls(List<Stmt> body, Map<Call, List<Stmt>> out) {
        for (Stmt st : body) {
            if (st instanceof Node.ExprStmt && ((Node.ExprStmt) st).value instanceof Call) {
                out.put((Call) ((Node.ExprStmt) st).value, body);
            } else if (st instanceof Node.Assign && ((Node.Assign) st).value instanceof Call) {
                out.put((Call) ((Node.Assign) st).value, body);
            } else if (st instanceof Node.If) {
                stmtCalls(((Node.If) st).body, out);
                stmtCalls(((Node.If) st).orelse, out);
            } else if (st instanceof Node.For) {
                stmtCalls(((Node.For) st).body, out);
                stmtCalls(((Node.For) st).orelse, out);
            } else if (st instanceof Node.While) {
                stmtCalls(((Node.While) st).body, out);
                stmtCalls(((Node.While) st).orelse, out);
            } else if (st instanceof Node.With) {
                stmtCalls(((Node.With) st).body, out);
            } else if (st instanceof Node.Try) {
                stmtCalls(((Node.Try) st).body, out);
                for (Node.ExceptHandler h : ((Node.Try) st).handlers) {
                    stmtCalls(h.body, out);
                }
                stmtCalls(((Node.Try) st).orelse, out);
                stmtCalls(((Node.Try) st).finalbody, out);
            } else if (st instanceof FunctionDef) {
                stmtCalls(((FunctionDef) st).body, out);
            } else if (st instanceof ClassDef) {
                stmtCalls(((ClassDef) st).body, out);
            }
        }
    }

    private static void scanFns(final String path, Object o, final Map<String, Integer> defCount,
            final Set<String> bad, final Map<String, List<Call>> calls, final Map<String, String> callPath,
            final IdentityHashMap<Object, Boolean> callees) {
        if (o instanceof FunctionDef) {
            String n = ((FunctionDef) o).name;
            Integer c = defCount.get(n);
            defCount.put(n, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
        }
        if (o instanceof Node.Arg) {
            bad.add(((Node.Arg) o).name);
        } else if (o instanceof Call && ((Call) o).func instanceof Name) {
            Call c = (Call) o;
            String n = ((Name) c.func).id;
            callees.put(c.func, Boolean.TRUE);
            String prev = callPath.get(n);
            if (prev != null && !prev.equals(path)) {
                bad.add(n);
            }
            callPath.put(n, path);
            List<Call> l = calls.get(n);
            if (l == null) {
                l = new ArrayList<Call>();
                calls.put(n, l);
            }
            l.add(c);
        } else if (o instanceof Name && !callees.containsKey(o)) {
            bad.add(((Name) o).id);
        } else if (o instanceof Attribute) {
            bad.add(((Attribute) o).attr);
        } else if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            bad.add(((Constant) o).text);
        } else if (o instanceof Node.Alias) {
            bad.add(((Node.Alias) o).name);
            bad.add(((Node.Alias) o).asname);
        } else if (o instanceof Node.Global) {
            bad.addAll(((Node.Global) o).names);
        } else if (o instanceof ClassDef) {
            bad.add(((ClassDef) o).name);
        }
        Infer.children(o, x -> scanFns(path, x, defCount, bad, calls, callPath, callees));
    }

    private static boolean specializeFn(String path, List<Stmt> body, int at, FunctionDef f, List<Call> cs,
            Map<String, Integer> defCount, Map<String, String> sources, Map<FunctionDef, Text> made) {
        Node.Arguments a = f.args;
        int n = a.positional.size();
        if (!f.decorators.isEmpty() || f.isAsync || a.vararg != null || a.kwarg != null || !a.kwonly.isEmpty()
                || n < 2 || a.positionalOnly != 0) {
            return false;
        }
        int idx = -1;
        for (int i = 1; i < n && idx < 0; i++) {
            if (namesAttribute(f.body, a.positional.get(i).name)) {
                idx = i;
            }
        }
        if (idx < 0) {
            return false;
        }
        String p = a.positional.get(idx).name;
        int firstDefault = n - a.defaults.size();
        Set<String> lits = new LinkedHashSet<String>();
        List<String> litOf = new ArrayList<String>();
        for (Call c : cs) {
            for (Expr x : c.args) {
                if (x instanceof Node.Starred) {
                    return false;
                }
            }
            if (c.args.size() > n) {
                return false;
            }
            Expr lit = null;
            for (Node.Keyword k : c.keywords) {
                if (k.arg == null) {
                    return false;
                }
                if (k.arg.equals(p)) {
                    lit = k.value;
                }
            }
            if (idx < c.args.size()) {
                if (lit != null) {
                    return false;
                }
                lit = c.args.get(idx);
            } else if (lit == null && idx >= firstDefault) {
                lit = a.defaults.get(idx - firstDefault);
            }
            if (!(lit instanceof Constant) || ((Constant) lit).kind != Constant.Kind.STR
                    || !ident(((Constant) lit).text)) {
                return false;
            }
            lits.add(((Constant) lit).text);
            litOf.add(((Constant) lit).text);
        }
        for (String l : lits) {
            if (defCount.containsKey(f.name + SUFFIX + l)) {
                return false;
            }
        }
        if (rebindsDeep(f.body, p)) {
            return false;
        }
        Text t = made.get(f);
        if (t == null) {
            t = extract(sources.get(path), f);
            if (t == null || !dump(f).equals(dump(parseMethod(t.src, t.line, path)))) {
                return false;
            }
        }
        // A callee name cannot be renamed in place, so each call must be a
        // statement of its own, which is replaced whole.
        Map<Call, List<Stmt>> holder = new IdentityHashMap<Call, List<Stmt>>();
        stmtCalls(body, holder);
        for (Call c : cs) {
            if (!holder.containsKey(c)) {
                return false;
            }
        }
        List<FunctionDef> clones = new ArrayList<FunctionDef>();
        Map<String, String> cloneName = new HashMap<String, String>();
        for (String l : lits) {
            String src = substitute(t.src, path, f.name, f.name + SUFFIX + l, p, "\"" + l + "\"", true);
            if (src == null) {
                return false;
            }
            FunctionDef c = parseMethod(src, t.line, path);
            made.put(c, new Text(src, t.line));
            clones.add(c);
            cloneName.put(l, c.name);
        }
        for (int j = 0; j < cs.size(); j++) {
            Call c = cs.get(j);
            List<Stmt> l = holder.get(c);
            int si = -1;
            for (int q = 0; q < l.size() && si < 0; q++) {
                Stmt sq = l.get(q);
                if (sq instanceof Node.ExprStmt && ((Node.ExprStmt) sq).value == c //NOPMD CompareObjectsWithEquals - identity is meant
                        || sq instanceof Node.Assign && ((Node.Assign) sq).value == c) { //NOPMD CompareObjectsWithEquals - identity is meant
                    si = q;
                }
            }
            Call nc = new Call(new Name(cloneName.get(litOf.get(j))).at(c.func.line, c.func.col)).at(c.line, c.col);
            nc.args.addAll(c.args);
            nc.keywords.addAll(c.keywords);
            Stmt old = l.get(si);
            Stmt rep = old instanceof Node.Assign ? new Node.Assign(((Node.Assign) old).targets, nc)
                    : new Node.ExprStmt(nc);
            l.set(si, rep.at(old.line, old.col));
            c = nc;
            if (idx < c.args.size()) {
                c.args.remove(idx);
            }
            for (int k = c.keywords.size() - 1; k >= 0; k--) {
                if (p.equals(c.keywords.get(k).arg)) {
                    c.keywords.remove(k);
                }
            }
        }
        body.remove(at);
        body.addAll(at, clones);
        made.remove(f);
        return true;
    }

    private static void index(final String path, Object o, final ClassDef owner, final Map<String, Integer> defCount,
            final Map<String, Method> methods, final Map<String, List<Call>> calls, final Set<String> escaped,
            final IdentityHashMap<Object, Boolean> callFuncs) {
        if (o instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) o;
            Integer n = defCount.get(f.name);
            defCount.put(f.name, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
            if (owner != null) {
                methods.put(f.name, new Method(path, owner, f));
            }
        } else if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Attribute) {
                callFuncs.put(c.func, Boolean.TRUE);
                String n = ((Attribute) c.func).attr;
                List<Call> l = calls.get(n);
                if (l == null) {
                    l = new ArrayList<Call>();
                    calls.put(n, l);
                }
                l.add(c);
            }
        } else if (o instanceof Attribute) {
            if (!callFuncs.containsKey(o)) {
                escaped.add(((Attribute) o).attr);
            }
        } else if (o instanceof Name) {
            escaped.add(((Name) o).id);
        } else if (o instanceof Constant && ((Constant) o).kind == Constant.Kind.STR) {
            escaped.add(((Constant) o).text);
        } else if (o instanceof Node.Alias) {
            escaped.add(((Node.Alias) o).name);
            escaped.add(((Node.Alias) o).asname);
        }
        final ClassDef inner = o instanceof ClassDef ? (ClassDef) o : null;
        if (o instanceof ClassDef) {
            for (Stmt s : inner.body) {
                index(path, s, inner, defCount, methods, calls, escaped, callFuncs);
            }
            for (Expr b : inner.bases) {
                index(path, b, null, defCount, methods, calls, escaped, callFuncs);
            }
            for (Expr d : inner.decorators) {
                index(path, d, null, defCount, methods, calls, escaped, callFuncs);
            }
            for (Node.Keyword k : inner.keywords) {
                index(path, k, null, defCount, methods, calls, escaped, callFuncs);
            }
            return;
        }
        Infer.children(o, x -> index(path, x, null, defCount, methods, calls, escaped, callFuncs));
    }

    private static boolean specialize(Method m, List<Call> cs, Map<String, Integer> defCount,
            Map<String, String> sources, Map<FunctionDef, Text> made) {
        FunctionDef f = m.fn;
        Node.Arguments a = f.args;
        if (!f.decorators.isEmpty() || f.isAsync || a.vararg != null || a.kwarg != null || !a.kwonly.isEmpty()
                || !a.defaults.isEmpty() || a.positional.size() < 2) {
            return false;
        }
        int idx = -1;
        for (int i = 1; i < a.positional.size() && idx < 0; i++) {
            if (namesAttribute(f.body, a.positional.get(i).name)) {
                idx = i;
            }
        }
        if (idx < 0) {
            return false;
        }
        String p = a.positional.get(idx).name;
        Set<String> lits = new LinkedHashSet<String>();
        for (Call c : cs) {
            if (!c.keywords.isEmpty() || c.args.size() != a.positional.size() - 1) {
                return false;
            }
            for (Expr x : c.args) {
                if (x instanceof Node.Starred) {
                    return false;
                }
            }
            Expr lit = c.args.get(idx - 1);
            if (!(lit instanceof Constant) || ((Constant) lit).kind != Constant.Kind.STR
                    || !ident(((Constant) lit).text)) {
                return false;
            }
            lits.add(((Constant) lit).text);
        }
        for (String l : lits) {
            if (defCount.containsKey(f.name + SUFFIX + l)) {
                return false;
            }
        }
        if (rebinds(f.body, p)) {
            return false;
        }
        Text t = made.get(f);
        if (t == null) {
            t = extract(sources.get(m.path), f);
            if (t == null || !dump(f).equals(dump(parseMethod(t.src, t.line, m.path)))) {
                return false;
            }
        }
        List<FunctionDef> clones = new ArrayList<FunctionDef>();
        Map<String, String> cloneName = new HashMap<String, String>();
        for (String l : lits) {
            String src = substitute(t.src, m.path, f.name, f.name + SUFFIX + l, p, "\"" + l + "\"", false);
            if (src == null) {
                return false;
            }
            FunctionDef c = parseMethod(src, t.line, m.path);
            made.put(c, new Text(src, t.line));
            clones.add(c);
            cloneName.put(l, c.name);
        }
        for (Call c : cs) {
            String l = ((Constant) c.args.get(idx - 1)).text;
            ((Attribute) c.func).attr = cloneName.get(l);
            c.args.remove(idx - 1);
        }
        int at = indexOf(m.owner.body, f);
        m.owner.body.remove(at);
        m.owner.body.addAll(at, clones);
        made.remove(f);
        return true;
    }

    /// Specializes a method on a sequence of attribute names it is handed:
    ///
    /// ```python
    /// def flush_cache(self, properties=None):
    ///     props = self.cached_properties if properties is None else properties
    ///     for p in props:
    ///         if hasattr(self, p):
    ///             delattr(self, p)
    /// ```
    ///
    /// called as `self.flush_cache()` and `self.flush_cache(Container.cached_properties)`.
    /// When the last parameter defaults to `None`, is read only to be
    /// compared with `None` or as a branch of a conditional bound to a local
    /// that is only iterated, and every call either leaves it out or passes
    /// a sequence of identifiers LoopUnroll can name at build time (proving
    /// a list never mutated), the method is cloned per distinct argument
    /// with `None` or the tuple literal written in. LoopUnroll then folds
    /// the conditional and unrolls the loop into literal accesses.
    private static boolean specializeSeq(Map<String, Node.Module> trees, Method m, List<Call> cs,
            Map<String, Integer> defCount, Map<String, String> sources, Map<FunctionDef, Text> made) {
        FunctionDef f = m.fn;
        Node.Arguments a = f.args;
        int n = a.positional.size();
        if (!f.decorators.isEmpty() || f.isAsync || a.vararg != null || a.kwarg != null || !a.kwonly.isEmpty()
                || a.defaults.size() != 1 || n < 2 || !isNone(a.defaults.get(0)) || !computedAccess(f.body)) {
            return false;
        }
        String p = a.positional.get(n - 1).name;
        boolean[] used = {false};
        for (Stmt st : f.body) {
            if (!seqUses(f, st, p, used)) {
                return false;
            }
        }
        if (!used[0] || rebinds(f.body, p)) {
            return false;
        }
        Map<String, String> values = new java.util.LinkedHashMap<String, String>();
        List<String> keyOf = new ArrayList<String>();
        for (Call c : cs) {
            if (!c.keywords.isEmpty() || c.args.size() != n - 1 && c.args.size() != n - 2) {
                return false;
            }
            for (Expr x : c.args) {
                if (x instanceof Node.Starred) {
                    return false;
                }
            }
            String key;
            String text;
            if (c.args.size() == n - 2) {
                key = "None";
                text = "None";
            } else {
                String path = pathOf(trees, c);
                List<String> names = path == null ? null : LoopUnroll.staticSeq(trees, path, c.args.get(n - 2), cs);
                if (names == null) {
                    return false;
                }
                StringBuilder b = new StringBuilder("(");
                for (String nm : names) {
                    if (!ident(nm)) {
                        return false;
                    }
                    b.append('"').append(nm).append("\", ");
                }
                text = b.append(')').toString();
                key = text;
            }
            if (!values.containsKey(key)) {
                values.put(key, text);
            }
            keyOf.add(key);
        }
        Map<String, String> cloneName = new HashMap<String, String>();
        int i = 0;
        for (String k : values.keySet()) {
            String cn = f.name + SUFFIX + ("None".equals(k) ? "None" : "seq" + i++);
            if (defCount.containsKey(cn)) {
                return false;
            }
            cloneName.put(k, cn);
        }
        Text t = made.get(f);
        if (t == null) {
            t = extract(sources.get(m.path), f);
            if (t == null || !dump(f).equals(dump(parseMethod(t.src, t.line, m.path)))) {
                return false;
            }
        }
        List<FunctionDef> clones = new ArrayList<FunctionDef>();
        for (Map.Entry<String, String> e : values.entrySet()) {
            String src = substitute(t.src, m.path, f.name, cloneName.get(e.getKey()), p, e.getValue(), true);
            if (src == null) {
                return false;
            }
            FunctionDef c = parseMethod(src, t.line, m.path);
            made.put(c, new Text(src, t.line));
            clones.add(c);
        }
        for (int j = 0; j < cs.size(); j++) {
            Call c = cs.get(j);
            ((Attribute) c.func).attr = cloneName.get(keyOf.get(j));
            if (c.args.size() == n - 1) {
                c.args.remove(n - 2);
            }
        }
        int at = indexOf(m.owner.body, f);
        m.owner.body.remove(at);
        m.owner.body.addAll(at, clones);
        made.remove(f);
        return true;
    }

    private static boolean isNone(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.NONE;
    }

    /// True when `o` holds a getattr/setattr/hasattr/delattr whose name is
    /// a variable: what the specialization is for.
    private static boolean computedAccess(Object o) {
        if (o instanceof Call && ((Call) o).func instanceof Name && ((Call) o).args.size() >= 2
                && ((Call) o).args.get(1) instanceof Name) {
            String n = ((Name) ((Call) o).func).id;
            if ("getattr".equals(n) || "setattr".equals(n) || "hasattr".equals(n) || "delattr".equals(n)) {
                return true;
            }
        }
        final boolean[] found = {false};
        Infer.children(o, x -> found[0] |= !found[0] && computedAccess(x));
        return found[0];
    }

    /// Whether every read of `p` in `o` (a statement of `f`) is a comparison
    /// with `None` by identity, or sits in the conditional value of an
    /// assignment to a local of `f` that is read only as a loop's iterable.
    private static boolean seqUses(FunctionDef f, Object o, String p, boolean[] used) {
        if (o instanceof Node.Lambda || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Comp) {
            return !mentions(o, p);
        }
        if (o instanceof Name && p.equals(((Name) o).id)) {
            return false;
        }
        if (o instanceof Node.Compare && ((Node.Compare) o).ops.size() == 1
                && ("is".equals(((Node.Compare) o).ops.get(0)) || "is not".equals(((Node.Compare) o).ops.get(0)))) {
            Expr l = ((Node.Compare) o).left;
            Expr r = ((Node.Compare) o).comparators.get(0);
            if (l instanceof Name && p.equals(((Name) l).id) && isNone(r)
                    || r instanceof Name && p.equals(((Name) r).id) && isNone(l)) {
                used[0] = true;
                return true;
            }
        }
        if (o instanceof Node.Assign && ((Node.Assign) o).targets.size() == 1
                && ((Node.Assign) o).targets.get(0) instanceof Name
                && ((Node.Assign) o).value instanceof Node.IfExp
                && onlyIterated(f, ((Name) ((Node.Assign) o).targets.get(0)).id)) {
            Node.IfExp x = (Node.IfExp) ((Node.Assign) o).value;
            for (Expr b : new Expr[] {x.body, x.orelse}) {
                if (b instanceof Name && p.equals(((Name) b).id)) {
                    used[0] = true;
                } else if (!seqUses(f, b, p, used)) {
                    return false;
                }
            }
            return seqUses(f, x.test, p, used);
        }
        final boolean[] ok = {true};
        Infer.children(o, x -> ok[0] &= !ok[0] || seqUses(f, x, p, used));
        return ok[0];
    }

    /// Whether local `n` of `f` is bound once and read only as the
    /// iterable of one loop.
    private static boolean onlyIterated(FunctionDef f, final String n) {
        final int[] c = {0, 0};
        countUses(f, n, c);
        return c[0] == 2 && c[1] == 1;
    }

    private static void countUses(Object o, final String n, final int[] c) {
        if (o instanceof Name && n.equals(((Name) o).id)) {
            c[0]++;
        }
        if (o instanceof Node.For && ((Node.For) o).iter instanceof Name && n.equals(((Name) ((Node.For) o).iter).id)) {
            c[1]++;
        }
        if (o instanceof Node.Global && ((Node.Global) o).names.contains(n)) {
            c[0] += 100;
        }
        Infer.children(o, x -> countUses(x, n, c));
    }

    private static String pathOf(Map<String, Node.Module> trees, Call c) {
        for (Map.Entry<String, Node.Module> e : trees.entrySet()) {
            if (contains(e.getValue(), c)) {
                return e.getKey();
            }
        }
        return null;
    }

    private static boolean contains(Object o, final Object target) {
        if (o == target) { //NOPMD CompareObjectsWithEquals - identity is meant
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> found[0] |= !found[0] && contains(x, target));
        return found[0];
    }

    private static int indexOf(List<Stmt> body, Stmt s) {
        for (int i = 0; i < body.size(); i++) {
            if (body.get(i) == s) { //NOPMD CompareObjectsWithEquals - identity is meant
                return i;
            }
        }
        throw new IllegalStateException("method not in its class");
    }

    /// True when `p` is the name argument of a getattr/setattr/hasattr in `o`.
    private static boolean namesAttribute(Object o, final String p) {
        if (o instanceof Call) {
            Call c = (Call) o;
            if (c.func instanceof Name && c.args.size() >= 2 && c.args.get(1) instanceof Name
                    && p.equals(((Name) c.args.get(1)).id)) {
                String n = ((Name) c.func).id;
                if ("getattr".equals(n) || "setattr".equals(n) || "hasattr".equals(n)) {
                    return true;
                }
            }
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && namesAttribute(x, p)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// True when `o` might bind `p` or hide it in a nested scope.
    private static boolean rebinds(Object o, final String p) {
        if (o instanceof Node.Lambda || o instanceof FunctionDef || o instanceof ClassDef || o instanceof Node.Comp
                || o instanceof Node.Global || o instanceof Node.Match) {
            return true;
        }
        if (rebindsShallow(o, p)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && rebinds(x, p)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// [#rebinds] for a module function, which may hold nested functions
    /// reading `p` as a closure: a nested function or lambda is looked
    /// into rather than refused, and refused only when a parameter of it
    /// is named `p` (or it declares `p` global or nonlocal, or binds it).
    /// A name never rebound in any scope holds the literal throughout.
    private static boolean rebindsDeep(Object o, final String p) {
        if (o instanceof FunctionDef || o instanceof Node.Lambda) {
            Node.Arguments a = o instanceof FunctionDef ? ((FunctionDef) o).args : ((Node.Lambda) o).args;
            if (a.names().contains(p) || o instanceof FunctionDef && p.equals(((FunctionDef) o).name)) {
                return true;
            }
            final boolean[] found = {false};
            Infer.children(o, x -> found[0] |= !found[0] && !(x instanceof Node.Arguments) && rebindsDeep(x, p));
            if (o instanceof FunctionDef) {
                for (Expr d : ((FunctionDef) o).decorators) {
                    found[0] |= rebindsDeep(d, p);
                }
            }
            return found[0];
        }
        if (o instanceof ClassDef || o instanceof Node.Comp || o instanceof Node.Global || o instanceof Node.Match
                || o instanceof Node.Alias) {
            return true;
        }
        if (rebindsShallow(o, p)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> found[0] |= !found[0] && rebindsDeep(x, p));
        return found[0];
    }

    /// Whether `o` itself binds `p` through its own targets.
    private static boolean rebindsShallow(Object o, String p) {
        List<Object> targets = new ArrayList<Object>();
        if (o instanceof Node.Assign) {
            targets.addAll(((Node.Assign) o).targets);
        } else if (o instanceof Node.AugAssign) {
            targets.add(((Node.AugAssign) o).target);
        } else if (o instanceof Node.AnnAssign) {
            targets.add(((Node.AnnAssign) o).target);
        } else if (o instanceof Node.For) {
            targets.add(((Node.For) o).target);
        } else if (o instanceof Node.WithItem) {
            targets.add(((Node.WithItem) o).vars);
        } else if (o instanceof Node.NamedExpr) {
            targets.add(((Node.NamedExpr) o).target);
        } else if (o instanceof Node.Delete) {
            targets.addAll(((Node.Delete) o).targets);
        } else if (o instanceof Node.ExceptHandler && p.equals(((Node.ExceptHandler) o).name)) {
            return true;
        }
        for (Object t : targets) {
            if (t != null && mentions(t, p)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(Object o, final String p) {
        if (o instanceof Name && p.equals(((Name) o).id)) {
            return true;
        }
        final boolean[] found = {false};
        Infer.children(o, x -> {
            if (!found[0] && mentions(x, p)) {
                found[0] = true;
            }
        });
        return found[0];
    }

    /// The dedented text of `f` in `src`, or null when it cannot be found.
    static Text extract(String src, FunctionDef f) {
        if (src == null) {
            return null;
        }
        List<Token> toks = new Lexer(src, "<extract>").tokenize();
        int start = -1;
        for (int i = 0; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.line == f.line && k.is("def") && i + 1 < toks.size() && toks.get(i + 1).text.equals(f.name)) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return null;
        }
        int depth = 0;
        int end = -1;
        int last = f.line;
        boolean header = true;
        for (int i = start; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.kind == Token.Kind.EOF) {
                end = i;
                break;
            }
            if (k.kind == Token.Kind.INDENT) {
                depth++;
            } else if (k.kind == Token.Kind.DEDENT) {
                depth--;
                if (depth == 0) {
                    end = i;
                    break;
                }
            } else if (k.kind == Token.Kind.NEWLINE) {
                if (header && depth == 0 && !(i + 1 < toks.size() && toks.get(i + 1).kind == Token.Kind.INDENT)) {
                    end = i;
                    break;
                }
                header = false;
            } else {
                last = Math.max(last, k.line + count(k.text, '\n'));
            }
        }
        if (end < 0) {
            return null;
        }
        String[] lines = src.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (last > lines.length) {
            return null;
        }
        String first = lines[f.line - 1];
        int ind = 0;
        while (ind < first.length() && first.charAt(ind) == ' ') {
            ind++;
        }
        StringBuilder b = new StringBuilder();
        for (int l = f.line; l <= last; l++) {
            String s = lines[l - 1];
            int cut = 0;
            while (cut < ind && cut < s.length() && s.charAt(cut) == ' ') {
                cut++;
            }
            b.append(s.substring(cut)).append('\n');
        }
        return new Text(b.toString(), f.line);
    }

    static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    static FunctionDef parseMethod(String src, int line, String path) {
        StringBuilder b = new StringBuilder();
        for (int i = 2; i < line; i++) {
            b.append('\n');
        }
        b.append("class _cn1_c:\n").append(src.replaceAll("(?m)^(?=.)", "    "));
        Node.Module m = Parser.parse(b.toString(), path);
        ClassDef c = (ClassDef) m.body.get(m.body.size() - 1);
        return (FunctionDef) c.body.get(0);
    }

    /// `src` (a dedented def of `name`) renamed to `clone`, with `p`
    /// dropped from its parameters and the literal `lit` written for it.
    /// Null when a use of `p` cannot be rewritten.
    private static String substitute(String src, String path, String name, String clone, String p, String q,
            boolean dflt) {
        // The literal itself, for a getattr/setattr naming `p` (string mode only).
        String lit = q.length() > 1 && q.charAt(0) == '"' ? q.substring(1, q.length() - 1) : q;
        List<Token> toks = new Lexer(src, path).tokenize();
        int[] starts = lineStarts(src);
        List<int[]> edits = new ArrayList<int[]>();
        List<String> repl = new ArrayList<String>();
        // def NAME ( self , ... p ... ) -- rename and drop p with its comma.
        if (toks.size() < 3 || !toks.get(1).text.equals(name) || !toks.get(2).is("(")) {
            return null;
        }
        edit(edits, repl, off(starts, toks.get(1)), off(starts, toks.get(1)) + name.length(), clone);
        int close = match(toks, 2);
        if (close < 0) {
            return null;
        }
        boolean dropped = false;
        for (int i = 3; i < close; i++) {
            Token k = toks.get(i);
            if (k.kind == Token.Kind.NAME && k.text.equals(p) && toks.get(i - 1).is(",")) {
                int stop = off(starts, k) + p.length();
                if (toks.get(i + 1).is("=")) {
                    if (!dflt) {
                        return null;
                    }
                    int c = topComma(toks, i + 2, close);
                    int to = c < 0 ? close : c;
                    Token lastTok = toks.get(to - 1);
                    stop = off(starts, lastTok) + lastTok.text.length();
                }
                if (toks.get(i + 1).is(":")) {
                    // An annotated parameter (`name: str`, the common typed
                    // form) goes with its annotation, up to the next
                    // top-level comma or the closing bracket.
                    int c = topComma(toks, i + 2, close);
                    int to = c < 0 ? close : c;
                    for (int j = i + 2; j < to && !dflt; j++) {
                        if (toks.get(j).is("=")) {
                            return null;
                        }
                    }
                    Token lastTok = toks.get(to - 1);
                    stop = off(starts, lastTok) + lastTok.text.length();
                    if (lastTok.kind == Token.Kind.STRING || count(lastTok.text, '\n') > 0) {
                        return null;
                    }
                }
                edit(edits, repl, off(starts, toks.get(i - 1)), stop, "");
                dropped = true;
            }
        }
        if (!dropped) {
            return null;
        }
        for (int i = close + 1; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.kind == Token.Kind.STRING && k.prefix != null && k.prefix.indexOf('f') >= 0
                    && k.text.contains(p)) {
                return null;
            }
            if (k.kind != Token.Kind.NAME) {
                continue;
            }
            if ((k.is("getattr") || k.is("setattr")) && toks.get(i + 1).is("(") && !toks.get(i - 1).is(".")) {
                int end = match(toks, i + 1);
                int comma = topComma(toks, i + 2, end);
                if (end > 0 && comma > 0 && toks.get(comma + 1).kind == Token.Kind.NAME
                        && toks.get(comma + 1).text.equals(p)) {
                    for (int j = i + 2; j < comma; j++) {
                        if (toks.get(j).kind == Token.Kind.NAME && toks.get(j).text.equals(p)) {
                            return null;
                        }
                    }
                    int objS = off(starts, toks.get(i + 2));
                    int objE = off(starts, toks.get(comma));
                    String obj = src.substring(objS, objE).trim();
                    // `getattr(super(C, self), "lit")` keeps its call form:
                    // Layouts reads `super().name` as a name a super() call
                    // names, which bars a mixin binding it from being merged
                    // under a later Java parent (python-pptx's
                    // _InheritsDimensions over Shape). The literal getattr on
                    // super is typed the same, through the MRO after C.
                    boolean onSuper = toks.get(i + 2).is("super") && toks.get(i + 3).is("(");
                    if (k.is("getattr") && comma + 2 == end && !onSuper) {
                        edit(edits, repl, off(starts, k), off(starts, toks.get(end)) + 1,
                                "(" + obj + ")." + lit);
                        i = end;
                        continue;
                    }
                    Token prev = toks.get(i - 1);
                    boolean stmt = prev.kind == Token.Kind.NEWLINE || prev.kind == Token.Kind.INDENT
                            || prev.kind == Token.Kind.DEDENT;
                    if (k.is("setattr") && stmt && toks.get(comma + 2).is(",")
                            && toks.get(end + 1).kind == Token.Kind.NEWLINE) {
                        int valS = off(starts, toks.get(comma + 3));
                        int valE = off(starts, toks.get(end));
                        edit(edits, repl, off(starts, k), off(starts, toks.get(end)) + 1,
                                "(" + obj + ")." + lit + " = (" + src.substring(valS, valE).trim() + ")");
                        i = end;
                        continue;
                    }
                }
            }
            if (k.text.equals(p) && !toks.get(i - 1).is(".")) {
                if (toks.get(i + 1).is("=")) {
                    // A keyword argument's name, not a use.
                    return null;
                }
                edit(edits, repl, off(starts, k), off(starts, k) + p.length(), q);
            }
        }
        // Edits were collected left to right without overlap except where
        // a getattr span replaced its own inner use; apply right to left.
        StringBuilder b = new StringBuilder(src);
        for (int i = edits.size() - 1; i >= 0; i--) {
            int[] e = edits.get(i);
            b.replace(e[0], e[1], repl.get(i));
        }
        return b.toString();
    }

    private static void edit(List<int[]> edits, List<String> repl, int s, int e, String r) {
        edits.add(new int[] {s, e});
        repl.add(r);
    }

    static int[] lineStarts(String src) {
        List<Integer> l = new ArrayList<Integer>();
        l.add(Integer.valueOf(0));
        for (int i = 0; i < src.length(); i++) {
            if (src.charAt(i) == '\n') {
                l.add(Integer.valueOf(i + 1));
            }
        }
        int[] r = new int[l.size()];
        for (int i = 0; i < r.length; i++) {
            r[i] = l.get(i).intValue();
        }
        return r;
    }

    static int off(int[] starts, Token t) {
        return starts[t.line - 1] + t.col - 1;
    }

    /// The index of the bracket closing the one at `open`, or -1.
    private static int match(List<Token> toks, int open) {
        int d = 0;
        for (int i = open; i < toks.size(); i++) {
            Token k = toks.get(i);
            if (k.kind != Token.Kind.OP) {
                continue;
            }
            if (k.is("(") || k.is("[") || k.is("{")) {
                d++;
            } else if (k.is(")") || k.is("]") || k.is("}")) {
                d--;
                if (d == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    /// The first comma at bracket depth zero in `[from, to)`, or -1.
    private static int topComma(List<Token> toks, int from, int to) {
        int d = 0;
        for (int i = from; i < to; i++) {
            Token k = toks.get(i);
            if (k.kind != Token.Kind.OP) {
                continue;
            }
            if (k.is("(") || k.is("[") || k.is("{")) {
                d++;
            } else if (k.is(")") || k.is("]") || k.is("}")) {
                d--;
            } else if (d == 0 && k.is(",")) {
                return i;
            }
        }
        return -1;
    }

    /// A structural rendering of `o`, ignoring positions and scopes.
    static String dump(Object o) {
        StringBuilder b = new StringBuilder();
        dump(o, b);
        return b.toString();
    }

    private static void dump(Object o, StringBuilder b) {
        if (o instanceof List) {
            b.append('[');
            for (Object x : (List<?>) o) {
                dump(x, b);
                b.append(',');
            }
            b.append(']');
            return;
        }
        if (!(o instanceof Node)) {
            b.append(o);
            return;
        }
        b.append(o.getClass().getSimpleName()).append('(');
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || "line".equals(f.getName()) || "col".equals(f.getName())) {
                    continue;
                }
                Class<?> t = f.getType();
                if (!Node.class.isAssignableFrom(t) && !List.class.isAssignableFrom(t) && !t.isPrimitive()
                        && t != String.class && !t.isEnum()) {
                    continue;
                }
                try {
                    dump(f.get(o), b);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
                b.append(';');
            }
        }
        b.append(')');
    }

    static boolean ident(String s) {
        if (s.isEmpty() || !(Character.isLetter(s.charAt(0)) || s.charAt(0) == '_')) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch >= 0x80 || !(Character.isLetterOrDigit(ch) || ch == '_')) {
                return false;
            }
        }
        return true;
    }
}
