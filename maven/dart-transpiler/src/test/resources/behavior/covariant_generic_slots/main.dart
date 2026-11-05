import 'dart:async';
import 'dart:collection';
import 'dart:typed_data';

// List.generate typed by its generator's result.
List<Uint32List> makeRows(int n) {
  var rows = List.generate(n, (_) => Uint32List(2));
  for (var j = 0; j < n; j++) {
    rows[j][1] = j * 3;
  }
  return rows;
}

// `??` defaulting an Iterable to a list literal.
bool offers(Iterable<String>? names, String n) => (names ?? []).contains(n);

// Covariant arguments into slots typed by dynamic and by a class type variable.
abstract class Store<E> {
  final Map<dynamic, E> items = {};
  void putAll(Map<dynamic, E> entries) => items.addAll(entries);
  void removeAll(Iterable<dynamic> keys) {
    for (var k in keys) {
      items.remove(k);
    }
  }

  void addValues(Iterable<E> values) {
    var entries = <int, E>{};
    var i = 0;
    for (var v in values) {
      entries[i++] = v;
    }
    putAll(entries);
  }
}

class NameStore extends Store<String> {}

// Iterating a raw, bound-instantiated program list.
class Tag {
  final String label;
  Tag(this.label);
}

class TagList<E extends Tag> with ListMixin<E> {
  final List<E> _inner = [];
  @override
  int get length => _inner.length;
  @override
  set length(int v) => _inner.length = v;
  @override
  E operator [](int i) => _inner[i];
  @override
  void operator []=(int i, E v) => _inner[i] = v;
  @override
  void add(E v) => _inner.add(v);
}

String joinTags(TagList list) {
  var out = <String>[];
  for (var t in list) {
    out.add(t.label);
  }
  return out.join(',');
}

// A Never getter implementing an untyped, initialized field.
class Registry {
  final _byId = <int, String>{};
  void register(int id, String s) => _byId[id] = s;
  String? find(int id) => _byId[id];
}

class NullRegistry implements Registry {
  @override
  Never get _byId => throw UnsupportedError('none');
  @override
  void register(int id, String s) {}
  @override
  String? find(int id) => null;
}

// A factory tear-off as a FutureOr-returning callback.
class Wrapped {
  final int v;
  Wrapped(this.v);
  factory Wrapped.doubled(int v) => Wrapped(v * 2);
}

FutureOr<Wrapped> wrap(FutureOr<int> x) =>
    x is Future<int> ? x.then(Wrapped.doubled) as FutureOr<Wrapped> : Wrapped.doubled(x);

Future<void> main() async {
  var rows = makeRows(3);
  print('${rows.length} ${rows[2][1]} ${rows[0].runtimeType}');
  print('${offers(null, "a")} ${offers(["a", "b"], "b")}');
  var store = NameStore();
  store.addValues(['x', 'y', 'z']);
  print(store.items);
  store.removeAll(<int>[0, 2]);
  print(store.items);
  var tags = TagList<Tag>();
  tags.add(Tag('one'));
  tags.add(Tag('two'));
  print(joinTags(tags));
  Registry r = NullRegistry();
  r.register(1, 'a');
  print(r.find(1));
  var real = Registry();
  real.register(2, 'b');
  print(real.find(2));
  print((await wrap(Future.value(4))).v);
  print((await wrap(5)).v);
}
