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

import com.codename1.impl.LocalLlmImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.EasyThread;

import java.util.ArrayList;
import java.util.List;

/// An [LlmClient] that runs a GGUF model on the device with llama.cpp. Create
/// one with [LlmClient#local(String)].
///
/// Nothing leaves the device and no network is needed. The model file is
/// loaded on the first request, which can take several seconds for a large
/// model, and stays in memory until [#close()].
///
/// ```java
/// LocalLlmClient llm = LlmClient.local(FileSystemStorage.getInstance()
///         .getAppHomePath() + "qwen2.5-0.5b-instruct-q4_k_m.gguf");
/// llm.chat(ChatRequest.builder()
///         .addMessage(ChatMessage.user("Name three primes"))
///         .build()).ready(r -> Log.p(r.getText()));
/// ```
///
/// The prompt is rendered by the chat template the model file carries, so any
/// instruction-tuned GGUF works without configuration.
/// [ResponseFormat#JSON_OBJECT] is enforced with a grammar, so the output is
/// always valid JSON. Tools and image parts are not supported and fail the
/// request with [LlmException.ErrorType#INVALID_REQUEST].
///
/// #### Availability
///
/// Android and iOS. The builders link llama.cpp only into applications that
/// call [LlmClient#local(String)]. Elsewhere, the simulator included, every
/// request fails with [LlmException.ErrorType#INVALID_REQUEST]; use
/// [LlmClient#ollama()] to develop against the same model on the desktop.
///
/// Requests run one at a time on a background thread, in the order they were
/// made. Results and streaming callbacks arrive on the EDT.
public final class LocalLlmClient extends LlmClient {
    /// Grammar for any JSON object, used for [ResponseFormat#JSON_OBJECT].
    static final String JSON_GRAMMAR =
            "root ::= object\n"
            + "value ::= object | array | string | number | (\"true\" | \"false\" | \"null\") ws\n"
            + "object ::= \"{\" ws ( string \":\" ws value (\",\" ws string \":\" ws value)* )? \"}\" ws\n"
            + "array ::= \"[\" ws ( value (\",\" ws value)* )? \"]\" ws\n"
            + "string ::= \"\\\"\" ( [^\"\\\\\\x7F\\x00-\\x1F] | \"\\\\\" ([\"\\\\/bfnrt] | \"u\" [0-9a-fA-F]{4}) )* \"\\\"\" ws\n"
            + "number ::= (\"-\"? ([0-9] | [1-9] [0-9]*)) (\".\" [0-9]+)? ([eE] [-+]? [0-9]+)? ws\n"
            + "ws ::= [ \\t\\n]*\n";

    private static final int DEFAULT_MAX_TOKENS = 512;

    private final String modelPath;
    private int contextSize;
    private int gpuLayers = -1;
    private int threads;
    private EasyThread worker;
    private LocalLlmImpl impl;
    private int handle;

    LocalLlmClient(String modelPath) {
        super("");
        if (modelPath == null || modelPath.length() == 0) {
            throw new IllegalArgumentException("modelPath is required");
        }
        this.modelPath = modelPath;
    }

    /// @return the model file this client loads
    public String getModelPath() {
        return modelPath;
    }

    /// Sets the context window. Takes effect when the model is next loaded.
    ///
    /// @param tokens context size in tokens, `0` for the model's default
    /// @return this client
    public LocalLlmClient setContextSize(int tokens) {
        this.contextSize = tokens;
        return this;
    }

    /// Sets how many layers run on the GPU (Metal on iOS). Takes effect when
    /// the model is next loaded.
    ///
    /// @param layers layer count, `0` for CPU only, `-1` (the default) for all
    /// @return this client
    public LocalLlmClient setGpuLayers(int layers) {
        this.gpuLayers = layers;
        return this;
    }

    /// Sets the CPU threads used for generation. Takes effect when the model
    /// is next loaded.
    ///
    /// @param threads thread count, `0` (the default) to let the port choose
    /// @return this client
    public LocalLlmClient setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    /// Whether this platform can run local models at all. `false` in the
    /// simulator and on any target the builder did not link llama.cpp into.
    ///
    /// @return `true` when requests can succeed
    public static boolean isSupported() {
        return Display.getInstance().getLocalLlmBackend() != null;
    }

    @Override
    public String getProvider() {
        return "llama.cpp";
    }

    @Override
    public AsyncResource<ChatResponse> chat(ChatRequest req) {
        return chatStream(req, null);
    }

