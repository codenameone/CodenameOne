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
package com.codename1.io.mqtt;

/// Everything an [MqttClient] needs to open and identify a connection.
/// The setters return `this`.
public final class MqttOptions {
    private final String uri;
    private final String clientId;
    private boolean cleanSession = true;
    private int keepAliveSeconds = 60;
    private String username;
    private byte[] password;
    private String willTopic;
    private byte[] willPayload;
    private int willQos;
    private boolean willRetain;
    private int connectTimeoutMillis = 15000;
    private int maxPacketSize = 1048576;

    /// Creates options.
    ///
    /// #### Parameters
    ///
    /// - `uri`: `tcp://host[:port]` (default port 1883), `ws://host[:port]/path`
    ///   or `wss://host[:port]/path`, the last two over a WebSocket with the
    ///   `mqtt` subprotocol
    ///
    /// - `clientId`: the client identifier; may be empty only with a clean
    ///   session, in which case the broker assigns one
    public MqttOptions(String uri, String clientId) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }
        this.uri = uri;
        this.clientId = clientId == null ? "" : clientId;
    }

    /// The broker URI.
    public String getUri() {
        return uri;
    }

    /// The client identifier.
    public String getClientId() {
        return clientId;
    }

    /// Whether the broker should discard any session state of this client id.
    public boolean isCleanSession() {
        return cleanSession;
    }

    /// Sets the clean session flag (default true). With false, the client keeps
    /// unacknowledged QoS 1 publishes across a reconnect and re-sends them with
    /// the DUP flag.
    public MqttOptions cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        return this;
    }

    /// The keep alive interval in seconds; 0 disables it.
    public int getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    /// Sets the keep alive interval in seconds (default 60, 0 to 65535).
    public MqttOptions keepAliveSeconds(int seconds) {
        if (seconds < 0 || seconds > 65535) {
            throw new IllegalArgumentException("keep alive out of range: " + seconds);
        }
        this.keepAliveSeconds = seconds;
        return this;
    }

    /// The user name, or null.
    public String getUsername() {
        return username;
    }

    /// The password bytes, or null. The array is not copied.
    public byte[] getPassword() {
        return password;
    }

    /// Sets the credentials. MQTT 3.1.1 allows a password only with a user name.
    ///
    /// #### Parameters
    ///
    /// - `username`: the user name, or null for none
    ///
    /// - `password`: the password, or null for none
    public MqttOptions credentials(String username, byte[] password) {
        if (username == null && password != null) {
            throw new IllegalArgumentException("A password needs a user name");
        }
        this.username = username;
        this.password = password;
        return this;
    }

    /// Sets the will the broker publishes if this client vanishes.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the will topic, or null to clear the will
    ///
    /// - `payload`: the will message
    ///
    /// - `qos`: 0 or 1
    ///
    /// - `retain`: whether the broker retains the will
    public MqttOptions will(String topic, byte[] payload, int qos, boolean retain) {
        if (topic != null) {
            MqttTopic.validateName(topic);
        }
        if (qos < 0 || qos > 1) {
            throw new IllegalArgumentException("will qos must be 0 or 1");
        }
        this.willTopic = topic;
        this.willPayload = payload == null ? new byte[0] : payload;
        this.willQos = qos;
        this.willRetain = retain;
        return this;
    }

    /// The will topic, or null.
    public String getWillTopic() {
        return willTopic;
    }

    /// The will payload, or null when there is no will.
    public byte[] getWillPayload() {
        return willTopic == null ? null : willPayload;
    }

    /// The will QoS.
    public int getWillQos() {
        return willQos;
    }

    /// The will retain flag.
    public boolean isWillRetain() {
        return willRetain;
    }

    /// The time allowed for the transport to open and the broker to answer the
    /// CONNECT, in milliseconds.
    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    /// Sets the connect timeout in milliseconds (default 15000).
    public MqttOptions connectTimeoutMillis(int millis) {
        this.connectTimeoutMillis = millis;
        return this;
    }

    /// The largest packet, in bytes after the fixed header, the client accepts.
    public int getMaxPacketSize() {
        return maxPacketSize;
    }

    /// Sets the largest incoming packet accepted (default 1 MiB, at most the
    /// protocol's 268435455). A larger one fails the connection.
    public MqttOptions maxPacketSize(int bytes) {
        if (bytes < 1 || bytes > MqttCodec.MAX_REMAINING_LENGTH) {
            throw new IllegalArgumentException("max packet size out of range: " + bytes);
        }
        this.maxPacketSize = bytes;
        return this;
    }
}
