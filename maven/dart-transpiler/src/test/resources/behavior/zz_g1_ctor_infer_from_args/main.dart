class Box<T> {
  T value;
  Box(this.value);
  Box.named(this.value);
  factory Box.make(T v) => Box(v);
}

class Pair<A, B> {
  final A first;
  final B second;
  Pair(this.first, this.second);
}

class Holder<T> {
  final List<T> items;
  Holder(this.items);
}

void main() {
  final b = Box(3);
  print(b.runtimeType);
  print(b.value.isEven);
  print(b is Box<int>);
  print(b is Box<String>);
  final n = Box.named('hi');
  print(n.runtimeType);
  print(n.value.length);
  final m = Box.make(2.5);
  print(m.runtimeType);
  print(m.value.floor());
  final p = Pair(1, 'x');
  print(p.runtimeType);
  print(p.first + 1);
  print(p.second.toUpperCase());
  final p2 = Pair(Box(true), [1, 2]);
  print(p2.runtimeType);
  print(p2.first.value);
  final h = Holder([1, 2, 3]);
  print(h.runtimeType);
  print(h.items.first.isOdd);
  final lf = List.filled(3, 'a');
  print(lf.runtimeType);
  print(lf.first.length);
  final mo = Map.of({'k': 1});
  print(mo is Map<String, int>);
  print(mo['k']!.isOdd);
  final lo = List.of([1, 2]);
  print(lo.runtimeType);
  print(List.from([1]).runtimeType);
  final so = Set.of(['a']);
  print(so is Set<String>);
  final mixed = Pair(1, 2.0);
  print(mixed.runtimeType);
  Box<num> bn = Box(3);
  print(bn.runtimeType);
  Box<Object> bo = Box.named(1);
  print(bo.runtimeType);
  Pair<num, Object> pn = Pair(1, 'a');
  print(pn.runtimeType);
  final nested = Box(Box(1));
  print(nested.runtimeType);
  print(nested.value.value.isOdd);
  final nb = Box<int?>(null);
  print(nb.runtimeType);
  final bnull = Box(null);
  print(bnull.runtimeType);
  dynamic d = b;
  print(d is Box<int>);
  print(d is Box<num>);
}
