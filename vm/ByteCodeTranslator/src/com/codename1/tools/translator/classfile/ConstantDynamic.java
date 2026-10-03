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

/** A CONSTANT_Dynamic: a constant computed by a bootstrap method. */
public final class ConstantDynamic {
    private final String name;
    private final String descriptor;
    private final Handle bootstrapMethod;
    private final Object[] bootstrapMethodArguments;

    public ConstantDynamic(String name, String descriptor, Handle bootstrapMethod, Object[] bootstrapMethodArguments) {
        this.name = name;
        this.descriptor = descriptor;
        this.bootstrapMethod = bootstrapMethod;
        this.bootstrapMethodArguments = bootstrapMethodArguments;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public Handle getBootstrapMethod() {
        return bootstrapMethod;
    }

    public int getBootstrapMethodArgumentCount() {
        return bootstrapMethodArguments.length;
    }

    public Object getBootstrapMethodArgument(int index) {
        return bootstrapMethodArguments[index];
    }

    /** Slots the constant occupies: 2 for a long or double, otherwise 1. */
    public int getSize() {
        char c = descriptor.charAt(0);
        return c == 'J' || c == 'D' ? 2 : 1;
    }

    @Override
    public String toString() {
        return name + " : " + descriptor + ' ' + bootstrapMethod;
    }
}
