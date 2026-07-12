/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

import com.codename1.ui.Component;
import java.util.List;

/// Supplies semantic children that are not represented by persistent lightweight
/// components, such as renderer-backed list rows and table cells.
public interface AccessibilityChildProvider {
    List<AccessibilityNode> getAccessibilityChildren(Component host);
}
