import 'dart:collection';

abstract class Shape {
  int get size;
  String get label;
}

mixin ShapeMixin implements Shape {
  Shape bigger(Shape other) {
    final pick = size >= other.size ? this : other;
    return pick;
  }

  String describe(Shape other) {
    final pick = size >= other.size ? this : other;
    return 'bigger of $this and $other is ${pick.label} (${pick.size})';
  }
}

class Square with ShapeMixin {
  final int side;
  Square(this.side);
  @override
  int get size => side * side;
  @override
  String get label => 'square$side';
  @override
  String toString() => 'Square($side)';
}

class Dot implements Shape {
  @override
  int get size => 1;
  @override
  String get label => 'dot';
  @override
  String toString() => 'Dot';
}

extension Shout on String {
  String shout() => '$this!';
}

class Named {
  final String name;
  Named(this.name);
  String greet() => 'hi $name';
}

String greetAll<N extends Named>(List<N> people) =>
    people.map((p) => p.greet()).join(', ');

int totalLength<S extends Named>(S a, S b) => a.name.length + b.name.length;

class Holder<T> {
  final T value;
  Holder(this.value);
  String show<R extends Named>(R extra) => '${extra.name}:$value';
}

int weight(Shape s) {
  if (s is! Square) return s.size;
  return s.side * 10;
}

String kind(Object o) {
  if (o is! Dot) {
    return 'other';
  } else {
    return o.label;
  }
}

bool sameSide(Square a, Object other) {
  if (other is! Shape) return false;
  if (other is! Square) {
    return a.size == other.size;
  }
  return a.side == other.side;
}

class Ticket {
  final int n;
  Ticket(this.n);
  String stamp(String who) => '$who#$n';
}

class Line<E> extends Object with ListMixin<E> {
  final List<E> _items = [];
  @override
  int get length => _items.length;
  @override
  set length(int v) => _items.length = v;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
  @override
  void add(E v) => _items.add(v);
}

String front(Line<Ticket> line) => line.first.stamp('front') + ' ' + line.last.stamp('back');

void main() {
  final line = Line<Ticket>()..add(Ticket(1))..add(Ticket(2));
  print(front(line));
  print(weight(Dot()));
  print(weight(Square(4)));
  print(kind(Dot()));
  print(kind(3));
  print(sameSide(Square(2), Square(2)));
  print(sameSide(Square(1), Dot()));
  print(sameSide(Square(1), 'x'));
  final s = Square(3);
  print(s.describe(Dot()));
  print(Square(1).describe(Square(2)));
  print(s.bigger(Dot()).label);
  print('wow'.shout());
  print(greetAll([Named('ann'), Named('bo')]));
  print(totalLength(Named('abc'), Named('de')));
  print(Holder(7).show(Named('x')));
}
