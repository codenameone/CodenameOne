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
package com.codenameone.developerguide.backend.wayline;

import java.io.IOException;

// tag::wayline-sms-sender[]
/// Sends a text message. The one place the server touches an SMS provider, so
/// changing provider is one class.
public interface SmsSender {
    /// - `IOException`: when the message could not be handed to the provider
    void send(String phone, String text) throws IOException;

    /// Whether a message really reaches a phone. False for the sender that only
    /// logs, which is what lets a development server hand the code back to the
    /// app in place of the text message nobody is going to receive.
    boolean delivers();
}
// end::wayline-sms-sender[]
