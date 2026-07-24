package com.codename1.flutter;

import dart.typed_data.Uint8List;

/**
 * An {@link ImageProvider} that decodes an image from an in-memory byte buffer
 * — Flutter's {@code MemoryImage}. new_gallery uses it for asset thumbnails it
 * has already loaded into a {@code Uint8List}.
 */
public class MemoryImage extends ImageProvider {

    private final Uint8List bytes;
    private double scale = 1.0;

    public MemoryImage(Uint8List bytes) {
        this.bytes = bytes;
    }

    /** Named parameter setter for the Dart {@code scale:} parameter. */
    public void scale(double v) {
        this.scale = v;
    }

    public Uint8List getBytes() {
        return bytes;
    }

    public double getScale() {
        return scale;
    }

    @Override
    public String sourceKey() {
        return "memory:" + System.identityHashCode(bytes) + "@" + scale;
    }
}
