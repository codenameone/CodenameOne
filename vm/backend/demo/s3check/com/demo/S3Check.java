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
package com.demo;

import java.util.ArrayList;
import java.util.List;

import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import com.codename1.backend.aws.S3;

/**
 * Exercises SigV4 and the S3 client against a REAL S3-compatible server.
 *
 * Signature code cannot be tested against itself. A wrong canonical form -- a
 * misencoded space, an unsorted query parameter, a header case -- produces a
 * signature this code agrees with completely and the service rejects with a bare
 * 403. So the checks below run against a server (MinIO locally, and any
 * S3-compatible endpoint in CI) and the KNOWN-ANSWER vectors from the AWS
 * documentation run everywhere, because those pin the canonical form itself.
 *
 * Point it at a server with CN1_S3CHECK_ENDPOINT / _KEY / _SECRET / _BUCKET.
 * Without one only the known-answer vectors run.
 */
public class S3Check {
    private static int passed;
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        knownAnswers();
        liveServer();

        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "S3CHECK OK" : "S3CHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    /**
     * The vectors AWS publishes for SigV4, which fix the canonical form
     * independently of any server. These are the checks that say WHICH part is
     * wrong when a live request comes back 403.
     */
    private static void knownAnswers() throws Exception {
        // The documented derivation for the key AWS uses in its own examples.
        byte[] key = Aws.signingKey("wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY",
                "20150830", "us-east-1", "iam");
        check("the signing key matches the published vector",
                "c4afb1cc5771d871763a393e44b703571b55cc28424d1a5e86da6ed3c154a4b9",
                Aws.hex(key));

        // Percent-encoding: a space is %20 and never '+', '~' is left alone, and
        // the hex is upper case. All three differ from URLEncoder, and each one on
        // its own is a 403.
        check("a space encodes as %20", "a%20b", Aws.encode("a b"));
        check("a tilde is not encoded", "~", Aws.encode("~"));
        check("a slash inside a segment is encoded", "a%2Fb", Aws.encode("a/b"));
        check("a slash between segments is not", "/a/b%20c", Aws.encodePath("/a/b c"));
        check("non-ASCII is UTF-8 percent encoded", "%C3%A9", Aws.encode("\u00e9"));

        // Query parameters are sorted by their ENCODED name.
        java.util.Map query = new java.util.LinkedHashMap();
        query.put("marker", "b");
        query.put("acl", "");
        query.put("Prefix", "a b");
        check("query parameters are sorted and encoded",
                "Prefix=a%20b&acl=&marker=b", Aws.canonicalQuery(query));

        check("an empty body hashes to the documented value",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                Aws.sha256Hex(new byte[0]));

        // Header values have their internal whitespace collapsed.
        check("header whitespace is collapsed", "a b", Aws.collapse("  a    b  "));
    }

    private static void liveServer() throws Exception {
        String endpoint = System.getenv("CN1_S3CHECK_ENDPOINT");
        if(endpoint == null || endpoint.length() == 0) {
            System.out.println("NOTE live S3 checks skipped: set CN1_S3CHECK_ENDPOINT");
            return;
        }
        String bucket = System.getenv("CN1_S3CHECK_BUCKET");
        if(bucket == null || bucket.length() == 0) {
            bucket = "cn1-backend-check";
        }
        Credentials credentials = new Credentials(System.getenv("CN1_S3CHECK_KEY"),
                System.getenv("CN1_S3CHECK_SECRET"), null);
        S3 s3 = S3.forEndpoint(credentials, System.getenv("CN1_S3CHECK_REGION"), endpoint);
        System.out.println("checking s3://" + bucket + " at " + endpoint);
        s3.createBucket(bucket);

        String key = "folder/an object with spaces & symbols.txt";
        byte[] content = "hello from the backend".getBytes("UTF-8");

        String etag = s3.putObject(bucket, key, content, "text/plain");
        check("a put returns an etag", "true", String.valueOf(etag.length() > 0));

        byte[] fetched = s3.getObject(bucket, key);
        check("the object round trips", new String(content, "UTF-8"),
                new String(fetched, "UTF-8"));

        S3.ObjectInfo info = s3.headObject(bucket, key);
        check("head reports the size", String.valueOf(content.length),
                String.valueOf(info.getSize()));
        check("head reports the content type", "text/plain", info.getContentType());

        check("head on a missing key is null", "null",
                String.valueOf(s3.headObject(bucket, "no/such/key")));

        List listed = s3.listObjects(bucket, "folder/", 100);
        check("the key is listed", "true", String.valueOf(listed.contains(key)));

        // A key outside the BMP, round-tripped through a real listing.
        //
        // Note what this does NOT prove. The entity decoder truncated a numeric
        // reference above U+FFFF by casting it to char, and MinIO returns this key
        // as raw UTF-8 rather than as &#x1F600; -- so this check passes with that
        // bug present. It was written to prove the fix and does not; it is kept
        // because a supplementary key surviving put/list end to end is worth
        // holding on to, and the next reader should not mistake it for coverage of
        // the entity path. That path is package-private and no server here emits
        // it, so the narrowing fix rests on language semantics instead: a cast to
        // char keeps the low 16 bits, which is not a judgement call.
        String emoji = "folder/grinning-" + new String(Character.toChars(0x1F600)) + ".txt";
        s3.putObject(bucket, emoji, content, "text/plain");
        List withEmoji = s3.listObjects(bucket, "folder/", 100);
        check("a supplementary code point survives the listing", "true",
                String.valueOf(withEmoji.contains(emoji)));
        s3.deleteObject(bucket, emoji);

        // A presigned URL is the whole point of this for a mobile client: it must
        // work with NO credentials on the request.
        String url = s3.presignGet(bucket, key, 300);
        Web.Result direct = Web.request("GET", url, null, null);
        check("a presigned GET works unauthenticated", "200",
                String.valueOf(direct.getStatus()));
        check("a presigned GET returns the object", new String(content, "UTF-8"),
                direct.getBodyAsString());

        // ...and must stop working when tampered with, or it is not a signature.
        Web.Result tampered = Web.request("GET", url.substring(0, url.length() - 1) + "0",
                null, null);
        check("a tampered presigned URL is rejected", "true",
                String.valueOf(tampered.getStatus() >= 400));

        s3.deleteObject(bucket, key);
        check("the object is gone after delete", "null",
                String.valueOf(s3.headObject(bucket, key)));
    }

    private static void check(String name, String expected, String actual) {
        if(expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
