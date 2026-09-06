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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.util.EasyThread;
import com.codename1.notifications.LocalNotification;
import com.codename1.ui.table.TableLayout;
import java.util.*;


class MiscellaneousFeaturesJava042Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    // tag::miscellaneous-features-java-042[]
    /// Every state's font as the theme installed it, so a second call scales
    /// the original rather than the font the first call derived. Without this a
    /// 1.3 preference applied twice gives 1.69, and turning larger text off
    /// never restores the original size.
    private final java.util.Map<Style, Font> originalFonts =
            new java.util.HashMap<Style, Font>();

    void applyLargerText(Component someComponent) {
        Display display = Display.getInstance();
        // scale 1 means "no scaling", and the helper hands the original back,
        // so this same path also undoes a previous scaling
        float scale = display.isLargerTextEnabled() ? display.getLargerTextScale() : 1f;
        applyLargerText(someComponent.getUnselectedStyle(), scale);
        applyLargerText(someComponent.getSelectedStyle(), scale);
        applyLargerText(someComponent.getPressedStyle(), scale);
        applyLargerText(someComponent.getDisabledStyle(), scale);
    }

    private void applyLargerText(Style style, float scale) {
        Font original = originalFonts.get(style);
        if (original == null) {
            original = style.getFont();
            originalFonts.put(style, original);
        }
        // each state keeps its own face: a bold selected font stays bold
        style.setFont(scaleForLargerText(original, scale));
    }

    /// Mirrors UIManager's own scaler: derive() handles only TrueType and
    /// native fonts, and takes a requested pixel size rather than the rendered
    /// line height getHeight() reports. Anything it cannot scale comes back
    /// unchanged rather than throwing.
    private Font scaleForLargerText(Font font, float scale) {
        if (font == null || !font.isTTFNativeFont() || scale <= 1f) {
            return font;
        }
        float baseSize = font.getPixelSize();
        if (baseSize <= 0) {
            baseSize = font.getHeight();
        }
        if (baseSize <= 0) {
            return font;
        }
        return font.derive(baseSize * scale, font.getStyle());
    }
    // end::miscellaneous-features-java-042[]

    Component someComponent = new Label();

}
