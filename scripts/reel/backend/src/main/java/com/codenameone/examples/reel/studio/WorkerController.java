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

import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.Json;
import com.codename1.backend.StaticFiles;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.PutMapping;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.api.WorkerJobDto;
import com.codenameone.examples.reel.api.WorkerProgressDto;
import com.codenameone.examples.reel.api.WorkerResultDto;
import com.codenameone.examples.reel.playback.MediaOrigin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The worker protocol (`tools/packager worker`), outside the OAuth chain and
/// behind `Authorization: Bearer {reel.worker.token}`.
///
/// With no token configured every worker route answers 503: there is no
/// default token, because whoever holds it receives every source file and
/// every content key. Keep these routes off the public internet when you can.
@RestController
public class WorkerController {
    private final Jobs jobs;
    private final Uploads uploads;
    private final byte[] token;

    public WorkerController(Jobs jobs, Uploads uploads, @Value("${reel.worker.token:}") String token)
            throws Exception {
        this.jobs = jobs;
        this.uploads = uploads;
        this.token = token.trim().getBytes("UTF-8");
    }

    @PostMapping("/worker/jobs/claim")
    public HttpServer.Response claim(HttpServer.Request request,
            @RequestParam(value = "worker", required = false) String worker) throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        WorkerJobDto job = jobs.claim(worker == null ? "worker" : worker);
        if (job == null) {
            return HttpServer.Response.empty(204, "text/plain", null);
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", job.id());
        out.put("videoId", job.videoId());
        out.put("kind", job.kind());
        out.put("sourceUrl", job.sourceUrl());
        out.put("keyId", job.keyId());
        out.put("keyHex", job.keyHex());
        out.put("ivHex", job.ivHex());
        out.put("renditions", job.renditions());
        out.put("segmentSeconds", Integer.valueOf(job.segmentSeconds()));
        return HttpServer.Response.json(200, Json.write(out));
    }

    @GetMapping("/worker/source/{video}")
    public HttpServer.Response source(HttpServer.Request request, @PathVariable("video") String video)
            throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        if (!MediaOrigin.safeId(video)) {
            return HttpServer.Response.text(404, "not found");
        }
        HttpServer.Response out = new StaticFiles(uploads.directory(), "/worker/source", "none", "no-store")
                .handle(request);
        return out == null ? HttpServer.Response.text(404, "not found") : out;
    }

    @PostMapping("/worker/jobs/{id}/progress")
    public HttpServer.Response progress(HttpServer.Request request, @PathVariable("id") String id)
            throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        jobs.progress(id, progressOf(request));
        return HttpServer.Response.text(200, "ok");
    }

    @PutMapping("/worker/jobs/{id}/files/{name}")
    public HttpServer.Response file(HttpServer.Request request, @PathVariable("id") String id,
            @PathVariable("name") String name, @RequestParam(value = "offset", required = false) String offset)
            throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        long at;
        try {
            at = offset == null || offset.length() == 0 ? 0L : Long.parseLong(offset);
        } catch (NumberFormatException e) {
            return HttpServer.Response.text(400, "offset is a number");
        }
        jobs.file(id, name, at, request.getBodyBytes());
        return HttpServer.Response.text(200, "ok");
    }

    @PostMapping("/worker/jobs/{id}/done")
    public HttpServer.Response done(HttpServer.Request request, @PathVariable("id") String id) throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        Map<String, Object> m = body(request);
        jobs.done(id, new WorkerResultDto((int) Rows.number(m.get("durationSeconds")),
                (int) Rows.number(m.get("width")), (int) Rows.number(m.get("height")), strings(m.get("renditions")),
                strings(m.get("frameHashes")), Rows.text(m.get("posterPath")), Rows.text(m.get("spritePath"))));
        return HttpServer.Response.text(200, "ok");
    }

    @PostMapping("/worker/jobs/{id}/fail")
    public HttpServer.Response fail(HttpServer.Request request, @PathVariable("id") String id) throws Exception {
        HttpServer.Response denied = check(request);
        if (denied != null) {
            return denied;
        }
        jobs.fail(id, progressOf(request));
        return HttpServer.Response.text(200, "ok");
    }

    private HttpServer.Response check(HttpServer.Request request) throws Exception {
        if (token.length == 0) {
            return HttpServer.Response.text(503, "workers are not enabled: set reel.worker.token");
        }
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")
                || !Crypto.equalsConstantTime(auth.substring(7).trim().getBytes("UTF-8"), token)) {
            return HttpServer.Response.text(401, "unauthorized");
        }
        return null;
    }

    private static WorkerProgressDto progressOf(HttpServer.Request request) throws Exception {
        Map<String, Object> m = body(request);
        return new WorkerProgressDto((int) Rows.number(m.get("progressPercent")), Rows.text(m.get("status")),
                Rows.text(m.get("error")));
    }

    private static Map<String, Object> body(HttpServer.Request request) throws Exception {
        byte[] raw = request.getBodyBytes();
        if (raw == null || raw.length == 0) {
            return new LinkedHashMap<String, Object>();
        }
        Map<String, Object> m = Json.parseObject(new String(raw, "UTF-8"));
        return m == null ? new LinkedHashMap<String, Object>() : m;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            List<?> l = (List<?>) value;
            for (int iter = 0; iter < l.size(); iter++) {
                out.add(String.valueOf(l.get(iter)));
            }
        }
        return out;
    }
}
