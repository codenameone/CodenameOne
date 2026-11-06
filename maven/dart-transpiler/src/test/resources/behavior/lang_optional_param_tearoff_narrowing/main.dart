typedef OnValue = void Function(String);
typedef OnNothing = void Function();
typedef OnPair = void Function(int, int);

class Config {
  final OnValue onValue;
  final OnNothing onNothing;
  final OnPair onPair;

  Config({this.onValue = _noOp, this.onNothing = _noOp, this.onPair = _noOp});

  static void _noOp([a, b]) {
    print('noop ${a ?? "-"} ${b ?? "-"}');
  }
}

void report([Object? a, Object? b, Object? c]) {
  print('report $a $b $c');
}

void apply(void Function(int) f) => f(7);

void main() {
  final c = Config();
  c.onValue('x');
  c.onNothing();
  c.onPair(1, 2);
  final d = Config(onValue: (s) => print('got $s'));
  d.onValue('y');
  OnValue v = report;
  v('z');
  apply(report);
  final OnNothing n = report;
  n();
}
