// The device plugin packs, headless: the parts that answer without a platform --
// math, mocks, in-memory files, option holders and Firebase's failure paths.
import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:cross_file/cross_file.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:package_info_plus/package_info_plus.dart';

Future<void> main() async {
  try { await run(); } catch (e) { print('ERR $e'); }
}

Future<void> run() async {
  final km = Geolocator.distanceBetween(52.5200, 13.4050, 48.8566, 2.3522) / 1000;
  print('berlin-paris ${km.round()} km');
  final bearing = Geolocator.bearingBetween(0, 0, 1, 0);
  print('north ${bearing.round()}');

  PackageInfo.setMockInitialValues(appName: 'Pets', packageName: 'com.example.pets',
      version: '1.2.3', buildNumber: '45', buildSignature: '');
  final info = await PackageInfo.fromPlatform();
  print('${info.appName} ${info.version}+${info.buildNumber}');

  final net = await Connectivity().checkConnectivity();
  print('offline ${net.contains(ConnectivityResult.none)}');

  final file = XFile.fromData(Uint8List.fromList(utf8.encode('hello')), name: 'a.txt', mimeType: 'text/plain');
  print('${file.name} ${file.mimeType} ${await file.readAsString()} ${await file.length()}');

  const details = NotificationDetails(
      android: AndroidNotificationDetails('pets', 'Pets', importance: Importance.high, priority: Priority.high));
  print('details ${details != null}');
  final plugin = FlutterLocalNotificationsPlugin();
  final android = plugin.resolvePlatformSpecificImplementation<AndroidFlutterLocalNotificationsPlugin>();
  print('platform impl ${android == null}');

  try {
    await FirebaseAuth.instance.signInWithEmailAndPassword(email: 'a@b.c', password: 'x');
  } on FirebaseAuthException catch (e) {
    print('before init: ${e.code}');
  }
  await Firebase.initializeApp(
      options: const FirebaseOptions(apiKey: 'k', appId: 'a', messagingSenderId: 'm', projectId: 'p'));
  print('apps ${Firebase.apps.length} ${Firebase.app().options.projectId}');
  try {
    await FirebaseAuth.instance.signInAnonymously();
  } on FirebaseAuthException catch (e) {
    print('headless: ${e.code}');
  }
  print('user ${FirebaseAuth.instance.currentUser}');

  final m = Marker(markerId: const MarkerId('home'), position: const LatLng(10, 190),
      infoWindow: const InfoWindow(title: 'Home'));
  print('${m.markerId.value} ${m.position.latitude} ${m.position.longitude} ${m.infoWindow.title}');
  final cam = CameraPosition(target: const LatLng(1, 2), zoom: 12);
  print('camera ${cam.target.latitude} ${cam.zoom}');
}
