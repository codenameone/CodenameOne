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
package com.codenameone.examples.reel.studio;

import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.UploadDto;

import java.util.LinkedHashMap;
import java.util.Map;

/// The upload pieces, as raw bytes: a JSON contract cannot carry a body that
/// is not JSON, so this one route is written by hand beside the generated
/// `StudioApi` dispatcher. It is under `/api/studio/` and so behind the same
/// sign-in and role rules.
@RestController
public class UploadController {
    private final Uploads uploads;
    private final Studio studio;

    public UploadController(Uploads uploads, Studio studio) {
        this.uploads = uploads;
        this.studio = studio;
    }

    @PutMapping("/api/studio/uploads/{id}")
    public HttpServer.Response chunk(HttpServer.Request request, @PathVariable("id") String id,
            @RequestParam(value = "offset", required = true) String offset) throws Exception {
        studio.requireCreator();
        long at;
        try {
            at = Long.parseLong(offset);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(400, "offset is a number");
        }
        UploadDto u = uploads.chunk(Caller.name(), id, at, request.getBodyBytes());
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", u.id());
        out.put("videoId", u.videoId());
        out.put("fileName", u.fileName());
        out.put("totalBytes", Long.valueOf(u.totalBytes()));
        out.put("receivedBytes", Long.valueOf(u.receivedBytes()));
        out.put("chunkBytes", Integer.valueOf(u.chunkBytes()));
        out.put("status", u.status());
        return HttpServer.Response.json(200, Json.write(out));
    }
}
