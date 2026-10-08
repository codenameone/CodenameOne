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
package com.codename1.desktopcompat.java.awt.datatransfer;

import java.io.IOException;

/// A place to put a [Transferable] for something else to take.
///
/// One made with `new Clipboard(name)` is private to the application and
/// holds whatever it is given. The system clipboard, which
/// `Toolkit.getDefaultToolkit().getSystemClipboard()` answers, is the
/// device's: text put on it can be pasted into other applications, and
/// text copied elsewhere is read from it as a [StringSelection].
///
/// What differs from the desktop: there are no flavor listeners, and the
/// system clipboard carries text only.
public class Clipboard {

    protected ClipboardOwner owner;
    protected Transferable contents;
    private final String name;

    public Clipboard(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /// Puts `contents` on the clipboard. The owner of what was there
    /// before is told it lost it, unless it is the new owner too.
    public void setContents(Transferable contents, ClipboardOwner owner) {
        ClipboardOwner oldOwner = this.owner;
        Transferable oldContents = this.contents;
        this.owner = owner;
        this.contents = contents;
        if (oldOwner != null && oldOwner != owner) {
            oldOwner.lostOwnership(this, oldContents);
        }
    }

    /// What the clipboard holds, or `null` when it is empty. `requestor`
    /// is not used.
    public Transferable getContents(Object requestor) {
        return contents;
    }

    public DataFlavor[] getAvailableDataFlavors() {
        Transferable c = getContents(null);
        return c == null ? new DataFlavor[0] : c.getTransferDataFlavors();
    }

    public boolean isDataFlavorAvailable(DataFlavor flavor) {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = getContents(null);
        return c != null && c.isDataFlavorSupported(flavor);
    }

    /// What the clipboard holds, in one flavor.
    ///
    /// #### Throws
    ///
    /// - `UnsupportedFlavorException`: if the clipboard is empty or does not
    ///   hold `flavor`
    ///
    /// - `IOException`: if the data is no longer there to read
    public Object getData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor == null) {
            throw new NullPointerException("flavor");
        }
        Transferable c = getContents(null);
        if (c == null) {
            throw new UnsupportedFlavorException(flavor);
        }
        return c.getTransferData(flavor);
    }
}
