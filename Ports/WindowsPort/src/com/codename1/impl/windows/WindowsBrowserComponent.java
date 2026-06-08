package com.codename1.impl.windows;

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.UITimer;

/// Native Windows BrowserComponent peer backed by a WebView2 instance (the
/// native lifecycle lives in cn1_windows_browser.cpp). The component is rendered
/// from a cached image: the native side CapturePreview's the WebView2 to PNG
/// bytes after each navigation, which `generatePeerImage()` turns into the peer
/// image that `PeerComponent.paint()` draws (so it appears in the offscreen
/// Direct2D screenshot, where the live WebView2 visual would not). The peer polls
/// the native event queue to fire `onLoad` and to route the JS return-value
/// bridge (a cancelled navigation to a `/!cn1return/` URL) into the
/// BrowserComponent's navigation callbacks.
class WindowsBrowserComponent extends PeerComponent {
    private final long peer;
    private final BrowserComponent browser;
    private UITimer poller;

    WindowsBrowserComponent(BrowserComponent browser) {
        super(null);
        this.browser = browser;
        this.peer = WindowsNative.browserCreate(800, 600);
    }

    long peer() {
        return peer;
    }

    @Override
    protected boolean shouldRenderPeerImage() {
        return true;
    }

    @Override
    protected Image generatePeerImage() {
        byte[] png = WindowsNative.browserCapturePng(peer);
        if (png == null) {
            return null;
        }
        try {
            return EncodedImage.create(png);
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    protected Dimension calcPreferredSize() {
        return new Dimension(Display.getInstance().getDisplayWidth() / 2,
                Display.getInstance().getDisplayHeight() / 2);
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        WindowsNative.browserSetBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
        if (poller == null && getComponentForm() != null) {
            poller = UITimer.timer(60, true, getComponentForm(), new Runnable() {
                public void run() {
                    poll();
                }
            });
        }
    }

    @Override
    protected void deinitialize() {
        if (poller != null) {
            poller.cancel();
            poller = null;
        }
        super.deinitialize();
    }

    @Override
    protected void onPositionSizeChange() {
        super.onPositionSizeChange();
        WindowsNative.browserSetBounds(peer, getAbsoluteX(), getAbsoluteY(), getWidth(), getHeight());
    }

    void setHtml(String html) {
        WindowsNative.browserSetHtml(peer, html);
    }

    void setUrl(String url) {
        WindowsNative.browserSetUrl(peer, url);
    }

    void execute(String js) {
        WindowsNative.browserExecute(peer, js);
    }

    void destroy() {
        WindowsNative.browserDestroy(peer);
    }

    private void poll() {
        String ev;
        while ((ev = WindowsNative.browserPollEvent(peer)) != null) {
            if ("LOAD".equals(ev)) {
                Image img = generatePeerImage();
                if (img != null) {
                    setPeerImage(img);
                    repaint();
                }
                browser.fireWebEvent(BrowserComponent.onLoad, new ActionEvent(""));
            } else if (ev.startsWith("NAV|")) {
                browser.fireBrowserNavigationCallbacks(ev.substring(4));
            }
        }
    }
}
