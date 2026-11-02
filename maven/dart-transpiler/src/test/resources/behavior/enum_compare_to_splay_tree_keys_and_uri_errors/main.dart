import 'dart:collection';

enum Priority implements Comparable<Priority> {
  low(3),
  high(1),
  mid(2);

  final int rank;
  const Priority(this.rank);

  @override
  int compareTo(Priority other) => rank.compareTo(other.rank);
}

int viaComparable(Comparable<Priority> a, Priority b) => a.compareTo(b);

void main() {
  print(Priority.low.compareTo(Priority.high));
  print(Priority.high.compareTo(Priority.mid));
  dynamic d = Priority.mid;
  print(d.compareTo(Priority.low));
  print(viaComparable(Priority.low, Priority.mid));
  final list = [Priority.low, Priority.mid, Priority.high];
  list.sort();
  print(list.map((p) => p.name).join(','));
  final list2 = [Priority.mid, Priority.low, Priority.high];
  list2.sort((a, b) => b.compareTo(a));
  print(list2.map((p) => p.name).join(','));
  print(Comparable.compare(Priority.low, Priority.high));
  final tree = SplayTreeMap<Priority, String>();
  tree[Priority.low] = 'L';
  tree[Priority.high] = 'H';
  tree[Priority.mid] = 'M';
  print(tree.keys.map((p) => p.name).join(','));

  final m = SplayTreeMap<int, String>();
  for (final k in [5, 1, 9, 3]) {
    m[k] = 'v$k';
  }
  print(m.firstKey());
  print(m.lastKey());
  print(m.firstKeyAfter(3));
  print(m.firstKeyAfter(4));
  print(m.firstKeyAfter(9));
  print(m.lastKeyBefore(5));
  print(m.lastKeyBefore(1));
  final empty = SplayTreeMap<String, int>();
  print(empty.firstKey());
  print(empty.lastKey());
  final s = SplayTreeSet<String>()..addAll(['pear', 'apple', 'fig']);
  print(s.first);
  print(s.last);

  for (final bad in ['http://[::1', 'http://a:b/', 'a b:c', '%zz', ':x', '1a:b', 'http://[::1]x/',
      'http://[:1::2]/', 'http://[1::2::3]/', 'http://[1:2:]/', 'http://[v1.^]/',
      'http://u@h:8 0/', 'http://[fe80::1%25%zz]/']) {
    try {
      Uri.parse(bad);
      print('parsed $bad');
    } on FormatException catch (e) {
      print(e);
      print(e.offset);
    }
  }
}
