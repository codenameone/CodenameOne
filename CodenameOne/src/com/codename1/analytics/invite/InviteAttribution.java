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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/// The invite that caused this install or open. Immutable; delivered to an
/// [InviteListener].
///
/// [#getMatchType] says how the invite was identified, and every answer is
/// exact:
///
/// - [Invites#MATCH_DIRECT] -- the link opened an app that was already
///   installed.
/// - [Invites#MATCH_REFERRER] -- the code travelled through Google Play and
///   came back verbatim.
/// - [Invites#MATCH_APP_CLIP] -- an iOS App Clip was launched by the invite
///   link, so it received the code exactly, and handed it to the app the
///   person then installed.
///
/// Nothing here is matched, estimated or guessed, so a referral bounty can be
/// paid on any of them. An earlier design added a statistical match for iOS,
/// because the App Store carries no referrer of its own; App Clips made it
/// unnecessary and it is gone, along with the profile of the visitor it
/// needed.
public final class InviteAttribution {
    private final String code;
    private final String campaign;
    private final String channel;
    private final String payload;
    private final String matchType;
    private final double confidence;
    private final boolean deferred;
    private final long clickTimestamp;
    private final long resolvedTimestamp;
    private final Map<String, String> parameters;

    InviteAttribution(String code, String campaign, String channel, String payload,
            String matchType, double confidence, boolean deferred, long clickTimestamp,
            long resolvedTimestamp, Map<String, String> parameters) {
        this.code = code;
        this.campaign = campaign;
        this.channel = channel;
        this.payload = payload;
        this.matchType = matchType;
        this.confidence = confidence;
        this.deferred = deferred;
        this.clickTimestamp = clickTimestamp;
        this.resolvedTimestamp = resolvedTimestamp;
        Map<String, String> copy = new LinkedHashMap<String, String>();
        if (parameters != null) {
            copy.putAll(parameters);
        }
        this.parameters = Collections.unmodifiableMap(copy);
    }

    /// The invite code that was matched.
    ///
    /// #### Returns
    ///
    /// the code, never null
    public String getCode() {
        return code;
    }

    /// The campaign the invite belonged to, or null when the server could not
    /// be reached to look it up.
    ///
    /// #### Returns
    ///
    /// the campaign
    public String getCampaign() {
        return campaign;
    }

    /// The channel the invite was sent through, or null.
    ///
    /// #### Returns
    ///
    /// the channel
    public String getChannel() {
        return channel;
    }

    /// The payload the inviter attached, or null.
    ///
    /// #### Returns
    ///
    /// the payload
    public String getPayload() {
        return payload;
    }

    /// How this attribution was established: [Invites#MATCH_DIRECT],
    /// [Invites#MATCH_REFERRER] or [Invites#MATCH_APP_CLIP].
    ///
    /// #### Returns
    ///
    /// the match type, never null
    public String getMatchType() {
        return matchType;
    }

    /// How much to trust this attribution, from 0 to 1.
    ///
    /// Always 1. Every match type is exact now, so there is nothing left for
    /// this to discount -- it survives because an application that branched on
    /// it should keep compiling and keep taking the same branch.
    ///
    /// #### Returns
    ///
    /// the confidence
    public double getConfidence() {
        return confidence;
    }

    /// Whether this attribution explains the install itself, as opposed to a
    /// link opened by someone who already had the application.
    ///
    /// #### Returns
    ///
    /// true when the invite caused the install
    public boolean isDeferred() {
        return deferred;
    }

    /// When the link was tapped, in milliseconds since the epoch, or 0 when
    /// nothing observed it.
    ///
    /// Zero is a real answer and not rare. The tap is observed by whichever
    /// side of the exchange saw it: the invite redirect for a link that
    /// reached it, or the App Clip for an iOS invocation, which iOS resolves
    /// from the association file without ever reaching the redirect. An
    /// install whose tap neither side recorded has no time to report, so
    /// compare against 0 before subtracting it from anything.
    ///
    /// #### Returns
    ///
    /// the click time, or 0
    public long getClickTimestamp() {
        return clickTimestamp;
    }

    /// When this attribution was resolved, in milliseconds since the epoch.
    ///
    /// #### Returns
    ///
    /// the resolution time
    public long getResolvedTimestamp() {
        return resolvedTimestamp;
    }

    /// The custom parameters the inviter attached.
    ///
    /// #### Returns
    ///
    /// an unmodifiable, insertion ordered map, never null
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String toString() {
        return "InviteAttribution[" + code + " " + matchType + "]";
    }
}
