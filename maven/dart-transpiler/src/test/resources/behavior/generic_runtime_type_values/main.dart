// runtimeType and Type values of program generic classes carry their type arguments:
// they print with them, compare equal to the matching type literal and key a map.

class Box<T> {
  final T value;
  Box(this.value);
  Type get argType => T;
  bool argIsInt() => T == int;
  bool holdsT(Object? o) => o is T;
}

class Pair<A, B> {
  final A a;
  final B b;
  Pair(this.a, this.b);
}

class IntBox extends Box<int> {
  IntBox(int v) : super(v);
}

class Plain {}

// Implemented as well as extended: checks go through the class's implicit interface.
// A generic method that only constructs a generic class: its type argument is needed at
// run time only for runtimeType, so a call that cannot name it still compiles.
class Pending<V> {
  Pending<R> map<R>(R Function() f) => Pending<R>();
  Pending<R> wrap<R>(R value) => Pending<R>();
}

class Shape<T> {
  final T size;
  Shape(this.size);
}

// An int implementation of a getter the interface types as T: the override returns a
// primitive int where the interface's getter returns the erased T.
class Square implements Shape<int> {
  @override
  int get size => 4;
}

class Badge implements Shape<String> {
  @override
  String get size => 'four';
}

class Labelled<L> implements Shape<L> {
  final L label;
  Labelled(this.label);
  @override
  L get size => label;
}

String shapeChecks(Object o) =>
    '${o is Shape<int>}/${o is Shape<num>}/${o is Shape<String>}/${o is Shape<Object?>}';

Type typeOf<T>() => T;

bool isOf<T>(Object? o) => o is T;

void main() {
  final bi = Box<int>(1);
  final bs = Box<String>('s');
  print(bi.runtimeType);
  print(bs.runtimeType);
  print(Pair<int, String>(1, 'a').runtimeType);
  print(Box<Box<double>>(Box<double>(1.5)).runtimeType);
  print(Box<int?>(null).runtimeType);
  print(IntBox(2).runtimeType);
  print(Plain().runtimeType);
  print(bi.runtimeType == Box<int>);
  print(bi.runtimeType == Box<String>);
  print(bs.runtimeType == Box<String>);
  print(bi.runtimeType == Box<int>(5).runtimeType);
  print(bi.runtimeType == bs.runtimeType);
  print(IntBox(3).runtimeType == IntBox);
  print(Plain().runtimeType == Plain);
  print(Box<int>);
  print(Pair<String, bool>);

  final names = <Type, String>{};
  names[Box<int>] = 'ints';
  names[Box<String>] = 'strings';
  names[Plain] = 'plain';
  print(names[Box<int>(9).runtimeType]);
  print(names[Box<String>('x').runtimeType]);
  print(names[Plain().runtimeType]);
  print(names[Box<double>(1.0).runtimeType]);
  print(names.length);

  print(bi.argType);
  print(bs.argType);
  print(bi.argIsInt());
  print(bs.argIsInt());
  print(bi.holdsT(3));
  print(bi.holdsT('3'));
  print(bs.holdsT('3'));
  print(typeOf<int>());
  print(typeOf<String>());
  print(typeOf<int>() == int);
  print(isOf<int>(1));
  print(isOf<int>('1'));
  print(isOf<String>('1'));

  final shapes = <Object>[Shape<int>(1), Shape<String>('s'), Square(), Badge(), Labelled<String>('l'), Labelled<double>(2.0)];
  for (final o in shapes) {
    print(shapeChecks(o));
  }
  print(shapes.whereType<Shape<int>>().length);
  print(shapes.whereType<Shape<String>>().length);
  print(Shape<int>(1).runtimeType);
  final Shape<int> sq = Square();
  print(sq.size + 1);
  print(Square().size * 2);
  print(Labelled<String>('x').runtimeType == Labelled<String>);
  final pv = Pending<int>();
  print(pv.map(() => 'text').runtimeType);
  print(pv.map<double>(() => 1.5).runtimeType);
  print(pv.wrap(7).runtimeType);
}
