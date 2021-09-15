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

package com.codename1.ui;

import com.codename1.compat.java.util.Objects;
import com.codename1.io.File;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.Resources;
import com.codename1.util.EasyThread;
import com.codename1.util.FailureCallback;
import com.codename1.util.OnComplete;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>{@code URLImage} allows us to create an image from a URL. If the image was downloaded
 * already it is fetched from cache; if not it is downloaded optionally scaled/adapted
 * and placed in cache.</p>
 * <p>By default an image is fetched lazily as it is asked for by the GUI unless
 * the fetch() method is invoked in which case the IO code is executed immediately.</p>
 *
 *  <p>
 * This sample code show a {@code URLImage} that is fetched to the title area background and scaled/cropped
 * to fit device specific dimensions.
 * </p>
 * <script src="https://gist.github.com/codenameone/085e3a8fa1c36829d812.js"></script>

 * <p>
 * This sample code shows the usage of the nestoria API to fill out an infinitely scrolling list in it
 * we use {@code URLImage} to fetch the icon.
 * </p>
 * <script src="https://gist.github.com/codenameone/af27af111ba766627363.js"></script>
 *
 * <img src="https://www.codenameone.com/img/developer-guide/components-infinitescrolladapter.png" alt="Sample usage of infinite scroll adapter" /><br><br>
 *
 * <p>
 * You can use adapters with masks using syntax similar to this to create a round image mask for a {@code URLImage}:
 * </p>
 * <script src="https://gist.github.com/codenameone/2515be7528ef3e402ec0.js"></script>
 *
 * @author Shai Almog
 */
public class URLImage extends EncodedImage {

    private static final Map<String,URLImage> pendingToStorage = new HashMap<String,URLImage>();
    private static final Map<String,URLImage> pendingToFile = new HashMap<String,URLImage>();
    private static EasyThread imageLoader = EasyThread.start("ImageLoader");

    /**
     * Flag used by {@link #createCachedImage(java.lang.String, java.lang.String, com.codename1.ui.Image, int) }.
     * Equivalent to {@link #RESIZE_FAIL}
     */
    public static final int FLAG_RESIZE_FAIL = 3;

    /**
     * Will fail if the downloaded image has a different size from the placeholder image
     */
    public static final ImageAdapter RESIZE_FAIL = new ImageAdapter() {
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if(downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                throw new RuntimeException("Invalid image size");
            }
            return downloadedImage;
        }

