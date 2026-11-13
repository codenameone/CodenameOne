// Untyped members take their types from the members they override.
class Point {
  final int x;
  final int y;
  Point(this.x, this.y);

  operator ==(other) => other is Point && other.x == x && other.y == y;

  get hashCode => x * 31 + y;

  toString() => 'Point($x, $y)';

  noSuchMethod(invocation) => 'nsm';
}

abstract class Shape {
  double area();
  String describe(int precision);
  num get sides;
  set label(String value);
  String get name;
}

class Square extends Shape {
  final double side;
  String _label = '';
  Square(this.side);

  area() => side * side;

  describe(precision) => '${name} ${area().toStringAsFixed(precision)} $_label';

  get sides => 4;

  set label(value) {
    _label = value.toUpperCase();
  }

  // A field overriding a getter.
  final name = 'square';
}

abstract class Sized {
  int get size;
  int scale(int by);
}

abstract class Named {
  String get title;
}

abstract class Measured {
  num measure(int factor);
}

abstract class Measured2 {
  num measure(num factor);
}

// Several supertypes: each member comes from the one interface declaring it,
// and `measure` combines both signatures (the parameter type
// that both accept).
class Box implements Sized, Named, Measured, Measured2 {
  get size => 7;
  scale(by) => size * by;
  get title => 'box';
  measure(factor) => (factor * 2).toInt();
}

abstract class Base {
  int compute(int a, [int b = 1]);
}

class Derived extends Base {
  compute(a, [b = 5]) => a * b;
}

void main() {
  final a = Point(1, 2);
  final b = Point(1, 2);
  final c = Point(2, 1);
  print(a == b);
  print(a == c);
  print(a == 'x');
  print(a.hashCode == b.hashCode);
  print(a);
  print('$c');
  final Set<Point> s = {a, b, c};
  print(s.length);

  final sq = Square(1.5);
  sq.label = 'big';
  print(sq.area());
  print(sq.describe(2));
  print(sq.sides);
  print(sq.name);
  Shape sh = sq;
  print(sh.name);
  print(sh.sides);
  print(sh.area().runtimeType);

  final box = Box();
  print(box.size + 1);
  print(box.scale(3));
  print(box.title.length);
  print(box.measure(2.5));
  Measured m = box;
  print(m.measure(3));

  print(Derived().compute(3));
  print(Derived().compute(3, 2));
}
