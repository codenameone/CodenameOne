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
package com.codename1.capture;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.media.MediaRecorderBuilder;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.ImageIO;

import java.io.IOException;
import java.io.OutputStream;

/// `Capture` can "capture" media files from the device e.g. record audio, video and snap photos.
/// Notice that files returned by this class are potentially temporary files and might be
/// deleted by the OS in the future.
///
/// The code below demonstrates the capturing of a photo thru this API:
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
/// The code below demonstrates capturing and playing back audio files using this API:
///
/// ```java
/// Form hi = new Form("Capture", BoxLayout.y());
/// hi.setToolbar(new Toolbar());
/// Style s = UIManager.getInstance().getComponentStyle("Title");
/// FontImage icon = FontImage.createMaterial(FontImage.MATERIAL_MIC, s);
///
/// FileSystemStorage fs = FileSystemStorage.getInstance();
/// String recordingsDir = fs.getAppHomePath() + "recordings/";
/// fs.mkdir(recordingsDir);
/// try {
///     for(String file : fs.listFiles(recordingsDir)) {
///         MultiButton mb = new MultiButton(file.substring(file.lastIndexOf("/") + 1));
///         mb.addActionListener((e) -> {
///             try {
///                 Media m = MediaManager.createMedia(recordingsDir + file, false);
///                 m.play();
///             } catch(IOException err) {
///                 Log.e(err);
///             }
///         });
///         hi.add(mb);
///     }
///
///     hi.getToolbar().addCommandToRightBar("", icon, (ev) -> {
///         try {
///             String file = Capture.captureAudio();
///             if(file != null) {
///                 SimpleDateFormat sd = new SimpleDateFormat("yyyy-MMM-dd-kk-mm");
///                 String fileName =sd.format(new Date());
///                 String filePath = recordingsDir + fileName;
///                 Util.copy(fs.openInputStream(file), fs.openOutputStream(filePath));
///                 MultiButton mb = new MultiButton(fileName);
///                 mb.addActionListener((e) -> {
///                     try {
///                         Media m = MediaManager.createMedia(filePath, false);
///                         m.play();
///                     } catch(IOException err) {
///                         Log.e(err);
///                     }
///                 });
///                 hi.add(mb);
///                 hi.revalidate();
///             }
///         } catch(IOException err) {
///             Log.e(err);
///         }
///     });
/// } catch(IOException err) {
///     Log.e(err);
/// }
/// hi.show();
/// ```
///
/// @author Chen
public abstract class Capture {

    /// Returns true if the device has camera false otherwise.
    ///
    /// #### Returns
    ///
    /// true if the device has a camera
    public static boolean hasCamera() {
        return Display.getInstance().hasCamera();
    }

    /// This method tries to invoke the device native camera to capture images.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The image is saved as a jpeg to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// if evt returns null the image capture was canceled by the user.
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    public static void capturePhoto(ActionListener<ActionEvent> response) {
        Display.getInstance().capturePhoto(response);
    }

    /// Invokes the camera and takes a photo synchronously while blocking the EDT
    ///
    /// #### Returns
    ///
    /// the photo file location or null if the user canceled
    public static String capturePhoto() {
        return capturePhoto(-1, -1);
    }

