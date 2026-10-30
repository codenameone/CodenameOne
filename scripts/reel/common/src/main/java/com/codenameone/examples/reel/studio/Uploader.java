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

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.CN;
import com.codenameone.examples.reel.Telemetry;
import com.codenameone.examples.reel.api.UploadDto;
import com.codenameone.examples.reel.api.UploadStartDto;
import com.codenameone.examples.reel.net.Api;
import com.codenameone.examples.reel.net.Net;
import com.codenameone.examples.reel.net.Session;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// Uploads a video file in pieces the server sizes (`chunkBytes`), each a
/// PUT of raw bytes at its offset. A piece that fails is sent again from the
/// offset the server reports, so a dropped connection costs one piece, not
/// the file. The server queues packaging once the last byte arrives.
public final class Uploader {
    /// Told how far it got, and when it is done or failed.
    public interface Progress {
        void progress(long sent, long total);

        void done();

        void failed(String message);
    }

    private Uploader() {
    }

    public static void upload(final String videoId, final String path, final Progress progress) {
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final long total = fs.getLength(path);
        String name = path.substring(path.lastIndexOf('/') + 1);
        Api.studio().startUpload(new UploadStartDto(videoId, name, total, ""), Net.to(
                u -> CN.startThread(() -> send(u, path, total, progress), "upload").start(),
                (code, message) -> progress.failed(message)));
    }

    private static void send(UploadDto u, String path, long total, final Progress progress) {
        long offset = u.receivedBytes();
        int size = Math.max(1, u.chunkBytes());
        int tries = 0;
        // One stream for the whole file, read forward piece by piece; it is
        // reopened only when the server sends us back to an earlier offset.
        // Reopening it for every piece reread the file from the start each
        // time, which is quadratic in the file's size.
        InputStream in = null;
        long streamAt = 0;
        try {
            while (offset < total) {
                byte[] piece;
                try {
                    if (in == null || streamAt > offset) {
                        Util.cleanup(in);
                        in = FileSystemStorage.getInstance().openInputStream(path);
                        streamAt = 0;
                    }
                    skip(in, offset - streamAt);
                    streamAt = offset;
                    piece = read(in, (int) Math.min(size, total - offset));
                    streamAt += piece.length;
                } catch (IOException e) {
                    fail(progress, "Could not read the file");
                    return;
                }
                Chunk c = new Chunk(Session.serverUrl() + "/api/studio/uploads/" + u.id() + "?offset=" + offset, piece);
                NetworkManager.getInstance().addToQueueAndWait(c);
                int code = c.getResponseCode();
                if (code == 200) {
                    offset += piece.length;
                    tries = 0;
                    final long sent = offset;
                    CN.callSerially(() -> progress.progress(sent, total));
                } else if (code == 409 && tries < 3) {
                    // Out of step with the server: ask where it is and go on from there.
                    tries++;
                    long at = receivedBytes(u.id());
                    if (at < 0) {
                        fail(progress, "The server did not answer");
                        return;
                    }
                    offset = at;
                } else if ((code <= 0 || code >= 500) && tries < 3) {
                    // No answer at all (a dropped connection, a timeout) is
                    // the case a resumable upload exists for.
                    tries++;
                    Util.sleep(1000 * tries);
                } else {
                    fail(progress, c.getResponseErrorMessage() == null ? "Upload failed" : c.getResponseErrorMessage());
                    return;
                }
            }
        } finally {
            Util.cleanup(in);
        }
        CN.callSerially(() -> {
            Telemetry.uploaded();
            progress.done();
        });
    }

    /// Where the server is in the upload, asked on this thread, or -1.
    private static long receivedBytes(String id) {
        ConnectionRequest r = new ConnectionRequest(Session.serverUrl() + "/api/studio/uploads/" + id, false);
        r.setFailSilently(true);
        r.setReadResponseForErrors(false);
        NetworkManager.getInstance().addToQueueAndWait(r);
        if (r.getResponseCode() != 200 || r.getResponseData() == null) {
            return -1;
        }
        try {
            java.util.Map<String, Object> m = new com.codename1.io.JSONParser().parseJSON(
                    new java.io.InputStreamReader(new java.io.ByteArrayInputStream(r.getResponseData()), "UTF-8"));
            Object v = m.get("receivedBytes");
            return v instanceof Number ? ((Number) v).longValue() : -1;
        } catch (IOException e) {
            return -1;
        }
    }

    private static void fail(final Progress progress, final String message) {
        CN.callSerially(() -> progress.failed(message));
    }

    private static void skip(InputStream in, long count) throws IOException {
        long skipped = 0;
        while (skipped < count) {
            long n = in.skip(count - skipped);
            if (n <= 0) {
                throw new IOException("short file");
            }
            skipped += n;
        }
    }

    private static byte[] read(InputStream in, int length) throws IOException {
        byte[] out = new byte[length];
        int at = 0;
        while (at < length) {
            int n = in.read(out, at, length - at);
            if (n < 0) {
                throw new IOException("short file");
            }
            at += n;
        }
        return out;
    }

    /// One piece, as the body of a PUT.
    private static final class Chunk extends ConnectionRequest {
        private final byte[] body;

        Chunk(String url, byte[] body) {
            this.body = body;
            setUrl(url);
            setPost(true);
            setHttpMethod("PUT");
            setContentType("application/octet-stream");
            setFailSilently(true);
            setReadResponseForErrors(true);
        }

        @Override
        protected void buildRequestBody(OutputStream os) throws IOException {
            os.write(body);
        }

        @Override
        protected void handleErrorResponseCode(int code, String message) {
        }
    }
}
