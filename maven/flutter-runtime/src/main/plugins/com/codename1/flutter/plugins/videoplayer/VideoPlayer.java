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

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.Size;

/** video_player's VideoPlayer: hosts the controller's Media video component. */
public class VideoPlayer extends Widget {
    final VideoPlayerController controller;

    public VideoPlayer(VideoPlayerController controller) {
        this.controller = controller;
    }

    Size preferredLayoutSize() {
        VideoPlayerValue v = controller == null ? null : controller.value();
        if (v == null || v.width <= 0 || v.height <= 0) {
            return new Size(320, 180);
        }
        return new Size(v.width, v.height);
    }

    @Override
    public Element createElement() {
        return new VideoPlayerRenderElement(this);
    }
}
