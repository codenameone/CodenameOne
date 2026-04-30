package com.codenameone.playground;

import com.codename1.l10n.L10NManager;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import java.util.Date;
import java.util.List;

final class PlaygroundHistoryPanel {
    interface Listener {
        void onHistorySelected(PlaygroundStateStore.HistoryEntry entry);
    }

    private final PlaygroundSidePanel panel;
    private final Container listContainer;
    private final Listener listener;
    private boolean darkMode;
    private List<PlaygroundStateStore.HistoryEntry> currentEntries;

    PlaygroundHistoryPanel(boolean darkMode, Runnable onClose, Listener listener) {
        this.darkMode = darkMode;
        this.listener = listener;
        panel = new PlaygroundSidePanel("HISTORY", darkMode, onClose);
        listContainer = new Container(BoxLayout.y());
        listContainer.getAllStyles().setBgTransparency(0);
        panel.getBody().add(listContainer);
    }

    Component getComponent() {
        return panel;
    }

    void setEntries(List<PlaygroundStateStore.HistoryEntry> entries) {
        currentEntries = entries;
        rebuild();
    }

    void applyTheme(boolean dark) {
        this.darkMode = dark;
        panel.applyTheme(dark);
        rebuild();
    }

    private void rebuild() {
        listContainer.removeAll();
        if (currentEntries == null || currentEntries.isEmpty()) {
            Label empty = new Label("No saved runs yet");
            empty.setUIID(darkMode ? "PlaygroundPropEmptyDark" : "PlaygroundPropEmpty");
            listContainer.add(empty);
        } else {
            for (int i = 0; i < currentEntries.size(); i++) {
                listContainer.add(createRow(currentEntries.get(i)));
            }
        }
        if (listContainer.getComponentForm() != null) {
            listContainer.revalidate();
        }
    }

    private Component createRow(PlaygroundStateStore.HistoryEntry entry) {
        Label line1 = new Label(entry.detail(currentEntries));
        line1.setUIID(darkMode ? "PlaygroundHistoryLine1Dark" : "PlaygroundHistoryLine1");
        Label line2 = new Label(formatRelative(entry.timestamp));
        line2.setUIID(darkMode ? "PlaygroundHistoryLine2Dark" : "PlaygroundHistoryLine2");

        Container lines = new Container(BoxLayout.y());
        lines.getAllStyles().setBgTransparency(0);
        lines.add(line1);
        lines.add(line2);

        Button row = new Button();
        row.setUIID(darkMode ? "PlaygroundHistoryItemDark" : "PlaygroundHistoryItem");
        row.setText("");
        row.addActionListener(e -> {
            if (listener != null) {
                listener.onHistorySelected(entry);
            }
        });

        Container wrapper = new Container(new com.codename1.ui.layouts.BorderLayout());
        wrapper.setLeadComponent(row);
        wrapper.add(com.codename1.ui.layouts.BorderLayout.CENTER, lines);
        wrapper.add(com.codename1.ui.layouts.BorderLayout.EAST, row);
        wrapper.getAllStyles().setBgTransparency(0);
        // Align row text to the 3 mm left padding used by the panel header so
        // entries line up underneath the "HISTORY" title. setPadding takes
        // (top, bottom, left, right), so left=3 puts the text column under
        // the header's 3mm inset.
        wrapper.getAllStyles().setPaddingUnit(com.codename1.ui.plaf.Style.UNIT_TYPE_DIPS);
        wrapper.getAllStyles().setPadding(1, 1, 3, 2);
        return wrapper;
    }

    private String formatRelative(long timestamp) {
        if (timestamp <= 0) {
            return "Earlier";
        }
        long now = System.currentTimeMillis();
        long diff = Math.max(0, now - timestamp);
        long sec = diff / 1000;
        if (sec < 60) {
            return "Just now";
        }
        long min = sec / 60;
        if (min < 60) {
            return min + (min == 1 ? " min ago" : " min ago");
        }
        long hours = min / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }
        long days = hours / 24;
        if (days < 2) {
            return "Yesterday " + L10NManager.getInstance().formatDateTimeShort(new Date(timestamp));
        }
        return L10NManager.getInstance().formatDateTimeShort(new Date(timestamp));
    }
}
