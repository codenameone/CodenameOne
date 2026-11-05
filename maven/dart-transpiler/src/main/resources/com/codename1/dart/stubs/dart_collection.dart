// Built-in transpiler stubs for the dart:collection mixins. These are always
// registered (independent of the runtime stub classpath) so that classes such
// as `class Board extends Object with IterableMixin<T>` resolve their mixin
// instead of raising E0402. Each maps to a Java interface with default methods
// (its @JavaName); the applying class supplies the abstract members it requires
// (e.g. `iterator` for IterableMixin), and every other member resolves as an
// inherited default. The Java interfaces live in the dart-runtime module.

// Abstract base of the Iterable protocol: the class provides `iterator`, and the
// mixin contributes forEach / map / where / length / ... as default methods.
@JavaName('dart.collection.IterableMixin')
mixin IterableMixin<E> {
  Iterator<E> get iterator;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  bool contains(Object? element);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
  List<E> toList({bool growable = true});
  Set<E> toSet();
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
}

// List protocol: adds indexed access on top of the Iterable surface.
@JavaName('dart.collection.ListMixin')
mixin ListMixin<E> implements IterableMixin<E> {
  int get length;
  set length(int newLength);
  E operator [](int index);
  void operator []=(int index, E value);
  Iterator<E> get iterator;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  void add(E element);
  void addAll(Iterable<E> iterable);
  E removeLast();
  void clear();
  void fillRange(int start, int end, [E? fill]);
  void setAll(int index, Iterable<E> iterable);
  void setRange(int start, int end, Iterable<E> iterable, [int skipCount = 0]);
  bool contains(Object? element);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  int indexOf(Object? element, [int start = 0]);
  int lastIndexOf(Object? element);
  List<E> toList({bool growable = true});
  List<E> sublist(int start, [int? end]);
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
}

// Map protocol.
@JavaName('dart.collection.MapMixin')
mixin MapMixin<K, V> {
  V? operator [](Object? key);
  void operator []=(K key, V value);
  Iterable<K> get keys;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  bool containsKey(Object? key);
  bool containsValue(Object? value);
  void forEach(void Function(K key, V value) action);
  void addAll(Map<K, V> other);
  V putIfAbsent(K key, V Function() ifAbsent);
  V? remove(Object? key);
  void clear();
}

// Set protocol.
@JavaName('dart.collection.SetMixin')
mixin SetMixin<E> {
  Iterator<E> get iterator;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  bool contains(Object? element);
  E? lookup(Object? element);
  bool add(E value);
  bool remove(Object? value);
  Set<E> toSet();
  void clear();
  void addAll(Iterable<E> elements);
  void removeAll(Iterable<Object?> elements);
  bool containsAll(Iterable<Object?> other);
  void removeWhere(bool Function(E element) test);
  void retainWhere(bool Function(E element) test);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
  List<E> toList({bool growable = true});
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
}

// dart:collection's *Base classes, declared as mixins so a class extending one takes the
// mixin-as-superclass path: same protocol and runtime class as the mixin of the family.

@JavaName('dart.collection.IterableMixin')
mixin IterableBase<E> {
  external static String iterableToFullString(Iterable iterable, [String leftDelimiter, String rightDelimiter]);
  Iterator<E> get iterator;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  bool contains(Object? element);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
  List<E> toList({bool growable = true});
  Set<E> toSet();
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
}

@JavaName('dart.collection.ListMixin')
mixin ListBase<E> {
  int get length;
  set length(int newLength);
  E operator [](int index);
  void operator []=(int index, E value);
  Iterator<E> get iterator;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  void add(E element);
  void addAll(Iterable<E> iterable);
  E removeLast();
  void clear();
  void fillRange(int start, int end, [E? fill]);
  void setAll(int index, Iterable<E> iterable);
  void setRange(int start, int end, Iterable<E> iterable, [int skipCount = 0]);
  bool contains(Object? element);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  int indexOf(Object? element, [int start = 0]);
  int lastIndexOf(Object? element);
  List<E> toList({bool growable = true});
  List<E> sublist(int start, [int? end]);
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
}

@JavaName('dart.collection.MapMixin')
mixin MapBase<K, V> {
  external static String mapToString(Map map);
  V? operator [](Object? key);
  void operator []=(K key, V value);
  Iterable<K> get keys;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  bool containsKey(Object? key);
  bool containsValue(Object? value);
  void forEach(void Function(K key, V value) action);
  void addAll(Map<K, V> other);
  V putIfAbsent(K key, V Function() ifAbsent);
  V? remove(Object? key);
  void clear();
}

@JavaName('dart.collection.UnmodifiableMapBase')
mixin UnmodifiableMapBase<K, V> implements MapBase<K, V> {
  V? operator [](Object? key);
  void operator []=(K key, V value);
  Iterable<K> get keys;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  bool containsKey(Object? key);
  bool containsValue(Object? value);
  void forEach(void Function(K key, V value) action);
  void addAll(Map<K, V> other);
  V putIfAbsent(K key, V Function() ifAbsent);
  V? remove(Object? key);
  void clear();
}

@JavaName('dart.collection.SetMixin')
mixin SetBase<E> {
  external static String setToString(Set set);
  Iterator<E> get iterator;
  int get length;
  bool get isEmpty;
  bool get isNotEmpty;
  E get first;
  E get last;
  E get single;
  E firstWhere(bool Function(E element) test, {E Function()? orElse});
  E lastWhere(bool Function(E element) test, {E Function()? orElse});
  E singleWhere(bool Function(E element) test, {E Function()? orElse});
  bool contains(Object? element);
  E? lookup(Object? element);
  bool add(E value);
  bool remove(Object? value);
  Set<E> toSet();
  void clear();
  void addAll(Iterable<E> elements);
  void removeAll(Iterable<Object?> elements);
  bool containsAll(Iterable<Object?> other);
  void removeWhere(bool Function(E element) test);
  void retainWhere(bool Function(E element) test);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
  List<E> toList({bool growable = true});
  Iterable<E> where(bool Function(E element) test);
  Iterable<T> map<T>(T Function(E e) toElement);
  Iterable<E> skip(int count);
  Iterable<E> take(int count);
  T fold<T>(T initialValue, T Function(T previousValue, E element) combine);
}