    @Override
    public AsyncResource<ChatResponse> chatStream(final ChatRequest req,
            final StreamingListener listener) {
        final AsyncResource<ChatResponse> result = new AsyncResource<ChatResponse>();
        String reject = runSafetyFilter(req);
        if (reject != null) {
            result.error(new LlmException("Blocked by safety filter: " + reject,
                    400, "safety_filter", null, null, LlmException.ErrorType.INVALID_REQUEST));
            return result;
        }
        String unsupported = unsupportedFeature(req);
        if (unsupported != null) {
            result.error(invalid(unsupported));
            return result;
        }
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    deliver(result, generate(req, listener), null, listener);
                } catch (LlmException ex) {
                    deliver(result, null, ex, listener);
                }
            }
        }, result, listener);
        return result;
    }

    @Override
    public AsyncResource<EmbeddingResponse> embed(final EmbeddingRequest req) {
        final AsyncResource<EmbeddingResponse> result = new AsyncResource<EmbeddingResponse>();
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalLlmImpl llm = ensureOpen();
                    List<String> inputs = req.getInputs();
                    List<Embedding> data = new ArrayList<Embedding>(inputs.size());
                    for (int i = 0; i < inputs.size(); i++) {
                        float[] v = llm.embed(handle, inputs.get(i));
                        if (v == null) {
                            throw failure("Embedding failed");
                        }
                        data.add(new Embedding(v, i));
                    }
                    final EmbeddingResponse r = new EmbeddingResponse(data, null, modelName());
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            result.complete(r);
                        }
                    });
                } catch (final LlmException ex) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            result.error(ex);
                        }
                    });
                }
            }
        }, result, null);
        return result;
    }

    /// Unloads the model and stops the worker thread. Requests already queued
    /// still run first; a later request loads the model again.
    public void close() {
        if (worker == null) {
            return;
        }
        final EasyThread w = worker;
        worker = null;
        w.run(new Runnable() {
            @Override
            public void run() {
                if (impl != null && handle != 0) {
                    impl.close(handle);
                }
                handle = 0;
            }
        });
        w.killWhenIdle();
    }

    private void submit(Runnable task, AsyncResource<?> result, StreamingListener listener) {
        if (Display.getInstance().getLocalLlmBackend() == null) {
            LlmException ex = invalid("Local models are not available on this platform; "
                    + "they run on Android and iOS devices. Use LlmClient.ollama() "
                    + "on the desktop.");
            if (listener != null) {
                listener.onError(ex);
            }
            result.error(ex);
            return;
        }
        if (worker == null) {
            worker = EasyThread.start("llama.cpp");
        }
        worker.run(task);
    }

    /// Runs on the worker thread.
    private ChatResponse generate(ChatRequest req, final StreamingListener listener)
            throws LlmException {
        LocalLlmImpl llm = ensureOpen();
        List<ChatMessage> messages = req.getMessages();
        String[] roles = new String[messages.size()];
        String[] contents = new String[messages.size()];
        for (int i = 0; i < roles.length; i++) {
            ChatMessage m = messages.get(i);
            roles[i] = roleName(m.getRole());
            contents[i] = m.getText();
        }
        int maxTokens = req.getMaxTokens() == null ? DEFAULT_MAX_TOKENS
                : req.getMaxTokens().intValue();
        float temperature = req.getTemperature() == null ? 0.8f
                : req.getTemperature().floatValue();
        float topP = req.getTopP() == null ? 0.95f : req.getTopP().floatValue();
        int seed = req.getSeed() == null ? -1 : (int) req.getSeed().longValue();
        String grammar = req.getResponseFormat() == ResponseFormat.JSON_OBJECT
                ? JSON_GRAMMAR : null;
        if (!llm.start(handle, roles, contents, maxTokens, temperature, topP, seed, grammar)) {
            String err = llm.getLastError(handle);
            throw new LlmException(err == null ? "The prompt could not be evaluated" : err,
                    -1, null, null, null, LlmException.ErrorType.CONTEXT_LENGTH);
        }
        StopScanner stops = new StopScanner(req.getStopSequences());
        String finish = "length";
        String piece = llm.next(handle);
        while (piece != null) {
            String delta = stops.append(piece);
            emit(listener, delta);
            if (stops.isStopped()) {
                finish = "stop";
                break;
            }
            piece = llm.next(handle);
        }
        if (piece == null) {
            emit(listener, stops.flush());
            if (llm.getCompletionTokens(handle) < maxTokens) {
                finish = "stop";
            }
        }
        int prompt = llm.getPromptTokens(handle);
        int completion = llm.getCompletionTokens(handle);
        final Usage usage = new Usage(prompt, completion, prompt + completion);
        if (listener != null) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    listener.onUsage(usage);
                }
            });
        }
        return new ChatResponse(ChatMessage.assistant(stops.text()), null, finish, usage,
                modelName());
    }

    private static void emit(final StreamingListener listener, final String delta) {
        if (listener == null || delta.length() == 0) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                listener.onContentDelta(delta);
            }
        });
    }

    private static void deliver(final AsyncResource<ChatResponse> result,
            final ChatResponse response, final LlmException error,
            final StreamingListener listener) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (error != null) {
                    if (listener != null) {
                        listener.onError(error);
                    }
                    result.error(error);
                } else {
                    result.complete(response);
                }
            }
        });
    }

    /// Runs on the worker thread.
    private LocalLlmImpl ensureOpen() throws LlmException {
        if (impl == null) {
            impl = Display.getInstance().getLocalLlmBackend();
        }
        if (handle == 0) {
            String path = modelPath.startsWith("file://") ? modelPath.substring(7) : modelPath;
            handle = impl.open(path, contextSize, gpuLayers, threads);
            if (handle == 0) {
                String err = impl.getLastError(0);
                throw failure("Could not load " + modelPath
                        + (err == null ? "" : ": " + err));
            }
        }
        return impl;
    }

    private String modelName() {
        int slash = modelPath.lastIndexOf('/');
        return slash < 0 ? modelPath : modelPath.substring(slash + 1);
    }

    static String unsupportedFeature(ChatRequest req) {
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            return "Tools are not supported by local models";
        }
        List<ChatMessage> messages = req.getMessages();
        for (ChatMessage m : messages) {
            if (m.getRole() == Role.TOOL) {
                return "Tool results are not supported by local models";
            }
            List<MessagePart> parts = m.getParts();
            for (MessagePart part : parts) {
                if (!(part instanceof TextPart)) {
                    return "Only text parts are supported by local models";
                }
            }
        }
        return null;
    }

    private static String roleName(Role r) {
        if (r == Role.SYSTEM) {
            return "system";
        }
        if (r == Role.ASSISTANT) {
            return "assistant";
        }
        return "user";
    }

    private static LlmException invalid(String message) {
        return new LlmException(message, -1, null, null, null,
                LlmException.ErrorType.INVALID_REQUEST);
    }

    private static LlmException failure(String message) {
        return new LlmException(message, -1, null, null, null, LlmException.ErrorType.UNKNOWN);
    }

    /// Accumulates generated text, ends it at the first stop sequence, and
    /// holds back any tail that could still turn out to begin one, so a
    /// stream never shows text the final response does not contain.
    static final class StopScanner {
        private final List<String> stops;
        private final int holdBack;
        private final StringBuilder text = new StringBuilder(); //NOPMD AvoidStringBufferField - the scanner exists to accumulate one response
        private int emitted;
        private boolean stopped;

        StopScanner(List<String> stops) {
            this.stops = stops == null ? new ArrayList<String>() : stops;
            int longest = 0;
            for (int i = 0; i < this.stops.size(); i++) {
                longest = Math.max(longest, this.stops.get(i).length());
            }
            holdBack = longest == 0 ? 0 : longest - 1;
        }

        /// @return the text that may now be shown
        String append(String piece) {
            int from = Math.max(0, text.length() - holdBack);
            text.append(piece);
            int cut = -1;
            for (String s : stops) {
                if (s.length() == 0) {
                    continue;
                }
                int at = text.indexOf(s, from);
                if (at >= 0 && (cut < 0 || at < cut)) {
                    cut = at;
                }
            }
            if (cut >= 0) {
                text.setLength(cut);
                stopped = true;
                return flush();
            }
            int safe = text.length() - holdBack;
            if (safe <= emitted) {
                return "";
            }
            String out = text.substring(emitted, safe);
            emitted = safe;
            return out;
        }

        /// @return whatever has not been shown yet
        String flush() {
            String out = text.substring(Math.min(emitted, text.length()));
            emitted = text.length();
            return out;
        }

        boolean isStopped() {
            return stopped;
        }

        String text() {
            return text.toString();
        }
    }
}
