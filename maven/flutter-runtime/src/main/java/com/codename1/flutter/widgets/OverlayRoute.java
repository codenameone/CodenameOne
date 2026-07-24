package com.codename1.flutter.widgets;

import com.codename1.flutter.navigation.Route;

import dart.core.DartIterable;
import dart.core.DartList;

/**
 * A {@link Route} that inserts one or more {@link OverlayEntry} objects into the
 * navigator's {@link Overlay} — Flutter's {@code OverlayRoute}. Subclasses
 * override {@link #createOverlayEntries()} to supply their entries (e.g. a page
 * plus its modal barrier). This pass captures the entries; wiring them into the
 * live overlay lands with the navigation renderer.
 *
 * @param <T> the value the route completes with when popped
 */
public class OverlayRoute<T> extends Route<T> {

    /** The overlay entries this route paints. Subclasses override. */
    public DartIterable<OverlayEntry> createOverlayEntries() {
        return DartIterable.wrap(new DartList<OverlayEntry>());
    }
}
