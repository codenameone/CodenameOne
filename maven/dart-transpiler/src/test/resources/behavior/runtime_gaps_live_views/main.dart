import 'dart:async';
import 'dart:collection';
import 'dart:convert';

class Bag extends ListMixin<int> {
  final List<int?> _items = [];
  @override
  int get length => _items.length;
  @override
  set length(int n) {
    while (_items.length > n) {
      _items.removeLast();
    }
    while (_items.length < n) {
      _items.add(null);
    }
  }
  @override
  int operator [](int i) => _items[i]!;
  @override
  void operator []=(int i, int v) => _items[i] = v;
}

class Doubled extends StreamView<int> {
  Doubled(Stream<int> s) : super(s.map((x) => x * 2));
}

Future<void> main() async {
  final bag = Bag();
  bag.add(1);
  bag.add(2);
  final Iterable<int> evens = bag.where((int x) => x.isEven);
  final Iterable<int> squares = bag.map((int x) => x * x);
  final Iterable<int> tail = bag.skip(1);
  final Iterable<int> head = bag.take(2);
  bag.add(4);
  bag.add(6);
  print('evens ${evens.toList()}');
  print('squares ${squares.toList()}');
  print('tail ${tail.toList()}');
  print('head ${head.toList()}');
  bag.setAll(1, [7, 8]);
  print('setAll $bag');
  bag.setRange(0, 2, [9, 10, 11], 1);
  print('setRange ${bag.toList()}');
  print('bag $bag');

  final log = <String>[];
  final c = StreamController<int>(onListen: () => log.add('listen'));
  print('has onListen ${c.onListen != null}');
  print('has onCancel ${c.onCancel != null}');
  c.onCancel = () => log.add('cancel');
  print('has onCancel now ${c.onCancel != null}');
  final sub = c.stream.listen((v) => log.add('v$v'));
  c.add(3);
  await Future.delayed(Duration.zero);
  await sub.cancel();
  print(log);

  final Codec<Object?, String> codec = json;
  final text = codec.encode({'a': [1, 2]});
  print(text);
  print((codec.decode(text) as Map)['a']);

  final String Function() f = Zone.current.bindCallback(() => 'bound');
  print(f());
  final int Function(int) g = Zone.current.bindUnaryCallback((int x) => x + 1);
  print(g(41));
  final int Function(int, int) h = Zone.current.bindBinaryCallback((int a, int b) => a * b);
  print(h(6, 7));

  Doubled(Stream.fromIterable([1, 2, 3])).listen((int v) => print('view $v'));
}
