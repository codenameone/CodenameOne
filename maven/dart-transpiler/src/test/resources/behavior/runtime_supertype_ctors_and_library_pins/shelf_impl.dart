// A library declaring classes with the SAME names, implementing the interface's.
import 'shelf_api.dart' as api;

class Crate<V> implements api.Crate<V> {
  @override
  final V item;
  Crate(this.item);
}

class Shelf implements api.Shelf {
  final Map<String, Object?> seed;
  Shelf(this.seed);

  @override
  Crate<V> open<V>(String name, {api.Crate<V> Function(String, Shelf)? maker}) {
    if (maker != null) {
      return maker(name, this) as Crate<V>;
    }
    return Crate<V>(seed[name] as V);
  }
}
