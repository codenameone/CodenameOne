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
package com.codename1.backend.sql;

import java.io.IOException;

/**
 * The ceiling on one message from a database server, shared by both wire
 * protocols because the hazard is identical.
 *
 * <p>Both clients read a LENGTH the peer chose and allocate to match, before the
 * connection is authenticated and -- with the default sslmode=prefer, or before
 * MySQL's TLS is negotiated at all -- before it is even encrypted. PostgreSQL's
 * header carries a 32-bit length, so a peer can ask for a 2GB array in one
 * message; MySQL's logical packet is assembled from 16MB continuations with no
 * limit on how many, so a peer can stream them until the process dies. Neither
 * failure is an IOException the caller can handle: an OutOfMemoryError unwinds
 * past every catch in this package and takes the backend with it.
 *
 * <p>64MB is the same figure CN1_HTTP_MAX_UPLOAD_MB uses inbound, and it is far
 * above any real message -- the largest legitimate one is a row carrying a blob
 * column, and a driver that cannot read a 64MB blob is a problem worth having
 * over a process that can be killed by a hostile DNS answer.
 */
final class SqlLimits {
    private SqlLimits() {
    }

    /** CN1_DB_MAX_MESSAGE_MB, or 64. A value outside 1..2047 is ignored. */
    static long maxMessageBytes() {
        long mb = 64;
        String text = System.getenv("CN1_DB_MAX_MESSAGE_MB");
        if(text != null && text.length() > 0 && text.length() <= 4) {
            long parsed = 0;
            boolean digits = true;
            for(int iter = 0 ; digits && iter < text.length() ; iter++) {
                char c = text.charAt(iter);
                if(c < '0' || c > '9') {
                    digits = false;
                } else {
                    parsed = parsed * 10 + (c - '0');
                }
            }
            if(digits && parsed >= 1 && parsed <= 2047) {
                mb = parsed;
            }
        }
        return mb * 1024L * 1024L;
    }

    /**
     * Refuses an sslmode this code does not implement.
     *
     * <p>FAILING CLOSED. Both engines compare the mode against "disable" and
     * "require" and treat everything else as "prefer", so "Require",
     * "required" or a plain typo asked for TLS and accepted plaintext if the
     * server declined -- the one outcome the person who wrote the word was
     * trying to prevent, reached by spelling it slightly wrong. A mode nobody
     * implements is a configuration error, and it is worth more as a refused
     * connection than as a quiet downgrade.
     *
     * <p>Null is the default and stays it: both entry points document the
     * absent mode as "prefer", and a URL without the parameter is the ordinary
     * case rather than a mistake.
     */
    static void requireSslMode(String sslMode) throws IOException {
        if(sslMode == null) {
            return;
        }
        if(!"require".equals(sslMode) && !"prefer".equals(sslMode)
                && !"disable".equals(sslMode)) {
            throw new IOException("sslmode=" + sslMode + " is not one this runtime "
                    + "implements: use require, prefer or disable. It is refused "
                    + "rather than read as prefer, which would accept plaintext");
        }
    }
}
