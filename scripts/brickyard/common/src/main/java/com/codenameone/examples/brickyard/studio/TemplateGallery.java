/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.studio;

import com.codename1.rbx.PlaceThumbnail;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.rbx.place.Templates;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.TextField;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.brickyard.ui.Ui;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// Where a new experience starts: every template as a card with its place
/// drawn, a line on what it plays like, and a name to give it.
final class TemplateGallery {
    private static final Map<String, Image> PICTURES = new HashMap<String, Image>();

    private TemplateGallery() {
    }

    static void show(final Form previous) {
        final Form f = Ui.form("Start something new", "templateGallery");
        Ui.back(f, () -> {
            if (previous != null) {
                previous.showBack();
            }
        });
        f.add(Ui.text("Pick a template. Every part and script in it is yours to change, and Play tests it "
                + "on the spot.", "Body"));
        Container grid = new Container(new GridLayout(2));
        for (final String id : Templates.ids()) {
            // The title is the card's button: a real, visible hit area, so a
            // screen reader or a test that activates it taps this card and
            // never the gap between two.
            Button pick = new Button(title(id), "CardTitle");
            pick.setName("template-" + id);
            pick.setAccessibilityText(title(id) + " template: " + blurb(id));
            pick.addActionListener(ev -> name(id, previous));
            Container card = Ui.panel(new Picture(id), pick, Ui.text(blurb(id), "CardMeta"));
            card.setLeadComponent(pick);
            grid.add(card);
        }
        f.add(grid);
        f.show();
    }

    private static void name(final String id, final Form previous) {
        final TextField t = new TextField("My " + title(id), "Name", 30, TextField.ANY);
        t.setUIID("StudioField");
        t.setName("experienceTitle");
        final Dialog d = Studio.darkDialog("Name your experience");
        Container buttons = new Container(new GridLayout(1, 2));
        Button cancel = new Button("Cancel", "StudioDialogButton");
        cancel.addActionListener(ev -> d.dispose());
        Button go = new Button("Create", "StudioPublish");
        go.setName("confirmCreate");
        go.addActionListener(ev -> {
            d.dispose();
            String name = t.getText().trim();
            Studio.create(id, name.length() == 0 ? "My " + title(id) : name, previous);
        });
        buttons.add(cancel).add(go);
        d.add(BorderLayout.CENTER, BoxLayout.encloseY(Ui.label("You can rename it when you publish.",
                "StudioPropName"), t));
        d.add(BorderLayout.SOUTH, buttons);
        d.show();
    }

    static String title(String id) {
        if ("baseplate".equals(id)) {
            return "Baseplate";
        }
        if ("obby".equals(id)) {
            return "Obby";
        }
        if ("tycoon".equals(id)) {
            return "Tycoon";
        }
        if ("arena".equals(id)) {
            return "Arena";
        }
        if ("racing".equals(id)) {
            return "Racing";
        }
        if ("simulator".equals(id)) {
            return "Simulator";
        }
        return id;
    }

    /// The genre an experience made from `id` is listed under.
    static String genre(String id) {
        if ("baseplate".equals(id)) {
            return "Adventure";
        }
        return "arena".equals(id) ? "Fighting" : title(id);
    }

    static String blurb(String id) {
        if ("baseplate".equals(id)) {
            return "An empty world. Build your own game from the first brick.";
        }
        if ("obby".equals(id)) {
            return "Islands over the bay, a spinner and a trophy at the top.";
        }
        if ("tycoon".equals(id)) {
            return "Droppers, conveyors and upgrades that pay out.";
        }
        if ("arena".equals(id)) {
            return "Pillars, pickups and a round timer for PvP.";
        }
        if ("racing".equals(id)) {
            return "A loop track with laps and a finish line.";
        }
        if ("simulator".equals(id)) {
            return "Mine, sell, upgrade, repeat.";
        }
        return "";
    }

    /// The template's place drawn from above, rendered once per run.
    private static final class Picture extends Component {
        private final String id;

        Picture(String id) {
            this.id = id;
        }

        @Override
        protected void initComponent() {
            super.initComponent();
            if (PICTURES.containsKey(id)) {
                return;
            }
            final Dimension d = calcPreferredSize();
            CN.callSerially(() -> {
                try {
                    // The cover captured from the real 3D view when the
                    // template ships one; the top-down sketch otherwise.
                    byte[] cover = Templates.picture(id);
                    if (cover.length > 0) {
                        PICTURES.put(id, EncodedImage.create(cover));
                        repaint();
                        return;
                    }
                    PICTURES.put(id, PlaceThumbnail.render(PlaceFile.read(Templates.bytes(id)),
                            d.getWidth() * 2, d.getHeight() * 2));
                } catch (IOException err) {
                    PICTURES.put(id, null);
                }
                repaint();
            });
        }

        @Override
        public void paint(Graphics g) {
            Image img = PICTURES.get(id);
            if (img != null) {
                g.drawImage(img, getX(), getY(), getWidth(), getHeight());
            } else {
                g.setColor(0x222a45);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
            }
        }

        @Override
        protected Dimension calcPreferredSize() {
            int w = CN.getDisplayWidth() / 2 - CN.convertToPixels(8);
            return new Dimension(w, w * 9 / 16);
        }
    }
}
