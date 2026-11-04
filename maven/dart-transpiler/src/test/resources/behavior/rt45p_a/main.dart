extension type Id(int v) {
  Id.zero() : v = 0;
  factory Id.of(int x) = Id;
  int get twice => v * 2;
  Id operator +(Id o) => Id(v + o.v);
  bool isBig() => v > 10;
}
extension type const Name(String s) implements Comparable<String> {
  int get len => s.length;
}
extension type Pair((int, String) r) {
  int get a => r.$1;
}
extension type Fn(int Function(int) f) {
  int call2(int x) => f(f(x));
}
extension type Opt(Object? o) {
  bool get isNull => o == null;
}
extension type MaybeInt(int? v) {
  int orZero() => v ?? 0;
}
abstract class Shape { double area(); }
class Sq implements Shape { final double s; Sq(this.s); double area() => s * s; }
extension type ShapeView(Sq sq) implements Shape {
  double doubled() => sq.area() * 2;
}
extension Neg on Id { Id neg() => Id(-v); }
double total(Shape s) => s.area();
Future<Id> later(int x) async { await null; return Id(x); }
String describe(Id id) => switch (id) { Id(v: 0) => 'zero', Id(v: var n) when n > 5 => 'big $n', _ => 'small' };
void main() async {
  var a = Id(3);
  var b = Id.of(4);
  print(a.twice);
  print((a + b).v);
  print(a.isBig());
  print(Id.zero().v);
  print(a.neg().v);
  print(a);
  print('val=$a');
  print(a == Id(3));
  print(a.hashCode == 3.hashCode);
  var s = <Id>{Id(1), Id(1), Id(2)};
  print(s.length);
  var m = <Id, String>{Id(5): 'five'};
  print(m[Id(5)]);
  const n = Name('hey');
  print(n.len);
  print(n.compareTo('a') > 0);
  print(Pair((7, 'x')).a);
  print(Fn((x) => x + 1).call2(1));
  print(Opt(null).isNull);
  print(Opt(1).isNull);
  print(MaybeInt(null).orZero());
  var sv = ShapeView(Sq(2));
  print(total(sv));
  print(sv.doubled());
  print((await later(9)).twice);
  print(describe(Id(0)));
  print(describe(Id(8)));
  print(describe(Id(2)));
  Object? o = a;
  print(o is int);
  print(o is Id);
  List<Id> ids = [Id(1), Id(2)];
  print(ids.map((e) => e.twice).toList());
  var c = a;
  c += Id(10);
  print(c.v);
}
