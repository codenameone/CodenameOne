/*
 * Codename One - software (AES-at-rest) secrets store fallback.
 */
package com.codename1.secrets;

import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import com.codename1.security.Cipher;
import com.codename1.security.KeyGenerator;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/// The software fallback [SecretsStore]: each value is encrypted with
/// authenticated AES-256-GCM (via [com.codename1.security.Cipher], which runs
/// on the platform's native crypto provider) before being written to
/// `Storage`, so secrets are never stored in the clear and tampering is
/// detected on read. The 256-bit master key is generated once with the
/// platform CSPRNG and kept in `Preferences`; every value uses a fresh
/// 12-byte nonce.
///
/// This is secure *at rest* but software-only ([#isHardwareBacked()] is
/// false): the master key lives in app storage, not a secure enclave. On a
/// device, register a hardware-backed store (iOS Keychain / Android Keystore)
/// via [Secrets#setStore(SecretsStore)] instead -- this fallback exists so
/// `Secrets` works everywhere (notably the simulator) without a plaintext
/// write.
final class DefaultSecretsStore implements SecretsStore {

    private static final String ENTRY_PREFIX = "cn1secret$";
    private static final String MASTER_KEY_PREF = "cn1$secrets$master";
    private static final int NONCE_LEN = 12;

    private SecretKey key;

    private synchronized SecretKey key() {
        if (key != null) {
            return key;
        }
        String mk = Preferences.get(MASTER_KEY_PREF, (String) null);
        byte[] raw;
        if (mk == null || mk.length() == 0) {
            raw = KeyGenerator.aes(256).getEncoded();
            Preferences.set(MASTER_KEY_PREF, hex(raw));
        } else {
            raw = unhex(mk);
        }
        key = new SecretKey("AES", raw);
        return key;
    }

    public void set(String secretKey, String value) {
        if (value == null) {
            delete(secretKey);
            return;
        }
        byte[] nonce = SecureRandom.bytes(NONCE_LEN);
        byte[] ct = Cipher.aesEncrypt(Cipher.AES_GCM, key(), nonce, null, utf8(value));
        Storage.getInstance().writeObject(entryName(secretKey), hex(concat(nonce, ct)));
    }

    public String get(String secretKey) {
        Object raw = Storage.getInstance().readObject(entryName(secretKey));
        if (!(raw instanceof String)) {
            return null;
        }
        try {
            byte[] all = unhex((String) raw);
            if (all.length <= NONCE_LEN) {
                return null;
            }
            byte[] nonce = new byte[NONCE_LEN];
            byte[] ct = new byte[all.length - NONCE_LEN];
            System.arraycopy(all, 0, nonce, 0, NONCE_LEN);
            System.arraycopy(all, NONCE_LEN, ct, 0, ct.length);
            byte[] pt = Cipher.aesDecrypt(Cipher.AES_GCM, key(), nonce, null, ct);
            return new String(pt, "UTF-8");
        } catch (Exception e) {
            // tampered ciphertext (GCM tag mismatch), wrong key, or corruption
            return null;
        }
    }

    public boolean contains(String secretKey) {
        return Storage.getInstance().exists(entryName(secretKey));
    }

    public void delete(String secretKey) {
        Storage.getInstance().deleteStorageFile(entryName(secretKey));
    }

    public List<String> keys() {
        List<String> out = new ArrayList<String>();
        String[] entries = Storage.getInstance().listEntries();
        if (entries != null) {
            for (int i = 0; i < entries.length; i++) {
                String e = entries[i];
                if (e != null && e.startsWith(ENTRY_PREFIX)) {
                    try {
                        out.add(new String(unhex(e.substring(ENTRY_PREFIX.length())), "UTF-8"));
                    } catch (UnsupportedEncodingException ex) {
                        // skip un-decodable entry name
                    }
                }
            }
        }
        return out;
    }

    public boolean isHardwareBacked() {
        return false;
    }

    // ---- helpers -------------------------------------------------------------

    private static String entryName(String secretKey) {
        return ENTRY_PREFIX + hex(utf8(secretKey));
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is guaranteed by the platform; never reached.
            throw new IllegalStateException("UTF-8 unsupported", e);
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(HEX[(b[i] >> 4) & 0xF]).append(HEX[b[i] & 0xF]);
        }
        return sb.toString();
    }

    private static byte[] unhex(String s) {
        int n = s.length() / 2;
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }
}
