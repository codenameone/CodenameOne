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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.datatransfer.Clipboard;
import com.codename1.desktopcompat.java.awt.datatransfer.ClipboardOwner;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.StringSelection;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.java.awt.datatransfer.UnsupportedFlavorException;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import java.io.IOException;

/// The system clipboard: the Codename One clipboard, seen as an AWT one.
/// Text goes both ways; anything else an application puts here stays in
/// the application.
public final class SystemClipboard extends Clipboard {

    /// The text last handed to the device, to tell the application's own
    /// contents from something copied elsewhere since.
    private String pushed;

    public SystemClipboard() {
        super("System");
    }

    @Override
    public void setContents(Transferable contents, ClipboardOwner owner) {
        super.setContents(contents, owner);
        pushed = null;
        if (contents == null || !contents.isDataFlavorSupported(DataFlavor.stringFlavor)
                || !Display.isInitialized()) {
            return;
        }
        Object text;
        try {
            text = contents.getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException e) {
            return;
        } catch (IOException e) {
            return;
        }
        if (text instanceof String) {
            pushed = (String) text;
            Display.getInstance().copyToClipboard(pushed);
        }
    }

    /// What the device's clipboard holds now. While that is still the
    /// text this application put there, the application's own transferable
    /// is answered, with every flavor it has.
    @Override
    public Transferable getContents(Object requestor) {
        if (!Display.isInitialized()) {
            return contents;
        }
        Object now = Display.getInstance().getPasteDataFromClipboard();
        String text = null;
        if (now instanceof String) {
            text = (String) now;
        } else if (now instanceof ClipboardContent) {
            text = ((ClipboardContent) now).getText("text/plain");
        }
        if (text == null) {
            // Nothing the device can show as text: only what was put here
            // and never reached the device is still ours to answer.
            return pushed == null ? contents : null;
        }
        if (contents != null && text.equals(pushed)) {
            return contents;
        }
        return new StringSelection(text);
    }
}
