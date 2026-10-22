// package:image_picker, over Capture (camera) and Display.openGallery. Resizing
// and quality arguments are accepted and not applied.

@JavaName('com.codename1.flutter.plugins.imagepicker.ImageSource')
enum ImageSource { camera, gallery }

@JavaName('com.codename1.flutter.plugins.imagepicker.CameraDevice')
enum CameraDevice { rear, front }

@JavaName('com.codename1.flutter.plugins.imagepicker.ImagePicker')
class ImagePicker {
  external ImagePicker();
  external Future<XFile?> pickImage({required ImageSource source, double? maxWidth, double? maxHeight,
      int? imageQuality, CameraDevice preferredCameraDevice, bool requestFullMetadata});
  external Future<XFile?> pickVideo({required ImageSource source, CameraDevice preferredCameraDevice,
      Duration? maxDuration});
  external Future<XFile?> pickMedia({double? maxWidth, double? maxHeight, int? imageQuality,
      bool requestFullMetadata});
  external Future<List<XFile>> pickMultiImage({double? maxWidth, double? maxHeight, int? imageQuality,
      int? limit, bool requestFullMetadata});
  external Future<List<XFile>> pickMultipleMedia({double? maxWidth, double? maxHeight, int? imageQuality,
      int? limit, bool requestFullMetadata});
}
