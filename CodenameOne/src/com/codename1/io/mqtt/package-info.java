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
/// A small MQTT 3.1.1 client.
///
/// `MqttClient` connects to a broker over plain TCP (`tcp://`) or over a
/// WebSocket with the `mqtt` subprotocol (`ws://` and `wss://`, which is how TLS
/// is reached: the TCP transport has no TLS), publishes and subscribes at QoS 0
/// and QoS 1, and delivers every callback on the Codename One EDT.
///
/// `MqttCodec` and `MqttPacket` are the transport independent packet layer and
/// work on plain streams. QoS 2 is not implemented: a client never asks a
/// broker for it, and a packet that needs it is a protocol error.
///
/// ```java
/// MqttOptions o = new MqttOptions("tcp://broker.example.com:1883", "my-client");
/// MqttClient c = new MqttClient();
/// c.connect(o, new MqttListener() {
///     public void connected() { c.subscribe("sensors/#", 1); }
///     public void messageArrived(String topic, byte[] payload, boolean retained) { }
///     public void connectionLost(String reason) { }
/// });
/// ```
package com.codename1.io.mqtt;
