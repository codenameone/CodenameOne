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
package com.codenameone.examples.pulse.sms;

import java.io.IOException;
import java.net.URLEncoder;

/// Builds an `application/x-www-form-urlencoded` body, which is what all three
/// providers' send calls take.
final class SmsForm {
    private SmsForm() {
    }

    /// Names and values in turn, each escaped as UTF-8.
    static byte[] of(String... pairs) throws IOException {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter + 1 < pairs.length; iter += 2) {
            if (out.length() > 0) {
                out.append('&');
            }
            out.append(URLEncoder.encode(pairs[iter], "UTF-8")).append('=')
                    .append(URLEncoder.encode(pairs[iter + 1], "UTF-8"));
        }
        return out.toString().getBytes("UTF-8");
    }
}
