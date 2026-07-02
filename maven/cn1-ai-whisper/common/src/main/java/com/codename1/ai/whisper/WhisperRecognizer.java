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

package com.codename1.ai.whisper;

import com.codename1.ai.LlmException;
import com.codename1.media.Transcriber;
import com.codename1.media.TranscriptionRequest;
import com.codename1.media.TranscriptionResult;
import com.codename1.media.TranscriptionSegment;
import com.codename1.system.NativeLookup;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.util.ArrayList;
import java.util.List;

/// On-device speech-to-text via whisper.cpp.
///
/// Transcribes audio files using whisper.cpp -- works offline. The cn1lib ships
/// the model loader; callers supply the model file and the audio file path.
///
public final class WhisperRecognizer {
    private WhisperRecognizer() { }

    /// True only when the running platform has a native bridge wired up.
    public static boolean isSupported() {
        NativeWhisperRecognizer bridge = NativeLookup.create(NativeWhisperRecognizer.class);
        return bridge != null && bridge.isSupported();
    }

    /// Transcribes audio using a whisper.cpp model. `modelPath` is the
    /// filesystem path to a ggml-format whisper model (e.g. `ggml-base.bin`);
    /// `audioPath` is a 16kHz mono WAV file.
    public static AsyncResource<String> transcribe(final String modelPath,
                                                    final String audioPath) {
        final AsyncResource<String> out = new AsyncResource<String>();
        final NativeWhisperRecognizer bridge =
                NativeLookup.create(NativeWhisperRecognizer.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("WhisperRecognizer.transcribe is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final String r = bridge.transcribe(modelPath, audioPath);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(r == null ? "" : r); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("WhisperRecognizer.transcribe failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }

    /// Provider-pluggable transcriber backed by whisper.cpp. `modelPath`
    /// is the filesystem path to a ggml-format Whisper model; each
    /// request supplies the 16kHz mono WAV audio path.
    public static Transcriber transcriber(final String modelPath) {
        return new Transcriber() {
            @Override
            public AsyncResource<TranscriptionResult> transcribe(TranscriptionRequest request) {
                if (request == null) {
                    AsyncResource<TranscriptionResult> out = new AsyncResource<TranscriptionResult>();
                    out.error(new IllegalArgumentException("request is required"));
                    return out;
                }
                return transcribeSegments(modelPath, request.getAudioPath());
            }

            @Override
            public String getProvider() {
                return "whisper";
            }
        };
    }

    /// Transcribes audio and returns timed segments. This exposes the
    /// segment timestamps already emitted by whisper.cpp, converted to
    /// millisecond offsets from the start of the audio.
    public static AsyncResource<TranscriptionResult> transcribeSegments(final String modelPath,
                                                                         final String audioPath) {
        final AsyncResource<TranscriptionResult> out = new AsyncResource<TranscriptionResult>();
        final NativeWhisperRecognizer bridge =
                NativeLookup.create(NativeWhisperRecognizer.class);
        if (bridge == null || !bridge.isSupported()) {
            out.error(new LlmException("WhisperRecognizer.transcribeSegments is not supported on this platform.",
                    -1, null, null, null, LlmException.ErrorType.UNKNOWN));
            return out;
        }
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override public void run() {
                try {
                    final String payload = bridge.transcribeSegments(modelPath, audioPath);
                    final TranscriptionResult result = parseSegments(payload);
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() { out.complete(result); }
                    });
                } catch (final Throwable t) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override public void run() {
                            out.error(new LlmException("WhisperRecognizer.transcribeSegments failed: " + t.getMessage(),
                                    -1, null, null, t, LlmException.ErrorType.UNKNOWN));
                        }
                    });
                }
            }
        });
        return out;
    }

    static TranscriptionResult parseSegments(String payload) {
        ArrayList<TranscriptionSegment> segments = new ArrayList<TranscriptionSegment>();
        if (payload != null && payload.length() > 0) {
            String[] lines = splitLines(payload);
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                if (line.length() == 0) {
                    continue;
                }
                String[] parts = splitTabs(line);
                if (parts.length != 3) {
                    throw new IllegalArgumentException("Invalid Whisper segment payload line: " + line);
                }
                segments.add(new TranscriptionSegment(
                        Long.parseLong(parts[0]),
                        Long.parseLong(parts[1]),
                        decodeText(parts[2])));
            }
        }
        return new TranscriptionResult(segments);
    }

    static String encodeSegmentsPayload(List<TranscriptionSegment> segments) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < segments.size(); i++) {
            TranscriptionSegment s = segments.get(i);
            out.append(s.getStartTimeMs()).append('\t')
                    .append(s.getEndTimeMs()).append('\t')
                    .append(encodeText(s.getText())).append('\n');
        }
        return out.toString();
    }

    private static String[] splitLines(String payload) {
        ArrayList<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < payload.length(); i++) {
            char ch = payload.charAt(i);
            if (ch == '\n') {
                int end = i;
                if (end > start && payload.charAt(end - 1) == '\r') {
                    end--;
                }
                lines.add(payload.substring(start, end));
                start = i + 1;
            }
        }
        if (start < payload.length()) {
            lines.add(payload.substring(start));
        }
        return lines.toArray(new String[lines.size()]);
    }

    private static String[] splitTabs(String line) {
        ArrayList<String> parts = new ArrayList<String>(3);
        int start = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                parts.add(line.substring(start, i));
                start = i + 1;
            }
        }
        parts.add(line.substring(start));
        return parts.toArray(new String[parts.size()]);
    }

    private static String encodeText(String text) {
        String value = text == null ? "" : text;
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '\t':
                    out.append("\\t");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    out.append(ch);
                    break;
            }
        }
        return out.toString();
    }

    private static String decodeText(String encoded) {
        if (encoded == null || encoded.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(encoded.length());
        boolean escaping = false;
        for (int i = 0; i < encoded.length(); i++) {
            char ch = encoded.charAt(i);
            if (!escaping) {
                if (ch == '\\') {
                    escaping = true;
                } else {
                    out.append(ch);
                }
                continue;
            }
            switch (ch) {
                case '\\':
                    out.append('\\');
                    break;
                case 't':
                    out.append('\t');
                    break;
                case 'n':
                    out.append('\n');
                    break;
                case 'r':
                    out.append('\r');
                    break;
                default:
                    out.append(ch);
                    break;
            }
            escaping = false;
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }
}
