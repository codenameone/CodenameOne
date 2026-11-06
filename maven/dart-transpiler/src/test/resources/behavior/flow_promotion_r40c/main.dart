void sh<T>(T x) {
  print('$T');
}

String? maybe(bool b) => b ? 'm' : null;

Future<String?> fut(bool b) async => b ? 'f' : null;

Future<void> asy(bool f) async {
  String? x = 'a';
  sh(x);
  x = await fut(f);
  sh(x);
  if (x == null) return;
  await Future.value(1);
  sh(x);
  String? y = 'y';
  await Future(() {
    y = null;
  });
  sh(y);
  String? z = 'z';
  try {
    z = await fut(false);
    throw StateError('s');
  } catch (e) {
    sh(z);
  }
  print([z]);
}

Iterable<String> gen(bool f) sync* {
  String? x = 'a';
  sh(x);
  yield x;
  x = maybe(f);
  sh(x);
  if (x != null) {
    yield x;
    sh(x);
  }
  for (var i = 0; i < 2; i++) {
    sh(x);
    x = null;
    yield 'i$i';
  }
  sh(x);
}

Stream<String> sgen() async* {
  String? x = 'b';
  yield x;
  sh(x);
  x = null;
  sh(x);
  yield '$x';
}

void main() async {
  await asy(true);
  await asy(false);
  print(gen(true).toList());
  print(await sgen().toList());
}
