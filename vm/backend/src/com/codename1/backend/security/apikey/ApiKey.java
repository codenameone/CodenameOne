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
package com.codename1.backend.security.apikey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// What a server keeps about an API key: who it acts for, what it may do,
/// whether it has been revoked, and the SHA-256 by which a presented key is
/// recognized. The key itself is not here and cannot be had from this.
///
/// The prefix and the last four characters are kept for showing a person which
/// key a row is -- `cn1_...x7Qa` -- which is all of a key that should ever be
/// shown again.
public final class ApiKey {
    private final String id;
    private final String owner;
    private final List<String> scopes;
    private final String hash;
    private final String prefix;
    private final String lastFour;
    private final boolean revoked;

    /// @param id the key's own id, for naming it in a list and revoking it
    /// @param owner whom requests made with the key are from:
    /// `Authentication.getName()`
    /// @param scopes what the key grants; each becomes the authority `SCOPE_x`
    /// @param hash the lowercase hexadecimal SHA-256 of the key, as
    /// [ApiKeyGenerator#hash] computes it
    /// @param prefix the prefix the key was made with
    /// @param lastFour the key's last four characters
    /// @param revoked whether the key has been withdrawn
    public ApiKey(String id, String owner, List<String> scopes, String hash, String prefix,
                  String lastFour, boolean revoked) {
        if (id == null || owner == null || hash == null) {
            throw new IllegalArgumentException("An API key needs an id, an owner and a hash");
        }
        this.id = id;
        this.owner = owner;
        this.scopes = Collections.unmodifiableList(scopes == null ? new ArrayList<String>()
                : new ArrayList<String>(scopes));
        this.hash = hash;
        this.prefix = prefix == null ? "" : prefix;
        this.lastFour = lastFour == null ? "" : lastFour;
        this.revoked = revoked;
    }

    public String getId() {
        return id;
    }

    public String getOwner() {
        return owner;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getHash() {
        return hash;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLastFour() {
        return lastFour;
    }

    public boolean isRevoked() {
        return revoked;
    }

    /// The key as it may be shown: its prefix and its last four characters.
    public String getDisplayName() {
        return prefix + "..." + lastFour;
    }

    /// This key, withdrawn.
    public ApiKey revoke() {
        return new ApiKey(id, owner, scopes, hash, prefix, lastFour, true);
    }

    @Override
    public String toString() {
        return "ApiKey [" + id + ", " + owner + ", " + getDisplayName() + (revoked ? ", revoked" : "")
                + "]";
    }
}
