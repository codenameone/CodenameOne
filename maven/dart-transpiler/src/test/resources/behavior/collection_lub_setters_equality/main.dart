import 'dart:collection';

abstract class Shape {
  String describe();
}

class Dot extends Shape {
  final String label;
  Dot(this.label);
  @override
  String describe() => 'dot($label)';
}

class Box extends Shape {
  final List<Shape> parts;
  Box(this.parts);
  @override
  String describe() => 'box[${parts.map((p) => p.describe()).join(',')}]';
}

extension _BoxMaker on Box {
  static Box marker(String name, int i) => Box([Dot('$name$i')]);
}

mixin Bag<E> {
  Iterable<E> get items;
  int get size => items.length;
  String show() => items.join('|');
}

class Shelf<E> with Bag<E> {
  @override
  final List<E> items;
  Shelf(this.items);
}

void main() {
  final shapes = <Shape>[Dot('a')];
  final extra = [
    for (var i = 0; i < 2; i++) ...[
      Dot(' '),
      _BoxMaker.marker('m', i),
    ],
  ];
  shapes.addAll(extra);
  print(shapes.map((s) => s.describe()).join(' '));
  final last = shapes.last;
  shapes.last = Box([last, ...extra]);
  print(shapes.last.describe());
  shapes.first = Dot('z');
  print(shapes.first.describe());
  final nums = [1, 2, 3, 4];
  nums.length = 2;
  print(nums);
  nums.first = 9;
  nums.last = 8;
  print(nums);

  final ci = LinkedHashMap<String, int>(
      equals: (a, b) => a.toLowerCase() == b.toLowerCase(),
      hashCode: (k) => k.toLowerCase().hashCode,
      isValidKey: (k) => k is String);
  ci['Content-Type'] = 1;
  ci['content-type'] = 2;
  ci['Accept'] = 3;
  print(ci.length);
  print(ci['CONTENT-TYPE']);
  print(ci.containsKey('accept'));
  print(ci.keys.toList());
  print(ci.remove('ACCEPT'));
  print(ci);

  final shelf = Shelf<String>(['x', 'y']);
  print(shelf.size);
  print(shelf.show());
}
