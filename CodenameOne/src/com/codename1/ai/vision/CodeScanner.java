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
package com.codename1.ai.vision;

import com.codename1.camera.Camera;
import com.codename1.camera.CameraInfo;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.components.SpanLabel;
import com.codename1.util.AsyncResource;

/// A ready-made full-screen barcode and QR scanner.
///
/// This is the one-call entry point that {@link BarcodeScanner} deliberately
/// is not. It shows a camera form, decodes codes from the live frames, returns
/// the first accepted one, and restores the form the application was on. Use
/// {@link BarcodeScanner} directly when scanning a still image or when the
/// scanner has to look like the rest of a custom camera screen, and
/// {@link VisionCameraView} when the preview belongs inside a form of your own.
///
/// ```java
/// if (!CodeScanner.isSupported()) {
///     ToastBar.showErrorMessage("This device cannot scan codes");
///     return;
/// }
/// CodeScanner.scan().ready(code -> {
///     if (code == null) {
///         return;                       // the user pressed back
///     }
///     urlField.setText(code.getValue());
/// }).except(error -> Log.e(error));
/// ```
///
/// Restrict the symbologies, and reword the screen, through
/// {@link CodeScannerOptions}:
///
/// ```java
/// CodeScanner.scan(new CodeScannerOptions()
///         .title("Boarding pass")
///         .hint("Hold the pass flat inside the frame")
///         .formats(BarcodeFormat.PDF417, BarcodeFormat.AZTEC))
///     .ready(code -> {
///         if (code != null) {
///             checkIn(code.getValue());
///         }
///     });
/// ```
///
/// The result completes with {@code null} when the user backs out, which is
/// how the old `CodeScanner` cn1lib's `scanCanceled()` maps onto an
/// {@link AsyncResource}. A failure to open the camera or run the decoder
/// arrives through {@link AsyncResource#except} instead, and closes the
/// scanner form.
///
/// Referencing this class is what tells the build pipeline to package barcode
/// scanning and the camera, exactly as referencing {@link BarcodeScanner}
/// does. It adds no other vision model.
///
/// **Import the right one.** Two older classes share this simple name and this
/// class replaces both: the {@code cn1-codescan} cn1lib's
/// {@code com.codename1.ext.codescan.CodeScanner}, and the long-deprecated
/// {@link com.codename1.codescan.CodeScanner} in core, whose
/// {@code getInstance()} returns {@code null} on current targets. Make sure
/// the import reads:
///
/// ```java
/// import com.codename1.ai.vision.CodeScanner;
/// ```
///
/// Migrating from either is mechanical. The cn1lib's three-method
/// {@code ScanResult} callback becomes one asynchronous result:
/// {@code scanCompleted} is {@link AsyncResource#ready}, {@code scanCanceled}
/// is a {@code null} value, and {@code scanError} is
/// {@link AsyncResource#except}.
public final class CodeScanner {
    private CodeScanner() {
    }

    /// Whether this device can run the scanner. This is the check to make
    /// before offering a scan button: it covers both the camera and the
    /// barcode backend, either of which a target may lack.
    ///
    /// @return {@code true} when {@link #scan()} can open a working scanner
    public static boolean isSupported() {
        if (!Camera.isSupported()) {
            return false;
        }
        BarcodeScanner scanner = new BarcodeScanner();
        try {
            return scanner.isSupported();
        } finally {
            scanner.close();
        }
    }

    /// Shows the scanner with the default wording, accepting every symbology
    /// the platform decodes.
    ///
    /// @return the first decoded code, or {@code null} if the user backed out
    public static AsyncResource<Barcode> scan() {
        return scan(null);
    }

    /// Shows the scanner configured by {@code options}.
    ///
    /// @param options the scanner configuration, or {@code null} for the
    ///        defaults
    /// @return the first accepted code, or {@code null} if the user backed out
    public static AsyncResource<Barcode> scan(CodeScannerOptions options) {
        return new Session(options == null
                ? new CodeScannerOptions() : options).show();
    }

