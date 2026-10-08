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
package com.codename1.backend.security.crypto;

import com.codename1.backend.Json;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A set of keys, as a JSON Web Key Set publishes one (RFC 7517 5): what a
/// server that signs tokens serves at its `jwks_uri`, and what a server that
/// verifies them reads from there.
///
/// ```java
/// JwkSet keys = JwkSet.of(current, previous);
/// return HttpServer.Response.json(200, keys.toJson());   // public halves only
/// ```
///
/// The set does not change. A server that rotates its key builds a new set and
/// answers with it from its [JwkSource].
public final class JwkSet implements JwkSource {
    private final List<Jwk> keys;

    public JwkSet(List<Jwk> keys) {
        List<Jwk> copy = new ArrayList<Jwk>();
        if (keys != null) {
            for (Jwk key : keys) {
                if (key == null) {
                    throw new IllegalArgumentException("A key set cannot hold a null key");
                }
                copy.add(key);
            }
        }
        this.keys = Collections.unmodifiableList(copy);
    }

    /// A set of these keys, the one to sign with first.
    public static JwkSet of(Jwk... keys) {
        List<Jwk> list = new ArrayList<Jwk>();
        if (keys != null) {
            for (Jwk key : keys) {
                list.add(key);
            }
        }
        return new JwkSet(list);
    }

    /// The public keys in a JSON Web Key Set document.
    ///
    /// A key this runtime has no use for -- a type or a curve it does not
    /// verify with, an encryption key -- is left out rather than failing the
    /// set: a provider adding a key of a new kind must not stop the ones that
    /// were working.
    ///
    /// @throws IOException when the text is not a key set at all
    @SuppressWarnings("unchecked")
    public static JwkSet parse(String json) throws IOException {
        Map document;
        try {
            document = Json.parseObject(json);
        } catch (RuntimeException malformed) {
            throw new IOException("Not a JSON Web Key Set: " + malformed.getMessage(), malformed);
        }
        Object listed = document.get("keys");
        if (!(listed instanceof List)) {
            throw new IOException("Not a JSON Web Key Set: it has no \"keys\" array");
        }
        List<Jwk> keys = new ArrayList<Jwk>();
        for (Object entry : (List) listed) {
            if (!(entry instanceof Map)) {
                continue;
            }
            Map<String, Object> key = (Map<String, Object>) entry;
            if ("enc".equals(key.get("use"))) {
                continue;
            }
            try {
                keys.add(Jwk.parse(key));
            } catch (IOException unusable) {
                continue; //NOPMD - see the method's comment: not ours to verify with
            }
        }
        return new JwkSet(keys);
    }

    @Override
    public List<Jwk> getKeys() {
        return keys;
    }

    /// The key with this id, or null.
    public Jwk get(String keyId) {
        for (Jwk key : keys) {
            if (keyId != null && keyId.equals(key.getKeyId())) {
                return key;
            }
        }
        return null;
    }

    /// The set as a `jwks_uri` serves it: the public half of every RSA and EC
    /// key. A shared secret is left out.
    public Map<String, Object> toPublicJson() {
        List<Object> published = new ArrayList<Object>();
        for (Jwk key : keys) {
            if (key.getPublicKey() != null) {
                published.add(key.toPublicJson());
            }
        }
        Map<String, Object> document = new LinkedHashMap<String, Object>();
        document.put("keys", published);
        return document;
    }

    /// [#toPublicJson] as text.
    public String toJson() {
        return Json.write(toPublicJson());
    }

    @Override
    public String toString() {
        return "JwkSet " + keys;
    }
}
