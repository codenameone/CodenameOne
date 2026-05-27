package com.codename1.ai.tflite;


public class NativeInterpreterImpl {
    public float[] run(byte[] modelBytes, float[] input, int outputLength) {
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.allocateDirect(modelBytes.length);
        bb.order(java.nio.ByteOrder.nativeOrder());
        bb.put(modelBytes);
        bb.rewind();
        org.tensorflow.lite.Interpreter interp = new org.tensorflow.lite.Interpreter(bb);
        float[][] out = new float[1][outputLength];
        interp.run(new float[][]{input}, out);
        interp.close();
        return out[0];
    }

    public boolean isSupported() {
        return true;
    }
}
