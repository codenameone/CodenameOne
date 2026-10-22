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
package com.codenameone.examples.parley.chat;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.parley.api.PhotoDto;

/// Checks a profile or group photo before it is stored. These are the only
/// pictures the server can see -- attachments arrive encrypted -- so they are
/// the only ones it can check: a JPEG or a PNG, and small.
public final class Photos {
    /// The largest photo kept. The app scales a picture down before sending.
    public static final int MAX_BYTES = 512 * 1024;

    private Photos() {
    }

    /// The photo's bytes, or a 400 when it is not a picture this server keeps.
    public static byte[] image(PhotoDto photo) {
        if (photo == null || photo.data() == null) {
            throw new ResponseStatusException(400, "No photo");
        }
        byte[] data = Base64.decode(photo.data());
        if (data == null || data.length < 8) {
            throw new ResponseStatusException(400, "That is not a picture");
        }
        if (data.length > MAX_BYTES) {
            throw new ResponseStatusException(413, "That photo is too large");
        }
        boolean jpeg = (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xd8;
        boolean png = (data[0] & 0xff) == 0x89 && data[1] == 'P' && data[2] == 'N'
                && data[3] == 'G';
        if (!(jpeg && "image/jpeg".equals(photo.contentType())
                || png && "image/png".equals(photo.contentType()))) {
            throw new ResponseStatusException(415, "A photo is a JPEG or a PNG");
        }
        return data;
    }
}
