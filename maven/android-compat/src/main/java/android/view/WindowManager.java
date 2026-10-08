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
package android.view;

/// The window manager. Views added directly to it (overlays) are shown on the
/// current activity's root; this is enough for toasts and simple overlays.
public interface WindowManager extends ViewManager {

    Display getDefaultDisplay();

    class LayoutParams extends ViewGroup.LayoutParams {
        public static final int FLAG_ALLOW_LOCK_WHILE_SCREEN_ON = 0x00000001;
        public static final int FLAG_DIM_BEHIND = 0x00000002;
        public static final int FLAG_NOT_FOCUSABLE = 0x00000008;
        public static final int FLAG_NOT_TOUCHABLE = 0x00000010;
        public static final int FLAG_NOT_TOUCH_MODAL = 0x00000020;
        public static final int FLAG_KEEP_SCREEN_ON = 0x00000080;
        public static final int FLAG_LAYOUT_IN_SCREEN = 0x00000100;
        public static final int FLAG_LAYOUT_NO_LIMITS = 0x00000200;
        public static final int FLAG_FULLSCREEN = 0x00000400;
        public static final int FLAG_FORCE_NOT_FULLSCREEN = 0x00000800;
        public static final int FLAG_SECURE = 0x00002000;
        public static final int FLAG_SHOW_WHEN_LOCKED = 0x00080000;
        public static final int FLAG_TURN_SCREEN_ON = 0x00200000;
        public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;
        public static final int SOFT_INPUT_STATE_UNSPECIFIED = 0;
        public static final int SOFT_INPUT_STATE_UNCHANGED = 1;
        public static final int SOFT_INPUT_STATE_HIDDEN = 2;
        public static final int SOFT_INPUT_STATE_ALWAYS_HIDDEN = 3;
        public static final int SOFT_INPUT_STATE_VISIBLE = 4;
        public static final int SOFT_INPUT_STATE_ALWAYS_VISIBLE = 5;
        public static final int SOFT_INPUT_ADJUST_UNSPECIFIED = 0x00;
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x10;
        public static final int SOFT_INPUT_ADJUST_PAN = 0x20;
        public static final int SOFT_INPUT_ADJUST_NOTHING = 0x30;
        public static final int TYPE_APPLICATION = 2;
        public static final int TYPE_APPLICATION_PANEL = 1000;
        public static final int TYPE_APPLICATION_OVERLAY = 2038;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT = 0;
        public static final int LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES = 1;
        public static final float BRIGHTNESS_OVERRIDE_NONE = -1.0f;

        public int flags;
        public int type = TYPE_APPLICATION;
        public int softInputMode;
        public int gravity;
        public int x;
        public int y;
        public float dimAmount = 1.0f;
        public float alpha = 1.0f;
        public float screenBrightness = BRIGHTNESS_OVERRIDE_NONE;
        public int windowAnimations;
        public int format;
        public int layoutInDisplayCutoutMode;
        public CharSequence title;

        public LayoutParams() {
            super(MATCH_PARENT, MATCH_PARENT);
        }

        public LayoutParams(int type) {
            this();
            this.type = type;
        }

        public LayoutParams(int w, int h, int type, int flags, int format) {
            super(w, h);
            this.type = type;
            this.flags = flags;
            this.format = format;
        }

        public final void setTitle(CharSequence title) {
            this.title = title;
        }

        public final CharSequence getTitle() {
            return title;
        }
    }
}
