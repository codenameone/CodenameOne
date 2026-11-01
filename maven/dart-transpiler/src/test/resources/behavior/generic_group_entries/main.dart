Map<T, List<S>> groupIt<S, T>(Iterable<S> values, T Function(S) key) {
  var map = <T, List<S>>{};
  for (var element in values) {
    (map[key(element)] ??= []).add(element);
  }
  return map;
}

class Item implements Comparable<Item> {
  final String name;
  final int size;
  Item(this.name, this.size);
  @override
  int compareTo(Item other) => name.compareTo(other.name);
}

void main() {
  final items = [Item('a', 1), Item('bb', 2), Item('c', 1)];
  final byUrl = groupIt<Item, Object>(items, (i) => i.size);
  for (var list in byUrl.values) {
    list.sort((a, b) => b.name.compareTo(a.name));
  }
  final out = byUrl.entries.expand((entry) {
    final key = entry.key;
    final forKey = entry.value;
    final names = <String>[];
    for (var it in forKey) {
      names.add('$key:${it.name}');
    }
    for (var it in forKey.skip(1)) {
      names.add('skip:${it.name.length}');
    }
    return names;
  }).toList();
  print(out);
  items.sort((x, y) => y.compareTo(x));
  print(items.map((i) => i.name).join(' '));
}
