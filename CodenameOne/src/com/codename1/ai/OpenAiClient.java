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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// OpenAI-compatible chat / embeddings client. Drives Ollama,
/// llama.cpp, vLLM, Together, and any other endpoint that speaks the
/// `/v1/chat/completions` + `/v1/embeddings` shape.
class OpenAiClient extends LlmClient {
    private final String apiKey;
    private String defaultModel = "gpt-4o-mini";

    OpenAiClient(String apiKey, String baseUrl) {
        super(baseUrl);
        this.apiKey = apiKey == null ? "" : apiKey;
    }

    void setDefaultModel(String m) {
        this.defaultModel = m;
    }

    public String getProvider() {
        return "openai";
    }

    public AsyncResource<ChatResponse> chat(ChatRequest req) {
        final AsyncResource<ChatResponse> result = new AsyncResource<ChatResponse>();
        String reject = runSafetyFilter(req);
        if (reject != null) {
            result.error(new LlmInvalidRequestException("Blocked by safety filter: " + reject,
                    400, "safety_filter", null));
            return result;
        }
        final byte[] body;
        try {
            body = buildChatBody(req, false);
        } catch (IOException ioe) {
            result.error(ioe);
            return result;
        }
        ConnectionRequest cr = new ConnectionRequest() {
            @Override
            protected void buildRequestBody(OutputStream os) throws IOException {
                os.write(body);
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                // Suppress framework's default dialog — we'll deliver
                // an exception through the AsyncResource instead.
            }

            @Override
            protected void postResponse() {
                final byte[] data = getResponseData();
                try {
                    Map root = JsonHelper.parseObject(data);
                    final ChatResponse cr2 = OpenAiSseDecoder.parseNonStreaming(root);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.complete(cr2);
                        }
                    });
                } catch (Exception ex) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.error(new LlmException("Failed to parse response", ex));
                        }
                    });
                }
            }

            @Override
            protected void handleException(Exception err) {
                final int sc;
                try {
                    sc = getResponseCode();
                } catch (Throwable ignore) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.error(new LlmNetworkException(err.getMessage(), err));
                        }
                    });
                    return;
                }
                final String bodyText;
                try {
                    byte[] d = getResponseData();
                    bodyText = d == null ? "" : new String(d, "UTF-8");
                } catch (Exception ex) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.error(new LlmNetworkException(err.getMessage(), err));
                        }
                    });
                    return;
                }
                final LlmException mapped = OpenAiSseDecoder.mapErrorStatic(sc, bodyText);
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        result.error(mapped);
                    }
                });
            }
        };
        configureRequest(cr, "/chat/completions");
        NetworkManager.getInstance().addToQueue(cr);
        return result;
    }

    public AsyncResource<ChatResponse> chatStream(ChatRequest req, StreamingListener listener) {
        final AsyncResource<ChatResponse> result = new AsyncResource<ChatResponse>();
        String reject = runSafetyFilter(req);
        if (reject != null) {
            result.error(new LlmInvalidRequestException("Blocked by safety filter: " + reject,
                    400, "safety_filter", null));
            return result;
        }
        final byte[] body;
        try {
            body = buildChatBody(req, true);
        } catch (IOException ioe) {
            result.error(ioe);
            return result;
        }
        final StreamingChatRequest scr = new StreamingChatRequest(
                resolveUrl("/chat/completions"), body,
                listener == null ? new StreamingListener.Adapter() : listener,
                result,
                new OpenAiSseDecoder(req.getModel() != null ? req.getModel() : defaultModel)) {
            // Inherit body / SSE plumbing.
        };
        configureRequest(scr, null);
        scr.addRequestHeader("Accept", "text/event-stream");
        NetworkManager.getInstance().addToQueue(scr);
        // Bridge cancellation: when the caller cancels the AsyncResource
        // we kill the underlying socket so no further deltas arrive.
        // AsyncResource is Observable and fires `setChanged()` from
        // cancel(), complete(), and error(); we only act on cancellation.
        result.addObserver(new java.util.Observer() {
            public void update(java.util.Observable o, Object arg) {
                if (result.isCancelled()) {
                    scr.kill();
                }
            }
        });
        return result;
    }

    public AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req) {
        final AsyncResource<EmbeddingResponse> result = new AsyncResource<EmbeddingResponse>();
        final byte[] body;
        try {
            Map<String, Object> root = new HashMap<String, Object>();
            root.put("model", req.getModel() != null ? req.getModel() : "text-embedding-3-small");
            if (req.getInputs().size() == 1) {
                root.put("input", req.getInputs().get(0));
            } else {
                root.put("input", req.getInputs());
            }
            if (req.getDimensions() != null) {
                root.put("dimensions", req.getDimensions());
            }
            body = JsonHelper.serialize(root).getBytes("UTF-8");
        } catch (IOException ioe) {
            result.error(ioe);
            return result;
        }
        ConnectionRequest cr = new ConnectionRequest() {
            @Override
            protected void buildRequestBody(OutputStream os) throws IOException {
                os.write(body);
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
            }

            @Override
            protected void postResponse() {
                try {
                    Map root = JsonHelper.parseObject(getResponseData());
                    List<Object> dataArr = JsonHelper.asList(root.get("data"));
                    List<Embedding> out = new ArrayList<Embedding>(dataArr == null ? 0 : dataArr.size());
                    if (dataArr != null) {
                        for (int i = 0; i < dataArr.size(); i++) {
                            Map e = JsonHelper.asMap(dataArr.get(i));
                            List<Object> v = JsonHelper.asList(e.get("embedding"));
                            float[] vec = new float[v == null ? 0 : v.size()];
                            for (int j = 0; j < vec.length; j++) {
                                Object n = v.get(j);
                                vec[j] = n instanceof Number ? ((Number) n).floatValue()
                                        : Float.parseFloat(n.toString());
                            }
                            out.add(new Embedding(vec, JsonHelper.intValue(e, "index", i)));
                        }
                    }
                    Map usageMap = JsonHelper.asMap(root.get("usage"));
                    Usage u = usageMap == null ? null
                            : new Usage(
                                JsonHelper.intValue(usageMap, "prompt_tokens", -1),
                                -1,
                                JsonHelper.intValue(usageMap, "total_tokens", -1));
                    final EmbeddingResponse er = new EmbeddingResponse(out, u,
                            JsonHelper.string(root, "model"));
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.complete(er);
                        }
                    });
                } catch (final Exception ex) {
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            result.error(new LlmException("Failed to parse embedding response", ex));
                        }
                    });
                }
            }

            @Override
            protected void handleException(Exception err) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        result.error(new LlmNetworkException(err.getMessage(), err));
                    }
                });
            }
        };
        configureRequest(cr, "/embeddings");
        NetworkManager.getInstance().addToQueue(cr);
        return result;
    }

    private void configureRequest(ConnectionRequest cr, String pathOrNull) {
        if (pathOrNull != null) {
            cr.setUrl(resolveUrl(pathOrNull));
        }
        cr.setPost(true);
        cr.setReadResponseForErrors(true);
        cr.setDuplicateSupported(true);
        cr.setContentType("application/json");
        cr.setTimeout(getHttpTimeoutMs());
        if (apiKey.length() > 0) {
            cr.addRequestHeader("Authorization", "Bearer " + apiKey);
        }
    }

    private String resolveUrl(String path) {
        String base = getBaseUrl();
        if (base == null) {
            base = "";
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }

    @SuppressWarnings("unchecked")
    private byte[] buildChatBody(ChatRequest req, boolean stream) throws IOException {
        Map<String, Object> root = new HashMap<String, Object>();
        root.put("model", req.getModel() != null ? req.getModel() : defaultModel);
        root.put("messages", encodeMessages(req));
        root.put("stream", stream ? Boolean.TRUE : Boolean.FALSE);
        if (stream) {
            // Ask for usage in the final SSE chunk; modern OpenAI
            // endpoints only emit it when this option is set.
            Map<String, Object> so = new HashMap<String, Object>();
            so.put("include_usage", Boolean.TRUE);
            root.put("stream_options", so);
        }
        if (req.getTemperature() != null) root.put("temperature", req.getTemperature());
        if (req.getMaxTokens() != null) root.put("max_tokens", req.getMaxTokens());
        if (req.getTopP() != null) root.put("top_p", req.getTopP());
        if (req.getSeed() != null) root.put("seed", req.getSeed());
        if (!req.getStopSequences().isEmpty()) root.put("stop", req.getStopSequences());
        if (req.getResponseFormat() == ResponseFormat.JSON_OBJECT) {
            Map<String, Object> rf = new HashMap<String, Object>();
            rf.put("type", "json_object");
            root.put("response_format", rf);
        }
        if (!req.getTools().isEmpty()) {
            root.put("tools", encodeTools(req.getTools()));
            if (req.getToolChoice() != null) {
                root.put("tool_choice", encodeToolChoice(req.getToolChoice()));
            }
        }
        if (!req.getMetadata().isEmpty()) {
            root.put("metadata", req.getMetadata());
        }
        return JsonHelper.serialize(root).getBytes("UTF-8");
    }

    private List<Object> encodeMessages(ChatRequest req) {
        List<Object> out = new ArrayList<Object>(req.getMessages().size());
        for (int i = 0; i < req.getMessages().size(); i++) {
            ChatMessage m = req.getMessages().get(i);
            Map<String, Object> jm = new HashMap<String, Object>();
            jm.put("role", roleString(m.getRole()));
            // OpenAI accepts content as either a string (text-only)
            // or a content-array (multi-modal). Prefer the string
            // form when there's only one TextPart.
            if (m.getParts().size() == 1 && m.getParts().get(0) instanceof TextPart) {
                jm.put("content", ((TextPart) m.getParts().get(0)).getText());
            } else if (m.getRole() == Role.TOOL && m.getToolCallId() != null) {
                jm.put("tool_call_id", m.getToolCallId());
                StringBuilder buf = new StringBuilder();
                for (int p = 0; p < m.getParts().size(); p++) {
                    MessagePart part = m.getParts().get(p);
                    if (part instanceof TextPart) {
                        buf.append(((TextPart) part).getText());
                    } else if (part instanceof ToolResultPart) {
                        buf.append(((ToolResultPart) part).getResultJson());
                    }
                }
                jm.put("content", buf.toString());
            } else {
                List<Object> parts = new ArrayList<Object>();
                for (int p = 0; p < m.getParts().size(); p++) {
                    MessagePart part = m.getParts().get(p);
                    if (part instanceof TextPart) {
                        Map<String, Object> jp = new HashMap<String, Object>();
                        jp.put("type", "text");
                        jp.put("text", ((TextPart) part).getText());
                        parts.add(jp);
                    } else if (part instanceof ImagePart) {
                        ImagePart ip = (ImagePart) part;
                        Map<String, Object> jp = new HashMap<String, Object>();
                        jp.put("type", "image_url");
                        Map<String, Object> iu = new HashMap<String, Object>();
                        if (ip.isUrl()) {
                            iu.put("url", ip.getUrl());
                        } else {
                            iu.put("url", "data:" + ip.getMimeType() + ";base64,"
                                    + com.codename1.util.Base64.encodeNoNewline(ip.getData()));
                        }
                        jp.put("image_url", iu);
                        parts.add(jp);
                    }
                }
                jm.put("content", parts);
            }
            if (m.getName() != null) {
                jm.put("name", m.getName());
            }
            if (!m.getToolCalls().isEmpty()) {
                List<Object> tcs = new ArrayList<Object>();
                for (ToolCall tc : m.getToolCalls()) {
                    Map<String, Object> jtc = new HashMap<String, Object>();
                    jtc.put("id", tc.getId());
                    jtc.put("type", "function");
                    Map<String, Object> fn = new HashMap<String, Object>();
                    fn.put("name", tc.getName());
                    fn.put("arguments", tc.getArgumentsJson());
                    jtc.put("function", fn);
                    tcs.add(jtc);
                }
                jm.put("tool_calls", tcs);
            }
            out.add(jm);
        }
        return out;
    }

    private static String roleString(Role r) {
        switch (r) {
            case SYSTEM: return "system";
            case USER: return "user";
            case ASSISTANT: return "assistant";
            case TOOL: return "tool";
            default: return "user";
        }
    }

    private List<Object> encodeTools(List<Tool> tools) {
        List<Object> out = new ArrayList<Object>(tools.size());
        for (Tool t : tools) {
            Map<String, Object> jt = new HashMap<String, Object>();
            jt.put("type", "function");
            Map<String, Object> fn = new HashMap<String, Object>();
            fn.put("name", t.getName());
            fn.put("description", t.getDescription());
            fn.put("parameters", new JsonHelper.RawJson(t.getParametersJsonSchema()));
            jt.put("function", fn);
            out.add(jt);
        }
        return out;
    }

    private Object encodeToolChoice(ToolChoice c) {
        if ("named".equals(c.getMode())) {
            Map<String, Object> tc = new HashMap<String, Object>();
            tc.put("type", "function");
            Map<String, Object> fn = new HashMap<String, Object>();
            fn.put("name", c.getForcedToolName());
            tc.put("function", fn);
            return tc;
        }
        return c.getMode();
    }
}
