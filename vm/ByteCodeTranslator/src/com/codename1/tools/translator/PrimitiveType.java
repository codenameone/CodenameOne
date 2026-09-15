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
package com.codename1.tools.translator;

/**
 * The nine primitive types, as a token the translator can compare and hash.
 *
 * <p>This used to be {@code java.lang.Class}, holding {@code Integer.TYPE} and its
 * eight siblings. Nothing ever reflected on those objects: every use was an
 * identity comparison against one of the nine constants, or a lookup in a
 * {@code HashMap<Class,String>} keyed on them. {@code Class} was standing in for
 * an enum, and it carried two problems that an enum does not.
 *
 * <p>The first is that {@code X.TYPE} does not exist on a ParparVM target. javac
 * lowers the primitive class literal in {@code Integer.TYPE = int.class} to a read
 * of the field being initialized, so the wrapper's own {@code <clinit>} stores
 * null into it; three of the nine wrappers do not declare the field at all. Keyed
 * on those, both maps collapsed to a single entry and {@code getCType} answered the
 * same C type for every primitive -- valid C, every type wrong, nothing thrown.
 * That made the maps unusable in a self-hosted translator, which is what forced
 * this change.
 *
 * <p>The second is ordering. {@code Class} has no {@code hashCode} of its own, so
 * {@code ByteCodeMethodArg.hashCode} was returning an identity hash, which varies
 * between runs of one JVM. Anything that iterated a hash container of those keys
 * and wrote the result would emit a different file each time.
 *
 * <p>Note for the same reason that {@link #ordinal()} is used explicitly wherever a
 * hash is needed rather than calling {@code hashCode()} on a constant here:
 * {@code Enum.hashCode} is an identity hash on OpenJDK and the ordinal in
 * ParparVM's {@code java.lang.Enum}, so relying on it would make the JVM-hosted and
 * self-hosted translators disagree on hash order -- a difference the self-hosting
 * gate would report as a VM divergence.
 */
public enum PrimitiveType {
    INT("JAVA_INT", "int", "I"),
    LONG("JAVA_LONG", "long", "J"),
    SHORT("JAVA_SHORT", "short", "S"),
    BYTE("JAVA_BYTE", "byte", "B"),
    DOUBLE("JAVA_DOUBLE", "double", "D"),
    FLOAT("JAVA_FLOAT", "float", "F"),
    BOOLEAN("JAVA_BOOLEAN", "boolean", "Z"),
    CHAR("JAVA_CHAR", "char", "C"),
    VOID("JAVA_VOID", "void", "V");

    private final String cType;
    private final String sigType;
    private final String descriptor;

    private PrimitiveType(String cType, String sigType, String descriptor) {
        this.cType = cType;
        this.sigType = sigType;
        this.descriptor = descriptor;
    }

    /** The C type the generated code uses for this primitive, e.g. JAVA_INT. */
    public String getCType() {
        return cType;
    }

    /** The Java keyword, as it appears in a mangled C method name. */
    public String getSigType() {
        return sigType;
    }

    /** The JVM field descriptor character, e.g. I for int. */
    public String getDescriptor() {
        return descriptor;
    }
}
