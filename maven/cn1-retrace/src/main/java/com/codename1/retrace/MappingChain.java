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
package com.codename1.retrace;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies mappings in order. On Android a device frame is doubly renamed -- R8 over
 * the hardening engine's rename -- so it is inverted through the R8 mapping first
 * and the cross-platform mapping second. On every other port the chain is a single
 * mapping. Chaining at query time is the robust alternative to pre-composing the two
 * files, which is lossy where the stages' line ranges do not nest.
 */
public final class MappingChain {

    private final List<MappingFile> mappings = new ArrayList<MappingFile>();

    /** @param inOrder the mappings to apply, device-nearest first (e.g. R8 then cross-platform). */
    public MappingChain(List<MappingFile> inOrder) {
        if (inOrder != null) {
            mappings.addAll(inOrder);
        }
    }

    public MappingChain add(MappingFile m) {
        if (m != null) {
            mappings.add(m);
        }
        return this;
    }

    public Frame retrace(Frame frame) {
        Frame f = frame;
        for (MappingFile m : mappings) {
            f = m.retrace(f);
        }
        return f;
    }

    public boolean isEmpty() {
        return mappings.isEmpty();
    }
}
