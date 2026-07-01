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

/// One timed transcript span. Times are offsets from the start of
/// the transcribed audio in milliseconds.
public final class TranscriptionSegment {
    private final long startTimeMs;
    private final long endTimeMs;
    private final String text;

    public TranscriptionSegment(long startTimeMs, long endTimeMs, String text) {
        if (startTimeMs < 0) {
            throw new IllegalArgumentException("startTimeMs must be >= 0");
        }
        if (endTimeMs < startTimeMs) {
            throw new IllegalArgumentException("endTimeMs must be >= startTimeMs");
        }
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.text = text == null ? "" : text;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public String getText() {
        return text;
    }
}
