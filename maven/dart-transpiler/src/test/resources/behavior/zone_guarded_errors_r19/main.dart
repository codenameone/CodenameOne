import 'dart:async';
void main() {
  runZonedGuarded(() {
    runZonedGuarded(() {
      Future.error('inner');
      scheduleMicrotask(() => throw 'innerMicro');
    }, (e, s) { print('innerH $e'); if (e == 'innerMicro') throw 'fromHandler'; });
    final c = StreamController<int>.broadcast();
    c.stream.listen((x) { if (x == 2) throw 'onData$x'; print('d $x'); });
    c.add(1); c.add(2);
    final f = Future.error('late');
    scheduleMicrotask(() => f.catchError((e) { print('caught $e'); return null; }));
    final g = Future.error('handledLater');
    Timer(Duration.zero, () => g.catchError((e) { print('timer caught $e'); return null; }));
    Future.wait([Future.delayed(Duration(milliseconds: 5), () => 1), Future.error('w')], eagerError: true)
        .then((v) => print('w $v'), onError: (e) => print('wait err $e'));
    final sc = Completer<int>.sync();
    sc.future.then((v) { print('t1 $v'); Future.value(v).then((w) { print('t2 $w'); throw 'deep'; }); });
    sc.complete(7);
    print('sync end');
  }, (e, s) => print('outerH $e'));
}
