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
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.util.AsyncResource;
import com.codename1.util.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Cloud-first image generation. The `openai` and `replicate` factory
/// methods cover the two dominant managed endpoints. On-device
/// generation via Core ML / ONNX Stable Diffusion is provided
/// separately by the optional `cn1-ai-stablediffusion` cn1lib; see
/// [#onDevice()].
///
/// ```
/// ImageGenerator.openai(KeyStore.get("openai_key"))
///     .generate(new GenerateImageRequest("A cat in a sombrero").setSize("1024x1024"))
///     .ready(img -> imageComponent.setIcon(img));
/// ```
public abstract class ImageGenerator {

    public static ImageGenerator openai(String apiKey) {
        return new OpenAiImageGenerator(apiKey);
    }

    /// Replicate runs a wide catalog of third-party image models
    /// (SDXL, Flux, etc.) behind a uniform REST API. Pass the API
    /// token from `https://replicate.com/account`.
    public static ImageGenerator replicate(String apiKey) {
        return new ReplicateImageGenerator(apiKey);
    }

    /// On-device generator. Requires the optional cn1lib
    /// `cn1-ai-stablediffusion`; without it this returns an
    /// `AsyncResource` that completes with
    /// `UnsupportedOperationException`.
    public static ImageGenerator onDevice() {
        // Lazy lookup so app code can compile even without the
        // cn1lib. The cn1lib registers an implementation via
        // `NativeLookup.register(...)` when it ships, but for the
        // base framework we just return a no-op stub.
        return new ImageGenerator() {
            @Override
            public AsyncResource<Image> generate(GenerateImageRequest req) {
                AsyncResource<Image> out = new AsyncResource<Image>();
                out.error(new UnsupportedOperationException(
                        "On-device image generation requires the cn1-ai-stablediffusion cn1lib. "
                        + "Add it to your dependencies or use ImageGenerator.openai(...) instead."));
                return out;
            }
        };
    }

    public abstract AsyncResource<Image> generate(GenerateImageRequest req);

    // --------------------- OpenAI ---------------------

    private static final class OpenAiImageGenerator extends ImageGenerator {
        private final String apiKey;

        OpenAiImageGenerator(String apiKey) {
            this.apiKey = apiKey == null ? "" : apiKey;
        }

        @Override
        public AsyncResource<Image> generate(GenerateImageRequest req) {
            final AsyncResource<Image> result = new AsyncResource<Image>();
            final byte[] body;
            try {
                Map<String, Object> root = new HashMap<String, Object>();
                root.put("model", req.getModel() != null ? req.getModel() : "dall-e-3");
                root.put("prompt", req.getPrompt());
                root.put("n", Integer.valueOf(req.getCount()));
                root.put("size", req.getSize());
                if (req.getStyle() != null) {
                    root.put("style", req.getStyle());
                }
                if (req.getQuality() != null) {
                    root.put("quality", req.getQuality());
                }
                // b64_json keeps the request self-contained; the
                // alternative `url` requires a second fetch and
                // expires after an hour, which is hostile to caching.
                root.put("response_format", "b64_json");
                body = JSONParser.toJson(root).getBytes("UTF-8");
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
                        Map root = JSONParser.parseJSON(getResponseData());
                        List<Object> data = JSONParser.asList(root.get("data"));
                        if (data == null || data.isEmpty()) {
                            failOnEdt(result, new LlmException(
                                    "Empty data[] in image generation response", 200, null, null, null, LlmException.ErrorType.INVALID_REQUEST));
                            return;
                        }
                        Map first = JSONParser.asMap(data.get(0));
                        String b64 = JSONParser.getString(first, "b64_json");
                        if (b64 == null) {
                            failOnEdt(result, new LlmException(
                                    "Missing b64_json in image generation response", 200, null, null, null, LlmException.ErrorType.INVALID_REQUEST));
                            return;
                        }
                        byte[] bytes = Base64.decode(b64.getBytes("UTF-8"));
                        final Image img = Image.createImage(bytes, 0, bytes.length);
                        Display.getInstance().callSerially(new Runnable() {
                            @Override
                            public void run() {
                                result.complete(img);
                            }
                        });
                    } catch (IOException ex) {
                        failOnEdt(result, new LlmException("Failed to decode image", ex));
                    } catch (RuntimeException ex) {
                        failOnEdt(result, new LlmException("Failed to decode image", ex));
                    }
                }

                @Override
                protected void handleException(Exception err) {
                    int sc;
                    try {
                        sc = getResponseCode();
                    } catch (Throwable ignore) {
                        sc = -1;
                    }
                    String bodyText = "";
                    try {
                        byte[] d = getResponseData();
                        bodyText = d == null ? "" : new String(d, "UTF-8");
                    } catch (UnsupportedEncodingException ignored) {
                        // UTF-8 is universally available; defensive only.
                    }
                    failOnEdt(result, OpenAiSseDecoder.mapErrorStatic(sc, bodyText));
                }
            };
            cr.setUrl("https://api.openai.com/v1/images/generations");
            cr.setPost(true);
            cr.setReadResponseForErrors(true);
            cr.setDuplicateSupported(true);
            cr.setContentType("application/json");
            cr.setTimeout(120000);
            if (apiKey.length() > 0) {
                cr.addRequestHeader("Authorization", "Bearer " + apiKey);
            }
            NetworkManager.getInstance().addToQueue(cr);
            return result;
        }
    }

    // --------------------- Replicate ---------------------

    private static final class ReplicateImageGenerator extends ImageGenerator {
        @SuppressWarnings("PMD.UnusedFormalParameter")
        ReplicateImageGenerator(String apiKey) {
            // apiKey is currently unused -- this generator is a
            // scaffold pending long-poll support. The parameter is
            // accepted so the factory signature stays stable when
            // the real implementation lands.
        }

        @Override
        public AsyncResource<Image> generate(GenerateImageRequest req) {
            AsyncResource<Image> result = new AsyncResource<Image>();
            // Replicate's "predictions" API is async/polled; full
            // long-poll support is a follow-up. For now we surface a
            // clear error so callers know to use a self-hosted
            // Replicate-compatible endpoint or the OpenAI path.
            result.error(new UnsupportedOperationException(
                    "Replicate's prediction API requires long-polling that is not implemented yet. "
                    + "Use ImageGenerator.openai(...) or run a Replicate-compatible server behind LlmClient.localOpenAiCompatible(...)."));
            return result;
        }
    }

    private static void failOnEdt(final AsyncResource<Image> result, final Throwable t) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                result.error(t);
            }
        });
    }
}
