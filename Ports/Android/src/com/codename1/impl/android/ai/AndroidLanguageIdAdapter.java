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
import com.codename1.util.AsyncResource;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.nl.languageid.IdentifiedLanguage;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions;
import com.google.mlkit.nl.languageid.LanguageIdentifier;

import java.util.List;

/** ML Kit language ID; retained only for {@code LanguageIdentifier} users. */
final class AndroidLanguageIdAdapter extends AndroidLanguageAdapter {
    private LanguageIdentifier client;

    @Override
    AsyncResource<LanguageCandidate[]> identify(
            String text, LanguageOptions options) {
        final AsyncResource<LanguageCandidate[]> out =
                new AsyncResource<LanguageCandidate[]>();
        final LanguageIdentifier current = client(options);
        current.identifyPossibleLanguages(text).addOnSuccessListener(
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
            }
        }).addOnFailureListener(failure(out));
        return out;
    }

    private synchronized LanguageIdentifier client(LanguageOptions options) {
        if (client == null) {
            client = LanguageIdentification.getClient(
                    new LanguageIdentificationOptions.Builder()
                            .setConfidenceThreshold(
                                    options.getMinimumConfidence())
                            .build());
        }
        return client;
    }

    public synchronized void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
