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

/// The non-secret record describing one vault: which data key it uses, and the wrapped copies of
/// that key that can be unwrapped back into it.
///
/// Everything here is safe to store in ordinary storage and safe to send to a sync server. The
/// wraps are [SecureEnvelope]s of the data key -- ciphertext under a password-derived key or a
/// recovery-code-derived key -- so a server holding this can neither read the vault nor help
/// anyone else to. The device wrap is deliberately **not** here: it is local to one device, must
/// never sync, and is kept in a separate record.
///
/// #### Wire format
///
/// A text record, because it has to be readable by a person debugging a sync problem and by an
/// implementation in another language, and because it is small enough that the density of a
/// binary format buys nothing. Lines are `key=value`, values are lower case hex or decimal, and
/// the first line is a magic that a reader checks before anything else.
///
/// ```text
/// CN1VAULT1
/// vault=<32 hex characters, the vault's random id>
/// key.id=<name of the data key>
/// key.version=<decimal>
/// counter=<decimal, monotonic>
/// wrap.password=<hex envelope>
/// wrap.recovery=<hex envelope, optional>
/// retired.<n>=<hex envelope of data key version n, sealed under data key version n+1>
/// ```
///
/// The `retired` lines are what makes rotation a real operation rather than a rename. Rotating
/// generates a new data key and seals the outgoing one **under the new one**, so an envelope
/// written months ago still opens: [Vault#open] reads the key version out of the envelope and
/// walks the chain down from the current key one link at a time. Rewriting every record at
/// rotation time is the alternative, and on a device holding a synced database it is not
/// something that can be done atomically.
///
/// A chain rather than a flat set: rotating again re-seals only the outgoing key, so the cost of
/// a rotation does not grow with the number of rotations already performed. Opening an old record
/// costs one unwrap per rotation since it was written, which is the right place to pay.
///
/// Unknown lines are preserved on read and written back out. That is what lets an older client
/// round-trip a record a newer one wrote without deleting the field it did not understand -- the
/// alternative is a sync loop where two versions of an application take turns destroying each
/// other's data.
final class VaultMetadata {

    static final String MAGIC = "CN1VAULT1";

    String vaultId;
    String dataKeyId = "dk";
    int dataKeyVersion = 1;
    long counter;
    byte[] passwordWrap;
    byte[] recoveryWrap;

    /// Authentication tag over [#serializeForMac()], keyed by this record's own data key.
    ///
    /// Keyed by the data key and not by anything derived from the password directly, because the
    /// key is what a reader recovers by opening the password wrap -- so a reader can verify this
    /// exactly when it is entitled to, and an attacker who can edit the record cannot recompute
    /// it without the password.
    byte[] mac;

    /// Version to envelope, each holding data key version n sealed under version n+1.
    final java.util.Hashtable<Integer, byte[]> retired = new java.util.Hashtable<Integer, byte[]>();

    /// Fingerprints of the records this one descends from, oldest first.
    ///
    /// Each is the first [#FINGERPRINT_BYTES] bytes of that record's own tag, in hex. This is what
    /// makes ancestry decidable: the counter counts mutations on whichever device made them, so
    /// two devices that diverge from the same base both raise it and neither is newer, and no
    /// comparison of two integers can tell that apart from one device simply being ahead. A list
    /// of what a record came from can.
    ///
    /// Bounded at [#MAX_ANCESTORS], so a record cannot grow without limit. Past that the oldest
    /// entries are dropped and a device more than that many changes behind can no longer be shown
    /// to be an ancestor -- the import is refused rather than guessed at, which is the safe
    /// direction: the application resolves it, and nothing is silently overwritten.
    final java.util.ArrayList<String> ancestors = new java.util.ArrayList<String>();

    /// How much of a record's tag identifies it. Eight bytes is far past any accidental collision
    /// for a list this short, and a deliberate one is not available: the list is inside the tagged
    /// body, so it cannot be edited without the data key.
    static final int FINGERPRINT_BYTES = 8;

    /// How many generations of ancestry a record carries.
    static final int MAX_ANCESTORS = 64;

    /// Lines this build did not recognise, kept so they survive a round trip.
    String unknown = "";

    /// A detached copy, for building the next state without touching the one in use.
    ///
    /// The wrap arrays are shared with the original rather than cloned, and that is safe for one
    /// reason only: every write in this package **replaces** a wrap, never edits one in place. No
    /// caller zeroes `passwordWrap`, `recoveryWrap` or a retired envelope -- they are ciphertext,
    /// not key material, so there is nothing in them worth clearing. A change that started editing
    /// a wrap's bytes would silently reach through every copy ever taken, so clone there rather
    /// than here if that day comes.
    ///
    /// Mutating the live record in place and writing it afterwards means a failed write leaves the
    /// vault holding a record it never persisted -- and, during a rotation, one whose key version
    /// has advanced past the key actually in hand. Everything sealed after that is labelled with a
    /// version it was not encrypted under.
    VaultMetadata copy() {
        VaultMetadata out = new VaultMetadata();
        out.vaultId = vaultId;
        out.dataKeyId = dataKeyId;
        out.dataKeyVersion = dataKeyVersion;
        out.counter = counter;
        out.passwordWrap = passwordWrap;
        out.recoveryWrap = recoveryWrap;
        out.retired.putAll(retired);
        out.unknown = unknown;
        out.mac = mac;
        out.ancestors.addAll(ancestors);
        return out;
    }

