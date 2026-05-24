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

import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/// SSE decoder for OpenAI-style `/chat/completions` streams. Also
/// drives Ollama, vLLM, and the other OpenAI-compatible endpoints
/// because the wire format is identical.
///
/// Accumulates assistant content + tool-call argument fragments and
/// assembles a final [ChatResponse] at stream close. Listener
/// callbacks are dispatched on the EDT via [Display#callSerially].
final class OpenAiSseDecoder implements StreamingChatRequest.SseDecoder {

    private final String requestedModel;
    private final StringBuilder content = new StringBuilder();
    private final List<StreamingToolCall> toolCalls = new ArrayList<StreamingToolCall>();
    private String finishReason;
    private Usage usage;
    private String modelUsed;

    OpenAiSseDecoder(String requestedModel) {
        this.requestedModel = requestedModel;
    }

    public void consume(String dataPayload, final StreamingListener listener) throws Exception {
        Map root = JsonHelper.parseObject(dataPayload);
        if (root == null) {
            return;
        }
        String modelInChunk = JsonHelper.string(root, "model");
        if (modelInChunk != null) {
            modelUsed = modelInChunk;
        }
        Map u = JsonHelper.asMap(root.get("usage"));
        if (u != null) {
            usage = new Usage(
                    JsonHelper.intValue(u, "prompt_tokens", -1),
                    JsonHelper.intValue(u, "completion_tokens", -1),
                    JsonHelper.intValue(u, "total_tokens", -1));
            final Usage emit = usage;
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    listener.onUsage(emit);
                }
            });
        }
        List<Object> choices = JsonHelper.asList(root.get("choices"));
        if (choices == null || choices.isEmpty()) {
            return;
        }
        Map choice = JsonHelper.asMap(choices.get(0));
        String fr = JsonHelper.string(choice, "finish_reason");
        if (fr != null) {
            finishReason = fr;
        }
        Map delta = JsonHelper.asMap(choice.get("delta"));
        if (delta == null) {
            // Non-streaming response shape can appear at the very end
            // for some servers — `message` instead of `delta`.
            delta = JsonHelper.asMap(choice.get("message"));
            if (delta == null) {
                return;
            }
        }
        String contentDelta = JsonHelper.string(delta, "content");
        if (contentDelta != null && contentDelta.length() > 0) {
            content.append(contentDelta);
            final String emit = contentDelta;
            Display.getInstance().callSerially(new Runnable() {
                public void run() {
                    listener.onContentDelta(emit);
                }
            });
        }
        List<Object> tcs = JsonHelper.asList(delta.get("tool_calls"));
        if (tcs != null) {
            for (int i = 0; i < tcs.size(); i++) {
                Map tc = JsonHelper.asMap(tcs.get(i));
                final int idx = JsonHelper.intValue(tc, "index", i);
                while (toolCalls.size() <= idx) {
                    toolCalls.add(new StreamingToolCall());
                }
                StreamingToolCall acc = toolCalls.get(idx);
                String id = JsonHelper.string(tc, "id");
                if (id != null) {
                    acc.id = id;
                }
                Map fn = JsonHelper.asMap(tc.get("function"));
                String name = fn == null ? null : JsonHelper.string(fn, "name");
                if (name != null) {
                    acc.name = name;
                }
                String argsFrag = fn == null ? null : JsonHelper.string(fn, "arguments");
                if (argsFrag != null) {
                    acc.arguments.append(argsFrag);
                }
                final String emitId = acc.id;
                final String emitName = name;
                final String emitArgs = argsFrag == null ? "" : argsFrag;
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        listener.onToolCallDelta(idx, emitId, emitName, emitArgs);
                    }
                });
            }
        }
    }

    public ChatResponse finish() {
        List<ToolCall> calls = new ArrayList<ToolCall>(toolCalls.size());
        for (int i = 0; i < toolCalls.size(); i++) {
            StreamingToolCall sc = toolCalls.get(i);
            calls.add(new ToolCall(sc.id, sc.name, sc.arguments.toString()));
        }
        ChatMessage assistant = new ChatMessage(Role.ASSISTANT,
                Arrays.<MessagePart>asList(new TextPart(content.toString())),
                calls, null, null);
        return new ChatResponse(assistant, calls,
                finishReason == null ? "stop" : finishReason,
                usage,
                modelUsed == null ? requestedModel : modelUsed);
    }

    public LlmException mapError(int httpStatus, String body) {
        return mapErrorStatic(httpStatus, body);
    }

    /// Shared with the non-streaming code path, hence static.
    static LlmException mapErrorStatic(int httpStatus, String body) {
        String code = null;
        String message = body;
        try {
            Map root = JsonHelper.parseObject(body);
            Map err = JsonHelper.asMap(root.get("error"));
            if (err != null) {
                code = JsonHelper.string(err, "code");
                String em = JsonHelper.string(err, "message");
                if (em != null) {
                    message = em;
                }
                String type = JsonHelper.string(err, "type");
                if ("context_length_exceeded".equals(code) || "context_length_exceeded".equals(type)) {
                    return new LlmContextLengthException(message, code, body);
                }
            }
        } catch (Exception ignored) {
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return new LlmAuthException(message, httpStatus, code, body);
        }
        if (httpStatus == 429) {
            return new LlmRateLimitException(message, -1, code, body);
        }
        if (httpStatus == 503 || httpStatus == 529) {
            return new LlmModelOverloadedException(message, httpStatus, code, body);
        }
        if (httpStatus >= 400 && httpStatus < 500) {
            return new LlmInvalidRequestException(message, httpStatus, code, body);
        }
        if (httpStatus >= 500) {
            return new LlmServerException(message, httpStatus, code, body);
        }
        return new LlmException(message, httpStatus, code, body, null);
    }

    /// Parses a single non-streaming response body into a
    /// `ChatResponse`. Shared with the synchronous `chat()` path.
    static ChatResponse parseNonStreaming(Map root) {
        StringBuilder content = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        String finishReason = "stop";

        List<Object> choices = JsonHelper.asList(root.get("choices"));
        if (choices != null && !choices.isEmpty()) {
            Map choice = JsonHelper.asMap(choices.get(0));
            String fr = JsonHelper.string(choice, "finish_reason");
            if (fr != null) {
                finishReason = fr;
            }
            Map msg = JsonHelper.asMap(choice.get("message"));
            if (msg != null) {
                String c = JsonHelper.string(msg, "content");
                if (c != null) {
                    content.append(c);
                }
                List<Object> tcs = JsonHelper.asList(msg.get("tool_calls"));
                if (tcs != null) {
                    for (int i = 0; i < tcs.size(); i++) {
                        Map tc = JsonHelper.asMap(tcs.get(i));
                        Map fn = JsonHelper.asMap(tc.get("function"));
                        toolCalls.add(new ToolCall(
                                JsonHelper.string(tc, "id"),
                                fn == null ? null : JsonHelper.string(fn, "name"),
                                fn == null ? null : JsonHelper.string(fn, "arguments")));
                    }
                }
            }
        }
        Map u = JsonHelper.asMap(root.get("usage"));
        Usage usage = u == null ? null : new Usage(
                JsonHelper.intValue(u, "prompt_tokens", -1),
                JsonHelper.intValue(u, "completion_tokens", -1),
                JsonHelper.intValue(u, "total_tokens", -1));

        ChatMessage assistant = new ChatMessage(Role.ASSISTANT,
                Arrays.<MessagePart>asList(new TextPart(content.toString())),
                toolCalls, null, null);
        return new ChatResponse(assistant, toolCalls, finishReason, usage,
                JsonHelper.string(root, "model"));
    }

    private static final class StreamingToolCall {
        String id;
        String name;
        StringBuilder arguments = new StringBuilder();
    }
}
