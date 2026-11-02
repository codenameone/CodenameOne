void check(String tag, void Function() f) {
  try {
    f();
    print('$tag ok');
  } on TypeError {
    print('$tag TypeError');
  } on NoSuchMethodError {
    print('$tag NoSuchMethodError');
  } catch (e) {
    print('$tag other ${e.runtimeType}');
  }
}

void main() {
  dynamic one = 1;
  dynamic word = 'x';
  dynamic intPred = (int x) => true;
  final names = <String>['a', 'b'];
  check('split', () => 'a,b'.split(one));
  check('contains', () => 'abc'.contains(one));
  check('removeWhere', () => names.removeWhere(intPred));
  check('fillRange', () => names.fillRange(0, 1, one));
  check('durationTimes', () => const Duration(seconds: 1) * word);
  check('hasMatch', () => RegExp('a').hasMatch(one));
  print(names);
  print('a,b'.split(word == 'x' ? ',' : one));
  print(const Duration(seconds: 2) * (one as dynamic));

  dynamic dt = DateTime(2020);
  check('dtAdd', () => dt.add(5));
  check('dtCompare', () => dt.compareTo('x'));
  print(dt.add(const Duration(days: 1)).day);
  print(dt.compareTo(DateTime(2021)));
  dynamic n = 5;
  check('radix', () => n.toRadixString('2'));
  print(n.toRadixString(2));
  dynamic d = const Duration(seconds: 3);
  check('dynDurationTimes', () => d * 'x');
  print(d * 2);
  dynamic re = RegExp('b');
  check('dynHasMatch', () => re.hasMatch(5));
  print(re.hasMatch('abc'));
  dynamic s = 'a-b';
  check('dynSplit', () => s.split(1));
  print(s.split('-'));

  dynamic sb = StringBuffer();
  sb.write('hi');
  sb.write(3);
  sb.writeln('!');
  check('sbMissing', () => sb.writeAll2('x'));
  print(sb.toString().trim());
  print(sb.length);
  dynamic u = Uri.parse('https://e.com/p?a=1&b=two');
  print(u.queryParameters['b']);
  print(u.queryParameters.length);
  print(u.path);
  check('uriMissing', () => u.nope);
}
