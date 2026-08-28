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
package com.codename1.builders;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// What a submitted cn1lib has to do before the builder calls it API usage.
///
/// The scan used to be a raw byte search, so a package name that merely
/// appeared in a STRING constant -- a registry listing packages, a log
/// message -- was reported as usage. On iOS that is not cosmetic:
/// com/codename1/call/directory/ turns on the Call Directory extension, and a
/// signed build then aborts unless a separate extension provisioning profile
/// is supplied, so a library that only named the package broke every app that
/// included it.
class LibraryClassPrefixScanReferencesTest {

    private static final String USED = "com/codename1/call/directory/Blocked";

    /// A minimal class file whose pool holds one entry of the given kind.
    ///
    /// Hand-assembled rather than compiled, because the whole question is
    /// which POOL ENTRY the name sits in, and that is not something source
    /// code lets you choose.
    private static byte[] classFile(int tag) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(0xCAFEBABE);
        d.writeShort(0);
        d.writeShort(52);
        d.writeShort(3);          // pool count: entries 1 and 2
        d.writeByte(1);           // #1 Utf8
        d.writeUTF(USED);
        d.writeByte(tag);         // #2 Class or String, both pointing at #1
        d.writeShort(1);
        d.writeShort(0);          // access
        d.writeShort(0);          // this_class
        d.writeShort(0);          // super_class
        d.writeShort(0);          // interfaces
        d.writeShort(0);          // fields
        d.writeShort(0);          // methods
        d.writeShort(0);          // attributes
        d.flush();
        return out.toByteArray();
    }

    @Test
    void aClassReferenceCounts() throws Exception {
        // CONSTANT_Class, which is what referring to a type produces.
        Set<String> refs = LibraryClassPrefixScan.classReferences(classFile(7));
        assertTrue(refs.contains(USED), "a class reference is usage");
    }

    @Test
    void aStringConstantDoesNot() throws Exception {
        // CONSTANT_String: the same bytes, in the entry that means "a literal
        // this code carries" rather than "a type this code refers to".
        Set<String> refs = LibraryClassPrefixScan.classReferences(classFile(8));
        assertFalse(refs.contains(USED),
                "naming a package in a string constant is not using it");
    }

    /// A class file whose only mention of USED is a METHOD DESCRIPTOR.
    ///
    /// The pool holds it as a plain Utf8 with nothing pointing at it, which
    /// is what an interface method returning that type -- or an abstract or
    /// native declaration -- actually produces: the descriptor is referenced
    /// by method_info, not by any CONSTANT_Class, NameAndType or MethodType.
    private static byte[] descriptorOnly(boolean asField) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(0xCAFEBABE);
        d.writeShort(0);
        d.writeShort(52);
        d.writeShort(3);
        d.writeByte(1);
        d.writeUTF(asField ? "L" + USED + ";" : "()L" + USED + ";");
        d.writeByte(1);
        d.writeUTF("get");
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);          // interfaces
        d.writeShort(asField ? 1 : 0);
        if (asField) {
            d.writeShort(0);      // access
            d.writeShort(2);      // name
            d.writeShort(1);      // descriptor
            d.writeShort(1);      // one attribute, to prove they are skipped
            d.writeShort(2);
            d.writeInt(3);
            d.write(new byte[]{9, 9, 9});
        }
        d.writeShort(asField ? 0 : 1);
        if (!asField) {
            d.writeShort(0);
            d.writeShort(2);
            d.writeShort(1);
            d.writeShort(0);
        }
        d.writeShort(0);          // class attributes
        d.flush();
        return out.toByteArray();
    }

    @Test
    void aMethodDescriptorCounts() throws Exception {
        // A library that names a type only in a signature it never calls was
        // invisible, so the app built against it got no permissions, no
        // services and no defines for a package it plainly uses.
        Set<String> refs =
                LibraryClassPrefixScan.classReferences(descriptorOnly(false));
        assertTrue(refs.contains("()L" + USED + ";"),
                "a method descriptor is a reference: " + refs);
    }

    @Test
    void aFieldDescriptorCountsAndAttributesAreSkipped() throws Exception {
        // The field table comes first and carries attributes; misreading one
        // shifts everything after it, so this fixture gives the field an
        // attribute with a body.
        Set<String> refs =
                LibraryClassPrefixScan.classReferences(descriptorOnly(true));
        assertTrue(refs.contains("L" + USED + ";"),
                "a field descriptor is a reference too: " + refs);
    }

    @Test
    void aGenericSignatureCounts() throws Exception {
        // javac ERASES a generic member -- List<...Call> becomes
        // ()Ljava/util/List; -- and puts the real type in the member's
        // Signature attribute. Collecting the descriptor and skipping every
        // attribute therefore left a library whose only mention of a package
        // is a generic declaration exactly as invisible as the plain
        // descriptor case was.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(0xCAFEBABE);
        d.writeShort(0);
        d.writeShort(52);
        d.writeShort(5);
        d.writeByte(1);
        d.writeUTF("()Ljava/util/List;");      // #1 erased descriptor
        d.writeByte(1);
        d.writeUTF("get");                     // #2 name
        d.writeByte(1);
        d.writeUTF("Signature");               // #3 attribute name
        d.writeByte(1);
        d.writeUTF("()Ljava/util/List<L" + USED + ";>;"); // #4 the real type
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);                       // interfaces
        d.writeShort(0);                       // fields
        d.writeShort(1);                       // one method
        d.writeShort(0);
        d.writeShort(2);
        d.writeShort(1);
        d.writeShort(1);                       // one attribute
        d.writeShort(3);
        d.writeInt(2);
        d.writeShort(4);
        d.writeShort(0);                       // class attributes
        d.flush();
        Set<String> refs =
                LibraryClassPrefixScan.classReferences(out.toByteArray());
        boolean sawIt = false;
        for (String r : refs) {
            if (r.indexOf(USED) >= 0) {
                sawIt = true;
            }
        }
        assertTrue(sawIt, "the generic signature is a reference: " + refs);
    }

    @Test
    void aClassLevelGenericSignatureCounts() throws Exception {
        // "class Calls extends ArrayList<...Call>" puts ArrayList in
        // super_class and the argument in the CLASS's Signature attribute,
        // so a walk that stopped after the member tables never saw it.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(out);
        d.writeInt(0xCAFEBABE);
        d.writeShort(0);
        d.writeShort(52);
        d.writeShort(3);
        d.writeByte(1);
        d.writeUTF("Signature");
        d.writeByte(1);
        d.writeUTF("Ljava/util/ArrayList<L" + USED + ";>;");
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);
        d.writeShort(0);          // interfaces
        d.writeShort(0);          // fields
        d.writeShort(0);          // methods
        d.writeShort(1);          // one class attribute
        d.writeShort(1);          // named by #1
        d.writeInt(2);
        d.writeShort(2);          // pointing at #2
        d.flush();
        Set<String> refs =
                LibraryClassPrefixScan.classReferences(out.toByteArray());
        boolean sawIt = false;
        for (String r : refs) {
            if (r.indexOf(USED) >= 0) {
                sawIt = true;
            }
        }
        assertTrue(sawIt, "a generic supertype is a reference: " + refs);
    }

    @Test
    void somethingThatIsNotAClassFileIsNotParsed() {
        // Null, not empty: the caller falls back to the raw scan rather than
        // reading "no references" as "does not use anything", which would
        // silently disable a feature the app really uses.
        assertNull(LibraryClassPrefixScan.classReferences(
                "com/codename1/call/directory/ in a text file".getBytes()));
        assertNull(LibraryClassPrefixScan.classReferences(new byte[]{1, 2, 3}));
    }
}
