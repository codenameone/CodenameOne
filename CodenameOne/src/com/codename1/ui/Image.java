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
package com.codename1.ui;

import com.codename1.impl.CodenameOneImplementation;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.ActionSource;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.ImageIO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

/// Abstracts the underlying platform images allowing us to treat them as a uniform
/// object.
///
/// @author Chen Fishbein
public class Image implements ActionSource {
    int transform;
    private EventDispatcher listeners;
    private Object rgbCache;
    private Object image;
    private boolean opaqueTested = false;
    private boolean opaque;
    private Object scaleCache;
    private boolean animated;
    private long imageTime = -1;
    private String svgBaseURL;
    private byte[] svgData;
    private String imageName;

    /// Subclasses may use this and point to an underlying native image which might be
    /// null for a case of an image that doesn't use native drawing
    ///
    /// #### Parameters
    ///
    /// - `image`: native image object passed to the Codename One implementation
    protected Image(Object image) {
        this.image = image;
        animated = Display.impl.isAnimation(image);
    }

    /// Creates a new instance of ImageImpl
    Image(int[] imageArray, int w, int h) {
        this(Display.impl.createImage(imageArray, w, h));
    }

    /// Indicates whether the underlying platform supports creating an SVG Image
    ///
    /// #### Returns
    ///
    /// @return true if the method create SVG image would return a valid image object
    /// from an SVG Input stream
    public static boolean isSVGSupported() {
        return Display.impl.isSVGSupported();
    }

    /// Creates an SVG Image from the given byte array data and the base URL, this method
    /// will throw an exception if SVG is unsupported.
    ///
    /// #### Parameters
    ///
    /// - `baseURL`: URL which is used to resolve relative references within the SVG file
    ///
    /// - `animated`: indicates if the SVG features an animation
    ///
    /// - `data`: the conten of the SVG file
    ///
    /// #### Returns
    ///
    /// an image object that can be used as any other image object.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if resource lookup fail SVG is unsupported
    public static Image createSVG(String baseURL, boolean animated, byte[] data) throws IOException {
        Image i = new Image(Display.impl.createSVGImage(baseURL, data));
        i.animated = animated;
        i.svgBaseURL = baseURL;
        i.svgData = data;
        return i;
    }

    /// Creates an indexed image with byte data this method may return a native indexed image rather than
    /// an instance of the IndexedImage class
    ///
    /// #### Parameters
    ///
    /// - `width`: image width
    ///
    /// - `height`: image height
    ///
    /// - `palette`: the color palette to use with the byte data
    ///
    /// - `data`: byte data containing palette offsets to map to ARGB colors
    ///
    /// #### Deprecated
    ///
    /// try to avoid using indexed images explicitly
    public static Image createIndexed(int width, int height, int[] palette, byte[] data) {
        IndexedImage i = new IndexedImage(width, height, palette, data);
        CodenameOneImplementation impl = Display.impl;
        if (impl.isNativeIndexed()) {
            return new Image(impl.createNativeIndexed(i));
        }
        return i;
    }

    /// Creates an image from a path.
    ///
    /// #### Parameters
    ///
    /// - `path`: @param path If path begins with file: then the image will be loaded from FileSystemStorage.  Otherwise
    /// it will load from the jar resources.
    ///
    /// #### Returns
    ///
    /// newly created image object
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public static Image createImage(String path) throws IOException {
        try {
            return new Image(Display.impl.createImage(path));
        } catch (OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that
            // there is no Image.dispose method in existance.
            System.gc();
            System.gc();
            return new Image(Display.impl.createImage(path));
        }
    }

    /// creates an image from the given native image (e.g. MIDP image object)
    ///
    /// #### Parameters
    ///
    /// - `nativeImage`
    ///
    /// #### Returns
    ///
    /// newly created Codename One image object
    ///
    /// #### Deprecated
    ///
    /// @deprecated this method is deprecated as a warning! Don't use this method unless you actually
    /// know what you are doing, if you are invoking this method without understanding the distinction
    /// between native image and Codename One image then you are using the wrong method.
    public static Image createImage(Object nativeImage) {
        return new Image(nativeImage);
    }

    /// creates an image from an InputStream
    ///
    /// #### Parameters
    ///
    /// - `stream`: a given InputStream
    ///
    /// #### Returns
    ///
    /// the newly created image
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public static Image createImage(InputStream stream) throws IOException {
        try {
            return new Image(Display.impl.createImage(stream));
        } catch (OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that
            // there is no Image.dispose method in existence.
            System.gc();
            System.gc();
            return new Image(Display.impl.createImage(stream));
        }
    }

    /// creates an image from an RGB image
    ///
    /// #### Parameters
    ///
    /// - `rgb`: the RGB image array data
    ///
    /// - `width`: the image width
    ///
    /// - `height`: the image height
    ///
    /// #### Returns
    ///
    /// an image from an RGB image
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public static Image createImage(int[] rgb, int width, int height) {
        try {
            return new Image(Display.impl.createImage(rgb, width, height));
        } catch (OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that
            // there is no Image.dispose method in existance.
            System.gc();
            System.gc();
            return new Image(Display.impl.createImage(rgb, width, height));
        }
    }

    /// Creates a white opaque mutable image that may be manipulated using `#getGraphics()`.
    ///
    /// The sample below shows this method being used to create a screenshot for sharing the image:
    ///
    /// ```java
    /// Form hi = new Form("ShareButton");
    /// ShareButton sb = new ShareButton();
    /// sb.setText("Share Screenshot");
    /// hi.add(sb);
    ///
    /// Image screenshot = Image.createImage(hi.getWidth(), hi.getHeight());
    /// hi.revalidate();
    /// hi.setVisible(true);
    /// hi.paintComponent(screenshot.getGraphics(), true);
    ///
    /// String imageFile = FileSystemStorage.getInstance().getAppHomePath() + "screenshot.png";
    /// try(OutputStream os = FileSystemStorage.getInstance().openOutputStream(imageFile)) {
    ///     ImageIO.getImageIO().save(screenshot, os, ImageIO.FORMAT_PNG, 1);
    /// } catch(IOException err) {
    ///     Log.e(err);
    /// }
    /// sb.setImageToShare(imageFile, "image/png");
    /// ```
    ///
    /// The sample below demonstrates the drawing of a mask image to create a round image effect
    ///
    /// ```java
    /// Toolbar.setGlobalToolbar(true);
    /// Form hi = new Form("Rounder", new BorderLayout());
    /// Label picture = new Label("", "Container");
    /// hi.add(BorderLayout.CENTER, picture);
    /// hi.getUnselectedStyle().setBgColor(0xff0000);
    /// hi.getUnselectedStyle().setBgTransparency(255);
    /// Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
    /// Image camera = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);
    /// hi.getToolbar().addCommandToRightBar("", camera, (ev) -> {
    ///     try {
    ///         int width = Display.getInstance().getDisplayWidth();
    ///         Image capturedImage = Image.createImage(Capture.capturePhoto(width, -1));
    ///         Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
    ///         Graphics gr = roundMask.getGraphics();
    ///         gr.setColor(0xffffff);
    ///         gr.fillArc(0, 0, width, width, 0, 360);
    ///         Object mask = roundMask.createMask();
    ///         capturedImage = capturedImage.applyMask(mask);
    ///         picture.setIcon(capturedImage);
    ///         hi.revalidate();
    ///     } catch(IOException err) {
    ///         Log.e(err);
    ///     }
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `width`: the image width
    ///
    /// - `height`: the image height
    ///
    /// #### Returns
    ///
    /// an image in a given width and height dimension
    public static Image createImage(int width, int height) {
        return createImage(width, height, 0xffffffff);
    }

