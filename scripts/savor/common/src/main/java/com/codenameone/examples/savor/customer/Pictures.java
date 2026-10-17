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
package com.codenameone.examples.savor.customer;

import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.util.Base64;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The photos of restaurants and dishes, fetched from the server once each.
///
/// Every photo is the server's: a merchant uploads it, and the seed data
/// carries its own. Nothing is loaded from anywhere else, so a photo is as
/// available as the menu it belongs to.
public final class Pictures {
    /// Photos kept decoded. A list of restaurants scrolls past a few dozen;
    /// more than that and the oldest goes.
    private static final int KEPT = 48;
    private static final Map<String, Image> DECODED = new HashMap<String, Image>();
    private static final List<String> ORDER = new ArrayList<String>();
    /// The labels waiting for a photo already asked for.
    private static final Map<String, List<Label>> WAITING = new HashMap<String, List<Label>>();

    private Pictures() {
    }

    /// Shows photo `id` on `label`, cropped to fill `width` by `height`
    /// pixels. The label keeps what it shows -- its style's background -- until
    /// the photo comes, and keeps it for good when there is none.
    public static void into(final Label label, String id, final int width, final int height) {
        if (id == null || id.length() == 0) {
            return;
        }
        Image known = DECODED.get(id);
        if (known != null) {
            label.setIcon(known.fill(width, height));
            return;
        }
        // The size goes with the label: the same photo is a banner on one
        // screen and a thumbnail on another.
        label.putClientProperty("savor.photo.size", new int[] {width, height});
        List<Label> waiting = WAITING.get(id);
        if (waiting != null) {
            waiting.add(label);
            return;
        }
        waiting = new ArrayList<Label>();
        waiting.add(label);
        WAITING.put(id, waiting);
        final String asked = id;
        Api.catalog().photo(id, Net.to(photo -> arrived(asked, photo),
                (status, message) -> WAITING.remove(asked)));
    }

    private static void arrived(String id, PhotoDto photo) {
        List<Label> waiting = WAITING.remove(id);
        Image image = decode(photo);
        if (image == null || waiting == null) {
            return;
        }
        DECODED.put(id, image);
        ORDER.add(id);
        if (ORDER.size() > KEPT) {
            DECODED.remove(ORDER.remove(0));
        }
        for (int iter = 0; iter < waiting.size(); iter++) {
            Label label = waiting.get(iter);
            Object size = label.getClientProperty("savor.photo.size");
            if (size instanceof int[]) {
                int[] wanted = (int[]) size;
                label.setIcon(image.fill(wanted[0], wanted[1]));
                label.repaint();
            }
        }
    }

    private static Image decode(PhotoDto photo) {
        if (photo == null || photo.data() == null || photo.data().length() == 0) {
            return null;
        }
        try {
            byte[] data = Base64.decode(photo.data().getBytes("UTF-8"));
            return data == null ? null : Image.createImage(data, 0, data.length);
        } catch (UnsupportedEncodingException | RuntimeException unreadable) {
            // A photo that does not decode is a photo not shown, not a screen
            // that fails.
            return null;
        }
    }
}
