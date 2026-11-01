// package:video_compress. Codename One has no transcoder: compressVideo answers
// the ORIGINAL file's MediaInfo and logs a warning; see VideoCompress.java.

@JavaName('com.codename1.flutter.plugins.videocompress.VideoQuality')
enum VideoQuality { DefaultQuality, LowQuality, MediumQuality, HighestQuality, Res640x480Quality,
    Res960x540Quality, Res1280x720Quality, Res1920x1080Quality }

@JavaName('com.codename1.flutter.plugins.videocompress.MediaInfo')
class MediaInfo {
  external String? get path;
  external String? get title;
  external String? get author;
  external int? get width;
  external int? get height;
  external int? get orientation;
  external int? get filesize;
  external double? get duration;
  external bool? get isCancel;
  external File? get file;
}

@JavaName('com.codename1.flutter.plugins.videocompress.Subscription')
class Subscription {
  external void unsubscribe();
}

@JavaName('com.codename1.flutter.plugins.videocompress.ObservableBuilder')
class ObservableBuilder<T> {
  external Subscription subscribe(void Function(T event) onData, {void Function()? onError, void Function()? onDone});
}

@JavaName('com.codename1.flutter.plugins.videocompress.VideoCompress')
class VideoCompress {
  external static ObservableBuilder<double> get compressProgress$;
  external static bool get isCompressing;
  external static Future<MediaInfo?> compressVideo(String path, {VideoQuality quality, bool deleteOrigin,
      int? startTime, int? duration, bool? includeAudio, int frameRate});
  external static Future<MediaInfo> getMediaInfo(String path);
  external static Future<File> getFileThumbnail(String path, {int quality, int position});
  external static Future<Uint8List?> getByteThumbnail(String path, {int quality, int position});
  external static Future<void> cancelCompression();
  external static Future<bool?> deleteAllCache();
}
