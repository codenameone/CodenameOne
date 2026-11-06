class Shape {
  final int w;
  Shape(this.w);
  String toString() => 'Shape($w)';
}

extension type Boxed<T>(List<T> items) {
  T get head => items.first;
  T? get maybe => items.isEmpty ? null : items.last;
  List<T> twice() => [...items, ...items];
  T at(int i) => items[i];
  T operator [](int i) => items[i];
  Boxed<T> operator +(Boxed<T> o) => Boxed<T>([...items, ...o.items]);
  Map<String, T> keyed() => {for (var i = 0; i < items.length; i++) 'k$i': items[i]};
}

extension type Pair<A, B>((A, B) rec) {
  A get left => rec.$1;
  B get right => rec.$2;
  Pair<B, A> swap() => Pair<B, A>((rec.$2, rec.$1));
}

extension type Base<T>(List<T> xs) {
  T get firstOne => xs.first;
  List<T> rev() => xs.reversed.toList();
}

extension type Sub<T>(List<T> xs) implements Base<T> {
  T get lastOne => xs.last;
}

extension type Wrapper<T>(T value) {
  T get get => value;
}

T pick<T>(T a, T b) => a;

void main() {
  final b = Boxed<Shape>([Shape(3), Shape(4)]);
  print(b.items.first.w);
  print(b.head.w);
  print(b.maybe?.w);
  print(b.twice().length + b.twice()[1].w);
  print(b.at(1).w);
  print(b[0].w);
  print((b + b).items.length);
  print((b + b).head.w);
  print(b.keyed()['k1']!.w);
  final inferred = Boxed([Shape(7)]);
  print(inferred.head.w);
  print(inferred.items.first.w);
  final e = Boxed<Shape>([]);
  print(e.maybe?.w);
  final p = Pair<String, Shape>(('a', Shape(9)));
  print(p.left.length + p.right.w);
  print(p.swap().left.w);
  print(p.swap().right.toUpperCase());
  final nested = Boxed<List<Shape>>([[Shape(5), Shape(6)]]);
  print(nested.head.last.w);
  print(nested.items.first.first.w);
  final bb = Boxed<Boxed<Shape>>([b]);
  print(bb.head.head.w);
  final s = Sub<Shape>([Shape(1), Shape(2)]);
  print(s.firstOne.w + s.lastOne.w);
  print(s.rev().first.w);
  Base<Shape> asBase = s;
  print(asBase.firstOne.w);
  final w = Wrapper<Shape>(Shape(11));
  print(w.get.w);
  print(Wrapper(Shape(12)).value.w);
  print(pick(Boxed<Shape>([Shape(21)]), Boxed<Shape>([Shape(22)])).head.w);
  print([b].first.head.w);
  Boxed<Shape>? nb = b;
  print(nb?.head.w);
  Boxed<Shape>? none;
  print(none?.head.w);
  final fromList = <Boxed<Shape>>[b, Boxed([Shape(30)])];
  print(fromList.map((x) => x.head.w).toList());
}
