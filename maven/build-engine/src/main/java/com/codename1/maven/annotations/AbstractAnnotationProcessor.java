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
