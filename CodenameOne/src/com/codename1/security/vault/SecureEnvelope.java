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
package com.codename1.security.vault;

import com.codename1.security.Cipher;
import com.codename1.security.CryptoException;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;

/// The one encrypted-blob format this package writes, and the only one it reads.
///
/// An envelope is self-describing: it carries the format version, the cipher suite, the key
/// derivation parameters, the salt, the nonce, the identity and version of the key that sealed
/// it, and the ciphertext with its tag. Everything except the ciphertext is authenticated but not
/// encrypted, so a reader can decide what to do before it has a key, and an attacker who edits
/// any of it fails the tag rather than steering the decryption.
///
/// #### Wire format, version 1
///
/// ```text
/// offset size  field
/// 0      4     magic, the ASCII bytes CN1V
/// 4      1     format version, 1
/// 5      1     cipher suite, 1 = AES-256-GCM with a 12 byte nonce and a 128 bit tag
/// 6      1     KDF id, 0 = none, 1 = PBKDF2-HMAC-SHA256
/// 7      4     KDF iterations, 0 when the KDF id is 0
/// 11     1     salt length, 0 to 64
/// 12     n     salt
///        1     nonce length, exactly 12 for suite 1
///        n     nonce
///        1     key id length, 0 to 64
///        n     key id, UTF-8
///        4     key version
///        4     ciphertext length
///        n     ciphertext, with the 16 byte GCM tag appended
/// ```
///
/// All integers are big-endian. The associated data fed to AES-GCM is **the whole header above,
/// followed by the caller's [AssociatedData] bytes** -- so the version, the suite, the iteration
/// count and the key id are covered by the tag. That is what makes a downgrade attempt fail
/// rather than succeed: an attacker who rewrites the iteration count to 1000 has changed the
/// authenticated bytes, and the decrypt returns [VaultError#AUTHENTICATION_FAILED].
///
/// #### What parsing rejects
///
/// A blob that is not this format, is truncated, declares a length that does not fit in the
/// bytes present, declares a suite or version this build does not implement, or exceeds
/// [#MAX_CIPHERTEXT] is refused before any key is touched. No field is clamped into range on
/// read, and there is no fallback to an earlier format: an envelope from the future is
/// [VaultError#UNSUPPORTED_FORMAT] and not something to guess at.
public final class SecureEnvelope {

    /// The four leading bytes of every envelope.
    static final byte[] MAGIC = {'C', 'N', '1', 'V'};

    /// The only format version this build writes.
    public static final int VERSION_1 = 1;

    /// AES-256-GCM, 12 byte nonce, 128 bit tag. The only suite version 1 defines.
    public static final int SUITE_AES_256_GCM = 1;

    /// GCM nonce length in bytes. Twelve, which is the size AES-GCM is defined for; any other
    /// length goes through an extra derivation step that not every platform implements alike.
    public static final int NONCE_LENGTH = 12;

    /// The GCM authentication tag length in bytes, appended to the ciphertext.
    public static final int TAG_LENGTH = 16;

    /// Ciphertext larger than this is refused on parse. Sixty-four megabytes is far past what a
    /// vault record should be, and the bound exists so a corrupt or hostile length field cannot
    /// make a reader allocate until it dies.
    public static final int MAX_CIPHERTEXT = 64 * 1024 * 1024;

    private static final int MAX_SALT = 64;
    private static final int MAX_KEY_ID = 64;

    private final int version;
    private final int suite;
    private final KdfProfile kdf;
    private final byte[] salt;
    private final byte[] nonce;
    private final String keyId;
    private final int keyVersion;
    private final byte[] ciphertext;
    private final byte[] header;

    private SecureEnvelope(int version, int suite, KdfProfile kdf, byte[] salt, byte[] nonce,
                           String keyId, int keyVersion, byte[] ciphertext, byte[] header) {
        this.version = version;
        this.suite = suite;
        this.kdf = kdf;
        this.salt = salt;
        this.nonce = nonce;
        this.keyId = keyId;
        this.keyVersion = keyVersion;
        this.ciphertext = ciphertext;
        this.header = header;
    }

    /// Seals plaintext under a key the caller already has.
    ///
    /// #### Parameters
    ///
    /// - `key`: 32 bytes of AES-256 key material
    ///
    /// - `keyId`: which key this is, so a reader with several can pick. At most 64 UTF-8 bytes
    ///
    /// - `keyVersion`: the key's rotation counter
    ///
    /// - `aad`: the binding this envelope may only be opened against
    ///
    /// - `plaintext`: the bytes to protect
    ///
    /// #### Returns
    ///
    /// the envelope bytes, safe to store or to send to a server that must not read them
    public static byte[] seal(byte[] key, String keyId, int keyVersion,
                              AssociatedData aad, byte[] plaintext) {
        return seal(key, keyId, keyVersion, aad == null ? null : aad.serialize(), plaintext);
    }

