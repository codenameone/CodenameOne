/*
 * Codename One - software (AES-at-rest) secrets store fallback.
 */
package com.codename1.secrets;

import com.codename1.io.Preferences;
import com.codename1.io.Storage;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The software fallback {@link SecretsStore}: each value is AES-256-CBC
 * encrypted and HMAC-SHA256 authenticated (encrypt-then-MAC) before being
 * written to {@link Storage}, so secrets are never stored in the clear. The
 * master key is generated once with {@link SecureRandom} and kept in
 * {@link Preferences}; per-value encryption and MAC subkeys are derived from
 * it with domain separation.
 *
 * <p>This is secure <em>at rest</em> but software-only ({@link
 * #isHardwareBacked()} is false): the master key lives in app storage, not a
 * secure enclave. On a device, register a hardware-backed store (iOS Keychain
 * / Android Keystore) via {@link Secrets#setStore(SecretsStore)} instead --
 * this fallback exists so {@code Secrets} works everywhere (notably the
 * simulator) without a plaintext write.
 */
final class DefaultSecretsStore implements SecretsStore {

    private static final String ENTRY_PREFIX = "cn1secret$";
    private static final String MASTER_KEY_PREF = "cn1$secrets$master";
    private static final int IV_LEN = 16;
    private static final int MAC_LEN = 32;

    private byte[] encKey;
    private byte[] macKey;

    private synchronized void ensureKeys() {
        if (encKey != null) {
            return;
        }
        String mk = Preferences.get(MASTER_KEY_PREF, (String) null);
        byte[] master;
        if (mk == null || mk.length() == 0) {
            master = new byte[32];
            new SecureRandom().nextBytes(master);
            Preferences.set(MASTER_KEY_PREF, hex(master));
        } else {
            master = unhex(mk);
        }
        encKey = derive("cn1-enc", master);
        macKey = derive("cn1-mac", master);
    }

    public void set(String key, String value) {
        if (value == null) {
            delete(key);
            return;
        }
        ensureKeys();
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encKey, "AES"), new IvParameterSpec(iv));
            byte[] ct = c.doFinal(utf8(value));
            byte[] body = concat(iv, ct);
            byte[] mac = hmac(body);
            Storage.getInstance().writeObject(entryName(key), hex(concat(body, mac)));
        } catch (Exception e) {
            throw new IllegalStateException("secret encryption failed", e);
        }
    }

    public String get(String key) {
        ensureKeys();
        Object raw = Storage.getInstance().readObject(entryName(key));
        if (!(raw instanceof String)) {
            return null;
        }
        try {
            byte[] all = unhex((String) raw);
            if (all.length < IV_LEN + MAC_LEN) {
                return null;
            }
            int bodyLen = all.length - MAC_LEN;
            byte[] body = new byte[bodyLen];
            byte[] mac = new byte[MAC_LEN];
            System.arraycopy(all, 0, body, 0, bodyLen);
            System.arraycopy(all, bodyLen, mac, 0, MAC_LEN);
            if (!constantTimeEquals(mac, hmac(body))) {
                return null; // tampered or wrong key
            }
            byte[] iv = new byte[IV_LEN];
            byte[] ct = new byte[bodyLen - IV_LEN];
            System.arraycopy(body, 0, iv, 0, IV_LEN);
            System.arraycopy(body, IV_LEN, ct, 0, ct.length);
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encKey, "AES"), new IvParameterSpec(iv));
            return new String(c.doFinal(ct), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public boolean contains(String key) {
        return Storage.getInstance().exists(entryName(key));
    }

    public void delete(String key) {
        Storage.getInstance().deleteStorageFile(entryName(key));
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

    private static String entryName(String key) {
        return ENTRY_PREFIX + hex(utf8(key));
    }

    private byte[] hmac(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(macKey, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] derive(String label, byte[] master) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(utf8(label));
            md.update(master);
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException("secret key derivation failed", e);
        }
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s.getBytes();
        }
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = new byte[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < a.length; i++) {
            r |= a[i] ^ b[i];
        }
        return r == 0;
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
