/*
 * Copyright (c) 2018, Codename One and/or its affiliates. All rights reserved.
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
package java.util.concurrent.atomic;

public class AtomicReference<V> {
    private Object lock = new Object();
    V ref;
    
    public AtomicReference() {
        
    }
    
    public AtomicReference(V initialValue) {
        ref = initialValue;
    }
    
    public final boolean compareAndSet(V expect, V update) {
        synchronized(lock) {
            if (expect == ref) {
                ref = update;
                return true;
            }
            return false;
        }
        
    }
    
    public V get() {
        synchronized(lock) {
            return ref;
        }
    }
    
    public final V getAndSet(V newValue) {
        synchronized(lock) {
            V old = ref;
            ref = newValue;
            return old;
        }
    }
    
    public final  void lazySet(V newValue) {
        synchronized(lock) {
            ref = newValue;
        }
    }
    
    public String toString() {
        synchronized(lock) {
            return String.valueOf(ref);
        }
    }
    
    public final boolean weakCompareAndSet(V expect, V update) {
        return compareAndSet(expect, update);
    }
    
    public final void set(V newValue) {
        synchronized(lock) {
            ref = newValue;
        }
        
    }
}
