// package:camera over com.codename1.camera: a live CameraView preview, photos
// and video from the CameraSession. Exposure, focus and image streams are not
// offered.

@JavaName('com.codename1.flutter.plugins.camera.CameraLensDirection')
enum CameraLensDirection { front, back, external }

@JavaName('com.codename1.flutter.plugins.camera.ResolutionPreset')
enum ResolutionPreset { low, medium, high, veryHigh, ultraHigh, max }

@JavaName('com.codename1.flutter.plugins.camera.ImageFormatGroup')
enum ImageFormatGroup { unknown, yuv420, bgra8888, jpeg, nv21 }

@JavaName('com.codename1.flutter.plugins.camera.FlashMode')
enum FlashMode { off, auto, always, torch }

@JavaName('com.codename1.flutter.plugins.camera.CameraDescription')
class CameraDescription {
  external CameraDescription({required String name, required CameraLensDirection lensDirection,
      required int sensorOrientation});
  external String get name;
  external CameraLensDirection get lensDirection;
  external int get sensorOrientation;
}

@JavaName('com.codename1.flutter.plugins.camera.CameraException')
class CameraException {
  external CameraException(String code, String? description);
  external String get code;
  external String? get description;
}

@JavaName('com.codename1.flutter.plugins.camera.CameraValue')
class CameraValue {
  external bool get isInitialized;
  external bool get isRecordingVideo;
  external bool get isTakingPicture;
  external Size? get previewSize;
  external double get aspectRatio;
  external FlashMode get flashMode;
  external String? get errorDescription;
  external bool get hasError;
}

@JavaName('com.codename1.flutter.plugins.camera.Cameras.availableCameras')
external Future<List<CameraDescription>> availableCameras();

@JavaName('com.codename1.flutter.plugins.camera.CameraController')
class CameraController extends ValueNotifier<CameraValue> {
  external CameraController(CameraDescription description, ResolutionPreset resolutionPreset,
      {bool enableAudio, ImageFormatGroup? imageFormatGroup});
  external CameraDescription get description;
  external CameraValue get value;
  external Future<void> initialize();
  external Future<XFile> takePicture();
  external Future<void> startVideoRecording({dynamic onAvailable});
  external Future<XFile> stopVideoRecording();
  external Future<void> setFlashMode(FlashMode mode);
  external Future<void> setZoomLevel(double zoom);
  external Future<void> pausePreview();
  external Future<void> resumePreview();
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external Future<void> dispose();
}

@JavaName('com.codename1.flutter.plugins.camera.CameraPreview')
class CameraPreview extends Widget {
  external CameraPreview(CameraController controller, {Key? key, Widget? child});
}
