package com.codename1.initializr;

import com.codename1.system.NativeInterface;

/**
 * Platform hook for handing the user a freshly-generated file to download.
 * Necessary on the JavaScript port because the standard CN1
 * ``Display.execute(file:// path)`` flow there round-trips through
 * LocalForage/IndexedDB, and the bundled localforage's setItem callback
 * returns null even when the input Uint8Array is non-null -- the value
 * never lands in storage, exists() returns false, and the user is stuck
 * forever on a "Generating..." toast.
 *
 * Implementations on real platforms (JS port) wrap the bytes in a Blob and
 * trigger an in-page download (Generate is always a user-gesture call so
 * browser policies allow the programmatic click). The JavaSE stub returns
 * false so the caller falls back to writing the bytes to the file system
 * and opening with the OS handler.
 */
public interface DownloadNative extends NativeInterface {
    /**
     * Hand the bytes to the platform's download UI under ``fileName``.
     * Returns true on success, false if the platform doesn't support
     * this fast path -- the caller should then fall back to its
     * file-system + execute() flow.
     */
    boolean downloadBytes(String fileName, byte[] bytes);
}
