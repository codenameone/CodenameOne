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
package android.security.keystore;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.AEADBadTagException;
import javax.crypto.BadPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

import com.codename1.androidcompat.jdk.Cipher;
import com.codename1.androidcompat.jdk.KeyGenerator;
import com.codename1.androidcompat.jdk.KeyStore;
import com.codename1.androidcompat.jdk.SecretKeySpec;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/// The license/token flow written for the platform's `AndroidKeyStore`, run
/// against the shims the remap step points it at. The JDK names in these
/// signatures (`GCMParameterSpec`, `SecretKey`, the exceptions) are what the
/// shims are compiled against; on a device they are remapped too.
public class AndroidKeyStoreTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static final byte[] DATA;

    static {
        try {
            DATA = "license:4242-ACME".getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Before
    public void clean() {
        HeadlessImplementation.install();
        HeadlessImplementation.STORAGE.clear();
        HeadlessImplementation.SECURE.clear();
        HeadlessImplementation.SECURE_STORAGE_ON.set(false);
        com.codename1.io.Storage.getInstance().clearCache();
    }

    private static KeyStore loaded() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ks;
    }

    private static void generate(String alias, String mode, String padding) throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(mode).setEncryptionPaddings(padding).build());
        SecretKey k = kg.generateKey();
        assertNull("keystore keys do not export", k.getEncoded());
    }

    @Test
    public void gcmRoundTripsThroughAStoredKey() throws Exception {
        KeyStore ks = loaded();
        assertFalse(ks.containsAlias("lic"));
        generate("lic", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        assertTrue(ks.containsAlias("lic"));

        SecretKey k = (SecretKey) ks.getKey("lic", null);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] iv = c.getIV();
        assertEquals(12, iv.length);
        byte[] ct = c.doFinal(DATA);
        assertEquals(DATA.length + 16, ct.length);

        Cipher d = Cipher.getInstance("AES/GCM/NoPadding");
        d.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, iv));
        assertArrayEquals(DATA, d.doFinal(ct));
    }

    @Test
    public void cbcPkcs7RoundTrips() throws Exception {
        generate("cbc", KeyProperties.BLOCK_MODE_CBC, KeyProperties.ENCRYPTION_PADDING_PKCS7);
        SecretKey k = (SecretKey) loaded().getKey("cbc", null);
        Cipher c = Cipher.getInstance("AES/CBC/PKCS7Padding");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] iv = c.getIV();
        assertEquals(16, iv.length);
        c.update(DATA, 0, 4);
        byte[] ct = c.doFinal(DATA, 4, DATA.length - 4);
        assertEquals(0, ct.length % 16);

        Cipher d = Cipher.getInstance("AES/CBC/PKCS7Padding");
        d.init(Cipher.DECRYPT_MODE, k, new IvParameterSpec(iv));
        assertArrayEquals(DATA, d.doFinal(ct));
    }

    @Test
    public void aliasesPersistAcrossInstancesAndDelete() throws Exception {
        generate("one", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        generate("two/with space", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        SecretKey first = (SecretKey) loaded().getKey("one", null);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, first);
        byte[] iv = c.getIV();
        byte[] ct = c.doFinal(DATA);

        com.codename1.io.Storage.getInstance().clearCache();
        KeyStore fresh = loaded();
        List<String> names = new ArrayList<String>();
        for (Enumeration<String> e = fresh.aliases(); e.hasMoreElements();) {
            names.add(e.nextElement());
        }
        Collections.sort(names);
        assertEquals("[one, two/with space]", names.toString());
        assertEquals(2, fresh.size());
        KeyStore.Entry entry = fresh.getEntry("one", null);
        assertTrue(entry instanceof KeyStore.SecretKeyEntry);

        Cipher d = Cipher.getInstance("AES/GCM/NoPadding");
        d.init(Cipher.DECRYPT_MODE, fresh.getKey("one", null), new GCMParameterSpec(128, iv));
        assertArrayEquals(DATA, d.doFinal(ct));

        fresh.deleteEntry("one");
        assertFalse(loaded().containsAlias("one"));
        assertNull(loaded().getKey("one", null));
        assertTrue(loaded().containsAlias("two/with space"));
    }

    @Test
    public void wrongIvOrTamperedTagFailsWithAeadBadTag() throws Exception {
        generate("lic", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        SecretKey k = (SecretKey) loaded().getKey("lic", null);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] iv = c.getIV();
        byte[] ct = c.doFinal(DATA);

        byte[] wrongIv = iv.clone();
        wrongIv[0] ^= 1;
        Cipher d = Cipher.getInstance("AES/GCM/NoPadding");
        d.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, wrongIv));
        try {
            d.doFinal(ct);
            fail("wrong IV decrypted");
        } catch (AEADBadTagException expected) {
            // expected
        }

        byte[] tampered = ct.clone();
        tampered[tampered.length - 1] ^= 1;
        d.init(Cipher.DECRYPT_MODE, k, new GCMParameterSpec(128, iv));
        try {
            d.doFinal(tampered);
            fail("tampered tag decrypted");
        } catch (AEADBadTagException expected) {
            // expected
        }
    }

    @Test
    public void keystoreKeysEnforceModeAndRandomizedIv() throws Exception {
        generate("gcm", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        SecretKey k = (SecretKey) loaded().getKey("gcm", null);
        try {
            Cipher.getInstance("AES/CBC/PKCS7Padding").init(Cipher.ENCRYPT_MODE, k);
            fail("CBC on a GCM-only key");
        } catch (java.security.InvalidKeyException expected) {
            // expected
        }
        try {
            Cipher.getInstance("AES/GCM/NoPadding").init(Cipher.ENCRYPT_MODE, k,
                    new GCMParameterSpec(128, new byte[12]));
            fail("caller IV accepted");
        } catch (java.security.InvalidAlgorithmParameterException expected) {
            // expected
        }
    }

    @Test
    public void plainSecretKeySpecWorksAndBadPaddingIsReported() throws Exception {
        SecretKeySpec key = new SecretKeySpec(new byte[16], "AES");
        byte[] iv = new byte[16];
        Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] ct = c.doFinal(DATA);
        Cipher d = Cipher.getInstance("AES/CBC/PKCS5Padding");
        d.init(Cipher.DECRYPT_MODE, new SecretKeySpec(new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16},
                "AES"), new IvParameterSpec(iv));
        try {
            byte[] out = d.doFinal(ct);
            // A wrong key can, rarely, still unpad; it never yields the plaintext.
            assertFalse(java.util.Arrays.equals(DATA, out));
        } catch (BadPaddingException expected) {
            // expected
        }
        SecretKey generated = KeyGenerator.getInstance("AES").generateKey();
        assertEquals(16, generated.getEncoded().length);
        assertEquals("RAW", generated.getFormat());
    }

    @Test
    public void materialGoesToSecureStorageWhenThePortHasOne() throws Exception {
        HeadlessImplementation.SECURE_STORAGE_ON.set(true);
        generate("lic", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        assertEquals(1, HeadlessImplementation.SECURE.size());
        String material = HeadlessImplementation.SECURE.values().iterator().next();
        assertTrue("an AES key, hex", material.length() == 32 || material.length() == 64);
        byte[] raw = new byte[material.length() / 2];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) Integer.parseInt(material.substring(i * 2, i * 2 + 2), 16);
        }
        for (byte[] entry : HeadlessImplementation.STORAGE.values()) {
            assertFalse("the key material must not be in plain storage", indexOf(entry, raw) >= 0);
        }

        com.codename1.io.Storage.getInstance().clearCache();
        SecretKey k = (SecretKey) loaded().getKey("lic", null);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] iv = c.getIV();
        byte[] ct = c.doFinal(DATA);
        Cipher d = Cipher.getInstance("AES/GCM/NoPadding");
        d.init(Cipher.DECRYPT_MODE, loaded().getKey("lic", null), new GCMParameterSpec(128, iv));
        assertArrayEquals(DATA, d.doFinal(ct));

        // The platform dropping the secret reads as an invalidated key.
        HeadlessImplementation.SECURE.clear();
        assertNull(loaded().getKey("lic", null));

        generate("gone", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        loaded().deleteEntry("gone");
        assertTrue("delete removes the secret too", HeadlessImplementation.SECURE.isEmpty());
    }

    @Test
    public void withoutSecureStorageTheMaterialStaysInPrivateStorage() throws Exception {
        generate("lic", KeyProperties.BLOCK_MODE_GCM, KeyProperties.ENCRYPTION_PADDING_NONE);
        assertTrue(HeadlessImplementation.SECURE.isEmpty());
        com.codename1.io.Storage.getInstance().clearCache();
        assertTrue(loaded().getKey("lic", null) instanceof SecretKey);
    }

    private static int indexOf(byte[] hay, byte[] needle) {
        for (int i = 0; i + needle.length <= hay.length; i++) {
            int j = 0;
            while (j < needle.length && hay[i + j] == needle[j]) {
                j++;
            }
            if (j == needle.length) {
                return i;
            }
        }
        return -1;
    }
}
