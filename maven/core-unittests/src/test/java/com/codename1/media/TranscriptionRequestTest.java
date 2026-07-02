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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptionRequestTest {

    @Test
    void defaultsAndSettersRoundTrip() {
        TranscriptionRequest request = TranscriptionRequest.file("audio.wav");
        assertEquals("audio.wav", request.getAudioPath());
        assertEquals("en-US", request.getLanguageTag());
        assertNull(request.getPrompt());

        assertSame(request, request.setLanguageTag("fr-CA"));
        assertSame(request, request.setPrompt("names: Codename One"));
        assertSame(request, request.setOption("temperature", "0"));

        assertEquals("fr-CA", request.getLanguageTag());
        assertEquals("names: Codename One", request.getPrompt());
        assertEquals("0", request.getOption("temperature"));
        assertEquals("0", request.getOptions().get("temperature"));
    }

    @Test
    void requiredFieldsAreValidated() {
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new TranscriptionRequest(null);
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                new TranscriptionRequest("");
            }
        });
        assertThrows(IllegalArgumentException.class, new org.junit.jupiter.api.function.Executable() {
            public void execute() {
                TranscriptionRequest.file("a.wav").setOption("", "x");
            }
        });
    }
}
