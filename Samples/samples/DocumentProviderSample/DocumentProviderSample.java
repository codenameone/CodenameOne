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

import com.codename1.documents.DocumentNode;
import com.codename1.documents.DocumentProvider;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Demonstrates the document provider API ({@code com.codename1.documents}): publishing an app's
 * content so it appears as a location in the system file browser. The sample keeps a tiny library
 * of text "notes", writes each one into the shared directory and publishes a two-level tree over
 * them. Adding or deleting a note republishes, and the browser picks the change up.
 *
 * <p>Where to see it: on iOS open the Files app and look under "Browse" alongside iCloud Drive; on
 * macOS look in Finder's sidebar; on Android open any app's storage picker and choose "Browse".
 * The location is served by a separate process that runs while this app is dead, which is why the
 * app publishes data rather than answering questions -- see the Document Provider chapter of the
 * developer guide.</p>
 *
 * <p>The build wires the native plumbing (the iOS/macOS file provider extension and its App Group,
 * the Android documents provider) purely because this class references
 * {@code com.codename1.documents}. In the simulator {@code isSupported()} answers false -- there is
 * no desktop browser to publish into -- but everything is still written under {@code cn1documents}
 * in the app's home directory, so the published index can be read back and checked.</p>
 */
public class DocumentProviderSample {
    private Form current;
    private Resources theme;

    /** Names of the notes this sample has published, in publish order. */
    private final java.util.List<String> notes = new java.util.ArrayList<String>();

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("Document Provider", BoxLayout.y());

        hi.add(new Label(DocumentProvider.isSupported()
                ? "This device can show your documents in its file browser."
                : "This platform has no file browser to publish into; publishing still runs "
                        + "and is a safe no-op."));

        Button addNote = new Button("Add a note");
        addNote.addActionListener(e -> {
            notes.add("Note " + (notes.size() + 1));
            publish();
            Dialog.show("Published", "The location now lists " + notes.size() + " note(s).", "OK", null);
        });
        hi.add(addNote);

        Button clear = new Button("Withdraw everything");
        clear.addActionListener(e -> {
            notes.clear();
            // Withdraws the location and empties the shared directory. Worth doing on logout: the
            // published content outlives the process, so anything left behind stays browsable.
            DocumentProvider.clear();
            Dialog.show("Cleared", "The location is gone from the file browser.", "OK", null);
        });
        hi.add(clear);

        hi.add(new Label("Shared directory:"));
        String shared = DocumentProvider.getSharedDirectory();
        hi.add(new Label(shared == null ? "(unavailable on this platform)" : shared));

        hi.show();
        current = hi;
    }

    /**
     * Writes every note into the shared directory and republishes the whole tree.
     *
     * <p>The whole tree every time, rather than a delta: publishing replaces what came before, and
     * an app that tried to publish only what changed would have to model the browser's idea of the
     * tree as well as its own.</p>
     */
    private void publish() {
        String shared = DocumentProvider.getSharedDirectory();
        DocumentNode root = DocumentNode.folder("root", "Sample Notes");
        DocumentNode folder = DocumentNode.folder("all", "All notes");
        long now = new Date().getTime();
        for (int i = 0; i < notes.size(); i++) {
            String title = notes.get(i);
            String fileName = "note-" + (i + 1) + ".txt";
            byte[] body = (title + "\n\nWritten by the Codename One document provider sample.\n")
                    .getBytes();
            if (shared != null) {
                write(shared + "/" + fileName, body);
            }
            // The id is derived from the note's own identity, never from list position: the
            // platform remembers ids -- a favourite, a recent document -- so renumbering them on
            // every publish would point the browser at the wrong note later.
            folder.add(DocumentNode.file("note-" + title.hashCode(), title + ".txt")
                    .setContentType("text/plain")
                    .setPath(shared == null ? null : fileName)
                    .setSize(body.length)
                    .setLastModified(now));
        }
        root.add(folder);
        DocumentProvider.publish(root);
    }

    private void write(String path, byte[] data) {
        try {
            OutputStream out = FileSystemStorage.getInstance().openOutputStream(path);
            try {
                out.write(data);
            } finally {
                out.close();
            }
        } catch (IOException err) {
            Log.e(err);
        }
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }

    public void destroy() {
    }
}
