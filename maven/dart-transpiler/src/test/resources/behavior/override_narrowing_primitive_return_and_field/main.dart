abstract class Shape {
  num get area;
  final num h = 0;
  num scale(num f) => f;
}
class Square extends Shape {
  final int side;
  Square(this.side);
  @override
  int get area => side * side;
  @override
  final int h = 3;
  @override
  int scale(num f) => (side * f).round();
  int doubled() => area * 2 + h;
}
class Cube extends Square {
  Cube(int s) : super(s);
  int get volume => area * side + h;
}
class Pt {
  num get x => 1.5;
  double get y => 2.5;
}
class IntPt extends Pt {
  @override
  int get x => 4;
}
void main() {
  Square s = Square(3);
  int a = s.area;
  print(a + 1);
  print(s.h * 2);
  print(s.scale(1.5));
  print(s.doubled());
  Shape sh = s;
  print(sh.area);
  print(sh.h);
  print(sh.scale(2));
  print(sh.area is int);
  Pt p = IntPt();
  print(p.x);
  print(p.x is int);
  IntPt ip = IntPt();
  int xi = ip.x;
  print(xi + ip.y);
  print(Pt().x);
  Cube c = Cube(2);
  int hc = c.h;
  print(c.volume + hc + c.area);
}