    /// Returns true if mutable images support alpha transparency
    ///
    /// #### Returns
    ///
    /// true if mutable images support alpha in their fillColor argument
    public static boolean isAlphaMutableImageSupported() {
        return Display.impl.isAlphaMutableImageSupported();
    }

    /// Creates a mutable image that may be manipulated using `#getGraphics()`.
    ///
    /// The sample below shows this method being used to create a screenshot for sharing the image:
    ///
    /// ```java
    /// Form hi = new Form("ShareButton");
    /// ShareButton sb = new ShareButton();
    /// sb.setText("Share Screenshot");
    /// hi.add(sb);
    ///
    /// Image screenshot = Image.createImage(hi.getWidth(), hi.getHeight());
    /// hi.revalidate();
    /// hi.setVisible(true);
    /// hi.paintComponent(screenshot.getGraphics(), true);
    ///
    /// String imageFile = FileSystemStorage.getInstance().getAppHomePath() + "screenshot.png";
    /// try(OutputStream os = FileSystemStorage.getInstance().openOutputStream(imageFile)) {
    ///     ImageIO.getImageIO().save(screenshot, os, ImageIO.FORMAT_PNG, 1);
    /// } catch(IOException err) {
    ///     Log.e(err);
    /// }
    /// sb.setImageToShare(imageFile, "image/png");
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `width`: the image width
    ///
    /// - `height`: the image height
    ///
    /// - `fillColor`: the color with which the image should be initially filled
    ///
    /// #### Returns
    ///
    /// an image in a given width and height dimension
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public static Image createImage(int width, int height, int fillColor) {
        try {
            return new Image(Display.impl.createMutableImage(width, height, fillColor));
        } catch (OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that
            // there is no Image.dispose method in existance.
            System.gc();
            System.gc();
            return new Image(Display.impl.createMutableImage(width, height, fillColor));
        }
    }

    /// creates an image from a given byte array data
    ///
    /// #### Parameters
    ///
    /// - `bytes`: the array of image data in a supported image format
    ///
    /// - `offset`: the offset of the start of the data in the array
    ///
    /// - `len`: the length of the data in the array
    ///
    /// #### Returns
    ///
    /// the newly created image
    @SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
    public static Image createImage(byte[] bytes, int offset, int len) {
        try {
            Object o = Display.impl.createImage(bytes, offset, len);
            if (o == null) {
                throw new IllegalArgumentException("create image failed for the given image data of length: " + len);
            }
            return new Image(o);
        } catch (OutOfMemoryError err) {
            // Images have a major bug on many phones where they sometimes throw
            // an OOM with no reason. A system.gc followed by the same call over
            // solves the problem. This has something to do with the fact that
            // there is no Image.dispose method in existance.
            System.gc();
            System.gc();
            return new Image(Display.impl.createImage(bytes, offset, len));
        }
    }

    /// The main use case of this method is the automatic rotation and flipping
    /// of an image returned from the camera or from the gallery, preserving the
    /// original format (jpeg or png); it detects the Exif Orientation Tag, if
    /// available (all the possible Exif Orientation Tag values are
    /// supported); transparency is not preserved.
    ///
    /// If there is no rotation or flipping, the image is only copied or scaled
    /// if necessary; if the capturedImage has a format different from jpeg and
    /// png, it is copied as it is.
    /// Note that this method doesn't rely on the
    /// file extension, but on the mime type of the capturedImage, since some
    /// devices don't give appropriate extension to images returned from the
    /// gallery.
    ///
    /// You can test all the possible orientation values downloading the images
    /// from the repository
    /// [EXIF Orientation-flag example images](https://github.com/recurser/exif-orientation-examples)
    ///
    /// Code example:
    ///
    /// ```java
    /// Form hi = new Form("Hi World", BoxLayout.y());
    /// Button cameraBtn = new Button("Open Camera");
    /// Button galleryBtn = new Button("Open Gallery");
    /// Label imageLbl = new Label();
    /// hi.addAll(cameraBtn, galleryBtn, FlowLayout.encloseCenter(imageLbl));
    /// hi.show();
    ///
    /// SuccessCallback callback = (String capturedPhoto) -> {
    ///     String rotatedPhoto = FileSystemStorage.getInstance().getAppHomePath() + "rotatedPhoto.jpg";
    ///     if (capturedPhoto != null) {
    ///         try {
    ///             // note: we set a maxSize to perform a faster rotation
    ///             int maxSize = CN.convertToPixels(50);
    ///             Image img = Image.exifRotation(capturedPhoto, rotatedPhoto, maxSize);
    ///             imageLbl.setIcon(img);
    ///             hi.revalidate();
    ///         } catch (IOException ex) {
    ///             Log.e(ex);
    ///         }
    ///     };
    /// };
    /// cameraBtn.addActionListener(a -> Capture.capturePhoto(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }));
    /// galleryBtn.addActionListener(a -> CN.openGallery(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }, CN.GALLERY_IMAGE));
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `capturedImage`: @param capturedImage is the FileSystemStorage path of a captured photo,
    /// usually inside a temporary directory
    ///
    /// #### Returns
    ///
    /// the rotated and/or flipped image
    public static Image exifRotation(String capturedImage) throws IOException {
        return exifRotation(capturedImage, null, -1);
    }

    /// The main use case of this method is the automatic rotation and flipping
    /// of an image returned from the camera or from the gallery, preserving the
    /// original format (jpeg or png); it detects the Exif Orientation Tag, if
    /// available (all the possible Exif Orientation Tag values are
    /// supported); transparency is not preserved.
    ///
    /// If there is no rotation or flipping, the image is only copied or scaled
    /// if necessary; if the capturedImage has a format different from jpeg and
    /// png, it is copied as it is.
    /// Note that this method doesn't rely on the
    /// file extension, but on the mime type of the capturedImage, since some
    /// devices don't give appropriate extension to images returned from the
    /// gallery.
    ///
    /// You can test all the possible orientation values downloading the images
    /// from the repository
    /// [EXIF Orientation-flag example images](https://github.com/recurser/exif-orientation-examples)
    ///
    /// Code example:
    ///
    /// ```java
    /// Form hi = new Form("Hi World", BoxLayout.y());
    /// Button cameraBtn = new Button("Open Camera");
    /// Button galleryBtn = new Button("Open Gallery");
    /// Label imageLbl = new Label();
    /// hi.addAll(cameraBtn, galleryBtn, FlowLayout.encloseCenter(imageLbl));
    /// hi.show();
    ///
    /// SuccessCallback callback = (String capturedPhoto) -> {
    ///     String rotatedPhoto = FileSystemStorage.getInstance().getAppHomePath() + "rotatedPhoto.jpg";
    ///     if (capturedPhoto != null) {
    ///         try {
    ///             // note: we set a maxSize to perform a faster rotation
    ///             int maxSize = CN.convertToPixels(50);
    ///             Image img = Image.exifRotation(capturedPhoto, rotatedPhoto, maxSize);
    ///             imageLbl.setIcon(img);
    ///             hi.revalidate();
    ///         } catch (IOException ex) {
    ///             Log.e(ex);
    ///         }
    ///     };
    /// };
    /// cameraBtn.addActionListener(a -> Capture.capturePhoto(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }));
    /// galleryBtn.addActionListener(a -> CN.openGallery(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }, CN.GALLERY_IMAGE));
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `capturedImage`: @param capturedImage is the FileSystemStorage path of a captured photo,
    /// usually inside a temporary directory
    ///
    /// - `rotatedImage`: @param rotatedImage  is the FileSystemStorage path in which the rotated
    /// photo is stored, normally this should be inside the
    /// FileSystemStorage.getAppHomePath(); it can be null if you don't want to
    /// save the rotated image to the FileSystemStorage.
    ///
    /// #### Returns
    ///
    /// the rotated and/or flipped image
    public static Image exifRotation(String capturedImage, String rotatedImage) throws IOException {
        return exifRotation(capturedImage, rotatedImage, -1);
    }

