class Box {
  final String label;
  Box(this.label);
  String show() => 'B.Box("$label")';
}

class Shelf<K> {
  final Map<K, int> counts = <K, int>{};
  void put(K key) => counts[key] = (counts[key] ?? 0) + 1;
  int count(K key) => counts[key] ?? 0;
  String describe() => 'B.Shelf keys ${counts.keys.toList()}';
}

mixin Tagger {
  String tag(String s) => '<b:$s>';
}

class Sizer {
  int size(String s) => s.length * 10;
}

class LoudSizer implements Sizer {
  int size(String s) => -s.length;
}

class Worker with Tagger {
  String work(String s) => tag(s);
}

String useB() {
  final s = Shelf<String>();
  s.put('x');
  s.put('x');
  s.put('y');
  Sizer z = LoudSizer();
  return '${Box('q').show()} ${s.describe()} ${s.count('x')} ${Worker().work('w')} ${z.size('abc')}';
}
