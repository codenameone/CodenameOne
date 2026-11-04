import 'dart:async';

extension type Boxed<T>(T value) {
  T get() => value;
  Boxed<R> map<R>(R Function(T) f) => Boxed<R>(f(value));
  void Function() printer() => () => print('boxed $value');
  List<T> rep(int n) => List<T>.filled(n, value);
  Future<T> later() async { await Future<void>.delayed(Duration.zero); return value; }
}

extension type Num<T extends num>(T n) {
  T twice() => (n * 2) as T;
  bool gt(T o) => n > o;
}

abstract interface class HasLen { int get length; }
extension type Wrap<E>(List<E> raw) implements Iterable<E> {
  E head() => raw.first;
}
extension type Wrap2<Q>(List<Q> raw) implements Wrap<Q> {
  Q tail() => raw.last;
}

extension type Pair<A, B>((A, B) rec) {
  A get first => rec.$1;
  B get second => rec.$2;
  Pair<B, A> swap() => Pair<B, A>((rec.$2, rec.$1));
}

Future<void> main() async {
  final b = Boxed<int>(3);
  print(b.get() + 1);
  print(b.map((x) => 'n$x').get().length);
  b.printer()();
  print(b.rep(2));
  print(await b.later() + 10);
  List<Boxed<int>> bs = [Boxed(1), Boxed(2), Boxed(3)];
  print(bs.map((e) => e.get()).fold<int>(0, (p, e) => p + e));
  bs.sort((x, y) => y.get().compareTo(x.get()));
  print(bs.first.get());
  final nn = Num<int>(5);
  print(nn.twice() + 1);
  print(nn.gt(4));
  print(Num<double>(1.5).twice());
  final w2 = Wrap2<String>(['a', 'b', 'c']);
  print(w2.head() + w2.tail());
  print(w2.length);
  print(w2.map((s) => s.toUpperCase()).join());
  Wrap<String> w = w2;
  print(w.head());
  final p = Pair<int, String>((1, 'one'));
  print(p.swap().first.length + p.swap().second);
  Boxed<Boxed<String>> nested = Boxed(Boxed('deep'));
  print(nested.get().get());
  final fs = <Future<int>>[Boxed(4).later(), Boxed(5).later()];
  print((await Future.wait(fs)).reduce((a, c) => a + c));
}
