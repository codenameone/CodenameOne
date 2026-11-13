// Type parameters used as values: `x is T`, `T == int`, `Type get t => T`, `null is T`.

class Holder<T> {
  final List<Object?> seen = [];
  Holder();
  Holder.named(Object? first) {
    seen.add(first);
  }

  Type get valueType => T;

  bool accepts(Object? v) => v is T;

  bool get nullable => null is T;

  bool sameAs(Type other) => T == other;
}

class Plain<V> {
  int count = 0;
  bool isText() => V == String;
}

class Sub<S> extends Holder<S> {
  Sub() : super();
  bool both(Object? v) => accepts(v) && v is S;
}

class Fixed extends Holder<int> {}

class Checked<V> {
  final String label;
  Checked(this.label) {
    if (!(V is Object)) {
      throw StateError('never');
    }
  }
  String describe(Object? v) => '$label:${v is V}';
}

class Pair<A, B> {
  final A first;
  Pair(this.first);
  bool secondMatches(Object? v) => v is B;
}

class Built<E> {
  final int n;
  Built._(this.n);
  factory Built.make(int n) => Built<E>._(n * 2);
  bool test(Object? v) => v is E;
}

void main() {
  final h = Holder<String>();
  print(h.valueType == String);
  print(h.accepts('a'));
  print(h.accepts(3));
  print(h.nullable);
  print(Holder<int?>().nullable);
  print(Holder<int?>().accepts(null));
  print(Holder<int>().accepts(4));
  print(Holder<int>().sameAs(int));
  print(Holder<num>().accepts(2.5));
  print(Holder<Object?>().accepts(null));
  print(Holder<Object>().accepts(null));
  print(Holder<List<int>>().accepts(<int>[1]));
  print(Holder<String>.named(1).seen);
  print(Holder<double>.named(null).accepts(1.5));
  print(Plain<String>().isText());
  print(Plain<int>().isText());
  final s = Sub<int>();
  print(s.both(1));
  print(s.both('x'));
  print(s.sameAs(int));
  print(Fixed().accepts(7));
  print(Fixed().accepts('7'));
  print(Checked<int>('c').describe(1));
  print(Checked<String>('d').describe(1));
  print(Pair<int, String>(1).secondMatches('s'));
  print(Pair<int, String>(1).secondMatches(1));
  final b = Built<String>.make(3);
  print(b.n);
  print(b.test('q'));
  print(b.test(1));
  final Holder<int> inferred = Holder();
  print(inferred.accepts(1));
  print(inferred.accepts('1'));
}
