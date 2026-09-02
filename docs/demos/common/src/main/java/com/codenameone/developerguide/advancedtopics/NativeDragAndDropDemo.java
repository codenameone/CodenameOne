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


package com.codenameone.developerguide.advancedtopics;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.ClipboardDataProvider;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.NativeDragAndDrop;
import com.codename1.ui.NativeDragOperation;
import com.codename1.ui.NativeDropEvent;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.OutputStream;

public class NativeDragAndDropDemo {

    // tag::nativeDragSource[]
    public void showDragSource() {
        Form hi = new Form("Drag Out", BoxLayout.y());
        Label card = new Label("Drag me into another application");

        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "Dragged out of Codename One")
                .setData(ClipboardContent.MIME_HTML, "<b>Dragged</b> out of Codename One");

        card.setNativeDragOperation(new NativeDragOperation(content)
                .setAllowedActions(NativeDragOperation.ACTION_COPY));

        hi.add(card);
        hi.show();
    }
    // end::nativeDragSource[]

    // tag::nativeFileDrag[]
    public void showFileDragSource() {
        Form hi = new Form("Drag A File Out", BoxLayout.y());
        Label card = new Label("Drag me onto the desktop");

        // The file is promised rather than written. The provider runs when a receiver actually
        // asks for the file list, so a drag the user abandons costs nothing.
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "note.txt")
                .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    public Object getClipboardData(String mimeType) {
                        return writeNote();
                    }
                });

        NativeDragOperation op = new NativeDragOperation(content);
        op.addCompletionListener(e ->
                System.out.println("performed action: "
                        + ((NativeDragOperation) e.getSource()).getPerformedAction()));
        card.setNativeDragOperation(op);

        hi.add(card);
        hi.show();
    }

    private String writeNote() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath() + "note.txt";
        try {
            OutputStream out = fs.openOutputStream(path);
            try {
                out.write("Written on drop\n".getBytes("UTF-8"));
            } finally {
                out.close();
            }
            return path;
        } catch (Exception err) {
            return null;
        }
    }
    // end::nativeFileDrag[]

    // tag::nativeDropTarget[]
    public void showDropTarget() {
        Form hi = new Form("Drop Here", new BorderLayout());
        Container zone = new Container(BoxLayout.y());
        zone.add(new Label("Drop files here"));

        zone.setNativeDropTarget(true);
        zone.setAcceptedDropMimeTypes(ClipboardContent.MIME_FILE);
        zone.setAcceptedDropActions(NativeDragOperation.ACTION_COPY);
        zone.addNativeDropListener(e -> {
            NativeDropEvent drop = (NativeDropEvent) e;
            for (String path : drop.getFiles()) {
                zone.add(new Label(path));
            }
            zone.getComponentForm().revalidateWithAnimationSafety();
        });

        hi.add(BorderLayout.CENTER, zone);
        hi.show();
    }
    // end::nativeDropTarget[]

    // tag::nativeDragSupport[]
    public boolean canDragOut() {
        return NativeDragAndDrop.isSupported()
                && NativeDragAndDrop.isDragOutsideApplicationSupported();
    }
    // end::nativeDragSupport[]
}
