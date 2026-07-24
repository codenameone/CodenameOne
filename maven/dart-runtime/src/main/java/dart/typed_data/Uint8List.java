package dart.typed_data;

import java.util.List;

/**
 * Dart's {@code dart:typed_data} Uint8List: a fixed-length list of unsigned
 * 8-bit integers backed by a Java {@code byte[]}. Only the surface used by
 * the Flutter gallery (construction + length) is implemented.
 */
public final class Uint8List {

    private final byte[] bytes;

    public Uint8List(long length) {
        this.bytes = new byte[(int) length];
    }

    private Uint8List(byte[] bytes) {
        this.bytes = bytes;
    }

    /** {@code Uint8List.fromList(<int>[...])}. */
    public static Uint8List fromList(List<?> elements) {
        byte[] b = new byte[elements.size()];
        for (int i = 0; i < b.length; i++) {
            Object o = elements.get(i);
            b[i] = o instanceof Number ? ((Number) o).byteValue() : 0;
        }
        return new Uint8List(b);
    }

    public long length() {
        return bytes.length;
    }

    /** The raw backing array (used by image decoders). */
    public byte[] toBytes() {
        return bytes;
    }
}