    /// The main use case of this method is the automatic rotation and flipping
    /// of an image returned from the camera or from the gallery, preserving the
    /// original format (jpeg or png); it detects the Exif Orientation Tag, if
    /// available (all the possible Exif Orientation Tag values are
    /// supported); transparency is not preserved.
    ///
    /// However, rotating and/or flipping an hi-res image is very inefficient,
    /// that's why you should consider to pass a maxSize value as small as
    /// possible: it makes this method working faster.
    ///
    /// If there is no rotation or flipping, the image is only copied or scaled
    /// if necessary; if the capturedImage has a format different from jpeg and
    /// png, it is copied as it is.
    /// Note that this method doesn't rely on the
    /// file extension, but on the mime type of the capturedImage, since some
    /// devices don't give appropriate extension to images returned from the
    /// gallery.
    ///
    /// You can test all the possible orientation values downloading the images
    /// from the repository
    /// [EXIF Orientation-flag example images](https://github.com/recurser/exif-orientation-examples)
    ///
    /// Code example:
    ///
    /// ```java
    /// Form hi = new Form("Hi World", BoxLayout.y());
    /// Button cameraBtn = new Button("Open Camera");
    /// Button galleryBtn = new Button("Open Gallery");
    /// Label imageLbl = new Label();
    /// hi.addAll(cameraBtn, galleryBtn, FlowLayout.encloseCenter(imageLbl));
    /// hi.show();
    ///
    /// SuccessCallback callback = (String capturedPhoto) -> {
    ///     String rotatedPhoto = FileSystemStorage.getInstance().getAppHomePath() + "rotatedPhoto.jpg";
    ///     if (capturedPhoto != null) {
    ///         try {
    ///             // note: we set a maxSize to perform a faster rotation
    ///             int maxSize = CN.convertToPixels(50);
    ///             Image img = Image.exifRotation(capturedPhoto, rotatedPhoto, maxSize);
    ///             imageLbl.setIcon(img);
    ///             hi.revalidate();
    ///         } catch (IOException ex) {
    ///             Log.e(ex);
    ///         }
    ///     };
    /// };
    /// cameraBtn.addActionListener(a -> Capture.capturePhoto(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }));
    /// galleryBtn.addActionListener(a -> CN.openGallery(l -> {
    ///     if (l != null && l.getSource() != null) {
    ///         callback.onSucess((String) l.getSource());
    ///     }
    /// }, CN.GALLERY_IMAGE));
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `capturedImage`: @param capturedImage is the FileSystemStorage path of a captured photo,
    /// usually inside a temporary directory
    ///
    /// - `rotatedImage`: @param rotatedImage  is the FileSystemStorage path in which the rotated
    /// photo is stored, normally this should be inside the
    /// FileSystemStorage.getAppHomePath(); it can be null if you don't want to
    /// save the rotated image to the FileSystemStorage.
    ///
    /// - `maxSize`: @param maxSize       is the maximum value of the width and height of the
    /// rotated images, that is scaled if necessary, keeping the ratio.
    ///
    /// #### Returns
    ///
    /// the com.codename1.ui.Image
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`
    public static Image exifRotation(String capturedImage, String rotatedImage, int maxSize) throws IOException {
        FileSystemStorage fss = FileSystemStorage.getInstance();
        boolean isJpeg = isJPEG(fss.openInputStream(capturedImage));
        boolean isPNG = isPNG(fss.openInputStream(capturedImage));
        String format;
        // IMPORTANT: we cannot rely on the file extension of the capturedImage path,
        // because some Android devices return images from the gallery without extension!
        if (!isJpeg && !isPNG) {
            // Only jpeg and png images are supported, but some devices can return also different formats from the gallery (like gif).
            // In this case, we simply copy the file.
            if (rotatedImage != null) {
                Util.copy(fss.openInputStream(capturedImage), fss.openOutputStream(rotatedImage));
            }
            return EncodedImage.create(fss.openInputStream(capturedImage), (int) fss.getLength(capturedImage));
        } else if (isJpeg) {
            format = ImageIO.FORMAT_JPEG;
        } else {
            format = ImageIO.FORMAT_PNG;
        }
        int orientation = getExifOrientationTag(fss.openInputStream(capturedImage));
        Image img = EncodedImage.create(fss.openInputStream(capturedImage), (int) fss.getLength(capturedImage));
        img.lock();
        if (maxSize > 0 && (img.getWidth() > maxSize || img.getHeight() > maxSize)) {
            // Tested that scaling the image before rotating is a lot more efficient than rotating before scaling
            Image scaled = img.scaledSmallerRatio(maxSize, maxSize);
            img.unlock();
            img = scaled;
            img.lock();
        }
        Image result;
        Image temp;
        switch (orientation) {
            case 0:
            case 1:
                // no rotation (but the image may have been scaled)
                result = img;
                break;
            case 2:
                // action required: flip horizontally
                result = img.flipHorizontally(false);
                break;
            case 3:
                //  action required: rotate 180 degrees
                result = img.rotate180Degrees(false);
                break;
            case 4:
                //  action required: flip vertically
                result = img.flipVertically(false);
                break;
            case 5:
                //  action required: rotate 270 degrees
                result = img.rotate270Degrees(false);
                break;
            case 6:
                //  action required: rotate 90 degrees
                result = img.rotate90Degrees(false);
                break;
            case 7:
                //  action required: flip horizontally and rotate 90 degrees
                temp = img.flipHorizontally(false);
                temp.lock();
                result = temp.rotate90Degrees(false);
                temp.unlock();
                break;
            case 8:
                //  action required: flip horizontally and rotate 270 degrees
                temp = img.flipHorizontally(false);
                temp.lock();
                result = temp.rotate270Degrees(false);
                temp.unlock();
                break;
            default:
                // this never should happen
                throw new IllegalStateException("Unsupported rotation");
        }
        img.unlock();
        if (rotatedImage != null) {
            OutputStream out = null; //NOPMD CloseResource
            try {
                out = fss.openOutputStream(rotatedImage);
                ImageIO.getImageIO().save(result, out, format, 0.9f);
            } finally {
                Util.cleanup(out);
            }
        }
        return EncodedImage.createFromImage(result, isJpeg);
    }

    /// Gets the EXIF orientation tag of an image if it's available.
    ///
    /// The Exif Orientation Tag is a number from 0 to 8, for the explanation of
    /// each value see the
    /// [Exif Orientation Tag](http://sylvana.net/jpegcrop/exif_orientation.html) page
    ///
    /// You can test all the possible orientation values downloading the images
    /// from the repository
    /// [EXIF Orientation-flag example images](https://github.com/recurser/exif-orientation-examples)
    ///
    /// #### Parameters
    ///
    /// - `path`: FileSystemStorage path
    ///
    /// #### Returns
    ///
    /// @return a value from 0 to 8; 0 is default in case of error or unavailable
    /// EXIF data.
    ///
    /// #### Throws
    ///
    /// - `java.io.IOException`
    public static int getExifOrientationTag(String path) throws IOException {
        InputStream in = null; //NOPMD CloseResource
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            return getExifOrientationTag(in);
        } finally {
            Util.cleanup(in);
        }
    }

    /// Gets the EXIF orientation tag of an image, if it's available.
    ///
    /// The Exif Orientation Tag is a number from 0 to 8, for the explanation of
    /// each value see the
    /// [Exif Orientation Tag](http://sylvana.net/jpegcrop/exif_orientation.html) page
    ///
    /// You can test all the possible orientation values downloading the images
    /// from the repository
    /// [EXIF Orientation-flag example images](https://github.com/recurser/exif-orientation-examples)
    ///
    /// #### Parameters
    ///
    /// - `is`
    ///
    /// #### Returns
    ///
    /// @return a value from 0 to 8; 0 is default in case of error or unavailable
    /// EXIF data.
    public static int getExifOrientationTag(InputStream is) {
        if (is == null) {
            return 0;
        }

        byte[] buf = new byte[8];
        int length = 0;

        // ISO/IEC 10918-1:1993(E)
        while (read(is, buf, 2) && (buf[0] & 0xFF) == 0xFF) {
            int marker = buf[1] & 0xFF;

            // Check if the marker is a padding.
            if (marker == 0xFF) {
                continue;
            }

            // Check if the marker is SOI or TEM.
            if (marker == 0xD8 || marker == 0x01) {
                continue;
            }
            // Check if the marker is EOI or SOS.
            if (marker == 0xD9 || marker == 0xDA) {
                return 0;
            }

            // Get the length and check if it is reasonable.
            if (!read(is, buf, 2)) {
                return 0;
            }
            length = pack(buf, 0, 2, false);
            if (length < 2) {
                Log.p("EXIF Invalid length", Log.ERROR);
                return 0;
            }
            length -= 2;

            // Break if the marker is EXIF in APP1.
            if (marker == 0xE1 && length >= 6) {
                if (!read(is, buf, 6)) {
                    return 0;
                }
                length -= 6;
                if (pack(buf, 0, 4, false) == 0x45786966
                        && pack(buf, 4, 2, false) == 0) {
                    break;
                }
            }

            // Skip other markers.
            try {
                long remaining = length;
                while (remaining > 0) {
                    long skipped = is.skip(remaining);
                    if (skipped > 0) {
                        remaining -= skipped;
                    } else if (read(is, buf, 1)) {
                        remaining--;
                    } else {
                        return 0;
                    }
                }
            } catch (IOException ex) {
                return 0;
            }
            length = 0;
        }

        // JEITA CP-3451 Exif Version 2.2
        if (length > 8) {
            int offset = 0;
            byte[] jpeg = new byte[length];
            if (!read(is, jpeg, length)) {
                return 0;
            }

            // Identify the byte order.
            int tag = pack(jpeg, offset, 4, false);
            if (tag != 0x49492A00 && tag != 0x4D4D002A) {
                Log.p("EXIF Invalid byte order", Log.ERROR);
                return 0;
            }
            boolean littleEndian = (tag == 0x49492A00);

            // Get the offset and check if it is reasonable.
            int count = pack(jpeg, offset + 4, 4, littleEndian) + 2;
            if (count < 10 || count > length) {
                Log.p("EXIF Invalid offset", Log.ERROR);
                return 0;
            }
            offset += count;
            length -= count;

            // Get the count and go through all the elements.
            count = pack(jpeg, offset - 2, 2, littleEndian);
            while (count-- > 0 && length >= 12) {
                // Get the tag and check if it is orientation.
                tag = pack(jpeg, offset, 2, littleEndian);
                if (tag == 0x0112) {
                    // We do not really care about type and count, do we?
                    return pack(jpeg, offset + 8, 2, littleEndian);
                }
                offset += 12;
                length -= 12;
            }
        }

        Log.p("EXIF Orientation not found", Log.DEBUG);
        return 0;
    }

    private static int pack(byte[] bytes, int offset, int length,
                            boolean littleEndian) {
        int step = 1;
        if (littleEndian) {
            offset += length - 1;
            step = -1;
        }

        int value = 0;
        while (length-- > 0) {
            value = (value << 8) | (bytes[offset] & 0xFF);
            offset += step;
        }
        return value;
    }

    private static boolean read(InputStream is, byte[] buf, int length) {
        try {
            return is.read(buf, 0, length) == length;
        } catch (IOException ex) {
            return false;
        }
    }

    /// Very fast method to detect if the given inputStream is a JPEG image
    /// (according to its guessed mime type)
    ///
    /// #### Parameters
    ///
    /// - `inputStream`
    ///
    /// #### Returns
    ///
    /// true if jpeg, false otherwise
    public static boolean isJPEG(InputStream inputStream) throws IOException {
        String type = Util.guessMimeType(inputStream);
        return "image/jpeg".equals(type) || "image/jpg".equals(type);
    }

    /// Very fast method to detect if the given inputStream is a PNG image
    /// (according to its guessed mime type)
    ///
    /// #### Parameters
    ///
    /// - `inputStream`
    ///
    /// #### Returns
    ///
    /// true if PNG, false otherwise
    public static boolean isPNG(InputStream inputStream) throws IOException {
        String type = Util.guessMimeType(inputStream);
        return "image/png".equals(type);
    }

    private HashMap<Dimension, Object> getScaleCache() {
        if (scaleCache == null) {
            HashMap<Dimension, Object> h = new HashMap<Dimension, Object>();
            scaleCache = Display.getInstance().createSoftWeakRef(h);
            return h;
        }
        HashMap<Dimension, Object> h = (HashMap<Dimension, Object>) Display.getInstance().extractHardRef(scaleCache);
        if (h == null) {
            h = new HashMap<Dimension, Object>();
            scaleCache = Display.getInstance().createSoftWeakRef(h);
        }
        return h;
    }

    /// Returns a cached scaled image
    ///
    /// #### Parameters
    ///
    /// - `size`: the size of the cached image
    ///
    /// #### Returns
    ///
    /// cached image
    Image getCachedImage(Dimension size) {
        Object w = getScaleCache().get(size);
        return (Image) Display.getInstance().extractHardRef(w);
    }

    /// Returns a cached scaled image
    ///
    /// #### Parameters
    ///
    /// - `size`: the size of the cached image
    ///
    /// #### Returns
    ///
    /// cached image
    void cacheImage(Dimension size, Image i) {
        Object w = Display.getInstance().createSoftWeakRef(i);
        getScaleCache().put(size, w);
    }

    /// Async lock is the equivalent of a lock operation, however it uses the given image as
    /// the hard cache and performs the actual image loading asynchronously. On completion this
    /// method will invoke repaint on the main form if applicable.
    ///
    /// #### Parameters
    ///
    /// - `internal`: the image to show while the actual image loads.
    public void asyncLock(Image internal) {
    }

    /// This callback indicates that a component pointing at this image is initialized, this allows
    /// an image to make performance sensitive considerations e.g. an encoded image
    /// might choose to cache itself in RAM.
    /// This method may be invoked multiple times.
    public void lock() {
    }

    /// Returns true if the image is locked
    ///
    /// #### Returns
    ///
    /// false by default
    public boolean isLocked() {
        return false;
    }

    /// This callback indicates that a component pointing at this image is now deinitilized
    /// This method may be invoked multiple times.
    public void unlock() {
    }

    /// Returns a platform specific DOM object that can be manipulated by the user
    /// to change the SVG Image
    ///
    /// #### Returns
    ///
    /// @return Platform dependent object, when JSR 226 is supported an SVGSVGElement might
    /// be returned.
    public Object getSVGDocument() {
        return Display.impl.getSVGDocument(image);
    }

    /// Indicates if this image represents an SVG file or a bitmap file
    ///
    /// #### Returns
    ///
    /// true if this is an SVG file
    public boolean isSVG() {
        return svgData != null;
    }

    /// Creates a mask from the given image, a mask can be used to apply an arbitrary
    /// alpha channel to any image. A mask is derived from the blue channel (LSB) of
    /// the given image, other channels are ignored.
    ///
    /// The generated mask can be used with the apply mask method.
    ///
    /// The sample below demonstrates the masking of an image based on a circle drawn on a mutable image:
    ///
    /// ```java
    /// Toolbar.setGlobalToolbar(true);
    /// Form hi = new Form("Rounder", new BorderLayout());
    /// Label picture = new Label("", "Container");
    /// hi.add(BorderLayout.CENTER, picture);
    /// hi.getUnselectedStyle().setBgColor(0xff0000);
    /// hi.getUnselectedStyle().setBgTransparency(255);
    /// Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
    /// Image camera = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);
    /// hi.getToolbar().addCommandToRightBar("", camera, (ev) -> {
    ///     try {
    ///         int width = Display.getInstance().getDisplayWidth();
    ///         Image capturedImage = Image.createImage(Capture.capturePhoto(width, -1));
    ///         Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
    ///         Graphics gr = roundMask.getGraphics();
    ///         gr.setColor(0xffffff);
    ///         gr.fillArc(0, 0, width, width, 0, 360);
    ///         Object mask = roundMask.createMask();
    ///         capturedImage = capturedImage.applyMask(mask);
    ///         picture.setIcon(capturedImage);
    ///         hi.revalidate();
    ///     } catch(IOException err) {
    ///         Log.e(err);
    ///     }
    /// });
    /// ```
    ///
    /// #### Returns
    ///
    /// mask object that can be used with applyMask
    public Object createMask() {
        int[] rgb = getRGBCached();
        int rlen = rgb.length;
        byte[] mask = new byte[rlen];
        for (int iter = 0; iter < rlen; iter++) {
            mask[iter] = (byte) (rgb[iter] & 0xff);
        }
        return new IndexedImage(getWidth(), getHeight(), null, mask);
    }

