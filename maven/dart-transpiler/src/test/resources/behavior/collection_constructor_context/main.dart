import 'dart:async';

class Style {
  String separator() => '/';
}

class Parsed {
  final Style style = Style();
  List<String> parts;
  List<String> separators;
  Map<String, String> headers;
  Set<int> seen;
  Parsed(this.parts, this.separators) : headers = {}, seen = {};
  void reset() {
    separators = List.filled(parts.length + 1, style.separator());
    headers = Map.of({});
    headers = Map.from(headers);
    seen = Set.from([1, 2]);
  }
  Parsed copy() => Parsed(List.from(parts), List.from(separators));
  void grow(Parsed other) {
    separators.insertAll(1, List.filled(other.parts.length, style.separator()));
  }
}

class Node {
  final String name;
  Node(this.name);
}

List<Node> nodes(List<String> names) {
  var refs = names.map((n) => Node(n)).toList();
  var out = <Node>[];
  out.addAll(refs);
  out.addAll(names.where((n) => n.length > 1).map((n) => Node(n)));
  return out;
}

Map<Object, E> keyed<E>(List<E> items) {
  var map = {};
  for (var i = 0; i < items.length; i++) {
    map[i] = items[i];
  }
  return Map<Object, E>.from(map);
}

class Memo<T> {
  Future<T>? _f;
  Future<T> runOnce(Future<T> Function() fn) => _f ??= fn();
}

abstract class SinkBase {
  final _memo = Memo<void>();
  Future<void> onClose();
  Future<void> close() => _memo.runOnce(onClose);
}

class PrintSink extends SinkBase {
  @override
  Future<void> onClose() async {
    print('closing');
  }
}

Future<void> main() async {
  var p = Parsed(['a', 'b'], ['', '/', '']);
  p.reset();
  print(p.separators);
  print(p.headers);
  print(p.seen);
  var c = p.copy();
  c.grow(p);
  print(c.separators);
  print(nodes(['x', 'yy', 'zzz']).map((n) => n.name).toList());
  print(keyed(['p', 'q']));
  var s = PrintSink();
  await s.close();
  await s.close();
  print('done');
}
