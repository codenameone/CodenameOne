/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.components.ToastBar.Status;
import com.codename1.maps.layers.*;
import com.codename1.charts.*;
import com.codename1.ui.validation.*;
import com.codename1.xml.*;
import com.codename1.charts.util.*;
import com.codename1.javascript.*;
import com.codename1.ui.tree.*;
import com.codename1.ui.table.*;
import com.codename1.contacts.*;
import java.util.*;


class TheComponentsOfCodenameOneJava189Snippet {


    Object context;
    Object url;
    Object value;
    Object body;
    Object event;
    String apiKey = "test-key";
    String myHttpsURL = "https://example.com";
    java.util.List<String> validKeysList = new java.util.ArrayList<>();
    Image myImage;
    Graphics graphics;
    Graphics g;
    GraphicsDevice device;
    Form form;
    Form hi;
    Container cnt;
    Container myForm;
    Component component;
    Button button;
    MultiButton myMultiButton;
    Label label;
    BrowserComponent browserComponent;
    Resources theme;
    
    void snippet() throws Exception {
        // tag::the-components-of-codename-one-java-189[]
        Form hi = new Form("ImageViewer", new BorderLayout());
        final EncodedImage placeholder = EncodedImage.createFromImage(
                FontImage.createMaterial(FontImage.MATERIAL_SYNC, s).
                        scaled(300, 300), false);

        class ImageList implements ListModel<Image> {
            private int selection;
            private String[] imageURLs = {
                "http://awoiaf.westeros.org/images/thumb/9/93/AGameOfThrones.jpg/300px-AGameOfThrones.jpg",
                "http://awoiaf.westeros.org/images/thumb/3/39/AClashOfKings.jpg/300px-AClashOfKings.jpg",
                "http://awoiaf.westeros.org/images/thumb/2/24/AStormOfSwords.jpg/300px-AStormOfSwords.jpg",
                "http://awoiaf.westeros.org/images/thumb/a/a3/AFeastForCrows.jpg/300px-AFeastForCrows.jpg",
                "http://awoiaf.westeros.org/images/7/79/ADanceWithDragons.jpg"
            };
            private Image[] images;
            private EventDispatcher listeners = new EventDispatcher();

            public ImageList() {
                this.images = new EncodedImage[imageURLs.length];
            }

            public Image getItemAt(final int index) {
                if(images[index] == null) {
                    images[index] = placeholder;
                    Util.downloadUrlToStorageInBackground(imageURLs[index], "list" + index, (e) -> {
                            try {
                                images[index] = EncodedImage.create(Storage.getInstance().createInputStream("list" + index));
                                listeners.fireDataChangeEvent(index, DataChangedListener.CHANGED);
                            } catch(IOException err) {
                                err.printStackTrace();
                            }
                    });
                }
                return images[index];
            }

            public int getSize() {
                return imageURLs.length;
            }

            public int getSelectedIndex() {
                return selection;
            }

            public void setSelectedIndex(int index) {
                selection = index;
            }

            public void addDataChangedListener(DataChangedListener l) {
                listeners.addListener(l);
            }

            public void removeDataChangedListener(DataChangedListener l) {
                listeners.removeListener(l);
            }

            public void addSelectionListener(SelectionListener l) {
            }

            public void removeSelectionListener(SelectionListener l) {
            }

            public void addItem(Image item) {
            }

            public void removeItem(int index) {
            }
        };

        ImageList imodel = new ImageList();

        ImageViewer iv = new ImageViewer(imodel.getItemAt(0));
        iv.setImageList(imodel);
        hi.add(BorderLayout.CENTER, iv);
        hi.show();
        // end::the-components-of-codename-one-java-189[]
    }

    Style s = new Style();

}
