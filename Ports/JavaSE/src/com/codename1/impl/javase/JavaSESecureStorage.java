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
package com.codename1.impl.javase;

import com.codename1.security.AuthenticationOptions;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codename1.security.Biometrics;
import com.codename1.security.SecureStorage;
import com.codename1.io.Log;
import com.codename1.util.AsyncResource;
import com.codename1.util.AsyncResult;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.BackingStoreException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Simulator backing for {@link SecureStorage}. Reads gate behind the
 * {@link Biometrics} prompt (which the simulator menu controls); writes
 * persist to {@code java.util.prefs} so values survive a JVM restart.
 *
 * <p>The non-prompting tier is <em>obfuscation, not security</em>. It encrypts
 * with a key derived from the OS user account plus a per-node random salt, both
 * of which live on the same machine as the ciphertext -- anything running as
 * this user can recover the plaintext. Its job is to keep API keys out of
 * cleartext in the project tree during simulator runs, which is exactly what
 * the base-class contract promises for the desktop. Device platforms back the
 * same API with real hardware-held keys.</p>
 */
public final class JavaSESecureStorage extends SecureStorage {

    private static final String NODE = "com.codename1.simulator.secureStorage";
    private static final String PLAIN_NODE = "com.codename1.simulator.secureStorage.plain";
    private static final String SALT_KEY = "__cn1_salt";
    private static final String VALUE_PREFIX = "v_";
    private static final int GCM_TAG_BITS = 128;
    private static final int PBKDF2_ROUNDS = 120000;

    private final java.util.prefs.Preferences prefs;
    private final java.util.prefs.Preferences plainPrefs;
    private final JavaSEBiometrics biometrics;
    private SecretKey plainKey;

    JavaSESecureStorage(JavaSEBiometrics biometrics) {
        this.biometrics = biometrics;
        this.prefs = java.util.prefs.Preferences.userRoot().node(NODE);
        this.plainPrefs = java.util.prefs.Preferences.userRoot().node(PLAIN_NODE);
    }

    /**
     * The node this application's entries live in, under the shared one.
     *
     * <p>The simulator runs every project on one machine under one OS user, and this class kept
     * every account in one fixed Preferences node -- so two projects that both asked for a managed
     * key under the same alias read each other's key, and forgetting it in either one removed the
     * only copy either had. The device ports are separated by an OS sandbox; here the separation
     * has to be written down.</p>
     *
     * <p>Resolved per call rather than in the constructor: this object is built while the port is
     * coming up, before {@code Display} can answer what the application is, and an answer cached
     * from that moment would name every project the same thing.</p>
     */
    private java.util.prefs.Preferences appNode(java.util.prefs.Preferences shared) {
        return shared.node(applicationNamespace(launcherPackage()));
    }

    /**
     * The package of the class the simulator was launched with, or null if there is none.
     *
     * <p>Preferred over asking {@code Display}, because this object is built while the port is
     * still coming up: {@code Display} answers {@code package_name} from this very property once
     * it is running, so this is the same identity a moment earlier.</p>
     */
    private static String launcherPackage() {
        String mainClass = System.getProperty("MainClass", null);
        if (mainClass == null) {
            return null;
        }
        int lastDot = mainClass.lastIndexOf('.');
        return lastDot > 0 ? mainClass.substring(0, lastDot) : mainClass;
    }

    /**
     * Moves an entry an earlier run left in the shared node into this application's own.
     *
     * <p>The value moves as it is: the salt behind the key stays in the shared node, so ciphertext
     * written before this split still decrypts with the same key afterwards. Written to the new
     * place before it is dropped from the old one, because for a managed database key this entry
     * is the only copy there is.</p>
     *
     * @param shared the node entries used to live in
     * @param key the preference key
     * @return the value, or null when there was nothing to adopt
     */
    private String adoptSharedEntry(java.util.prefs.Preferences shared, String key) {
        String stored = shared.get(key, null);
        if (stored == null) {
            return null;
        }
        java.util.prefs.Preferences mine = appNode(shared);
        mine.put(key, stored);
        try {
            mine.flush();
            shared.remove(key);
            shared.flush();
        } catch (BackingStoreException cannotMove) {
            // The value is still readable where it was, which is what the caller is given.
            Log.e(cannotMove);
        }
        return stored;
    }

    @Override
    public AsyncResource<String> get(final String reason, final String account) {
        final AsyncResource<String> result = new AsyncResource<String>();
        String mine = appNode(prefs).get(account, null);
        if (mine == null) {
            mine = adoptSharedEntry(prefs, account);
        }
        final String stored = mine;
        if (stored == null) {
            result.error(new BiometricException(BiometricError.UNKNOWN,
                    "No secure storage entry for account: " + account));
            return result;
        }
        AsyncResource<Boolean> auth = biometrics.authenticate(
                new AuthenticationOptions().setReason(reason));
        auth.onResult(new AsyncResult<Boolean>() {
            @Override
            public void onReady(Boolean ok, Throwable err) {
                if (err != null) {
                    result.error(err);
                } else {
                    result.complete(stored);
                }
            }
        });
        return result;
    }

