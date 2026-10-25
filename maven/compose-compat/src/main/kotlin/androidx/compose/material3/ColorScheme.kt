/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package androidx.compose.material3

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
/// The Material 3 color roles. The defaults are the baseline scheme.
@Immutable
data class ColorScheme(    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val inversePrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceTint: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerLowest: Color)

fun lightColorScheme(    primary: Color = Color(0xFF6750A4),
    onPrimary: Color = Color(0xFFFFFFFF),
    primaryContainer: Color = Color(0xFFEADDFF),
    onPrimaryContainer: Color = Color(0xFF21005D),
    inversePrimary: Color = Color(0xFFD0BCFF),
    secondary: Color = Color(0xFF625B71),
    onSecondary: Color = Color(0xFFFFFFFF),
    secondaryContainer: Color = Color(0xFFE8DEF8),
    onSecondaryContainer: Color = Color(0xFF1D192B),
    tertiary: Color = Color(0xFF7D5260),
    onTertiary: Color = Color(0xFFFFFFFF),
    tertiaryContainer: Color = Color(0xFFFFD8E4),
    onTertiaryContainer: Color = Color(0xFF31111D),
    background: Color = Color(0xFFFEF7FF),
    onBackground: Color = Color(0xFF1D1B20),
    surface: Color = Color(0xFFFEF7FF),
    onSurface: Color = Color(0xFF1D1B20),
    surfaceVariant: Color = Color(0xFFE7E0EC),
    onSurfaceVariant: Color = Color(0xFF49454F),
    surfaceTint: Color = Color(0xFF6750A4),
    inverseSurface: Color = Color(0xFF322F35),
    inverseOnSurface: Color = Color(0xFFF5EFF7),
    error: Color = Color(0xFFB3261E),
    onError: Color = Color(0xFFFFFFFF),
    errorContainer: Color = Color(0xFFF9DEDC),
    onErrorContainer: Color = Color(0xFF410E0B),
    outline: Color = Color(0xFF79747E),
    outlineVariant: Color = Color(0xFFCAC4D0),
    scrim: Color = Color(0xFF000000),
    surfaceBright: Color = Color(0xFFFEF7FF),
    surfaceDim: Color = Color(0xFFDED8E1),
    surfaceContainer: Color = Color(0xFFF3EDF7),
    surfaceContainerHigh: Color = Color(0xFFECE6F0),
    surfaceContainerHighest: Color = Color(0xFFE6E0E9),
    surfaceContainerLow: Color = Color(0xFFF7F2FA),
    surfaceContainerLowest: Color = Color(0xFFFFFFFF)): ColorScheme = ColorScheme(primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, error, onError, errorContainer, onErrorContainer, outline, outlineVariant, scrim, surfaceBright, surfaceDim, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest, surfaceContainerLow, surfaceContainerLowest)

fun darkColorScheme(    primary: Color = Color(0xFFD0BCFF),
    onPrimary: Color = Color(0xFF381E72),
    primaryContainer: Color = Color(0xFF4F378B),
    onPrimaryContainer: Color = Color(0xFFEADDFF),
    inversePrimary: Color = Color(0xFF6750A4),
    secondary: Color = Color(0xFFCCC2DC),
    onSecondary: Color = Color(0xFF332D41),
    secondaryContainer: Color = Color(0xFF4A4458),
    onSecondaryContainer: Color = Color(0xFFE8DEF8),
    tertiary: Color = Color(0xFFEFB8C8),
    onTertiary: Color = Color(0xFF492532),
    tertiaryContainer: Color = Color(0xFF633B48),
    onTertiaryContainer: Color = Color(0xFFFFD8E4),
    background: Color = Color(0xFF141218),
    onBackground: Color = Color(0xFFE6E0E9),
    surface: Color = Color(0xFF141218),
    onSurface: Color = Color(0xFFE6E0E9),
    surfaceVariant: Color = Color(0xFF49454F),
    onSurfaceVariant: Color = Color(0xFFCAC4D0),
    surfaceTint: Color = Color(0xFFD0BCFF),
    inverseSurface: Color = Color(0xFFE6E0E9),
    inverseOnSurface: Color = Color(0xFF322F35),
    error: Color = Color(0xFFF2B8B5),
    onError: Color = Color(0xFF601410),
    errorContainer: Color = Color(0xFF8C1D18),
    onErrorContainer: Color = Color(0xFFF9DEDC),
    outline: Color = Color(0xFF938F99),
    outlineVariant: Color = Color(0xFF49454F),
    scrim: Color = Color(0xFF000000),
    surfaceBright: Color = Color(0xFF3B383E),
    surfaceDim: Color = Color(0xFF141218),
    surfaceContainer: Color = Color(0xFF211F26),
    surfaceContainerHigh: Color = Color(0xFF2B2930),
    surfaceContainerHighest: Color = Color(0xFF36343B),
    surfaceContainerLow: Color = Color(0xFF1D1B20),
    surfaceContainerLowest: Color = Color(0xFF0F0D13)): ColorScheme = ColorScheme(primary, onPrimary, primaryContainer, onPrimaryContainer, inversePrimary, secondary, onSecondary, secondaryContainer, onSecondaryContainer, tertiary, onTertiary, tertiaryContainer, onTertiaryContainer, background, onBackground, surface, onSurface, surfaceVariant, onSurfaceVariant, surfaceTint, inverseSurface, inverseOnSurface, error, onError, errorContainer, onErrorContainer, outline, outlineVariant, scrim, surfaceBright, surfaceDim, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest, surfaceContainerLow, surfaceContainerLowest)

/// The color that goes on `background`: its `on` role, or the content color in
/// effect when the color is not one of the scheme's.
fun ColorScheme.contentColorFor(backgroundColor: Color): Color = when (backgroundColor) {
    primary -> onPrimary
    secondary -> onSecondary
    tertiary -> onTertiary
    background -> onBackground
    error -> onError
    primaryContainer -> onPrimaryContainer
    secondaryContainer -> onSecondaryContainer
    tertiaryContainer -> onTertiaryContainer
    errorContainer -> onErrorContainer
    inverseSurface -> inverseOnSurface
    surface, surfaceContainer, surfaceContainerHigh, surfaceContainerHighest, surfaceContainerLow,
    surfaceContainerLowest, surfaceBright, surfaceDim -> onSurface
    surfaceVariant -> onSurfaceVariant
    else -> Color.Unspecified
}
