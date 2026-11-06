abstract class Store<V> {
  Iterable<dynamic> get keys;
}

class Shelf<E> implements Store<E> {
  final Map<dynamic, E> items = {};
  Iterable<dynamic> get keys => items.keys;
  Future<E?> fetch(dynamic key, {E? fallback}) async => items[key] ?? fallback;
}

class Wrapper<V> {
  final Store<V> store;
  Wrapper(this.store);

  Future<List<V>> all() async {
    final s = store;
    final ks = s.keys.toList();
    if (s is Shelf<V>) {
      final vs = await Future.wait(ks.map(s.fetch));
      return [for (final v in vs) v as V];
    }
    return [];
  }

  List<String> names() {
    return store.keys
        .cast<String>()
        .map((key) {
          if (key.startsWith('_')) {
            key = key.substring(1);
          }
          return key;
        })
        .map((k) => k.toUpperCase())
        .toList();
  }
}

class Bag<E> {
  final List<E> inner = [];
  int get length => inner.length;
}

mixin Counted<E> {
  Map<String, E> table = {};
  Map<String, E> copy() => Map<String, E>.from(table);
}

class Tally<E> with Counted<E> {}

void main() async {
  final shelf = Shelf<int>();
  shelf.items['a'] = 1;
  shelf.items['_b'] = 2;
  final w = Wrapper<int>(shelf);
  print(await w.all());
  print(w.names());
  final bag = Bag<String>()..inner.addAll(['x', 'y']);
  print(bag.length);
  final t = Tally<int>()..table['q'] = 7;
  print(t.copy());
}
