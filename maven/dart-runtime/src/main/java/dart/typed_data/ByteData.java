package dart.typed_data;

/**
 * Dart's {@code dart:typed_data} ByteData: a fixed-length, random-access view
 * over a byte buffer with typed accessors. Minimal big-endian implementation
 * covering the 8- and 32-bit integer accessors.
 */
public final class ByteData {

    private final byte[] buffer;

    public ByteData(long length) {
        this.buffer = new byte[(int) length];
    }

    public long lengthInBytes() {
        return buffer.length;
    }

    public long getUint8(long byteOffset) {
        return buffer[(int) byteOffset] & 0xFF;
    }

    public void setUint8(long byteOffset, long value) {
        buffer[(int) byteOffset] = (byte) value;
    }

    public long getInt32(long byteOffset) {
        int o = (int) byteOffset;
        return ((buffer[o] & 0xFF) << 24)
                | ((buffer[o + 1] & 0xFF) << 16)
                | ((buffer[o + 2] & 0xFF) << 8)
                | (buffer[o + 3] & 0xFF);
    }

    public void setInt32(long byteOffset, long value) {
        int o = (int) byteOffset;
        buffer[o] = (byte) (value >> 24);
        buffer[o + 1] = (byte) (value >> 16);
        buffer[o + 2] = (byte) (value >> 8);
        buffer[o + 3] = (byte) value;
    }
}
