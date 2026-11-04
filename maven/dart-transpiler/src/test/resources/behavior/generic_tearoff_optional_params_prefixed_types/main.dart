import 'lib.dart' as p;
String gen<T>(T x, [T? y]) => '$T:$x:$y';
void main() {
  String Function(p.Box<int>, [p.Box<int>?]) a = gen;
  String Function(p.Box<int>, [p.Box<int>?]) b = gen;
  print(a == b);
  print(a(p.Box(1)));
  print(a(p.Box(1), p.Box(2)));
  String Function(p.Box<int>, [p.Box<int>?]) c = p.K.s;
  print(c(p.Box(3)));
  print(c == p.K.s<p.Box<int>>);
  var k = p.K();
  String Function(p.Box<String>, {p.Box<String>? y}) d = k.m;
  String Function(p.Box<String>, {p.Box<String>? y}) e = k.m;
  print(d(p.Box('a'), y: p.Box('b')));
  print(d == e);
  String Function(p.Box<int>, [p.Box<int>?]) f = gen<p.Box<int>>;
  print(a == f);
}
