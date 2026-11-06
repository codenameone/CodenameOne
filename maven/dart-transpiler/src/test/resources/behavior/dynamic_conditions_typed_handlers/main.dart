import 'dart:async';

class Matcher {
  final String prefix;
  Matcher(this.prefix);
  bool hasMatch(String s) => s.startsWith(prefix);
}

String describe(Object error, [StackTrace? trace]) =>
    'error $error trace ${trace != null}';

T apply<T>(String s, T Function(String, int) f) {
  return f(s, s.length);
}

void main() async {
  dynamic m = Matcher('ab');
  if (m.hasMatch('abc')) {
    print('if matched');
  }
  var n = 0;
  while (m.hasMatch('ab' * (3 - n))) {
    n++;
    if (n > 2) break;
  }
  print('while ran $n');
  print(m.hasMatch('zz') ? 'ternary yes' : 'ternary no');
  var items = [for (var w in ['ab1', 'cd', 'ab2']) if (m.hasMatch(w)) w];
  print(items);

  var log = <String>[];
  StreamSubscription<int>? sub;
  sub = Stream<int>.error('boom').listen((v) {
    log.add('value $v');
  }, onError: (Object error, StackTrace stackTrace) {
    log.add(describe(error, stackTrace));
    print(log);
    sub?.cancel();
  });

  String joined = apply('hey', (s, len) => '$s:$len');
  int doubled = apply('four', (s, len) => len * 2);
  print('$joined $doubled');
}
