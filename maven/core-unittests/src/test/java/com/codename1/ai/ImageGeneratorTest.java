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

import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.testing.TestCodenameOneImplementation.TestConnection;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Image;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for {@link ImageGenerator}: the {@code openai} generator's
 * request-body construction and its {@code postResponse} parse / error
 * branches (driven over the mock network), plus the synchronous
 * unsupported-operation paths of {@code onDevice} and {@code replicate}.
 *
 * <p>The actual image-decode success path needs a platform image codec and is
 * intentionally not asserted here; the parse-failure branches give equivalent
 * coverage of the response handling.
 */
class ImageGeneratorTest extends UITestBase {

    private static final String URL = "https://api.openai.com/v1/images/generations";

    @AfterEach
    void clearMocks() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
        TestCodenameOneImplementation.getInstance().clearConnections();
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private void mock(int code, String body) {
        TestCodenameOneImplementation.getInstance()
                .addNetworkMockResponse(URL, code, code == 200 ? "OK" : "ERR", utf8(body));
    }

    private static final class Outcome {
        final Image value;
        final Throwable error;

        Outcome(Image value, Throwable error) {
            this.value = value;
            this.error = error;
        }
    }

    private Outcome await(AsyncResource<Image> r) {
        final AtomicReference<Image> value = new AtomicReference<Image>();
        final AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        r.ready(new SuccessCallback<Image>() {
            public void onSucess(Image v) {
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
        assertTrue(r.isDone(), "image request did not settle within the timeout");
        return new Outcome(value.get(), error.get());
    }

    @Test
    void onDeviceFailsWithUnsupportedOperation() {
        Outcome r = await(ImageGenerator.onDevice().generate(new GenerateImageRequest("a cat")));
        assertNull(r.value);
        assertTrue(r.error instanceof UnsupportedOperationException);
    }

    @Test
    void replicateFailsWithUnsupportedOperation() {
        Outcome r = await(ImageGenerator.replicate("token").generate(new GenerateImageRequest("a cat")));
        assertNull(r.value);
        assertTrue(r.error instanceof UnsupportedOperationException);
    }

    @Test
    void openAiEmptyDataYieldsInvalidRequest() {
        mock(200, "{\"model\":\"dall-e-3\"}");
        Outcome r = await(ImageGenerator.openai("k").generate(new GenerateImageRequest("a cat")));
        assertNull(r.value);
        assertTrue(r.error instanceof LlmException);
        LlmException ex = (LlmException) r.error;
        assertEquals(LlmException.ErrorType.INVALID_REQUEST, ex.getType());
        assertTrue(ex.getMessage().contains("Empty data"));
    }

    @Test
    void openAiMissingB64YieldsInvalidRequest() {
        mock(200, "{\"data\":[{\"revised_prompt\":\"a cat\"}]}");
        Outcome r = await(ImageGenerator.openai("k").generate(new GenerateImageRequest("a cat")));
        assertNull(r.value);
        assertTrue(r.error instanceof LlmException);
        assertEquals(LlmException.ErrorType.INVALID_REQUEST, ((LlmException) r.error).getType());
        assertTrue(r.error.getMessage().contains("b64_json"));
    }

    @Test
    void openAiBuildsExpectedRequestBodyAndAuthHeader() {
        mock(200, "{\"model\":\"dall-e-3\"}");
        await(ImageGenerator.openai("test-key")
                .generate(new GenerateImageRequest("a sunset").setSize("1792x1024")
                        .setStyle("vivid").setQuality("hd")));

        TestConnection conn = TestCodenameOneImplementation.getInstance().getConnection(URL);
        assertNotNull(conn, "the generator should have opened a connection to the images endpoint");
        String body = new String(conn.getOutputData());
        assertTrue(body.contains("\"prompt\""), body);
        assertTrue(body.contains("a sunset"), body);
        assertTrue(body.contains("dall-e-3"), body);
        assertTrue(body.contains("1792x1024"), body);
        assertTrue(body.contains("b64_json"), body);
        assertTrue(body.contains("vivid"), body);
        assertTrue(body.contains("hd"), body);
        assertEquals("Bearer test-key", conn.getHeaders().get("Authorization"));
    }

    @Test
    void openAiOmitsAuthHeaderWhenKeyBlank() {
        mock(200, "{\"model\":\"dall-e-3\"}");
        await(ImageGenerator.openai("").generate(new GenerateImageRequest("a cat")));
        TestConnection conn = TestCodenameOneImplementation.getInstance().getConnection(URL);
        assertNotNull(conn);
        assertNull(conn.getHeaders().get("Authorization"));
    }
}
