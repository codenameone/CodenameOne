// ASM: a very small and fast Java bytecode manipulation framework
// Copyright (c) 2000-2011 INRIA, France Telecom
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
// 3. Neither the name of the copyright holders nor the names of its
//    contributors may be used to endorse or promote products derived from
//    this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.
//
// Codename One modifications Copyright (c) 2026, Codename One and/or its
// affiliates, distributed under the license above.
//
// This file is a rewrite of ASM's org.objectweb.asm.tree.InsnList for ParparVM: it keeps
// ASM's API and design, reduced to what the translator uses, so the translator
// can read class files without ASM and still be translated by itself (see
// vm/ByteCodeTranslator/src/com/codename1/tools/translator/classfile/README.md).
package com.codename1.tools.translator.classfile.tree;

/** A doubly linked list of nodes, with index lookups served from a cached array. */
public class InsnList {
    private AbstractInsnNode first;
    private AbstractInsnNode last;
    private int size;
    private AbstractInsnNode[] cache;

    public int size() {
        return size;
    }

    public AbstractInsnNode getFirst() {
        return first;
    }

    public AbstractInsnNode getLast() {
        return last;
    }

    public void add(AbstractInsnNode node) {
        if (last == null) {
            first = node;
        } else {
            last.next = node;
            node.previous = last;
        }
        last = node;
        size++;
        cache = null;
    }

    public AbstractInsnNode[] toArray() {
        AbstractInsnNode[] out = new AbstractInsnNode[size];
        int i = 0;
        for (AbstractInsnNode n = first; n != null; n = n.next) {
            n.index = i;
            out[i++] = n;
        }
        return out;
    }

    private AbstractInsnNode[] cache() {
        if (cache == null) {
            cache = toArray();
        }
        return cache;
    }

    public AbstractInsnNode get(int index) {
        return cache()[index];
    }

    /** The index of a node that belongs to this list. */
    public int indexOf(AbstractInsnNode node) {
        cache();
        return node.index;
    }
}
