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

import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.util.AsyncResource;

/// The [KeyHandle] a vault hands out for [Vault#operationalKey(String)].
///
/// The material is a subkey derived from the vault's data key and lives in this object while the
/// vault is unlocked, which is why [#isExportable()] answers `true`: the bytes exist in the
/// process, and claiming otherwise would be the kind of overstatement this package is built to
/// avoid. What the handle guarantees is narrower and still worth having -- there is no accessor
/// that returns them, the usages are fixed, and the handle stops working the moment the vault
/// locks.
///
/// A port whose platform can hold a key the process genuinely cannot read supplies its own
/// subclass and answers `false`.
final class VaultKeyHandle extends KeyHandle {

    private final Vault owner;
    private final int generation;
    private final int keyGeneration;
    private final String purpose;
    private final int version;
    private final ProtectionReport protection;
    private byte[] material;

    private static final KeyUsage[] USAGES = {
        KeyUsage.SEAL, KeyUsage.OPEN, KeyUsage.MAC, KeyUsage.VERIFY_MAC
    };

    VaultKeyHandle(Vault owner, int generation, int keyGeneration, byte[] material, String purpose,
                   int version, ProtectionReport protection) {
        this.owner = owner;
        this.generation = generation;
        this.keyGeneration = keyGeneration;
        this.material = material;
        this.purpose = purpose == null ? "" : purpose;
        this.version = version;
        this.protection = protection;
    }

    @Override
    public String getAlgorithm() {
        return "AES-GCM";
    }

    @Override
    public String getKeyId() {
        return purpose;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public boolean isExportable() {
        return true;
    }

    @Override
    public KeyUsage[] getUsages() {
        KeyUsage[] copy = new KeyUsage[USAGES.length];
        System.arraycopy(USAGES, 0, copy, 0, USAGES.length);
        return copy;
    }

    @Override
    public ProtectionReport getProtection() {
        return protection;
    }

    @Override
    public AsyncResource<byte[]> seal(byte[] plaintext, AssociatedData aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] key = null;
        try {
            key = live();
            int at = owner.generation();
            completeBytes(out, at, SecureEnvelope.seal(key, purpose, version, aad, plaintext));
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            Bytes.zero(key);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> open(byte[] sealed, AssociatedData aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] current = null;
        try {
            SecureEnvelope envelope = SecureEnvelope.parse(sealed);
            // live() first either way: it is the liveness check, and a destroyed or locked
            // handle must answer LOCKED rather than reach into the vault for an older key.
            current = live();
            int at = owner.generation();
            if (envelope.getKeyVersion() == version) {
                completeBytes(out, at, envelope.open(current, aad));
            } else {
                // Every subkey is derived from the data key, so a rotation changes this handle's
                // material and a record sealed before it no longer opens under the live one --
                // even though the envelope records the version that sealed it and the vault
                // still holds the retired chain reaching back to it. getVersion() documents that
                // such a record stays readable, and opening only with the live key is what broke
                // that promise: an application that cached ciphertext from operationalKey lost
                // it at the first rotation.
                byte[] older = owner.subkeyAtVersion(purpose, envelope.getKeyVersion());
                try {
                    completeBytes(out, at, envelope.open(older, aad));
                } finally {
                    Bytes.zero(older);
                }
            }
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            Bytes.zero(current);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> mac(byte[] data) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        byte[] key = null;
        try {
            key = live();
            Hmac hmac = Hmac.create(Hash.SHA256, key);
            int at = owner.generation();
            completeBytes(out, at, hmac.doFinal(data == null ? new byte[0] : data));
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            Bytes.zero(key);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> verifyMac(byte[] data, byte[] tag) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        byte[] key = null;
        try {
            key = live();
            Hmac hmac = Hmac.create(Hash.SHA256, key);
            int at = owner.generation();
            byte[] computed = hmac.doFinal(data == null ? new byte[0] : data);
            boolean same = Bytes.constantTimeEquals(computed, tag);
            Bytes.zero(computed);
            completeVerification(out, at, same);
        } catch (VaultException failed) {
            out.error(failed);
        } finally {
            Bytes.zero(key);
        }
        return out;
    }

    @Override
    public synchronized void destroy() {
        Bytes.zero(material);
        material = null;
    }

    @Override
    public synchronized boolean isDestroyed() {
        return material == null || generation != owner.generation()
                || keyGeneration != owner.keyGeneration();
    }

    /// Publication shares the destruction monitor, so destruction cannot complete between the
    /// final liveness check and delivery. Crypto and storage run outside this monitor.
    private synchronized void completeBytes(AsyncResource<byte[]> out, int at, byte[] produced) {
        try {
            requireStillOurs(at);
        } catch (VaultException locked) {
            Bytes.zero(produced);
            throw locked;
        }
        out.complete(produced);
    }

    private synchronized void completeVerification(AsyncResource<Boolean> out, int at, boolean same) {
        requireStillOurs(at);
        out.complete(Boolean.valueOf(same));
    }

    private void requireStillOurs(int at) {
        if (at != owner.generation() || isDestroyed()) {
            throw new VaultException(VaultError.LOCKED,
                    "the vault was locked while this key handle was being used, so the result "
                    + "was discarded");
        }
    }

    /// The material, or a refusal. Checked on every operation rather than only at creation,
    /// because the vault can lock between the two and a handle that kept working afterwards would
    /// make [Vault#lock()] a suggestion.
    ///
    /// A rotation counts as well, and that is the less obvious half. Rotation leaves the vault
    /// open, so the lock generation does not move -- and this handle went on using the subkey
    /// derived from the superseded data key. Sealing stayed readable, because the envelope
    /// carries the version this handle was made at and [#open] walks the retired chain back to
    /// it. [#mac] has nothing to carry: its tag records no version, so one produced here after a
    /// rotation cannot be verified by any handle obtained afterwards, and nothing says so. A
    /// refusal the caller can see is the smaller failure.
    private byte[] live() {
        if (isDestroyed()) {
            throw new VaultException(VaultError.LOCKED,
                    "this key handle is no longer valid: destroy() was called, or its vault was "
                    + "locked, or the data key it derives from was rotated or replaced by a sync "
                    + "import. Ask the vault for a new handle");
        }
        owner.noteHandleUse();
        return copyMaterial();
    }

    /// Copies under the destruction monitor. An alias would let destroy() wipe a key while
    /// crypto was still consuming it. Every caller wipes its private copy in a finally block.
    private synchronized byte[] copyMaterial() {
        if (material == null) {
            throw new VaultException(VaultError.LOCKED,
                    "this key handle was destroyed while it was being used");
        }
        byte[] copy = new byte[material.length];
        System.arraycopy(material, 0, copy, 0, material.length);
        return copy;
    }
}
