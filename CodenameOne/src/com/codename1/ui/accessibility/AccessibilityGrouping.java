/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui.accessibility;

/// Controls how a component and its descendants participate in the semantic tree.
public enum AccessibilityGrouping {
    /// Infer exposure from the role, content, actions, and component type.
    AUTO,
    /// Expose this node and omit all semantic descendants.
    LEAF,
    /// Expose this node and its semantic descendants.
    GROUP,
    /// Expose a single node whose label and description include its descendants.
    MERGE_DESCENDANTS,
    /// Omit this node but promote its semantic children.
    EXCLUDE,
    /// Omit this node and its entire semantic subtree.
    EXCLUDE_SUBTREE
}
