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
    private AndroidLanguageAdapter languageIdAdapter;
    private AndroidLanguageAdapter translationAdapter;
    private AndroidLanguageAdapter smartReplyAdapter;

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

    private synchronized AndroidLanguageAdapter adapter(String feature) {
        if (closed) {
            return null;
        }
        AndroidLanguageAdapter cached;
        String className;
        if ("language-id".equals(feature)) {
            cached = languageIdAdapter;
            className =
                    "com.codename1.impl.android.ai.AndroidLanguageIdAdapter";
        } else if ("translation".equals(feature)) {
            cached = translationAdapter;
            className =
                    "com.codename1.impl.android.ai.AndroidTranslationAdapter";
        } else if ("smart-reply".equals(feature)) {
            cached = smartReplyAdapter;
            className =
                    "com.codename1.impl.android.ai.AndroidSmartReplyAdapter";
        } else {
            return null;
        }
        if (cached != null) {
            return cached;
        }
        try {
            cached = (AndroidLanguageAdapter)
                    Class.forName(className).newInstance();
            if ("language-id".equals(feature)) {
                languageIdAdapter = cached;
            } else if ("translation".equals(feature)) {
                translationAdapter = cached;
            } else {
                smartReplyAdapter = cached;
            }
            return cached;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        closeAdapter(languageIdAdapter);
        closeAdapter(translationAdapter);
        closeAdapter(smartReplyAdapter);
        languageIdAdapter = null;
        translationAdapter = null;
        smartReplyAdapter = null;
    }

    private static void closeAdapter(AndroidLanguageAdapter adapter) {
        if (adapter != null) {
            adapter.close();
        }
    }
}
