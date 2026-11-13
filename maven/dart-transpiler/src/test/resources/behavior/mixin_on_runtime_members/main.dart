import 'dart:collection';

// `super` from a mixin with an `on` clause reaches the superclass.
class Greeter {
  String greet(String who) => 'hello $who';
  int get weight => 3;
}

mixin Loud on Greeter {
  @override
  String greet(String who) => super.greet(who).toUpperCase() + '!';
  @override
  int get weight => super.weight * 2;
}

class LoudGreeter extends Greeter with Loud {}

// The last mixin applied wins over both the superclass and an earlier mixin.
mixin Polite {
  String greet(String who) => 'dear $who';
}

class PoliteGreeter extends Greeter with Loud, Polite {}

// A mixin on Iterable uses inherited members and iterates `this`.
abstract class Item {
  String get name;
}

class Named implements Item {
  final String name;
  Named(this.name);
}

mixin ItemCollection<E extends Item> on Iterable<E> {
  String firstName() => first.name;
  String lastName() => last.name;
  List<String> names() {
    final out = <String>[];
    for (final e in this) {
      out.add(e.name);
    }
    return out;
  }

  Map<String, E> byName() {
    final map = <String, E>{};
    for (final e in this) {
      map[e.name] = e;
    }
    return map;
  }
}

class ItemList extends ListBase<Named> with ItemCollection<Named> {
  final List<Named> _items = [];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  Named operator [](int i) => _items[i];
  @override
  void operator []=(int i, Named v) => _items[i] = v;
  @override
  void add(Named v) => _items.add(v);
}

// Keyword-named lambda parameters.
int countUpper(String s) =>
    s.split('').where((char) => char.toUpperCase() == char).length;

// Flow promotion sees the original object, not a view.
class Bag extends SetBase<int> {
  final Set<int> _s = {1, 2};
  @override
  bool add(int v) => _s.add(v);
  @override
  bool contains(Object? v) => _s.contains(v);
  @override
  Iterator<int> get iterator => _s.iterator;
  @override
  int get length => _s.length;
  @override
  int? lookup(Object? v) => _s.lookup(v);
  @override
  bool remove(Object? v) => _s.remove(v);
  @override
  Set<int> toSet() => Set.of(_s);
  String label() => 'bag$length';
}

String describe(Set<int> s) {
  if (s is Bag) {
    return s.label();
  }
  return switch (s) { Bag b => b.label(), _ => 'set' };
}

void main() {
  final g = LoudGreeter();
  print(g.greet('x'));
  print(g.weight);
  final Greeter pg = PoliteGreeter();
  print(pg.greet('y'));
  print(pg.weight);
  final items = ItemList()
    ..add(Named('a'))
    ..add(Named('b'));
  print(items.firstName());
  print(items.lastName());
  print(items.names());
  print(items.byName().keys.toList());
  print(countUpper('AbC'));
  print(describe(Bag()));
  print(describe({1}));
}
