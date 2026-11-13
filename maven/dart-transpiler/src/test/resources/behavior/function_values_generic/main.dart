T identity<T>(T x) => x;

List<E> applyAll<E>(List<E> xs, E Function(E) f) => [for (final x in xs) f(x)];

class Box<T> {
  final T value;
  Box(this.value);
  R map<R>(R Function(T) f) => f(value);
  static Box<int> ofInt(int v) => Box<int>(v);
}

class Cell<T> {
  T v;
  Cell(this.v);
  @override
  String toString() => 'Cell($v)';
}

typedef Handler = void Function([dynamic, dynamic]);
typedef Named = String Function(String a, {int count});

class Config {
  static void _noOp([_, __]) {}
  final Handler onEvent;
  Config({this.onEvent = _noOp});
}

void log2([a, b]) {
  print('log2 $a $b');
}

String rep(String a, {int count = 1}) => a * count;

List<E> sortWith<E>(List<E> xs, int Function(E, E) cmp, E Function(E) key) {
  final c = [...xs];
  c.sort((a, b) => cmp(key(a), key(b)));
  return c;
}

List<E> sortedIds<E extends Comparable<E>>(List<E> xs) =>
    sortWith(xs, (E a, E b) => a.compareTo(b), identity<E>);

void main() {
  int Function(int) idInt = identity<int>;
  print(idInt(3));
  final idS = identity<String>;
  print(idS('x'));
  print(applyAll(<int>[1, 2, 3], identity<int>));
  print(sortedIds(<int>[3, 1, 2]));
  final b = Box<int>(5);
  String Function(String Function(int)) m = b.map<String>;
  print(m((v) => 'v=$v'));
  final mk = Cell<int>.new;
  print(mk(7));
  print(Cell.new(8));
  final ofInt = Box.ofInt;
  print(ofInt(9).value);

  final c = Config();
  c.onEvent();
  c.onEvent(1);
  c.onEvent(1, 2);
  final c2 = Config(onEvent: log2);
  c2.onEvent();
  c2.onEvent('a');
  c2.onEvent('a', 'b');
  Handler h = log2;
  h(1);
  Named n = rep;
  print(n('ab', count: 3));
  print(n('z'));
  final r = ();
  print(r);
  () e = ();
  print(e == r);
  print('done');
}
