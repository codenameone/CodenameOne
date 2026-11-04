void sh<T>(T x) { print('$T'); }

Iterable<T> gen<T>(T a, T b, bool fail) sync* {
  try {
    yield a;
    if (fail) throw StateError('x');
    yield b;
  } catch (e) {
    yield b;
    try { yield a; throw 'inner'; } catch (e2) { yield b; } finally { yield a; }
  } finally {
    yield a;
    yield b;
  }
  yield a;
}

Future<List<T>> agen<T>(T a, T b, bool fail) async {
  final out = <T>[];
  try {
    out.add(await Future.value(a));
    if (fail) throw 'boom';
  } catch (e) {
    out.add(await Future.value(b));
    try { await Future.error('e2'); } catch (_) { out.add(a); } finally { out.add(await Future.value(b)); }
  } finally {
    out.add(await Future.value(a));
  }
  return out;
}

Stream<T> sgen<T>(T a, String? n) async* {
  try {
    if (n == null) throw 'n';
    yield a;
  } catch (e) {
    await null;
    yield a;
  } finally {
    if (n != null) { await null; sh(n); yield a; }
  }
}

Iterable<int> early(bool b) sync* {
  try { yield 1; if (b) return; yield 2; } finally { yield 9; }
  yield 3;
}

Future<String> rethrowIt() async {
  try {
    try { await Future.error('a'); } finally { await null; print('fin1'); }
  } catch (e) { return 'caught $e'; }
  return 'no';
}

void main() async {
  print(gen<int>(1, 2, false).toList());
  print(gen<String>('a', 'b', true).toList());
  print(await agen<int>(1, 2, false));
  print(await agen<String>('a', 'b', true));
  print(await sgen<int>(5, null).toList());
  print(await sgen<int>(6, 'q').toList());
  print(early(true).toList());
  print(early(false).toList());
  print(early(true).take(1).toList());
  print(await rethrowIt());
  String? s = 'x';
  Future<void> f() async { if (s != null) { await null; sh(s); } }
  await f();
}
