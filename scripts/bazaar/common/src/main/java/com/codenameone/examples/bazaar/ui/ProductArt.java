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
package com.codenameone.examples.bazaar.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.util.Base64;
import com.codenameone.examples.bazaar.net.Api;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The picture of a product.
///
/// Sample products have no photos, and a grid of grey boxes looks broken, so
/// a product without one is drawn: a gradient whose colours come from its id,
/// a soft disc and the icon of its kind (a mug, a shirt, a book). The
/// same product is the same picture everywhere ([ArtPalette]
/// makes it deterministic). A product with a photo shows the art first and
/// the photo as soon as it has been fetched.
///
/// Both are cached, bounded, by id, size and light or dark.
public final class ProductArt {
    private static final int MOST = 160;
    private static final Map<String, Image> CACHE = new HashMap<String, Image>();
    private static final List<String> ORDER = new ArrayList<String>();
    /// Photos being fetched, and the labels waiting for each.
    private static final Map<String, List<Label>> WAITING = new HashMap<String, List<Label>>();
    private static final Map<String, Image> PHOTOS = new HashMap<String, Image>();
    private static final List<String> PHOTO_ORDER = new ArrayList<String>();
    /// Downloaded photos, by photo id, each size scaled from one of these.
    private static final Map<String, EncodedImage> SOURCES = new HashMap<String, EncodedImage>();
    private static final List<String> SOURCE_ORDER = new ArrayList<String>();

    private static final char[] ICONS = {
        FontImage.MATERIAL_LOCAL_CAFE, FontImage.MATERIAL_LOCAL_CAFE,
        FontImage.MATERIAL_EMOJI_OBJECTS, FontImage.MATERIAL_RESTAURANT,
        FontImage.MATERIAL_RESTAURANT, FontImage.MATERIAL_LOCAL_FLORIST,
        FontImage.MATERIAL_CHECKROOM, FontImage.MATERIAL_CHECKROOM,
        FontImage.MATERIAL_CHECKROOM, FontImage.MATERIAL_CHECKROOM,
        FontImage.MATERIAL_CHECKROOM, FontImage.MATERIAL_CHECKROOM,
        FontImage.MATERIAL_HIKING, FontImage.MATERIAL_HIKING,
        FontImage.MATERIAL_SHOPPING_BAG, FontImage.MATERIAL_MENU_BOOK,
        FontImage.MATERIAL_COFFEE, FontImage.MATERIAL_EMOJI_FOOD_BEVERAGE,
        FontImage.MATERIAL_RESTAURANT, FontImage.MATERIAL_WB_INCANDESCENT,
        FontImage.MATERIAL_LIGHTBULB, FontImage.MATERIAL_LIGHTBULB,
        FontImage.MATERIAL_YARD, FontImage.MATERIAL_TOYS,
        FontImage.MATERIAL_SPORTS_ESPORTS, FontImage.MATERIAL_WATCH,
        FontImage.MATERIAL_DIAMOND, FontImage.MATERIAL_DIAMOND,
        FontImage.MATERIAL_SMARTPHONE, FontImage.MATERIAL_HEADPHONES,
        FontImage.MATERIAL_HEADPHONES, FontImage.MATERIAL_PHOTO_CAMERA,
        FontImage.MATERIAL_PALETTE, FontImage.MATERIAL_PALETTE,
        FontImage.MATERIAL_CARD_GIFTCARD, FontImage.MATERIAL_HOME,
        FontImage.MATERIAL_KITCHEN, FontImage.MATERIAL_SPA,
        FontImage.MATERIAL_SPA, FontImage.MATERIAL_SPORTS_SOCCER,
        FontImage.MATERIAL_DIRECTIONS_BIKE, FontImage.MATERIAL_PETS
    };

    private ProductArt() {
    }

    /// The icon a product of `type` is drawn with.
    public static char icon(String type, String title) {
        int word = ArtPalette.typeWord(type, title);
        return word >= 0 && word < ICONS.length ? ICONS[word] : FontImage.MATERIAL_CATEGORY;
    }

    /// A `type` for [#art] that draws no glyph: for art that text is laid
    /// over, such as the storefront's hero.
    public static final String NO_GLYPH = "-no-glyph-";

