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

package com.codename1.io.services;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Label;
import com.codename1.ui.List;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.NetworkEvent;
import com.codename1.io.NetworkManager;
import com.codename1.io.Storage;
import com.codename1.components.FileEncodedImage;
import com.codename1.components.FileEncodedImageAsync;
import com.codename1.components.StorageImage;
import com.codename1.components.StorageImageAsync;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.list.ContainerList;
import com.codename1.ui.list.DefaultListModel;
import com.codename1.ui.list.ListModel;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Simplifies the process of implementing an image link for labels and lists by
 * binding a request to a component. On the completion of the request a Codename One
 * encoded image is created and installs itself into the given component.
 * For the case of a Label this is seamless, in case of a List renderer the model
 * or the renderer must register itself as a listener and update the data when
 * the response arrives.
 *
 * @author Shai Almog
 */
public class ImageDownloadService extends ConnectionRequest {

    /**
     * Fast scaling uses runtime draw scaling rather than the Image scaled method. This works 
     * better on smartphones but doesn't work well on feature phones
     * @return the fastScale
     */
    public static boolean isFastScale() {
        return fastScale;
    }

    /**
     * Fast scaling uses runtime draw scaling rather than the Image scaled method. This works 
     * better on smartphones but doesn't work well on feature phones
     * @param aFastScale the fastScale to set
     */
    public static void setFastScale(boolean aFastScale) {
        fastScale = aFastScale;
    }
    
    private static boolean alwaysRevalidate;

    /**
     * By default lists don't revalidate on every change to avoid "jumpiness" when scrolling
     * @return the alwaysRevalidate
     */
    public static boolean isAlwaysRevalidate() {
        return alwaysRevalidate;
    }

    /**
     * By default lists don't revalidate on every change to avoid "jumpiness" when scrolling
     * @param aAlwaysRevalidate the alwaysRevalidate to set
     */
    public static void setAlwaysRevalidate(boolean aAlwaysRevalidate) {
        alwaysRevalidate = aAlwaysRevalidate;
    }
        
    private Label parentLabel;
    private EncodedImage result;
    private Component targetList;
    private int targetOffset;
    private String targetKey;
    private boolean cacheImages;
    private String destinationFile;
    private Dimension toScale;
    private String cacheId;
    private boolean keep;
    
    private static boolean fastScale = true;
    private Image placeholder;
    
