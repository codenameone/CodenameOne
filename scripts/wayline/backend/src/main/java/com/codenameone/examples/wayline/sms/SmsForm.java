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
package com.codenameone.examples.wayline.sms;

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
