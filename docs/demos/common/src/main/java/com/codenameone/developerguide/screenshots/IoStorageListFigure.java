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

import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// The storage browser listing the files it holds.
class IoStorageListFigure implements GuideFigure {

    @Override
    public String id() {
        return "storage-list";
    }

    @Override
    public Form build() {
        seed();
        Toolbar.setGlobalToolbar(true);
        Form hi = new Form("Storage", new BoxLayout(BoxLayout.Y_AXIS));
        hi.getToolbar().addCommandToRightBar("+", null, e -> {
        });
        // The chapter's own loop asks storage for everything it holds. A figure
        // cannot: the simulator's storage also carries whatever the framework
        // and the app have put there -- cn1surfaces and the app state file both
        // turned up in the first render -- so the picture would differ between
        // this machine and the runner. These are the three the figure wrote.
        for (String file : FILES) {
            createFileEntry(hi, file);
        }
        hi.show();
        return hi;
    }

    /// The listing reads whatever storage already holds, and a figure has to
    /// bring its own -- a run with an empty storage photographs an empty form.
    /// Written every time so the picture does not depend on what an earlier
    /// figure happened to leave behind.
    private static final String[] FILES = {"itinerary.txt", "packing.txt", "notes.txt"};

    private static void seed() {
        write(FILES[0], "Meet at the main lobby at 9am.");
        write(FILES[1], "Boots, maps, a warm coat.");
        write(FILES[2], "The road goes ever on.");
    }

    private static void write(String name, String body) {
        try (OutputStream os = Storage.getInstance().createOutputStream(name)) {
            os.write(body.getBytes("UTF-8"));
        } catch (IOException err) {
            Log.e(err);
        }
    }

    // tag::io-java-093-helper[]
    private void createFileEntry(Form hi, String file) {
       Label fileField = new Label(file);
       Button delete = new Button();
       Button view = new Button();
       FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
       FontImage.setMaterialIcon(view, FontImage.MATERIAL_OPEN_IN_NEW);
       Container content = BorderLayout.center(fileField);
       int size = Storage.getInstance().entrySize(file);
       content.add(BorderLayout.EAST, BoxLayout.encloseX(new Label(size + "bytes"), delete, view));
       delete.addActionListener(e -> {
           Storage.getInstance().deleteStorageFile(file);
           content.setY(hi.getWidth());
           hi.getContentPane().animateUnlayoutAndWait(150, 255);
           hi.removeComponent(content);
           hi.getContentPane().animateLayout(150);
       });
       view.addActionListener(e -> {
           try(InputStream is = Storage.getInstance().createInputStream(file);) {
               String s = Util.readToString(is, "UTF-8");
               Dialog.show(file, s, "OK", null);
           } catch(IOException err) {
               Log.e(err);
           }
       });
       hi.add(content);
    }
    // end::io-java-093-helper[]
}
