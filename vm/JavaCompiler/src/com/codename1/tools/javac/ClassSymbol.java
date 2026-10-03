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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class, interface, enum, record or annotation type. Library classes are
 * completed lazily from their class file the first time their structure is
 * needed; source classes are completed by the compiler's enter phases.
 */
final class ClassSymbol extends Symbol {
    /** JVM internal name, e.g. {@code java/util/Map$Entry}. */
    final String internalName;
    String simpleName;
    ClassSymbol outer;
    /** A local or anonymous class, or a member class that is not static: has an outer instance. */
    boolean hasOuterInstance;
    boolean local;
    boolean anonymous;

    private final List<Type.TypeVar> typeParams = new ArrayList<Type.TypeVar>();
    private Type superclass;
    private final List<Type> interfaces = new ArrayList<Type>();
    final List<VarSymbol> fields = new ArrayList<VarSymbol>();
    final List<MethodSymbol> methods = new ArrayList<MethodSymbol>();
    final Map<String, ClassSymbol> memberClasses = new LinkedHashMap<String, ClassSymbol>();
    final List<ClassSymbol> permitted = new ArrayList<ClassSymbol>();
    /** Record components in declaration order. */
    final List<VarSymbol> recordComponents = new ArrayList<VarSymbol>();
    Type.ClassType thisType;

    /** Source declaration, null for a library class. */
    Tree.ClassDecl decl;
    Tree.CompilationUnit unit;
    /** Library bytes not yet read. */
    byte[] pendingBytes;
    Symtab symtab;
    boolean completing;

    /** For a local or anonymous class: locals it captures, in field order. */
    final List<VarSymbol> capturedVars = new ArrayList<VarSymbol>();
    /** For a local or anonymous class: the method it is declared in. */
    MethodSymbol enclosingMethod;
    /** Counter for synthetic names (lambda$, anonymous classes) allocated in this class. */
    int lambdaCount;
    int anonCount;
    int localCount;
    /** For a local or anonymous class: the scope it was declared in (sees the enclosing locals). */
    Env declEnv;
    boolean attributed;
    /** Lambda bodies to generate as synthetic methods of this class. */
    final List<Tree.Lambda> lambdas = new ArrayList<Tree.Lambda>();
    /** Synthetic methods (lambda bodies, accessors) added during attribution. */

    ClassSymbol(String internalName, int flags) {
        super(simpleNameOf(internalName), flags);
        this.internalName = internalName;
        this.simpleName = simpleNameOf(internalName);
    }

    private static String simpleNameOf(String internalName) {
        int slash = internalName.lastIndexOf('/');
        String tail = internalName.substring(slash + 1);
        int dollar = tail.lastIndexOf('$');
        return dollar < 0 ? tail : tail.substring(dollar + 1);
    }

    void complete() {
        if (pendingBytes != null && !completing) {
            completing = true;
            byte[] bytes = pendingBytes;
            pendingBytes = null;
            symtab.completeFromClassFile(this, bytes);
            completing = false;
        }
    }

    boolean isInterface() {
        complete();
        return (flags & ACC_INTERFACE) != 0;
    }

    boolean isEnum() {
        complete();
        return (flags & ACC_ENUM) != 0;
    }

    boolean isRecord() {
        complete();
        return (flags & RECORD) != 0;
    }

    boolean isAnnotation() {
        complete();
        return (flags & ACC_ANNOTATION) != 0;
    }

    List<Type.TypeVar> typeParams() {
        complete();
        return typeParams;
    }

    Type superclass() {
        complete();
        return superclass;
    }

    void setSuperclass(Type t) {
        superclass = t;
    }

    List<Type> interfaces() {
        complete();
        return interfaces;
    }

    List<VarSymbol> fields() {
        complete();
        return fields;
    }

    List<MethodSymbol> methods() {
        complete();
        return methods;
    }

    Map<String, ClassSymbol> memberClasses() {
        complete();
        return memberClasses;
    }

    Type.ClassType thisType() {
        complete();
        if (thisType == null) {
            List<Type> args = new ArrayList<Type>(typeParams);
            Type.ClassType outerType = hasOuterInstance && outer != null ? outer.thisType() : null;
            thisType = new Type.ClassType(this, args, outerType);
        }
        return thisType;
    }

    /** Erased type of this class. */
    Type.ClassType erasure() {
        return new Type.ClassType(this, new ArrayList<Type>(), null);
    }

    String javaName() {
        if (anonymous) {
            return "<anonymous " + internalName.replace('/', '.') + ">";
        }
        if (outer != null && !local) {
            return outer.javaName() + "." + simpleName;
        }
        if (local) {
            return simpleName;
        }
        return internalName.replace('/', '.');
    }

    @Override
    public String toString() {
        return javaName();
    }
}
