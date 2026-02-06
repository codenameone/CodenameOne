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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Storage;
import com.codename1.io.Util;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.Resources;
import com.codename1.util.EasyThread;
import com.codename1.util.FailureCallback;
import com.codename1.util.StringUtil;
import com.codename1.util.SuccessCallback;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/// `URLImage` allows us to create an image from a URL. If the image was downloaded
/// already it is fetched from cache; if not it is downloaded optionally scaled/adapted
/// and placed in cache.
///
/// By default an image is fetched lazily as it is asked for by the GUI unless
/// the fetch() method is invoked in which case the IO code is executed immediately.
///
/// This sample code show a `URLImage` that is fetched to the title area background and scaled/cropped
/// to fit device specific dimensions.
///
/// ```java
/// Toolbar.setGlobalToolbar(true);
///
/// Form hi = new Form("Toolbar", new BoxLayout(BoxLayout.Y_AXIS));
/// EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(hi.getWidth(), hi.getWidth() / 5, 0xffff0000), true);
/// URLImage background = URLImage.createToStorage(placeholder, "400px-AGameOfThrones.jpg",
///         "http://awoiaf.westeros.org/images/thumb/9/93/AGameOfThrones.jpg/400px-AGameOfThrones.jpg");
/// background.fetch();
/// Style stitle = hi.getToolbar().getTitleComponent().getUnselectedStyle();
/// stitle.setBgImage(background);
/// stitle.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FILL);
/// stitle.setPaddingUnit(Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS, Style.UNIT_TYPE_DIPS);
/// stitle.setPaddingTop(15);
/// SpanButton credit = new SpanButton("This excerpt is from A Wiki Of Ice And Fire. Please check it out by clicking here!");
/// credit.addActionListener((e) -> Display.getInstance().execute("http://awoiaf.westeros.org/index.php/A_Game_of_Thrones"));
/// hi.add(new SpanLabel("A Game of Thrones is the first of seven planned novels in A Song of Ice and Fire, an epic fantasy series by American author George R. R. Martin. It was first published on 6 August 1996. The novel was nominated for the 1998 Nebula Award and the 1997 World Fantasy Award,[1] and won the 1997 Locus Award.[2] The novella Blood of the Dragon, comprising the Daenerys Targaryen chapters from the novel, won the 1997 Hugo Award for Best Novella. ")).
///         add(new Label("Plot introduction", "Heading")).
///         add(new SpanLabel("A Game of Thrones is set in the Seven Kingdoms of Westeros, a land reminiscent of Medieval Europe. In Westeros the seasons last for years, sometimes decades, at a time.\n\n" +
///             "Fifteen years prior to the novel, the Seven Kingdoms were torn apart by a civil war, known alternately as \"Robert's Rebellion\" and the \"War of the Usurper.\" Prince Rhaegar Targaryen kidnapped Lyanna Stark, arousing the ire of her family and of her betrothed, Lord Robert Baratheon (the war's titular rebel). The Mad King, Aerys II Targaryen, had Lyanna's father and eldest brother executed when they demanded her safe return. Her second brother, Eddard, joined his boyhood friend Robert Baratheon and Jon Arryn, with whom they had been fostered as children, in declaring war against the ruling Targaryen dynasty, securing the allegiances of House Tully and House Arryn through a network of dynastic marriages (Lord Eddard to Catelyn Tully and Lord Arryn to Lysa Tully). The powerful House Tyrell continued to support the King, but House Lannister and House Martell both stalled due to insults against their houses by the Targaryens. The civil war climaxed with the Battle of the Trident, when Prince Rhaegar was killed in battle by Robert Baratheon. The Lannisters finally agreed to support King Aerys, but then brutally... ")).
///         add(credit);
///
/// ComponentAnimation title = hi.getToolbar().getTitleComponent().createStyleAnimation("Title", 200);
/// hi.getAnimationManager().onTitleScrollAnimation(title);
/// hi.show();
/// ```
///
/// This sample code shows the usage of the nestoria API to fill out an infinitely scrolling list in it
/// we use `URLImage` to fetch the icon.
///
/// ```java
/// public void showForm() {
///     Form hi = new Form("InfiniteScrollAdapter", new BoxLayout(BoxLayout.Y_AXIS));
///
///     Style s = UIManager.getInstance().getComponentStyle("MultiLine1");
///     FontImage p = FontImage.createMaterial(FontImage.MATERIAL_PORTRAIT, s);
///     EncodedImage placeholder = EncodedImage.createFromImage(p.scaled(p.getWidth() * 3, p.getHeight() * 3), false);
///
///     InfiniteScrollAdapter.createInfiniteScroll(hi.getContentPane(), () -> {
///         java.util.List> data = fetchPropertyData("Leeds");
///         MultiButton[] cmps = new MultiButton[data.size()];
///         for(int iter = 0 ; iter  currentListing = data.get(iter);
///             if(currentListing == null) {
///                 InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), new Component[0], false);
///                 return;
///             }
///             String thumb_url = (String)currentListing.get("thumb_url");
///             String guid = (String)currentListing.get("guid");
///             String summary = (String)currentListing.get("summary");
///             cmps[iter] = new MultiButton(summary);
///             cmps[iter].setIcon(URLImage.createToStorage(placeholder, guid, thumb_url));
///         }
///         InfiniteScrollAdapter.addMoreComponents(hi.getContentPane(), cmps, true);
///     }, true);
///     hi.show();
/// }
/// int pageNumber = 1;
/// java.util.List> fetchPropertyData(String text) {
///     try {
///         ConnectionRequest r = new ConnectionRequest();
///         r.setPost(false);
///         r.setUrl("http://api.nestoria.co.uk/api");
///         r.addArgument("pretty", "0");
///         r.addArgument("action", "search_listings");
///         r.addArgument("encoding", "json");
///         r.addArgument("listing_type", "buy");
///         r.addArgument("page", "" + pageNumber);
///         pageNumber++;
///         r.addArgument("country", "uk");
///         r.addArgument("place_name", text);
///         NetworkManager.getInstance().addToQueueAndWait(r);
///         Map result = new JSONParser().parseJSON(new InputStreamReader(new ByteArrayInputStream(r.getResponseData()), "UTF-8"));
///         Map response = (Map)result.get("response");
///         return (java.util.List>)response.get("listings");
///     } catch(Exception err) {
///         Log.e(err);
///         return null;
///     }
/// }
/// ```
///
/// You can use adapters with masks using syntax similar to this to create a round image mask for a `URLImage`:
///
/// ```java
/// Image roundMask = Image.createImage(placeholder.getWidth(), placeholder.getHeight(), 0xff000000);
/// Graphics gr = roundMask.getGraphics();
/// gr.setColor(0xffffff);
/// gr.fillArc(0, 0, placeholder.getWidth(), placeholder.getHeight(), 0, 360);
///
/// URLImage.ImageAdapter ada = URLImage.createMaskAdapter(roundMask);
/// Image i = URLImage.createToStorage(placeholder, "fileNameInStorage", "http://xxx/myurl.jpg", ada);
/// ```
///
/// @author Shai Almog
public final class URLImage extends EncodedImage {

