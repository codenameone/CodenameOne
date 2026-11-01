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

import dart.core.DartList;
import dart.core.DartMap;

/** firebase_ai's {@code GenerationConfig}; {@code responseSchema} is accepted and not sent. */
public class GenerationConfig {
    private Long candidateCount;
    private DartList<String> stopSequences;
    private Long maxOutputTokens;
    private Double temperature;
    private Double topP;
    private Long topK;
    private String responseMimeType;

    /// Named arguments of the Dart constructor arrive as the setters below.
    public GenerationConfig() {
    }

    public void candidateCount(Long v) {
        candidateCount = v;
    }

    public void stopSequences(Iterable<?> v) {
        DartList<String> stops = null;
        if (v != null) {
            stops = new DartList<String>();
            for (Object o : v) {
                stops.add(String.valueOf(o));
            }
        }
        stopSequences = stops;
    }

    public void maxOutputTokens(Long v) {
        maxOutputTokens = v;
    }

    public void temperature(Double v) {
        temperature = v;
    }

    public void topP(Double v) {
        topP = v;
    }

    public void topK(Long v) {
        topK = v;
    }

    public void responseMimeType(String v) {
        responseMimeType = v;
    }

    public void responseSchema(Object v) {
        // accepted, not sent
    }

    public DartMap<String, Object> toJson() {
        DartMap<String, Object> m = new DartMap<String, Object>();
        put(m, "candidateCount", candidateCount);
        put(m, "maxOutputTokens", maxOutputTokens);
        put(m, "temperature", temperature);
        put(m, "topP", topP);
        put(m, "topK", topK);
        put(m, "stopSequences", stopSequences);
        put(m, "responseMimeType", responseMimeType);
        return m;
    }

    private static void put(DartMap<String, Object> m, String k, Object v) {
        if (v != null) {
            m.put(k, v);
        }
    }
}
