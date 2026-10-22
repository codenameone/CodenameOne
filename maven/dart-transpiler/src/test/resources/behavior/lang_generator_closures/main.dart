import 'dart:async';

typedef Gen = Stream<int> Function(int n);

Stream<int> run(Gen g, int n) => g(n);

Iterable<String> pick(Iterable<String> Function() make) => make();


Future<void> main() async {
  await for (final v in run((n) async* {
    for (var i = 0; i < n; i++) {
      yield i * i;
    }
  }, 4)) {
    print('sq $v');
  }
  print(pick(() sync* {
    yield 'a';
    yield 'b';
  }).join(','));
  final echo = <T>(T x) => x;
  print(echo(7));
  print(echo('seven'));
  print(1 >>> 0);
}
