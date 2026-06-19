/// Session based camera API for live preview, photo capture and video recording.
///
/// A `CameraSession` is configured through `CameraSessionOptions` (selecting the
/// `CameraFacing`, `FlashMode`, `FrameFormat` and so on) and rendered through a
/// `CameraView` using a `ScaleType`. Individual `CameraFrame`s can be observed
/// with a `FrameListener`, still images are returned as a `CapturedPhoto`
/// according to `PhotoCaptureOptions`, and movies are produced as a
/// `VideoRecording`. `CameraInfo` describes the devices available on the
/// platform.
package com.codename1.camera;
