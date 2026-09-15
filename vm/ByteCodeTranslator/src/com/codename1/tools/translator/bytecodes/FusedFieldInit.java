/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.objectweb.asm.Opcodes;

/**
 * KEEP-IF-NULL fused-field initialization inside a {@code @Fused} class's
 * constructor. Replaces the matched {@code ALOAD 0; <len>; NEWARRAY T;
 * PUTFIELD f} quadruple (see {@link FusedConstructor}): when the allocation
 * site pre-installed the fused child (field already non-null) the store is
 * skipped; every OTHER instantiation path -- reflection, __NEW_INSTANCE, the
 * oversize/BiBOP-unavailable fallback -- arrives with the field null and
 * allocates an ordinary array exactly like the original bytecode. This single
 * conditional is what keeps constructors chainable and every allocation path
 * semantically identical, fused or not.
 *
 * Emitted as a self-contained C statement with net-zero stack effect, immune
 * to the later expression-fusion passes (which never see the raw quadruple).
 * The length expression reads the constructor's C parameter ({@code __cn1ArgN}
 * -- stable for the whole constructor regardless of frame flavor) or an int
 * constant; the analyzer already rejected constructors that reassign the
 * parameter's slot before this point.
 */
public class FusedFieldInit extends Instruction {
    private final FusedConstructor.Child child;

    public FusedFieldInit(FusedConstructor.Child child) {
        super(Opcodes.NOP);
        this.child = child;
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        String owner = child.getCOwner();
        String field = owner + "_" + child.getFieldName();
        String lhs = "((struct obj__" + owner + "*)__cn1ThisObject)->" + field;
        b.append("    if(").append(lhs).append(" == JAVA_NULL) { /* fused field: not pre-installed */\n");
        // Through a TEMPORARY, so the barrier can see the value before the field does.
        //
        // This path publishes an INDEPENDENT array into a field of an already-existing
        // object, and a raw C assignment takes neither half of the store barrier that the
        // ordinary PUTFIELD path emits. That is not cosmetic. CN1_WRITE_BARRIER is the
        // SATB insertion half, so without it an array allocated and installed during a
        // concurrent mark is recorded nowhere and can be swept while the field still
        // points at it -- the same hazard cloneArray and System.arraycopy each carry an
        // explicit bulk barrier for. Under a generational build it is also the promotion
        // hook: the array is freshly allocated and therefore young, the owner is not, and
        // a verifier over the self-hosting corpus reported exactly this shape 1552 times
        // (BiBOP owner -> young Object[]/Label[]/Frame[]) before this line existed.
        //
        // FusedConstructor's own children need no barrier and get none: those arrays are
        // carved out of the OWNER'S block by cn1FusedInstallPrimArray and have no
        // independent GC identity. This one calls allocArray, so it does.
        b.append("        JAVA_OBJECT __cn1ffi = allocArray(threadStateData, ")
         .append(child.ctorLengthExpr()).append(", ").append(child.arrayClassRef())
         .append(", sizeof(").append(child.elemCType()).append("), 1);\n");
        b.append("        CN1_WRITE_BARRIER(__cn1ThisObject, __cn1ffi);\n");
        b.append("        ").append(lhs).append(" = __cn1ffi;\n");
        b.append("    }\n");
    }
}
