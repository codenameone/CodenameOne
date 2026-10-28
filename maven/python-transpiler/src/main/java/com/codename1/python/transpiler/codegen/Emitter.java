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

import com.codename1.python.transpiler.api.PythonSyntaxError;
import com.codename1.python.transpiler.ast.Node;
import com.codename1.python.transpiler.ast.Node.*;
import com.codename1.python.transpiler.ast.Scope;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// Writes one Python module as one Java 17 class against
/// `com.codename1.python.runtime`.
///
/// The shape of the output, so the rules below make sense:
///
/// - Module globals live in a slot array (`G`); a read of slot `i` is `g(i)`,
///   which raises NameError while it is unbound.
/// - Every function body is a `PyFunction.Body` lambda. Its locals are Java
///   locals; a local an inner scope captures is a one-element array (a
///   cell), so the lambdas only ever capture effectively final values.
/// - Every Java name is unique within the class (`v12_x`, `c12_x`, `t12`),
///   because a Java lambda may not redeclare a local of its enclosing method.
/// - `return`, `raise`, `break` and `continue` are written `if (true) ...`:
///   Python allows dead code after them and Java rejects it as unreachable.
/// - Comprehensions are immediately invoked lambdas: they are scopes of
///   their own in Python 3.
public final class Emitter {
    private static final Map<String, String> BIN = new HashMap<String, String>();
    private static final Map<String, String> CMP = new HashMap<String, String>();

    static {
        BIN.put("+", "add");
        BIN.put("-", "sub");
        BIN.put("*", "mul");
        BIN.put("/", "truediv");
        BIN.put("//", "floordiv");
        BIN.put("%", "mod");
        BIN.put("**", "pow");
        BIN.put("<<", "lshift");
        BIN.put(">>", "rshift");
        BIN.put("&", "and");
        BIN.put("|", "or");
        BIN.put("^", "xor");
        CMP.put("<", "lt");
        CMP.put("<=", "le");
        CMP.put(">", "gt");
        CMP.put(">=", "ge");
        CMP.put("==", "eq");
        CMP.put("!=", "ne");
        CMP.put("in", "in");
        CMP.put("not in", "notIn");
        CMP.put("is", "is");
        CMP.put("is not", "isNot");
    }

    /// One Java lambda (or the module body): owns a temp array.
    private static final class Frame {
        final int id;
        final Scope scope;
        int temps;
        int ltemps;

        Frame(int id, Scope scope) {
            this.id = id;
            this.scope = scope;
        }
    }

    private static final class Loop {
        final String label;
        final String breakLabel;

        Loop(String label, String breakLabel) {
            this.label = label;
            this.breakLabel = breakLabel;
        }
    }

    private final String file;
    private final String moduleName;
    /// Set while the plain call inside a `type(name, bases, ns)` wrapper is
    /// emitted, so it is not wrapped twice.
    private boolean inTypeCall;
    private final boolean isPackage;
    private final String className;
    private final String packageName;

    private final Map<Scope, Integer> ids = new HashMap<Scope, Integer>();
    private final Map<String, String> constants = new LinkedHashMap<String, String>();
    private final Map<String, String> builtins = new LinkedHashMap<String, String>();
    private final List<String> slotNames = new ArrayList<String>();
    private final Map<String, Integer> slotIndex = new HashMap<String, Integer>();
    private final StringBuilder fields = new StringBuilder(); //NOPMD AvoidStringBufferField - the generator's output
    private int uid;
    private final Scope moduleScope;

    private Frame frame;
    private Scope sc;
    private final List<Loop> loops = new ArrayList<Loop>();
    /// The lowering of the generator or coroutine body being emitted, if any.
    private GenLowering gen;
    /// Whether the next function() is an `async def`.
    private boolean nextAsync;
    final List<String> handlers = new ArrayList<String>();

    public Emitter(String file, String moduleName, boolean isPackage, String packageName, String className,
            Scope moduleScope) {
        this.file = file;
        this.moduleScope = moduleScope;
        this.moduleName = moduleName;
        this.isPackage = isPackage;
        this.packageName = packageName;
        this.className = className;
    }

    PythonSyntaxError unsupported(Node n, String what) {
        return new PythonSyntaxError(file, n.line, n.col, what + " is not supported yet");
    }

    /// Builtins that run, or look up, code the build never compiled. A
    /// translated program has no compiler and no module loader at run time,
    /// so each one is a build error at the line that names it -- never a
    /// NameError the first time a device reaches it.
    private static final Set<String> DYNAMIC_CODE = new HashSet<String>(Arrays.asList("eval", "exec", "compile",
            "__import__"));

    /// `sys` members that reach the interpreter's frames, which a
    /// translated program does not have.
    private static final Set<String> FRAME_HOOKS = new HashSet<String>(Arrays.asList("settrace", "setprofile",
            "_getframe", "gettrace", "getprofile"));

    private PythonSyntaxError dynamicCode(Node n, String what) {
        return new PythonSyntaxError(file, n.line, n.col, what
                + " is not supported: a translated program cannot run or inspect code that was not compiled with it");
    }

    private int id(Scope s) {
        Integer i = ids.get(s);
        if (i == null) {
            i = Integer.valueOf(++uid);
            ids.put(s, i);
        }
        return i.intValue();
    }

    String fresh(String prefix) {
        return prefix + (++uid);
    }

    // ---- module ----------------------------------------------------------

    public String emit(Node.Module m) {
        Scope ms = moduleScope;
        slot("__name__");
        slot("__doc__");
        for (String n : ms.assigned) {
            slot(n);
        }
        frame = new Frame(id(ms), ms);
        sc = ms;
        StringBuilder body = new StringBuilder();
        List<Stmt> stmts = m.body;
        if (!stmts.isEmpty() && isDocstring(stmts.get(0))) {
            body.append("        G[1] = ").append(constant((Constant) ((ExprStmt) stmts.get(0)).value)).append(";\n");
        }
        findDirectLong(stmts);
        layouts = Layouts.plan(stmts, "", ms.attrStores);
        Infer infer = closed ? Infer.run(stmts, layouts, ms) : null;
        if (infer != null && System.getProperty("cn1.python.dumpTypes") != null) {
            System.err.println(infer.dump());
        }
        if (infer != null) {
            typedEmit = new TypedEmit(this, infer, layouts);
            typedEmit.plan();
        }
        block(stmts, body, 2);

        StringBuilder out = new StringBuilder();
        out.append("// Generated from ").append(javaComment(file)).append(" by the Codename One Python transpiler.\n");
        out.append("package ").append(packageName).append(";\n\n");
        out.append("import com.codename1.python.runtime.*;\n\n");
        out.append("@SuppressWarnings(\"all\")\n");
        out.append("public final class ").append(className).append(" {\n");
        out.append("    private static PyModule M;\n");
        out.append("    private static Object[] G;\n");
        for (Map.Entry<String, String> e : builtins.entrySet()) {
            out.append("    private static final Object ").append(e.getValue()).append(" = Builtins.lookup(")
                    .append(javaString(e.getKey())).append(");\n");
        }
        out.append(fields);
        out.append(layoutCode());
        if (typedEmit != null) {
            out.append(typedEmit.members());
        }
        out.append("\n    private ").append(className).append("() {\n    }\n\n");
        out.append("    /// Makes `import ").append(javaComment(moduleName)).append("` find this module.\n");
        out.append("    public static void register() {\n");
        out.append("        PyModule.register(").append(javaString(moduleName)).append(", m -> exec(")
                .append(javaString(moduleName)).append("));\n    }\n\n");
        out.append("    /// Runs the module body as module `runName` (`__main__` for the entry module).\n");
        out.append("    public static PyModule exec(String runName) {\n");
        out.append("        M = PyModule.begin(runName, new String[] {");
        for (int i = 0; i < slotNames.size(); i++) {
            out.append(i == 0 ? "" : ", ").append(javaString(slotNames.get(i)));
        }
        out.append("});\n");
        out.append("        G = M.slots;\n");
        out.append("        G[0] = Py.str(runName);\n");
        if ((frame.temps > 0 || frame.ltemps > 0)) {
            out.append("        ").append(tempDecls()).append("\n");
        }
        out.append(body);
        out.append("        return M;\n    }\n\n");
        out.append(HELPERS);
        out.append("}\n");
        return out.toString();
    }

    private static final String HELPERS = ""
            + "    private static Object g(int i) {\n"
            + "        Object v = G[i];\n"
            + "        return v != null ? v : M.unbound(i);\n"
            + "    }\n\n"
            + "    private static Object l(Object v, String n) {\n"
            + "        if (v == null) {\n"
            + "            throw Py.unboundLocal(n);\n"
            + "        }\n"
            + "        return v;\n"
            + "    }\n\n"
            + "    private static Object ul(String n) {\n"
            + "        throw Py.unboundLocal(n);\n"
            + "    }\n\n"
            + "    private static Object b(Object v, String n) {\n"
            + "        return v != null ? v : dyn(n);\n"
            + "    }\n\n"
            + "    private static Object dyn(String n) {\n"
            + "        Object v = M.getAttrOrNull(n);\n"
            + "        if (v == null) {\n"
            + "            v = Builtins.lookup(n);\n"
            + "        }\n"
            + "        if (v == null) {\n"
            + "            throw Py.nameError(n);\n"
            + "        }\n"
            + "        return v;\n"
            + "    }\n\n"
            + "    private static Object cr(PyType k, String n, int slot) {\n"
            + "        Object v = k.own(n);\n"
            + "        if (v != null) {\n"
            + "            return v;\n"
            + "        }\n"
            + "        if (slot >= 0 && G[slot] != null) {\n"
            + "            return G[slot];\n"
            + "        }\n"
            + "        return dyn(n);\n"
            + "    }\n\n"
            + "    private static void noop(Object v) {\n"
            + "    }\n";

    Scope moduleScope() {
        return moduleScope;
    }

    boolean moduleScopeHasStarImport() {
        return moduleScope.hasStarImport;
    }

    int slot(String n) {
        Integer i = slotIndex.get(n);
        if (i == null) {
            i = Integer.valueOf(slotNames.size());
            slotNames.add(n);
            slotIndex.put(n, i);
        }
        return i.intValue();
    }

    private static boolean isDocstring(Stmt s) {
        return s instanceof ExprStmt && ((ExprStmt) s).value instanceof Constant
                && ((Constant) ((ExprStmt) s).value).kind == Constant.Kind.STR;
    }

    // ---- names -----------------------------------------------------------

