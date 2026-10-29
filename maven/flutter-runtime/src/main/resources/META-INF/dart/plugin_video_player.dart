// package:video_player over Codename One's MediaManager: the VideoPlayer widget
// hosts the Media's video component, and the position is polled while playing.
// Captions and mixWithOthers are accepted and not applied.

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoPlayerOptions')
class VideoPlayerOptions {
  external VideoPlayerOptions({bool mixWithOthers, bool allowBackgroundPlayback});
}

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoPlayerValue')
class VideoPlayerValue {
  external Duration get duration;
  external Duration get position;
  external Size get size;
  external bool get isInitialized;
  external bool get isPlaying;
  external bool get isLooping;
  external bool get isBuffering;
  external bool get isCompleted;
  external double get volume;
  external double get playbackSpeed;
  external bool get hasError;
  external String? get errorDescription;
  external double get aspectRatio;
}

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoPlayerController')
class VideoPlayerController extends ValueNotifier<VideoPlayerValue> {
  @Unapplied('httpHeaders, closedCaptionFile, formatHint')
  external static VideoPlayerController networkUrl(Uri url, {VideoPlayerOptions? videoPlayerOptions,
      Map<String, String> httpHeaders, dynamic formatHint, dynamic closedCaptionFile});
  @Unapplied('httpHeaders, closedCaptionFile, formatHint')
  external static VideoPlayerController network(String dataSource, {VideoPlayerOptions? videoPlayerOptions,
      Map<String, String> httpHeaders, dynamic formatHint, dynamic closedCaptionFile});
  @Unapplied('httpHeaders, closedCaptionFile, formatHint')
  external static VideoPlayerController file(File file, {VideoPlayerOptions? videoPlayerOptions,
      Map<String, String> httpHeaders, dynamic closedCaptionFile});
  external static VideoPlayerController asset(String dataSource, {String? package,
      VideoPlayerOptions? videoPlayerOptions, dynamic closedCaptionFile});
  external String get dataSource;
  external VideoPlayerValue get value;
  external Future<Duration?> get position;
  external Future<void> initialize();
  external Future<void> play();
  external Future<void> pause();
  external Future<void> seekTo(Duration position);
  external Future<void> setLooping(bool looping);
  external Future<void> setVolume(double volume);
  external Future<void> setPlaybackSpeed(double speed);
  external void addListener(VoidCallback listener);
  external void removeListener(VoidCallback listener);
  external Future<void> dispose();
}

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoPlayer')
class VideoPlayer extends Widget {
  external VideoPlayer(VideoPlayerController controller, {Key? key});
}

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoProgressColors')
class VideoProgressColors {
  external VideoProgressColors({Color playedColor, Color bufferedColor, Color backgroundColor});
}

@JavaName('com.codename1.flutter.plugins.videoplayer.VideoProgressIndicator')
class VideoProgressIndicator extends Widget {
  external VideoProgressIndicator(VideoPlayerController controller, {Key? key,
      VideoProgressColors? colors, required bool allowScrubbing, EdgeInsets? padding});
}
