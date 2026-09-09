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
package com.codename1.components;

import com.codename1.analytics.invite.Invite;
import com.codename1.analytics.invite.InviteRequest;
import com.codename1.analytics.invite.Invites;
import com.codename1.share.ShareResultListener;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;

/// A [ShareButton] that mints a fresh invite on every press and shares it, so
/// the whole invite funnel is wired with one component.
///
/// ```java
/// InviteButton invite = new InviteButton("Invite a friend");
/// invite.setCampaign("spring");
/// invite.setMessage("Come and try this with me");
/// form.add(invite);
/// ```
///
/// The button owns the share result, so `invite_shared` is reported only when
/// the platform confirms the user really shared. Your own
/// [#setShareResultListener] still works and is still called.
///
/// See [Invites] for the receiving half and for how attribution reaches your
/// analytics reports.
public class InviteButton extends ShareButton {
    private String campaign;
    private String channel;
    private String payload;
    private String message;
    private Invite invite;
    private ShareResultListener appListener;

    /// Default constructor.
    public InviteButton() {
        setUIID("InviteButton");
        FontImage.setMaterialIcon(this, FontImage.MATERIAL_GROUP_ADD);
        installChain();
    }

    /// Creates a button with the given label.
    ///
    /// #### Parameters
    ///
    /// - `text`: the button label
    public InviteButton(String text) {
        this();
        setText(text);
    }

    // ShareButton.actionPerformed reads its private listener FIELD, not the
    // getter, so the chaining listener has to be installed through super's
    // setter exactly once. The overridden accessors below then keep the
    // application's listener in a field of our own -- without that, setting a
    // listener would silently replace the chain and the funnel would lose
    // every share.
    private void installChain() {
        super.setShareResultListener(new ShareResultListener() {
            @Override
            public void onResult(com.codename1.share.ShareResult result) {
                Invite current = invite;
                if (current != null) {
                    Invites.reportShareResult(current, result);
                }
                if (appListener != null) {
                    appListener.onResult(result);
                }
            }
        });
    }

    /// Groups the invites this button mints under a campaign.
    ///
    /// #### Parameters
    ///
    /// - `campaign`: the campaign name
    public void setCampaign(String campaign) {
        this.campaign = campaign;
    }

    /// The campaign, or null.
    ///
    /// #### Returns
    ///
    /// the campaign
    public String getCampaign() {
        return campaign;
    }

    /// Records how the invite is being sent.
    ///
    /// #### Parameters
    ///
    /// - `channel`: the channel name
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /// The channel, or null.
    ///
    /// #### Returns
    ///
    /// the channel
    public String getChannel() {
        return channel;
    }

    /// An application defined string handed to the invited device.
    ///
    /// #### Parameters
    ///
    /// - `payload`: the payload
    public void setPayload(String payload) {
        this.payload = payload;
    }

    /// The payload, or null.
    ///
    /// #### Returns
    ///
    /// the payload
    public String getPayload() {
        return payload;
    }

    /// The text placed before the link in the shared message.
    ///
    /// #### Parameters
    ///
    /// - `message`: the message
    public void setMessage(String message) {
        this.message = message;
    }

    /// The message, or null.
    ///
    /// #### Returns
    ///
    /// the message
    public String getMessage() {
        return message;
    }

    /// The invite minted for the most recent press, or null before the first
    /// press.
    ///
    /// #### Returns
    ///
    /// the invite
    public Invite getInvite() {
        return invite;
    }

    /// {@inheritDoc}
    @Override
    public void setShareResultListener(ShareResultListener listener) {
        this.appListener = listener;
    }

    /// {@inheritDoc}
    @Override
    public ShareResultListener getShareResultListener() {
        return appListener;
    }

    /// {@inheritDoc}
    @Override
    public void actionPerformed(ActionEvent evt) {
        InviteRequest.Builder b = InviteRequest.create();
        if (campaign != null) {
            b.campaign(campaign);
        }
        if (channel != null) {
            b.channel(channel);
        }
        if (payload != null) {
            b.payload(payload);
        }
        invite = Invites.create(b.build());
        String text = message == null || message.length() == 0
                ? invite.getUrl() : message + " " + invite.getUrl();
        setTextToShare(text);
        // ShareButton defers the share by one EDT cycle, so setting the text
        // here is in time.
        super.actionPerformed(evt);
    }

    /// {@inheritDoc}
    @Override
    public String[] getPropertyNames() {
        return new String[]{"textToShare", "campaign", "channel", "payload", "message"};
    }

    /// {@inheritDoc}
    @Override
    public Class[] getPropertyTypes() {
        return new Class[]{String.class, String.class, String.class, String.class, String.class};
    }

    /// {@inheritDoc}
    @Override
    public Object getPropertyValue(String name) {
        if ("campaign".equals(name)) {
            return getCampaign();
        }
        if ("channel".equals(name)) {
            return getChannel();
        }
        if ("payload".equals(name)) {
            return getPayload();
        }
        if ("message".equals(name)) {
            return getMessage();
        }
        return super.getPropertyValue(name);
    }

    /// {@inheritDoc}
    @Override
    public String setPropertyValue(String name, Object value) {
        String v = value instanceof String ? (String) value : null;
        if ("campaign".equals(name)) {
            setCampaign(v);
            return null;
        }
        if ("channel".equals(name)) {
            setChannel(v);
            return null;
        }
        if ("payload".equals(name)) {
            setPayload(v);
            return null;
        }
        if ("message".equals(name)) {
            setMessage(v);
            return null;
        }
        return super.setPropertyValue(name, value);
    }
}
