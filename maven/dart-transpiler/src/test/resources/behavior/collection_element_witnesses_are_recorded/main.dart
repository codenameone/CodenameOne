import 'dart:async';
import 'dart:collection';
import 'dart:convert';

class Shape {}

class Circle extends Shape {}

class Ring<E> extends ListBase<E> {
  final List<E> _items;
  Ring(this._items);
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
}

Iterable<Duration> ticks() sync* {
  yield const Duration(seconds: 1);
}

Future<void> main() async {
  // A list of a runtime class (Duration, Uri) records its element type.
  final durations = <Duration>[const Duration(seconds: 1)];
  print(durations is List<String>);
  print(durations is List<Duration>);
  print(durations is List<Object>);
  final uris = [Uri.parse('a/b')];
  print(uris is List<Uri>);
  print(uris is List<int>);
  print(<Shape>[Circle()] is List<Circle>);

  // Lists made by the List constructors record the argument they were given.
  print(List<int>.generate(3, (i) => i) is List<String>);
  print(List<String>.filled(2, 'x') is List<String>);
  print(List<num>.filled(2, 1) is List<int>);
  print(List<Object>.of(<int>[1, 2]) is List<int>);
  print(List<String>.from(<Object>['a']) is List<String>);
  print(List<Duration>.from(durations) is List<Object>);

  // Decoded JSON is List<dynamic> and Map<String, dynamic>, whatever it holds.
  final decoded = jsonDecode('[1, 2, 3]');
  print(decoded is List<int>);
  print(decoded is List<dynamic>);
  print(decoded.runtimeType);
  final obj = jsonDecode('{"a": "x", "b": ["y"]}');
  print(obj is Map<String, String>);
  print(obj is Map<String, dynamic>);
  print(obj['b'] is List<String>);

  // A string split is a List<String>.
  print('a,b'.split(',') is List<String>);

  // Copies and concatenations keep the element type they were made from.
  final ring = Ring<Duration>([const Duration(seconds: 2)]);
  print(ring.toList() is List<String>);
  print(ring.toList() is List<Duration>);
  print(ring.sublist(0, 1) is List<int>);
  print((durations + durations) is List<String>);
  print((durations + durations) is List<Duration>);
  // A sync* body's elements are of its declared element type.
  print(ticks() is Iterable<String>);
  print(ticks().toList() is List<String>);
  print(ticks().toList() is List<Duration>);
  // A stream's toList is a list of the stream's element type.
  final fromStream = await Stream<Duration>.fromIterable(durations).toList();
  print(fromStream is List<String>);
  print(fromStream is List<Duration>);
}
