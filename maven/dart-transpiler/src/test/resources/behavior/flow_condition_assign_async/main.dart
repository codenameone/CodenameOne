void sh<T>(T x) {
  print('$T');
}

String? pick(int i) => i > 0 ? 'p$i' : null;

void boom(bool b) {
  if (b) throw StateError('boom');
}

void orAssign(String? x) {
  String? y;
  if (x == null || (y = x).isEmpty) {
    print('early');
    return;
  }
  sh(y);
}

void andAssign(String? x) {
  String? y;
  if (x != null && (y = x).isNotEmpty) {
    sh(y);
  }
}

void notOrAssign(String? x) {
  String? y;
  if (!(x == null || (y = x).isEmpty)) {
    sh(y);
  }
}

Future<void> asyncCatch(bool fail) async {
  String? y = 'a';
  String? z = 'k';
  try {
    y = null;
    await Future<void>.value();
    boom(fail);
    y = 'c';
  } catch (e) {
    sh(y);
    sh(z);
    print('caught $y $z');
  }
  sh(y);
}

Iterable<String> genCatch(bool fail) sync* {
  String? y = 'a';
  String? z = 'k';
  try {
    y = null;
    yield 'one';
    boom(fail);
    y = 'c';
  } catch (e) {
    sh(y);
    sh(z);
    yield 'caught $y $z';
  }
}

Future<void> main() async {
  orAssign('v');
  orAssign(null);
  orAssign(pick(0));
  andAssign(pick(2));
  notOrAssign('w');
  await asyncCatch(true);
  await asyncCatch(false);
  for (final s in genCatch(true)) {
    print(s);
  }
}
