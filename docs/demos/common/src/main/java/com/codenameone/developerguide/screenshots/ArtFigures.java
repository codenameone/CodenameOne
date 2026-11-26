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
import com.codenameone.developerguide.art.ArtSnippets;

import java.io.IOException;

/// The figures of the graphics documents chapter. Each is what a sample of
/// `ArtSnippets` builds; the renderer is pure software, so every run draws
/// the same pixels.
public final class ArtFigures {
    private ArtFigures() {
    }

    private interface Scene {
        Form build() throws IOException;
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
            try {
                return scene.build();
            } catch (IOException err) {
                throw new IllegalStateException(err.toString());
            }
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        return new FigureVariant[]{
            new FigureVariant(new Fig("art-load", ArtSnippets::load), FigureDevice.ANDROID, false, "art-load.png"),
            new FigureVariant(new Fig("art-effects", ArtSnippets::effects), FigureDevice.ANDROID, false, true,
                    "art-effects.png"),
            new FigureVariant(new Fig("art-editor", ArtSnippets::editor), FigureDevice.ANDROID, false, true,
                    "art-editor.png"),
        };
    }
}
