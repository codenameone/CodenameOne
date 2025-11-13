package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.system.NativeInterface;

/**
 * Native helper used by the media playback screenshot test to convert an
 * absolute filesystem path into a platform-appropriate content URI when
 * running on Android. Other platforms simply return the original path.
 */
public interface MediaPlaybackNative extends NativeInterface {
    /**
     * Resolves the provided filesystem path into a content URI that the
     * Android media player can open. Non-Android implementations should
     * return the original path.
     *
     * @param absolutePath path to the generated audio sample
     * @return platform-specific URI or the original path if conversion isn't
     *         supported
     */
    String resolveContentUri(String absolutePath);
}
