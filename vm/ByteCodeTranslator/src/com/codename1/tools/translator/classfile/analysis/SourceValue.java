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
package com.codename1.tools.translator.classfile.analysis;

import com.codename1.tools.translator.classfile.tree.AbstractInsnNode;
import java.util.Collections;
import java.util.Set;

/** A value tracked by the instructions that may have produced it. */
public class SourceValue implements Value {
    public final int size;
    /** The producing instructions. Never modified once the value exists. */
    public final Set<AbstractInsnNode> insns;

    public SourceValue(int size) {
        this(size, Collections.<AbstractInsnNode>emptySet());
    }

    public SourceValue(int size, AbstractInsnNode insn) {
        this(size, Collections.singleton(insn));
    }

    public SourceValue(int size, Set<AbstractInsnNode> insns) {
        this.size = size;
        this.insns = insns;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SourceValue)) {
            return false;
        }
        SourceValue v = (SourceValue) o;
        return size == v.size && insns.equals(v.insns);
    }

    @Override
    public int hashCode() {
        return insns.hashCode();
    }
}