    /**
     * Accepts the url to bind to the list renderer, on completion the action listener
     * will be invoked with the image so a list can be updated with the data
     *
     * @param url the image URL
     * @param l an action listener callback
     */
    public ImageDownloadService(String url, ActionListener l) {
        setUrl(url);
        addResponseListener(l);
        setPost(false);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives. This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     */
    public ImageDownloadService(String url, Component targetList, int targetOffset, String targetKey) {
        this.targetList = targetList;
        this.targetKey = targetKey;
        this.targetOffset = targetOffset;
        setUrl(url);
        setPost(false);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives. This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     */
    public ImageDownloadService(String url, List targetList, int targetOffset, String targetKey) {
        this.targetList = targetList;
        this.targetKey = targetKey;
        this.targetOffset = targetOffset;
        setUrl(url);
        setPost(false);
    }

    /**
     * Accepts the url to bind to the label, on completion the label will be updated
     * and revalidated with the new image.
     *
     * @param url the image URL
     * @param parentLabel the label to update
     */
    public ImageDownloadService(String url, Label parentLabel) {
        setUrl(url);
        this.parentLabel = parentLabel;
        setPost(false);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param destFile local file to store the data into the given path
     */
    public static void createImageToFileSystem(String url, List targetList, int targetOffset, 
            String targetKey, String destFile, Dimension toScale) {
        createImageToFileSystem(url, targetList, targetOffset, targetKey, destFile, toScale, PRIORITY_NORMAL);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param destFile local file to store the data into the given path
     */
    public static void createImageToFileSystem(String url, Component targetList, int targetOffset, 
            String targetKey, String destFile, Dimension toScale, byte priority) {
        createImageToFileSystem(url, targetList, targetOffset, targetKey, destFile, toScale, priority, null);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param destFile local file to store the data into the given path
     */
    public static void createImageToFileSystem(String url, Component targetList, int targetOffset, 
            String targetKey, String destFile, Image placeholder, byte priority) {
        createImageToFileSystem(url, targetList, targetOffset, targetKey, destFile, null, priority, placeholder);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param destFile local file to store the data into the given path
     */
    private static void createImageToFileSystem(final String url, final Component targetList, final int targetOffset,
            final String targetKey, final String destFile, final Dimension toScale, final byte priority, final Image placeholderImage) {
        if (Display.getInstance().isEdt()) {
            Display.getInstance().scheduleBackgroundTask(new Runnable() {

                public void run() {
                    createImageToFileSystem(url, targetList, targetOffset,
                            targetKey, destFile, toScale, priority, placeholderImage);
                }
            });
            return;
        }


        Image im = cacheImage(null, false, destFile, toScale, placeholderImage);
        if (im != null) {
            Hashtable h;
            ListModel model;
            if(targetList instanceof List) {
                model = ((List)targetList).getModel();
            } else {
                model = ((ContainerList)targetList).getModel();
            }
            h = (Hashtable)model.getItemAt(targetOffset);
            if(!fastScale && toScale != null){
                im = im.scaled(toScale.getWidth(), toScale.getHeight());
            }
            h.put(targetKey, im);
            if(model instanceof DefaultListModel) {
                 ((DefaultListModel)model).setItem(targetOffset, h);
            }
            targetList.repaint();
            
            return;
        }
        //image not found on cache go and download from the url
        ImageDownloadService i = new ImageDownloadService(url, targetList, targetOffset, targetKey);
        i.cacheImages = true;
        i.destinationFile = destFile;
        i.toScale = toScale;
        i.placeholder = placeholderImage;
        i.setPriority(priority);
        i.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(i);
    }
    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param scale the scale of the image to put in the List or null
     */
    public static void createImageToStorage(String url, List targetList, int targetOffset, 
            String targetKey, String cacheId, Dimension scale) {
        createImageToStorage(url, targetList, targetOffset, targetKey, cacheId, scale, PRIORITY_NORMAL);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param scale the scale of the image to put in the List or null
     */
    public static void createImageToStorage(String url, Component targetList, int targetOffset, 
            String targetKey, String cacheId, Dimension scale, byte priority) {
        createImageToStorage(url, targetList, targetOffset, targetKey, cacheId, false, scale, priority, null);
    }

    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param placeholderImage the image placeholder
     */
    public static void createImageToStorage(String url, Component targetList, int targetOffset, 
            String targetKey, String cacheId, Image placeholderImage, byte priority) {
        createImageToStorage(url, targetList, targetOffset, targetKey, cacheId, false, null, priority, placeholderImage);
    }
    
    /**
     * Constructs an image request that will automatically populate the given list
     * when the response arrives, it will cache the file locally as a file
     * in the file storage.
     * This assumes the GenericListCellRenderer style of
     * list which relies on a hashtable based model approach.
     *
     * @param url the image URL
     * @param targetList the list that should be updated when the data arrives
     * @param targetOffset the offset within the list to insert the image
     * @param targetKey the key for the hashtable in the target offset
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param keep if set to true keeps the file in RAM once loaded
     * @param scale the scale of the image to put in the List or null
     */
    private static void createImageToStorage(final String url, final Component targetList, final int targetOffset,
            final String targetKey, final String cacheId, final boolean keep, final Dimension scale, final byte priority, final Image placeholderImage) {
        if (Display.getInstance().isEdt()) {
            Display.getInstance().scheduleBackgroundTask(new Runnable() {

                public void run() {
                    createImageToStorage(url, targetList, targetOffset,
                            targetKey, cacheId, keep, scale, priority, placeholderImage);
                }
            });
            return;
        }
        Image im = cacheImage(cacheId, keep, null, scale, placeholderImage);
        if (im != null) {
            Hashtable h;
            ListModel model;
            if (targetList instanceof List) {
                model = ((List) targetList).getModel();
            } else {
                model = ((ContainerList)targetList).getModel();
            }
            h = (Hashtable)model.getItemAt(targetOffset);
            if(!fastScale && scale != null){
                im = im.scaled(scale.getWidth(), scale.getHeight());
            }
            h.put(targetKey, im);
            if(model instanceof DefaultListModel) {
                 ((DefaultListModel)model).setItem(targetOffset, h);
            }
            targetList.repaint();            
            
            return;
        }
        //image not found on cache go and download from the url
        ImageDownloadService i = new ImageDownloadService(url, targetList, targetOffset, targetKey);
        i.cacheImages = true;
        i.cacheId = cacheId;
        i.keep = keep;
        i.toScale = scale;
        i.placeholder = placeholderImage;
        i.setPriority(priority);
        i.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(i);
    }

    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally to the Storage
     *
     * @param url the image URL
     * @param l the Label that should be updated when the data arrives
     * to just use storage and the url as the key
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param toScale the scale dimension or null
     */
    public static void createImageToStorage(String url, Label l, String cacheId, Dimension toScale) {
        createImageToStorage(url, l, cacheId, toScale, PRIORITY_NORMAL);
    }

    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally to the Storage
     *
     * @param url the image URL
     * @param l the Label that should be updated when the data arrives
     * to just use storage and the url as the key
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param toScale the scale dimension or null
     * @param priority the priority for the task
     */
    public static void createImageToStorage(String url, Label l, String cacheId, Dimension toScale,
            byte priority) {
        createImageToStorage(url, l, cacheId, false, toScale, priority, null);
    }

    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally to the Storage
     *
     * @param url the image URL
     * @param l the Label that should be updated when the data arrives
     * to just use storage and the url as the key
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param placeholder the image that will appear as a placeholder
     * @param priority the priority for the task
     */
    public static void createImageToStorage(String url, Label l, String cacheId, Image placeholder,
            byte priority) {
        createImageToStorage(url, l, cacheId, false, null, priority, placeholder);
    }
    
    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally to the Storage
     *
     * @param url the image URL
     * @param l the Label that should be updated when the data arrives
     * to just use storage and the url as the key
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param toScale the scale dimension or null
     * @param priority the priority for the task
     */
    private static void createImageToStorage(final String url, final Label l, final String cacheId, final boolean keep, final Dimension toScale,
            final byte priority, final Image placeholder) {
        if (Display.getInstance().isEdt()) {
            Display.getInstance().scheduleBackgroundTask(new Runnable() {

                public void run() {
                    createImageToStorage(url, l, cacheId, keep, toScale,
                            priority, placeholder);
                }
            });
            return;
        }
        
        Image im = cacheImage(cacheId, keep, null, toScale, placeholder);
        if (im != null) {
            if (!fastScale && toScale != null) {
                im = im.scaled(toScale.getWidth(), toScale.getHeight());
            }
            l.setIcon(im);
            return;
        }
        //image not found on cache go and download from the url
        ImageDownloadService i = new ImageDownloadService(url, l);
        i.setDuplicateSupported(true);
        i.cacheImages = true;
        i.toScale = toScale;
        i.cacheId = cacheId;
        i.placeholder = placeholder;
        i.setPriority(priority);
        i.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(i);
    }


    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally.
     *
     * @param url the image URL
     * @param callback the callback that should be updated when the data arrives
     * @param destFile local file to store the data into the given path
     */
    public static void createImageToFileSystem(String url, ActionListener callback, String destFile) {

        Image im = cacheImage(null, false, destFile, null, null);
        if (im != null) {
            callback.actionPerformed(new NetworkEvent(null, im));
            return;
        }
        //image not found on cache go and download from the url
        ImageDownloadService i = new ImageDownloadService(url, callback);
        i.cacheImages = true;
        i.destinationFile = destFile;
        i.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(i);
    }

    public static void createImageToStorage(String url, ActionListener callback, String cacheId) {
        createImageToStorage(url, callback, cacheId, false);
    }
    /**
     * Constructs an image request that will automatically populate the given Label
     * when the response arrives, it will cache the file locally.
     *
     * @param url the image URL
     * @param callback the callback that should be updated when the data arrives
     * @param cacheId a unique identifier to be used to store the image into storage
     * @param keep if set to true keeps the file in RAM once loaded
     */
    public static void createImageToStorage(String url, ActionListener callback, String cacheId, boolean keep) {

        Image im = cacheImage(cacheId, keep, null, null, null);
        if (im != null) {
            callback.actionPerformed(new NetworkEvent(null, im));
            return;
        }
        //image not found on cache go and download from the url
        ImageDownloadService i = new ImageDownloadService(url, callback);
        i.cacheImages = true;
        i.cacheId = cacheId;
        i.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(i);
    }

    private static Image cacheImage(String cacheKey, boolean keep, String destFile, Dimension scale, Image placeholderImage) {
        
        if (destFile != null) {
            if (FileSystemStorage.getInstance().exists(destFile)) {
                Image f;
                if(placeholderImage != null) {
                    f = FileEncodedImageAsync.create(destFile, placeholderImage);
                } else {
                    if(fastScale && scale != null) {
                        f = FileEncodedImage.create(destFile, scale.getWidth(), scale.getHeight());
                    } else {
                        f = FileEncodedImage.create(destFile, -1, -1);
                    }
                }
                return f;
            }
        } else if(cacheKey != null){
            if (Storage.getInstance().exists(cacheKey)) {
                Image s;
                if(placeholderImage != null) {
                    s = StorageImageAsync.create(cacheKey, placeholderImage);
                } else {
                    if(fastScale && scale != null) {
                        s = StorageImage.create(cacheKey, scale.getWidth(), scale.getHeight(), keep);
                    } else {
                        s = StorageImage.create(cacheKey, -1, -1, keep);
                    }
                    
                    // due to the way the storage image works the data might be corrupted!
                    if(((StorageImage)s).getImageData() == null) {
                        return null;
                    }
                }
                return s;
            }
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    protected void readResponse(InputStream input) throws IOException  {
        int imageScaleWidth = -1, imageScaleHeight = -1;
        if(fastScale) {
            if(toScale != null) {
                imageScaleWidth = toScale.getWidth();
                imageScaleHeight = toScale.getHeight();
            } else {
                if(placeholder != null) {
                    imageScaleWidth = placeholder.getWidth();
                    imageScaleHeight = placeholder.getHeight();
                }
            }
        }
        if(cacheImages) {
            if(destinationFile != null) {
                result = FileEncodedImage.create(destinationFile, input, imageScaleWidth, imageScaleHeight);
            } else {
                EncodedImage e = EncodedImage.create(input);
                result = StorageImage.create(cacheId, e.getImageData(), imageScaleWidth, imageScaleHeight, keep);
                //if the storage has failed create the image from the stream
                if(result == null){
                    result = e;
                }
            }
        } else {
            result = EncodedImage.create(input);
        }
        
        
        // trigger an exception in case of an invalid image
        result.getWidth();
        Image image = result;

        if (toScale != null && toScale.getWidth() != image.getWidth() && toScale.getHeight() != image.getHeight()) {
            image = image.scaled(toScale.getWidth(), toScale.getHeight());
        }

        if(parentLabel != null) {
            Dimension pref = parentLabel.getPreferredSize();
            if(parentLabel.getComponentForm() != null) {
                parentLabel.setIcon(image);
                Dimension newPref = parentLabel.getPreferredSize();

                // if the preferred size changed we need to reflow the UI
                // this might not be necessary if the label already had an identically
                // sized image in place or has a hardcoded preferred size.
                if(pref.getWidth() != newPref.getWidth() || pref.getHeight() != newPref.getHeight()) {
                    parentLabel.getComponentForm().revalidate();
                }
            } else {
                parentLabel.setIcon(image);
            }
            parentLabel.repaint();
            return;
        } else {
            if(targetList != null) {
                Hashtable h;
                ListModel model;
                if(targetList instanceof List) {
                    model = ((List)targetList).getModel();
                } else {
                    model = ((ContainerList)targetList).getModel();
                }
                h = (Hashtable)model.getItemAt(targetOffset);
                h.put(targetKey, image);
                
                // revalidate only once to avoid multiple revalidate refreshes during scroll
                if(targetList.getParent() != null) {
                    if(alwaysRevalidate) {
                        targetList.getParent().revalidate();
                    } else {
                        if(targetList.getClientProperty("$imgDSReval") == null) {
                            targetList.putClientProperty("$imgDSReval", Boolean.TRUE);
                            targetList.getParent().revalidate();
                        } else {
                            if(model instanceof DefaultListModel) {
                                 ((DefaultListModel)model).setItem(targetOffset, h);
                            }
                            targetList.repaint();
                        }
                    }
                }
            }
        }

        // if this is a list cell renderer component
        fireResponseListener(new NetworkEvent(this, result));
    }

    /**
     * Returns the image returned from the server, this method is useful for renderers
     *
     * @return the result
     */
    public EncodedImage getResult() {
        return result;
    }
}
