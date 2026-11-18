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
package com.codename1.backend.storage.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/// Azure's signers held to strings written out from Microsoft's documentation
/// ("Authorize with Shared Key", "Create a service SAS") and signed here with
/// the JDK's own HMAC, so the canonicalization is what is under test. Azurite,
/// which checks both signatures for real, is the end-to-end half
/// (vm/backend/demo/storagecheck).
class AzureBlobSigningTest {
    private static final byte[] KEY = Base64.decode("a2V5LWZvci10ZXN0cy1vbmx5LW5vdC1hLXNlY3JldA==");

    private static String hmac(String text) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(KEY, "HmacSHA256"));
        return Base64.encode(mac.doFinal(text.getBytes("UTF-8")));
    }

    @Test
    void sharedKeyCanonicalizesHeadersAndResource() throws Exception {
        List<String> headers = new ArrayList<String>();
        headers.add("x-ms-blob-type: BlockBlob");
        headers.add("Content-Type: text/plain");
        headers.add("X-MS-Date: Mon, 16 Nov 2026 12:00:00 GMT");
        headers.add("x-ms-version: 2021-08-06");
        String expected = "PUT\n\n\n5\n\ntext/plain\n\n\n\n\n\n\n"
                + "x-ms-blob-type:BlockBlob\n"
                + "x-ms-date:Mon, 16 Nov 2026 12:00:00 GMT\n"
                + "x-ms-version:2021-08-06\n"
                + "/myaccount/photos/a%20b.png";
        assertEquals(hmac(expected), AzureBlobStorage.sharedKey(KEY, "myaccount", "PUT",
                "/photos/a%20b.png", null, headers, 5));
    }

    @Test
    void sharedKeySignsAZeroLengthAsEmptyAndSortsTheQuery() throws Exception {
        List<String> headers = new ArrayList<String>();
        headers.add("x-ms-version: 2021-08-06");
        Map<String, String> query = new TreeMap<String, String>();
        query.put("restype", "container");
        query.put("comp", "list");
        query.put("prefix", "a b");
        String expected = "GET\n\n\n\n\n\n\n\n\n\n\n\nx-ms-version:2021-08-06\n"
                + "/myaccount/photos\ncomp:list\nprefix:a b\nrestype:container";
        assertEquals(hmac(expected), AzureBlobStorage.sharedKey(KEY, "myaccount", "GET",
                "/photos", query, headers, 0));
    }

    @Test
    void serviceSasSignsTheDocumentedFields() throws Exception {
        AzureBlobStorage storage = AzureBlobStorage.forAccountKey("myaccount",
                Base64.encode(KEY));
        long now = (com.codename1.impl.backend.CivilTime.daysFromCivil(2026, 11, 23) * 86400L
                + 12 * 3600L) * 1000L;
        String url = storage.presign("r", "photos", "dir/a b.png", 3600, now);
        String toSign = "r\n\n2026-11-23T13:00:00Z\n/blob/myaccount/photos/dir/a b.png\n\n\n"
                + "https\n" + AzureBlobStorage.VERSION + "\nb\n\n\n\n\n\n\n";
        String sig = hmac(toSign);
        assertTrue(url.startsWith("https://myaccount.blob.core.windows.net/photos/dir/a%20b.png?"),
                url);
        assertTrue(url.contains("se=2026-11-23T13%3A00%3A00Z"), url);
        assertTrue(url.contains("sp=r&spr=https&sr=b&sv=" + AzureBlobStorage.VERSION), url);
        assertTrue(url.contains("sig=" + com.codename1.backend.aws.Aws.encode(sig)), url);
    }

    @Test
    void azuriteConnectionStringAddressesTheAccountPathStyle() throws Exception {
        AzureBlobStorage storage = AzureBlobStorage.forConnectionString(
                "UseDevelopmentStorage=true");
        String url = storage.presign("r", "photos", "a.png", 60, 0);
        assertTrue(url.startsWith("http://127.0.0.1:10000/devstoreaccount1/photos/a.png?"), url);
        assertTrue(url.contains("spr=https%2Chttp"), url);
    }

    @Test
    void connectionStringBuildsTheEndpointFromItsSuffix() throws Exception {
        AzureBlobStorage storage = AzureBlobStorage.forConnectionString(
                "DefaultEndpointsProtocol=https;AccountName=acct1;AccountKey="
                + Base64.encode(KEY) + ";EndpointSuffix=core.chinacloudapi.cn");
        assertTrue(storage.presign("r", "box", "k", 60, 0)
                .startsWith("https://acct1.blob.core.chinacloudapi.cn/box/k?"));
    }

    @Test
    void refusesNamesThatWouldMoveTheRequest() {
        assertThrows(IllegalArgumentException.class,
                () -> AzureBlobStorage.forAccountKey("evil.example/x", "AAAA"));
        AzureBlobStorage storage = AzureBlobStorage.forAccountKey("myaccount", "AAAA");
        assertThrows(IllegalArgumentException.class,
                () -> storage.presign("r", "Up", "k", 60, 0));
        assertThrows(IllegalArgumentException.class,
                () -> storage.presign("r", "a--b", "k", 60, 0));
    }

    @Test
    void listedNamesAreReadWithAndWithoutEncoding() throws Exception {
        String xml = "<Blobs><Blob><Name>a&amp;b</Name></Blob>"
                + "<Blob><Name Encoded=\"true\">x%01y%2B%20</Name></Blob>"
                + "<Blob><NameSpace>no</NameSpace><Name>c+d</Name></Blob>"
                + "<Blob><Name/></Blob></Blobs>";
        List<String> names = new ArrayList<String>();
        for (String[] t : com.codename1.impl.backend.FlatXml.tagged(xml, "Name")) {
            names.add(AzureBlobStorage.blobName(t[0], t[1]));
        }
        assertEquals(java.util.Arrays.asList("a&b", "x\u0001y+ ", "c+d", ""), names);
        assertThrows(java.io.IOException.class,
                () -> AzureBlobStorage.blobName("Encoded=\"true\"", "bad%zz"));
    }
}
