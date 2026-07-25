/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ai.inference.InferenceOptions;
import com.codename1.ai.inference.InferenceSession;
import com.codename1.ai.inference.ModelSource;
import com.codename1.ai.inference.Tensor;
import com.codename1.ai.inference.TensorInfo;
import com.codename1.ai.inference.TensorType;
import com.codename1.io.Log;
import com.codename1.util.AsyncResource;

/**
 * Cross-port, non-visual contract coverage for LiteRT model inference.
 *
 * <p>The portable tensor and model-source contracts run everywhere. The test
 * also queries the native runtime so the builders select it where supported;
 * an actual model is deliberately not bundled into the conformance app.</p>
 */
public class InferenceOnDeviceApiTest extends BaseTest {
    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            checkTensorContracts();
            checkModelAndOptions();
            checkCapability();
            done();
            return true;
        } catch (Throwable t) {
            fail("On-device inference API test failed: " + t);
            return false;
        }
    }

    private void checkTensorContracts() {
        int[] shape = new int[] {1, 2};
        float[] values = new float[] {1f, 2f};
        Tensor tensor = Tensor.floats("input", shape, values);
        shape[1] = 9;
        values[0] = 9f;
        checkEqual(2, tensor.getShape()[1],
                "Tensor must copy the input shape");
        checkEqual(1f, ((float[]) tensor.getData())[0],
                "Tensor must copy the input data");
        int[] outputShape = tensor.getShape();
        float[] outputData = (float[]) tensor.getData();
        outputShape[1] = 8;
        outputData[1] = 8f;
        checkEqual(2, tensor.getShape()[1],
                "Tensor must copy the output shape");
        checkEqual(2f, ((float[]) tensor.getData())[1],
                "Tensor must copy the output data");
        check(tensor.getType() == TensorType.FLOAT32,
                "Tensor FLOAT32 type");

        TensorInfo info = new TensorInfo(
                "output", TensorType.INT32, new int[] {1, 3}, 2);
        int[] infoShape = info.getShape();
        infoShape[1] = 7;
        checkEqual(3, info.getShape()[1],
                "TensorInfo must copy the output shape");
        checkEqual(2, info.getIndex(), "TensorInfo index");

        try {
            Tensor.floats("bad", new int[] {3}, new float[] {1f, 2f});
            throw new IllegalStateException(
                    "Tensor accepted data that did not match its shape");
        } catch (IllegalArgumentException expected) {
            // Shape/data validation is part of the public contract.
        }
    }

    private void checkModelAndOptions() {
        byte[] bytes = new byte[] {1, 2, 3};
        ModelSource source = ModelSource.bytes(bytes);
        bytes[0] = 9;
        checkEqual(1, source.getBytes()[0],
                "ModelSource must copy input bytes");
        byte[] returned = source.getBytes();
        returned[1] = 9;
        checkEqual(2, source.getBytes()[1],
                "ModelSource must copy output bytes");
        checkEqual(ModelSource.BYTES, source.getKind(),
                "ModelSource byte kind");
        check("model.tflite".equals(
                ModelSource.resource("model.tflite").getPath()),
                "ModelSource resource path");

        InferenceOptions options = new InferenceOptions()
                .accelerator(InferenceOptions.Accelerator.CORE_ML)
                .threads(-1)
                .allowFallback(false);
        check(options.getAccelerator()
                        == InferenceOptions.Accelerator.CORE_ML,
                "inference accelerator");
        checkEqual(0, options.getThreads(), "thread lower clamp");
        check(!options.isFallbackAllowed(), "inference fallback option");
    }

    private void checkCapability() {
        boolean supported = InferenceSession.isSupported();
        Log.p("InferenceOnDeviceApiTest: supported=" + supported);
        if (!supported) {
            AsyncResource<InferenceSession> result = InferenceSession.open(
                    ModelSource.bytes(new byte[] {1}), null);
            check(result.isDone(),
                    "unsupported inference must complete immediately");
            try {
                result.get();
                throw new IllegalStateException(
                        "unsupported inference unexpectedly opened a session");
            } catch (AsyncResource.AsyncExecutionException expected) {
                // The documented unsupported-resource contract.
            }
        }
    }

    private void check(boolean value, String label) {
        if (!value) {
            throw new IllegalStateException(label);
        }
    }

    private void checkEqual(int expected, int actual, String label) {
        if (expected != actual) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }

    private void checkEqual(float expected, float actual, String label) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new IllegalStateException(label + ": expected "
                    + expected + " got " + actual);
        }
    }
}
