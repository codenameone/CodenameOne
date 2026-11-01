// package:video_thumbnail. Codename One has no portable frame extractor, so every
// call completes with an error naming the gap; see VideoThumbnail.java.

@JavaName('com.codename1.flutter.plugins.videothumbnail.ImageFormat')
enum ImageFormat { JPEG, PNG, WEBP }

@JavaName('com.codename1.flutter.plugins.videothumbnail.VideoThumbnail')
class VideoThumbnail {
  external static Future<Uint8List?> thumbnailData({required String video, Map<String, String>? headers,
      ImageFormat imageFormat, int maxHeight, int maxWidth, int timeMs, int quality});
  external static Future<String?> thumbnailFile({required String video, Map<String, String>? headers,
      String? thumbnailPath, ImageFormat imageFormat, int maxHeight, int maxWidth, int timeMs, int quality});
}
