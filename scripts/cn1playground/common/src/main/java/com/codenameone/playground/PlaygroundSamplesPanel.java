package com.codenameone.playground;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.ArrayList;
import java.util.List;

final class PlaygroundSamplesPanel {
    interface Listener {
        void onSampleSelected(PlaygroundExamples.Sample sample);
    }

    private final PlaygroundSidePanel panel;
    private final TextField search;
    private final Container listContainer;
    private final List<Button> itemButtons = new ArrayList<Button>();
    private final Listener listener;
    private String selectedSlug;
    private boolean darkMode;

    PlaygroundSamplesPanel(boolean darkMode, Runnable onClose, Listener listener) {
        this.darkMode = darkMode;
        this.listener = listener;
        panel = new PlaygroundSidePanel("SAMPLES", darkMode, onClose);

        search = new TextField();
        search.setHint("Search");
        search.setSingleLineTextArea(true);
        search.setUIID(darkMode ? "PlaygroundSearchFieldDark" : "PlaygroundSearchField");
        applySearchHintIcon(search);
        search.addDataChangedListener((type, index) -> rebuildList());

        listContainer = new Container(BoxLayout.y());
        listContainer.getAllStyles().setBgTransparency(0);

        Container header = new Container(new BorderLayout());
        header.getAllStyles().setBgTransparency(0);
        header.add(BorderLayout.CENTER, search);

        Container body = panel.getBody();
        body.add(header);
        body.add(listContainer);

        rebuildList();
    }

    Component getComponent() {
        return panel;
    }

    void setSelected(String slug) {
        selectedSlug = slug;
        applyItemStyles();
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        panel.applyTheme(dark);
        search.setUIID(dark ? "PlaygroundSearchFieldDark" : "PlaygroundSearchField");
        applySearchHintIcon(search);
        applyItemStyles();
    }

    /// Render a magnifying-glass glyph on the hint label so the hint reads
    /// "[search icon] Search" until the user starts typing, at which point
    /// the hint hides and only the user's text remains.
    private static void applySearchHintIcon(TextField field) {
        Label hint = field.getHintLabel();
        if (hint == null) {
            return;
        }
        FontImage.setMaterialIcon(hint, FontImage.MATERIAL_SEARCH, 3f);
        hint.setTextPosition(Component.RIGHT);
        hint.setGap(com.codename1.ui.Display.getInstance().convertToPixels(1.3f));
    }

    private void rebuildList() {
        listContainer.removeAll();
        itemButtons.clear();
        String filter = search.getText();
        String needle = filter == null ? "" : filter.trim().toLowerCase();
        for (int i = 0; i < PlaygroundExamples.SAMPLES.length; i++) {
            PlaygroundExamples.Sample sample = PlaygroundExamples.SAMPLES[i];
            if (!needle.isEmpty() && sample.title.toLowerCase().indexOf(needle) < 0) {
                continue;
            }
            Button row = new Button(sample.title);
            row.addActionListener(e -> {
                selectedSlug = sample.slug;
                applyItemStyles();
                if (listener != null) {
                    listener.onSampleSelected(sample);
                }
            });
            itemButtons.add(row);
            listContainer.add(row);
        }
        applyItemStyles();
        if (listContainer.getComponentForm() != null) {
            listContainer.revalidate();
        }
    }

    private void applyItemStyles() {
        for (int i = 0; i < itemButtons.size(); i++) {
            Button btn = itemButtons.get(i);
            String label = btn.getText();
            PlaygroundExamples.Sample match = null;
            for (int j = 0; j < PlaygroundExamples.SAMPLES.length; j++) {
                if (PlaygroundExamples.SAMPLES[j].title.equals(label)) {
                    match = PlaygroundExamples.SAMPLES[j];
                    break;
                }
            }
            boolean selected = match != null && match.slug.equals(selectedSlug);
            String uiid;
            if (selected) {
                uiid = darkMode ? "PlaygroundSampleItemSelectedDark" : "PlaygroundSampleItemSelected";
            } else {
                uiid = darkMode ? "PlaygroundSampleItemDark" : "PlaygroundSampleItem";
            }
            btn.setUIID(uiid);
        }
    }
}
