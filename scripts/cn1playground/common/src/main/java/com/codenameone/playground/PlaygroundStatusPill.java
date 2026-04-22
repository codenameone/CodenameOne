package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;

final class PlaygroundStatusPill extends Container {
    private final Label dot;
    private final Label text;
    private boolean darkMode;
    private boolean failed;

    PlaygroundStatusPill(boolean darkMode) {
        super(new FlowLayout(Container.CENTER, Container.CENTER));
        this.darkMode = darkMode;
        dot = new Label(" ");
        dot.setPreferredW(Display.getInstance().convertToPixels(1.8f));
        dot.setPreferredH(Display.getInstance().convertToPixels(1.8f));
        RoundBorder round = RoundBorder.create()
                .rectangle(false)
                .color(0x36A853);
        dot.getAllStyles().setBorder(round);
        dot.getAllStyles().setBgTransparency(255);
        text = new Label("Live");
        add(dot);
        add(text);
        applyState(false, darkMode, "Live");
    }

    void setDarkMode(boolean dark) {
        this.darkMode = dark;
        applyState(failed, dark, text.getText());
    }

    void showLive() {
        failed = false;
        applyState(false, darkMode, "Live");
    }

    void showFailed() {
        failed = true;
        applyState(true, darkMode, "Build failed");
    }

    boolean isFailed() {
        return failed;
    }

    private void applyState(boolean error, boolean dark, String label) {
        text.setText(label);
        String pillUiid;
        int dotColor;
        if (error) {
            pillUiid = dark ? "PlaygroundStatusPillErrorDark" : "PlaygroundStatusPillError";
            dotColor = dark ? 0xFF6B6B : 0xD93636;
        } else {
            pillUiid = dark ? "PlaygroundStatusPillDark" : "PlaygroundStatusPill";
            dotColor = dark ? 0x5FCF7C : 0x36A853;
        }
        setUIID(pillUiid);
        text.setUIID(pillUiid);
        RoundBorder round = RoundBorder.create()
                .rectangle(false)
                .color(dotColor);
        dot.getAllStyles().setBorder(round);
        dot.getAllStyles().setBgColor(dotColor);
        dot.getAllStyles().setBgTransparency(255);
        if (getComponentForm() != null) {
            revalidate();
        }
    }
}
