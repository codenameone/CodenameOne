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
import com.codename1.camera.CameraFacing;
import com.codename1.camera.CameraInfo;
import com.codename1.camera.CameraSession;
import com.codename1.camera.CameraSessionOptions;
import com.codename1.camera.CapturedPhoto;
import com.codename1.camera.VideoRecording;
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

    /// Whether a capture device exists. The low level API answers this from
    /// AVFoundation, which is the same question the Capture API is asking.
    static boolean hasCamera() {
        try {
            return Camera.isSupported() && Camera.getDefault(CameraFacing.BACK) != null;
        } catch (Throwable t) {
            // Never let a capability probe take the application down: an
            // application that asks whether a camera exists is by definition
            // ready to be told no.
            Log.e(t);
            return false;
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
    /// Permission first, and asynchronously, through the system rather than
    /// through Camera.requestPermissions().
    ///
    /// That method probes by opening a session with a sentinel id, and this
    /// port's back end returns success for the sentinel without asking anyone --
    /// so it answered "granted" while access was undetermined or refused, and
    /// the capture went ahead to a preview that stayed black. Video also asks
    /// for the microphone, because a recording made without it is a silent movie
    /// reported as a success.
    private static void start(final ActionListener response, final boolean video) {
        com.codename1.impl.ios.IOSImplementation.requestCameraAccess(video,
                new SuccessCallback<Boolean>() {
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
        CameraInfo info = Camera.getDefault(CameraFacing.BACK);
        if (info == null) {
            respond(response, null);
            return;
        }
        final CameraSession session;
        try {
            // Through Camera.open rather than the CameraImpl SPI directly, and
            // deliberately: it is what enforces one session at a time. Opening
            // the back end behind it replaced the active callback target, so an
            // application that already had a low level session running had it
            // silently stop receiving frames -- still open, still believed
            // working -- and closing this one cleared the target rather than
            // giving it back.
            //
            // An application already holding a session gets a refusal here. That
            // is the same answer Camera.open gives, and a modal capture that
            // declines is better than one that breaks the session it interrupted.
            session = Camera.open(info, new CameraSessionOptions().captureAudio(video));
        } catch (Throwable t) {
            Log.e(t);
            respond(response, null);
            return;
        }

        final Form previous = Display.getInstance().getCurrent();
        final Form capture = new Form(video ? "Record Video" : "Take Photo", new BorderLayout());
        capture.add(BorderLayout.CENTER, session.createView());

        final Button shutter = new Button(video ? "Record" : "Capture");
        final Button cancel = new Button("Cancel");
        Container buttons = new Container(new FlowLayout(com.codename1.ui.Component.CENTER));
        buttons.add(cancel);
        buttons.add(shutter);
        capture.add(BorderLayout.SOUTH, buttons);

        // One flag for both endings. The user can cancel while a photo is still
        // being written, and both paths close the session and show the previous
        // form -- doing either twice is a closed session being closed again and a
        // form being restored over itself.
        final boolean[] finished = {false};

        // Declared before Cancel so that cancelling mid-recording can reach it.
        // Closing the session stops the capture but does not finish the file the
        // recorder had already started writing, so a cancelled recording left a
        // part-written recording in the application home for ever -- and the
        // callback said "cancelled", so nothing downstream knew to clean it up.
        final VideoRecording[] recording = {null};

        // Whether Stop has already begun finalizing, and whether Cancel was
        // pressed while it was. They exist because VideoRecording.stopAndAwait()
        // is only an await the FIRST time: a recording already marked stopped
        // completes the resource immediately with the requested path rather
        // than waiting for anything. So once Stop has started finalization,
        // Cancel calling discard() would not await at all -- it would delete a
        // file the recorder is still writing and then close the session, which
        // removes movieOutput and releases the camera while the native
        // didFinishRecordingToOutputFileAtURL: callback is still pending. That
        // is exactly the failure the Cancel path below was written to avoid,
        // reached by the one route where the await it relies on is a no-op.
        final boolean[] stopPending = {false};
        final boolean[] cancelRequested = {false};

        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (recording[0] == null) {
                    finish(finished, session, previous, response, null);
                    return;
                }
                if (stopPending[0]) {
                    // Finalization is already in flight and owns the only
                    // resource that will ever report the final path. Record the
                    // intent and let that callback do the discarding, rather
                    // than starting a second stop that resolves instantly and
                    // tears the session down underneath the first one.
                    cancelRequested[0] = true;
                    cancel.setEnabled(false);
                    return;
                }
                // The session stays open until the recorder has finished with
                // it. stopAndAwait() completes through the native
                // didFinishRecordingToOutputFileAtURL: callback, and closing
                // the session removes movieOutput and releases the camera --
                // so closing first meant the completion never arrived: the
                // part-written file was never deleted and its callback entry
                // was never released. Cancelling twice is already impossible;
                // finish() has its own guard.
                discard(recording[0], new Runnable() {
                    @Override
                    public void run() {
                        finish(finished, session, previous, response, null);
                    }
                });
            }
        });

        if (video) {
            shutter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (recording[0] == null) {
                        // mov, because that is what the bytes are. The backend
                        // records through AVCaptureMovieFileOutput, which writes
                        // a QuickTime container and honours whatever extension
                        // it is handed -- its own comment says so. Asking for
                        // mp4 therefore produced QuickTime bytes under a name
                        // that misdescribed them, and anything deriving a MIME
                        // type, a decoder or upload metadata from the extension
                        // this API documents itself as returning got the wrong
                        // answer.
                        String path = tempFile("mov");
                        if (path == null) {
                            finish(finished, session, previous, response, null);
                            return;
                        }
                        try {
                            recording[0] = session.startVideoRecording(path);
                        } catch (Throwable t) {
                            Log.e(t);
                            finish(finished, session, previous, response, null);
                            return;
                        }
                        shutter.setText("Stop");
                        capture.revalidate();
                        return;
                    }
                    // Disabled the moment finalization starts, exactly as the
                    // photo branch below does before its own await. A second
                    // Stop click ran stopAndAwait() against a recording that had
                    // already stopped; it resolved immediately with the same
                    // path, finish() reported success and set finished[0], and
                    // the FIRST stop's callback then took the finished[0] branch
                    // and deleted the file it had just finalized. The
                    // application was left holding a path to nothing, and the
                    // capture had reported success.
                    shutter.setEnabled(false);
                    stopPending[0] = true;
                    // The best guess at what was left on disk if finalization
                    // fails, since no final path will then be reported. It is
                    // what discard() falls back to for the same reason.
                    final String requestedPath = recording[0].getRequestedPath();
                    recording[0].stopAndAwait().ready(new SuccessCallback<String>() {
                        @Override
                        public void onSucess(String path) {
                            if (cancelRequested[0]) {
                                // Cancelled while this stop was finalizing. The
                                // file is finished now, so it can be removed --
                                // and only now is it safe to close the session.
                                deleteQuietly(path);
                                finish(finished, session, previous, response, null);
                                return;
                            }
                            if (finished[0]) {
                                // Cancelled while this stop was still pending.
                                // finish() would ignore the result, and the file
                                // it names has just been finalized, so it is the
                                // one nothing will ever claim.
                                deleteQuietly(path);
                                return;
                            }
                            finish(finished, session, previous, response, path);
                        }
                    }).except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            // Finalization failed, so no path will ever be
                            // reported and the capture answers null. Whatever
                            // the recorder had written is therefore a file
                            // nobody will be told about -- the same position
                            // discard() takes on its own error path, and it
                            // holds however this stop was reached, cancelled or
                            // not. Without this the one route that skips
                            // discard() -- cancelling while a stop is already
                            // finalizing -- was also the one route that leaked
                            // the partial file.
                            Log.e(err);
                            deleteQuietly(requestedPath);
                            finish(finished, session, previous, response, null);
                        }
                    });
                }
            });
        } else {
            shutter.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    shutter.setEnabled(false);
                    session.takePhoto().ready(new SuccessCallback<CapturedPhoto>() {
                        @Override
                        public void onSucess(CapturedPhoto photo) {
                            if (finished[0]) {
                                // Cancelled while the capture was still in
                                // flight. finish() would ignore this, but
                                // store() runs first and would write the photo
                                // out anyway -- and the back end may already have
                                // written its own file, which nothing else will
                                // ever claim because the application was told the
                                // capture was cancelled.
                                deleteQuietly(photo == null ? null : photo.getFilePath());
                                return;
                            }
                            finish(finished, session, previous, response, store(photo));
                        }
                    }).except(new SuccessCallback<Throwable>() {
                        @Override
                        public void onSucess(Throwable err) {
                            Log.e(err);
                            finish(finished, session, previous, response, null);
                        }
                    });
                }
            });
        }

        capture.show();
    }

    /// Stops a recording the user is abandoning and removes what it wrote, once
    /// there is something finished to remove.
    ///
    /// Deliberately asynchronous. stop() is documented as fire and forget and
    /// AVFoundation finishes the file later, from
    /// didFinishRecordingToOutputFileAtURL: -- so deleting straight after
    /// stopping raced the writer, and the delegate could recreate or finish
    /// writing the file that had just been removed. stopAndAwait resolves when
    /// the file is closed, which is the first moment deleting it means anything.
    ///
    /// The cancellation itself is NOT made to wait for this: the user pressed
    /// Cancel and the form closes now. Only the cleanup is deferred, which is
    /// why it takes the path the recorder finally reports rather than the one it
    /// was asked for -- they are usually the same file, and when they are not,
    /// the reported one is the file that exists.
    /// Stops and deletes a cancelled recording, then runs `andThen`.
    ///
    /// The continuation is what keeps the camera session alive long enough:
    /// the stop only completes through the native recorder's own callback, and
    /// closing the session first removes the output it would have come from.
    /// Run on the EDT, because the caller uses it to show a Form.
    /// Runs the continuation on the event dispatch thread.
    ///
    /// The stop completion arrives from the native recorder's own thread, and
    /// what the caller does with it shows a Form.
    private static void continueOnEdt(final Runnable andThen) {
        if (andThen == null) {
            return;
        }
        if (Display.getInstance().isEdt()) {
            andThen.run();
        } else {
            Display.getInstance().callSerially(andThen);
        }
    }

    private static void discard(VideoRecording recording, final Runnable andThen) {
        if (recording == null) {
            continueOnEdt(andThen);
            return;
        }
        final String requested = recording.getRequestedPath();
        try {
            recording.stopAndAwait().ready(new SuccessCallback<String>() {
                @Override
                public void onSucess(String path) {
                    deleteQuietly(path != null && path.length() > 0 ? path : requested);
                    continueOnEdt(andThen);
                }
            }).except(new SuccessCallback<Throwable>() {
                @Override
                public void onSucess(Throwable err) {
                    // The stop failed, so nothing will report a final path. The
                    // requested one is the best guess at what was left behind.
                    Log.e(err);
                    deleteQuietly(requested);
                    continueOnEdt(andThen);
                }
            });
        } catch (Throwable t) {
            Log.e(t);
            deleteQuietly(requested);
            continueOnEdt(andThen);
        }
    }

    /// Removes a file nobody will ever be told about, ignoring the failure to.
    private static void deleteQuietly(String path) {
        if (path == null || path.length() == 0) {
            return;
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
            // Closed HERE rather than only in the finally, and its failure is a
            // failure of the write. A stream buffers, so the error that matters
            // -- the disk filling as the last block is flushed -- surfaces from
            // close() and nowhere else. Logging it and returning the path anyway
            // handed the application a truncated JPEG reported as a success.
            out.close();
            out = null;
        } catch (Throwable t) {
            Log.e(t);
            deleteQuietly(path);
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

    private static void finish(boolean[] finished, CameraSession session, Form previous,
            ActionListener response, String path) {
        if (finished[0]) {
            return;
        }
        finished[0] = true;
        try {
            session.close();
        } catch (Throwable t) {
            Log.e(t);
        }
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
