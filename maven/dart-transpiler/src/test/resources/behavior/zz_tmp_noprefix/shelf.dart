import 'shelf_stub.dart';

class Shelf implements ShelfBase {
  final Map<String, Bin<Object?>> bins = {};
  @override
  Future<Crate2<V>> open<V>(String name) async {
    final c = Crate2<V>(name);
    c.bin = await openBin(name);
    return c;
  }

  Future<Bin<E>> openBin<E>(String name) async => Bin<E>();
}

class Crate2<V> implements CrateBase<V> {
  @override
  final String name;
  Bin<V>? bin;
  Crate2(this.name);
  bool holds(Object? o) => o is V && (bin?.holds(o) ?? false);
}

class Bin<E> {
  Type get valueType => E;
  bool holds(Object? o) => o is E;
}

abstract class LazyBin<E> {
  bool get lazy => true;
}

String binName(Map<String, Bin<Object?>> bins, String n) {
  var bin = bins[n];
  if (bin != null) {
    return bin is LazyBin ? 'LazyBin<${bin.valueType}>' : 'Bin<${bin.valueType}>';
  }
  return 'none';
}

Future<List<Object?>> exerciseShelf() async {
  final shelf = Shelf();
  final c = await shelf.open<int>('c');
  shelf.bins['x'] = Bin<String>();
  return [c.holds(1), c.holds('s'), binName(shelf.bins, 'x')];
}
