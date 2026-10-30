/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.audioplayers;

import com.codename1.media.Media;

import java.io.IOException;

/** audioplayers' {@code Source}: what a player opens. */
public abstract class Source {
    private String mimeType;

    protected Source(String mimeType) {
        this.mimeType = mimeType;
    }

    /** The {@code mimeType} constructor argument. */
    public void mimeType(String v) {
        mimeType = v;
    }

    public String mimeType() {
        return mimeType;
    }

    /** Opens the sound as a Codename One Media; {@code onComplete} runs when it ends. */
    abstract Media open(Runnable onComplete) throws IOException;

    static String guessMime(String name, String given) {
        if (given != null) {
            return given;
        }
        String n = name == null ? "" : name;
        if (endsWith(n, ".wav")) {
            return "audio/wav";
        }
        if (endsWith(n, ".ogg")) {
            return "audio/ogg";
        }
        if (endsWith(n, ".m4a") || endsWith(n, ".aac") || endsWith(n, ".mp4")) {
            return "audio/mp4";
        }
        return "audio/mpeg";
    }

    private static boolean endsWith(String s, String suffix) {
        return s.length() >= suffix.length()
                && s.regionMatches(true, s.length() - suffix.length(), suffix, 0, suffix.length());
    }
}
