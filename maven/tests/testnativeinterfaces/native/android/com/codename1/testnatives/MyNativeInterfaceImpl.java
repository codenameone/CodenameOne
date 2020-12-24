package com.codename1.testnatives;

import java.util.Arrays;

public class MyNativeInterfaceImpl {
    public double[] getDouble() {
        return new double[]{1, 2, 3, -1};
    }

    public byte[] getBytes() {
        return new byte[]{1, 2, 3, -1};
    }

    public int[] getInts() {
        return new int[]{1, 2, 3, -1};
    }

    public int[] setInts(int[] param) {
        Arrays.toString(param);
        return param;
    }

    public double[] setDoubles(double[] param) {
        Arrays.toString(param);
        return param;
    }

    public byte[] setBytes(byte[] param) {
        Arrays.toString(param);
        return param;
    }

    public boolean isSupported() {
        return true;
    }

}
