/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.crypto.hd;

import com.codename1.security.Hash;
import com.codename1.security.Pbkdf2;
import com.codename1.security.SecureRandom;

/// BIP-39 mnemonic sentences: the 12 to 24 English words a wallet shows as its backup, and
/// the 64-byte seed [Bip32] and [Slip10] derive every key from.
///
/// ```java
/// String words = Mnemonic.generate(128);           // 12 words
/// byte[] seed = Mnemonic.toSeed(words, "");        // the empty passphrase
/// Bip32 account = Bip32.master(seed).derive("m/44'/60'/0'/0/0");
/// ```
///
/// Only the English wordlist is built in, which is what every wallet exports by default.
@com.codename1.impl.SharedWithBackend
public final class Mnemonic {
    /// The iteration count BIP-39 fixes for the seed.
    private static final int SEED_ITERATIONS = 2048;

    private Mnemonic() {
    }

    /// A new mnemonic over fresh entropy from [SecureRandom].
    ///
    /// #### Parameters
    ///
    /// - `strengthBits`: 128, 160, 192, 224 or 256, for 12, 15, 18, 21 or 24 words
    public static String generate(int strengthBits) {
        if (strengthBits < 128 || strengthBits > 256 || strengthBits % 32 != 0) {
            throw new IllegalArgumentException("strength is 128 to 256 bits, a multiple of 32");
        }
        byte[] entropy = SecureRandom.bytes(strengthBits / 8);
        try {
            return fromEntropy(entropy);
        } finally {
            wipe(entropy);
        }
    }

    /// The mnemonic that encodes `entropy` (16 to 32 bytes, a multiple of 4).
    public static String fromEntropy(byte[] entropy) {
        int len = entropy == null ? 0 : entropy.length;
        if (len < 16 || len > 32 || len % 4 != 0) {
            throw new IllegalArgumentException("entropy is 16 to 32 bytes, a multiple of 4");
        }
        byte[] sum = Hash.sha256(entropy);
        int bits = len * 8 + len / 4;
        String[] list = Bip39English.words();
        StringBuilder out = new StringBuilder();
        for (int word = 0; word < bits / 11; word++) {
            int index = 0;
            for (int b = 0; b < 11; b++) {
                int pos = word * 11 + b;
                int bit = pos < len * 8 ? bitAt(entropy, pos) : bitAt(sum, pos - len * 8);
                index = (index << 1) | bit;
            }
            if (word > 0) {
                out.append(' ');
            }
            out.append(list[index]);
        }
        return out.toString();
    }

    /// The entropy a mnemonic encodes, after checking every word and the checksum.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: for an unknown word, a word count other than 12, 15,
    ///   18, 21 or 24, or a checksum mismatch
    public static byte[] toEntropy(String mnemonic) {
        String[] words = split(mnemonic);
        int count = words.length;
        if (count < 12 || count > 24 || count % 3 != 0) {
            throw new IllegalArgumentException("a mnemonic has 12, 15, 18, 21 or 24 words");
        }
        int bits = count * 11;
        int checksumBits = bits / 33;
        int entropyBytes = (bits - checksumBits) / 8;
        byte[] all = new byte[(bits + 7) / 8];
        for (int word = 0; word < count; word++) {
            int index = indexOf(words[word]);
            if (index < 0) {
                throw new IllegalArgumentException("not a BIP-39 word: " + words[word]);
            }
            for (int b = 0; b < 11; b++) {
                if (((index >> (10 - b)) & 1) != 0) {
                    int pos = word * 11 + b;
                    all[pos >> 3] |= (byte) (0x80 >> (pos & 7));
                }
            }
        }
        byte[] entropy = new byte[entropyBytes];
        System.arraycopy(all, 0, entropy, 0, entropyBytes);
        byte[] sum = Hash.sha256(entropy);
        int diff = 0;
        for (int b = 0; b < checksumBits; b++) {
            diff |= bitAt(sum, b) ^ bitAt(all, entropyBytes * 8 + b);
        }
        wipe(all);
        if (diff != 0) {
            wipe(entropy);
            throw new IllegalArgumentException("mnemonic checksum mismatch");
        }
        return entropy;
    }

    /// Tells whether the text is a mnemonic with known words and a correct checksum.
    public static boolean isValid(String mnemonic) {
        try {
            wipe(toEntropy(mnemonic));
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    /// The 64-byte BIP-39 seed: PBKDF2-HMAC-SHA512 over the words, salted with
    /// `"mnemonic" + passphrase`, 2048 iterations. The words are not checked, as the
    /// specification asks; call [#isValid] first to reject a mistyped backup.
    ///
    /// BIP-39 normalizes both strings to Unicode NFKD before encoding them as UTF-8. The
    /// words always are ASCII and the device class library has no normalizer, so a
    /// passphrase outside ASCII must be given already in NFKD form -- a precomposed accent
    /// otherwise derives a different wallet than other software does.
    ///
    /// #### Parameters
    ///
    /// - `mnemonic`: the words, separated by whitespace
    ///
    /// - `passphrase`: the optional extra word ("25th word"); `null` or `""` for none
    public static byte[] toSeed(String mnemonic, String passphrase) {
        String[] words = split(mnemonic);
        StringBuilder joined = new StringBuilder();
        for (int iter = 0; iter < words.length; iter++) {
            if (iter > 0) {
                joined.append(' ');
            }
            joined.append(words[iter]);
        }
        byte[] password = utf8(joined.toString());
        byte[] salt = utf8("mnemonic" + (passphrase == null ? "" : passphrase));
        try {
            return Pbkdf2.hmacSha512(password, salt, SEED_ITERATIONS, 64);
        } finally {
            wipe(password);
            wipe(salt);
        }
    }

    /// The position of an English word in the BIP-39 list, or -1.
    public static int indexOf(String word) {
        String[] list = Bip39English.words();
        int lo = 0;
        int hi = list.length - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int c = list[mid].compareTo(word);
            if (c == 0) {
                return mid;
            }
            if (c < 0) {
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return -1;
    }

    /// The English word at `index` (0 to 2047).
    public static String word(int index) {
        return Bip39English.words()[index];
    }

    private static String[] split(String text) {
        if (text == null) {
            return new String[0];
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<String>();
        int start = -1;
        int len = text.length();
        for (int iter = 0; iter <= len; iter++) {
            boolean space = iter == len || text.charAt(iter) <= ' ' || text.charAt(iter) == '\u3000';
            if (space) {
                if (start >= 0) {
                    out.add(text.substring(start, iter));
                    start = -1;
                }
            } else if (start < 0) {
                start = iter;
            }
        }
        return out.toArray(new String[out.size()]);
    }

    private static int bitAt(byte[] data, int pos) {
        return (data[pos >> 3] >> (7 - (pos & 7))) & 1;
    }

    static byte[] utf8(String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException impossible) {
            throw new IllegalStateException("UTF-8 is always supported");
        }
    }

    static void wipe(byte[] data) {
        if (data != null) {
            for (int iter = 0; iter < data.length; iter++) {
                data[iter] = 0;
            }
        }
    }
}
