/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.builders.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uploads per-build symbol artifacts to the BuildCloud crash protection
 * service. Invoked by the cloud build server's post-build hook on
 * successful release builds — local Maven invocations skip this because
 * neither the endpoint URL nor the shared secret are set.
 *
 * <p>This is a deliberately tiny helper: a single multipart POST. No
 * dependency on the Spring stack so it can also be invoked from the
 * stand-alone CN1 build executor.
 */
public final class CrashSymbolUploader {

    private static final Logger LOG = Logger.getLogger(CrashSymbolUploader.class.getName());

    /** Platform constant for an Android ProGuard / R8 {@code mapping.txt}. */
    public static final String PLATFORM_ANDROID = "android";
    /** Platform constant for an iOS dSYM zip. */
    public static final String PLATFORM_IOS = "ios";

    private CrashSymbolUploader() {
    }

    /**
     * Upload a symbol artifact. Returns {@code true} on HTTP 204; logs
     * and returns {@code false} otherwise. Never throws — a build must
     * not fail because the auxiliary crash protection upload failed.
     *
     * @param endpointBase e.g. {@code https://cloud.codenameone.com}.
     *     If {@code null} or empty, the upload is skipped silently.
     * @param sharedSecret value for the {@code X-Buildserver-Secret}
     *     header. Required.
     * @param buildKey the {@code buildEntryKey} of the build.
     * @param platform one of {@link #PLATFORM_ANDROID} or
     *     {@link #PLATFORM_IOS}.
     * @param dsymUuid the dSYM UUID for iOS uploads; null for Android.
     * @param payload the file to upload.
     */
    public static boolean upload(String endpointBase, String sharedSecret,
            String buildKey, String platform, String dsymUuid, File payload) {
        if (endpointBase == null || endpointBase.isEmpty()) return false;
        if (sharedSecret == null || sharedSecret.isEmpty()) return false;
        if (buildKey == null || buildKey.isEmpty()) return false;
        if (payload == null || !payload.isFile()) return false;

        String url = endpointBase.endsWith("/") ? endpointBase : endpointBase + "/";
        url = url + "api/v2/build/" + buildKey + "/symbols";

        String boundary = "----CN1CrashSym" + System.nanoTime();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(180_000);
            conn.setChunkedStreamingMode(64 * 1024);
            conn.setRequestProperty("X-Buildserver-Secret", sharedSecret);
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary);

            try (OutputStream out = conn.getOutputStream()) {
                writePart(out, boundary, "platform", platform);
                if (dsymUuid != null && !dsymUuid.isEmpty()) {
                    writePart(out, boundary, "dsymUuid", dsymUuid);
                }
                writeFilePart(out, boundary, "file", payload);
                writeBoundary(out, boundary, true);
            }

            int code = conn.getResponseCode();
            if (code == 204) return true;
            LOG.warning("crash symbol upload returned " + code + " for build " + buildKey);
            return false;
        } catch (IOException ex) {
            LOG.log(Level.WARNING, "crash symbol upload failed", ex);
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static void writeBoundary(OutputStream out, String boundary, boolean closing) throws IOException {
        out.write(("--" + boundary + (closing ? "--" : "") + "\r\n").getBytes("UTF-8"));
    }

    private static void writePart(OutputStream out, String boundary, String name, String value) throws IOException {
        writeBoundary(out, boundary, false);
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes("UTF-8"));
        out.write(value == null ? new byte[0] : value.getBytes("UTF-8"));
        out.write("\r\n".getBytes("UTF-8"));
    }

    private static void writeFilePart(OutputStream out, String boundary, String name, File file) throws IOException {
        writeBoundary(out, boundary, false);
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\""
                + file.getName() + "\"\r\n").getBytes("UTF-8"));
        out.write("Content-Type: application/octet-stream\r\n\r\n".getBytes("UTF-8"));
        try (InputStream in = new FileInputStream(file)) {
            byte[] buf = new byte[64 * 1024];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        out.write("\r\n".getBytes("UTF-8"));
    }

    /**
     * Convenience helper to upload an Android {@code mapping.txt} if it
     * exists at the conventional gradle output path. Returns
     * {@code false} (without warning) when the mapping file is absent
     * (e.g. ProGuard disabled).
     */
    public static boolean uploadAndroidMapping(String endpointBase, String sharedSecret,
            String buildKey, File projectDir) {
        File mapping = new File(projectDir, "app/build/outputs/mapping/release/mapping.txt");
        if (!mapping.isFile()) {
            return false;
        }
        return upload(endpointBase, sharedSecret, buildKey, PLATFORM_ANDROID, null, mapping);
    }

    /**
     * Convenience helper to upload an iOS dSYM. The caller is
     * responsible for zipping the {@code .dSYM} bundle (Xcode emits a
     * directory) prior to invoking; bundles are typically 50-300 MB so
     * the multipart streamer in {@link #upload} is essential.
     *
     * @param dsymZip already-zipped dSYM bundle from Xcode's archive.
     * @param dsymUuid the UUID extracted from
     *     {@code dwarfdump --uuid <dSYM>/Contents/Resources/DWARF/<bin>};
     *     used server-side to confirm the bundle matches the crashing
     *     binary before invoking the symbolizer.
     */
    public static boolean uploadIosDsym(String endpointBase, String sharedSecret,
            String buildKey, String dsymUuid, File dsymZip) {
        return upload(endpointBase, sharedSecret, buildKey, PLATFORM_IOS, dsymUuid, dsymZip);
    }
}
