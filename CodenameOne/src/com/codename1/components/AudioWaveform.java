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
package com.codename1.components;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Graphics;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;

/// The bars of an audio clip's loudness, with the played part in the accent
/// colour: the waveform of a voice message, a recording or a podcast scrubber.
///
/// ```java
/// AudioWaveform wave = new AudioWaveform(AudioWaveform.levelsFromPcm(samples, 48));
/// wave.addActionListener(e -> player.setTime(
///         (int) (wave.getProgress() * player.getDuration())));
/// // as playback advances
/// wave.setProgress(player.getTime() / (float) player.getDuration());
/// ```
///
/// Levels are fractions from 0 to 1, one per bar, and are drawn evenly across
/// the width. Tapping or dragging seeks: the progress follows the finger and
/// action listeners fire, so the component doubles as the clip's scrubber. Call
/// `setSeekable(false)` for a display only waveform.
///
/// UIIDs: `AudioWaveform`. The played bars take the foreground of
/// `AudioWaveformPlayed` and the rest the background of `AudioWaveformTrack`
/// when the theme defines them, and otherwise the theme's `Slider` colours.
public class AudioWaveform extends Component {
    private float[] levels;
    private float progress;
    private boolean seekable = true;
    private boolean seeking;
    private float barMM = 0.6f;
    private float gapMM = 0.4f;
    private float heightMM = 8f;
    private final EventDispatcher listeners = new EventDispatcher();

    /// An empty waveform; call `#setLevels(float[])`.
    public AudioWaveform() {
        this(new float[0]);
    }

    /// A waveform of the given levels.
    ///
    /// #### Parameters
    ///
    /// - `levels`: one loudness per bar, 0 to 1
    public AudioWaveform(float[] levels) {
        setUIID("AudioWaveform");
        setLevels(levels);
    }

    @Override
    protected void initUnselectedStyle(Style unselectedStyle) {
        super.initUnselectedStyle(unselectedStyle);
        if (unselectedStyle != null && !getUIManager().isThemeStyleDefined(getUIID())) {
            unselectedStyle.setBgTransparency(0);
        }
    }

    /// Reduces raw 16 bit PCM samples to `bars` levels: the peak of each slice,
    /// scaled so the loudest bar is 1.
    ///
    /// #### Parameters
    ///
    /// - `samples`: the PCM samples
    ///
    /// - `bars`: how many levels to produce
    ///
    /// #### Returns
    ///
    /// the levels, 0 to 1
    public static float[] levelsFromPcm(short[] samples, int bars) {
        float[] out = new float[Math.max(0, bars)];
        if (samples == null || samples.length == 0 || bars <= 0) {
            return out;
        }
        float max = 0;
        for (int b = 0; b < bars; b++) {
            int from = (int) ((long) samples.length * b / bars);
            int to = Math.max(from + 1, (int) ((long) samples.length * (b + 1) / bars));
            int peak = 0;
            for (int i = from; i < to && i < samples.length; i++) {
                peak = Math.max(peak, Math.abs((int) samples[i]));
            }
            out[b] = peak;
            max = Math.max(max, peak);
        }
        for (int b = 0; b < bars; b++) {
            out[b] = max == 0 ? 0 : out[b] / max;
        }
        return out;
    }

    /// The levels drawn, one per bar.
    ///
    /// #### Returns
    ///
    /// a copy of the levels
    public float[] getLevels() {
        float[] copy = new float[levels.length];
        System.arraycopy(levels, 0, copy, 0, levels.length);
        return copy;
    }

    /// Sets the levels, one per bar, each clamped to 0 to 1.
    ///
    /// #### Parameters
    ///
    /// - `levels`: the loudness of each bar
    public final void setLevels(float[] levels) {
        float[] copy = new float[levels == null ? 0 : levels.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = Math.max(0, Math.min(1, levels[i]));
        }
        this.levels = copy;
        repaint();
    }

    /// How much has played, 0 to 1.
    ///
    /// #### Returns
    ///
    /// the played fraction
    public float getProgress() {
        return progress;
    }

    /// Sets how much has played, clamped to 0 to 1.
    ///
    /// #### Parameters
    ///
    /// - `progress`: the played fraction
    public void setProgress(float progress) {
        float p = Math.max(0, Math.min(1, progress));
        if (Float.compare(p, this.progress) != 0) {
            this.progress = p;
            repaint();
        }
    }

