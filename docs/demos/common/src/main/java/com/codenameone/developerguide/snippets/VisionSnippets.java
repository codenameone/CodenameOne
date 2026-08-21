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
package com.codenameone.developerguide.snippets;

import com.codename1.ai.vision.Barcode;
import com.codename1.ai.vision.BarcodeFormat;
import com.codename1.ai.vision.BarcodeScanner;
import com.codename1.ai.vision.CodeScanner;
import com.codename1.ai.vision.CodeScannerOptions;
import com.codename1.ai.vision.DocumentScanner;
import com.codename1.ai.vision.Face;
import com.codename1.ai.vision.FaceDetector;
import com.codename1.ai.vision.ImageLabel;
import com.codename1.ai.vision.ImageLabeler;
import com.codename1.ai.vision.Pose;
import com.codename1.ai.vision.PoseDetector;
import com.codename1.ai.vision.PoseLandmarks;
import com.codename1.ai.vision.SegmentationMask;
import com.codename1.ai.vision.SelfieSegmenter;
import com.codename1.ai.vision.TextRecognitionResult;
import com.codename1.ai.vision.TextRecognizer;
import com.codename1.ai.vision.VisionCameraView;
import com.codename1.ai.vision.VisionImage;
import com.codename1.ai.vision.VisionOptions;
import com.codename1.ai.vision.VisionPipelineListener;
import com.codename1.camera.CameraFacing;
import com.codename1.components.ToastBar;
import com.codename1.io.Log;
import com.codename1.ui.Button;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Compiled source snippets for the on-device vision guide chapter. */
public class VisionSnippets {
    private String photoPath;
    private byte[] jpegBytes;
    private Label resultLabel;
    private Label statusLabel;
    private TextArea textArea;
    private Label preview;
    private Button shutter;
    private int reps;

    /** The whole scanner screen, in one call. */
    public void codeScanner() {
        // tag::vision-code-scanner[]
        if (!CodeScanner.isSupported()) {
            ToastBar.showErrorMessage("This device cannot scan codes");
            return;
        }

        CodeScanner.scan().ready(code -> {
            if (code == null) {
                return;                       // the user backed out
            }
            resultLabel.setText(code.getValue());
        }).except(error -> Log.e(error));
        // end::vision-code-scanner[]
    }

    /** The same screen, restricted to the codes this app actually wants. */
    public void codeScannerOptions() {
        // tag::vision-code-scanner-options[]
        CodeScanner.scan(new CodeScannerOptions()
                .title("Boarding pass")
                .hint("Hold the pass flat inside the frame")
                .formats(BarcodeFormat.PDF417, BarcodeFormat.AZTEC))
            .ready(code -> {
                if (code != null) {
                    checkIn(code.getValue());
                }
            })
            .except(error -> Log.e(error));
        // end::vision-code-scanner-options[]
    }

    /** A live preview inside a form of your own, analyzing every frame. */
    public void cameraView() {
        // tag::vision-camera-view[]
        FaceDetector detector = new FaceDetector();
        VisionCameraView<Face[]> view = new VisionCameraView<>(detector);
        view.setFacing(CameraFacing.FRONT);
        view.setListener(new VisionPipelineListener<Face[]>() {
            @Override
            public void result(Face[] faces, VisionImage source) {
                shutter.setEnabled(faces.length == 1);
                statusLabel.setText(faces.length == 1
                        ? "Looking good"
                        : "Fit exactly one face in the frame");
            }

            @Override
            public void error(Throwable error) {
                Log.e(error);
            }
        });

        Form form = new Form("Selfie", new BorderLayout());
        form.add(BorderLayout.CENTER, view);
        form.add(BorderLayout.SOUTH, BoxLayout.encloseY(statusLabel, shutter));
        form.show();
        // The camera is opened when the form is shown and released when the
        // user navigates away. Call view.close() when the screen is finished
        // with for good, which also releases the detector.
        // end::vision-camera-view[]
    }

    /** Decoding codes out of a picture the user already has. */
    public void stillBarcode() throws IOException {
        // tag::vision-still-barcode[]
        BarcodeScanner scanner = new BarcodeScanner();
        scanner.process(VisionImage.fromFile(photoPath)).ready(codes -> {
            for (Barcode code : codes) {
                if (BarcodeFormat.matches(code, BarcodeFormat.EAN_13,
                        BarcodeFormat.UPC_A)) {
                    lookUpProduct(code.getValue());
                }
            }
            scanner.close();
        }).except(error -> {
            Log.e(error);
            scanner.close();
        });
        // end::vision-still-barcode[]
    }

    /** Reading the text out of a photographed receipt or label. */
    public void recognizeText() throws IOException {
        // tag::vision-text[]
        TextRecognizer recognizer = new TextRecognizer();
        recognizer.process(VisionImage.fromFile(photoPath)).ready(result -> {
            textArea.setText(result.getText());
            recognizer.close();
        }).except(error -> {
            Log.e(error);
            recognizer.close();
        });
        // end::vision-text[]
    }

    /** Live OCR that stops at the first block matching the expected shape. */
    public void recognizeTextLive() {
        // tag::vision-text-live[]
        VisionCameraView<TextRecognitionResult> view =
                new VisionCameraView<>(new TextRecognizer());
        view.setListener(new VisionPipelineListener<TextRecognitionResult>() {
            @Override
            public void result(TextRecognitionResult text, VisionImage source) {
                for (TextRecognitionResult.TextBlock block : text.getBlocks()) {
                    // Every frame is analyzed until one block looks like the
                    // value being hunted for.
                    if (block.getText().startsWith("SN-")) {
                        accept(block.getText());
                        return;
                    }
                }
            }

            @Override
            public void error(Throwable error) {
                Log.e(error);
            }
        });
        // end::vision-text-live[]
    }

