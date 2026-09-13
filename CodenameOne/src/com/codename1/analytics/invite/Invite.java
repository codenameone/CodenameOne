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
package com.codename1.analytics.invite;

/// An invite that has been minted and is ready to share. Immutable; create one
/// with [Invites#create].
///
/// [#getUrl] is the link to send. It is usable the moment [Invites#create]
/// returns, including with no network at all, so the share sheet never waits
/// on a server.
public final class Invite {
    private final String code;
    private final String url;
    private final String campaign;
    private final String channel;
    private final String payload;
    private final long createdTimestamp;

    Invite(String code, String url, String campaign, String channel, String payload,
            long createdTimestamp) {
        this.code = code;
        this.url = url;
        this.campaign = campaign;
        this.channel = channel;
        this.payload = payload;
        this.createdTimestamp = createdTimestamp;
    }

    /// The opaque invite code. This identifies the invite and authorizes
    /// nothing, so it is safe to print, log or show to the user.
    ///
    /// #### Returns
    ///
    /// the code, never null
    public String getCode() {
        return code;
    }

    /// The link to share.
    ///
    /// #### Returns
    ///
    /// an absolute https url, never null
    public String getUrl() {
        return url;
    }

    /// The campaign this invite belongs to, or null.
    ///
    /// #### Returns
    ///
    /// the campaign
    public String getCampaign() {
        return campaign;
    }

    /// The channel this invite was minted for, or null.
    ///
    /// #### Returns
    ///
    /// the channel
    public String getChannel() {
        return channel;
    }

    /// The application defined payload carried to the invited device, or null.
    ///
    /// #### Returns
    ///
    /// the payload
    public String getPayload() {
        return payload;
    }

    /// When this invite was minted, in milliseconds since the epoch.
    ///
    /// #### Returns
    ///
    /// the creation time
    public long getCreatedTimestamp() {
        return createdTimestamp;
    }

    // There is deliberately no isRegistered() here. This object is a value
    // captured the moment the invite was minted, and registration completes
    // asynchronously afterwards, so any flag stored on it could only ever
    // report the value it was constructed with -- false, for ever, contradicting
    // its own documentation. Ask [Invites#isRegistered(Invite)] instead, which
    // reads the durable outbox and can actually answer.

    @Override
    public String toString() {
        return "Invite[" + code + "]";
    }
}
