package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

final class PlaygroundSidePanel extends Container {
    private final Label headerLabel;
    private final Button closeButton;
    private final Container body;
    private boolean darkMode;

    PlaygroundSidePanel(String title, boolean darkMode, Runnable onClose) {
        super(new BorderLayout());
        this.darkMode = darkMode;
        setPreferredW(Display.getInstance().convertToPixels(60f));
        setUIID(darkMode ? "PlaygroundSidePanelDark" : "PlaygroundSidePanel");

        headerLabel = new Label(title);
        headerLabel.setUIID(darkMode ? "PlaygroundSidePanelHeaderDark" : "PlaygroundSidePanelHeader");

        closeButton = new Button();
        closeButton.setUIID(darkMode ? "PlaygroundSidePanelCloseDark" : "PlaygroundSidePanelClose");
        FontImage.setMaterialIcon(closeButton, FontImage.MATERIAL_CLOSE, 3f);
        closeButton.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });

        Container header = new Container(new BorderLayout());
        header.getAllStyles().setBgTransparency(0);
        header.add(BorderLayout.CENTER, headerLabel);
        header.add(BorderLayout.EAST, closeButton);

        body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.getAllStyles().setBgTransparency(0);

        add(BorderLayout.NORTH, header);
        add(BorderLayout.CENTER, body);
    }

    void setTitle(String title) {
        headerLabel.setText(title);
    }

    Container getBody() {
        return body;
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        setUIID(dark ? "PlaygroundSidePanelDark" : "PlaygroundSidePanel");
        headerLabel.setUIID(dark ? "PlaygroundSidePanelHeaderDark" : "PlaygroundSidePanelHeader");
        closeButton.setUIID(dark ? "PlaygroundSidePanelCloseDark" : "PlaygroundSidePanelClose");
        FontImage.setMaterialIcon(closeButton, FontImage.MATERIAL_CLOSE, 3f);
    }

    boolean isDarkMode() {
        return darkMode;
    }

    static Component searchField(String placeholder, boolean darkMode) {
        com.codename1.ui.TextField search = new com.codename1.ui.TextField();
        search.setHint(placeholder);
        search.setSingleLineTextArea(true);
        search.setUIID(darkMode ? "PlaygroundSearchFieldDark" : "PlaygroundSearchField");
        return search;
    }
}
