import 'dart:async';
import 'dart:convert';

import 'search_algos.dart' as algos;

// Values a stub types as dynamic, and tear-offs handed to Future.then, reaching
// slots whose static type is precise.

class Holder<T> {
  List<T>? items;
  Object? err;
  void setItems(List<T> v) {
    items = v;
  }

  void setError(Object e, [StackTrace? st]) {
    err = e;
  }
}

class Payload {
  final List<int> bytes;
  Encoding get encoding => utf8;
  Payload(this.bytes);
  String get text => encoding.decode(bytes);
  List<int> pack(String s) => encoding.encode(s);
}

String decodeWith(Encoding e, List<int> b) {
  return e.decode(b);
}

class Relay<T> {
  final Holder<T> h = Holder<T>();
  Future<void> feed(Future<List<T>> f) => f.then(h.setItems, onError: h.setError);
}

extension Finder<E> on List<E> {
  int findBy(E element, int Function(E, E) compare) =>
      algos.findBy<E, E>(this, algos.same, compare, element);
  int same(E element) => algos.findBy(this, (E e) => e, (E a, E b) => a == b ? 0 : 1, element);
}

class Quiet {
  final Future<Object?> done;
  Quiet({Future<Object?>? done}) : done = done ?? Future.value('quiet');
  Quiet.loud(String why) : done = Future.value(why);
}

abstract class SinkCore<T> {
  final List<Object?> got = [];
  void add(T data) {
    got.add(data);
  }
}

class ByteSink extends SinkCore<List<int>> {
  Encoding encoding = utf8;
  void write(Object? o) {
    add(encoding.encode(o.toString()));
    this.add(encoding.encode('!'));
  }
}

dynamic notAString() => 42;

String takeString(String s) => s;

Future<void> main() async {
  print([3, 4, 5].findBy(5, (a, b) => a - b));
  print(['x', 'y'].same('y'));
  print(await Quiet().done);
  print(await Quiet.loud('l').done);
  final sink = ByteSink();
  sink.write('hi');
  print(sink.got);
  try {
    print(takeString(notAString()));
  } on TypeError {
    print('TypeError on implicit downcast');
  }

  final p = Payload([104, 105]);
  print(p.text);
  print(p.pack('ok'));
  print(decodeWith(latin1, [65, 66]));
  final h = Holder<int>();
  final f = Future<List<int>>.value([1, 2]);
  await f.then(h.setItems, onError: h.setError);
  print(h.items);
  final r = Relay<String>();
  await r.feed(Future.value(['p', 'q']));
  print(r.h.items);
}
