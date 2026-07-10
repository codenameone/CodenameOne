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

/**
 *
 * @author Shai Almog
 */
public class LineNumber extends Instruction {
    private String sourceFile;
    private int line;
    // True when every instruction belonging to this source line is provably
    // non-throwing and non-calling, so this line can never be the one reported in
    // a stack trace -- its line-info store is then emitted as the elidable variant
    // (no-op in release, full under the debugger). Set by
    // BytecodeMethod.analyzeElidableLineInfo(). Defaults false (keep the store).
    private boolean elidable;

    public LineNumber(String sourceFile, int line) {
        super(-1);
        this.sourceFile = sourceFile;
        this.line = line;
    }

    public int getLine() {
        return line;
    }

    public void setElidable(boolean elidable) {
        this.elidable = elidable;
    }

    @Override
    public void appendInstruction(StringBuilder b) {
        // Frameless methods don't bump callStackOffset, so a per-line
        // __CN1_DEBUG_INFO store (which writes callStackLine[callStackOffset - 1])
        // would clobber the caller's call-stack slot. Suppress it entirely.
        if(getMethod() != null && getMethod().isFrameless()) {
            return;
        }
        if(hasInstructions && (getMethod() == null || !getMethod().isDisableDebugInfo())) {
            b.append(elidable ? "    __CN1_DEBUG_INFO_NT(" : "    __CN1_DEBUG_INFO(");
            b.append(line);
            b.append(");\n");
        }
    }
}
