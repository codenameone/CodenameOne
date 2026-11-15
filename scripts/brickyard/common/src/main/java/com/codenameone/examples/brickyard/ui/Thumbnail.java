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
package com.codenameone.examples.brickyard.ui;

import com.codename1.io.Storage;
import com.codename1.rbx.PlaceThumbnail;
import com.codename1.rbx.place.PlaceFile;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.ImageIO;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// An experience's picture: its published place, drawn from above by
/// [PlaceThumbnail]. The render is kept in storage per published version, so
/// each version is drawn once per device. Until it is ready, and for an
/// experience that has never been published, a little skyline of blocks
/// whose colours come from the id stands in.
public class Thumbnail extends Component {
    private static final int[] COLORS = {0xf0563c, 0xffc43c, 0x3caaff, 0x5ac86e, 0x8a5cf0, 0xff8fc8};
    /// Renders already made this run, by storage key.
    private static final Map<String, Image> CACHE = new HashMap<String, Image>();
    private final int seed;
    private final float widthMm;
    private final String genre;
    private Image picture;

    public Thumbnail(ExperienceDto e, float widthMm) {
        this.seed = e.id() == null ? 0 : e.id().hashCode();
        this.widthMm = widthMm;
        this.genre = e.genre() == null || "ALL".equals(e.genre()) ? null : e.genre();
        if (e.publishedVersion() > 0) {
            load(e.id() + "-" + e.publishedVersion(), e.id());
        }
    }

    private void load(final String version, String id) {
        // The renderer revision is part of the key: a better renderer redraws.
        final String key = "thumb6-" + version + ".png";
        Image cached = CACHE.get(key);
        if (cached == null && Storage.getInstance().exists(key)) {
            try (InputStream in = Storage.getInstance().createInputStream(key)) {
                cached = EncodedImage.create(in);
                CACHE.put(key, cached);
            } catch (IOException err) {
                Storage.getInstance().deleteStorageFile(key);
                cached = null;
            }
        }
        if (cached != null) {
            picture = cached;
            return;
        }
        // The creator's capture from the real renderer first; a place with
        // none is drawn here from its parts.
        Api.experiences().thumbnail(id, Net.to(t -> {
            byte[] jpeg = t == null || t.data() == null || t.data().length() == 0 ? null
                    : com.codename1.util.Base64.decode(t.data().getBytes());
            if (jpeg == null || jpeg.length == 0) {
                drawn(key, id);
                return;
            }
            Image img = EncodedImage.create(jpeg);
            CACHE.put(key, img);
            picture = img;
            repaint();
            try (OutputStream out = Storage.getInstance().createOutputStream(key)) {
                out.write(jpeg);
            } catch (IOException err) {
                Storage.getInstance().deleteStorageFile(key);
            }
        }, (status, message) -> drawn(key, id)));
    }

    private void drawn(final String key, String id) {
        Api.experiences().place(id, Net.to(place -> {
            byte[] bytes = ExperienceForm.decode(place == null ? null : place.data());
            if (bytes == null) {
                return;
            }
            Dimension d = size();
            Image img = PlaceThumbnail.render(PlaceFile.read(bytes), d.getWidth() * 2, d.getHeight() * 2);
            CACHE.put(key, img);
            picture = img;
            repaint();
            save(key, img);
        }, (status, message) -> { }));
    }

    private static void save(String key, Image img) {
        ImageIO io = ImageIO.getImageIO();
        if (io == null) {
            return;
        }
        try (OutputStream out = Storage.getInstance().createOutputStream(key)) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            io.save(img, b, ImageIO.FORMAT_PNG, 1);
            out.write(b.toByteArray());
        } catch (IOException err) {
            // only a cache: the next run renders it again
            Storage.getInstance().deleteStorageFile(key);
        }
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (picture != null) {
            g.drawImage(picture, getX(), getY(), w, h);
            badge(g, w, h);
            return;
        }
        g.setColor(0x222a45);
        g.fillRect(getX(), getY(), w, h);
        int columns = 6;
        int cw = Math.max(1, w / columns);
        int s = seed;
        for (int i = 0; i < columns; i++) {
            s = s * 1103515245 + 12345;
            int rows = 1 + ((s >>> 16) & 3);
            int bh = Math.max(1, h / 6);
            for (int r = 0; r < rows; r++) {
                g.setColor(COLORS[(((s >>> 8) + r) & 0x7fffffff) % COLORS.length]);
                g.fillRect(getX() + i * cw + 1, getY() + h - (r + 1) * bh, cw - 2, bh - 1);
            }
        }
    }

    /// The genre as a pill in the picture's corner, over a soft shade so it
    /// reads on any sky.
    private void badge(Graphics g, int w, int h) {
        if (genre == null) {
            return;
        }
        com.codename1.ui.Font font = com.codename1.ui.Font.createTrueTypeFont("native:MainBold")
                .derive(Math.max(8, h / 9f), com.codename1.ui.Font.STYLE_BOLD);
        String text = genre.toUpperCase();
        int pad = Math.max(3, h / 30);
        int tw = font.stringWidth(text) + pad * 2;
        int th = font.getHeight() + pad;
        int x = getX() + pad * 2;
        int y = getY() + pad * 2;
        g.setAntiAliased(true);
        g.setColor(0x161b2e);
        g.setAlpha(190);
        g.fillRoundRect(x, y, tw, th, th, th);
        g.setAlpha(255);
        g.setColor(0xffc400);
        g.setFont(font);
        g.drawString(text, x + pad, y + pad / 2);
    }

    private Dimension size() {
        int w = CN.convertToPixels(widthMm);
        return new Dimension(w, w * 9 / 16);
    }

    @Override
    protected Dimension calcPreferredSize() {
        return size();
    }
}
