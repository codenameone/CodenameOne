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

import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

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

    private static byte[] captureScreenshot(ActivityScenario<Activity> scenario, String testName) {
        final byte[][] holder = new byte[1][];
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

                ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(1024, w * h / 2));
                if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)) {
                    throw new RuntimeException("Bitmap.compress returned false");
                }
                holder[0] = baos.toByteArray();
                println("CN1SS:INFO:test=" + testName + " png_bytes=" + holder[0].length);
            } catch (Throwable t) {
                println("CN1SS:ERR:test=" + testName + " " + t);
                t.printStackTrace(System.out);
            }
        });
        return holder[0];
    }

    private static String sanitizeTestName(String testName) {
        return testName.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private static void emitScreenshot(byte[] pngBytes, String testName) {
        String safeName = sanitizeTestName(testName);
        if (pngBytes == null || pngBytes.length == 0) {
            println("CN1SS:END:" + safeName);
            Assert.fail("Screenshot capture produced 0 bytes for " + testName);
            return;
        }
        String b64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP);
        int count = 0;
        for (int pos = 0; pos < b64.length(); pos += CHUNK_SIZE) {
            int end = Math.min(pos + CHUNK_SIZE, b64.length());
            String chunk = b64.substring(pos, end);
            System.out.println("CN1SS:" + safeName + ":" + String.format(Locale.US, "%06d", pos) + ":" + chunk);
            count++;
        }
        println("CN1SS:INFO:test=" + safeName + " chunks=" + count + " total_b64_len=" + b64.length());
        System.out.println("CN1SS:END:" + safeName);
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

    @Test
    public void testUseAppContext_andEmitScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        String pkg = "@PACKAGE@";
        Assert.assertEquals("Package mismatch", pkg, ctx.getPackageName());

        byte[] pngBytes;
        try (ActivityScenario<Activity> scenario = launchMainActivity(ctx)) {
            settle(750);
            pngBytes = captureScreenshot(scenario, MAIN_SCREEN_TEST);
        }

        emitScreenshot(pngBytes, MAIN_SCREEN_TEST);
    }

    @Test
    public void testBrowserComponentScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        byte[] pngBytes;

        try (ActivityScenario<Activity> scenario = launchMainActivity(ctx)) {
            settle(750);
            prepareBrowserComponentContent(scenario);
            settle(500);
            pngBytes = captureScreenshot(scenario, BROWSER_TEST);
        }

        emitScreenshot(pngBytes, BROWSER_TEST);
    }
}
