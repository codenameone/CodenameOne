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
package com.codenameone.examples.bazaar.channel;

import com.codenameone.examples.bazaar.marketing.Tokens;
import java.io.IOException;

/// `sha256=<hex>` HMAC signatures over a raw notification body, compared in
/// constant time. An empty secret verifies nothing.
final class SignedBodies {
    private SignedBodies() {
    }

    static boolean verify(String secret, byte[] body, String signature) {
        if (secret == null || secret.length() == 0 || body == null || signature == null
                || !signature.startsWith("sha256=")) {
            return false;
        }
        try {
            return Tokens.verify(secret, new String(body, "UTF-8"), signature.substring(7));
        } catch (IOException e) {
            return false;
        }
    }
}
