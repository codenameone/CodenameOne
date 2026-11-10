import 'package:flutter/material.dart';

class Tile extends StatelessWidget {
  const Tile({Key? key}) : super(key: key);
  @override
  Widget build(BuildContext context) => const Text('t');
}

void main() {
  const k = ValueKey<String>('a');
  print(k.value);
  print(k.value.length);
  final w = Tile(key: k);
  print(w.key == k);
  print((w.key as ValueKey).value);
  print(const Tile().key == null);
  final o = Object();
  final ok = ObjectKey(o);
  print(identical(ok.value, o));
  final g = GlobalKey();
  print(g.currentState == null);
  print(g.currentContext == null);
}
