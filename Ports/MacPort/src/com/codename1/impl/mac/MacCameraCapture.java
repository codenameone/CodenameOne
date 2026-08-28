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
package com.codename1.impl.mac;

import com.codename1.camera.Camera;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.PhotoCaptureOptions;
import com.codename1.impl.CameraImpl;
import com.codename1.util.AsyncResource;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.util.SuccessCallback;
import java.io.OutputStream;

/// The modal Capture API, built on the portable camera session rather than on a
/// native picker.
///
/// macOS has no UIImagePickerController, and that is what the inherited
/// implementation drives -- which is why this port used to answer "no camera"
/// rather than take a photo. It does not need one: com.codename1.camera is the
/// same AVFoundation bridge on both platforms, CameraView is an ordinary
/// Component, and "show a preview with a shutter button" is a Form. So the
/// modal API is served here by driving the low level one, in portable code.
///
/// Nothing here is macOS specific beyond living in this port. It is written as
/// its own class so that a desktop port needing the same thing can lift it
/// rather than reimplement it.
final class MacCameraCapture {

    private MacCameraCapture() {
    }

    /// The camera back end, or null when this build has none.
    ///
    /// Reached through the implementation's own SPI rather than through the
    /// com.codename1.camera facade, and that is not a style choice. The build
    /// decides whether an application uses the camera by scanning for calls to
    /// Camera.open()/getDefault()/getCameras(); if this class made those calls,
    /// every macOS application would look like a camera application and would
    /// ship a camera entitlement and a camera privacy string it never uses.
    /// Going through the SPI keeps the port out of the evidence.
    private static CameraImpl backend() {
        try {
            return Display.getInstance().getCameraBackend();
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    /// Whether a capture device exists, answered by AVFoundation.
    static boolean hasCamera() {
        CameraImpl impl = backend();
        if (impl == null) {
            return false;
        }
        try {
            CameraInfo[] cameras = impl.enumerateCameras();
            return cameras != null && cameras.length > 0;
        } catch (Throwable t) {
            // Never let a capability probe take the application down: an
            // application that asks whether a camera exists is by definition
            // ready to be told no.
            Log.e(t);
            return false;
        } finally {
            try {
                impl.close();
            } catch (Throwable t) {
                Log.e(t);
            }
        }
    }

    static void capturePhoto(ActionListener response) {
        start(response, false);
    }

    static void captureVideo(ActionListener response) {
        start(response, true);
    }

    /// Opens the camera and shows the capture form.
    ///
    /// Permission first, and asynchronously: on macOS the first use of a capture
    /// device raises the system prompt, and opening a session before the answer
    /// arrives gives a session with no device rather than an error worth
    /// reporting.
    private static void start(final ActionListener response, final boolean video) {
        Camera.requestPermissions(video, new SuccessCallback<Boolean>() {
            @Override
            public void onSucess(Boolean granted) {
                if (granted == null || !granted.booleanValue()) {
                    respond(response, null);
                    return;
                }
                open(response, video);
            }
        });
    }

    private static void open(final ActionListener response, final boolean video) {
        final CameraImpl impl = backend();
        if (impl == null) {
            respond(response, null);
            return;
        }
        CameraInfo[] cameras;
        try {
            cameras = impl.enumerateCameras();
        } catch (Throwable t) {
            Log.e(t);
            closeQuietly(impl);
            respond(response, null);
            return;
        }
        if (cameras == null || cameras.length == 0) {
            closeQuietly(impl);
            respond(response, null);
            return;
        }
        try {
            impl.open(cameras[0].getId(), new CameraSessionOptions().captureAudio(video));
        } catch (Throwable t) {
            Log.e(t);
            closeQuietly(impl);
            respond(response, null);
            return;
        }

        final Form previous = Display.getInstance().getCurrent();
        final Form capture = new Form(video ? "Record Video" : "Take Photo", new BorderLayout());
        capture.add(BorderLayout.CENTER, impl.createPreviewPeer());

        final Button shutter = new Button(video ? "Record" : "Capture");
        Button cancel = new Button("Cancel");
        Container buttons = new Container(new FlowLayout(com.codename1.ui.Component.CENTER));
        buttons.add(cancel);
        buttons.add(shutter);
        capture.add(BorderLayout.SOUTH, buttons);

        // One flag for both endings. The user can cancel while a photo is still
        // being written, and both paths close the back end and show the previous
        // form -- doing either twice is a closed session being closed again and a
        // form being restored over itself.
        final boolean[] finished = {false};
        // The path being recorded to, so cancelling can remove it. Closing the
        // back end stops the capture but does not finish that file, and the
        // callback reports a cancellation, so nothing downstream is ever told a
        // path to clean up.
        final String[] recordingPath = {null};

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                discard(impl, recordingPath[0]);
                finish(finished, impl, previous, response, null);
            }
        });

