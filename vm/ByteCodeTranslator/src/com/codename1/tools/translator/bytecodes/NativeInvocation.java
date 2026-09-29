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
package com.codename1.tools.translator.bytecodes;

import java.util.List;

/** Emits calls without materializing a second copy of their generated C. */
final class NativeInvocation {
    private NativeInvocation() { }

    static void append(StringBuilder out, String function, String receiver,
                       List<String> qualifiers, String[] literals, int stackOffset,
                       boolean checkedReceiver) {
        if (checkedReceiver) {
            out.append("({ JAVA_OBJECT __cn1DirectReceiver = ").append(receiver).append("; ");
            int offset = stackOffset;
            for (int i = 0; i < qualifiers.size(); i++) {
                String q = qualifiers.get(i);
                String type = "o".equals(q) ? "JAVA_OBJECT" : "l".equals(q) ? "JAVA_LONG"
                        : "d".equals(q) ? "JAVA_DOUBLE" : "f".equals(q) ? "JAVA_FLOAT" : "JAVA_INT";
                out.append(type).append(" __cn1DirectArg_").append(i).append(" = ");
                offset = appendArgument(out, q, literals, i, offset);
                out.append("; ");
            }
            // Java evaluates every argument before throwing for a null receiver.
            out.append("if (__cn1DirectReceiver == JAVA_NULL) { cn1ThrowNullPointerOrDie(threadStateData); } ");
        }
        out.append(function).append("(threadStateData");
        if (receiver != null) out.append(", ").append(checkedReceiver ? "__cn1DirectReceiver" : receiver);
        int offset = stackOffset;
        for (int i = 0; i < qualifiers.size(); i++) {
            out.append(", ");
            if (checkedReceiver) out.append("__cn1DirectArg_").append(i);
            else offset = appendArgument(out, qualifiers.get(i), literals, i, offset);
        }
        out.append(")");
        if (checkedReceiver) out.append("; })");
    }

    /**
     * A virtual call whose receiver can only be one of a few concrete classes, emitted
     * as a compare on the class id and a DIRECT call per case, with the ordinary
     * virtual thunk as the last resort.
     *
     * The point is not the compare, it is what the direct call enables. An indirect
     * call through the vtable is five dependent loads -- class pointer, class id,
     * interface map row, slot, target -- and the C compiler cannot see through it, so
     * nothing downstream of the call is optimised. Replacing it with a compare and a
     * named callee lets ThinLTO inline the callee and optimise across it, which is the
     * same transformation profile-guided optimisation performs when it observes a hot
     * receiver type. In a closed world the set of possible receivers is KNOWN, so no
     * observation is needed.
     *
     * The fallback arm is not dead code to be tidied away. The cone is computed from
     * the classes the dead-code pass kept, and a receiver reaching this site from a
     * path the analysis could not see must still dispatch correctly rather than fall
     * off the end of the chain.
     *
     * @param out destination
     * @param guards class-id token and target function for each concrete receiver
     * @param fallback the virtual thunk, used when no guard matches
     * @param receiver receiver expression
     * @param qualifiers argument stack qualifiers
     * @param literals literal arguments, or null
     * @param stackOffset first stack slot of the arguments
     * @param isVoid whether the call yields no value
     */
    static void appendGuarded(StringBuilder out, List<String[]> guards, String fallback,
                              String receiver, List<String> qualifiers, String[] literals,
                              int stackOffset, boolean isVoid) {
        out.append("({ JAVA_OBJECT __cn1DirectReceiver = ").append(receiver).append("; ");
        int offset = stackOffset;
        for (int i = 0; i < qualifiers.size(); i++) {
            String q = qualifiers.get(i);
            String type = "o".equals(q) ? "JAVA_OBJECT" : "l".equals(q) ? "JAVA_LONG"
                    : "d".equals(q) ? "JAVA_DOUBLE" : "f".equals(q) ? "JAVA_FLOAT" : "JAVA_INT";
            out.append(type).append(" __cn1DirectArg_").append(i).append(" = ");
            offset = appendArgument(out, q, literals, i, offset);
            out.append("; ");
        }
        // Java evaluates every argument before throwing for a null receiver.
        out.append("if (__cn1DirectReceiver == JAVA_NULL) { cn1ThrowNullPointerOrDie(threadStateData); } ");
        out.append("JAVA_INT __cn1Cid = GET_CLASS_ID(__cn1DirectReceiver); ");
        if (isVoid) {
            // A void call cannot be a ternary chain, because the arms have no value.
            for (int g = 0; g < guards.size(); g++) {
                out.append(g == 0 ? "if (" : "else if (").append("__cn1Cid == ").append(guards.get(g)[0]).append(") ");
                appendArgs(out, guards.get(g)[1], qualifiers);
                out.append("; ");
            }
            out.append("else ");
            appendArgs(out, fallback, qualifiers);
            out.append("; })");
            return;
        }
        for (String[] g : guards) {
            out.append("__cn1Cid == ").append(g[0]).append(" ? ");
            appendArgs(out, g[1], qualifiers);
            out.append(" : ");
        }
        appendArgs(out, fallback, qualifiers);
        out.append("; })");
    }

    private static void appendArgs(StringBuilder out, String function, List<String> qualifiers) {
        out.append(function).append("(threadStateData, __cn1DirectReceiver");
        for (int i = 0; i < qualifiers.size(); i++) {
            out.append(", __cn1DirectArg_").append(i);
        }
        out.append(")");
    }

    private static int appendArgument(StringBuilder out, String qualifier, String[] literals, int index, int offset) {
        if (literals != null && literals[index] != null) out.append(literals[index]);
        else { out.append("SP[-").append(offset--).append("].data.").append(qualifier); }
        return offset;
    }
}