    /** Cropping a photo down to the face it contains. */
    public void detectFaceInPhoto() {
        // tag::vision-face-still[]
        Image photo = EncodedImage.create(jpegBytes);
        FaceDetector detector = new FaceDetector();
        detector.process(VisionImage.fromImage(photo)).ready(faces -> {
            if (faces.length > 0) {
                Rectangle box = faces[0].getBounds()
                        .toBounds(0, 0, photo.getWidth(), photo.getHeight());
                preview.setIcon(photo.subImage(box.getX(), box.getY(),
                        box.getWidth(), box.getHeight(), true));
            }
            detector.close();
        }).except(error -> {
            Log.e(error);
            detector.close();
        });
        // end::vision-face-still[]
    }

    /** Classifying what an image contains. */
    public void labelImage() throws IOException {
        // tag::vision-labels[]
        ImageLabeler labeler = new ImageLabeler(new VisionOptions()
                .minimumConfidence(0.6f)
                .maximumResults(5));

        labeler.process(VisionImage.fromFile(photoPath)).ready(labels -> {
            StringBuilder summary = new StringBuilder();
            for (ImageLabel label : labels) {
                summary.append(label.getText())
                       .append(" (")
                       .append(Math.round(label.getConfidence() * 100))
                       .append("%)\n");
            }
            resultLabel.setText(summary.toString());
            labeler.close();
        }).except(error -> {
            Log.e(error);
            labeler.close();
        });
        // end::vision-labels[]
    }

    /** Counting arm raises from the live camera. */
    public void countReps() {
        // tag::vision-pose[]
        VisionCameraView<Pose> view = new VisionCameraView<>(new PoseDetector());
        view.setListener(new VisionPipelineListener<Pose>() {
            private boolean wasUp;

            @Override
            public void result(Pose pose, VisionImage source) {
                Pose.Landmark wrist = pose.getLandmark(PoseLandmarks.RIGHT_WRIST);
                Pose.Landmark shoulder =
                        pose.getLandmark(PoseLandmarks.RIGHT_SHOULDER);
                if (wrist == null || shoulder == null
                        || wrist.getConfidence() < 0.6f) {
                    return;                    // this frame did not see the arm
                }
                // Y grows downwards, so "above" is the smaller value.
                boolean up = wrist.getPosition().getY()
                        < shoulder.getPosition().getY();
                if (up && !wasUp) {
                    reps++;
                    statusLabel.setText(String.valueOf(reps));
                }
                wasUp = up;
            }

            @Override
            public void error(Throwable error) {
                Log.e(error);
            }
        });
        // end::vision-pose[]
    }

    /** Removing the background from a selfie. */
    public void removeBackground() {
        // tag::vision-segmentation[]
        EncodedImage photo = EncodedImage.create(jpegBytes);
        SelfieSegmenter segmenter = new SelfieSegmenter();

        segmenter.process(VisionImage.encoded(jpegBytes)).ready(mask -> {
            // Below 60% foreground confidence becomes transparent, so whatever
            // is painted behind this image shows through.
            Image person = mask.cutOut(photo, 0.6f);
            preview.setIcon(person);
            segmenter.close();
        }).except(error -> {
            Log.e(error);
            segmenter.close();
        });
        // end::vision-segmentation[]
    }

    /** Flattening a photographed page. */
    public void scanDocument() throws IOException {
        // tag::vision-document[]
        DocumentScanner scanner = new DocumentScanner();
        if (!scanner.isSupported()) {
            // Google's Android document scanner is an interactive flow rather
            // than an analyzer, so Android reports this unsupported.
            scanner.close();
            return;
        }
        scanner.process(VisionImage.fromFile(photoPath)).ready(result -> {
            List<Image> pages = new ArrayList<>();
            for (int i = 0; i < result.getPageCount(); i++) {
                pages.add(EncodedImage.create(result.getPage(i)));
            }
            show(pages);
            scanner.close();
        }).except(error -> {
            Log.e(error);
            scanner.close();
        });
        // end::vision-document[]
    }

    /** Drawing normalized result geometry over the image it was found in. */
    // tag::vision-overlay[]
    class FaceOverlay extends Label {
        private Face[] faces = new Face[0];

        FaceOverlay(Image photo) {
            super(photo);
        }

        void setFaces(Face[] value) {
            faces = value;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            g.setColor(0x34c759);
            for (Face face : faces) {
                // Results are normalized to 0..1, so the same observation maps
                // onto whatever size this component happens to be.
                Rectangle r = face.getBounds().toBounds(this);
                g.drawRect(r.getX(), r.getY(), r.getWidth(), r.getHeight(), 3);
            }
        }
    }
    // end::vision-overlay[]

    private void checkIn(String value) {
        Log.p("checking in " + value);
    }

    private void lookUpProduct(String value) {
        Log.p("looking up " + value);
    }

    private void accept(String value) {
        Log.p("accepted " + value);
    }

    private void show(List<Image> pages) {
        Log.p("showing " + pages.size() + " page(s)");
    }
}
