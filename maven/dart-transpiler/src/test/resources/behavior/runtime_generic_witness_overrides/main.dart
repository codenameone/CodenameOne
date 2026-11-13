import 'dart:async';
import 'dart:collection';

class Gather<T> {
  final List<Object?> _seen = [];
  final Completer<List<T>> _done = Completer<List<T>>();
  void add(Object? v) => _seen.add(v);
  List<T> now() => _seen.whereType<T>().toList();
  Future<List<T>> later() {
    Future<void>.value().then((_) {
      _done.complete(_seen.whereType<T>().toList());
    });
    return _done.future;
  }
}

class Shelf extends ListMixin<Object?> {
  final List<Object?> _inner = [];
  @override
  int get length => _inner.length;
  @override
  set length(int n) => _inner.length = n;
  @override
  Object? operator [](int i) => _inner[i];
  @override
  void operator []=(int i, Object? v) => _inner[i] = v;
  @override
  Iterable<T> whereType<T>() {
    print('shelf whereType');
    return _inner.whereType<T>();
  }
}

Future<void> main() async {
  final g = Gather<int>();
  g.add(1);
  g.add('two');
  g.add(3);
  g.add(null);
  print(g.now());
  print(await g.later());

  final s = Shelf();
  s.addAll([1, 'a', 2.5, 'b', null]);
  print(s.whereType<String>().toList());
  print(s.whereType<num?>().toList());
  print(s.whereType<int>().length);

  final mixed = Stream<Object>.fromIterable([1, 'q', 2]);
  print(await mixed.where((Object o) => o is int).toList());
  final Stream<int> nums = Stream.fromIterable([4, 5]);
  print(await nums.map((int n) => n * 10).toList());
  print(await Stream<String>.value('one').toList());
  print(await Stream.fromFuture(Future<int>.value(9)).first);
  final Stream<int> none = Stream.empty();
  print(await none.toList());
  print(await Stream<int>.empty().length);
  final periodic = Stream.periodic(const Duration(milliseconds: 1), (int i) => i * 3).take(3);
  print(await periodic.toList());
}
