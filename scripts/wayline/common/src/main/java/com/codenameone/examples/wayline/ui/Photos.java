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
package com.codenameone.examples.wayline.ui;

import com.codename1.capture.Capture;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.util.ImageIO;
import com.codenameone.examples.wayline.Telemetry;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// A picture from the camera or from the gallery, made small enough to send.
///
/// A phone's camera makes pictures of several megabytes, and the server takes
/// documents of up to one and a half. So whatever comes back is scaled to fit
/// in [#LONGEST_SIDE] and written as a JPEG, which leaves a page of a licence
/// perfectly readable in a fraction of that.
public final class Photos {
    /// Where the pictures come from, for a test that has no camera.
    public interface Source {
        /// @param camera true when the camera was asked for, false the gallery
        /// @return the picture, or null as when the user backs out
        Image take(boolean camera);
    }

    public interface Taken {
        /// @param jpeg the picture, encoded
        /// @param picture the same picture, to show
        void taken(byte[] jpeg, Image picture);
    }

    private static final int LONGEST_SIDE = 1280;
    private static final float QUALITY = 0.8f;

    private static Source source;

    private Photos() {
    }

    /// Takes every picture from `from` from now on, and opens neither the
    /// camera nor the gallery. The tests use it to hand in a picture of their
    /// own.
    public static void useSource(Source from) {
        source = from;
    }

    public static void camera(Taken taken) {
        if (source != null) {
            deliver(source.take(true), taken);
            return;
        }
        Capture.capturePhoto(e -> picked(e, taken));
    }

    public static void gallery(Taken taken) {
        if (source != null) {
            deliver(source.take(false), taken);
            return;
        }
        CN.openGallery(e -> picked(e, taken), CN.GALLERY_IMAGE);
    }

    /// The camera and the gallery both answer with the path of a file, or
    /// with nothing when the user backed out.
    private static void picked(ActionEvent event, Taken taken) {
        Object path = event == null ? null : event.getSource();
        if (!(path instanceof String)) {
            return;
        }
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream((String) path);
            deliver(Image.createImage(in), taken);
        } catch (IOException failed) {
            Log.e(failed);
            Telemetry.error(failed);
            Ui.say("That picture could not be read");
        } finally {
            Util.cleanup(in);
        }
    }

    private static void deliver(Image picture, Taken taken) {
        if (picture == null) {
            return;
        }
        Image small = picture;
        if (picture.getWidth() > LONGEST_SIDE || picture.getHeight() > LONGEST_SIDE) {
            small = picture.scaledSmallerRatio(LONGEST_SIDE, LONGEST_SIDE);
        }
        ImageIO encoder = ImageIO.getImageIO();
        if (encoder == null) {
            Ui.say("This device cannot prepare a picture to send");
            return;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            encoder.save(small, out, ImageIO.FORMAT_JPEG, QUALITY);
            taken.taken(out.toByteArray(), small);
        } catch (IOException failed) {
            Log.e(failed);
            Telemetry.error(failed);
            Ui.say("That picture could not be read");
        }
    }
}
