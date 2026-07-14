/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

/// Immutable configuration handed to the platform when a `TextInputClient` binds to the low level text
/// input source (see `com.codename1.impl.CodenameOneImplementation#startTextInput`). It tells the
/// platform which soft keyboard flavor to present and how aggressive autocorrect / autocapitalization
/// should be. A source editor typically disables autocorrect and autocapitalization so it does not
/// mangle code, whereas a prose editor leaves them on.
///
/// The constraint constants mirror the `TextArea` constraint bits so the same keyboard mapping the
/// framework already uses for native fields applies here.
public final class TextInputConfig {
    /// The action label shown on the keyboard's return key: a plain newline / carriage return.
    public static final int ACTION_DEFAULT = 0;

    /// The action label shown on the keyboard's return key: a "done" affordance that finishes editing.
    public static final int ACTION_DONE = 1;

    /// The action label shown on the keyboard's return key: a "next" affordance moving to the next field.
    public static final int ACTION_NEXT = 2;

    /// The action label shown on the keyboard's return key: a "search" affordance.
    public static final int ACTION_SEARCH = 3;

    /// The action label shown on the keyboard's return key: a "send" affordance.
    public static final int ACTION_SEND = 4;

    private int constraint = TextArea.ANY;
    private boolean autoCorrect = true;
    private boolean autoCapitalize = true;
    private boolean multiline = true;
    private int actionType = ACTION_DEFAULT;

    /// Creates a config with prose oriented defaults (autocorrect and autocapitalize enabled, multiline,
    /// `TextArea#ANY` constraint).
    public TextInputConfig() {
    }

    /// Returns the `TextArea` style constraint bits describing the keyboard type.
    public int getConstraint() {
        return constraint;
    }

    /// Sets the `TextArea` style constraint bits describing the keyboard type.
    ///
    /// #### Parameters
    ///
    /// - `constraint`: one of the `TextArea` constraint constants
    ///
    /// Returns this instance for chaining.
    public TextInputConfig setConstraint(int constraint) {
        this.constraint = constraint;
        return this;
    }

    /// True when the platform should apply autocorrect / predictive text.
    public boolean isAutoCorrect() {
        return autoCorrect;
    }

    /// Enables or disables platform autocorrect / predictive text. Disable this for source code.
    ///
    /// Returns this instance for chaining.
    public TextInputConfig setAutoCorrect(boolean autoCorrect) {
        this.autoCorrect = autoCorrect;
        return this;
    }

    /// True when the platform should auto capitalize.
    public boolean isAutoCapitalize() {
        return autoCapitalize;
    }

    /// Enables or disables platform auto capitalization. Disable this for source code.
    ///
    /// Returns this instance for chaining.
    public TextInputConfig setAutoCapitalize(boolean autoCapitalize) {
        this.autoCapitalize = autoCapitalize;
        return this;
    }

    /// True when the editor accepts multiple lines (the return key inserts a newline).
    public boolean isMultiline() {
        return multiline;
    }

    /// Sets whether the editor accepts multiple lines.
    ///
    /// Returns this instance for chaining.
    public TextInputConfig setMultiline(boolean multiline) {
        this.multiline = multiline;
        return this;
    }

    /// Returns the return key action type, one of the `ACTION_*` constants.
    public int getActionType() {
        return actionType;
    }

    /// Sets the return key action type, one of the `ACTION_*` constants.
    ///
    /// Returns this instance for chaining.
    public TextInputConfig setActionType(int actionType) {
        this.actionType = actionType;
        return this;
    }
}
