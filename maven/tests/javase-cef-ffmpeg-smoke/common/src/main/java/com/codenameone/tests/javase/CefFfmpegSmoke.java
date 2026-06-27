package com.codenameone.tests.javase;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.media.Media;
import com.codename1.media.MediaManager;
import com.codename1.system.Lifecycle;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.ImageIO;
import com.codename1.ui.util.UITimer;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CefFfmpegSmoke extends Lifecycle {
    private static final String SCREENSHOT_NAME = "cef-ffmpeg-smoke.png";
    private static final String STATUS_NAME = "cef-ffmpeg-smoke-status.txt";
    private static final int CAPTURE_DELAY_MS = 3000;
    private static final int FALLBACK_TIMEOUT_MS = 20000;

    private Form current;
    private Media media;
    private boolean browserLoaded;
    private boolean browserJsVerified;
    private String browserColor = "";
    private String browserReadyState = "";
    private boolean videoFrameRendered;
    private int videoFrameCount;
    private String videoAverageColor = "";
    private boolean cefLoaded;
    private String displayImplementationClass = "";
    private String browserPeerClass = "";
    private boolean browserPeerReady;
    private boolean captureScheduled;
    private boolean captured;

    @Override
    public void init(Object context) {
        CN.updateNetworkThreadCount(2);
        Toolbar.setGlobalToolbar(true);
        Display.getInstance().setProperty("BrowserComponent.useCEF", "true");
    }

    @Override
    public void runApp() {
        start();
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }

        String videoPath = System.getProperty("cn1.test.video", "");
        if (videoPath.length() == 0) {
            failAndExit("Missing cn1.test.video system property");
            return;
        }

        Form form = new Form("CEF + FFmpeg Smoke", new BorderLayout());
        form.getAllStyles().setPadding(0, 0, 0, 0);
        form.getAllStyles().setMargin(0, 0, 0, 0);

        Container content = new Container(new GridLayout(1, 2));
        clearSpacing(content);

        BrowserComponent browser = new BrowserComponent();
        browser.getAllStyles().setPadding(0, 0, 0, 0);
        browser.getAllStyles().setMargin(0, 0, 0, 0);
        browser.setPreferredW(320);
        browser.setPreferredH(240);
        browser.addWebEventListener(BrowserComponent.onLoad, evt -> {
            probeBrowser(browser, form);
        });
        browser.setPage("<html><body style='margin:0;background:#00ff00;'></body></html>", null);

        Component videoComponent;
        try {
            media = MediaManager.createMedia(videoPath, true, null);
            media.setNativePlayerMode(false);
            videoComponent = media.getVideoComponent();
            videoComponent.setPreferredW(320);
            videoComponent.setPreferredH(240);
        } catch (IOException ex) {
            failAndExit("Failed to create media: " + ex.getMessage());
            return;
        }

        content.add(browser);
        content.add(videoComponent);
        form.add(BorderLayout.CENTER, content);
        form.add(BorderLayout.SOUTH, new Label("Browser should be green; video should be red."));

        current = form;
        form.show();

        final UITimer[] browserProbe = new UITimer[1];
        browserProbe[0] = UITimer.timer(500, true, form, () -> {
            if (captured) {
                browserProbe[0].cancel();
                return;
            }
            if (probeBrowser(browser, form)) {
                browserProbe[0].cancel();
            }
        });

        UITimer.timer(300, false, form, () -> {
            try {
                if (media != null) {
                    media.setTime(0);
                    media.play();
                }
            } catch (RuntimeException ex) {
                failAndExit("Failed to start playback: " + ex.getMessage());
                return;
            }
            maybeScheduleCapture(form);
        });

        UITimer.timer(FALLBACK_TIMEOUT_MS, false, form, () -> {
            if (!captured) {
                captureAndExit("fallback-timeout");
            }
        });
    }

    public void stop() {
        current = CN.getCurrentForm();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = CN.getCurrentForm();
        }
        if (media != null) {
            media.pause();
        }
    }

    public void destroy() {
        if (media != null) {
            media.cleanup();
            media = null;
        }
    }

    private void maybeScheduleCapture(Form form) {
        inspectMediaState();
        if (captureScheduled || captured || !browserPeerReady || media == null || !videoFrameRendered) {
            return;
        }
        captureScheduled = true;
        UITimer.timer(CAPTURE_DELAY_MS, false, form, () -> captureAndExit("browser-peer-and-video-ready"));
    }

    private void captureAndExit(String reason) {
        if (captured) {
            return;
        }
        captured = true;
        writeStatus("captureReason=" + reason + "\n" +
                "cefLoaded=" + cefLoaded + "\n" +
                "displayImplementationClass=" + displayImplementationClass + "\n" +
                "browserPeerClass=" + browserPeerClass + "\n" +
                "browserPeerReady=" + browserPeerReady + "\n" +
                "videoFrameRendered=" + videoFrameRendered + "\n" +
                "videoFrameCount=" + videoFrameCount + "\n" +
                "videoAverageColor=" + videoAverageColor + "\n" +
                "browserLoaded=" + browserLoaded + "\n" +
                "browserJsVerified=" + browserJsVerified + "\n" +
                "browserReadyState=" + browserReadyState + "\n" +
                "browserColor=" + browserColor + "\n" +
                "mediaImplementation=" + System.getProperty("cn1.javase.mediaImplementation", "") + "\n" +
                "browserImplementation=" + System.getProperty("cn1.javase.implementation", "") + "\n");
        Display.getInstance().screenshot(img -> {
            if (img == null) {
                failAndExit("Screenshot callback returned null");
                return;
            }
            try {
                saveScreenshot(img);
                Log.p("SMOKE:screenshot=" + getAppHomePath() + SCREENSHOT_NAME);
            } catch (IOException ex) {
                Log.e(ex);
                writeStatus("error=failed-to-save-screenshot\nmessage=" + ex.getMessage() + "\n");
            } finally {
                img.dispose();
                if (media != null) {
                    media.pause();
                }
                Display.getInstance().exitApplication();
            }
        });
    }

    private void failAndExit(String message) {
        Log.p("SMOKE:FAIL:" + message);
        writeStatus("error=" + message + "\n");
        Display.getInstance().exitApplication();
    }

    private void saveScreenshot(Image img) throws IOException {
        String screenshotPath = getAppHomePath() + SCREENSHOT_NAME;
        try (OutputStream os = FileSystemStorage.getInstance().openOutputStream(screenshotPath)) {
            ImageIO io = ImageIO.getImageIO();
            if (io == null) {
                throw new IOException("ImageIO unavailable");
            }
            io.save(img, os, ImageIO.FORMAT_PNG, 1f);
        }
    }

    private void writeStatus(String text) {
        String statusPath = getAppHomePath() + STATUS_NAME;
        try (OutputStream os = FileSystemStorage.getInstance().openOutputStream(statusPath)) {
            os.write(text.getBytes("UTF-8"));
        } catch (IOException ex) {
            Log.e(ex);
        }
    }

    private boolean probeBrowser(BrowserComponent browser, Form form) {
        inspectBrowserBackend(browser);
        inspectMediaState();
        try {
            browserReadyState = browser.executeAndReturnString("document.readyState");
            browserColor = browser.executeAndReturnString("window.getComputedStyle(document.body).backgroundColor");
            browserLoaded = browserReadyState != null && browserReadyState.length() > 0;
            browserJsVerified = browserColor != null && browserColor.indexOf("0, 255, 0") >= 0;
            if (browserLoaded || browserPeerReady) {
                maybeScheduleCapture(form);
            }
            return browserLoaded;
        } catch (RuntimeException ex) {
            browserReadyState = "error:" + ex.getMessage();
            browserColor = "";
            browserJsVerified = false;
            if (browserPeerReady) {
                maybeScheduleCapture(form);
            }
            return false;
        }
    }

    private void inspectBrowserBackend(BrowserComponent browser) {
        cefLoaded = isCefLoaded();
        displayImplementationClass = getDisplayImplementationClass();
        browserPeerClass = getBrowserPeerClass(browser);
        browserPeerReady = browserPeerClass.length() > 0;
    }

    private void inspectMediaState() {
        if (media == null) {
            videoFrameRendered = false;
            videoFrameCount = 0;
            return;
        }
        Object rendered = media.getVariable("ffmpeg.videoFrameRendered");
        videoFrameRendered = Boolean.TRUE.equals(rendered);
        Object count = media.getVariable("ffmpeg.videoFrameCount");
        if (count instanceof Number) {
            videoFrameCount = ((Number) count).intValue();
        } else {
            videoFrameCount = 0;
        }
        Object avg = media.getVariable("ffmpeg.videoAverageColor");
        videoAverageColor = avg == null ? "" : String.valueOf(avg);
    }

    private boolean isCefLoaded() {
        try {
            Class<?> cls = Class.forName("com.codename1.impl.javase.CN1Bootstrap");
            Method m = cls.getMethod("isCEFLoaded");
            Object out = m.invoke(null);
            return Boolean.TRUE.equals(out);
        } catch (Throwable ex) {
            return false;
        }
    }

    private String getDisplayImplementationClass() {
        try {
            Method m = Display.class.getDeclaredMethod("getImplementation");
            m.setAccessible(true);
            Object impl = m.invoke(Display.getInstance());
            return impl == null ? "" : impl.getClass().getName();
        } catch (Throwable ex) {
            return "error:" + ex.getClass().getName();
        }
    }

    private String getBrowserPeerClass(BrowserComponent browser) {
        try {
            Field f = BrowserComponent.class.getDeclaredField("internal");
            f.setAccessible(true);
            Object peer = f.get(browser);
            return peer == null ? "" : peer.getClass().getName();
        } catch (Throwable ex) {
            return "error:" + ex.getClass().getName();
        }
    }

    private String getAppHomePath() {
        return FileSystemStorage.getInstance().getAppHomePath();
    }

    private static void clearSpacing(Container cnt) {
        Style style = cnt.getAllStyles();
        style.setPadding(0, 0, 0, 0);
        style.setMargin(0, 0, 0, 0);
    }
}
