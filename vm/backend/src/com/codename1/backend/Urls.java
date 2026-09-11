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
package com.codename1.backend;

import java.io.IOException;

/**
 * The scheme rule for an OUTBOUND request, shared by both Web implementations.
 *
 * <p>Web documents an HTTP client. The packaged arm hands its URL to libcurl,
 * which speaks whatever its build enabled -- and the shipped builds enable
 * file://, so "file:///etc/passwd" read the file into the handler-visible
 * response body. Measured before this existed: file:///etc/hosts came back as
 * 684 bytes of it. Any application that passes a caller-supplied URL to
 * Web.request -- a webhook target, an avatar URL, a callback -- therefore had a
 * local file disclosure in it.
 *
 * <p>Checked HERE rather than left to each arm, for the usual reason: the Java SE
 * arm would have failed this by ClassCastException out of the HttpURLConnection
 * cast, which is a different error, and on a future arm might be no error at all.
 * libcurl is ALSO told to accept only http and https, on the initial request and
 * on redirects, so a URL this misses cannot become a file read further down.
 */
final class Urls {
    private Urls() {
    }

    /**
     * @throws IOException unless {@code url} names http or https. The scheme is
     *                     compared without folding case, which is what a scheme
     *                     is: RFC 3986 says it is case insensitive, and
     *                     toLowerCase would be the locale-sensitive way to get
     *                     that wrong.
     */
    static void requireHttp(String url) throws IOException {
        if(url == null) {
            throw new IOException("No URL");
        }
        int colon = url.indexOf(':');
        if(colon < 1
                || !(url.regionMatches(true, 0, "http:", 0, 5)
                     || url.regionMatches(true, 0, "https:", 0, 6))) {
            // The SCHEME only. The rest of a URL can carry credentials in its
            // userinfo, and this message goes wherever the caller logs it.
            throw new IOException("Web speaks http and https, not "
                    + (colon < 1 ? "that" : url.substring(0, colon)));
        }
    }
}
