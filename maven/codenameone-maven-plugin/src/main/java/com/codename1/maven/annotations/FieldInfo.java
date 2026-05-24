/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
    private final int access;
    private final Map<String, AnnotationValues> annotations;

    FieldInfo(String name, String descriptor, int access, Map<String, AnnotationValues> annotations) {
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
    public boolean isFinal() { return (access & Opcodes.ACC_FINAL) != 0; }

    public Map<String, AnnotationValues> getAnnotations() { return annotations; }
    public AnnotationValues getAnnotation(String descriptor) { return annotations.get(descriptor); }
}
