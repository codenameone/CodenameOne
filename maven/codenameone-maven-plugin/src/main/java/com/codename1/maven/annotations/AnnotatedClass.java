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
package com.codename1.maven.annotations;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;

/// Immutable snapshot of a class produced by `ClassScanner`.
///
/// `internalName` is the JVM internal form (e.g. `com/example/ProfileForm`);
/// most processors only ever care about the internal name. The supplemental
/// `binaryName()` method returns the `.`-form (`com.example.ProfileForm`) when
/// embedding into generated source or bytecode invocations.
public final class AnnotatedClass {

    private final String internalName;
    private final String superInternalName;
    private final List<String> interfaceInternalNames;
    private final int access;
    private final Map<String, AnnotationValues> annotations;
    private final List<MethodInfo> methods;
    private final List<FieldInfo> fields;
    private final File classFile;

    AnnotatedClass(
            String internalName,
            String superInternalName,
            List<String> interfaceInternalNames,
            int access,
            Map<String, AnnotationValues> annotations,
            List<MethodInfo> methods,
            List<FieldInfo> fields,
            File classFile) {
        this.internalName = internalName;
        this.superInternalName = superInternalName;
        this.interfaceInternalNames = interfaceInternalNames == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(interfaceInternalNames));
        this.access = access;
        this.annotations = annotations == null
                ? Collections.<String, AnnotationValues>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, AnnotationValues>(annotations));
        this.methods = methods == null
                ? Collections.<MethodInfo>emptyList()
                : Collections.unmodifiableList(new ArrayList<MethodInfo>(methods));
        this.fields = fields == null
                ? Collections.<FieldInfo>emptyList()
                : Collections.unmodifiableList(new ArrayList<FieldInfo>(fields));
        this.classFile = classFile;
    }

    /// JVM internal name (`com/example/ProfileForm`).
    public String getInternalName() { return internalName; }

    /// Dotted binary name (`com.example.ProfileForm`).
    public String getBinaryName() { return internalName.replace('/', '.'); }

    public String getSuperInternalName() { return superInternalName; }
    public List<String> getInterfaceInternalNames() { return interfaceInternalNames; }
    public int getAccess() { return access; }

    public boolean isAbstract() { return (access & Opcodes.ACC_ABSTRACT) != 0; }
    public boolean isInterface() { return (access & Opcodes.ACC_INTERFACE) != 0; }
    public boolean isPublic() { return (access & Opcodes.ACC_PUBLIC) != 0; }
    public boolean isSynthetic() { return (access & Opcodes.ACC_SYNTHETIC) != 0; }

    /// `true` when the class file's `ACC_RECORD` flag is set (Java 16+ record).
    /// Inlined as a constant so this code keeps compiling against ASM versions
    /// that predate `Opcodes.ACC_RECORD`.
    public boolean isRecord() { return (access & ACC_RECORD) != 0; }

    private static final int ACC_RECORD = 0x10000;

    /// Class-level annotations, keyed by JVM descriptor.
    public Map<String, AnnotationValues> getClassAnnotations() { return annotations; }

    /// Looks up a class-level annotation by descriptor, returning null if absent.
    public AnnotationValues getClassAnnotation(String descriptor) { return annotations.get(descriptor); }

    public List<MethodInfo> getMethods() { return methods; }
    public List<FieldInfo> getFields() { return fields; }

    /// The `.class` file this snapshot was loaded from. Useful for log/error
    /// messages so users see the on-disk location of the offending class.
    public File getClassFile() { return classFile; }

    @Override
    public String toString() {
        return internalName;
    }
}
