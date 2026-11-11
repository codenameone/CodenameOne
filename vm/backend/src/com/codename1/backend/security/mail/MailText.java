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
package com.codename1.backend.security.mail;

/// The plain mails the flows send when the application names no template.
final class MailText {
    private MailText() {
    }

    /// `url` with `token` as a parameter.
    static String link(String url, String token) {
        if (url == null || !(url.regionMatches(true, 0, "https://", 0, 8)
                || url.regionMatches(true, 0, "http://", 0, 7))) {
            throw new IllegalArgumentException("A mailed link needs a whole address, starting "
                    + "with https://: " + url);
        }
        return url + (url.indexOf('?') < 0 ? '?' : '&') + "token=" + token;
    }

    /// Three lines with the link after the second.
    static String text(String[] lines, String link) {
        return lines[0] + "\r\n\r\n" + lines[1] + "\r\n\r\n" + link + "\r\n\r\n" + lines[2]
                + "\r\n";
    }

    static String html(String[] lines, String link, String label) {
        String href = escape(link);
        return "<!DOCTYPE html>\n<html lang=\"en\">\n<body>\n<p>" + escape(lines[0]) + "</p>\n<p>"
                + escape(lines[1]) + "</p>\n<p><a href=\"" + href + "\">" + escape(label)
                + "</a></p>\n<p>" + href + "</p>\n<p>" + escape(lines[2])
                + "</p>\n</body>\n</html>\n";
    }

    static String escape(String value) {
        StringBuilder sb = new StringBuilder(value.length() + 8);
        for (int iter = 0 ; iter < value.length() ; iter++) {
            char c = value.charAt(iter);
            switch (c) {
                case '&': sb.append("&amp;"); break;
                case '<': sb.append("&lt;"); break;
                case '>': sb.append("&gt;"); break;
                case '"': sb.append("&quot;"); break;
                case '\'': sb.append("&#39;"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Notes a mail that could not be handed over; never whom it was for.
    static void notSent(String what, Exception err) {
        System.err.println("cn1: a " + what + " mail was not sent: " + err.getMessage());
    }
}
