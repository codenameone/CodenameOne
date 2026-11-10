import 'dart:collection';

// `super.x` in a class whose only supertypes are mixins: Dart's linearization
// sends it to the last mixin that declares the member.
mixin Base {
  String greet(String who) => 'base $who';
  int get size => 1;
  int operator [](int i) => i * 10;
}

mixin Counted {
  int get size => 2;
  String tag() => 'counted';
}

class Plain with Base, Counted {
  @override
  String greet(String who) => 'plain ' + super.greet(who);
  @override
  int get size => super.size + 100;
  @override
  int operator [](int i) => super[i] + 1;
  @override
  String tag() => super.tag().toUpperCase();
}

void main() {
  final p = Plain();
  print(p.greet('a'));
  print(p.size);
  print(p[4]);
  print(p.tag());
  bagMain();
}

class Bag with ListMixin<int> {
  final List<int> _items = [1, 2, 3, 4];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  int operator [](int i) => _items[i];
  @override
  void operator []=(int i, int v) => _items[i] = v;
  @override
  bool contains(Object? o) => super.contains(o) || o == 0;
}

void bagMain() {
  final b = Bag();
  b.setRange(1, 3, [9, 8]);
  print(b.toList());
  print('${b.contains(0)} ${b.contains(9)} ${b.contains(7)}');
}
