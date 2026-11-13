// Members reached through `this` inside an extension on a dart:core type keep the
// on-type's element type, and calls to functions returning Never are usable in any
// type position.

extension IterableDoubleStats on Iterable<double> {
  double get maxOrNaN {
    var iterator = this.iterator;
    if (!iterator.moveNext()) return double.nan;
    var value = iterator.current;
    var result = value;
    if (value.isNaN) return value;
    while (iterator.moveNext()) {
      value = iterator.current;
      if (value.isNaN) return value;
      if (value > result) result = value;
    }
    return result;
  }

  double get total {
    var sum = 0.0;
    for (var v in this) {
      sum += v;
    }
    return sum;
  }
}

extension IterableNumStats<T extends num> on Iterable<T> {
  T? get smallest {
    var iterator = this.iterator;
    if (!iterator.moveNext()) return null;
    var result = iterator.current;
    while (iterator.moveNext()) {
      var v = iterator.current;
      if (v < result) result = v;
    }
    return result;
  }
}

extension IterableNumNaN on Iterable<num> {
  bool get anyNaN {
    var iterator = this.iterator;
    while (iterator.moveNext()) {
      var v = iterator.current;
      if (v.isNaN || v.isInfinite) return true;
    }
    return false;
  }
}

Never _fail(String why) => throw UnsupportedError(why);

abstract class Store<E> {
  bool add(E value);
  E at(int index);
  Future<void> flush();
  int get length;
}

class NullStore<E> implements Store<E> {
  @override
  bool add(E value) => _fail('add');
  @override
  E at(int index) => _fail('at $index');
  @override
  Future<void> flush() => _fail('flush');
  @override
  int get length => 0;
}

abstract class Shelf<E> {
  Future<int> put(E value);
  Iterable<E> get items;
  String get label;
}

// Never-returning overrides of members typed Future<int>, Iterable<E> and String.
class ClosedShelf<E> implements Shelf<E> {
  @override
  Never put(E value) => throw UnsupportedError('closed put');
  @override
  Never get items => throw UnsupportedError('closed items');
  @override
  Never get label {
    throw UnsupportedError('closed label');
  }
}

mixin ReadOnlySet<E> {
  bool add(E value) => ReadOnlySet._throw();
  bool remove(Object? value) {
    return ReadOnlySet._throw();
  }

  static Never _throw() {
    throw UnsupportedError('read-only');
  }
}

class FrozenSet<E> with ReadOnlySet<E> {}

String attempt(String label, Object? Function() body) {
  try {
    return '$label: ${body()}';
  } on UnsupportedError catch (e) {
    return '$label threw ${e.message}';
  }
}

void main() {
  print(<double>[1.5, 4.25, 2.0].maxOrNaN);
  print(<double>[1.5, double.nan, 2.0].maxOrNaN.isNaN);
  print(<double>[].maxOrNaN.isNaN);
  print(<double>[1.0, 2.5].total);
  print(<int>[5, 3, 9].smallest);
  print(<double>[2.5, 0.5].smallest);
  print(<int>[].smallest);
  print(<num>[1, 2.5].anyNaN);
  print(<num>[1, double.nan].anyNaN);
  print(<num>[3, double.infinity].anyNaN);
  Store<String> s = NullStore<String>();
  print(attempt('add', () => s.add('x')));
  print(attempt('at', () => s.at(3)));
  print(attempt('flush', () => s.flush()));
  print(s.length);
  Shelf<int> shelf = ClosedShelf<int>();
  print(attempt('put', () => shelf.put(1)));
  print(attempt('items', () => shelf.items));
  print(attempt('label', () => shelf.label));
  var f = FrozenSet<int>();
  print(attempt('set add', () => f.add(1)));
  print(attempt('set remove', () => f.remove(1)));
}
