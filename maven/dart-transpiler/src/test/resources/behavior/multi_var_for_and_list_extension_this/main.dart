// Multi-variable for-loop declarations and implicit-this members in a List extension.
extension Walk<E> on List<E> {
  int countWhere(bool Function(E) test) {
    var n = 0;
    for (var index = 0; index < length; index++) {
      if (test(this[index])) n++;
    }
    return n;
  }
}

void reverseRange<E>(List<E> elements, int start, int end) {
  for (var i = start, j = end - 1; i < j; i++, j--) {
    var tmp = elements[i];
    elements[i] = elements[j];
    elements[j] = tmp;
  }
}

void main() {
  final xs = [1, 2, 3, 4, 5];
  print(xs.countWhere((int x) => x.isOdd));
  reverseRange(xs, 1, 4);
  print(xs);
}
