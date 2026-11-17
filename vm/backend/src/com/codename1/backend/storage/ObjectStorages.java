/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.storage;

import com.codename1.backend.Config;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/// The server's [ObjectStorage], opened from `cn1.storage.url` the first time it
/// is asked for.
///
/// | URL | provider |
/// |---|---|
/// | `memory:` | in this process, for tests; the default when nothing is set |
/// | `s3://us-east-1` | Amazon S3 in that region (`s3://` alone reads `AWS_REGION`) |
/// | `s3://us-east-1` with `cn1.storage.s3.endpoint` | an S3-compatible service: MinIO, Ceph, B2, Wasabi |
/// | `r2://<account id>` | Cloudflare R2 |
/// | `gs://` or `gs://<project>` | Google Cloud Storage |
/// | `azblob://<account>` | Azure Blob Storage |
///
/// Each provider is a separate class the build compiles in only when the
/// configuration names it, the way messaging transports are: the generated
/// wiring calls [#register] for that one. A test or a hand-wired server
/// registers the factory itself.
public final class ObjectStorages {
    private static final String RESOURCE = "cn1.storage";
    private static final Map<String, Factory> FACTORIES = new HashMap<String, Factory>();

    private ObjectStorages() {
    }

    /// Opens a provider from its URL.
    public interface Factory {
        /// The storage `url` names, configured from `config` (which may be null).
        ObjectStorage open(String url, Config config) throws IOException;
    }

    /// Makes `scheme` (`s3`, `r2`, `gs`, `azblob`) open through `factory`.
    public static void register(String scheme, Factory factory) {
        synchronized (FACTORIES) {
            FACTORIES.put(scheme, factory);
        }
    }

    /// The calling thread's server's storage, opened on first use.
    public static ObjectStorage current() throws IOException {
        return (ObjectStorage) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return ObjectStorages.open(config);
                    }
                });
    }

    /// `cn1.storage.url`, trimmed, or null when it is not set.
    public static String url(Config config) throws IOException {
        if (config == null) {
            return null;
        }
        String url = config.get("cn1.storage.url");
        return url == null || url.trim().length() == 0 ? null : url.trim();
    }

    /// Whether the configuration names a real provider rather than the
    /// in-memory store.
    public static boolean isConfigured(Config config) throws IOException {
        String url = url(config);
        return url != null && !url.startsWith("memory:");
    }

    /// The storage the configuration names; the in-memory store when it names
    /// none.
    public static ObjectStorage open(Config config) throws IOException {
        String url = url(config);
        return open(url == null ? "memory:" : url, config);
    }

    /// The storage `url` names, configured from `config` (which may be null).
    public static ObjectStorage open(String url, Config config) throws IOException {
        if (url.startsWith("memory:")) {
            return new MemoryObjectStorage();
        }
        int colon = url.indexOf(':');
        String scheme = colon < 0 ? url : url.substring(0, colon);
        Factory factory;
        synchronized (FACTORIES) {
            factory = FACTORIES.get(scheme);
            if (factory == null) {
                // Schemes are case-insensitive. equalsIgnoreCase, never
                // toLowerCase: the latter is locale sensitive.
                for (Map.Entry<String, Factory> e : FACTORIES.entrySet()) {
                    if (e.getKey().equalsIgnoreCase(scheme)) {
                        factory = e.getValue();
                        break;
                    }
                }
            }
        }
        if (factory == null) {
            throw new IOException("cn1.storage.url \"" + url + "\" names no storage "
                    + "provider this server was built with. The build compiles in the "
                    + "provider the configuration names (s3://, r2://, gs://, azblob://); "
                    + "a server wired by hand registers it with ObjectStorages.register");
        }
        return factory.open(url, config);
    }

    /// A configuration value, else the environment variable `env` (which may
    /// be null), else null. Shared by the providers' factories.
    public static String setting(Config config, String key, String env) throws IOException {
        String value = config == null ? null : config.get(key);
        if ((value == null || value.trim().length() == 0) && env != null) {
            value = System.getenv(env);
        }
        return value == null || value.trim().length() == 0 ? null : value.trim();
    }

    /// The part of `url` after `scheme://`, up to any slash; empty when there
    /// is none.
    public static String authority(String url) {
        int at = url.indexOf("://");
        if (at < 0) {
            return "";
        }
        String rest = url.substring(at + 3);
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