        public boolean isAsyncAdapter() {
            return false;
        }
    };

    /**
     * Flag used by {@link #createCachedImage(java.lang.String, java.lang.String, com.codename1.ui.Image, int) }
     * Equivalent to {@link #RESIZE_SCALE}.
     */
    public static final int FLAG_RESIZE_SCALE = 1;


    /**
     * Scales the image to match the size of the new image exactly
     */
    public static final ImageAdapter RESIZE_SCALE = new ImageAdapter() {
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if(downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                return downloadedImage.scaledEncoded(placeholderImage.getWidth(), placeholderImage.getHeight());
            }
            return downloadedImage;
        }

        public boolean isAsyncAdapter() {
            return false;
        }
    };

    /**
     * The exception handler is used for callbacks in case of an error
     * @return the exceptionHandler
     */
    public static ErrorCallback getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * The exception handler is used for callbacks in case of an error
     * @param aExceptionHandler the exceptionHandler to set
     */
    public static void setExceptionHandler(
            ErrorCallback aExceptionHandler) {
        exceptionHandler = aExceptionHandler;
    }

    static class ScaleToFill implements ImageAdapter {
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if(downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                Image tmp = downloadedImage.getInternal().scaledLargerRatio(placeholderImage.getWidth(), placeholderImage.getHeight());
                Image i = Image.createImage(placeholderImage.getWidth(), placeholderImage.getHeight(), 0);
                Graphics g = i.getGraphics();
                if(tmp.getWidth() > placeholderImage.getWidth()) {
                    int diff = tmp.getWidth() - placeholderImage.getWidth();
                    int x = diff / 2;
                    g.drawImage(tmp, -x, 0);
                    tmp = i;
                } else {
                    if(tmp.getHeight() > placeholderImage.getHeight()) {
                        int diff = tmp.getHeight() - placeholderImage.getHeight();
                        int y = diff / 2;
                        g.drawImage(tmp, 0, -y);
                        tmp = i;
                    }
                }
                tmp = postProcess(tmp);
                //return EncodedImage.createFromImage(tmp, tmp.isOpaque());
                return EncodedImage.createFromImage(tmp, false);
            }
            return downloadedImage;
        }

        Image postProcess(Image i) {
            return i;
        }

        public boolean isAsyncAdapter() {
            return false;
        }
    }

    /**
     * Flag used by {@link #createCachedImage(java.lang.String, java.lang.String, com.codename1.ui.Image, int) }.
     * Equivalent to {@link #RESIZE_SCALE_TO_FILL}.
     */
    public static final int FLAG_RESIZE_SCALE_TO_FILL = 2;
    /**
     * Scales the image to match to fill the area while preserving aspect ratio
     */
    public static final ImageAdapter RESIZE_SCALE_TO_FILL = new ScaleToFill();


    private final EncodedImage placeholder;
    private final String url;
    private final ImageAdapter adapter;
    private final String storageFile;
    private final String fileSystemFile;
    private boolean fetching;
    private byte[] imageData;
    private boolean repaintImage;
    private static final String IMAGE_SUFFIX = "ImageURLTMP";
    private boolean locked;

    /**
     * Invoked in a case of an error
     */
    public static interface ErrorCallback {
        public void onError(URLImage source, Exception err);
    }

    /**
     * The exception handler is used for callbacks in case of an error
     */
    private static ErrorCallback exceptionHandler;

    private URLImage(EncodedImage placeholder, String url, ImageAdapter adapter, String storageFile, String fileSystemFile) {
        super(placeholder.getWidth(), placeholder.getHeight());
        this.placeholder = placeholder;
        this.url = url;
        this.adapter = adapter;
        this.storageFile = storageFile;
        this.fileSystemFile = fileSystemFile;
    }

    /**
     * <p>Creates an adapter that uses an image as a Mask, this is roughly the same as SCALE_TO_FILL with the
     * exception that a mask will be applied later on. This adapter requires that the resulting image be in the size
     * of the imageMask!<br>
     * See the sample usage code below that implements a circular image masked downloader:</p>
     * <script src="https://gist.github.com/codenameone/2515be7528ef3e402ec0.js"></script>
     *
     * @param imageMask the mask image see the createMask() method of image for details of what a mask is, it
     * will be used as the reference size for the image and resulting images must be of the same size!
     * @return the adapter
     */
    public static ImageAdapter createMaskAdapter(Image imageMask) {
        final Object mask = imageMask.createMask();
        return new ScaleToFill() {
            @Override
            Image postProcess(Image i) {
                return i.applyMask(mask);
            }
        };
    }

    public static ImageAdapter createMaskAdapter(final Object mask) {
        return new ScaleToFill() {
            @Override
            Image postProcess(Image i) {
                return i.applyMask(mask);
            }
        };
    }

    class DownloadCompleted implements ActionListener, Runnable {
        private EncodedImage adapt;
        private EncodedImage adaptedIns;
        private Image sourceImage;
        public void run() {
            adaptedIns = adapter.adaptImage(adapt, placeholder);
        }

        public void actionPerformed(ActionEvent evt) {
            if(adapter != null) {
                try {
                    EncodedImage img;
                    if (sourceImage == null) {
                        byte[] d;
                        InputStream is;
                        if(storageFile != null) {
                            d = new byte[Storage.getInstance().entrySize(storageFile + IMAGE_SUFFIX)];
                            is = Storage.getInstance().createInputStream(storageFile + IMAGE_SUFFIX);
                        } else {
                            d = new byte[(int)FileSystemStorage.getInstance().getLength(fileSystemFile + IMAGE_SUFFIX)];
                            is = FileSystemStorage.getInstance().openInputStream(fileSystemFile + IMAGE_SUFFIX);
                        }
                        Util.readFully(is, d);
                        img = EncodedImage.create(d);
                    } else {
                        img = EncodedImage.createFromImage(sourceImage, false);
                    }
                    EncodedImage adapted;
                    if(adapter.isAsyncAdapter()) {
                        adapt = img;
                        Display.getInstance().invokeAndBlock(this);
                        adapted = adaptedIns;
                        adaptedIns = null;
                        adapt = null;
                    } else {
                        try {
                            adapted = adapter.adaptImage(img, placeholder);
                        } catch(Exception err) {
                            if(exceptionHandler != null) {
                                exceptionHandler.onError(URLImage.this, err);
                            } else {
                                Log.p("Failed to load image from URL: " + url);
                                Log.e(err);
                            }
                            return;
                        }
                    }
                    if(storageFile != null) {
                        OutputStream o = Storage.getInstance().createOutputStream(storageFile);
                        o.write(adapted.getImageData());
                        o.close();
                        Storage.getInstance().deleteStorageFile(storageFile + IMAGE_SUFFIX);
                        pendingToStorage.remove(storageFile);
                    } else if (fileSystemFile != null) {
                        OutputStream o = FileSystemStorage.getInstance().openOutputStream(fileSystemFile);
                        o.write(adapted.getImageData());
                        o.close();
                        FileSystemStorage.getInstance().delete(fileSystemFile + IMAGE_SUFFIX);
                        pendingToFile.remove(fileSystemFile);
                    }
                } catch (IOException ex) {
                    if(exceptionHandler != null) {
                        exceptionHandler.onError(URLImage.this, ex);
                    } else {
                        Log.e(ex);
                    }
                    return;
                }
            }
            fetching = false;
            // invoke fetch again to load the local files
            fetch();
        }

        /**
         * Used in cases where the source image is already downloaded ( so we don't need to try to load it from storage/file system.
         * @param sourceImage
         */
        void setSourceImage(Image sourceImage) {
            this.sourceImage = sourceImage;
        }
    }

    private void runAndWait(Runnable r) {
        if (platformSupportsImageLoadingOffEdt()) {
            String oldShowEDTWarnings = CN.getProperty("platformHint.showEDTWarnings", "false");
            CN.setProperty("platformHint.showEDTWarnings", "false");
            try {
                r.run();
            } finally {
                CN.setProperty("platformHint.showEDTWarnings", oldShowEDTWarnings);
            }
        } else {
            CN.callSeriallyAndWait(r);
        }
    }

    private boolean platformSupportsImageLoadingOffEdt() {
        return CN.isSimulator() || !CN.getPlatformName().equals("ios");
    }


    private void loadImageFromStorageURLToStorage(final String targetKey) {
        imageLoader.run(new Runnable() {
            public void run() {
                try {
                    if (!Objects.equals(url, targetKey)) {
                        InputStream input = Storage.getInstance().createInputStream(url);
                        OutputStream output = Storage.getInstance().createOutputStream(targetKey);
                        Util.copy(input, output);
                    }
                    runAndWait(new Runnable() {
                        public void run() {
                            try {
                                Image value = Image.createImage(Storage.getInstance().createInputStream(targetKey));
                                DownloadCompleted onComplete = new DownloadCompleted();
                                onComplete.setSourceImage(value);
                                onComplete.actionPerformed(new ActionEvent(value));
                            } catch (Exception ex) {
                                if(exceptionHandler != null) {
                                    exceptionHandler.onError(URLImage.this, ex);
                                } else {
                                    Log.e(new RuntimeException(ex.toString()));
                                }
                            }
                        }
                    });
                } catch (Exception t) {
                    if(exceptionHandler != null) {
                        exceptionHandler.onError(URLImage.this, t);
                    } else {
                        Log.e(new RuntimeException(t.toString()));
                    }
                }
            }
        });
    }

    private void loadImageFromLocalUrl(final String targetKey, final boolean useFileSystemStorage) {
        imageLoader.run(new Runnable() {
            public void run() {
                try {
                    InputStream input;
                    if (url.startsWith("file:/")) {
                        input = FileSystemStorage.getInstance().openInputStream(url);
                    } else if (url.startsWith("jar:/")) {
                        input = CN.getResourceAsStream(url.substring(url.lastIndexOf("/")));
                    } else if (url.startsWith("image:")) {
                        input = null;
                    } else {
                        input = Storage.getInstance().createInputStream(url);
                    }
                    if (input != null) {
                        OutputStream output = useFileSystemStorage ? FileSystemStorage.getInstance().openOutputStream(targetKey) : Storage.getInstance().createOutputStream(targetKey);
                        Util.copy(input, output);
                    }
                    runAndWait(new Runnable() {
                        public void run() {
                            try {
                                Image value = url.startsWith("image:") ? Resources.getGlobalResources().getImage(url) :
                                        Image.createImage(useFileSystemStorage ? FileSystemStorage.getInstance().openInputStream(targetKey) : Storage.getInstance().createInputStream(targetKey));
                                DownloadCompleted onComplete = new DownloadCompleted();
                                onComplete.setSourceImage(value);
                                onComplete.actionPerformed(new ActionEvent(value));
                            } catch (Exception ex) {
                                if(exceptionHandler != null) {
                                    exceptionHandler.onError(URLImage.this, ex);
                                } else {
                                    Log.e(new RuntimeException(ex.toString()));
                                }
                            }
                        }
                    });
                } catch (Exception t) {
                    if(exceptionHandler != null) {
                        exceptionHandler.onError(URLImage.this, t);
                    } else {
                        Log.e(new RuntimeException(t.toString()));
                    }
                }
            }
        });
    }





    /**
     * Images are normally fetched from storage or network only as needed,
     * however if the download must start before the image is drawn this method
     * can be invoked. Notice that "immediately" doesn't mean synchronously, it just
     * means that the image will be added to the queue right away but probably won't be
     * available by the time the method completes.
     */
    public void fetch() {
        if(fetching || imageData != null) {
            return;
        }
        fetching = true;
        try {
            locked = super.isLocked();
            if(storageFile != null) {
                if(Storage.getInstance().exists(storageFile)) {
                    super.unlock();
                    imageData = new byte[Storage.getInstance().entrySize(storageFile)];
                    InputStream is = Storage.getInstance().createInputStream(storageFile);
                    Util.readFully(is, imageData);
                    resetCache();
                    fetching = false;
                    repaintImage = true;
                    fireChangedEvent();
                    return;
                }
                if (adapter != null) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        Util.downloadImageToStorage(url, storageFile + IMAGE_SUFFIX,
                                new SuccessCallback<Image>() {
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            public void run()  {
                                                runAndWait(new Runnable() {
                                                    public void run() {
                                                        DownloadCompleted onComplete = new DownloadCompleted();
                                                        onComplete.setSourceImage(value);
                                                        onComplete.actionPerformed(new ActionEvent(value));
                                                    }
                                                });
                                            }
                                        });


                                    }

                                });
                    } else {
                        // from file
                        loadImageFromLocalUrl(storageFile + IMAGE_SUFFIX, false);
                    }
                } else {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        // Load image from http
                        Util.downloadImageToStorage(url, storageFile,
                                new SuccessCallback<Image>() {
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    public void run() {
                                                        DownloadCompleted onComplete = new DownloadCompleted();
                                                        onComplete.setSourceImage(value);
                                                        onComplete.actionPerformed(new ActionEvent(value));
                                                    }
                                                });
                                            }
                                        });


                                    }
                                });
                    } else {
                        //load image from file system
                        loadImageFromLocalUrl(storageFile, false);
                    }
                }
            } else {
                if(FileSystemStorage.getInstance().exists(fileSystemFile)) {
                    super.unlock();
                    imageData = new byte[(int)FileSystemStorage.getInstance().getLength(fileSystemFile)];
                    InputStream is = FileSystemStorage.getInstance().openInputStream(fileSystemFile);
                    Util.readFully(is, imageData);
                    resetCache();
                    fetching = false;
                    repaintImage = true;
                    fireChangedEvent();
                    return;
                }
                if(adapter != null) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        // Load image over http
                        Util.downloadImageToFileSystem(url, fileSystemFile + IMAGE_SUFFIX,
                                new SuccessCallback<Image>() {

                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    public void run() {
                                                        DownloadCompleted onComplete = new DownloadCompleted();
                                                        onComplete.setSourceImage(value);
                                                        onComplete.actionPerformed(new ActionEvent(value));
                                                    }
                                                });
                                            }
                                        });


                                    }

                                });
                    } else  {
                        // load image from file system
                        loadImageFromLocalUrl(fileSystemFile + IMAGE_SUFFIX, true);
                    }
                } else {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        Util.downloadImageToFileSystem(url, fileSystemFile,
                                new SuccessCallback<Image>() {

                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    public void run() {
                                                        DownloadCompleted onComplete = new DownloadCompleted();
                                                        onComplete.setSourceImage(value);
                                                        onComplete.actionPerformed(new ActionEvent(value));
                                                    }
                                                });
                                            }
                                        });


                                    }

                                });
                    } else {
                        loadImageFromLocalUrl(fileSystemFile, true);
                    }
                }
            }
        } catch(IOException ioErr) {
            if(exceptionHandler != null) {
                exceptionHandler.onError(URLImage.this, ioErr);
            } else {
                throw new RuntimeException(ioErr.toString());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    protected Image getInternal() {
        if(imageData == null) {
            fetch();
            return placeholder;
        }
        return super.getInternal();
    }

    @Override
    public boolean requiresDrawImage() {
        // IT is important to override this for URLImage because the default implementation will
        // trigger that the URL image downloads its image, which is disastrous for performance
        // if you have a lot of URL images.
        // Ideally, the image doesn't get downloaded until it is needed for painting.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getImageData() {
        if(imageData != null) {
            return imageData;
        }
        return placeholder.getImageData();
    }

    /**
     * {@inheritDoc}
     */
    public boolean animate() {
        if(repaintImage) {
            repaintImage = false;
            if(locked) {
                super.lock();
                locked = false;
            }
            return true;
        }
        return false;
    }

    /**
     * Block this method from external callers as it might break the functionality
     */
    @Override
    public void lock() {
    }

    /**
     * Block this method from external callers as it might break the functionality
     */
    @Override
    public void unlock() {
    }




    /**
     * {@inheritDoc}
     */
    public boolean isAnimation() {
        return repaintImage || imageData == null;
    }

    /**
     * Creates an image the will be downloaded on the fly as necessary with RESIZE_SCALE_TO_FILL as
     * the default behavior
     *
     * @param placeholder the image placeholder is shown as the image is loading/downloading 
     * and serves as the guideline to the size of the downloaded image.
     * @param storageFile the file in storage to which the image will be stored
     * @param url the url from which the image is fetched
     * @return a URLImage that will initialy just delegate to the placeholder
     */
    public static URLImage createToStorage(EncodedImage placeholder, String storageFile, String url) {
        return createToStorage(placeholder, storageFile, url, RESIZE_SCALE_TO_FILL);
    }

    /**
     * Creates an image the will be downloaded on the fly as necessary
     *
     * @param placeholder the image placeholder is shown as the image is loading/downloading 
     * and serves as the guideline to the size of the downloaded image.
     * @param storageFile the file in storage to which the image will be stored
     * @param url the url from which the image is fetched
     * @param adapter the adapter used to adapt the image into place, it should scale the image
     * if necessary
     * @return a URLImage that will initialy just delegate to the placeholder
     */
    public static URLImage createToStorage(EncodedImage placeholder, String storageFile, String url, ImageAdapter adapter) {
        // intern is used to trigger an NPE in case of a null URL or storage file
        URLImage out = pendingToStorage.get(storageFile);
        if (out != null) {
            return out;
        }
        out = new URLImage(placeholder, url.intern(), adapter, storageFile.intern(), null);
        pendingToStorage.put(storageFile, out);
        return out;
    }

    /**
     * Creates an image the will be downloaded on the fly as necessary
     *
     * @param placeholder the image placeholder is shown as the image is loading/downloading 
     * and serves as the guideline to the size of the downloaded image.
     * @param file the file in the file system to which the image will be stored
     * @param url the url from which the image is fetched
     * @param adapter the adapter used to adapt the image into place, it should scale the image
     * if necessary
     * @return a URLImage that will initialy just delegate to the placeholder
     */
    public static URLImage createToFileSystem(EncodedImage placeholder, String file, String url, ImageAdapter adapter) {
        // intern is used to trigger an NPE in case of a null URL or storage file
        URLImage out = pendingToFile.get(file);
        if (out != null) {
            return out;
        }
        out = new URLImage(placeholder, url.intern(), adapter, null, file.intern());
        pendingToFile.put(file, out);
        return out;
    }

    /**
     * Creates an image that will be downloaded on the fly as necessary.  On platforms that support a native
     * image cache (e.g. Javascript), the image will be loaded directly from the native cache (i.e. it defers to the 
     * platform to handle all caching considerations.  On platforms that don't have a native image cache but
     * do have a caches directory {@link FileSystemStorage#hasCachesDir()}, this will call {@link #createToFileSystem(com.codename1.ui.EncodedImage, java.lang.String, java.lang.String, com.codename1.ui.URLImage.ImageAdapter) }
     * with a file location in the caches directory.  In all other cases, this will call {@link #createToStorage(com.codename1.ui.EncodedImage, java.lang.String, java.lang.String) }.
     *
     * @param imageName The name of the image.
     * @param url the URL from which the image is fetched
     * @param placeholder the image placeholder is shown as the image is loading/downloading 
     * and serves as the guideline to the size of the downloaded image.
     * @param resizeRule One of {@link #FLAG_RESIZE_FAIL}, {@link #FLAG_RESIZE_SCALE}, or {@link #FLAG_RESIZE_SCALE_TO_FILL}.
     * @return a Image that will initially just delegate to the placeholder
     */
    public static Image createCachedImage(String imageName, String url, Image placeholder, int resizeRule) {
        if (Display.getInstance().supportsNativeImageCache()) {
            CachedImage im = new CachedImage(placeholder, url, resizeRule);
            im.setImageName(imageName);
            return im;
        } else {
            ImageAdapter adapter = null;
            switch (resizeRule) {
                case FLAG_RESIZE_FAIL:
                    adapter = RESIZE_FAIL;
                    break;
                case FLAG_RESIZE_SCALE:
                    adapter = RESIZE_SCALE;
                    break;
                case FLAG_RESIZE_SCALE_TO_FILL:
                    adapter = RESIZE_SCALE_TO_FILL;
                    break;
                default:
                    adapter = RESIZE_SCALE_TO_FILL;
                    break;
            }
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (fs.hasCachesDir()) {
                String name = "cn1_image_cache["+url+"]";
                name = StringUtil.replaceAll(name, "/", "_");
                name = StringUtil.replaceAll(name, "\\", "_");
                name = StringUtil.replaceAll(name, "%", "_");
                name = StringUtil.replaceAll(name, "?", "_");
                name = StringUtil.replaceAll(name, "*", "_");
                name = StringUtil.replaceAll(name, ":", "_");
                name = StringUtil.replaceAll(name, "=", "_");

                String filePath = fs.getCachesDir() + fs.getFileSystemSeparator() + name;
                //System.out.println("Creating to file system "+filePath);
                URLImage im = createToFileSystem(
                        EncodedImage.createFromImage(placeholder, false),
                        filePath,
                        url,
                        adapter
                );
                im.setImageName(imageName);
                return im;
            } else {
                //System.out.println("Creating to storage ");
                URLImage im = createToStorage(EncodedImage.createFromImage(placeholder, false),
                        "cn1_image_cache[" + url + "@" + placeholder.getWidth() + "x" + placeholder.getHeight(),
                        url,
                        adapter
                );
                im.setImageName(imageName);
                return im;
            }
        }
    }

    /**
     * Allows applying resize logic to downloaded images you can use constant
     * resize behaviors defined in this class. This class allows masking and various
     * other effects to be applied to downloaded images.
     * <p>Notice: adapters happen before the image is saved so they will only happen once 
     * and the image will be saved as "adapted" which can be great for performance but
     * is also permanent. E.g. If you mask an image it will remain masked.
     */
    public static interface ImageAdapter {
        /**
         * Allows the downloaded image to be adapted e.g if it isn't the same size of the placeholder image.
         *
         * @param downloadedImage the downloaded image
         * @param placeholderImage the placeholder image
         * @return the adapted image or the same image
         */
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage);

        /**
         * Return true if the adapter should work on a separate thread to avoid blocking the EDT
         * this is especially important for image masks and heavy image manipulation
         * @return true to run off the EDT
         */
        public boolean isAsyncAdapter();
    }


    /**
     *CachedImage used by {@link #createCachedImage(java.lang.String, java.lang.String, com.codename1.ui.Image, int) }
     */
    private static class CachedImage extends Image {
        boolean fetching;
        int resizeRule;
        Object image;
        Image placeholderImage;
        String url;
        boolean repaintImage;

        @Override
        public boolean animate() {
            if (repaintImage) {
                repaintImage = false;
                return true;
            }
            return false;
        }

        @Override
        public boolean isAnimation() {
            return repaintImage || image == null;
        }

        @Override
        public Object getImage() {
            if (image != null) {
                return image;
            }
            return super.getImage();
        }

        public CachedImage(Image placeholder, String url, int resize) {
            super(placeholder.getImage());
            this.url = url;
            this.resizeRule = resize;
            this.placeholderImage = placeholder;
            Util.downloadImageToCache(url, new SuccessCallback<Image>() {
                public void onSucess(Image downloadedImage) {
                    fetching = false;
                    switch (resizeRule) {
                        case FLAG_RESIZE_FAIL: {
                            if(downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                                throw new RuntimeException("Invalid image size");
                            }
                            break;
                        }
                        case FLAG_RESIZE_SCALE: {
                            downloadedImage = downloadedImage.scaled(placeholderImage.getWidth(), placeholderImage.getHeight());
                            break;
                        }
                        case FLAG_RESIZE_SCALE_TO_FILL: {
                            if(downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                                Image tmp = downloadedImage.scaledLargerRatio(placeholderImage.getWidth(), placeholderImage.getHeight());
                                Image i = Image.createImage(placeholderImage.getWidth(), placeholderImage.getHeight(), 0);
                                Graphics g = i.getGraphics();
                                g.setAntiAliased(true);
                                if(tmp.getWidth() > placeholderImage.getWidth()) {
                                    int diff = tmp.getWidth() - placeholderImage.getWidth();
                                    int x = diff / 2;
                                    g.drawImage(tmp, -x, 0);
                                    tmp = i;
                                } else {
                                    if(tmp.getHeight() > placeholderImage.getHeight()) {
                                        int diff = tmp.getHeight() - placeholderImage.getHeight();
                                        int y = diff / 2;
                                        g.drawImage(tmp, 0, -y);
                                        tmp = i;
                                    }
                                }

                                downloadedImage = tmp;
                            }
                            break;
                        }
                    }

                    image = downloadedImage.getImage();
                    repaintImage = true;
                    fireChangedEvent();
                }

            }, new FailureCallback<Image>() {
                public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                    throw new RuntimeException("Failed to download image "+CachedImage.this.url+" from cache");
                }
            });
        }
    }
}
