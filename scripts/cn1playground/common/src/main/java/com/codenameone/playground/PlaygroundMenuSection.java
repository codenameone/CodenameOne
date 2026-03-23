package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

final class PlaygroundMenuSection extends Container {
    PlaygroundMenuSection(String text) {
        super(new BorderLayout());
        setUIID("PlaygroundMenuSection");
        putClientProperty("playgroundThemeRole", "sideMenu");
        Label title = new Label(text);
        title.setUIID("PlaygroundMenuSectionTitle");
        title.putClientProperty("playgroundThemeRole", "sideMenu");
        add(BorderLayout.SOUTH, title);
    }
}
