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

package com.codename1.backend.aws;

/**
 * Reaches Credentials.expiryMillis, which is package private and staying that
 * way.
 *
 * <p>The parser is worth testing directly -- an expiry that reads LATER than the
 * provider meant is the failure that matters, and driving it through
 * Credentials.resolve() would mean a metadata endpoint, which is chosen by an
 * environment variable a process cannot set for itself. So the test sits in the
 * package instead, which is what package-private access is for.
 *
 * <p>This file lives in the SELF-TEST tree, not in the product: it is compiled
 * into the test binary and ships nowhere. Making expiryMillis public to reach it
 * would widen the API of a class whose whole surface is deliberately small.
 */
public final class ExpiryProbe {
    private ExpiryProbe() {
    }

    /** Epoch millis, or 0 for anything this runtime will not accept. */
    public static long parse(String iso) {
        return Credentials.expiryMillis(iso);
    }

    /**
     * Credentials.fromJson, which is package private for the same reason
     * expiryMillis is.
     *
     * <p>What it accepts is the whole contract with the metadata providers, and
     * the interesting cases are the ones it must REFUSE -- a response that parses
     * but describes a credential nothing can sign with.
     */
    public static String rejects(String json) {
        try {
            Credentials.fromJson(json);
            return "accepted";
        } catch (Exception refused) {
            return "refused";
        }
    }
}
