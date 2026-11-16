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
package com.codename1.impl.pdf;

import com.codename1.security.Hash;
import java.io.IOException;
import java.util.List;

/// The standard security handler: RC4 and AES-128 (revisions 2 to 4) and
/// AES-256 (revisions 5 and 6). Decryption only. A password is tried as the
/// user password, then as the owner password; the empty password opens the
/// many files that are encrypted only to carry permission flags.
@com.codename1.impl.SharedWithBackend
final class PdfSecurity {
    private static final byte[] PAD = {
        (byte) 0x28, (byte) 0xBF, (byte) 0x4E, (byte) 0x5E, (byte) 0x4E, (byte) 0x75, (byte) 0x8A, (byte) 0x41,
        (byte) 0x64, (byte) 0x00, (byte) 0x4E, (byte) 0x56, (byte) 0xFF, (byte) 0xFA, (byte) 0x01, (byte) 0x08,
        (byte) 0x2E, (byte) 0x2E, (byte) 0x00, (byte) 0xB6, (byte) 0xD0, (byte) 0x68, (byte) 0x3E, (byte) 0x80,
        (byte) 0x2F, (byte) 0x0C, (byte) 0xA9, (byte) 0xFE, (byte) 0x64, (byte) 0x53, (byte) 0x69, (byte) 0x7A};

    private byte[] key;
    private int revision;
    private boolean aesStrings;
    private boolean aesStreams;
    private boolean identityStrings;
    private boolean identityStreams;

    PdfSecurity(PdfDict enc, byte[] id0, String password) throws IOException {
        if (!"Standard".equals(enc.getName("Filter"))) {
            throw new IOException("Unsupported security handler " + enc.getName("Filter"));
        }
        int v = enc.getInt("V", 0);
        revision = enc.getInt("R", 2);
        int length = enc.getInt("Length", 40) / 8;
        if (v == 1 || length < 5) {
            length = 5;
        }
        if (v >= 4) {
            String stmF = enc.getName("StmF");
            String strF = enc.getName("StrF");
            String streamMethod = method(enc, stmF);
            String stringMethod = method(enc, strF);
            identityStreams = stmF == null || "Identity".equals(stmF);
            identityStrings = strF == null || "Identity".equals(strF);
            aesStreams = streamMethod.startsWith("AES");
            aesStrings = stringMethod.startsWith("AES");
            if (v == 4) {
                length = 16;
            }
        }
        byte[] o = bytes(enc.get("O"));
        byte[] u = bytes(enc.get("U"));
        byte[] pw = password == null ? new byte[0] : utf8(password);
        if (revision >= 5 && pw.length > 127) {
            // ISO 32000-2 7.6.4.3.3: an R5/R6 password is its first 127
            // UTF-8 bytes. SASLprep is not applied, so a password that
            // normalizes differently (rare outside some scripts) must be
            // typed in the form the author used.
            pw = sub(pw, 0, 127);
        }
        if (revision >= 5) {
            key = aes256Key(pw, o, u, bytes(enc.get("OE")), bytes(enc.get("UE")));
        } else {
            // Writers disagree on the sign of /P; both spellings name the same
            // 32 bits, and a double's intValue() would clamp the unsigned one.
            int p = (int) (long) enc.getNumber("P", 0);
            boolean meta = !Boolean.FALSE.equals(enc.get("EncryptMetadata"));
            byte[] k = userKey(pad(pw), o, p, id0, length, meta);
            if (!checkUser(k, u, id0)) {
                byte[] user = ownerToUser(pw, o, length);
                k = userKey(user, o, p, id0, length, meta);
                if (!checkUser(k, u, id0)) {
                    k = null;
                }
            }
            key = k;
        }
        if (key == null) {
            throw new IOException("Incorrect password");
        }
    }

