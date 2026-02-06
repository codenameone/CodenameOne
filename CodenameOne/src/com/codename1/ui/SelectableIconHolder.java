/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

/// An interface that is implemented by "selectable" components that hold icons, such as `Button`,
/// `SpanButton`, `MultiButton`, etc...  This interface includes
/// methods for managing different icons for different component states (e.g. pressed, disabled, etc..).
///
/// @author shannah
///
/// #### Since
///
/// 7.0
public interface SelectableIconHolder extends IconHolder {

    /// Indicates the icon that is displayed on the button when the button is in
    /// rolled over state
    ///
    /// #### Returns
    ///
    /// icon used
    ///
    /// #### See also
    ///
    /// - Button#STATE_ROLLOVER
    Image getRolloverIcon();

    /// Indicates the icon that is displayed on the button when the button is in
    /// rolled over state
    ///
    /// #### Parameters
    ///
    /// - `rolloverIcon`: icon to use
    ///
    /// #### See also
    ///
    /// - Button#STATE_ROLLOVER
    void setRolloverIcon(Image rolloverIcon);

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state
    ///
    /// #### Returns
    ///
    /// icon used
    ///
    /// #### See also
    ///
    /// - Button#STATE_PRESSED
    Image getPressedIcon();

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state
    ///
    /// #### Parameters
    ///
    /// - `pressedIcon`: icon used
    ///
    /// #### See also
    ///
    /// - Button#STATE_PRESSED
    void setPressedIcon(Image pressedIcon);

    /// Indicates the icon that is displayed on the button when the button is in
    /// the disabled state
    ///
    /// #### Returns
    ///
    /// icon used
    Image getDisabledIcon();

    /// Indicates the icon that is displayed on the button when the button is in
    /// the disabled state
    ///
    /// #### Parameters
    ///
    /// - `disabledIcon`: icon used
    void setDisabledIcon(Image disabledIcon);

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state and is selected. This is ONLY applicable to toggle buttons
    ///
    /// #### Returns
    ///
    /// icon used
    Image getRolloverPressedIcon();

    /// Indicates the icon that is displayed on the button when the button is in
    /// pressed state and is selected. This is ONLY applicable to toggle buttons
    ///
    /// #### Parameters
    ///
    /// - `rolloverPressedIcon`: icon used
    void setRolloverPressedIcon(Image rolloverPressedIcon);

    /// Returns the icon for the button based on its current state
    ///
    /// #### Returns
    ///
    /// the button icon based on its current state
    Image getIconFromState();

}
