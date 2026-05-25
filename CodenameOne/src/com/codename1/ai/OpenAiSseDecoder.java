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

import com.codename1.io.JSONParser;
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
    // The decoder accumulates tokens for a single short-lived SSE
    // stream; not a long-running container that risks growing
    // unbounded -- AvoidStringBufferField doesn't apply here.
    @SuppressWarnings("PMD.AvoidStringBufferField")
    private final StringBuilder content = new StringBuilder();
    private final List<StreamingToolCall> toolCalls = new ArrayList<StreamingToolCall>();
    private String finishReason;
    private Usage usage;
    private String modelUsed;

    OpenAiSseDecoder(String requestedModel) {
        this.requestedModel = requestedModel;
    }

    @Override
    public void consume(String dataPayload, final StreamingListener listener) throws Exception {
        Map root = JSONParser.parseJSON(dataPayload);
        if (root == null) {
            return;
        }
        String modelInChunk = JSONParser.getString(root, "model");
        if (modelInChunk != null) {
            modelUsed = modelInChunk;
        }
        Map u = JSONParser.asMap(root.get("usage"));
        if (u != null) {
            usage = new Usage(
                    JSONParser.getInt(u, "prompt_tokens", -1),
                    JSONParser.getInt(u, "completion_tokens", -1),
                    JSONParser.getInt(u, "total_tokens", -1));
            final Usage emit = usage;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    listener.onUsage(emit);
                }
            });
        }
        List<Object> choices = JSONParser.asList(root.get("choices"));
        if (choices == null || choices.isEmpty()) {
            return;
        }
        Map choice = JSONParser.asMap(choices.get(0));
        String fr = JSONParser.getString(choice, "finish_reason");
        if (fr != null) {
            finishReason = fr;
        }
        Map delta = JSONParser.asMap(choice.get("delta"));
        if (delta == null) {
            // Non-streaming response shape can appear at the very end
            // for some servers -- `message` instead of `delta`.
            delta = JSONParser.asMap(choice.get("message"));
            if (delta == null) {
                return;
            }
        }
        String contentDelta = JSONParser.getString(delta, "content");
        if (contentDelta != null && contentDelta.length() > 0) {
            content.append(contentDelta);
            final String emit = contentDelta;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    listener.onContentDelta(emit);
                }
            });
        }
        List<Object> tcs = JSONParser.asList(delta.get("tool_calls"));
        if (tcs != null) {
            int i = 0;
            for (Object rawTc : tcs) {
                Map tc = JSONParser.asMap(rawTc);
                final int idx = JSONParser.getInt(tc, "index", i);
                while (toolCalls.size() <= idx) {
                    toolCalls.add(new StreamingToolCall());
                }
                StreamingToolCall acc = toolCalls.get(idx);
                String id = JSONParser.getString(tc, "id");
                if (id != null) {
                    acc.id = id;
                }
                Map fn = JSONParser.asMap(tc.get("function"));
                String name = fn == null ? null : JSONParser.getString(fn, "name");
                if (name != null) {
                    acc.name = name;
                }
                String argsFrag = fn == null ? null : JSONParser.getString(fn, "arguments");
                if (argsFrag != null) {
                    acc.arguments.append(argsFrag);
                }
                final String emitId = acc.id;
                final String emitName = name;
                final String emitArgs = argsFrag == null ? "" : argsFrag;
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        listener.onToolCallDelta(idx, emitId, emitName, emitArgs);
                    }
                });
                i++;
            }
        }
    }

    @Override
    public ChatResponse finish() {
        List<ToolCall> calls = new ArrayList<ToolCall>(toolCalls.size());
        for (StreamingToolCall sc : toolCalls) {
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

    @Override
    public LlmException mapError(int httpStatus, String body) {
        return mapErrorStatic(httpStatus, body);
    }

    /// Shared with the non-streaming code path, hence static.
    static LlmException mapErrorStatic(int httpStatus, String body) {
        String code = null;
        String message = body;
        try {
            Map root = JSONParser.parseJSON(body);
            Map err = JSONParser.asMap(root.get("error"));
            if (err != null) {
                code = JSONParser.getString(err, "code");
                String em = JSONParser.getString(err, "message");
                if (em != null) {
                    message = em;
                }
                String type = JSONParser.getString(err, "type");
                if ("context_length_exceeded".equals(code) || "context_length_exceeded".equals(type)) {
                    return new LlmException(message, 400, code, body, null, LlmException.ErrorType.CONTEXT_LENGTH);
                }
            }
        } catch (java.io.IOException ignored) {
            // Body wasn't valid JSON; fall through to status-only mapping.
        } catch (RuntimeException ignored) {
            // Cast / NPE while walking the error map; same fallback.
        }
        if (httpStatus == 401 || httpStatus == 403) {
            return new LlmException(message, httpStatus, code, body, null, LlmException.ErrorType.AUTH);
        }
        if (httpStatus == 429) {
            return new LlmException(message, 429, code, body, null, LlmException.ErrorType.RATE_LIMIT, -1);
        }
        if (httpStatus == 503 || httpStatus == 529) {
            return new LlmException(message, httpStatus, code, body, null, LlmException.ErrorType.MODEL_OVERLOADED);
        }
        if (httpStatus >= 400 && httpStatus < 500) {
            return new LlmException(message, httpStatus, code, body, null, LlmException.ErrorType.INVALID_REQUEST);
        }
        if (httpStatus >= 500) {
            return new LlmException(message, httpStatus, code, body, null, LlmException.ErrorType.SERVER);
        }
        return new LlmException(message, httpStatus, code, body, null, LlmException.ErrorType.UNKNOWN);
    }

    /// Parses a single non-streaming response body into a
    /// `ChatResponse`. Shared with the synchronous `chat()` path.
    static ChatResponse parseNonStreaming(Map root) {
        StringBuilder content = new StringBuilder();
        List<ToolCall> toolCalls = new ArrayList<ToolCall>();
        String finishReason = "stop";

        List<Object> choices = JSONParser.asList(root.get("choices"));
        if (choices != null && !choices.isEmpty()) {
            Map choice = JSONParser.asMap(choices.get(0));
            String fr = JSONParser.getString(choice, "finish_reason");
            if (fr != null) {
                finishReason = fr;
            }
            Map msg = JSONParser.asMap(choice.get("message"));
            if (msg != null) {
                String c = JSONParser.getString(msg, "content");
                if (c != null) {
                    content.append(c);
                }
                List<Object> tcs = JSONParser.asList(msg.get("tool_calls"));
                if (tcs != null) {
                    for (Object rawTc : tcs) {
                        Map tc = JSONParser.asMap(rawTc);
                        Map fn = JSONParser.asMap(tc.get("function"));
                        toolCalls.add(new ToolCall(
                                JSONParser.getString(tc, "id"),
                                fn == null ? null : JSONParser.getString(fn, "name"),
                                fn == null ? null : JSONParser.getString(fn, "arguments")));
                    }
                }
            }
        }
        Map u = JSONParser.asMap(root.get("usage"));
        Usage usage = u == null ? null : new Usage(
                JSONParser.getInt(u, "prompt_tokens", -1),
                JSONParser.getInt(u, "completion_tokens", -1),
                JSONParser.getInt(u, "total_tokens", -1));

        ChatMessage assistant = new ChatMessage(Role.ASSISTANT,
                Arrays.<MessagePart>asList(new TextPart(content.toString())),
                toolCalls, null, null);
        return new ChatResponse(assistant, toolCalls, finishReason, usage,
                JSONParser.getString(root, "model"));
    }

    private static final class StreamingToolCall {
        String id;
        String name;
        // Accumulates argument fragments for one tool call only;
        // same justification as `content` above.
        @SuppressWarnings("PMD.AvoidStringBufferField")
        StringBuilder arguments = new StringBuilder();
    }
}
