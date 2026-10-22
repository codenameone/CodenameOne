/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.crypto;

import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.security.Cipher;
import com.codename1.security.CryptoException;
import com.codename1.security.Hmac;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.security.SecureStorage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/// Everything this app keeps on the device, encrypted at rest.
///
/// Keys, sessions and message history are JSON documents in [Storage],
/// each sealed with AES-GCM under a vault key that lives in the platform
/// keystore ([SecureStorage]: the Keychain, the Android Keystore). A copy of
/// the app's files -- a backup, a forensic image -- holds nothing readable
/// without that key, and signing out deletes it, which makes the files noise.
///
/// Every account on the device has its own vault, named by a hash of the
/// account so the file names do not say whose they are.
public final class Vault {
    private static String prefix;
    private static SecretKey key;

    private Vault() {
    }

    /// Opens the vault of `owner`, making a key the first time.
    public static void open(String owner) {
        prefix = "pv-" + tag(owner) + "-";
        // Named by the tag too: an owner is a server address and a username,
        // longer than the 80 characters a desktop keystore name may be.
        String account = "parley.vault." + tag(owner);
        SecureStorage keystore = SecureStorage.getInstance();
        byte[] raw = Codec.unb64(keystore.get(account));
        if (raw == null || raw.length != 32) {
            Object fallback = Storage.getInstance().readObject(prefix + "key");
            raw = Codec.unb64(fallback);
        }
        if (raw == null || raw.length != 32) {
            raw = SecureRandom.bytes(32);
            if (!keystore.set(account, Codec.b64(raw))) {
                // A platform with no keystore (a browser profile, some
                // desktops) keeps the key beside the data. The files are
                // then only as private as the device's own storage.
                Storage.getInstance().writeObject(prefix + "key", Codec.b64(raw));
            }
        }
        key = new SecretKey("AES", raw);
    }

    private static String tag(String owner) {
        return Codec.b64(Hmac.sha256(Codec.utf8("parley"), Codec.utf8(owner)))
                .substring(0, 16).replace('/', '_').replace('+', '-');
    }

    public static boolean isOpen() {
        return key != null;
    }

    /// Deletes every document of the open vault and forgets its key.
    public static void destroy(String owner) {
        if (prefix != null) {
            String[] names = Storage.getInstance().listEntries();
            for (int iter = 0; names != null && iter < names.length; iter++) {
                if (names[iter].startsWith(prefix)) {
                    Storage.getInstance().deleteStorageFile(names[iter]);
                }
            }
        }
        SecureStorage.getInstance().remove("parley.vault." + tag(owner));
        close();
    }

    public static void close() {
        key = null;
        prefix = null;
    }

    /// A document that could not be written: storage is full, or the write
    /// was cut off. Callers must not act as though the state was saved -- a
    /// ratchet that moved on in memory only would send what it cannot read
    /// back after a restart -- so this is thrown, not logged.
    public static final class WriteFailed extends RuntimeException {
        WriteFailed(String name, IOException cause) {
            super("Could not save " + name + ": " + cause.getMessage());
        }
    }

    /// Saves `document` under `name`. It is written whole to a side file
    /// first, then over the old copy, and the side file is dropped only after
    /// that: an interrupted write leaves one complete copy either way, and
    /// [#get] prefers the side file while it exists.
    public static void put(String name, Map<String, ?> document) {
        if (key == null) {
            return;
        }
        // A side file left by an earlier put that was cut off is the only
        // current copy. It is promoted over the stale main file before it is
        // overwritten, so a crash in the write below falls back to it rather
        // than to the older state, which would roll a ratchet back.
        Map<String, Object> pending = read(prefix + name + SIDE, name);
        if (pending != null) {
            seal(prefix + name, name, pending);
        }
        seal(prefix + name + SIDE, name, document);
        seal(prefix + name, name, document);
        Storage.getInstance().deleteStorageFile(prefix + name + SIDE);
    }

    private static void seal(String file, String name, Map<String, ?> document) {
        byte[] nonce = SecureRandom.bytes(12);
        byte[] sealed = Cipher.aesEncrypt(Cipher.AES_GCM, key, nonce, Codec.utf8(name),
                Codec.utf8(Codec.json(document)));
        write(file, nonce, sealed, name);
    }

    private static final String SIDE = ".new";

    private static void write(String file, byte[] nonce, byte[] sealed, String name) {
        OutputStream out = null;
        try {
            out = Storage.getInstance().createOutputStream(file);
            out.write(nonce);
            out.write(sealed);
            out.close();
            out = null;
        } catch (IOException failed) {
            throw new WriteFailed(name, failed);
        } finally {
            Util.cleanup(out);
        }
    }

    /// The document called `name`, or null when there is none or it does not
    /// open: a file from a vault whose key is gone reads as absent.
    public static Map<String, Object> get(String name) {
        if (key == null) {
            return null;
        }
        Map<String, Object> side = read(prefix + name + SIDE, name);
        return side != null ? side : read(prefix + name, name);
    }

    private static Map<String, Object> read(String file, String name) {
        if (!Storage.getInstance().exists(file)) {
            return null;
        }
        InputStream in = null;
        try {
            in = Storage.getInstance().createInputStream(file);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Util.copy(in, bytes);
            byte[] all = bytes.toByteArray();
            if (all.length < 28) {
                return null;
            }
            byte[] plain = Cipher.aesDecrypt(Cipher.AES_GCM, key, Codec.slice(all, 0, 12),
                    Codec.utf8(name), Codec.slice(all, 12, all.length - 12));
            return Codec.parse(Codec.text(plain));
        } catch (IOException | CryptoException unreadable) {
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    public static void remove(String name) {
        if (prefix != null) {
            Storage.getInstance().deleteStorageFile(prefix + name);
            Storage.getInstance().deleteStorageFile(prefix + name + SIDE);
        }
    }
}
