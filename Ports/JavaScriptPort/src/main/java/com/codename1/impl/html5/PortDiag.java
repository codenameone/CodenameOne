/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.html5;

/// Port-side diagnostic toggles. Short-lived: flip on for a targeted rebuild,
/// inspect logs, then flip off before committing long-term. Not a public API.
public final class PortDiag {
    /// Flip to true temporarily when hunting a render bug in the picker / scene.
    /// Leaves diagnostic log lines in the console including every primitive /
    /// transform op submitted. Keep off on main; trigger only on throwaway builds.
    private static final boolean PICKER_DIAG = false;

    private PortDiag() {
    }

    public static boolean isPickerDiag() {
        return PICKER_DIAG;
    }
}
