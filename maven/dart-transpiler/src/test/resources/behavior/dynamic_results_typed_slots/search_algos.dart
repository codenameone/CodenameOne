// Top-level functions an extension below names through an import prefix while
// declaring members of the same names.

int findBy<E, K>(List<E> list, K Function(E) keyOf, int Function(K, K) compare, E element) {
  final key = keyOf(element);
  for (var i = 0; i < list.length; i++) {
    if (compare(keyOf(list[i]), key) == 0) {
      return i;
    }
  }
  return -1;
}

T same<T>(T v) => v;
