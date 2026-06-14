package com.codename1.initializr;

import com.codename1.system.NativeInterface;

/**
 * JavaScript-port-only file download. The storage-backed
 * {@code Display.execute(file:// URL)} path is broken on the JS port
 * (localforage getItem/exists never sees the just-written file), and the
 * worker-side {@code @JSBody invokeHostNative} save-blob bridge does not
 * reach the host. This routes the download through the proven NativeInterface
 * dispatch ({@code __cn1_native_interface_call__}), whose JS implementation
 * runs on the main thread and drives an {@code <a download>} click directly.
 *
 * Implemented only for the JavaScript port; on other platforms
 * {@link com.codename1.system.NativeLookup#create} returns {@code null} and
 * the generator falls back to the storage + execute path that works there.
 */
public interface JsDownloader extends NativeInterface {
    /**
     * Offers the given data: URL to the user as a download.
     *
     * @param fileName suggested file name
     * @param dataUrl a {@code data:} URL holding the file contents
     * @return true if the download was triggered
     */
    boolean download(String fileName, String dataUrl);
}
