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
package com.codename1.tools.translator.classfile;

/**
 * Receives an annotation's element values. A primitive array element arrives as one
 * {@link #visit} call carrying the Java array; an array of strings, classes, enums
 * or annotations arrives through {@link #visitArray}, whose visitor is then called
 * with a null name once per element.
 */
public abstract class AnnotationVisitor {
    protected AnnotationVisitor av;

    protected AnnotationVisitor() {
    }

    protected AnnotationVisitor(AnnotationVisitor av) {
        this.av = av;
    }

    public void visit(String name, Object value) {
        if (av != null) {
            av.visit(name, value);
        }
    }

    public void visitEnum(String name, String desc, String value) {
        if (av != null) {
            av.visitEnum(name, desc, value);
        }
    }

    public AnnotationVisitor visitAnnotation(String name, String desc) {
        return av != null ? av.visitAnnotation(name, desc) : null;
    }

    public AnnotationVisitor visitArray(String name) {
        return av != null ? av.visitArray(name) : null;
    }

    public void visitEnd() {
        if (av != null) {
            av.visitEnd();
        }
    }
}
