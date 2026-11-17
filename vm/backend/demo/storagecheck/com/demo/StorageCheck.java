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
package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.codename1.backend.Config;
import com.codename1.backend.Web;
import com.codename1.backend.aws.S3;
import com.codename1.backend.storage.ObjectInfo;
import com.codename1.backend.storage.ObjectStorage;
import com.codename1.backend.storage.ObjectStorages;
import com.codename1.backend.storage.azure.AzureBlobStorage;
import com.codename1.backend.storage.gcs.GcsStorage;

/// One contract, run against every object storage provider through the
/// provider-neutral API: each argument is a `cn1.storage.url`, optionally
/// followed by `key=value` configuration for it, and `--` separates providers.
///
/// ```
/// run-javase.sh com.demo.StorageCheck \
///     s3://us-east-1 cn1.storage.s3.endpoint=http://localhost:9000 \
///         cn1.storage.s3.access-key-id=minio cn1.storage.s3.secret-access-key=minio123 -- \
///     gs://demo cn1.storage.gcs.emulator-host=http://localhost:4443 -- \
///     azblob:// cn1.storage.azure.connection-string=UseDevelopmentStorage=true
/// ```
///
/// A signature cannot be tested against itself, so this exists to run against
/// servers that check one: MinIO and Azurite verify every signature and every
/// presigned URL. fake-gcs-server does not, which is why the GCS signer is held
/// to Google's conformance vectors in the unit tests as well.
public class StorageCheck {
    private static int passed;
    private static final List<String> failures = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        ObjectStorages.register("s3", new S3.Factory());
        ObjectStorages.register("r2", new S3.Factory());
        ObjectStorages.register("gs", new GcsStorage.Factory());
        ObjectStorages.register("azblob", new AzureBlobStorage.Factory());
        int at = 0;
        while (at < args.length) {
            String url = args[at++];
            Properties p = new Properties();
            while (at < args.length && !"--".equals(args[at])) {
                int eq = args[at].indexOf('=');
                p.setProperty(args[at].substring(0, eq), args[at].substring(eq + 1));
                at++;
            }
            at++;
            try {
                contract(url, ObjectStorages.open(url, Config.of(p, null)));
            } catch (Exception err) {
                failures.add(url + ": " + err);
            }
        }
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for (String f : failures) {
            System.out.println("FAIL " + f);
        }
        System.out.println(failures.isEmpty() ? "STORAGECHECK OK" : "STORAGECHECK FAILED");
        if (!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void contract(String url, ObjectStorage s) throws Exception {
        String tag = url + " (" + s.getName() + ")";
        String bucket = "cn1-storagecheck";
        s.createBucket(bucket);
        s.createBucket(bucket);
        check(tag, "creating an owned bucket twice is fine", true, true);

        byte[] body = "hello object storage".getBytes("UTF-8");
        String etag = s.putObject(bucket, "dir/a b+c.txt", body, "text/plain");
        check(tag, "put returns an etag", true, etag.length() > 0);
        check(tag, "get returns the bytes", new String(body, "UTF-8"),
                new String(s.getObject(bucket, "dir/a b+c.txt"), "UTF-8"));
        ObjectInfo info = s.headObject(bucket, "dir/a b+c.txt");
        check(tag, "head reports the size", (long) body.length, info.getSize());
        check(tag, "head reports the type", true,
                info.getContentType() != null && info.getContentType().startsWith("text/plain"));
        check(tag, "head of a missing key is null", null, s.headObject(bucket, "nope"));

        s.putObject(bucket, "dir/2", new byte[] {1}, null);
        s.putObject(bucket, "dir/3", new byte[] {2}, null);
        s.putObject(bucket, "other", new byte[] {3}, null);
        check(tag, "list honours the prefix", 3, s.listObjects(bucket, "dir/", 100).size());
        check(tag, "list honours max", 2, s.listObjects(bucket, "dir/", 2).size());

        String put = s.presignPut(bucket, "presigned.bin", 300);
        List<String> headers = new ArrayList<String>();
        headers.add("x-ms-blob-type: BlockBlob");
        headers.add("Content-Type: application/octet-stream");
        Web.Result up = Web.request("PUT", put, headers, new byte[] {9, 8, 7});
        check(tag, "a presigned PUT is accepted (" + up.getStatus() + " "
                + snippet(up) + ")", true, up.isSuccess());
        String get = s.presignGet(bucket, "presigned.bin", 300);
        Web.Result down = Web.request("GET", get, null, null);
        check(tag, "a presigned GET is accepted (" + down.getStatus() + " "
                + snippet(down) + ")", true, down.isSuccess());
        check(tag, "and returns what the presigned PUT stored", 3,
                down.getBody() == null ? -1 : down.getBody().length);
        Web.Result tampered = Web.request("GET", get.replace("presigned.bin", "other"), null,
                null);
        check(tag, "a presigned URL does not open another key", false, tampered.isSuccess()
                && !s.getName().equals("gcs"));

        for (String k : s.listObjects(bucket, null, 100)) {
            s.deleteObject(bucket, k);
        }
        s.deleteObject(bucket, "other");
        check(tag, "delete empties the bucket", 0, s.listObjects(bucket, null, 100).size());
    }

    private static String snippet(Web.Result r) {
        String b = r.getBodyAsString();
        return b == null ? "" : b.length() > 160 ? b.substring(0, 160) : b;
    }

    private static void check(String tag, String what, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add(tag + ": " + what + ": expected " + expected + " but was " + actual);
        }
    }
}
