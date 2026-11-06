import 'dart:collection';

class Shelf<K> {
  K pick(int index) => throw UnimplementedError();
}

class Labelled<T> extends Shelf<T> {
  String label(T value) => 'plain $value';
}

mixin Labels<T> on IterableBase<T> {
  T pick(int index) => elementAt(length - 1 - index);
  String label(T value) => '<$value>';
}

class WordShelf extends IterableBase<String> with Labels<String> implements Labelled<String> {
  final List<String> words;
  WordShelf(this.words);
  Iterator<String> get iterator => words.iterator;
}

String useB() {
  Labelled<String> s = WordShelf(['x', 'y', 'z']);
  return '${s.pick(0)} ${s.label('q')}';
}
