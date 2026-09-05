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


package com.codename1.samples;

import static com.codename1.ui.CN.*;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.ClipboardDataProvider;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDragOperation;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.OutputStream;

/**
 * Native operating system drag and drop: dragging content out of the application, and accepting
 * a drag that came from somewhere else.
 *
 * <p>Drag the first card onto a text editor and the text lands there; drag the second onto the
 * desktop or into a file manager and a real file appears -- written only at the moment of the
 * drop, because the payload promises it rather than building it. Drag anything from another
 * application onto the drop zone and it is described there.</p>
 */
public class NativeDragAndDropSample {

    private Form current;
    private Resources theme;
    private Label status;

    public void init(Object context) {
        updateNetworkThreadCount(2);
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form f = new Form("Native Drag and Drop", new BorderLayout());

        status = new Label(NativeDragAndDrop.isSupported()
                ? (NativeDragAndDrop.isDragOutsideApplicationSupported()
                        ? "Drags can leave this application"
                        : "Drags work inside this application only")
                : "This platform has no native drag and drop");

        Container body = new Container(BoxLayout.y());
        body.add(status);
        body.add(textDragSource());
        body.add(fileDragSource());
        body.add(dropZone());

        f.add(BorderLayout.CENTER, body);
        f.show();
    }

    /** A card that drags plain text and HTML, so every receiver takes the best form it knows. */
    private Component textDragSource() {
        Label card = new Label("Drag me into a text editor");
        card.getStyle().setBorder(Border.createLineBorder(2, 0x3366cc));
        card.getAllStyles().setPadding(8, 8, 8, 8);

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "Dragged out of Codename One")
                .setData(ClipboardContent.MIME_HTML,
                        "<b>Dragged</b> out of <i>Codename One</i>");
        card.setNativeDragOperation(new NativeDragOperation(content)
                .setAllowedActions(NativeDragOperation.ACTION_COPY)
                .setLabel("Codename One text"));
        return card;
    }

    /**
     * A card that drags a file which does not exist yet.
     *
     * <p>The file is registered as a provider rather than written up front, so a drag the user
     * abandons costs nothing: the provider only runs if a receiver actually asks for the file
     * list, which is what dropping on the desktop or in a file manager does.</p>
     */
    private Component fileDragSource() {
        Label card = new Label("Drag me onto the desktop");
        card.getStyle().setBorder(Border.createLineBorder(2, 0x33aa55));
        card.getAllStyles().setPadding(8, 8, 8, 8);

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "codenameone-note.txt")
                .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    @Override
                    public Object getClipboardData(String mimeType) {
                        return writeNote();
                    }
                });

        NativeDragOperation op = new NativeDragOperation(content)
                .setAllowedActions(NativeDragOperation.ACTION_COPY)
                .setLabel("codenameone-note.txt");
        op.addCompletionListener(e -> {
            NativeDragOperation done = (NativeDragOperation) e.getSource();
            setStatus(done.getPerformedAction() == NativeDragOperation.ACTION_NONE
                    ? "The file drag was cancelled"
                    : "The file was dropped");
        });
        card.setNativeDragOperation(op);
        return card;
    }

    /** Writes the promised file and returns its path, or null when it could not be written. */
    private String writeNote() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + "codenameone-note.txt";
        try {
            OutputStream out = fs.openOutputStream(path);
            try {
                out.write("Written by Codename One when you dropped it.\n".getBytes("UTF-8"));
            } finally {
                out.close();
            }
            return path;
        } catch (Exception err) {
            Log.e(err);
            return null;
        }
    }

    /** A zone that accepts anything dropped on it, from this application or from another one. */
    private Component dropZone() {
        final Container zone = new Container(BoxLayout.y()) {
            @Override
            protected void nativeDragEnter(NativeDropEvent ev) {
                getAllStyles().setBgColor(0xddeeff);
                getAllStyles().setBgTransparency(255);
                repaint();
            }

            @Override
            protected void nativeDragExit(NativeDropEvent ev) {
                getAllStyles().setBgTransparency(0);
                repaint();
            }

            @Override
            protected void nativeDrop(NativeDropEvent ev) {
                getAllStyles().setBgTransparency(0);
                repaint();
            }
        };
        zone.getStyle().setBorder(Border.createDashedBorder(2, 0x888888));
        zone.getAllStyles().setPadding(16, 16, 16, 16);
        zone.add(new Label("Drop anything here"));
        zone.setNativeDropTarget(true);
        zone.addNativeDropListener(e -> {
            NativeDropEvent drop = (NativeDropEvent) e;
            zone.removeAll();
            zone.add(new Label(drop.isLocal() ? "Dropped from this app" : "Dropped from elsewhere"));
            String[] files = drop.getFiles();
            if (files != null) {
                for (String file : files) {
                    zone.add(new Label(file));
                }
            } else {
                String text = drop.getText();
                zone.add(new Label(text == null ? "no text" : text));
                for (String mime : drop.getContent().getMimeTypes()) {
                    zone.add(new Label("offered: " + mime));
                }
            }
            zone.getComponentForm().revalidateWithAnimationSafety();
        });
        return zone;
    }

    private void setStatus(String text) {
        status.setText(text);
        status.repaint();
    }

    public void stop() {
        current = getCurrentForm();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = getCurrentForm();
        }
    }

    public void destroy() {
    }
}
