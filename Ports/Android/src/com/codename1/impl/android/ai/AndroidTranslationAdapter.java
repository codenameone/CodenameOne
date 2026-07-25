/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.language.LanguageOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

/** ML Kit translation; retained only for {@code Translator} users. */
final class AndroidTranslationAdapter extends AndroidLanguageAdapter {
    @Override
    AsyncResource<String> translate(
            final String text, String sourceLanguage, String targetLanguage,
            LanguageOptions options) {
        final AsyncResource<String> out = new AsyncResource<String>();
        TranslatorOptions translatorOptions =
                new TranslatorOptions.Builder()
                        .setSourceLanguage(sourceLanguage)
                        .setTargetLanguage(targetLanguage).build();
        final Translator client =
                Translation.getClient(translatorOptions);
        client.downloadModelIfNeeded().continueWithTask(
                new Continuation<Void, Task<String>>() {
            public Task<String> then(Task<Void> ignored) {
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
}
