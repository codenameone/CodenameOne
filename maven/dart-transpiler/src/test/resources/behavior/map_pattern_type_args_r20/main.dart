String describe(Object? o) {
  switch (o) {
    case <String, int>{'n': var n}:
      return 'ints $n';
    case <String, String>{'n': var s}:
      return 'strings $s';
    case <String, num>{'n': var x}:
      return 'nums $x';
    case <String, dynamic>{'n': var d}:
      return 'dynamic $d';
    case {'n': var any}:
      return 'any $any';
    default:
      return 'none';
  }
}

void main() {
  print(describe(<String, int>{'n': 1}));
  print(describe(<String, String>{'n': 'a'}));
  print(describe(<String, double>{'n': 2.5}));
  print(describe(<String, Object>{'n': 3}));
  print(describe(<int, int>{1: 1}));
  print(describe(<Object, Object>{'n': true}));
  print(describe({'n': 4}));
  print(describe('n'));
  final Object m = <String, int>{'a': 1, 'b': 2};
  if (m case <String, String>{'a': _}) {
    print('wrong');
  } else {
    print('not strings');
  }
  var <String, int>{'b': b} = m as Map<String, int>;
  print('b $b');
}
