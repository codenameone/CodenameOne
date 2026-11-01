import 'dart:async';

String tag() => '${Zone.current[#tag] ?? 'root'}';

Future<void> main() async {
  final log = <String>[];
  final done = Completer<void>();
  final timerDone = Completer<void>();
  final ctl = StreamController<int>();
  runZoned(() {
    Timer(Duration.zero, () {
      log.add('timer in ${tag()}');
      timerDone.complete();
    });
    scheduleMicrotask(() => log.add('microtask in ${tag()}'));
    Future.value(1).then((v) => log.add('then $v in ${tag()}'));
    ctl.stream.listen((v) => log.add('data $v in ${tag()}'), onDone: () {
      log.add('done in ${tag()}');
      done.complete();
    });
  }, zoneValues: {#tag: 'A'});
  print('outside ${tag()}');
  ctl.add(5);
  await ctl.close();
  await done.future;
  await timerDone.future;
  log.sort();
  log.forEach(print);

  final lines = <String>[];
  runZoned(() {
    print('hello');
    print('world');
  }, zoneSpecification: ZoneSpecification(
      print: (Zone self, ZoneDelegate parent, Zone zone, String line) {
    lines.add(line);
    parent.print(zone, '> $line');
  }));
  print('captured ${lines.length}');

  final caught = Completer<String>();
  runZonedGuarded(() {
    Timer(Duration.zero, () => throw StateError('boom'));
  }, (Object e, StackTrace s) {
    caught.complete('guarded ${e is StateError} ${tag()}');
  }, zoneValues: {#tag: 'G'});
  print(await caught.future);

  final handled = Completer<String>();
  runZoned(() {
    Future<void>.error(ArgumentError('bad'));
  }, zoneSpecification: ZoneSpecification(
      handleUncaughtError: (Zone self, ZoneDelegate parent, Zone zone,
          Object error, StackTrace st) {
    handled.complete('handled ${error is ArgumentError}');
  }));
  print(await handled.future);

  var runs = 0;
  var registered = 0;
  final r = runZoned(() {
    final cb = Zone.current.bindCallback(() => 'bound ${tag()}');
    return cb();
  }, zoneValues: {#tag: 'R'}, zoneSpecification: ZoneSpecification(
      run: <R>(Zone self, ZoneDelegate parent, Zone zone, R Function() f) {
        runs++;
        return parent.run(zone, f);
      },
      registerCallback: <R>(Zone self, ZoneDelegate parent, Zone zone,
          R Function() f) {
        registered++;
        return parent.registerCallback(zone, f);
      }));
  print('$r runs>0 ${runs > 0} registered>0 ${registered > 0}');
  final forked = Zone.current.fork(zoneValues: {#tag: 'F'});
  print(forked.run(() => tag()));
  print('end ${tag()}');
}
