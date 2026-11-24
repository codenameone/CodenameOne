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

import com.codename1.impl.SystemLlmImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.EasyThread;

import java.util.List;

/// An [LlmClient] backed by the language model the operating system ships:
/// Apple's Foundation Models (Apple Intelligence) on iOS and macOS, and Gemini
/// Nano through ML Kit GenAI on Android. Create one with [LlmClient#system()].
///
/// Nothing leaves the device, nothing is bundled with the application, and the
/// OS keeps the model up to date. In exchange the model is small, and whether
/// it exists at all depends on the device, the OS version and the user's
/// settings, so an application asks first:
///
/// ```java
/// SystemLlmClient llm = LlmClient.system();
/// SystemLlmStatus s = SystemLlmClient.getStatus(SystemLlmFeature.CHAT);
/// if (s == SystemLlmStatus.DOWNLOADABLE) {
///     llm.download(SystemLlmFeature.CHAT);
/// } else if (s.isAvailable()) {
///     llm.chat(ChatRequest.builder()
///             .addMessage(ChatMessage.user("Suggest a title for this note: ..."))
///             .build()).ready(r -> Log.p(r.getText()));
/// }
/// ```
///
/// Each capability is answered separately ([SystemLlmFeature]): a device can
/// summarize without accepting images in chat. Besides [#chat(ChatRequest)],
/// the client has [#summarize(String, int)], [#proofread(String)],
/// [#rewrite(String, RewriteStyle)] and [#describeImage(byte[])]. On Android
/// these use ML Kit's dedicated task models; on Apple platforms they are
/// prompts to the same general model.
///
/// #### Availability
///
/// - **iOS and macOS 26 or newer**, on a device with Apple Intelligence turned
///   on. Images are not accepted. Older OS versions answer
///   [SystemLlmStatus#UNSUPPORTED], as do applications built with an Xcode that
///   predates Foundation Models.
/// - **Android 8 or newer**, on a device AICore supports with Gemini Nano --
///   recent Pixel, Samsung Galaxy and other flagship phones. Using this class
///   raises the application's minimum SDK to 23; older
///   Android versions report [SystemLlmStatus#UNSUPPORTED].
/// - Everywhere else, the simulator included, every feature is
///   [SystemLlmStatus#UNSUPPORTED] and requests fail with
///   [LlmException.ErrorType#INVALID_REQUEST]. Use [LlmClient#ollama()] to
///   develop against a local model on the desktop.
///
/// The builders add the platform bridge only to applications that call
/// [LlmClient#system()]; an application that checks the status here without
/// ever calling it has no bridge and is told [SystemLlmStatus#UNSUPPORTED].
/// A request's seed has no effect on Apple platforms, whose model takes none.
/// Tools, [ResponseFormat#JSON_OBJECT] and embeddings are not supported
/// and fail the request with [LlmException.ErrorType#INVALID_REQUEST].
///
/// Requests run one at a time on a background thread, in the order they were
/// made. Results and streaming callbacks arrive on the EDT.
public final class SystemLlmClient extends LlmClient {
    private EasyThread worker;

    SystemLlmClient() {
        super("");
    }

    /// Whether this device can chat with an OS model, now or after a download.
    /// Blocks briefly while the OS is asked; prefer
    /// [#checkStatus(SystemLlmFeature)] on the EDT.
    ///
    /// @return `true` when [#getStatus(SystemLlmFeature)] for
    /// [SystemLlmFeature#CHAT] is supported
    public static boolean isSupported() {
        return getStatus(SystemLlmFeature.CHAT).isSupported();
    }

    /// Asks the OS what it can do for a feature. Blocks briefly while the OS
    /// answers; prefer [#checkStatus(SystemLlmFeature)] on the EDT.
    ///
    /// @param feature the capability
    /// @return its status on this device
    public static SystemLlmStatus getStatus(SystemLlmFeature feature) {
        SystemLlmImpl impl = Display.getInstance().getSystemLlmBackend();
        if (impl == null) {
            return SystemLlmStatus.UNSUPPORTED;
        }
        return toStatus(impl.getStatus(feature.ordinal()));
    }

