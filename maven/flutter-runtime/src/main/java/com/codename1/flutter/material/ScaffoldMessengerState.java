package com.codename1.flutter.material;

import com.codename1.components.ToastBar;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Text;
import com.codename1.io.Log;
import com.codename1.ui.Display;

/**
 * Shows {@link SnackBar}s through CN1's {@link ToastBar}. The SnackBar's
 * content widget is consumed as a message string: a {@code Text} child
 * supplies its data; any other widget falls back to {@code toString()} with
 * a log warning (mirroring button-label consumption). Headless (no Display)
 * the message is only recorded, which keeps the consumption logic testable.
 */
public class ScaffoldMessengerState {

    private String lastMessage;
    private long lastDurationMillis;

    ScaffoldMessengerState() {
    }

    public void showSnackBar(SnackBar snackBar) {
        if (snackBar == null) {
            return;
        }
        String msg = consumeMessage(snackBar.getContent());
        long ms = snackBar.durationMillis();
        lastMessage = msg;
        lastDurationMillis = ms;
        if (!Display.isInitialized()) {
            return;
        }
        ToastBar.Status status = ToastBar.getInstance().createStatus();
        status.setMessage(msg);
        status.setExpires((int) ms);
        status.show();
    }

    private static String consumeMessage(Widget content) {
        if (content == null) {
            return "";
        }
        if (content instanceof Text) {
            String d = ((Text) content).getData();
            return d == null ? "" : d;
        }
        try {
            Log.p("Flutter runtime: SnackBar content " + content.getClass().getSimpleName()
                    + " is not a Text; using its toString() as the message");
        } catch (Throwable t) {
            // headless: Log has no storage backend
        }
        return String.valueOf(content);
    }

    /**
     * The message most recently passed to {@link #showSnackBar} (test hook).
     */
    public String lastMessage() {
        return lastMessage;
    }

    /**
     * The duration (ms) most recently passed to {@link #showSnackBar}
     * (test hook).
     */
    public long lastDurationMillis() {
        return lastDurationMillis;
    }
}
