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

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises {@link OpenAiClient} (reached through the
 * {@link LlmClient#localOpenAiCompatible} factory, which on the
 * non-simulator test platform returns a bare {@code OpenAiClient}) against
 * the mock network layer in {@link TestCodenameOneImplementation}. Covers
 * configuration, the safety-filter guard, and the chat / embeddings
 * request-build + response-parse + error-mapping paths that pure-logic
 * tests cannot reach.
 */
class OpenAiClientTest extends UITestBase {

    private static final String BASE_URL = "http://llm.test/v1";
    private static final String CHAT_URL = BASE_URL + "/chat/completions";
    private static final String EMBED_URL = BASE_URL + "/embeddings";

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

    private void mock(String url, int code, String body) {
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(url, code, code == 200 ? "OK" : "ERR", utf8(body));
    }

    private LlmClient client() {
        return LlmClient.localOpenAiCompatible(BASE_URL, "secret-key", "test-model");
    }

    private ChatRequest userChat(String text) {
        return ChatRequest.builder().addMessage(ChatMessage.user(text)).build();
    }

    /** Blocks (driving the EDT) until the resource settles, then returns it. */
    private <T> Outcome<T> await(AsyncResource<T> resource) {
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);
        resource.ready(new SuccessCallback<T>() {
            public void onSucess(T v) {
                value.set(v);
                latch.countDown();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });
        int budget = 20000;
        while (latch.getCount() > 0 && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 10;
        }
        assertTrue(resource.isDone(), "async resource did not settle within the timeout");
        return new Outcome<T>(value.get(), error.get());
    }

    // ---- configuration (no network) ----------------------------------

    @Test
    void providerIsOpenAi() {
        assertEquals("openai", client().getProvider());
    }

    @Test
    void baseUrlAndTimeoutAreConfigurable() {
        LlmClient c = client();
        assertEquals(BASE_URL, c.getBaseUrl());
        assertEquals(60000, c.getHttpTimeoutMs());
        c.setBaseUrl("http://other/v2");
        c.setHttpTimeoutMs(1234);
        assertEquals("http://other/v2", c.getBaseUrl());
        assertEquals(1234, c.getHttpTimeoutMs());
    }

    @Test
    void ollamaFactoryUsesOllamaBaseUrl() {
        LlmClient c = LlmClient.ollama();
        assertEquals("openai", c.getProvider());
        assertEquals(LlmClient.DEFAULT_OLLAMA_URL, c.getBaseUrl());
    }

    // ---- safety filter (short-circuits before the network) -----------

    @Test
    void chatBlockedBySafetyFilterFailsWithoutNetwork() {
        ChatRequest req = ChatRequest.builder()
                .addMessage(ChatMessage.user("anything"))
                .safetyFilter(new SafetyFilter() {
                    public String check(List<ChatMessage> messages) {
                        return "policy violation";
                    }
                })
                .build();
        Outcome<ChatResponse> r = await(client().chat(req));
        assertNull(r.value);
        assertInstanceOf(LlmException.class, r.error);
        LlmException ex = (LlmException) r.error;
        assertEquals(LlmException.ErrorType.INVALID_REQUEST, ex.getType());
        assertTrue(ex.getMessage().contains("policy violation"));
    }

    // ---- chat success / parsing --------------------------------------

    @Test
    void chatParsesAssistantContentAndUsage() {
        mock(CHAT_URL, 200,
                "{\"model\":\"test-model\",\"choices\":[{\"finish_reason\":\"stop\","
                        + "\"message\":{\"role\":\"assistant\",\"content\":\"Hello there\"}}],"
                        + "\"usage\":{\"prompt_tokens\":7,\"completion_tokens\":3,\"total_tokens\":10}}");

        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.error);
        assertNotNull(r.value);
        assertEquals("Hello there", r.value.getText());
        assertEquals("stop", r.value.getFinishReason());
        assertEquals("test-model", r.value.getModelUsed());
        assertNotNull(r.value.getUsage());
        assertEquals(7, r.value.getUsage().getPromptTokens());
        assertEquals(10, r.value.getUsage().getTotalTokens());
        assertTrue(r.value.getToolCalls().isEmpty());
    }

    @Test
    void chatParsesToolCalls() {
        mock(CHAT_URL, 200,
                "{\"choices\":[{\"finish_reason\":\"tool_calls\",\"message\":{"
                        + "\"role\":\"assistant\",\"content\":null,\"tool_calls\":[{"
                        + "\"id\":\"call_1\",\"type\":\"function\",\"function\":{"
                        + "\"name\":\"get_weather\",\"arguments\":\"{\\\"city\\\":\\\"NYC\\\"}\"}}]}}]}");

        Outcome<ChatResponse> r = await(client().chat(userChat("weather?")));
        assertNull(r.error);
        assertEquals("tool_calls", r.value.getFinishReason());
        assertEquals(1, r.value.getToolCalls().size());
        ToolCall tc = r.value.getToolCalls().get(0);
        assertEquals("call_1", tc.getId());
        assertEquals("get_weather", tc.getName());
        assertTrue(tc.getArgumentsJson().contains("NYC"));
    }

    @Test
    void chatDefaultsFinishReasonToStopWhenAbsent() {
        mock(CHAT_URL, 200,
                "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.error);
        assertEquals("stop", r.value.getFinishReason());
        assertEquals("ok", r.value.getText());
    }

    // ---- chat error handling -----------------------------------------
    //
    // configureRequest sets readResponseForErrors=true and the client
    // suppresses handleErrorResponseCode, so the framework routes HTTP
    // 4xx/5xx through postResponse() (never handleException). postResponse
    // inspects the status code and routes errors through
    // OpenAiSseDecoder.mapErrorStatic so a typed LlmException is surfaced
    // instead of an error envelope being mis-parsed as an empty success.

    @Test
    void chatSurfaces401AsAuthError() {
        mock(CHAT_URL, 401,
                "{\"error\":{\"code\":\"invalid_api_key\",\"message\":\"bad key\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertInstanceOf(LlmException.class, r.error);
        LlmException ex = (LlmException) r.error;
        assertEquals(LlmException.ErrorType.AUTH, ex.getType());
        assertEquals(401, ex.getHttpStatus());
        assertEquals("invalid_api_key", ex.getProviderErrorCode());
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    void chatSurfaces429AsRateLimit() {
        mock(CHAT_URL, 429,
                "{\"error\":{\"message\":\"slow down\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertInstanceOf(LlmException.class, r.error);
        assertEquals(LlmException.ErrorType.RATE_LIMIT, ((LlmException) r.error).getType());
    }

    @Test
    void chatSurfacesGeneric400AsInvalidRequest() {
        mock(CHAT_URL, 400, "{\"error\":{\"message\":\"bad request\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertEquals(LlmException.ErrorType.INVALID_REQUEST,
                ((LlmException) r.error).getType());
    }

    @Test
    void chatSurfaces500AsServerError() {
        mock(CHAT_URL, 500, "{\"error\":{\"message\":\"boom\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertEquals(LlmException.ErrorType.SERVER, ((LlmException) r.error).getType());
    }

    @Test
    void chatSurfaces503AsModelOverloaded() {
        mock(CHAT_URL, 503, "{\"error\":{\"message\":\"overloaded\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertEquals(LlmException.ErrorType.MODEL_OVERLOADED,
                ((LlmException) r.error).getType());
    }

    @Test
    void chatMapsContextLengthExceededRegardlessOfStatus() {
        mock(CHAT_URL, 400,
                "{\"error\":{\"code\":\"context_length_exceeded\","
                        + "\"message\":\"too many tokens\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertEquals(LlmException.ErrorType.CONTEXT_LENGTH,
                ((LlmException) r.error).getType());
    }

    @Test
    void chatSurfaces200WithErrorEnvelopeAsError() {
        // Some OpenAI-compatible servers answer HTTP 200 with an {"error":...}
        // body; that must still surface as an LlmException, not a blank reply.
        mock(CHAT_URL, 200,
                "{\"error\":{\"code\":\"server_error\",\"message\":\"oops\"}}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.value);
        assertInstanceOf(LlmException.class, r.error);
        assertEquals("oops", ((LlmException) r.error).getMessage());
    }

    @Test
    void chatWithNoChoicesYieldsEmptyResponse() {
        // A 200 body with no "choices" array still parses cleanly; the
        // lenient parser produces an empty assistant message.
        mock(CHAT_URL, 200, "{\"model\":\"test-model\"}");
        Outcome<ChatResponse> r = await(client().chat(userChat("hi")));
        assertNull(r.error);
        assertNotNull(r.value);
        assertEquals("", r.value.getText());
        assertEquals("stop", r.value.getFinishReason());
        assertNull(r.value.getUsage());
    }

    // ---- embeddings --------------------------------------------------

    @Test
    void embedParsesVectorsAndUsage() {
        mock(EMBED_URL, 200,
                "{\"model\":\"embed-1\",\"data\":[{\"index\":0,\"embedding\":[0.1,0.2,0.3]}],"
                        + "\"usage\":{\"prompt_tokens\":5,\"total_tokens\":5}}");

        EmbeddingRequest req = EmbeddingRequest.of("embed-1", "hello world");
        Outcome<EmbeddingResponse> r = await(client().embed(req));
        assertNull(r.error);
        assertNotNull(r.value);
        assertEquals("embed-1", r.value.getModelUsed());
        assertEquals(1, r.value.getData().size());
        Embedding e = r.value.getData().get(0);
        assertEquals(0, e.getIndex());
        assertEquals(3, e.getVector().length);
        assertEquals(0.2f, e.getVector()[1], 1e-6f);
        assertNotNull(r.value.getUsage());
        assertEquals(5, r.value.getUsage().getPromptTokens());
    }

    @Test
    void embedHandlesMultipleInputs() {
        mock(EMBED_URL, 200,
                "{\"data\":[{\"index\":0,\"embedding\":[1.0]},"
                        + "{\"index\":1,\"embedding\":[2.0]}]}");

        EmbeddingRequest req = EmbeddingRequest.builder()
                .model("embed-1")
                .addInput("a")
                .addInput("b")
                .dimensions(1)
                .build();
        Outcome<EmbeddingResponse> r = await(client().embed(req));
        assertNull(r.error);
        assertEquals(2, r.value.getData().size());
        assertEquals(1, r.value.getData().get(1).getIndex());
        assertEquals(2.0f, r.value.getData().get(1).getVector()[0], 1e-6f);
    }

    @Test
    void embedWithNoDataYieldsEmptyResponse() {
        // A 200 body lacking a "data" array parses cleanly to an empty
        // embedding list rather than failing.
        mock(EMBED_URL, 200, "{\"model\":\"embed-1\"}");
        Outcome<EmbeddingResponse> r = await(client().embed(EmbeddingRequest.of("embed-1", "x")));
        assertNull(r.error);
        assertNotNull(r.value);
        assertTrue(r.value.getData().isEmpty());
        assertNull(r.value.getUsage());
    }

    @Test
    void embedSurfacesHttpErrorAsTypedException() {
        // The same error-routing fix applies to the embeddings endpoint.
        mock(EMBED_URL, 401,
                "{\"error\":{\"code\":\"invalid_api_key\",\"message\":\"bad key\"}}");
        Outcome<EmbeddingResponse> r = await(client().embed(EmbeddingRequest.of("embed-1", "x")));
        assertNull(r.value);
        assertInstanceOf(LlmException.class, r.error);
        assertEquals(LlmException.ErrorType.AUTH, ((LlmException) r.error).getType());
    }

    // ---- helpers -----------------------------------------------------

    private static final class Outcome<T> {
        final T value;
        final Throwable error;

        Outcome(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }
}
