class Box<T> {
  T v;
  Box(this.v);
  void set(T x) { v = x; }
  T get() => v;
}
class MyList<E> {
  final List<E> inner = <E>[];
  void add(E e) => inner.add(e);
  void addAll(List<E> es) { for (final e in es) inner.add(e); }
  int get length => inner.length;
}
class Pair<A, B> {
  A first;
  B second;
  Pair(this.first, this.second);
  void swapIn(B b, A a) { first = a; second = b; }
}
class IntBox extends Box<int> {
  IntBox(int v) : super(v);
}
void report(String label, void Function() f) {
  try {
    f();
    print('$label: no throw');
  } catch (e) {
    print('$label: TypeError ${e is TypeError}');
  }
}
void main() {
  Box<num> b = Box<int>(1);
  report('field write of double into Box<int>', () { b.v = 1.5; });
  report('method arg double into Box<int>', () { b.set(2.5); });
  report('field write of int into Box<int>', () { b.v = 7; });
  print(b.get());
  MyList<num> ml = MyList<int>();
  report('add double to MyList<int>', () { ml.add(1.5); });
  report('add int to MyList<int>', () { ml.add(3); });
  report('addAll List<double> to MyList<int>', () { ml.addAll(<num>[2.5]); });
  print(ml.length);
  Box<int> exact = Box<int>(4);
  exact.v = 5;
  exact.set(6);
  print(exact.get() + 1);
  Box<Object> bo = Box<String>('s');
  report('int into Box<String>', () { bo.set(1); });
  report('String into Box<String>', () { bo.set('t'); });
  print(bo.v);
  Pair<Object, Object> p = Pair<String, int>('a', 1);
  report('swapIn wrong order', () { p.swapIn('x', 2); });
  report('swapIn right order', () { p.swapIn(3, 'y'); });
  print('${p.first} ${p.second}');
  Box<num> ib = IntBox(1);
  report('double into IntBox', () { ib.set(0.5); });
  dynamic d = Box<int>(1);
  report('dynamic set double', () { d.set(1.5); });
  report('dynamic field double', () { d.v = 1.5; });
}
