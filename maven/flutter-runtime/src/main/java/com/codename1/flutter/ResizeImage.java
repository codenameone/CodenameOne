package com.codename1.flutter;

/**
 * An {@link ImageProvider} that wraps another provider and resizes its decoded
 * image to a target {@code width}/{@code height} — Flutter's {@code ResizeImage}.
 * The wrapped provider's identity is threaded into {@link #sourceKey()} so the
 * framework still detects source changes across in-place updates.
 */
public class ResizeImage extends ImageProvider {

    private final ImageProvider imageProvider;
    private Long width;
    private Long height;
    private Object policy;
    private boolean allowUpscaling;

    public ResizeImage(ImageProvider imageProvider) {
        this.imageProvider = imageProvider;
    }

    /** Named parameter setter for the Dart {@code width:} parameter. */
    public void width(long v) {
        this.width = v;
    }

    /** Named parameter setter for the Dart {@code height:} parameter. */
    public void height(long v) {
        this.height = v;
    }

    /** Named parameter setter for the Dart {@code policy:} parameter. */
    public void policy(Object v) {
        this.policy = v;
    }

    /** Named parameter setter for the Dart {@code allowUpscaling:} parameter. */
    public void allowUpscaling(boolean v) {
        this.allowUpscaling = v;
    }

    public ImageProvider getImageProvider() {
        return imageProvider;
    }

    public Long getWidth() {
        return width;
    }

    public Long getHeight() {
        return height;
    }

    @Override
    public String sourceKey() {
        String inner = imageProvider == null ? "null" : imageProvider.sourceKey();
        return "resize:" + width + "x" + height + ":" + inner;
    }
}
