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
package com.codename1.androidcompat.jdk;

import com.codename1.io.Storage;
import com.codename1.security.SecureStorage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// Where the `AndroidKeyStore` keeps its keys.
///
/// The key material goes into the quiet (non-prompting) half of
/// [SecureStorage] -- `SecureStorage.set(account, value)`, which is the iOS
/// keychain (`kSecAttrAccessibleAfterFirstUnlock`) and, on Android, AES-GCM
/// under a key of the platform's own `AndroidKeyStore`. Neither prompts, which
/// matters: an Android Keystore key made without
/// `setUserAuthenticationRequired(true)` -- the license or token case -- never
/// asks for a fingerprint, so an application that suddenly did would be broken,
/// not hardened. The biometric half of `SecureStorage` is never used.
///
/// The alias, algorithm, modes and creation time are not secret and stay in
/// the application's private [Storage], one entry per alias: `SecureStorage`
/// cannot list its accounts, and `KeyStore.aliases()` must.
///
/// #### Where the material is not protected
///
/// A port with no quiet secure store (the base `SecureStorage`, whose `set`
/// answers false) gets the material written into that same [Storage] entry, in
/// the clear. That is the sandbox's protection and no more: a rooted device or
/// a backup of the application's data can read it. The entry records which of
/// the two it used, so a key is read back from where it was written.
///
/// A key whose material is in `SecureStorage` and has gone missing (the user
/// cleared the keychain, the platform revoked the wrapping key) reads as
/// absent, as a key invalidated by the platform does on Android: the
/// application generates a new one.
final class AndroidKeyStoreEntries {
    private static final String PREFIX = "cn1-androidks-";
    private static final int VERSION = 2;
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private AndroidKeyStoreEntries() {
    }

    static String entryName(String alias) {
        byte[] b;
        try {
            b = alias.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 missing");
        }
        StringBuilder sb = new StringBuilder(PREFIX.length() + b.length * 2);
        sb.append(PREFIX);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX[(b[i] >> 4) & 0xf]).append(HEX[b[i] & 0xf]);
        }
        return sb.toString();
    }

    private static String aliasOf(String entry) {
        if (!entry.startsWith(PREFIX)) {
            return null;
        }
        int n = entry.length() - PREFIX.length();
        if (n % 2 != 0) {
            return null;
        }
        byte[] b = new byte[n / 2];
        for (int i = 0; i < b.length; i++) {
            int hi = Character.digit(entry.charAt(PREFIX.length() + i * 2), 16);
            int lo = Character.digit(entry.charAt(PREFIX.length() + i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            b[i] = (byte) ((hi << 4) | lo);
        }
        try {
            return new String(b, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return null;
        }
    }

    static boolean contains(String alias) {
        return Storage.getInstance().exists(entryName(alias));
    }

    static List<String> aliases() {
        List<String> out = new ArrayList<String>();
        String[] entries = Storage.getInstance().listEntries();
        if (entries == null) {
            return out;
        }
        for (int i = 0; i < entries.length; i++) {
            String a = entries[i] == null ? null : aliasOf(entries[i]);
            if (a != null) {
                out.add(a);
            }
        }
        return out;
    }

    static void delete(String alias) {
        String name = entryName(alias);
        SecureStorage.getInstance().remove(name);
        Storage.getInstance().deleteStorageFile(name);
    }

    static void write(AndroidKeyStoreSecretKey key) throws IOException {
        byte[] m = key.material();
        boolean secure = SecureStorage.getInstance().set(entryName(key.alias()), hex(m));
        OutputStream os = Storage.getInstance().createOutputStream(entryName(key.alias()));
        if (os == null) {
            throw new IOException("Storage unavailable");
        }
        DataOutputStream out = new DataOutputStream(os);
        try {
            out.writeInt(VERSION);
            out.writeUTF(key.getAlgorithm());
            out.writeInt(key.purposes());
            writeStrings(out, key.blockModes());
            writeStrings(out, key.paddings());
            out.writeBoolean(key.randomizedEncryptionRequired());
            out.writeLong(key.created());
            out.writeBoolean(secure);
            if (secure) {
                out.writeInt(0);
            } else {
                out.writeInt(m.length);
                out.write(m);
            }
        } finally {
            out.close();
        }
    }

    /// The stored key, or null when nothing is stored under `alias`.
    static AndroidKeyStoreSecretKey read(String alias) throws IOException {
        String name = entryName(alias);
        if (!Storage.getInstance().exists(name)) {
            return null;
        }
        InputStream is = Storage.getInstance().createInputStream(name);
        if (is == null) {
            return null;
        }
        DataInputStream in = new DataInputStream(is);
        try {
            int version = in.readInt();
            if (version != VERSION) {
                throw new IOException("Unknown key entry version " + version);
            }
            String algorithm = in.readUTF();
            int purposes = in.readInt();
            String[] modes = readStrings(in);
            String[] paddings = readStrings(in);
            boolean randomized = in.readBoolean();
            long created = in.readLong();
            boolean secure = in.readBoolean();
            int len = in.readInt();
            byte[] m;
            if (secure) {
                m = unhex(SecureStorage.getInstance().get(name));
                if (m == null) {
                    return null;
                }
            } else {
                if (len <= 0 || len > 64) {
                    throw new IOException("Corrupt key entry");
                }
                m = new byte[len];
                in.readFully(m);
            }
            return new AndroidKeyStoreSecretKey(alias, algorithm, purposes, modes, paddings,
                    randomized, created, m);
        } finally {
            in.close();
        }
    }

    private static void writeStrings(DataOutputStream out, String[] s) throws IOException {
        out.writeInt(s.length);
        for (int i = 0; i < s.length; i++) {
            out.writeUTF(s[i]);
        }
    }

    private static String[] readStrings(DataInputStream in) throws IOException {
        int n = in.readInt();
        if (n < 0 || n > 32) {
            throw new IOException("Corrupt key entry");
        }
        String[] s = new String[n];
        for (int i = 0; i < n; i++) {
            s[i] = in.readUTF();
        }
        return s;
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX[(b[i] >> 4) & 0xf]).append(HEX[b[i] & 0xf]);
        }
        return sb.toString();
    }

    /// The bytes `s` spells, or null when it is missing or not a key.
    private static byte[] unhex(String s) {
        if (s == null || s.length() == 0 || s.length() % 2 != 0 || s.length() > 128) {
            return null;
        }
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < b.length; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                return null;
            }
            b[i] = (byte) ((hi << 4) | lo);
        }
        return b;
    }
}
