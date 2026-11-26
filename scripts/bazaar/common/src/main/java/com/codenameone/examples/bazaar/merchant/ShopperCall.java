/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.merchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The store's side of one call from a shopper, as signalling alone: which
/// frames go out when, and what is held back until it may.
///
/// The shopper rings and sends the offer and its candidates at once, to every
/// staff connection of the store; the server replays them to one that opens
/// the conversation later. So the offer is kept from the moment it arrives,
/// and remote candidates wait until the offer is the connection's remote
/// description. The other way, the server drops a staff candidate until that
/// staff member's ANSWER has claimed the call, so local candidates wait until
/// the answer is sent. TAKEN (someone else answered) and END can arrive at
/// any point, mid-answer included, and release whatever was opened.
///
/// The media itself is behind [Media], so this runs in a plain unit test.
final class ShopperCall {
    /// The camera, microphone and peer connection. Each call reports back on
    /// the EDT through [#remoteSet], [#answered], [#localCandidate] and
    /// [#failed].
    interface Media {
        /// Opens the media and the connection, applies `offer` as the remote
        /// description and creates the answer.
        void start(ShopperCall call, boolean video, String offer);

        void addRemoteCandidate(String sdpMid, int sdpMLineIndex, String candidate);

        void release();
    }

    /// Where frames go: the chat socket.
    interface Out {
        void send(Map<String, Object> frame);
    }

    static final int RINGING = 0;
    static final int ACCEPTING = 1;
    static final int ANSWERED = 2;
    static final int ENDED = 3;

    private final String chat;
    private final Out out;
    private boolean video;
    private String offer;
    private int state = RINGING;
    private Media media;
    private boolean remoteSet;
    private boolean connected;
    private final List<Map> remoteEarly = new ArrayList<Map>();
    private final List<Map<String, Object>> localEarly = new ArrayList<Map<String, Object>>();

    ShopperCall(String chat, boolean video, Out out) {
        this.chat = chat;
        this.video = video;
        this.out = out;
    }

    String chat() {
        return chat;
    }

    boolean video() {
        return video;
    }

    int state() {
        return state;
    }

    boolean connected() {
        return connected;
    }

    /// Whether Accept can be pressed: still ringing, with the offer here.
    boolean canAccept() {
        return state == RINGING && offer != null;
    }

    /// A `call` frame about this conversation. Answers whether the call ended.
    boolean signal(Map frame) {
        if (state == ENDED) {
            return true;
        }
        String kind = ChatFrames.text(frame, "kind");
        if (ChatFrames.OFFER.equals(kind)) {
            if (state == RINGING) {
                offer = ChatFrames.text(frame, "sdp");
                video |= ChatFrames.truth(frame, "video");
            }
        } else if (ChatFrames.ICE.equals(kind)) {
            Map c = ChatFrames.map(frame, "candidate");
            if (c != null) {
                if (remoteSet && media != null) {
                    remote(c);
                } else {
                    remoteEarly.add(c);
                }
            }
        } else if (ChatFrames.RING.equals(kind)) {
            video |= ChatFrames.truth(frame, "video");
        } else if (ChatFrames.TAKEN.equals(kind) || ChatFrames.END.equals(kind)) {
            finish();
        }
        return state == ENDED;
    }

    private void remote(Map c) {
        media.addRemoteCandidate(ChatFrames.text(c, "sdpMid"),
                (int) ChatFrames.number(c, "sdpMLineIndex"), ChatFrames.text(c, "candidate"));
    }

    /// Accept was pressed. Answers false when it is too early or too late.
    boolean accept(Media with) {
        if (!canAccept()) {
            return false;
        }
        state = ACCEPTING;
        media = with;
        with.start(this, video, offer);
        return true;
    }

    /// The offer is the remote description: held candidates may go in.
    void remoteSet() {
        if (state == ENDED || media == null) {
            return;
        }
        remoteSet = true;
        for (int iter = 0; iter < remoteEarly.size(); iter++) {
            remote(remoteEarly.get(iter));
        }
        remoteEarly.clear();
    }

    /// The answer is the local description: it claims the call, and the
    /// candidates gathered meanwhile follow it.
    void answered(String sdp) {
        if (state != ACCEPTING) {
            return;
        }
        state = ANSWERED;
        out.send(ChatFrames.answer(chat, sdp));
        for (int iter = 0; iter < localEarly.size(); iter++) {
            out.send(localEarly.get(iter));
        }
        localEarly.clear();
    }

    void localCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
        if (state == ENDED) {
            return;
        }
        Map<String, Object> f = ChatFrames.ice(chat, sdpMid, sdpMLineIndex, candidate);
        if (state == ANSWERED) {
            out.send(f);
        } else {
            localEarly.add(f);
        }
    }

    /// The peer connection reports it is through.
    void connectedNow() {
        if (state == ANSWERED) {
            connected = true;
        }
    }

    /// Decline while it rings: the others on the staff keep ringing.
    void decline() {
        if (state == RINGING) {
            out.send(ChatFrames.call(chat, ChatFrames.DECLINE));
            finish();
        }
    }

    /// Hang up, or give up an answer under way.
    void hangUp() {
        if (state == ACCEPTING || state == ANSWERED) {
            out.send(ChatFrames.call(chat, ChatFrames.END));
        }
        finish();
    }

    /// The media failed: the shopper is told the call is over.
    void failed() {
        hangUp();
    }

    private void finish() {
        if (state == ENDED) {
            return;
        }
        state = ENDED;
        connected = false;
        remoteEarly.clear();
        localEarly.clear();
        if (media != null) {
            Media m = media;
            media = null;
            m.release();
        }
    }
}
