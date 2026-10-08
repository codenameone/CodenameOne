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
package com.codename1.tools.javac;

/** A named declaration: class, method or variable. */
abstract class Symbol {
    static final int ACC_PUBLIC = 0x0001;
    static final int ACC_PRIVATE = 0x0002;
    static final int ACC_PROTECTED = 0x0004;
    static final int ACC_STATIC = 0x0008;
    static final int ACC_FINAL = 0x0010;
    static final int ACC_SUPER = 0x0020;
    static final int ACC_SYNCHRONIZED = 0x0020;
    static final int ACC_VOLATILE = 0x0040;
    static final int ACC_BRIDGE = 0x0040;
    static final int ACC_VARARGS = 0x0080;
    static final int ACC_TRANSIENT = 0x0080;
    static final int ACC_NATIVE = 0x0100;
    static final int ACC_INTERFACE = 0x0200;
    static final int ACC_ABSTRACT = 0x0400;
    static final int ACC_STRICT = 0x0800;
    static final int ACC_SYNTHETIC = 0x1000;
    static final int ACC_ANNOTATION = 0x2000;
    static final int ACC_ENUM = 0x4000;
    static final int ACC_MANDATED = 0x8000;
    /** Pseudo flags above the class-file range. */
    static final int RECORD = 1 << 16;
    static final int DEFAULT_METHOD = 1 << 17;
    static final int SEALED = 1 << 21;
    static final int NON_SEALED = 1 << 22;

    final String name;
    int flags;

    Symbol(String name, int flags) {
        this.name = name;
        this.flags = flags;
    }

    boolean isStatic() {
        return (flags & ACC_STATIC) != 0;
    }

    boolean isPrivate() {
        return (flags & ACC_PRIVATE) != 0;
    }

    boolean isPublic() {
        return (flags & ACC_PUBLIC) != 0;
    }

    boolean isProtected() {
        return (flags & ACC_PROTECTED) != 0;
    }

    boolean isFinal() {
        return (flags & ACC_FINAL) != 0;
    }

    boolean isAbstract() {
        return (flags & ACC_ABSTRACT) != 0;
    }
}
