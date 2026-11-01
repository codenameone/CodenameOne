import 'dart:async';
import 'dart:collection';

// A list built on ListMixin, passed where List is declared.
class Ring with ListMixin<int> implements List<int> {
  final List<int> _items = <int>[];
  @override
  int get length => _items.length;
  @override
  set length(int n) => _items.length = n;
  @override
  int operator [](int i) => _items[i];
  @override
  void operator []=(int i, int v) => _items[i] = v;
  @override
  void add(int v) => _items.add(v);
}

// Implements List without a mixin: every member is its own.
class Fixed implements List<int> {
  final List<int> _d = [7, 8];
  @override
  int get length => _d.length;
  @override
  int operator [](int i) => _d[i];
  @override
  dynamic noSuchMethod(i) => throw UnimplementedError();
}

class Letters with IterableMixin<String> implements Iterable<String> {
  @override
  Iterator<String> get iterator => ['p', 'q'].iterator;
}

int total(List<int> xs) {
  var s = 0;
  for (final x in xs) {
    s += x;
  }
  return s;
}

String joined(Iterable<String> it) => it.join('+');

void pour(Sink<int> s) {
  s.add(5);
  s.close();
}

StackTrace here() => StackTrace.current;

Future<void> main() async {
  final r = Ring();
  r.add(1);
  r.add(2);
  r.add(3);
  print('join ${r.join("-")} ${r.length} ${Letters().join("+")}');
  final f = Fixed();
  print('fixed ${f.length} ${f[0]} ${f[1]}');

  final a = StackTrace.current;
  final b = here();
  print('current ${a is StackTrace} ${a.toString().isNotEmpty} ${identical(a, b)}');

  try {
    throw StateError('x');
  } catch (e, st) {
    print('caught nonempty ${st.toString().isNotEmpty}');
  }

  final custom = StackTrace.fromString('my-trace');
  final c1 = Completer<int>();
  c1.future.then((v) => print('no'), onError: (Object e, StackTrace st) {
    print('then onError $e $st');
  });
  c1.completeError('e1', custom);
  final c2 = Completer<int>();
  c2.future.catchError((Object e, StackTrace st) {
    print('catchError $e $st');
    return 0;
  });
  c2.completeError('e2', custom);
  try {
    await Future<int>.error('e3', custom);
  } catch (e, st) {
    print('await $e $st');
  }

  final ctl = StreamController<int>();
  final done = Completer<void>();
  ctl.stream.listen((v) {}, onError: (Object e, StackTrace st) {
    print('listen onError $e $st');
  }, onDone: () => done.complete());
  ctl.addError('e4', custom);
  final StreamSink<int> ss = ctl.sink;
  final EventSink<int> es = ss;
  pour(es);
  await done.future;
  print('sink done');
}
