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
        while (offset < total) {
            byte[] piece;
            try {
                piece = read(path, offset, (int) Math.min(size, total - offset));
            } catch (IOException e) {
                fail(progress, "Could not read the file");
                return;
            }
            Chunk c = new Chunk(Session.serverUrl() + "/api/studio/uploads/" + u.id() + "?offset=" + offset, piece);
            NetworkManager.getInstance().addToQueueAndWait(c);
            if (c.getResponseCode() == 200) {
                offset += piece.length;
                tries = 0;
                final long sent = offset;
                CN.callSerially(() -> progress.progress(sent, total));
            } else if (c.getResponseCode() == 409 && tries < 3) {
                // Out of step with the server: ask where it is and go on from there.
                tries++;
                final long[] at = {-1};
                final String[] err = {null};
                Object lock = new Object();
                Api.studio().upload(u.id(), Net.to(fresh -> at[0] = fresh.receivedBytes(),
                        (code, message) -> err[0] = message));
                long until = System.currentTimeMillis() + 15000;
                while (at[0] < 0 && err[0] == null && System.currentTimeMillis() < until) {
                    Util.sleep(100);
                }
                if (at[0] < 0) {
                    fail(progress, err[0] == null ? "The server did not answer" : err[0]);
                    return;
                }
                offset = at[0];
            } else if (c.getResponseCode() >= 500 && tries < 3) {
                tries++;
                Util.sleep(1000 * tries);
            } else {
                fail(progress, c.getResponseErrorMessage() == null ? "Upload failed" : c.getResponseErrorMessage());
                return;
            }
        }
        CN.callSerially(() -> {
            Telemetry.uploaded();
            progress.done();
        });
    }

    private static void fail(final Progress progress, final String message) {
        CN.callSerially(() -> progress.failed(message));
    }

    private static byte[] read(String path, long offset, int length) throws IOException {
        InputStream in = FileSystemStorage.getInstance().openInputStream(path);
        try {
            long skipped = 0;
            while (skipped < offset) {
                long n = in.skip(offset - skipped);
                if (n <= 0) {
                    throw new IOException("short file");
                }
                skipped += n;
            }
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
        } finally {
            Util.cleanup(in);
        }
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
