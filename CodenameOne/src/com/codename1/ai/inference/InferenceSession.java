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
    /// @param source bytes, resource, or file containing a `.tflite` model
    /// @param options execution options; {@code null} uses defaults
    /// @return an asynchronous session, failed with {@link InferenceException}
    ///         when the model or requested accelerator cannot be used
    public static AsyncResource<InferenceSession> open(ModelSource source,
                                                       InferenceOptions options) {
        if (source == null) {
            throw new NullPointerException("source");
        }
        final AsyncResource<InferenceSession> out = new AsyncResource<InferenceSession>();
        final InferenceImpl impl = Display.getInstance().getInferenceBackend();
        if (impl == null || !impl.isSupported()) {
            out.error(new InferenceException("LiteRT inference is not supported"));
            return out;
        }
        impl.open(source, options == null ? new InferenceOptions() : options)
                .ready(new SuccessCallback<Object>() {
                    @Override
                    public void onSucess(Object value) {
                        out.complete(new InferenceSession(impl, value));
                    }
                }).except(new SuccessCallback<Throwable>() {
                    @Override
                    public void onSucess(Throwable error) {
                        out.error(error);
                    }
                });
        return out;
    }

    /// Returns the model's current input metadata. Shapes reflect the most
    /// recent successful {@link #resizeInput(String, int[])} call.
    ///
    /// @return a defensive copy of the input metadata array
    public TensorInfo[] getInputs() {
        ensureOpen();
        return implementation.getInputs(handle);
    }

    /// @return a defensive copy of the model's current output metadata
    public TensorInfo[] getOutputs() {
        ensureOpen();
        return implementation.getOutputs(handle);
    }

    /// Copies input tensors to native memory, invokes the model, and returns
    /// every output tensor. Named tensors are matched by name; unnamed tensors
    /// are matched by position.
    ///
    /// @param inputs one tensor for each model input
    /// @return asynchronous output tensors in model output order
    public AsyncResource<Tensor[]> run(Tensor[] inputs) {
        ensureOpen();
        return implementation.run(handle, inputs == null ? new Tensor[0] : inputs);
    }

    /// Resizes an input and reallocates native tensors before the next run.
    ///
    /// @param name model input name, or {@code null} for the first input
    /// @param shape new non-negative dimensions
    public void resizeInput(String name, int[] shape) {
        ensureOpen();
        implementation.resizeInput(handle, name, shape);
    }

    @Override
    /// Releases the interpreter, delegates, and any temporary staged model.
    /// The original file supplied by {@link ModelSource#file(String)} is never
    /// deleted. Calling this method more than once has no effect.
    public void close() {
        if (!closed) {
            closed = true;
            implementation.close(handle);
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Inference session is closed");
        }
    }
}
