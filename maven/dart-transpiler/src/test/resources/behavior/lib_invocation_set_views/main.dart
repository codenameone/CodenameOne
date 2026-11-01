abstract class _Base<E> implements Iterable<E> {
  Iterable<E> get _inner;
  @override
  Iterator<E> get iterator => _inner.iterator;
  @override
  int get length => _inner.length;
  dynamic noSuchMethod(Invocation i) => super.noSuchMethod(i);
}

class ValueSet<K, V> extends _Base<V> implements Set<V> {
  final Map<K, V> _map;
  ValueSet(this._map);
  @override
  Iterable<V> get _inner => _map.values;

  @override
  Set<T> cast<T>() {
    if (this is Set<T>) {
      return this as Set<T>;
    }
    return Set.castFrom<V, T>(this);
  }

  @override
  bool contains(Object? element) => _map.values.contains(element);
}

class Recorder {
  final List<String> log = [];
  @override
  dynamic noSuchMethod(Invocation i) {
    final name = i.memberName.toString();
    log.add('$name get=${i.isGetter} set=${i.isSetter} method=${i.isMethod} '
        'pos=${i.positionalArguments} named=${i.namedArguments.length}');
    if (i.memberName == #answer) return 42;
    if (i.isGetter) return 'got';
    return null;
  }
}

void main() {
  final vs = ValueSet<String, int>({'a': 1, 'b': 2});
  print(vs is Set<int>);
  final Object o = vs;
  print(o is Set);
  print(vs.contains(2));
  print(vs.cast<int>().contains(1));
  print(vs.length);

  dynamic r = Recorder();
  print(r.answer());
  print(r.colour);
  r.colour = 'red';
  r.paint(1, 'two', size: 3);
  for (final line in (r as Recorder).log) {
    print(line);
  }
  print(#answer == Symbol('answer'));
  const s = #answer;
  print(s.toString());
}