    /// The drawn picture of a product, `width` by `height` pixels.
    public static Image art(String id, String title, String type, int width, int height) {
        int w = Math.max(8, width);
        int h = Math.max(8, height);
        boolean dark = Look.dark();
        String key = id + ":" + w + "x" + h + (dark ? ":d" : ":l");
        Image cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int[] colors = ArtPalette.colors(id == null ? title : id, dark);
        Image image = Image.createImage(w, h, 0xff000000 | colors[0]);
        Graphics g = image.getGraphics();
        g.setAntiAliased(true);
        g.fillLinearGradient(colors[0], colors[1], 0, 0, w, h, false);
        // A soft disc off one corner, so that a grid of one hue still reads as
        // different things.
        int corner = ArtPalette.corner(id);
        int d = Math.max(w, h);
        int dx = corner % 2 == 0 ? -d / 3 : w - d * 2 / 3;
        int dy = corner < 2 ? -d / 3 : h - d * 2 / 3;
        g.setColor(0xffffff);
        g.setAlpha(dark ? 22 : 46);
        g.fillArc(dx, dy, d, d, 0, 360);
        g.setAlpha(255);

        if (!NO_GLYPH.equals(type)) {
            int side = Math.min(w, h);
            Font material = FontImage.getMaterialDesignFont().derive(side * 0.42f, Font.STYLE_PLAIN);
            String glyph = String.valueOf(icon(type, title));
            g.setFont(material);
            g.setColor(0xffffff);
            g.setAlpha(dark ? 150 : 200);
            g.drawString(glyph, (w - material.stringWidth(glyph)) / 2,
                    (h - material.getHeight()) / 2 - side / 14);
        }

        // No initials: cards lay their caption over the bottom of the art,
        // and letters there read as stray text under the scrim.
        g.setAlpha(255);
        remember(CACHE, ORDER, key, image);
        return image;
    }

    /// A label showing a product: its photo when `photoId` is set (the art
    /// until the photo arrives), its art otherwise.
    public static Label label(String id, String title, String type, String photoId,
            int width, int height, String uiid) {
        Label label = new Label("", uiid);
        label.setIcon(art(id, title, type, width, height));
        label.putClientProperty("artSize", new int[] {width, height});
        if (photoId != null && photoId.length() > 0) {
            photo(label, photoId, width, height);
        }
        return label;
    }

    /// Puts the photo `photoId` on `label`, fetched once and cached.
    ///
    /// The download is cached by photo, not by size: a desktop register
    /// lays its grid out again as the window settles or is resized, each
    /// pass at another tile size, and keying the fetch by size downloaded
    /// every photo once per pass -- the tiles sat on their drawn art for
    /// most of a minute. Each size is scaled from the one download.
    public static void photo(Label label, final String photoId, final int width,
            final int height) {
        final String key = photoId + ":" + width + "x" + height;
        Image ready = PHOTOS.get(key);
        if (ready != null) {
            label.setIcon(ready);
            return;
        }
        EncodedImage source = SOURCES.get(photoId);
        if (source != null) {
            label.setIcon(sized(source, key, width, height));
            return;
        }
        label.putClientProperty("photoSize", new int[] {width, height});
        List<Label> waiting = WAITING.get(photoId);
        if (waiting != null) {
            if (!waiting.contains(label)) {
                waiting.add(label);
            }
            return;
        }
        waiting = new ArrayList<Label>();
        waiting.add(label);
        WAITING.put(photoId, waiting);
        Api.catalog().photo(photoId, Net.to(photo -> {
            List<Label> told = WAITING.remove(photoId);
            if (photo == null || photo.data() == null) {
                return;
            }
            try {
                byte[] bytes = Base64.decode(photo.data().getBytes("UTF-8"));
                EncodedImage decoded = EncodedImage.create(bytes);
                SOURCES.put(photoId, decoded);
                SOURCE_ORDER.add(photoId);
                while (SOURCE_ORDER.size() > MOST) {
                    SOURCES.remove(SOURCE_ORDER.remove(0));
                }
                for (int iter = 0; told != null && iter < told.size(); iter++) {
                    Label l = told.get(iter);
                    Object size = l.getClientProperty("photoSize");
                    int w = size instanceof int[] ? ((int[]) size)[0] : width;
                    int h = size instanceof int[] ? ((int[]) size)[1] : height;
                    l.setIcon(sized(decoded, photoId + ":" + w + "x" + h, w, h));
                    l.repaint();
                }
            } catch (java.io.IOException err) {
                // Not an image after all: the drawn art stays.
            }
        }, (status, message) -> WAITING.remove(photoId)));
    }

    private static Image sized(EncodedImage source, String key, int width, int height) {
        Image ready = PHOTOS.get(key);
        if (ready == null) {
            ready = source.fill(width, height);
            remember(PHOTOS, PHOTO_ORDER, key, ready);
        }
        return ready;
    }

    /// Forgets every drawn picture: after a change between light and dark.
    public static void clear() {
        CACHE.clear();
        ORDER.clear();
    }

    private static void remember(Map<String, Image> cache, List<String> order, String key,
            Image image) {
        cache.put(key, image);
        order.add(key);
        while (order.size() > MOST) {
            cache.remove(order.remove(0));
        }
    }
}