    /// Applies the given alpha mask onto this image and returns the resulting image
    /// see the createMask method for indication on how to convert an image into an alpha
    /// mask.
    ///
    /// The sample below demonstrates the masking of an image based on a circle drawn on a mutable image:
    ///
    /// ```java
    /// Toolbar.setGlobalToolbar(true);
    /// Form hi = new Form("Rounder", new BorderLayout());
    /// Label picture = new Label("", "Container");
    /// hi.add(BorderLayout.CENTER, picture);
    /// hi.getUnselectedStyle().setBgColor(0xff0000);
    /// hi.getUnselectedStyle().setBgTransparency(255);
    /// Style s = UIManager.getInstance().getComponentStyle("TitleCommand");
    /// Image camera = FontImage.createMaterial(FontImage.MATERIAL_CAMERA, s);
    /// hi.getToolbar().addCommandToRightBar("", camera, (ev) -> {
    ///     try {
    ///         int width = Display.getInstance().getDisplayWidth();
    ///         Image capturedImage = Image.createImage(Capture.capturePhoto(width, -1));
    ///         Image roundMask = Image.createImage(width, capturedImage.getHeight(), 0xff000000);
    ///         Graphics gr = roundMask.getGraphics();
    ///         gr.setColor(0xffffff);
    ///         gr.fillArc(0, 0, width, width, 0, 360);
    ///         Object mask = roundMask.createMask();
    ///         capturedImage = capturedImage.applyMask(mask);
    ///         picture.setIcon(capturedImage);
    ///         hi.revalidate();
    ///     } catch(IOException err) {
    ///         Log.e(err);
    ///     }
    /// });
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `mask`: mask object created by the createMask() method.
    ///
    /// - `x`: starting x where to apply the mask
    ///
    /// - `y`: starting y where to apply the mask
    ///
    /// #### Returns
    ///
    /// image masked based on the given object
    public Image applyMask(Object mask, int x, int y) {
        int[] rgb = getRGB();
        byte[] maskData = ((IndexedImage) mask).getImageDataByte();
        int mWidth = ((IndexedImage) mask).getWidth();
        int mHeight = ((IndexedImage) mask).getHeight();
        int imgWidth = getWidth();
        int aWidth = imgWidth - x;
        int aHeight = getHeight() - y;
        if (aWidth > mWidth) {
            aWidth = mWidth;
        }
        if (aHeight > mHeight) {
            aHeight = mHeight;
        }

        for (int xPos = 0; xPos < aWidth; xPos++) {
            for (int yPos = 0; yPos < aHeight; yPos++) {
                int aX = x + xPos;
                int aY = y + yPos;
                int imagePos = aX + aY * imgWidth;
                int maskAlpha = maskData[aX + aY * mWidth] & 0xff;
                maskAlpha = (maskAlpha << 24) & 0xff000000;
                rgb[imagePos] = (rgb[imagePos] & 0xffffff) | maskAlpha;

            }
        }
        return createImage(rgb, imgWidth, getHeight());
    }

