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

package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Dialog;
import com.codename1.ui.Form;

/// The dialog the storage browser opens when a file is viewed.
class IoStorageContentFigure implements GuideFigure {

    private final IoStorageListFigure browser = new IoStorageListFigure();

    @Override
    public String id() {
        return "storage-content";
    }

    /// The same storage browser the figure above shows; the chapter includes its
    /// listing once.
    @Override
    public Form build() {
        return browser.build();
    }

    /// The view button reads the file and hands it to Dialog.show, which is
    /// modal and never returns while nothing is there to dismiss it. The same
    /// dialog is built and shown modelessly instead -- same title, same text,
    /// same place.
    @Override
    public void afterShow(Form form) {
        Dialog d = new Dialog("itinerary.txt");
        d.setLayout(new com.codename1.ui.layouts.BorderLayout());
        d.add(com.codename1.ui.layouts.BorderLayout.CENTER,
                new com.codename1.components.SpanLabel("Meet at the main lobby at 9am.", "DialogBody"));
        d.showModeless();
    }
}
