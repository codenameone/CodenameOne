// Codename One Dart runtime API stubs -- dart:core instance members.
//
// Backed by dart.core.CoreExt (maven/dart-runtime): members of String, List,
// Iterable, Map, Set, int, RegExp and Iterator the transpiler has no inline
// lowering for. Each is a static method taking the receiver first. They live
// beside dart_io.dart because the transpiler reads its stubs from this module.

@JavaName('dart.core.CoreExt')
extension DartCoreStringExt on String {
  external List<int> get codeUnits;
  external String trimLeft();
  external String trimRight();
  external String replaceRange(int start, int? end, String replacement);
  external String splitMapJoin(Pattern pattern, {String Function(RegExpMatch)? onMatch, String Function(String)? onNonMatch});
  external String replaceFirstMapped(Pattern from, String Function(RegExpMatch) replace, [int startIndex = 0]);
  external String replaceAllMapped(Pattern from, String Function(RegExpMatch) replace);
  external Iterable<RegExpMatch> allMatches(String string, [int start = 0]);
  external RegExpMatch? matchAsPrefix(String string, [int start = 0]);
}

@JavaName('dart.core.CoreExt')
extension DartCoreRegExpExt on RegExp {
  external RegExpMatch? matchAsPrefix(String string, [int start = 0]);
}

@JavaName('dart.core.CoreExt')
extension DartCoreListExt<E> on List<E> {
  // No type parameter: the transpiler's stub-extension call path cannot yet
  // place explicit type arguments, and erasure makes the cast a view anyway.
  external List<dynamic> cast();
  external void setRange(int start, int end, Iterable<E> iterable, [int skipCount = 0]);
  external void removeRange(int start, int end);
  external void fillRange(int start, int end, [E? fillValue]);
  external void replaceRange(int start, int end, Iterable<E> replacements);
  external void setAll(int index, Iterable<E> iterable);
  external E get single;
  external Iterator<dynamic> get iterator;
  external void shuffle([Object? random]);
  external Iterable<E> get nonNulls;
}

@JavaName('dart.core.CoreExt')
extension DartCoreIterableExt<E> on Iterable<E> {
  external Iterable<dynamic> cast();
  external Iterator<dynamic> get iterator;
  external Iterable<E> get nonNulls;
  external E get single;
}

@JavaName('dart.core.CoreExt')
extension DartCoreSetExt<E> on Set<E> {
  external Set<dynamic> cast();
  external E? lookup(Object? object);
  external E get single;
  external Iterator<dynamic> get iterator;
}

@JavaName('dart.core.CoreExt')
extension DartCoreMapExt<K, V> on Map<K, V> {
  external Map<K2, V2> map<K2, V2>(MapEntry<K2, V2> Function(K key, V value) convert);
  external void updateAll(V Function(K key, V value) update);
  external Map<dynamic, dynamic> cast();
}

@JavaName('dart.core.CoreExt')
extension DartCoreIntExt on int {
  external int get sign;
  external bool get isNegative;
  external int remainder(int other);
}

@JavaName('dart.core.CoreExt')
extension DartCoreIteratorExt<E> on Iterator<E> {
  external bool moveNext();
  external E get current;
}
