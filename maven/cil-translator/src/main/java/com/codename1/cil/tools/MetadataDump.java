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
package com.codename1.cil.tools;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.MethodBody;
import com.codename1.cil.metadata.Universe;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/// Prints what the metadata reader sees in an assembly, one line per type,
/// field and method.
///
/// The format is shared with `scripts/unity-compat-samples/metadata-dump`,
/// which prints the same lines from .NET's own `System.Reflection.Metadata`.
/// Comparing the two outputs is how the reader is checked against an
/// independent implementation, so change both together.
public final class MetadataDump {
    private MetadataDump() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: MetadataDump <assembly> [reference assembly ...]");
            System.exit(2);
        }
        List<File> files = new ArrayList<File>();
        boolean il = false;
        for (String arg : args) {
            if ("--il".equals(arg)) {
                il = true;
            } else {
                files.add(new File(arg));
            }
        }
        Universe universe = new Universe();
        CilAssembly assembly = universe.load(files).get(0);
        // Flushed, not closed: closing it would close System.out.
        PrintStream out = new PrintStream(System.out, false, "UTF-8"); // NOPMD CloseResource
        if (il) {
            dumpIl(assembly, out);
        } else {
            dump(assembly, out);
        }
        out.flush();
    }

    /// Lists every method body with its tokens resolved. A reading aid for
    /// whoever works on the translator; nothing compares this output.
    static void dumpIl(CilAssembly assembly, PrintStream out) {
        for (CilAssembly.TypeDef t : assembly.types()) {
            for (CilAssembly.MethodDef m : t.methods) {
                out.println(m);
                if (!m.hasBody()) {
                    continue;
                }
                MethodBody body = m.body();
                for (int i = 0; i < body.locals.length; i++) {
                    out.println("    local " + i + " : " + body.locals[i]);
                }
                for (MethodBody.Clause c : body.clauses) {
                    out.println("    clause flags=" + c.flags + " try=" + Integer.toHexString(c.tryStart) + ".."
                            + Integer.toHexString(c.tryEnd) + " handler=" + Integer.toHexString(c.handlerStart)
                            + ".." + Integer.toHexString(c.handlerEnd)
                            + (c.flags == MethodBody.Clause.CATCH ? " " + assembly.typeFromToken(c.classToken) : ""));
                }
                for (MethodBody.Instr in : body.instructions) {
                    out.println("  " + in + operand(assembly, in));
                }
            }
        }
        out.flush();
    }

    private static String operand(CilAssembly assembly, MethodBody.Instr in) {
        String op = in.op;
        if ("call".equals(op) || "callvirt".equals(op) || "newobj".equals(op) || "ldftn".equals(op)
                || "ldvirtftn".equals(op)) {
            CilAssembly.MethodRef ref = assembly.methodRef(in.operand);
            String inst = "";
            if (ref.methodArgs != null) {
                inst = " <" + java.util.Arrays.toString(ref.methodArgs) + ">";
            }
            return " " + ref + inst;
        }
        if ("ldfld".equals(op) || "ldflda".equals(op) || "stfld".equals(op) || "ldsfld".equals(op)
                || "ldsflda".equals(op) || "stsfld".equals(op)) {
            CilAssembly.FieldRef ref = assembly.fieldRef(in.operand);
            return " " + ref + " : " + ref.type;
        }
        if ("ldstr".equals(op)) {
            return " \"" + assembly.userString(in.operand) + "\"";
        }
        if ("ldtoken".equals(op)) {
            return assembly.isFieldToken(in.operand) ? " field " + assembly.fieldRef(in.operand) : " token";
        }
        if ("newarr".equals(op) || "box".equals(op) || "unbox".equals(op) || "unbox.any".equals(op)
                || "castclass".equals(op) || "isinst".equals(op) || "initobj".equals(op) || "constrained.".equals(op)
                || "ldobj".equals(op) || "stobj".equals(op) || "cpobj".equals(op) || "ldelem".equals(op)
                || "stelem".equals(op) || "ldelema".equals(op) || "sizeof".equals(op)) {
            return " " + assembly.typeFromToken(in.operand);
        }
        if ("ldc.i8".equals(op)) {
            return " " + in.longOperand;
        }
        if ("ldc.r4".equals(op) || "ldc.r8".equals(op)) {
            return " " + in.doubleOperand;
        }
        if ("switch".equals(op)) {
            StringBuilder sb = new StringBuilder(" [");
            for (int target : in.targets) {
                sb.append(" IL_").append(Integer.toHexString(target));
            }
            return sb.append(" ]").toString();
        }
        if (op.startsWith("b") && !"box".equals(op) && !"break".equals(op) || "leave".equals(op)) {
            return " IL_" + Integer.toHexString(in.operand);
        }
        if (op.startsWith("ld") || op.startsWith("st")) {
            if ("ldarg".equals(op) || "ldarga".equals(op) || "starg".equals(op) || "ldloc".equals(op)
                    || "ldloca".equals(op) || "stloc".equals(op) || "ldc.i4".equals(op)) {
                return " " + in.operand;
            }
        }
        return "";
    }

    static void dump(CilAssembly assembly, PrintStream out) {
        for (CilAssembly.TypeDef t : assembly.types()) {
            out.println("T " + t.fullName() + " flags=" + Integer.toHexString(t.flags) + " base="
                    + (t.baseType == null ? "-" : plain(t.baseType)) + " generic=" + t.genericParamCount);
            for (CilType i : t.interfaces) {
                out.println("  I " + plain(i));
            }
            for (CilAssembly.FieldDef f : t.fields) {
                out.println("  F " + f.name + " flags=" + Integer.toHexString(f.flags) + " : " + f.type);
            }
            for (CilAssembly.MethodDef m : t.methods) {
                String code = " code=-1 locals=0 clauses=0";
                if (m.hasBody()) {
                    MethodBody body = m.body();
                    int size = 0;
                    if (!body.instructions.isEmpty()) {
                        size = body.instructions.get(body.instructions.size() - 1).next;
                    }
                    code = " code=" + size + " locals=" + body.locals.length + " clauses=" + body.clauses.size();
                }
                out.println("  M " + m.name + " flags=" + Integer.toHexString(m.flags) + " " + m.sig + code);
            }
        }
        out.flush();
    }

    // A base type or an implemented interface is named by a bare token, which
    // carries no class-or-value-type marker; print the name alone so that the
    // line does not depend on resolving it.
    private static String plain(CilType type) {
        String text = type.toString();
        return text.startsWith("valuetype ") ? text.substring("valuetype ".length()) : text;
    }
}
