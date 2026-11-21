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
package com.codenameone.examples.bazaar.web.security;

import com.codename1.backend.ImageInfo;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.image.JpegCodec;
import com.codename1.backend.image.PngCodec;
import com.codename1.backend.image.RasterImage;

/// Checks an uploaded image before anything keeps it, and keeps only what the
/// server drew itself.
///
/// 1. **Size.** More than `maxBytes` is refused (413) before anything reads it.
/// 2. **Type, by its bytes.** The format is read from the file's signature
///    ([ImageInfo]), never from the name or the type the client declared; a
///    declared type that disagrees with the bytes is refused too. Only PNG and
///    JPEG are taken.
/// 3. **Dimensions, from the header.** An image wider or taller than
///    [#MAX_SIDE] or larger than [#MAX_PIXELS] is refused before it is
///    decoded: a small file can claim a huge canvas (a decompression bomb).
/// 4. **Re-encoded.** The pixels are decoded and written out again by the
///    backend's own codecs. What is kept is that new file: whatever else the
///    upload carried -- metadata with a location in it, a script appended after
///    the image, a file that is also valid as another format -- is gone. A file
///    the decoder cannot read is refused.
public final class Uploads {
    /// The longest side an image may claim.
    public static final int MAX_SIDE = 6000;
    /// The most pixels an image may claim (24 megapixels).
    public static final long MAX_PIXELS = 24000000L;
    /// The quality a JPEG is written back at.
    static final float JPEG_QUALITY = 0.9f;

    private Uploads() {
    }

    /// An accepted image: its type and the bytes the server wrote.
    public static final class Image {
        public final String contentType;
        public final byte[] bytes;
        public final int width;
        public final int height;

        Image(String contentType, byte[] bytes, int width, int height) {
            this.contentType = contentType;
            this.bytes = bytes;
            this.width = width;
            this.height = height;
        }
    }

    /// Checks `data` as an image upload and answers it re-encoded. `declared`,
    /// the type the client named, may be null; when given it must match.
    public static Image image(byte[] data, String declared, int maxBytes) {
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(400, "No image");
        }
        if (data.length > maxBytes) {
            throw new ResponseStatusException(413, "That image is too large");
        }
        ImageInfo info = ImageInfo.read(data);
        String type;
        if (info != null && info.getFormat() == ImageInfo.Format.PNG) {
            type = "image/png";
        } else if (info != null && info.getFormat() == ImageInfo.Format.JPEG) {
            type = "image/jpeg";
        } else {
            throw new ResponseStatusException(415, "An image is a JPEG or a PNG");
        }
        if (declared != null && declared.length() > 0 && !type.equalsIgnoreCase(declared.trim())) {
            throw new ResponseStatusException(400, "The image is not the type it says it is");
        }
        int w = info.getWidth();
        int h = info.getHeight();
        if (w <= 0 || h <= 0 || w > MAX_SIDE || h > MAX_SIDE || (long) w * h > MAX_PIXELS) {
            throw new ResponseStatusException(413, "That image is too large");
        }
        RasterImage pixels;
        try {
            pixels = "image/png".equals(type) ? PngCodec.decode(data) : JpegCodec.decode(data);
        } catch (Exception unreadable) {
            throw new ResponseStatusException(400, "That image could not be read");
        }
        if (pixels == null || pixels.getWidth() != w || pixels.getHeight() != h) {
            throw new ResponseStatusException(400, "That image could not be read");
        }
        byte[] clean = "image/png".equals(type) ? PngCodec.encode(pixels)
                : JpegCodec.encode(pixels, JPEG_QUALITY);
        return new Image(type, clean, w, h);
    }
}
