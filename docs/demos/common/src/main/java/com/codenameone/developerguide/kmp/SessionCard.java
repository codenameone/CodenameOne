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
package com.codenameone.developerguide.kmp;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

// tag::kotlin-multiplatform-interop-session-card[]
public class SessionCard extends Container {
    public final Label time = new Label("", "SessionTime");
    public final Label title = new Label("", "SessionTitle");
    public final Label speaker = new Label("", "SessionSpeaker");
    public final Button heart = new Button("", "Favorite");

    public SessionCard(Runnable onFavorite) {
        super(new BorderLayout(), "SessionCard");
        add(BorderLayout.CENTER, BoxLayout.encloseY(time, title, speaker));
        add(BorderLayout.EAST, heart);
        heart.addActionListener(e -> onFavorite.run());
    }

    public void bind(String when, String talk, String by, boolean favorite) {
        time.setText(when);
        title.setText(talk);
        speaker.setText(by);
        FontImage.setMaterialIcon(heart, favorite
                ? FontImage.MATERIAL_FAVORITE : FontImage.MATERIAL_FAVORITE_BORDER);
    }
}
// end::kotlin-multiplatform-interop-session-card[]
