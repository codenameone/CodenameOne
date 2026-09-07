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

package java.lang.ref;
/**
 * This class provides support for weak references. Weak references are most often used to implement canonicalizing mappings. Suppose that the garbage collector determines at a certain point in time that an object is weakly reachable. At that time it will atomically clear all the weak references to that object and all weak references to any other weakly- reachable objects from which that object is reachable through a chain of strong and weak references.
 * Since: JDK1.2, CLDC 1.1
 */
public class WeakReference extends java.lang.ref.Reference{
    private Object objReference;
    
    /**
     * Creates a new weak reference that refers to the given object.
     * <p>
     * Note that ParparVM's collector has no notion of a weak root: it never
     * clears this field, so the referent lives exactly as long as the
     * reference object does and {@link #get()} keeps answering it until
     * {@link Reference#clear()} is called by hand. That is a legal (if
     * pessimistic) implementation of the contract -- "may be cleared" is not
     * "must be cleared" -- and it is what the callers need. What is NOT legal
     * is the reverse: this constructor used to assign the field to itself
     * ({@code this.objReference = objReference}) and drop {@code ref} on the
     * floor, so every reference was born empty and {@code get()} was hardwired
     * to null. Everything built on
     * {@code CodenameOneImplementation.createSoftWeakRef} -- the EncodedImage
     * decode cache, Image's scale cache, Border's round-rect cache -- was then
     * a cache that could never hit.
     */
    public WeakReference(java.lang.Object ref){
         this.objReference = ref;
    }

    Object getImpl() {
        return objReference;
    }
    
    void clearImpl() {
        objReference = null;
    }
}
