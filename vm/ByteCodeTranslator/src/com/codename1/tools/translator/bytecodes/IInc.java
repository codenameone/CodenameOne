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
 *
 * @author Shai Almog
 */
public class IInc extends Instruction {
    private int var;
    private int num;
    private static char[] ZERO_CHAR = new char[0];
    public IInc(int var, int num) {
        super(Opcodes.IINC);
        this.var = var;
        this.num = num;
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        b.append("    BC_IINC(");
        b.append(var);
        b.append(", ");
        b.append(num);
        b.append(");\n");
    }

    @Override
    public char[] getStackInputTypes() {
        return ZERO_CHAR;
    }

    @Override
    public char[] getStackOutputTypes() {
        return ZERO_CHAR;
    }
    
    

    
}
