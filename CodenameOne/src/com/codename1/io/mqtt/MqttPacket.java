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

/// One MQTT 3.1.1 control packet as a value. Build one with the static
/// factories and give it to [MqttCodec#write(MqttPacket, java.io.OutputStream)];
/// [MqttCodec#read(java.io.InputStream, int)] returns one. Only the accessors
/// that belong to the packet's [#getType()] carry meaning.
public final class MqttPacket {
    /// Packet type CONNECT.
    public static final int CONNECT = 1;
    /// Packet type CONNACK.
    public static final int CONNACK = 2;
    /// Packet type PUBLISH.
    public static final int PUBLISH = 3;
    /// Packet type PUBACK.
    public static final int PUBACK = 4;
    /// Packet type SUBSCRIBE.
    public static final int SUBSCRIBE = 8;
    /// Packet type SUBACK.
    public static final int SUBACK = 9;
    /// Packet type UNSUBSCRIBE.
    public static final int UNSUBSCRIBE = 10;
    /// Packet type UNSUBACK.
    public static final int UNSUBACK = 11;
    /// Packet type PINGREQ.
    public static final int PINGREQ = 12;
    /// Packet type PINGRESP.
    public static final int PINGRESP = 13;
    /// Packet type DISCONNECT.
    public static final int DISCONNECT = 14;

    private final int type;
    private int packetId;
    private String topic;
    private byte[] payload;
    private int qos;
    private boolean retain;
    private boolean dup;
    private boolean sessionPresent;
    private int returnCode;
    private String[] filters;
    private int[] qosList;
    private String clientId;
    private boolean cleanSession;
    private int keepAlive;
    private String username;
    private byte[] password;
    private String willTopic;
    private byte[] willPayload;
    private int willQos;
    private boolean willRetain;

    MqttPacket(int type) {
        this.type = type;
    }

    /// Builds a CONNECT packet from the options.
    public static MqttPacket connect(MqttOptions o) {
        MqttPacket p = new MqttPacket(CONNECT);
        p.clientId = o.getClientId();
        p.cleanSession = o.isCleanSession();
        p.keepAlive = o.getKeepAliveSeconds();
        p.username = o.getUsername();
        p.password = o.getPassword();
        p.willTopic = o.getWillTopic();
        p.willPayload = o.getWillPayload();
        p.willQos = o.getWillQos();
        p.willRetain = o.isWillRetain();
        return p;
    }

    /// Builds a CONNACK packet.
    public static MqttPacket connack(boolean sessionPresent, int returnCode) {
        MqttPacket p = new MqttPacket(CONNACK);
        p.sessionPresent = sessionPresent;
        p.returnCode = returnCode;
        return p;
    }

    /// Builds a PUBLISH packet.
    ///
    /// #### Parameters
    ///
    /// - `topic`: the topic name
    ///
    /// - `payload`: the body
    ///
    /// - `qos`: 0 or 1
    ///
    /// - `retain`: the retain flag
    ///
    /// - `dup`: the DUP flag, set on a re-delivery
    ///
    /// - `packetId`: 1 to 65535 for QoS 1, ignored for QoS 0
    public static MqttPacket publish(String topic, byte[] payload, int qos, boolean retain, boolean dup, int packetId) {
        MqttPacket p = new MqttPacket(PUBLISH);
        p.topic = topic;
        p.payload = payload == null ? new byte[0] : payload;
        p.qos = qos;
        p.retain = retain;
        p.dup = dup;
        p.packetId = packetId;
        return p;
    }

    /// Builds a PUBACK packet.
    public static MqttPacket puback(int packetId) {
        MqttPacket p = new MqttPacket(PUBACK);
        p.packetId = packetId;
        return p;
    }

    /// Builds a SUBSCRIBE packet with parallel arrays of filters and requested QoS.
    public static MqttPacket subscribe(int packetId, String[] filters, int[] qos) {
        MqttPacket p = new MqttPacket(SUBSCRIBE);
        p.packetId = packetId;
        p.filters = filters;
        p.qosList = qos;
        return p;
    }

