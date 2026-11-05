import 'dart:async';

int looseCompare(Object? a, Object? b) => (a as Comparable).compareTo(b);

class Ordered<E> {
  final int Function(E, E) comparison;
  Ordered([int Function(E, E)? comparison])
      : comparison = comparison ?? looseCompare;
  Ordered.pick(bool loose, int Function(E, E) strict)
      : comparison = loose ? looseCompare : strict;
  int cmp(E a, E b) => comparison(a, b);
}

abstract class Wrapped<T> {
  T get value;
  static Future<Wrapped<T>> capture<T>(Future<T> f) => f.then((v) => Boxed<T>(v));
}

class Boxed<T> implements Wrapped<T> {
  @override
  final T value;
  Boxed(this.value);
}

class Holder<T> {
  Wrapped<T>? held;
  final Completer<void> done = Completer<void>();
  Holder(Future<T> f) {
    Wrapped.capture(f).then((w) {
      held = w;
      done.complete();
    });
  }
}

abstract class Bits {
  Bits._();
  factory Bits(int n, {bool fill = false}) {
    Bits b;
    if (n > 8) {
      b = _BigBits(n);
    } else {
      b = _SmallBits(n);
    }
    if (fill) b.setAll();
    return b;
  }
  int get size;
  bool get full;
  void setAll();
}

class _SmallBits extends Bits {
  final int size;
  bool full = false;
  _SmallBits(this.size) : super._();
  void setAll() { full = true; }
}

class _BigBits extends Bits {
  final int size;
  bool full = false;
  _BigBits(this.size) : super._();
  void setAll() { full = true; }
}

Future<void> main() async {
  var o = Ordered<String>();
  print(o.cmp('a', 'b'));
  print(Ordered<int>.pick(true, (a, b) => b - a).cmp(1, 2));
  print(Ordered<int>.pick(false, (a, b) => b - a).cmp(1, 2));
  var h = Holder<int>(Future.value(42));
  await h.done.future;
  print(h.held!.value + 1);
  var s = Bits(4, fill: true);
  var g = Bits(20);
  print('${s.size} ${s.full} ${g.size} ${g.full}');
}
