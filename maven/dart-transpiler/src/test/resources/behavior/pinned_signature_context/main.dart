import 'dart:collection';

// Named parameters of a generic method whose types differ only by the
// implementer's own (covariant) classes.
abstract class Shelf {
  Future<Crate<V>> open<V>(String name,
      {bool warm = false, Crate<V> Function(String, Shelf)? maker});
}

abstract class Crate<V> {
  String get label;
}

class PlainCrate<V> implements Crate<V> {
  final String label;
  PlainCrate(this.label);
}

class Store implements Shelf {
  @override
  Future<PlainCrate<V>> open<V>(String name,
      {bool warm = false, Crate<V> Function(String, Store)? maker}) async {
    if (maker == null) return PlainCrate<V>('$name/$warm');
    final made = maker(name, this);
    return PlainCrate<V>(made.label);
  }
}

// A raw parameter implementing a member specialized to Never.
abstract class Outcome<T> {
  void deliver(List<T> sink);
}

class Failure implements Outcome<Never> {
  final Object error;
  Failure(this.error);
  @override
  void deliver(List sink) => sink.add('failed: $error');
}

// A raw superclass reference.
class Holder<T> {
  final List<T> items = [];
  void put(T t) => items.add(t);
}

class LooseHolder extends Holder {
  String describe(Map<String, Object> extra) => '${items.length} ${extra.keys}';
}

// Field typing from the declaration and the initializer list.
class Headers {
  final Map<String, String> headers;
  final Map<String, String> params;
  final Queue<List<int>> pending = ListQueue();
  Headers(Map<String, String> p)
      : headers = LinkedHashMap(
            equals: (a, b) => a.toLowerCase() == b.toLowerCase(),
            hashCode: (k) => k.toLowerCase().hashCode),
        params = UnmodifiableMapView(Map.from(p));
}

// Arguments to a private named constructor.
class Parts {
  final List<String> parts;
  final List<String> seps;
  Parts._(this.parts, this.seps);
  factory Parts(String s) => Parts._(s.split('/'), ['/']);
  Parts clone() => Parts._(List.from(parts), List.from(seps));
}

// A typed map literal inside a mixin.
mixin Keyed<E> {
  Iterable<E> get all;
  Map<dynamic, E> toMap() {
    var map = <dynamic, E>{};
    var i = 0;
    for (var e in all) {
      map[i++] = e;
    }
    return map;
  }
}

class Bag<E> with Keyed<E> {
  final Iterable<E> all;
  Bag(this.all);
}

// List literals of mixed subtypes.
abstract class Node {
  String get text;
}

class TextNode implements Node {
  final String text;
  TextNode(this.text);
}

class Elem implements Node {
  final List<Node> children;
  Elem(this.children);
  String get text => children.map((c) => c.text).join();
}

Elem wrap(List<TextNode> refs, bool more) =>
    Elem([TextNode('<'), ...refs, if (more) Elem([TextNode('+')]), for (var r in refs) Elem([r])]);

void main() async {
  final c = await Store().open<int>('box', warm: true);
  print(c.label);
  final Shelf s = Store();
  print((await s.open<String>('two', maker: (n, sh) => PlainCrate<String>('made $n'))).label);

  final sink = [];
  Failure('boom').deliver(sink);
  final Outcome<Never> o = Failure('bang');
  (o as dynamic).deliver(sink);
  print(sink);

  final h = LooseHolder();
  h.put(1);
  h.put('x');
  print(h.describe({'a': 1}));

  final hd = Headers({'Content-Type': 'text'});
  hd.headers['ACCEPT'] = 'json';
  print('${hd.headers.length} ${hd.params} ${hd.pending.length}');

  final p = Parts('a/b').clone();
  print('${p.parts} ${p.seps}');

  print(Bag<String>(['x', 'y']).toMap());
  print(wrap([TextNode('a'), TextNode('b')], true).text);
}
