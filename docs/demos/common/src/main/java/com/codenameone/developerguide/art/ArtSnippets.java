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
package com.codenameone.developerguide.art;

import com.codename1.art.ArtDocument;
import com.codename1.art.ArtEditor;
import com.codename1.art.ArtFormats;
import com.codename1.art.ArtModel;
import com.codename1.art.ArtRenderer;
import com.codename1.art.ArtView;
import com.codename1.art.BlendMode;
import com.codename1.art.FigmaClient;
import com.codename1.art.GradientPaint;
import com.codename1.art.ImportNote;
import com.codename1.art.Matrix;
import com.codename1.art.ShapeNode;
import com.codename1.art.SolidPaint;
import com.codename1.art.TextNode;
import com.codename1.io.Log;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/// The samples of the graphics documents chapter. Each builds what the figure
/// of the same name shows, so the pictures are drawn by the code the chapter
/// prints.
public final class ArtSnippets {
    private ArtSnippets() {
    }

    /// Reads an SVG that arrives at run time and shows it as an image.
    public static Form load() throws IOException {
        // tag::art-load[]
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='160' height='120'>"
                + "<defs><linearGradient id='sky' x2='0' y2='1'>"
                + "<stop offset='0' stop-color='#4fc3f7'/><stop offset='1' stop-color='#e1f5fe'/>"
                + "</linearGradient></defs>"
                + "<rect width='160' height='120' fill='url(#sky)'/>"
                + "<circle cx='120' cy='34' r='16' fill='#ffca28'/>"
                + "<path d='M0 120 L50 60 L85 95 L110 70 L160 120 Z' fill='#43a047'/>"
                + "</svg>";
        InputStream in = new ByteArrayInputStream(svg.getBytes("UTF-8"));
        ArtDocument doc = ArtFormats.read(in);      // SVG, PSD, Sketch, Figma or .cn1gfx
        Image image = ArtRenderer.toImage(doc, 2);  // two pixels per document unit
        Form form = new Form("Runtime SVG", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        form.add(BorderLayout.CENTER, new Label(image));
        form.show();
        // end::art-load[]
        return form;
    }

    /// Builds a document in code with effects and blend modes and shows it.
    public static Form effects() {
        // tag::art-effects[]
        ArtDocument doc = new ArtDocument(320, 200);
        ShapeNode card = ShapeNode.rect(20, 20, 130, 90, 14);
        card.fill(GradientPaint.linear(20, 20, 150, 110,
                new float[]{0, 1}, new int[]{0xff7e57c2, 0xff26c6da}));
        card.getEffects().add(com.codename1.art.effects.Effects.dropShadow(0x66000000, 6, 8, 10, 0));
        ShapeNode badge = ShapeNode.ellipse(235, 65, 50, 50);
        badge.fill(new SolidPaint(0xffffb300));
        badge.getEffects().add(com.codename1.art.effects.Effects.bevel("inner", 6, 1, 120, 30));
        badge.getEffects().add(com.codename1.art.effects.Effects.outerGlow(0xccffd54f, 12, 0));
        ShapeNode overlap = ShapeNode.rect(110, 90, 150, 80, 0);
        overlap.fill(new SolidPaint(0xffef5350));
        overlap.setBlendMode(BlendMode.MULTIPLY);   // darkens what lies below it
        TextNode label = new TextNode("Layers", 22, 0xff212121);
        label.setTransform(Matrix.translate(30, 180));
        doc.getRoot().getChildren().add(card);
        doc.getRoot().getChildren().add(badge);
        doc.getRoot().getChildren().add(overlap);
        doc.getRoot().getChildren().add(label);

        Form form = new Form("Effects", new BorderLayout());
        form.add(BorderLayout.CENTER, new ArtView(new ArtModel(doc)));
        form.show();
        // end::art-effects[]
        return form;
    }

    /// Lists what a reader could not keep.
    public static void notes() throws IOException {
        // tag::art-notes[]
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='40' height='40'>"
                + "<rect width='40' height='40' fill='#1e88e5'>"
                + "<animate attributeName='opacity' from='1' to='0' dur='1s'/></rect></svg>";
        ArtDocument doc = ArtFormats.read(svg.getBytes("UTF-8"));
        for (ImportNote note : doc.getNotes()) {
            Log.p("Import: " + note);
        }
        // end::art-notes[]
    }

    /// Fetches a Figma file with a token the user supplies.
    public static void figma() {
        // tag::art-figma[]
        String token = "PERSONAL_ACCESS_TOKEN";              // typed by the user
        Display.getInstance().startThread(() -> {
            try {
                FigmaClient figma = new FigmaClient(token);  // never stored
                ArtDocument doc = figma.fetch("FILE_KEY");    // blocks: off the EDT
                Display.getInstance().callSerially(() -> {
                    Form form = new Form("Figma", new BorderLayout());
                    form.add(BorderLayout.CENTER, new ArtView(new ArtModel(doc)));
                    form.show();
                });
            } catch (IOException err) {
                Log.e(err);
            }
        }, "figma").start();
        // end::art-figma[]
    }

    /// Edits a document through the undoable model.
    public static ArtModel editing() {
        // tag::art-editing[]
        ArtModel model = new ArtModel(new ArtDocument(400, 300));
        ShapeNode box = ShapeNode.rect(10, 10, 100, 60, 8);
        box.fill(new SolidPaint(0xff1e88e5));
        model.add(box);
        model.setBlendMode(box, BlendMode.MULTIPLY);
        model.addEffect(box, com.codename1.art.effects.Effects.dropShadow(0x80000000, 4, 4, 8, 0));
        model.undo();                       // the shadow is gone again
        model.beginGroup();                 // several changes, one undo step
        model.setOpacity(box, 0.5f);
        model.setName(box, "Half box");
        model.endGroup();
        // end::art-editing[]
        return model;
    }

    /// Shows the ready-made editor.
    public static Form editor() {
        // tag::art-editor[]
        ArtModel model = new ArtModel(new ArtDocument(400, 300));
        ShapeNode sun = ShapeNode.ellipse(300, 80, 40, 40);
        sun.fill(new SolidPaint(0xffffca28));
        model.add(sun);
        ShapeNode hill = ShapeNode.rect(0, 200, 400, 100, 0);
        hill.fill(new SolidPaint(0xff66bb6a));
        model.add(hill);
        model.select(sun, false);

        ArtEditor editor = new ArtEditor(model);
        Form form = new Form("Draw", new BorderLayout());
        form.add(BorderLayout.CENTER, editor);
        form.show();
        // end::art-editor[]
        return form;
    }
}
