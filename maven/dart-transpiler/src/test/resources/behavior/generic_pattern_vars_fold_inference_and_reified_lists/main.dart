import 'dart:async';

sealed class Outcome<T> {}
class Hit<T> extends Outcome<T> { final T value; Hit(this.value); }
class Miss<T> extends Outcome<T> { final String why; Miss(this.why); }

class Pair<A, B> { final A first; final B second; Pair(this.first, this.second); }

class Line { final int qty; final double price; Line(this.qty, this.price); double cost() => qty * price; }

class QuotaExceeded implements Exception {
  final int limit;
  QuotaExceeded(this.limit);
  @override
  String toString() => 'QuotaExceeded($limit)';
}

String label(Outcome<int> o) => switch (o) {
      Hit(value: final v) when v >= 100 => 'large ${v + 1}',
      Hit(:final value) => 'small ${value * 2}',
      Miss(:final why) => 'miss $why',
    };

String names(Outcome<List<String>> o) {
  if (o case Hit(:final value)) {
    return value.map((s) => s.toUpperCase()).join('+');
  }
  return 'none';
}

String pairText(Object p) => switch (p) {
      Pair<String, int>(first: final f, second: final s) => '$f=${s + 1}',
      _ => 'other',
    };

Future<void> main() async {
  print(label(Hit(7)));
  print(label(Hit(250)));
  print(label(Miss('timeout')));
  print(names(Hit(['a', 'b'])));
  print(names(Miss('x')));
  print(pairText(Pair('k', 41)));
  print(pairText(3));

  final lines = [Line(2, 1.25), Line(3, 0.5)];
  print(lines.fold<double>(0, (sum, l) => sum + l.cost()).toStringAsFixed(2));
  final count = lines.fold<int>(0, (n, l) => n + l.qty);
  print(count + 1);

  final ctl = StreamController<int>();
  final pending = ctl.stream.where((e) => e > 1).map((e) => e * 3).toList();
  for (final n in [1, 2, 3]) {
    ctl.add(n);
  }
  await ctl.close();
  final got = await pending;
  print(got);
  print(got.reduce((a, b) => a + b));

  try {
    throw QuotaExceeded(5);
  } on QuotaExceeded catch (e) {
    print('caught $e ${e.limit}');
  }
  try {
    throw QuotaExceeded(9);
  } on Exception catch (e) {
    print('as exception $e');
  }
}
