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

import com.codename1.impl.InferenceImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class InferenceApiTest extends UITestBase {
    @Test
    void tensorsAreImmutableAndValidateShape() {
        float[] source = new float[] {1, 2};
        Tensor tensor = Tensor.floats("input", new int[] {1, 2}, source);
        source[0] = 9;
        assertArrayEquals(new float[] {1, 2}, (float[]) tensor.getData());
        float[] returned = (float[]) tensor.getData();
        returned[1] = 9;
        assertArrayEquals(new float[] {1, 2}, (float[]) tensor.getData());
        assertThrows(IllegalArgumentException.class, () ->
                Tensor.floats("bad", new int[] {3}, new float[] {1, 2}));
        assertThrows(IllegalArgumentException.class, () ->
                Tensor.bytes("overflow", TensorType.UINT8,
                        new int[] {Integer.MAX_VALUE, 2}, new byte[] {1}));
    }

    @Test
    void modelCacheCompletionIsSingleShot() {
        AsyncResource<String> resource = new AsyncResource<String>();
        ModelCache.Completion<String> completion =
                new ModelCache.Completion<String>(resource);
        completion.complete("first");
        completion.fail(new RuntimeException("late failure"));
        completion.complete("second");
        flushSerialCalls();
        assertEquals("first", resource.get());
    }

    @Test
    void modelCacheRejectsInsecureAndMalformedRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> ModelCache.fetch("http://example.com/model.tflite", "model"));
        assertThrows(IllegalArgumentException.class,
                () -> ModelCache.fetch("https://example.com/model.tflite", ""));
        assertThrows(IllegalArgumentException.class,
                () -> ModelCache.fetch("https://example.com/model.tflite", "model", "xyz"));
        assertThrows(IllegalArgumentException.class,
                () -> ModelCache.fetch("https://example.com/model.tflite", "model",
                        "gggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg"));
    }

    @Test
    void modelCacheNamesDoNotAliasSanitizedKeys() {
        assertNotEquals(ModelCache.safeName("model/v1"),
                ModelCache.safeName("model?v1"));
        assertNotEquals(ModelCache.safeName("model/v1"),
                ModelCache.safeName("model_v1"));
        assertTrue(ModelCache.safeName("model-v1_2")
                .startsWith("model-v1_2-"));
        StringBuilder longKey = new StringBuilder();
        for (int i = 0; i < 512; i++) {
            longKey.append('a');
        }
        assertTrue((ModelCache.safeName(longKey.toString())
                + ".tflite.download").length() <= 255);
    }

    @Test
    void modelCacheDiscardsPartialAndDigestMismatchFiles() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String temporary = fs.getAppHomePath() + "model-cache-test.download";
        write(temporary, new byte[] {1, 2, 3});
        ModelCache.prepareTemporary(fs, temporary);
        assertFalse(fs.exists(temporary), "a previous partial download must not be resumed");

        write(temporary, new byte[] {4, 5, 6});
        assertThrows(java.io.IOException.class, () ->
                ModelCache.verifyDownloaded(fs, temporary,
                        "0000000000000000000000000000000000000000000000000000000000000000"));
        assertFalse(fs.exists(temporary), "a digest mismatch must delete executable data");
    }

    private static void write(String path, byte[] value) throws Exception {
        OutputStream output = FileSystemStorage.getInstance().openOutputStream(path);
        try {
            output.write(value);
        } finally {
            output.close();
        }
    }

    @Test
    void sessionLifecycleForwardsToBackend() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        implementation.setInferenceImpl(backend);
        InferenceSession session = await(InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions().threads(2)));
        assertEquals(2, session.getInputs()[0].getShape()[1]);
        Tensor[] output = await(session.run(new Tensor[] {
                Tensor.floats("input", new int[] {1, 2}, new float[] {1, 2})
        }));
        assertArrayEquals(new float[] {3}, (float[]) output[0].getData());
        session.resizeInput("input", new int[] {1, 4});
        assertEquals("input", backend.resizedName);
        session.close();
        session.close();
        assertEquals(1, backend.closeCount);
        assertThrows(IllegalStateException.class, session::getInputs);
    }

    @Test
    void sessionDefersCloseUntilPendingRunSettles() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        backend.pendingRun = new AsyncResource<Tensor[]>();
        implementation.setInferenceImpl(backend);
        InferenceSession session = await(InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions()));
        AsyncResource<Tensor[]> run = session.run(new Tensor[] {
                Tensor.floats("input", new int[] {1, 2},
                        new float[] {1, 2})
        });
        assertThrows(IllegalStateException.class, () -> session.run(
                new Tensor[] {Tensor.floats("input", new int[] {1, 2},
                        new float[] {3, 4})}));
        assertThrows(IllegalStateException.class,
                () -> session.resizeInput("input", new int[] {1, 4}));
        session.close();
        assertEquals(0, backend.closeCount);
        backend.pendingRun.complete(new Tensor[] {
                Tensor.floats("output", new int[] {1}, new float[] {3})
        });
        flushSerialCalls();
        assertTrue(run.isDone());
        assertEquals(1, backend.closeCount);
        assertThrows(IllegalStateException.class, session::getInputs);
    }

    private <T> T await(AsyncResource<T> resource) {
        final AtomicReference<T> value = new AtomicReference<T>();
        resource.ready(new SuccessCallback<T>() {
            public void onSucess(T result) {
                value.set(result);
            }
        });
        flushSerialCalls();
        assertTrue(resource.isDone());
        assertNotNull(value.get());
        return resource.get();
    }

    private static final class RecordingInferenceImpl extends InferenceImpl {
        final Object handle = new Object();
        String resizedName;
        int closeCount;
        AsyncResource<Tensor[]> pendingRun;

        public boolean isSupported() {
            return true;
        }

        public AsyncResource<Object> open(ModelSource source, InferenceOptions options) {
            AsyncResource<Object> result = new AsyncResource<Object>();
            result.complete(handle);
            return result;
        }

        public TensorInfo[] getInputs(Object value) {
            assertSame(handle, value);
            return new TensorInfo[] {
                    new TensorInfo("input", TensorType.FLOAT32, new int[] {1, 2}, 0)
            };
        }

        public TensorInfo[] getOutputs(Object value) {
            return new TensorInfo[] {
                    new TensorInfo("output", TensorType.FLOAT32, new int[] {1}, 0)
            };
        }

        public AsyncResource<Tensor[]> run(Object value, Tensor[] inputs) {
            if (pendingRun != null) {
                return pendingRun;
            }
            AsyncResource<Tensor[]> result = new AsyncResource<Tensor[]>();
            result.complete(new Tensor[] {
                    Tensor.floats("output", new int[] {1}, new float[] {3})
            });
            return result;
        }

        public void resizeInput(Object value, String name, int[] shape) {
            resizedName = name;
        }

        public void close(Object value) {
            closeCount++;
        }
    }
}
