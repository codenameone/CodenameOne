package com.codenameone.playground;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

final class PlaygroundMenuSection extends Container {
    PlaygroundMenuSection(String text) {
        super(new BorderLayout());
        setUIID("PlaygroundMenuSection");
        Label title = new Label(text);
        title.setUIID("PlaygroundMenuSectionTitle");
        add(BorderLayout.SOUTH, title);
    }
}
