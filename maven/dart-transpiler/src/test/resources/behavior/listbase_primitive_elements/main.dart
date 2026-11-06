import 'dart:collection';

mixin Tagged<E> on ListBase<E> {
  String tag() => 'tagged:$length';
}

class Doubling extends ListBase<int> with Tagged<int> {
  final List<int> _b = [];
  int get length => _b.length;
  set length(int n) => _b.length = n;
  int operator [](int i) => _b[i];
  void operator []=(int i, int v) => _b[i] = v;
  @override
  void add(int v) => _b.add(v * 2);
}

class Halves extends ListBase<double> {
  final List<double> _b = [];
  int get length => _b.length;
  set length(int n) {
    while (_b.length < n) _b.add(0.0);
    while (_b.length > n) _b.removeLast();
  }
  double operator [](int i) => _b[i];
  void operator []=(int i, double v) => _b[i] = v / 2;
}

class Counts with ListMixin<int> {
  final List<int> _b = [10, 20];
  int get length => _b.length;
  set length(int n) {
    while (_b.length < n) _b.add(0);
    while (_b.length > n) _b.removeLast();
  }
  int operator [](int i) => _b[i];
  void operator []=(int i, int v) => _b[i] = v + 1;
}

int sumOf(List<int> xs) => xs.fold(0, (p, e) => p + e);

void main() {
  var d = Doubling();
  d.add(3);
  d.addAll([4, 5]);
  print(d);
  print(d.length);
  print(d[1] + 1);
  d[0] = 7;
  print(d.first);
  print(d.map((x) => x + 1).toList());
  print(d.tag());
  print(sumOf(d));
  d.length = 1;
  print(d);

  var h = Halves();
  h.length = 2;
  h[0] = 3.0;
  h[1] = 5.0;
  h.add(9.0);
  print(h);
  print(h.fold<double>(0.0, (a, b) => a + b));
  print(h.where((x) => x > 1.6).toList());

  var c = Counts();
  c[1] = 40;
  c.add(5);
  print(c);
  print(c.contains(41));
  print(c.indexOf(6));
  List<int> asList = c;
  print(asList.reversed.toList());
  print(c.join('-'));
}
