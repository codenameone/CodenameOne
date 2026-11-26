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

import com.codename1.presentation.Presentation;
import com.codename1.presentation.SlideShow;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
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

    /// What a figure does with the form once it is shown.
    private enum Kind {
        /// A slide played to the end of its builds and cropped to the slide's own shape.
        SLIDE,
        /// Presenter mode: the slide, the next one and the notes, cropped to a wide shape.
        PRESENTER,
        /// The editor, which fills whatever it is given.
        EDITOR
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;
        private final Kind kind;

        Fig(String id, Scene scene, Kind kind) {
            this.id = id;
            this.scene = scene;
            this.kind = kind;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            Form f = scene.build(new PresentationSnippets());
            if (kind != Kind.EDITOR) {
                // A slide show letterboxes the slide in black inside whatever it is given;
                // photographed in a portrait phone that was two black bands taller than
                // the slide. Ask for the slide's own shape so the crop stops at its edge.
                Component show = f.getContentPane().getComponentAt(0);
                int w = FigureDevice.ANDROID.width();
                float wide = kind == Kind.PRESENTER ? 0.64f : 1f;
                show.setPreferredSize(new Dimension(w, Math.round(w * wide
                        * Presentation.WIDE_HEIGHT / Presentation.WIDE_WIDTH)
                        + (kind == Kind.PRESENTER ? w / 3 : 0)));
            }
            return f;
        }

        @Override
        public void afterShow(Form form) {
            if (kind != Kind.EDITOR) {
                // Entrance builds wait for a click, and a figure captioned with the
                // slide's shapes must show them: play every build of the slide. next()
                // moves on to the following slide once the builds are done, and
                // previous() comes back to this one fully built, with no transition.
                SlideShow show = (SlideShow) form.getContentPane().getComponentAt(0);
                int at = show.getSlideIndex();
                while (show.next() && show.getSlideIndex() == at) {
                    // one build per call
                }
                if (show.getSlideIndex() != at) {
                    show.previous();
                }
            }
            GuideFigure.settleAnimations(SETTLE_MILLIS);
            form.getContentPane().repaint();
        }

        @Override
        public boolean fillsViewport() {
            return kind == Kind.EDITOR;
        }
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();
        v.add(fig("slides-shapes", s -> at(s.show(s.deck()), 0), Kind.SLIDE));
        v.add(fig("slides-charts", s -> at(s.show(s.deck()), 1), Kind.SLIDE));
        v.add(fig("slides-diagrams", s -> at(s.show(s.deck()), 2), Kind.SLIDE));
        v.add(fig("slides-presenter", s -> s.presenter(s.deck()), Kind.PRESENTER));
        v.add(fig("slides-editor", s -> s.editor(s.deck()), Kind.EDITOR));
        return v.toArray(new FigureVariant[v.size()]);
    }

    /// The show on a form, moved to a slide with no transition.
    private static Form at(Form form, int slide) {
        ((SlideShow) form.getContentPane().getComponentAt(0)).goTo(slide);
        return form;
    }

    private static FigureVariant fig(String name, Scene scene, Kind kind) {
        return new FigureVariant(new Fig(name, scene, kind), FigureDevice.ANDROID, false, name + ".png");
    }
}
