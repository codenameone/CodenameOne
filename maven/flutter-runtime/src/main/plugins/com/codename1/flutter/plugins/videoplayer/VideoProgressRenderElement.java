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
package com.codename1.flutter.plugins.videoplayer;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Slider;
import com.codename1.ui.events.DataChangedListener;

import dart.core.Duration;
import dart.runtime.Funcs;

/** Hosts a Slider for {@link VideoProgressIndicator}; none headless. */
public class VideoProgressRenderElement extends RenderElement {
    private Slider slider;
    private boolean syncing;
    private Funcs.VoidFunc0 listener;

    public VideoProgressRenderElement(VideoProgressIndicator widget) {
        super(widget);
    }

    private VideoProgressIndicator indicator() {
        return (VideoProgressIndicator) widget();
    }

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized() || indicator().controller == null) {
            return null;
        }
        slider = new Slider();
        slider.setMinValue(0);
        slider.setMaxValue(1000);
        slider.setEditable(indicator().allowScrubbing);
        if (indicator().colors != null && indicator().colors.playedColor != null) {
            slider.getSliderFullSelectedStyle().setBgColor(indicator().colors.playedColor.argb() & 0xffffff);
        }
        slider.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (syncing) {
                    return;
                }
                VideoPlayerValue v = indicator().controller.value();
                indicator().controller.seekTo(Duration.ofMicroseconds(v.durationMs * slider.getProgress()));
            }
        });
        listener = new Funcs.VoidFunc0() {
            @Override
            public void call() {
                sync();
            }
        };
        indicator().controller.addListener(listener);
        sync();
        return slider;
    }

    private void sync() {
        if (slider == null) {
            return;
        }
        VideoPlayerValue v = indicator().controller.value();
        syncing = true;
        slider.setProgress(v.durationMs <= 0 ? 0 : (int) (v.positionMs * 1000 / v.durationMs));
        syncing = false;
    }

    @Override
    public void unmount() {
        if (listener != null) {
            indicator().controller.removeListener(listener);
            listener = null;
        }
        super.unmount();
    }

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        double w = constraints.hasBoundedWidth() ? constraints.maxWidth() : Dp.px(300);
        return constraints.constrain(new Size(w, Dp.px(16)));
    }
}
