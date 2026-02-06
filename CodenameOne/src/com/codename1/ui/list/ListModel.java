/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui.list;

import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.SelectionListener;

/// Represents the data structure of the list, thus allowing a list to
/// represent any potential data source by referencing different implementations of this
/// interface, **notice** that
/// [we strongly discourage usage of lists](https://www.codenameone.com/blog/avoiding-lists.html).. E.g. a list model can be implemented in such a way that it retrieves data
/// directly from storage (although caching would be recommended).
///
/// It is the responsibility of the list to notify observers (specifically the view
/// `com.codename1.ui.List` of any changes to its state (items removed/added/changed etc.)
/// thus the data would get updated on the view.
///
/// ```java
/// class GRMMModel implements ListModel> {
/// @Override
///     public Map getItemAt(int index) {
///         int idx = index % 7;
///         switch(idx) {
///             case 0:
///                 return createListEntry("A Game of Thrones " + index, "1996");
///             case 1:
///                 return createListEntry("A Clash Of Kings " + index, "1998");
///             case 2:
///                 return createListEntry("A Storm Of Swords " + index, "2000");
///             case 3:
///                 return createListEntry("A Feast For Crows " + index, "2005");
///             case 4:
///                 return createListEntry("A Dance With Dragons " + index, "2011");
///             case 5:
///                 return createListEntry("The Winds of Winter " + index, "2016 (please, please, please)");
///             default:
///                 return createListEntry("A Dream of Spring " + index, "Ugh");
///         }
///     }
/// @Override
///     public int getSize() {
///         return 1000000;
///     }
/// @Override
///     public int getSelectedIndex() {
///         return 0;
///     }
/// @Override
///     public void setSelectedIndex(int index) {
///     }
/// @Override
///     public void addDataChangedListener(DataChangedListener l) {
///     }
/// @Override
///     public void removeDataChangedListener(DataChangedListener l) {
///     }
/// @Override
///     public void addSelectionListener(SelectionListener l) {
///     }
/// @Override
///     public void removeSelectionListener(SelectionListener l) {
///     }
/// @Override
///     public void addItem(Map item) {
///     }
/// @Override
///     public void removeItem(int index) {
///     }
/// }
/// ```
///
/// A `ListModel` can be used in conjunction with an `com.codename1.components.ImageViewer`
/// to fetch images dynamically into the view:
///
/// ```java
/// Form hi = new Form("ImageViewer", new BorderLayout());
/// final EncodedImage placeholder = EncodedImage.createFromImage(
///         FontImage.createMaterial(FontImage.MATERIAL_SYNC, s).
///                 scaled(300, 300), false);
///
/// class ImageList implements ListModel {
///     private int selection;
///     private String[] imageURLs = {
///         "http://awoiaf.westeros.org/images/thumb/9/93/AGameOfThrones.jpg/300px-AGameOfThrones.jpg",
///         "http://awoiaf.westeros.org/images/thumb/3/39/AClashOfKings.jpg/300px-AClashOfKings.jpg",
///         "http://awoiaf.westeros.org/images/thumb/2/24/AStormOfSwords.jpg/300px-AStormOfSwords.jpg",
///         "http://awoiaf.westeros.org/images/thumb/a/a3/AFeastForCrows.jpg/300px-AFeastForCrows.jpg",
///         "http://awoiaf.westeros.org/images/7/79/ADanceWithDragons.jpg"
///     };
///     private Image[] images;
///     private EventDispatcher listeners = new EventDispatcher();
///
///     public ImageList() {
///         this.images = new EncodedImage[imageURLs.length];
///     }
///
///     public Image getItemAt(final int index) {
///         if(images[index] == null) {
///             images[index] = placeholder;
///             Util.downloadUrlToStorageInBackground(imageURLs[index], "list" + index, (e) -> {
///                     try {
///                         images[index] = EncodedImage.create(Storage.getInstance().createInputStream("list" + index));
///                         listeners.fireDataChangeEvent(index, DataChangedListener.CHANGED);
///                     } catch(IOException err) {
///                         err.printStackTrace();
///                     }
///             });
///         }
///         return images[index];
///     }
///
///     public int getSize() {
///         return imageURLs.length;
///     }
///
///     public int getSelectedIndex() {
///         return selection;
///     }
///
///     public void setSelectedIndex(int index) {
///         selection = index;
///     }
///
///     public void addDataChangedListener(DataChangedListener l) {
///         listeners.addListener(l);
///     }
///
///     public void removeDataChangedListener(DataChangedListener l) {
///         listeners.removeListener(l);
///     }
///
///     public void addSelectionListener(SelectionListener l) {
///     }
///
///     public void removeSelectionListener(SelectionListener l) {
///     }
///
///     public void addItem(Image item) {
///     }
///
///     public void removeItem(int index) {
///     }
/// };
///
/// ImageList imodel = new ImageList();
///
/// ImageViewer iv = new ImageViewer(imodel.getItemAt(0));
/// iv.setImageList(imodel);
/// hi.add(BorderLayout.CENTER, iv);
/// ```
/// @author Chen Fishbein
public interface ListModel<T> {

    /// Returns the item at the given offset
    ///
    /// #### Parameters
    ///
    /// - `index`: an index into this list
    ///
    /// #### Returns
    ///
    /// the item at the specified index
    T getItemAt(int index);

    /// Returns the number of items in the list
    ///
    /// #### Returns
    ///
    /// the number of items in the list
    int getSize();

    /// Returns the selected list offset
    ///
    /// #### Returns
    ///
    /// the selected list index
    int getSelectedIndex();

    /// Sets the selected list offset can be set to -1 to clear selection
    ///
    /// #### Parameters
    ///
    /// - `index`: an index into this list
    void setSelectedIndex(int index);

    /// Invoked to indicate interest in future change events
    ///
    /// #### Parameters
    ///
    /// - `l`: a data changed listener
    void addDataChangedListener(DataChangedListener l);

    /// Invoked to indicate no further interest in future change events
    ///
    /// #### Parameters
    ///
    /// - `l`: a data changed listener
    void removeDataChangedListener(DataChangedListener l);

    /// Invoked to indicate interest in future selection events
    ///
    /// #### Parameters
    ///
    /// - `l`: a selection listener
    void addSelectionListener(SelectionListener l);

    /// Invoked to indicate no further interest in future selection events
    ///
    /// #### Parameters
    ///
    /// - `l`: a selection listener
    void removeSelectionListener(SelectionListener l);

    /// Adds the specified item to the end of this list.
    /// An optional operation for mutable lists, it can throw an unsupported operation
    /// exception if a list model is not mutable.
    ///
    /// #### Parameters
    ///
    /// - `item`: the item to be added
    void addItem(T item);

    /// Removes the item at the specified position in this list.
    ///
    /// #### Parameters
    ///
    /// - `index`: the index of the item to removed
    void removeItem(int index);

}
