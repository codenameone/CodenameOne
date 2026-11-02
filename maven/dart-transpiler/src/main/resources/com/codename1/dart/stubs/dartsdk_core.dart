// dart:core member signatures, as the Dart SDK declares them (lib/core/*.dart).
//
// These are declarations only, never emitted: dart:core's String, List, Set, Map and
// Iterable are lowered by the transpiler's intrinsics, not called through a stub. The
// transpiler reads the declared parameter types to check a dynamic argument the way
// Dart's implicit downcast checks it (JavaEmitter.checkDynamicCoreArgs). A file named
// dartsdk_*.dart is kept apart from the stub classes (StubRegistry.sdkClasses), so
// declaring List here does not make List a stub class.
//
// Copy a member's signature from the SDK verbatim when adding one; a parameter whose
// type a value cannot fail (Object?, a callback) needs no entry but is harmless.

abstract class Iterable<E> {
  external E elementAt(int index);
  external Iterable<E> take(int count);
  external Iterable<E> skip(int count);
  external String join([String separator = ""]);
  external Iterable<E> followedBy(Iterable<E> other);
}

abstract class List<E> implements Iterable<E> {
  external factory List.from(Iterable elements, {bool growable = true});
  external factory List.of(Iterable<E> elements, {bool growable = true});
  external void add(E value);
  external void addAll(Iterable<E> iterable);
  external void insert(int index, E element);
  external int indexOf(E element, [int start = 0]);
  external int lastIndexOf(E element, [int? start]);
  external E removeAt(int index);
  external List<E> sublist(int start, [int? end]);
  external Iterable<E> getRange(int start, int end);
  external void removeRange(int start, int end);
}

abstract class Set<E> implements Iterable<E> {
  external factory Set.from(Iterable elements);
  external factory Set.of(Iterable<E> elements);
  external bool add(E value);
  external void addAll(Iterable<E> elements);
}

abstract class Map<K, V> {
  external factory Map.from(Map other);
  external factory Map.of(Map<K, V> other);
  external void addAll(Map<K, V> other);
}

abstract class String {
  external bool endsWith(String other);
  external String padLeft(int width, [String padding = ' ']);
  external String padRight(int width, [String padding = ' ']);
  external String substring(int start, [int? end]);
  external int codeUnitAt(int index);
  external int compareTo(String other);
}
