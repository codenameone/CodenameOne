package com.codename1.flutter.services;

import com.codename1.ui.Display;
import dart.async.Future;

/**
 * System clipboard access, mirroring Flutter's {@code Clipboard}. Backed by
 * CN1's {@code Display.copyToClipboard}/{@code getPasteDataFromClipboard}.
 */
public abstract class Clipboard {

    private Clipboard() {
    }

    public static Future<Object> setData(ClipboardData data) {
        if (data != null && Display.isInitialized()) {
            Display.getInstance().copyToClipboard(data.text());
        }
        return Future.value((Object) null);
    }

    public static Future<Object> getData(String format) {
        Object contents = Display.isInitialized() ? Display.getInstance().getPasteDataFromClipboard() : null;
        return Future.value((Object) new ClipboardData(contents == null ? null : contents.toString()));
    }
}
