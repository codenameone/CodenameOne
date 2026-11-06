// A dynamic argument passed to a runtime-library method whose parameter has a
// non-dynamic type goes through Dart's implicit downcast: a mismatch throws a
// TypeError at the call, and a match is used as the parameter's type.
void check(String label, void Function() body) {
  try {
    body();
    print('$label: ok');
  } on TypeError {
    print('$label: TypeError');
  }
}

void main() {
  dynamic notIterable = 42;
  dynamic ints = <int>[7, 8];
  dynamic someInts = <int>[7, 8].where((x) => x > 0);
  dynamic notString = 3;
  dynamic aString = 'lo';
  dynamic notMap = 'x';
  dynamic aMap = <String, int>{'b': 2};
  dynamic notInt = 'one';
  dynamic aRegExp = RegExp('l+');

  var set = <int>{1};
  check('Set.addAll mismatch', () => set.addAll(notIterable));
  check('Set.addAll match', () => set.addAll(someInts));
  check('Set.addAll list match', () => set.addAll(ints));
  print(set);

  var map = <String, int>{'a': 1};
  check('Map.addAll mismatch', () => map.addAll(notMap));
  check('Map.addAll match', () => map.addAll(aMap));
  print(map);

  var s = 'hello';
  check('String.endsWith mismatch', () => print(s.endsWith(notString)));
  check('String.endsWith match', () => print(s.endsWith(aString)));
  check('String.compareTo mismatch', () => print(s.compareTo(notString)));
  check('String.padRight match', () => print(s.padRight(ints[1], aString)));
  check('String.padLeft mismatch', () => print(s.padLeft(notString, notString)));
  check('String.substring mismatch', () => print(s.substring(notInt)));
  check('String.contains match', () => print(s.contains(aString)));
  check('String.contains regexp', () => print(s.contains(aRegExp)));
  check('String.contains mismatch', () => print(s.contains(notString)));
  check('String.startsWith match', () => print(s.startsWith(aRegExp)));
  check('String.startsWith mismatch', () => print(s.startsWith(notString)));
  check('String.indexOf match', () => print(s.indexOf(aString)));
  check('String.indexOf regexp', () => print(s.indexOf(aRegExp)));
  check('String.indexOf mismatch', () => print(s.indexOf(notString)));
  check('String.split match', () => print(s.split(aString)));
  check('String.split regexp', () => print(s.split(aRegExp)));
  check('String.split mismatch', () => print(s.split(notString)));
  check('String.replaceAll match', () => print(s.replaceAll(aString, '_')));
  check('String.replaceAll regexp', () => print(s.replaceAll(aRegExp, '_')));
  check('String.replaceAll mismatch', () => print(s.replaceAll(notString, '_')));

  var list = <int>[1, 2];
  check('List.insert mismatch', () => list.insert(notInt, 5));
  check('List.insert match', () => list.insert(0, ints[0]));
  check('List.insert bad element', () => list.insert(0, notInt));
  check('List.addAll match', () => list.addAll(ints));
  check('List.addAll mismatch', () => list.addAll(notIterable));
  print(list);

  var sb = StringBuffer();
  check('StringBuffer.writeAll match', () => sb.writeAll(someInts, '-'));
  check('StringBuffer.writeAll list match', () => sb.writeAll(ints, '+'));
  check('StringBuffer.writeAll mismatch', () => sb.writeAll(notIterable));
  print(sb);
}
