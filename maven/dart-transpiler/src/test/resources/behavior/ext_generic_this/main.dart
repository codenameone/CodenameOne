// Extension bodies that hand `this` to a generic function, spread it into a
// list literal, and pass it on to another generic helper.
Map<K, T> lastByKey<T, K>(Iterable<T> items, K Function(T) key) {
  var out = <K, T>{};
  for (var e in items) {
    out[key(e)] = e;
  }
  return out;
}

void sortWith<E, K extends Comparable<K>>(List<E> list, K Function(E) keyOf) {
  list.sort((a, b) => keyOf(a).compareTo(keyOf(b)));
}

extension IterX<T> on Iterable<T> {
  Map<K, T> lastBy<K>(K Function(T) key) => lastByKey(this, key);
  List<T> sortedBy<K extends Comparable<K>>(K Function(T) keyOf) {
    var elements = [...this];
    sortWith<T, K>(elements, keyOf);
    return elements;
  }
}

extension ListX<E> on List<E> {
  void sortByKey<K extends Comparable<K>>(K Function(E) keyOf) {
    sortWith(this, keyOf);
  }
}

void main() {
  var words = ['pear', 'fig', 'banana', 'kiwi'];
  print(words.lastBy((w) => w.length));
  List<String> s = words.sortedBy((w) => w.length);
  print(s);
  var l = [3, 1, 2];
  l.sortByKey((x) => -x);
  print(l);
}