    /// Flag used by `java.lang.String, com.codename1.ui.Image, int)`.
    /// Equivalent to `#RESIZE_FAIL`
    public static final int FLAG_RESIZE_FAIL = 3;
    /// Will fail if the downloaded image has a different size from the placeholder image
    public static final ImageAdapter RESIZE_FAIL = new ImageAdapter() {
        @Override
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if (downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                throw new RuntimeException("Invalid image size");
            }
            return downloadedImage;
        }

        @Override
        public boolean isAsyncAdapter() {
            return false;
        }
    };
    /// Flag used by `java.lang.String, com.codename1.ui.Image, int)`
    /// Equivalent to `#RESIZE_SCALE`.
    public static final int FLAG_RESIZE_SCALE = 1;
    /// Scales the image to match the size of the new image exactly
    public static final ImageAdapter RESIZE_SCALE = new ImageAdapter() {
        @Override
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if (downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                return downloadedImage.scaledEncoded(placeholderImage.getWidth(), placeholderImage.getHeight());
            }
            return downloadedImage;
        }

        @Override
        public boolean isAsyncAdapter() {
            return false;
        }
    };
    /// Flag used by `java.lang.String, com.codename1.ui.Image, int)`.
    /// Equivalent to `#RESIZE_SCALE_TO_FILL`.
    public static final int FLAG_RESIZE_SCALE_TO_FILL = 2;
    /// Scales the image to match to fill the area while preserving aspect ratio
    public static final ImageAdapter RESIZE_SCALE_TO_FILL = new ScaleToFill();
    private static final Map<String, URLImage> pendingToStorage = new HashMap<String, URLImage>();
    private static final Map<String, URLImage> pendingToFile = new HashMap<String, URLImage>();
    private static final String IMAGE_SUFFIX = "ImageURLTMP";
    private static final EasyThread imageLoader = EasyThread.start("ImageLoader");
    /// The exception handler is used for callbacks in case of an error
    private static ErrorCallback exceptionHandler;
    private final EncodedImage placeholder;
    private final String url;
    private final ImageAdapter adapter;
    private final String storageFile;
    private final String fileSystemFile;
    private boolean fetching;
    private byte[] imageData;
    private boolean repaintImage;
    private boolean locked;

    private URLImage(EncodedImage placeholder, String url, ImageAdapter adapter, String storageFile, String fileSystemFile) {
        super(placeholder.getWidth(), placeholder.getHeight());
        this.placeholder = placeholder;
        this.url = url;
        this.adapter = adapter;
        this.storageFile = storageFile;
        this.fileSystemFile = fileSystemFile;
    }

    /// The exception handler is used for callbacks in case of an error
    ///
    /// #### Returns
    ///
    /// the exceptionHandler
    public static ErrorCallback getExceptionHandler() {
        return exceptionHandler;
    }

    /// The exception handler is used for callbacks in case of an error
    ///
    /// #### Parameters
    ///
    /// - `aExceptionHandler`: the exceptionHandler to set
    public static void setExceptionHandler(
            ErrorCallback aExceptionHandler) {
        exceptionHandler = aExceptionHandler;
    }

    /// Creates an adapter that uses an image as a Mask, this is roughly the same as SCALE_TO_FILL with the
    /// exception that a mask will be applied later on. This adapter requires that the resulting image be in the size
    /// of the imageMask!
    ///
    /// See the sample usage code below that implements a circular image masked downloader:
    ///
    /// ```java
    /// Image roundMask = Image.createImage(placeholder.getWidth(), placeholder.getHeight(), 0xff000000);
    /// Graphics gr = roundMask.getGraphics();
    /// gr.setColor(0xffffff);
    /// gr.fillArc(0, 0, placeholder.getWidth(), placeholder.getHeight(), 0, 360);
    ///
    /// URLImage.ImageAdapter ada = URLImage.createMaskAdapter(roundMask);
    /// Image i = URLImage.createToStorage(placeholder, "fileNameInStorage", "http://xxx/myurl.jpg", ada);
    /// ```
    ///
    /// #### Parameters
    ///
    /// - `imageMask`: @param imageMask the mask image see the createMask() method of image for details of what a mask is, it
    ///                  will be used as the reference size for the image and resulting images must be of the same size!
    ///
    /// #### Returns
    ///
    /// the adapter
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

    /// Creates an image the will be downloaded on the fly as necessary with RESIZE_SCALE_TO_FILL as
    /// the default behavior
    ///
    /// #### Parameters
    ///
    /// - `placeholder`: @param placeholder the image placeholder is shown as the image is loading/downloading
    ///                    and serves as the guideline to the size of the downloaded image.
    ///
    /// - `storageFile`: the file in storage to which the image will be stored
    ///
    /// - `url`: the url from which the image is fetched
    ///
    /// #### Returns
    ///
    /// a URLImage that will initialy just delegate to the placeholder
    public static URLImage createToStorage(EncodedImage placeholder, String storageFile, String url) {
        return createToStorage(placeholder, storageFile, url, RESIZE_SCALE_TO_FILL);
    }

    /// Creates an image the will be downloaded on the fly as necessary
    ///
    /// #### Parameters
    ///
    /// - `placeholder`: @param placeholder the image placeholder is shown as the image is loading/downloading
    ///                    and serves as the guideline to the size of the downloaded image.
    ///
    /// - `storageFile`: the file in storage to which the image will be stored
    ///
    /// - `url`: the url from which the image is fetched
    ///
    /// - `adapter`: @param adapter     the adapter used to adapt the image into place, it should scale the image
    ///                    if necessary
    ///
    /// #### Returns
    ///
    /// a URLImage that will initialy just delegate to the placeholder
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

    /// Creates an image the will be downloaded on the fly as necessary
    ///
    /// #### Parameters
    ///
    /// - `placeholder`: @param placeholder the image placeholder is shown as the image is loading/downloading
    ///                    and serves as the guideline to the size of the downloaded image.
    ///
    /// - `file`: the file in the file system to which the image will be stored
    ///
    /// - `url`: the url from which the image is fetched
    ///
    /// - `adapter`: @param adapter     the adapter used to adapt the image into place, it should scale the image
    ///                    if necessary
    ///
    /// #### Returns
    ///
    /// a URLImage that will initialy just delegate to the placeholder
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

    /// Creates an image that will be downloaded on the fly as necessary.  On platforms that support a native
    /// image cache (e.g. Javascript), the image will be loaded directly from the native cache (i.e. it defers to the
    /// platform to handle all caching considerations.  On platforms that don't have a native image cache but
    /// do have a caches directory `FileSystemStorage#hasCachesDir()`, this will call `java.lang.String, java.lang.String, com.codename1.ui.URLImage.ImageAdapter)`
    /// with a file location in the caches directory.  In all other cases, this will call `java.lang.String, java.lang.String)`.
    ///
    /// #### Parameters
    ///
    /// - `imageName`: The name of the image.
    ///
    /// - `url`: the URL from which the image is fetched
    ///
    /// - `placeholder`: @param placeholder the image placeholder is shown as the image is loading/downloading
    ///                    and serves as the guideline to the size of the downloaded image.
    ///
    /// - `resizeRule`: One of `#FLAG_RESIZE_FAIL`, `#FLAG_RESIZE_SCALE`, or `#FLAG_RESIZE_SCALE_TO_FILL`.
    ///
    /// #### Returns
    ///
    /// a Image that will initially just delegate to the placeholder
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
                String name = "cn1_image_cache[" + url + "]";
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
        return CN.isSimulator() || !"ios".equals(CN.getPlatformName());
    }

    private void loadImageFromLocalUrl(final String targetKey, final boolean useFileSystemStorage) {
        imageLoader.run(new Runnable() {
            @Override
            public void run() {
                try {
                    InputStream input = null; //NOPMD CloseResource
                    OutputStream output = null; //NOPMD CloseResource
                    try {
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
                            output = useFileSystemStorage ? FileSystemStorage.getInstance().openOutputStream(targetKey) : Storage.getInstance().createOutputStream(targetKey);
                            Util.copy(input, output);
                        }
                    } finally {
                        Util.cleanup(output);
                        Util.cleanup(input);
                    }
                    runAndWait(new Runnable() {
                        @Override
                        public void run() {
                            InputStream imageInput = null; //NOPMD CloseResource
                            try {
                                Image value = url.startsWith("image:") ? Resources.getGlobalResources().getImage(url) :
                                        Image.createImage(imageInput = useFileSystemStorage ? FileSystemStorage.getInstance().openInputStream(targetKey) : Storage.getInstance().createInputStream(targetKey));
                                DownloadCompleted onComplete = new DownloadCompleted();
                                onComplete.setSourceImage(value);
                                onComplete.actionPerformed(new ActionEvent(value));
                            } catch (IOException ex) {
                                if (exceptionHandler != null) {
                                    exceptionHandler.onError(URLImage.this, ex);
                                } else {
                                    Log.e(new RuntimeException(ex.toString()));
                                }
                            } finally {
                                Util.cleanup(imageInput);
                            }
                        }
                    });
                } catch (IOException t) {
                    if (exceptionHandler != null) {
                        exceptionHandler.onError(URLImage.this, t);
                    } else {
                        Log.e(new RuntimeException(t.toString()));
                    }
                }
            }
        });
    }

    /// Images are normally fetched from storage or network only as needed,
    /// however if the download must start before the image is drawn this method
    /// can be invoked. Notice that "immediately" doesn't mean synchronously, it just
    /// means that the image will be added to the queue right away but probably won't be
    /// available by the time the method completes.
    public void fetch() {
        if (fetching || imageData != null) {
            return;
        }
        fetching = true;
        try {
            locked = super.isLocked();
            if (storageFile != null) {
                if (Storage.getInstance().exists(storageFile)) {
                    super.unlock();
                    imageData = new byte[Storage.getInstance().entrySize(storageFile)];
                    InputStream is = null; //NOPMD CloseResource
                    try {
                        is = Storage.getInstance().createInputStream(storageFile);
                        Util.readFully(is, imageData);
                    } finally {
                        Util.cleanup(is);
                    }
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
                                    @Override
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            @Override
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    @Override
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
                                    @Override
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            @Override
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    @Override
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
                if (FileSystemStorage.getInstance().exists(fileSystemFile)) {
                    super.unlock();
                    imageData = new byte[(int) FileSystemStorage.getInstance().getLength(fileSystemFile)];
                    InputStream is = null; //NOPMD CloseResource
                    try {
                        is = FileSystemStorage.getInstance().openInputStream(fileSystemFile);
                        Util.readFully(is, imageData);
                    } finally {
                        Util.cleanup(is);
                    }
                    resetCache();
                    fetching = false;
                    repaintImage = true;
                    fireChangedEvent();
                    return;
                }
                if (adapter != null) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        // Load image over http
                        Util.downloadImageToFileSystem(url, fileSystemFile + IMAGE_SUFFIX,
                                new SuccessCallback<Image>() {

                                    @Override
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            @Override
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    @Override
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
                        // load image from file system
                        loadImageFromLocalUrl(fileSystemFile + IMAGE_SUFFIX, true);
                    }
                } else {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        Util.downloadImageToFileSystem(url, fileSystemFile,
                                new SuccessCallback<Image>() {

                                    @Override
                                    public void onSucess(final Image value) {
                                        imageLoader.run(new Runnable() {
                                            @Override
                                            public void run() {
                                                runAndWait(new Runnable() {
                                                    @Override
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
        } catch (IOException ioErr) {
            if (exceptionHandler != null) {
                exceptionHandler.onError(this, ioErr);
            } else {
                throw new RuntimeException(ioErr.toString(), ioErr);
            }
        }
    }

    /// {@inheritDoc}
    @Override
    protected Image getInternal() {
        if (imageData == null) {
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

    /// {@inheritDoc}
    @Override
    public byte[] getImageData() {
        if (imageData != null) {
            return imageData;
        }
        return placeholder.getImageData();
    }

    /// {@inheritDoc}
    @Override
    public boolean animate() {
        if (repaintImage) {
            repaintImage = false;
            if (locked) {
                super.lock();
                locked = false;
            }
            return true;
        }
        return false;
    }

    /// Block this method from external callers as it might break the functionality
    @Override
    public void lock() {
    }

    /// Block this method from external callers as it might break the functionality
    @Override
    public void unlock() {
    }

    /// {@inheritDoc}
    @Override
    public boolean isAnimation() {
        return repaintImage || imageData == null;
    }

    /// Invoked in a case of an error
    public interface ErrorCallback {
        void onError(URLImage source, Exception err);
    }

    /// Allows applying resize logic to downloaded images you can use constant
    /// resize behaviors defined in this class. This class allows masking and various
    /// other effects to be applied to downloaded images.
    ///
    /// Notice: adapters happen before the image is saved so they will only happen once
    /// and the image will be saved as "adapted" which can be great for performance but
    /// is also permanent. E.g. If you mask an image it will remain masked.
    public interface ImageAdapter {
        /// Allows the downloaded image to be adapted e.g if it isn't the same size of the placeholder image.
        ///
        /// #### Parameters
        ///
        /// - `downloadedImage`: the downloaded image
        ///
        /// - `placeholderImage`: the placeholder image
        ///
        /// #### Returns
        ///
        /// the adapted image or the same image
        EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage);

        /// Return true if the adapter should work on a separate thread to avoid blocking the EDT
        /// this is especially important for image masks and heavy image manipulation
        ///
        /// #### Returns
        ///
        /// true to run off the EDT
        boolean isAsyncAdapter();
    }

    static class ScaleToFill implements ImageAdapter {
        @Override
        public EncodedImage adaptImage(EncodedImage downloadedImage, EncodedImage placeholderImage) {
            if (downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                Image tmp = downloadedImage.getInternal().scaledLargerRatio(placeholderImage.getWidth(), placeholderImage.getHeight());
                Image i = Image.createImage(placeholderImage.getWidth(), placeholderImage.getHeight(), 0);
                Graphics g = i.getGraphics();
                if (tmp.getWidth() > placeholderImage.getWidth()) {
                    int diff = tmp.getWidth() - placeholderImage.getWidth();
                    int x = diff / 2;
                    g.drawImage(tmp, -x, 0);
                    tmp = i;
                } else {
                    if (tmp.getHeight() > placeholderImage.getHeight()) {
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

        @Override
        public boolean isAsyncAdapter() {
            return false;
        }
    }

    /// CachedImage used by `java.lang.String, com.codename1.ui.Image, int)`
    private static class CachedImage extends Image {
        int resizeRule;
        Object image;
        Image placeholderImage;
        String url;
        boolean repaintImage;

        public CachedImage(Image placeholder, String url, int resize) {
            super(placeholder.getImage());
            this.url = url;
            this.resizeRule = resize;
            this.placeholderImage = placeholder;
            Util.downloadImageToCache(url, new SuccessCallback<Image>() {
                @Override
                public void onSucess(Image downloadedImage) {
                    switch (resizeRule) {
                        case FLAG_RESIZE_FAIL: {
                            if (downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                                throw new RuntimeException("Invalid image size");
                            }
                            break;
                        }
                        case FLAG_RESIZE_SCALE: {
                            downloadedImage = downloadedImage.scaled(placeholderImage.getWidth(), placeholderImage.getHeight());
                            break;
                        }
                        case FLAG_RESIZE_SCALE_TO_FILL: {
                            if (downloadedImage.getWidth() != placeholderImage.getWidth() || downloadedImage.getHeight() != placeholderImage.getHeight()) {
                                Image tmp = downloadedImage.scaledLargerRatio(placeholderImage.getWidth(), placeholderImage.getHeight());
                                Image i = Image.createImage(placeholderImage.getWidth(), placeholderImage.getHeight(), 0);
                                Graphics g = i.getGraphics();
                                g.setAntiAliased(true);
                                if (tmp.getWidth() > placeholderImage.getWidth()) {
                                    int diff = tmp.getWidth() - placeholderImage.getWidth();
                                    int x = diff / 2;
                                    g.drawImage(tmp, -x, 0);
                                    tmp = i;
                                } else {
                                    if (tmp.getHeight() > placeholderImage.getHeight()) {
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
                        default:
                            break;
                    }

                    image = downloadedImage.getImage();
                    repaintImage = true;
                    fireChangedEvent();
                }

            }, new FailureCallback<Image>() {
                @Override
                public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                    throw new RuntimeException("Failed to download image " + CachedImage.this.url + " from cache");
                }
            });
        }

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
    }

    class DownloadCompleted implements ActionListener, Runnable {
        private EncodedImage adapt;
        private EncodedImage adaptedIns;
        private Image sourceImage;

        @Override
        public void run() {
            adaptedIns = adapter.adaptImage(adapt, placeholder);
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (adapter != null) {
                try {
                    EncodedImage img;
                    if (sourceImage == null) {
                        byte[] d;
                        InputStream is; //NOPMD CloseResource
                        if (storageFile != null) {
                            d = new byte[Storage.getInstance().entrySize(storageFile + IMAGE_SUFFIX)];
                            is = Storage.getInstance().createInputStream(storageFile + IMAGE_SUFFIX);
                        } else {
                            d = new byte[(int) FileSystemStorage.getInstance().getLength(fileSystemFile + IMAGE_SUFFIX)];
                            is = FileSystemStorage.getInstance().openInputStream(fileSystemFile + IMAGE_SUFFIX);
                        }
                        try {
                            Util.readFully(is, d);
                        } finally {
                            Util.cleanup(is);
                        }
                        img = EncodedImage.create(d);
                    } else {
                        img = EncodedImage.createFromImage(sourceImage, false);
                    }
                    EncodedImage adapted;
                    if (adapter.isAsyncAdapter()) {
                        adapt = img;
                        Display.getInstance().invokeAndBlock(this);
                        adapted = adaptedIns;
                        adaptedIns = null;
                        adapt = null;
                    } else {
                        try {
                            adapted = adapter.adaptImage(img, placeholder);
                        } catch (Exception err) {
                            if (exceptionHandler != null) {
                                exceptionHandler.onError(URLImage.this, err);
                            } else {
                                Log.p("Failed to load image from URL: " + url);
                                Log.e(err);
                            }
                            return;
                        }
                    }
                    if (storageFile != null) {
                        OutputStream o = null; //NOPMD CloseResource
                        try {
                            o = Storage.getInstance().createOutputStream(storageFile);
                            o.write(adapted.getImageData());
                        } finally {
                            Util.cleanup(o);
                        }
                        Storage.getInstance().deleteStorageFile(storageFile + IMAGE_SUFFIX);
                        pendingToStorage.remove(storageFile);
                    } else if (fileSystemFile != null) {
                        OutputStream o = null; //NOPMD CloseResource
                        try {
                            o = FileSystemStorage.getInstance().openOutputStream(fileSystemFile);
                            o.write(adapted.getImageData());
                        } finally {
                            Util.cleanup(o);
                        }
                        FileSystemStorage.getInstance().delete(fileSystemFile + IMAGE_SUFFIX);
                        pendingToFile.remove(fileSystemFile);
                    }
                } catch (IOException ex) {
                    if (exceptionHandler != null) {
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

        /// Used in cases where the source image is already downloaded ( so we don't need to try to load it from storage/file system.
        ///
        /// #### Parameters
        ///
        /// - `sourceImage`
        void setSourceImage(Image sourceImage) {
            this.sourceImage = sourceImage;
        }
    }
}
