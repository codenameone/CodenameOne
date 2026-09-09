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
 *
 * <p>The referent lives in {@link Reference}, and the collector clears it once
 * the object is reachable no other way. Two properties of THIS collector make
 * that best-effort rather than prompt, and both are legal -- the contract says a
 * reference "may" be cleared, never that it must be:</p>
 *
 * <ul>
 * <li>A newly allocated object is kept unconditionally for one cycle by the
 *     sweep's grace rule, so a referent is never cleared in the cycle it dies.</li>
 * <li>The root scan reads native C stacks conservatively, so a stale machine
 *     word that happens to look like the referent keeps it marked. {@code get()}
 *     will occasionally keep answering an object nothing references any more.</li>
 * </ul>
 *
 * <p>Historical note worth keeping, because both halves were real shipped bugs.
 * This class first held its referent in an ordinary field, which the translator
 * traced like any other -- so a "weak" reference was strong and the caches built
 * on {@code CodenameOneImplementation.createSoftWeakRef} pinned every decoded
 * bitmap for the life of the process. Before that, the constructor assigned the
 * field to itself ({@code this.objReference = objReference}) and dropped the
 * argument, so every reference was born empty and {@code get()} was hardwired to
 * null: the same caches could then never hit. Note the failure modes are exact
 * opposites, which is why this class needs tests that pin BOTH ends -- that the
 * referent is answered while it is reachable, and that it stops being answered
 * once it is not.</p>
 */
public class WeakReference extends java.lang.ref.Reference{
    /**
     * Creates a new weak reference that refers to the given object.
     */
    public WeakReference(java.lang.Object ref){
         super(ref, STRENGTH_WEAK);
    }
}
