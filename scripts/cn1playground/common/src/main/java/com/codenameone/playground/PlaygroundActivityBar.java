package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

final class PlaygroundActivityBar extends Container {
    interface Listener {
        void onActivitySelected(String key);
    }

    static final class Item {
        final String key;
        final String label;
        final char icon;

        Item(String key, String label, char icon) {
            this.key = key;
            this.label = label;
            this.icon = icon;
        }
    }

    static final String NONE = "";

    private final List<Item> items = new ArrayList<Item>();
    private final List<Button> buttons = new ArrayList<Button>();
    private final Listener listener;
    private boolean darkMode;
    private String activeKey = NONE;

    PlaygroundActivityBar(Item[] entries, String activeKey, boolean darkMode, Listener listener) {
        super(BoxLayout.y());
        this.listener = listener;
        this.darkMode = darkMode;
        this.activeKey = activeKey == null ? NONE : activeKey;
        setUIID(darkMode ? "PlaygroundActivityBarDark" : "PlaygroundActivityBar");
        setPreferredW(Display.getInstance().convertToPixels(11f));
        for (int i = 0; i < entries.length; i++) {
            Item entry = entries[i];
            items.add(entry);
            Button btn = new Button();
            final String key = entry.key;
            btn.addActionListener(e -> toggle(key));
            buttons.add(btn);
            add(btn);
        }
        applyStyles();
    }

    String getActiveKey() {
        return activeKey;
    }

    void setActive(String key) {
        activeKey = key == null ? NONE : key;
        applyStyles();
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        setUIID(dark ? "PlaygroundActivityBarDark" : "PlaygroundActivityBar");
        applyStyles();
    }

    private void toggle(String key) {
        if (key.equals(activeKey)) {
            activeKey = NONE;
        } else {
            activeKey = key;
        }
        applyStyles();
        if (listener != null) {
            listener.onActivitySelected(activeKey);
        }
    }

    private void applyStyles() {
        for (int i = 0; i < items.size(); i++) {
            Item entry = items.get(i);
            Button btn = buttons.get(i);
            boolean active = entry.key.equals(activeKey);
            String uiid;
            if (active) {
                uiid = darkMode ? "PlaygroundActivityButtonActiveDark" : "PlaygroundActivityButtonActive";
            } else {
                uiid = darkMode ? "PlaygroundActivityButtonDark" : "PlaygroundActivityButton";
            }
            btn.setUIID(uiid);
            FontImage.setMaterialIcon(btn, entry.icon, 4.5f);
            btn.setText("");
            btn.setTextPosition(Component.BOTTOM);
        }
        if (getComponentForm() != null) {
            revalidate();
        }
    }
}
