// Non-void bodies whose last statement calls a function answering Never: Dart
// knows they cannot complete normally, so they need no return.
class ReadOnlyBag {
  final List<int> items;
  ReadOnlyBag(this.items);

  static Never _refuse(String op) {
    throw UnsupportedError('cannot $op a read-only bag');
  }

  bool remove(Object? element) {
    _refuse('remove from');
  }

  int removeLast() {
    _refuse('remove the last element of');
  }

  Future<bool> removeLater(Object? element) async {
    _refuse('later remove from');
  }
}

Never fail(String why) => throw StateError(why);

class Guard {
  Never stop(String why) => throw ArgumentError(why);
}

int throughReceiver(Guard g, int x) {
  if (x > 0) return x;
  g.stop('not positive: $x');
}

String pick(int i) {
  if (i == 0) return 'zero';
  fail('no name for $i');
}

Future<void> main() async {
  final bag = ReadOnlyBag([1, 2]);
  for (final f in [() => bag.remove(1), () => bag.removeLast(), () => pick(3)]) {
    try {
      f();
    } catch (e) {
      print(e);
    }
  }
  print(pick(0));
  print(throughReceiver(Guard(), 2));
  try {
    throughReceiver(Guard(), -1);
  } catch (e) {
    print(e);
  }
  try {
    await bag.removeLater(2);
  } catch (e) {
    print(e);
  }
}
