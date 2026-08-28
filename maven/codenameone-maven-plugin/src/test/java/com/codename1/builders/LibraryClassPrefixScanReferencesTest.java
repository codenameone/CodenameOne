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
