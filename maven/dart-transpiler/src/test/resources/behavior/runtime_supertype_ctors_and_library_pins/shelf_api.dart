// The interface library: its own Shelf and Crate.
abstract class Crate<V> {
  V get item;
}

abstract class Shelf {
  Crate<V> open<V>(String name, {Crate<V> Function(String, Shelf)? maker});
}
