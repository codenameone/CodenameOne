// dart:ui offscreen drawing: PictureRecorder -> Canvas(recorder) -> endRecording()
// -> Picture.toImage(w, h) -> Image.toByteData(format: ImageByteFormat.png).
// The picture is replayed onto a mutable Codename One image; PNG bytes come from the
// port's ImageIO, or a built-in encoder headless. dart:ui's Image is UiImage here,
// because `Image` is the widget's name in these stubs.

@JavaName('com.codename1.flutter.ImageByteFormat')
enum ImageByteFormat { rawRgba, rawStraightRgba, rawUnmodified, rawExtendedRgba128, png }

@JavaName('com.codename1.flutter.PictureRecorder')
class PictureRecorder {
  external PictureRecorder();
  external bool get isRecording;
  external Picture endRecording();
}

@JavaName('com.codename1.flutter.Picture')
class Picture {
  external Future<UiImage> toImage(int width, int height);
  external UiImage toImageSync(int width, int height);
  external int get approximateBytesUsed;
  external void dispose();
  external bool get debugDisposed;
}

@JavaName('com.codename1.flutter.UiImage')
class UiImage {
  external int get width;
  external int get height;
  external Future<ByteData?> toByteData({ImageByteFormat format});
  external void dispose();
  external bool get debugDisposed;
}
