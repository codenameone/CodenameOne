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
package com.codename1.impl.android.ai;

import com.codename1.ai.inference.InferenceException;
import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.inference.TensorInfo;
import com.codename1.ai.inference.TensorType;
import com.codename1.impl.InferenceImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

/** Android LiteRT backend. */
public final class AndroidInferenceImpl extends InferenceImpl {
    private static volatile Method outputShapeRefreshMethod;

    private static final class Handle {
        final Interpreter interpreter;

        Handle(Interpreter interpreter) {
            this.interpreter = interpreter;
        }
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    @Override
    public AsyncResource<Object> open(final ModelSource source,
                                      final InferenceOptions options) {
        final AsyncResource<Object> out = new AsyncResource<Object>();
        final InferenceOptions.Accelerator accelerator =
                options.getAccelerator();
        final int threads = options.getThreads();
        final boolean allowFallback = options.isFallbackAllowed();
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    if ((accelerator == InferenceOptions.Accelerator.GPU
                            || accelerator == InferenceOptions.Accelerator.CORE_ML)
                            && !allowFallback) {
                        throw new InferenceException(accelerator
                                + " acceleration is unavailable on Android");
                    }
                    if (accelerator == InferenceOptions.Accelerator.NPU
                            && !allowFallback) {
                        throw new InferenceException(
                                "Strict NPU execution cannot be verified on "
                                + "Android; NNAPI may leave unsupported "
                                + "operations on the CPU");
                    }
                    ByteBuffer model = loadModel(source);
                    Interpreter.Options nativeOptions = new Interpreter.Options();
                    if (threads > 0) {
                        nativeOptions.setNumThreads(threads);
                    }
                    if (accelerator == InferenceOptions.Accelerator.NPU) {
                        nativeOptions.setUseNNAPI(true);
                    }
                    Interpreter interpreter;
                    try {
                        interpreter = new Interpreter(model, nativeOptions);
                    } catch (Throwable acceleratedFailure) {
                        if (accelerator != InferenceOptions.Accelerator.NPU
                                || !allowFallback) {
                            throw acceleratedFailure;
                        }
                        Interpreter.Options cpuOptions = new Interpreter.Options();
                        if (threads > 0) {
                            cpuOptions.setNumThreads(threads);
                        }
                        model.rewind();
                        interpreter = new Interpreter(model, cpuOptions);
                    }
                    final Handle handle = new Handle(interpreter);
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.complete(handle);
                        }
                    });
                } catch (final Throwable error) {
                    fail(out, "Could not open LiteRT model", error);
                }
            }
        });
        return out;
    }

    @Override
    public TensorInfo[] getInputs(Object handle) {
        Interpreter interpreter = checked(handle).interpreter;
        TensorInfo[] result = new TensorInfo[interpreter.getInputTensorCount()];
        for (int i = 0; i < result.length; i++) {
            org.tensorflow.lite.Tensor tensor = interpreter.getInputTensor(i);
            result[i] = info(tensor, i);
        }
        return result;
    }

    @Override
    public TensorInfo[] getOutputs(Object handle) {
        Interpreter interpreter = checked(handle).interpreter;
        TensorInfo[] result = new TensorInfo[interpreter.getOutputTensorCount()];
        for (int i = 0; i < result.length; i++) {
            org.tensorflow.lite.Tensor tensor = interpreter.getOutputTensor(i);
            result[i] = info(tensor, i);
        }
        return result;
    }

    @Override
    public AsyncResource<Tensor[]> run(Object handle, final Tensor[] inputs) {
        final AsyncResource<Tensor[]> out = new AsyncResource<Tensor[]>();
        final Interpreter interpreter = checked(handle).interpreter;
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    int inputCount = interpreter.getInputTensorCount();
                    if (inputs == null || inputs.length != inputCount) {
                        throw new IllegalArgumentException("Expected "
                                + inputCount + " input tensors but received "
                                + (inputs == null ? 0 : inputs.length));
                    }
                    Object[] nativeInputs = new Object[inputCount];
                    boolean[] resolved = new boolean[inputCount];
                    for (int i = 0; i < inputs.length; i++) {
                        Tensor value = inputs[i];
                        int index = value.getName() == null ? i
                                : inputIndex(interpreter, value.getName());
                        org.tensorflow.lite.Tensor metadata =
                                interpreter.getInputTensor(index);
                        if (resolved[index]) {
                            throw new IllegalArgumentException(
                                    "Model input " + metadata.name()
                                            + " was supplied more than once");
                        }
                        resolved[index] = true;
                        if (!sameShape(value.getShape(),
                                metadata.shape())) {
                            throw new IllegalArgumentException(
                                    "Input " + metadata.name()
                                            + " shape does not match the "
                                            + "model metadata; call "
                                            + "resizeInput() before run()");
                        }
                        nativeInputs[index] =
                                toBuffer(value, metadata.dataType());
                    }
                    Map<Integer, Object> nativeOutputs =
                            new HashMap<Integer, Object>();
                    int outputCount = interpreter.getOutputTensorCount();
                    for (int i = 0; i < outputCount; i++) {
                        // LiteRT 1.0.1 treats a null output as invocation-only:
                        // the native tensor remains available through
                        // Tensor.asReadOnlyBuffer() after the run. This avoids
                        // allocating from a pre-run size that may still contain
                        // unresolved, value-dependent output dimensions.
                        nativeOutputs.put(Integer.valueOf(i), null);
                    }
                    interpreter.runForMultipleInputsOutputs(nativeInputs,
                            nativeOutputs);
                    final Tensor[] result = new Tensor[outputCount];
                    for (int i = 0; i < result.length; i++) {
                        org.tensorflow.lite.Tensor metadata =
                                interpreter.getOutputTensor(i);
                        refreshOutputShape(metadata);
                        result[i] = fromBuffer(metadata.name(),
                                metadata.shape(), metadata.dataType(),
                                metadata.asReadOnlyBuffer());
                    }
                    Display.getInstance().callSerially(new Runnable() {
                        public void run() {
                            out.complete(result);
                        }
                    });
                } catch (final Throwable error) {
                    fail(out, "LiteRT inference failed", error);
                }
            }
        });
        return out;
    }

    @Override
    public void resizeInput(Object handle, String name, int[] shape) {
        Interpreter interpreter = checked(handle).interpreter;
        int index = inputIndex(interpreter, name);
        interpreter.resizeInput(index, shape);
        interpreter.allocateTensors();
    }

    @Override
    public void close(Object handle) {
        checked(handle).interpreter.close();
    }

    private static Handle checked(Object value) {
        if (!(value instanceof Handle)) {
            throw new IllegalArgumentException("Invalid LiteRT session handle");
        }
        return (Handle) value;
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

    private static void refreshOutputShape(
            org.tensorflow.lite.Tensor metadata) {
        try {
            // TensorImpl caches shape(), and LiteRT refreshes that cache only
            // when the invocation also reallocates tensors. Value-dependent
            // output shapes can change without input reallocation, so refresh
            // the package-private cache after every run. The builders retain
            // this one method name for R8 release builds.
            Method refresh = outputShapeRefreshMethod;
            if (refresh == null) {
                synchronized (AndroidInferenceImpl.class) {
                    refresh = outputShapeRefreshMethod;
                    if (refresh == null) {
                        refresh = metadata.getClass()
                                .getDeclaredMethod("refreshShape");
                        refresh.setAccessible(true);
                        outputShapeRefreshMethod = refresh;
                    }
                }
            }
            refresh.invoke(metadata);
        } catch (Throwable error) {
            throw new InferenceException(
                    "Could not refresh the LiteRT output shape", error);
        }
    }

    private static int inputIndex(Interpreter interpreter, String name) {
        for (int i = 0; i < interpreter.getInputTensorCount(); i++) {
            if (name == null || name.equals(interpreter.getInputTensor(i).name())) {
                return i;
            }
        }
        throw new IllegalArgumentException("Unknown model input " + name);
    }

    private static TensorInfo info(org.tensorflow.lite.Tensor value, int index) {
        return new TensorInfo(value.name(), type(value.dataType()), value.shape(), index);
    }

    private static TensorType type(DataType type) {
        if (type == DataType.FLOAT32) return TensorType.FLOAT32;
        if (type == DataType.INT32) return TensorType.INT32;
        if (type == DataType.INT64) return TensorType.INT64;
        if (type == DataType.UINT8) return TensorType.UINT8;
        if (type == DataType.INT8) return TensorType.INT8;
        if (type == DataType.BOOL) return TensorType.BOOL;
        throw new InferenceException("Unsupported LiteRT tensor type " + type);
    }

    private static ByteBuffer toBuffer(Tensor value, DataType nativeType) {
        if (value.getType() != type(nativeType)) {
            throw new IllegalArgumentException("Input " + value.getName()
                    + " type does not match the model");
        }
        Object data = value.getDataUnsafe();
        int byteCount;
        if (data instanceof float[]) byteCount = ((float[]) data).length * 4;
        else if (data instanceof int[]) byteCount = ((int[]) data).length * 4;
        else if (data instanceof long[]) byteCount = ((long[]) data).length * 8;
        else if (data instanceof byte[]) byteCount = ((byte[]) data).length;
        else throw new InferenceException("Unsupported input tensor data");
        ByteBuffer out = ByteBuffer.allocateDirect(byteCount).order(ByteOrder.nativeOrder());
        if (data instanceof float[]) out.asFloatBuffer().put((float[]) data);
        else if (data instanceof int[]) out.asIntBuffer().put((int[]) data);
        else if (data instanceof long[]) out.asLongBuffer().put((long[]) data);
        else out.put((byte[]) data);
        out.rewind();
        return out;
    }

    private static Tensor fromBuffer(String name, int[] shape, DataType nativeType,
                                     ByteBuffer value) {
        value.rewind();
        value.order(ByteOrder.nativeOrder());
        TensorType type = type(nativeType);
        int count = elementCount(shape);
        if (type == TensorType.FLOAT32) {
            float[] data = new float[count];
            value.asFloatBuffer().get(data);
            return new Tensor(name, type, shape, data);
        }
        if (type == TensorType.INT32) {
            int[] data = new int[count];
            value.asIntBuffer().get(data);
            return new Tensor(name, type, shape, data);
        }
        if (type == TensorType.INT64) {
            long[] data = new long[count];
            value.asLongBuffer().get(data);
            return new Tensor(name, type, shape, data);
        }
        byte[] data = new byte[count];
        value.get(data);
        return new Tensor(name, type, shape, data);
    }

    private static int elementCount(int[] shape) {
        int count = 1;
        for (int i = 0; i < shape.length; i++) {
            count *= shape[i];
        }
        return count;
    }

    private static ByteBuffer loadModel(ModelSource source) throws IOException {
        if (source.getKind() == ModelSource.FILE) {
            String nativePath = FileSystemStorage.getInstance().toNativePath(
                    source.getPath());
            FileInputStream file = new FileInputStream(nativePath);
            try {
                return file.getChannel().map(FileChannel.MapMode.READ_ONLY,
                        0, file.getChannel().size());
            } finally {
                file.close();
            }
        }
        byte[] bytes;
        if (source.getKind() == ModelSource.BYTES) {
            bytes = source.getBytes();
        } else {
            InputStream input = Display.getInstance().getResourceAsStream(
                    AndroidInferenceImpl.class, source.getPath());
            if (input == null) {
                throw new IOException("Model resource not found: " + source.getPath());
            }
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[16384];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                }
                bytes = output.toByteArray();
            } finally {
                input.close();
            }
        }
        ByteBuffer result = ByteBuffer.allocateDirect(bytes.length)
                .order(ByteOrder.nativeOrder());
        result.put(bytes);
        result.rewind();
        return result;
    }

    private static void fail(final AsyncResource<?> out, final String message,
                             final Throwable cause) {
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                out.error(new InferenceException(message, cause));
            }
        });
    }
}