    /// Applies the given alpha mask onto this image and returns the resulting image
    /// see the createMask method for indication on how to convert an image into an alpha
    /// mask.
    ///
    /// #### Parameters
    ///
    /// - `mask`: mask object created by the createMask() method.
    ///
    /// #### Returns
    ///
    /// image masked based on the given object
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: if the image size doesn't match the mask size
    public Image applyMask(Object mask) {
        int[] rgb = getRGB();
        byte[] maskData = ((IndexedImage) mask).getImageDataByte();
        int mWidth = ((IndexedImage) mask).getWidth();
        int mHeight = ((IndexedImage) mask).getHeight();
        if (mWidth != getWidth() || mHeight != getHeight()) {
            throw new IllegalArgumentException("Mask and image sizes don't match");
        }
        int mdlen = maskData.length;
        for (int iter = 0; iter < mdlen; iter++) {
            int maskAlpha = maskData[iter] & 0xff;
            maskAlpha = (maskAlpha << 24) & 0xff000000;
            rgb[iter] = (rgb[iter] & 0xffffff) | maskAlpha;
        }
        return createImage(rgb, mWidth, mHeight);
    }

    /// Applies the given alpha mask onto this image and returns the resulting image
    /// see the createMask method for indication on how to convert an image into an alpha
    /// mask. If the image is of a different size it will be scaled to mask size.
    ///
    /// #### Parameters
    ///
    /// - `mask`: mask object created by the createMask() method.
    ///
    /// #### Returns
    ///
    /// image masked based on the given object
    public Image applyMaskAutoScale(Object mask) {
        try {
            int mWidth = ((IndexedImage) mask).getWidth();
            int mHeight = ((IndexedImage) mask).getHeight();
            if (mWidth != getWidth() || mHeight != getHeight()) {
                return scaled(mWidth, mHeight).applyMask(mask);
            }
            return applyMask(mask);
        } catch (Throwable t) {
            Log.e(t);
        }
        return this;
    }

    /// Extracts a subimage from the given image allowing us to breakdown a single large image
    /// into multiple smaller images in RAM, this actually creates a standalone version
    /// of the image for use.
    ///
    /// #### Parameters
    ///
    /// - `x`: the x offset from the image
    ///
    /// - `y`: the y offset from the image
    ///
    /// - `width`: the width of internal images
    ///
    /// - `height`: the height of internal images
    ///
    /// - `processAlpha`: whether alpha should be processed as well as part of the cutting
    ///
    /// #### Returns
    ///
    /// An array of all the possible images that can be created from the source
    public Image subImage(int x, int y, int width, int height, boolean processAlpha) {
        // we use the getRGB API rather than the mutable image API to allow translucency to
        // be maintained in the newly created image
        int[] arr = new int[width * height];
        getRGB(arr, 0, x, y, width, height);

        Image i = new Image(Display.impl.createImage(arr, width, height));
        i.opaque = opaque;
        i.opaqueTested = opaqueTested;
        return i;
    }

