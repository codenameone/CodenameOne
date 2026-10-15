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

/// What the server sends with when the SMS settings are incomplete: every
/// message fails, and says why.
///
/// The problem is logged once when the server starts and again on every send,
/// naming the settings that are missing, so it is in front of whoever reads the
/// log at either moment. The failure is an `IOException` like any provider
/// outage, which the phone verification answers with 503: a server whose SMS
/// settings were forgotten verifies nobody, rather than everybody.
public class MisconfiguredSmsSender implements SmsSender {
    private final String problem;

    /// @param problem what is wrong, naming the settings to fix
    public MisconfiguredSmsSender(String problem) {
        this.problem = problem;
        System.err.println("[config] " + problem);
    }

    @Override
    public void send(String phone, String text) throws IOException {
        System.err.println("[config] a text message was not sent: " + problem);
        throw new IOException(problem);
    }

    /// True, because it stands in for a provider that would: the demo code a
    /// development server hands back is only for the console sender.
    @Override
    public boolean delivers() {
        return true;
    }
}
