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

    @Override
    public AsyncResource<String> get(final String reason, final String account) {
        final AsyncResource<String> result = new AsyncResource<String>();
        final String stored = prefs.get(account, null);
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
        prefs.put(account, value);
        result.complete(Boolean.TRUE);
        return result;
    }

    @Override
    public AsyncResource<Boolean> remove(String reason, String account) {
        AsyncResource<Boolean> result = new AsyncResource<Boolean>();
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
            plainPrefs.put(VALUE_PREFIX + account,
                    b64.encodeToString(c.getIV()) + ":" + b64.encodeToString(enc));
            // flush(), and its outcome is this method's answer. Preferences writes back
            // on its own schedule, so returning true said the secret was stored while it
            // was still only in memory -- and the simulator is killed abruptly all the
            // time, by the run button and by the IDE. The same reasoning as the Android
            // tier committing rather than applying: a write that reports success has to
            // have happened.
            plainPrefs.flush();
            return true;
        } catch (Exception e) {
            Log.e(e);
            return false;
        }
    }

    @Override
    public String get(String account) {
        if (account == null) {
            return null;
        }
        String stored = plainPrefs.get(VALUE_PREFIX + account, null);
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
        plainPrefs.remove(VALUE_PREFIX + account);
        try {
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
