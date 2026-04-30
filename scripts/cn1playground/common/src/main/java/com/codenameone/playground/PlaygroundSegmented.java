package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

final class PlaygroundSegmented extends Container {
    interface Listener {
        void onSegmentSelected(String key);
    }

    static final class Option {
        final String key;
        final String label;
        final char icon;

        Option(String key, String label, char icon) {
            this.key = key;
            this.label = label;
            this.icon = icon;
        }
    }

    private final List<Option> options = new ArrayList<Option>();
    private final List<Button> buttons = new ArrayList<Button>();
    private final Listener listener;
    private boolean iconsOnly;
    private boolean darkMode;
    private String selectedKey;

    PlaygroundSegmented(Option[] opts, String initialKey, boolean darkMode, Listener listener) {
        super(BoxLayout.x());
        this.listener = listener;
        this.darkMode = darkMode;
        setUIID(darkMode ? "PlaygroundSegmentDark" : "PlaygroundSegment");
        for (int i = 0; i < opts.length; i++) {
            options.add(opts[i]);
            Button btn = new Button();
            btn.setTextPosition(Component.RIGHT);
            btn.setGap(Display.getInstance().convertToPixels(1.3f));
            final String key = opts[i].key;
            btn.addActionListener(e -> selectInternal(key, true));
            buttons.add(btn);
            add(btn);
        }
        setSelected(initialKey);
    }

    void setSelected(String key) {
        selectInternal(key, false);
    }

    String getSelectedKey() {
        return selectedKey;
    }

    void setIconsOnly(boolean iconsOnly) {
        this.iconsOnly = iconsOnly;
        applyStyles();
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        setUIID(dark ? "PlaygroundSegmentDark" : "PlaygroundSegment");
        applyStyles();
    }

    private void selectInternal(String key, boolean fire) {
        selectedKey = key;
        applyStyles();
        if (fire && listener != null) {
            listener.onSegmentSelected(key);
        }
    }

    private void applyStyles() {
        for (int i = 0; i < options.size(); i++) {
            Option opt = options.get(i);
            Button btn = buttons.get(i);
            boolean selected = opt.key.equals(selectedKey);
            String uiid;
            if (selected) {
                uiid = darkMode ? "PlaygroundSegmentOptionSelectedDark" : "PlaygroundSegmentOptionSelected";
            } else {
                uiid = darkMode ? "PlaygroundSegmentOptionDark" : "PlaygroundSegmentOption";
            }
            btn.setUIID(uiid);
            btn.setText(iconsOnly ? "" : opt.label);
            if (opt.icon != 0) {
                FontImage.setMaterialIcon(btn, opt.icon, 2.8f);
            }
        }
        refreshLayout();
    }

    private void refreshLayout() {
        if (getComponentForm() != null) {
            revalidate();
        }
    }
}
