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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;

/// An `ActionListener` that also carries named properties and an enabled
/// state, so that one action can drive several buttons and menu items.
public interface Action extends ActionListener {

    String DEFAULT = "Default";

    String NAME = "Name";

    String SHORT_DESCRIPTION = "ShortDescription";

    String LONG_DESCRIPTION = "LongDescription";

    String SMALL_ICON = "SmallIcon";

    String ACTION_COMMAND_KEY = "ActionCommandKey";

    String ACCELERATOR_KEY = "AcceleratorKey";

    String MNEMONIC_KEY = "MnemonicKey";

    String SELECTED_KEY = "SwingSelectedKey";

    String DISPLAYED_MNEMONIC_INDEX_KEY = "SwingDisplayedMnemonicIndexKey";

    String LARGE_ICON_KEY = "SwingLargeIconKey";

    Object getValue(String key);

    void putValue(String key, Object value);

    void setEnabled(boolean b);

    boolean isEnabled();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
