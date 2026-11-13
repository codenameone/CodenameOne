// Dart's toStringAsPrecision, toStringAsExponential and truncateToDouble on
// int, double and num receivers, statically typed and dynamic, plus
// isNegative for negative zero.

void show(String label, Object? Function() f) {
  try {
    print('$label: ${f()}');
  } catch (e) {
    print('$label: threw ${e.runtimeType}');
  }
}

void main() {
  final doubles = <double>[
    0.0, -0.0, 1.0, -1.0, 123.456, -123.456, 0.000123456, 1e-7, 1.5e-7,
    9.995, 0.5, 2.5, 1.25, 1e21, 1.2345e25, 5e-324, 1.7976931348623157e308,
    999.9, 0.1, 100.0, 1234567.0, double.nan, double.infinity,
    double.negativeInfinity,
  ];
  for (final d in doubles) {
    print('-- $d');
    show('p1', () => d.toStringAsPrecision(1));
    show('p3', () => d.toStringAsPrecision(3));
    show('p7', () => d.toStringAsPrecision(7));
    show('p21', () => d.toStringAsPrecision(21));
    show('e', () => d.toStringAsExponential());
    show('e0', () => d.toStringAsExponential(0));
    show('e2', () => d.toStringAsExponential(2));
    show('e20', () => d.toStringAsExponential(20));
    show('t', () => d.truncateToDouble());
    show('neg', () => d.isNegative);
    final dynamic y = d;
    show('dp3', () => y.toStringAsPrecision(3));
    show('de', () => y.toStringAsExponential());
    show('de2', () => y.toStringAsExponential(2));
    show('dt', () => y.truncateToDouble());
    show('dneg', () => y.isNegative);
    final num z = d;
    show('np3', () => z.toStringAsPrecision(3));
    show('ne1', () => z.toStringAsExponential(1));
    show('nt', () => z.truncateToDouble());
    show('nneg', () => z.isNegative);
  }
  final ints = <int>[0, 7, -7, 123456789, -1000, 9007199254740993];
  for (final i in ints) {
    print('-- int $i');
    show('p2', () => i.toStringAsPrecision(2));
    show('p20', () => i.toStringAsPrecision(20));
    show('e', () => i.toStringAsExponential());
    show('e3', () => i.toStringAsExponential(3));
    show('t', () => i.truncateToDouble());
    show('neg', () => i.isNegative);
    final dynamic y = i;
    show('dp2', () => y.toStringAsPrecision(2));
    show('de', () => y.toStringAsExponential());
    show('dt', () => y.truncateToDouble());
    final num z = i;
    show('np2', () => z.toStringAsPrecision(2));
    show('ne', () => z.toStringAsExponential());
    show('nt', () => z.truncateToDouble());
  }
  show('t-0.5', () => (-0.5).truncateToDouble().isNegative);
  show('dt-0.5', () => (-0.5 as dynamic).truncateToDouble().isNegative);
  show('t-2.7', () => (-2.7).truncateToDouble());
  show('t1e300', () => 1e300.truncateToDouble());
  show('p0', () => 1.0.toStringAsPrecision(0));
  show('p22', () => 1.0.toStringAsPrecision(22));
  show('e21', () => 1.0.toStringAsExponential(21));
  show('e-1', () => 1.0.toStringAsExponential(-1));
  show('dp0', () => (1.0 as dynamic).toStringAsPrecision(0));
}
