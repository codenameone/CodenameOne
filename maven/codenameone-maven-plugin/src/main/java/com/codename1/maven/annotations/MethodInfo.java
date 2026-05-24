/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.annotations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.objectweb.asm.Opcodes;

/// Lightweight description of a method discovered during the class-scanning
/// pass. Mirrors ASM's `MethodVisitor` signature without retaining the visitor
/// itself.
///
/// `descriptor` is the JVM signature (e.g., `(Ljava/lang/String;)V`).
/// `annotations` are keyed by their JVM descriptor.
public final class MethodInfo {

    private final String name;
    private final String descriptor;
    private final int access;
    private final Map<String, AnnotationValues> annotations;

    MethodInfo(String name, String descriptor, int access, Map<String, AnnotationValues> annotations) {
        this.name = name;
        this.descriptor = descriptor;
        this.access = access;
        this.annotations = (annotations == null)
                ? Collections.<String, AnnotationValues>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, AnnotationValues>(annotations));
    }

    public String getName() { return name; }
    public String getDescriptor() { return descriptor; }
    public int getAccess() { return access; }

    public boolean isPublic() { return (access & Opcodes.ACC_PUBLIC) != 0; }
    public boolean isStatic() { return (access & Opcodes.ACC_STATIC) != 0; }
    public boolean isAbstract() { return (access & Opcodes.ACC_ABSTRACT) != 0; }
    public boolean isSynthetic() { return (access & Opcodes.ACC_SYNTHETIC) != 0; }

    /// Returns true when this is a `<init>` method (constructor).
    public boolean isConstructor() { return "<init>".equals(name); }

    /// All annotations on this method, keyed by JVM descriptor (e.g.
    /// `Lcom/codename1/annotations/Async$Schedule;`).
    public Map<String, AnnotationValues> getAnnotations() { return annotations; }

    /// Convenience: look up an annotation by descriptor, returning null if absent.
    public AnnotationValues getAnnotation(String descriptor) { return annotations.get(descriptor); }

    @Override
    public String toString() {
        return name + descriptor;
    }
}
