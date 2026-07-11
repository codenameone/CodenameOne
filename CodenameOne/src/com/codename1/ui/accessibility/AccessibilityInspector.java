/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

import com.codename1.ui.Form;

/// Read-only semantic-tree inspection entry point for tests and developer tools.
public final class AccessibilityInspector {
    private AccessibilityInspector() {
    }

    public static AccessibilityTreeSnapshot snapshot(Form form) {
        return AccessibilityManager.getInstance().getSnapshot(form);
    }

    public static AccessibilityTreeSnapshot currentSnapshot() {
        return AccessibilityManager.getInstance().getCurrentSnapshot();
    }
}
