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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The inbox's live list, its frames, and the store's side of a shopper's call.
class InboxLogicTest {
    private static Map<String, Object> frame(Object... pairs) {
        Map<String, Object> f = new HashMap<String, Object>();
        for (int iter = 0; iter < pairs.length; iter += 2) {
            f.put((String) pairs[iter], pairs[iter + 1]);
        }
        return f;
    }

    private static InboxBoard board() {
        InboxBoard b = new InboxBoard();
        List<InboxBoard.Row> rows = new ArrayList<InboxBoard.Row>();
        rows.add(new InboxBoard.Row("c1", "Ada", "Order", "OPEN", "", 2, 0, 100));
        rows.add(new InboxBoard.Row("c2", "Ben", "Sizes", "OPEN", "", 1, 1, 50));
        b.reset(rows);
        return b;
    }

    @Test
    void aShopperLineCountsUnreadAndMovesUp() {
        InboxBoard b = board();
        assertEquals(1, b.unread());
        assertEquals(InboxBoard.LISTED, b.message(frame("chat", "c2", "id", "m1",
                "staff", Boolean.FALSE, "body", "hi", "at", Double.valueOf(200))));
        assertEquals("c2", b.rows().get(0).id);
        assertEquals(2, b.row("c2").unread);
        assertEquals(200, b.row("c2").lastAt);
        // A staff reply moves the row but is never unread.
        assertEquals(InboxBoard.LISTED, b.message(frame("chat", "c1", "id", "m2",
                "staff", "true", "body", "ok", "at", Long.valueOf(300))));
        assertEquals(0, b.row("c1").unread);
        assertEquals("c1", b.rows().get(0).id);
        assertEquals(InboxBoard.UNKNOWN, b.message(frame("chat", "c9", "id", "m3")));
        assertEquals(InboxBoard.IGNORED, b.message(frame("id", "m4")));
    }

    @Test
    void theOpenThreadIsReadAndDedupesItsEcho() {
        InboxBoard b = board();
        List<InboxBoard.Line> lines = new ArrayList<InboxBoard.Line>();
        lines.add(new InboxBoard.Line("m0", "Ben", false, "hello", 40));
        b.open("c2", lines);
        assertEquals(0, b.row("c2").unread, "opening reads it");
        assertEquals(InboxBoard.SHOWN, b.message(frame("chat", "c2", "id", "m1",
                "staff", Boolean.FALSE, "body", "there?", "at", Long.valueOf(60))));
        assertEquals(0, b.row("c2").unread);
        assertEquals(60, b.newestAt());
        // The staff's own reply returns as a frame; a second copy is dropped.
        assertEquals(InboxBoard.SHOWN, b.message(frame("chat", "c2", "id", "m2",
                "staff", Boolean.TRUE, "body", "yes", "at", Long.valueOf(70))));
        assertEquals(InboxBoard.IGNORED, b.message(frame("chat", "c2", "id", "m2",
                "staff", Boolean.TRUE, "body", "yes", "at", Long.valueOf(70))));
        assertEquals(3, b.lines().size());
        // The watched thread replaces the lines; another conversation's is ignored.
        List<Object> sent = new ArrayList<Object>();
        sent.add(frame("id", "m9", "author", "Ben", "staff", Boolean.FALSE, "body", "x",
                "at", Long.valueOf(90)));
        assertFalse(b.thread(frame("chat", "c1", "messages", sent)));
        assertTrue(b.thread(frame("chat", "c2", "messages", sent)));
        assertEquals(1, b.lines().size());
        assertEquals(90, b.newestAt());
        b.close();
        assertEquals(InboxBoard.LISTED, b.message(frame("chat", "c2", "id", "m10",
                "staff", Boolean.FALSE, "at", Long.valueOf(95))));
        assertEquals(1, b.row("c2").unread, "closed, it counts again");
    }

    @Test
    void aRingMarksTheRowUntilTakenOrEnded() {
        InboxBoard b = board();
        assertTrue(b.call(frame("chat", "c1", "kind", "RING", "video", Boolean.TRUE)));
        assertEquals("video", b.row("c1").ringing);
        assertFalse(b.call(frame("chat", "c1", "kind", "OFFER")));
        assertTrue(b.call(frame("chat", "c1", "kind", "TAKEN")));
        assertEquals("", b.row("c1").ringing);
        assertFalse(b.call(frame("chat", "nope", "kind", "RING")));
    }

