// A void arrow body passed where a program function's parameter returns a type
// variable (or dynamic): Dart infers the type variable as void, Java needs a value.

List<T> collect<T>(int n, T Function() element) {
  final out = <T>[];
  for (var i = 0; i < n; i++) {
    out.add(element());
  }
  return out;
}

R withIndex<R>(R Function(int) body) => body(7);

dynamic runDyn(dynamic Function(String) f) => f('dyn');

class Box {
  final List<String> log = [];
  void note(String s) {
    log.add(s);
  }

  static void shout(List<String> into, String s) {
    into.add(s.toUpperCase());
  }

  List<T> each<T>(T Function(String) f) => [f('a'), f('b')];
}

void record(List<String> into, String s) {
  into.add(s);
}

void main() {
  final log = <String>[];
  final got = collect(3, () => record(log, 'x'));
  print('${got.length} $log');
  withIndex((i) => record(log, 'i$i'));
  print(log);
  runDyn((s) => record(log, s));
  print(log);
  final b = Box();
  b.each((s) => b.note(s));
  b.each((s) => Box.shout(b.log, s));
  print(b.log);
  print(collect(2, () => 'v'));
  final xs = collect(2, () => 5);
  print('${xs.length} ${xs[0] + xs[1]}');
  final ds = collect(2, () => 1.5);
  print('${ds.length} ${ds[0] + ds[1]}');
  final bs = collect(2, () => true);
  print(bs);
}
