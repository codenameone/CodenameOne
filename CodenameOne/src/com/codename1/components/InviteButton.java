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
import com.codename1.io.Log;
import com.codename1.ui.Display;
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
    // Two fields, because they answer two different questions and clearing
    // one on a result silently broke the other. `invite` is what getInvite()
    // reports -- the invite minted for the most recent press, which an
    // application reads from inside its own ShareResultListener to tell which
    // invite the ShareResult belongs to, so it has to survive the result.
    // `outstanding` is the one still awaiting a result, and exists only to
    // stop a second press minting a second code; it is cleared as soon as the
    // outcome is taken, so that outcome can be reported exactly once.
    private Invite invite;
    private Invite outstanding;
    private ShareResultListener appListener;
    // The chained listener super was given. Package private so a test can
    // deliver a ShareResult without the share sheet -- getShareResultListener()
    // is overridden to answer with the application's listener, so the chain is
    // otherwise unreachable from outside a real press.
    ShareResultListener chain;
    // True from a press until the next EDT cycle, which is when ShareButton's
    // deferred runnable has already presented. Its ONLY job is to collapse
    // presses that arrive before that; it is never a "share in progress"
    // flag, because nothing guarantees a share ever reports.
    private boolean presenting;

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
        chain = new ShareResultListener() {
            @Override
            public void onResult(com.codename1.share.ShareResult result) {
                // Taken and CLEARED, so the next press mints again and this
                // outcome can only ever be reported once. Only the outstanding
                // mark is cleared -- getInvite() still answers, because the
                // application's listener runs below and correlating the result
                // with its invite is the whole reason that accessor exists.
                Invite current = outstanding;
                outstanding = null;
                if (current != null) {
                    Invites.reportShareResult(current, result);
                }
                if (appListener != null) {
                    appListener.onResult(result);
                }
            }
        };
        super.setShareResultListener(chain);
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
        // A press is dropped only while ANOTHER PRESS IS STILL ON ITS WAY to
        // the share sheet -- not for as long as a share is outstanding.
        //
        // ShareButton defers to the next EDT cycle and then shares
        // unconditionally, so two presses within one cycle enqueue two
        // presentations: two native sheets attempted, the application's
        // listener called twice, and -- because the first result takes
        // `outstanding` -- the second share reported to nobody.
        //
        // Keying that on `outstanding` instead would have been a far worse
        // bug than the one it fixed. Display.share() documents that the
        // listener always runs, but on Android the API 22+ chooser callback
        // deliberately does not: "Android does not expose a dismissal signal
        // for the chooser, so the listener simply does not fire on user-cancel"
        // (AndroidImplementation.buildShareChooserWithCallback). A user who
        // opens the sheet and backs out would leave `outstanding` set with
        // nothing to clear it, and the button would never share again until
        // the form was rebuilt.
        //
        // This flag cannot do that: it is cleared on the next EDT cycle
        // whatever happens, by a runnable queued behind the one ShareButton
        // itself queues. Nothing about the sheet, the platform or the user's
        // answer can hold it.
        if (presenting) {
            return;
        }
        presenting = true;
        if (mintForShare() == null) {
            // Nothing was minted -- the device could not supply secure
            // randomness -- so there is no link to share. Presenting anyway
            // would open the sheet on whatever text was set last.
            presenting = false;
            return;
        }
        presentShare(evt);
        Display d = Display.getInstance();
        if (d == null) {
            // No EDT to clear it on, so it was never set.
            presenting = false;
            return;
        }
        d.callSerially(new Runnable() {
            @Override
            public void run() {
                presenting = false;
            }
        });
    }

    /// Hands the press to [ShareButton], which presents the sheet.
    ///
    /// Package private so a test can count presentations. Whether a second
    /// press presents a second time is not observable otherwise: ShareButton
    /// defers to the next EDT cycle, and the sheet it opens there is the one
    /// part of a press that cannot run headless.
    ///
    /// ShareButton defers by one EDT cycle, so the text set in
    /// `mintForShare()` is in time.
    ///
    /// #### Parameters
    ///
    /// - `evt`: the press
    void presentShare(ActionEvent evt) {
        super.actionPerformed(evt);
    }

    /// Mints the invite this press will share, or keeps the one still
    /// outstanding, and sets the text.
    ///
    /// Package private so a test can drive it without the share sheet: the
    /// sheet is the one part of a press that cannot run headless, and the
    /// question this answers -- how many invites two presses mint -- is
    /// decided before it opens.
    Invite mintForShare() {
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
        // One outstanding invite at a time, and a second press before the
        // first sheet has answered reuses it rather than minting another.
        //
        // The share sheet is modal, so a double tap does not open two of them
        // -- it mints two codes and shares the later one, leaving the first
        // registered, counted as invite_created, and never shared by anybody.
        // Worse, the result is reported against whichever invite the field
        // held when it arrived, so with two sheets the answer for one could be
        // recorded against the other.
        //
        // Reusing removes both: the outcome belongs to exactly one invite by
        // construction. Sharing one code more than once is the ordinary shape
        // of a referral anyway -- a code is not per recipient, it is the
        // inviter's -- so nothing is lost by not minting a second.
        if (outstanding == null) {
            try {
                outstanding = Invites.create(b.build());
            } catch (IllegalStateException e) {
                // The device could not supply secure randomness, so there is
                // no invite to share. Nothing is presented rather than
                // sharing a link somebody else could claim -- see
                // Invites.create().
                Log.e(e);
                return null;
            }
            invite = outstanding;
        }
        // The outstanding one, not the accessor's: this is the invite whose
        // url goes into the sheet, and the two only ever differ if a future
        // change lets them.
        String text = message == null || message.length() == 0
                ? outstanding.getUrl() : message + " " + outstanding.getUrl();
        setTextToShare(text);
        return outstanding;
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
