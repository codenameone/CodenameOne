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
import com.codename1.python.transpiler.ast.Node.Alias;
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Call;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.DictExpr;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.ImportFrom;
import com.codename1.python.transpiler.ast.Node.Keyword;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.parser.Parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The per-class hooks a pydantic model class gets, written into the class
/// at compile time as plain Python naming each field literally.
///
/// The bundled pydantic validates and dumps through a dict of field values;
/// CPython's reads and writes those values with `getattr`/`setattr` over the
/// field names, which is reflection. Here every `class M(BaseModel)` (or a
/// subclass of one in the same module) gets, as if its author had written
/// them:
///
/// ```python
/// class Item(BaseModel):
///     name: str
///     size: int = 0
///     # added:
///     def __cn1_pd_load__(self): return {"name": self.name, "size": self.size}
///     def __cn1_pd_store__(self, values): ...   # self.name = values["name"] ...
///     @classmethod
///     def __cn1_pd_from_attrs__(cls, v): ...    # from_attributes=True
///     def __cn1_pd_init_private__(self): ...    # PrivateAttr defaults
///     def __cn1_pd_copy_private__(self, src, deep): ...
///     def __cn1_pd_computed__(self): ...        # @computed_field values
/// ```
///
/// A frozen or `validate_assignment` model has its fields replaced by
/// properties whose setter calls `BaseModel._cn1_pd_assign` with the field's
/// FieldInfo by position and its literal name (a class binding `__setattr__` has no layout); the hooks read and
/// write the backing slots directly, so construction never meets the frozen
/// check. `class P(RootModel[T])` becomes `class P(RootModel)` with
/// `root: T`, the hooks RootModel itself declares.
///
/// What cannot be lowered exactly fails the build, naming the class: a
/// model whose base is a model of another module, class keywords, a
/// `model_config` that is not a literal `ConfigDict(...)`/dict, a name-mangled
/// private attribute.
///
/// Every field of a non-root model is such a property: on a model that is
/// neither frozen nor validate_assignment the setter stores the slot and
/// adds the literal name to `model_fields_set`. A name that is not a field
/// reaches `BaseModel.__cn1_new_attr__` (the runtime's new-attribute hook),
/// which raises pydantic's ValueError or writes the extras dict.
public final class PydanticLowering {

    private PydanticLowering() {
    }

    private static final class Model {
        final List<String> fields = new ArrayList<String>();
        final List<String> privates = new ArrayList<String>();
        final List<String> computed = new ArrayList<String>();
        /// The class's own field annotations, in order, for __cn1_pd_ann__.
        final List<Expr> annNodes = new ArrayList<Expr>();
        /// Fields are properties over `_cn1pd_<name>` slots: every non-root model.
        boolean props;
        /// frozen or validate_assignment: the setter goes through _cn1_pd_assign.
        boolean guarded;
        boolean frozen;
    }

    /// Lowers the model classes of `tree`, the module at path `path`.
    public static void rewrite(Node.Module tree, String path) {
        if (path.endsWith("pydantic/__init__.py")) {
            return;
        }
        Set<String> base = new HashSet<String>();
        Set<String> root = new HashSet<String>();
        Set<String> computedDeco = new HashSet<String>();
        Set<String> fieldFn = new HashSet<String>();
        Set<String> privateFn = new HashSet<String>();
        for (Stmt s : tree.body) {
            if (s instanceof ImportFrom && "pydantic".equals(((ImportFrom) s).module) && ((ImportFrom) s).level == 0) {
                for (Alias a : ((ImportFrom) s).names) {
                    String bound = a.asname != null ? a.asname : a.name;
                    if ("BaseModel".equals(a.name)) {
                        base.add(bound);
                    } else if ("RootModel".equals(a.name)) {
                        root.add(bound);
                    } else if ("computed_field".equals(a.name)) {
                        computedDeco.add(bound);
                    } else if ("Field".equals(a.name)) {
                        fieldFn.add(bound);
                    } else if ("PrivateAttr".equals(a.name)) {
                        privateFn.add(bound);
                    }
                }
            }
        }
        if (base.isEmpty() && root.isEmpty()) {
            return;
        }
        Map<String, Model> models = new HashMap<String, Model>();
        for (int i = 0; i < tree.body.size(); i++) {
            Stmt s = tree.body.get(i);
            if (s instanceof ClassDef
                    && lower((ClassDef) s, path, base, root, computedDeco, fieldFn, privateFn, models)) {
                // The tables the metaclass used to build, made once the
                // class exists: `Item.__cn1_pd_setup__()`.
                for (Stmt g : Parser.parse(((ClassDef) s).name + ".__cn1_pd_setup__()\n", path).body) {
                    DataclassInit.atLine(g, s.line);
                    tree.body.add(++i, g);
                }
            }
        }
        refuseNested(tree.body, path, base, root, models, false);
    }

