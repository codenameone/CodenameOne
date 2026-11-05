import 'dart:collection';

void main() {
  final l = <int>[1, 2, 3, 4, 5];
  print(l.cast<num>().length);
  l.setRange(0, 2, [9, 8]);
  l.removeRange(3, 4);
  l.fillRange(0, 1, 7);
  l.replaceRange(0, 1, [6, 6]);
  l.setAll(0, [1]);
  print([42].single);
  final it = l.iterator;
  while (it.moveNext()) { print(it.current); }
  l.shuffle();
  l.sort();
  print(l);
  final ni = <int?>[1, null, 2];
  print(ni.nonNulls.toList());
  print(ni.cast<int?>().length);
  final m = <String, int>{'a': 1, 'b': 2};
  print(m.map((k, v) => MapEntry(v, k)));
  m.updateAll((k, v) => v * 10);
  print(m.cast<String, num>());
  final s = <String>{'x', 'y'};
  print(s.lookup('x'));
  print(s.cast<Object>().length);
  final str = '  hi  ';
  print(str.codeUnits);
  print('[' + str.trimLeft() + '][' + str.trimRight() + ']');
  print('a1b2'.splitMapJoin(RegExp(r'\d'), onMatch: (m) => '<${m.group(0)}>', onNonMatch: (n) => n.toUpperCase()));
  print('hello'.replaceRange(1, 3, 'XY'));
  print('a1b2'.replaceFirstMapped(RegExp(r'\d'), (m) => '#'));
  print('a1b2'.replaceAllMapped(RegExp(r'\d'), (m) => '[${m.start}-${m.end}]'));
  print(RegExp(r'\d').allMatches('a1b22').length);
  print('ab'.allMatches('xxabab').length);
  print(RegExp('a').matchAsPrefix('abc')?.end);
  print('ab'.matchAsPrefix('abc', 0)?.end);
  print((-5).sign);
  print((-5).isNegative);
  print(7.remainder(3));
  print(l.length);
  try { [1, 2].setRange(0, 3, [1, 2, 3]); } catch (e) { print('range error'); }
  print('xay'.splitMapJoin('a', onMatch: (m) => '-'));
  print('  '.trimLeft().isEmpty);
}
