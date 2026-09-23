// Shifts, rounding, trim and startsWith with Dart's rules; Set.add's result; a
// mixed int/double conditional; Map<int, int>.entries; Iterable.forEach; and a
// list modified while it is iterated.

void main() {
  var big = 64;
  var neg = -1;
  print(1 >> big);
  print(-8 >> big);
  print(1 << big);
  try {
    print(1 << neg);
  } on ArgumentError {
    print('negative shift refused');
  }
  var n = 5;
  n <<= big;
  print(n);
  print(1 << 3);

  print((-1.5).round());
  print((2.5).round());
  print((-1.5).roundToDouble());
  print(0.49999999999999994.round());

  print('\u00a0 padded \u2003'.trim().length);
  print('abc'.startsWith('b', 1));

  final seen = <int>{};
  var added = seen.add(1);
  var again = seen.add(1);
  print('$added $again');

  var flag = true;
  Object picked = flag ? 1 : 1.5;
  print(picked is int);

  final counts = <int, int>{1: 10, 2: 20};
  print(counts.entries.map((e) => e.key + e.value).toList());

  var total = 0;
  [1, 2, 3].map((x) => x * 2).forEach((x) {
    total += x;
  });
  print(total);

  final grow = [1, 2];
  try {
    for (final x in grow) {
      grow.add(x);
    }
    print('no error');
  } catch (e) {
    print('modified during iteration');
  }
}
