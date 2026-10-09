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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.image.ImageObserver;
import com.codename1.desktopcompat.javax.accessibility.Accessible;
import com.codename1.desktopcompat.javax.accessibility.AccessibleContext;
import com.codename1.desktopcompat.rt.ImageLoader;
import com.codename1.desktopcompat.rt.NativeImage;
import java.net.URL;

/// An icon that paints an image, drawn at one logical pixel per image
/// pixel.
///
/// A file name is read as a file when there is one, and is otherwise
/// looked up among the application's resources by its last path element:
/// the pictures a desktop application keeps beside it are resources once
/// it is built for a device. A URL is read before the constructor returns.
public class ImageIcon implements Icon, Accessible {

    private AccessibleContext cn1Accessible;

    private Image image;
    private String description;
    private int width = -1;
    private int height = -1;
    private ImageObserver imageObserver;

    public ImageIcon() {
    }

    public ImageIcon(Image image) {
        setImage(image);
    }

    public ImageIcon(Image image, String description) {
        setImage(image);
        this.description = description;
    }

    public ImageIcon(byte[] imageData) {
        cn1Loaded(imageData == null ? null : ImageLoader.decode(imageData, 0, imageData.length));
    }

    public ImageIcon(byte[] imageData, String description) {
        this(imageData);
        this.description = description;
    }

    public ImageIcon(String filename) {
        this(filename, filename);
    }

    /// The image a path names: the file when there is one, else the
    /// application's resource of the path's last name.
    public ImageIcon(String filename, String description) {
        this.description = description;
        cn1Loaded(ImageLoader.fromPath(filename));
    }

    /// The image a URL names, read before the constructor returns.
    public ImageIcon(URL location) {
        this(location, location == null ? null : location.toExternalForm());
    }

    public ImageIcon(URL location, String description) {
        this.description = description;
        cn1Loaded(ImageLoader.fromUrl(location));
    }

    /// An icon whose image could not be read has no image, and a width and
    /// height of -1.
    private void cn1Loaded(com.codename1.ui.Image decoded) {
        if (decoded != null) {
            setImage(new NativeImage(decoded));
        }
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
        width = image == null ? -1 : image.getWidth(imageObserver);
        height = image == null ? -1 : image.getHeight(imageObserver);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (image != null) {
            g.drawImage(image, x, y, imageObserver != null ? imageObserver : c);
        }
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    public void setImageObserver(ImageObserver observer) {
        imageObserver = observer;
    }

    public ImageObserver getImageObserver() {
        return imageObserver;
    }

    @Override
    public String toString() {
        return description != null ? description : super.toString();
    }

    /// The accessible context of the icon. Its description starts as the
    /// description of the icon.
    @Override
    public AccessibleContext getAccessibleContext() {
        if (cn1Accessible == null) {
            cn1Accessible = new Cn1AccessibleIcon();
            cn1Accessible.setAccessibleDescription(description);
        }
        return cn1Accessible;
    }

    private static final class Cn1AccessibleIcon extends AccessibleContext {

        Cn1AccessibleIcon() {
        }
    }
}
