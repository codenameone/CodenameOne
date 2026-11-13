extension ON on String? { String tag() => 'nullable'; }
extension NN on String { String tag() => 'nonnull'; }
void sh<T>(T x) { print('$T'); }
String? mk(bool b) => b ? 'v' : null;

Future<void> af(String? s) async {
  if (s == null) return;
  try {
    sh(s);
    print(s.tag());
    await Future.value(0);
    sh(s);
    s = mk(false);
    sh(s);
    await Future.value(1);
  } catch (e) {
    sh(s);
  }
  sh(s);
}

Iterable<String> gen(String? s) sync* {
  if (s == null) return;
  try {
    sh(s);
    yield s.tag();
    sh(s);
  } finally {
    sh(s);
  }
}

Stream<String> ag(String? s) async* {
  if (s == null) return;
  try {
    sh(s);
    yield s.tag();
    await Future.value(2);
    print(s.tag());
  } finally {
    sh(s);
  }
}

void main() async {
  await af('a');
  print(gen('b').toList());
  await for (final v in ag('c')) print(v);
}
