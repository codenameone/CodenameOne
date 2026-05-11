package com.codenameone.examples.hellocodenameone;

import com.codename1.ui.Display;
import com.codename1.ui.Form;

public class StatusBarTapDiagnosticNativeImpl {
    private int tapCount;

    public boolean simulateStatusBarTap() {
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return false;
        }
        int x = Display.getInstance().getDisplayWidth() / 2;
        f.pointerPressed(x, 0);
        f.pointerReleased(x, 0);
        tapCount++;
        return true;
    }

    public int getTapCount() {
        return tapCount;
    }

    public boolean isSupported() {
        return true;
    }
}