    /// One showing of the scanner form. Holds the state that the camera view,
    /// the back command, and the result callback all have to agree on, so that
    /// a code arriving in the same frame the user pressed back cannot deliver
    /// two results or restore the previous form twice.
    private static final class Session
            implements VisionPipelineListener<Barcode[]> {
        private final CodeScannerOptions options;
        private final AsyncResource<Barcode> result =
                new AsyncResource<Barcode>();
        private final String[] formats;
        private final VisionCameraView<Barcode[]> view;
        private Form previous;
        private Form scannerForm;
        private boolean finished;

        Session(CodeScannerOptions options) {
            this.options = options;
            this.formats = options.getFormats();
            this.view = new VisionCameraView<Barcode[]>(
                    new BarcodeScanner(options.getVisionOptions()));
            this.view.setFacing(options.getFacing());
        }

        AsyncResource<Barcode> show() {
            // Installed here rather than in the constructor so `this` does not
            // escape before the session is fully built.
            view.setListener(this);

            previous = Display.getInstance().getCurrent();
            scannerForm = new Form(new BorderLayout());
            Toolbar toolbar = scannerForm.getToolbar();
            if (toolbar == null) {
                toolbar = new Toolbar();
                scannerForm.setToolbar(toolbar);
            }
            if (options.getTitle() != null) {
                scannerForm.setTitle(options.getTitle());
            }
            Command back = Command.create("", null,
                    new ActionListener<ActionEvent>() {
                        @Override
                        public void actionPerformed(ActionEvent event) {
                            finish(null, null);
                        }
                    });
            toolbar.setBackCommand(back, Toolbar.BackCommandPolicy.ALWAYS);
            scannerForm.add(BorderLayout.CENTER, view);
            Container footer = buildFooter();
            if (footer != null) {
                scannerForm.add(BorderLayout.SOUTH, footer);
            }
            scannerForm.show();
            // Showing the form installs the toolbar's own menu bar, which
            // drops a back command registered against the one the form had
            // before. Re-register it so the Android hardware back button and
            // the iOS edge swipe dismiss the scanner, not just the arrow.
            scannerForm.setBackCommand(back);
            return result;
        }

        private Container buildFooter() {
            Container footer = new Container(new BorderLayout());
            boolean populated = false;
            if (options.getHint() != null) {
                footer.add(BorderLayout.CENTER, new SpanLabel(options.getHint()));
                populated = true;
            }
            if (options.isTorchButton() && hasFlash()) {
                final Button torch = new Button("");
                FontImage.setMaterialIcon(torch, FontImage.MATERIAL_FLASH_ON);
                torch.addActionListener(new ActionListener() {
                    private boolean on;

                    @Override
                    public void actionPerformed(ActionEvent event) {
                        on = !on;
                        view.setTorchEnabled(on);
                        FontImage.setMaterialIcon(torch, on
                                ? FontImage.MATERIAL_FLASH_OFF
                                : FontImage.MATERIAL_FLASH_ON);
                    }
                });
                footer.add(BorderLayout.EAST,
                        FlowLayout.encloseCenterMiddle(torch));
                populated = true;
            }
            return populated ? footer : null;
        }

        /// Whether the camera this scan will open has a flash. Asked of the
        /// enumerated camera rather than the open session, because the footer
        /// is built before the form is shown and the session does not exist
        /// yet.
        ///
        /// @return {@code true} when a torch toggle is worth offering
        private boolean hasFlash() {
            CameraInfo info = Camera.getDefault(options.getFacing());
            return info != null && info.hasFlash();
        }

        @Override
        public void result(Barcode[] codes, VisionImage source) {
            if (codes == null) {
                return;
            }
            for (Barcode code : codes) {
                if (code != null && code.getValue() != null
                        && BarcodeFormat.matches(code, formats)) {
                    finish(code, null);
                    return;
                }
            }
        }

        @Override
        public void error(Throwable error) {
            finish(null, error);
        }

        private void finish(Barcode code, Throwable error) {
            if (finished) {
                return;
            }
            finished = true;
            view.close();
            if (previous != null) {
                previous.showBack();
            }
            if (error != null) {
                result.error(error);
            } else {
                result.complete(code);
            }
        }
    }
}
