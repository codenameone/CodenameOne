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
package com.codename1.impl.javase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codename1.security.SecureStorage;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The simulator runs every project on one machine under one OS user, and its secure storage kept
 * every account in one fixed Preferences node. Two projects that both asked for a managed database
 * key under the same alias therefore read each other's key, and forgetting it in either one removed
 * the only copy either had.
 */
public class JavaSESecureStorageNamespaceTest {

    private static final String SHARED_NODE = "com.codename1.simulator.secureStorage.plain";

    private String originalMainClass;

    @BeforeEach
    public void setUp() {
        originalMainClass = System.getProperty("MainClass");
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (originalMainClass == null) {
            System.clearProperty("MainClass");
        } else {
            System.setProperty("MainClass", originalMainClass);
        }
        // Both projects' nodes and anything left in the shared one, so a rerun starts clean.
        Preferences shared = Preferences.userRoot().node(SHARED_NODE);
        for (String child : shared.childrenNames()) {
            if (child.startsWith("com.example")) {
                shared.node(child).removeNode();
            }
        }
        shared.remove("v_cn1.db.key.shared");
        shared.flush();
    }

    private static JavaSESecureStorage storageFor(String mainClass) {
        System.setProperty("MainClass", mainClass);
        return new JavaSESecureStorage(new JavaSEBiometrics());
    }

    @Test
    public void oneProjectCannotSeeAnotherProjectsKey() {
        JavaSESecureStorage first = storageFor("com.example.first.Main");
        assertTrue(first.set("cn1.db.key.shared", "1111"), "the first project stores its key");

        JavaSESecureStorage second = storageFor("com.example.second.Main");
        assertEquals(SecureStorage.ENTRY_ABSENT, second.entryState("cn1.db.key.shared"),
                "the second project has no key of that name, whatever the first one called its");
        assertNull(second.get("cn1.db.key.shared"), "and cannot read the first project's");

        // Its own key under the same alias is its own, and does not disturb the first.
        assertTrue(second.set("cn1.db.key.shared", "2222"));
        assertEquals("2222", second.get("cn1.db.key.shared"));

        JavaSESecureStorage firstAgain = storageFor("com.example.first.Main");
        assertEquals("1111", firstAgain.get("cn1.db.key.shared"),
                "the first project still has the key it stored");
    }

    @Test
    public void forgettingAKeyInOneProjectLeavesTheOtherAlone() {
        JavaSESecureStorage first = storageFor("com.example.first.Main");
        first.set("cn1.db.key.shared", "1111");
        JavaSESecureStorage second = storageFor("com.example.second.Main");
        second.set("cn1.db.key.shared", "2222");

        assertTrue(second.remove("cn1.db.key.shared"), "the second project forgets its own key");
        assertEquals(SecureStorage.ENTRY_ABSENT, second.entryState("cn1.db.key.shared"));

        JavaSESecureStorage firstAgain = storageFor("com.example.first.Main");
        assertEquals(SecureStorage.ENTRY_PRESENT, firstAgain.entryState("cn1.db.key.shared"),
                "the other project's key is not the one that was forgotten");
        assertEquals("1111", firstAgain.get("cn1.db.key.shared"));
    }

    @Test
    public void anEntryFromBeforeTheSplitIsStillReadable() {
        // What an earlier simulator run left behind: the value sitting in the shared node. It has
        // to keep working, and for a managed database key it is the only copy there is.
        JavaSESecureStorage writer = storageFor("com.example.first.Main");
        writer.set("cn1.db.key.shared", "1111");
        Preferences shared = Preferences.userRoot().node(SHARED_NODE);
        String ciphertext = shared.node("com.example.first").get("v_cn1.db.key.shared", null);
        assertTrue(ciphertext != null, "the fixture needs the stored form to move down a level");
        shared.put("v_cn1.db.key.shared", ciphertext);
        shared.node("com.example.first").remove("v_cn1.db.key.shared");

        JavaSESecureStorage reader = storageFor("com.example.first.Main");
        assertEquals(SecureStorage.ENTRY_PRESENT, reader.entryState("cn1.db.key.shared"),
                "an entry in the shared node still counts as present");
        assertEquals("1111", reader.get("cn1.db.key.shared"),
                "and still decrypts, because the salt behind the key did not move");
        assertNull(shared.get("v_cn1.db.key.shared", null),
                "and it has been moved into this project's own node");
    }
}
