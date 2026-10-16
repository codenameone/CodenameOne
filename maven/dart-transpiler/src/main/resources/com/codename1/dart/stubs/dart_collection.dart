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
  bool contains(Object? element);
  void forEach(void Function(E element) action);
  E elementAt(int index);
  bool any(bool Function(E element) test);
  bool every(bool Function(E element) test);
  String join([String separator = '']);
}

// List protocol: adds indexed access on top of the Iterable surface.
@JavaName('dart.collection.ListMixin')
mixin ListMixin<E> {
  int get length;
  set length(int newLength);
  E operator [](int index);
  void operator []=(int index, E value);
  Iterator<E> get iterator;
  bool get isEmpty;
  bool get isNotEmpty;
  void add(E element);
  bool contains(Object? element);
  void forEach(void Function(E element) action);
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
  void forEach(void Function(K key, V value) action);
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
  bool contains(Object? element);
  bool add(E value);
  bool remove(Object? value);
  void forEach(void Function(E element) action);
}
