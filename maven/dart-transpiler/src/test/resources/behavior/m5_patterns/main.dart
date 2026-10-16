sealed class Shape {}

class Circle extends Shape {
  final double radius;
  Circle(this.radius);
}

class Square extends Shape {
  final double side;
  Square(this.side);
}

class Rect extends Shape {
  final double w;
  final double h;
  Rect(this.w, this.h);
}

double area(Shape s) {
  return switch (s) {
    Circle(radius: var r) => 3.14 * r * r,
    Square(side: var x) => x * x,
    Rect(w: var w, h: var h) => w * h,
  };
}

String describe(int n) {
  switch (n) {
    case 0:
      return 'zero';
    case 1:
    case 2:
      return 'small';
    default:
      return 'many';
  }
}

String sign(int n) {
  return switch (n) {
    < 0 => 'negative',
    0 => 'zero',
    _ => 'positive',
  };
}

String grade(int score) {
  return switch (score) {
    >= 90 => 'A',
    >= 80 => 'B',
    int x when x >= 70 => 'C',
    _ => 'F',
  };
}

String classify(Object v) {
  return switch (v) {
    (0, 0) => 'origin',
    (var x, var y) => 'point $x,$y',
    _ => 'other',
  };
}

void main() {
  print(area(Circle(2.0)));
  print(area(Square(3.0)));
  print(area(Rect(2.0, 5.0)));
  print(describe(0));
  print(describe(1));
  print(describe(2));
  print(describe(9));
  print(sign(-3));
  print(sign(0));
  print(sign(7));
  print(grade(95));
  print(grade(85));
  print(grade(72));
  print(grade(50));
  var p = (3, 4);
  print(p.$1);
  print(p.$2);
  var named = (x: 10, y: 20);
  print(named.x);
  print(named.y);
  print(classify((0, 0)));
  print(classify((5, 6)));
  print(classify('hi'));
}
