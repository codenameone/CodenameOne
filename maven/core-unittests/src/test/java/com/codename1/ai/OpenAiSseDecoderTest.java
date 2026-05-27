/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiSseDecoderTest {

    /**
     * The decoder ordinarily marshals deltas via Display.callSerially.
     * Outside a running Codename One UI (which is the case in
     * core-unittests) those callSerially blocks may not actually
     * execute. We can still verify the *accumulated* state by
     * driving the decoder directly and reading {@link OpenAiSseDecoder#finish()}.
     */
    @Test
    void aggregatesContentDeltasIntoFinalResponse() throws Exception {
        OpenAiSseDecoder dec = new OpenAiSseDecoder("gpt-4o-mini");
        RecordingListener listener = new RecordingListener();
        dec.consume("{\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{\"content\":\", \"}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{\"content\":\"world!\"}}],\"usage\":{\"prompt_tokens\":3,\"completion_tokens\":2,\"total_tokens\":5}}", listener);
        ChatResponse r = dec.finish();
        assertEquals("Hello, world!", r.getText());
        assertEquals(3, r.getUsage().getPromptTokens());
        assertEquals(5, r.getUsage().getTotalTokens());
        assertEquals("stop", r.getFinishReason());
    }

    @Test
    void capturesFinishReasonFromTerminalChunk() throws Exception {
        OpenAiSseDecoder dec = new OpenAiSseDecoder("gpt-4o-mini");
        RecordingListener listener = new RecordingListener();
        dec.consume("{\"choices\":[{\"delta\":{\"content\":\"x\"}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{},\"finish_reason\":\"length\"}]}", listener);
        assertEquals("length", dec.finish().getFinishReason());
    }

    @Test
    void reassemblesStreamedToolCallArguments() throws Exception {
        // OpenAI sends tool_calls in pieces: the first chunk has
        // name + id + the opening `{`; subsequent chunks each ship a
        // few more characters of arguments JSON. The aggregated
        // string must be reconstructable.
        OpenAiSseDecoder dec = new OpenAiSseDecoder("gpt-4o-mini");
        RecordingListener listener = new RecordingListener();
        dec.consume("{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"id\":\"call_abc\",\"type\":\"function\",\"function\":{\"name\":\"get_weather\",\"arguments\":\"\"}}]}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"{\\\"location\\\":\"}}]}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{\"tool_calls\":[{\"index\":0,\"function\":{\"arguments\":\"\\\"Paris\\\"}\"}}]}}]}", listener);
        dec.consume("{\"choices\":[{\"delta\":{},\"finish_reason\":\"tool_calls\"}]}", listener);
        ChatResponse r = dec.finish();
        assertEquals(1, r.getToolCalls().size());
        ToolCall tc = r.getToolCalls().get(0);
        assertEquals("call_abc", tc.getId());
        assertEquals("get_weather", tc.getName());
        assertEquals("{\"location\":\"Paris\"}", tc.getArgumentsJson());
        assertEquals("tool_calls", r.getFinishReason());
    }

    @Test
    void mapErrorRecognizes429AsRateLimit() {
        LlmException e = OpenAiSseDecoder.mapErrorStatic(429,
                "{\"error\":{\"message\":\"slow down\",\"type\":\"rate_limit\",\"code\":\"rate_limited\"}}");
        assertTrue((e instanceof LlmException && ((LlmException) e).getType() == LlmException.ErrorType.RATE_LIMIT));
        assertEquals(429, e.getHttpStatus());
    }

    @Test
    void mapErrorRecognizesContextLengthSubtype() {
        // 400 with an "context_length_exceeded" code/type is one of
        // the cases we want callers to handle specially -- they
        // typically respond by truncating older messages.
        LlmException e = OpenAiSseDecoder.mapErrorStatic(400,
                "{\"error\":{\"message\":\"too long\",\"type\":\"invalid_request_error\",\"code\":\"context_length_exceeded\"}}");
        assertTrue((e instanceof LlmException && ((LlmException) e).getType() == LlmException.ErrorType.CONTEXT_LENGTH),
                "expected LlmContextLengthException, got " + e.getClass().getSimpleName());
    }

    @Test
    void mapErrorRoutes401To403ToAuth() {
        assertEquals(LlmException.ErrorType.AUTH,
                OpenAiSseDecoder.mapErrorStatic(401, "{}").getType());
        assertEquals(LlmException.ErrorType.AUTH,
                OpenAiSseDecoder.mapErrorStatic(403, "{}").getType());
    }

    private static final class RecordingListener implements StreamingListener {
        final List<String> deltas = new ArrayList<String>();

        public void onContentDelta(String textDelta) {
            deltas.add(textDelta);
        }

        public void onToolCallDelta(int index, String id, String name, String argumentsFragment) {
        }

        public void onUsage(Usage usage) {
        }

        public void onError(Throwable t) {
        }
    }
}
