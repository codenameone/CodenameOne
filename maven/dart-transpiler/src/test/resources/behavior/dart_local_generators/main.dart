Future<void> main() async {
  Iterable<int> evens(int n) sync* {
    for (var i = 0; i < n; i++) {
      if (i.isEven) yield i;
    }
  }

  Stream<String> words() async* {
    yield 'a';
    yield 'b';
  }

  print(evens(6).toList());
  await for (final w in words()) {
    print(w);
  }
}
