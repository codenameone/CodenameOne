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
package javafx.scene.control;

/// The holder of [ButtonData], which says what a button of a dialog is
/// for.
///
/// In JavaFX this is also a control that lays buttons out in the order of
/// the platform. That control is not part of this layer: a `DialogPane`
/// shows its buttons in the order they were added. The class cannot be
/// instantiated.
public class ButtonBar {

    private ButtonBar() {
    }

    /// What a button of a dialog is for.
    public enum ButtonData {

        /// A button placed on the left.
        LEFT("L", false, false),

        /// A button placed on the right.
        RIGHT("R", false, false),

        /// A help button.
        HELP("H", false, false),

        /// A second help button.
        HELP_2("E", false, false),

        /// The button that answers yes; a default button.
        YES("Y", false, true),

        /// The button that answers no; a cancel button.
        NO("N", true, false),

        /// The button that goes on to the next step; a default button.
        NEXT_FORWARD("X", false, true),

        /// The button that goes back a step.
        BACK_PREVIOUS("B", false, false),

        /// The button that finishes; a default button.
        FINISH("I", false, true),

        /// The button that applies.
        APPLY("A", false, false),

        /// The button that cancels or closes; a cancel button.
        CANCEL_CLOSE("C", true, false),

        /// The button that accepts; a default button.
        OK_DONE("O", false, true),

        /// A button with no meaning of its own.
        OTHER("U", false, false),

        /// A large gap between buttons.
        BIG_GAP("+", false, false),

        /// A small gap between buttons.
        SMALL_GAP("_", false, false);

        private final String typeCode;
        private final boolean cancelButton;
        private final boolean defaultButton;

        ButtonData(String type, boolean cancelButton, boolean defaultButton) {
            this.typeCode = type;
            this.cancelButton = cancelButton;
            this.defaultButton = defaultButton;
        }

        /// Returns the letter that stands for this kind in a button order.
        public String getTypeCode() {
            return typeCode;
        }

        /// Returns whether a button of this kind cancels its dialog.
        public final boolean isCancelButton() {
            return cancelButton;
        }

        /// Returns whether a button of this kind is the default button.
        public final boolean isDefaultButton() {
            return defaultButton;
        }
    }
}
