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
package com.codenameone.examples.bazaar.shop;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

/// Star ratings: a row of five to show one, and a row of five buttons to
/// pick one.
public final class Stars {
    private Stars() {
    }

    /// `rating` of five as stars, half stars from tenths.
    public static Container show(int ratingTenths, float size) {
        Container row = new Container(BoxLayout.x());
        row.setUIID("ShopStars");
        for (int iter = 0; iter < 5; iter++) {
            int left = ratingTenths - iter * 10;
            char icon = left >= 8 ? FontImage.MATERIAL_STAR
                    : left >= 3 ? FontImage.MATERIAL_STAR_HALF : FontImage.MATERIAL_STAR_BORDER;
            Label star = new Label("", "ShopStar");
            FontImage.setMaterialIcon(star, icon, size);
            row.add(star);
        }
        return row;
    }

    /// The listener hears the stars picked, 1 to 5.
    public interface Picked {
        void picked(int stars);
    }

    /// Five buttons; tapping the third picks three.
    public static Container pick(final int[] value, final Picked picked) {
        final Container row = new Container(BoxLayout.x());
        row.setUIID("ShopStars");
        for (int iter = 0; iter < 5; iter++) {
            final int stars = iter + 1;
            Button b = new Button("", "ShopStarPick");
            b.setName("star-" + stars);
            b.addActionListener(e -> {
                value[0] = stars;
                paint(row, stars);
                if (picked != null) {
                    picked.picked(stars);
                }
            });
            row.add(b);
        }
        paint(row, value[0]);
        return row;
    }

    private static void paint(Container row, int stars) {
        for (int iter = 0; iter < row.getComponentCount(); iter++) {
            FontImage.setMaterialIcon((Label) row.getComponentAt(iter), iter < stars
                    ? FontImage.MATERIAL_STAR : FontImage.MATERIAL_STAR_BORDER, 5f);
        }
    }
}
