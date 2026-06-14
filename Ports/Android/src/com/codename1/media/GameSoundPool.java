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
package com.codename1.media;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import com.codename1.impl.android.AndroidImplementation;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/// Android low latency sound pool backed by `android.media.SoundPool`, the platform
/// API purpose built for short game sound effects.
///
/// A loaded sound is an Android sound id; a playing voice is an Android stream id,
/// returned to the gaming layer as the voice id. Loads are asynchronous in the
/// platform; a sound may play silently if triggered before its load completes,
/// which for games (load up front, play later) is rarely an issue.
public class GameSoundPool implements SoundPoolPeer {
    private final AndroidImplementation impl;
    private final SoundPool pool;
    private final Map state = new ConcurrentHashMap();   // streamId -> float[]{volume, pan}
    private final Map tempFiles = new ConcurrentHashMap(); // soundId -> File

    public GameSoundPool(AndroidImplementation impl, int maxStreams) {
        this.impl = impl;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(maxStreams < 1 ? 1 : maxStreams)
                .setAudioAttributes(attrs)
                .build();
    }

    private static Context context() {
        return AndroidImplementation.getContext();
    }

    private Object loadFromFile(File f) {
        int soundId = pool.load(f.getAbsolutePath(), 1);
        tempFiles.put(Integer.valueOf(soundId), f);
        return Integer.valueOf(soundId);
    }

    private File copyToTemp(InputStream data) throws IOException {
        File f = File.createTempFile("cn1sfx", ".dat", context().getCacheDir());
        OutputStream os = new FileOutputStream(f);
        try {
            byte[] buf = new byte[8192];
            int r;
            while ((r = data.read(buf)) > 0) {
                os.write(buf, 0, r);
            }
        } finally {
            os.close();
            try {
                data.close();
            } catch (IOException e) {
                // best effort; ignore
            }
        }
        return f;
    }

    public Object loadSound(InputStream data, String mimeType) throws IOException {
        return loadFromFile(copyToTemp(data));
    }

    public Object loadSound(String uri) throws IOException {
        InputStream in = impl.getResourceAsStream(impl.getClass(), uri);
        if (in == null) {
            throw new IOException("sound not found: " + uri);
        }
        return loadFromFile(copyToTemp(in));
    }

    private static float[] gains(float volume, float pan) {
        if (volume < 0) {
            volume = 0;
        }
        if (volume > 1) {
            volume = 1;
        }
        if (pan < -1) {
            pan = -1;
        }
        if (pan > 1) {
            pan = 1;
        }
        float left = (float) Math.cos((pan + 1) * Math.PI / 4);
        float right = (float) Math.sin((pan + 1) * Math.PI / 4);
        return new float[]{volume * left, volume * right};
    }

    public int play(Object sound, float volume, float pan, float rate, int loop) {
        int soundId = ((Integer) sound).intValue();
        float[] g = gains(volume, pan);
        float r = rate <= 0 ? 1f : rate;
        int streamId = pool.play(soundId, g[0], g[1], 1, loop, r);
        if (streamId == 0) {
            return -1;
        }
        state.put(Integer.valueOf(streamId), new float[]{volume, pan});
        return streamId;
    }

    public void setVolume(int voiceId, float volume) {
        float[] s = (float[]) state.get(Integer.valueOf(voiceId));
        float pan = s == null ? 0f : s[1];
        float[] g = gains(volume, pan);
        pool.setVolume(voiceId, g[0], g[1]);
        state.put(Integer.valueOf(voiceId), new float[]{volume, pan});
    }

    public void setRate(int voiceId, float rate) {
        pool.setRate(voiceId, rate <= 0 ? 1f : rate);
    }

    public void setPan(int voiceId, float pan) {
        float[] s = (float[]) state.get(Integer.valueOf(voiceId));
        float vol = s == null ? 1f : s[0];
        float[] g = gains(vol, pan);
        pool.setVolume(voiceId, g[0], g[1]);
        state.put(Integer.valueOf(voiceId), new float[]{vol, pan});
    }

    public void pauseVoice(int voiceId) {
        pool.pause(voiceId);
    }

    public void resumeVoice(int voiceId) {
        pool.resume(voiceId);
    }

    public void stopVoice(int voiceId) {
        pool.stop(voiceId);
        state.remove(Integer.valueOf(voiceId));
    }

    public void stopAll() {
        pool.autoPause();
        state.clear();
    }

    public void autoPause() {
        pool.autoPause();
    }

    public void autoResume() {
        pool.autoResume();
    }

    public void unloadSound(Object sound) {
        int soundId = ((Integer) sound).intValue();
        pool.unload(soundId);
        File f = (File) tempFiles.remove(Integer.valueOf(soundId));
        deleteQuietly(f);
    }

    public void release() {
        pool.release();
        state.clear();
        java.util.Iterator it = tempFiles.values().iterator();
        while (it.hasNext()) {
            deleteQuietly((File) it.next());
        }
        tempFiles.clear();
    }

    /// Best-effort delete of a temp file; a failed delete is not actionable here
    /// (the OS cleans the cache dir), so the return value is intentionally consumed.
    private static void deleteQuietly(File f) {
        if (f != null && !f.delete()) {
            f.deleteOnExit();
        }
    }
}
