abstract class ShelfBase {
  Future<CrateBase<V>> open<V>(String name);
}

abstract class CrateBase<V> {
  String get name;
}
