import 'dart:async';
import 'shelf_impl.dart';
import 'shelf_api.dart' as api;

class Stamp extends DateTime {
  Stamp.fromMillisecondsSinceEpoch(int ms) : super.fromMillisecondsSinceEpoch(ms, isUtc: true);

  @override
  String toString() => 'Stamp<${toIso8601String()}>';
}

List<T> collect<T>(int n, T Function() each) {
  final out = <T>[];
  for (var i = 0; i < n; i++) {
    out.add(each());
  }
  return out;
}

int calls = 0;
void bump(Map<String, int> m) {
  calls++;
  m['k'] = calls;
}

class Holder {
  final Object error;
  final StackTrace trace;
  Holder(this.error, [StackTrace? trace])
      : trace = trace ?? AsyncError.defaultStackTrace(error);
}

abstract class Pool<E, L extends List<E>> {
  L makeFixed(int n);
  List<E> makeGrowable(int n);
  List<E> make(bool growable, int n) {
    var list = growable ? makeGrowable(n) : makeFixed(n);
    return list;
  }
}

class IntPool extends Pool<int, List<int>> {
  @override
  List<int> makeFixed(int n) => List<int>.filled(n, 1);
  @override
  List<int> makeGrowable(int n) => List<int>.generate(n, (i) => i);
}

void main() {
  final s = Stamp.fromMillisecondsSinceEpoch(86400000);
  print(s);
  print(s.year);
  print(s.isUtc);
  print(s is DateTime);
  print(s.add(const Duration(days: 1)).day);
  print(s.millisecondsSinceEpoch);

  final m = <String, int>{};
  collect(3, () {
    bump(m);
    return 0;
  });
  print(calls);
  print(m['k']);

  final h = Holder('boom');
  print(h.trace is StackTrace);

  final p = IntPool();
  print(p.make(true, 3));
  print(p.make(false, 2));

  final shelf = Shelf({'a': 7});
  final api.Shelf asApi = shelf;
  print(asApi is Shelf);
  print(shelf.open<int>('a').item);
  print(shelf.open<String>('b', maker: (n, sh) => Crate<String>('made ' + n)).item);
}
