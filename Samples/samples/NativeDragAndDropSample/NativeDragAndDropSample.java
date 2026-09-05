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

import com.codename1.components.ToastBar;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.Button;
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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.io.OutputStream;

/**
 * Drags content out of the application and accepts drags coming the other way, using the
 * operating system's own drag and drop.
 *
 * <p>Everything here needs a second application to be worth anything: drag the cards onto a
 * text editor, a browser or the desktop, and drag a file or a selection from those back onto
 * the drop zones. The zones report what actually arrived -- the types, whether the drag
 * started inside this application, and the action the system settled on.</p>
 */
public class NativeDragAndDropSample {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        updateNetworkThreadCount(2);
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Native Drag & Drop", BoxLayout.y());
        hi.add(supportBanner());
        hi.add(dragOutCard());
        hi.add(fileDragCard());
        hi.add(anythingZone());
        hi.add(filesOnlyZone());
        hi.show();
    }

    /**
     * What this device can actually do. A platform without native drag and drop leaves the
     * lightweight in-form dragging untouched, so the rest of the sample is inert rather than
     * broken.
     */
    private Component supportBanner() {
        String text;
        if (!NativeDragAndDrop.isSupported()) {
            text = "This platform has no native drag and drop; the cards below do nothing.";
        } else if (NativeDragAndDrop.isDragOutsideApplicationSupported()) {
            text = "Drags can leave this application and arrive from other ones.";
        } else {
            text = "Drops arrive from other applications; drags out stay inside this one.";
        }
        Label banner = new Label(text);
        banner.setUIID("SidemenuTitle");
        return banner;
    }

    /**
     * One payload, three readings of it. A rich text editor takes the HTML, a plain text field
     * takes the text, and anything that wants a URL takes the link -- from a single drag.
     */
    private Component dragOutCard() {
        Label card = card("Drag this text into another application");

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "Dragged out of Codename One")
                .setData(ClipboardContent.MIME_HTML,
                        "<b>Dragged</b> out of <i>Codename One</i>")
                .setData(ClipboardContent.MIME_URI_LIST, "https://www.codenameone.com/");

        NativeDragOperation op = new NativeDragOperation(content)
                .setAllowedActions(NativeDragOperation.ACTION_COPY
                        | NativeDragOperation.ACTION_MOVE)
                .setLabel("Codename One text");
        op.addCompletionListener(e -> reportOutcome("text", e));
        card.setNativeDragOperation(op);
        return card;
    }

    /**
     * A file that does not exist until somebody asks for it. The provider runs when a receiver
     * reads the file list -- when the drop lands on the desktop, say -- so a drag the user
     * abandons costs nothing. Android and iOS resolve it as the drag begins instead, because
     * both need a complete payload to start a session at all.
     */
    private Component fileDragCard() {
        Label card = card("Drag this onto the desktop to get a file");

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "codenameone-note.txt")
                .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    @Override
                    public Object getClipboardData(String mimeType) {
                        String path = writeNote();
                        return path == null ? null : new String[]{path};
                    }
                });

        NativeDragOperation op = new NativeDragOperation(content)
                .setAllowedActions(NativeDragOperation.ACTION_COPY);
        op.addCompletionListener(e -> reportOutcome("file", e));
        card.setNativeDragOperation(op);
        return card;
    }

    /** Takes whatever arrives, and says what that was. */
    private Component anythingZone() {
        Container zone = zone("Drop anything here");
        zone.setNativeDropTarget(true);
        zone.addNativeDropListener(e -> describeDrop(zone, (NativeDropEvent) e));
        highlightWhileHovering(zone);
        return zone;
    }

    /**
     * Filtered twice over: only a drag carrying files is offered this zone at all, and only a
     * copy is allowed -- so a file manager offering to *move* the file is answered with a copy
     * rather than being refused.
     */
    private Component filesOnlyZone() {
        Container zone = zone("Drop files here (copy only)");
        zone.setNativeDropTarget(true);
        zone.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);
        zone.setAcceptedDropActions(NativeDragOperation.ACTION_COPY);
        zone.addNativeDropListener(e -> describeDrop(zone, (NativeDropEvent) e));
        highlightWhileHovering(zone);
        return zone;
    }

    /**
     * The hover callbacks, which are what a drop target uses to show it will take the drag.
     * Enter and exit bracket the hover; over arrives while the pointer moves within the zone.
     */
    private void highlightWhileHovering(Container zone) {
        zone.addNativeDragOverListener(e -> {
            if (e.getEventType() == ActionEvent.Type.NativeDragExit) {
                zone.getAllStyles().setBorder(Border.createLineBorder(2, 0x808080));
            } else {
                zone.getAllStyles().setBorder(Border.createLineBorder(2, 0x0080ff));
            }
            zone.repaint();
        });
        zone.addNativeDropListener(e ->
                zone.getAllStyles().setBorder(Border.createLineBorder(2, 0x808080)));
    }

    private void describeDrop(Container zone, NativeDropEvent drop) {
        StringBuilder sb = new StringBuilder();
        sb.append(drop.isLocal() ? "from this application" : "from another application");
        sb.append(", action ").append(actionName(drop.getAcceptedAction()));
        zone.add(new Label(sb.toString()));

        String text = drop.getText();
        if (text != null && text.length() > 0) {
            zone.add(new Label("text: " + text));
        }
        String[] files = drop.getFiles();
        if (files != null) {
            for (int iter = 0; iter < files.length; iter++) {
                zone.add(new Label("file: " + files[iter]));
            }
        }
        String[] types = drop.getContent().getMimeTypes();
        for (int iter = 0; iter < types.length; iter++) {
            zone.add(new Label("offered: " + types[iter]));
        }
        zone.getComponentForm().revalidateWithAnimationSafety();
    }

    /**
     * What the operating system did with the drag, which the source only learns once the
     * gesture is over -- and which a source that allows ACTION_MOVE has to wait for before it
     * deletes anything.
     */
    private void reportOutcome(String what, ActionEvent e) {
        NativeDragOperation op = (NativeDragOperation) e.getSource();
        ToastBar.showInfoMessage("The " + what + " drag ended as "
                + actionName(op.getPerformedAction()));
    }

    private String actionName(int action) {
        if (action == NativeDragOperation.ACTION_COPY) {
            return "copy";
        }
        if (action == NativeDragOperation.ACTION_MOVE) {
            return "move";
        }
        if (action == NativeDragOperation.ACTION_LINK) {
            return "link";
        }
        return "nothing";
    }

    private String writeNote() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + "codenameone-note.txt";
        try {
            OutputStream out = fs.openOutputStream(path);
            try {
                out.write("Written when the drop asked for it\n".getBytes("UTF-8"));
            } finally {
                out.close();
            }
            return path;
        } catch (Exception err) {
            Log.e(err);
            return null;
        }
    }

    private Label card(String text) {
        Label card = new Label(text);
        card.getAllStyles().setBorder(Border.createLineBorder(2, 0x404040));
        card.getAllStyles().setPadding(Component.TOP, 4);
        card.getAllStyles().setPadding(Component.BOTTOM, 4);
        return card;
    }

    private Container zone(String title) {
        Container zone = new Container(BoxLayout.y());
        zone.add(new Label(title));
        zone.getAllStyles().setBorder(Border.createLineBorder(2, 0x808080));
        return zone;
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
