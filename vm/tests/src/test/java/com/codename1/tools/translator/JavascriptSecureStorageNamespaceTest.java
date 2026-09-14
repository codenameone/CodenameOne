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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/// The browser secure store's two namespaces cannot collide.
///
/// `HTML5SecureStorage` keeps plaintext entries under one prefix and encrypted ones under another,
/// and migrates between them. If either prefix is a prefix of the other, two different accounts
/// map to one storage key: with `cn1secure.` and `cn1secure.v2.`, the encrypted entry for account
/// `foo` and the plaintext entry for an account literally named `v2.foo` are the same key. Writing
/// one then destroys the other, a read cannot tell ciphertext from plaintext, and `remove` takes
/// out somebody else's secret.
///
/// Read from the source because that is where the constants live, and because this is a property
/// of two string literals rather than of any behaviour a unit test could drive -- the JavaScript
/// port's classes are not on this module's classpath.
class JavascriptSecureStorageNamespaceTest {

    private static final Path SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src",
            "main", "java", "com", "codename1", "impl", "html5", "HTML5SecureStorage.java")
            .toAbsolutePath().normalize();

    private static String constant(String source, String name) {
        Matcher m = Pattern.compile("String\\s+" + name + "\\s*=\\s*\"([^\"]*)\"").matcher(source);
        assertTrue(m.find(), "could not find the " + name + " constant in " + SOURCE);
        return m.group(1);
    }

    @Test
    void neitherNamespaceIsAPrefixOfTheOther() throws Exception {
        assertTrue(Files.exists(SOURCE), "HTML5SecureStorage.java not found at " + SOURCE);
        String source = new String(Files.readAllBytes(SOURCE), StandardCharsets.UTF_8);
        String legacy = constant(source, "LEGACY_PREFIX");
        String encrypted = constant(source, "ENCRYPTED_PREFIX");

        assertNotEquals(legacy, encrypted, "the two namespaces must differ");
        // The whole property. `legacy + a == encrypted + b` has a solution for some pair of
        // account names exactly when one prefix is a prefix of the other.
        assertFalse(encrypted.startsWith(legacy),
                "the encrypted namespace " + encrypted + " is inside the legacy namespace "
                + legacy + ", so an account name can address both");
        assertFalse(legacy.startsWith(encrypted),
                "the legacy namespace " + legacy + " is inside the encrypted namespace "
                + encrypted + ", so an account name can address both");
    }

    @Test
    void theCheckWouldHaveCaughtTheCollisionItWasWrittenFor() {
        // Guards the guard: the assertion above must actually reject the spelling that shipped in
        // the first draft, or it is a test that only ever agrees.
        assertTrue("cn1secure.v2.".startsWith("cn1secure."),
                "the original pair really was nested, so the check above has something to catch");
        assertFalse("cn1secure2.".startsWith("cn1secure."),
                "and the replacement really is disjoint");
    }
}