    /// A model class not at module level is refused: its hooks would be
    /// lowered with nothing to check the base against.
    private static void refuseNested(Object o, String path, Set<String> base, Set<String> root,
            Map<String, Model> models, boolean nested) {
        if (o instanceof ClassDef) {
            ClassDef c = (ClassDef) o;
            if (nested && !c.bases.isEmpty() && modelBase(c.bases.get(0), base, root, models)) {
                throw error(path, c, "pydantic model class " + c.name
                        + " is not defined at module level; its per-class hooks cannot be generated");
            }
            for (Stmt s : c.body) {
                refuseNested(s, path, base, root, models, true);
            }
            return;
        }
        if (o instanceof FunctionDef) {
            for (Stmt s : ((FunctionDef) o).body) {
                refuseNested(s, path, base, root, models, true);
            }
            return;
        }
        if (o instanceof List) {
            for (Object x : (List<?>) o) {
                refuseNested(x, path, base, root, models, nested);
            }
        }
    }

    private static boolean modelBase(Expr b, Set<String> base, Set<String> root, Map<String, Model> models) {
        if (b instanceof Name) {
            String id = ((Name) b).id;
            return base.contains(id) || root.contains(id) || models.containsKey(id);
        }
        return b instanceof Subscript && ((Subscript) b).value instanceof Name
                && root.contains(((Name) ((Subscript) b).value).id);
    }

