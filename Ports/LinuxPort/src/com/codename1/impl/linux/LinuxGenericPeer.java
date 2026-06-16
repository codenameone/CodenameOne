package com.codename1.impl.linux;

import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.geom.Dimension;

/// Generic native-peer (child HWND) placement for the Linux port, the analog of
/// iOS's `NativeIPhoneView`. When an app `@NativeInterface` returns a native peer,
/// the core boxes the HWND as a `long[]` and asks the impl to wrap it; this class
/// drives the symmetric native lifecycle (`peerInitialized` / `peerSetBounds` /
/// `peerSetVisible` / `peerDeinitialized` in cn1_linux_peer.cpp) so the child
/// window tracks the lightweight component's bounds.
///
/// The offscreen screenshot pipeline renders the Form into a WIC bitmap where a
/// live child HWND is not composited, so during transitions / lightweight mode the
/// peer falls back to an image captured with `PrintWindow` (`peerCaptureArgb`),
/// mirroring the WebView2 peer's cached-capture approach.
class LinuxGenericPeer extends PeerComponent {
    private final long peer;
    private boolean lightweight;

    LinuxGenericPeer(Object nativePeer) {
        super(nativePeer);
        this.peer = ((long[]) nativePeer)[0];
    }

    long peer() {
        return peer;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        LinuxNative.peerInitialized(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
    }

    @Override
    protected void deinitialize() {
        Image img = generatePeerImage();
        if (img != null) {
            setPeerImage(img);
        }
        LinuxNative.peerDeinitialized(peer);
        super.deinitialize();
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        LinuxNative.peerSetBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
    }

    @Override
    protected Dimension calcPreferredSize() {
        Display d = Display.getInstance();
        int[] wh = new int[2];
        LinuxNative.peerCalcPreferredSize(peer, d.getDisplayWidth(), d.getDisplayHeight(), wh);
        if (wh[0] <= 0 || wh[1] <= 0) {
            // No intrinsic size yet (the HWND may not be realized): a sensible default.
            return new Dimension(d.getDisplayWidth() / 2, d.getDisplayHeight() / 4);
        }
        return new Dimension(wh[0], wh[1]);
    }

    @Override
    protected Image generatePeerImage() {
        int[] dims = new int[2];
        int[] argb = LinuxNative.peerCaptureArgb(peer, dims);
        if (argb == null || dims[0] <= 0 || dims[1] <= 0) {
            return null;
        }
        try {
            return Image.createImage(argb, dims[0], dims[1]);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected boolean shouldRenderPeerImage() {
        return lightweight || !isInitialized();
    }

    @Override
    protected void setLightweightMode(boolean l) {
        if (lightweight != l) {
            lightweight = l;
            LinuxNative.peerSetVisible(peer, !l);
        }
    }
}
