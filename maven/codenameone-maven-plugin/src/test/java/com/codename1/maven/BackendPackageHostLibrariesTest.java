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
package com.codename1.maven;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The packaging goal has to find the TLS and HTTP/2 headers where the machine
/// keeps them.
///
/// macOS ships libcrypto without its headers, so `cn1:backend-package` on a
/// stock machine with Homebrew's OpenSSL failed on openssl/ssl.h and the only way
/// out was to work out cn1.backend.cflags by hand. vm/backend/build.sh has always
/// probed for the prefixes; this is the same probe, so the two ways of building a
/// backend look in the same places.
class BackendPackageHostLibrariesTest {

    @Test
    void takesTheFirstPrefixThatCarriesTheHeaders(@TempDir File tmp) throws Exception {
        File preferred = prefixWith(tmp, "preferred", "include/openssl/sha.h");
        File fallback = prefixWith(tmp, "fallback", "include/openssl/sha.h");
        File h2 = prefixWith(tmp, "nghttp2", "include/nghttp2/nghttp2.h");

        List<String> flags = BackendPackageMojo.hostLibraryFlags(
                Arrays.asList("", null, preferred.getAbsolutePath(),
                        fallback.getAbsolutePath()),
                Collections.singletonList(h2.getAbsolutePath()));

        assertEquals(Arrays.asList(
                "-I" + new File(preferred, "include").getAbsolutePath(),
                "-L" + new File(preferred, "lib").getAbsolutePath(),
                "-I" + new File(h2, "include").getAbsolutePath(),
                "-L" + new File(h2, "lib").getAbsolutePath()), flags,
                "the first prefix that carries the header wins, and an empty or "
                        + "null entry is skipped rather than probed");
    }

    @Test
    void addsNothingWhenTheHeadersAreWhereClangLooks(@TempDir File tmp) throws Exception {
        // A Linux box with the distribution's -dev package: none of the probes
        // match and the command is left exactly as it was.
        File bare = new File(tmp, "empty-prefix");
        assertTrue(bare.mkdirs());

        List<String> flags = BackendPackageMojo.hostLibraryFlags(
                Collections.singletonList(bare.getAbsolutePath()),
                Collections.singletonList(bare.getAbsolutePath()));

        assertTrue(flags.isEmpty(), "a prefix without the header must add nothing: " + flags);
    }

    @Test
    void toleratesNoCandidatesAtAll() {
        assertTrue(BackendPackageMojo.hostLibraryFlags(null, null).isEmpty());
    }

    private static File prefixWith(File tmp, String name, String probe) throws Exception {
        File prefix = new File(tmp, name);
        File header = new File(prefix, probe);
        assertTrue(header.getParentFile().mkdirs());
        assertTrue(header.createNewFile());
        return prefix;
    }
}