    private static boolean lower(ClassDef c, String path, Set<String> base, Set<String> root, Set<String> computedDeco,
            Set<String> fieldFn, Set<String> privateFn, Map<String, Model> models) {
        if (c.bases.isEmpty()) {
            return false;
        }
        Expr b0 = c.bases.get(0);
        Model parent = null;
        boolean isRoot = false;
        if (b0 instanceof Subscript && ((Subscript) b0).value instanceof Name
                && root.contains(((Name) ((Subscript) b0).value).id)) {
            // class Pets(RootModel[List[str]]) -> class Pets(RootModel): root: List[str]
            Subscript sb = (Subscript) b0;
            c.bases.set(0, sb.value);
            AnnAssign ann = new AnnAssign(new Name("root"), sb.index, null);
            DataclassInit.atLine(ann, c.line);
            c.body.add(0, ann);
            isRoot = true;
        } else if (b0 instanceof Name && root.contains(((Name) b0).id)) {
            isRoot = true;
        } else if (b0 instanceof Name && models.containsKey(((Name) b0).id)) {
            parent = models.get(((Name) b0).id);
        } else if (!(b0 instanceof Name && base.contains(((Name) b0).id))) {
            for (Expr b : c.bases) {
                if (modelBase(b, base, root, models)) {
                    throw error(path, c, "pydantic model class " + c.name
                            + " must name its model base first; its hooks cannot be generated");
                }
            }
            return false;
        }
        for (int i = 1; i < c.bases.size(); i++) {
            if (!(c.bases.get(i) instanceof Name) || modelBase(c.bases.get(i), base, root, models)) {
                throw error(path, c, "pydantic model class " + c.name + " has more than one base;"
                        + " its per-class hooks cannot be generated");
            }
        }
        if (!c.keywords.isEmpty()) {
            throw error(path, c, "pydantic model class " + c.name + " takes class keywords; write them in"
                    + " model_config = ConfigDict(...) so the build can read them");
        }
        Model m = new Model();
        if (parent != null) {
            m.fields.addAll(parent.fields);
            m.privates.addAll(parent.privates);
            m.computed.addAll(parent.computed);
            m.props = parent.props;
            m.guarded = parent.guarded;
            m.frozen = parent.frozen;
        }
        if (isRoot) {
            m.fields.add("root");
        }
        List<String> own = new ArrayList<String>();
        List<String> ownPrivates = new ArrayList<String>();
        Set<String> fieldProps = new HashSet<String>();
        boolean ownConfig = false;
        for (Stmt s : c.body) {
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
                AnnAssign a = (AnnAssign) s;
                String n = ((Name) a.target).id;
                Expr annBase = a.annotation instanceof Subscript ? ((Subscript) a.annotation).value : a.annotation;
                String ann = annBase instanceof Name || annBase instanceof Node.Attribute ? Layouts.text(annBase) : "";
                if ("ClassVar".equals(ann) || "typing.ClassVar".equals(ann)
                        || isConst(a.annotation, Node.Constant.Kind.STR)
                        && ((Node.Constant) a.annotation).text.startsWith("ClassVar")) {
                    continue;
                }
                if ("model_config".equals(n)) {
                    ownConfig = true;
                    config(c, path, a.value, m);
                    continue;
                }
                if (n.startsWith("__")) {
                    throw error(path, s, "pydantic model " + c.name + " field " + n
                            + " is name-mangled; its hooks cannot name it");
                }
                if (n.startsWith("_")) {
                    if (a.value != null && !m.privates.contains(n)) {
                        m.privates.add(n);
                        ownPrivates.add(n);
                    }
                    continue;
                }
                if (!m.fields.contains(n)) {
                    m.fields.add(n);
                }
                own.add(n);
                if (a.value instanceof Call && ((Call) a.value).func instanceof Name
                        && fieldFn.contains(((Name) ((Call) a.value).func).id)) {
                    for (Keyword k : ((Call) a.value).keywords) {
                        if ("frozen".equals(k.arg) && isConst(k.value, Node.Constant.Kind.TRUE)) {
                            fieldProps.add(n);
                        }
                    }
                }
            } else if (s instanceof Assign && ((Assign) s).targets.size() == 1
                    && ((Assign) s).targets.get(0) instanceof Name) {
                Assign a = (Assign) s;
                String n = ((Name) a.targets.get(0)).id;
                if ("model_config".equals(n)) {
                    ownConfig = true;
                    config(c, path, a.value, m);
                } else if (a.value instanceof Call && ((Call) a.value).func instanceof Name
                        && privateFn.contains(((Name) ((Call) a.value).func).id)) {
                    if (n.startsWith("__")) {
                        throw error(path, s, "pydantic model " + c.name + " private attribute " + n
                                + " is name-mangled; its hooks cannot name it");
                    }
                    if (!m.privates.contains(n)) {
                        m.privates.add(n);
                        ownPrivates.add(n);
                    }
                }
            } else if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                for (Expr d : f.decorators) {
                    Expr fn = d instanceof Call ? ((Call) d).func : d;
                    if (fn instanceof Name && computedDeco.contains(((Name) fn).id) && !m.computed.contains(f.name)) {
                        m.computed.add(f.name);
                    }
                }
            }
        }
        if (!ownConfig && parent == null) {
            m.frozen = false;
        }
        if (!fieldProps.isEmpty()) {
            m.guarded = true;
        }
        if (m.guarded && parent != null && !parent.guarded && !parent.fields.isEmpty()) {
            throw error(path, c, "pydantic model " + c.name + " turns on frozen/validate_assignment over the"
                    + " fields of " + Layouts.text(b0) + "; declare it on the base");
        }
        // Every field of a non-root model is a property, so a plain store
        // adds the name to model_fields_set as pydantic's __setattr__ does.
        m.props = !isRoot && !(parent != null && !parent.props && !parent.fields.isEmpty());
        if (isRoot && m.guarded) {
            throw error(path, c, "pydantic root model " + c.name + " cannot be frozen or validate_assignment here");
        }
        models.put(c.name, m);
        if (ownConfig && parent != null) {
            // The metaclass merged the bases' config into the class's own;
            // the class body does it now: `{**Base.model_config, **(...)}`.
            for (Stmt s : c.body) {
                Expr v = s instanceof Assign && ((Assign) s).targets.get(0) instanceof Name
                        && "model_config".equals(((Name) ((Assign) s).targets.get(0)).id) ? ((Assign) s).value : null;
                if (v != null) {
                    throw error(path, c, "pydantic model " + c.name + " sets model_config over a model base;"
                            + " not supported yet");
                }
            }
        }
        // The annotations, evaluated when the table is asked for: the
        // generated dict's values are the class's own annotation nodes.
        StringBuilder ann = new StringBuilder("@classmethod\ndef __cn1_pd_ann__(cls):\n    return {");
        for (Stmt s : c.body) {
            if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name
                    && own.contains(((Name) ((AnnAssign) s).target).id)) {
                ann.append('"').append(((Name) ((AnnAssign) s).target).id).append("\": None, ");
                m.annNodes.add(((AnnAssign) s).annotation);
            }
        }
        if (isRoot && !own.contains("root")) {
            throw error(path, c, "pydantic root model " + c.name + " has no root annotation the build can see");
        }
        ann.append("}\n__cn1_pd_privates__ = (");
        for (String p : ownPrivates) {
            ann.append('"').append(p).append("\", ");
        }
        ann.append(")\n");
        emit(c, path, m, own, isRoot, ann.toString());
        return true;
    }

    private static void config(ClassDef c, String path, Expr v, Model m) {
        List<Keyword> kws = new ArrayList<Keyword>();
        if (v instanceof Call && ((Call) v).func instanceof Name && "ConfigDict".equals(((Name) ((Call) v).func).id)
                && ((Call) v).args.isEmpty()) {
            kws = ((Call) v).keywords;
        } else if (v instanceof DictExpr) {
            DictExpr d = (DictExpr) v;
            for (int i = 0; i < d.keys.size(); i++) {
                Expr k = d.keys.get(i);
                if (!isConst(k, Node.Constant.Kind.STR)) {
                    throw error(path, c, "pydantic model " + c.name + " has a model_config the build cannot read");
                }
                kws.add(new Keyword(((Node.Constant) k).text, d.values.get(i)));
            }
        } else {
            throw error(path, c, "pydantic model " + c.name + " has a model_config the build cannot read;"
                    + " write it as ConfigDict(...) with keywords");
        }
        for (Keyword k : kws) {
            if (k.arg == null) {
                throw error(path, c, "pydantic model " + c.name + " has a model_config the build cannot read");
            }
            if ("frozen".equals(k.arg) || "validate_assignment".equals(k.arg)) {
                if (isConst(k.value, Node.Constant.Kind.TRUE)) {
                    m.guarded = true;
                } else if (!isConst(k.value, Node.Constant.Kind.FALSE)) {
                    throw error(path, c, "pydantic model " + c.name + " sets " + k.arg + " to something"
                            + " other than a literal True or False");
                }
            }
        }
    }

    private static String slot(Model m, String f) {
        return m.props ? "self._cn1pd_" + f : "self." + f;
    }

    private static void emit(ClassDef c, String path, Model m, List<String> own, boolean isRoot, String tables) {
        StringBuilder src = new StringBuilder(tables);
        if (!isRoot) {
            src.append("def __cn1_pd_load__(self):\n    return {");
            for (String f : m.fields) {
                src.append('"').append(f).append("\": ").append(slot(m, f)).append(", ");
            }
            src.append("}\n");
            src.append("def __cn1_pd_store__(self, values):\n    pass\n");
            for (String f : m.fields) {
                src.append("    if \"").append(f).append("\" in values:\n        ").append(slot(m, f))
                        .append(" = values[\"").append(f).append("\"]\n");
            }
            src.append("@classmethod\ndef __cn1_pd_from_attrs__(cls, v):\n    d = {}\n");
            for (String f : m.fields) {
                src.append("    if hasattr(v, \"").append(f).append("\"):\n        d[\"").append(f)
                        .append("\"] = v.").append(f).append('\n');
            }
            src.append("    return d\n");
        }
        if (!m.privates.isEmpty()) {
            src.append("def __cn1_pd_init_private__(self):\n");
            src.append("def __cn1_pd_copy_private__(self, src, deep):\n");
            StringBuilder init = new StringBuilder();
            StringBuilder copy = new StringBuilder();
            for (String p : m.privates) {
                init.append("    v = self._cn1_pd_private(\"").append(p).append("\")\n    if self._cn1_pd_has(v):\n        self.")
                        .append(p).append(" = v\n");
                copy.append("    if hasattr(src, \"").append(p).append("\"):\n        self.").append(p)
                        .append(" = self._cn1_pd_copy(src.").append(p).append(", deep)\n");
            }
            int at = src.indexOf("def __cn1_pd_copy_private__");
            src.insert(at, init.toString());
            src.append(copy);
        }
        if (!m.computed.isEmpty()) {
            src.append("def __cn1_pd_computed__(self):\n    return {");
            for (String f : m.computed) {
                src.append('"').append(f).append("\": self.").append(f).append(", ");
            }
            src.append("}\n");
        }
        if (m.props) {
            for (String f : own) {
                src.append("@property\ndef ").append(f).append("(self):\n    return self._cn1pd_").append(f).append('\n');
                src.append('@').append(f).append(".setter\ndef ").append(f).append("(self, value):\n");
                if (m.guarded) {
                    // The field's plan entry by its position: model_fields is
                    // built in m.fields order (__cn1_pd_names__ holds it), so
                    // nothing is looked up by name.
                    src.append("    self._cn1pd_").append(f).append(" = self._cn1_pd_assign(")
                            .append(m.fields.indexOf(f)).append(", \"").append(f).append("\", value)\n");
                } else {
                    src.append("    self._cn1pd_").append(f).append(" = value\n    self.__pydantic_fields_set__.add(\"")
                            .append(f).append("\")\n");
                }
            }
            src.append("__cn1_pd_names__ = (");
            for (String f : m.fields) {
                src.append('"').append(f).append("\", ");
            }
            src.append(")\n");
            if (m.guarded && !hasDef(c, "__hash__")) {
                src.append("def __hash__(self):\n    return self._cn1_pd_hash()\n");
            }
            // The defaults the properties displace: the metaclass reads a
            // property's field default from `_cn1pd_default_<name>`.
            for (int i = 0; i < c.body.size(); i++) {
                Stmt s = c.body.get(i);
                if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name
                        && own.contains(((Name) ((AnnAssign) s).target).id) && ((AnnAssign) s).value != null) {
                    AnnAssign a = (AnnAssign) s;
                    String n = ((Name) a.target).id;
                    AnnAssign bare = new AnnAssign(a.target, a.annotation, null);
                    Assign dflt = new Assign(new ArrayList<Expr>(Collections.singletonList(new Name("_cn1pd_default_" + n))),
                            a.value);
                    DataclassInit.atLine(bare, s.line);
                    DataclassInit.atLine(dflt.targets, s.line);
                    dflt.line = s.line;
                    c.body.set(i, bare);
                    c.body.add(i + 1, dflt);
                    i++;
                }
            }
        }
        if (src.length() == 0) {
            return;
        }
        int line = c.body.isEmpty() ? c.line : c.body.get(c.body.size() - 1).line;
        for (Stmt s : Parser.parse(src.toString(), path).body) {
            DataclassInit.atLine(s, line);
            if (s instanceof FunctionDef && "__cn1_pd_ann__".equals(((FunctionDef) s).name)) {
                Node.DictExpr d = (Node.DictExpr) ((Node.Return) ((FunctionDef) s).body.get(0)).value;
                for (int i = 0; i < m.annNodes.size(); i++) {
                    d.values.set(i, m.annNodes.get(i));
                }
            }
            c.body.add(s);
        }
    }

    private static boolean isConst(Expr e, Node.Constant.Kind k) {
        return e instanceof Node.Constant && ((Node.Constant) e).kind == k;
    }

    private static boolean hasDef(ClassDef c, String name) {
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef && name.equals(((FunctionDef) s).name)) {
                return true;
            }
        }
        return false;
    }

    private static PythonSyntaxError error(String path, Node at, String why) {
        return new PythonSyntaxError(path, at.line, 1, why);
    }
}
