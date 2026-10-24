/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.account;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.Base64Url;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codenameone.examples.bastion.api.PasskeyDto;
import com.codenameone.examples.bastion.audit.Audit;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// The passkeys on an account, for the app's Settings screen.
///
/// Adding one stays a WebAuthn ceremony (`/webauthn/register`, on the
/// signed-in session). Listing and removing need only the caller's token:
/// removing a passkey takes nothing away the password and second factor do
/// not still guard, so a stolen token can at worst make its owner sign in the
/// longer way -- and every removal is in the audit log.
@Component
public class PasskeyList {
    private final UserCredentialRepository credentials;
    private final PublicKeyCredentialUserEntityRepository holders;
    private final Audit audit;

    public PasskeyList(UserCredentialRepository credentials,
            PublicKeyCredentialUserEntityRepository holders, Audit audit) {
        this.credentials = credentials;
        this.holders = holders;
        this.audit = audit;
    }

    /// `username`'s passkeys, oldest first.
    public List<PasskeyDto> of(String username) {
        List<PasskeyDto> out = new ArrayList<PasskeyDto>();
        PublicKeyCredentialUserEntity holder = holders.findByUsername(username);
        if (holder == null) {
            return out;
        }
        for (CredentialRecord r : credentials.findByUserId(holder.getId())) {
            out.add(new PasskeyDto(Base64Url.encode(r.getCredentialId()), r.getLabel(),
                    r.getCreated(), r.getLastUsed()));
        }
        return out;
    }

    /// Removes `username`'s passkey `id`; 404 for one that is not theirs, so
    /// another account's ids cannot be probed.
    public List<PasskeyDto> remove(String username, String id) throws IOException {
        byte[] credentialId = decode(id);
        PublicKeyCredentialUserEntity holder = holders.findByUsername(username);
        CredentialRecord record = credentialId == null || holder == null
                ? null : credentials.findByCredentialId(credentialId);
        if (record == null || !Arrays.equals(record.getUserEntityUserId(), holder.getId())) {
            throw Refused.with(404, "no-such-passkey", "No such passkey");
        }
        credentials.delete(credentialId);
        audit.append(username, "PASSKEY_REMOVED", username, record.getLabel());
        return of(username);
    }

    private static byte[] decode(String id) {
        try {
            return id == null ? null : Base64Url.decode(id);
        } catch (RuntimeException notBase64) {
            return null;
        }
    }
}
