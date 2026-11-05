class Box<E> { final E v; Box(this.v); }
String sh<T>(T x) => '$T';
String gen<T>(T x, [T? y]) => '$T:$x:$y';
String wrap<T>(T? x) => sh(x);
void main() {
  int? a = null; String? b = 'x'; Box<int>? c = Box(1); Object? d;
  print(sh(a)); print(sh(b)); print(sh(c)); print(sh(d));
  print(sh<int?>(3));
  print(wrap<int>(3));
  print(wrap<String?>(null));
  String Function(int?, [int?]) f = gen;
  print(f(null));
  String Function(Box<int>?, [Box<int>?]) g = gen;
  print(g(null));
  String Function(Object?, [Object?]) o = gen;
  print(o(null));
  dynamic dy = 1;
  print(sh(dy));
  List<int?> li = [1];
  print(sh(li));
  print(sh(li.first));
}
