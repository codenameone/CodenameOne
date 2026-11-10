// Fields that override (or are overridden by) members of an override set whose
// Java return is boxed, across classes, interfaces and mixins, and calls through
// mixin-typed receivers whose results are compared, added and used as map keys.

abstract class Shape {
  num get area;
}

class Square extends Shape {
  @override
  final int area;
  Square(this.area);
}

abstract class Sized {
  int get size;
}

abstract class Measured {
  num get size;
}

class Box implements Sized, Measured {
  @override
  final int size;
  Box(this.size);
}

mixin Weighted {
  double get weight;
  num scaled(int k) => weight * k;
}

class Crate with Weighted {
  @override
  double weight;
  Crate(this.weight);
}

abstract class Loose {
  num get weight;
}

class Pallet extends Crate implements Loose {
  Pallet(double w) : super(w);
}

mixin Counter {
  int get count => 3;
  bool get even => count % 2 == 0;
}

abstract class Countable {
  num get count;
  Object get even;
}

class Tally with Counter implements Countable {}

class Tally2 with Counter implements Countable {
  @override
  int count;
  @override
  bool even;
  Tally2(this.count, this.even);
}

void main() {
  Shape s = Square(7);
  Square q = Square(7);
  print('${s.area} ${q.area} ${q.area + 1} ${q.area == s.area}');
  Sized a = Box(4);
  Measured b = Box(4);
  Box c = Box(4);
  print('${a.size} ${b.size} ${c.size * 2} ${a.size == c.size} ${identical(a.size, c.size)}');
  Weighted w = Crate(1.5);
  Weighted w2 = Pallet(1.5);
  Crate cr = Crate(1.5);
  Loose l = Pallet(1.5);
  print('${w.weight} ${w2.weight} ${cr.weight + 1} ${l.weight}');
  print('${w.weight == w2.weight} ${identical(w.weight, w2.weight)} ${w.weight + w2.weight}');
  print('${w.scaled(2)} ${w.scaled(2) == w2.scaled(2)}');
  cr.weight = 2.5;
  print('${cr.weight} ${(cr as Weighted).weight}');
  Counter m1 = Tally();
  Counter m2 = Tally();
  print('${m1.count == m2.count} ${identical(m1.count, m2.count)} ${m1.count + m2.count} ${m1.even == m2.even}');
  Counter m3 = Tally2(1000000, true);
  Counter m4 = Tally2(1000000, true);
  print('${m3.count == m4.count} ${identical(m3.count, m4.count)} ${m3.count - m4.count} ${m3.even == m4.even}');
  final map = <int, String>{};
  map[m3.count] = 'a';
  map[m4.count] = 'b';
  final bm = <bool, int>{};
  bm[m3.even] = 1;
  bm[m4.even] = 2;
  print('${map.length} ${map[1000000]} ${bm.length} ${bm[true]}');
  final dm = <double, int>{};
  dm[w.weight] = 1;
  dm[w2.weight] = 2;
  print('${dm.length} ${dm[1.5]}');
  Countable k = Tally2(5, false);
  print('${k.count} ${k.even}');
}
