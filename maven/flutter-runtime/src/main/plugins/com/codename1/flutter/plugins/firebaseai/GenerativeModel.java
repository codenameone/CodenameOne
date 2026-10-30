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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.codename1.flutter.plugins.firebase.FirebaseOptions;
import com.codename1.flutter.plugins.firebase.User;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;

import dart.async.Completer;
import dart.async.Future;
import dart.async.Stream;
import dart.runtime.Funcs;

/** firebase_ai's {@code GenerativeModel}; see {@link FirebaseAI} for the transport and what is not applied. */
public class GenerativeModel {
    private static final String BASE = "https://firebasevertexai.googleapis.com/v1beta/projects/";

    private final FirebaseAI ai;
    private final String name;
    private final GenerationConfig generationConfig;
    private final List<SafetySetting> safetySettings = new ArrayList<SafetySetting>();
    private final List<Tool> tools = new ArrayList<Tool>();
    private final Content systemInstruction;

    GenerativeModel(FirebaseAI ai, String model, GenerationConfig generationConfig, Iterable<?> safety,
            Iterable<?> tools, Content systemInstruction) {
        this.ai = ai;
        String m = model == null ? "" : model;
        int idx = m.lastIndexOf("models/");
        this.name = idx >= 0 ? m.substring(idx + 7) : m;
        this.generationConfig = generationConfig;
        addSafety(this.safetySettings, safety);
        addTools(this.tools, tools);
        this.systemInstruction = systemInstruction;
    }

    private static void addSafety(List<SafetySetting> out, Iterable<?> in) {
        if (in != null) {
            for (Object o : in) {
                if (o instanceof SafetySetting) {
                    out.add((SafetySetting) o);
                }
            }
        }
    }

    private static void addTools(List<Tool> out, Iterable<?> in) {
        if (in != null) {
            for (Object o : in) {
                if (o instanceof Tool) {
                    out.add((Tool) o);
                }
            }
        }
    }