    /// Whether tapping and dragging seek.
    ///
    /// #### Returns
    ///
    /// true by default
    public boolean isSeekable() {
        return seekable;
    }

    /// Turns seeking by touch on or off.
    ///
    /// #### Parameters
    ///
    /// - `seekable`: false for a display only waveform
    public void setSeekable(boolean seekable) {
        this.seekable = seekable;
    }

    /// True while the user drags across the waveform.
    ///
    /// #### Returns
    ///
    /// whether a seek drag is in progress
    public boolean isSeeking() {
        return seeking;
    }

    /// Sets the bar geometry.
    ///
    /// #### Parameters
    ///
    /// - `barMM`: each bar's width in millimetres
    ///
    /// - `gapMM`: the space between bars in millimetres
    ///
    /// - `heightMM`: the preferred height in millimetres
    public void setBarSize(float barMM, float gapMM, float heightMM) {
        this.barMM = barMM;
        this.gapMM = gapMM;
        this.heightMM = heightMM;
        setShouldCalcPreferredSize(true);
        repaint();
    }

    /// Adds a listener called when the user seeks.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addActionListener(ActionListener l) {
        listeners.addListener(l);
    }

    /// Removes a listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeActionListener(ActionListener l) {
        listeners.removeListener(l);
    }

    @Override
    protected Dimension calcPreferredSize() {
        Style s = getStyle();
        int n = Math.max(levels.length, 1);
        int bar = Math.max(1, CN.convertToPixels(barMM));
        int gap = Math.max(1, CN.convertToPixels(gapMM));
        return new Dimension(n * (bar + gap) - gap + s.getHorizontalPadding(),
                CN.convertToPixels(heightMM) + s.getVerticalPadding());
    }

    @Override
    public void paint(Graphics g) {
        int n = levels.length;
        if (n == 0) {
            return;
        }
        Style s = getStyle();
        int left = getX() + s.getPaddingLeftNoRTL();
        int w = getWidth() - s.getHorizontalPadding();
        int h = getHeight() - s.getVerticalPadding();
        int mid = getY() + s.getPaddingTop() + h / 2;
        float slot = w / (float) n;
        int bar = Math.max(1, Math.min(CN.convertToPixels(barMM), Math.round(slot * 0.7f)));
        int played = Math.round(progress * n);
        int minH = Math.max(1, bar);
        int color = g.getColor();
        boolean aa = g.isAntiAliased();
        g.setAntiAliased(true);
        int accent = Palette.accent(this, "AudioWaveformPlayed");
        int track = Palette.track(this, "AudioWaveformTrack");
        for (int i = 0; i < n; i++) {
            int index = isRTL() ? n - 1 - i : i;
            int bh = Math.max(minH, Math.round(levels[index] * h));
            int x = left + Math.round(i * slot + (slot - bar) / 2);
            g.setColor(index < played ? accent : track);
            g.fillRoundRect(x, mid - bh / 2, bar, bh, bar, bar);
        }
        g.setColor(color);
        g.setAntiAliased(aa);
    }

    /// The progress a touch at an x coordinate (parent coordinates) seeks to.
    float progressAt(int x) {
        Style s = getStyle();
        int w = Math.max(1, getWidth() - s.getHorizontalPadding());
        float f = (x - getX() - s.getPaddingLeftNoRTL()) / (float) w;
        f = Math.max(0, Math.min(1, f));
        return isRTL() ? 1 - f : f;
    }

    @Override
    public void pointerPressed(int x, int y) {
        super.pointerPressed(x, y);
        if (seekable && isEnabled()) {
            seeking = true;
            seek(x);
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!seeking) {
            super.pointerDragged(x, y);
            return;
        }
        seek(x);
    }

    @Override
    public void pointerReleased(int x, int y) {
        super.pointerReleased(x, y);
        if (seeking) {
            seeking = false;
            listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));
        }
    }

    @Override
    protected boolean isStickyDrag() {
        return seeking;
    }

    private void seek(int absX) {
        setProgress(progressAt(absX - getAbsoluteX() + getX()));
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));
    }
}
