namespace com.codename1.testnatives{


public class MyNativeInterfaceImpl : IMyNativeInterfaceImpl {
    public double[] getDouble() {
        return new double[]{1, 2, 3, -1};
    }

    public byte[] getBytes() {
        return new byte[]{ 1, 2, 3, -1};
    }

    public int[] getInts() {
        return new int[]{1, 2, 3, -1};
    }

    public int[] setInts(int[] param) {
        return param;
    }

    public double[] setDoubles(double[] param) {
        return param;
    }

    public byte[] setBytes(byte[] param) {
        return param;
    }

    public bool isSupported() {
        return true;
    }

}
}
