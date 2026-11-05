import 'dart:async';

class Collector implements Sink<String> {
  final List<String> seen = <String>[];
  bool closed = false;

  @override
  void add(String data) => seen.add(data);

  @override
  Future close() {
    closed = true;
    return Future.value(seen.length);
  }
}

abstract class Store {
  Future<void> compact(Iterable<int> items);
}

class MemoryStore extends Store {
  @override
  Future<List<int>> compact(Iterable<int> items) => Future.value(items.toList());
}

class Flags implements Iterator<bool> {
  final List<bool> _values;
  int _i = -1;
  Flags(this._values);

  @override
  bool get current => _values[_i];

  @override
  bool moveNext() => ++_i < _values.length;
}

void feed(Sink<String> sink) {
  sink.add('a');
  sink.add('b');
  sink.close();
}

Future<void> main() async {
  var c = Collector();
  feed(c);
  print('${c.seen} ${c.closed}');
  Store s = MemoryStore();
  await s.compact([3, 1]);
  var m = MemoryStore();
  print(await m.compact([5, 6]));
  var f = Flags([true, false, true]);
  var out = <bool>[];
  while (f.moveNext()) {
    out.add(f.current);
  }
  print(out);
  Iterator<bool> it = Flags([false]);
  it.moveNext();
  print(it.current);
}
