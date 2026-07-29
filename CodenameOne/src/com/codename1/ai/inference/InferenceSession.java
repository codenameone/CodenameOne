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
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// Reusable, native on-device session for a TensorFlow Lite model.
///
/// Opening and execution are asynchronous because model allocation and
/// delegates can be expensive. Metadata and resize operations are synchronous.
/// A session is not usable after {@link #close()}; applications should retain
/// and reuse one session instead of reopening the model for every input.
public final class InferenceSession implements AutoCloseable {
    private final InferenceImpl implementation;
    private final Object handle;
    private boolean closed;
    private int activeRuns;
    private boolean closePending;

    private InferenceSession(InferenceImpl implementation, Object handle) {
        this.implementation = implementation;
        this.handle = handle;
    }

    /// Tests whether the current port includes a native LiteRT runtime.
    ///
    /// @return {@code true} when sessions can be opened on this target
    public static boolean isSupported() {
        InferenceImpl impl = Display.getInstance().getInferenceBackend();
        return impl != null && impl.isSupported();
    }

    /// Opens and allocates a model session off the EDT.
    ///
    /// The option values are copied before asynchronous backend work is
    /// scheduled. Reusing or changing the supplied {@link InferenceOptions}
    /// after this method returns therefore cannot alter the pending open.
    /// Canceling the returned resource prevents session publication; if the
    /// native backend finishes opening afterward, its handle is closed
    /// automatically.
    ///
    /// @param source bytes, resource, or file containing a `.tflite` model
    /// @param options execution options; {@code null} uses defaults
    /// @return an asynchronous session, failed with {@link InferenceException}
    ///         when the model or requested accelerator cannot be used
    public static AsyncResource<InferenceSession> open(ModelSource source,
                                                       InferenceOptions options) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        final SessionOpenResource out = new SessionOpenResource();
        final InferenceImpl impl = Display.getInstance().getInferenceBackend();
        if (impl == null || !impl.isSupported()) {
            out.error(new InferenceException("LiteRT inference is not supported"));
            return out;
        }
        InferenceOptions requested = options == null
                ? new InferenceOptions() : options;
        impl.open(source, requested.snapshot())
                .ready(new SuccessCallback<Object>() {
                    @Override
                    public void onSucess(Object value) {
                        out.publish(impl, value);
                    }
                }).except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable error) {
                        out.fail(error);
                    }
                });
        return out;
    }

    private static final class SessionOpenResource
            extends AsyncResource<InferenceSession> {
        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            return super.cancel(mayInterruptIfRunning);
        }

        synchronized void publish(InferenceImpl implementation,
                                  Object nativeHandle) {
            if (isCancelled()) {
                implementation.close(nativeHandle);
                return;
            }
            complete(new InferenceSession(implementation, nativeHandle));
        }

        synchronized void fail(Throwable error) {
            if (!isCancelled()) {
                error(error);
            }
        }
    }

    /// Returns the model's current input metadata. Shapes reflect the most
    /// recent successful {@link #resizeInput(String, int[])} call. Metadata
    /// cannot be queried while {@link #run(Tensor[])} is pending because the
    /// native interpreter is mutable and may be updating tensor state.
    ///
    /// @return a defensive copy of the input metadata array
    /// @throws IllegalStateException if the session is closed or a run is pending
    public TensorInfo[] getInputs() {
        synchronized (this) {
            ensureOpen();
            ensureNotRunning("read input metadata");
            return copyMetadata(implementation.getInputs(handle));
        }
    }

    /// Returns the model's current output metadata. Backends refresh this
    /// information after an invocation so models with dynamically resolved
    /// output dimensions report the shape used to decode the returned tensor.
    /// Metadata cannot be queried while {@link #run(Tensor[])} is pending
    /// because the native interpreter is mutable and may be updating tensor
    /// state.
    ///
    /// @return a defensive copy of the output metadata array
    /// @throws IllegalStateException if the session is closed or a run is pending
    public TensorInfo[] getOutputs() {
        synchronized (this) {
            ensureOpen();
            ensureNotRunning("read output metadata");
            return copyMetadata(implementation.getOutputs(handle));
        }
    }

    private static TensorInfo[] copyMetadata(TensorInfo[] metadata) {
        if (metadata == null || metadata.length == 0) {
            return new TensorInfo[0];
        }
        TensorInfo[] copy = new TensorInfo[metadata.length];
        System.arraycopy(metadata, 0, copy, 0, metadata.length);
        return copy;
    }

    /// Copies input tensors to native memory, invokes the model, and returns
    /// every output tensor. Named tensors are matched by name; unnamed tensors
    /// are matched by position. Calling {@link #close()} while this operation
    /// is pending prevents new work immediately but defers native release until
    /// the returned resource succeeds or fails. A session accepts one run at a
    /// time because the underlying native interpreter is mutable. The array
    /// container is defensively copied before it is handed to the asynchronous
    /// backend, so replacing an element after this method returns cannot change
    /// the pending invocation. Each {@link Tensor} is itself immutable.
    /// Canceling the returned resource suppresses result publication but does
    /// not interrupt an invocation that has already entered LiteRT. The
    /// session remains busy, and a pending {@link #close()} remains deferred,
    /// until the native operation actually succeeds or fails.
    ///
    /// @param inputs one tensor for each model input; {@code null} is treated
    /// as an empty input array
    /// @return asynchronous output tensors in model output order
    /// @throws IllegalStateException if the session is closed or already running
    /// @throws IllegalArgumentException if an input name, count, or shape does
    ///         not match the model's current input metadata
    public AsyncResource<Tensor[]> run(Tensor[] inputs) {
        final AsyncResource<Tensor[]> backendResult;
        synchronized (this) {
            ensureOpen();
            if (activeRuns > 0) {
                throw new IllegalStateException(
                        "Inference session is already running");
            }
            activeRuns++;
            try {
                Tensor[] inputSnapshot = inputs == null
                        ? new Tensor[0] : copyInputs(inputs);
                validateInputShapes(inputSnapshot,
                        implementation.getInputs(handle));
                backendResult = implementation.run(handle,
                        inputSnapshot);
                if (backendResult == null) {
                    throw new InferenceException(
                            "Inference backend returned no asynchronous result");
                }
            } catch (RuntimeException error) {
                activeRuns--;
                throw error;
            } catch (Error error) {
                activeRuns--;
                throw error;
            }
        }
        final PendingRun pending = new PendingRun(this);
        return new SessionRunResource(backendResult, pending);
    }

    private static void validateInputShapes(Tensor[] inputs,
                                            TensorInfo[] metadata) {
        if (metadata == null || inputs.length != metadata.length) {
            throw new IllegalArgumentException("Expected "
                    + (metadata == null ? 0 : metadata.length)
                    + " input tensors but received " + inputs.length);
        }
        boolean[] resolved = new boolean[metadata.length];
        for (int i = 0; i < inputs.length; i++) {
            Tensor tensor = inputs[i];
            if (tensor == null) {
                throw new IllegalArgumentException(
                        "Input tensor " + i + " is null");
            }
            int metadataPosition = i;
            if (tensor.getName() != null) {
                metadataPosition = -1;
                for (int candidate = 0; candidate < metadata.length;
                        candidate++) {
                    if (tensor.getName().equals(
                            metadata[candidate].getName())) {
                        metadataPosition = candidate;
                        break;
                    }
                }
                if (metadataPosition < 0) {
                    throw new IllegalArgumentException(
                            "Unknown model input " + tensor.getName());
                }
            }
            TensorInfo expected = metadata[metadataPosition];
            if (resolved[metadataPosition]) {
                throw new IllegalArgumentException("Model input "
                        + expected.getName() + " was supplied more than once");
            }
            resolved[metadataPosition] = true;
            if (!sameShape(tensor.getShape(), expected.getShape())) {
                throw new IllegalArgumentException("Input "
                        + expected.getName() + " shape does not match the "
                        + "model metadata; call resizeInput() before run()");
            }
        }
    }

    private static Tensor[] copyInputs(Tensor[] inputs) {
        Tensor[] copy = new Tensor[inputs.length];
        for (int i = 0; i < inputs.length; i++) {
            copy[i] = inputs[i];
        }
        return copy;
    }

    private static boolean sameShape(int[] supplied, int[] expected) {
        if (supplied.length != expected.length) {
            return false;
        }
        for (int i = 0; i < supplied.length; i++) {
            if (supplied[i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    /// Resizes an input and reallocates native tensors before the next run.
    ///
    /// This method throws while an asynchronous {@link #run(Tensor[])} is
    /// pending because native runtimes cannot safely reallocate tensors during
    /// an invocation.
    ///
    /// @param name model input name, or {@code null} for the first input
    /// @param shape new non-negative dimensions
    /// @throws IllegalStateException if the session is closed or a run is pending
    public void resizeInput(String name, int[] shape) {
        synchronized (this) {
            ensureOpen();
            if (activeRuns > 0) {
                throw new IllegalStateException(
                        "Cannot resize inputs while inference is running");
            }
            implementation.resizeInput(handle, name, shape);
        }
    }

    /// Releases the interpreter, delegates, and any temporary staged model.
    /// The original file supplied by {@link ModelSource#file(String)} is never
    /// deleted. If a run is pending, release is deferred until that run
    /// settles. Calling this method more than once has no effect.
    @Override
    public void close() {
        boolean release = false;
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            if (activeRuns == 0) {
                release = true;
            } else {
                closePending = true;
            }
        }
        if (release) {
            implementation.close(handle);
        }
    }

    private void runFinished() {
        boolean release = false;
        synchronized (this) {
            if (activeRuns > 0) {
                activeRuns--;
            }
            if (activeRuns == 0 && closePending) {
                closePending = false;
                release = true;
            }
        }
        if (release) {
            implementation.close(handle);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Inference session is closed");
        }
    }

    private void ensureNotRunning(String operation) {
        if (activeRuns > 0) {
            throw new IllegalStateException(
                    "Cannot " + operation + " while inference is running");
        }
    }

    private static final class PendingRun {
        private final InferenceSession session;
        private boolean finished;

        PendingRun(InferenceSession session) {
            this.session = session;
        }

        synchronized void finish() {
            if (!finished) {
                finished = true;
                session.runFinished();
            }
        }
    }

    private static final class SessionRunResource
            extends AsyncResource<Tensor[]> {
        private final PendingRun pending;

        SessionRunResource(AsyncResource<Tensor[]> backend,
                           PendingRun pending) {
            this.pending = pending;
            backend.ready(new SuccessCallback<Tensor[]>() {
                @Override
                public void onSucess(Tensor[] value) {
                    SessionRunResource.this.pending.finish();
                    if (!isCancelled()) {
                        complete(value);
                    }
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable error) {
                    SessionRunResource.this.pending.finish();
                    if (!isCancelled()) {
                        error(error);
                    }
                }
            });
        }
    }
}
