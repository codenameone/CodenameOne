package com.codename1.ai.tflite;

public class NativeInterpreterImpl implements NativeInterpreter {
    public float[] run(byte[] modelBytes, float[] input, int outputLength) {
        // Identity stub: returns first outputLength entries of input
        // (or zero-padded if input shorter). Lets simulator test plumbing.
        float[] out = new float[outputLength];
        int n = Math.min(input.length, outputLength);
        System.arraycopy(input, 0, out, 0, n);
        return out;
    }

    public boolean isSupported() {
        return true;
    }
}