        if (video) {
            shutter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (recordingPath[0] == null) {
                        String path = tempFile("mp4");
                        if (path == null) {
                            finish(finished, impl, previous, response, null);
                            return;
                        }
                        try {
                            impl.startVideoRecording(path, true);
                        } catch (Throwable t) {
                            Log.e(t);
                            finish(finished, impl, previous, response, null);
                            return;
                        }
                        recordingPath[0] = path;
                        shutter.setText("Stop");
                        capture.revalidate();
                        return;
                    }
                    AsyncResource<String> done = new AsyncResource<String>();
                    done.ready(new SuccessCallback<String>() {
                        @Override
                        public void onSucess(String path) {
                            finish(finished, impl, previous, response, path);
                        }
                    }).except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            Log.e(err);
                            finish(finished, impl, previous, response, null);
                        }
                    });
                    impl.stopVideoRecording(done);
                }
            });
        } else {
            shutter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    shutter.setEnabled(false);
                    AsyncResource<CapturedPhoto> shot = new AsyncResource<CapturedPhoto>();
                    shot.ready(new SuccessCallback<CapturedPhoto>() {
                        @Override
                        public void onSucess(CapturedPhoto photo) {
                            finish(finished, impl, previous, response, store(photo));
                        }
                    }).except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            Log.e(err);
                            finish(finished, impl, previous, response, null);
                        }
                    });
                    impl.takePhoto(new PhotoCaptureOptions(), shot);
                }
            });
        }

        capture.show();
    }

    private static void closeQuietly(CameraImpl impl) {
        try {
            impl.close();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// Stops a recording the user is abandoning and removes what it wrote.
    ///
    /// Closing the back end stops the capture, but the recorder has already been
    /// writing to the destination and nothing else will ever finish or claim
    /// that file: the callback reports a cancellation, so no application is told
    /// a path to clean up. Stopped first so the file is closed before it is
    /// deleted -- removing one still being written is how a half-flushed file
    /// survives on some filesystems.
    private static void discard(CameraImpl impl, String path) {
        if (path == null) {
            return;
        }
        try {
            impl.stopVideoRecording(new AsyncResource<String>());
        } catch (Throwable t) {
            Log.e(t);
        }
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            if (fs.exists(path)) {
                fs.delete(path);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    /// The photo the low level API produced, as a file, because the Capture API
    /// hands the application a path rather than bytes.
    ///
    /// getFilePath() first: a port that already wrote the photo to disk has
    /// nothing to gain from a second copy, and the JPEG bytes may not be held in
    /// memory at all.
    private static String store(CapturedPhoto photo) {
        if (photo == null) {
            return null;
        }
        String existing = photo.getFilePath();
        if (existing != null && existing.length() > 0) {
            return existing;
        }
        byte[] jpeg = photo.getJpegBytes();
        if (jpeg == null || jpeg.length == 0) {
            return null;
        }
        String path = tempFile("jpg");
        if (path == null) {
            return null;
        }
        OutputStream out = null;
        try {
            out = FileSystemStorage.getInstance().openOutputStream(path);
            out.write(jpeg);
        } catch (Throwable t) {
            Log.e(t);
            return null;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Throwable t) {
                    Log.e(t);
                }
            }
        }
        return path;
    }

    /// A path under the application home, named so two captures in the same
    /// session cannot land on the same file.
    private static String tempFile(String extension) {
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String home = fs.getAppHomePath();
            if (!home.endsWith(fs.getFileSystemSeparator() + "")) {
                home += fs.getFileSystemSeparator();
            }
            return home + "cn1-capture-" + System.currentTimeMillis() + "." + extension;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    private static void finish(boolean[] finished, CameraImpl impl, Form previous,
            ActionListener response, String path) {
        if (finished[0]) {
            return;
        }
        finished[0] = true;
        closeQuietly(impl);
        if (previous != null) {
            previous.showBack();
        }
        respond(response, path);
    }

    /// The Capture API contract: the listener is invoked with the path, or with
    /// a null source when the user cancelled or nothing could be captured.
    private static void respond(final ActionListener response, final String path) {
        if (response == null) {
            return;
        }
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                response.actionPerformed(new ActionEvent(path));
            }
        });
    }
}
