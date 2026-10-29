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

import java.io.IOException;
import java.util.Map;
import java.util.List;

import com.codename1.io.JSONParser;

import dart.core.DartList;

/**
 * firebase_ai's {@code GenerateContentResponse}. {@code text} joins the first candidate's text
 * parts and, like the real class, throws when the prompt itself was blocked.
 */
public class GenerateContentResponse {
    private final DartList<Candidate> candidates = new DartList<Candidate>();
    private final UsageMetadata usageMetadata;
    private final String blockReason;

    GenerateContentResponse(Iterable<Candidate> candidates, UsageMetadata usage, String blockReason) {
        for (Candidate c : candidates) {
            this.candidates.add(c);
        }
        this.usageMetadata = usage;
        this.blockReason = blockReason;
    }

    public DartList<Candidate> candidates() {
        return candidates;
    }

    public UsageMetadata usageMetadata() {
        return usageMetadata;
    }

    public String text() {
        if (candidates.isEmpty()) {
            if (blockReason != null) {
                throw new FirebaseAIException("Response was blocked due to " + blockReason);
            }
            return null;
        }
        return candidates.get(0).text();
    }

    /** Parses a generateContent reply; an {@code error} object raises FirebaseAIException. */
    public static GenerateContentResponse cn1FromJson(String json) {
        return fromMap(parse(json));
    }

    static Map<String, Object> parse(String json) {
        Map<String, Object> m;
        try {
            m = JSONParser.parseJSON(json);
        } catch (IOException e) {
            throw new FirebaseAIException("Unhandled format for response: " + e.getMessage());
        }
        if (m == null) {
            throw new FirebaseAIException("Unhandled format for response: empty body");
        }
        Object err = m.get("error");
        if (err instanceof Map) {
            Object msg = ((Map<?, ?>) err).get("message");
            throw new FirebaseAIException(msg instanceof String ? (String) msg : "Unknown server error");
        }
        return m;
    }

    static GenerateContentResponse fromMap(Map<String, Object> m) {
        List<Candidate> cs = new java.util.ArrayList<Candidate>();
        Object arr = m.get("candidates");
        if (arr instanceof List) {
            for (Object o : (List<?>) arr) {
                if (!(o instanceof Map)) {
                    continue;
                }
                Map<?, ?> c = (Map<?, ?>) o;
                Object content = c.get("content");
                Content ct = content instanceof Map ? Content.fromMap((Map<?, ?>) content) : new Content("model", null);
                cs.add(new Candidate(ct, FinishReason.parse(str(c.get("finishReason"))), str(c.get("finishMessage"))));
            }
        }
        UsageMetadata usage = null;
        Object u = m.get("usageMetadata");
        if (u instanceof Map) {
            Map<?, ?> um = (Map<?, ?>) u;
            usage = new UsageMetadata(num(um.get("promptTokenCount")), num(um.get("candidatesTokenCount")),
                    num(um.get("totalTokenCount")));
        }
        String block = null;
        Object pf = m.get("promptFeedback");
        if (pf instanceof Map) {
            block = str(((Map<?, ?>) pf).get("blockReason"));
        }
        return new GenerateContentResponse(cs, usage, block);
    }

    static String str(Object o) {
        return o instanceof String ? (String) o : null;
    }

    static Long num(Object o) {
        return o instanceof Number ? Long.valueOf(((Number) o).longValue()) : null;
    }
}
