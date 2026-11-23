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
package com.codename1.impl.android.ai;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.codename1.impl.SystemLlmImpl;
import com.codename1.impl.android.AndroidNativeUtil;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.genai.common.DownloadCallback;
import com.google.mlkit.genai.common.FeatureStatus;
import com.google.mlkit.genai.common.GenAiException;
import com.google.mlkit.genai.common.StreamingCallback;
import com.google.mlkit.genai.imagedescription.ImageDescriber;
import com.google.mlkit.genai.imagedescription.ImageDescriberOptions;
import com.google.mlkit.genai.imagedescription.ImageDescription;
import com.google.mlkit.genai.imagedescription.ImageDescriptionRequest;
import com.google.mlkit.genai.prompt.Content;
import com.google.mlkit.genai.prompt.GenerateContentRequest;
import com.google.mlkit.genai.prompt.Generation;
import com.google.mlkit.genai.prompt.SystemInstruction;
import com.google.mlkit.genai.prompt.java.GenerativeModelFutures;
import com.google.mlkit.genai.proofreading.Proofreader;
import com.google.mlkit.genai.proofreading.ProofreaderOptions;
import com.google.mlkit.genai.proofreading.Proofreading;
import com.google.mlkit.genai.proofreading.ProofreadingRequest;
import com.google.mlkit.genai.rewriting.Rewriter;
import com.google.mlkit.genai.rewriting.RewriterOptions;
import com.google.mlkit.genai.rewriting.Rewriting;
import com.google.mlkit.genai.rewriting.RewritingRequest;
import com.google.mlkit.genai.summarization.Summarization;
import com.google.mlkit.genai.summarization.SummarizationRequest;
import com.google.mlkit.genai.summarization.Summarizer;
import com.google.mlkit.genai.summarization.SummarizerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Gemini Nano through ML Kit GenAI, behind {@code SystemLlmClient}. The model
 * runs inside AICore, the system service Google ships on Pixel, Samsung
 * Galaxy and other supported devices; nothing is bundled with the app.
 *
 * <p>Chat uses the prompt API. Summarize, proofread, rewrite and describe use
 * ML Kit's dedicated task APIs, each with its own model and its own feature
 * status, so a device can offer one and not another.</p>
 *
 * <p>Compiled only in the generated Gradle project, against the ML Kit GenAI
 * artifacts the catalog adds, and deleted by the builder from apps that do
 * not use {@code SystemLlmClient}. Every call arrives on the client's worker
 * thread, so the futures are waited on directly.</p>
 */
