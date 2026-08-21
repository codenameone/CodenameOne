---
title: "A complete low-level API can still be the wrong starting point"
slug: 2026-08-26-0400-shai-camera-vision-scanners
platform: linkedin
account: shai
source_slug: camera-vision-scanners
publish_at: '2026-08-26T04:00:00'
timezone: Asia/Jerusalem
image: /blog/camera-vision-scanners.jpg
---

We added on-device analyzers for barcodes, faces, poses, text, documents, labels, and segmentation. Then we made a developer wire the entire camera pipeline to scan one QR code.

The low-level pieces were useful. They were also the wrong starting point for the common case.

`CodeScanner.scan()` now owns a complete scanner screen and returns one asynchronous result. `VisionCameraView` packages the camera-to-analyzer pipeline as a component for forms that need a custom flow.

Typed barcode formats and landmark names replace backend strings. Geometry helpers convert normalized results into component coordinates. `SegmentationMask.cutOut()` makes the mask useful without another image-processing layer.

The lower level remains available when a product needs direct frame control. The normal case no longer starts by rebuilding camera setup, backpressure, event-thread delivery, cancellation, and cleanup.

Good API design includes a short path and an escape hatch.

{{canonical}}
