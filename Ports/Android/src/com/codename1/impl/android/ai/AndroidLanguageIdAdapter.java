/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.impl.android.ai;

import com.codename1.ai.language.LanguageCandidate;
import com.codename1.ai.language.LanguageOptions;
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.List;

/** ML Kit language ID; retained only for {@code LanguageIdentifier} users. */
final class AndroidLanguageIdAdapter extends AndroidLanguageAdapter {
    @Override
    AsyncResource<LanguageCandidate[]> identify(
            String text, LanguageOptions options) {
        final AsyncResource<LanguageCandidate[]> out =
                new AsyncResource<LanguageCandidate[]>();
        final LanguageIdentifier client = LanguageIdentification.getClient(
                new LanguageIdentificationOptions.Builder()
                        .setConfidenceThreshold(options.getMinimumConfidence())
                        .build());
        client.identifyPossibleLanguages(text).addOnSuccessListener(
                new OnSuccessListener<List<IdentifiedLanguage>>() {
            public void onSuccess(List<IdentifiedLanguage> values) {
                LanguageCandidate[] result =
                        new LanguageCandidate[values.size()];
                for (int i = 0; i < result.length; i++) {
                    IdentifiedLanguage value = values.get(i);
                    result[i] = new LanguageCandidate(
                            value.getLanguageTag(), value.getConfidence());
                }
                complete(out, result);
                client.close();
            }
        }).addOnFailureListener(failure(out, client));
        return out;
    }
}
