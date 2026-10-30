// package:audioplayers, over MediaManager.createMedia. AssetSource paths are under
// the assets/ prefix, as with AudioCache. Without a display nothing sounds; the
// player state still follows the calls.

@JavaName('com.codename1.flutter.plugins.audioplayers.PlayerState')
enum PlayerState { stopped, playing, paused, completed, disposed }

@JavaName('com.codename1.flutter.plugins.audioplayers.ReleaseMode')
enum ReleaseMode { release, loop, stop }

@JavaName('com.codename1.flutter.plugins.audioplayers.PlayerMode')
enum PlayerMode { mediaPlayer, lowLatency }

@JavaName('com.codename1.flutter.plugins.audioplayers.Source')
abstract class Source {
  external String? get mimeType;
}

@JavaName('com.codename1.flutter.plugins.audioplayers.AssetSource')
class AssetSource extends Source {
  external AssetSource(String path, {String? mimeType});
  external String get path;
}

@JavaName('com.codename1.flutter.plugins.audioplayers.UrlSource')
class UrlSource extends Source {
  external UrlSource(String url, {String? mimeType});
  external String get url;
}

@JavaName('com.codename1.flutter.plugins.audioplayers.DeviceFileSource')
class DeviceFileSource extends Source {
  external DeviceFileSource(String path, {String? mimeType});
  external String get path;
}

@JavaName('com.codename1.flutter.plugins.audioplayers.BytesSource')
class BytesSource extends Source {
  external BytesSource(Uint8List bytes, {String? mimeType});
  external Uint8List get bytes;
}

@JavaName('com.codename1.flutter.plugins.audioplayers.AudioPlayer')
class AudioPlayer {
  external AudioPlayer({String? playerId});
  external String get playerId;
  external PlayerState get state;
  external Source? get source;
  external double get volume;
  external ReleaseMode get releaseMode;
  external Stream<void> get onPlayerComplete;
  external Stream<Duration> get onPositionChanged;
  external Stream<Duration> get onDurationChanged;
  external Stream<PlayerState> get onPlayerStateChanged;
  external Future<void> setSource(Source source);
  external Future<void> setSourceUrl(String url, {String? mimeType});
  external Future<void> setSourceDeviceFile(String path, {String? mimeType});
  external Future<void> setSourceAsset(String path, {String? mimeType});
  external Future<void> setSourceBytes(Uint8List bytes, {String? mimeType});
  external Future<void> play(Source source, {double? volume, double? balance, Object? ctx,
      Duration? position, PlayerMode? mode});
  external Future<void> resume();
  external Future<void> pause();
  external Future<void> stop();
  external Future<void> release();
  external Future<void> seek(Duration position);
  external Future<void> setVolume(double volume);
  external Future<void> setBalance(double balance);
  external Future<void> setPlaybackRate(double playbackRate);
  external Future<void> setReleaseMode(ReleaseMode releaseMode);
  external Future<void> setPlayerMode(PlayerMode mode);
  external Future<Duration?> getDuration();
  external Future<Duration?> getCurrentPosition();
  external Future<void> dispose();
}
