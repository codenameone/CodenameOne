/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.firebaseai;

import java.util.ArrayList;
import java.util.List;

import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.core.DartList;
import dart.runtime.Funcs;

/** firebase_ai's {@code ChatSession}: the history grows by each message and the reply that answered it. */
public class ChatSession {
    private final GenerativeModel model;
    private final DartList<Content> history = new DartList<Content>();
    private final Iterable<?> safetySettings;
    private final GenerationConfig generationConfig;

    ChatSession(GenerativeModel model, List<Content> history, Iterable<?> safetySettings,
            GenerationConfig generationConfig) {
        this.model = model;
        this.history.addAll(history);
        this.safetySettings = safetySettings;
        this.generationConfig = generationConfig;
    }

    public DartList<Content> history() {
        return history;
    }

    public Future<GenerateContentResponse> sendMessage(final Content message) {
        List<Content> prompt = new ArrayList<Content>(history);
        prompt.add(message);
        final Completer<GenerateContentResponse> done = new Completer<GenerateContentResponse>();
        model.generateContent(prompt, safetySettings, generationConfig, null, null).then(
                new Funcs.VoidFunc1<GenerateContentResponse>() {
                    @Override
                    public void call(GenerateContentResponse r) {
                        history.add(message);
                        if (!r.candidates().isEmpty()) {
                            history.add(r.candidates().get(0).content());
                        }
                        done.complete(r);
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object err) {
                        done.completeError(err);
                    }
                });
        return done.future();
    }

    /** One event, the whole reply (see {@link GenerativeModel#generateContentStream}). */
    public Stream<GenerateContentResponse> sendMessageStream(Content message) {
        return Stream.fromFuture(sendMessage(message));
    }
}
