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
package com.codename1.backend.security.oauth2.jwt;

import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// The header of a signature that is about to be made: which algorithm, and
/// optionally which key and what type of token.
///
/// ```java
/// JwsHeader header = JwsHeader.with(SignatureAlgorithm.ES256).type("at+jwt").build();
/// ```
public final class JwsHeader {
    private final JwsAlgorithm algorithm;
    private final Map<String, Object> headers;

    private JwsHeader(JwsAlgorithm algorithm, Map<String, Object> headers) {
        this.algorithm = algorithm;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(headers));
    }

    /// A header for a token signed with `algorithm`.
    public static Builder with(JwsAlgorithm algorithm) {
        if (algorithm == null) {
            throw new IllegalArgumentException("algorithm cannot be null");
        }
        return new Builder(algorithm);
    }

    public JwsAlgorithm getAlgorithm() {
        return algorithm;
    }

    /// `kid`, or null to sign with the first key that fits.
    public String getKeyId() {
        Object value = headers.get("kid");
        return value == null ? null : value.toString();
    }

    /// Every header, `alg` first.
    public Map<String, Object> getHeaders() {
        return headers;
    }

    /// Builds a [JwsHeader].
    public static final class Builder {
        private final JwsAlgorithm algorithm;
        private final Map<String, Object> headers = new LinkedHashMap<String, Object>();

        private Builder(JwsAlgorithm algorithm) {
            this.algorithm = algorithm;
            headers.put("alg", algorithm.getName());
        }

        /// `kid`: sign with the key of this id.
        public Builder keyId(String keyId) {
            return header("kid", keyId);
        }

        /// `typ`: `JWT`, or `at+jwt` for an access token.
        public Builder type(String type) {
            return header("typ", type);
        }

        /// Any other header.
        public Builder header(String name, Object value) {
            if (name == null || name.length() == 0 || value == null) {
                throw new IllegalArgumentException("A header needs a name and a value");
            }
            if ("alg".equals(name)) {
                throw new IllegalArgumentException("The algorithm is given to JwsHeader.with");
            }
            headers.put(name, value);
            return this;
        }

        public JwsHeader build() {
            return new JwsHeader(algorithm, headers);
        }
    }
}
