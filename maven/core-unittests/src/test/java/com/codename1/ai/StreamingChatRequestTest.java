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
package com.codename1.ai;

import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the provider-agnostic SSE plumbing in {@link StreamingChatRequest}
 * (line reassembly, {@code data:} extraction, the {@code [DONE]} sentinel,
 * clean completion, and HTTP-error mapping) by feeding a controlled response
 * body straight into {@code readResponse} with a recording
 * {@link StreamingChatRequest.SseDecoder}.
 *
 * <p>The request is driven directly rather than through {@code NetworkManager}
 * so the test is deterministic and never depends on the network worker thread
 * (the blocking enqueue path can deadlock under some JDKs). {@code readResponse}
 * is {@code protected} and reachable here because the test shares the
 * {@code com.codename1.ai} package; the subclass overrides {@code getResponseCode}
 * to select the HTTP status under test.
 */
class StreamingChatRequestTest extends UITestBase {

    private static final String URL = "http://sse.test/v1/stream";

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** Records every event/finish/error the request routes to it. */
    private static final class RecordingDecoder implements StreamingChatRequest.SseDecoder {
        final List<String> events = new CopyOnWriteArrayList<String>();
        final ChatResponse finishResponse =
                new ChatResponse(ChatMessage.assistant("done"), null, "stop", null, "m");
        final LlmException mappedError =
                new LlmException("mapped", 500, "server_error", null, null, LlmException.ErrorType.SERVER);
        volatile int mapErrorStatus = -1;
        volatile String mapErrorBody;
        volatile boolean consumeThrows;

        public void consume(String dataPayload, StreamingListener listener) throws Exception {
            if (consumeThrows) {
                throw new IllegalStateException("decoder rejected payload");
            }
            events.add(dataPayload);
        }

        public ChatResponse finish() {
            return finishResponse;
        }

        public LlmException mapError(int httpStatus, String body) {
            this.mapErrorStatus = httpStatus;
            this.mapErrorBody = body;
            return mappedError;
        }
    }

    /** Minimal concrete request that lets the test pick the HTTP status. */
    private static final class TestStreamingChatRequest extends StreamingChatRequest {
        private final int status;

        TestStreamingChatRequest(int status, StreamingListener listener,
                                 AsyncResource<ChatResponse> result, SseDecoder decoder) {
            super(URL, utf8("{}"), listener, result, decoder);
            this.status = status;
        }

        @Override
        public int getResponseCode() {
            return status;
        }
    }

    /** The settled state of a streaming request. */
    private static final class Outcome {
        final ChatResponse value;
        final Throwable error;

        Outcome(ChatResponse value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }

    /** Feeds {@code body} through {@code readResponse} at the given status and
     * returns once the result settles. */
    private Outcome run(int status, String body, RecordingDecoder decoder) {
        AsyncResource<ChatResponse> result = new AsyncResource<ChatResponse>();
        final AtomicReference<ChatResponse> value = new AtomicReference<ChatResponse>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        result.ready(new SuccessCallback<ChatResponse>() {
            public void onSucess(ChatResponse v) {
                value.set(v);
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
            }
        });
        StreamingListener listener = new StreamingListener.Adapter();
        TestStreamingChatRequest req = new TestStreamingChatRequest(status, listener, result, decoder);
        try {
            req.readResponse(new ByteArrayInputStream(utf8(body)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // completeWith / failWith marshal the settle onto the EDT via callSerially.
        int budget = 200;
        while (!result.isDone() && budget > 0) {
            DisplayTest.flushEdt();
            budget--;
        }
        DisplayTest.flushEdt();
        assertTrue(result.isDone(), "streaming request did not settle");
        return new Outcome(value.get(), error.get());
    }

    @Test
    void parsesDataLinesAndCompletesWithDecoderFinish() {
        RecordingDecoder d = new RecordingDecoder();
        Outcome r = run(200, "data: hello\n\ndata: world\n\n", d);
        assertNull(r.error);
        assertSame(d.finishResponse, r.value);
        assertEquals(java.util.Arrays.asList("hello", "world"), d.events);
    }

    @Test
    void joinsMultipleDataLinesWithinOneEvent() {
        RecordingDecoder d = new RecordingDecoder();
        run(200, "data: line1\ndata: line2\n\n", d);
        assertEquals(Collections.singletonList("line1\nline2"), d.events);
    }

    @Test
    void doneSentinelIsNotForwardedToDecoder() {
        RecordingDecoder d = new RecordingDecoder();
        Outcome r = run(200, "data: [DONE]\n\n", d);
        assertTrue(d.events.isEmpty());
        assertSame(d.finishResponse, r.value);
    }

    @Test
    void handlesCrlfLineEndings() {
        RecordingDecoder d = new RecordingDecoder();
        run(200, "data: crlf\r\n\r\n", d);
        assertEquals(Collections.singletonList("crlf"), d.events);
    }

    @Test
    void stripsSingleLeadingSpaceAfterColon() {
        RecordingDecoder d = new RecordingDecoder();
        run(200, "data:  twospace\n\n", d);
        // Exactly one leading space is stripped, leaving the rest intact.
        assertEquals(Collections.singletonList(" twospace"), d.events);
    }

    @Test
    void dispatchesTrailingEventWithoutBlankLineTerminator() {
        RecordingDecoder d = new RecordingDecoder();
        run(200, "data: trailing\n", d);
        assertEquals(Collections.singletonList("trailing"), d.events);
    }

    @Test
    void ignoresNonDataLines() {
        RecordingDecoder d = new RecordingDecoder();
        run(200, "event: ping\nid: 7\n:comment\ndata: payload\n\n", d);
        assertEquals(Collections.singletonList("payload"), d.events);
    }

    @Test
    void httpErrorStatusIsMappedThroughDecoder() {
        RecordingDecoder d = new RecordingDecoder();
        Outcome r = run(500, "boom-body", d);
        assertNull(r.value);
        assertSame(d.mappedError, r.error);
        assertEquals(500, d.mapErrorStatus);
        assertEquals("boom-body", d.mapErrorBody);
        assertTrue(d.events.isEmpty());
    }

    @Test
    void decoderExceptionFailsTheResult() {
        RecordingDecoder d = new RecordingDecoder();
        d.consumeThrows = true;
        Outcome r = run(200, "data: anything\n\n", d);
        assertNull(r.value);
        assertNotNull(r.error);
        assertTrue(r.error instanceof IllegalStateException);
    }
}
