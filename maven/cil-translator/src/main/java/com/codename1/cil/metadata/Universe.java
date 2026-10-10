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
package com.codename1.cil.metadata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Every assembly a translation can see: the ones being translated and the
/// reference assemblies they were compiled against.
///
/// Types are looked up by full name alone. Which assembly a reference claims
/// the type lives in is ignored, because a compile-time reference names a
/// facade or a reference assembly, never the place the code ends up at run time.
public final class Universe {
    private final List<CilAssembly> assemblies = new ArrayList<CilAssembly>();
    private final Map<String, CilAssembly.TypeDef> types = new HashMap<String, CilAssembly.TypeDef>();

    /// The debug information of an assembly is the file of the same name
    /// with `.pdb` where the assembly has `.dll` or `.exe`.
    private static File pdbBeside(File assembly) {
        String name = assembly.getName();
        int dot = name.lastIndexOf('.');
        return new File(assembly.getAbsoluteFile().getParentFile(),
                (dot > 0 ? name.substring(0, dot) : name) + ".pdb");
    }

    /// Loads a set of assemblies together. The first to define a name wins,
    /// so list the assemblies being translated before their references.
    public List<CilAssembly> load(List<File> files) throws IOException {
        List<CilAssembly> loaded = new ArrayList<CilAssembly>();
        for (File file : files) {
            CilAssembly assembly = new CilAssembly(Files.readAllBytes(file.toPath()), file.getName());
            File pdb = pdbBeside(file);
            if (pdb.isFile()) {
                assembly.debugInformation(PortablePdb.read(Files.readAllBytes(pdb.toPath())));
            }
            assembly.load(this);
            for (CilAssembly.TypeDef t : assembly.types()) {
                if (!types.containsKey(t.fullName())) {
                    types.put(t.fullName(), t);
                }
            }
            loaded.add(assembly);
        }
        for (CilAssembly assembly : loaded) {
            assembly.linkBases();
        }
        for (CilAssembly assembly : loaded) {
            assembly.link();
        }
        for (CilAssembly assembly : loaded) {
            assembly.linkOverrides();
        }
        assemblies.addAll(loaded);
        return loaded;
    }

    public List<CilAssembly> assemblies() {
        return Collections.unmodifiableList(assemblies);
    }

    public CilAssembly.TypeDef find(String fullName) {
        return types.get(fullName);
    }

    public CilAssembly.TypeDef require(String fullName, String neededBy) {
        CilAssembly.TypeDef found = types.get(fullName);
        if (found == null) {
            throw new CilFormatException(neededBy + ": type " + fullName
                    + " is not defined by any loaded assembly (is a reference assembly missing?)");
        }
        return found;
    }

    /// The definition behind a type, or null for one that has none of its
    /// own: an array, a type variable, a pointer.
    public CilAssembly.TypeDef definitionOf(CilType type) {
        String name = type.typeName();
        return name == null ? null : types.get(name);
    }

    /// Finds the method a reference names, looking up the inheritance chain
    /// when the named type only inherits it. Signatures are compared as
    /// declared, type variables included, which is how a reference spells them.
    public CilAssembly.MethodDef resolve(CilAssembly.MethodRef ref) {
        if (ref.def != null) {
            return ref.def;
        }
        CilAssembly.TypeDef type = definitionOf(ref.declaringType);
        while (type != null) {
            for (CilAssembly.MethodDef m : type.methods) {
                if (m.name.equals(ref.name) && m.sig.sameShape(ref.sig)) {
                    return m;
                }
            }
            type = type.baseType == null ? null : definitionOf(type.baseType);
        }
        return null;
    }

    public CilAssembly.FieldDef resolve(CilAssembly.FieldRef ref) {
        if (ref.def != null) {
            return ref.def;
        }
        CilAssembly.TypeDef type = definitionOf(ref.declaringType);
        while (type != null) {
            CilAssembly.FieldDef f = type.field(ref.name);
            if (f != null) {
                return f;
            }
            type = type.baseType == null ? null : definitionOf(type.baseType);
        }
        return null;
    }

    /// True when `type` is `ancestor` or derives from it, through base
    /// classes or implemented interfaces.
    public boolean derivesFrom(String type, String ancestor) {
        if (type.equals(ancestor) || "System.Object".equals(ancestor)) {
            return true;
        }
        CilAssembly.TypeDef def = types.get(type);
        if (def == null) {
            return false;
        }
        for (CilType i : def.interfaces) {
            String name = i.typeName();
            if (name != null && derivesFrom(name, ancestor)) {
                return true;
            }
        }
        String base = def.baseType == null ? null : def.baseType.typeName();
        return base != null && derivesFrom(base, ancestor);
    }
}
