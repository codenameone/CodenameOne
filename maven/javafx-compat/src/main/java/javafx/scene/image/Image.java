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
package javafx.scene.image;

import java.io.IOException;
import java.io.InputStream;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/// A picture decoded by Codename One, for an [ImageView] or a canvas.
///
/// #### Where a URL is looked up
///
/// - `file:` URLs are opened through Codename One's
///   `FileSystemStorage`, so they name files of the device's file system.
/// - Anything else that is not `http:` or `https:` is a resource of the
///   application, as a class path URL is in JavaFX: `"/images/logo.png"`,
///   `"logo.png"` and the `jar:` or `file:`-less result of
///   `getResource(...).toExternalForm()` all work. The whole path is
///   tried first and then the file name alone, because a Codename One
///   application keeps its resources in one flat directory on a device.
/// - `http:` and `https:` URLs are **not loaded**; the image reports an
///   error.
///
/// #### Loading is synchronous
///
/// The constructor returns with the image decoded or failed, also when
/// `backgroundLoading` is asked for; the flag is recorded and
/// `getProgress()` is 1 for a loaded image from the start. A failure
/// never throws: `isError()` answers `true`, `getException()` says why
/// and the size is zero.
///
/// The size is in pixels of the picture, and one of them is drawn as one
/// logical pixel. A requested size scales the decoded picture once, here.
///
/// #### Not part of this layer
///
/// `getPixelReader()`, `WritableImage`, animated images and `smooth` as
/// anything but a recorded flag.
public class Image {

    private final String url;
    private final double requestedWidth;
    private final double requestedHeight;
    private final boolean preserveRatio;
    private final boolean smooth;
    private final boolean backgroundLoading;
    private final ReadOnlyDoubleWrapper width = new ReadOnlyDoubleWrapper(this, "width", 0);
    private final ReadOnlyDoubleWrapper height = new ReadOnlyDoubleWrapper(this, "height", 0);
    private final ReadOnlyDoubleWrapper progress = new ReadOnlyDoubleWrapper(this, "progress", 0);
    private final ReadOnlyBooleanWrapper error = new ReadOnlyBooleanWrapper(this, "error", false);
    private final ReadOnlyObjectWrapper<Exception> exception = new ReadOnlyObjectWrapper<Exception>(this,
            "exception");
    private com.codename1.ui.Image nativeImage;

    /// Loads an image from a URL at its own size.
    public Image(String url) {
        this(url, 0, 0, false, false, false);
    }

    /// Loads an image from a URL at its own size. Loading is synchronous
    /// whatever the flag says.
    public Image(String url, boolean backgroundLoading) {
        this(url, 0, 0, false, false, backgroundLoading);
    }

    /// Loads an image from a URL and scales it to a requested size; a
    /// dimension that is not positive keeps the picture's own, or follows
    /// the other one when the ratio is preserved.
    public Image(String url, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth) {
        this(url, requestedWidth, requestedHeight, preserveRatio, smooth, false);
    }

    /// Loads an image from a URL and scales it to a requested size.
    /// Loading is synchronous whatever the flag says.
    public Image(String url, double requestedWidth, double requestedHeight, boolean preserveRatio, boolean smooth,
            boolean backgroundLoading) {
        if (url == null) {
            throw new NullPointerException("URL must not be null");
        }
        this.url = url;
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        this.preserveRatio = preserveRatio;
        this.smooth = smooth;
        this.backgroundLoading = backgroundLoading;
        InputStream in = null;
        try {
            in = open(url);
            decode(in);
        } catch (IOException failed) {
            fail(failed);
        } finally {
            close(in);
        }
    }

    /// Decodes an image from a stream at its own size. The stream is read
    /// to its end and left open.
    public Image(InputStream is) {
        this(is, 0, 0, false, false);
    }

