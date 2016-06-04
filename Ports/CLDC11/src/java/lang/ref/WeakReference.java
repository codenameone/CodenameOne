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
package java.lang.ref;
/**
 * This class provides support for weak references. Weak references are most often used to implement canonicalizing mappings. Suppose that the garbage collector determines at a certain point in time that an object is weakly reachable. At that time it will atomically clear all the weak references to that object and all weak references to any other weakly- reachable objects from which that object is reachable through a chain of strong and weak references.
 * Since: JDK1.2, CLDC 1.1
 */
public class WeakReference extends java.lang.ref.Reference{
    /**
     * Creates a new weak reference that refers to the given object.
     */
    public WeakReference(java.lang.Object ref){
         //TODO codavaj!!
    }

}
