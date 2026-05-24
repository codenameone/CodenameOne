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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Internal `ConnectionRequest` subclass that streams an SSE response
/// line-by-line, dispatching each `data:` payload through a
/// provider-specific parser. Provider-agnostic plumbing (line
/// reassembly, EDT dispatch, error mapping, cancellation, completion
/// signaling) lives here; the parser and the request body are
/// supplied by the concrete provider client.
abstract class StreamingChatRequest extends ConnectionRequest {
    private final StreamingListener listener;
    private final AsyncResource<ChatResponse> result;
    private final byte[] requestBody;

    /// Provider-supplied accumulator that aggregates `data:` lines
    /// into a final [ChatResponse] and fires per-delta listener
    /// callbacks along the way.
    private final SseDecoder decoder;

    StreamingChatRequest(String url, byte[] requestBody,
                         StreamingListener listener,
                         AsyncResource<ChatResponse> result,
                         SseDecoder decoder) {
        setUrl(url);
        setPost(true);
        setReadResponseForErrors(true);
        setContentType("application/json");
        // The framework's default duplicate-suppression collapses two
        // identical-URL chat calls into one; that's actively wrong
        // for chats, so opt out.
        setDuplicateSupported(true);
        this.requestBody = requestBody;
        this.listener = listener;
        this.result = result;
        this.decoder = decoder;
    }

    @Override
    protected void buildRequestBody(OutputStream os) throws IOException {
        if (requestBody != null) {
            os.write(requestBody);
        }
    }

    @Override
    protected void readResponse(InputStream input) throws IOException {
        int status = getResponseCode();
        if (status >= 400) {
            // The framework will normally pump the body into `data`
            // and we map to an LlmException via handleErrorResponseCode.
            // Still drain so the body is captured.
            // Util.readInputStream is documented to never return null;
            // skip the redundant null guard to satisfy the static
            // analyzer.
            byte[] body = com.codename1.io.Util.readInputStream(input);
            String text = new String(body, "UTF-8");
            failWith(decoder.mapError(status, text));
            return;
        }

        StringBuilder lineBuf = new StringBuilder(256);
        StringBuilder dataBuf = new StringBuilder(1024);
        int b;
        while (!isKilled() && (b = input.read()) != -1) {
            if (b == '\n') {
                String line = lineBuf.toString();
                lineBuf.setLength(0);
                if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
                    line = line.substring(0, line.length() - 1);
                }
                if (line.length() == 0) {
                    // Dispatch the accumulated event.
                    if (dataBuf.length() > 0) {
                        dispatchEvent(dataBuf.toString());
                        dataBuf.setLength(0);
                    }
                } else if (line.startsWith("data:")) {
                    String payload = line.substring(5);
                    if (payload.length() > 0 && payload.charAt(0) == ' ') {
                        payload = payload.substring(1);
                    }
                    if (dataBuf.length() > 0) {
                        dataBuf.append('\n');
                    }
                    dataBuf.append(payload);
                }
                // Ignore other line types (event:, id:, retry:, comments).
            } else {
                lineBuf.append((char) b);
            }
        }
        // Final dispatch for any trailing event without a blank-line
        // terminator (some providers omit the trailing CRLF).
        if (dataBuf.length() > 0) {
            dispatchEvent(dataBuf.toString());
        }
        if (isKilled()) {
            return;
        }
        // Stream ended without an explicit terminator from the
        // decoder; ask it to finalize whatever it has.
        ChatResponse finalResponse = decoder.finish();
        completeWith(finalResponse);
    }

    private void dispatchEvent(String payload) {
        if ("[DONE]".equals(payload)) {
            // OpenAI / Ollama sentinel -- the next call to finish()
            // will assemble whatever the decoder accumulated.
            return;
        }
        try {
            decoder.consume(payload, listener);
        } catch (Throwable t) {
            failWith(t);
        }
    }

    private void completeWith(final ChatResponse r) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (!result.isDone()) {
                    result.complete(r);
                }
            }
        });
    }

    private void failWith(final Throwable t) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                if (listener != null) {
                    try {
                        listener.onError(t);
                    } catch (Throwable ignore) {
                    }
                }
                if (!result.isDone()) {
                    result.error(t);
                }
            }
        });
    }

    /// Invoked by the framework when an exception escapes the network
    /// path. Convert to an `LlmNetworkException` and surface.
    @Override
    protected void handleException(Exception err) {
        failWith(new LlmNetworkException(err.getMessage(), err));
    }

    /// Provider-specific SSE event decoder. Implementations are
    /// stateful (accumulating text content and tool-call fragments)
    /// and call back into the listener as deltas arrive.
    interface SseDecoder {
        /// Process one `data:` payload. Fire listener deltas as needed.
        void consume(String dataPayload, StreamingListener listener) throws Exception;

        /// Build the final aggregated [ChatResponse]. Called when the
        /// stream closes cleanly (either after `[DONE]` or after EOF).
        ChatResponse finish();

        /// Convert an HTTP-error body into the right [LlmException]
        /// subtype.
        LlmException mapError(int httpStatus, String body);
    }
}
