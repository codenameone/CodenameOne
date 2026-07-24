package com.codename1.flutter;

/**
 * Identifies an image without committing to how it is loaded — Flutter's
 * {@code ImageProvider}. Concrete providers: {@link AssetImage},
 * {@link NetworkImage}.
 */
public abstract class ImageProvider<T> {

    /**
     * A stable identity for the image source, used by consumers to detect
     * source changes across in-place widget updates.
     */
    public abstract String sourceKey();
}
