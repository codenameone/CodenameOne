// Dart 3 if-case statements, pattern for-in (record destructuring), and
// collection-for inside typed set/map literals.

String describe(Object v) {
  if (v case int x when x > 5) {
    return 'big $x';
  } else if (v case int x) {
    return 'small $x';
  } else {
    return 'other';
  }
}

void main() {
  print(describe(10));
  print(describe(3));
  print(describe('hi'));

  var pairs = [(1, 'a'), (2, 'b'), (3, 'c')];

  // typed set literal with collection-for over a record pattern + collection-if
  Set<int> big = <int>{
    for (final (int i, String s) in pairs)
      if (i > 1) i,
  };
  print(big);

  // map literal with collection-for
  Map<int, String> m = <int, String>{
    for (final (int i, String s) in pairs) i: s,
  };
  print(m);

  // pattern for-in statement
  var out = '';
  for (final (int i, String s) in pairs) {
    out += '$i$s';
  }
  print(out);
}
