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
    void modelCacheRequiresDigestWhenIosHidesRedirects() {
        implementation.setPlatformName("ios");
        assertTrue(ModelCache.requiresPinnedModelDigest());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> ModelCache.fetch(
                        "https://example.com/model.tflite", "ios-model"));
        assertTrue(error.getMessage().contains("SHA-256"));
    }

    @Test
    void modelCacheRejectsInsecureRedirects() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String temporary = fs.getAppHomePath()
                + "model-cache-redirect-test.download";
        write(temporary, new byte[] {1, 2, 3});
        AsyncResource<ModelSource> resource = new AsyncResource<ModelSource>();
        AtomicReference<Throwable> error = new AtomicReference<Throwable>();
        resource.except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                error.set(value);
            }
        });
        ModelCache.ModelDownloadRequest request =
                new ModelCache.ModelDownloadRequest(
                        new ModelCache.Completion<ModelSource>(resource),
                        fs, temporary);

        assertFalse(request.onRedirect(
                "https://cdn.example.com/model.tflite"));
        assertTrue(request.onRedirect(
                "http://cdn.example.com/model.tflite"));
        flushSerialCalls();
        assertFalse(fs.exists(temporary),
                "a downgrade redirect must discard partial model data");
        assertNotNull(error.get(),
                "a downgrade redirect must fail the model resource");
        assertTrue(error.get().getCause().getMessage().contains("HTTPS"));
        assertTrue(ModelCache.isHttpsUrl(
                "HTTPS://cdn.example.com/model.tflite"));
        assertFalse(ModelCache.isHttpsUrl(
                "http://cdn.example.com/model.tflite"));
    }

    @Test
    void modelCacheCoalescesIdenticalConcurrentFetches() {
        String fileName = "model-cache-coalescing-test.tflite";
        ModelCache.FetchRegistration first = ModelCache.registerFetch(
                fileName, "https://one.example/model.tflite", null);
        ModelCache.FetchRegistration duplicate = ModelCache.registerFetch(
                fileName, "https://one.example/model.tflite", null);
        assertTrue(first.owner);
        assertFalse(duplicate.owner);
        assertSame(first.resource, duplicate.resource);

        ModelCache.FetchRegistration conflict = ModelCache.registerFetch(
                fileName, "https://two.example/model.tflite", null);
        AtomicReference<Throwable> conflictError =
                new AtomicReference<Throwable>();
        conflict.resource.except(new SuccessCallback<Throwable>() {
            @Override
            public void onSucess(Throwable value) {
                conflictError.set(value);
            }
        });
        flushSerialCalls();
        assertNotNull(conflictError.get(),
                "conflicting content for one cache key must fail");

        first.completion.complete(ModelSource.file("test-model.tflite"));
        flushSerialCalls();
        ModelCache.FetchRegistration afterCompletion =
                ModelCache.registerFetch(fileName,
                        "https://two.example/model.tflite", null);
        assertTrue(afterCompletion.owner,
                "completion must release the cache-key download slot");
        afterCompletion.completion.complete(
                ModelSource.file("test-model-2.tflite"));
        flushSerialCalls();
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

    @Test
    void modelCacheVerifiesPromotionBeforePublishingPath() throws Exception {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String directory = fs.getAppHomePath();
        String fileName = "model-cache-promoted-test.tflite";
        String target = directory + fileName;
        String temporary = target + ".download";
        if (fs.exists(target)) {
            fs.delete(target);
        }
        if (fs.exists(temporary)) {
            fs.delete(temporary);
        }

        assertThrows(java.io.IOException.class, () ->
                ModelCache.promoteDownloaded(fs, temporary, target,
                        fileName, null));
        assertFalse(fs.exists(target),
                "a failed rename must not publish a nonexistent cache path");

        write(temporary, new byte[] {1, 2, 3});
        assertThrows(java.io.IOException.class, () ->
                ModelCache.promoteDownloaded(fs, temporary, target,
                        fileName,
                        "00000000000000000000000000000000"
                        + "00000000000000000000000000000000"));
        assertFalse(fs.exists(target),
                "a final digest mismatch must remove the promoted path");
        assertFalse(fs.exists(temporary),
                "a final digest mismatch must remove the temporary file");

        write(temporary, new byte[] {1, 2, 3});
        ModelCache.promoteDownloaded(fs, temporary, target, fileName,
                "039058c6f2c0cb492c533b0a4d14ef7"
                + "7cc0f78abccced5287d84a1a2011cfb81");
        assertTrue(fs.exists(target));
        assertFalse(fs.exists(temporary));

        fs.delete(target);
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
    void sessionSnapshotsOptionsBeforeAsyncOpen() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        implementation.setInferenceImpl(backend);
        InferenceOptions options = new InferenceOptions()
                .accelerator(InferenceOptions.Accelerator.GPU)
                .threads(3)
                .allowFallback(false);

        AsyncResource<InferenceSession> opening = InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), options);
        options.accelerator(InferenceOptions.Accelerator.CPU)
                .threads(8)
                .allowFallback(true);

        assertNotSame(options, backend.openOptions);
        assertEquals(InferenceOptions.Accelerator.GPU,
                backend.openOptions.getAccelerator());
        assertEquals(3, backend.openOptions.getThreads());
        assertFalse(backend.openOptions.isFallbackAllowed());
        await(opening).close();
    }

    @Test
    void cancelledOpenClosesLateNativeHandle() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        backend.pendingOpen = new AsyncResource<Object>();
        implementation.setInferenceImpl(backend);

        AsyncResource<InferenceSession> opening = InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions());
        assertTrue(opening.cancel(false));
        backend.pendingOpen.complete(backend.handle);
        flushSerialCalls();

        assertTrue(opening.isCancelled());
        assertEquals(1, backend.closeCount,
                "a late native handle must not be orphaned after cancellation");
        assertThrows(AsyncResource.AsyncExecutionException.class,
                opening::get);
    }

    @Test
    void sessionRejectsShapeMismatchBeforeBackendRun() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        implementation.setInferenceImpl(backend);
        InferenceSession session = await(InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions()));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> session.run(
                        new Tensor[] {Tensor.floats("input",
                                new int[] {2, 1},
                                new float[] {1, 2})}));
        assertTrue(error.getMessage().contains("resizeInput"));
        assertNull(backend.receivedInputs,
                "shape mismatch must not reach the asynchronous backend");
        session.close();
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

    @Test
    void cancelledRunStaysSerializedUntilNativeWorkSettles() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        backend.pendingRun = new AsyncResource<Tensor[]>();
        implementation.setInferenceImpl(backend);
        InferenceSession session = await(InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions()));
        AsyncResource<Tensor[]> run = session.run(new Tensor[] {
                Tensor.floats("input", new int[] {1, 2},
                        new float[] {1, 2})
        });

        assertTrue(run.cancel(false));
        assertFalse(backend.pendingRun.isCancelled(),
                "outward cancellation must not interrupt a native invocation");
        assertThrows(IllegalStateException.class, () -> session.run(
                new Tensor[] {Tensor.floats("input", new int[] {1, 2},
                        new float[] {3, 4})}));
        session.close();
        assertEquals(0, backend.closeCount,
                "cancellation must not close an interpreter still in use");
        backend.pendingRun.complete(new Tensor[] {
                Tensor.floats("output", new int[] {1}, new float[] {3})
        });
        flushSerialCalls();
        assertTrue(run.isCancelled());
        assertEquals(1, backend.closeCount,
                "native settlement must complete deferred close");
    }

    @Test
    void sessionSnapshotsInputArrayBeforeAsyncInference() {
        RecordingInferenceImpl backend = new RecordingInferenceImpl();
        backend.pendingRun = new AsyncResource<Tensor[]>();
        implementation.setInferenceImpl(backend);
        InferenceSession session = await(InferenceSession.open(
                ModelSource.bytes(new byte[] {1}), new InferenceOptions()));
        Tensor original = Tensor.floats("input", new int[] {1, 2},
                new float[] {1, 2});
        Tensor replacement = Tensor.floats("replacement", new int[] {1, 2},
                new float[] {9, 9});
        Tensor[] callerInputs = new Tensor[] {original};

        AsyncResource<Tensor[]> run = session.run(callerInputs);
        callerInputs[0] = replacement;

        assertNotSame(callerInputs, backend.receivedInputs);
        assertSame(original, backend.receivedInputs[0],
                "a pending backend must see the run() call-time inputs");
        backend.pendingRun.complete(new Tensor[] {
                Tensor.floats("output", new int[] {1}, new float[] {3})
        });
        flushSerialCalls();
        assertTrue(run.isDone());
        session.close();
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
        Tensor[] receivedInputs;
        InferenceOptions openOptions;
        AsyncResource<Object> pendingOpen;

        public boolean isSupported() {
            return true;
        }

        public AsyncResource<Object> open(ModelSource source, InferenceOptions options) {
            openOptions = options;
            if (pendingOpen != null) {
                return pendingOpen;
            }
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
            receivedInputs = inputs;
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