    @Test
    void framesReadEveryParserForm() {
        assertEquals(12, ChatFrames.number(frame("a", "12"), "a"));
        assertEquals(12, ChatFrames.number(frame("a", "12.0"), "a"));
        assertEquals(-3, ChatFrames.number(frame("a", Double.valueOf(-3)), "a"));
        assertEquals(0, ChatFrames.number(frame("a", "x"), "a"));
        assertTrue(ChatFrames.truth(frame("a", "true"), "a"));
        assertFalse(ChatFrames.truth(frame("a", "yes"), "a"));
        assertArrayEquals(new String[] {"stun:a", "turn:b"},
                ChatFrames.urls(frame("urls", Arrays.asList("stun:a", "turn:b"))));
        assertArrayEquals(new String[] {"stun:a"}, ChatFrames.urls(frame("urls", "stun:a")));
        assertEquals("ping", ChatFrames.ping().get("op"));
        Map<String, Object> ice = ChatFrames.ice("c1", null, 1, "candidate:1");
        Map c = ChatFrames.map(ice, "candidate");
        assertEquals("", c.get("sdpMid"));
        assertEquals(Integer.valueOf(1), c.get("sdpMLineIndex"));
        assertEquals("ICE", ice.get("kind"));
    }

    /// Records what the call asked of the media.
    private static final class FakeMedia implements ShopperCall.Media {
        String offer;
        boolean video;
        boolean released;
        final List<String> remote = new ArrayList<String>();

        @Override
        public void start(ShopperCall call, boolean video, String offer) {
            this.offer = offer;
            this.video = video;
        }

        @Override
        public void addRemoteCandidate(String sdpMid, int sdpMLineIndex, String candidate) {
            remote.add(sdpMid + "/" + sdpMLineIndex + "/" + candidate);
        }

        @Override
        public void release() {
            released = true;
        }
    }

    private static Map<String, Object> candidate(String text) {
        return frame("kind", "ICE", "candidate", frame("candidate", text, "sdpMid", "0",
                "sdpMLineIndex", Double.valueOf(0)));
    }

    @Test
    void anAnswerWaitsForTheOfferAndCandidatesWaitTheirTurn() {
        final List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        ShopperCall call = new ShopperCall("c1", false, out::add);
        FakeMedia media = new FakeMedia();
        assertFalse(call.canAccept(), "no offer yet");
        assertFalse(call.accept(media));
        call.signal(candidate("early"));
        call.signal(frame("kind", "OFFER", "sdp", "v=0\r\n", "video", Boolean.TRUE));
        assertTrue(call.canAccept());
        assertTrue(call.video(), "the offer asks for video");
        assertTrue(call.accept(media));
        assertEquals("v=0\r\n", media.offer);
        assertTrue(media.video);
        // Remote candidates wait for the remote description.
        call.signal(candidate("second"));
        assertTrue(media.remote.isEmpty());
        call.remoteSet();
        assertEquals(Arrays.asList("0/0/early", "0/0/second"), media.remote);
        call.signal(candidate("third"));
        assertEquals(3, media.remote.size());
        // Local candidates wait for the answer, which claims the call.
        call.localCandidate("0", 0, "mine");
        assertTrue(out.isEmpty());
        call.answered("v=0 answer");
        assertEquals(2, out.size());
        assertEquals("ANSWER", out.get(0).get("kind"));
        assertEquals("c1", out.get(0).get("chat"));
        assertEquals("ICE", out.get(1).get("kind"));
        call.localCandidate("0", 0, "later");
        assertEquals(3, out.size());
        call.connectedNow();
        assertTrue(call.connected());
        call.hangUp();
        assertEquals("END", out.get(3).get("kind"));
        assertTrue(media.released);
        assertEquals(ShopperCall.ENDED, call.state());
        call.localCandidate("0", 0, "after");
        assertEquals(4, out.size(), "nothing after the end");
    }

    @Test
    void takenMidAnswerReleasesAndDeclineSendsOnlyWhileRinging() {
        final List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        ShopperCall call = new ShopperCall("c1", true, out::add);
        FakeMedia media = new FakeMedia();
        call.signal(frame("kind", "OFFER", "sdp", "v=0"));
        call.accept(media);
        assertTrue(call.signal(frame("kind", "TAKEN")), "someone else answered");
        assertTrue(media.released);
        call.answered("too late");
        assertTrue(out.isEmpty(), "a lost answer is never sent");

        ShopperCall ringing = new ShopperCall("c2", false, out::add);
        ringing.decline();
        assertEquals(1, out.size());
        assertEquals("DECLINE", out.get(0).get("kind"));
        ringing.decline();
        ringing.hangUp();
        assertEquals(1, out.size(), "a declined call says nothing more");

        ShopperCall ended = new ShopperCall("c3", false, out::add);
        assertTrue(ended.signal(frame("kind", "END")));
        assertFalse(ended.canAccept());
        assertNull(ChatFrames.map(frame("candidate", "flat"), "candidate"));
    }
}
