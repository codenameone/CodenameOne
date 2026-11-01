// The media plugin packs, headless: the web view controller's own state and
// navigation decisions, player and camera defaults, the emoji set, the honest
// failures of the frame and transcode gaps, and widgets that build.
import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';
import 'package:video_player/video_player.dart';
import 'package:video_thumbnail/video_thumbnail.dart';
import 'package:video_compress/video_compress.dart';
import 'package:camera/camera.dart';
import 'package:emoji_picker_flutter/emoji_picker_flutter.dart';
import 'package:flutter_svg/flutter_svg.dart';

Future<void> main() async {
  try { await run(); } catch (e) { print('ERR $e'); }
}

Future<void> run() async {
  final web = WebViewController();
  await web.setJavaScriptMode(JavaScriptMode.unrestricted);
  await web.setNavigationDelegate(NavigationDelegate(
    onPageFinished: (url) => print('finished $url'),
    onProgress: (p) => print('progress $p'),
    onNavigationRequest: (request) {
      if (request.url.contains('blocked')) {
        print('prevent ${request.url}');
        return NavigationDecision.prevent;
      }
      return NavigationDecision.navigate;
    },
  ));
  await web.addJavaScriptChannel('Bridge', onMessageReceived: (m) => print('msg ${m.message}'));
  await web.loadRequest(Uri.parse('https://example.com/a'));
  await web.loadRequest(Uri.parse('https://blocked.example/b'));
  await web.loadRequest(Uri.parse('https://example.com/c'));
  print('current ${await web.currentUrl()}');
  await web.goBack();
  print('back ${await web.currentUrl()} ${await web.canGoBack()}');
  await web.runJavaScript('1 + 1');
  print('js ran');
  try {
    await web.runJavaScriptReturningResult('1 + 1');
  } catch (e) {
    print('js result headless');
  }

  final video = VideoPlayerController.networkUrl(Uri.parse('https://example.com/v.mp4'));
  final v = video.value;
  print('video ${v.isInitialized} ${v.isPlaying} ${v.position.inMilliseconds} ${v.aspectRatio} ${v.volume}');
  var notified = 0;
  video.addListener(() => notified++);
  await video.setLooping(true);
  print('looping ${video.value.isLooping} notified $notified');
  try {
    await video.initialize();
  } catch (e) {
    print('init headless ${video.value.hasError}');
  }

  try {
    await VideoThumbnail.thumbnailData(video: '/tmp/v.mp4', imageFormat: ImageFormat.JPEG, maxWidth: 128);
  } catch (e) {
    print('thumbnail unsupported');
  }
  final progress = <double>[];
  final sub = VideoCompress.compressProgress$.subscribe((p) => progress.add(p));
  final info = await VideoCompress.compressVideo('/tmp/v.mp4', quality: VideoQuality.MediumQuality);
  sub.unsubscribe();
  print('compress ${info?.path} $progress');

  final cams = await availableCameras();
  print('cameras ${cams.length}');
  final cam = CameraController(
      CameraDescription(name: '0', lensDirection: CameraLensDirection.back, sensorOrientation: 90),
      ResolutionPreset.high, enableAudio: false);
  print('camera ${cam.value.isInitialized} ${cam.description.lensDirection}');
  try {
    await cam.initialize();
  } on CameraException catch (e) {
    print('camera ${e.code}');
  }

  final sets = EmojiPicker.defaultEmojiSet;
  print('emoji ${sets.length} ${sets.first.category} ${sets.first.emoji.first.name}');
  final found = await EmojiPickerUtils().searchEmoji('pizza', sets);
  print('search ${found.length} ${found.first.name}');
  final controller = TextEditingController();
  final picker = EmojiPicker(textEditingController: controller, config: Config(height: 200));
  print('picker ${picker != null}');

  final svg = SvgPicture.string('<svg xmlns="http://www.w3.org/2000/svg"/>', width: 32, height: 16,
      colorFilter: ColorFilter.mode(Colors.red, BlendMode.srcIn));
  print('svg ${svg.width} ${svg.height}');
  final widgets = [WebViewWidget(controller: web), VideoPlayer(video), CameraPreview(cam), svg];
  print('widgets ${widgets.length}');
}
