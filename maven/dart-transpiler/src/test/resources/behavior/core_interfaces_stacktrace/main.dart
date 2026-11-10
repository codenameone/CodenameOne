import 'dart:async';
import 'dart:collection';

class Bag<E> with ListMixin<E> implements List<E> {
  final List<E> _items = <E>[];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  E operator [](int i) => _items[i];
  @override
  void operator []=(int i, E v) => _items[i] = v;
  @override
  void add(E v) => _items.add(v);
}

class Base {
  String tag() => 'base';
}

class Names extends Base with IterableMixin<String> implements Iterable<String> {
  @override
  Iterator<String> get iterator => ['x', 'y', 'z'].iterator;
}

class Collector<T> implements Sink<T> {
  final List<T> got = [];
  bool closed = false;
  @override
  void add(T data) => got.add(data);
  @override
  void close() => closed = true;
}

void feed(Sink<int> s) {
  s.add(1);
  s.add(2);
  s.close();
}

String describe(Object e, StackTrace st) => 'caught $e, trace is StackTrace: ${st is StackTrace}';

void onErr(Object e, StackTrace st) {
  print('tear-off handler $e ${st.toString().isNotEmpty || st.toString().isEmpty}');
}

Future<void> main() async {
  final b = Bag<int>();
  b.add(3);
  b.add(4);
  b[0] = 9;
  print('bag ${b.length} ${b[0]} ${b.toList()}');
  final n = Names();
  print('names ${n.join(",")} ${n.tag()} ${n.length}');
  final c = Collector<int>();
  feed(c);
  print('sink ${c.got} ${c.closed}');

  try {
    throw StateError('boom');
  } catch (e, st) {
    print(describe(e, st));
    final StackTrace t = st;
    print(t.runtimeType.toString().isNotEmpty);
  }
  final cur = StackTrace.current;
  print('current ${cur is StackTrace}');
  final parsed = StackTrace.fromString('frame one');
  print('fromString ${parsed.toString()}');

  final comp = Completer<int>();
  comp.completeError(ArgumentError('bad'), StackTrace.fromString('custom trace'));
  try {
    await comp.future;
  } catch (e, st) {
    print('completer ${e.runtimeType} ${st is StackTrace}');
  }

  final ctl = StreamController<int>();
  ctl.stream.listen((v) => print('v $v'), onError: (Object e, StackTrace st) => print('stream err $e ${st is StackTrace}'));
  ctl.add(1);
  ctl.addError('oops', StackTrace.fromString('stream trace'));
  await ctl.close();

  final r = await Future<int>.error('f1').catchError((Object e, StackTrace st) {
    print('two-arg $e ${st is StackTrace}');
    return 7;
  });
  print('r $r');
  final r2 = await Future<int>.error('f2').catchError((Object e) => 8);
  print('r2 $r2');
  await Future<void>.error('f3').catchError(onErr);
}