    static String mangle(String n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            if (c < 128 && (Character.isLetterOrDigit(c) || c == '_')) {
                sb.append(c);
            } else {
                sb.append('$').append(Integer.toHexString(c));
            }
        }
        return sb.toString();
    }

    private String localVar(Scope s, String n) {
        return "v" + id(s) + "_" + mangle(n);
    }

    private String cellVar(Scope s, String n) {
        return "c" + id(s) + "_" + mangle(n);
    }

    private final DefAssign defAssign = new DefAssign();

    private String load(Name nm) {
        String n = nm.id;
        if (DYNAMIC_CODE.contains(n) && sc.resolve(n) == Scope.Binding.BUILTIN) {
            throw dynamicCode(nm, n + "()");
        }
        if (inRegionFast && lazyIndex != null && lazyIndex[0].equals(n)) {
            return "(" + lazyIndex[1] + " == null ? Long.valueOf(" + lazyIndex[2] + ") : " + localVar(sc, n) + ")";
        }
        if (hoist != null && inRegionFast && !hoistAssigned.contains(n)) {
            // The versioned loop reads it once, ahead of the loop: nothing in
            // a region can rebind a name it does not assign (its calls run
            // pure twins only).
            Scope.Binding b = sc.resolve(n);
            String raw = b == Scope.Binding.LOCAL ? localVar(sc, n) : b == Scope.Binding.GLOBAL ? "G[" + slot(n) + "]" : null;
            if (raw != null) {
                String h = hoist.get(n);
                if (h == null) {
                    h = "h" + regionCount + "_" + hoist.size();
                    hoist.put(n, h);
                    hoistRaw.put(h, raw);
                }
                return h;
            }
        }
        switch (sc.resolve(n)) {
            case LOCAL:
                // A parameter is bound on entry; only `del` (or an except
                // clause's implicit one) can unbind it.
                if (sc.params.contains(n) && !sc.deleted.contains(n)) {
                    return localVar(sc, n);
                }
                // Inline: a call per read of a local is most of what a
                // small method costs once translated.
                if (defAssign.safe.contains(nm)) {
                    // Bound on every path here (DefAssign).
                    return localVar(sc, n);
                }
                return "(" + localVar(sc, n) + " != null ? " + localVar(sc, n) + " : ul(" + javaString(n) + "))";
            case CELL:
                return "l(" + cellVar(sc, n) + "[0], " + javaString(n) + ")";
            case FREE: {
                Scope o = sc.owner(n);
                return "l(" + cellVar(o, n) + "[0], " + javaString(n) + ")";
            }
            case GLOBAL:
                return "g(" + slot(n) + ")";
            case CLASS: {
                Integer s = slotIndex.get(n);
                if (s == null && moduleScope.assigned.contains(n)) {
                    s = Integer.valueOf(slot(n));
                }
                return "cr(" + sc.classVar + ", " + javaString(n) + ", " + (s == null ? -1 : s.intValue()) + ")";
            }
            case BUILTIN:
            default:
                if (moduleScope.hasStarImport) {
                    return "dyn(" + javaString(n) + ")";
                }
                return builtinRef(n);
        }
    }

    /// The builtin `n`, read through its lazily bound field.
    String builtinRef(String n) {
        String f = builtins.get(n);
        if (f == null) {
            f = "B" + builtins.size() + "_" + mangle(n);
            builtins.put(n, f);
        }
        return "b(" + f + ", " + javaString(n) + ")";
    }

    void storeName(String n, String value, StringBuilder out, int ind) {
        indent(out, ind);
        switch (sc.resolve(n)) {
            case LOCAL: {
                String v = localVar(sc, n);
                out.append(v).append(" = ").append(value).append(";\n");
                String h = shadowVar.get(v);
                if (h != null) {
                    String k = shadowCls.get(v).javaName;
                    indent(out, ind);
                    out.append(h).append(" = ").append(v).append(" instanceof ").append(k).append(" ? (").append(k)
                            .append(") ").append(v).append(" : null;\n");
                }
                return;
            }
            case CELL:
                out.append(cellVar(sc, n)).append("[0] = ").append(value).append(";\n");
                return;
            case FREE:
                out.append(cellVar(sc.owner(n), n)).append("[0] = ").append(value).append(";\n");
                return;
            case CLASS:
                out.append(sc.classVar).append(".define(").append(javaString(n)).append(", ").append(value)
                        .append(");\n");
                return;
            case GLOBAL:
            case BUILTIN:
            default:
                out.append("G[").append(slot(n)).append("] = ").append(value).append(";\n");
        }
    }

    /// The Java lvalue a walrus assigns, as an expression.
    String nameLvalue(String n) {
        switch (sc.resolve(n)) {
            case LOCAL:
                return localVar(sc, n);
            case CELL:
                return cellVar(sc, n) + "[0]";
            case FREE:
                return cellVar(sc.owner(n), n) + "[0]";
            default:
                return "G[" + slot(n) + "]";
        }
    }

    // ---- statements ------------------------------------------------------

    /// Prefix of the comment that names the Python line the code after it
    /// comes from. [com.codename1.python.transpiler.api.PythonTranspiler]
    /// turns these into the line table the build later writes into the
    /// class file, so a debugger and a stack trace show the Python source.
    public static final String LINE_MARK = "/* line ";

    static void lineMark(int line, StringBuilder out, int ind) {
        if (line > 0) {
            indent(out, ind);
            // A block comment, which stays a comment wherever the statement
            // is spliced, even inside an expression.
            out.append(LINE_MARK).append(line).append(" */\n");
        }
    }

    private static void indent(StringBuilder out, int ind) {
        for (int i = 0; i < ind; i++) {
            out.append("    ");
        }
    }

    private void block(List<Stmt> body, StringBuilder out, int ind) {
        for (int i = 0; i < body.size();) {
            int end = floatRegion(body, i, out, ind);
            if (end > i) {
                i = end;
                continue;
            }
            stmt(body.get(i), out, ind);
            i++;
        }
    }

    void stmt(Stmt s, StringBuilder out, int ind) {
        lineMark(s.line, out, ind);
        if (s instanceof ExprStmt) {
            Expr v = ((ExprStmt) s).value;
            if (v instanceof Constant) {
                return;
            }
            if (v instanceof Yield || v instanceof Await) {
                throw unsupported(v, "generators and async");
            }
            String e = expr(v);
            indent(out, ind);
            String app = appendFast(v, e);
            if (app != null) {
                out.append(app);
            } else if (v instanceof Call && !e.startsWith("(")) {
                out.append(e).append(";\n");
            } else {
                out.append("noop(").append(e).append(");\n");
            }
        } else if (s instanceof Assign) {
            Assign a = (Assign) s;
            if (a.targets.size() == 1 && isSlice(a.targets.get(0)) && isSlice(a.value)) {
                // `x[a:b] = y[c:d]` copies straight between the lists. The
                // source comes first in the argument list, as Python
                // evaluates the right-hand side before the target.
                Subscript dst = (Subscript) a.targets.get(0);
                Subscript src = (Subscript) a.value;
                Slice ds = (Slice) dst.index;
                Slice ss = (Slice) src.index;
                indent(out, ind);
                out.append("Py.setsliceFrom(").append(expr(src.value)).append(", ").append(exprOrNone(ss.lower))
                        .append(", ").append(exprOrNone(ss.upper)).append(", ").append(exprOrNone(ss.step))
                        .append(", ").append(expr(dst.value)).append(", ").append(exprOrNone(ds.lower))
                        .append(", ").append(exprOrNone(ds.upper)).append(", ").append(exprOrNone(ds.step))
                        .append(");\n");
                return;
            }
            String counted = dictCount(a);
            if (counted != null) {
                indent(out, ind);
                out.append(counted).append(";\n");
                return;
            }
            if (a.targets.size() == 1 && a.targets.get(0) instanceof Name
                    && recycled.contains(((Name) a.targets.get(0)).id) && ListRecycling.isEmptyList(a.value)) {
                // As below: the previous list is emptied and kept.
                String n = ((Name) a.targets.get(0)).id;
                storeName(n, "Py.recycleList(" + localVar(sc, n) + ")", out, ind);
                return;
            }
            if (a.targets.size() == 1 && a.targets.get(0) instanceof Name
                    && recycled.contains(((Name) a.targets.get(0)).id) && ListRecycling.isSliceCopy(a.value)) {
                // The local's previous list is unreachable from anywhere
                // else (ListRecycling), so the copy reuses its array.
                String n = ((Name) a.targets.get(0)).id;
                Subscript src = (Subscript) a.value;
                Slice ss = (Slice) src.index;
                storeName(n, "Py.recycleSlice(" + localVar(sc, n) + ", " + expr(src.value) + ", "
                        + exprOrNone(ss.lower) + ", " + exprOrNone(ss.upper) + ", " + exprOrNone(ss.step) + ")", out, ind);
                return;
            }
            String v = expr(a.value);
            if (a.targets.size() == 1 && a.targets.get(0) instanceof Name) {
                storeName(((Name) a.targets.get(0)).id, v, out, ind);
                return;
            }
            String tmp = fresh("s");
            indent(out, ind);
            out.append("final Object ").append(tmp).append(" = ").append(v).append(";\n");
            for (Expr t : a.targets) {
                store(t, tmp, out, ind);
            }
        } else if (s instanceof AugAssign) {
            augAssign((AugAssign) s, out, ind);
        } else if (s instanceof AnnAssign) {
            AnnAssign a = (AnnAssign) s;
            if (sc.kind == Scope.Kind.CLASS && a.target instanceof Name) {
                indent(out, ind);
                out.append("Py.setitem(").append(annotations()).append(", Py.str(")
                        .append(javaString(((Name) a.target).id)).append("), ").append(annotationExpr(a.annotation))
                        .append(");\n");
            }
            if (a.value != null) {
                String v = expr(a.value);
                if (a.target instanceof Name) {
                    storeName(((Name) a.target).id, v, out, ind);
                } else {
                    String tmp = fresh("s");
                    indent(out, ind);
                    out.append("final Object ").append(tmp).append(" = ").append(v).append(";\n");
                    store(a.target, tmp, out, ind);
                }
            }
        } else if (s instanceof Return) {
            if (frame.scope.kind == Scope.Kind.MODULE || sc.kind == Scope.Kind.CLASS) {
                throw new PythonSyntaxError(file, s.line, s.col, "'return' outside function");
            }
            Expr v = ((Return) s).value;
            indent(out, ind);
            if (gen != null) {
                // A generator's step returns its value with pc at -1.
                out.append("if (true) { pc = -1; return ").append(v == null ? "Py.NONE" : expr(v)).append("; }\n");
            } else {
                out.append("if (true) return ").append(v == null ? "Py.NONE" : expr(v)).append(";\n");
            }
        } else if (s instanceof Delete) {
            for (Expr t : ((Delete) s).targets) {
                delete(t, out, ind);
            }
        } else if (s instanceof Pass || s instanceof Global) {
            return;
        } else if (s instanceof Break) {
            if (loops.isEmpty()) {
                throw new PythonSyntaxError(file, s.line, s.col, "'break' outside loop");
            }
            indent(out, ind);
            out.append("if (true) break ").append(loops.get(loops.size() - 1).breakLabel).append(";\n");
        } else if (s instanceof Continue) {
            if (loops.isEmpty()) {
                throw new PythonSyntaxError(file, s.line, s.col, "'continue' not properly in loop");
            }
            indent(out, ind);
            out.append("if (true) continue ").append(loops.get(loops.size() - 1).label).append(";\n");
        } else if (s instanceof Raise) {
            Raise r = (Raise) s;
            indent(out, ind);
            if (r.exc == null) {
                if (handlers.isEmpty()) {
                    out.append("if (true) throw Py.runtimeError(\"No active exception to reraise\");\n");
                } else {
                    out.append("if (true) throw ").append(handlers.get(handlers.size() - 1)).append(";\n");
                }
            } else if (r.cause != null) {
                out.append("if (true) throw Py.raise(").append(expr(r.exc)).append(", ").append(expr(r.cause))
                        .append(");\n");
            } else {
                out.append("if (true) throw Py.raise(").append(expr(r.exc)).append(");\n");
            }
        } else if (s instanceof Assert) {
            Assert a = (Assert) s;
            indent(out, ind);
            out.append("if (!(").append(cond(a.test)).append(")) {\n");
            indent(out, ind + 1);
            if (a.msg == null) {
                out.append("throw Py.raise(Py.ASSERTION_ERROR);\n");
            } else {
                out.append("throw Py.raise(Py.call(Py.ASSERTION_ERROR, ").append(expr(a.msg)).append("));\n");
            }
            indent(out, ind);
            out.append("}\n");
        } else if (s instanceof Import) {
            for (Alias a : ((Import) s).names) {
                String load = "Py.importModule(" + javaString(a.name) + ")";
                if (a.asname != null) {
                    storeName(a.asname, load, out, ind);
                } else {
                    int dot = a.name.indexOf('.');
                    if (dot < 0) {
                        storeName(a.name, load, out, ind);
                    } else {
                        indent(out, ind);
                        out.append(load).append(";\n");
                        String top = a.name.substring(0, dot);
                        storeName(top, "Py.importModule(" + javaString(top) + ")", out, ind);
                    }
                }
            }
        } else if (s instanceof ImportFrom) {
            importFrom((ImportFrom) s, out, ind);
        } else if (s instanceof If) {
            ifStmt((If) s, out, ind);
        } else if (s instanceof While) {
            whileStmt((While) s, out, ind);
        } else if (s instanceof For) {
            forStmt((For) s, out, ind);
        } else if (s instanceof Try) {
            tryStmt((Try) s, out, ind);
        } else if (s instanceof With) {
            With w = (With) s;
            if (w.isAsync) {
                throw unsupported(s, "async with");
            }
            withItems(w, 0, out, ind);
        } else if (s instanceof FunctionDef) {
            FunctionDef f = (FunctionDef) s;
            nextAsync = f.isAsync;
            staticBody = layouts.staticNames.get(f);
            staticClass = layouts.methodClass.get(f);
            String fn = withMetadata(f, function(f.name, f.args, f.scope, f.body, null));
            storeName(f.name, decorate(f.decorators, fn), out, ind);
        } else if (s instanceof ClassDef) {
            classDef((ClassDef) s, out, ind);
        } else if (s instanceof Match) {
            throw unsupported(s, "match");
        } else if (s instanceof TypeAlias) {
            TypeAlias t = (TypeAlias) s;
            storeName(t.name, expr(t.value), out, ind);
        } else {
            throw unsupported(s, s.getClass().getSimpleName());
        }
    }

    private String annotations() {
        return "Py.getattr(" + sc.classVar + ", \"__annotations__\")";
    }

    /// `fn` with the function's docstring and annotations attached, when it
    /// has either. Annotations are evaluated on first read of
    /// `__annotations__` (the PEP 649 order), so a forward reference costs
    /// nothing until something asks.
    private String withMetadata(FunctionDef f, String fn) {
        String doc = !f.body.isEmpty() && isDocstring(f.body.get(0))
                ? constant((Constant) ((ExprStmt) f.body.get(0)).value) : "null";
        List<Arg> all = new ArrayList<Arg>(f.args.positional);
        if (f.args.vararg != null) {
            all.add(f.args.vararg);
        }
        all.addAll(f.args.kwonly);
        if (f.args.kwarg != null) {
            all.add(f.args.kwarg);
        }
        StringBuilder ann = new StringBuilder();
        for (Arg a : all) {
            if (a.annotation != null) {
                ann.append(ann.length() == 0 ? "" : ", ").append(javaString(a.name)).append(", ")
                        .append(annotationExpr(a.annotation));
            }
        }
        if (f.returns != null) {
            ann.append(ann.length() == 0 ? "" : ", ").append("\"return\", ").append(annotationExpr(f.returns));
        }
        if ("null".equals(doc) && ann.length() == 0) {
            return fn;
        }
        String thunk = ann.length() == 0 ? "null" : "() -> new Object[] {" + ann + "}";
        return "PyFunction.meta(" + fn + ", " + doc + ", " + thunk + ")";
    }

    /// An annotation's value: the evaluated expression when it only names
    /// module globals and builtins (falling back to the source-ish form if
    /// evaluating it raises), otherwise [#annotationValue].
    private String annotationExpr(Expr a) {
        if (!evaluable(a)) {
            return annotationValue(a);
        }
        return "PyFunction.annotation(() -> " + expr(a) + ", " + annotationValue(a) + ")";
    }

    private boolean evaluable(Expr a) {
        if (a instanceof Constant) {
            return true;
        }
        if (a instanceof Name) {
            Scope.Binding b = sc.resolve(((Name) a).id);
            return b == Scope.Binding.GLOBAL || b == Scope.Binding.BUILTIN;
        }
        if (a instanceof Attribute) {
            return evaluable(((Attribute) a).value);
        }
        if (a instanceof Subscript) {
            return evaluable(((Subscript) a).value) && evaluable(((Subscript) a).index);
        }
        if (a instanceof BinOp) {
            return "|".equals(((BinOp) a).op) && evaluable(((BinOp) a).left) && evaluable(((BinOp) a).right);
        }
        if (a instanceof TupleExpr) {
            for (Expr e : ((TupleExpr) a).elts) {
                if (!evaluable(e)) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof ListExpr) {
            for (Expr e : ((ListExpr) a).elts) {
                if (!evaluable(e)) {
                    return false;
                }
            }
            return true;
        }
        if (a instanceof UnaryOp) {
            return evaluable(((UnaryOp) a).operand);
        }
        if (a instanceof Starred) {
            return evaluable(((Starred) a).value);
        }
        if (a instanceof Lambda) {
            // `Annotated[int, AfterValidator(lambda v: ...)]`: the body runs
            // later, in its own scope; only its creation is evaluated here.
            return ((Lambda) a).scope != null;
        }
        if (a instanceof Call) {
            // `Annotated[int, Field(gt=0)]`, `conint(ge=2)`: libraries read
            // these annotations at run time, so they are evaluated as written.
            Call c = (Call) a;
            if (!evaluable(c.func)) {
                return false;
            }
            for (Expr e : c.args) {
                if (!evaluable(e)) {
                    return false;
                }
            }
            for (Keyword k : c.keywords) {
                if (!evaluable(k.value)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /// Annotations are not evaluated (as under `from __future__ import
    /// annotations`): a simple name becomes its string, anything else None.
    private String annotationValue(Expr a) {
        if (a instanceof Name) {
            return "Py.str(" + javaString(((Name) a).id) + ")";
        }
        if (a instanceof Constant && ((Constant) a).kind == Constant.Kind.STR) {
            return constant((Constant) a);
        }
        return "Py.NONE";
    }

    private String decorate(List<Expr> decorators, String value) {
        String v = value;
        for (int i = decorators.size() - 1; i >= 0; i--) {
            v = "Py.call(" + expr(decorators.get(i)) + ", " + v + ")";
        }
        // Java evaluates the decorator expressions before the PyFunction's
        // defaults, which is Python's order too.
        return v;
    }

    void store(Expr t, String value, StringBuilder out, int ind) {
        if (t instanceof Name) {
            storeName(((Name) t).id, value, out, ind);
        } else if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            indent(out, ind);
            if (layouts.fieldOwners.containsKey(a.attr)) {
                String recv = expr(a.value);
                String o;
                String rl = rawLocal(recv);
                if (rl != null && (rl.equals(selfLocal) || shadowVar.containsKey(rl))) {
                    // The receiver parameter, never rebound: read as it is.
                    o = rl;
                } else {
                    o = fresh("o");
                    out.append("final Object ").append(o).append(" = ").append(recv).append(";\n");
                }
                fieldSet(a.attr, o, value, site(a.attr), out, ind);
                return;
            }
            out.append(site(a.attr)).append(".set(").append(expr(a.value)).append(", ").append(value)
                    .append(");\n");
        } else if (t instanceof Subscript) {
            Subscript sub = (Subscript) t;
            indent(out, ind);
            if (sub.index instanceof Slice) {
                Slice sl = (Slice) sub.index;
                out.append("Py.setslice(").append(expr(sub.value)).append(", ").append(exprOrNone(sl.lower))
                        .append(", ").append(exprOrNone(sl.upper)).append(", ").append(exprOrNone(sl.step))
                        .append(", ").append(value).append(");\n");
            } else {
                out.append("Py.setitem(").append(expr(sub.value)).append(", ").append(expr(sub.index)).append(", ")
                        .append(value).append(");\n");
            }
        } else if (t instanceof TupleExpr || t instanceof ListExpr) {
            List<Expr> elts = t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts;
            int star = -1;
            for (int i = 0; i < elts.size(); i++) {
                if (elts.get(i) instanceof Starred) {
                    if (star >= 0) {
                        throw new PythonSyntaxError(file, t.line, t.col, "multiple starred expressions in assignment");
                    }
                    star = i;
                }
            }
            String arr = fresh("u");
            indent(out, ind);
            out.append("final Object[] ").append(arr).append(" = ");
            if (star < 0) {
                out.append(onlyNames(t) ? "Py.unpackNames(" : "Py.unpack(").append(value).append(", ")
                        .append(elts.size()).append(");\n");
            } else {
                out.append("Py.unpackStar(").append(value).append(", ").append(star).append(", ")
                        .append(elts.size() - star - 1).append(");\n");
            }
            for (int i = 0; i < elts.size(); i++) {
                Expr e = elts.get(i);
                if (e instanceof Starred) {
                    e = ((Starred) e).value;
                }
                store(e, arr + "[" + i + "]", out, ind);
            }
        } else if (t instanceof Starred) {
            throw new PythonSyntaxError(file, t.line, t.col, "starred assignment target must be in a list or tuple");
        } else {
            throw new PythonSyntaxError(file, t.line, t.col, "cannot assign to expression");
        }
    }

    /// `range(a)`, `range(a, b)` or `range(a, b, c)` with plain arguments.
    private static boolean rangeCall(Expr e) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        if (!(c.func instanceof Name) || !"range".equals(((Name) c.func).id) || !c.keywords.isEmpty()
                || c.args.isEmpty() || c.args.size() > 3) {
            return false;
        }
        for (Expr a : c.args) {
            if (a instanceof Starred) {
                return false;
            }
        }
        return true;
    }

    /// `x.split()` with no arguments, which a loop walks word by word
    /// (Py.splitWords) instead of splitting into a list first.
    private static boolean splitWords(Expr e) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        return c.args.isEmpty() && c.keywords.isEmpty() && c.func instanceof Attribute
                && "split".equals(((Attribute) c.func).attr);
    }

    /// `x.split(sep)`, walked piece by piece the same way (Py.splitOn).
    private static boolean splitOn(Expr e) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        return c.args.size() == 1 && !(c.args.get(0) instanceof Starred) && c.keywords.isEmpty()
                && c.func instanceof Attribute && "split".equals(((Attribute) c.func).attr);
    }

    private static boolean isSlice(Expr e) {
        return e instanceof Subscript && ((Subscript) e).index instanceof Slice;
    }

    /// Whether an unpacking target is made of plain names only, so storing
    /// into it cannot change the sequence being unpacked.
    private static boolean onlyNames(Expr t) {
        if (t instanceof Name) {
            return true;
        }
        List<Expr> elts = t instanceof TupleExpr ? ((TupleExpr) t).elts
                : t instanceof ListExpr ? ((ListExpr) t).elts : null;
        if (elts == null) {
            return false;
        }
        for (Expr e : elts) {
            if (!onlyNames(e)) {
                return false;
            }
        }
        return true;
    }

    private void delete(Expr t, StringBuilder out, int ind) {
        indent(out, ind);
        if (t instanceof Name) {
            String n = ((Name) t).id;
            switch (sc.resolve(n)) {
                case LOCAL:
                case CELL:
                case FREE:
                    out.append("noop(").append(load((Name) t)).append(");\n");
                    indent(out, ind);
                    out.append(nameLvalue(n)).append(" = null;\n");
                    return;
                case CLASS:
                    out.append(sc.classVar).append(".delAttr(").append(javaString(n)).append(");\n");
                    return;
                default:
                    out.append("M.delAttr(").append(javaString(n)).append(");\n");
                    return;
            }
        } else if (t instanceof Attribute) {
            Attribute a = (Attribute) t;
            out.append("Py.obj(").append(expr(a.value)).append(").delAttr(").append(javaString(a.attr)).append(");\n");
        } else if (t instanceof Subscript) {
            Subscript s = (Subscript) t;
            out.append("Py.obj(").append(expr(s.value)).append(").delItem(").append(expr(s.index)).append(");\n");
        } else if (t instanceof TupleExpr || t instanceof ListExpr) {
            out.setLength(out.length() - ind * 4);
            for (Expr e : t instanceof TupleExpr ? ((TupleExpr) t).elts : ((ListExpr) t).elts) {
                delete(e, out, ind);
            }
        } else {
            throw new PythonSyntaxError(file, t.line, t.col, "cannot delete expression");
        }
    }

    /// `cur op= value` as an expression, fused when the right side is float
    /// arithmetic.
    private String augValue(String op, String cur, AugAssign a) {
        if (numNode(new BinOp(AUG_CURRENT, a.op, a.value))) {
            String f = fused(new BinOp(AUG_CURRENT, a.op, a.value), cur);
            if (f != null) {
                return f;
            }
        }
        String in = inlineArith(a.op, cur, expr(a.value), true);
        if (in != null) {
            return in;
        }
        return "Py." + op + "(" + cur + ", " + expr(a.value) + ")";
    }

    /// Whether `e` always evaluates to a bool: `not`, an identity or
    /// membership test, a bool constant, or `and`/`or` of those. A rich
    /// comparison may return anything a `__lt__` returns.
    private static boolean boolValued(Expr e) {
        if (e instanceof UnaryOp) {
            return "not".equals(((UnaryOp) e).op);
        }
        if (e instanceof Constant) {
            return ((Constant) e).kind == Constant.Kind.TRUE || ((Constant) e).kind == Constant.Kind.FALSE;
        }
        if (e instanceof Compare) {
            for (String op : ((Compare) e).ops) {
                if (!"is".equals(op) && !"is not".equals(op) && !"in".equals(op) && !"not in".equals(op)) {
                    return false;
                }
            }
            return true;
        }
        if (e instanceof BoolOp) {
            for (Expr v : ((BoolOp) e).values) {
                if (!boolValued(v)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private void augAssign(AugAssign a, StringBuilder out, int ind) {
        String bin = BIN.get(a.op);
        String op = bin == null ? null : "i" + bin;
        if (op == null) {
            throw unsupported(a, "operator " + a.op + "=");
        }
        if (a.target instanceof Name) {
            Name n = (Name) a.target;
            storeName(n.id, augValue(op, load(n), a), out, ind);
        } else if (a.target instanceof Attribute) {
            Attribute at = (Attribute) a.target;
            String recv = expr(at.value);
            String o;
            String rl = rawLocal(recv);
            if (rl != null && (rl.equals(selfLocal) || shadowVar.containsKey(rl))
                    && layouts.fieldOwners.containsKey(at.attr)) {
                o = rl;
            } else {
                o = fresh("o");
                indent(out, ind);
                out.append("final Object ").append(o).append(" = ").append(recv).append(";\n");
            }
            indent(out, ind);
            if (layouts.fieldOwners.containsKey(at.attr)) {
                String nv = fresh("s");
                out.append("final Object ").append(nv).append(" = ")
                        .append(augValue(op, fieldGet(at.attr, o, site(at.attr)), a)).append(";\n");
                fieldSet(at.attr, o, nv, site(at.attr), out, ind);
            } else {
                out.append(site(at.attr)).append(".set(").append(o).append(", ")
                        .append(augValue(op, site(at.attr) + ".get(" + o + ")", a)).append(");\n");
            }
        } else if (a.target instanceof Subscript) {
            Subscript sb = (Subscript) a.target;
            String o = fresh("o");
            String k = fresh("k");
            indent(out, ind);
            out.append("final Object ").append(o).append(" = ").append(expr(sb.value)).append(";\n");
            indent(out, ind);
            out.append("final Object ").append(k).append(" = ").append(expr(sb.index)).append(";\n");
            indent(out, ind);
            out.append("Py.setitem(").append(o).append(", ").append(k).append(", ")
                    .append(augValue(op, "Py.getitem(" + o + ", " + k + ")", a)).append(");\n");
        } else {
            throw new PythonSyntaxError(file, a.line, a.col, "illegal expression for augmented assignment");
        }
    }

    private void importFrom(ImportFrom f, StringBuilder out, int ind) {
        String mod = resolveRelative(f);
        if ("__future__".equals(mod)) {
            return;
        }
        String m = fresh("m");
        indent(out, ind);
        out.append("final PyModule ").append(m).append(" = Py.importModule(").append(javaString(mod)).append(");\n");
        for (Alias a : f.names) {
            if ("*".equals(a.name)) {
                indent(out, ind);
                out.append("Py.importStar(M, ").append(m).append(");\n");
            } else {
                storeName(a.asname != null ? a.asname : a.name,
                        "Py.importFrom(" + m + ", " + javaString(a.name) + ")", out, ind);
            }
        }
    }

    private String resolveRelative(ImportFrom f) {
        if (f.level == 0) {
            return f.module;
        }
        String base = moduleName;
        int up = isPackage ? f.level - 1 : f.level;
        for (int i = 0; i < up; i++) {
            int dot = base.lastIndexOf('.');
            if (dot < 0) {
                if (i == up - 1 && !base.isEmpty()) {
                    base = "";
                    continue;
                }
                throw new PythonSyntaxError(file, f.line, f.col, "attempted relative import beyond top-level package");
            }
            base = base.substring(0, dot);
        }
        if (f.module == null) {
            return base;
        }
        return base.isEmpty() ? f.module : base + "." + f.module;
    }

    private void ifStmt(If s, StringBuilder out, int ind) {
        indent(out, ind);
        out.append("if (").append(cond(s.test)).append(") {\n");
        block(s.body, out, ind + 1);
        If cur = s;
        while (cur.orelse.size() == 1 && cur.orelse.get(0) instanceof If) {
            cur = (If) cur.orelse.get(0);
            indent(out, ind);
            out.append("} else if (").append(cond(cur.test)).append(") {\n");
            block(cur.body, out, ind + 1);
        }
        if (!cur.orelse.isEmpty()) {
            indent(out, ind);
            out.append("} else {\n");
            block(cur.orelse, out, ind + 1);
        }
        indent(out, ind);
        out.append("}\n");
    }

    private void whileStmt(While w, StringBuilder out, int ind) {
        String label = fresh("L");
        String outer = w.orelse.isEmpty() ? label : fresh("E");
        if (!w.orelse.isEmpty()) {
            indent(out, ind);
            out.append(outer).append(": {\n");
            ind++;
        }
        indent(out, ind);
        out.append(label).append(": while (").append(cond(w.test)).append(") {\n");
        loops.add(new Loop(label, outer));
        block(w.body, out, ind + 1);
        loops.remove(loops.size() - 1);
        indent(out, ind);
        out.append("}\n");
        if (!w.orelse.isEmpty()) {
            block(w.orelse, out, ind);
            indent(out, ind - 1);
            out.append("}\n");
        }
    }

    private void forStmt(For f, StringBuilder out, int ind) {
        if (f.isAsync) {
            throw unsupported(f, "async for");
        }
        String label = fresh("L");
        String outer = f.orelse.isEmpty() ? label : fresh("E");
        if (!f.orelse.isEmpty()) {
            indent(out, ind);
            out.append(outer).append(": {\n");
            ind++;
        }
        Map<String, String[]> lc = loopCarried(f);
        if (lc != null) {
            for (String[] c : lc.values()) {
                indent(out, ind);
                out.append("double ").append(c[0]).append(" = 0;\n");
                indent(out, ind);
                out.append("boolean ").append(c[1]).append(" = false;\n");
            }
        }
        String it = fresh("i");
        String x = fresh("x");
        String[] lazy = null;
        int vpos = -1;
        String[] vsig = null;
        if (rangeCall(f.iter)) {
            // `for x in range(...)` counts in a long while `range` is still
            // the builtin and its arguments are ints, so the loop allocates
            // no range object and no iterator; anything else iterates.
            List<Expr> ra = ((Call) f.iter).args;
            String rf = fresh("rf");
            indent(out, ind);
            out.append("final Object ").append(rf).append(" = ").append(expr(((Call) f.iter).func)).append(";\n");
            String[] av = new String[3];
            for (int i = 0; i < ra.size(); i++) {
                av[i] = fresh("ra");
                indent(out, ind);
                out.append("final Object ").append(av[i]).append(" = ").append(expr(ra.get(i))).append(";\n");
            }
            String start = ra.size() == 1 ? "Py.of(0L)" : av[0];
            String stop = ra.size() == 1 ? av[0] : av[1];
            String step = ra.size() == 3 ? av[2] : "Py.of(1L)";
            String ri = fresh("ri");
            String re = fresh("re");
            String rs = fresh("rs");
            indent(out, ind);
            out.append("long ").append(ri).append(" = 0, ").append(re).append(" = 0, ").append(rs).append(" = 0;\n");
            if (lc != null && f.target instanceof Name && sc.resolve(((Name) f.target).id) == Scope.Binding.LOCAL) {
                // The loop body is one float region (loopCarried), so the
                // index stays a long: the region reads `ri` itself, and the
                // local gets its Long only when the general path runs or the
                // loop ends. A counted loop then allocates nothing.
                lazy = new String[] {((Name) f.target).id, it, ri, rs, fresh("rst")};
                String[] saved = lazyIndex;
                lazyIndex = lazy;
                for (Stmt st : f.body) {
                    // A plain int result would come out a float.
                    if (st instanceof Assign && regionInt(((Assign) st).value)) {
                        lazy = null;
                    }
                }
                lazyIndex = saved;
            }
            indent(out, ind);
            out.append("PyObject ").append(it).append(" = null;\n");
            indent(out, ind);
            out.append("if (Py.isIntRange(").append(rf).append(", ").append(start).append(", ").append(stop)
                    .append(", ").append(step).append(")) {\n");
            indent(out, ind + 1);
            out.append(ri).append(" = Py.asLong(").append(start).append(");\n");
            indent(out, ind + 1);
            out.append(re).append(" = Py.asLong(").append(stop).append(");\n");
            indent(out, ind + 1);
            out.append(rs).append(" = Py.asLong(").append(step).append(");\n");
            indent(out, ind);
            out.append("} else {\n");
            indent(out, ind + 1);
            out.append(it).append(" = Py.iter(Py.call(").append(rf);
            for (int i = 0; i < ra.size(); i++) {
                out.append(", ").append(av[i]);
            }
            out.append("));\n");
            indent(out, ind);
            out.append("}\n");
            if (lazy != null) {
                indent(out, ind);
                out.append("final long ").append(lazy[4]).append(" = ").append(ri).append(";\n");
                vpos = out.length();
                vsig = new String[] {it, ri, re, rs, Integer.toString(ind)};
            }
            indent(out, ind);
            out.append(label).append(": for (Object ").append(x).append("; (").append(x).append(" = ").append(it)
                    .append(" != null ? ").append(it).append(".next() : (").append(rs).append(" > 0 ? ").append(ri)
                    .append(" < ").append(re).append(" : ").append(ri).append(" > ").append(re)
                    .append(") ? ").append(lazy != null ? "Py.NONE" : "Long.valueOf(" + ri + ")")
                    .append(" : null) != null; ").append(ri).append(" += ").append(rs).append(") {\n");
        } else {
            // A list is walked by index (re-reading its length every step, as
            // its own iterator does), so the loop allocates no iterator.
            String sq = fresh("sq");
            String ls = fresh("ls");
            String li = fresh("li");
            indent(out, ind);
            String src;
            if (splitWords(f.iter)) {
                src = "Py.splitWords(" + expr(((Attribute) ((Call) f.iter).func).value) + ")";
            } else if (splitOn(f.iter)) {
                Call sc1 = (Call) f.iter;
                src = "Py.splitOn(" + expr(((Attribute) sc1.func).value) + ", " + expr(sc1.args.get(0)) + ")";
            } else {
                src = expr(f.iter);
            }
            out.append("final Object ").append(sq).append(" = ").append(src).append(";\n");
            indent(out, ind);
            out.append("final PyList ").append(ls).append(" = ").append(sq).append(" instanceof PyList ? (PyList) ")
                    .append(sq).append(" : null;\n");
            indent(out, ind);
            out.append("final PyObject ").append(it).append(" = ").append(ls).append(" != null ? null : Py.iter(")
                    .append(sq).append(");\n");
            indent(out, ind);
            out.append("int ").append(li).append(" = 0;\n");
            indent(out, ind);
            out.append(label).append(": for (Object ").append(x).append("; (").append(x).append(" = ").append(it)
                    .append(" != null ? ").append(it).append(".next() : ").append(li).append(" < ").append(ls)
                    .append(".size() ? ").append(ls).append(".get(").append(li).append("++) : null) != null;) {\n");
        }
        if (lazy != null) {
            indent(out, ind + 1);
            out.append("if (").append(it).append(" != null) {\n");
            store(f.target, x, out, ind + 2);
            indent(out, ind + 1);
            out.append("}\n");
        } else {
            store(f.target, x, out, ind + 1);
        }
        loops.add(new Loop(label, outer));
        Map<String, String[]> savedCarried = carried;
        String[] savedLazy = lazyIndex;
        carried = lc;
        lazyIndex = lazy;
        String[] savedVersion = version;
        StringBuilder savedVersionOut = versionOut;
        StringBuilder vout = new StringBuilder();
        version = vsig;
        versionOut = vout;
        block(f.body, out, ind + 1);
        version = savedVersion;
        versionOut = savedVersionOut;
        carried = savedCarried;
        lazyIndex = savedLazy;
        if (vpos >= 0) {
            out.insert(vpos, vout);
        }
        loops.remove(loops.size() - 1);
        indent(out, ind);
        out.append("}\n");
        if (lazy != null) {
            indent(out, ind);
            out.append("if (").append(it).append(" == null && ").append(lazy[2]).append(" != ").append(lazy[4])
                    .append(") {\n");
            indent(out, ind + 1);
            out.append(localVar(sc, lazy[0])).append(" = Long.valueOf(").append(lazy[2]).append(" - ")
                    .append(lazy[3]).append(");\n");
            indent(out, ind);
            out.append("}\n");
        }
        if (lc != null) {
            // Only a local something else reads needs its object back; the
            // marks let resolveRegions drop the write-back of the others.
            indent(out, ind);
            out.append(REGION_START).append("\n");
            for (Map.Entry<String, String[]> e : lc.entrySet()) {
                indent(out, ind);
                out.append(IF_SHARED).append(localVar(sc, e.getKey())).append("*/");
                StringBuilder w = new StringBuilder();
                syncCarried(e.getKey(), e.getValue(), w, 0);
                out.append(w.toString().replace("\n", " ").trim()).append(COND_END).append("\n");
            }
            indent(out, ind);
            out.append(REGION_END).append("\n");
        }
        if (!f.orelse.isEmpty()) {
            block(f.orelse, out, ind);
            indent(out, ind - 1);
            out.append("}\n");
        }
    }

    private void tryStmt(Try t, StringBuilder out, int ind) {
        if (t.star) {
            throw unsupported(t, "except*");
        }
        boolean fin = !t.finalbody.isEmpty();
        if (fin) {
            indent(out, ind);
            out.append("try {\n");
            ind++;
        }
        if (t.handlers.isEmpty()) {
            block(t.body, out, ind);
        } else {
            String ok = t.orelse.isEmpty() ? null : fresh("ok");
            if (ok != null) {
                indent(out, ind);
                out.append("boolean ").append(ok).append(" = false;\n");
            }
            // A typed call's result an exception left untaken (see
            // Typed.take) is dropped where the exception is caught.
            String tk = fresh("tk");
            indent(out, ind);
            out.append("final int ").append(tk).append(" = Typed.taken();\n");
            indent(out, ind);
            out.append("try {\n");
            block(t.body, out, ind + 1);
            if (ok != null) {
                indent(out, ind + 1);
                out.append(ok).append(" = true;\n");
            }
            String th = fresh("th");
            String pe = fresh("pe");
            indent(out, ind);
            out.append("} catch (Throwable ").append(th).append(") {\n");
            indent(out, ind + 1);
            out.append("Typed.untake(").append(tk).append(");\n");
            indent(out, ind + 1);
            out.append("final PyException ").append(pe).append(" = Py.convert(").append(th).append(");\n");
            handlers.add(pe);
            boolean catchAll = false;
            for (int i = 0; i < t.handlers.size(); i++) {
                ExceptHandler h = t.handlers.get(i);
                indent(out, ind + 1);
                if (h.type == null) {
                    if (i != t.handlers.size() - 1) {
                        throw new PythonSyntaxError(file, h.line, h.col, "default 'except:' must be last");
                    }
                    out.append(i == 0 ? "{\n" : "} else {\n");
                    catchAll = true;
                } else {
                    out.append(i == 0 ? "if (" : "} else if (").append(pe).append(".matches(").append(expr(h.type))
                            .append(")) {\n");
                }
                if (h.name != null) {
                    storeName(h.name, pe + ".value", out, ind + 2);
                }
                // sys.exception() answers this exception while the body runs.
                String hv = fresh("hv");
                indent(out, ind + 2);
                out.append("final Object ").append(hv).append(" = Py.enterHandler(").append(pe).append(");\n");
                indent(out, ind + 2);
                out.append("try {\n");
                block(h.body, out, ind + 3);
                indent(out, ind + 2);
                out.append("} finally {\n");
                indent(out, ind + 3);
                out.append("Py.exitHandler(").append(hv).append(");\n");
                indent(out, ind + 2);
                out.append("}\n");
                if (h.name != null && sc.kind != Scope.Kind.CLASS) {
                    indent(out, ind + 2);
                    out.append(nameLvalue(h.name)).append(" = null;\n");
                }
            }
            if (!catchAll) {
                indent(out, ind + 1);
                out.append("} else {\n");
                indent(out, ind + 2);
                out.append("throw ").append(pe).append(";\n");
            }
            indent(out, ind + 1);
            out.append("}\n");
            handlers.remove(handlers.size() - 1);
            indent(out, ind);
            out.append("}\n");
            if (ok != null) {
                indent(out, ind);
                out.append("if (").append(ok).append(") {\n");
                block(t.orelse, out, ind + 1);
                indent(out, ind);
                out.append("}\n");
            }
        }
        if (fin) {
            ind--;
            indent(out, ind);
            out.append("} finally {\n");
            block(t.finalbody, out, ind + 1);
            indent(out, ind);
            out.append("}\n");
        }
    }

    private void withItems(With w, int i, StringBuilder out, int ind) {
        if (i == w.items.size()) {
            block(w.body, out, ind);
            return;
        }
        WithItem item = w.items.get(i);
        String mgr = fresh("w");
        String exit = fresh("ex");
        String ok = fresh("ok");
        String th = fresh("th");
        String pe = fresh("pe");
        indent(out, ind);
        out.append("final Object ").append(mgr).append(" = ").append(expr(item.context)).append(";\n");
        indent(out, ind);
        out.append("final Object ").append(exit).append(" = Py.getattr(").append(mgr)
                .append(", \"__exit__\");\n");
        String entered = "Py.callMethod(" + mgr + ", \"__enter__\")";
        if (item.vars != null) {
            String ev = fresh("en");
            indent(out, ind);
            out.append("final Object ").append(ev).append(" = ").append(entered).append(";\n");
            store(item.vars, ev, out, ind);
        } else {
            indent(out, ind);
            out.append(entered).append(";\n");
        }
        indent(out, ind);
        out.append("boolean ").append(ok).append(" = true;\n");
        String tk = fresh("tk");
        indent(out, ind);
        out.append("final int ").append(tk).append(" = Typed.taken();\n");
        indent(out, ind);
        out.append("try {\n");
        withItems(w, i + 1, out, ind + 1);
        indent(out, ind);
        out.append("} catch (Throwable ").append(th).append(") {\n");
        indent(out, ind + 1);
        out.append("Typed.untake(").append(tk).append(");\n");
        indent(out, ind + 1);
        out.append(ok).append(" = false;\n");
        indent(out, ind + 1);
        out.append("final PyException ").append(pe).append(" = Py.convert(").append(th).append(");\n");
        indent(out, ind + 1);
        out.append("if (!Py.isTrue(Py.call(").append(exit).append(", ").append("Py.typeOf(").append(pe).append(".value), ")
                .append(pe).append(".value, Py.NONE))) {\n");
        indent(out, ind + 2);
        out.append("throw ").append(pe).append(";\n");
        indent(out, ind + 1);
        out.append("}\n");
        indent(out, ind);
        out.append("} finally {\n");
        indent(out, ind + 1);
        out.append("if (").append(ok).append(") {\n");
        indent(out, ind + 2);
        out.append("Py.call(").append(exit).append(", Py.NONE, Py.NONE, Py.NONE);\n");
        indent(out, ind + 1);
        out.append("}\n");
        indent(out, ind);
        out.append("}\n");
    }

    private void classDef(ClassDef c, StringBuilder out, int ind) {
        Layouts.Cls lc = layouts.classes.get(c);
        String k = lc != null ? lc.typeVar : "k" + id(c.scope);
        c.scope.classVar = k;
        StringBuilder bases = new StringBuilder();
        StringBuilder stars = new StringBuilder();
        boolean starred = false;
        for (Expr b : c.bases) {
            boolean s = b instanceof Starred;
            starred |= s;
            bases.append(bases.length() == 0 ? "" : ", ").append(expr(s ? ((Starred) b).value : b));
            stars.append(stars.length() == 0 ? "" : ", ").append(s);
        }
        // The keywords (metaclass= among them) are evaluated before the
        // body, as the bases are; the metaclass runs after it (see
        // PyType.finish).
        StringBuilder kwNames = new StringBuilder();
        StringBuilder kwValues = new StringBuilder();
        for (Keyword kw : c.keywords) {
            String kv = fresh("kw");
            indent(out, ind);
            out.append("final Object ").append(kv).append(" = ").append(expr(kw.value)).append(";\n");
            kwNames.append(kwNames.length() == 0 ? "" : ", ").append(kw.arg == null ? "null" : javaString(kw.arg));
            kwValues.append(kwValues.length() == 0 ? "" : ", ").append(kv);
        }
        List<String> decos = new ArrayList<String>();
        for (Expr d : c.decorators) {
            String dv = fresh("d");
            indent(out, ind);
            out.append("final Object ").append(dv).append(" = ").append(expr(d)).append(";\n");
            decos.add(dv);
        }
        indent(out, ind);
        if (lc != null) {
            fields.append("    private static PyType ").append(k).append(";\n");
            out.append(k);
        } else {
            out.append("final PyType ").append(k);
        }
        out.append(" = PyType.newClass(").append(javaString(c.name));
        if (starred) {
            out.append(", Py.spread(new Object[] {").append(bases).append("}, new boolean[] {").append(stars)
                    .append("}));\n");
        } else {
            out.append(", new Object[] {").append(bases).append("});\n");
        }
        indent(out, ind);
        out.append(k).append(".define(\"__module__\", G[0]);\n");
        indent(out, ind);
        out.append(k).append(".define(\"__qualname__\", Py.str(").append(javaString(c.name)).append("));\n");
        boolean anns = false;
        for (Stmt s : c.body) {
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
                anns = true;
            }
        }
        if (anns) {
            indent(out, ind);
            out.append(k).append(".define(\"__annotations__\", new PyDict());\n");
        }
        List<Stmt> body = c.body;
        if (!body.isEmpty() && isDocstring(body.get(0))) {
            indent(out, ind);
            out.append(k).append(".define(\"__doc__\", ").append(constant((Constant) ((ExprStmt) body.get(0)).value))
                    .append(");\n");
        }
        Scope saved = sc;
        List<Loop> savedLoops = new ArrayList<Loop>(loops);
        loops.clear();
        sc = c.scope;
        block(body, out, ind);
        sc = saved;
        loops.addAll(savedLoops);
        if (lc != null) {
            registerLayout(lc, out, ind);
        }
        String v = fresh("c");
        indent(out, ind);
        out.append("final Object ").append(v).append(" = PyType.finish(").append(k);
        if (c.keywords.isEmpty()) {
            out.append(", Py.NO_KW, Py.NO_ARGS);\n");
        } else {
            out.append(", new String[] {").append(kwNames).append("}, new Object[] {").append(kwValues).append("});\n");
        }
        for (int i = decos.size() - 1; i >= 0; i--) {
            v = "Py.call(" + decos.get(i) + ", " + v + ")";
        }
        storeName(c.name, v, out, ind);
    }

    // ---- functions -------------------------------------------------------

    /// `__qualname__`: the enclosing classes and functions, as CPython
    /// names them. Comprehensions are inlined (PEP 709) and add nothing.
    private static String qualname(Scope fs) {
        StringBuilder q = new StringBuilder(fs.name);
        for (Scope s = fs.parent; s != null && s.kind != Scope.Kind.MODULE; s = s.parent) {
            if (s.kind == Scope.Kind.CLASS) {
                q.insert(0, s.name + ".");
            } else if (s.kind != Scope.Kind.COMPREHENSION) {
                q.insert(0, s.name + ".<locals>.");
            }
        }
        return q.toString();
    }

    /// A `new PyFunction(...)` expression. `exprBody` is set for a lambda.
    private String function(String name, Arguments args, Scope fs, List<Stmt> body, Expr exprBody) {
        boolean async = nextAsync;
        nextAsync = false;
        boolean generator = body != null && (async || fs.hasYield);
        GenLowering savedGen = gen;
        gen = null;
        if (body != null) {
            defAssign.function(body, fs.params);
        }
        String asStatic = staticBody;
        Layouts.Cls asClass = staticClass;
        staticBody = null;
        staticClass = null;
        String savedSelfLocal = selfLocal;
        Layouts.Cls savedSelfCls = selfCls;
        String savedSelfTyped = selfTyped;
        Map<String, String> savedShadowVar = shadowVar;
        Map<String, Layouts.Cls> savedShadowCls = shadowCls;
        shadowVar = new HashMap<String, String>();
        shadowCls = new HashMap<String, Layouts.Cls>();
        selfLocal = null;
        selfCls = null;
        selfTyped = null;
        StringBuilder sig = new StringBuilder();
        List<String> names = args.names();
        sig.append("new PyFunction(").append(javaString(name)).append(", new String[] {");
        for (int i = 0; i < names.size(); i++) {
            sig.append(i == 0 ? "" : ", ").append(javaString(names.get(i)));
        }
        sig.append("}, ").append(args.positional.size()).append(", ").append(args.vararg != null).append(", ")
                .append(args.kwonly.size()).append(", ").append(args.kwarg != null).append(", ");
        if (args.defaults.isEmpty()) {
            sig.append("Py.NO_ARGS, ");
        } else {
            sig.append("new Object[] {");
            for (int i = 0; i < args.defaults.size(); i++) {
                sig.append(i == 0 ? "" : ", ").append(expr(args.defaults.get(i)));
            }
            sig.append("}, ");
        }
        boolean anyKwDefault = false;
        for (Expr e : args.kwDefaults) {
            anyKwDefault |= e != null;
        }
        if (!anyKwDefault) {
            sig.append("null, ");
        } else {
            sig.append("new Object[] {");
            for (int i = 0; i < args.kwDefaults.size(); i++) {
                Expr e = args.kwDefaults.get(i);
                sig.append(i == 0 ? "" : ", ").append(e == null ? "null" : expr(e));
            }
            sig.append("}, ");
        }

        Frame savedFrame = frame;
        Scope savedScope = sc;
        List<Loop> savedLoops = new ArrayList<Loop>(loops);
        List<String> savedHandlers = new ArrayList<String>(handlers);
        loops.clear();
        handlers.clear();
        frame = new Frame(id(fs), fs);
        sc = fs;
        boolean savedHandlersFlag = fnHasHandlers;
        Map<String, String[]> savedCarried = carried;
        carried = null;
        // A generator's locals are fields that outlive a step, so none is
        // carried in a Java local across a loop.
        fnHasHandlers = body != null && (generator || hasHandlers(body));
        Set<String> savedRecycled = recycled;
        recycled = body != null ? ListRecycling.find(body, names, fs) : java.util.Collections.<String>emptySet();
        List<String> pnames = args.names();
        boolean directParams = args.vararg == null && args.kwarg == null && args.kwonly.isEmpty() && pnames.size() <= 4;
        if (asStatic != null && asClass != null && directParams && !pnames.isEmpty()
                && !fs.assigned.contains(pnames.get(0)) && !fs.cells.contains(pnames.get(0))) {
            selfLocal = localVar(fs, pnames.get(0));
            selfCls = asClass;
            selfTyped = "k" + frame.id;
        }
        if (body != null && !generator && !layouts.fieldOwners.isEmpty()) {
            for (Map.Entry<String, Layouts.Cls> e : ShadowTypes.of(body, layouts).entrySet()) {
                String n = e.getKey();
                if (fs.resolve(n) != Scope.Binding.LOCAL || fs.cells.contains(n)) {
                    continue;
                }
                String v = localVar(fs, n);
                if (v.equals(selfLocal)) {
                    continue;
                }
                shadowVar.put(v, "h" + frame.id + "_" + mangle(n));
                shadowCls.put(v, e.getValue());
            }
        }
        StringBuilder b = new StringBuilder();
        int ind = 3;
        if (exprBody != null) {
            String e = expr(exprBody);
            indent(b, ind);
            b.append("return ").append(e).append(";\n");
        } else if (generator) {
            gen = new GenLowering(this, file, fs);
            gen.asyncGen = async && fs.yields;
            b.append(resolveRegions(gen.step(body)));
        } else {
            block(body, b, ind);
            indent(b, ind);
            b.append("return Py.NONE;\n");
            String settled = resolveRegions(b.toString());
            b.setLength(0);
            b.append(settled);
            Infer.Fn tf = typedEmit != null ? typedEmit.fnFor(fs) : null;
            if (tf != null) {
                // Enter the typed version (TypedEmit) when the arguments fit.
                List<String> locals = new ArrayList<String>();
                for (String n : names) {
                    locals.add(localVar(fs, n));
                }
                StringBuilder g = new StringBuilder();
                for (String line : typedEmit.entry(tf, locals).split("\n")) {
                    indent(g, ind);
                    g.append(line).append('\n');
                }
                b.insert(0, g);
            }
        }
        String a = "a" + frame.id;
        // Only positional parameters, at most four: the body takes them as
        // Java parameters (PyFunction.Body0..Body4), so a call allocates no
        // argument array.
        boolean direct = args.vararg == null && args.kwarg == null && args.kwonly.isEmpty() && names.size() <= 4;
        StringBuilder pro = new StringBuilder();
        Set<String> declared = new LinkedHashSet<String>();
        for (int i = 0; i < names.size(); i++) {
            String n = names.get(i);
            declared.add(n);
            indent(pro, ind);
            if (fs.cells.contains(n)) {
                pro.append("final Object[] ").append(cellVar(fs, n)).append(" = new Object[] {").append(param(a, i, direct))
                        .append("};\n");
            } else {
                pro.append("Object ").append(localVar(fs, n)).append(" = ").append(param(a, i, direct))
                        .append(";\n");
            }
        }
        for (String n : fs.assigned) {
            if (declared.contains(n) || !fs.isLocal(n)) {
                continue;
            }
            declared.add(n);
            indent(pro, ind);
            if (fs.cells.contains(n)) {
                pro.append("final Object[] ").append(cellVar(fs, n)).append(" = new Object[1];\n");
            } else {
                pro.append("Object ").append(localVar(fs, n)).append(" = null;\n");
            }
        }
        if (selfTyped != null) {
            indent(pro, ind);
            pro.append("final ").append(selfCls.javaName).append(' ').append(selfTyped).append(" = ").append(selfLocal)
                    .append(" instanceof ").append(selfCls.javaName).append(" ? (").append(selfCls.javaName).append(") ")
                    .append(selfLocal).append(" : null;\n");
        }
        for (Map.Entry<String, String> e : shadowVar.entrySet()) {
            String k = shadowCls.get(e.getKey()).javaName;
            indent(pro, ind);
            // Every local is declared above (parameters with their value,
            // the rest null), so testing it here is right for both.
            pro.append(k).append(' ').append(e.getValue()).append(" = ").append(e.getKey()).append(" instanceof ")
                    .append(k).append(" ? (").append(k).append(") ").append(e.getKey()).append(" : null;\n");
        }
        selfLocal = savedSelfLocal;
        selfCls = savedSelfCls;
        selfTyped = savedSelfTyped;
        shadowVar = savedShadowVar;
        shadowCls = savedShadowCls;
        String temps = frame.temps > 0 || frame.ltemps > 0 ? tempDecls() : null;
        if (generator) {
            // The locals become fields of the generator object, and the
            // body its step(); temporaries never live across a suspension.
            StringBuilder g = new StringBuilder();
            indent(g, ind);
            if (async && fs.yields) {
                g.append("return new PyGenerator.Async(").append(javaString(name)).append(") {\n");
            } else {
                g.append("return new PyGenerator(").append(javaString(name)).append(", ").append(async).append(") {\n");
            }
            g.append(pro.toString().replaceAll("(?m)^ {12}", "                "));
            g.append(gen.fields());
            if (gen.usesReturnField()) {
                g.append("                Object ret$;\n");
            }
            g.append("\n                @Override\n                protected Object step() {\n");
            if (temps != null) {
                g.append("                    ").append(temps).append("\n");
            }
            g.append(b);
            g.append("                }\n");
            indent(g, ind);
            g.append("};\n");
            pro.setLength(0);
            b.setLength(0);
            b.append(g);
        } else if (temps != null) {
            indent(pro, ind);
            pro.append(temps).append("\n");
        }
        gen = savedGen;
        frame = savedFrame;
        sc = savedScope;
        fnHasHandlers = savedHandlersFlag;
        recycled = savedRecycled;
        carried = savedCarried;
        loops.clear();
        loops.addAll(savedLoops);
        handlers.clear();
        handlers.addAll(savedHandlers);

        StringBuilder r = new StringBuilder(sig);
        r.append('(');
        StringBuilder plist = new StringBuilder();
        StringBuilder alist = new StringBuilder();
        if (direct) {
            for (int i = 0; i < names.size(); i++) {
                plist.append(i == 0 ? "" : ", ").append("Object ").append(param(a, i, true));
                alist.append(i == 0 ? "" : ", ").append(param(a, i, true));
            }
        } else {
            plist.append("Object[] ").append(a);
            alist.append(a);
        }
        r.append(plist);
        if (asStatic != null && direct) {
            // A compiled method: the body is a static method the class
            // layout's dispatch calls directly (see Layouts).
            fields.append("    static Object ").append(asStatic).append("(").append(plist).append(") {\n")
                    .append(pro).append(b).append("    }\n");
            r.append(") -> ").append(asStatic).append("(").append(alist).append("))");
        } else {
            r.append(") -> {\n").append(pro).append(b);
            indent(r, ind - 1);
            r.append("})");
        }
        Expr ret = generator ? null : pureReturn(args, names, body, exprBody);
        if (ret != null) {
            String pure = pureVariant(names, ret);
            if (pure != null) {
                r.append(".pure(").append(pure).append(")");
                String pureL = pureLongVariant(names, ret);
                if (pureL != null) {
                    boolean module = sc == moduleScope && directLong.containsKey(name); //NOPMD CompareObjectsWithEquals - identity is meant
                    String method = module ? "PL_" + mangle(name) : "PL" + fusedCount++;
                    StringBuilder lsig = new StringBuilder();
                    StringBuilder lcall = new StringBuilder();
                    for (int i = 0; i < names.size(); i++) {
                        lsig.append(i == 0 ? "" : ", ").append("long q").append(i);
                        lcall.append(i == 0 ? "" : ", ").append("q").append(i);
                    }
                    fields.append("    private static double ").append(method).append("(").append(lsig)
                            .append(") {\n").append(pureL).append("    }\n");
                    r.append(".pure((PyFunction.PureL").append(names.size()).append(") (").append(lsig)
                            .append(") -> ").append(method).append("(").append(lcall).append("))");
                    if (module) {
                        fields.append("    private static Object DEF_").append(mangle(name)).append(";\n");
                        r.insert(0, "(DEF_" + mangle(name) + " = ").append(")");
                    }
                }
            }
        }
        String q = qualname(fs);
        if (!q.equals(name)) {
            r.append(".qual(").append(javaString(q)).append(")");
        }
        if (generator) {
            // co_flags' CO_GENERATOR, CO_COROUTINE or CO_ASYNC_GENERATOR,
            // which inspect reads.
            r.append(".kind(").append(async && fs.yields ? 0x200 : async ? 0x80 : 0x20).append(")");
        }
        return r.toString();
    }

    /// Parameter `i` of a function body: its own Java parameter, or an
    /// element of the argument array.
    private static String param(String a, int i, boolean direct) {
        return direct ? a + "_" + i : a + "[" + i + "]";
    }

    // ---- expressions -----------------------------------------------------

    private String temp() {
        return "t" + frame.id + "_" + (frame.temps++);
    }

    /// The temporaries of the current frame, as plain locals: an array of
    /// them would be one more allocation on every call.
    private String tempDecls() {
        StringBuilder sb = new StringBuilder();
        if (frame.temps > 0) {
            sb.append("Object ");
            for (int i = 0; i < frame.temps; i++) {
                sb.append(i == 0 ? "" : ", ").append("t").append(frame.id).append("_").append(i).append(" = null");
            }
            sb.append(";");
        }
        if (frame.ltemps > 0) {
            sb.append(sb.length() == 0 ? "" : " ").append("long ");
            for (int i = 0; i < frame.ltemps; i++) {
                sb.append(i == 0 ? "" : ", ").append("j").append(frame.id).append("_").append(i).append(" = 0");
            }
            sb.append(";");
        }
        return sb.toString();
    }

    /// The int constants among the hoisted fields: an operand known to be
    /// one needs no test and no unboxing.
    private final Map<String, Long> constLongs = new HashMap<String, Long>();

    /// The Java long for operand `v` once `t` holds it (and has been tested
    /// when not a constant).
    /// The hoisted constant for int `l`, known to the inline int paths.
    private String intConst(long l) {
        String k = hoist("Object", "Py.of(" + l + "L)");
        constLongs.put(k, Long.valueOf(l));
        return k;
    }

    private String longOf(String v, String t) {
        Long c = constLongs.get(v);
        return c != null ? c.longValue() + "L" : "((Long) " + t + ").longValue()";
    }

    /// `(t = v) instanceof Long`, or true for an int constant (still
    /// assigned, for the slow path).
    private String isLong(String v, String t) {
        return constLongs.containsKey(v) ? "(" + t + " = " + v + ") != null" : "(" + t + " = " + v + ") instanceof Long";
    }

    private String ltemp() {
        return "j" + frame.id + "_" + (frame.ltemps++);
    }

    /// The truth of `v` without a call for a bool or None.
    private String truth(String v) {
        String t = temp();
        return "((" + t + " = " + v + ") == Py.TRUE || " + t + " != Py.FALSE && " + t + " != Py.NONE && (" + t
                + " instanceof Long ? ((Long) " + t + ").longValue() != 0 : Py.isTrue(" + t + ")))";
    }

    /// A Java boolean for `l op r` with the int case inline: both operands
    /// are evaluated, in order, before either is tested.
    private String inlineCmp(String op, String l, String r) {
        String jop;
        String slow;
        switch (op) {
            case "<":
                jop = "<";
                slow = "Py.ltBool(";
                break;
            case "==":
                jop = "==";
                slow = "Py.eqBool(";
                break;
            case "!=":
                jop = "!=";
                slow = "!Py.eqBool(";
                break;
            case "<=":
                jop = "<=";
                slow = "Py.isTrue(Py.le(";
                break;
            case ">":
                jop = ">";
                slow = "Py.isTrue(Py.gt(";
                break;
            case ">=":
                jop = ">=";
                slow = "Py.isTrue(Py.ge(";
                break;
            default:
                return null;
        }
        String t = temp();
        String u = temp();
        String close = slow.startsWith("Py.isTrue") ? "))" : ")";
        return "((" + isLong(l, t) + " & " + isLong(r, u) + ") ? " + longOf(l, t) + " " + jop + " " + longOf(r, u) + " : "
                + slow + t + ", " + u + close + ")";
    }

    /// `l + r` or `l - r` with the int case inline (overflow goes the
    /// generic way, to a big int); null for any other operator.
    private String inlineArith(String op, String l, String r, boolean inPlace) {
        if (!"+".equals(op) && !"-".equals(op)) {
            return null;
        }
        String t = temp();
        String u = temp();
        String x = ltemp();
        String y = ltemp();
        String z = ltemp();
        String overflow = "+".equals(op)
                ? "((" + x + " ^ " + z + ") & (" + y + " ^ " + z + ")) >= 0"
                : "((" + x + " ^ " + y + ") & (" + x + " ^ " + z + ")) >= 0";
        String slow = "Py." + (inPlace ? "i" : "") + BIN.get(op) + "(" + t + ", " + u + ")";
        return "((" + isLong(l, t) + " & " + isLong(r, u) + ") && ((" + z + " = (" + x + " = " + longOf(l, t) + ") " + op
                + " (" + y + " = " + longOf(r, u) + ")) == " + z + " & " + overflow + ") ? (Object) Long.valueOf(" + z
                + ") : " + slow + ")";
    }

    /// A Java boolean expression for a Python condition.
    String cond(Expr e) {
        if (e instanceof Constant && ((Constant) e).kind == Constant.Kind.TRUE) {
            // Not the literal: javac would then reject the code after a
            // `while True` it cannot see a break out of.
            return "(Py.TRUE == Py.TRUE)";
        }
        if (e instanceof UnaryOp && "not".equals(((UnaryOp) e).op)) {
            return "!(" + cond(((UnaryOp) e).operand) + ")";
        }
        if (e instanceof BoolOp) {
            BoolOp b = (BoolOp) e;
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < b.values.size(); i++) {
                if (i > 0) {
                    sb.append(b.isAnd ? " && " : " || ");
                }
                sb.append(cond(b.values.get(i)));
            }
            return sb.append(")").toString();
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            StringBuilder sb = new StringBuilder("(");
            String left = expr(c.left);
            for (int i = 0; i < c.ops.size(); i++) {
                String right = expr(c.comparators.get(i));
                if (i < c.ops.size() - 1) {
                    String t = temp();
                    right = "(" + t + " = " + right + ")";
                    sb.append(cmpBool(c.ops.get(i), left, right)).append(" && ");
                    left = t;
                } else {
                    sb.append(cmpBool(c.ops.get(i), left, right));
                }
            }
            return sb.append(")").toString();
        }
        if (e instanceof Constant) {
            Constant k = (Constant) e;
            if (k.kind == Constant.Kind.TRUE) {
                return "Py.isTrue(Py.TRUE)";
            }
            if (k.kind == Constant.Kind.FALSE || k.kind == Constant.Kind.NONE) {
                return "Py.isTrue(Py.FALSE)";
            }
        }
        return truth(expr(e));
    }

    private String cmpBool(String op, String l, String r) {
        String in = inlineCmp(op, l, r);
        if (in != null) {
            return in;
        }
        switch (op) {
            case "==":
                return "Py.eqBool(" + l + ", " + r + ")";
            case "!=":
                return "!Py.eqBool(" + l + ", " + r + ")";
            case "<":
                return "Py.ltBool(" + l + ", " + r + ")";
            case "is":
                return "(" + l + " == " + r + ")";
            case "is not":
                return "(" + l + " != " + r + ")";
            default:
                return "Py.isTrue(Py." + CMP.get(op) + "(" + l + ", " + r + "))";
        }
    }

    String expr(Expr e) {
        if (e instanceof Name) {
            return load((Name) e);
        }
        if (e instanceof Constant) {
            return constant((Constant) e);
        }
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            String f = fused(e);
            if (f != null) {
                return f;
            }
            String op = BIN.get(b.op);
            if (op == null) {
                return "Py.callMethod(" + expr(b.left) + ", \"__matmul__\", " + expr(b.right) + ")";
            }
            String in = inlineArith(b.op, expr(b.left), expr(b.right), false);
            if (in != null) {
                return in;
            }
            return "Py." + op + "(" + expr(b.left) + ", " + expr(b.right) + ")";
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            switch (u.op) {
                case "not":
                    return "(" + cond(e) + " ? Py.TRUE : Py.FALSE)";
                case "-": {
                    if (u.operand instanceof Constant && ((Constant) u.operand).kind == Constant.Kind.INT) {
                        BigInteger v = parseInt(((Constant) u.operand).text);
                        if (v.bitLength() < 63) {
                            return intConst(-v.longValue());
                        }
                    }
                    String t = temp();
                    String j = ltemp();
                    return "((" + t + " = " + expr(u.operand) + ") instanceof Long && (" + j + " = ((Long) " + t
                            + ").longValue()) != Long.MIN_VALUE ? (Object) Long.valueOf(-" + j + ") : Py.neg(" + t + "))";
                }
                case "+":
                    return "Py.pos(" + expr(u.operand) + ")";
                default:
                    return "Py.invert(" + expr(u.operand) + ")";
            }
        }
        if (e instanceof BoolOp) {
            BoolOp b = (BoolOp) e;
            if (boolValued(e)) {
                // Every operand is a bool, so the result is one too.
                return "(" + cond(e) + " ? Py.TRUE : Py.FALSE)";
            }
            String r = expr(b.values.get(b.values.size() - 1));
            for (int i = b.values.size() - 2; i >= 0; i--) {
                String t = temp();
                String test = truth(t + " = " + expr(b.values.get(i)));
                r = "(" + (b.isAnd ? "!" : "") + test + " ? " + t + " : " + r + ")";
            }
            return r;
        }
        if (e instanceof Compare) {
            Compare c = (Compare) e;
            if (c.ops.size() == 1 && boolValued(e)) {
                return "(" + cond(e) + " ? Py.TRUE : Py.FALSE)";
            }
            if (c.ops.size() == 1) {
                return "Py." + CMP.get(c.ops.get(0)) + "(" + expr(c.left) + ", " + expr(c.comparators.get(0)) + ")";
            }
            // a < b < c: each link's value; stop at the first false one.
            String left = expr(c.left);
            List<String> parts = new ArrayList<String>();
            for (int i = 0; i < c.ops.size(); i++) {
                String right = expr(c.comparators.get(i));
                if (i < c.ops.size() - 1) {
                    String t = temp();
                    parts.add("Py." + CMP.get(c.ops.get(i)) + "(" + left + ", " + t + " = " + right + ")");
                    left = t;
                } else {
                    parts.add("Py." + CMP.get(c.ops.get(i)) + "(" + left + ", " + right + ")");
                }
            }
            String r = parts.get(parts.size() - 1);
            for (int i = parts.size() - 2; i >= 0; i--) {
                String t = temp();
                r = "(!Py.isTrue(" + t + " = " + parts.get(i) + ") ? " + t + " : " + r + ")";
            }
            return r;
        }
        if (e instanceof IfExp) {
            IfExp i = (IfExp) e;
            return "(" + cond(i.test) + " ? " + expr(i.body) + " : " + expr(i.orelse) + ")";
        }
        if (e instanceof NamedExpr) {
            NamedExpr n = (NamedExpr) e;
            String n2 = n.target.id;
            if (sc.resolve(n2) == Scope.Binding.CLASS) {
                throw unsupported(e, "an assignment expression in a class body");
            }
            return "(" + nameLvalue(n2) + " = " + expr(n.value) + ")";
        }
        if (e instanceof Attribute) {
            Attribute a = (Attribute) e;
            if (FRAME_HOOKS.contains(a.attr) && a.value instanceof Name && "sys".equals(((Name) a.value).id)) {
                throw dynamicCode(a, "sys." + a.attr);
            }
            if (layouts.fieldOwners.containsKey(a.attr)) {
                return fieldGet(a.attr, expr(a.value), site(a.attr));
            }
            return site(a.attr) + ".get(" + expr(a.value) + ")";
        }
        if (e instanceof Subscript) {
            Subscript s = (Subscript) e;
            if (s.index instanceof Slice) {
                Slice sl = (Slice) s.index;
                return "Py.getslice(" + expr(s.value) + ", " + exprOrNone(sl.lower) + ", " + exprOrNone(sl.upper)
                        + ", " + exprOrNone(sl.step) + ")";
            }
            String ci = constIndex(s.index);
            if (ci != null) {
                return "Py.getitem(" + expr(s.value) + ", " + ci + ")";
            }
            return "Py.getitem(" + expr(s.value) + ", " + expr(s.index) + ")";
        }
        if (e instanceof Slice) {
            Slice s = (Slice) e;
            return "Py.slice(" + exprOrNone(s.lower) + ", " + exprOrNone(s.upper) + ", " + exprOrNone(s.step) + ")";
        }
        if (e instanceof Call) {
            return call((Call) e);
        }
        if (e instanceof ListExpr) {
            return display("new PyList(", ((ListExpr) e).elts, ")", "Py.list(");
        }
        if (e instanceof TupleExpr) {
            List<Expr> elts = ((TupleExpr) e).elts;
            if (elts.isEmpty()) {
                return "PyTuple.EMPTY";
            }
            return display("new PyTuple(", elts, ")", "Py.tuple(");
        }
        if (e instanceof SetExpr) {
            List<Expr> elts = ((SetExpr) e).elts;
            if (elts.size() >= 3 && allConstant(elts)) {
                return display("PySet.ofConstants(", elts, ")", "PySet.ofConstants(");
            }
            return display("Py.set(", ((SetExpr) e).elts, ")", "Py.set(");
        }
        if (e instanceof DictExpr) {
            DictExpr d = (DictExpr) e;
            boolean spread = false;
            for (Expr k : d.keys) {
                spread |= k == null;
            }
            StringBuilder sb = new StringBuilder(spread ? "Py.dictSpread(" : "Py.dict(");
            for (int i = 0; i < d.keys.size(); i++) {
                Expr k = d.keys.get(i);
                sb.append(i == 0 ? "" : ", ").append(k == null ? "null" : expr(k)).append(", ")
                        .append(expr(d.values.get(i)));
            }
            return sb.append(")").toString();
        }
        if (e instanceof JoinedStr) {
            return joined((JoinedStr) e);
        }
        if (e instanceof FormattedValue) {
            JoinedStr j = new JoinedStr();
            j.parts.add(e);
            return joined(j);
        }
        if (e instanceof Lambda) {
            Lambda l = (Lambda) e;
            if (l.scope.hasYield) {
                throw unsupported(e, "generators and async");
            }
            return function("<lambda>", l.args, l.scope, null, l.body);
        }
        if (e instanceof Comp) {
            return comprehension((Comp) e);
        }
        if (e instanceof Starred) {
            throw new PythonSyntaxError(file, e.line, e.col, "can't use starred expression here");
        }
        if (e instanceof Yield || e instanceof Await) {
            throw unsupported(e, "generators and async");
        }
        throw unsupported(e, e.getClass().getSimpleName());
    }

    private String exprOrNone(Expr e) {
        return e == null ? "Py.NONE" : expr(e);
    }

    /// A list/tuple/set display; one with `*x` items goes through Py.spread.
    private String display(String open, List<Expr> elts, String close, String varargsOpen) {
        boolean star = false;
        for (Expr x : elts) {
            star |= x instanceof Starred;
        }
        if (!star) {
            StringBuilder sb = new StringBuilder(varargsOpen);
            for (int i = 0; i < elts.size(); i++) {
                sb.append(i == 0 ? "" : ", ").append(expr(elts.get(i)));
            }
            return sb.append(")").toString();
        }
        StringBuilder parts = new StringBuilder("new Object[] {");
        StringBuilder flags = new StringBuilder("new boolean[] {");
        for (int i = 0; i < elts.size(); i++) {
            Expr x = elts.get(i);
            boolean s = x instanceof Starred;
            parts.append(i == 0 ? "" : ", ").append(expr(s ? ((Starred) x).value : x));
            flags.append(i == 0 ? "" : ", ").append(s);
        }
        return open + "Py.spread(" + parts + "}, " + flags + "})" + close;
    }

    private String joined(JoinedStr j) {
        List<String> parts = new ArrayList<String>();
        for (Expr p : j.parts) {
            if (p instanceof Constant) {
                parts.add(constant((Constant) p));
                continue;
            }
            FormattedValue f = (FormattedValue) p;
            String v = expr(f.value);
            if (f.conversion == 'r') {
                v = "Py.str(Py.toRepr(" + v + "))";
            } else if (f.conversion == 's') {
                v = "Py.str(Py.strOf(" + v + "))";
            } else if (f.conversion == 'a') {
                v = "Py.call(Builtins.ASCII, " + v + ")";
            }
            String spec;
            if (f.spec == null) {
                spec = "\"\"";
            } else if (f.spec.parts.size() == 1 && f.spec.parts.get(0) instanceof Constant) {
                spec = javaString(((Constant) f.spec.parts.get(0)).text);
            } else if (f.spec.parts.isEmpty()) {
                spec = "\"\"";
            } else {
                spec = "Py.strOf(" + joined(f.spec) + ")";
            }
            parts.add("Py.format(" + v + ", " + spec + ")");
        }
        if (parts.size() == 1) {
            return parts.get(0);
        }
        StringBuilder sb = new StringBuilder("Py.concat(");
        for (int i = 0; i < parts.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(parts.get(i));
        }
        return sb.append(")").toString();
    }

    private String call(Call c) {
        if (!inTypeCall && c.func instanceof Name && "type".equals(((Name) c.func).id) && c.args.size() == 3
                && c.keywords.isEmpty() && sc.resolve("type") == Scope.Binding.BUILTIN) {
            // `type(name, bases, ns)` takes __module__ from the caller's
            // globals, which only the call site knows.
            inTypeCall = true;
            try {
                return "Py.typeModule(" + call(c) + ", M)";
            } finally {
                inTypeCall = false;
            }
        }
        if (c.func instanceof Name && c.args.isEmpty() && c.keywords.isEmpty()) {
            String fn = ((Name) c.func).id;
            if (("globals".equals(fn) || "locals".equals(fn)) && sc.resolve(fn) == Scope.Binding.BUILTIN) {
                if ("locals".equals(fn) && sc.kind != Scope.Kind.MODULE) {
                    // A function's locals are Java variables; there is no
                    // namespace to hand out.
                    throw unsupported(c, "locals() inside a function or class");
                }
                // A snapshot: the module's globals are fields, not a dict.
                return "M.globals()";
            }
        }
        if (c.func instanceof Name && "super".equals(((Name) c.func).id) && c.args.isEmpty()
                && c.keywords.isEmpty() && sc.resolve("super") == Scope.Binding.BUILTIN) {
            return zeroArgSuper(c);
        }
        if (c.func instanceof Name && "len".equals(((Name) c.func).id) && c.args.size() == 1
                && !(c.args.get(0) instanceof Starred) && c.keywords.isEmpty()
                && sc.resolve("len") == Scope.Binding.BUILTIN) {
            // Still the global's value that is called, so a module that
            // rebinds `len` later gets its own function.
            return "Py.len(" + expr(c.func) + ", " + expr(c.args.get(0)) + ")";
        }
        boolean spread = false;
        for (Expr a : c.args) {
            spread |= a instanceof Starred;
        }
        for (Keyword k : c.keywords) {
            spread |= k.arg == null;
        }
        if (spread) {
            StringBuilder parts = new StringBuilder("new Object[] {");
            StringBuilder kinds = new StringBuilder("new int[] {");
            StringBuilder names = new StringBuilder("new String[] {");
            int i = 0;
            for (Expr a : c.args) {
                boolean s = a instanceof Starred;
                parts.append(i == 0 ? "" : ", ").append(expr(s ? ((Starred) a).value : a));
                kinds.append(i == 0 ? "" : ", ").append(s ? 1 : 0);
                names.append(i == 0 ? "" : ", ").append("null");
                i++;
            }
            for (Keyword k : c.keywords) {
                parts.append(i == 0 ? "" : ", ").append(expr(k.value));
                kinds.append(i == 0 ? "" : ", ").append(k.arg == null ? 2 : 3);
                names.append(i == 0 ? "" : ", ").append(k.arg == null ? "null" : javaString(k.arg));
                i++;
            }
            return "Py.callSpread(" + expr(c.func) + ", " + parts + "}, " + kinds + "}, " + names + "})";
        }
        StringBuilder args = new StringBuilder();
        for (Expr a : c.args) {
            args.append(", ").append(expr(a));
        }
        if (!c.keywords.isEmpty()) {
            StringBuilder kw = new StringBuilder("new String[] {");
            for (int i = 0; i < c.keywords.size(); i++) {
                Keyword k = c.keywords.get(i);
                args.append(", ").append(expr(k.value));
                kw.append(i == 0 ? "" : ", ").append(javaString(k.arg));
            }
            String w = hoist("String[]", kw.append("}").toString());
            String arr = args.length() == 0 ? "Py.NO_ARGS" : "new Object[] {" + args.substring(2) + "}";
            return "Py.callKw(" + expr(c.func) + ", " + arr + ", " + w + ")";
        }
        if (c.func instanceof Attribute) {
            Attribute fa = (Attribute) c.func;
            if (FRAME_HOOKS.contains(fa.attr) && fa.value instanceof Name && "sys".equals(((Name) fa.value).id)) {
                throw dynamicCode(fa, "sys." + fa.attr);
            }
            Attribute a = (Attribute) c.func;
            int n = c.args.size();
            String recv = expr(a.value);
            if (n <= 3 && layouts.dispatch.contains(a.attr + "/" + n)) {
                usedCall.add(a.attr + "/" + n);
                String helper = "c_" + mangle(a.attr) + "_" + n + "(";
                String direct = directCall(a.attr, n, recv, args.toString(), helper, site(a.attr));
                return direct != null ? direct : helper + recv + args + ", " + site(a.attr) + ")";
            }
            if (n <= 3) {
                return site(a.attr) + ".call" + n + "(" + recv + args + ")";
            }
            return site(a.attr) + ".callN(" + recv + ", new Object[] {" + args.substring(2) + "})";
        }
        return "Py.call(" + expr(c.func) + args + ")";
    }

    private String zeroArgSuper(Call c) {
        Scope f = sc;
        while (f != null && f.kind == Scope.Kind.COMPREHENSION) {
            f = f.parent;
        }
        if (f == null || !f.isFunctionLike() || f.enclosingClass == null || f.params.isEmpty()) {
            throw new PythonSyntaxError(file, c.line, c.col, "super(): no arguments");
        }
        String first = f.params.iterator().next();
        Name n = new Name(first);
        n.line = c.line;
        n.col = c.col;
        return "new PySuper(" + f.enclosingClass.classVar + ", " + load(n) + ")";
    }

    private String comprehension(Comp c) {
        if (c.kind == Comp.Kind.GENERATOR || Linearize.awaited(c)) {
            return generatorExpression(c);
        }
        Frame savedFrame = frame;
        Scope savedScope = sc;
        String first = expr(c.generators.get(0).iter);
        Scope cs = c.scope;
        frame = new Frame(id(cs), cs);
        sc = cs;
        String a = "a" + frame.id;
        String r = fresh("r");
        StringBuilder b = new StringBuilder();
        int ind = 3;
        int depth = ind;
        for (int i = 0; i < c.generators.size(); i++) {
            Comprehension g = c.generators.get(i);
            if (g.isAsync) {
                throw unsupported(g, "async comprehensions");
            }
            String it = fresh("i");
            String x = fresh("x");
            indent(b, depth);
            b.append("final PyObject ").append(it).append(" = Py.iter(").append(i == 0 ? a + "[0]" : expr(g.iter))
                    .append(");\n");
            indent(b, depth);
            b.append("for (Object ").append(x).append("; (").append(x).append(" = ").append(it)
                    .append(".next()) != null;) {\n");
            depth++;
            store(g.target, x, b, depth);
            for (Expr cond : g.ifs) {
                indent(b, depth);
                b.append("if (").append(cond(cond)).append(") {\n");
                depth++;
            }
        }
        indent(b, depth);
        switch (c.kind) {
            case DICT:
                b.append(r).append(".setItem(").append(expr(c.elt)).append(", ").append(expr(c.value))
                        .append(");\n");
                break;
            case SET:
                b.append(r).append(".addItem(").append(expr(c.elt)).append(");\n");
                break;
            default:
                b.append(r).append(".append(").append(expr(c.elt)).append(");\n");
                break;
        }
        while (depth > ind) {
            depth--;
            indent(b, depth);
            b.append("}\n");
        }
        StringBuilder pro = new StringBuilder();
        for (String n : cs.assigned) {
            if (!cs.isLocal(n)) {
                continue;
            }
            indent(pro, ind);
            if (cs.cells.contains(n)) {
                pro.append("final Object[] ").append(cellVar(cs, n)).append(" = new Object[1];\n");
            } else {
                pro.append("Object ").append(localVar(cs, n)).append(" = null;\n");
            }
        }
        if ((frame.temps > 0 || frame.ltemps > 0)) {
            indent(pro, ind);
            pro.append(tempDecls()).append("\n");
        }
        String type;
        switch (c.kind) {
            case DICT:
                type = "PyDict";
                break;
            case SET:
                type = "PySet";
                break;
            default:
                type = "PyList";
                break;
        }
        indent(pro, ind);
        pro.append("final ").append(type).append(" ").append(r).append(" = new ").append(type).append("();\n");
        frame = savedFrame;
        sc = savedScope;
        StringBuilder out = new StringBuilder();
        out.append("((PyFunction.Body) (Object[] ").append(a).append(") -> {\n").append(pro).append(b);
        indent(out, ind);
        // Generator expressions are evaluated eagerly for now: an iterator
        // over the materialised list.
        out.append("return ").append(c.kind == Comp.Kind.GENERATOR ? "Py.iter(" + r + ")" : r).append(";\n");
        indent(out, ind - 1);
        out.append("}).run(new Object[] {").append(first).append("})");
        return out.toString();
    }

    /// A generator expression: a generator whose body is the loops of the
    /// comprehension around `yield elt`, lazy as in Python. The first
    /// iterable is evaluated here, in the enclosing scope, and handed in as
    /// the hidden local `.0`.
    private String generatorExpression(Comp c) {
        Frame savedFrame = frame;
        Scope savedScope = sc;
        GenLowering savedGen = gen;
        boolean async = Linearize.isAsync(c);
        boolean genexp = c.kind == Comp.Kind.GENERATOR;
        String first = expr(c.generators.get(0).iter);
        Scope cs = c.scope;
        frame = new Frame(id(cs), cs);
        sc = cs;
        cs.assigned.add(".0");
        List<Stmt> body = new ArrayList<Stmt>();
        Expr acc = new Name(".r").at(c.line, c.col);
        if (genexp) {
            body.add(new ExprStmt(new Yield(c.elt, false).at(c.elt.line, c.elt.col)).at(c.line, c.col));
        } else if (c.kind == Comp.Kind.DICT) {
            // An async comprehension: a coroutine that fills `.r` and returns it.
            cs.assigned.add(".r");
            body.add(new Assign(java.util.Collections.<Expr>singletonList(
                    new Subscript(acc, c.elt).at(c.line, c.col)), c.value).at(c.line, c.col));
        } else {
            cs.assigned.add(".r");
            Call add = new Call(new Attribute(acc, c.kind == Comp.Kind.SET ? "add" : "append").at(c.line, c.col));
            add.args.add(c.elt);
            body.add(new ExprStmt(add.at(c.elt.line, c.elt.col)).at(c.line, c.col));
        }
        for (int i = c.generators.size() - 1; i >= 0; i--) {
            Comprehension g = c.generators.get(i);
            for (int j = g.ifs.size() - 1; j >= 0; j--) {
                Expr cond = g.ifs.get(j);
                body = new ArrayList<Stmt>(java.util.Collections.<Stmt>singletonList(
                        new If(cond, body, new ArrayList<Stmt>()).at(cond.line, cond.col)));
            }
            Expr iter = i == 0 ? new Name(".0").at(g.line, g.col) : g.iter;
            body = new ArrayList<Stmt>(java.util.Collections.<Stmt>singletonList(
                    new For(g.target, iter, body, new ArrayList<Stmt>(), g.isAsync).at(g.line, g.col)));
        }
        if (!genexp) {
            body.add(new Return(acc).at(c.line, c.col));
        }
        gen = new GenLowering(this, file, cs);
        gen.asyncGen = async && genexp;
        String step = resolveRegions(gen.step(body));
        String a = "a" + frame.id;
        StringBuilder r = new StringBuilder();
        r.append("((PyFunction.Body) (Object[] ").append(a).append(") -> {\n");
        indent(r, 3);
        if (!genexp) {
            r.append("return new PyGenerator(\"<").append(c.kind.name().toLowerCase(java.util.Locale.ROOT))
                    .append("comp>\", true) {\n");
        } else if (async) {
            r.append("return new PyGenerator.Async(\"<genexpr>\") {\n");
        } else {
            r.append("return new PyGenerator(\"<genexpr>\", false) {\n");
        }
        String empty = c.kind == Comp.Kind.DICT ? "new PyDict()" : c.kind == Comp.Kind.SET ? "new PySet()" : "new PyList()";
        for (String n : cs.assigned) {
            if (!cs.isLocal(n)) {
                continue;
            }
            String init = ".0".equals(n) ? a + "[0]" : ".r".equals(n) ? empty : null;
            if (cs.cells.contains(n)) {
                r.append("                final Object[] ").append(cellVar(cs, n)).append(" = new Object[] {")
                        .append(init == null ? "null" : init).append("};\n");
            } else {
                r.append("                Object ").append(localVar(cs, n)).append(" = ").append(init == null ? "null" : init)
                        .append(";\n");
            }
        }
        if (gen.usesReturnField()) {
            r.append("                Object ret$;\n");
        }
        r.append(gen.fields());
        r.append("\n                @Override\n                protected Object step() {\n");
        if (frame.temps > 0 || frame.ltemps > 0) {
            r.append("                    ").append(tempDecls()).append("\n");
        }
        r.append(step).append("                }\n");
        indent(r, 3);
        r.append("};\n");
        indent(r, 2);
        String firstIter = c.generators.get(0).isAsync ? first : "Py.iter(" + first + ")";
        r.append("}).run(new Object[] {").append(firstIter).append("})");
        gen = savedGen;
        frame = savedFrame;
        sc = savedScope;
        return r.toString();
    }

    // ---- constants -------------------------------------------------------

    private int sites;

    // ---- compiled class layouts (see Layouts) -----------------------------

    private Layouts layouts = new Layouts();
    /// Whole-program types, when the program is closed (see `Closed`).
    private TypedEmit typedEmit;
    private boolean closed;

    /// Whether the program this module belongs to passed `Closed.check`.
    public Emitter closed(boolean c) {
        closed = c;
        return this;
    }
    /// The static method name the next `function` call puts its body in.
    private String staticBody;
    /// The class of the compiled method `staticBody` names.
    private Layouts.Cls staticClass;
    /// In a compiled method whose receiver parameter is never rebound: that
    /// parameter's Java local, its class, and the local holding it cast to
    /// that class, or null when it is not an instance of it. The receiver's
    /// type is tested once on entry instead of at every field access.
    private String selfLocal;
    private Layouts.Cls selfCls;
    private String selfTyped;
    /// Java local -> the typed copy kept of it, for the locals of the current
    /// function read for compiled fields (see ShadowTypes). The copy is set
    /// at every store to the local and each access checks `copy == local`,
    /// so a copy left stale only sends the access down the generic path.
    private Map<String, String> shadowVar = new HashMap<String, String>();
    private Map<String, Layouts.Cls> shadowCls = new HashMap<String, Layouts.Cls>();
    private final Set<String> usedCall = new LinkedHashSet<String>();

    /// Gives a compiled class its layout once its body has run, if its base
    /// is still the compiled class the layout extends.
    private void registerLayout(Layouts.Cls lc, StringBuilder out, int ind) {
        indent(out, ind);
        out.append("if (");
        if (lc.parent != null) {
            out.append(lc.typeVar).append(".bases.length == 1 && ").append(lc.typeVar).append(".bases[0] == ")
                    .append(lc.parent.typeVar).append(" && ").append(lc.parent.typeVar).append(".hasLayout()");
        } else {
            out.append("true");
        }
        out.append(") {\n");
        indent(out, ind + 1);
        out.append(lc.typeVar).append(".layout(t -> new ").append(lc.javaName).append("(t), new String[] {");
        int i = 0;
        for (String m : lc.methods.keySet()) {
            out.append(i++ == 0 ? "" : ", ").append(javaString(m));
        }
        out.append("}, new String[] {");
        i = 0;
        for (String f : lc.allFields) {
            out.append(i++ == 0 ? "" : ", ").append(javaString(f));
        }
        out.append("});\n");
        indent(out, ind);
        out.append("}\n");
    }

    private String layoutCode() {
        if (layouts.classes.isEmpty()) {
            return "";
        }
        StringBuilder o = new StringBuilder();
        List<String[]> disp = new ArrayList<String[]>();
        for (String d : layouts.dispatch) {
            int at = d.lastIndexOf('/');
            disp.add(new String[] {d.substring(0, at), d.substring(at + 1)});
        }
        o.append("\n    static class LB extends PyInstance {\n");
        o.append("        LB(PyType t) {\n            super(t);\n        }\n");
        for (String[] d : disp) {
            o.append("        Object m_").append(mangle(d[0])).append("(").append(dparams(Integer.parseInt(d[1])))
                    .append(") {\n            return null;\n        }\n");
        }
        if (typedEmit != null) {
            o.append(typedEmit.layoutMembers(null));
        }
        o.append("    }\n");
        for (Layouts.Cls k : layouts.classes.values()) {
            String sup = k.parent != null ? k.parent.javaName : "LB";
            int base = k.allFields.size() - k.fields.size();
            o.append("\n    static class ").append(k.javaName).append(" extends ").append(sup).append(" {\n");
            for (String f : k.fields) {
                String td = typedEmit != null ? typedEmit.fieldDecl(k, f) : null;
                o.append("        ").append(td != null ? td : "Object f_" + mangle(f) + ";").append("\n");
            }
            o.append("        private static final String[] FN = {");
            for (int i = 0; i < k.allFields.size(); i++) {
                o.append(i == 0 ? "" : ", ").append(javaString(k.allFields.get(i)));
            }
            o.append("};\n");
            o.append("        ").append(k.javaName).append("(PyType t) {\n            super(t);\n        }\n");
            String pc = typedEmit != null ? typedEmit.plainCtor(k) : null;
            if (pc != null) {
                o.append(pc);
            }
            if (!k.fields.isEmpty()) {
                o.append("        @Override\n        protected int fieldIndex(String n) {\n            switch (n) {\n");
                for (int i = 0; i < k.fields.size(); i++) {
                    o.append("                case ").append(javaString(k.fields.get(i))).append(":\n                    return ")
                            .append(base + i).append(";\n");
                }
                o.append("                default:\n                    return super.fieldIndex(n);\n            }\n        }\n");
                o.append("        @Override\n        protected Object getField(int i) {\n            switch (i) {\n");
                for (int i = 0; i < k.fields.size(); i++) {
                    String f = k.fields.get(i);
                    boolean t = typedEmit != null && typedEmit.typedField(k, f) != null;
                    o.append("                case ").append(base + i).append(":\n                    return ")
                            .append(t ? typedEmit.fieldGetCase(k, f) : "f_" + mangle(f)).append(";\n");
                }
                o.append("                default:\n                    return super.getField(i);\n            }\n        }\n");
                o.append("        @Override\n        protected void setField(int i, Object v) {\n            switch (i) {\n");
                for (int i = 0; i < k.fields.size(); i++) {
                    String f = k.fields.get(i);
                    boolean t = typedEmit != null && typedEmit.typedField(k, f) != null;
                    o.append("                case ").append(base + i).append(":\n                    ")
                            .append(t ? typedEmit.fieldSetCase(k, f, "v") : "f_" + mangle(f) + " = v;")
                            .append("\n                    return;\n");
                }
                o.append("                default:\n                    super.setField(i, v);\n            }\n        }\n");
                o.append("        @Override\n        protected String[] fieldNames() {\n            return FN;\n        }\n");
            }
            for (String[] d : disp) {
                Layouts.Cls r = Layouts.resolver(k, d[0]);
                Layouts.Cls rp = k.parent == null ? null : Layouts.resolver(k.parent, d[0]);
                if (r == rp) { //NOPMD CompareObjectsWithEquals - identity is meant
                    continue;
                }
                int ar = Integer.parseInt(d[1]);
                o.append("        @Override\n        Object m_").append(mangle(d[0])).append("(").append(dparams(ar))
                        .append(") {\n");
                FunctionDef f = r == null ? null : r.methods.get(d[0]);
                if (f != null && f.args.names().size() - 1 == ar) {
                    o.append("            if (cls.patched || extra) {\n                return null;\n            }\n");
                    o.append("            return ").append(layouts.staticNames.get(f)).append("(this");
                    for (int i = 0; i < ar; i++) {
                        o.append(", a").append(i);
                    }
                    o.append(");\n");
                } else {
                    o.append("            return null;\n");
                }
                o.append("        }\n");
            }
            if (typedEmit != null) {
                o.append(typedEmit.layoutMembers(k));
            }
            o.append("    }\n");
        }
        for (String d : usedCall) {
            int at = d.lastIndexOf('/');
            String n = d.substring(0, at);
            int ar = Integer.parseInt(d.substring(at + 1));
            StringBuilder args = new StringBuilder();
            for (int i = 0; i < ar; i++) {
                args.append(", a").append(i);
            }
            o.append("\n    private static Object c_").append(mangle(n)).append("_").append(ar).append("(Object o")
                    .append(ar == 0 ? "" : ", " + dparams(ar)).append(", AttrSite s) {\n");
            o.append("        if (o instanceof LB) {\n            Object r = ((LB) o).m_").append(mangle(n)).append("(")
                    .append(args.length() == 0 ? "" : args.substring(2)).append(");\n");
            o.append("            if (r != null) {\n                return r;\n            }\n        }\n");
            o.append("        return s.call").append(ar).append("(o").append(args).append(");\n    }\n");
        }
        return o.toString();
    }

    /// `recv.name` for a compiled field: the field itself when the receiver
    /// is of a class that declares it and the field is set, else the site.
    private String fieldGet(String name, String recv, String site) {
        String raw = rawLocal(recv);
        Layouts.Cls own = raw != null && raw.equals(selfLocal) ? selfOwner(name) : null;
        if (own != null) {
            String f = selfTyped + ".f_" + mangle(name);
            return "(" + selfTyped + " != null && " + f + " != null ? " + f + " : " + site + ".get(" + recv + "))";
        }
        String h = raw != null ? shadowVar.get(raw) : null;
        Layouts.Cls hk = h != null ? ownerFor(name, shadowCls.get(raw)) : null;
        if (raw != null) {
            // A local needs no temps (each a frame slot under ParparVM): the
            // field is read twice, a load, and an unbound local fails every
            // instanceof and raises from the generic path.
            StringBuilder sb = new StringBuilder("(");
            if (hk != null) {
                String f = h + ".f_" + mangle(name);
                sb.append(h).append(" == ").append(raw).append(" && ").append(f).append(" != null ? ").append(f)
                        .append(" : ");
            }
            for (Layouts.Cls k : layouts.fieldOwners.get(name)) {
                String f = "((" + k.javaName + ") " + raw + ").f_" + mangle(name);
                sb.append(raw).append(" instanceof ").append(k.javaName).append(" && ").append(f)
                        .append(" != null ? ").append(f).append(" : ");
            }
            return sb.append(site).append(".get(").append(recv).append("))").toString();
        }
        String t = temp();
        String u = temp();
        StringBuilder sb = new StringBuilder("(");
        String first = "(" + t + " = " + recv + ")";
        for (Layouts.Cls k : layouts.fieldOwners.get(name)) {
            sb.append(first).append(" instanceof ").append(k.javaName).append(" && (").append(u).append(" = ((")
                    .append(k.javaName).append(") ").append(t).append(").f_").append(mangle(name)).append(") != null ? ")
                    .append(u).append(" : ");
            first = t;
        }
        return sb.append(site).append(".get(").append(t).append("))").toString();
    }

    private static final java.util.regex.Pattern RAW_LOCAL =
            java.util.regex.Pattern.compile("\\(?([a-z][A-Za-z0-9_]*) != null \\? \\1 : ul\\(\"[^\"]*\"\\)\\)|[a-z][A-Za-z0-9_]*");

    /// The Java local a load expression reads, when it is one: a bare
    /// variable, or the unbound-checked load of one.
    private static String rawLocal(String e) {
        java.util.regex.Matcher m = RAW_LOCAL.matcher(e);
        if (!m.matches()) {
            return null;
        }
        return m.group(1) != null ? m.group(1) : e;
    }

    /// The class declaring field `name` that the compiled method's receiver
    /// class is or extends, or null when there is none (or no such method).
    private Layouts.Cls selfOwner(String name) {
        return ownerFor(name, selfCls);
    }

    /// The class declaring field `name` that `cls` is or extends, or null.
    private Layouts.Cls ownerFor(String name, Layouts.Cls cls) {
        if (cls == null) {
            return null;
        }
        List<Layouts.Cls> owners = layouts.fieldOwners.get(name);
        for (Layouts.Cls c = cls; c != null; c = c.parent) {
            if (owners.contains(c)) {
                return c;
            }
        }
        return null;
    }

    /// `o.name = v` for a compiled field; `o` and `v` are already evaluated.
    private void fieldSet(String name, String o, String v, String site, StringBuilder out, int ind) {
        indent(out, ind);
        if (o.equals(selfLocal) && selfOwner(name) != null) {
            String f = selfTyped + ".f_" + mangle(name);
            out.append("if (").append(selfTyped).append(" != null && (").append(f).append(" != null || !")
                    .append(selfTyped).append(".extra)) {\n");
            indent(out, ind + 1);
            out.append(f).append(" = ").append(v).append(";\n");
            indent(out, ind);
            out.append("} else {\n");
            indent(out, ind + 1);
            out.append(site).append(".set(").append(o).append(", ").append(v).append(");\n");
            indent(out, ind);
            out.append("}\n");
            return;
        }
        String h = shadowVar.get(o);
        if (h != null && ownerFor(name, shadowCls.get(o)) != null) {
            String f = h + ".f_" + mangle(name);
            out.append("if (").append(h).append(" == ").append(o).append(" && (").append(f).append(" != null || !")
                    .append(h).append(".extra)) {\n");
            indent(out, ind + 1);
            out.append(f).append(" = ").append(v).append(";\n");
            indent(out, ind);
            out.append("} else ");
        }
        for (Layouts.Cls k : layouts.fieldOwners.get(name)) {
            out.append("if (").append(o).append(" instanceof ").append(k.javaName).append(" && (((")
                    .append(k.javaName).append(") ").append(o).append(").f_").append(mangle(name))
                    .append(" != null || !((").append(k.javaName).append(") ").append(o).append(").extra)) {\n");
            indent(out, ind + 1);
            out.append("((").append(k.javaName).append(") ").append(o).append(").f_").append(mangle(name)).append(" = ")
                    .append(v).append(";\n");
            indent(out, ind);
            out.append("} else ");
        }
        out.append("{\n");
        indent(out, ind + 1);
        out.append(site).append(".set(").append(o).append(", ").append(v).append(");\n");
        indent(out, ind);
        out.append("}\n");
    }

    /// `d[k] = d.get(k, c) + e` as one probe of `d`, when every operand is
    /// a plain name or constant; `Py.dictCount` falls back to the two calls
    /// for anything but an exact dict. Null when the statement is not that.
    private String dictCount(Assign a) {
        if (a.targets.size() != 1 || !(a.targets.get(0) instanceof Subscript) || !(a.value instanceof BinOp)) {
            return null;
        }
        Subscript t = (Subscript) a.targets.get(0);
        BinOp b = (BinOp) a.value;
        if (!"+".equals(b.op) || !(t.value instanceof Name) || !(t.index instanceof Name)
                || !(b.left instanceof Call)) {
            return null;
        }
        Call c = (Call) b.left;
        if (!(c.func instanceof Attribute) || !"get".equals(((Attribute) c.func).attr)
                || !(((Attribute) c.func).value instanceof Name) || c.args.size() != 2 || !c.keywords.isEmpty()
                || !(c.args.get(0) instanceof Name)) {
            return null;
        }
        String d = ((Name) t.value).id;
        String k = ((Name) t.index).id;
        if (!d.equals(((Name) ((Attribute) c.func).value).id) || !k.equals(((Name) c.args.get(0)).id)) {
            return null;
        }
        String[] parts = {expr(t.value), expr(t.index), expr(c.args.get(1)), expr(b.right)};
        for (String p : parts) {
            if (rawLocal(p) == null && !SIMPLE_ARG.matcher(p).matches()) {
                return null;
            }
        }
        return "Py.dictCount(" + parts[0] + ", " + parts[1] + ", " + parts[2] + ", " + parts[3] + ")";
    }

    private static final java.util.regex.Pattern SITE_CALL1 =
            java.util.regex.Pattern.compile("(S[0-9]+)\\.call1\\(([A-Za-z][A-Za-z0-9_]*), (.*)\\)", java.util.regex.Pattern.DOTALL);

    /// `x.append(v)` as a statement, `e` being its generated call: the list's
    /// own append straight away when `x` holds a list (a final class whose
    /// method no program can replace), the site otherwise. Null when the
    /// statement is not that.
    private String appendFast(Expr v, String e) {
        if (!(v instanceof Call)) {
            return null;
        }
        Call c = (Call) v;
        if (!(c.func instanceof Attribute) || !"append".equals(((Attribute) c.func).attr) || c.args.size() != 1
                || c.args.get(0) instanceof Starred || !c.keywords.isEmpty()) {
            return null;
        }
        java.util.regex.Matcher m = SITE_CALL1.matcher(e);
        if (!m.matches()) {
            return null;
        }
        String recv = m.group(2);
        String arg = m.group(3);
        return "if (" + recv + " instanceof PyList) { ((PyList) " + recv + ").append(" + arg + "); } else { " + e
                + "; }\n";
    }

    private static final java.util.regex.Pattern SIMPLE_ARG =
            java.util.regex.Pattern.compile("[A-Za-z][A-Za-z0-9_]*|Py\\.(NONE|TRUE|FALSE)");

    /// `recv.name(args)` calling the compiled body straight from the call
    /// site, for each compiled class that resolves `name` to one: a static
    /// call after an `instanceof`, instead of the `c_` helper's virtual
    /// dispatch. Null when that is not certain for every compiled class or
    /// an argument is not a plain value (each branch repeats them).
    private String directCall(String name, int n, String recv, String args, String helper, String site) {
        for (String a : args.isEmpty() ? new String[0] : args.substring(2).split(", ")) {
            if (rawLocal(a) == null && !SIMPLE_ARG.matcher(a).matches()) {
                return null;
            }
        }
        List<Layouts.Cls> all = new ArrayList<Layouts.Cls>(layouts.classes.values());
        StringBuilder sb = new StringBuilder("(");
        String raw = rawLocal(recv);
        String t = raw;
        String first;
        if (raw == null) {
            t = temp();
            first = "(" + t + " = " + recv + ")";
        } else {
            first = raw;
        }
        int branches = 0;
        // Subclasses were planned after their bases: test them first.
        for (int i = all.size() - 1; i >= 0; i--) {
            Layouts.Cls k = all.get(i);
            Layouts.Cls r = Layouts.resolver(k, name);
            if (r == null || k.parent != null && Layouts.resolver(k.parent, name) == r) { //NOPMD CompareObjectsWithEquals - identity is meant
                // Answered by a base's branch, or not a class attribute here.
                continue;
            }
            FunctionDef f = r.methods.get(name);
            if (f == null || f.args.names().size() - 1 != n || branches >= 3) {
                return null;
            }
            branches++;
            String c = "((" + k.javaName + ") " + t + ")";
            sb.append(first).append(" instanceof ").append(k.javaName).append(" && !").append(c).append(".cls.patched && !")
                    .append(c).append(".extra ? ").append(layouts.staticNames.get(f)).append("(").append(t).append(args)
                    .append(") : ");
            first = t;
        }
        if (branches == 0) {
            return null;
        }
        return sb.append(helper).append(raw != null ? recv : t).append(args).append(", ").append(site).append("))")
                .toString();
    }

    private static String dparams(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(i == 0 ? "" : ", ").append("Object a").append(i);
        }
        return sb.toString();
    }

    /// A fresh inline cache for one attribute access. Never shared between two
    /// accesses, unlike `hoist`: each site caches what ITS receivers look like.
    String attrSite(String attr) {
        return site(attr);
    }

    private String site(String attr) {
        String f = "S" + (++sites);
        fields.append("    private static final AttrSite ").append(f).append(" = new AttrSite(")
                .append(javaString(attr)).append(");\n");
        return f;
    }

    private String hoist(String type, String init) {
        String key = type + " " + init;
        String f = constants.get(key);
        if (f == null) {
            f = "K" + constants.size();
            constants.put(key, f);
            fields.append("    private static final ").append(type).append(' ').append(f).append(" = ").append(init)
                    .append(";\n");
        }
        return f;
    }

    String constant(Constant c) {
        switch (c.kind) {
            case NONE:
                return "Py.NONE";
            case TRUE:
                return "Py.TRUE";
            case FALSE:
                return "Py.FALSE";
            case ELLIPSIS:
                return "Py.ELLIPSIS";
            case STR:
                if (c.text.isEmpty()) {
                    return "Py.EMPTY_STR";
                }
                // A str is a java.lang.String; the literal is interned already.
                return javaString(c.text);
            case BYTES: {
                StringBuilder sb = new StringBuilder("new PyBytes(new byte[] {");
                for (int i = 0; i < c.text.length(); i++) {
                    sb.append(i == 0 ? "" : ", ").append((int) (byte) c.text.charAt(i));
                }
                return hoist("PyBytes", sb.append("})").toString());
            }
            case FLOAT: {
                String text = c.text.replace("_", "");
                if (text.endsWith("j") || text.endsWith("J")) {
                    double im = Double.parseDouble(text.substring(0, text.length() - 1));
                    String imLit = Double.isInfinite(im) ? "Double.POSITIVE_INFINITY" : Double.toString(im);
                    return hoist("Object", "new PyComplex(0.0, " + imLit + ")");
                }
                double d = Double.parseDouble(text);
                String lit = Double.isInfinite(d) ? "Double.POSITIVE_INFINITY" : Double.toString(d);
                return hoist("Object", "Py.of(" + lit + ")");
            }
            case INT:
            default: {
                BigInteger v = parseInt(c.text);
                if (v.bitLength() < 64) {
                    return intConst(v.longValue());
                }
                return hoist("Object", "PyInt.parse(\"" + v.toString() + "\", 10)");
            }
        }
    }

    private BigInteger parseInt(String text) {
        String t = text.replace("_", "");
        String l = t.length() > 1 ? t.substring(0, 2) : "";
        if ("0x".equals(l) || "0X".equals(l)) {
            return new BigInteger(t.substring(2), 16);
        }
        if ("0o".equals(l) || "0O".equals(l)) {
            return new BigInteger(t.substring(2), 8);
        }
        if ("0b".equals(l) || "0B".equals(l)) {
            return new BigInteger(t.substring(2), 2);
        }
        return new BigInteger(t);
    }

    // ---- Java literals ---------------------------------------------------

    /// A Java string literal for any text, in ASCII. Line breaks, quotes and
    /// backslashes use their short escapes: a backslash-u escape for them
    /// would be decoded by javac before the literal is even lexed.
    public static String javaString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                default:
                    if (c < 0x20 || c > 0x7e) {
                        String h = Integer.toHexString(c);
                        sb.append("\\u");
                        for (int k = h.length(); k < 4; k++) {
                            sb.append('0');
                        }
                        sb.append(h);
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }

    private static String javaComment(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 0x20 && c < 0x7f && c != '\\' ? c : '?');
        }
        return sb.toString();
    }

    /// Whether CPython's optimizer folds every element to a constant, which
    /// turns a set display of three or more into a frozenset constant.
    private static boolean allConstant(List<Expr> elts) {
        for (Expr x : elts) {
            if (!isConstant(x)) {
                return false;
            }
        }
        return true;
    }

    // ---- float expression fusion ------------------------------------------
    //
    // An arithmetic tree such as `(dx * dx + dy * dy + dz * dz) ** 0.5` is
    // emitted as one static helper taking the tree's leaves. When every leaf
    // is a float the helper computes the whole tree in Java `double` and
    // boxes only the result; otherwise (or when a step would raise: a zero
    // divisor, a pow that overflows or leaves the reals) it evaluates the
    // tree through the generic operators exactly as it would have been
    // written. The leaves are still evaluated once each, left to right, at
    // the call site.

    private int fusedCount;

    /// Stands for the current value of an augmented assignment's target in
    /// a fused tree; the root operator of such a tree is the in-place one.
    private static final Name AUG_CURRENT = new Name("$current");

    private static boolean fuseNode(Expr e) {
        if (e instanceof BinOp) {
            String op = ((BinOp) e).op;
            return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op) || "**".equals(op);
        }
        return e instanceof UnaryOp && "-".equals(((UnaryOp) e).op);
    }

    private static boolean intConst(Expr e) {
        if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.INT) {
            return false;
        }
        String t = ((Constant) e).text;
        if (t.length() > 15 || (t.length() > 1 && t.charAt(0) == '0')) {
            return false;
        }
        for (int i = 0; i < t.length(); i++) {
            if (t.charAt(i) < '0' || t.charAt(i) > '9') {
                return false;
            }
        }
        return true;
    }

    private static boolean floatConst(Expr e) {
        return e instanceof Constant && ((Constant) e).kind == Constant.Kind.FLOAT
                && !((Constant) e).text.matches(".*[jJ]");
    }

    /// Whether the tree, given float leaves, is float at every node; -1 when
    /// it cannot be fused at all.
    private static int fuseCheck(Expr e) {
        if (fuseNode(e)) {
            if (e instanceof UnaryOp) {
                return fuseCheck(((UnaryOp) e).operand);
            }
            int l = fuseCheck(((BinOp) e).left);
            int r = fuseCheck(((BinOp) e).right);
            if (l < 0 || r < 0 || l == 0 && r == 0) {
                return -1;
            }
            return 1;
        }
        if (intConst(e)) {
            return 0;
        }
        if (e instanceof Constant && !floatConst(e)) {
            return -1;
        }
        return 1;
    }

    private String fused(Expr root) {
        return fused(root, null);
    }

    private String fused(Expr root, String current) {
        if (numOps(root) < 2) {
            return null;
        }
        // Which nodes are int and which float, when every leaf has the type
        // the tree implies for it: an operand of a bit, shift, floor or
        // modulo operator is an int, and so is every other occurrence of the
        // same name; any other leaf is taken for a float.
        Set<Expr> intCtx = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Expr, Boolean>());
        Set<String> intNames = new HashSet<String>();
        markInts(root, false, intCtx, intNames);
        Map<Expr, Character> types = new java.util.IdentityHashMap<Expr, Character>();
        if (numType(root, intCtx, intNames, types) == 0) {
            return null;
        }
        List<Expr> leaves = new ArrayList<Expr>();
        numLeaves(root, leaves);
        if (leaves.isEmpty()) {
            return null;
        }
        String name = "F" + fusedCount++;
        StringBuilder m = new StringBuilder();
        m.append("    private static Object ").append(name).append("(");
        for (int i = 0; i < leaves.size(); i++) {
            m.append(i == 0 ? "" : ", ").append("Object p").append(i);
        }
        m.append(") {\n");
        numPath(root, leaves, types, "fast", m);
        // A tree that only took its leaves for floats also gets an all-int
        // path when it has nothing an int cannot do: `i * n + j`.
        boolean anyFloat = false;
        for (Expr l : leaves) {
            anyFloat |= types.get(l).charValue() == 'f';
        }
        if (anyFloat) {
            Map<Expr, Character> ints = new java.util.IdentityHashMap<Expr, Character>();
            Set<Expr> all = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Expr, Boolean>());
            all.addAll(leaves);
            if (numType(root, all, new HashSet<String>(), ints) == 'i') {
                numPath(root, leaves, ints, "ints", m);
            }
        }
        int[] counters = new int[2];
        String boxed = fuseBoxed(root, counters);
        if (current != null) {
            boxed = "Py.i" + boxed.substring(3);
        }
        m.append("        return ").append(boxed).append(";\n    }\n");
        fields.append(m);
        StringBuilder call = new StringBuilder(name).append("(");
        for (int i = 0; i < leaves.size(); i++) {
            Expr leaf = leaves.get(i);
            call.append(i == 0 ? "" : ", ").append(leaf == AUG_CURRENT ? current : expr(leaf));
        }
        return call.append(")").toString();
    }

    /// The raw-double twin of a function whose body is `return <float
    /// arithmetic over its parameters>` (PyFunction.PureD1..4), or null.
    private String pureVariant(List<String> params, Expr ret) {
        if (ret == null || params.isEmpty() || params.size() > 4 || numOps(ret) < 1) {
            return null;
        }
        Set<Expr> intCtx = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Expr, Boolean>());
        Set<String> intNames = new HashSet<String>();
        markInts(ret, false, intCtx, intNames);
        Map<Expr, Character> types = new java.util.IdentityHashMap<Expr, Character>();
        if (numType(ret, intCtx, intNames, types) != 'f') {
            return null;
        }
        List<Expr> leaves = new ArrayList<Expr>();
        numLeaves(ret, leaves);
        StringBuilder m = new StringBuilder();
        m.append("(PyFunction.PureD").append(params.size()).append(") (");
        for (int i = 0; i < params.size(); i++) {
            m.append(i == 0 ? "" : ", ").append("Object q").append(i);
        }
        m.append(") -> {\n");
        for (int i = 0; i < leaves.size(); i++) {
            Expr l = leaves.get(i);
            int k = l instanceof Name ? params.indexOf(((Name) l).id) : -1;
            if (k < 0) {
                return null;
            }
            m.append("        final Object p").append(i).append(" = q").append(k).append(";\n");
        }
        numPath(ret, leaves, types, "fast", m, true);
        return m.append("        return Double.NaN;\n    }").toString();
    }

    /// A module-level function with a long twin, called directly: the static
    /// method `PL_<name>` holds the twin and `DEF_<name>` the function object
    /// its `def` made, so a call site that finds that very object in the
    /// global calls the method, which the C compiler can inline. Filled
    /// before the module is emitted, as callers may come first.
    private final Map<String, Integer> directLong = new HashMap<String, Integer>();

    private void findDirectLong(List<Stmt> body) {
        Map<String, Integer> defs = new HashMap<String, Integer>();
        for (Stmt s : body) {
            if (s instanceof FunctionDef) {
                String n = ((FunctionDef) s).name;
                defs.put(n, Integer.valueOf(defs.containsKey(n) ? 2 : 1));
            }
        }
        for (Stmt s : body) {
            if (!(s instanceof FunctionDef) || defs.get(((FunctionDef) s).name).intValue() != 1) {
                continue;
            }
            FunctionDef f = (FunctionDef) s;
            List<String> names = f.args.names();
            Expr ret = pureReturn(f.args, names, f.body, null);
            if (ret != null && !f.isAsync && !f.scope.hasYield && pureVariant(names, ret) != null
                    && pureLongVariant(names, ret) != null) {
                directLong.put(f.name, Integer.valueOf(names.size()));
            }
        }
    }

    /// The single returned expression a pure twin is made from, or null.
    private Expr pureReturn(Arguments args, List<String> names, List<Stmt> body, Expr exprBody) {
        boolean direct = args.vararg == null && args.kwarg == null && args.kwonly.isEmpty() && names.size() <= 4;
        if (!direct || !args.defaults.isEmpty()) {
            return null;
        }
        Expr ret = exprBody;
        List<Stmt> rest = body;
        if (rest != null && rest.size() == 2 && isDocstring(rest.get(0))) {
            rest = rest.subList(1, 2);
        }
        if (ret == null && rest != null && rest.size() == 1 && rest.get(0) instanceof Return) {
            ret = ((Return) rest.get(0)).value;
        }
        return ret;
    }

    /// The same twin taking its parameters as raw longs
    /// (PyFunction.PureL1..2), for a body whose every leaf is an int
    /// parameter: callers holding unboxed ints then box nothing.
    private String pureLongVariant(List<String> params, Expr ret) {
        if (params.size() > 2) {
            return null;
        }
        Set<Expr> intCtx = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Expr, Boolean>());
        Set<String> intNames = new HashSet<String>();
        markInts(ret, false, intCtx, intNames);
        Map<Expr, Character> types = new java.util.IdentityHashMap<Expr, Character>();
        if (numType(ret, intCtx, intNames, types) != 'f') {
            return null;
        }
        List<Expr> leaves = new ArrayList<Expr>();
        numLeaves(ret, leaves);
        StringBuilder m = new StringBuilder();
        m.append("        fast: {\n");
        for (int i = 0; i < leaves.size(); i++) {
            Expr l = leaves.get(i);
            int k = l instanceof Name ? params.indexOf(((Name) l).id) : -1;
            if (k < 0 || types.get(l).charValue() != 'i') {
                return null;
            }
            m.append("            long d").append(i).append(" = q").append(k).append(";\n");
        }
        String r = numPrim(ret, types, "fast", m, new int[2]);
        m.append("            return ").append(r).append(";\n        }\n");
        return m.append("        return Double.NaN;\n").toString();
    }

    private static boolean intOnlyOp(String op) {
        return "//".equals(op) || "%".equals(op) || "<<".equals(op) || ">>".equals(op) || "&".equals(op)
                || "|".equals(op) || "^".equals(op);
    }

    /// A node of a fused tree: arithmetic, bit and shift operators, negation.
    private static boolean numNode(Expr e) {
        if (e instanceof BinOp) {
            String op = ((BinOp) e).op;
            return fuseNode(e) || intOnlyOp(op);
        }
        return e instanceof UnaryOp && ("-".equals(((UnaryOp) e).op) || "~".equals(((UnaryOp) e).op));
    }

    private static int numOps(Expr e) {
        if (!numNode(e)) {
            return 0;
        }
        if (e instanceof UnaryOp) {
            return 1 + numOps(((UnaryOp) e).operand);
        }
        return 1 + numOps(((BinOp) e).left) + numOps(((BinOp) e).right);
    }

    private static void numLeaves(Expr e, List<Expr> out) {
        if (numNode(e)) {
            if (e instanceof UnaryOp) {
                numLeaves(((UnaryOp) e).operand, out);
            } else {
                numLeaves(((BinOp) e).left, out);
                numLeaves(((BinOp) e).right, out);
            }
        } else if (!(e instanceof Constant)) {
            out.add(e);
        }
    }

    private static void markInts(Expr e, boolean ctx, Set<Expr> intCtx, Set<String> intNames) {
        if (!numNode(e)) {
            if (ctx) {
                intCtx.add(e);
                if (e instanceof Name) {
                    intNames.add(((Name) e).id);
                }
            }
            return;
        }
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            markInts(u.operand, ctx || "~".equals(u.op), intCtx, intNames);
            return;
        }
        BinOp b = (BinOp) e;
        boolean c = intOnlyOp(b.op) || ctx && ("+".equals(b.op) || "-".equals(b.op) || "*".equals(b.op));
        markInts(b.left, c, intCtx, intNames);
        markInts(b.right, c, intCtx, intNames);
    }

    /// The value of an int literal that fits a Java long, or null.
    private static Long intLiteral(Expr e) {
        if (!(e instanceof Constant) || ((Constant) e).kind != Constant.Kind.INT) {
            return null;
        }
        String t = ((Constant) e).text.replace("_", "").toLowerCase(java.util.Locale.ROOT);
        int radix = 10;
        if (t.startsWith("0x")) {
            radix = 16;
        } else if (t.startsWith("0o")) {
            radix = 8;
        } else if (t.startsWith("0b")) {
            radix = 2;
        }
        if (radix != 10) {
            t = t.substring(2);
        }
        try {
            BigInteger v = new BigInteger(t, radix);
            return v.bitLength() < 63 ? Long.valueOf(v.longValue()) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /// 'i' or 'f' for `e` (recorded in `types`), or 0 when the tree cannot
    /// be fused with those leaf types.
    private static char numType(Expr e, Set<Expr> intCtx, Set<String> intNames, Map<Expr, Character> types) {
        char t;
        if (!numNode(e)) {
            if (e instanceof Constant) {
                t = intLiteral(e) != null ? 'i' : floatConst(e) ? 'f' : 0;
            } else {
                t = intCtx.contains(e) || e instanceof Name && intNames.contains(((Name) e).id) ? 'i' : 'f';
            }
        } else if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            t = numType(u.operand, intCtx, intNames, types);
            if ("~".equals(u.op) && t != 'i') {
                t = 0;
            }
        } else {
            BinOp b = (BinOp) e;
            char l = numType(b.left, intCtx, intNames, types);
            char r = numType(b.right, intCtx, intNames, types);
            if (l == 0 || r == 0) {
                t = 0;
            } else if (intOnlyOp(b.op)) {
                t = l == 'i' && r == 'i' ? 'i' : 0;
            } else if ("/".equals(b.op)) {
                t = 'f';
            } else if ("**".equals(b.op)) {
                t = l == 'i' && r == 'i' ? 0 : 'f';
            } else {
                t = l == 'i' && r == 'i' ? 'i' : 'f';
            }
        }
        if (t != 0) {
            types.put(e, Character.valueOf(t));
        }
        return t;
    }

    /// One typed fast path: taken when every leaf has the type `types`
    /// gives it, left by `break label` when a step would leave the range
    /// Java computes exactly (an int overflow, a zero divisor, an int too
    /// large to convert exactly).
    private void numPath(Expr root, List<Expr> leaves, Map<Expr, Character> types, String label, StringBuilder m) {
        numPath(root, leaves, types, label, m, false);
    }

    /// `raw`: the path returns the double itself (root type float).
    private void numPath(Expr root, List<Expr> leaves, Map<Expr, Character> types, String label, StringBuilder m,
            boolean raw) {
        StringBuilder guard = new StringBuilder();
        StringBuilder unbox = new StringBuilder();
        // A name read twice is one value (a fused tree calls nothing in
        // between), so it is tested and unboxed once.
        Map<String, Integer> seen = new HashMap<String, Integer>();
        for (int i = 0; i < leaves.size(); i++) {
            boolean isInt = types.get(leaves.get(i)).charValue() == 'i';
            String box = isInt ? "Long" : "Double";
            if (leaves.get(i) instanceof Name) {
                String key = ((Name) leaves.get(i)).id + (isInt ? ":i" : ":f");
                Integer first = seen.get(key);
                if (first != null) {
                    unbox.append("            ").append(isInt ? "long" : "double").append(" d").append(i)
                            .append(" = d").append(first).append(";\n");
                    continue;
                }
                seen.put(key, Integer.valueOf(i));
            }
            guard.append(guard.length() == 0 ? "" : " && ").append("p").append(i).append(" instanceof ").append(box);
            unbox.append("            ").append(isInt ? "long" : "double").append(" d").append(i).append(" = ((")
                    .append(box).append(") p").append(i).append(").").append(isInt ? "longValue" : "doubleValue")
                    .append("();\n");
        }
        m.append("        ").append(label).append(": if (").append(guard).append(") {\n").append(unbox);
        int[] counters = new int[2];
        String r = numPrim(root, types, label, m, counters);
        boolean isInt = types.get(root).charValue() == 'i';
        if (raw) {
            m.append("            return ").append(r).append(";\n        }\n");
        } else {
            m.append("            return ").append(isInt ? "Long" : "Double").append(".valueOf(").append(r)
                    .append(");\n        }\n");
        }
    }

    private static String asDouble(String v, char t) {
        return t == 'i' ? "((double) " + v + ")" : v;
    }

    private static void bail(StringBuilder m, String cond, String label) {
        m.append("            if (").append(cond).append(") {\n                break ").append(label)
                .append(";\n            }\n");
    }

    /// counters[0]: next leaf; counters[1]: next temp.
    private String numPrim(Expr e, Map<Expr, Character> types, String label, StringBuilder m, int[] counters) {
        char ty = types.get(e).charValue();
        if (!numNode(e)) {
            if (e instanceof Constant) {
                Long v = intLiteral(e);
                if (v != null) {
                    return ty == 'i' ? "(" + v + "L)" : "(" + v + ".0)";
                }
                double d = Double.parseDouble(((Constant) e).text.replace("_", ""));
                return Double.isInfinite(d) ? "Double.POSITIVE_INFINITY" : "(" + Double.toString(d) + ")";
            }
            return "d" + counters[0]++;
        }
        String t = "t" + counters[1]++;
        String jt = ty == 'i' ? "long" : "double";
        if (e instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) e;
            String a = numPrim(u.operand, types, label, m, counters);
            if ("~".equals(u.op)) {
                m.append("            long ").append(t).append(" = ~").append(a).append(";\n");
                return t;
            }
            if (ty == 'i') {
                bail(m, a + " == Long.MIN_VALUE", label);
            }
            m.append("            ").append(jt).append(' ').append(t).append(" = -").append(a).append(";\n");
            return t;
        }
        BinOp b = (BinOp) e;
        char lt = types.get(b.left).charValue();
        char rt = types.get(b.right).charValue();
        String l = numPrim(b.left, types, label, m, counters);
        String r = numPrim(b.right, types, label, m, counters);
        if (ty == 'f') {
            if ("/".equals(b.op) && lt == 'i' && rt == 'i') {
                // Exact as doubles, so one rounding, as Python's int division.
                bail(m, l + " > 9007199254740992L || " + l + " < -9007199254740992L || " + r
                        + " > 9007199254740992L || " + r + " < -9007199254740992L", label);
            }
            l = asDouble(l, lt);
            r = asDouble(r, rt);
            if ("**".equals(b.op)) {
                m.append("            double ").append(t).append(" = Math.pow(").append(l).append(", ").append(r)
                        .append(");\n");
                bail(m, "Double.isNaN(" + t + ") || Double.isInfinite(" + t + ")", label);
                return t;
            }
            if ("/".equals(b.op)) {
                bail(m, r + " == 0.0", label);
            }
            m.append("            double ").append(t).append(" = ").append(l).append(' ').append(b.op).append(' ')
                    .append(r).append(";\n");
            return t;
        }
        String op = b.op;
        if ("+".equals(op)) {
            m.append("            long ").append(t).append(" = ").append(l).append(" + ").append(r).append(";\n");
            bail(m, "((" + l + " ^ " + t + ") & (" + r + " ^ " + t + ")) < 0", label);
        } else if ("-".equals(op)) {
            m.append("            long ").append(t).append(" = ").append(l).append(" - ").append(r).append(";\n");
            bail(m, "((" + l + " ^ " + r + ") & (" + l + " ^ " + t + ")) < 0", label);
        } else if ("*".equals(op)) {
            m.append("            long ").append(t).append(" = ").append(l).append(" * ").append(r).append(";\n");
            // Two operands that fit in 32 bits cannot overflow 64, which
            // settles nearly every product without the division.
            bail(m, "(((" + l + " + 0x80000000L) | (" + r + " + 0x80000000L)) >>> 32) != 0 && " + l
                    + " != 0 && (" + t + " / " + l + " != " + r + " || " + l + " == -1 && " + r
                    + " == Long.MIN_VALUE)", label);
        } else if ("&".equals(op) || "|".equals(op) || "^".equals(op)) {
            m.append("            long ").append(t).append(" = ").append(l).append(' ').append(op).append(' ')
                    .append(r).append(";\n");
        } else if (">>".equals(op)) {
            bail(m, r + " < 0", label);
            m.append("            long ").append(t).append(" = ").append(r).append(" > 63 ? (").append(l)
                    .append(" < 0 ? -1L : 0L) : ").append(l).append(" >> ").append(r).append(";\n");
        } else if ("<<".equals(op)) {
            bail(m, r + " < 0 || " + r + " > 62 || (" + l + " << " + r + " >> " + r + ") != " + l, label);
            m.append("            long ").append(t).append(" = ").append(l).append(" << ").append(r).append(";\n");
        } else if ("//".equals(op)) {
            bail(m, r + " == 0 || " + l + " == Long.MIN_VALUE && " + r + " == -1", label);
            m.append("            long ").append(t).append(" = ").append(l).append(" / ").append(r).append(";\n");
            m.append("            if (").append(t).append(" * ").append(r).append(" != ").append(l).append(" && (")
                    .append(l).append(" < 0) != (").append(r).append(" < 0)) {\n                ").append(t)
                    .append("--;\n            }\n");
        } else {
            bail(m, r + " == 0", label);
            m.append("            long ").append(t).append(" = ").append(l).append(" % ").append(r).append(";\n");
            m.append("            if (").append(t).append(" != 0 && (").append(t).append(" < 0) != (").append(r)
                    .append(" < 0)) {\n                ").append(t).append(" += ").append(r)
                    .append(";\n            }\n");
        }
        return t;
    }

    private String fuseBoxed(Expr e, int[] counters) {
        if (!numNode(e)) {
            if (e instanceof Constant) {
                return constant((Constant) e);
            }
            return "p" + counters[0]++;
        }
        if (e instanceof UnaryOp) {
            String a = fuseBoxed(((UnaryOp) e).operand, counters);
            return "~".equals(((UnaryOp) e).op) ? "Py.invert(" + a + ")" : "Py.neg(" + a + ")";
        }
        BinOp b = (BinOp) e;
        String l = fuseBoxed(b.left, counters);
        String r = fuseBoxed(b.right, counters);
        return "Py." + BIN.get(b.op) + "(" + l + ", " + r + ")";
    }

    // ---- float regions -----------------------------------------------------
    //
    // A run of straight-line float statements (`dx = a - b`, `mag = ...`,
    // `v[0] -= dx * m`) is computed in Java doubles as one region. A local
    // the region assigns before anything reads it, and that nothing outside
    // the function's regions reads at all, never exists as an object: that
    // is decided after the whole function is emitted, from its Java text
    // (resolveRegions). Every other local the region assigns is boxed as
    // it is assigned, and every store into a list is boxed as it is stored.
    //
    // Any operand that is not a float -- an int, a user object, a missing
    // list item -- makes the region stop before the statement that read it
    // ("bail"); the boxed locals are then completed and the remaining
    // statements run through the generic operators, exactly as written. A
    // region that keeps bailing (int arithmetic that only looks like float
    // arithmetic) turns its fast path off after a few tries, so it costs a
    // counter test. Reads in the fast path never call user code: names, and
    // list or tuple items by int index, are all it reads.

    private static final String REGION_START = "/*@R{*/";
    private static final String REGION_END = "/*@}R*/";
    private static final String IF_SHARED = "/*@S:";
    private static final String IF_PRIVATE = "/*@P:";
    private static final String COND_END = "/*@E*/";
    private static final int REGION_BAILS = 8;

    private int regionCount;
    /// Locals some region reads before assigning: never private.
    private final Set<String> regionShared = new HashSet<String>();

    /// Locals of the function being emitted whose slice copies reuse their
    /// previous list ([ListRecycling]).
    private Set<String> recycled = java.util.Collections.<String>emptySet();
    /// While a loop body that is one region is emitted: each local it
    /// assigns, mapped to {the double that carries it across iterations,
    /// the flag saying that double is the current value}.
    private Map<String, String[]> carried;
    /// The `range` loop whose index the current region reads unboxed:
    /// {name, iterator var (null while counting), long index, step, start}.
    private String[] lazyIndex;
    /// Inside a region's fast path, where a lazy index's local is stale.
    private boolean inRegionFast;
    /// While a lazy `range` loop's body is emitted: {iterator var, long
    /// index, end, step, indent}, asking the region that is the whole body
    /// for a versioned copy of its fast path ([#versionedLoop]).
    private String[] version;
    /// Where that copy goes; the loop inserts it ahead of itself.
    private StringBuilder versionOut; //NOPMD AvoidStringBufferField - the generator's output
    /// While the versioned copy is emitted: each invariant name read, mapped
    /// to the final that holds it; null otherwise.
    private Map<String, String> hoist;
    /// The raw (non-throwing) read behind each of those finals.
    private Map<String, String> hoistRaw;
    /// The names the versioned region assigns, which are never invariant.
    private Set<String> hoistAssigned;
    /// The lists the versioned region stores items into.
    private Set<String> hoistStored;
    /// Each hoisted float list read by index: {its double[], its size}.
    private Map<String, String[]> hoistFloats;
    /// In a region's fast path, local -> {double[], count} it read once on
    /// entry: lists the region subscripts with ints and never rebinds.
    private Map<String, String[]> regionArrays;
    /// Conditions on hoisted values the versioned loop is entered under.
    private List<String> hoistTests;
    /// Unboxed copies of hoisted values, declared once those tests passed.
    private List<String> hoistDecls;
    /// Whether the function being emitted has a try, with or match: a loop
    /// then keeps no local as a double, since a handler could read it.
    private boolean fnHasHandlers;

    /// Writes a carried local back to its object, once, when code that
    /// reads the object runs next.
    private void syncCarried(String id, String[] c, StringBuilder r, int ind) {
        indent(r, ind);
        r.append("if (").append(c[1]).append(") {\n");
        indent(r, ind + 1);
        r.append(localVar(sc, id)).append(" = Double.valueOf(").append(c[0]).append(");\n");
        indent(r, ind + 1);
        r.append(c[1]).append(" = false;\n");
        indent(r, ind);
        r.append("}\n");
    }

    /// The locals a loop body keeps as doubles across its iterations: the
    /// body must be one float region (nothing in it can break out or run
    /// a handler), or null.
    private Map<String, String[]> loopCarried(For f) {
        if (sc == null || sc.kind != Scope.Kind.FUNCTION || fnHasHandlers || !f.orelse.isEmpty()
                || f.body.isEmpty()) {
            return null;
        }
        Map<String, String[]> c = new LinkedHashMap<String, String[]>();
        for (Stmt s : f.body) {
            if (!regionStmt(s)) {
                return null;
            }
            Expr t = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
            if (t instanceof Name && !c.containsKey(((Name) t).id)) {
                String n = fresh("lc");
                c.put(((Name) t).id, new String[] {n + "_d", n + "_f"});
            }
        }
        if (f.target instanceof Name && c.containsKey(((Name) f.target).id)) {
            return null;
        }
        return c;
    }

    private static boolean hasHandlers(List<Stmt> body) {
        for (Stmt s : body) {
            if (s instanceof Try || s instanceof With || s instanceof Match) {
                return true;
            }
            if (s instanceof If && (hasHandlers(((If) s).body) || hasHandlers(((If) s).orelse))
                    || s instanceof While && (hasHandlers(((While) s).body) || hasHandlers(((While) s).orelse))
                    || s instanceof For && (hasHandlers(((For) s).body) || hasHandlers(((For) s).orelse))) {
                return true;
            }
        }
        return false;
    }

    private static boolean regionOp(String op) {
        return "+".equals(op) || "-".equals(op) || "*".equals(op) || "/".equals(op);
    }

    /// A list or tuple read the fast path can do without user code.
    private boolean regionItem(Expr e) {
        if (!(e instanceof Subscript)) {
            return false;
        }
        Subscript sub = (Subscript) e;
        return sub.value instanceof Name && !(sub.index instanceof Slice)
                && (constIndex(sub.index) != null || sub.index instanceof Name);
    }

    private boolean regionLeaf(Expr e) {
        return e instanceof Name || regionItem(e) || intConst(e) || floatConst(e) || regionCall(e);
    }

    /// `f(a, b)` with plain names or numbers for arguments: the fast path
    /// asks `f` for its raw double (Py.callD), which runs no Python code,
    /// and leaves the region when it gets none.
    private static boolean regionCall(Expr e) {
        if (!(e instanceof Call)) {
            return false;
        }
        Call c = (Call) e;
        if (!(c.func instanceof Name) || !c.keywords.isEmpty() || c.args.isEmpty() || c.args.size() > 4) {
            return false;
        }
        for (Expr a : c.args) {
            if (!(a instanceof Name) && !intConst(a) && !floatConst(a)) {
                return false;
            }
        }
        return true;
    }

    /// Whether `e` is float arithmetic over region leaves.
    private boolean regionExpr(Expr e) {
        if (e instanceof BinOp) {
            BinOp b = (BinOp) e;
            return (regionOp(b.op) || "**".equals(b.op)) && regionExpr(b.left) && regionExpr(b.right);
        }
        if (e instanceof UnaryOp) {
            return "-".equals(((UnaryOp) e).op) && regionExpr(((UnaryOp) e).operand);
        }
        return regionLeaf(e);
    }

    private boolean localName(Expr e) {
        return e instanceof Name && sc.resolve(((Name) e).id) == Scope.Binding.LOCAL;
    }

    /// Whether a statement can be part of a float region, and has some
    /// arithmetic in it (a plain copy is left alone).
    private boolean regionStmt(Stmt s) {
        if (s instanceof Assign) {
            Assign a = (Assign) s;
            if (a.targets.size() != 1 || !(fuseNode(a.value)) || !regionExpr(a.value) || fuseCheck(a.value) != 1) {
                return false;
            }
            Expr t = a.targets.get(0);
            return localName(t) || regionItem(t);
        }
        if (s instanceof AugAssign) {
            AugAssign a = (AugAssign) s;
            // `n += 1` is int arithmetic in practice; a region would only bail.
            return regionOp(a.op) && !intConst(a.value) && (localName(a.target) || regionItem(a.target))
                    && regionExpr(a.value);
        }
        return false;
    }

    /// Emits the region starting at `start`, if there is one, and returns the
    /// index after it; returns `start` when there is none.
    private int floatRegion(List<Stmt> body, int start, StringBuilder out, int ind) {
        if (sc == null || sc.kind != Scope.Kind.FUNCTION) {
            return start;
        }
        int end = start;
        while (end < body.size() && regionStmt(body.get(end))) {
            end++;
        }
        // Worth a region: two statements (a local can then stay a double), or
        // any store into a list (the list can keep the float unboxed).
        boolean anyItem = false;
        for (int i = start; i < end; i++) {
            Stmt s = body.get(i);
            Expr t = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
            anyItem |= t instanceof Subscript;
        }
        boolean wholeLoop = carried != null && start == 0 && end == body.size();
        if (end == start || end - start < 2 && !anyItem && !wholeLoop) {
            return start;
        }
        Map<String, String[]> keep = carried;
        if (!wholeLoop) {
            carried = null;
        }
        try {
            return floatRegion(body, start, end, out, ind);
        } finally {
            carried = keep;
        }
    }

    private int floatRegion(List<Stmt> body, int start, int end, StringBuilder out, int ind) {
        List<Stmt> run = body.subList(start, end);
        int n = regionCount++;
        String label = "fr" + n;
        String done = "frs" + n;
        String bails = "FR" + n;
        fields.append("    private static int ").append(bails).append(";\n");

        // Doubles for every local the region assigns; which of them are read
        // before being assigned decides who may stay private.
        Map<String, Integer> firstStore = new LinkedHashMap<String, Integer>();
        Set<String> readFirst = new HashSet<String>();
        for (int i = 0; i < run.size(); i++) {
            Stmt s = run.get(i);
            List<Expr> reads = new ArrayList<Expr>();
            Expr target;
            if (s instanceof Assign) {
                target = ((Assign) s).targets.get(0);
                regionReads(((Assign) s).value, reads);
            } else {
                target = ((AugAssign) s).target;
                if (target instanceof Name) {
                    reads.add(target);
                }
                regionReads(((AugAssign) s).value, reads);
            }
            if (target instanceof Subscript) {
                regionReads(target, reads);
            }
            for (Expr r : reads) {
                String id = ((Name) r).id;
                if (sc.resolve(id) == Scope.Binding.LOCAL && !firstStore.containsKey(id)) {
                    readFirst.add(id);
                }
            }
            if (target instanceof Name && !firstStore.containsKey(((Name) target).id)) {
                firstStore.put(((Name) target).id, Integer.valueOf(i));
            }
        }
        for (String id : readFirst) {
            regionShared.add(localVar(sc, id));
        }
        if (carried != null) {
            firstStore.keySet().removeAll(carried.keySet());
        }
        String[] vsig = version;
        version = null;
        if (vsig != null && carried != null && firstStore.isEmpty()) {
            versionedLoop(run, vsig, bails, n);
        }

        StringBuilder r = new StringBuilder();
        Map<String, String[]> arrays = regionArrays(run, n);
        indent(r, ind);
        r.append(REGION_START).append("\n");
        indent(r, ind);
        r.append("int ").append(done).append(" = 0;\n");
        for (String id : firstStore.keySet()) {
            indent(r, ind);
            r.append("double ").append(localVar(sc, id)).append("_d = 0;\n");
        }
        indent(r, ind);
        r.append("if (").append(bails).append(" < ").append(REGION_BAILS).append(") {\n");
        for (Map.Entry<String, String[]> e : arrays.entrySet()) {
            // A list that is not all floats reads as empty: every index
            // misses and leaves the fast path.
            String v = localVar(sc, e.getKey());
            indent(r, ind + 1);
            r.append("final double[] ").append(e.getValue()[0]).append(" = Py.floatArray(").append(v).append(");\n");
            indent(r, ind + 1);
            r.append("final int ").append(e.getValue()[1]).append(" = Py.floatCount(").append(v).append(");\n");
        }
        indent(r, ind + 1);
        r.append(label).append(": {\n");
        Set<String> inDoubles = new HashSet<String>();
        for (int i = 0; i < run.size(); i++) {
            inRegionFast = true;
            regionArrays = arrays;
            try {
                regionFast(run.get(i), inDoubles, label, r, ind + 2);
            } finally {
                regionArrays = null;
            }
            inRegionFast = false;
            indent(r, ind + 2);
            r.append(done).append(" = ").append(i + 1).append(";\n");
        }
        indent(r, ind + 1);
        r.append("}\n");
        indent(r, ind + 1);
        r.append("if (").append(done).append(" < ").append(run.size()).append(") {\n");
        indent(r, ind + 2);
        r.append(bails).append("++;\n");
        indent(r, ind + 1);
        r.append("}\n");
        indent(r, ind);
        r.append("}\n");
        indent(r, ind);
        r.append("if (").append(done).append(" < ").append(run.size()).append(") {\n");
        if (lazyIndex != null) {
            indent(r, ind + 1);
            r.append("if (").append(lazyIndex[1]).append(" == null) {\n");
            indent(r, ind + 2);
            r.append(localVar(sc, lazyIndex[0])).append(" = Long.valueOf(").append(lazyIndex[2]).append(");\n");
            indent(r, ind + 1);
            r.append("}\n");
        }
        if (carried != null) {
            for (Map.Entry<String, String[]> e : carried.entrySet()) {
                syncCarried(e.getKey(), e.getValue(), r, ind + 1);
            }
        }
        for (Map.Entry<String, Integer> e : firstStore.entrySet()) {
            String v = localVar(sc, e.getKey());
            indent(r, ind + 1);
            r.append(IF_PRIVATE).append(v).append("*/if (").append(done).append(" > ").append(e.getValue())
                    .append(") {\n");
            indent(r, ind + 2);
            r.append(v).append(" = Double.valueOf(").append(v).append("_d);\n");
            indent(r, ind + 1);
            r.append("}").append(COND_END).append("\n");
        }
        for (int i = 0; i < run.size(); i++) {
            indent(r, ind + 1);
            r.append("if (").append(done).append(" <= ").append(i).append(") {\n");
            stmt(run.get(i), r, ind + 2);
            indent(r, ind + 1);
            r.append("}\n");
        }
        indent(r, ind);
        r.append("}\n");
        indent(r, ind);
        r.append(REGION_END).append("\n");
        out.append(r);
        return end;
    }

    /// A copy of a counted loop's only region, run ahead of the loop: the
    /// fast path in a tight loop, with every name it does not assign read
    /// once before the loop and none of the general loop's bookkeeping. An
    /// iteration that cannot finish fast stops it before committing anything
    /// (the fast path only commits at its end, and the calls in it are pure),
    /// and the general loop then resumes at that same index.
    private void versionedLoop(List<Stmt> run, String[] v, String bails, int n) {
        int ind = Integer.parseInt(v[4]);
        String vl = "VL" + n;
        String fl = "fv" + n;
        // A store into a list commits at once, so only the last statement,
        // after which nothing can bail, may make one.
        for (int i = 0; i < run.size(); i++) {
            Stmt s = run.get(i);
            Expr t = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
            // A store into something unnamed may be any list.
            if (t instanceof Subscript && (i < run.size() - 1 || !(((Subscript) t).value instanceof Name))) {
                return;
            }
        }
        // The carried doubles are written to shadows that commit when the
        // iteration finishes, so a bail part way leaves them as they were.
        Map<String, String[]> real = carried;
        Map<String, String[]> shadow = new LinkedHashMap<String, String[]>();
        StringBuilder enter = new StringBuilder();
        StringBuilder commit = new StringBuilder();
        for (Map.Entry<String, String[]> e : real.entrySet()) {
            String[] c = e.getValue();
            String[] s = {"s" + c[0], "s" + c[1]};
            shadow.put(e.getKey(), s);
            indent(enter, ind + 4);
            enter.append("double ").append(s[0]).append(" = ").append(c[0]).append(";\n");
            indent(enter, ind + 4);
            enter.append("boolean ").append(s[1]).append(" = ").append(c[1]).append(";\n");
            indent(commit, ind + 4);
            commit.append(c[0]).append(" = ").append(s[0]).append(";\n");
            indent(commit, ind + 4);
            commit.append(c[1]).append(" = ").append(s[1]).append(";\n");
        }
        hoist = new LinkedHashMap<String, String>();
        hoistRaw = new LinkedHashMap<String, String>();
        hoistAssigned = new HashSet<String>();
        hoistStored = new HashSet<String>();
        hoistFloats = new LinkedHashMap<String, String[]>();
        hoistTests = new ArrayList<String>();
        hoistDecls = new ArrayList<String>();
        for (Stmt s : run) {
            Expr t = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
            if (t instanceof Name) {
                hoistAssigned.add(((Name) t).id);
            } else if (t instanceof Subscript && ((Subscript) t).value instanceof Name) {
                hoistStored.add(((Name) ((Subscript) t).value).id);
            }
        }
        StringBuilder body = new StringBuilder();
        Set<String> inDoubles = new HashSet<String>();
        carried = shadow;
        try {
            for (Stmt s : run) {
                inRegionFast = true;
                regionFast(s, inDoubles, fl, body, ind + 4);
                inRegionFast = false;
            }
        } finally {
            inRegionFast = false;
            carried = real;
        }
        Map<String, String> raw = hoistRaw;
        hoist = null;
        hoistRaw = null;
        hoistAssigned = null;
        Map<String, String[]> floats = hoistFloats;
        List<String> tests = hoistTests;
        List<String> decls = hoistDecls;
        hoistStored = null;
        hoistFloats = null;
        hoistTests = null;
        hoistDecls = null;
        StringBuilder r = versionOut;
        indent(r, ind);
        r.append("if (").append(v[0]).append(" == null && ").append(bails).append(" < ").append(REGION_BAILS)
                .append(") {\n");
        StringBuilder test = new StringBuilder();
        for (Map.Entry<String, String> e : raw.entrySet()) {
            indent(r, ind + 1);
            r.append("final Object ").append(e.getKey()).append(" = ").append(e.getValue()).append(";\n");
            test.append(" && ").append(e.getKey()).append(" != null");
        }
        for (Map.Entry<String, String[]> e : floats.entrySet()) {
            String[] fa = e.getValue();
            indent(r, ind + 1);
            r.append("final double[] ").append(fa[0]).append(" = Py.floatArray(").append(e.getKey()).append(");\n");
            indent(r, ind + 1);
            r.append("final int ").append(fa[1]).append(" = Py.floatCount(").append(e.getKey()).append(");\n");
        }
        for (String t : tests) {
            test.append(" && ").append(t);
        }
        // An unbound name is left to the general loop, which raises for it
        // where the program reads it.
        indent(r, ind + 1);
        r.append("if (true").append(test).append(") {\n");
        for (String d : decls) {
            indent(r, ind + 2);
            r.append(d).append("\n");
        }
        indent(r, ind + 2);
        r.append(vl).append(": while (").append(v[3]).append(" > 0 ? ").append(v[1]).append(" < ").append(v[2])
                .append(" : ").append(v[1]).append(" > ").append(v[2]).append(") {\n");
        indent(r, ind + 3);
        r.append(fl).append(": {\n");
        r.append(enter);
        r.append(body);
        r.append(commit);
        indent(r, ind + 4);
        r.append(v[1]).append(" += ").append(v[3]).append(";\n");
        indent(r, ind + 4);
        r.append("continue ").append(vl).append(";\n");
        indent(r, ind + 3);
        r.append("}\n");
        indent(r, ind + 3);
        r.append("break;\n");
        indent(r, ind + 2);
        r.append("}\n");
        indent(r, ind + 1);
        r.append("}\n");
        indent(r, ind);
        r.append("}\n");
    }

    /// In the versioned loop, a call of the module's own long twin whose
    /// function and non-index arguments are all hoisted: the checks the
    /// general path makes per call (the global still being that function,
    /// each argument an int) are made once on entry, and the call is the
    /// twin alone. Null when the call is not of that shape.
    private StringBuilder hoistedDirectCall(Call c) {
        if (!(c.func instanceof Name)) {
            return null;
        }
        Integer arity = directLong.get(((Name) c.func).id);
        if (arity == null || arity.intValue() != c.args.size() || sc.resolve(((Name) c.func).id) != Scope.Binding.GLOBAL) {
            return null;
        }
        String fe = expr(c.func);
        if (!hoist.containsValue(fe)) {
            return null;
        }
        List<String> parts = new ArrayList<String>();
        List<String> tests = new ArrayList<String>();
        List<String> decls = new ArrayList<String>();
        for (Expr a : c.args) {
            if (lazyIndex != null && a instanceof Name && lazyIndex[0].equals(((Name) a).id)) {
                parts.add(lazyIndex[2]);
            } else if (intLiteral(a) != null) {
                parts.add(intLiteral(a) + "L");
            } else {
                String ae = expr(a);
                if (!hoist.containsValue(ae)) {
                    return null;
                }
                tests.add(ae + " instanceof Long");
                decls.add("final long " + ae + "L = ((Long) " + ae + ").longValue();");
                parts.add(ae + "L");
            }
        }
        String m = mangle(((Name) c.func).id);
        tests.add(0, fe + " == DEF_" + m);
        for (String t : tests) {
            if (!hoistTests.contains(t)) {
                hoistTests.add(t);
            }
        }
        for (String d : decls) {
            if (!hoistDecls.contains(d)) {
                hoistDecls.add(d);
            }
        }
        StringBuilder call = new StringBuilder("PL_").append(m).append("(");
        for (int i = 0; i < parts.size(); i++) {
            call.append(i > 0 ? ", " : "").append(parts.get(i));
        }
        // The caller closes the call.
        return call;
    }

    /// The lists a region may read through a `double[]` taken on entry: a
    /// local it subscripts with an int and never assigns. Only when the region
    /// calls nothing, since a call could grow the list and replace its array;
    /// the region's own stores write the array in place.
    private Map<String, String[]> regionArrays(List<Stmt> run, int n) {
        Map<String, String[]> m = new LinkedHashMap<String, String[]>();
        if (hoist != null) {
            return m;
        }
        Set<String> assigned = new HashSet<String>();
        List<Subscript> subs = new ArrayList<Subscript>();
        for (Stmt s : run) {
            Expr t = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
            Expr v = s instanceof Assign ? ((Assign) s).value : ((AugAssign) s).value;
            if (t instanceof Name) {
                assigned.add(((Name) t).id);
            } else if (!regionSubs(t, subs)) {
                return m;
            }
            if (!regionSubs(v, subs)) {
                return m;
            }
        }
        for (Subscript sub : subs) {
            if (!(sub.value instanceof Name) || constIndex(sub.index) == null && !(sub.index instanceof Name)) {
                continue;
            }
            String id = ((Name) sub.value).id;
            if (assigned.contains(id) || sc.resolve(id) != Scope.Binding.LOCAL || m.containsKey(id)) {
                continue;
            }
            int k = m.size();
            m.put(id, new String[] {"fra" + n + "_" + k, "frn" + n + "_" + k});
        }
        return m;
    }

    /// Collects the subscripts of a region expression; false when it calls.
    private static boolean regionSubs(Expr e, List<Subscript> out) {
        if (e instanceof Call) {
            return false;
        }
        if (e instanceof BinOp) {
            return regionSubs(((BinOp) e).left, out) && regionSubs(((BinOp) e).right, out);
        }
        if (e instanceof UnaryOp) {
            return regionSubs(((UnaryOp) e).operand, out);
        }
        if (e instanceof Subscript) {
            out.add((Subscript) e);
            return regionSubs(((Subscript) e).value, out) && regionSubs(((Subscript) e).index, out);
        }
        return true;
    }

    /// The hoisted array of a region subscript with an unboxed index, after
    /// a bounds check that leaves the fast path; null when not hoisted.
    private String regionArrayRef(Expr base, String k, String label, StringBuilder r, int ind) {
        String[] a = regionArrays == null || k == null || !(base instanceof Name) ? null
                : regionArrays.get(((Name) base).id);
        if (a == null) {
            return null;
        }
        indent(r, ind);
        r.append("if (").append(k).append(" < 0 || ").append(k).append(" >= ").append(a[1]).append(") {\n");
        indent(r, ind + 1);
        r.append("break ").append(label).append(";\n");
        indent(r, ind);
        r.append("}\n");
        return a[0] + "[(int) " + k + "]";
    }

    /// The names a region expression reads, in order.
    private void regionReads(Expr e, List<Expr> out) {
        if (e instanceof BinOp) {
            regionReads(((BinOp) e).left, out);
            regionReads(((BinOp) e).right, out);
        } else if (e instanceof UnaryOp) {
            regionReads(((UnaryOp) e).operand, out);
        } else if (e instanceof Name) {
            out.add(e);
        } else if (e instanceof Subscript) {
            regionReads(((Subscript) e).value, out);
            regionReads(((Subscript) e).index, out);
        } else if (e instanceof Call) {
            regionReads(((Call) e).func, out);
            for (Expr a : ((Call) e).args) {
                regionReads(a, out);
            }
        }
    }

    /// One statement of a region's fast path.
    private void regionFast(Stmt s, Set<String> inDoubles, String label, StringBuilder r, int ind) {
        Expr target = s instanceof Assign ? ((Assign) s).targets.get(0) : ((AugAssign) s).target;
        Expr value = s instanceof Assign ? ((Assign) s).value : ((AugAssign) s).value;
        indent(r, ind);
        r.append("{\n");
        int[] tmp = new int[1];
        String obj = null;
        String idx = null;
        String slot = null;
        if (target instanceof Subscript) {
            Subscript ts = (Subscript) target;
            String tk = constIndex(ts.index);
            if (tk == null && regionArrays != null && ts.value instanceof Name
                    && regionArrays.containsKey(((Name) ts.value).id)) {
                tk = lazyLong(ts.index, label, r, ind + 1);
            }
            slot = regionArrayRef(ts.value, tk, label, r, ind + 1);
        }
        // A hoisted slot needs no object or index temps.
        if (slot == null && target instanceof Subscript) {
            Subscript sub = (Subscript) target;
            obj = "fo" + tmp[0]++;
            idx = "fi" + tmp[0]++;
            indent(r, ind + 1);
            r.append("final Object ").append(obj).append(" = ").append(expr(sub.value)).append(";\n");
            String ci = constIndex(sub.index);
            String li = lazyLong(sub.index, label, r, ind + 1);
            indent(r, ind + 1);
            if (li != null || ci != null) {
                r.append("final long ").append(idx).append(" = ").append(li != null ? li : ci).append(";\n");
            } else {
                r.append("final Object ").append(idx).append(" = ").append(expr(sub.index)).append(";\n");
            }
        }
        String cur = null;
        if (s instanceof AugAssign) {
            if (slot != null) {
                cur = slot;
            } else if (target instanceof Subscript) {
                cur = regionItemLoad(obj, idx, label, tmp, r, ind + 1);
            } else {
                cur = regionName((Name) target, inDoubles, label, tmp, r, ind + 1);
            }
        }
        String v = regionValue(value, inDoubles, label, tmp, r, ind + 1);
        if (cur != null) {
            String op = ((AugAssign) s).op;
            if ("/".equals(op)) {
                indent(r, ind + 1);
                r.append("if (").append(v).append(" == 0.0) {\n");
                indent(r, ind + 2);
                r.append("break ").append(label).append(";\n");
                indent(r, ind + 1);
                r.append("}\n");
            }
            v = "(" + cur + " " + op + " " + v + ")";
        }
        indent(r, ind + 1);
        if (slot != null) {
            r.append(slot).append(" = ").append(v).append(";\n");
        } else if (target instanceof Subscript) {
            r.append("Py.setFloat(").append(obj).append(", ").append(idx).append(", ").append(v).append(");\n");
        } else if (carried != null && carried.containsKey(((Name) target).id)) {
            String[] c = carried.get(((Name) target).id);
            r.append(c[0]).append(" = ").append(v).append(";\n");
            indent(r, ind + 1);
            r.append(c[1]).append(" = true;\n");
            inDoubles.add(((Name) target).id);
        } else {
            String lv = localVar(sc, ((Name) target).id);
            r.append(lv).append("_d = ").append(v).append(";\n");
            indent(r, ind + 1);
            r.append(IF_SHARED).append(lv).append("*/").append(lv).append(" = Double.valueOf(").append(lv)
                    .append("_d);").append(COND_END).append("\n");
            inDoubles.add(((Name) target).id);
        }
        indent(r, ind);
        r.append("}\n");
    }

    /// Loads a boxed operand, leaving the region unless it is a float.
    private String regionLoad(String source, String label, int[] tmp, StringBuilder r, int ind) {
        String o = "fv" + tmp[0]++;
        indent(r, ind);
        r.append("final Object ").append(o).append(" = ").append(source).append(";\n");
        indent(r, ind);
        r.append("if (!(").append(o).append(" instanceof Double)) {\n");
        indent(r, ind + 1);
        r.append("break ").append(label).append(";\n");
        indent(r, ind);
        r.append("}\n");
        String d = "fd" + tmp[0]++;
        indent(r, ind);
        r.append("final double ").append(d).append(" = ((Double) ").append(o).append(").doubleValue();\n");
        return d;
    }

    /// Reads a list or tuple item unboxed, leaving the region unless it is a
    /// float.
    private String regionItemLoad(String o, String i, String label, int[] tmp, StringBuilder r, int ind) {
        String d = "fd" + tmp[0]++;
        indent(r, ind);
        r.append("final double ").append(d).append(" = Py.floatItem(").append(o).append(", ").append(i).append(");\n");
        indent(r, ind);
        r.append("if (").append(d).append(" != ").append(d).append(") {\n");
        indent(r, ind + 1);
        r.append("break ").append(label).append(";\n");
        indent(r, ind);
        r.append("}\n");
        return d;
    }

    private String regionName(Name nm, Set<String> inDoubles, String label, int[] tmp, StringBuilder r, int ind) {
        String li = lazyLong(nm, label, r, ind);
        if (li != null) {
            return "((double) " + li + ")";
        }
        String[] c = carried == null ? null : carried.get(nm.id);
        if (c != null) {
            if (inDoubles.contains(nm.id)) {
                return c[0];
            }
            // The loop holds it as a double once some iteration stored it.
            String d = "fd" + tmp[0]++;
            String o = "fv" + tmp[0]++;
            indent(r, ind);
            r.append("final double ").append(d).append(";\n");
            indent(r, ind);
            r.append("if (").append(c[1]).append(") {\n");
            indent(r, ind + 1);
            r.append(d).append(" = ").append(c[0]).append(";\n");
            indent(r, ind);
            r.append("} else {\n");
            indent(r, ind + 1);
            r.append("final Object ").append(o).append(" = ").append(expr(nm)).append(";\n");
            indent(r, ind + 1);
            r.append("if (!(").append(o).append(" instanceof Double)) {\n");
            indent(r, ind + 2);
            r.append("break ").append(label).append(";\n");
            indent(r, ind + 1);
            r.append("}\n");
            indent(r, ind + 1);
            r.append(d).append(" = ((Double) ").append(o).append(").doubleValue();\n");
            indent(r, ind);
            r.append("}\n");
            return d;
        }
        if (inDoubles.contains(nm.id) && sc.resolve(nm.id) == Scope.Binding.LOCAL) {
            return localVar(sc, nm.id) + "_d";
        }
        return regionLoad(expr(nm), label, tmp, r, ind);
    }

    /// The unboxed index of the lazy `range` loop when `e` names it, else
    /// null. Counting by a `range` that is not the builtin leaves the region.
    private String lazyLong(Expr e, String label, StringBuilder r, int ind) {
        if (lazyIndex == null || !(e instanceof Name) || !lazyIndex[0].equals(((Name) e).id)) {
            return null;
        }
        if (hoist != null) {
            // The versioned loop runs only while counting.
            return lazyIndex[2];
        }
        indent(r, ind);
        r.append("if (").append(lazyIndex[1]).append(" != null) {\n");
        indent(r, ind + 1);
        r.append("break ").append(label).append(";\n");
        indent(r, ind);
        r.append("}\n");
        return lazyIndex[2];
    }

    /// A call argument as the object the callee sees: a local the region
    /// holds as a double is boxed for it.
    private String regionBoxed(Expr a, Set<String> inDoubles) {
        if (a instanceof Name) {
            String id = ((Name) a).id;
            String[] c = carried == null ? null : carried.get(id);
            if (c != null) {
                return "(" + c[1] + " ? Double.valueOf(" + c[0] + ") : " + expr(a) + ")";
            }
            if (inDoubles.contains(id) && sc.resolve(id) == Scope.Binding.LOCAL) {
                return "Double.valueOf(" + localVar(sc, id) + "_d)";
            }
        }
        return expr(a);
    }

    /// Whether `e` is int arithmetic over the lazy loop index and int
    /// literals (`j`, `j + 1`, `-j * 2`): Python computes it exactly, as an
    /// int, so the region does too, in a long, before any float sees it.
    private boolean regionInt(Expr e) {
        if (e instanceof Name) {
            return lazyIndex != null && lazyIndex[0].equals(((Name) e).id);
        }
        if (e instanceof Constant) {
            return intLiteral(e) != null;
        }
        if (e instanceof UnaryOp) {
            return "-".equals(((UnaryOp) e).op) && regionInt(((UnaryOp) e).operand);
        }
        if (e instanceof BinOp) {
            String op = ((BinOp) e).op;
            return ("+".equals(op) || "-".equals(op) || "*".equals(op)) && regionInt(((BinOp) e).left)
                    && regionInt(((BinOp) e).right);
        }
        return false;
    }

    /// The long value of a regionInt expression, leaving the region where
    /// Python's int would outgrow 64 bits.
    private String regionLong(Expr e, String label, int[] tmp, StringBuilder r, int ind) {
        if (e instanceof Constant) {
            return "(" + intLiteral(e) + "L)";
        }
        if (e instanceof Name) {
            return lazyLong(e, label, r, ind);
        }
        String t = "fl" + tmp[0]++;
        if (e instanceof UnaryOp) {
            String a = regionLong(((UnaryOp) e).operand, label, tmp, r, ind);
            regionBail(a + " == Long.MIN_VALUE", label, r, ind);
            indent(r, ind);
            r.append("final long ").append(t).append(" = -").append(a).append(";\n");
            return t;
        }
        BinOp b = (BinOp) e;
        String l = regionLong(b.left, label, tmp, r, ind);
        String rt = regionLong(b.right, label, tmp, r, ind);
        indent(r, ind);
        r.append("final long ").append(t).append(" = ").append(l).append(' ').append(b.op).append(' ').append(rt)
                .append(";\n");
        if ("+".equals(b.op)) {
            regionBail("((" + l + " ^ " + t + ") & (" + rt + " ^ " + t + ")) < 0", label, r, ind);
        } else if ("-".equals(b.op)) {
            regionBail("((" + l + " ^ " + rt + ") & (" + l + " ^ " + t + ")) < 0", label, r, ind);
        } else {
            regionBail("(((" + l + " + 0x80000000L) | (" + rt + " + 0x80000000L)) >>> 32) != 0 && " + l
                    + " != 0 && (" + t + " / " + l + " != " + rt + " || " + l + " == -1 && " + rt
                    + " == Long.MIN_VALUE)", label, r, ind);
        }
        return t;
    }

    private static void regionBail(String cond, String label, StringBuilder r, int ind) {
        indent(r, ind);
        r.append("if (").append(cond).append(") {\n");
        indent(r, ind + 1);
        r.append("break ").append(label).append(";\n");
        indent(r, ind);
        r.append("}\n");
    }

    /// The fast path's double for `e`, its leaves read left to right.
    private String regionValue(Expr e, Set<String> inDoubles, String label, int[] tmp, StringBuilder r, int ind) {
        if (!(e instanceof Constant) && regionInt(e)) {
            // One rounding, from the exact int, as float(int) does.
            return "((double) " + regionLong(e, label, tmp, r, ind) + ")";
        }
        if (e instanceof Constant) {
            if (floatConst(e)) {
                double d = Double.parseDouble(((Constant) e).text.replace("_", ""));
                return Double.isInfinite(d) ? "Double.POSITIVE_INFINITY" : "(" + Double.toString(d) + ")";
            }
            return "(" + ((Constant) e).text + ".0)";
        }
        if (e instanceof Name) {
            return regionName((Name) e, inDoubles, label, tmp, r, ind);
        }
        if (e instanceof Call) {
            Call c = (Call) e;
            String d = "fd" + tmp[0]++;
            boolean longs = c.args.size() <= 2;
            boolean anyLazy = false;
            for (Expr a : c.args) {
                boolean lz = lazyIndex != null && a instanceof Name && lazyIndex[0].equals(((Name) a).id);
                anyLazy |= lz;
                longs &= lz || intLiteral(a) != null || a instanceof Name && !(carried != null
                        && carried.containsKey(((Name) a).id)) && !inDoubles.contains(((Name) a).id);
            }
            StringBuilder call = longs && anyLazy && hoist != null ? hoistedDirectCall(c) : null;
            // A call fully specialized in the versioned loop is emitted below.
            if (call == null && longs && anyLazy) {
                // An unboxed index among the arguments: the long twin when
                // the others are ints too, so nothing is boxed; otherwise the
                // object call, boxing the index.
                String fn = "ff" + tmp[0]++;
                indent(r, ind);
                r.append("final Object ").append(fn).append(" = ").append(expr(c.func)).append(";\n");
                StringBuilder cond = new StringBuilder("true");
                StringBuilder typed = new StringBuilder("Py.callDL(").append(fn);
                StringBuilder boxed = new StringBuilder("Py.callD(").append(fn);
                for (Expr a : c.args) {
                    String li = lazyLong(a, label, r, ind);
                    if (li != null) {
                        typed.append(", ").append(li);
                        boxed.append(", Long.valueOf(").append(li).append(")");
                    } else if (intLiteral(a) != null) {
                        typed.append(", ").append(intLiteral(a)).append("L");
                        boxed.append(", ").append(expr(a));
                    } else {
                        String ao = "fa" + tmp[0]++;
                        indent(r, ind);
                        r.append("final Object ").append(ao).append(" = ").append(expr(a)).append(";\n");
                        cond.append(" && ").append(ao).append(" instanceof Long");
                        typed.append(", ((Long) ").append(ao).append(").longValue()");
                        boxed.append(", ").append(ao);
                    }
                }
                Integer arity = c.func instanceof Name ? directLong.get(((Name) c.func).id) : null;
                if (arity != null && arity.intValue() == c.args.size()
                        && sc.resolve(((Name) c.func).id) == Scope.Binding.GLOBAL) {
                    // The module's own function, unless the global was rebound.
                    String m = mangle(((Name) c.func).id);
                    typed.replace(0, ("Py.callDL(" + fn + ", ").length(), "PL_" + m + "(");
                    cond.append(" && ").append(fn).append(" == DEF_").append(m);
                    String generic = "Py.callDL(" + fn + ", " + typed.substring(("PL_" + m + "(").length());
                    call = new StringBuilder("(").append(cond).append(") ? ").append(typed).append(") : (")
                            .append(cond.substring(0, cond.lastIndexOf(" && "))).append(") ? ").append(generic)
                            .append(") : ").append(boxed);
                } else {
                    call = new StringBuilder("(").append(cond).append(") ? ").append(typed).append(") : ")
                            .append(boxed);
                }
            } else {
                call = new StringBuilder("Py.callD(").append(expr(c.func));
                for (Expr a : c.args) {
                    call.append(", ").append(regionBoxed(a, inDoubles));
                }
            }
            indent(r, ind);
            r.append("final double ").append(d).append(" = ").append(call).append(");\n");
            indent(r, ind);
            r.append("if (").append(d).append(" != ").append(d).append(") {\n");
            indent(r, ind + 1);
            r.append("break ").append(label).append(";\n");
            indent(r, ind);
            r.append("}\n");
            return d;
        }
        if (e instanceof Subscript) {
            Subscript sub = (Subscript) e;
            String ci = constIndex(sub.index);
            String o = "fo" + tmp[0]++;
            String i = "fi" + tmp[0]++;
            String li = lazyLong(sub.index, label, r, ind);
            String ref = regionArrayRef(sub.value, li != null ? li : ci, label, r, ind);
            if (ref != null) {
                String d = "fd" + tmp[0]++;
                indent(r, ind);
                r.append("final double ").append(d).append(" = ").append(ref).append(";\n");
                return d;
            }
            String base = expr(sub.value);
            if ((li != null || ci != null) && hoist != null && hoist.containsValue(base)
                    && !hoistStored.contains(((Name) sub.value).id)) {
                // The versioned loop reads a list nothing in it stores into
                // straight from the double[] it took on entry: no call, and
                // a region cannot grow or retype a list.
                String[] fa = hoistFloats.get(base);
                if (fa == null) {
                    fa = new String[] {base + "f", base + "n"};
                    hoistFloats.put(base, fa);
                }
                String k = li != null ? li : ci;
                String d = "fd" + tmp[0]++;
                indent(r, ind);
                r.append("if (").append(k).append(" < 0 || ").append(k).append(" >= ").append(fa[1]).append(") {\n");
                indent(r, ind + 1);
                r.append("break ").append(label).append(";\n");
                indent(r, ind);
                r.append("}\n");
                indent(r, ind);
                r.append("final double ").append(d).append(" = ").append(fa[0]).append("[(int) ").append(k).append("];\n");
                return d;
            }
            indent(r, ind);
            r.append("final Object ").append(o).append(" = ").append(base).append(";\n");
            if (li != null || ci != null) {
                // An index already unboxed (or a constant) is passed as a long.
                return regionItemLoad(o, li != null ? li : ci, label, tmp, r, ind);
            }
            indent(r, ind);
            r.append("final Object ").append(i).append(" = ").append(expr(sub.index)).append(";\n");
            return regionItemLoad(o, i, label, tmp, r, ind);
        }
        String t = "ft" + tmp[0]++;
        if (e instanceof UnaryOp) {
            String a = regionValue(((UnaryOp) e).operand, inDoubles, label, tmp, r, ind);
            indent(r, ind);
            r.append("final double ").append(t).append(" = -").append(a).append(";\n");
            return t;
        }
        BinOp b = (BinOp) e;
        if ("/".equals(b.op) && regionInt(b.left) && regionInt(b.right)) {
            // int / int is correctly rounded in Python; dividing the doubles
            // is too while both convert exactly.
            String ll = regionLong(b.left, label, tmp, r, ind);
            String rl = regionLong(b.right, label, tmp, r, ind);
            regionBail(ll + " > (1L << 53) || " + ll + " < -(1L << 53) || " + rl + " > (1L << 53) || " + rl
                    + " < -(1L << 53) || " + rl + " == 0", label, r, ind);
            indent(r, ind);
            r.append("final double ").append(t).append(" = ((double) ").append(ll).append(") / ((double) ")
                    .append(rl).append(");\n");
            return t;
        }
        String lv = regionValue(b.left, inDoubles, label, tmp, r, ind);
        String rv = regionValue(b.right, inDoubles, label, tmp, r, ind);
        indent(r, ind);
        if ("**".equals(b.op) && regionInt(b.left) && regionInt(b.right)) {
            // int ** int is an int (or a float for a negative power):
            // the general path decides.
            r.append("if (").append(lv).append(" == ").append(lv).append(") {\n");
            indent(r, ind + 1);
            r.append("break ").append(label).append(";\n");
            indent(r, ind);
            r.append("}\n");
            indent(r, ind);
        }
        if ("**".equals(b.op)) {
            r.append("final double ").append(t).append(" = Math.pow(").append(lv).append(", ").append(rv)
                    .append(");\n");
            indent(r, ind);
            r.append("if (Double.isNaN(").append(t).append(") || Double.isInfinite(").append(t).append(")) {\n");
            indent(r, ind + 1);
            r.append("break ").append(label).append(";\n");
            indent(r, ind);
            r.append("}\n");
            return t;
        }
        if ("/".equals(b.op)) {
            r.append("if (").append(rv).append(" == 0.0) {\n");
            indent(r, ind + 1);
            r.append("break ").append(label).append(";\n");
            indent(r, ind);
            r.append("}\n");
            indent(r, ind);
        }
        r.append("final double ").append(t).append(" = ").append(lv).append(' ').append(b.op).append(' ').append(rv)
                .append(";\n");
        return t;
    }

    /// Settles the regions of one emitted function body: a local stays a
    /// double only if no region reads it before assigning it and no text
    /// outside the regions names it at all.
    private String resolveRegions(String body) {
        if (body.indexOf(REGION_START) < 0) {
            return body;
        }
        StringBuilder outside = new StringBuilder();
        int at = 0;
        while (true) {
            int st = body.indexOf(REGION_START, at);
            if (st < 0) {
                outside.append(body, at, body.length());
                break;
            }
            outside.append(body, at, st);
            at = body.indexOf(REGION_END, st) + REGION_END.length();
        }
        String rest = outside.toString();
        StringBuilder r = new StringBuilder();
        at = 0;
        while (true) {
            int a = nextCond(body, at);
            if (a < 0) {
                r.append(body, at, body.length());
                break;
            }
            r.append(body, at, a);
            boolean ifPrivate = body.startsWith(IF_PRIVATE, a);
            int nameEnd = body.indexOf("*/", a + IF_SHARED.length());
            String v = body.substring(a + IF_SHARED.length(), nameEnd);
            int e = body.indexOf(COND_END, nameEnd);
            boolean priv = !regionShared.contains(v) && !mentions(rest, v);
            if (priv == ifPrivate) {
                r.append(body, nameEnd + 2, e);
            }
            at = e + COND_END.length();
        }
        return r.toString().replace(REGION_START + "\n", "").replace(REGION_END + "\n", "")
                .replaceAll("(?m)^ +\n", "");
    }

    private static int nextCond(String s, int from) {
        int a = s.indexOf(IF_SHARED, from);
        int b = s.indexOf(IF_PRIVATE, from);
        return a < 0 ? b : b < 0 ? a : Math.min(a, b);
    }

    /// Whether `text` names the Java identifier `v`.
    private static boolean mentions(String text, String v) {
        int i = text.indexOf(v);
        while (i >= 0) {
            int e = i + v.length();
            boolean before = i > 0 && Character.isJavaIdentifierPart(text.charAt(i - 1));
            boolean after = e < text.length() && Character.isJavaIdentifierPart(text.charAt(e));
            if (!before && !after) {
                return true;
            }
            i = text.indexOf(v, e);
        }
        return false;
    }

    /// A subscript written as a small decimal int literal, as Java `long`
    /// source, or null: such an index needs no int object at all.
    private static String constIndex(Expr x) {
        String sign = "";
        if (x instanceof UnaryOp && "-".equals(((UnaryOp) x).op)) {
            sign = "-";
            x = ((UnaryOp) x).operand;
        }
        if (!(x instanceof Constant) || ((Constant) x).kind != Constant.Kind.INT) {
            return null;
        }
        String t = ((Constant) x).text;
        if (t.length() == 0 || t.length() > 9 || (t.length() > 1 && t.charAt(0) == '0')) {
            return null;
        }
        for (int k = 0; k < t.length(); k++) {
            if (t.charAt(k) < '0' || t.charAt(k) > '9') {
                return null;
            }
        }
        return sign + t + "L";
    }

    private static boolean isConstant(Expr x) {
        if (x instanceof Constant) {
            return true;
        }
        if (x instanceof UnaryOp) {
            UnaryOp u = (UnaryOp) x;
            return ("-".equals(u.op) || "+".equals(u.op)) && u.operand instanceof Constant
                    && (((Constant) u.operand).kind == Constant.Kind.INT
                    || ((Constant) u.operand).kind == Constant.Kind.FLOAT);
        }
        if (x instanceof BinOp) {
            // Folded when both sides are; CPython also declines a few that
            // would make huge results, which no set literal writes.
            return isConstant(((BinOp) x).left) && isConstant(((BinOp) x).right);
        }
        if (x instanceof TupleExpr) {
            return allConstant(((TupleExpr) x).elts);
        }
        return false;
    }

}
