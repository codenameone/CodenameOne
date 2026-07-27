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
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/// Downloads a large model into app-private storage and exposes it as a
/// file-backed {@link ModelSource}. The initial request requires HTTPS.
/// Ports that expose redirect responses reject any redirect to HTTP. Because
/// iOS follows redirects below the portable network layer, iOS downloads
/// require a SHA-256 digest so an unseen redirect cannot substitute the
/// executable model payload. Downloads use a temporary file and are promoted
/// only after optional digest verification.
///
/// Supply a SHA-256 digest for third-party or remotely mutable models. Without
/// a digest HTTPS authenticates the connection but does not pin the executable
/// model payload. Small first-party models can instead be packaged with
/// {@link ModelSource#resource(String)}.
public final class ModelCache {
    private static final int MAX_READABLE_CACHE_KEY_LENGTH = 160;
    private static final Hashtable<String, ActiveFetch> ACTIVE_FETCHES =
            new Hashtable<String, ActiveFetch>();

    private ModelCache() {
    }

    /// Fetches a model into the app-private {@code ai-models} directory.
    /// A stale {@code .download} file is deleted and restarted rather than
    /// resumed because the portable network layer cannot prove that a server's
    /// partial response still represents the pinned model.
    /// Concurrent requests for the same cache entry and content identity share
    /// one underlying operation, but each caller receives an independent
    /// resource. Canceling one caller's resource suppresses only that caller's
    /// notification and does not cancel the shared download or other callers.
    /// A different URL or digest using that cache key while the first operation
    /// is active fails instead of racing on the temporary file.
    ///
    /// @param url initial HTTPS URL of the model; observable redirects must
    /// also use HTTPS
    /// @param cacheKey stable cache name, independent of the URL
    /// @param sha256 lowercase or uppercase SHA-256 hex digest; optional on
    /// ports that expose redirects and required on iOS
    /// @return asynchronous cached model source
    /// @throws IllegalArgumentException if the URL, cache key, or digest is
    /// invalid, or if the digest is omitted on iOS
    public static AsyncResource<ModelSource> fetch(
            final String url, final String cacheKey, final String sha256) {
        if (!isHttpsUrl(url)) {
            throw new IllegalArgumentException("Model URL must use HTTPS");
        }
        if (cacheKey == null || cacheKey.length() == 0) {
            throw new IllegalArgumentException("cacheKey must not be empty");
        }
        if (sha256 != null && !isSha256(sha256)) {
            throw new IllegalArgumentException("SHA-256 must contain 64 hex characters");
        }
        if (sha256 == null && requiresPinnedModelDigest()) {
            throw new IllegalArgumentException(
                    "iOS model downloads require a SHA-256 digest because "
                    + "redirects are followed below the portable network layer");
        }

        final String fileName = safeName(cacheKey) + ".tflite";
        final FetchRegistration registration =
                registerFetch(fileName, url, sha256);
        if (!registration.owner) {
            return registration.resource;
        }
        final AsyncResource<ModelSource> out = registration.resource;
        final Completion<ModelSource> completion = registration.completion;
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            @Override
            public void run() {
                final FileSystemStorage fs = FileSystemStorage.getInstance();
                final String directory = fs.getAppHomePath() + "ai-models/";
                fs.mkdir(directory);
                final String target = directory + fileName;
                try {
                    if (fs.exists(target) && verify(target, sha256)) {
                        completion.complete(ModelSource.file(target));
                        return;
                    }
                    if (fs.exists(target)) {
                        fs.delete(target);
                    }
                } catch (IOException error) {
                    completion.fail(error);
                    return;
                }
                Display.getInstance().callSerially(new Runnable() {
                    @Override
                    public void run() {
                        download(completion, url, sha256, target, fileName);
                    }
                });
            }
        });
        return out;
    }

    /// Fetches a model without content pinning on ports that expose redirect
    /// responses to the portable network layer. Prefer the three-argument
    /// overload for any model that is not versioned by the app itself. iOS
    /// rejects this overload because its native network stack follows
    /// redirects before Codename One can validate their schemes.
    /// Concurrent unpinned calls share an operation only when their URL and
    /// cache key are identical.
    ///
    /// @param url HTTPS model URL
    /// @param cacheKey stable cache name
    /// @return asynchronous cached file source
    /// @throws IllegalArgumentException if the URL or cache key is invalid,
    /// or when called on iOS, where a digest is required
    public static AsyncResource<ModelSource> fetch(String url, String cacheKey) {
        return fetch(url, cacheKey, null);
    }

    private static void download(final Completion<ModelSource> completion,
                                 String url, final String sha256,
                                 final String target, final String fileName) {
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final String temporary = target + ".download";
        prepareTemporary(fs, temporary);
        final ConnectionRequest request = new ModelDownloadRequest(
                completion, fs, temporary);
        request.setPost(false);
        request.setFailSilently(true);
        request.setReadResponseForErrors(false);
        request.setDuplicateSupported(true);
        request.setUrl(url);
        request.setDestinationFile(temporary);
        request.addResponseListener(new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent event) {
                Display.getInstance().scheduleBackgroundTask(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            verifyDownloaded(fs, temporary, sha256);
                            promoteDownloaded(fs, temporary, target,
                                    fileName, sha256);
                            completion.complete(ModelSource.file(target));
                        } catch (IOException error) {
                            completion.fail(error);
                        }
                    }
                });
            }
        });
        ActionListener<NetworkEvent> failure = new ActionListener<NetworkEvent>() {
            @Override
            public void actionPerformed(NetworkEvent event) {
                if (fs.exists(temporary)) {
                    fs.delete(temporary);
                }
                Throwable error = event.getError();
                completion.fail(error == null
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

    static void prepareTemporary(FileSystemStorage fs, String temporary) {
        if (fs.exists(temporary)) {
            fs.delete(temporary);
        }
    }

    static void verifyDownloaded(FileSystemStorage fs, String temporary,
                                 String expected) throws IOException {
        if (!verify(temporary, expected)) {
            fs.delete(temporary);
            throw new IOException("Downloaded model SHA-256 does not match");
        }
    }

    static void promoteDownloaded(FileSystemStorage fs, String temporary,
                                  String target, String fileName,
                                  String expected) throws IOException {
        try {
            if (fs.exists(target)) {
                fs.delete(target);
                if (fs.exists(target)) {
                    throw new IOException(
                            "Could not replace existing cached model");
                }
            }
            fs.rename(temporary, fileName);
            if (!fs.exists(target)) {
                throw new IOException(
                        "Downloaded model could not be promoted to the cache");
            }
            if (!verify(target, expected)) {
                fs.delete(target);
                throw new IOException(
                        "Promoted model SHA-256 does not match");
            }
            if (fs.exists(temporary)) {
                fs.delete(temporary);
            }
        } catch (IOException error) {
            if (fs.exists(temporary)) {
                fs.delete(temporary);
            }
            throw error;
        }
    }

    static String safeName(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            boolean safe = (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9')
                    || c == '-' || c == '_';
            out.append(safe ? c : '_');
        }
        if (out.length() > MAX_READABLE_CACHE_KEY_LENGTH) {
            out.setLength(MAX_READABLE_CACHE_KEY_LENGTH);
        }
        Hash hash = Hash.create(Hash.SHA256);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            hash.update((byte) (c >>> 8));
            hash.update((byte) c);
        }
        out.append('-').append(Hash.toHex(hash.digest()).substring(0, 32));
        return out.toString();
    }

    private static boolean isSha256(String value) {
        if (value.length() != 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    static boolean isHttpsUrl(String url) {
        return url != null && url.regionMatches(true, 0,
                "https://", 0, "https://".length());
    }

    static boolean requiresPinnedModelDigest() {
        return "ios".equals(Display.getInstance().getPlatformName());
    }

    static FetchRegistration registerFetch(
            String fileName, String url, String sha256) {
        synchronized (ACTIVE_FETCHES) {
            ActiveFetch active = ACTIVE_FETCHES.get(fileName);
            if (active != null) {
                if (active.matches(url, sha256)) {
                    return new FetchRegistration(
                            subscribe(active.resource), null, false);
                }
                AsyncResource<ModelSource> conflict =
                        new AsyncResource<ModelSource>();
                new Completion<ModelSource>(conflict).fail(
                        new IOException("A different model download is "
                                + "already using cache key " + fileName));
                return new FetchRegistration(conflict, null, false);
            }
            AsyncResource<ModelSource> resource =
                    new AsyncResource<ModelSource>();
            Completion<ModelSource> completion =
                    new Completion<ModelSource>(resource, fileName);
            ACTIVE_FETCHES.put(fileName,
                    new ActiveFetch(url, sha256, resource));
            return new FetchRegistration(
                    subscribe(resource), completion, true);
        }
    }

    private static <T> AsyncResource<T> subscribe(
            AsyncResource<T> operation) {
        final SubscriberResource<T> subscriber =
                new SubscriberResource<T>();
        operation.ready(new SuccessCallback<T>() {
            @Override
            public void onSucess(T value) {
                subscriber.publish(value);
            }
        }).except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable error) {
                subscriber.fail(error);
            }
        });
        return subscriber;
    }

    private static void releaseFetch(
            String fileName, AsyncResource<?> resource) {
        if (fileName == null) {
            return;
        }
        synchronized (ACTIVE_FETCHES) {
            ActiveFetch active = ACTIVE_FETCHES.get(fileName);
            if (active != null && active.resource == resource) {
                ACTIVE_FETCHES.remove(fileName);
            }
        }
    }

    static final class ActiveFetch {
        private final String url;
        private final String sha256;
        private final AsyncResource<ModelSource> resource;

        ActiveFetch(String url, String sha256,
                    AsyncResource<ModelSource> resource) {
            this.url = url;
            this.sha256 = sha256;
            this.resource = resource;
        }

        boolean matches(String otherUrl, String otherSha256) {
            if (sha256 != null || otherSha256 != null) {
                return sha256 != null && otherSha256 != null
                        && sha256.equalsIgnoreCase(otherSha256);
            }
            return url.equals(otherUrl);
        }
    }

    private static final class SubscriberResource<T>
            extends AsyncResource<T> {
        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
        }

        synchronized void publish(T value) {
            if (!isCancelled()) {
                complete(value);
            }
        }

        synchronized void fail(Throwable error) {
            if (!isCancelled()) {
                error(error);
            }
        }
    }

    static final class FetchRegistration {
        final AsyncResource<ModelSource> resource;
        final Completion<ModelSource> completion;
        final boolean owner;

        FetchRegistration(AsyncResource<ModelSource> resource,
                          Completion<ModelSource> completion,
                          boolean owner) {
            this.resource = resource;
            this.completion = completion;
            this.owner = owner;
        }
    }

    static final class ModelDownloadRequest extends ConnectionRequest {
        private final Completion<ModelSource> completion;
        private final FileSystemStorage fs;
        private final String temporary;

        ModelDownloadRequest(Completion<ModelSource> completion,
                             FileSystemStorage fs, String temporary) {
            this.completion = completion;
            this.fs = fs;
            this.temporary = temporary;
        }

        /// Compares the inherited URL and request arguments together with the
        /// temporary destination path. Callback and storage-service instances
        /// do not change the logical identity of a download.
        ///
        /// @param other request to compare
        /// @return {@code true} when both requests download the same URL and
        ///         arguments to the same temporary path
        @Override
        public boolean equals(Object other) {
            if (!super.equals(other)) {
                return false;
            }
            ModelDownloadRequest request = (ModelDownloadRequest) other;
            return temporary == null ? request.temporary == null
                    : temporary.equals(request.temporary);
        }

        /// Produces a hash consistent with {@link #equals(Object)} from the
        /// inherited request identity and temporary destination.
        ///
        /// @return hash of the logical download identity
        @Override
        public int hashCode() {
            return 31 * super.hashCode()
                    + (temporary == null ? 0 : temporary.hashCode());
        }

        @Override
        public boolean onRedirect(String url) {
            if (isHttpsUrl(url)) {
                return false;
            }
            setKilled(true);
            if (fs.exists(temporary)) {
                fs.delete(temporary);
            }
            completion.fail(new IOException(
                    "Model download redirect must use HTTPS"));
            return true;
        }
    }

    static final class Completion<T> {
        private final AsyncResource<T> resource;
        private final String activeFileName;
        private boolean done;

        Completion(AsyncResource<T> resource) {
            this(resource, null);
        }

        Completion(AsyncResource<T> resource, String activeFileName) {
            this.resource = resource;
            this.activeFileName = activeFileName;
        }

        synchronized void complete(final T value) {
            if (done) {
                return;
            }
            done = true;
            releaseFetch(activeFileName, resource);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    resource.complete(value);
                }
            });
        }

        synchronized void fail(final Throwable error) {
            if (done) {
                return;
            }
            done = true;
            releaseFetch(activeFileName, resource);
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    resource.error(new InferenceException(
                            "Could not cache LiteRT model", error));
                }
            });
        }
    }
}
