/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.examples.hellocodenameone.tests.backend;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.MultipartRequest;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.io.gzip.GZConnectionRequest;

import java.io.IOException;
import java.io.InputStream;

/// Moving bytes: a multipart upload with a form field, a raw binary body, a
/// download straight to a file, and a gzipped answer decoded.
public class BackendTransferTest extends BackendClientTest {
    private static final int DOWNLOAD = 100000;

    @Override
    protected void defineSteps() {
        step(() -> {
            final int id = currentStep();
            final byte[] file = new byte[3000];
            for (int iter = 0 ; iter < file.length ; iter++) {
                file[iter] = (byte) (iter * 7);
            }
            MultipartRequest upload = new MultipartRequest() {
                @Override
                protected void readResponse(InputStream input) throws IOException {
                    String json = new String(Util.readInputStream(input), "UTF-8");
                    if (expect(json.indexOf("\"size\":3000") >= 0, "upload saw " + json)
                            && expect(json.indexOf("\"sum\":" + checksum(file)) >= 0,
                            "upload checksum " + json)
                            && expect(json.indexOf("\"filename\":\"probe.bin\"") >= 0,
                            "upload filename " + json)
                            && expect(json.indexOf("\"note\":\"hello\"") >= 0, "upload note " + json)) {
                        proceed(id);
                    }
                }

                @Override
                protected void handleErrorResponseCode(int code, String message) {
                    expect(false, "upload answered " + code + " " + message);
                }

                @Override
                protected void handleException(final Exception err) {
                    // On the network thread; fail on the EDT like every other step.
                    com.codename1.ui.CN.callSerially(new Runnable() {
                        public void run() {
                            expect(false, "upload failed: " + err);
                        }
                    });
                }
            };
            upload.setUrl(url("/api/upload"));
            upload.addData("file", file, "application/octet-stream");
            upload.setFilename("file", "probe.bin");
            upload.addArgument("note", "hello");
            NetworkManager.getInstance().addToQueue(upload);
        });
        step(() -> {
            final int id = currentStep();
            final byte[] raw = {0, 1, 2, (byte) 0xfe, (byte) 0xff, 13, 10};
            ConnectionRequest post = new ConnectionRequest() {
                @Override
                protected void buildRequestBody(java.io.OutputStream os) throws IOException {
                    os.write(raw);
                }

                @Override
                protected void readResponse(InputStream input) throws IOException {
                    String json = new String(Util.readInputStream(input), "UTF-8");
                    if (expect(json.indexOf("\"size\":7") >= 0
                            && json.indexOf("\"sum\":" + checksum(raw)) >= 0,
                            "raw upload saw " + json)) {
                        proceed(id);
                    }
                }

                @Override
                protected void handleErrorResponseCode(int code, String message) {
                    expect(false, "raw upload answered " + code + " " + message);
                }
            };
            post.setUrl(url("/api/raw"));
            post.setPost(true);
            post.setContentType("application/octet-stream");
            NetworkManager.getInstance().addToQueue(post);
        });
        step(() -> {
            final int id = currentStep();
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String home = fs.getAppHomePath();
            final String path = (home.endsWith("/") ? home : home + "/") + "backend-download.bin";
            if (fs.exists(path)) {
                fs.delete(path);
            }
            ConnectionRequest download = new ConnectionRequest() {
                @Override
                protected void postResponse() {
                    try {
                        InputStream in = FileSystemStorage.getInstance().openInputStream(path);
                        byte[] got = Util.readInputStream(in);
                        in.close();
                        boolean pattern = got.length == DOWNLOAD;
                        for (int iter = 0 ; pattern && iter < got.length ; iter++) {
                            pattern = got[iter] == (byte) iter;
                        }
                        if (expect(pattern, "the downloaded file was " + got.length
                                + " bytes or had the wrong content")) {
                            proceed(id);
                        }
                    } catch (IOException err) {
                        expect(false, "could not read the downloaded file: " + err);
                    }
                }

                @Override
                protected void handleErrorResponseCode(int code, String message) {
                    expect(false, "download answered " + code + " " + message);
                }
            };
            download.setUrl(url("/api/download/" + DOWNLOAD));
            download.setPost(false);
            download.setDestinationFile(path);
            NetworkManager.getInstance().addToQueue(download);
        });
        step(() -> {
            final int id = currentStep();
            GZConnectionRequest gz = new GZConnectionRequest() {
                @Override
                protected void readUnzipedResponse(InputStream input) throws IOException {
                    String text = new String(Util.readInputStream(input), "UTF-8");
                    if (expect(text.length() == 20000 && text.startsWith("Codename One backend probe. "),
                            "the gzipped answer decoded to " + text.length() + " characters")) {
                        proceed(id);
                    }
                }

                @Override
                protected void handleErrorResponseCode(int code, String message) {
                    expect(false, "gzip answered " + code + " " + message);
                }
            };
            gz.setUrl(url("/api/big?size=20000"));
            gz.setPost(false);
            NetworkManager.getInstance().addToQueue(gz);
        });
    }

    /// The backend's checksum: a 31-based polynomial over the bytes.
    static long checksum(byte[] data) {
        long sum = 0;
        for (int iter = 0 ; iter < data.length ; iter++) {
            sum = (sum * 31 + (data[iter] & 0xff)) & 0xffffffffL;
        }
        return sum;
    }
}
