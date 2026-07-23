/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.demos.push;

import com.codename1.io.Log;
import com.codename1.push.PushClient;
import com.codename1.push.PushError;
import com.codename1.push.PushListener;
import com.codename1.push.PushMessage;
import com.codename1.push.PushRegistrationSink;
import com.codename1.push.PushSubscription;
import com.codename1.push.PushTransport;

/**
 * Compiled backing source for the developer-guide push examples.
 */
public class PushNotificationSnippets {

    // tag::push-notifications-java-001[]
    private PushClient push;

    public void init(Object context) {
        push = PushClient.builder("cn1_push_application_key")
                .listener(new PushListener() {
                    @Override
                    public void onRegistration(PushSubscription subscription) {
                        Log.p("Push ready on " + subscription.getTransportId());
                    }

                    @Override
                    public void onMessage(PushMessage message) {
                        if (message.getDeepLink() != null) {
                            Log.p("Route to " + message.getDeepLink());
                        }
                    }

                    @Override
                    public void onError(PushError error) {
                        Log.p(error.getCode() + ": " + error.getMessage());
                    }
                })
                .build();
    }

    public void start() {
        push.register();
    }
    // end::push-notifications-java-001[]

    public PushMessage createMessage() {
        // tag::push-notifications-java-002[]
        PushMessage message = PushMessage.builder()
                .title("Order shipped")
                .body("Order 4815 is on its way")
                .deepLink("myapp://orders/4815")
                .imageUrl("https://example.com/orders/4815.png")
                .collapseKey("order-4815")
                .ttlSeconds(3600)
                .data("orderId", "4815")
                .build();
        // end::push-notifications-java-002[]
        return message;
    }

    public PushClient createCustomClient() {
        // tag::push-notifications-java-003[]
        PushTransport companyTransport = null; // Supplied by the native CN1Lib.
        PushListener listener = null; // The application's normal listener.
        PushClient client = PushClient.builder("private-app-id")
                .transport(companyTransport)
                .registrationSink(new PushRegistrationSink() {
                    @Override
                    public void registered(PushSubscription value) {
                        Log.p("Send the subscription to the company server");
                    }

                    @Override
                    public void unregistered(PushSubscription value) {
                        Log.p("Remove the subscription from the company server");
                    }
                })
                .listener(listener)
                .build();
        // end::push-notifications-java-003[]
        return client;
    }
}