    /// Capture the audio, blocking version that holds the EDT; alternatively you can use the Media API.
    ///
    /// #### Returns
    ///
    /// the audio file location or null if the user canceled
    public static String captureAudio() {
        CallBack c = new CallBack();
        captureAudio(c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }

    /// Capture the audio, blocking version that holds the EDT; alternatively you can use the Media API.
    ///
    /// #### Returns
    ///
    /// the audio file location or null if the user canceled
    ///
    /// #### Since
    ///
    /// 7.0
    public static String captureAudio(MediaRecorderBuilder recordingOptions) {
        CallBack c = new CallBack();
        captureAudio(recordingOptions, c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }


    /// Same as captureVideo only a blocking version that holds the EDT
    ///
    /// #### Returns
    ///
    /// the photo file location or null if the user canceled
    public static String captureVideo() {
        CallBack c = new CallBack();
        captureVideo(c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }

    /// Same as `com.codename1.ui.events.ActionListener)` only a
    /// blocking version that holds the EDT.
    ///
    /// #### Parameters
    ///
    /// - `constraints`
    ///
    /// #### Returns
    ///
    /// A video file location or null if the user canceled.
    ///
    /// #### Since
    ///
    /// 7.0
    public static String captureVideo(VideoCaptureConstraints constraints) {
        CallBack c = new CallBack();
        captureVideo(constraints, c);
        Display.getInstance().invokeAndBlock(c);
        return c.url;
    }

    /// Invokes the camera and takes a photo synchronously while blocking the EDT, the sample below
    /// demonstrates a simple usage and applying a mask to the result
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
    /// - `width`: the target width for the image if possible, some platforms don't support scaling. To maintain aspect ratio set to -1
    ///
    /// - `height`: the target height for the image if possible, some platforms don't support scaling. To maintain aspect ratio set to -1
    ///
    /// #### Returns
    ///
    /// the photo file location or null if the user canceled
    public static String capturePhoto(int width, int height) {
        CallBack c = new CallBack();
        if ("ios".equals(Display.getInstance().getPlatformName()) && (width != -1 || height != -1)) {
            // workaround for threading issues in iOS https://github.com/codenameone/CodenameOne/issues/2246
            capturePhoto(c);
            Display.getInstance().invokeAndBlock(c);
            if (c.url == null) {
                return null;
            }
            ImageIO scale = Display.getInstance().getImageIO();
            if (scale != null) {
                OutputStream os = null; //NOPMD CloseResource
                try {
                    String path = c.url.substring(0, c.url.indexOf(".")) + "s" + c.url.substring(c.url.indexOf("."));
                    os = FileSystemStorage.getInstance().openOutputStream(path);
                    scale.save(c.url, os, ImageIO.FORMAT_JPEG, width, height, 1);
                    FileSystemStorage.getInstance().delete(c.url);
                    return path;
                } catch (IOException ex) {
                    Log.e(ex);
                } finally {
                    Util.cleanup(os);
                }
            }
        } else {
            c.targetWidth = width;
            c.targetHeight = height;
            capturePhoto(c);
            Display.getInstance().invokeAndBlock(c);
        }
        return c.url;
    }

    /// This method tries to invoke the device native hardware to capture audio.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The audio is saved to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    public static void captureAudio(ActionListener<ActionEvent> response) {
        Display.getInstance().captureAudio(response);
    }

    /// This method tries to invoke the device native hardware to capture audio.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The audio record settings are specified in the recorderOptions parameter.
    ///
    /// use this in the actionPerformed to retrieve the file path.
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    ///
    /// #### Since
    ///
    /// 7.0
    public static void captureAudio(MediaRecorderBuilder recorderOptions, ActionListener<ActionEvent> response) {
        Display.getInstance().captureAudio(recorderOptions, response);
    }

    /// Captures video with some constraints, like width, height, and max length.  Video constraints
    /// may not be supported on all platforms.  Use `VideoCaptureConstraints#isSupported()` and `VideoCaptureConstraints#isSizeSupported()`
    /// to check whether constraints are supported on the current platform.  If constraints are not supported, then, in the worst case, this will fall
    /// back to just use `#captureVideo(com.codename1.ui.events.ActionListener)`, i.e. capture with no constraints.
    ///
    /// #### Parameters
    ///
    /// - `constraints`: The constraints to use for the video capture.
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Since
    ///
    /// 7.0
    public static void captureVideo(VideoCaptureConstraints constraints, ActionListener<ActionEvent> response) {
        Display.getInstance().captureVideo(constraints, response);
    }

    /// This method tries to invoke the device native camera to capture video.
    /// The method returns immediately and the response will be sent asynchronously
    /// to the given ActionListener Object
    /// The video is saved to a file on the device.
    ///
    /// use this in the actionPerformed to retrieve the file path
    /// String path = (String) evt.getSource();
    ///
    /// #### Parameters
    ///
    /// - `response`: a callback Object to retrieve the file path
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: if this feature failed or unsupported on the platform
    ///
    /// #### See also
    ///
    /// - #captureVideo(com.codename1.capture.VideoCaptureConstraints, com.codename1.ui.events.ActionListener)
    public static void captureVideo(ActionListener<ActionEvent> response) {
        Display.getInstance().captureVideo(response);
    }

    static class CallBack implements ActionListener<ActionEvent>, Runnable {
        String url;
        // We need volatile due to the usage of double locking optimization
        @SuppressWarnings("PMD.AvoidUsingVolatile")
        private volatile boolean completed;
        private int targetWidth = -1;
        private int targetHeight = -1;

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt == null) {
                url = null;
            } else {
                url = (String) evt.getSource();
            }
            synchronized (this) {
                completed = true;
                this.notifyAll();
            }
        }

        @Override
        public void run() {
            while (!completed) {
                synchronized (this) {
                    try {
                        // we need to recheck the condition within the synchronized block (double locking)
                        if (!completed) {
                            this.wait();
                        }
                    } catch (InterruptedException ex) {
                    }
                }
            }
            if (url == null) {
                return;
            }
            if (targetWidth > 0 || targetHeight > 0) {
                ImageIO scale = Display.getInstance().getImageIO();
                if (scale != null) {
                    OutputStream os = null; //NOPMD CloseResource
                    try {

                        String path = url.substring(0, url.lastIndexOf(".")) + "s" + url.substring(url.lastIndexOf("."));
                        os = FileSystemStorage.getInstance().openOutputStream(path);
                        scale.save(url, os, ImageIO.FORMAT_JPEG, targetWidth, targetHeight, 1);
                        FileSystemStorage.getInstance().delete(url);
                        url = path;
                    } catch (IOException ex) {
                        Log.e(ex);
                    } finally {
                        Util.cleanup(os);
                    }
                }
            }
        }

    }
}
