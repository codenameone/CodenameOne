/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.fxcompat.runtime;

/// The one entry point through which a style sheet engine sets the visual
/// attributes of a node. Every `javafx.scene.Node` implements it.
///
/// #### The call
///
/// `cn1ApplyStyle(property, value)` takes the JavaFX CSS property name,
/// spelled exactly as in a style sheet and in lower case (`-fx-text-fill`),
/// and a value that is already parsed:
///
/// - a colour or gradient: `javafx.scene.paint.Paint`
/// - a length or a plain number: `Number`, lengths in logical pixels
/// - four lengths: `javafx.geometry.Insets`
/// - corner radii: `javafx.scene.layout.CornerRadii`, or a `Number` for
///   one radius on every corner
/// - a font: `javafx.scene.text.Font`
/// - an alignment: `javafx.geometry.Pos`
/// - an enumerated keyword: the enum constant, or its CSS spelling as a
///   `String` (`"bold"`, `"round"`, `"dashed"`); the string is matched
///   without regard to ASCII case
/// - a flag: `Boolean`
/// - text: `String`
///
/// It answers `true` when the node took the value. It answers `false`,
/// and changes nothing, when the node does not have that property or the
/// value is of a type the property cannot use; an engine must treat that
/// as "this declaration does not apply", never as an error.
///
/// #### Undoing a value
///
/// The first time a property is styled the node remembers the value it had.
/// `cn1ApplyStyle(property, null)` puts that value back and forgets it,
/// which is how an engine withdraws a declaration that stopped matching
/// (a `:hover` rule when the pointer leaves). It answers whether the node
/// has the property. A value the application sets through the ordinary
/// setter while a style is in force is overwritten by the next styled
/// value and is not what a later `null` restores.
///
/// #### Shorthands
///
/// The engine expands shorthands that carry several values: `-fx-font`
/// arrives as one `Font`; the per side forms of `-fx-padding`,
/// `-fx-border-width` and `-fx-background-insets` arrive as one `Insets`.
/// Layered backgrounds and borders are not supported: one paint, one set of
/// radii and one set of insets per node.
///
/// #### Property names by node family
///
/// - every node: `-fx-opacity`, `-fx-rotate`, `-fx-scale-x`, `-fx-scale-y`,
///   `-fx-translate-x`, `-fx-translate-y` (`Number`); `-fx-cursor`
///   (`javafx.scene.Cursor`); `visibility` (`Boolean`, or `"visible"`,
///   `"hidden"`, `"collapse"`); `-fx-managed` (`Boolean`)
/// - `Region` and everything below it, controls included:
///   `-fx-background-color` (`Paint`), `-fx-background-radius`
///   (`CornerRadii` or `Number`), `-fx-background-insets` (`Insets` or
///   `Number`), `-fx-border-color` (`Paint`), `-fx-border-width`
///   (`Insets`, `BorderWidths` or `Number`), `-fx-border-radius`
///   (`CornerRadii` or `Number`), `-fx-border-style` (`BorderStrokeStyle`
///   or `"none"`, `"solid"`, `"dashed"`, `"dotted"`), `-fx-padding`
///   (`Insets` or `Number`), `-fx-min-width`, `-fx-pref-width`,
///   `-fx-max-width`, `-fx-min-height`, `-fx-pref-height`,
///   `-fx-max-height` (`Number`), `-fx-snap-to-pixel` (`Boolean`)
/// - `HBox`, `VBox`: `-fx-spacing` (`Number`), `-fx-alignment` (`Pos`),
///   `-fx-fill-height` / `-fx-fill-width` (`Boolean`)
/// - `StackPane`: `-fx-alignment`
/// - `FlowPane`, `TilePane`, `GridPane`: `-fx-hgap`, `-fx-vgap`
///   (`Number`), `-fx-alignment`; `FlowPane` and `TilePane` also
///   `-fx-orientation` (`javafx.geometry.Orientation` or its name)
/// - `Labeled` (labels, buttons, check boxes...): `-fx-text-fill`
///   (`Paint`), `-fx-font` (`Font`), `-fx-font-size` (`Number`),
///   `-fx-font-family` (`String`), `-fx-font-weight` (`FontWeight`, a
///   `Number` 100-900 or a keyword), `-fx-font-style` (`FontPosture` or a
///   keyword), `-fx-alignment`, `-fx-text-alignment` (`TextAlignment` or a
///   keyword), `-fx-wrap-text`, `-fx-underline` (`Boolean`),
///   `-fx-graphic-text-gap` (`Number`), `-fx-content-display`
///   (`ContentDisplay` or its name)
///
/// - text input controls (`TextField`, `PasswordField`, `TextArea`):
///   `-fx-text-fill`, `-fx-prompt-text-fill` (`Paint`), and the font
///   names of `Labeled`
/// - `ScrollPane`: `-fx-fit-to-width`, `-fx-fit-to-height`,
///   `-fx-pannable` (`Boolean`), `-fx-hbar-policy`, `-fx-vbar-policy`
///   (`ScrollPane.ScrollBarPolicy`, or `"never"`, `"always"`,
///   `"as-needed"`)
/// - `Shape` and everything below it: `-fx-fill`, `-fx-stroke` (`Paint`;
///   here `null` cannot mean "no paint", since it restores),
///   `-fx-stroke-width`, `-fx-stroke-miter-limit`,
///   `-fx-stroke-dash-offset` (`Number`), `-fx-stroke-line-cap`,
///   `-fx-stroke-line-join`, `-fx-stroke-type` (the enum or its keyword),
///   `-fx-smooth` (`Boolean`)
/// - `Text`, besides the shape names: the font names of `Labeled`,
///   `-fx-text-alignment`, `-fx-text-origin` (`VPos` or its keyword),
///   `-fx-underline`, `-fx-strikethrough` (`Boolean`),
///   `-fx-line-spacing` (`Number`)
///
/// A class that takes names of its own repeats them in its description,
/// where it overrides `cn1StyleValue` and `cn1SetStyleValue`.
public interface StyleTarget {

    /// Sets, or with a `null` value restores, one styleable attribute.
    /// See the interface description for the names and the value types.
    boolean cn1ApplyStyle(String property, Object value);
}