    /// Decodes an image from a stream and scales it to a requested size.
    public Image(InputStream is, double requestedWidth, double requestedHeight, boolean preserveRatio,
            boolean smooth) {
        if (is == null) {
            throw new NullPointerException("Input stream must not be null");
        }
        this.url = null;
        this.requestedWidth = requestedWidth;
        this.requestedHeight = requestedHeight;
        this.preserveRatio = preserveRatio;
        this.smooth = smooth;
        this.backgroundLoading = false;
        try {
            decode(is);
        } catch (IOException failed) {
            fail(failed);
        }
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
                // Nothing to do about a stream that will not close.
                return;
            }
        }
    }

    private static boolean startsWith(String s, String prefix) {
        return s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static InputStream resource(String path) {
        try {
            return Display.getInstance().getResourceAsStream(Image.class, path);
        } catch (RuntimeException unavailable) {
            // A port that keeps resources flat rejects a path with
            // directories outright.
            return null;
        }
    }

    private static InputStream open(String url) throws IOException {
        if (startsWith(url, "http:") || startsWith(url, "https:")) {
            throw new IOException("Remote images are not supported: " + url);
        }
        if (startsWith(url, "file:")) {
            return FileSystemStorage.getInstance().openInputStream(url);
        }
        String path = url;
        int bang = path.lastIndexOf("!/");
        if (bang >= 0) {
            // jar:file:/app.jar!/images/logo.png
            path = path.substring(bang + 1);
        } else {
            int scheme = path.indexOf(":/");
            if (scheme >= 0) {
                path = path.substring(scheme + 1);
            }
        }
        while (path.startsWith("//")) {
            path = path.substring(1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        InputStream in = resource(path);
        int slash = path.lastIndexOf('/');
        if (in == null && slash > 0) {
            in = resource(path.substring(slash));
        }
        if (in == null) {
            throw new IOException("Image not found: " + url);
        }
        return in;
    }

    private void fail(Exception why) {
        nativeImage = null;
        width.set(0);
        height.set(0);
        exception.set(why);
        error.set(true);
    }

    private void decode(InputStream in) throws IOException {
        com.codename1.ui.Image decoded;
        try {
            decoded = com.codename1.ui.Image.createImage(in);
        } catch (RuntimeException undecodable) {
            throw new IOException("Not an image: " + undecodable);
        }
        if (decoded == null || decoded.getWidth() <= 0 || decoded.getHeight() <= 0) {
            throw new IOException("Not an image");
        }
        double w = decoded.getWidth();
        double h = decoded.getHeight();
        double tw;
        double th;
        if (preserveRatio) {
            double k = 1;
            if (requestedWidth > 0 && requestedHeight > 0) {
                k = Math.min(requestedWidth / w, requestedHeight / h);
            } else if (requestedWidth > 0) {
                k = requestedWidth / w;
            } else if (requestedHeight > 0) {
                k = requestedHeight / h;
            }
            tw = w * k;
            th = h * k;
        } else {
            tw = requestedWidth > 0 ? requestedWidth : w;
            th = requestedHeight > 0 ? requestedHeight : h;
        }
        int pw = (int) Math.max(1, Math.round(tw));
        int ph = (int) Math.max(1, Math.round(th));
        if (pw != decoded.getWidth() || ph != decoded.getHeight()) {
            decoded = decoded.scaled(pw, ph);
        }
        nativeImage = decoded;
        width.set(pw);
        height.set(ph);
        progress.set(1);
    }

    /// Returns the Codename One image, or `null` when loading failed.
    public final com.codename1.ui.Image cn1Native() {
        return nativeImage;
    }

    /// Returns the URL the image was loaded from; `null` for a stream.
    public final String getUrl() {
        return url;
    }

    /// Returns how much of the image is loaded: 1 for a loaded image, 0
    /// for one that failed.
    public final double getProgress() {
        return progress.get();
    }

    /// How much of the image is loaded.
    public final ReadOnlyDoubleProperty progressProperty() {
        return progress.getReadOnlyProperty();
    }

    /// Returns the width asked for, or 0.
    public final double getRequestedWidth() {
        return requestedWidth;
    }

    /// Returns the height asked for, or 0.
    public final double getRequestedHeight() {
        return requestedHeight;
    }

    /// Returns the width in pixels; 0 when loading failed.
    public final double getWidth() {
        return width.get();
    }

    /// The width in pixels.
    public final ReadOnlyDoubleProperty widthProperty() {
        return width.getReadOnlyProperty();
    }

    /// Returns the height in pixels; 0 when loading failed.
    public final double getHeight() {
        return height.get();
    }

    /// The height in pixels.
    public final ReadOnlyDoubleProperty heightProperty() {
        return height.getReadOnlyProperty();
    }

    /// Returns whether a requested size keeps the picture's proportions.
    public final boolean isPreserveRatio() {
        return preserveRatio;
    }

    /// Returns whether a better scaling filter was asked for.
    public final boolean isSmooth() {
        return smooth;
    }

    /// Returns whether loading in the background was asked for.
    public final boolean isBackgroundLoading() {
        return backgroundLoading;
    }

    /// Returns whether loading failed.
    public final boolean isError() {
        return error.get();
    }

    /// Whether loading failed.
    public final ReadOnlyBooleanProperty errorProperty() {
        return error.getReadOnlyProperty();
    }

    /// Returns why loading failed, or `null`.
    public final Exception getException() {
        return exception.get();
    }

    /// Why loading failed.
    public final ReadOnlyObjectProperty<Exception> exceptionProperty() {
        return exception.getReadOnlyProperty();
    }

    /// Stops a background load. Loading is over before the constructor
    /// returns, so there is never one to stop.
    public void cancel() {
    }
}
