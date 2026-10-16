/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Icons;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.TargetPlatform;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Icon;

/**
 * The platform-appropriate back-arrow glyph, decoupled from its button —
 * Flutter's {@code BackButtonIcon}: a chevron on iOS/macOS, an arrow elsewhere.
 *
 * <p>It rendered NOTHING until now, which is a costlier omission than it sounds: the
 * gallery builds every demo page's back button as {@code IconButton(icon: BackButtonIcon())},
 * so each of those pages had an invisible — though still tappable — way back.</p>
 */
public class BackButtonIcon extends StatelessWidget
        implements com.codename1.flutter.widgets.HasIcon {

    /**
     * The glyph, for callers that want it without building — see
     * {@link com.codename1.flutter.widgets.HasIcon}.
     *
     * <p>There is no context here to read the ambient theme's platform from, so this asks
     * the one the app is RUNNING on, which is what {@code ThemeData.platform} defaults to
     * anyway. It used to answer the material arrow unconditionally, and the shortcut is
     * the path an extended FloatingActionButton takes for its icon -- so the gallery's
     * "Back to gallery" button wore a long arrow on iOS where the reference wears the
     * chevron. The glyph was not merely a different shape; it was the wrong platform's.</p>
     */
    @Override
    public com.codename1.flutter.IconData iconData() {
        return isApplePlatform() ? Icons.arrow_back_ios : Icons.arrow_back;
    }

    /** Whether the platform the app is running on uses the chevron. */
    private static boolean isApplePlatform() {
        try {
            TargetPlatform p = com.codename1.flutter.foundation.FoundationLib
                    .defaultTargetPlatform;
            return p == TargetPlatform.iOS || p == TargetPlatform.macOS;
        } catch (Throwable noPlatform) {
            return false;
        }
    }


    @Override
    public Widget build(BuildContext context) {
        return new Icon(isApplePlatform(context) ? Icons.arrow_back_ios : Icons.arrow_back);
    }

    /** Whether the ambient theme targets a platform that uses the chevron. */
    private static boolean isApplePlatform(BuildContext context) {
        try {
            Object p = Theme.of(context).platform();
            return p == TargetPlatform.iOS || p == TargetPlatform.macOS;
        } catch (Throwable t) {
            return false;
        }
    }
}
