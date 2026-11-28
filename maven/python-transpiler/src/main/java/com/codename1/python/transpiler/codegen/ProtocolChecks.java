/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
import com.codename1.python.transpiler.ast.Node.AnnAssign;
import com.codename1.python.transpiler.ast.Node.Assign;
import com.codename1.python.transpiler.ast.Node.Attribute;
import com.codename1.python.transpiler.ast.Node.ClassDef;
import com.codename1.python.transpiler.ast.Node.Expr;
import com.codename1.python.transpiler.ast.Node.FunctionDef;
import com.codename1.python.transpiler.ast.Node.Name;
import com.codename1.python.transpiler.ast.Node.Stmt;
import com.codename1.python.transpiler.ast.Node.Subscript;
import com.codename1.python.transpiler.parser.Parser;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/// Writes the structural check of every `typing.Protocol` class at build
/// time, so `isinstance`/`issubclass` against a `@runtime_checkable`
/// protocol need no member looked up by a computed name.
///
/// A protocol is a class with `Protocol` (or `Protocol[...]`, from `typing`
/// or `typing_extensions`) among its direct bases, as CPython's
/// `Protocol.__init_subclass__` decides. Its members are the names its body
/// binds -- methods, assignments and bare annotations -- less the names
/// CPython's `_get_protocol_attrs` leaves out. The class gets a static
/// `__cn1_protocol_check__(o)`: a method member must be present and not
/// None, any other member present (CPython's `__instancecheck__`), and each
/// further base -- a protocol too -- must hold as well. A class with a
/// non-method member also gets `__cn1_protocol_data__`, their quoted names, which makes
/// `issubclass` refuse it. The runtime (PyType.protocolCheck) calls the
/// check when the nominal test fails, and refuses a protocol that is not
/// runtime checkable.
public final class ProtocolChecks {
    private static final Set<String> EXCLUDED = new HashSet<String>(Arrays.asList(
            "__abstractmethods__", "__annotations__", "__dict__", "__doc__", "__init__", "__module__", "__new__",
            "__slots__", "__subclasshook__", "__weakref__", "__class_getitem__", "__match_args__",
            "__static_attributes__", "__firstlineno__", "_is_protocol", "_is_runtime_protocol",
            "__protocol_attrs__", "__non_callable_proto_members__", "__type_params__", "__orig_bases__",
            "__parameters__", "__qualname__", "_MutableMapping__marker", "__init_subclass__"));

    private ProtocolChecks() {
    }

    /// Whether `c` names `Protocol` among its direct bases.
    static boolean isProtocol(ClassDef c) {
        for (Expr x : c.bases) {
            Expr e = x instanceof Subscript ? ((Subscript) x).value : x;
            if (e instanceof Name && "Protocol".equals(((Name) e).id)
                    || e instanceof Attribute && "Protocol".equals(((Attribute) e).attr)) {
                return true;
            }
        }
        return false;
    }

    /// Adds the checks to the protocol classes at module level of `tree`.
    public static void rewrite(Node.Module tree, String path) {
        Map<String, ClassDef> protocols = new HashMap<String, ClassDef>();
        for (Stmt s : tree.body) {
            if (s instanceof ClassDef && isProtocol((ClassDef) s)) {
                ClassDef c = (ClassDef) s;
                protocols.put(c.name, c);
                lower(c, protocols, path);
            }
        }
    }

    private static void lower(ClassDef c, Map<String, ClassDef> protocols, String path) {
        // member name -> whether it is a method
        Map<String, Boolean> members = new LinkedHashMap<String, Boolean>();
        for (Stmt s : c.body) {
            if (s instanceof FunctionDef) {
                FunctionDef f = (FunctionDef) s;
                boolean prop = false;
                for (Expr d : f.decorators) {
                    prop |= d instanceof Name && "property".equals(((Name) d).id)
                            || d instanceof Attribute && ("setter".equals(((Attribute) d).attr)
                            || "deleter".equals(((Attribute) d).attr) || "property".equals(((Attribute) d).attr));
                }
                member(members, f.name, !prop);
            } else if (s instanceof AnnAssign && ((AnnAssign) s).target instanceof Name) {
                member(members, ((Name) ((AnnAssign) s).target).id, false);
            } else if (s instanceof Assign) {
                for (Expr t : ((Assign) s).targets) {
                    if (t instanceof Name) {
                        member(members, ((Name) t).id, false);
                    }
                }
            }
        }
        StringBuilder b = new StringBuilder();
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, Boolean> m : members.entrySet()) {
            if (!m.getValue()) {
                data.append(data.length() == 0 ? "" : ", ").append('\'').append(m.getKey()).append('\'');
            }
        }
        if (data.length() > 0) {
            b.append("__cn1_protocol_data__ = \"").append(data).append("\"\n");
        }
        b.append("@staticmethod\ndef __cn1_protocol_check__(o):\n    return True");
        for (Map.Entry<String, Boolean> m : members.entrySet()) {
            if (m.getValue()) {
                b.append(" and getattr(o, '").append(m.getKey()).append("', None) is not None");
            } else {
                b.append(" and hasattr(o, '").append(m.getKey()).append("')");
            }
        }
        for (Expr x : c.bases) {
            Expr e = x instanceof Subscript ? ((Subscript) x).value : x;
            String n = e instanceof Name ? ((Name) e).id : e instanceof Attribute ? ((Attribute) e).attr : null;
            if ("Protocol".equals(n) || "Generic".equals(n) || "object".equals(n)) {
                continue;
            }
            if (e instanceof Name && protocols.containsKey(n) && protocols.get(n) != c) { //NOPMD CompareObjectsWithEquals - identity is meant
                b.append(" and ").append(n).append(".__cn1_protocol_check__(o)");
            } else if (e instanceof Name || e instanceof Attribute && ((Attribute) e).value instanceof Name) {
                String ref = e instanceof Name ? n : ((Name) ((Attribute) e).value).id + "." + n;
                b.append(" and (issubclass(o, ").append(ref).append(") if isinstance(o, type) else isinstance(o, ")
                        .append(ref).append("))");
            }
        }
        b.append('\n');
        Node.Module gen = Parser.parse(b.toString(), path);
        DataclassInit.atLine(gen, c.line);
        c.body.addAll(gen.body);
    }

    private static void member(Map<String, Boolean> members, String name, boolean method) {
        if (!EXCLUDED.contains(name) && !name.startsWith("_abc_") && !members.containsKey(name)) {
            members.put(name, method);
        }
    }
}
