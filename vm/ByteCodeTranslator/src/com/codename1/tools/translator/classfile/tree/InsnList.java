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
