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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.aws.S3;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ObjectStoragesTest {
    private static Config config(String... pairs) {
        Properties p = new Properties();
        for (int i = 0; i < pairs.length; i += 2) {
            p.setProperty(pairs[i], pairs[i + 1]);
        }
        return Config.of(p, null);
    }

    @Test
    void defaultsToMemory() throws Exception {
        ObjectStorage storage = ObjectStorages.open(config());
        assertEquals("memory", storage.getName());
        storage.createBucket("b");
        storage.putObject("b", "x/1", "one".getBytes("UTF-8"), null);
        storage.putObject("b", "x/2", "two".getBytes("UTF-8"), "text/plain");
        storage.putObject("b", "y", new byte[0], null);
        assertArrayEquals("two".getBytes("UTF-8"), storage.getObject("b", "x/2"));
        assertEquals("text/plain", storage.headObject("b", "x/2").getContentType());
        assertEquals(3, storage.headObject("b", "x/1").getSize());
        assertEquals(Arrays.asList("x/1", "x/2"), storage.listObjects("b", "x/", 10));
        assertEquals(Collections.singletonList("x/1"), storage.listObjects("b", null, 1));
        assertEquals(Collections.emptyList(), storage.listObjects("b", null, 0));
        storage.deleteObject("b", "x/1");
        storage.deleteObject("b", "x/1");
        assertNull(storage.headObject("b", "x/1"));
        assertThrows(IOException.class, () -> storage.getObject("b", "x/1"));
        assertThrows(IOException.class, () -> storage.presignGet("b", "y", 60));
        // A misnamed bucket reads as an absent object, as a provider's 404 does.
        assertNull(storage.headObject("nope", "x"));
        storage.deleteObject("nope", "x");
        // Listing order is UTF-8 (code point) order, as every provider lists:
        // U+1F600 sorts after U+FF21, though its UTF-16 surrogates sort before.
        storage.putObject("b", "z\uff21", new byte[0], null);
        storage.putObject("b", "z\ud83d\ude00", new byte[0], null);
        assertEquals(Arrays.asList("z\uff21", "z\ud83d\ude00"), storage.listObjects("b", "z", 10));
    }

    @Test
    void anUnregisteredSchemeSaysHowToFixIt() {
        IOException err = assertThrows(IOException.class,
                () -> ObjectStorages.open("nosuch://x", config()));
        assertTrue(err.getMessage().contains("ObjectStorages.register"), err.getMessage());
    }

    @Test
    void r2IsTheAccountsS3CompatibleEndpoint() throws Exception {
        ObjectStorages.register("r2", new S3.Factory());
        ObjectStorage storage = ObjectStorages.open("R2://0123abcd", config(
                "cn1.storage.r2.access-key-id", "AKID",
                "cn1.storage.r2.secret-access-key", "secret"));
        String url = storage.presignGet("photos", "a.png", 60);
        assertTrue(url.startsWith("https://0123abcd.r2.cloudflarestorage.com/photos/a.png?"), url);
        assertTrue(url.contains("%2Fauto%2Fs3%2Faws4_request"), url);
    }

    @Test
    void r2RefusesAnAccountIdThatIsNotOneHostLabel() {
        ObjectStorages.register("r2", new S3.Factory());
        assertThrows(IOException.class, () -> ObjectStorages.open("r2://evil.example", config(
                "cn1.storage.r2.access-key-id", "AKID",
                "cn1.storage.r2.secret-access-key", "secret")));
    }

    @Test
    void s3WithAnEndpointIsPathStyle() throws Exception {
        ObjectStorages.register("s3", new S3.Factory());
        ObjectStorage storage = ObjectStorages.open("s3://us-east-1", config(
                "cn1.storage.s3.endpoint", "http://localhost:9000",
                "cn1.storage.s3.access-key-id", "AKID",
                "cn1.storage.s3.secret-access-key", "secret"));
        assertTrue(storage.presignGet("photos", "a.png", 60)
                .startsWith("http://localhost:9000/photos/a.png?"));
    }

    @Test
    void s3WithExplicitKeysIsVirtualHosted() throws Exception {
        ObjectStorages.register("s3", new S3.Factory());
        ObjectStorage storage = ObjectStorages.open("s3://eu-west-1", config(
                "cn1.storage.s3.access-key-id", "AKID",
                "cn1.storage.s3.secret-access-key", "secret"));
        assertTrue(storage.presignGet("photos", "a.png", 60)
                .startsWith("https://photos.s3.eu-west-1.amazonaws.com/a.png?"));
    }
}