    @Override
    public AsyncResource<Boolean> set(final String reason, final String account, final String value) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        if (!biometrics.canAuthenticate()) {
            result.error(new BiometricException(BiometricError.NOT_AVAILABLE,
                    "Simulator: biometrics not enabled for secure storage write"));
            return result;
        }
        appNode(prefs).put(account, value);
        result.complete(Boolean.TRUE);
        return result;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        appNode(prefs).remove(account);
        // The shared node too, so an entry written before the split cannot be read back after a
        // caller was told it was removed.
        prefs.remove(account);
        result.complete(Boolean.TRUE);
        return result;
    }

    @Override
    public void setKeychainAccessGroup(String group) {
        // No-op in the simulator.
    }

    // --- Non-prompting tier ------------------------------------------------

    @Override
    public boolean set(String account, String value) {
        if (account == null || value == null) {
            return false;
        }
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, plainKey());
            byte[] enc = c.doFinal(value.getBytes("UTF-8"));
            Base64.Encoder b64 = Base64.getEncoder();
            appNode(plainPrefs).put(VALUE_PREFIX + account,
                    b64.encodeToString(c.getIV()) + ":" + b64.encodeToString(enc));
            // Anything an earlier run left in the shared node goes, now that this one holds the
            // current value: leaving it would have a later read fall back to a stale secret.
            plainPrefs.remove(VALUE_PREFIX + account);
            // flush(), and its outcome is this method's answer. Preferences writes back
            // on its own schedule, so returning true said the secret was stored while it
            // was still only in memory -- and the simulator is killed abruptly all the
            // time, by the run button and by the IDE. The same reasoning as the Android
            // tier committing rather than applying: a write that reports success has to
            // have happened.
            appNode(plainPrefs).flush();
            plainPrefs.flush();
            return true;
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }

    @Override
    public int entryState(String account) {
        if (account == null) {
            return ENTRY_UNKNOWN;
        }
        // The stored string, not the decrypted value: an entry whose ciphertext will not decrypt
        // still exists, and reporting it absent is what would let a caller overwrite it.
        if (appNode(plainPrefs).get(VALUE_PREFIX + account, null) != null) {
            return ENTRY_PRESENT;
        }
        // Absent here is not absent: an earlier run wrote it in the shared node, and reporting
        // nothing is what would have ManagedKeys generate a second key over a database the first
        // one encrypted.
        return plainPrefs.get(VALUE_PREFIX + account, null) != null ? ENTRY_PRESENT : ENTRY_ABSENT;
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        String stored = appNode(plainPrefs).get(VALUE_PREFIX + account, null);
        if (stored == null) {
            stored = adoptSharedEntry(plainPrefs, VALUE_PREFIX + account);
        }
        if (stored == null) {
            return null;
        }
        int sep = stored.indexOf(':');
        if (sep < 0) {
            return null;
        }
        try {
            Base64.Decoder b64 = Base64.getDecoder();
            byte[] iv = b64.decode(stored.substring(0, sep));
            byte[] enc = b64.decode(stored.substring(sep + 1));
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, plainKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(c.doFinal(enc), "UTF-8");
        } catch (Exception e) {
            Log.e(e);
            return null;
        }
    }

    @Override
    public boolean remove(String account) {
        if (account == null) {
            return false;
        }
        appNode(plainPrefs).remove(VALUE_PREFIX + account);
        plainPrefs.remove(VALUE_PREFIX + account);
        try {
            appNode(plainPrefs).flush();
            // Same for the removal, and it matters more: this is the credential a logout
            // clears, so an unflushed deletion is one that comes back on the next launch.
            plainPrefs.flush();
        } catch (BackingStoreException e) {
            Log.e(e);
            return false;
        }
        return true;
    }

    /**
     * Derives (or returns) the key the non-prompting tier encrypts with.
     *
     * <p>Locked on the class, not on the instance. {@code JavaSEPort.getSecureStorage()}
     * creates its singleton without synchronization, so two threads making the first call
     * concurrently each get their own {@code JavaSESecureStorage} -- and an instance lock
     * then serializes nothing. Both would find the shared salt missing, generate different
     * ones, and write values encrypted under different keys before one salt overwrote the
     * other in the same Preferences node; whichever lost is permanently undecryptable. The
     * salt and the node are process-wide, so the lock has to be too.</p>
     */
    private SecretKey plainKey() throws Exception {
        synchronized (KEY_LOCK) {
            return plainKeyLocked();
        }
    }

    /** The salt is shared by every instance in the process, so the lock is as well. */
    private static final Object KEY_LOCK = new Object();

    private SecretKey plainKeyLocked() throws Exception {
        if (plainKey != null) {
            return plainKey;
        }
        String saltB64 = plainPrefs.get(SALT_KEY, null);
        byte[] salt;
        if (saltB64 == null) {
            salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            plainPrefs.put(SALT_KEY, Base64.getEncoder().encodeToString(salt));
        } else {
            salt = Base64.getDecoder().decode(saltB64);
        }
        char[] material = (System.getProperty("user.name", "cn1")
                // "\0", not a literal zero byte in the file. Java accepts the raw
                // character, but git then classifies the whole source as binary: diffs
                // report "- -" instead of lines, and grep stops matching it, so every
                // later review of this file is blind. The octal escape rather than
                // \u0000 because unicode escapes are processed before the source is
                // tokenized, which is a footgun this line does not need to inherit.
                + "\0" + System.getProperty("user.home", "")).toCharArray();
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] derived = f.generateSecret(
                new PBEKeySpec(material, salt, PBKDF2_ROUNDS, 256)).getEncoded();
        plainKey = new SecretKeySpec(derived, "AES");
        return plainKey;
    }
}
