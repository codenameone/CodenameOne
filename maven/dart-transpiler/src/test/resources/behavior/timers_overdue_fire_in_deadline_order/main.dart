import 'dart:async';

// Timers fire by deadline measured from their creation, then creation order; every
// timer that is overdue when main()'s synchronous body ends still waits for that body
// and for the microtasks it queued, and then fires in that order. The busy wait makes
// all of them overdue at once, so the order is Dart's whatever the machine's speed.
void spin(int ms) {
  var sw = Stopwatch()..start();
  while (sw.elapsedMilliseconds < ms) {}
}

void main() async {
  var log = <String>[];
  // The first timer of the run: whatever the runtime initialises on its way in is not
  // part of its 8ms, so the zero timer made right after it still fires first.
  Timer(Duration(milliseconds: 8), () => log.add('first'));
  Timer.run(() => log.add('zero'));
  for (var i = 0; i < 3; i++) {
    Timer(Duration(milliseconds: 60), () => log.add('a$i'));
  }
  Timer(Duration(milliseconds: 30), () {
    log.add('b');
    Timer(Duration.zero, () => log.add('bz'));
    scheduleMicrotask(() => log.add('bm'));
    Timer(Duration(milliseconds: 200), () => log.add('b200'));
  });
  Timer.run(() {
    log.add('run');
    scheduleMicrotask(() => log.add('runm'));
  });
  late Timer t2;
  t2 = Timer(Duration(milliseconds: 1), () => log.add('never'));
  t2.cancel();
  Future.delayed(Duration(milliseconds: 90), () => log.add('fd'));
  late Timer self;
  self = Timer(Duration(milliseconds: 10), () {
    log.add('self ${self.isActive}');
    self.cancel();
  });
  scheduleMicrotask(() {
    log.add('m');
    Timer(Duration.zero, () => log.add('mz'));
  });
  log.add('sync');
  spin(150);
  log.add('end');
  await Future.delayed(Duration(milliseconds: 300));
  print(log.join(' '));
  print(log.indexOf('bz') < log.indexOf('b200'));
}
