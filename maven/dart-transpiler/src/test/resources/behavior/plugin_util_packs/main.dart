// The utility plugin packs, headless: what each answers with no platform to ask.
import 'dart:typed_data';

import 'package:android_play_install_referrer/android_play_install_referrer.dart';
import 'package:app_settings/app_settings.dart';
import 'package:audioplayers/audioplayers.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter_image_compress/flutter_image_compress.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:flutter_web_auth_2/flutter_web_auth_2.dart';
import 'package:flutter/services.dart';
import 'package:geocoding/geocoding.dart';
import 'package:image_picker/image_picker.dart';
import 'package:image_picker_android/image_picker_android.dart';
import 'package:image_picker_platform_interface/image_picker_platform_interface.dart';
import 'package:mixpanel_flutter/mixpanel_flutter.dart';

Future<void> main() async {
  try { await run(); } catch (e) { print('ERR $e'); }
}

Future<void> run() async {
  final tz = await FlutterTimezone.getLocalTimezone();
  print('timezone ${tz.isNotEmpty}');
  final zones = await FlutterTimezone.getAvailableTimezones();
  print('zones has local ${zones.contains(tz)}');

  await AppSettings.openAppSettings(type: AppSettingsType.location);
  print('settings opened');

  try {
    await AndroidPlayInstallReferrer.installReferrer;
    print('referrer?');
  } on UnsupportedError catch (_) {
    print('referrer unsupported');
  }

  try {
    await placemarkFromCoordinates(52.52, 13.40);
    print('placemark?');
  } on NoResultFoundException catch (_) {
    print('no placemark');
  }
  await setLocaleIdentifier('en_US');
  print('geocoder ${await isPresent()}');

  final picked = await FilePicker.platform.pickFiles(type: FileType.image, allowMultiple: true);
  print('picked ${picked == null}');
  try {
    await FilePicker.platform.pickFiles(type: FileType.custom);
  } on ArgumentError catch (_) {
    print('custom needs extensions');
  }
  print('dir ${await FilePicker.platform.getDirectoryPath()}');

  final player = AudioPlayer(playerId: 'p1');
  print('player ${player.playerId} ${player.state}');
  await player.setReleaseMode(ReleaseMode.loop);
  await player.play(AssetSource('sounds/ding.mp3'));
  print('after play ${player.state} ${player.releaseMode}');
  await player.pause();
  print('after pause ${player.state}');
  await player.stop();
  await player.dispose();
  print('after dispose ${player.state}');

  final mp = await Mixpanel.init('tok', trackAutomaticEvents: false, superProperties: {'app': 'pets'});
  mp.setServerURL('https://api-eu.mixpanel.com');
  await mp.track('Opened', properties: {'screen': 'home'});
  await mp.identify('user-1');
  mp.getPeople().set('plan', 'pro');
  mp.getPeople().increment('visits', 1);
  print('mixpanel id ${await mp.getDistinctId()} queued ${mp.cn1PendingCount}');
  await mp.flush();
  print('mixpanel after headless flush ${mp.cn1PendingCount}');
  mp.optOutTracking();
  await mp.track('Hidden');
  print('mixpanel opted out ${await mp.hasOptedOutTracking()} ${mp.cn1PendingCount}');

  try {
    await FlutterWebAuth2.authenticate(url: 'https://example.com/auth', callbackUrlScheme: 'pets');
    print('auth?');
  } on PlatformException catch (e) {
    print('auth ${e.code}');
  }

  try {
    await FlutterImageCompress.compressWithList(Uint8List.fromList([1, 2, 3]), quality: 80,
        format: CompressFormat.jpeg);
    print('compressed?');
  } on UnsupportedError catch (_) {
    print('compress unsupported');
  }

  ImagePickerAndroid.registerWith();
  final android = ImagePickerPlatform.instance;
  if (android is ImagePickerAndroid) {
    android.useAndroidPhotoPicker = true;
    print('photo picker ${android.useAndroidPhotoPicker}');
  }
  final img = await ImagePickerPlatform.instance.getImageFromSource(source: ImageSource.gallery);
  print('image ${img == null}');
}
