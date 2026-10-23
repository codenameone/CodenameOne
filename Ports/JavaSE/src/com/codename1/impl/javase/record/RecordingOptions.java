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
package com.codename1.impl.javase.record;

import java.io.File;
import java.util.Map;

/// What a simulator recording contains and how it looks. Every field can be set
/// from the recording settings dialog and from the `sim_record_start` MCP tool,
/// whose argument names are the names used in [#apply(Map)].
public final class RecordingOptions {

    /// Which part of the window is recorded.
    public enum Frame {
        /// The device screen alone, at its own resolution.
        SCREEN,
        /// The screen inside the device skin, as the window shows it.
        SKIN
    }

    /// Where the audio track comes from.
    public enum Audio {
        /// No audio track, unless tap sounds are on.
        NONE,
        /// What the application plays: sound effects, game sounds, music and
        /// video soundtracks, captured where the simulator hands them to the
        /// sound card, so nothing else on the computer is recorded.
        APP,
        /// The default microphone, through javax.sound.
        MICROPHONE,
        /// The application's audio mixed with the microphone, for narration.
        APP_AND_MICROPHONE;

        boolean includesApp() {
            return this == APP || this == APP_AND_MICROPHONE;
        }

        boolean includesMicrophone() {
            return this == MICROPHONE || this == APP_AND_MICROPHONE;
        }
    }

    private String path;
    private String format = "mp4";
    private int fps = 30;
    private float scale = 1f;
    private Frame frame = Frame.SCREEN;
    private TouchOverlay.Style touches = TouchOverlay.Style.RIPPLE;
    private boolean touchTrail = true;
    private int touchColor = 0x80808080;
    private int touchRadius = 22;
    private boolean tapSound;
    private Audio audio = Audio.APP;
    private int maxSeconds = 600;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFormat() {
        return format;
    }

    /// mp4, mov, webm or gif. A GIF is encoded as an mp4 first and converted when
    /// the recording stops, and never carries audio.
    public void setFormat(String format) {
        String f = format == null ? "mp4" : asciiLower(format.trim());
        if (!"mp4".equals(f) && !"mov".equals(f) && !"webm".equals(f) && !"gif".equals(f)) {
            throw new IllegalArgumentException("format must be mp4, mov, webm or gif: " + format);
        }
        this.format = f;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        if (fps < 1 || fps > 120) {
            throw new IllegalArgumentException("fps must be between 1 and 120: " + fps);
        }
        this.fps = fps;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        if (scale <= 0.05f || scale > 4f) {
            throw new IllegalArgumentException("scale must be in (0.05, 4]: " + scale);
        }
        this.scale = scale;
    }

    public Frame getFrame() {
        return frame;
    }

    public void setFrame(Frame frame) {
        this.frame = frame == null ? Frame.SCREEN : frame;
    }

    public TouchOverlay.Style getTouches() {
        return touches;
    }

    public void setTouches(TouchOverlay.Style touches) {
        this.touches = touches == null ? TouchOverlay.Style.NONE : touches;
    }

    public boolean isTouchTrail() {
        return touchTrail;
    }

    public void setTouchTrail(boolean touchTrail) {
        this.touchTrail = touchTrail;
    }

    public int getTouchColor() {
        return touchColor;
    }

    public void setTouchColor(int touchColor) {
        this.touchColor = touchColor;
    }

    public int getTouchRadius() {
        return touchRadius;
    }

    public void setTouchRadius(int touchRadius) {
        this.touchRadius = touchRadius;
    }

    public boolean isTapSound() {
        return tapSound;
    }

    public void setTapSound(boolean tapSound) {
        this.tapSound = tapSound;
    }

    public Audio getAudio() {
        return audio;
    }

    public void setAudio(Audio audio) {
        this.audio = audio == null ? Audio.NONE : audio;
    }

    public int getMaxSeconds() {
        return maxSeconds;
    }

    /// A safety stop, so a recording an agent forgot to stop does not fill the disk.
    public void setMaxSeconds(int maxSeconds) {
        this.maxSeconds = Math.max(1, maxSeconds);
    }

