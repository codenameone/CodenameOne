// Dart's implicit interfaces: every class declares one, and a class can
// implement any number of them, whatever its own superclass is.

class Named {
  String label = 'named';
  String describe() => 'Named($label)';
}

class Counted {
  int count() => 0;
  int get total => 10;
}

class Base {
  final String tag;
  Base(this.tag);
  String who() => 'base:$tag';
}

// a superclass of its own plus two implemented concrete classes
class Both extends Base implements Named, Counted {
  Both() : super('both');
  @override
  String label = 'both-label';
  @override
  String describe() => 'Both($label, ${who()})';
  @override
  int count() => 2;
  @override
  int get total => 20;
}

// a getter overriding the implemented class's field
class Fixed implements Named {
  @override
  String get label => 'fixed';
  @override
  set label(String v) {
    print('ignored $v');
  }
  @override
  String describe() => 'Fixed($label)';
}

abstract class Shape {
  double area();
  String get kind;
}

// a mixin implementing an abstract class is assignable to it
mixin Square implements Shape {
  double get side;
  @override
  double area() => side * side;
  @override
  String get kind => 'square';
}

class Tile with Square {
  @override
  final double side;
  Tile(this.side);
}

// generic mixin applied with a primitive type argument
mixin Holder<T> {
  List<T> items = [];
  void put(T v) => items.add(v);
}

class IntBag with Holder<int> {
  int sum() => items.fold(0, (a, b) => a + b);
}

// a QueueList-like shape: a superclass plus an implemented generic class
class Seq<E> {
  final List<E> _data = [];
  void add(E e) => _data.add(e);
  E at(int i) => _data[i];
  int get size => _data.length;
}

class Store {
  String name() => 'store';
}

class Ring<E> extends Store implements Seq<E> {
  final List<E> _ring = [];
  @override
  List<E> get _data => _ring;
  @override
  void add(E e) => _ring.insert(0, e);
  @override
  E at(int i) => _ring[i];
  @override
  int get size => _ring.length;
}

String show(Named n) => '${n.describe()} label=${n.label}';
int tally(Counted c) => c.count() + c.total;

void main() {
  final both = Both();
  print(show(both));
  print(tally(both));
  print(both is Named);
  print(both is Base);
  final Named n = Fixed();
  print(show(n));
  n.label = 'x';
  print(show(Named()));
  print(tally(Counted()));
  final List<Named> all = [Named(), both, n];
  for (final x in all) {
    print(x.label);
  }
  Shape s = Tile(3);
  print('${s.kind} ${s.area()}');
  print(s is Square);
  final bag = IntBag();
  bag.put(4);
  bag.put(5);
  bag.items = [1, 2, 3];
  print(bag.sum());
  Seq<int> q = Ring<int>();
  q.add(1);
  q.add(2);
  print('${q.at(0)} ${q.size} ${(q as Store).name()}');
  final plain = Seq<String>();
  plain.add('a');
  print('${plain.at(0)} ${plain.size}');
  print(q is Seq<int>);
}
