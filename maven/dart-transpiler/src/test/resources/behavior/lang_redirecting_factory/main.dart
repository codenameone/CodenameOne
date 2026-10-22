abstract class Shape {
  const factory Shape.circle(double r) = Circle;
  const factory Shape.square({required double side}) = Square._;
  double get area;
}

class Circle implements Shape {
  final double r;
  const Circle(this.r);
  @override
  double get area => 3 * r * r;
}

class Square implements Shape {
  final double side;
  const Square._({required this.side});
  @override
  double get area => side * side;
}

class Box<T> {
  final T value;
  Box(this.value);
  factory Box.of(T v) = _Box<T>;
  String show() => 'Box($value)';
}

class _Box<T> extends Box<T> {
  _Box(T v) : super(v);
  @override
  String show() => '_Box($value)';
}

void main() {
  print(Shape.circle(2).area);
  print(Shape.square(side: 3).area);
  print(Box.of(5).show());
  print(Shape.circle(1) is Circle);
}
