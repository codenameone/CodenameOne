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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.MediaApiServer;
import com.codenameone.examples.parley.api.MediaDataDto;
import com.codenameone.examples.parley.api.MediaRefDto;
import com.codenameone.examples.parley.api.MediaUploadDto;

/// The server's half of `MediaApi`.
@Component
public class MediaEndpoint implements MediaApiServer {
    private final Media media;

    public MediaEndpoint(Media media) {
        this.media = media;
    }

    @Override
    public MediaRefDto upload(MediaUploadDto upload) throws Exception {
        return media.upload(Caller.name(), upload);
    }

    @Override
    public MediaDataDto download(String id) throws Exception {
        Caller.name();
        return media.download(id);
    }
}
