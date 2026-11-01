// package:flutter_image_compress, over Image and ImageIO. With no display there is
// no codec and every call fails with UnsupportedError.

@JavaName('com.codename1.flutter.plugins.imagecompress.CompressFormat')
enum CompressFormat { jpeg, png, heic, webp }

@JavaName('com.codename1.flutter.plugins.imagecompress.FlutterImageCompress')
class FlutterImageCompress {
  external static Future<Uint8List?> compressWithFile(String path, {int minWidth, int minHeight,
      int inSampleSize, int quality, int rotate, bool autoCorrectionAngle, CompressFormat format,
      bool keepExif, int numberOfRetries});
  external static Future<XFile?> compressAndGetFile(String path, String targetPath, {int minWidth,
      int minHeight, int inSampleSize, int quality, int rotate, bool autoCorrectionAngle,
      CompressFormat format, bool keepExif, int numberOfRetries});
  external static Future<Uint8List> compressWithList(Uint8List image, {int minWidth, int minHeight,
      int quality, int rotate, int inSampleSize, bool autoCorrectionAngle, CompressFormat format,
      bool keepExif});
  external static Future<Uint8List?> compressAssetImage(String assetName, {int minWidth, int minHeight,
      int quality, int rotate, bool autoCorrectionAngle, CompressFormat format, bool keepExif});
}
