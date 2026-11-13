abstract class Eq<E> {
  bool equals(E a, E b);
  int hash(E e);
}

class AnyEq<E> implements Eq<E> {
  const AnyEq();
  @override
  bool equals(Object? a, Object? b) => a == b;
  @override
  int hash(Object? e) => e.hashCode & 7;
}

abstract class Sink2<T> {
  void write(T value, [bool flag = false]);
}

class Ignoring implements Sink2<Object?> {
  int count = 0;
  @override
  void write(Object? value, [bool flag = false]) => count++;
}

abstract class Store<K, V> {
  void putAll(Map<K, V> entries);
  Future<Store<K, V>> open<X>(String name);
}

class MapStore<K, V> implements Store<K, V> {
  final Map<K, V> data = {};
  @override
  void putAll(Map<dynamic, dynamic> entries) {
    entries.forEach((k, v) => data[k as K] = v as V);
  }

  @override
  Future<MapStore<K, V>> open<X>(String name) async => this;
}

Future<void> main() async {
  Eq<String> e = const AnyEq<String>();
  print(e.equals('a', 'a'));
  print(e.equals('a', 'b'));
  print(const AnyEq<int>().equals(1, 1));
  print(e.hash('x') >= 0);
  final s = Ignoring();
  Sink2<Object?> sk = s;
  sk.write(1);
  s.write('x', true);
  print(s.count);
  Store<String, int> st = MapStore<String, int>();
  st.putAll({'a': 1, 'b': 2});
  print((st as MapStore<String, int>).data);
  final o = await st.open<int>('n');
  print(identical(o, st));
}
