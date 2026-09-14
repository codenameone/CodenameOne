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
    private final String purpose;
    private final int version;
    private final ProtectionReport protection;
    private byte[] material;

    private static final KeyUsage[] USAGES = {
        KeyUsage.SEAL, KeyUsage.OPEN, KeyUsage.MAC, KeyUsage.VERIFY_MAC
    };

    VaultKeyHandle(Vault owner, int generation, byte[] material, String purpose, int version,
                   ProtectionReport protection) {
        this.owner = owner;
        this.generation = generation;
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
        try {
            out.complete(SecureEnvelope.seal(live(), purpose, version, aad, plaintext));
        } catch (VaultException failed) {
            out.error(failed);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> open(byte[] sealed, AssociatedData aad) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        try {
            out.complete(SecureEnvelope.parse(sealed).open(live(), aad));
        } catch (VaultException failed) {
            out.error(failed);
        }
        return out;
    }

    @Override
    public AsyncResource<byte[]> mac(byte[] data) {
        AsyncResource<byte[]> out = new AsyncResource<byte[]>();
        try {
            Hmac hmac = Hmac.create(Hash.SHA256, live());
            out.complete(hmac.doFinal(data == null ? new byte[0] : data));
        } catch (VaultException failed) {
            out.error(failed);
        }
        return out;
    }

    @Override
    public AsyncResource<Boolean> verifyMac(byte[] data, byte[] tag) {
        AsyncResource<Boolean> out = new AsyncResource<Boolean>();
        try {
            Hmac hmac = Hmac.create(Hash.SHA256, live());
            byte[] computed = hmac.doFinal(data == null ? new byte[0] : data);
            boolean same = Bytes.constantTimeEquals(computed, tag);
            Bytes.zero(computed);
            out.complete(Boolean.valueOf(same));
        } catch (VaultException failed) {
            out.error(failed);
        }
        return out;
    }

    @Override
    public void destroy() {
        Bytes.zero(material);
        material = null;
    }

    @Override
    public boolean isDestroyed() {
        return material == null || generation != owner.generation();
    }

    /// The material, or a refusal. Checked on every operation rather than only at creation,
    /// because the vault can lock between the two and a handle that kept working afterwards would
    /// make [Vault#lock()] a suggestion.
    private byte[] live() {
        if (isDestroyed()) {
            throw new VaultException(VaultError.LOCKED,
                    "this key handle was invalidated, either by destroy() or because its vault "
                    + "was locked");
        }
        return material;
    }
}
