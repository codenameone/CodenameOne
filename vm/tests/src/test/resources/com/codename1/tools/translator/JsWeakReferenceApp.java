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
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * java.lang.ref.WeakReference under the translated runtime.
 *
 * ParparVM ships its own java.lang.ref in vm/JavaAPI, and its WeakReference
 * constructor used to assign the field to itself and drop the argument, so
 * every reference was born empty and get() was hardwired to null. Nothing
 * catches that at build time: it compiles, it links, and it turns every cache
 * built on CodenameOneImplementation.createSoftWeakRef -- the EncodedImage
 * decode cache above all -- into a cache that can never hit.
 *
 * The collector has no weak roots, so a reference here holds its referent until
 * it is cleared by hand. That is the pessimistic half of the contract and is
 * what these assertions pin down; what they exist to catch is the other half
 * going missing again.
 */
public class JsWeakReferenceApp {
    static int result;

    public static void main(String[] args) throws Exception {
        int mask = 0;

        // The referent the constructor was given is the referent get() answers.
        Object referent = new StringBuilder("cn1").toString();
        WeakReference ref = new WeakReference(referent);
        if (ref.get() == referent) {
            mask |= 1;
        }

        // Identity, not equality: a reference must hand back the very object it
        // was handed, which is what every cache built on it relies on.
        if (ref.get() != null && "cn1".equals(ref.get())) {
            mask |= 2;
        }

        // clear() is the only thing that empties a reference on this VM.
        ref.clear();
        if (ref.get() == null) {
            mask |= 4;
        }

        // Two references over one referent are independent.
        Object shared = new Object();
        WeakReference first = new WeakReference(shared);
        WeakReference second = new WeakReference(shared);
        first.clear();
        if (first.get() == null && second.get() == shared) {
            mask |= 8;
        }

        // A null referent is legal and reads back as null rather than throwing.
        WeakReference empty = new WeakReference(null);
        if (empty.get() == null) {
            mask |= 16;
        }

        // Through the abstract supertype, which is how the framework's
        // extractHardRef reaches it.
        Object viaBase = new Object();
        Reference base = new WeakReference(viaBase);
        if (base.get() == viaBase) {
            mask |= 32;
        }
        base.clear();
        if (base.get() == null) {
            mask |= 64;
        }

        // Distinct referents do not alias each other -- the self-assigning
        // constructor made every reference read the same (null) field, so a
        // test that only ever used one referent could not tell them apart.
        Object a = new StringBuilder("a").toString();
        Object b = new StringBuilder("b").toString();
        WeakReference refA = new WeakReference(a);
        WeakReference refB = new WeakReference(b);
        if (refA.get() == a && refB.get() == b && refA.get() != refB.get()) {
            mask |= 128;
        }

        // A reference survives being stored and read back out of a field.
        holder = new WeakReference(a);
        if (holder.get() == a) {
            mask |= 256;
        }

        result = mask;
        System.exit(mask);
    }

    static WeakReference holder;
}
