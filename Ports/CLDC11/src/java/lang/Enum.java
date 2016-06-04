/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package java.lang;

/**
 * Implementation class required to compile enums
 *
 * @author Shai Almog
 */
public class Enum<E extends Enum<E>> implements Comparable<E> {

    protected Enum(final String name, final int ordinal) {
    }

    protected Object clone() {
        return null;
    }

    protected static final void setEnumValues(final Object[] values, final Class c) {
    }

    protected static final <T> T[] getEnumValues(final Class<T> class_) {
        return null;
    }

    public static <T extends Enum<T>> T valueOf(final Class<T> enumType, final String name) {
        throw new IllegalArgumentException("No enum const " + enumType + "."
                + name);
    }

    public final boolean equals(final Object other) {
        return other == this;
    }

    public final int hashCode() {
        return 0;
    }

    public String toString() {
        return "";
    }

    public final int compareTo(final E e) {
        return 0;
    }

    public final String name() {
        return null;
    }

    public final int ordinal() {
        return 0;
    }

    public final Class<E> getDeclaringClass() {
        return null;
    }
}
