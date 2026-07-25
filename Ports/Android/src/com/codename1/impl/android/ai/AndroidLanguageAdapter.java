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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnFailureListener;

/** Shared contract and completion helpers for Android language adapters. */
abstract class AndroidLanguageAdapter {
    AsyncResource<LanguageCandidate[]> identify(
            String text, LanguageOptions options) {
        return unsupported("Language identification is not supported");
    }

    AsyncResource<String> translate(
            String text, String sourceLanguage, String targetLanguage,
            LanguageOptions options) {
        return unsupported("Translation is not supported");
    }

    AsyncResource<String[]> suggestReplies(
            SmartReplyMessage[] conversation, LanguageOptions options) {
        return unsupported("Smart Reply is not supported");
    }

    static <T> AsyncResource<T> unsupported(String message) {
        AsyncResource<T> out = new AsyncResource<T>();
        out.error(new UnsupportedOperationException(message));
        return out;
    }

    static <T> void complete(final AsyncResource<T> out, final T value) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.complete(value);
            }
        });
    }

    static OnFailureListener failure(final AsyncResource<?> out,
                                     final AutoCloseable client) {
        return new OnFailureListener() {
            public void onFailure(final Exception error) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        out.error(error);
                    }
                });
                try {
                    client.close();
                } catch (Exception ignored) {
                }
            }
        };
    }
}
