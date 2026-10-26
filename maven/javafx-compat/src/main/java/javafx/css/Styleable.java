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
package javafx.css;

import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;

/// What a style sheet selector can match against: a type, an id, style
/// classes, pseudo-class states and a parent.
///
/// The per property meta data of JavaFX (`getCssMetaData`) is not part of
/// this layer; styles reach a node through
/// `com.codename1.fxcompat.runtime.StyleTarget` instead.
public interface Styleable {

    /// Returns the name a type selector matches, `Button` for a button.
    String getTypeSelector();

    /// Returns the id an `#id` selector matches; may be `null`.
    String getId();

    /// Returns the classes a `.class` selector matches.
    ObservableList<String> getStyleClass();

    /// Returns the inline style, the text of a `style` attribute.
    String getStyle();

    /// Returns the parent selectors descend from; `null` at the root.
    Styleable getStyleableParent();

    /// Returns the pseudo-class states that are on.
    ObservableSet<PseudoClass> getPseudoClassStates();
}
