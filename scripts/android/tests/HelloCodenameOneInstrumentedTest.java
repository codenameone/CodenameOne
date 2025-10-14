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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

@RunWith(AndroidJUnit4.class)
public class HelloCodenameOneInstrumentedTest {

    private static void println(String s) {
        System.out.println(s);
    }

    @Test
    public void testUseAppContext_andEmitScreenshot() throws Exception {
        Context ctx = ApplicationProvider.getApplicationContext();
        String pkg = "@PACKAGE@";
        Assert.assertEquals("Package mismatch", pkg, ctx.getPackageName());

        Intent launch = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (launch == null) {
            Intent q = new Intent(Intent.ACTION_MAIN);
            q.addCategory(Intent.CATEGORY_LAUNCHER);
            q.setPackage(pkg);
            launch = q;
        }
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        println("CN1SS:INFO: about to launch Activity");
        byte[] pngBytes = null;

        try (ActivityScenario<Activity> scenario = ActivityScenario.launch(launch)) {
            Thread.sleep(750);

            println("CN1SS:INFO: activity launched");

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
                        println("CN1SS:INFO: forced layout to " + w + "x" + h);
                    } else {
                        println("CN1SS:INFO: natural layout " + w + "x" + h);
                    }

                    Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(bmp);
                    root.draw(c);

                    ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(1024, w * h / 2));
                    boolean ok = bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    if (!ok) {
                        throw new RuntimeException("Bitmap.compress returned false");
                    }
                    holder[0] = baos.toByteArray();
                    println("CN1SS:INFO: png_bytes=" + holder[0].length);
                } catch (Throwable t) {
                    println("CN1SS:ERR: onActivity " + t);
                    t.printStackTrace(System.out);
                }
            });

            pngBytes = holder[0];
        } catch (Throwable t) {
            println("CN1SS:ERR: launch " + t);
            t.printStackTrace(System.out);
        }

        if (pngBytes == null || pngBytes.length == 0) {
            println("CN1SS:END");
            Assert.fail("Screenshot capture produced 0 bytes");
            return;
        }

        String b64 = Base64.encodeToString(pngBytes, Base64.NO_WRAP);
        final int chunkSize = 2000;
        int count = 0;
        for (int pos = 0; pos < b64.length(); pos += chunkSize) {
            int end = Math.min(pos + chunkSize, b64.length());
            System.out.println("CN1SS:" + String.format("%06d", pos) + ":" + b64.substring(pos, end));
            count++;
        }
        println("CN1SS:INFO: chunks=" + count + " total_b64_len=" + b64.length());
        System.out.println("CN1SS:END");
        System.out.flush();
    }
}
