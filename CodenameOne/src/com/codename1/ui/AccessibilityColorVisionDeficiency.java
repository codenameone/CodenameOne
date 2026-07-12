/*
 * Copyright (c) 2026 Codename One and contributors. All rights reserved.
 */
package com.codename1.ui;

/// Describes a color-vision deficiency selected in the operating system or
/// simulated by the Java SE simulator.
///
/// Applications should never infer that color alone is safe when this value is
/// {@link #NONE} or {@link #UNKNOWN}. Use text, shape, iconography, or patterns
/// in addition to color for every important distinction.
public enum AccessibilityColorVisionDeficiency {
    /// The platform reports that no color-vision correction is enabled.
    NONE,
    /// Reduced sensitivity to red light.
    PROTANOPIA,
    /// Reduced sensitivity to green light.
    DEUTERANOPIA,
    /// Reduced sensitivity to blue light.
    TRITANOPIA,
    /// Colors are presented as shades of gray.
    MONOCHROMACY,
    /// The platform doesn't expose the selected correction mode.
    UNKNOWN
}
