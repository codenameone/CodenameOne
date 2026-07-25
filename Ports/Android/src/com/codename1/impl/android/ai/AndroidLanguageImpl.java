/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.language.LanguageCandidate;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.ai.language.SmartReplyMessage;
import com.codename1.impl.LanguageImpl;
import com.codename1.util.AsyncResource;

/**
 * Android language-service dispatcher. The builder retains only the adapter
 * source and ML Kit artifact for each language API referenced by the app.
 */
public final class AndroidLanguageImpl extends LanguageImpl {
    private volatile boolean closed;

    @Override
    public boolean isSupported(String feature, String backendId) {
        return !closed
                && ("auto".equals(backendId) || "ml-kit".equals(backendId))
                && adapter(feature) != null;
    }

    @Override
    public AsyncResource<LanguageCandidate[]> identify(
            String text, String backendId, LanguageOptions options) {
        AndroidLanguageAdapter adapter = adapter("language-id");
        return adapter == null
                ? AndroidLanguageAdapter.<LanguageCandidate[]>unsupported(
                        "Language identification is not included in this build")
                : adapter.identify(text, options);
    }

    @Override
    public AsyncResource<String> translate(
            String text, String sourceLanguage, String targetLanguage,
            String backendId, LanguageOptions options) {
        AndroidLanguageAdapter adapter = adapter("translation");
        return adapter == null
                ? AndroidLanguageAdapter.<String>unsupported(
                        "Translation is not included in this build")
                : adapter.translate(text, sourceLanguage, targetLanguage,
                        options);
    }

    @Override
    public AsyncResource<String[]> suggestReplies(
            SmartReplyMessage[] conversation, String backendId,
            LanguageOptions options) {
        AndroidLanguageAdapter adapter = adapter("smart-reply");
        return adapter == null
                ? AndroidLanguageAdapter.<String[]>unsupported(
                        "Smart Reply is not included in this build")
                : adapter.suggestReplies(conversation, options);
    }

    private static AndroidLanguageAdapter adapter(String feature) {
        String className;
        if ("language-id".equals(feature)) {
            className =
                    "com.codename1.impl.android.ai.AndroidLanguageIdAdapter";
        } else if ("translation".equals(feature)) {
            className =
                    "com.codename1.impl.android.ai.AndroidTranslationAdapter";
        } else if ("smart-reply".equals(feature)) {
            className =
                    "com.codename1.impl.android.ai.AndroidSmartReplyAdapter";
        } else {
            return null;
        }
        try {
            return (AndroidLanguageAdapter)
                    Class.forName(className).newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void close() {
        closed = true;
    }
}
