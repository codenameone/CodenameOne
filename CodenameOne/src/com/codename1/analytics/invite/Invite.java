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
    private final boolean registered;

    Invite(String code, String url, String campaign, String channel, String payload,
            long createdTimestamp, boolean registered) {
        this.code = code;
        this.url = url;
        this.campaign = campaign;
        this.channel = channel;
        this.payload = payload;
        this.createdTimestamp = createdTimestamp;
        this.registered = registered;
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

    /// Whether the link service has acknowledged this invite. An invite that
    /// has not been acknowledged is still shareable and still attributes --
    /// registration is retried in the background -- so this is a diagnostic,
    /// not a gate.
    ///
    /// #### Returns
    ///
    /// true once the server has acknowledged the invite
    public boolean isRegistered() {
        return registered;
    }

    @Override
    public String toString() {
        return "Invite[" + code + "]";
    }
}
