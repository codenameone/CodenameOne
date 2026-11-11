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
package com.codename1.backend.mail;

import com.codename1.impl.backend.WireBuffer;

/// Writes each mail to the log and sends nothing: `cn1.mail.url=log:`, for
/// reading a mail's link out of a terminal during development.
///
/// It prints addresses and bodies, which no other part of the mail support
/// writes to a log. It is for a developer's own machine.
final class LogTransport extends MailTransport {
    private static final int LIMIT = 8192;

    @Override
    public String getName() {
        return "log";
    }

    @Override
    public void send(String from, String[] recipients, byte[] mime) {
        StringBuilder sb = new StringBuilder(mime.length + 128);
        sb.append("cn1: mail from ").append(from).append(" to");
        for (String r : recipients) {
            sb.append(' ').append(r);
        }
        sb.append('\n');
        if (mime.length > LIMIT) {
            byte[] head = new byte[LIMIT];
            System.arraycopy(mime, 0, head, 0, LIMIT);
            sb.append(WireBuffer.fromUtf8(head)).append("\n... ").append(mime.length - LIMIT)
                    .append(" more bytes");
        } else {
            sb.append(WireBuffer.fromUtf8(mime));
        }
        System.out.println(sb.toString());
    }

    @Override
    public void close() {
        // Holds nothing.
    }
}