public final class AndroidSystemLlmImpl extends SystemLlmImpl {
    private static final String END = new String("\0end"); // NOPMD StringInstantiation - identity sentinel
    private static final Executor DIRECT = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };
    private static final int[] REWRITE_STYLES = {
        RewriterOptions.OutputType.ELABORATE,
        RewriterOptions.OutputType.EMOJIFY,
        RewriterOptions.OutputType.SHORTEN,
        RewriterOptions.OutputType.FRIENDLY,
        RewriterOptions.OutputType.PROFESSIONAL,
        RewriterOptions.OutputType.REPHRASE
    };
    private static final int[] SUMMARY_BULLETS = {
        SummarizerOptions.OutputType.ONE_BULLET,
        SummarizerOptions.OutputType.TWO_BULLETS,
        SummarizerOptions.OutputType.THREE_BULLETS
    };

    private final Map<Integer, Job> generations = new HashMap<Integer, Job>();
    private final String[] reasons = new String[FEATURE_COUNT];
    private int nextHandle = 1;
    private String lastError;
    private GenerativeModelFutures model;
    private Summarizer[] summarizers = new Summarizer[SUMMARY_BULLETS.length];
    private Proofreader proofreader;
    private Rewriter[] rewriters = new Rewriter[REWRITE_STYLES.length];
    private ImageDescriber describer;

    /** One generation (a job): ML Kit's callbacks fill the queue, next() drains it. */
    private static final class Job implements StreamingCallback {
        final LinkedBlockingQueue<String> pieces = new LinkedBlockingQueue<String>();
        // Written before END is queued and read after it is taken, so the
        // queue's hand-off makes it visible without further fencing.
        String error;
        ListenableFuture<?> future;

        @Override
        public void onNewText(String text) {
            if (text != null && text.length() > 0) {
                pieces.add(text);
            }
        }

        void finish() {
            try {
                future.get();
            } catch (ExecutionException ex) {
                error = describe(ex.getCause());
            } catch (InterruptedException ex) {
                error = "Interrupted";
            } catch (java.util.concurrent.CancellationException ex) {
                error = null;
            }
            pieces.add(END);
        }
    }

    @Override
    public String getProvider() {
        return "gemini-nano";
    }

    @Override
    public synchronized int getStatus(int feature) {
        if (feature < 0 || feature >= FEATURE_COUNT) {
            return STATUS_UNSUPPORTED;
        }
        try {
            ListenableFuture<Integer> f = statusFuture(feature, 0);
            // Bounded: AICore is a separate service, and a device where it
            // is missing or wedged must answer "unsupported", not hang the
            // client's worker forever.
            int s = f.get(10, java.util.concurrent.TimeUnit.SECONDS).intValue();
            reasons[feature] = reasonFor(s);
            return toStatus(s);
        } catch (Throwable t) {
            reasons[feature] = describe(t instanceof ExecutionException ? t.getCause() : t);
            return STATUS_UNSUPPORTED;
        }
    }

    @Override
    public synchronized String getStatusReason(int feature) {
        if (feature < 0 || feature >= FEATURE_COUNT) {
            return "Unknown feature";
        }
        if (reasons[feature] == null) {
            getStatus(feature);
        }
        return reasons[feature];
    }

    @Override
    public boolean download(int feature) {
        final String[] failure = new String[1];
        try {
            ListenableFuture<Void> f;
            DownloadCallback cb = new DownloadCallback() {
                @Override
                public void onDownloadFailed(GenAiException e) {
                    failure[0] = describe(e);
                }
            };
            synchronized (this) {
                f = downloadFuture(feature, cb);
            }
            f.get();
            return failure[0] == null || setError(failure[0]);
        } catch (ExecutionException ex) {
            return setError(describe(ex.getCause()));
        } catch (Throwable t) {
            return setError(describe(t));
        }
    }

    @Override
    public synchronized int start(int feature, String[] roles, String[] contents, byte[] image,
            int option, int maxTokens, float temperature, int seed) {
        try {
            final Job g = new Job();
            String text = contents.length == 0 ? "" : contents[contents.length - 1];
            switch (feature) {
                case FEATURE_CHAT:
                case FEATURE_IMAGE_INPUT:
                    g.future = model().generateContent(
                            chatRequest(roles, contents, image, maxTokens, temperature, seed), g);
                    break;
                case FEATURE_SUMMARIZE:
                    g.future = summarizer(option).runInference(
                            SummarizationRequest.builder(text).build(), g);
                    break;
                case FEATURE_PROOFREAD:
                    // The task API answers with suggestions, not a stream of
                    // the text; the first one is the corrected text.
                    g.future = proofreader().runInference(ProofreadingRequest.builder(text).build());
                    break;
                case FEATURE_REWRITE:
                    g.future = rewriter(option).runInference(RewritingRequest.builder(text).build());
                    break;
                case FEATURE_DESCRIBE_IMAGE:
                    Bitmap bmp = decode(image);
                    if (bmp == null) {
                        setError("The image could not be decoded");
                        return 0;
                    }
                    g.future = describer().runInference(ImageDescriptionRequest.builder(bmp).build(), g);
                    break;
                default:
                    setError("Unknown feature " + feature);
                    return 0;
            }
            final int feat = feature;
            g.future.addListener(new Runnable() {
                @Override
                public void run() {
                    if (feat == FEATURE_PROOFREAD || feat == FEATURE_REWRITE) {
                        g.onNewText(firstSuggestion(g.future));
                    }
                    g.finish();
                }
            }, DIRECT);
            int handle = nextHandle++;
            generations.put(Integer.valueOf(handle), g);
            return handle;
        } catch (Throwable t) {
            setError(describe(t));
            return 0;
        }
    }

    @Override
    public String next(int handle) {
        Job g;
        synchronized (this) {
            g = generations.get(Integer.valueOf(handle));
        }
        if (g == null) {
            return null;
        }
        try {
            String piece = g.pieces.take();
            if (piece == END) { // NOPMD CompareObjectsWithEquals - identity sentinel
                g.pieces.add(END);
                return null;
            }
            return piece;
        } catch (InterruptedException ex) {
            return null;
        }
    }

    @Override
    public synchronized String getLastError(int handle) {
        if (handle == 0) {
            return lastError;
        }
        Job g = generations.get(Integer.valueOf(handle));
        return g == null ? null : g.error;
    }

    @Override
    public void close(int handle) {
        Job g;
        synchronized (this) {
            g = generations.remove(Integer.valueOf(handle));
        }
        if (g != null && g.future != null && !g.future.isDone()) {
            g.future.cancel(true);
        }
    }

    private boolean setError(String message) {
        synchronized (this) {
            lastError = message;
        }
        return false;
    }

    private ListenableFuture<Integer> statusFuture(int feature, int option) {
        switch (feature) {
            case FEATURE_SUMMARIZE:
                return summarizer(option).checkFeatureStatus();
            case FEATURE_PROOFREAD:
                return proofreader().checkFeatureStatus();
            case FEATURE_REWRITE:
                return rewriter(option).checkFeatureStatus();
            case FEATURE_DESCRIBE_IMAGE:
                return describer().checkFeatureStatus();
            default:
                // Chat and images in chat share the prompt API's model.
                return model().checkStatus();
        }
    }

    private ListenableFuture<Void> downloadFuture(int feature, DownloadCallback cb) {
        switch (feature) {
            case FEATURE_SUMMARIZE:
                return summarizer(0).downloadFeature(cb);
            case FEATURE_PROOFREAD:
                return proofreader().downloadFeature(cb);
            case FEATURE_REWRITE:
                return rewriter(0).downloadFeature(cb);
            case FEATURE_DESCRIBE_IMAGE:
                return describer().downloadFeature(cb);
            default:
                return model().download(cb);
        }
    }

    private GenerateContentRequest chatRequest(String[] roles, String[] contents, byte[] image,
            int maxTokens, float temperature, int seed) {
        StringBuilder system = new StringBuilder();
        List<Content> turns = new ArrayList<Content>();
        Bitmap bmp = decode(image);
        int lastUser = -1;
        for (int i = 0; i < roles.length; i++) {
            if ("user".equals(roles[i])) {
                lastUser = i;
            }
        }
        for (int i = 0; i < roles.length; i++) {
            if ("system".equals(roles[i])) {
                if (system.length() > 0) {
                    system.append("\n\n");
                }
                system.append(contents[i]);
                continue;
            }
            Content.Builder c = new Content.Builder();
            // role(), not setRole(): Kotlin emits a void setRole and a fluent one,
            // and javac cannot choose between them.
            c.role("assistant".equals(roles[i]) ? Content.Role.MODEL : Content.Role.USER);
            // The image goes with the message that carried it, which the
            // core always delivers as the last user turn.
            if (i == lastUser && bmp != null) {
                c.image(bmp);
            }
            c.text(contents[i]);
            turns.add(c.build());
        }
        GenerateContentRequest.Builder b = new GenerateContentRequest.Builder(turns);
        if (system.length() > 0) {
            b.setSystemInstruction(new SystemInstruction(system.toString()));
        }
        if (maxTokens > 0) {
            b.setMaxOutputTokens(Integer.valueOf(maxTokens));
        }
        if (temperature >= 0) {
            b.setTemperature(Float.valueOf(temperature));
        }
        if (seed >= 0) {
            b.setSeed(Integer.valueOf(seed));
        }
        return b.build();
    }

    private static String firstSuggestion(ListenableFuture<?> future) {
        try {
            Object r = future.get();
            List<?> list = null;
            if (r instanceof com.google.mlkit.genai.proofreading.ProofreadingResult) {
                list = ((com.google.mlkit.genai.proofreading.ProofreadingResult) r).getResults();
                if (!list.isEmpty() && list.get(0)
                        instanceof com.google.mlkit.genai.proofreading.ProofreadingSuggestion) {
                    return ((com.google.mlkit.genai.proofreading.ProofreadingSuggestion)
                            list.get(0)).getText();
                }
            } else if (r instanceof com.google.mlkit.genai.rewriting.RewritingResult) {
                list = ((com.google.mlkit.genai.rewriting.RewritingResult) r).getResults();
                if (!list.isEmpty() && list.get(0)
                        instanceof com.google.mlkit.genai.rewriting.RewritingSuggestion) {
                    return ((com.google.mlkit.genai.rewriting.RewritingSuggestion)
                            list.get(0)).getText();
                }
            }
        } catch (Throwable t) {
            // The failure itself is reported by finish(), which reads the
            // same future.
            return null;
        }
        return null;
    }

    private static Bitmap decode(byte[] image) {
        if (image == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

    private static Context context() {
        return AndroidNativeUtil.getContext().getApplicationContext();
    }

    private GenerativeModelFutures model() {
        if (model == null) {
            model = GenerativeModelFutures.from(Generation.INSTANCE.getClient());
        }
        return model;
    }

    private Summarizer summarizer(int bullets) {
        int i = Math.max(0, Math.min(SUMMARY_BULLETS.length - 1, bullets - 1));
        if (summarizers[i] == null) {
            summarizers[i] = Summarization.getClient(SummarizerOptions.builder(context())
                    .setOutputType(SUMMARY_BULLETS[i])
                    .setLongInputAutoTruncationEnabled(true)
                    .build());
        }
        return summarizers[i];
    }

    private Proofreader proofreader() {
        if (proofreader == null) {
            proofreader = Proofreading.getClient(ProofreaderOptions.builder(context())
                    .setInputType(ProofreaderOptions.InputType.KEYBOARD)
                    .build());
        }
        return proofreader;
    }

    private Rewriter rewriter(int style) {
        int i = style < 0 || style >= REWRITE_STYLES.length ? REWRITE_STYLES.length - 1 : style;
        if (rewriters[i] == null) {
            rewriters[i] = Rewriting.getClient(RewriterOptions.builder(context())
                    .setOutputType(REWRITE_STYLES[i])
                    .build());
        }
        return rewriters[i];
    }

    private ImageDescriber describer() {
        if (describer == null) {
            describer = ImageDescription.getClient(ImageDescriberOptions.builder(context()).build());
        }
        return describer;
    }

    private static int toStatus(int mlkit) {
        switch (mlkit) {
            case FeatureStatus.AVAILABLE:
                return STATUS_AVAILABLE;
            case FeatureStatus.DOWNLOADABLE:
                return STATUS_DOWNLOADABLE;
            case FeatureStatus.DOWNLOADING:
                return STATUS_DOWNLOADING;
            default:
                return STATUS_UNSUPPORTED;
        }
    }

    private static String reasonFor(int mlkit) {
        switch (mlkit) {
            case FeatureStatus.AVAILABLE:
                return null;
            case FeatureStatus.DOWNLOADABLE:
                return "The model has to be downloaded first";
            case FeatureStatus.DOWNLOADING:
                return "The model is downloading";
            default:
                return "This device does not support Gemini Nano for this feature";
        }
    }

    static String describe(Throwable t) {
        if (t == null) {
            return "Unknown error";
        }
        if (t instanceof GenAiException) {
            GenAiException g = (GenAiException) t;
            return "Gemini Nano error " + g.getErrorCode()
                    + (g.getMessage() == null ? "" : ": " + g.getMessage());
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
