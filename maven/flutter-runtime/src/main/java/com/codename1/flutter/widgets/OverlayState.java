package com.codename1.flutter.widgets;

import dart.core.DartList;

/**
 * The mutable state of an {@link Overlay} — Flutter's {@code OverlayState}. Entries
 * are inserted above/below existing ones. This pass records the insertions; the
 * floating paint pass lands with the full overlay renderer.
 */
public class OverlayState {

    public void insert(OverlayEntry entry, OverlayEntry below, OverlayEntry above) {
    }

    public void insertAll(DartList<OverlayEntry> entries, OverlayEntry below, OverlayEntry above) {
    }
}
