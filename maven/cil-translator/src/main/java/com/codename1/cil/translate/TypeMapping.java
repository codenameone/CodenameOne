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
package com.codename1.cil.translate;

import com.codename1.cil.metadata.CilAssembly;
import com.codename1.cil.metadata.CilType;
import com.codename1.cil.metadata.Universe;
import java.util.HashSet;
import java.util.List;

/// How the translator names and represents a .NET type, for tools that
/// generate code to sit beside its output and must agree with it.
public final class TypeMapping {
    private final Names names;

    /// `translated` are the assemblies whose classes the translator emits;
    /// every other type is taken to live in the hand-written runtime.
    public TypeMapping(Universe universe, List<CilAssembly> translated) {
        names = new Names(universe, new HashSet<CilAssembly>(translated));
    }

    /// The class a type becomes, as Java source spells it.
    public String javaClass(CilType type) {
        return names.classRef(type).replace('/', '.').replace('$', '.');
    }

    /// The type with an enum reduced to the primitive it is stored as.
    public CilType storage(CilType type) {
        return names.norm(type);
    }

    public boolean isStruct(CilType type) {
        return names.isStruct(type);
    }
}
