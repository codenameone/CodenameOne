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
package com.codenameone.developerguide.backend.messaging;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.KafkaListener;
import com.codename1.backend.annotations.MessageListener;
import com.codename1.backend.messaging.Message;
import com.codenameone.developerguide.backend.orders.Order;

// tag::backend-messaging-listen[]
@Component
public class Shipping {
    @MessageListener(value = "orders.placed", group = "shipping", concurrency = 4)
    public void placed(Order order) {
        // runs once per message; throwing has it delivered again later
    }

    @MessageListener(value = "orders.placed.dlq", group = "support")
    public void gaveUp(Message message) {
        String why = message.getHeader(Message.ERROR);
        // the order that failed every attempt, and the last exception
    }
}
// end::backend-messaging-listen[]

// tag::backend-messaging-spring[]
@Component
class Invoicing {
    @KafkaListener(topics = "orders.placed", groupId = "invoicing")
    public void placed(Order order) {
        // the same listener, written as a Spring Kafka application writes it
    }
}
// end::backend-messaging-spring[]
