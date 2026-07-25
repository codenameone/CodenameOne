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
package com.codename1.ai.inference;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.security.Hash;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionListener;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.InputStream;

/// Downloads large LiteRT models once and exposes the cached file as a
/// {@link ModelSource}. Small models can instead be packaged directly with
/// {@link ModelSource#resource(String)}.
public final class ModelCache {
    private ModelCache() {
    }

    /// Fetches a model into the app-private {@code ai-models} directory.
    ///
    /// @param url HTTPS URL of the model
    /// @param cacheKey stable cache name, independent of the URL
    /// @param sha256 optional lowercase or uppercase SHA-256 hex digest
    /// @return asynchronous cached model source
    public static AsyncResource<ModelSource> fetch(
            final String url, final String cacheKey, final String sha256) {
        if (url == null || !url.startsWith("https://")) {
            throw new IllegalArgumentException("Model URL must use HTTPS");
        }
        if (cacheKey == null || cacheKey.length() == 0) {
            throw new IllegalArgumentException("cacheKey must not be empty");
        }
        if (sha256 != null && sha256.length() != 64) {
            throw new IllegalArgumentException("SHA-256 must contain 64 hex characters");
        }

        final AsyncResource<ModelSource> out = new AsyncResource<ModelSource>();
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                final FileSystemStorage fs = FileSystemStorage.getInstance();
                final String directory = fs.getAppHomePath() + "ai-models/";
                fs.mkdir(directory);
                final String fileName = safeName(cacheKey) + ".tflite";
                final String target = directory + fileName;
                try {
                    if (fs.exists(target) && verify(target, sha256)) {
                        complete(out, ModelSource.file(target));
                        return;
                    }
                    if (fs.exists(target)) {
                        fs.delete(target);
                    }
                } catch (IOException error) {
                    fail(out, error);
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        download(out, url, sha256, target, fileName);
                    }
                });
            }
        });
        return out;
    }

    public static AsyncResource<ModelSource> fetch(String url, String cacheKey) {
        return fetch(url, cacheKey, null);
    }

    private static void download(final AsyncResource<ModelSource> out,
                                 String url, final String sha256,
                                 final String target, final String fileName) {
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final String temporary = target + ".download";
        if (fs.exists(temporary)) {
            fs.delete(temporary);
        }
        final ConnectionRequest request = new ConnectionRequest();
        request.setPost(false);
        request.setFailSilently(true);
        request.setReadResponseForErrors(false);
        request.setDuplicateSupported(true);
        request.setUrl(url);
        request.setDestinationFile(temporary);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            public void actionPerformed(NetworkEvent event) {
                Display.getInstance().scheduleBackgroundTask(new Runnable() {
                    public void run() {
                        try {
                            if (!verify(temporary, sha256)) {
                                fs.delete(temporary);
                                throw new IOException("Downloaded model SHA-256 does not match");
                            }
                            if (fs.exists(target)) {
                                fs.delete(target);
                            }
                            fs.rename(temporary, fileName);
                            complete(out, ModelSource.file(target));
                        } catch (IOException error) {
                            fail(out, error);
                        }
                    }
                });
            }
        });
        ActionListener<NetworkEvent> failure = new ActionListener<NetworkEvent>() {
            public void actionPerformed(NetworkEvent event) {
                if (fs.exists(temporary)) {
                    fs.delete(temporary);
                }
                Throwable error = event.getError();
                fail(out, error == null
                        ? new IOException("Model download failed with HTTP "
                                + event.getResponseCode())
                        : error);
            }
        };
        request.addExceptionListener(failure);
        request.addResponseCodeListener(failure);
        NetworkManager.getInstance().addToQueue(request);
    }

    private static boolean verify(String path, String expected) throws IOException {
        if (expected == null || expected.length() == 0) {
            return true;
        }
        InputStream input = FileSystemStorage.getInstance().openInputStream(path);
        try {
            Hash hash = Hash.create(Hash.SHA256);
            byte[] buffer = new byte[16384];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                hash.update(buffer, 0, read);
            }
            return expected.equalsIgnoreCase(Hash.toHex(hash.digest()));
        } finally {
            input.close();
        }
    }

    private static String safeName(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            out.append((c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' ? c : '_');
        }
        return out.toString();
    }

    private static <T> void complete(final AsyncResource<T> out, final T value) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.complete(value);
            }
        });
    }

    private static void fail(final AsyncResource<?> out, final Throwable error) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.error(new InferenceException("Could not cache LiteRT model", error));
            }
        });
    }
}
