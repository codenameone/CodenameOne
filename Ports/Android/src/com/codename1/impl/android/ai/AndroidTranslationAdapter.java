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

import com.codename1.ai.language.LanguageOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.Locale;

/** ML Kit translation; retained only for {@code Translator} users. */
final class AndroidTranslationAdapter extends AndroidLanguageAdapter {
    @Override
    AsyncResource<String> translate(
            final String text, String sourceLanguage, String targetLanguage,
            LanguageOptions options) {
        final AsyncResource<String> out = new AsyncResource<String>();
        String sourceCode = languageCode(sourceLanguage);
        String targetCode = languageCode(targetLanguage);
        if (sourceCode == null || targetCode == null) {
            out.error(new IllegalArgumentException(
                    "Unsupported translation language"));
            return out;
        }
        TranslatorOptions translatorOptions =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceCode)
                        .setTargetLanguage(targetCode)
                        .build();
        final Translator client =
                Translation.getClient(translatorOptions);
        client.downloadModelIfNeeded().onSuccessTask(
                new SuccessContinuation<Void, String>() {
            public Task<String> then(Void ignored) {
                return client.translate(text);
            }
        }).addOnSuccessListener(new OnSuccessListener<String>() {
            public void onSuccess(String value) {
                complete(out, value);
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
        return out;
    }

    private static String languageCode(String languageTag) {
        if (languageTag == null) {
            return null;
        }
        String language = Locale.forLanguageTag(languageTag).getLanguage();
        if (language.length() == 0) {
            return null;
        }
        return TranslateLanguage.fromLanguageTag(language);
    }
}
