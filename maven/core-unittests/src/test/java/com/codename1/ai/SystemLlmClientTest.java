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
package com.codename1.ai;

import com.codename1.impl.SystemLlmImpl;
import com.codename1.junit.UITestBase;
import com.codename1.ui.DisplayTest;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link SystemLlmClient} over a scripted port backend: status mapping, the
 * unsupported path, and a streamed task assembled into one result.
 */
class SystemLlmClientTest extends UITestBase {

    /** Answers every feature with one status and streams fixed pieces. */
    private static final class FakeImpl extends SystemLlmImpl {
        int status = STATUS_AVAILABLE;
        int lastFeature = -1;
        int lastOption = -1;
        String lastText;
        private String[] pending;
        private int index;

        @Override
        public String getProvider() {
            return "fake";
        }

        @Override
        public int getStatus(int feature) {
            return feature == FEATURE_DESCRIBE_IMAGE ? STATUS_UNSUPPORTED : status;
        }

        @Override
        public String getStatusReason(int feature) {
            return status == STATUS_AVAILABLE ? null : "scripted";
        }

        @Override
        public boolean download(int feature) {
            return true;
        }

        @Override
        public synchronized int start(int feature, String[] roles, String[] contents, byte[] image,
                int option, int maxTokens, float temperature, int seed) {
            lastFeature = feature;
            lastOption = option;
            lastText = contents.length == 0 ? null : contents[contents.length - 1];
            pending = new String[] {"Hel", "lo"};
            index = 0;
            return 7;
        }

        @Override
        public synchronized String next(int handle) {
            return index < pending.length ? pending[index++] : null;
        }

        @Override
        public String getLastError(int handle) {
            return null;
        }

        @Override
        public void close(int handle) {
        }
    }

    @AfterEach
    void clearBackend() {
        implementation.setSystemLlmImpl(null);
    }

    @Test
    void withoutABackendEverythingIsUnsupported() {
        assertFalse(SystemLlmClient.isSupported());
        for (SystemLlmFeature f : SystemLlmFeature.values()) {
            assertEquals(SystemLlmStatus.UNSUPPORTED, SystemLlmClient.getStatus(f));
        }
        assertEquals(0, SystemLlmClient.getSupportedFeatures().length);
        SystemLlmClient c = LlmClient.system();
        try {
            assertEquals("none", c.getProvider());
            Throwable err = await(c.summarize("text", 2)).error;
            assertTrue(err instanceof LlmException, String.valueOf(err));
        } finally {
            c.close();
        }
    }

    @Test
    void statusesMapAndUnsupportedFeaturesAreLeftOut() {
        FakeImpl impl = new FakeImpl();
        impl.status = SystemLlmImpl.STATUS_DOWNLOADABLE;
        implementation.setSystemLlmImpl(impl);
        assertEquals(SystemLlmStatus.DOWNLOADABLE, SystemLlmClient.getStatus(SystemLlmFeature.CHAT));
        assertFalse(SystemLlmClient.getStatus(SystemLlmFeature.CHAT).isAvailable());
        assertEquals("scripted", SystemLlmClient.getStatusReason(SystemLlmFeature.CHAT));
        SystemLlmFeature[] supported = SystemLlmClient.getSupportedFeatures();
        assertEquals(SystemLlmFeature.values().length - 1, supported.length);
        for (SystemLlmFeature f : supported) {
            assertNotEquals(SystemLlmFeature.DESCRIBE_IMAGE, f);
        }
    }

    @Test
    void streamedPiecesAssembleIntoTheResult() {
        FakeImpl impl = new FakeImpl();
        implementation.setSystemLlmImpl(impl);
        assertTrue(SystemLlmClient.isSupported());
        SystemLlmClient c = LlmClient.system();
        try {
            assertEquals("fake", c.getProvider());
            Outcome o = await(c.rewrite("make it short", RewriteStyle.SHORTEN));
            assertNull(o.error);
            assertEquals("Hello", o.value);
            assertEquals(SystemLlmImpl.FEATURE_REWRITE, impl.lastFeature);
            assertEquals(RewriteStyle.SHORTEN.ordinal(), impl.lastOption);
            assertEquals("make it short", impl.lastText);
        } finally {
            c.close();
        }
    }

    private static final class Outcome {
        final String value;
        final Throwable error;

        Outcome(String value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }

    private Outcome await(AsyncResource<String> r) {
        final AtomicReference<String> value = new AtomicReference<String>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        r.ready(new SuccessCallback<String>() {
            public void onSucess(String v) {
                value.set(v);
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                error.set(t);
            }
        });
        int budget = 20000;
        while (!r.isDone() && budget > 0) {
            DisplayTest.flushEdt();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            budget -= 10;
        }
        DisplayTest.flushEdt();
        assertTrue(r.isDone(), "request did not settle within the timeout");
        return new Outcome(value.get(), error.get());
    }
}
