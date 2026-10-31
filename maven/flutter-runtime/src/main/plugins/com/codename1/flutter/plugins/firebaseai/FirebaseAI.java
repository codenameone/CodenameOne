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

import com.codename1.flutter.plugins.firebase.Firebase;
import com.codename1.flutter.plugins.firebase.FirebaseApp;
import com.codename1.flutter.plugins.firebase.FirebaseAppCheck;
import com.codename1.flutter.plugins.firebase.FirebaseAuth;

/**
 * firebase_ai (Firebase AI Logic, Gemini) over the REST API at firebasevertexai.googleapis.com,
 * authenticated with the apiKey of the FirebaseApp from the firebase pack. An App Check token
 * (the firebase pack's debug token) and the signed-in user's ID token are sent when present.
 * {@code googleAI} addresses the Gemini Developer API, {@code vertexAI} Vertex AI in
 * {@code location} (default us-central1). Not applied: function calling ({@code Tool.functionDeclarations}),
 * {@code toolConfig}, response schemas, the block method of a SafetySetting, and live/imagen models.
 * With Codename One not initialized (headless) every request fails at once with a FirebaseAIException.
 */
public class FirebaseAI {
    private final FirebaseApp app;
    private final FirebaseAppCheck appCheck;
    private final FirebaseAuth auth;
    private final String location;
    private final boolean vertex;

    private FirebaseAI(FirebaseApp app, FirebaseAppCheck appCheck, FirebaseAuth auth, String location,
            boolean vertex) {
        this.app = app == null ? Firebase.app(null) : app;
        this.appCheck = appCheck;
        this.auth = auth;
        this.location = location;
        this.vertex = vertex;
    }

    public static FirebaseAI googleAI(FirebaseApp app, FirebaseAppCheck appCheck, FirebaseAuth auth) {
        return new FirebaseAI(app, appCheck, auth, null, false);
    }

    public static FirebaseAI vertexAI(FirebaseApp app, FirebaseAppCheck appCheck, FirebaseAuth auth,
            String location) {
        return new FirebaseAI(app, appCheck, auth, location == null ? "us-central1" : location, true);
    }

    public FirebaseApp app() {
        return app;
    }

    public String location() {
        return location;
    }

    FirebaseAppCheck appCheck() {
        return appCheck == null ? FirebaseAppCheck.instance : appCheck;
    }

    FirebaseAuth auth() {
        return auth == null ? FirebaseAuth.instanceFor(app, null) : auth;
    }

    boolean isVertex() {
        return vertex;
    }

    public GenerativeModel generativeModel(String model, GenerationConfig generationConfig,
            Iterable<?> safetySettings, Iterable<?> tools, Object toolConfig, Content systemInstruction) {
        return new GenerativeModel(this, model, generationConfig, safetySettings, tools, systemInstruction);
    }
}
