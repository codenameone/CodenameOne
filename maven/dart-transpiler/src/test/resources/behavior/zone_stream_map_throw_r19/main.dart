import 'dart:async';
void main() {
  Stream<int>.error('se').listen((_) {}, onError: (e) => print('se handled $e'));
  runZonedGuarded(() {
    Stream<int>.fromIterable([1]).map((x) => throw 'mapErr').listen(print);
  }, (e, s) => print('zone2 $e'));
  runZonedGuarded(() {
    Timer.run(() => throw 'timerRun');
  }, (e, s) => print('zone3 $e'));
  runZonedGuarded(() {
    Timer(Duration.zero, () => throw 'timerZero');
  }, (e, s) => print('zone4 $e'));
  Future.delayed(Duration.zero, () => print('delayed zero'));
  Timer.run(() => print('timer last'));
}
