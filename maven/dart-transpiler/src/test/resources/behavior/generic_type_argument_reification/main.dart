// Type arguments of program generic classes are reified: `is`, `as` and whereType
// check them, covariantly, with dynamic and Object? as top.

class Box<T> {
  final T value;
  Box(this.value);
  T get first => value;
}

class Pair<A, B> {
  final A a;
  final B b;
  Pair(this.a, this.b);
}

class IntBox extends Box<int> {
  IntBox(int v) : super(v);
}

class Wrapper<T> extends Box<T> {
  Wrapper(T v) : super(v);
}

class Triple<A, B, C> {
  final A a;
  final B b;
  final C c;
  Triple(this.a, this.b, this.c);
}

class Same<T> {
  final T x;
  final T y;
  Same(this.x, this.y);
}

class Named<T> {
  final T? v;
  Named({this.v});
  Named.of(T value) : v = value;
}

class Maker<T> {
  Box<T> make(T v) => Box<T>(v);
  bool holds(Object o) => o is Box<T>;
}

String check(Object o) {
  final parts = <String>[];
  parts.add('int:${o is Box<int>}');
  parts.add('num:${o is Box<num>}');
  parts.add('String:${o is Box<String>}');
  parts.add('Object:${o is Box<Object>}');
  parts.add('ObjectQ:${o is Box<Object?>}');
  parts.add('dynamic:${o is Box<dynamic>}');
  parts.add('intQ:${o is Box<int?>}');
  parts.add('notString:${o is! Box<String>}');
  return parts.join(' ');
}

void main() {
  print(check(Box<int>(1)));
  print(check(Box<String>('s')));
  print(check(Box<int?>(null)));
  print(check(Box<dynamic>(3)));
  print(check(IntBox(4)));
  print(check(Wrapper<String>('w')));
  print(check(Wrapper<double>(1.5)));
  final Box<int> typed = Box<int>(9);
  print(check(typed));

  final List<Object> mixed = [Box<int>(1), Box<String>('a'), Box<double>(2.5), IntBox(7), 'x', Wrapper<int>(8)];
  print(mixed.whereType<Box<int>>().map((b) => b.value).toList());
  print(mixed.whereType<Box<num>>().map((b) => b.value).toList());
  print(mixed.whereType<Box<String>>().map((b) => b.value).toList());
  print(mixed.whereType<Box<Object?>>().length);

  final nested = <Object>[Box<Box<int>>(Box<int>(1)), Box<Box<String>>(Box<String>('n')), Box<int>(3)];
  print(nested.map((o) => '${o is Box<Box<int>>}/${o is Box<Box<num>>}/${o is Box<Box<String>>}').toList());

  final p = Pair<int, String>(1, 'p');
  print('${p is Pair<int, String>} ${p is Pair<num, Object>} ${p is Pair<String, int>} ${p is Pair<int, int>}');

  final Object o = Box<int>(5);
  final Box<num> asNum = o as Box<num>;
  print(asNum.value);
  try {
    final s = o as Box<String>;
    print('no throw ${s.value}');
  } on TypeError {
    print('TypeError for Box<String>');
  }
  Object? none;
  print(none as Box<int>?);

  final m = Maker<String>();
  print('${m.make('q') is Box<String>} ${m.make('q') is Box<int>}');
  print('${m.holds(Box<String>('z'))} ${m.holds(Box<int>(1))}');

  // Inferred from the constructor arguments, as Dart infers them.
  print(check(Box(1)));
  print(check(Box('s')));
  print(check(Box(2.5)));
  final inferredPair = Pair(1, 'p');
  print('${inferredPair is Pair<int, String>} ${inferredPair is Pair<String, int>}');
  print('${Same(1, 2.5) is Same<num>} ${Same(1, 2.5) is Same<int>} ${Same(1, 'x') is Same<Object>} ${Same(1, 'x') is Same<int>}');
  print('${Named(v: 3) is Named<int>} ${Named(v: 3) is Named<String>} ${Named.of('n') is Named<String>} ${Named.of('n') is Named<int>}');
  // Nothing to infer from: instantiated to bounds (dynamic).
  print(check(Named()));
  // From the context type.
  final Box<num> ctxBox = Box(1);
  print(check(ctxBox));

  // Three type parameters.
  final t = Triple<int, String, double>(1, 's', 2.0);
  print('${t is Triple<int, String, double>} ${t is Triple<num, Object, num>} ${t is Triple<int, String, int>}');
  final ti = Triple(1, 's', true);
  print('${ti is Triple<int, String, bool>} ${ti is Triple<int, String, int>}');

  // `on Box<int>` checks the type argument.
  for (final thrown in <Object>[Box<int>(1), Box<String>('s'), Box(2)]) {
    try {
      throw thrown;
    } on Box<int> catch (b) {
      print('caught Box<int> ${b.value}');
    } on Box<String> catch (b) {
      print('caught Box<String> ${b.value}');
    }
  }
}