    public Future<GenerateContentResponse> generateContent(Iterable<?> prompt, Iterable<?> safetySettings,
            GenerationConfig generationConfig, Iterable<?> tools, Object toolConfig) {
        List<Content> contents = contents(prompt);
        Map<String, Object> body = body(contents, safetySettings, generationConfig, tools);
        final Completer<GenerateContentResponse> done = new Completer<GenerateContentResponse>();
        call("generateContent", body).then(new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                try {
                    done.complete(GenerateContentResponse.fromMap(r));
                } catch (RuntimeException e) {
                    done.completeError(e);
                }
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object err) {
                done.completeError(err);
            }
        });
        return done.future();
    }

    /**
     * {@code generateContentStream}: the REST streaming endpoint is not used, so the stream is
     * one event, the whole reply of {@link #generateContent}.
     */
    public Stream<GenerateContentResponse> generateContentStream(Iterable<?> prompt, Iterable<?> safetySettings,
            GenerationConfig generationConfig, Iterable<?> tools, Object toolConfig) {
        return Stream.fromFuture(generateContent(prompt, safetySettings, generationConfig, tools, toolConfig));
    }

    public Future<CountTokensResponse> countTokens(Iterable<?> contents) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        List<Object> cs = new ArrayList<Object>();
        for (Content c : contents(contents)) {
            cs.add(c.toJson());
        }
        body.put("contents", cs);
        final Completer<CountTokensResponse> done = new Completer<CountTokensResponse>();
        call("countTokens", body).then(new Funcs.VoidFunc1<Map<String, Object>>() {
            @Override
            public void call(Map<String, Object> r) {
                Long total = GenerateContentResponse.num(r.get("totalTokens"));
                done.complete(new CountTokensResponse(total == null ? 0L : total.longValue(),
                        GenerateContentResponse.num(r.get("totalBillableCharacters"))));
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object err) {
                done.completeError(err);
            }
        });
        return done.future();
    }

    public ChatSession startChat(Iterable<?> history, Iterable<?> safetySettings, GenerationConfig generationConfig) {
        return new ChatSession(this, contents(history), safetySettings, generationConfig);
    }

    private static List<Content> contents(Iterable<?> in) {
        List<Content> out = new ArrayList<Content>();
        if (in != null) {
            for (Object o : in) {
                if (o instanceof Content) {
                    out.add((Content) o);
                }
            }
        }
        return out;
    }

    private Map<String, Object> body(List<Content> contents, Iterable<?> safetyOverride,
            GenerationConfig configOverride, Iterable<?> toolsOverride) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        List<Object> cs = new ArrayList<Object>();
        for (Content c : contents) {
            cs.add(c.toJson());
        }
        body.put("contents", cs);
        List<SafetySetting> safety = this.safetySettings;
        if (safetyOverride != null) {
            safety = new ArrayList<SafetySetting>();
            addSafety(safety, safetyOverride);
        }
        if (!safety.isEmpty()) {
            List<Object> ss = new ArrayList<Object>();
            for (SafetySetting s : safety) {
                ss.add(s.toJson());
            }
            body.put("safetySettings", ss);
        }
        GenerationConfig cfg = configOverride == null ? generationConfig : configOverride;
        if (cfg != null) {
            body.put("generationConfig", cfg.toJson());
        }
        List<Tool> ts = this.tools;
        if (toolsOverride != null) {
            ts = new ArrayList<Tool>();
            addTools(ts, toolsOverride);
        }
        List<Object> tj = new ArrayList<Object>();
        for (Tool t : ts) {
            Map<String, Object> e = t.toJson();
            if (e != null) {
                tj.add(e);
            }
        }
        if (!tj.isEmpty()) {
            body.put("tools", tj);
        }
        if (systemInstruction != null) {
            body.put("systemInstruction", systemInstruction.toJson());
        }
        return body;
    }

    /** The endpoint of {@code method}. */
    String url(String method) {
        FirebaseOptions o = ai.app().options();
        StringBuilder sb = new StringBuilder(BASE).append(o.projectId());
        if (ai.isVertex()) {
            sb.append("/locations/").append(ai.location()).append("/publishers/google/models/");
        } else {
            sb.append("/models/");
        }
        return sb.append(name).append(':').append(method).toString();
    }

    Future<Map<String, Object>> call(final String method, final Map<String, Object> body) {
        final Completer<Map<String, Object>> done = new Completer<Map<String, Object>>();
        FirebaseOptions o = ai.app().options();
        if (o.apiKey() == null || o.projectId() == null) {
            done.completeError(new FirebaseAIException(
                    "Call Firebase.initializeApp with FirebaseOptions that carry an apiKey and a projectId first"));
            return done.future();
        }
        if (!Display.isInitialized()) {
            done.completeError(new FirebaseAIException(
                    "A network error has occurred: Codename One is not initialized"));
            return done.future();
        }
        final List<String[]> headers = new ArrayList<String[]>();
        headers.add(new String[] {"x-goog-api-key", o.apiKey()});
        final Runnable send = new Runnable() {
            @Override
            public void run() {
                AiTransport.post(url(method), headers, JSONParser.toJson(body)).then(
                        new Funcs.VoidFunc1<Map<String, Object>>() {
                            @Override
                            public void call(Map<String, Object> r) {
                                done.complete(r);
                            }
                        }, new Funcs.VoidFunc1<Object>() {
                            @Override
                            public void call(Object err) {
                                done.completeError(err);
                            }
                        });
            }
        };
        final Runnable withAuth = new Runnable() {
            @Override
            public void run() {
                User u = ai.auth().currentUser();
                if (u == null) {
                    send.run();
                    return;
                }
                u.getIdToken(Boolean.FALSE).then(new Funcs.VoidFunc1<String>() {
                    @Override
                    public void call(String t) {
                        if (t != null) {
                            headers.add(new String[] {"Authorization", "Firebase " + t});
                        }
                        send.run();
                    }
                }, new Funcs.VoidFunc1<Object>() {
                    @Override
                    public void call(Object err) {
                        send.run();
                    }
                });
            }
        };
        ai.appCheck().getToken(Boolean.FALSE).then(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String t) {
                if (t != null) {
                    headers.add(new String[] {"X-Firebase-AppCheck", t});
                }
                withAuth.run();
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object err) {
                withAuth.run();
            }
        });
        return done.future();
    }
}
