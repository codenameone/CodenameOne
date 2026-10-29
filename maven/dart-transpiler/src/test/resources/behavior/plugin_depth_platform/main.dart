// Deeper platform plugin paths, headless: picture-to-PNG, marker hues and overlays,
// notification launch details and channel sounds, messaging permissions, App Check.
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:firebase_app_check/firebase_app_check.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

Future<void> main() async {
  try { await run(); } catch (e) { print('ERR $e'); }
}

Future<void> run() async {
  final recorder = ui.PictureRecorder();
  final canvas = Canvas(recorder);
  canvas.drawRect(Rect.fromLTWH(0, 0, 4, 4), Paint()..color = const Color(0xFFFF0000));
  print('recording ${recorder.isRecording}');
  final picture = recorder.endRecording();
  print('ops ${picture.approximateBytesUsed} recording ${recorder.isRecording}');
  final ui.Image img = await picture.toImage(4, 3);
  print('image ${img.width}x${img.height}');
  final png = await img.toByteData(format: ui.ImageByteFormat.png);
  print('png ${png!.getUint8(0)} ${png.getUint8(1)} ${png.getUint8(2)} ${png.getUint8(3)}');
  final Uint8List pngBytes = png.buffer.asUint8List();
  print('marker bytes ${pngBytes.length} ${BitmapDescriptor.fromBytes(pngBytes).hasBytes()}');
  final raw = await img.toByteData();
  print('raw ${raw!.lengthInBytes}');

  final azure = BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueAzure);
  print('azure ${azure.hue()} rgb ${BitmapDescriptor.hueToRgb(BitmapDescriptor.hueGreen)}');
  print('rose ${BitmapDescriptor.defaultMarkerWithHue(BitmapDescriptor.hueRose).hue()}');
  final bytes = BitmapDescriptor.fromBytes(Uint8List.fromList([1, 2, 3]));
  print('bytes ${bytes.hasBytes()} default ${BitmapDescriptor.defaultMarker.hue()}');
  final m = Marker(markerId: const MarkerId('a'), position: const LatLng(1, 2), icon: azure,
      infoWindow: const InfoWindow(title: 'A'), onTap: () {});
  print('marker icon ${m.icon!.hue()}');
  final line = Polyline(polylineId: const PolylineId('p'),
      points: const [LatLng(0, 0), LatLng(1, 1)], color: const Color(0xFF0000FF), width: 4);
  final circle = Circle(circleId: const CircleId('c'), center: const LatLng(0, 0), radius: 100);
  print('line ${line.polylineId.value} ${line.width} circle ${circle.circleId.value} ${circle.radius}');

  final plugin = FlutterLocalNotificationsPlugin();
  await plugin.initialize(const InitializationSettings(),
      onDidReceiveNotificationResponse: (r) => print('tap ${r.payload}'),
      onDidReceiveBackgroundNotificationResponse: (r) => print('bg ${r.payload}'));
  final launch = await plugin.getNotificationAppLaunchDetails();
  print('launched ${launch!.didNotificationLaunchApp} ${launch.notificationResponse?.payload}');
  final android = plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
  final sound = RawResourceAndroidNotificationSound('bells');
  print('sound ${sound.sound} ${sound.alertSoundPath()}');
  await android?.createNotificationChannel(AndroidNotificationChannel('ch', 'Channel', sound: sound));
  print('perm ${await android?.requestNotificationsPermission()}');
  await plugin.show(1, 'T', 'B', null, payload: 'p1');
  print('shown');

  final messaging = FirebaseMessaging.instance;
  print('before ${(await messaging.getNotificationSettings()).authorizationStatus}');
  final prov = await messaging.requestPermission(alert: true, badge: true, sound: true, provisional: true);
  print('provisional ${prov.authorizationStatus}');
  final full = await messaging.requestPermission(alert: true, badge: true, sound: true);
  print('full ${full.authorizationStatus}');
  FirebaseMessaging.onBackgroundMessage(_bg);
  print('initial ${await messaging.getInitialMessage()}');

  await FirebaseAppCheck.instance.activate(androidProvider: AndroidProvider.debug,
      appleProvider: AppleProvider.appAttest);
  print('appcheck ${await FirebaseAppCheck.instance.getToken()}');
  await FirebaseAppCheck.instance.setTokenAutoRefreshEnabled(false);
  print('auto ${FirebaseAppCheck.instance.isTokenAutoRefreshEnabled}');
  try {
    await FirebaseAppCheck.instance.getLimitedUseToken();
  } catch (e) {
    print('limited failed');
  }
}

Future<void> _bg(RemoteMessage message) async {
  print('background ${message.data}');
}
