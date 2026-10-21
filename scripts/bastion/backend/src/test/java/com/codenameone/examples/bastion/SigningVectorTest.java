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
package com.codenameone.examples.bastion;

import com.codenameone.examples.bastion.api.SignedRequest;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The canonical text the server verifies, for the vector the app's
/// `RequestSignerTest` signs: the same request must give the same text on
/// both sides, byte for byte.
class SigningVectorTest {
    @Test
    void canonicalForm() throws Exception {
        String body = "{\"amount\":1500,\"memo\":\"rent\"}";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(body.getBytes("UTF-8"));
        assertEquals("POST|/api/accounts/a1/withdraw|"
                + "27faf49a231437b9d9308db1799c1ab06d8665c087da17478d59acbefa20a125|"
                + "AAECAwQFBgcICQoLDA0ODw|1767225600000",
                SignedRequest.canonical("POST", "/api/accounts/a1/withdraw", digest,
                        "AAECAwQFBgcICQoLDA0ODw", 1767225600000L));
        assertEquals("dev1", SignedRequest.field(
                "v1;device=dev1;ts=1;nonce=n;sig=s", "device"));
    }
}
