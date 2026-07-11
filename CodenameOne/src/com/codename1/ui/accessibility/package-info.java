/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */

/// Defines the portable accessibility semantics tree for Codename One's
/// lightweight user-interface components. Applications use this package to
/// describe roles, states, values, ranges, actions, traversal, grouping,
/// live regions, panes, collections, and virtual descendants. Platform ports
/// translate the resulting immutable tree to their native accessibility APIs.
///
/// The entry point for configuring a component is
/// {@link com.codename1.ui.Component#getSemantics()}. Tests and development
/// tools can inspect a resolved tree with
/// {@link com.codename1.ui.accessibility.AccessibilityInspector} and validate
/// it with {@link com.codename1.ui.accessibility.AccessibilityAssertions}.
package com.codename1.ui.accessibility;
