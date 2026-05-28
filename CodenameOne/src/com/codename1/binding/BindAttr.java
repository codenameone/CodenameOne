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
package com.codename1.binding;

/// Picks which property of a `Component` a `@Bind` field mirrors. Only TEXT
/// and SELECTED participate in two-way bindings; the rest are write-only from
/// model to component.
public enum BindAttr {
    /// `getText` / `setText` on `Label`, `TextField`, `TextArea`, `Button`,
    /// `SpanLabel`, `SpanButton`. Two-way for editable text inputs.
    TEXT,
    /// `getUIID` / `setUIID`. Write-only.
    UIID,
    /// `isVisible` / `setVisible`. Inverted -- `true` hides the component.
    /// Write-only.
    HIDDEN,
    /// `isVisible` / `setVisible`. Write-only.
    VISIBLE,
    /// `isEnabled` / `setEnabled`. Write-only.
    ENABLED,
    /// `isSelected` / `setSelected` on `CheckBox`, `RadioButton`, and any
    /// other selectable component. Two-way.
    SELECTED,
    /// `getIcon` -- accepts a String resource name; the binder runs it
    /// through `Resources.getGlobalResources().getImage(name)`. Write-only.
    ICON_NAME,
    /// `getName` / `setName`. Write-only.
    NAME
}
