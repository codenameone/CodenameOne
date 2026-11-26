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
package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Form;
import com.codenameone.developerguide.presentation.PresentationSnippets;

import java.util.ArrayList;
import java.util.List;

/// The figures of the presentations chapter, each the form its snippet in
/// `PresentationSnippets` builds. Live charts animate in when their slide shows; a figure
/// waits that out before the picture is taken.
public final class PresentationFigures {
    private static final int SETTLE_MILLIS = 1500;

    private PresentationFigures() {
    }

    private interface Scene {
        Form build(PresentationSnippets s);
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;

        Fig(String id, Scene scene) {
            this.id = id;
            this.scene = scene;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            return scene.build(new PresentationSnippets());
        }

        @Override
        public void afterShow(Form form) {
            GuideFigure.settleAnimations(SETTLE_MILLIS);
            form.getContentPane().repaint();
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();
        v.add(fig("slides-shapes", s -> s.show(s.deck(), 0)));
        v.add(fig("slides-charts", s -> s.show(s.deck(), 1)));
        v.add(fig("slides-diagrams", s -> s.show(s.deck(), 2)));
        v.add(fig("slides-presenter", s -> s.presenter(s.deck())));
        v.add(fig("slides-editor", s -> s.editor(s.deck())));
        return v.toArray(new FigureVariant[v.size()]);
    }

    private static FigureVariant fig(String name, Scene scene) {
        return new FigureVariant(new Fig(name, scene), FigureDevice.ANDROID, false, name + ".png");
    }
}
