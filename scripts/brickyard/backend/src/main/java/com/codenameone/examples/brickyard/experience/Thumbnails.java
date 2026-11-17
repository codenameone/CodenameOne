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
package com.codenameone.examples.brickyard.experience;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.api.ThumbnailDto;
import com.codenameone.examples.brickyard.domain.Experience;
import com.codenameone.examples.brickyard.domain.ExperienceThumbnail;
import com.codenameone.examples.brickyard.repository.ExperienceThumbnailRepository;
import java.io.IOException;

/// Experience tile pictures. A creator's client captures one from the real
/// renderer when publishing; players' clients only read it.
@Component
public class Thumbnails {
    /// A tile is shown at most a few hundred pixels wide; a larger upload is
    /// a mistake or an abuse, never a better tile.
    static final int MAX_BYTES = 400 * 1024;

    private final ExperienceThumbnailRepository thumbnails;
    private final Experiences experiences;

    public Thumbnails(ExperienceThumbnailRepository thumbnails, Experiences experiences) {
        this.thumbnails = thumbnails;
        this.experiences = experiences;
    }

    @Transactional(readOnly = true)
    public ThumbnailDto get(String id) {
        Experience e = experiences.visible(id);
        ExperienceThumbnail t = thumbnails.findById(e.getId());
        return new ThumbnailDto(t == null ? "" : Base64.encode(t.getImage()));
    }

    @Transactional(rollbackFor = IOException.class)
    public ThumbnailDto set(String id, String data) throws IOException {
        Experience e = experiences.visible(id);
        Access.requireOwner(e);
        byte[] image = data == null ? null : Base64.decode(data);
        if (image == null || image.length == 0) {
            throw new ResponseStatusException(400, "The thumbnail is not base64");
        }
        if (image.length > MAX_BYTES) {
            throw new ResponseStatusException(413, "The thumbnail is larger than " + MAX_BYTES / 1024 + " KB");
        }
        if (!isImage(image)) {
            throw new ResponseStatusException(400, "The thumbnail must be a JPEG or a PNG");
        }
        ExperienceThumbnail t = thumbnails.findById(e.getId());
        if (t == null) {
            t = new ExperienceThumbnail();
            t.setExperienceId(e.getId());
        }
        t.setImage(image);
        t.setUpdatedAt(Clock.now());
        thumbnails.save(t);
        return new ThumbnailDto(Base64.encode(image));
    }

    /// True for JPEG (FF D8 FF) or PNG (89 'P' 'N' 'G') bytes; anything else
    /// would reach every player's image decoder.
    static boolean isImage(byte[] b) {
        boolean jpeg = b.length > 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8 && (b[2] & 0xff) == 0xff;
        boolean png = b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        return jpeg || png;
    }
}