    /// Creates a mirror image for the given image which is useful for some RTL scenarios. Notice that this
    /// method isn't the most efficient way to perform this task and is designed for portability over efficiency.
    ///
    /// #### Returns
    ///
    /// a mirrored image
    public Image mirror() {
        int width = getWidth();
        int height = getHeight();
        int[] tmp = getRGB();
        int[] arr = new int[width * height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                arr[x + y * width] = tmp[width - x - 1 + y * width];
            }
        }
        Image i = new Image(Display.impl.createImage(arr, width, height));
        i.opaque = opaque;
        i.opaqueTested = opaqueTested;
        return i;
    }

    /// Returns an instance of this image rotated by the given number of degrees. By default 90 degree
    /// angle divisions are supported, anything else is implementation dependent. This method assumes
    /// a square image. Notice that it is inefficient in the current implementation to rotate to
    /// non-square angles,
    ///
    /// E.g. rotating an image to 45, 90 and 135 degrees is inefficient. Use rotatate to 45, 90
    /// and then rotate the 45 to another 90 degrees to achieve the same effect with less memory.
    ///
    /// #### Parameters
    ///
    /// - `degrees`: A degree in right angle must be larger than 0 and up to 359 degrees
    ///
    /// #### Returns
    ///
    /// new image instance with the closest possible rotation
    public Image rotate(int degrees) {
        CodenameOneImplementation i = Display.impl;
        if (i.isRotationDrawingSupported()) {
            if (degrees >= 90) {
                int newTransform = 0;
                if (transform != 0) {
                    newTransform = (transform + degrees) % 360;
                } else {
                    newTransform = degrees % 360;
                }
                degrees %= 90;
                newTransform -= degrees;
                if (degrees != 0) {
                    Image newImage = new Image(Display.impl.rotate(image, degrees));
                    newImage.transform = newTransform;
                    return newImage;
                } else {
                    Image newImage = new Image(image);
                    newImage.transform = newTransform;
                    return newImage;
                }
            }
            if (degrees != 0) {
                return new Image(Display.impl.rotate(image, degrees));
            }
            return this;
        } else {
            return new Image(Display.impl.rotate(image, degrees));
        }
    }

    /// Creates a new image instance with the alpha channel of opaque/translucent
    /// pixels within the image using the new alpha value. Transparent (alpha == 0)
    /// pixels remain transparent. All other pixels will have the new alpha value.
    ///
    /// #### Parameters
    ///
    /// - `alpha`: New value for the entire alpha channel
    ///
    /// #### Returns
    ///
    /// @return Translucent/Opaque image based on the alpha value and the pixels of
    /// this image
    public Image modifyAlpha(byte alpha) {
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = getRGB();
        int alphaInt = (((int) alpha) << 24) & 0xff000000;
        for (int iter = 0; iter < size; iter++) {
            int currentAlpha = (arr[iter] >> 24) & 0xff;
            if (currentAlpha != 0) {
                arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
            }
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }

    /// Creates a new image instance with the alpha channel of opaque
    /// pixels within the image using the new alpha value. Transparent (alpha == 0)
    /// pixels remain transparent. Semi translucent pixels will be multiplied by the
    /// ratio difference and their translucency reduced appropriately.
    ///
    /// #### Parameters
    ///
    /// - `alpha`: New value for the entire alpha channel
    ///
    /// #### Returns
    ///
    /// @return Translucent/Opaque image based on the alpha value and the pixels of
    /// this image
    public Image modifyAlphaWithTranslucency(byte alpha) {
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = getRGB();
        int alphaInt = (((int) alpha) << 24) & 0xff000000;
        float alphaRatio = (alpha & 0xff) / 255.0f;
        for (int iter = 0; iter < size; iter++) {
            int currentAlpha = (arr[iter] >> 24) & 0xff;
            if (currentAlpha != 0) {
                if (currentAlpha == 0xff) {
                    arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
                } else {
                    int relative = (int) (currentAlpha * alphaRatio);
                    relative = (relative << 24) & 0xff000000;
                    arr[iter] = (arr[iter] & 0xffffff) | relative;
                }
            }
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }

    /// Creates a new image instance with the alpha channel of opaque/translucent
    /// pixels within the image using the new alpha value. Transparent (alpha == 0)
    /// pixels remain transparent. All other pixels will have the new alpha value.
    ///
    /// #### Parameters
    ///
    /// - `alpha`: New value for the entire alpha channel
    ///
    /// - `removeColor`: pixels matching this color are made transparent (alpha channel ignored)
    ///
    /// #### Returns
    ///
    /// @return Translucent/Opaque image based on the alpha value and the pixels of
    /// this image
    public Image modifyAlpha(byte alpha, int removeColor) {
        removeColor = removeColor & 0xffffff;
        int w = getWidth();
        int h = getHeight();
        int size = w * h;
        int[] arr = new int[size];
        getRGB(arr, 0, 0, 0, w, h);
        int alphaInt = (((int) alpha) << 24) & 0xff000000;
        for (int iter = 0; iter < size; iter++) {
            if ((arr[iter] & 0xff000000) != 0) {
                arr[iter] = (arr[iter] & 0xffffff) | alphaInt;
                if (removeColor == (0xffffff & arr[iter])) {
                    arr[iter] = 0;
                }
            }
        }
        Image i = new Image(arr, w, h);
        i.opaqueTested = true;
        i.opaque = false;
        return i;
    }

    /// If this is a mutable image a graphics object allowing us to draw on it
    /// is returned.
    ///
    /// #### Returns
    ///
    /// Graphics object allowing us to manipulate the content of a mutable image
    public Graphics getGraphics() {
        Graphics g = new Graphics(Display.impl.getNativeGraphics(image));
        rgbCache = null;    // the cache will become invalid
        return g;
    }

    /// Returns the width of the image
    ///
    /// #### Returns
    ///
    /// the width of the image
    public int getWidth() {
        if (transform != 0) {
            if (transform == 90 || transform == 270) {
                return Display.impl.getImageHeight(image);
            }
        }
        return Display.impl.getImageWidth(image);
    }

    /// Returns the height of the image
    ///
    /// #### Returns
    ///
    /// the height of the image
    public int getHeight() {
        if (transform != 0) {
            if (transform == 90 || transform == 270) {
                return Display.impl.getImageWidth(image);
            }
        }
        return Display.impl.getImageHeight(image);
    }

    /// Callback invoked internally by Codename One to draw the image/frame onto the display.
    /// Image subclasses can override this method to perform drawing of custom image types.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `nativeGraphics`: the underlying native graphics which might be essential for some image types
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y) {
        g.drawImage(image, x, y, transform);
    }

    /// Callback invoked internally by Codename One to draw the image/frame onto the display.
    /// Image subclasses can override this method to perform drawing of custom image types.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `nativeGraphics`: the underlying native graphics which might be essential for some image types
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `w`: the width to occupy
    ///
    /// - `h`: the height to occupy
    protected void drawImage(Graphics g, Object nativeGraphics, int x, int y, int w, int h) {
        g.drawImageWH(image, x, y, w, h);
    }

    /// Callback invoked internally by Codename One to draw a portion of the image onto the display.
    /// Image subclasses can override this method to perform drawing of custom image types.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics object
    ///
    /// - `nativeGraphics`: the underlying native graphics which might be essential for some image types
    ///
    /// - `x`: the x coordinate
    ///
    /// - `y`: the y coordinate
    ///
    /// - `imageX`: location within the image to draw
    ///
    /// - `imageY`: location within the image to draw
    ///
    /// - `imageWidth`: size of the location within the image to draw
    ///
    /// - `imageHeight`: size of the location within the image to draw
    void drawImageArea(Graphics g, Object nativeGraphics, int x, int y, int imageX, int imageY, int imageWidth, int imageHeight) {
        Display.impl.drawImageArea(nativeGraphics, image, x, y, imageX, imageY, imageWidth, imageHeight);
    }

    /// Obtains ARGB pixel data from the specified region of this image and
    /// stores it in the provided array of integers. Each pixel value is
    /// stored in 0xAARRGGBB format, where the high-order byte contains the
    /// alpha channel and the remaining bytes contain color components for red,
    /// green and blue, respectively. The alpha channel specifies the opacity of
    /// the pixel, where a value of 0x00  represents a pixel that is fully
    /// transparent and a value of 0xFF  represents a fully opaque pixel.
    /// The rgb information contained within the image, this method ignors
    /// rotation and mirroring in some/most situations and cannot be
    /// used in such cases.
    ///
    /// #### Parameters
    ///
    /// - `rgbData`: @param rgbData    an array of integers in which the ARGB pixel data is
    /// stored
    ///
    /// - `offset`: @param offset     the index into the array where the first ARGB value is
    /// stored
    ///
    /// - `scanlength`: @param scanlength the relative offset in the array between
    /// corresponding pixels in consecutive rows of the region
    ///
    /// - `x`: the x-coordinate of the upper left corner of the region
    ///
    /// - `y`: the y-coordinate of the upper left corner of the region
    ///
    /// - `width`: the width of the region
    ///
    /// - `height`: the height of the region
    void getRGB(int[] rgbData,
                int offset,
                int x,
                int y,
                int width,
                int height) {
        Display.impl.getRGB(image, rgbData, offset, x, y, width, height);
    }

    /// Extracts data from this image into the given RGBImage
    ///
    /// #### Parameters
    ///
    /// - `image`: RGBImage that would receive pixel data
    ///
    /// - `destX`: @param destX  x location within RGBImage into which the data will
    /// be written
    ///
    /// - `destY`: @param destY  y location within RGBImage into which the data will
    /// be written
    ///
    /// - `x`: location within the source image
    ///
    /// - `y`: location within the source image
    ///
    /// - `width`: size of the image to extract from the source image
    ///
    /// - `height`: size of the image to extract from the source image
    public void toRGB(RGBImage image,
                      int destX,
                      int destY,
                      int x,
                      int y,
                      int width,
                      int height) {
        getRGB(image.getRGB(), destX + destY * image.getWidth(), x, y, width, height);
    }

    /// Returns the content of this image as a newly created ARGB array.
    ///
    /// #### Returns
    ///
    /// new array instance containing the ARGB data within this image
    public int[] getRGB() {
        return getRGBImpl();
    }

    /// Returns the content of this image in the supplied ARGB array.
    ///
    /// #### Parameters
    ///
    /// - `rgbData`
    public void getRGB(int[] rgbData) {
        int width = getWidth();
        int height = getHeight();
        getRGB(rgbData, 0, 0, 0, width, height);

    }

    /// Returns the content of this image as a newly created ARGB array or a cached
    /// instance if possible. Note that cached instances may be garbage collected.
    ///
    /// #### Returns
    ///
    /// array instance containing the ARGB data within this image
    public int[] getRGBCached() {
        int[] r = getRGBCache();
        if (r == null) {
            r = getRGBImpl();
            rgbCache = Display.getInstance().createSoftWeakRef(r);
        }
        return r;
    }

    int[] getRGBCache() {
        if (rgbCache != null) {
            return (int[]) Display.getInstance().extractHardRef(rgbCache);
        }
        return null;
    }

    int[] getRGBImpl() {
        int width = getWidth();
        int height = getHeight();
        int[] rgbData = new int[width * height];
        getRGB(rgbData, 0, 0, 0, width, height);
        return rgbData;
    }

    /// Scales the image to the given width while updating the height based on the
    /// aspect ratio of the width
    ///
    /// #### Parameters
    ///
    /// - `width`: the given new image width
    ///
    /// #### Returns
    ///
    /// the newly created image
    public Image scaledWidth(int width) {
        float ratio = ((float) width) / ((float) getWidth());
        return scaled(width, Math.max(1, (int) (getHeight() * ratio)));
    }

    /// Scales the image to the given height while updating the width based on the
    /// aspect ratio of the height
    ///
    /// #### Parameters
    ///
    /// - `height`: the given new image height
    ///
    /// #### Returns
    ///
    /// the newly created image
    public Image scaledHeight(int height) {
        float ratio = ((float) height) / ((float) getHeight());
        return scaled(Math.max(1, (int) (getWidth() * ratio)), height);
    }

    /// Scales the image while maintaining the aspect ratio to the smaller size
    /// image
    ///
    /// #### Parameters
    ///
    /// - `width`: the given new image width
    ///
    /// - `height`: the given new image height
    ///
    /// #### Returns
    ///
    /// the newly created image
    public Image scaledSmallerRatio(int width, int height) {
        float hRatio = ((float) height) / ((float) getHeight());
        float wRatio = ((float) width) / ((float) getWidth());
        if (hRatio < wRatio) {
            return scaled((int) (getWidth() * hRatio), height);
        } else {
            return scaled(width, (int) (getHeight() * wRatio));
        }
    }

    /// Scales the image while maintaining the aspect ratio to the larger size
    /// image
    ///
    /// #### Parameters
    ///
    /// - `width`: the given new image width
    ///
    /// - `height`: the given new image height
    ///
    /// #### Returns
    ///
    /// the newly created image
    public Image scaledLargerRatio(int width, int height) {
        float hRatio = ((float) height) / ((float) getHeight());
        float wRatio = ((float) width) / ((float) getWidth());
        if (hRatio > wRatio) {
            return scaled(Math.round(getWidth() * hRatio), height);
        } else {
            return scaled(width, Math.round(getHeight() * wRatio));
        }
    }

    /// Returns a scaled version of this image image using the given width and height,
    /// this is a fast algorithm that preserves translucent information.
    /// The method accepts -1 to preserve aspect ratio in the given axis.
    ///
    /// #### Parameters
    ///
    /// - `width`: width for the scaling
    ///
    /// - `height`: height of the scaled image
    ///
    /// #### Returns
    ///
    /// new image instance scaled to the given height and width
    public Image scaled(int width, int height) {
        return scaledImpl(width, height);
    }

    /// Returns a scaled version of this image image using the given width and height,
    /// this is a fast algorithm that preserves translucent information.
    /// The method accepts -1 to preserve aspect ratio in the given axis.
    ///
    /// #### Parameters
    ///
    /// - `width`: width for the scaling
    ///
    /// - `height`: height of the scaled image
    ///
    /// #### Returns
    ///
    /// new image instance scaled to the given height and width
    Image scaledImpl(int width, int height) {
        if (width == -1) {
            return scaledHeight(height);
        }
        if (height == -1) {
            return scaledWidth(width);
        }
        Dimension d = new Dimension(width, height);
        Image i = getCachedImage(d);
        if (i != null) {
            return i;
        }

        if (svgData != null) {
            try {
                i = createSVG(svgBaseURL, animated, svgData);
            } catch (IOException ex) {
                i = new Image(this.image);
            }
        } else {
            i = new Image(this.image);
        }
        i.scaleCache = scaleCache;
        i.scale(width, height);
        i.transform = this.transform;
        i.animated = animated;
        i.svgBaseURL = svgBaseURL;
        i.svgData = svgData;
        cacheImage(new Dimension(width, height), i);
        return i;
    }

    /// Resizes/crops the image so that its center fills the given dimensions. This is similar to `com.codename1.ui.plaf.Style#BACKGROUND_IMAGE_SCALED_FILL`
    ///
    /// #### Parameters
    ///
    /// - `width`: the width to fill
    ///
    /// - `height`: the height to fill
    ///
    /// #### Returns
    ///
    /// a new image (or the same image if dimensions happen to match) filling the width/height
    public Image fill(int width, int height) {
        if (getWidth() == width && getHeight() == height) {
            return this;
        }
        Image nimage = scaledLargerRatio(width, height);
        if (nimage.getWidth() > width) {
            int diff = nimage.getWidth() - width;
            nimage = nimage.subImage(diff / 2, 0, width, height, true);
        } else {
            if (nimage.getHeight() > height) {
                int diff = nimage.getHeight() - height;
                nimage = nimage.subImage(0, diff / 2, width, height, true);
            }
        }
        return nimage;
    }

    /// Returns the platform specific image implementation, **warning** the
    /// implementation class can change between revisions of Codename One and platforms.
    ///
    /// #### Returns
    ///
    /// platform specific native implementation for this image object
    public Object getImage() {
        return image;
    }

    void setImage(Object image) {
        this.image = image;
    }

    /// Scale the image to the given width and height, this is a fast algorithm
    /// that preserves translucent information
    ///
    /// #### Parameters
    ///
    /// - `width`: width for the scaling
    ///
    /// - `height`: height of the scaled image
    ///
    /// #### Deprecated
    ///
    /// @deprecated scale should return an image rather than modify the image in place
    /// use scaled(int, int) instead
    public void scale(int width, int height) {
        //resize image
        image = Display.impl.scale(image, width, height);
    }

    boolean scaleArray(int srcWidth, int srcHeight, int height, int width, int[] currentArray, int[] destinationArray) {
        // Horizontal Resize
        int yRatio = (srcHeight << 16) / height;
        int xRatio = (srcWidth << 16) / width;
        int xPos = xRatio / 2;
        int yPos = yRatio / 2;

        // if there is more than 16bit color there is no point in using mutable
        // images since they won't save any memory
        boolean testOpaque = Display.getInstance().numColors() <= 65536 && (!opaqueTested);
        boolean currentOpaque = true;
        for (int y = 0; y < height; y++) {
            int srcY = yPos >> 16;
            getRGB(currentArray, 0, 0, srcY, srcWidth, 1);
            for (int x = 0; x < width; x++) {
                int srcX = xPos >> 16;
                int destPixel = x + y * width;
                if ((destPixel >= 0 && destPixel < destinationArray.length) && (srcX < currentArray.length)) {
                    destinationArray[destPixel] = currentArray[srcX];

                    // if all the pixels have an opaque alpha channel then the image is opaque
                    currentOpaque = testOpaque && currentOpaque && (currentArray[srcX] & 0xff000000) == 0xff000000;
                }
                xPos += xRatio;
            }
            yPos += yRatio;
            xPos = xRatio / 2;
        }
        if (testOpaque) {
            this.opaque = currentOpaque;
        }
        return opaque;
    }

    /// Returns true if this is an animated image
    ///
    /// #### Returns
    ///
    /// true if this image represents an animation
    public boolean isAnimation() {
        return animated;
    }

    /// {@inheritDoc}
    public boolean animate() {
        if (imageTime == -1) {
            imageTime = System.currentTimeMillis();
        }
        boolean val = Display.impl.animateImage(image, imageTime);
        imageTime = System.currentTimeMillis();
        return val;
    }

    /// Indicates whether this image is opaque or not
    ///
    /// #### Returns
    ///
    /// true if the image is completely opqaque which allows for some heavy optimizations
    public boolean isOpaque() {
        if (!opaqueTested) {
            opaque = Display.impl.isOpaque(this, image);
            opaqueTested = true;
        }
        return opaque;
    }

    void setOpaque(boolean opaque) {
        this.opaque = opaque;
        opaqueTested = true;
    }

    /// The name of the image is set for some images mostly to ease the debugging of Codename One application
    ///
    /// #### Returns
    ///
    /// the imageName
    public String getImageName() {
        return imageName;
    }

    /// The name of the image is set for some images mostly to ease the debugging of Codename One application
    ///
    /// #### Parameters
    ///
    /// - `imageName`: the imageName to set
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    /// DO NOT CALL THIS METHOD UNLESS YOU KNOW WHAT YOU ARE DOING, IT WILL CAUSE PLATFORM SPECIFC CRASHES OTHERWISE! Images dispose
    /// automatically for most cases except for very rare special cases.
    /// Images on devices usually holds a native memory, some platforms garbage
    /// collectors might fail to release the native and to fail with out of memory
    /// errors.
    /// Use this method to make sure the image will be released from memory, after
    /// calling this the image will become unusable.
    ///
    /// #### Deprecated
    ///
    /// SERIOUSLY, DON'T INVOKE THIS METHOD UNLESS YOU KNOW WHAT YOU ARE DOING IT WILL CAUSE PLATFORM SPECIFC CRASHES OTHERWISE. IF YOU INVOKED THIS METHOD YOU ARE PROBABLY MAKING A MISTAKE
    public void dispose() {
        if (image != null) {
            Display.impl.releaseImage(image);
        }
        image = null;
    }

    /// Rotates this image by 90 degrees while changing the ratio of the picture
    ///
    /// #### Parameters
    ///
    /// - `maintainOpacity`: whether the opacity in the image should be maintained
    ///
    /// #### Returns
    ///
    /// a new image rotated by 90 degrees
    public Image rotate90Degrees(boolean maintainOpacity) {
        return Display.impl.rotate90Degrees(this, maintainOpacity);
    }

    /// Rotates the image by 180 degrees
    ///
    /// #### Parameters
    ///
    /// - `maintainOpacity`: whether the opacity in the image should be maintained
    ///
    /// #### Returns
    ///
    /// a new image rotated by 180 degrees
    public Image rotate180Degrees(boolean maintainOpacity) {
        return Display.impl.rotate180Degrees(this, maintainOpacity);
    }

    /// Rotates the image by 270 degrees while changing the ratio of the picture
    ///
    /// #### Parameters
    ///
    /// - `maintainOpacity`: whether the opacity in the image should be maintained
    ///
    /// #### Returns
    ///
    /// a new image rotated by 270 degrees
    public Image rotate270Degrees(boolean maintainOpacity) {
        return Display.impl.rotate270Degrees(this, maintainOpacity);
    }

    /// Flips this image on the horizontal axis
    ///
    /// #### Parameters
    ///
    /// - `maintainOpacity`: whether the opacity in the image should be maintained
    ///
    /// #### Returns
    ///
    /// a new image flipped
    public Image flipHorizontally(boolean maintainOpacity) {
        return Display.impl.flipImageHorizontally(this, maintainOpacity);
    }

    /// Flips this image on the vertical axis
    ///
    /// #### Parameters
    ///
    /// - `maintainOpacity`: whether the opacity in the image should be maintained
    ///
    /// #### Returns
    ///
    /// a new image flipped
    public Image flipVertically(boolean maintainOpacity) {
        return Display.impl.flipImageVertically(this, maintainOpacity);
    }

    /// New label optimizations don't invoke drawImage and instead just pass the native image directly to
    /// the underlying renderer. This is problematic for some image types specifically timeline & FontImage
    /// and this method allows these classes to indicate that they need that legacy behavior of calling drawImage.
    ///
    /// #### Returns
    ///
    /// true if a drawImage call is a required
    public boolean requiresDrawImage() {
        return getImage() == null;
    }

    @Override
    public void addActionListener(ActionListener l) {
        if (listeners == null) {
            listeners = new EventDispatcher();
        }
        listeners.addListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        if (listeners != null) {
            listeners.removeListener(l);
        }
    }

    public void fireChangedEvent() {
        if (listeners == null) {
            return;
        }
        listeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Change));

    }
}