    /// True when the output gets an audio track.
    public boolean hasAudioTrack() {
        return !"gif".equals(format) && (tapSound || audio != Audio.NONE);
    }

    /// The output file: the configured path, or a time stamped file in the user's
    /// home directory.
    public File resolveOutput() {
        if (path != null && path.length() > 0) {
            String p = path;
            if (p.startsWith("file:")) {
                p = p.substring(5);
                while (p.startsWith("//")) {
                    p = p.substring(1);
                }
            }
            return new File(p);
        }
        File dir = new File(System.getProperty("user.home"), "CodenameOneRecordings");
        String stamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
        return new File(dir, "simulator-" + stamp + "." + format);
    }

    /// Applies MCP style arguments. Unknown keys are ignored; a bad value throws
    /// [IllegalArgumentException] naming it.
    public void apply(Map<String, Object> args) {
        if (args == null) {
            return;
        }
        if (args.get("path") instanceof String) {
            setPath((String) args.get("path"));
        }
        if (args.get("format") instanceof String) {
            setFormat((String) args.get("format"));
        } else if (path != null) {
            // A path ending in .gif with no format used to get mp4 bytes under a
            // .gif name; the extension is the format when none is given.
            String ext = extension(path);
            if ("mp4".equals(ext) || "mov".equals(ext) || "webm".equals(ext) || "gif".equals(ext)) {
                setFormat(ext);
            }
        }
        if (args.get("fps") instanceof Number) {
            setFps(((Number) args.get("fps")).intValue());
        }
        if (args.get("scale") instanceof Number) {
            setScale(((Number) args.get("scale")).floatValue());
        }
        if (args.get("frame") instanceof String) {
            setFrame(Frame.valueOf(enumName((String) args.get("frame"), "frame")));
        }
        if (args.get("touches") instanceof String) {
            setTouches(TouchOverlay.Style.valueOf(enumName((String) args.get("touches"), "touches")));
        }
        if (args.get("touchTrail") != null) {
            setTouchTrail(bool(args.get("touchTrail")));
        }
        if (args.get("touchColor") instanceof String) {
            setTouchColor(parseColor((String) args.get("touchColor")));
        }
        if (args.get("touchRadius") instanceof Number) {
            setTouchRadius(((Number) args.get("touchRadius")).intValue());
        }
        if (args.get("tapSound") != null) {
            setTapSound(bool(args.get("tapSound")));
        }
        if (args.get("audio") instanceof String) {
            setAudio(Audio.valueOf(enumName((String) args.get("audio"), "audio")));
        }
        if (args.get("maxSeconds") instanceof Number) {
            setMaxSeconds(((Number) args.get("maxSeconds")).intValue());
        }
    }

    /// A JSON boolean. Codename One's JSONParser hands `true` over as the string
    /// "true" unless it was told to use Boolean instances, so both are accepted.
    public static boolean bool(Object v) {
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        return "true".equals(String.valueOf(v));
    }

    private static String extension(String p) {
        int dot = p.lastIndexOf('.');
        int slash = Math.max(p.lastIndexOf('/'), p.lastIndexOf('\\'));
        if (dot < 0 || dot < slash) {
            return "";
        }
        return asciiLower(p.substring(dot + 1));
    }

    private static String enumName(String value, String key) {
        String upper = asciiUpper(value.trim());
        if (upper.length() == 0) {
            throw new IllegalArgumentException(key + " must not be empty");
        }
        return upper;
    }

    /// Parses `#RRGGBB` or `#AARRGGBB`.
    static int parseColor(String s) {
        String v = s.trim();
        if (v.startsWith("#")) {
            v = v.substring(1);
        }
        if (v.length() != 6 && v.length() != 8) {
            throw new IllegalArgumentException("touchColor must be #RRGGBB or #AARRGGBB: " + s);
        }
        long parsed = Long.parseLong(v, 16);
        if (v.length() == 6) {
            parsed |= 0xff000000L;
        }
        return (int) parsed;
    }

    static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + ('a' - 'A'));
            }
        }
        return new String(c);
    }

    static String asciiUpper(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'a' && c[i] <= 'z') {
                c[i] = (char) (c[i] - ('a' - 'A'));
            }
        }
        return new String(c);
    }
}
