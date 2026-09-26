// sync* generators lowered to a list-collecting body.

Iterable<int> countTo(int n) sync* {
  for (int i = 1; i <= n; i++) {
    yield i;
  }
}

Iterable<int> combined() sync* {
  yield 0;
  yield* countTo(3);
  yield 99;
}

void main() {
  var out = '';
  for (final x in combined()) {
    out += '$x ';
  }
  print(out);
}
