/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.media;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// File transcription request shared by transcription providers.
public final class TranscriptionRequest {
    private final String audioPath;
    private String languageTag = "en-US";
    private String prompt;
    private final Map<String, String> options = new HashMap<String, String>();

    public TranscriptionRequest(String audioPath) {
        if (audioPath == null || audioPath.length() == 0) {
            throw new IllegalArgumentException("audioPath is required");
        }
        this.audioPath = audioPath;
    }

    public static TranscriptionRequest file(String audioPath) {
        return new TranscriptionRequest(audioPath);
    }

    public String getAudioPath() {
        return audioPath;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public TranscriptionRequest setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
        return this;
    }

    public String getPrompt() {
        return prompt;
    }

    public TranscriptionRequest setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    public TranscriptionRequest setOption(String key, String value) {
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("key is required");
        }
        if (value == null) {
            options.remove(key);
        } else {
            options.put(key, value);
        }
        return this;
    }

    public String getOption(String key) {
        return options.get(key);
    }

    public Map<String, String> getOptions() {
        return Collections.unmodifiableMap(options);
    }
}
