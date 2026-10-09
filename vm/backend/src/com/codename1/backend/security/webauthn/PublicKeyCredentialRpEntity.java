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
package com.codename1.backend.security.webauthn;

/// The relying party of a passkey: the site a credential belongs to.
///
/// The `id` is a domain name -- `example.com` -- and it is what a credential
/// is bound to: an authenticator answers only on that domain and its
/// subdomains, and on the applications the domain vouches for. It is not an
/// origin: no scheme, no port. The `name` is what the user is shown.
public final class PublicKeyCredentialRpEntity {
    private final String id;
    private final String name;

    public PublicKeyCredentialRpEntity(String id, String name) {
        if (id == null || id.length() == 0 || id.indexOf('/') >= 0 || id.indexOf(':') >= 0) {
            throw new IllegalArgumentException("A relying party id is a domain name, such as "
                    + "example.com, without a scheme, a port or a path: " + id);
        }
        this.id = id;
        this.name = name == null || name.length() == 0 ? id : name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
