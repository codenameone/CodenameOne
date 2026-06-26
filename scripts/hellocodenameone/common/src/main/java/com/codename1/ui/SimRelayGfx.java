package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;

/**
 * Relay access to the live CodenameOneImplementation.
 *
 * The relay replays draw batches by invoking the impl's native-graphics APIs
 * directly -- impl.fillRect(ng, ...), impl.drawString(ng, ...),
 * impl.flushGraphics() -- exactly as core's Graphics does. It NEVER allocates a
 * Graphics (Graphics is a single global object owned by Display). The
 * "native graphics" is just the opaque surface pointer that identifies the draw
 * target: the display via getNativeGraphics(), or a mutable image via
 * getNativeGraphics(imagePeer). Display.getImplementation() is package-private
 * to com.codename1.ui, so this shim lives in that package.
 */
public final class SimRelayGfx {
    private SimRelayGfx() {
    }

    public static CodenameOneImplementation impl() {
        return Display.getInstance().getImplementation();
    }
}
