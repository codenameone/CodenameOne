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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

/// Lightweight description of a field discovered during the class-scanning
/// pass.
public final class FieldInfo {

    private final String name;
    private final String descriptor;
    private final String signature;
    private final int access;
    private final Map<String, AnnotationValues> annotations;

    FieldInfo(String name, String descriptor, String signature, int access,
              Map<String, AnnotationValues> annotations) {
        this.name = name;
        this.descriptor = descriptor;
        this.signature = signature;
        this.access = access;
        this.annotations = (annotations == null)
                ? Collections.<String, AnnotationValues>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, AnnotationValues>(annotations));
    }

    public String getName() { return name; }
    public String getDescriptor() { return descriptor; }

    /// The JVM generic-type signature (e.g.
    /// `Lcom/codename1/properties/Property<Ljava/lang/String;Lfoo/User;>;`)
    /// when one is recorded in the class file. Null for fields whose static
    /// type carries no parameterization. Processors that need to inspect
    /// type arguments (`Property<T>`, `ListProperty<T>`, `List<T>`) parse this
    /// string; for declarative use the descriptor still wins.
    public String getSignature() { return signature; }

    public int getAccess() { return access; }

    public boolean isPublic() { return (access & Opcodes.ACC_PUBLIC) != 0; }
    public boolean isStatic() { return (access & Opcodes.ACC_STATIC) != 0; }
    public boolean isFinal() { return (access & Opcodes.ACC_FINAL) != 0; }

    public Map<String, AnnotationValues> getAnnotations() { return annotations; }
    public AnnotationValues getAnnotation(String descriptor) { return annotations.get(descriptor); }
}