    /// The same, against associated data the caller has already serialized.
    ///
    /// [AssociatedData] is the spelling application code should use; this overload exists for the
    /// two callers that hold bytes rather than fields -- the device-protection SPI, whose
    /// associated data is the port's own, and an implementation of this format in another
    /// language checking itself against test vectors.
    ///
    /// #### Parameters
    ///
    /// - `key`: 32 bytes of AES-256 key material
    ///
    /// - `keyId`: which key this is, at most 64 UTF-8 bytes
    ///
    /// - `keyVersion`: the key's rotation counter
    ///
    /// - `associatedData`: authenticated, not encrypted; may be null for none
    ///
    /// - `plaintext`: the bytes to protect
    ///
    /// #### Returns
    ///
    /// the envelope bytes
    public static byte[] seal(byte[] key, String keyId, int keyVersion,
                              byte[] associatedData, byte[] plaintext) {
        return sealInternal(key, KdfProfile.forStored(KdfProfile.DIRECT, 0), new byte[0],
                keyId, keyVersion, associatedData, plaintext);
    }

    /// Seals plaintext under a key derived from a password.
    ///
    /// A fresh salt and nonce are generated for every call. Reusing either is the classic way to
    /// destroy AES-GCM, and there is no overload that lets a caller supply them.
    ///
    /// #### Parameters
    ///
    /// - `password`: the password, cleared by the caller afterwards
    ///
    /// - `profile`: the derivation profile, normally [KdfProfile#current()]
    ///
    /// - `keyId`: which key this is
    ///
    /// - `keyVersion`: the key's rotation counter
    ///
    /// - `aad`: the binding this envelope may only be opened against
    ///
    /// - `plaintext`: the bytes to protect
    ///
    /// #### Returns
    ///
    /// the envelope bytes
    public static byte[] sealWithPassword(char[] password, KdfProfile profile, String keyId,
                                          int keyVersion, AssociatedData aad, byte[] plaintext) {
        if (profile == null || profile.getKdfId() == KdfProfile.DIRECT) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "sealing with a password needs a derivation profile");
        }
        byte[] salt = SecureRandom.bytes(KdfProfile.SALT_LENGTH);
        byte[] key = profile.derive(password, salt, 32);
        try {
            return sealInternal(key, profile, salt, keyId, keyVersion,
                    aad == null ? null : aad.serialize(), plaintext);
        } finally {
            Bytes.zero(key);
        }
    }

    private static byte[] sealInternal(byte[] key, KdfProfile profile, byte[] salt, String keyId,
                                       int keyVersion, byte[] associatedData, byte[] plaintext) {
        if (key == null || key.length != 32) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "suite 1 requires a 32 byte key");
        }
        byte[] keyIdBytes = Bytes.utf8(keyId == null ? "" : keyId);
        if (keyIdBytes.length > MAX_KEY_ID) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "key id is longer than " + MAX_KEY_ID + " bytes");
        }
        if (salt.length > MAX_SALT) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "salt is longer than " + MAX_SALT + " bytes");
        }
        byte[] nonce = SecureRandom.bytes(NONCE_LENGTH);
        byte[] body = plaintext == null ? new byte[0] : plaintext;

        // Built before the encryption because the header is what the tag covers. The ciphertext
        // length is known in advance: GCM adds exactly the tag.
        byte[] header = header(VERSION_1, SUITE_AES_256_GCM, profile, salt, nonce, keyIdBytes,
                keyVersion, body.length + TAG_LENGTH);
        byte[] associated = Bytes.concat(header, associatedData);
        byte[] ciphertext;
        try {
            ciphertext = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", key),
                    nonce, associated, body);
        } catch (CryptoException failed) {
            throw new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "the platform could not perform AES-GCM encryption", failed);
        }
        if (ciphertext.length != body.length + TAG_LENGTH) {
            // The header already committed to a length; a provider that appended a different
            // amount would produce an envelope whose own tag does not cover its real size.
            throw new VaultException(VaultError.CRYPTO_UNAVAILABLE,
                    "the platform's AES-GCM produced an unexpected ciphertext length");
        }
        byte[] out = new byte[header.length + ciphertext.length];
        System.arraycopy(header, 0, out, 0, header.length);
        System.arraycopy(ciphertext, 0, out, header.length, ciphertext.length);
        return out;
    }

    private static byte[] header(int version, int suite, KdfProfile profile, byte[] salt,
                                 byte[] nonce, byte[] keyIdBytes, int keyVersion, int cipherLength) {
        byte[] out = new byte[12 + salt.length + 1 + nonce.length + 1 + keyIdBytes.length + 8];
        int at = 0;
        System.arraycopy(MAGIC, 0, out, at, 4);
        at += 4;
        out[at++] = (byte) version;
        out[at++] = (byte) suite;
        out[at++] = (byte) profile.getKdfId();
        Bytes.putInt(out, at, profile.getIterations());
        at += 4;
        out[at++] = (byte) salt.length;
        System.arraycopy(salt, 0, out, at, salt.length);
        at += salt.length;
        out[at++] = (byte) nonce.length;
        System.arraycopy(nonce, 0, out, at, nonce.length);
        at += nonce.length;
        out[at++] = (byte) keyIdBytes.length;
        System.arraycopy(keyIdBytes, 0, out, at, keyIdBytes.length);
        at += keyIdBytes.length;
        Bytes.putInt(out, at, keyVersion);
        at += 4;
        Bytes.putInt(out, at, cipherLength);
        return out;
    }

    /// Parses an envelope without opening it.
    ///
    /// Useful before a key is available: the key id and version say which key is needed, and
    /// [#getKdf()] says how long deriving one will take, which is worth showing a user before a
    /// six hundred thousand iteration derivation begins.
    ///
    /// #### Parameters
    ///
    /// - `sealed`: the envelope bytes
    ///
    /// #### Returns
    ///
    /// the parsed envelope
    ///
    /// #### Throws
    ///
    /// - `VaultException`: [VaultError#CORRUPT] for a malformed blob,
    ///   [VaultError#UNSUPPORTED_FORMAT] for a version, suite or KDF this build does not
    ///   implement
    public static SecureEnvelope parse(byte[] sealed) {
        if (sealed == null || sealed.length < 12) {
            throw new VaultException(VaultError.CORRUPT, "not an envelope: too short");
        }
        for (int iter = 0; iter < 4; iter++) {
            if (sealed[iter] != MAGIC[iter]) {
                throw new VaultException(VaultError.CORRUPT, "not an envelope: bad magic");
            }
        }
        int version = sealed[4] & 0xff;
        if (version != VERSION_1) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "envelope format version " + version + " is not supported by this build");
        }
        int suite = sealed[5] & 0xff;
        if (suite != SUITE_AES_256_GCM) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "envelope cipher suite " + suite + " is not supported by this build");
        }
        int kdfId = sealed[6] & 0xff;
        int iterations = Bytes.getInt(sealed, 7);
        KdfProfile profile = KdfProfile.forStored(kdfId, iterations);

        int at = 11;
        int saltLength = sealed[at++] & 0xff;
        if (saltLength > MAX_SALT) {
            throw new VaultException(VaultError.CORRUPT, "envelope salt is implausibly long");
        }
        if (at + saltLength > sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated in the salt");
        }
        byte[] salt = Bytes.slice(sealed, at, saltLength);
        at += saltLength;

        if (at >= sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated before the nonce");
        }
        int nonceLength = sealed[at++] & 0xff;
        if (nonceLength != NONCE_LENGTH) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "suite 1 requires a " + NONCE_LENGTH + " byte nonce, found " + nonceLength);
        }
        if (at + nonceLength > sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated in the nonce");
        }
        byte[] nonce = Bytes.slice(sealed, at, nonceLength);
        at += nonceLength;

        if (at >= sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated before the key id");
        }
        int keyIdLength = sealed[at++] & 0xff;
        if (keyIdLength > MAX_KEY_ID) {
            throw new VaultException(VaultError.CORRUPT, "envelope key id is implausibly long");
        }
        if (at + keyIdLength > sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated in the key id");
        }
        String keyId = Bytes.fromUtf8(sealed, at, keyIdLength);
        at += keyIdLength;

        if (at + 8 > sealed.length) {
            throw new VaultException(VaultError.CORRUPT, "envelope truncated in the key version");
        }
        int keyVersion = Bytes.getInt(sealed, at);
        at += 4;
        int cipherLength = Bytes.getInt(sealed, at);
        at += 4;
        if (cipherLength < TAG_LENGTH || cipherLength > MAX_CIPHERTEXT) {
            // Checked against the declared length before the buffer, so an implausible value is
            // rejected rather than used to size an allocation.
            throw new VaultException(VaultError.CORRUPT,
                    "envelope declares an implausible ciphertext length");
        }
        if (at + cipherLength != sealed.length) {
            // Exactly, not at least. Trailing bytes are not covered by the tag, so accepting them
            // would leave room to smuggle data past every check in this class.
            throw new VaultException(VaultError.CORRUPT,
                    "envelope length does not match its declared ciphertext length");
        }
        byte[] header = Bytes.slice(sealed, 0, at);
        byte[] ciphertext = Bytes.slice(sealed, at, cipherLength);
        return new SecureEnvelope(version, suite, profile, salt, nonce, keyId, keyVersion,
                ciphertext, header);
    }

    /// Opens an envelope with a key the caller already has.
    ///
    /// #### Parameters
    ///
    /// - `key`: 32 bytes of AES-256 key material
    ///
    /// - `aad`: the binding the envelope was sealed against. A different binding fails the tag,
    ///   which is the point of it
    ///
    /// #### Returns
    ///
    /// the plaintext
    ///
    /// #### Throws
    ///
    /// - `VaultException`: [VaultError#AUTHENTICATION_FAILED] when the tag does not verify --
    ///   wrong key, altered ciphertext, or the wrong binding. No plaintext is returned in that
    ///   case, not even a partial one
    public byte[] open(byte[] key, AssociatedData aad) {
        return open(key, aad == null ? null : aad.serialize());
    }

    /// The same, against associated data the caller has already serialized. See
    /// [#seal(byte[], String, int, byte[], byte[])] for why this overload exists.
    ///
    /// #### Parameters
    ///
    /// - `key`: 32 bytes of AES-256 key material
    ///
    /// - `associatedData`: the exact bytes the envelope was sealed against
    ///
    /// #### Returns
    ///
    /// the plaintext
    public byte[] open(byte[] key, byte[] associatedData) {
        if (key == null || key.length != 32) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "suite 1 requires a 32 byte key");
        }
        byte[] associated = Bytes.concat(header, associatedData);
        try {
            return Cipher.aesDecrypt(Cipher.AES_GCM, new SecretKey("AES", key),
                    nonce, associated, ciphertext);
        } catch (CryptoException failed) {
            // Everything a provider can report for a failed GCM open arrives as one exception, and
            // the reasons are not distinguishable from here: a wrong key, a flipped bit and a
            // mismatched binding all look the same. They are reported as one code on purpose --
            // telling a caller which of the three it was is telling an attacker too.
            throw new VaultException(VaultError.AUTHENTICATION_FAILED,
                    "the envelope did not authenticate: wrong key, altered data, or a binding "
                    + "that does not match the one it was sealed with", failed);
        }
    }

    /// Opens an envelope sealed with [#sealWithPassword].
    ///
    /// #### Parameters
    ///
    /// - `password`: the password, cleared by the caller afterwards
    ///
    /// - `aad`: the binding the envelope was sealed against
    ///
    /// #### Returns
    ///
    /// the plaintext
    ///
    /// #### Throws
    ///
    /// - `VaultException`: [VaultError#AUTHENTICATION_FAILED] for a wrong password
    public byte[] openWithPassword(char[] password, AssociatedData aad) {
        if (kdf.getKdfId() == KdfProfile.DIRECT) {
            throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                    "this envelope was not sealed with a password");
        }
        byte[] key = kdf.derive(password, salt, 32);
        try {
            return open(key, aad);
        } finally {
            Bytes.zero(key);
        }
    }

    /// The format version, always [#VERSION_1] for anything this build parses.
    public int getVersion() {
        return version;
    }

    /// The cipher suite, always [#SUITE_AES_256_GCM] for anything this build parses.
    public int getSuite() {
        return suite;
    }

    /// The derivation profile the envelope declares. [KdfProfile#needsUpgrade()] on the result
    /// says whether it is worth rewrapping.
    public KdfProfile getKdf() {
        return kdf;
    }

    /// Which key sealed this, so a reader holding several can pick without trying each.
    public String getKeyId() {
        return keyId;
    }

    /// The key's rotation counter at the time of sealing.
    public int getKeyVersion() {
        return keyVersion;
    }

    /// The salt, for a caller that derives the key itself rather than through
    /// [#openWithPassword].
    public byte[] getSalt() {
        return Bytes.slice(salt, 0, salt.length);
    }
}
