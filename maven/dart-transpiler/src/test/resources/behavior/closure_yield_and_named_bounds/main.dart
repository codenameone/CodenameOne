Iterable<int> evens(int n) sync* {
  for (var i = 0; i < n; i++) {
    if (i.isEven) yield i;
  }
}

void main() async {
  // A generator closure's element type comes from what it yields, whatever the
  // expression: a local, a call, a yield* over another iterable.
  final gen = () sync* {
    var x = 7;
    yield x;
    yield x * 2;
    yield* evens(5);
    yield* [100, 200];
  };
  final it = gen();
  print(it is Iterable<int>);
  print(it is Iterable<String>);
  print(it.toList());
  print(it.toList().runtimeType);

  final words = () sync* {
    final s = 'ab';
    yield s.toUpperCase();
    yield* ['c', 'd'];
  };
  print(words() is Iterable<String>);
  print(words().map((w) => w.length).toList());

  final mixed = () sync* {
    var i = 1;
    yield i;
    yield* [2.5];
  };
  print(mixed() is Iterable<num>);
  print(mixed() is Iterable<int>);

  final st = () async* {
    final v = 'x';
    yield v;
    yield* Stream.fromIterable(['y', 'z']);
  };
  print(st() is Stream<String>);
  print(await st().toList());

  // Function upper bounds keep the named parameters both sides declare.
  int Function(int, {String s}) g1 = (int x, {String s = ''}) => s.length + x;
  int Function(Object?, {String s}) g2 = (Object? y, {String s = '-'}) => 1;
  final gs = [g1, g2];
  print(gs.runtimeType);
  print(gs[0](1, s: 'abc') + gs[1](2, s: 'q'));
  int Function({required int k}) h1 = ({required int k}) => k;
  num Function({required int k}) h2 = ({required int k}) => k / 2;
  final hs = [h1, h2];
  print(hs.runtimeType);
  print(hs.map((h) => h(k: 3)).toList());
}
