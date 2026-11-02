void tryIt(String label, void Function() f) {
  try { f(); print('$label ok'); } catch (e) { print('$label ${e is TypeError ? "TypeError" : e.runtimeType}'); }
}
Iterable<Object> lazy(List<Object> xs) sync* { for (final x in xs) { print('yield $x'); yield x; } }
void main() {
  List<num> ln = <int>[1, 2];
  tryIt('addAllDoubleLiteral', () => ln.addAll([3.5]));
  tryIt('addAllNumTypedIntLiteral', () => ln.addAll([3]));
  tryIt('addAllIntList', () => ln.addAll(<int>[4]));
  print(ln);
  tryIt('insertAllMixed', () => ln.insertAll(0, <num>[5, 6.5]));
  tryIt('setAllDouble', () => ln.setAll(0, <num>[7.5]));
  tryIt('setRangeDouble', () => ln.setRange(0, 1, <num>[8.5]));
  tryIt('setRangeInts', () => ln.setRange(0, 1, <int>[9]));
  print(ln);
  List<Object> lo = <String>['a'];
  tryIt('addAllUntypedIterableMixed', () => lo.addAll(lazy(['b', 2]).cast<Object>()));
  print(lo);
  Set<num> sn = <int>{1};
  tryIt('setAddAllDouble', () => sn.addAll(<num>[2, 2.5]));
  tryIt('setAddAllInts', () => sn.addAll(<int>[3]));
  print(sn);
}
