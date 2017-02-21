/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package java.util;
/**
 * An object that implements the Enumeration interface generates a series of elements, one at a time. Successive calls to the nextElement method return successive elements of the series.
 * For example, to print all elements of a vector v:
 * Methods are provided to enumerate through the elements of a vector, the keys of a hashtable, and the values in a hashtable.
 * Since: JDK1.0, CLDC 1.0 Version: 12/17/01 (CLDC 1.1) See Also:nextElement(), Hashtable, Hashtable.elements(), Hashtable.keys(), Vector, Vector.elements()
 */
public interface Enumeration<T>{
    /**
     * Tests if this enumeration contains more elements.
     */
    abstract boolean hasMoreElements();

    /**
     * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
     */
    abstract T nextElement();

}