    private static String method(PdfDict enc, String filter) {
        PdfDict cf = enc.getDict("CF");
        PdfDict f = cf == null || filter == null ? null : cf.getDict(filter);
        String m = f == null ? null : f.getName("CFM");
        return m == null ? "V2" : m;
    }

    private static byte[] bytes(Object o) {
        return o instanceof PdfString ? ((PdfString) o).bytes : new byte[0];
    }

    private static byte[] utf8(String s) {
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c < 0x80) {
                bo.write(c);
            } else if (c < 0x800) {
                bo.write(0xc0 | (c >> 6));
                bo.write(0x80 | (c & 0x3f));
            } else {
                bo.write(0xe0 | (c >> 12));
                bo.write(0x80 | ((c >> 6) & 0x3f));
                bo.write(0x80 | (c & 0x3f));
            }
        }
        byte[] b = bo.toByteArray();
        if (b.length > 127) {
            byte[] t = new byte[127];
            System.arraycopy(b, 0, t, 0, 127);
            return t;
        }
        return b;
    }

    private static byte[] pad(byte[] pw) {
        byte[] out = new byte[32];
        int n = Math.min(32, pw.length);
        System.arraycopy(pw, 0, out, 0, n);
        System.arraycopy(PAD, 0, out, n, 32 - n);
        return out;
    }

    private byte[] userKey(byte[] padded, byte[] o, int p, byte[] id0, int length, boolean meta) {
        Hash md5 = Hash.create(Hash.MD5);
        md5.update(padded);
        md5.update(o.length > 32 ? sub(o, 0, 32) : o);
        md5.update(new byte[] {(byte) p, (byte) (p >> 8), (byte) (p >> 16), (byte) (p >> 24)});
        md5.update(id0);
        if (revision >= 4 && !meta) {
            md5.update(new byte[] {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff});
        }
        byte[] h = md5.digest();
        if (revision >= 3) {
            for (int i = 0; i < 50; i++) {
                h = Hash.md5(sub(h, 0, length));
            }
        }
        return sub(h, 0, length);
    }

    private boolean checkUser(byte[] k, byte[] u, byte[] id0) {
        if (revision == 2) {
            return startsWith(rc4(k, PAD), u, 32);
        }
        Hash md5 = Hash.create(Hash.MD5);
        md5.update(PAD);
        md5.update(id0);
        byte[] h = md5.digest();
        for (int i = 0; i < 20; i++) {
            h = rc4(xor(k, i), h);
        }
        return startsWith(h, u, 16);
    }

    private byte[] ownerToUser(byte[] pw, byte[] o, int length) {
        byte[] h = Hash.md5(pad(pw));
        if (revision >= 3) {
            for (int i = 0; i < 50; i++) {
                h = Hash.md5(h);
            }
        }
        byte[] k = sub(h, 0, length);
        byte[] user = o.length > 32 ? sub(o, 0, 32) : o;
        if (revision == 2) {
            return rc4(k, user);
        }
        for (int i = 19; i >= 0; i--) {
            user = rc4(xor(k, i), user);
        }
        return user;
    }

    private byte[] aes256Key(byte[] pw, byte[] o, byte[] u, byte[] oe, byte[] ue) {
        if (o.length < 48 || u.length < 48) {
            return null;
        }
        byte[] u48 = sub(u, 0, 48);
        if (startsWith(hash(pw, sub(o, 32, 8), u48), o, 32)) {
            return unwrap(hash(pw, sub(o, 40, 8), u48), oe);
        }
        if (startsWith(hash(pw, sub(u, 32, 8), new byte[0]), u, 32)) {
            return unwrap(hash(pw, sub(u, 40, 8), new byte[0]), ue);
        }
        return null;
    }

    private static byte[] unwrap(byte[] k, byte[] wrapped) {
        if (wrapped.length < 32) {
            return null;
        }
        return new PdfAes(k).decryptCbc(new byte[16], wrapped, 0, 32, false);
    }

    /// Algorithm 2.A/2.B: SHA-256 for revision 5, the iterated hash for 6.
    private byte[] hash(byte[] pw, byte[] salt, byte[] udata) {
        byte[] k = Hash.sha256(concat(pw, salt, udata));
        if (revision < 6) {
            return k;
        }
        int round = 0;
        while (true) {
            byte[] k1 = concat(pw, k, udata);
            byte[] rep = new byte[k1.length * 64];
            for (int i = 0; i < 64; i++) {
                System.arraycopy(k1, 0, rep, i * k1.length, k1.length);
            }
            byte[] e = new PdfAes(sub(k, 0, 16)).encryptCbc(sub(k, 16, 16), rep);
            int mod = 0;
            for (int i = 0; i < 16; i++) {
                mod += e[i] & 0xff;
            }
            mod %= 3;
            k = mod == 0 ? Hash.sha256(e) : mod == 1 ? Hash.sha384(e) : Hash.sha512(e);
            round++;
            if (round >= 64 && (e[e.length - 1] & 0xff) <= round - 32) {
                break;
            }
        }
        return sub(k, 0, 32);
    }

    /// Decrypts a string of object `num`.
    byte[] decryptString(byte[] data, int num, int gen) {
        if (identityStrings && revision >= 4) {
            return data;
        }
        return decrypt(data, num, gen, aesStrings);
    }

    /// Decrypts the data of a stream of object `num`, unless it names the
    /// Identity crypt filter.
    byte[] decryptStream(PdfStream s, byte[] data, int num, int gen) {
        if (identityStreams && revision >= 4) {
            return data;
        }
        List<String> names = PdfFilters.names(s);
        if (!names.isEmpty() && "Crypt".equals(names.get(0))) {
            return data;
        }
        return decrypt(data, num, gen, aesStreams);
    }

    private byte[] decrypt(byte[] data, int num, int gen, boolean aes) {
        byte[] k;
        if (revision >= 5) {
            k = key;
        } else {
            Hash md5 = Hash.create(Hash.MD5);
            md5.update(key);
            md5.update(new byte[] {(byte) num, (byte) (num >> 8), (byte) (num >> 16), (byte) gen,
                    (byte) (gen >> 8)});
            if (aes) {
                md5.update(new byte[] {'s', 'A', 'l', 'T'});
            }
            k = sub(md5.digest(), 0, Math.min(16, key.length + 5));
        }
        if (!aes) {
            return rc4(k, data);
        }
        if (data.length < 32) {
            // The IV and one padded block are the smallest AES ciphertext,
            // even for an empty string, so anything shorter decrypts to
            // nothing; that is what other readers show for it too.
            return new byte[0];
        }
        return new PdfAes(k).decryptCbc(sub(data, 0, 16), data, 16, data.length - 16, true);
    }

    static byte[] rc4(byte[] key, byte[] data) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xff)) & 0xff;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        byte[] out = new byte[data.length];
        int i = 0;
        j = 0;
        for (int n = 0; n < data.length; n++) {
            i = (i + 1) & 0xff;
            j = (j + s[i]) & 0xff;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
            out[n] = (byte) (data[n] ^ s[(s[i] + s[j]) & 0xff]);
        }
        return out;
    }

    private static byte[] xor(byte[] k, int v) {
        byte[] out = new byte[k.length];
        for (int i = 0; i < k.length; i++) {
            out[i] = (byte) (k[i] ^ v);
        }
        return out;
    }

    private static boolean startsWith(byte[] a, byte[] b, int n) {
        if (a.length < n || b.length < n) {
            return false;
        }
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] sub(byte[] b, int off, int len) {
        byte[] out = new byte[Math.max(0, Math.min(len, b.length - off))];
        System.arraycopy(b, off, out, 0, out.length);
        return out;
    }

    private static byte[] concat(byte[] a, byte[] b, byte[] c) {
        byte[] out = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        System.arraycopy(c, 0, out, a.length + b.length, c.length);
        return out;
    }
}
