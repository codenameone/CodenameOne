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
package com.codename1.hardening;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

/** The upper-bound byte estimator must never undercount an instruction's widest legal encoding. */
public class MethodSizeTest {

    @Test
    public void conditionalJumpChargesItsWidenedEightBytes() {
        // A conditional IF* has no wide form, so ASM widens an out-of-range one to an inverted 3-byte
        // conditional over a 5-byte GOTO_W == 8 bytes. Charging fewer would let a method dense with
        // long-range conditionals pass the preflight and then throw MethodTooLargeException at write time.
        LabelNode l = new LabelNode();
        InsnList insns = new InsnList();
        insns.add(new JumpInsnNode(Opcodes.IFEQ, l));
        insns.add(l);
        assertEquals(8, MethodSize.estimateBytes(insns));
    }

    @Test
    public void gotoAndJsrChargeTheirFiveByteWideForm() {
        // GOTO/JSR DO have a 5-byte wide form (GOTO_W/JSR_W), so 5 is their widest encoding.
        LabelNode g = new LabelNode();
        InsnList gotoList = new InsnList();
        gotoList.add(new JumpInsnNode(Opcodes.GOTO, g));
        gotoList.add(g);
        assertEquals(5, MethodSize.estimateBytes(gotoList));

        LabelNode j = new LabelNode();
        InsnList jsrList = new InsnList();
        jsrList.add(new JumpInsnNode(Opcodes.JSR, j));
        jsrList.add(j);
        assertEquals(5, MethodSize.estimateBytes(jsrList));
    }
}
