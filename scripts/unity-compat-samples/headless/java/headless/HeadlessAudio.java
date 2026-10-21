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
package headless;

import com.codename1.unitycompat.unityengine.AudioClip;
import com.codename1.unitycompat.unityengine.AudioOutput;
import com.codename1.unitycompat.unityengine.Time;
import java.util.ArrayList;

/// An output that makes no sound and keeps the account a device's would:
/// which voices are sounding and at what volume, the listener's included.
/// It speaks only when a volume changes -- a source's on a voice that is
/// playing, or the listener's -- so a trace of a project that changes
/// neither has no line of it.
///
/// Whether a voice that does not loop has ended is read off the clip's
/// length, its pitch and the game's unscaled clock. That is an estimate,
/// as a device's ear would be: a pause held over a voice is not counted,
/// and a seek is not either.
final class HeadlessAudio implements AudioOutput {
    private static final class Voice {
        int id;
        String clip;
        float level;
        boolean loop;
        float endsAt;
    }

    private final ArrayList voices = new ArrayList();
    private final ArrayList lines = new ArrayList();
    private float listener = 1f;

    private static int percent(float volume) {
        return (int) (volume * 100f + 0.5f);
    }

    private Voice find(int id) {
        for (int i = 0; i < voices.size(); i++) {
            Voice v = (Voice) voices.get(i);
            if (v.id == id) {
                return v;
            }
        }
        return null;
    }

    private boolean sounding(Voice v) {
        return v.loop || Time.get_unscaledTime() < v.endsAt;
    }

    @Override
    public void play(int voice, AudioClip clip, float volume, float pitch, boolean loop) {
        stop(voice);
        if (clip == null) {
            return;
        }
        Voice v = new Voice();
        v.id = voice;
        v.clip = clip.get_name();
        v.level = volume;
        v.loop = loop;
        float length = clip.get_length();
        v.endsAt = Time.get_unscaledTime() + (pitch > 0f ? length / pitch : length);
        voices.add(v);
    }

    @Override
    public void stop(int voice) {
        Voice v = find(voice);
        if (v != null) {
            voices.remove(v);
        }
    }

    @Override
    public void pause(int voice, boolean paused) {
        // Not counted: see the head of this class.
    }

    @Override
    public void volume(int voice, float volume) {
        Voice v = find(voice);
        if (v != null) {
            v.level = volume;
            lines.add("output volume " + v.clip + " " + percent(volume * listener));
        }
    }

    @Override
    public void listenerVolume(float volume) {
        if (volume >= listener && volume <= listener) {
            // The same volume again, as an output is given when it is
            // installed: nothing a device would change.
            return;
        }
        listener = volume;
        StringBuilder sb = new StringBuilder("output listener ").append(percent(volume)).append(':');
        for (int i = 0; i < voices.size(); i++) {
            Voice v = (Voice) voices.get(i);
            if (sounding(v)) {
                sb.append(' ').append(v.clip).append(v.loop ? " looping " : " ").append(percent(v.level * listener));
            }
        }
        lines.add(sb.toString());
    }

    @Override
    public void pitch(int voice, float pitch) {
        // The volume is what this output records.
    }

    @Override
    public void seek(int voice, float seconds) {
        // Not counted: see the head of this class.
    }

    @Override
    public void loop(int voice, boolean loop) {
        Voice v = find(voice);
        if (v != null) {
            v.loop = loop;
        }
    }

    @Override
    public void hold(boolean held) {
        // Not counted: see the head of this class.
    }

    /// The lines recorded since the last call, and forgets them.
    String[] drain() {
        String[] out = new String[lines.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = (String) lines.get(i);
        }
        lines.clear();
        return out;
    }
}
