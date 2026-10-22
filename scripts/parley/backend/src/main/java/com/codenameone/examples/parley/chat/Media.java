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
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.parley.Ids;
import com.codenameone.examples.parley.api.MediaDataDto;
import com.codenameone.examples.parley.api.MediaRefDto;
import com.codenameone.examples.parley.api.MediaUploadDto;
import com.codenameone.examples.parley.domain.MediaBlob;
import com.codenameone.examples.parley.repository.MediaBlobRepository;

import java.io.IOException;

/// Encrypted attachments.
///
/// The app encrypts a photo, a video or a voice note with a fresh AES key,
/// uploads the ciphertext here, and puts the id, the key and a digest of the
/// ciphertext inside the message. The server stores bytes it cannot read, for
/// anyone who presents the id: the id is 128 random bits, and holding it is
/// no use without the key that only the message's recipients have.
@Component
public class Media {
    /// The largest attachment: a minute or two of phone video.
    public static final int MAX_BYTES = 16 * 1024 * 1024;

    /// The bytes one account may upload in a day.
    static final long DAILY_BYTES = 256L * 1024 * 1024;

    private static final long DAY_MILLIS = 24L * 60 * 60 * 1000;

    private final MediaBlobRepository blobs;

    public Media(MediaBlobRepository blobs) {
        this.blobs = blobs;
    }

    @Transactional(rollbackFor = IOException.class)
    public MediaRefDto upload(String username, MediaUploadDto media) throws IOException {
        byte[] data = media == null || media.data() == null ? null : Base64.decode(media.data());
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(400, "Nothing to store");
        }
        if (data.length > MAX_BYTES) {
            throw new ResponseStatusException(413, "That attachment is too large");
        }
        // The request limit alone still lets one account store gigabytes an
        // hour, kept for the 30 days before unclaimed blobs are swept. A
        // day's bytes are capped as well, well past what anyone sharing
        // photos and voice notes uploads.
        long now = System.currentTimeMillis();
        Long stored = blobs.storedSince(username, now - DAY_MILLIS);
        if ((stored == null ? 0L : stored.longValue()) + data.length > DAILY_BYTES) {
            throw new ResponseStatusException(429, "You have uploaded too much today");
        }
        MediaBlob row = new MediaBlob();
        row.setId(Ids.next());
        row.setOwner(username);
        row.setSize(data.length);
        row.setCreatedAt(now);
        row.setData(data);
        blobs.insert(row);
        return new MediaRefDto(row.getId(), data.length);
    }

    @Transactional(readOnly = true)
    public MediaDataDto download(String id) {
        MediaBlob row = id == null ? null : blobs.findById(id);
        if (row == null) {
            throw new ResponseStatusException(404, "That attachment has expired");
        }
        return new MediaDataDto(row.getId(), Base64.encode(row.getData()));
    }
}
