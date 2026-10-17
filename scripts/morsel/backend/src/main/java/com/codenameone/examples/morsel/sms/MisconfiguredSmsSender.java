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
package com.codenameone.examples.morsel.sms;

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
