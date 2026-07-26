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
package com.codename1.impl.ios;

import com.codename1.ai.inference.InferenceException;
import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.inference.TensorInfo;
import com.codename1.ai.inference.TensorType;
import com.codename1.impl.InferenceImpl;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.JSONParser;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

/** LiteRT Objective-C backend with optional Core ML delegation. */
public final class IOSInferenceImpl extends InferenceImpl {
    private static final class Handle {
        final int id;
        TensorInfo[] inputs;
        TensorInfo[] outputs;

        Handle(int id, TensorInfo[] inputs, TensorInfo[] outputs) {
            this.id = id;
            this.inputs = inputs;
            this.outputs = outputs;
        }
    }

    @Override
    public boolean isSupported() {
        return IOSImplementation.nativeInstance.cn1InferenceIsSupported();
    }

    @Override
    public AsyncResource<Object> open(final ModelSource source,
                                      final InferenceOptions options) {
        final AsyncResource<Object> out = new AsyncResource<Object>();
        openInBackground(out, source, options);
        return out;
    }

    private static void openInBackground(final AsyncResource<Object> out,
                                         final ModelSource source,
                                         final InferenceOptions options) {
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    String opened = source.getKind() == ModelSource.FILE
                            ? IOSImplementation.nativeInstance.cn1InferenceOpenFile(
                                    FileSystemStorage.getInstance().toNativePath(
                                            source.getPath()),
                                    options.getThreads(),
                                    options.getAccelerator().ordinal(),
                                    options.isFallbackAllowed())
                            : IOSImplementation.nativeInstance.cn1InferenceOpen(
                                    loadModel(source), options.getThreads(),
                                    options.getAccelerator().ordinal(),
                                    options.isFallbackAllowed());
                    Map root = parse(opened);
                    int id = integer(root, "handle");
                    final Handle handle;
                    try {
                        handle = new Handle(id,
                                metadata(id, false), metadata(id, true));
                    } catch (Throwable metadataError) {
                        IOSImplementation.nativeInstance.cn1InferenceClose(id);
                        throw metadataError;
                    }
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
    }

    @Override
    public TensorInfo[] getInputs(Object handle) {
        return copy(checked(handle).inputs);
    }

    @Override
    public TensorInfo[] getOutputs(Object handle) {
        return copy(checked(handle).outputs);
    }

    @Override
    public AsyncResource<Tensor[]> run(Object handle, final Tensor[] inputs) {
        final AsyncResource<Tensor[]> out = new AsyncResource<Tensor[]>();
        final Handle checked = checked(handle);
        runInBackground(out, checked, inputs);
        return out;
    }

