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

    /// The body of one method, by brace counting from its signature.
    private static String methodBody(String source, String signature) {
        int at = source.indexOf(signature);
        assertTrue(at >= 0, "could not find " + signature + " in " + SOURCE);
        int open = source.indexOf('{', at);
        assertTrue(open >= 0, "malformed " + signature);
        int depth = 0;
        for (int iter = open; iter < source.length(); iter++) {
            char c = source.charAt(iter);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, iter + 1);
                }
            }
        }
        throw new AssertionError("unterminated " + signature);
    }

    @Test
    void theMigrationRereadsBeforeItOverwrites() throws Exception {
        // A browser runs many tabs against one store, and seal() is where migrate() pauses. A tab
        // that read the plaintext and then waited there would write it over whatever arrived
        // meanwhile -- set() puts a new value in the encrypted entry and removes the plaintext, so
        // a token another tab had just refreshed was silently replaced by the stale one this
        // migration set out with.
        //
        // Structural rather than behavioural, for the reason this whole class exists: the
        // JavaScript port's classes are not on this module's classpath. What it asserts is the
        // ORDER, which is the whole of the property -- a re-read after the write proves nothing.
        assertTrue(Files.exists(SOURCE), "HTML5SecureStorage.java not found at " + SOURCE);
        String source = new String(Files.readAllBytes(SOURCE), StandardCharsets.UTF_8);
        String body = methodBody(source, "private void migrate(String account, String value)");

        // The establishing write is the atomic create, not a plain writeObject: a check followed
        // by a blind write let another tab's set() be overwritten by the stale legacy value.
        int write = body.indexOf("nativeSetIfAbsent(encryptedKey(account)");
        assertTrue(write > 0, "migrate no longer establishes the encrypted entry: " + body);
        String beforeTheWrite = body.substring(0, write);
        assertTrue(beforeTheWrite.indexOf("readUncached(legacyKey(account))") > 0,
                "migrate must re-read the plaintext it is migrating before it overwrites the "
                + "encrypted entry, and past Storage's cache -- readObject answers a copy this "
                + "tab took earlier, which is exactly the stale value being guarded against");
        // definitelyGone rather than exists: a port that cannot tell answers false to exists(),
        // and false here means "no encrypted entry", which is the claim that must not be guessed.
        assertTrue(beforeTheWrite.indexOf("definitelyGone(encryptedKey(account))") > 0,
                "migrate must check that no encrypted entry has appeared before writing one; "
                + "get() only reaches migrate when there was none, so one now is newer");

        // And the plaintext is only removed while it is still the value that was migrated.
        int delete = body.indexOf("deleteStorageFile(legacyKey(account))");
        assertTrue(delete > 0, "migrate no longer removes the plaintext: " + body);
        assertTrue(body.lastIndexOf("readUncached(legacyKey(account))", delete) > write,
                "the plaintext must be re-read between the encrypted write and its deletion, and "
                + "past the cache, or a value that arrived in between is deleted rather than "
                + "kept");
    }

    @Test
    void accountNamesAreEscapedBeforeTheyBecomeStorageKeys() {
        // Storage.fixFileName rewrites '/' and six other characters to '_' when normalizeNames is
        // on, which is the default -- so `api/token` and `api_token` addressed ONE entry. The
        // second set() overwrote the first account's ciphertext, and because the AAD still names
        // the original account every later read of the first failed authentication.
        //
        // Structural, like the ordering check below, because this module cannot load the port's
        // classes. What it holds is that neither key builder concatenates the account raw.
        String source = readSource();
        assertTrue(methodBody(source, "private static String encryptedKey(String account)")
                        .indexOf("escaped(account)") > 0,
                "the encrypted key must escape the account before it becomes a storage key");
        // legacyKey deliberately does NOT. It addresses what a PREVIOUS version already wrote,
        // which used the raw name, and escaping it made every existing credential invisible on
        // the first launch after an upgrade. Asserted so the asymmetry cannot be "tidied up".
        assertTrue(methodBody(source, "private static String legacyKey(String account)")
                        .indexOf("escaped(account)") < 0,
                "the legacy key must stay raw, or entries written by the previous version of "
                + "this port can no longer be found");
        // And the escape has to be one fixFileName leaves alone -- '_' plus hex, never the
        // characters it rewrites.
        String escape = methodBody(source, "private static String escaped(String account)");
        assertTrue(escape.indexOf("b.append('_')") > 0, escape);
    }

    @Test
    void theEscapeCheckWouldHaveCaughtTheConcatenationItWasWrittenFor() {
        // Guards that guard: the shipped draft returned PREFIX + account with nothing between.
        String original = "{ return LEGACY_PREFIX + account; }";
        assertFalse(original.indexOf("escaped(account)") > 0,
                "the original really did concatenate the account raw");
    }

    private static String readSource() {
        try {
            return new String(Files.readAllBytes(SOURCE), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theAtomicCreateNativeIsBoundUnderTheNameTheTranslatorWillEmit() {
        // A native whose binding name is wrong does not fail: it compiles, it links, and the
        // feature is simply inert on the device -- which for THIS native means setIfAbsent
        // answering null and two tabs going back to overwriting each other's keys. So the name is
        // derived from the translator rather than read back from the file that declares it.
        // methodIdentifier already carries the cn1_ prefix; adding another is what the first
        // version of this test did, and it reported the binding missing when it was correct.
        String portJs = read(Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main",
                "webapp", "port.js").toAbsolutePath().normalize());
        String source = readSource();
        String[][] natives = {
            {"nativeSetIfAbsent", "(Ljava/lang/String;Ljava/lang/String;)[B"},
            {"nativeForget", "(Ljava/lang/String;)[B"},
        };
        for (String[] one : natives) {
            String expected = JavascriptNameUtil.methodIdentifier(
                    "com/codename1/impl/html5/HTML5SecureStorage", one[0], one[1]);
            assertTrue(portJs.indexOf(expected) > 0,
                    "port.js must bind the name the translator emits for " + one[0]
                    + ", which is:\n  " + expected);
            assertTrue(source.indexOf("static native byte[] " + one[0] + "(") > 0,
                    "HTML5SecureStorage must declare " + one[0] + ", or the binding is for "
                    + "nothing");
        }
        assertTrue(source.indexOf("public String setIfAbsent(") > 0,
                "and override setIfAbsent to use it, or the inherited check-then-write stands");

        // The gate is a gate, not the value store: a created entry has to be visible to the
        // ordinary reads as well, or forgetManagedKey cannot see a key that exists and remove()
        // reports success over a record that survives.
        String create = methodBody(source, "public String setIfAbsent(String account, String value)");
        // The mirror lives in its own method now, because it has to re-read the gate and copy
        // what the gate actually holds -- a single blind write let the two namespaces disagree.
        // So the property is checked in two halves: the create delegates, and the delegate is
        // what writes the ordinary entry.
        assertTrue(create.indexOf("mirrorUntilItAgreesWithTheGate(") > 0,
                "the settled value must be mirrored into the namespace every read uses: " + create);
        String mirror = methodBody(source,
                "private String mirrorUntilItAgreesWithTheGate(String account, String settled)");
        assertTrue(mirror.indexOf("writeObject(encryptedKey(account)") > 0,
                "and the mirror must actually write the ordinary entry: " + mirror);
        assertTrue(mirror.indexOf("nativeRead(") > 0,
                "and re-read the gate, or it can return a value it never wrote: " + mirror);
        // nativeForgetIf, not nativeForget: the release is a compare-and-delete now, because a
        // set() in another tab can replace the record first and deleting THAT discards a
        // ciphertext whose own call is about to report success. The property this holds is
        // unchanged -- remove() must release the gate -- so it accepts either spelling rather
        // than pinning the weaker one.
        String removeBody = methodBody(source, "public boolean remove(String account)");
        assertTrue(removeBody.indexOf("nativeForgetIf(") > 0
                        || removeBody.indexOf("nativeForget(") > 0,
                "and remove() must release the gate, or a later create answers with what it "
                + "forgot: " + removeBody);
    }

    private static String read(Path path) {
        assertTrue(Files.exists(path), "not found: " + path);
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theOrderingCheckWouldHaveCaughtTheMigrationItWasWrittenFor() {
        // Guards that guard too. The shipped draft sealed and then wrote with nothing in between,
        // so the text before the write held no re-read at all.
        String original = "{ String sealed = seal(account, value);"
                + " if (!Storage.getInstance().writeObject(encryptedKey(account), sealed)) {"
                + " return; } }";
        int write = original.indexOf("writeObject(encryptedKey(account)");
        assertTrue(write > 0);
        assertFalse(original.substring(0, write).indexOf("readObject(legacyKey(account))") > 0,
                "the original really did write without re-reading, so the check has something "
                + "to catch");
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
