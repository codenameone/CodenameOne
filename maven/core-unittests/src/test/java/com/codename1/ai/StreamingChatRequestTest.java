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

import com.codename1.io.NetworkManager;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the provider-agnostic SSE plumbing in {@link StreamingChatRequest}
 * (line reassembly, {@code data:} extraction, the {@code [DONE]} sentinel,
 * clean completion, and HTTP-error mapping) by running a concrete subclass
 * through the mock network layer with a recording {@link StreamingChatRequest.SseDecoder}.
 */
class StreamingChatRequestTest extends UITestBase {

    private static final String URL = "http://sse.test/v1/stream";

    @AfterEach
    void clearMocks() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /** Records every event/finish/error the framework routes to it. */
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

    /** Minimal concrete request used purely to drive the abstract base class. */
    private static final class TestStreamingChatRequest extends StreamingChatRequest {
        TestStreamingChatRequest(StreamingListener listener,
                                 AsyncResource<ChatResponse> result,
                                 SseDecoder decoder) {
            super(URL, utf8("{}"), listener, result, decoder);
            // Suppress the framework's default unhandled-error dialog so the
            // 4xx/5xx path reaches StreamingChatRequest.readResponse() (which
            // opts into reading the error body) instead of blocking on a modal.
            setFailSilently(true);
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

    /** Runs a request against a mock SSE response and returns once settled. */
    private Outcome run(int status, String body, RecordingDecoder decoder) {
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, status, status == 200 ? "OK" : "ERR", utf8(body));
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
        TestStreamingChatRequest req = new TestStreamingChatRequest(listener, result, decoder);
        NetworkManager.getInstance().addToQueueAndWait(req);

        int budget = 8000;
        while (!result.isDone() && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 5;
        }
        DisplayTest.flushEdt();
        assertTrue(result.isDone(), "streaming request did not settle within the timeout");
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
