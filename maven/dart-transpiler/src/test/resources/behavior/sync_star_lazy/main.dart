Iterable<int> counted(int n) sync* {
  for (var i = 0; i < n; i++) {
    print('make $i');
    yield i;
  }
  print('counted done');
}

Iterable<int> naturals() sync* {
  var i = 0;
  while (true) {
    yield i++;
  }
}

Iterable<String> failing() sync* {
  yield 'a';
  yield 'b';
  throw StateError('boom');
}

Iterable<int> nested(int depth) sync* {
  yield depth;
  if (depth > 0) {
    print('descend $depth');
    yield* nested(depth - 1);
  }
  yield* [100 + depth];
}

Iterable<int> guarded() sync* {
  try {
    print('enter try');
    yield 1;
    yield 2;
    yield 3;
  } finally {
    print('finally ran');
  }
}

Iterable<int> caught() sync* {
  try {
    yield 1;
    throw 'oops';
  } catch (e) {
    print('caught $e');
    yield 2;
  } finally {
    yield 3;
  }
  yield 4;
}

class Box {
  final List<String> items;
  Box(this.items);
  Iterable<String> shout() sync* {
    for (final s in items) {
      yield s.toUpperCase();
    }
  }
  static Iterable<int> squares(int n) sync* {
    for (var i = 1; i <= n; i++) {
      yield i * i;
    }
  }
}

extension Pairs on List<int> {
  Iterable<(int, int)> pairs() sync* {
    for (var i = 0; i + 1 < length; i += 2) {
      final (int a, int b) = (this[i], this[i + 1]);
      yield (a, b);
    }
  }
}

Iterable<int> early(int n) sync* {
  if (n == 0) return;
  yield n;
  var label = 0;
  outer:
  for (var i = 0; i < 3; i++) {
    for (var j = 0; j < 3; j++) {
      if (j == 2) continue outer;
      if (i == 2) break outer;
      label++;
      yield label;
    }
  }
}

void main() {
  final it = counted(3);
  print('created');
  for (final v in it) {
    print('got $v');
  }
  print(naturals().take(5).toList());
  print(naturals().skip(3).first);
  print(naturals().where((x) => x.isOdd).map((x) => x * 10).take(3).toList());
  final f = failing().iterator;
  print(f.moveNext());
  print(f.current);
  print(f.moveNext());
  print(f.current);
  try {
    f.moveNext();
  } catch (e) {
    print('threw $e');
  }
  print(f.moveNext());
  print(nested(2).toList());
  for (final v in guarded()) {
    print('guarded $v');
    if (v == 2) break;
  }
  print('after break');
  print(guarded().toList());
  print(caught().toList());
  final twice = counted(2);
  final a = twice.iterator;
  final b = twice.iterator;
  a.moveNext();
  b.moveNext();
  a.moveNext();
  print('a ${a.current} b ${b.current}');
  print(Box(['x', 'y']).shout().join(','));
  print(Box.squares(4).toList());
  print([1, 2, 3, 4, 5].pairs().toList());
  print(early(0).toList());
  print(early(7).toList());
  Iterable<int> local(int k) sync* {
    for (var i = 0; i < k; i++) {
      yield i * k;
    }
  }
  print(local(3).toList());
  final closure = (int k) sync* {
    yield k;
    yield* local(k);
  };
  print(closure(2).toList());
  print(naturals().takeWhile((x) => x < 4).length);
}
