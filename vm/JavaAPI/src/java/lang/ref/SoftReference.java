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
 * A reference the collector keeps while the referent is being used and memory
 * allows, and clears before the process runs out.
 *
 * <p>This is the reference a CACHE wants, and the difference from
 * {@link WeakReference} is not a detail: a weak referent is dropped by the first
 * collection after the last ordinary reference to it, which for a decoded bitmap
 * behind an {@code EncodedImage} means the cache is emptied faster than it can be
 * filled and never hits. A soft reference survives that collection, and the one
 * after it, for as long as something is still asking for it.</p>
 *
 * <p><b>Retention is ranked by use.</b> The collector ages every reference by one
 * on each cycle and resets the age to zero when {@code get()} is called, then
 * keeps a soft referent while that age is within a budget it recomputes each
 * cycle from the memory still available to the process. So a cache under memory
 * pressure loses its cold entries first rather than all of them at once, and an
 * entry read on every frame is the last thing to go.</p>
 *
 * <p>The whole per-read cost of that ranking is a store of a constant into the
 * reference, which is why it is ranked by age rather than by reading a clock.
 * The decision itself is made once per cycle, when the collector first reaches
 * the reference, so ranking costs the mark nothing beyond the walk it was
 * already doing -- deciding it afterwards would mean computing a second
 * reachability closure over the retained set, on a mark that already spends most
 * of its time in the grace pass.</p>
 */
public class SoftReference extends java.lang.ref.Reference{
    /**
     * Creates a new soft reference that refers to the given object.
     */
    public SoftReference(java.lang.Object ref){
        super(ref, STRENGTH_SOFT);
    }
}
