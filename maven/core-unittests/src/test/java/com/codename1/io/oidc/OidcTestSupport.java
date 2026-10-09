/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** What the token tests share: a store in memory, token sets, and a wait that drives the EDT. */
public final class OidcTestSupport {

    private OidcTestSupport() {
    }

    /** Keeps one token set and counts what was asked of it. */
    public static final class MemoryStore implements TokenStore {
        public OidcTokens saved;
        public int clears;

        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> r = new AsyncResource<OidcTokens>();
            r.complete(saved);
            return r;
        }

        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            this.saved = tokens;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }

        public AsyncResource<Boolean> clear(String key) {
            this.saved = null;
            clears++;
            AsyncResource<Boolean> r = new AsyncResource<Boolean>();
            r.complete(Boolean.TRUE);
            return r;
        }
    }

    public static final class Outcome<T> {
        public final T value;
        public final Throwable error;

        Outcome(T value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }

    public static OidcTokens tokens(String access, String refresh) {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", access);
        json.put("token_type", "Bearer");
        json.put("expires_in", Integer.valueOf(3600));
        if (refresh != null) {
            json.put("refresh_token", refresh);
        }
        return OidcTokens.fromTokenResponse(json, null);
    }

    public static String tokenJson(String access, String refresh) {
        return "{\"access_token\":\"" + access + "\",\"token_type\":\"Bearer\",\"expires_in\":3600"
                + (refresh == null ? "" : ",\"refresh_token\":\"" + refresh + "\"") + "}";
    }

    public static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String text(byte[] b) {
        try {
            return b == null ? null : new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static TestCodenameOneImplementation impl() {
        return TestCodenameOneImplementation.getInstance();
    }

    /** Blocks, driving the EDT, until the resource settles or the budget runs out. */
    public static <T> Outcome<T> await(AsyncResource<T> resource) {
        final AtomicReference<T> value = new AtomicReference<T>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        final CountDownLatch latch = new CountDownLatch(1);
        resource.ready(new SuccessCallback<T>() {
            public void onSucess(T v) {
                value.set(v);
                latch.countDown();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
                latch.countDown();
            }
        });
        int budget = 20000;
        while (latch.getCount() > 0 && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 10;
        }
        if (latch.getCount() > 0) {
            throw new AssertionError("async resource did not settle within the timeout");
        }
        return new Outcome<T>(value.get(), error.get());
    }
}
