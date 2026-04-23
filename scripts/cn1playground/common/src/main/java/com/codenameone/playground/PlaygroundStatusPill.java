package com.codenameone.playground;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.RoundBorder;
import com.codename1.ui.plaf.Style;

final class PlaygroundStatusPill extends Container {
    private final Label dot;
    private final Label text;
    private boolean darkMode;
    private boolean failed;

    PlaygroundStatusPill(boolean darkMode) {
        super(BoxLayout.x());
        this.darkMode = darkMode;

        // The dot is a circle rendered via RoundBorder. It's built as its own
        // wrapper so BoxLayout.x's center alignment lines it up with the label
        // regardless of font ascent differences.
        dot = new Label(" ");
        int dotSize = Display.getInstance().convertToPixels(1.8f);
        dot.setPreferredW(dotSize);
        dot.setPreferredH(dotSize);
        dot.getAllStyles().setPadding(0, 0, 0, 0);
        dot.getAllStyles().setMargin(0, 0, 0, 0);
        dot.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        dot.getAllStyles().setMarginUnit(Style.UNIT_TYPE_PIXELS);

        text = new Label("Live");
        text.getAllStyles().setPadding(0, 0, 0, 0);

        // Wrap the dot in a centering container; BoxLayout.x aligns children to
        // their own top edge, so wrapping in FlowLayout(CENTER, CENTER) gives us
        // a cross-axis-centered dot.
        Container dotWrap = new Container(new com.codename1.ui.layouts.FlowLayout(Component.CENTER, Component.CENTER));
        dotWrap.getAllStyles().setBgTransparency(0);
        dotWrap.getAllStyles().setPadding(0, 0, 0, 0);
        dotWrap.getAllStyles().setMargin(0, 0, 0, 1);
        dotWrap.getAllStyles().setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        dotWrap.getAllStyles().setMarginUnit(Style.UNIT_TYPE_PIXELS);
        dotWrap.add(dot);

        Container textWrap = new Container(new com.codename1.ui.layouts.FlowLayout(Component.LEFT, Component.CENTER));
        textWrap.getAllStyles().setBgTransparency(0);
        textWrap.getAllStyles().setPadding(0, 0, 0, 0);
        textWrap.getAllStyles().setMargin(0, 0, 0, 0);
        textWrap.add(text);

        add(dotWrap);
        add(textWrap);
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
        RoundBorder circle = RoundBorder.create()
                .rectangle(false)
                .color(dotColor);
        dot.getAllStyles().setBorder(circle);
        dot.getAllStyles().setBgColor(dotColor);
        dot.getAllStyles().setBgTransparency(255);
        if (getComponentForm() != null) {
            revalidate();
        }
    }
}