    /// Explains the status of a feature: why it is unsupported or disabled.
    ///
    /// @param feature the capability
    /// @return a human readable reason, or `null` when there is nothing to add
    public static String getStatusReason(SystemLlmFeature feature) {
        SystemLlmImpl impl = Display.getInstance().getSystemLlmBackend();
        if (impl == null) {
            return "No operating system language model is available on this platform";
        }
        return impl.getStatusReason(feature.ordinal());
    }

    /// Asks the OS what it can do for a feature without blocking the caller.
    ///
    /// @param feature the capability
    /// @return the status, delivered on the EDT
    public static AsyncResource<SystemLlmStatus> checkStatus(final SystemLlmFeature feature) {
        final AsyncResource<SystemLlmStatus> result = new AsyncResource<SystemLlmStatus>();
        EasyThread t = EasyThread.start("system-llm-status");
        t.run(new Runnable() {
            @Override
            public void run() {
                final SystemLlmStatus s = getStatus(feature);
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        result.complete(s);
                    }
                });
            }
        });
        // killWhenIdle, not kill: kill() drops a task the new thread has not
        // picked up yet, which here is always, and the status never arrived.
        t.killWhenIdle();
        return result;
    }

    /// Every feature this device supports, now or after a download.
    /// Blocks briefly while the OS is asked.
    ///
    /// @return the supported features, in declaration order
    public static SystemLlmFeature[] getSupportedFeatures() {
        SystemLlmFeature[] all = SystemLlmFeature.values();
        int count = 0;
        boolean[] keep = new boolean[all.length];
        for (int i = 0; i < all.length; i++) {
            keep[i] = getStatus(all[i]).isSupported();
            if (keep[i]) {
                count++;
            }
        }
        SystemLlmFeature[] out = new SystemLlmFeature[count];
        int n = 0;
        for (int i = 0; i < all.length; i++) {
            if (keep[i]) {
                out[n++] = all[i];
            }
        }
        return out;
    }

    /// The model behind this client, such as `apple-foundation-models` or
    /// `gemini-nano`, or `none` where there is none.
    @Override
    public String getProvider() {
        SystemLlmImpl impl = Display.getInstance().getSystemLlmBackend();
        return impl == null ? "none" : impl.getProvider();
    }

    /// Asks the OS to download the model behind a feature, for a status of
    /// [SystemLlmStatus#DOWNLOADABLE]. On Android it completes when the
    /// model is ready. Apple platforms download on their own: there it
    /// completes at once, with `true` while the OS is still fetching the model
    /// too, so check [#getStatus(SystemLlmFeature)] before relying on it.
    ///
    /// @param feature the capability
    /// @return completes with `true`, or fails with an [LlmException]
    public AsyncResource<Boolean> download(final SystemLlmFeature feature) {
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        submit(new Runnable() {
            @Override
            public void run() {
                SystemLlmImpl impl = Display.getInstance().getSystemLlmBackend();
                if (impl.download(feature.ordinal())) {
                    complete(result, Boolean.TRUE);
                } else {
                    String err = impl.getLastError(0);
                    fail(result, failure(err == null ? "The model download failed" : err));
                }
            }
        }, result, null);
        return result;
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
            fail(result, listener, new LlmException("Blocked by safety filter: " + reject,
                    400, "safety_filter", null, null, LlmException.ErrorType.INVALID_REQUEST));
            return result;
        }
        String unsupported = unsupportedFeature(req);
        if (unsupported != null) {
            fail(result, listener, invalid(unsupported));
            return result;
        }
        // Read on the caller's thread: a generic get() is a cast, and a cast
        // under a RuntimeException handler is one ParparVM never fails
        // (scripts/check-cast-semantics.sh).
        List<ChatMessage> messages = req.getMessages();
        final String[] roles = new String[messages.size()];
        final String[] contents = new String[messages.size()];
        byte[] image = null;
        int lastUser = -1;
        for (int i = 0; i < roles.length; i++) {
            roles[i] = roleName(messages.get(i).getRole());
            if ("user".equals(roles[i])) {
                lastUser = i;
            }
        }
        for (int i = 0; i < roles.length; i++) {
            ChatMessage m = messages.get(i);
            // getText() is never null (an image-only message answers ""),
            // which matters: the ports hand it to APIs that refuse null.
            contents[i] = m.getText();
            for (MessagePart part : m.getParts()) {
                if (part instanceof ImagePart) {
                    // The OS models take the image with the newest user turn
                    // only; anywhere else it would be moved silently.
                    if (i != lastUser) {
                        fail(result, listener, invalid(
                                "The system model accepts an image only in the last user message"));
                        return result;
                    }
                    image = ((ImagePart) part).getData();
                }
            }
        }
        final byte[] img = image;
        final int feature = img == null ? SystemLlmImpl.FEATURE_CHAT
                : SystemLlmImpl.FEATURE_IMAGE_INPUT;
        final int maxTokens = req.getMaxTokens() == null ? 0 : req.getMaxTokens().intValue();
        final float temperature = req.getTemperature() == null ? -1f
                : req.getTemperature().floatValue();
        final int seed = req.getSeed() == null ? -1 : (int) req.getSeed().longValue();
        final List<String> stopSequences = req.getStopSequences();
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalLlmClient.StopScanner stops = new LocalLlmClient.StopScanner(stopSequences);
                    generate(feature, roles, contents, img, 0, maxTokens,
                            temperature, seed, stops, listener);
                    final ChatResponse r = new ChatResponse(ChatMessage.assistant(stops.text()),
                            null, "stop", null, getProvider());
                    complete(result, r);
                } catch (LlmException ex) {
                    fail(result, listener, ex);
                } catch (RuntimeException ex) {
                    fail(result, listener, failure(String.valueOf(ex)));
                }
            }
        }, result, listener);
        return result;
    }

    /// Embeddings are not offered by OS models; always fails with
    /// [LlmException.ErrorType#INVALID_REQUEST].
    @Override
    public AsyncResource<EmbeddingResponse> embed(EmbeddingRequest req) {
        AsyncResource<EmbeddingResponse> result = new AsyncResource<EmbeddingResponse>();
        result.error(invalid("Embeddings are not supported by operating system models"));
        return result;
    }

    /// Summarizes a text.
    ///
    /// @param text the text, an article or a conversation
    /// @param bullets how many bullet points, 1 to 3
    /// @return the summary
    public AsyncResource<String> summarize(String text, int bullets) {
        int b = bullets < 1 ? 1 : (bullets > 3 ? 3 : bullets);
        return runTask(SystemLlmImpl.FEATURE_SUMMARIZE, text, null, b);
    }

    /// Corrects grammar and spelling, keeping the meaning and the voice.
    ///
    /// @param text the text
    /// @return the corrected text
    public AsyncResource<String> proofread(String text) {
        return runTask(SystemLlmImpl.FEATURE_PROOFREAD, text, null, 0);
    }

    /// Rewrites a text in another style.
    ///
    /// @param text the text
    /// @param style the style to rewrite it in
    /// @return the rewritten text
    public AsyncResource<String> rewrite(String text, RewriteStyle style) {
        return runTask(SystemLlmImpl.FEATURE_REWRITE, text, null, style.ordinal());
    }

    /// Describes an image in a sentence or two, for example as alternative text.
    ///
    /// @param image an encoded PNG or JPEG
    /// @return the description
    public AsyncResource<String> describeImage(byte[] image) {
        return runTask(SystemLlmImpl.FEATURE_DESCRIBE_IMAGE, "", image, 0);
    }

    /// Stops the worker thread. Requests already queued still run first.
    public void close() {
        if (worker != null) {
            // kill() would drop queued requests and leave their results
            // unsettled forever; the documented contract is that they run.
            worker.killWhenIdle();
            worker = null;
        }
    }

    private AsyncResource<String> runTask(final int feature, final String text,
            final byte[] image, final int option) {
        final AsyncResource<String> result = new AsyncResource<String>();
        if (text == null) {
            result.error(invalid("text is required"));
            return result;
        }
        submit(new Runnable() {
            @Override
            public void run() {
                try {
                    LocalLlmClient.StopScanner out = new LocalLlmClient.StopScanner(null);
                    generate(feature, new String[] {"user"}, new String[] {text}, image, option,
                            0, -1f, -1, out, null);
                    complete(result, out.text().trim());
                } catch (LlmException ex) {
                    fail(result, ex);
                } catch (RuntimeException ex) {
                    fail(result, failure(String.valueOf(ex)));
                }
            }
        }, result, null);
        return result;
    }

    /// Runs on the worker thread.
    ///
    /// @return `true` when a stop sequence ended the text
    private boolean generate(int feature, String[] roles, String[] contents, byte[] image,
            int option, int maxTokens, float temperature, int seed,
            LocalLlmClient.StopScanner stops, StreamingListener listener) throws LlmException {
        SystemLlmImpl impl = Display.getInstance().getSystemLlmBackend();
        SystemLlmStatus status = toStatus(impl.getStatus(feature));
        if (!status.isAvailable()) {
            String reason = impl.getStatusReason(feature);
            throw invalid("The operating system model cannot run this request ("
                    + status + ")" + (reason == null ? "" : ": " + reason));
        }
        int handle = impl.start(feature, roles, contents, image, option, maxTokens,
                temperature, seed);
        if (handle == 0) {
            String err = impl.getLastError(0);
            throw failure(err == null ? "The request could not be started" : err);
        }
        try {
            String piece = impl.next(handle);
            while (piece != null) {
                emit(listener, stops.append(piece));
                if (stops.isStopped()) {
                    return true;
                }
                piece = impl.next(handle);
            }
            emit(listener, stops.flush());
            String err = impl.getLastError(handle);
            if (err != null) {
                throw failure(err);
            }
            return false;
        } finally {
            impl.close(handle);
        }
    }

    private void submit(Runnable task, AsyncResource<?> result, StreamingListener listener) {
        if (Display.getInstance().getSystemLlmBackend() == null) {
            LlmException ex = invalid("No operating system language model is available on "
                    + "this platform; it needs iOS or macOS 26 with Apple Intelligence, or an "
                    + "Android device with Gemini Nano. Use LlmClient.ollama() on the desktop.");
            if (listener != null) {
                listener.onError(ex);
            }
            result.error(ex);
            return;
        }
        if (worker == null) {
            worker = EasyThread.start("system-llm");
        }
        worker.run(task);
    }

    static String unsupportedFeature(ChatRequest req) {
        if (req.getTools() != null && !req.getTools().isEmpty()) {
            return "Tools are not supported by operating system models";
        }
        if (req.getResponseFormat() == ResponseFormat.JSON_OBJECT) {
            return "JSON output is not supported by operating system models";
        }
        List<ChatMessage> messages = req.getMessages();
        int images = 0;
        for (ChatMessage m : messages) {
            if (m.getRole() == Role.TOOL) {
                return "Tool results are not supported by operating system models";
            }
            List<MessagePart> parts = m.getParts();
            for (MessagePart part : parts) {
                if (part instanceof ImagePart) {
                    ImagePart ip = (ImagePart) part;
                    if (ip.isUrl()) {
                        return "Image URLs are not supported by operating system models; "
                                + "pass the image bytes";
                    }
                    images++;
                } else if (!(part instanceof TextPart)) {
                    return "Only text and image parts are supported by operating system models";
                }
            }
        }
        if (images > 1) {
            return "Operating system models accept one image per request";
        }
        return null;
    }

    static SystemLlmStatus toStatus(int s) {
        switch (s) {
            case SystemLlmImpl.STATUS_DISABLED:
                return SystemLlmStatus.DISABLED;
            case SystemLlmImpl.STATUS_DOWNLOADABLE:
                return SystemLlmStatus.DOWNLOADABLE;
            case SystemLlmImpl.STATUS_DOWNLOADING:
                return SystemLlmStatus.DOWNLOADING;
            case SystemLlmImpl.STATUS_AVAILABLE:
                return SystemLlmStatus.AVAILABLE;
            default:
                return SystemLlmStatus.UNSUPPORTED;
        }
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

    private static <T> void complete(final AsyncResource<T> result, final T value) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                result.complete(value);
            }
        });
    }

    private static void fail(final AsyncResource<?> result, final LlmException ex) {
        fail(result, null, ex);
    }

    private static void fail(final AsyncResource<?> result, final StreamingListener listener,
            final LlmException ex) {
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.onError(ex);
                }
                result.error(ex);
            }
        });
    }

    private static LlmException invalid(String message) {
        return new LlmException(message, -1, null, null, null,
                LlmException.ErrorType.INVALID_REQUEST);
    }

    private static LlmException failure(String message) {
        return new LlmException(message, -1, null, null, null, LlmException.ErrorType.UNKNOWN);
    }
}
