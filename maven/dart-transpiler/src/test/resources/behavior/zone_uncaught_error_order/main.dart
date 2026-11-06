import 'dart:async';
void main() {
  var out = <String>[];
  runZonedGuarded(() {
    Future.error('a');
    scheduleMicrotask(() => out.add('m1'));
    Future(() => throw 'b');
    Timer.run(() => out.add('t'));
    Future.value(1).then((_) => throw 'c');
    var f = Future.error('handled');
    f.catchError((e) => out.add('h$e'));
    out.add('body');
  }, (e, s) => out.add('Z$e'));
  runZonedGuarded(() {
    var c = StreamController<int>();
    c.stream.listen((v) => out.add('v$v'));
    c.addError('se');
    c.add(1);
  }, (e, s) => out.add('Z2$e'));
  Timer(Duration(milliseconds: 50), () => print(out));

  var o4 = <String>[];
  runZonedGuarded(() {
    var early = Future.error('early');
    var late = Future.error('late');
    var viaThen = Future.value(2).then((_) => throw 'thenLate');
    scheduleMicrotask(() {
      early.catchError((e) => o4.add('h$e'));
    });
    scheduleMicrotask(() {
      scheduleMicrotask(() {
        late.catchError((e) => o4.add('h$e'));
        viaThen.then((_) {}, onError: (e) { o4.add('h$e'); });
      });
    });
    var same = Future.error('same');
    same.then((v) => v).catchError((e) => o4.add('h$e'));
  }, (e, s) => o4.add('Z$e'));
  Timer(Duration(milliseconds: 60), () => print(o4));
  var ticks = <String>[];
  var sw = Stopwatch()..start();
  StreamSubscription? sub;
  sub = Stream.periodic(Duration(milliseconds: 20), (i) => i).take(4).listen((i) {
    ticks.add('$i');
    if (i == 1) { sub!.pause(Future.delayed(Duration(milliseconds: 60))); }
  }, onDone: () { print('ticks $ticks ${sw.elapsedMilliseconds >= 100}'); });
}
