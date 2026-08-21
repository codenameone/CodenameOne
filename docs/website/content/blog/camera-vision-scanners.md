---
title: "CodeScanner.scan(): Barcode Scanning Without Rebuilding the Camera Pipeline"
slug: camera-vision-scanners
url: /blog/camera-vision-scanners/
date: '2026-08-26'
author: Shai Almog
description: "CodeScanner and VisionCameraView put full-screen scanning and embedded live analysis above Codename One's on-device vision APIs, with typed results and simulator scripting."
feed_html: '<img src="https://www.codenameone.com/blog/camera-vision-scanners.jpg" alt="A phone camera recognizing a barcode, face, pose, document, and text through one vision pipeline" /> CodeScanner and VisionCameraView put full-screen scanning and embedded live analysis above Codename One&#39;s on-device vision APIs, with typed results and simulator scripting.'
series: ["release-2026-08-21"]
---

![A phone camera recognizing a barcode, face, pose, document, and text through one vision pipeline](/blog/camera-vision-scanners.jpg)

The new vision analyzers could read barcodes, faces, poses, text, documents, and segmentation masks. Scanning one QR code still meant opening a camera, configuring a session, listening for frames, converting each frame, feeding a pipeline, moving the result to the event thread, and restoring the previous form.

The low-level layer remains necessary for custom camera products. It should not be a prerequisite for reading one code or counting faces.

[PR #5575](https://github.com/codenameone/CodenameOne/pull/5575) adds the missing layers above the analyzers. For the other work that shipped this week, see the [weekly release overview](/blog/sqlite-portable-encrypted/).

## One call owns the scanner screen

`CodeScanner.scan()` opens a scanner form, runs the barcode analyzer, returns the first accepted code, and restores the form the application was showing:

```java
if (!CodeScanner.isSupported()) {
    ToastBar.showErrorMessage("This device cannot scan codes");
    return;
}

CodeScanner.scan().ready(code -> {
    if (code == null) {
        return; // the user pressed back
    }
    urlField.setText(code.getValue());
}).except(error -> Log.e(error));
```

Cancellation is a `null` result. A camera or decoder failure reaches `except(...)`. That maps the old scanner library's three callbacks onto one `AsyncResource` without treating the back button as an error.

`CodeScannerOptions` changes the screen text and restricts the accepted formats:

```java
CodeScanner.scan(new CodeScannerOptions()
        .title("Boarding pass")
        .hint("Hold the pass flat inside the frame")
        .formats(BarcodeFormat.PDF417, BarcodeFormat.AZTEC))
    .ready(code -> {
        if (code != null) {
            checkIn(code.getValue());
        }
    });
```

Restricting formats prevents a nearby product barcode from completing a QR-only flow.

## One component owns the live pipeline

`VisionCameraView` covers the case where the preview belongs inside an application form. It opens the camera when shown, keeps only the newest frame while analysis is busy, delivers results on the event dispatch thread, and releases the camera when the user leaves.

{{< mermaid >}}
flowchart LR
    A[Camera frames] --> B[VisionCameraView]
    B --> C[Keep newest frame]
    C --> D[Caller-selected analyzer]
    D --> E[Typed result on EDT]
    E --> F[Application UI]
    G[Form hidden] --> H[Release camera]
    I[close] --> J[Release analyzer]
{{< /mermaid >}}

```java
Form previous = Display.getInstance().getCurrent();
FaceDetector detector = new FaceDetector();
VisionCameraView<Face[]> view = new VisionCameraView<>(detector);
view.setFacing(CameraFacing.FRONT);
view.setListener(new VisionPipelineListener<Face[]>() {
    public void result(Face[] faces, VisionImage source) {
        countLabel.setText(faces.length + " face(s)");
    }

    public void error(Throwable error) {
        Log.e(error);
    }
});

Form form = new Form("Faces", new BorderLayout());
form.add(BorderLayout.CENTER, view);
form.add(BorderLayout.SOUTH, countLabel);
form.getToolbar().setBackCommand("Back", event -> {
    view.close();
    previous.showBack();
});
form.show();
```

The analyzer remains caller-constructed. That is how the build decides which native model to package. A face-detection application should not carry barcode, pose, and segmentation dependencies merely because a high-level component knows those analyzers exist.

Leaving the form temporarily stops the camera but keeps the analyzer ready in case the form is shown again. Call `close()` from the navigation path that discards the screen permanently. It releases both resources and cannot be reversed.

## Results use types and component coordinates

Backend strings such as `"QR_CODE"` and `"leftEye"` are now represented by `BarcodeFormat`, `FaceLandmarks`, and `PoseLandmarks`. Geometry helpers turn normalized analyzer coordinates into pixels an application can draw.

```java
Rectangle box = faces[0].getBounds()
        .toBounds(0, 0, photo.getWidth(), photo.getHeight());

Image face = photo.subImage(box.getX(), box.getY(),
        box.getWidth(), box.getHeight(), true);
```

`VisionImage.fromFile(...)` and `VisionImage.fromImage(...)` bridge picked images into the analyzer path. Encoded images keep their original bytes instead of being decoded and encoded again.

Selfie segmentation also gets the operation that makes its result immediately usable:

```java
SelfieSegmenter segmenter = new SelfieSegmenter();
EncodedImage photo = EncodedImage.create(jpegBytes);

segmenter.process(VisionImage.encoded(jpegBytes)).ready(mask -> {
    Image person = mask.cutOut(photo, 0.6f);
    preview.setIcon(person);
    segmenter.close();
}).except(error -> {
    Log.e(error);
    segmenter.close();
});
```

Pixels below 60 percent foreground confidence become transparent.

## High-level classes must still select native dependencies

The build scans application classes, not every class in core. An application referencing only `CodeScanner` never directly names `BarcodeScanner`. Without an explicit mapping, the build could prune the barcode adapter and camera natives while still compiling successfully.

The new high-level classes select the dependencies they need. `HighLevelVisionDependencyTest` walks the vision sources and fails when a future convenience class is added without a corresponding build mapping. This tests the failure mode that would otherwise appear only as an inert feature on a device.

## Vision can be scripted before hardware arrives

In the simulator, **Simulate > Vision** can mark a feature supported or unsupported and select a result, no-result, backend-error, or unsupported outcome. Scripted results include plausible geometry.

That lets an application test cancellation, debounce behavior, overlays, and error copy without a camera or a trained model. It also makes the same application code run in desktop UI tests.

The preview remains a native view. iOS and Android do not give native peers the same stacking order, so portable components should sit around the preview rather than assume they can paint a reticle on top of it. Only one camera session can be open at a time. The non-default preview scale modes are currently honored by the simulator but ignored by the iOS and Android camera previews.

The low-level APIs remain available for products that need direct frame control. The change is that barcode scanning, face counting, and common camera analysis no longer start by rebuilding the plumbing.

The {{< post-link path="/blog/app-intents-siri-spotlight-shortcuts" text="next post shows how to expose a Java intent to Siri, Spotlight, and Shortcuts" >}}.

---

## Discussion

_Which vision task needs a ready-made screen, and which one needs a camera component you fully control?_

{{< giscus >}}
