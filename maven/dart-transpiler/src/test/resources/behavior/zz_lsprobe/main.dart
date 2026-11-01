import 'dart:collection';

class KeyedSet<T> extends SetBase<T> {
  final Map<String, T> _byKey = {};
  final String Function(T) keyOf;
  KeyedSet(this.keyOf);

  @override
  bool add(T value) {
    final k = keyOf(value);
    if (_byKey.containsKey(k)) return false;
    _byKey[k] = value;
    return true;
  }

  @override
  bool contains(Object? element) => element is T && _byKey.containsKey(keyOf(element));
  @override
  T? lookup(Object? element) => element is T ? _byKey[keyOf(element)] : null;
  @override
  bool remove(Object? value) => value is T && _byKey.remove(keyOf(value)) != null;
  @override
  Iterator<T> get iterator => _byKey.values.iterator;
  @override
  int get length => _byKey.length;
  @override
  Set<T> toSet() => Set<T>.of(this);

  bool selfIsSet() => this is Set;
  Set<T> asSet() => this as Set<T>;
}

int countSet<E>(Set<E> s) => s.length;

String describe(Object o) {
  if (o is Set) {
    return 'set of ${o.length}';
  }
  return 'other';
}

void main() {
  final ks = KeyedSet<String>((s) => s.toLowerCase());
  ks.add('Apple');
  ks.add('apple');
  ks.add('Pear');
  print(ks.length);
  print(ks.selfIsSet());
  final Set<String> s = ks.asSet();
  print(s.contains('PEAR'));
  s.add('plum');
  print(ks.length);
  print(countSet(ks));
  print(describe(ks));
  final Object o = ks;
  print(o is Set<String>);
  print(o is KeyedSet);
  final Set<String> other = {'pear', 'fig'};
  print(other.intersection(ks).toList());
  print(ks.toList());
}
