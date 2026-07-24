package com.codename1.flutter;

/**
 * An {@link ImageProvider} that fetches an image over the network — Flutter's
 * {@code NetworkImage}.
 */
public class NetworkImage extends ImageProvider {

    private final String url;
    private double scale = 1.0;
    private Object headers;

    public NetworkImage(String url) {
        this.url = url;
    }

    /** Named parameter setter for the Dart {@code scale:} parameter. */
    public void scale(double v) {
        this.scale = v;
    }

    /** Named parameter setter for the Dart {@code headers:} parameter. */
    public void headers(Object v) {
        this.headers = v;
    }

    public String getUrl() {
        return url;
    }

    public double getScale() {
        return scale;
    }

    @Override
    public String sourceKey() {
        return "url:" + url;
    }
}
