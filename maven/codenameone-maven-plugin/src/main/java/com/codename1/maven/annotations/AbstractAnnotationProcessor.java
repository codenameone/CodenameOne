/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.maven.annotations;

/// Convenience base class so processors only override the lifecycle hooks they
/// actually need. The plugin compiles at Java 7 so we can't rely on default
/// methods in `AnnotationProcessor`.
public abstract class AbstractAnnotationProcessor implements AnnotationProcessor {

    @Override
    public void emitStubs(ProcessorContext ctx) throws ProcessingException {
        // No-op default.
    }

    @Override
    public void start(ProcessorContext ctx) throws ProcessingException {
        // No-op default.
    }

    @Override
    public void finish(ProcessorContext ctx) throws ProcessingException {
        // No-op default.
    }

    /// Helper: walks the class index following `superInternalName` links to test
    /// whether `cls` is a subtype of the given JVM internal type. Returns false
    /// once the chain leaves the project (the JDK / dependency classes aren't in
    /// the index). Use this for the typical "must extend Form" checks.
    protected static boolean isSubtypeWithinProject(AnnotatedClass cls, String superInternalName,
                                                     ProcessorContext ctx) {
        if (cls == null || superInternalName == null) return false;
        if (superInternalName.equals(cls.getInternalName())) return true;
        if (superInternalName.equals(cls.getSuperInternalName())) return true;
        AnnotatedClass parent = ctx.lookup(cls.getSuperInternalName());
        while (parent != null) {
            if (superInternalName.equals(parent.getInternalName())) return true;
            if (superInternalName.equals(parent.getSuperInternalName())) return true;
            parent = ctx.lookup(parent.getSuperInternalName());
        }
        return false;
    }
}
