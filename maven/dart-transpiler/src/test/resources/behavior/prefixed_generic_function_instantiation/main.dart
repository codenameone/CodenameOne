import 'lib.dart' as p;
void main() {
  var f = p.gen<int>;
  print(f(1));
  print(f(1, 2));
  print(f == p.gen<int>);
  print(identical(f, p.gen<int>));
  String Function(int, [int?]) g = p.gen;
  print(g == f);
  print(f == p.gen<String>);
  var h = p.gen2<String>;
  print(h('a'));
  print(h == p.gen2<String>);
  print(p.tn<p.Box<int>>());
  print(p.tn<p.Box<p.Box<String>>>());
  print(p.tn<p.Inner<int, List<p.Box<int>>>>());
  print(p.Outer<int>().mk().runtimeType);
  print(p.tn<p.Box<int>?>());
}