    /// Builds a SUBACK packet; each code is the granted QoS or 0x80 for failure.
    public static MqttPacket suback(int packetId, int[] returnCodes) {
        MqttPacket p = new MqttPacket(SUBACK);
        p.packetId = packetId;
        p.qosList = returnCodes;
        return p;
    }

    /// Builds an UNSUBSCRIBE packet.
    public static MqttPacket unsubscribe(int packetId, String[] filters) {
        MqttPacket p = new MqttPacket(UNSUBSCRIBE);
        p.packetId = packetId;
        p.filters = filters;
        return p;
    }

    /// Builds an UNSUBACK packet.
    public static MqttPacket unsuback(int packetId) {
        MqttPacket p = new MqttPacket(UNSUBACK);
        p.packetId = packetId;
        return p;
    }

    /// Builds a PINGREQ packet.
    public static MqttPacket pingreq() {
        return new MqttPacket(PINGREQ);
    }

    /// Builds a PINGRESP packet.
    public static MqttPacket pingresp() {
        return new MqttPacket(PINGRESP);
    }

    /// Builds a DISCONNECT packet.
    public static MqttPacket disconnect() {
        return new MqttPacket(DISCONNECT);
    }

    /// The packet type, one of the constants of this class.
    public int getType() {
        return type;
    }

    /// The packet identifier of PUBLISH (QoS 1), PUBACK, SUBSCRIBE, SUBACK,
    /// UNSUBSCRIBE and UNSUBACK.
    public int getPacketId() {
        return packetId;
    }

    /// The topic name of a PUBLISH.
    public String getTopic() {
        return topic;
    }

    /// The body of a PUBLISH.
    public byte[] getPayload() {
        return payload;
    }

    /// The QoS of a PUBLISH.
    public int getQos() {
        return qos;
    }

    /// The retain flag of a PUBLISH.
    public boolean isRetain() {
        return retain;
    }

    /// The DUP flag of a PUBLISH.
    public boolean isDup() {
        return dup;
    }

    /// Sets the DUP flag, used when a PUBLISH is sent again.
    public void setDup(boolean dup) {
        this.dup = dup;
    }

    /// The session present flag of a CONNACK.
    public boolean isSessionPresent() {
        return sessionPresent;
    }

    /// The return code of a CONNACK (0 accepted, 1 to 5 refused).
    public int getReturnCode() {
        return returnCode;
    }

    /// The topic filters of a SUBSCRIBE or UNSUBSCRIBE.
    public String[] getFilters() {
        return filters;
    }

    /// The requested QoS list of a SUBSCRIBE, or the return codes of a SUBACK.
    public int[] getQosList() {
        return qosList;
    }

    /// The client identifier of a CONNECT.
    public String getClientId() {
        return clientId;
    }

    /// The clean session flag of a CONNECT.
    public boolean isCleanSession() {
        return cleanSession;
    }

    /// The keep alive, in seconds, of a CONNECT.
    public int getKeepAlive() {
        return keepAlive;
    }

    /// The user name of a CONNECT, or null.
    public String getUsername() {
        return username;
    }

    /// The password of a CONNECT, or null.
    public byte[] getPassword() {
        return password;
    }

    /// The will topic of a CONNECT, or null.
    public String getWillTopic() {
        return willTopic;
    }

    /// The will payload of a CONNECT, or null.
    public byte[] getWillPayload() {
        return willPayload;
    }

    /// The will QoS of a CONNECT.
    public int getWillQos() {
        return willQos;
    }

    /// The will retain flag of a CONNECT.
    public boolean isWillRetain() {
        return willRetain;
    }

    void setConnectFields(String clientId, boolean clean, int keepAlive, String user, byte[] pass,
            String willTopic, byte[] willPayload, int willQos, boolean willRetain) {
        this.clientId = clientId;
        this.cleanSession = clean;
        this.keepAlive = keepAlive;
        this.username = user;
        this.password = pass;
        this.willTopic = willTopic;
        this.willPayload = willPayload;
        this.willQos = willQos;
        this.willRetain = willRetain;
    }
}
