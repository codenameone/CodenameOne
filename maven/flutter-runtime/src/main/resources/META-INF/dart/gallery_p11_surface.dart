// Codename One Flutter runtime API stubs -- images from files and their builders,
// scale gestures, recognizers, text baselines and the remaining framework constants
// (see gallery_p4_apitail.dart for the conventions: named params -> setters,
// named constructors -> static methods).

// FileImage: a picture read from a dart:io File, drawn as a memory image.
@JavaName('com.codename1.flutter.FileImage')
class FileImage extends ImageProvider {
  external FileImage(Object file, {double? scale});
  external Object get file;
  external double get scale;
}

// How much of a picture has arrived, for Image(loadingBuilder:).
@JavaName('com.codename1.flutter.widgets.ImageChunkEvent')
class ImageChunkEvent {
  external int get cumulativeBytesLoaded;
  external int? get expectedTotalBytes;
}

// The text size when no style gives one.
@JavaName('com.codename1.flutter.material.MaterialConstants.kDefaultFontSize')
double kDefaultFontSize = 14.0;

@JavaName('com.codename1.flutter.TextBaseline')
enum TextBaseline { alphabetic, ideographic }

// A table column as wide as its widest cell.
@JavaName('com.codename1.flutter.widgets.IntrinsicColumnWidth')
class IntrinsicColumnWidth extends TableColumnWidth {
  external IntrinsicColumnWidth({double? flex});
}

// The base of every gesture recognizer.
@JavaName('com.codename1.flutter.gestures.GestureRecognizer')
abstract class GestureRecognizer {
  external void dispose();
}

// A scale gesture in progress: scale and rotation since the gesture started.
@JavaName('com.codename1.flutter.gestures.ScaleUpdateDetails')
class ScaleUpdateDetails {
  external Offset get focalPoint;
  external Offset get localFocalPoint;
  external Offset get focalPointDelta;
  external double get scale;
  external double get horizontalScale;
  external double get verticalScale;
  external double get rotation;
}

// The end of a scale gesture.
@JavaName('com.codename1.flutter.gestures.ScaleEndDetails')
class ScaleEndDetails {
  external Velocity get velocity;
  external int get pointerCount;
}
