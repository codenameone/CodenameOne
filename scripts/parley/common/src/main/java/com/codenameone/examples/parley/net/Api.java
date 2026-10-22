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
package com.codenameone.examples.parley.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.parley.api.AccountApi;
import com.codenameone.examples.parley.api.AdminApi;
import com.codenameone.examples.parley.api.BridgeApi;
import com.codenameone.examples.parley.api.CallApi;
import com.codenameone.examples.parley.api.ContactApi;
import com.codenameone.examples.parley.api.ConversationApi;
import com.codenameone.examples.parley.api.DeviceApi;
import com.codenameone.examples.parley.api.MediaApi;
import com.codenameone.examples.parley.api.MessageApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static AdminApi admin;
    private static BridgeApi bridges;
    private static CallApi calls;
    private static ContactApi contacts;
    private static ConversationApi conversations;
    private static DeviceApi devices;
    private static MediaApi media;
    private static MessageApi messages;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        bridges = RestClients.create(BridgeApi.class, baseUrl);
        calls = RestClients.create(CallApi.class, baseUrl);
        contacts = RestClients.create(ContactApi.class, baseUrl);
        conversations = RestClients.create(ConversationApi.class, baseUrl);
        devices = RestClients.create(DeviceApi.class, baseUrl);
        media = RestClients.create(MediaApi.class, baseUrl);
        messages = RestClients.create(MessageApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static AdminApi admin() {
        return admin;
    }

    /// Linking groups to Telegram and WhatsApp.
    public static BridgeApi bridges() {
        return bridges;
    }

    public static CallApi calls() {
        return calls;
    }

    /// Finding people, blocking them and reporting them.
    public static ContactApi contacts() {
        return contacts;
    }

    public static ConversationApi conversations() {
        return conversations;
    }

    /// This device's keys, and other devices' prekey bundles.
    public static DeviceApi devices() {
        return devices;
    }

    /// Encrypted attachments, which the server stores and cannot open.
    public static MediaApi media() {
        return media;
    }

    public static MessageApi messages() {
        return messages;
    }
}
