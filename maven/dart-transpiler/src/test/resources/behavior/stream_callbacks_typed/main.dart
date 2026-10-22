// A closure handed to a Stream callback types its parameter from the stream:
// xs is a List<int>, so xs.contains is a typed call and its bool lands in a bool.
// The callbacks were typed Object, the parameter was dynamic, and javac rejected
// the untyped result.
import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';

Future<void> main() async {
  final c = StreamController<List<int>>();
  bool sawOne = false;
  final done = Completer<void>();
  c.stream.where((xs) => xs.isNotEmpty).map((xs) => xs.length).listen((n) {
    print('length ${n + 0}');
  });
  final c2 = StreamController<List<int>>();
  c2.stream.listen((xs) {
    sawOne = xs.contains(1);
  }, onDone: () => done.complete());
  c.add(<int>[]);
  c.add([1, 2]);
  c2.add([3, 1]);
  await c2.close();
  await done.future;
  print('saw one $sawOne');

  bool offline = false;
  final sub = Connectivity().onConnectivityChanged.listen((results) {
    offline = results.contains(ConnectivityResult.none);
  });
  await sub.cancel();
  print('offline $offline');
}
