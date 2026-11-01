// Plugin gaps closed, headless: CachedNetworkImage builds its errorWidget when a load
// cannot start, its placeholder / progress builder while loading and the picture from
// cached bytes; polylines and circles hit-test taps in map coordinates and call onTap
// when they consume them.
import 'dart:typed_data';

import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

final log = <String>[];

Widget mark(String s) {
  log.add(s);
  return const Text('x');
}

void main() {
  final err = CachedNetworkImage(
    imageUrl: 'http://127.0.0.1:9/a.png',
    placeholder: (c, u) => mark('ph'),
    errorWidget: (c, u, e) => mark('err $u ${e != null}'),
  );
  final w = err.buildOnce(null);
  print(w is Text);
  print(log.join('|'));

  // no builders: an empty box, not a throw
  print(CachedNetworkImage(imageUrl: 'http://127.0.0.1:9/b.png').buildOnce(null) is SizedBox);
  // an empty url fails as well
  print(CachedNetworkImage(imageUrl: '', errorWidget: (c, u, e) => mark('empty')).buildOnce(null) is Text);

  final loading = CachedNetworkImage(
    imageUrl: 'u1',
    placeholder: (c, u) => mark('ph $u'),
  );
  loading.buildLoading(null, 0, null);
  final prog = CachedNetworkImage(
    imageUrl: 'u2',
    placeholder: (c, u) => mark('ph'),
    progressIndicatorBuilder: (c, u, p) => mark('p ${p.downloaded} ${p.totalSize} ${p.progress} ${p.originalUrl}'),
  );
  prog.buildLoading(null, 50, 200);
  prog.buildLoading(null, 7, null);
  print(log.join('|'));

  final cached = CachedNetworkImage(imageUrl: 'u3', cacheKey: 'k3', width: 10, height: 10,
      errorWidget: (c, u, e) => mark('bad'));
  cached.seedCache(Uint8List.fromList([1, 2, 3]));
  print(cached.buildOnce(null) is Image);
  print(log.length);

  final taps = <String>[];
  final line = Polyline(
    polylineId: const PolylineId('l'),
    points: const [LatLng(10, 10), LatLng(10, 10.01)],
    consumeTapEvents: true,
    onTap: () => taps.add('line'),
  );
  // 0.0001 degrees of latitude is about 11 m
  print(line.hitTest(const LatLng(10, 10.005), 5));
  print(line.hitTest(const LatLng(10.0001, 10.005), 5));
  print(line.hitTest(const LatLng(10.0001, 10.005), 20));
  print(line.hitTest(const LatLng(10, 10.02), 20));
  print(line.hitTest(const LatLng(10, 10.0101), 200));
  print(line.tapAt(const LatLng(10, 10.005), 5));
  print(line.tapAt(const LatLng(11, 10.005), 5));

  final quiet = Polyline(polylineId: const PolylineId('q'),
      points: const [LatLng(0, 0), LatLng(0, 1)], onTap: () => taps.add('quiet'));
  print(quiet.hitTest(const LatLng(0, 0.5), 5));
  print(quiet.tapAt(const LatLng(0, 0.5), 5));

  final circle = Circle(
    circleId: const CircleId('c'),
    center: const LatLng(48, 2),
    radius: 1000,
    consumeTapEvents: true,
    onTap: () => taps.add('circle'),
  );
  print(circle.hitTest(const LatLng(48.005, 2)));
  print(circle.hitTest(const LatLng(48.02, 2)));
  print(circle.tapAt(const LatLng(48, 2.005)));
  print(circle.tapAt(const LatLng(48, 2.05)));
  final hidden = Circle(circleId: const CircleId('h'), center: const LatLng(0, 0), radius: 1000,
      consumeTapEvents: true, visible: false, onTap: () => taps.add('hidden'));
  print(hidden.tapAt(const LatLng(0, 0)));
  print(taps.join(','));
}
