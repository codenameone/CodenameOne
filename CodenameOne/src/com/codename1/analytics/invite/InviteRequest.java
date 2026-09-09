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

/// Describes the invite to mint. Immutable; build one with [#create].
///
/// ```java
/// InviteRequest request = InviteRequest.create()
///         .campaign("spring")
///         .channel("whatsapp")
///         .title("Join me")
///         .description("I am using this and thought of you.")
///         .payload("room-42")
///         .build();
/// ```
///
/// `title`, `description` and `imageUrl` drive the preview card the link
/// service renders, which is what makes a shared link look like an invitation
/// in a messaging application rather than a bare address.
///
/// [Builder#build] validates and throws `IllegalArgumentException` naming the
/// offending field, so a mistake surfaces at the call you can see rather than
/// as a silently dropped value later.
public final class InviteRequest {
    /// The longest accepted [Builder#payload].
    public static final int MAX_PAYLOAD_LENGTH = 512;

    /// The longest accepted [Builder#title].
    public static final int MAX_TITLE_LENGTH = 128;

    /// The longest accepted [Builder#description].
    public static final int MAX_DESCRIPTION_LENGTH = 256;

    /// The longest accepted [Builder#campaign] or [Builder#channel].
    public static final int MAX_TOKEN_LENGTH = 64;

    private final String campaign;
    private final String channel;
    private final String payload;
    private final String title;
    private final String description;
    private final String imageUrl;
    private final Map<String, String> parameters;

    private InviteRequest(Builder b) {
        this.campaign = b.campaign;
        this.channel = b.channel;
        this.payload = b.payload;
        this.title = b.title;
        this.description = b.description;
        this.imageUrl = b.imageUrl;
        this.parameters = Collections.unmodifiableMap(
                new LinkedHashMap<String, String>(b.parameters));
    }

    /// Starts building a request.
    ///
    /// #### Returns
    ///
    /// a new builder
    public static Builder create() {
        return new Builder();
    }

    /// The campaign, or null.
    ///
    /// #### Returns
    ///
    /// the campaign
    public String getCampaign() {
        return campaign;
    }

    /// The channel, or null.
    ///
    /// #### Returns
    ///
    /// the channel
    public String getChannel() {
        return channel;
    }

    /// The application defined payload, or null.
    ///
    /// #### Returns
    ///
    /// the payload
    public String getPayload() {
        return payload;
    }

    /// The preview card title, or null.
    ///
    /// #### Returns
    ///
    /// the title
    public String getTitle() {
        return title;
    }

    /// The preview card description, or null.
    ///
    /// #### Returns
    ///
    /// the description
    public String getDescription() {
        return description;
    }

    /// The preview card image address, or null.
    ///
    /// #### Returns
    ///
    /// the image address
    public String getImageUrl() {
        return imageUrl;
    }

    /// The custom parameters carried to the invited device.
    ///
    /// #### Returns
    ///
    /// an unmodifiable, insertion ordered map, never null
    public Map<String, String> getParameters() {
        return parameters;
    }

    /// Builds an [InviteRequest].
    public static final class Builder {
        private String campaign;
        private String channel;
        private String payload;
        private String title;
        private String description;
        private String imageUrl;
        private final Map<String, String> parameters = new LinkedHashMap<String, String>();

        Builder() {
        }

        /// Groups this invite with others for reporting, for example a
        /// seasonal push. Letters, digits, `.`, `_` and `-` only.
        ///
        /// #### Parameters
        ///
        /// - `campaign`: the campaign name
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder campaign(String campaign) {
            this.campaign = campaign;
            return this;
        }

        /// How the invite is being sent, for example `sms` or `whatsapp`.
        /// Letters, digits, `.`, `_` and `-` only.
        ///
        /// #### Parameters
        ///
        /// - `channel`: the channel name
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder channel(String channel) {
            this.channel = channel;
            return this;
        }

        /// An application defined string handed back to the invited device,
        /// for example the room or team the friend is being invited to.
        ///
        /// #### Parameters
        ///
        /// - `payload`: the payload
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder payload(String payload) {
            this.payload = payload;
            return this;
        }

        /// The headline on the preview card the link shows in a messaging
        /// application.
        ///
        /// #### Parameters
        ///
        /// - `title`: the title
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /// The body text on the preview card.
        ///
        /// #### Parameters
        ///
        /// - `description`: the description
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /// The image on the preview card, as an absolute address.
        ///
        /// #### Parameters
        ///
        /// - `url`: the image address
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder imageUrl(String url) {
            this.imageUrl = url;
            return this;
        }

        /// Adds a custom parameter carried to the invited device. A null
        /// value removes the key.
        ///
        /// #### Parameters
        ///
        /// - `key`: the parameter name
        ///
        /// - `value`: the parameter value, or null to remove it
        ///
        /// #### Returns
        ///
        /// this builder
        public Builder param(String key, String value) {
            if (key == null || key.length() == 0) {
                return this;
            }
            if (value == null) {
                parameters.remove(key);
            } else {
                parameters.put(key, value);
            }
            return this;
        }

        /// Validates and builds the request.
        ///
        /// #### Returns
        ///
        /// the immutable request
        public InviteRequest build() {
            checkToken("campaign", campaign);
            checkToken("channel", channel);
            checkLength("payload", payload, MAX_PAYLOAD_LENGTH);
            checkLength("title", title, MAX_TITLE_LENGTH);
            checkLength("description", description, MAX_DESCRIPTION_LENGTH);
            return new InviteRequest(this);
        }

        private static void checkLength(String field, String value, int max) {
            if (value != null && value.length() > max) {
                throw new IllegalArgumentException(
                        field + " is longer than " + max + " characters");
            }
        }

        // Campaign and channel end up both in a url and as an analytics
        // dimension value, so they are restricted to characters that are safe
        // unescaped in either. Checked here rather than silently rewritten,
        // because a rewritten campaign name stops matching the one in the
        // report.
        private static void checkToken(String field, String value) {
            if (value == null) {
                return;
            }
            if (value.length() == 0 || value.length() > MAX_TOKEN_LENGTH) {
                throw new IllegalArgumentException(
                        field + " must be 1 to " + MAX_TOKEN_LENGTH + " characters");
            }
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
                if (!ok) {
                    throw new IllegalArgumentException(
                            field + " may only contain letters, digits, '.', '_' and '-'");
                }
            }
        }
    }
}
