package @PACKAGE@;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.codename1.ui.Container;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class HelloCodenameOneInstrumentedTest {

    private static final int CHUNK_SIZE = 2000;
    private static final String PREVIEW_CHANNEL = "PREVIEW";
    private static final int[] PREVIEW_JPEG_QUALITIES = new int[] {60, 50, 40, 35, 30, 25, 20, 15, 10};
    private static final int MAX_PREVIEW_BYTES = 600 * 1024; // 600 KiB target for comment previews
    private static final String MAIN_SCREEN_TEST = "MainActivity";
    private static final String BROWSER_TEST = "BrowserComponent";

    private static void println(String s) {
        System.out.println(s);
    }

    private static void settle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private static ActivityScenario<Activity> launchMainActivity(Context ctx) {
        String pkg = "@PACKAGE@";
        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            q.setPackage(pkg);
            launch = q;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        println("CN1SS:INFO: launching activity for test");
        return ActivityScenario.launch(launch);
    }

    private static final class ScreenshotCapture {
        final byte[] png;
        final byte[] previewJpeg;
        final int previewQuality;

        ScreenshotCapture(byte[] png, byte[] previewJpeg, int previewQuality) {
            this.png = png;
            this.previewJpeg = previewJpeg;
            this.previewQuality = previewQuality;
        }
    }

    private static ScreenshotCapture captureScreenshot(ActivityScenario<Activity> scenario, String testName) {
        final byte[][] holder = new byte[2][];
        final int[] qualityHolder = new int[1];
        scenario.onActivity(activity -> {
            try {
                View root = activity.getWindow().getDecorView().getRootView();
                int w = root.getWidth();
                int h = root.getHeight();
                if (w <= 0 || h <= 0) {
                    DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                    w = Math.max(1, dm.widthPixels);
                    h = Math.max(1, dm.heightPixels);
                    int sw = View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY);
                    int sh = View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY);
                    root.measure(sw, sh);
                    root.layout(0, 0, w, h);
                    println("CN1SS:INFO:test=" + testName + " forced layout to " + w + "x" + h);
                } else {
                    println("CN1SS:INFO:test=" + testName + " natural layout " + w + "x" + h);
                }

                Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(bmp);
                root.draw(c);

                ByteArrayOutputStream pngOut = new ByteArrayOutputStream(Math.max(1024, w * h / 2));
                if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, pngOut)) {
                    throw new RuntimeException("Bitmap.compress returned false");
                }
                holder[0] = pngOut.toByteArray();
                println(
                        "CN1SS:INFO:test="
                                + testName
                                + " png_bytes="
                                + holder[0].length);

                int chosenQuality = 0;
                byte[] chosenPreview = null;
                int smallestBytes = Integer.MAX_VALUE;

                for (int quality : PREVIEW_JPEG_QUALITIES) {
                    ByteArrayOutputStream jpegOut = new ByteArrayOutputStream(Math.max(1024, w * h / 2));
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, quality, jpegOut)) {
                        continue;
                    }
                    byte[] jpegBytes = jpegOut.toByteArray();
                    int length = jpegBytes.length;
                    if (length < smallestBytes) {
                        smallestBytes = length;
                        chosenQuality = quality;
                        chosenPreview = jpegBytes;
                    }
                    if (length <= MAX_PREVIEW_BYTES) {
                        break;
                    }
                }

                holder[1] = chosenPreview;
                qualityHolder[0] = chosenQuality;
                if (chosenPreview != null) {
                    println(
                            "CN1SS:INFO:test="
                                    + testName
                                    + " preview_jpeg_bytes="
                                    + chosenPreview.length
                                    + " preview_quality="
                                    + chosenQuality);
                } else {
                    println("CN1SS:INFO:test=" + testName + " preview_jpeg_bytes=0 preview_quality=0");
                }
                bmp.recycle();
            } catch (Throwable t) {
                println("CN1SS:ERR:test=" + testName + " " + t);
                t.printStackTrace(System.out);
            }
        });
        if (holder[0] == null) {
            return new ScreenshotCapture(null, null, 0);
        }
        return new ScreenshotCapture(holder[0], holder[1], qualityHolder[0]);
    }

    private static String sanitizeTestName(String testName) {
        return testName.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static void emitScreenshot(ScreenshotCapture capture, String testName) {
        if (capture == null || capture.png == null || capture.png.length == 0) {
            println("CN1SS:END:" + sanitizeTestName(testName));
            Assert.fail("Screenshot capture produced 0 bytes for " + testName);
            return;
        }
        emitScreenshotChannel(capture.png, testName, "");
        if (capture.previewJpeg != null && capture.previewJpeg.length > 0) {
            emitScreenshotChannel(capture.previewJpeg, testName, PREVIEW_CHANNEL);
        }
    }

    private static void emitScreenshotChannel(byte[] bytes, String testName, String channel) {
        String safeName = sanitizeTestName(testName);
        String prefix = "CN1SS";
        if (channel != null && channel.length() > 0) {
            prefix += channel;
        }
        if (bytes == null || bytes.length == 0) {
            println(prefix + ":END:" + safeName);
            return;
        }
        String b64 = Base64.encodeToString(bytes, Base64.NO_WRAP);
        int count = 0;
        for (int pos = 0; pos < b64.length(); pos += CHUNK_SIZE) {
            int end = Math.min(pos + CHUNK_SIZE, b64.length());
            String chunk = b64.substring(pos, end);
            System.out.println(
                    prefix
                            + ":"
                            + safeName
                            + ":"
                            + String.format(Locale.US, "%06d", pos)
                            + ":"
                            + chunk);
            count++;
        }
        println("CN1SS:INFO:test=" + safeName + " chunks=" + count + " total_b64_len=" + b64.length());
        System.out.println(prefix + ":END:" + safeName);
        System.out.flush();
    }

    private static void prepareBrowserComponentContent(ActivityScenario<Activity> scenario) throws InterruptedException {
        final CountDownLatch supportLatch = new CountDownLatch(1);
        final boolean[] supported = new boolean[1];

        scenario.onActivity(activity -> Display.getInstance().callSerially(() -> {
            try {
                supported[0] = BrowserComponent.isNativeBrowserSupported();
            } finally {
                supportLatch.countDown();
            }
        }));

        if (!supportLatch.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timed out while verifying BrowserComponent support");
        }

        Assume.assumeTrue("BrowserComponent native support required for this test", supported[0]);

        final CountDownLatch loadLatch = new CountDownLatch(1);
        final String html = "<html><head><meta charset='utf-8'/>"
                + "<style>body{margin:0;font-family:sans-serif;background:#0e1116;color:#f3f4f6;}"
                + ".container{padding:24px;text-align:center;}h1{font-size:24px;margin-bottom:12px;}"
                + "p{font-size:16px;line-height:1.4;}span{color:#4cc9f0;}</style></head>"
                + "<body><div class='container'><h1>Codename One</h1>"
                + "<p>BrowserComponent <span>instrumentation</span> test content.</p></div></body></html>";

        scenario.onActivity(activity -> Display.getInstance().callSerially(() -> {
            Form current = Display.getInstance().getCurrent();
            if (current == null) {
                current = new Form("Browser Test", new BorderLayout());
                current.show();
            } else {
                current.setLayout(new BorderLayout());
                current.setTitle("Browser Test");
                current.removeAll();
            }

            BrowserComponent browser = new BrowserComponent();
            browser.addWebEventListener(BrowserComponent.onLoad, evt -> loadLatch.countDown());
            browser.setPage(html, null);
            current.add(BorderLayout.CENTER, browser);
            current.revalidate();
        }));

        if (!loadLatch.await(10, TimeUnit.SECONDS)) {
            Assert.fail("Timed out waiting for BrowserComponent to load content");
        }
    }

    private static void prepareMainActivityContent(ActivityScenario<Activity> scenario) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        scenario.onActivity(activity -> Display.getInstance().callSerially(() -> {
            try {
                Form current = Display.getInstance().getCurrent();
                if (current == null) {
                    current = new Form("Main Screen", new BorderLayout());
                    current.show();
                } else {
                    current.setLayout(new BorderLayout());
                    current.setTitle("Main Screen");
                    current.removeAll();
                }

                Container content = new Container(BoxLayout.y());
                content.getAllStyles().setBgColor(0x1f2937);
                content.getAllStyles().setBgTransparency(255);
                content.getAllStyles().setPadding(6, 6, 6, 6);
                content.getAllStyles().setFgColor(0xf9fafb);

                Label heading = new Label("Hello Codename One");
                heading.getAllStyles().setFgColor(0x38bdf8);
                heading.getAllStyles().setMargin(0, 4, 0, 0);

                Label body = new Label("Instrumentation main activity preview");
                body.getAllStyles().setFgColor(0xf9fafb);

                content.add(heading);
                content.add(body);

                current.add(BorderLayout.CENTER, content);
                current.revalidate();
            } finally {
                latch.countDown();
            }
        }));

        if (!latch.await(5, TimeUnit.SECONDS)) {
            Assert.fail("Timed out preparing main activity content");
        }
    }

    @Test
    public void testUseAppContext_andEmitScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        String pkg = "@PACKAGE@";
        Assert.assertEquals("Package mismatch", pkg, ctx.getPackageName());

        ScreenshotCapture capture;
        try (ActivityScenario<Activity> scenario = launchMainActivity(ctx)) {
            settle(750);
            prepareMainActivityContent(scenario);
            settle(500);
            capture = captureScreenshot(scenario, MAIN_SCREEN_TEST);
        }

        emitScreenshot(capture, MAIN_SCREEN_TEST);
    }

    @Test
    public void testBrowserComponentScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        ScreenshotCapture capture;

        try (ActivityScenario<Activity> scenario = launchMainActivity(ctx)) {
            settle(750);
            prepareBrowserComponentContent(scenario);
            settle(500);
            capture = captureScreenshot(scenario, BROWSER_TEST);
        }

        emitScreenshot(capture, BROWSER_TEST);
    }
}
