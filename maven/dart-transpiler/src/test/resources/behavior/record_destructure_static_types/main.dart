(int, String) pair(int n) => (n * 2, 'n$n');

({int count, double ratio}) stats(List<int> xs) =>
    (count: xs.length, ratio: xs.isEmpty ? 0.0 : xs.reduce((a, b) => a + b) / xs.length);

Iterable<(int, int)> swapped(List<(int, int)> src) sync* {
  for (final p in src) {
    final (a, b) = p;
    yield (b, a);
  }
}

Future<int> sumAsync(List<(int, int)> src) async {
  var total = 0;
  for (final p in src) {
    await Future<void>.delayed(Duration.zero);
    final (x, y) = p;
    total += x * y;
  }
  return total;
}

void main() async {
  final (a, b) = (3, 4);
  final int c = a * b + 1;
  print('c=$c ${(a + b).isEven}');
  var (n, label) = pair(5);
  n += 1;
  print('$n ${label.toUpperCase()} ${label.length}');
  final (:count, :ratio) = stats([1, 2, 3, 6]);
  print('${count + 1} ${ratio.toStringAsFixed(2)}');
  final List<(int, int)> list = [(1, 2), (3, 4)];
  for (final (p, q) in swapped(list)) {
    print('swap ${p - q}');
  }
  final ((i, j), k) = ((1, 2), 3);
  print(i + j + k);
  print(await sumAsync(list));
}
