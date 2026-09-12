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
        // AND NO CONTROL CHARACTER, which is not a matter of tidiness. The
        // translated arm hands this string to libcurl as a C string --
        // stringToUTF8 encodes through String.getBytes("UTF-8"), so a U+0000 is
        // one 0x00 byte and everything after it is GONE. "http://127.0.0.1\u0000
        // .example.com/" is a request to 127.0.0.1 that an application checking
        // the .example.com suffix has already approved, and the JavaSE arm
        // refuses the same URL, so the two arms disagreed about where a request
        // was even going. CR, LF, TAB and DEL are refused with it: none of them
        // is legal in a URL, and each is a way to mean two things at once to
        // whatever parses it next.
        for(int iter = 0 ; iter < url.length() ; iter++) {
            char c = url.charAt(iter);
            if(c <= 0x20 || c == 0x7f) {
                throw new IOException("A URL cannot hold a control character or a "
                        + "space; this one does, at index " + iter
                        + ". Percent-encode it");
            }
        }
    }

    /**
     * A URL with everything secret taken out of it, for a message a caller will
     * log.
     *
     * Userinfo goes, and so does the WHOLE query -- not the parameters whose
     * names look sensitive. That list is never finished (X-Amz-Signature,
     * X-Amz-Credential, access_token, sig, key, token, password) and the case
     * that matters most here is a presigned S3 URL, whose signature IS the
     * credential. Dropping the query wholesale and saying so is the only version
     * of this that cannot be wrong about a name nobody thought of.
     *
     * <p>The host and path stay, because an error naming no endpoint at all is
     * not worth logging. requireHttp above prints only the scheme for the same
     * reason, and Database.Url.describe does the same thing for a database URL.
     *
     * <p>AND THE FRAGMENT, for the same reason as the query: an implicit-flow
     * OAuth token arrives as "#access_token=...", which is a credential in the
     * one part of a URL that never even reaches the server. Cutting at the query
     * alone left it in the message verbatim.
     *
     * <p>indexOf with a String and not a char: vm/JavaAPI has indexOf(String,
     * int) and no indexOf(int, int), so the char form compiles on the JavaSE arm
     * and fails to link in a translated build.
     */
    static String forMessage(String url) {
        if(url == null) {
            return "a request with no URL";
        }
        // Whichever comes first. A fragment starts at the first '#' and runs to
        // the end, so a '#' before the '?' means there is no query at all.
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        char marker = '?';
        if(fragment >= 0 && (query < 0 || fragment < query)) {
            query = fragment;
            marker = '#';
        }
        String out = query < 0 ? url : url.substring(0, query);
        int scheme = out.indexOf("://");
        if(scheme >= 0) {
            int at = out.indexOf("@", scheme + 3);
            int slash = out.indexOf("/", scheme + 3);
            // A '@' before the authority ends is userinfo; one after it is an
            // ordinary path character and has nothing to do with credentials.
            if(at >= 0 && (slash < 0 || at < slash)) {
                out = out.substring(0, scheme + 3) + "<redacted>@"
                        + out.substring(at + 1);
            }
        }
        return query < 0 ? out : out + marker + "<redacted>";
    }
}