    private static void runInBackground(final AsyncResource<Tensor[]> out,
                                        final Handle checked,
                                        final Tensor[] inputs) {
        final int id = checked.id;
        Display.getInstance().scheduleBackgroundTask(new Runnable() {
            public void run() {
                try {
                    TensorInfo[] metadata = checked.inputs;
                    if (inputs.length != metadata.length) {
                        throw new IllegalArgumentException("Expected "
                                + metadata.length + " input tensors but received "
                                + inputs.length);
                    }
                    int[] indices = new int[inputs.length];
                    for (int i = 0; i < inputs.length; i++) {
                        Tensor tensor = inputs[i];
                        int index = inputIndex(tensor, i, metadata);
                        TensorInfo expected = find(metadata, index);
                        for (int previous = 0; previous < i; previous++) {
                            if (indices[previous] == index) {
                                throw new IllegalArgumentException(
                                        "Model input " + expected.getName()
                                                + " was supplied more than once");
                            }
                        }
                        indices[i] = index;
                        if (tensor.getType() != expected.getType()) {
                            throw new IllegalArgumentException("Input "
                                    + expected.getName() + " expects "
                                    + expected.getType() + " but received "
                                    + tensor.getType());
                        }
                    }
                    for (int i = 0; i < inputs.length; i++) {
                        Tensor tensor = inputs[i];
                        parse(IOSImplementation.nativeInstance.cn1InferenceCopyInput(
                                id, indices[i], encodeData(tensor.getType(),
                                        tensor.getDataUnsafe())));
                    }
                    parse(IOSImplementation.nativeInstance.cn1InferenceInvoke(id));
                    TensorInfo[] outputs = metadata(id, true);
                    checked.outputs = outputs;
                    final Tensor[] result = new Tensor[outputs.length];
                    for (int i = 0; i < result.length; i++) {
                        TensorInfo output = outputs[i];
                        long data = IOSImplementation.nativeInstance
                                .cn1InferenceOutputData(id, output.getIndex());
                        if (data == 0) {
                            throw new InferenceException(
                                    "Could not read LiteRT output "
                                            + output.getName());
                        }
                        byte[] bytes;
                        try {
                            bytes = new byte[IOSImplementation.nativeInstance
                                    .getNSDataSize(data)];
                            IOSImplementation.nativeInstance.nsDataToByteArray(
                                    data, bytes);
                        } finally {
                            IOSImplementation.nativeInstance.releasePeer(data);
                        }
                        int[] shape = output.getShape();
                        result[i] = new Tensor(output.getName(),
                                output.getType(), shape,
                                decodeData(output.getType(), bytes,
                                        elementCount(shape)));
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
    }

    @Override
    public void resizeInput(Object handle, String name, int[] shape) {
        Handle checked = checked(handle);
        TensorInfo[] inputs = checked.inputs;
        int index = -1;
        for (int i = 0; i < inputs.length; i++) {
            if (name == null || name.equals(inputs[i].getName())) {
                index = inputs[i].getIndex();
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException("Unknown model input " + name);
        }
        try {
            parse(IOSImplementation.nativeInstance.cn1InferenceResize(
                    checked.id, index, shape));
            checked.inputs = metadata(checked.id, false);
            checked.outputs = metadata(checked.id, true);
        } catch (Exception error) {
            throw new InferenceException("Could not resize input " + name, error);
        }
    }

    @Override
    public void close(Object handle) {
        IOSImplementation.nativeInstance.cn1InferenceClose(checked(handle).id);
    }

    private static TensorInfo[] metadata(int handle, boolean outputs) {
        try {
            Map root = parse(IOSImplementation.nativeInstance.cn1InferenceMetadata(
                    handle, outputs));
            List items = list(root, "items");
            TensorInfo[] result = new TensorInfo[items.size()];
            for (int i = 0; i < result.length; i++) {
                Map value = (Map) items.get(i);
                result[i] = new TensorInfo(string(value, "name"),
                        TensorType.valueOf(string(value, "type")),
                        intArray(list(value, "shape")),
                        integer(value, "index"));
            }
            return result;
        } catch (Exception error) {
            throw new InferenceException("Could not read LiteRT metadata", error);
        }
    }

    private static Handle checked(Object value) {
        if (!(value instanceof Handle)) {
            throw new IllegalArgumentException("Invalid LiteRT session handle");
        }
        return (Handle) value;
    }

    private static int inputIndex(Tensor tensor, int fallback,
                                  TensorInfo[] metadata) {
        if (tensor.getName() != null) {
            for (int i = 0; i < metadata.length; i++) {
                if (tensor.getName().equals(metadata[i].getName())) {
                    return metadata[i].getIndex();
                }
            }
            throw new IllegalArgumentException("Unknown model input "
                    + tensor.getName());
        }
        return metadata[fallback].getIndex();
    }

    private static TensorInfo find(TensorInfo[] metadata, int index) {
        for (int i = 0; i < metadata.length; i++) {
            if (metadata[i].getIndex() == index) {
                return metadata[i];
            }
        }
        throw new IllegalArgumentException("Unknown model input index " + index);
    }

    private static byte[] encodeData(TensorType type, Object value) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof float[]) {
            float[] values = (float[]) value;
            byte[] out = new byte[values.length * 4];
            for (int i = 0; i < values.length; i++) {
                writeInt(out, i * 4, Float.floatToIntBits(values[i]));
            }
            return out;
        }
        if (value instanceof int[]) {
            int[] values = (int[]) value;
            byte[] out = new byte[values.length * 4];
            for (int i = 0; i < values.length; i++) {
                writeInt(out, i * 4, values[i]);
            }
            return out;
        }
        if (value instanceof long[]) {
            long[] values = (long[]) value;
            byte[] out = new byte[values.length * 8];
            for (int i = 0; i < values.length; i++) {
                writeLong(out, i * 8, values[i]);
            }
            return out;
        }
        throw new InferenceException("Unsupported input tensor data");
    }

    private static Object decodeData(TensorType type, byte[] bytes, int count) {
        if (type == TensorType.FLOAT32) {
            float[] out = new float[count];
            for (int i = 0; i < count; i++) {
                out[i] = Float.intBitsToFloat(readInt(bytes, i * 4));
            }
            return out;
        }
        if (type == TensorType.INT32) {
            int[] out = new int[count];
            for (int i = 0; i < count; i++) {
                out[i] = readInt(bytes, i * 4);
            }
            return out;
        }
        if (type == TensorType.INT64) {
            long[] out = new long[count];
            for (int i = 0; i < count; i++) {
                out[i] = readLong(bytes, i * 8);
            }
            return out;
        }
        if (bytes.length != count) {
            throw new InferenceException("Unexpected output tensor byte count");
        }
        return bytes;
    }

    private static void writeInt(byte[] out, int offset, int value) {
        out[offset] = (byte) value;
        out[offset + 1] = (byte) (value >>> 8);
        out[offset + 2] = (byte) (value >>> 16);
        out[offset + 3] = (byte) (value >>> 24);
    }

    private static int readInt(byte[] value, int offset) {
        return (value[offset] & 255)
                | ((value[offset + 1] & 255) << 8)
                | ((value[offset + 2] & 255) << 16)
                | ((value[offset + 3] & 255) << 24);
    }

    private static void writeLong(byte[] out, int offset, long value) {
        for (int i = 0; i < 8; i++) {
            out[offset + i] = (byte) (value >>> (i * 8));
        }
    }

    private static long readLong(byte[] value, int offset) {
        long out = 0;
        for (int i = 0; i < 8; i++) {
            out |= (long) (value[offset + i] & 255) << (i * 8);
        }
        return out;
    }

    private static int elementCount(int[] shape) {
        int out = 1;
        for (int i = 0; i < shape.length; i++) {
            out *= shape[i];
        }
        return out;
    }

    private static int[] intArray(List values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = ((Number) values.get(i)).intValue();
        }
        return out;
    }

    private static Map parse(String json) throws Exception {
        if (json == null || json.length() == 0) {
            throw new InferenceException("LiteRT returned no result");
        }
        Map root = new JSONParser().parseJSON(new StringReader(json));
        Object error = root.get("error");
        if (error != null) {
            throw new InferenceException(String.valueOf(error));
        }
        return root;
    }

    private static List list(Map value, String key) {
        Object out = value.get(key);
        return out instanceof List ? (List) out : java.util.Collections.EMPTY_LIST;
    }

    private static String string(Map value, String key) {
        Object out = value.get(key);
        return out == null ? "" : String.valueOf(out);
    }

    private static int integer(Map value, String key) {
        Object out = value.get(key);
        return out instanceof Number ? ((Number) out).intValue() : 0;
    }

    private static byte[] loadModel(ModelSource source) throws IOException {
        if (source.getKind() == ModelSource.BYTES) {
            return source.getBytes();
        }
        InputStream input;
        input = Display.getInstance().getResourceAsStream(
                IOSInferenceImpl.class, source.getPath());
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
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static TensorInfo[] copy(TensorInfo[] value) {
        TensorInfo[] out = new TensorInfo[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
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