    /// The record as it is authenticated: everything except the tag itself.
    ///
    /// Everything else in here is either ciphertext or is covered by a wrap's associated data --
    /// the counter is neither. It is plaintext an attacker who serves sync state can edit, and
    /// raising it on an OLD record makes that record look newer while its password wrap still
    /// opens, so the newer key and retired chain are overwritten and everything sealed since
    /// becomes unreadable. A tag over these bytes is what makes the counter unforgeable.
    String serializeForMac() {
        return serializeBody();
    }

    String serialize() {
        StringBuilder b = new StringBuilder(serializeBody());
        if (mac != null) {
            b.append("mac=").append(Bytes.toHex(mac)).append('\n');
        }
        return b.toString();
    }

    private String serializeBody() {
        StringBuilder b = new StringBuilder();
        b.append(MAGIC).append('\n');
        b.append("vault=").append(vaultId).append('\n');
        b.append("key.id=").append(dataKeyId).append('\n');
        b.append("key.version=").append(dataKeyVersion).append('\n');
        b.append("counter=").append(counter).append('\n');
        if (passwordWrap != null) {
            b.append("wrap.password=").append(Bytes.toHex(passwordWrap)).append('\n');
        }
        if (recoveryWrap != null) {
            b.append("wrap.recovery=").append(Bytes.toHex(recoveryWrap)).append('\n');
        }
        // Ascending, so the record is stable and two clients that hold the same state serialize
        // to the same bytes -- which is what lets a sync layer compare them at all.
        for (int version = 1; version < dataKeyVersion; version++) {
            byte[] envelope = retired.get(Integer.valueOf(version));
            if (envelope != null) {
                b.append("retired.").append(version).append('=')
                        .append(Bytes.toHex(envelope)).append('\n');
            }
        }
        if (!ancestors.isEmpty()) {
            b.append("ancestors=");
            for (int iter = 0; iter < ancestors.size(); iter++) {
                if (iter > 0) {
                    b.append(',');
                }
                b.append(ancestors.get(iter));
            }
            b.append('\n');
        }
        b.append(unknown);
        return b.toString();
    }

    static VaultMetadata parse(String text) {
        if (text == null) {
            return null;
        }
        VaultMetadata out = new VaultMetadata();
        StringBuilder unknown = new StringBuilder();
        int at = 0;
        boolean first = true;
        while (at <= text.length()) {
            int end = text.indexOf('\n', at);
            if (end < 0) {
                end = text.length();
            }
            String line = text.substring(at, end);
            at = end + 1;
            if (first) {
                first = false;
                if (!MAGIC.equals(line)) {
                    throw new VaultException(VaultError.UNSUPPORTED_FORMAT,
                            "vault record is not in a format this build understands");
                }
                continue;
            }
            if (line.length() == 0) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq < 0) {
                throw new VaultException(VaultError.CORRUPT, "malformed line in the vault record");
            }
            String key = line.substring(0, eq);
            String value = line.substring(eq + 1);
            if ("vault".equals(key)) {
                out.vaultId = value;
            } else if ("key.id".equals(key)) {
                out.dataKeyId = value;
            } else if ("key.version".equals(key)) {
                out.dataKeyVersion = parseInt(value);
            } else if ("mac".equals(key)) {
                out.mac = Bytes.fromHex(value);
            } else if ("counter".equals(key)) {
                out.counter = parseLong(value);
            } else if ("wrap.password".equals(key)) {
                out.passwordWrap = hex(value);
            } else if ("wrap.recovery".equals(key)) {
                out.recoveryWrap = hex(value);
            } else if ("ancestors".equals(key)) {
                int from = 0;
                while (from < value.length()) {
                    int comma = value.indexOf(',', from);
                    if (comma < 0) {
                        comma = value.length();
                    }
                    String one = value.substring(from, comma);
                    if (one.length() > 0) {
                        out.ancestors.add(one);
                    }
                    from = comma + 1;
                }
            } else if (key.startsWith("retired.")) {
                out.retired.put(Integer.valueOf(parseInt(key.substring(8))), hex(value));
            } else {
                // Kept rather than dropped: see the class note on round-tripping.
                unknown.append(line).append('\n');
            }
        }
        out.unknown = unknown.toString();
        if (out.vaultId == null || out.vaultId.length() == 0) {
            throw new VaultException(VaultError.CORRUPT, "vault record has no vault id");
        }
        return out;
    }

    private static byte[] hex(String value) {
        byte[] out = Bytes.fromHex(value);
        if (out == null) {
            throw new VaultException(VaultError.CORRUPT, "vault record holds a malformed wrap");
        }
        return out;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException bad) {
            throw new VaultException(VaultError.CORRUPT,
                    "vault record holds a malformed number", bad);
        }
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException bad) {
            throw new VaultException(VaultError.CORRUPT,
                    "vault record holds a malformed number", bad);
        }
    }
}
